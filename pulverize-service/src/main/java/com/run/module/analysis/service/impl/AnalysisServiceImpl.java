package com.run.module.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.run.common.ai.AiClient;
import com.run.common.exception.BizException;
import com.run.module.analysis.dto.AnalysisResultVO;
import com.run.module.analysis.entity.RunningAnalysis;
import com.run.module.analysis.mapper.RunningAnalysisMapper;
import com.run.module.analysis.service.AnalysisService;
import com.run.common.math.VdotCalculator;
import com.run.module.analysis.dto.MuscleMapVO;
import com.run.module.analysis.dto.VdotVO;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.entity.RunningLap;
import com.run.module.running.mapper.RunningLapMapper;
import com.run.module.running.service.RunningStatsService;
import com.run.module.user.entity.User;
import com.run.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 跑步数据分析
 *
 * - 所有分析维度的 Map key 一律使用中文，前端直接展示（要求：分析界面全中文名称）
 * - aiSummary 优先由大模型生成（AiClient / DeepSeek），未配置 key 时自动降级为规则文案
 * - 心率区间: 优先使用 COROS 官方区间（活动 hr_zone_json 中 type=126 分组的 percent/second），
 *            没有同步到心率区间时回退为按用户档案储备心率(HRR)分桶
 * - 分析缓存类型为 FULL_V3（V2 及更早的缓存不再读取：心率区间已改为 COROS 官方数据/HRR 口径，重新分析即得）
 */
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private static final String ANALYSIS_TYPE = "FULL_V3";

    /** 心率区间 key（COROS zoneIndex 0-5 顺序） */
    private static final String[] HR_ZONE_KEYS =
            {"恢复", "Z1热身", "Z2燃脂", "Z3有氧", "Z4乳酸阈", "Z5无氧"};

    private final RunningStatsService runningStats;
    private final RunningLapMapper lapMapper;
    private final RunningAnalysisMapper analysisMapper;
    private final UserMapper userMapper;
    private final AiClient aiClient;

    @Override
    public AnalysisResultVO analyzeActivity(Long userId, Long activityId) {
        RunningActivity activity = runningStats.getById(userId, activityId);
        if (activity == null) {
            throw new BizException("跑步记录不存在");
        }

        List<RunningLap> laps = lapMapper.selectList(
                new LambdaQueryWrapper<RunningLap>()
                        .eq(RunningLap::getActivityId, activityId)
                        .orderByAsc(RunningLap::getLapIndex));

        AnalysisResultVO vo = new AnalysisResultVO();
        vo.setActivityId(activityId);
        vo.setActivityName(activity.getActivityName());

        // 1. 配速分析
        vo.setPaceAnalysis(analyzePace(laps, activity));

        // 2. 心率分析
        vo.setHeartRateAnalysis(analyzeHeartRate(laps, activity));

        // 3. 步频分析
        vo.setCadenceAnalysis(analyzeCadence(laps, activity));

        // 4. 疲劳度分析
        vo.setFatigueAnalysis(analyzeFatigue(laps));

        // 5. 训练效果评估
        vo.setTrainingEffect(analyzeTrainingEffect(activity, laps));

        // 6. 综合评分
        int score = calculateScore(vo);
        vo.setScore(score);

        // 7. AI 文字摘要（大模型优先，规则兜底）
        vo.setAiSummary(buildAiSummary(vo, activity));

        // 存储分析结果
        RunningAnalysis analysis = new RunningAnalysis();
        analysis.setUserId(userId);
        analysis.setActivityId(activityId);
        analysis.setAnalysisType(ANALYSIS_TYPE);
        analysis.setScore(score);
        analysis.setAiSummary(vo.getAiSummary());
        analysis.setAnalysisData(serializeAnalysis(vo));
        analysisMapper.insert(analysis);

        return vo;
    }

    @Override
    public AnalysisResultVO getAnalysis(Long userId, Long activityId) {
        RunningAnalysis analysis = analysisMapper.selectOne(
                new LambdaQueryWrapper<RunningAnalysis>()
                        .eq(RunningAnalysis::getUserId, userId)
                        .eq(RunningAnalysis::getActivityId, activityId)
                        .eq(RunningAnalysis::getAnalysisType, ANALYSIS_TYPE)
                        .orderByDesc(RunningAnalysis::getCreateTime)
                        .last("LIMIT 1"));

        if (analysis != null) {
            AnalysisResultVO vo = deserializeAnalysis(analysis.getAnalysisData());
            vo.setScore(analysis.getScore());
            vo.setAiSummary(analysis.getAiSummary());
            RunningActivity activity = runningStats.getById(userId, activityId);
            if (activity != null) vo.setActivityName(activity.getActivityName());
            return vo;
        }
        // 没有存储的分析则实时计算
        return analyzeActivity(userId, activityId);
    }

    // ==================== 跑力指数 VDOT ====================

    /**
     * 跑力指数：遍历【全部历史】满足条件的跑步（距离 ≥ 2km、时长 ≥ 8 分钟、配速 2:00~15:00/km），
     * 取 VDOT 最高的一次（历史最优有氧表现，不限"近期比赛"），推算指数、等效完赛与训练配速区间。
     */
    @Override
    public VdotVO getVdot(Long userId) {
        VdotVO vo = new VdotVO();
        List<RunningActivity> activities = runningStats.listAll(userId);

        RunningActivity best = null;
        double bestVdot = 0;
        for (RunningActivity a : activities) {
            Double dist = a.getDistanceM();
            Integer dur = a.getDurationSeconds();
            if (dist == null || dur == null || dist < 2000 || dur < 480) {
                continue;
            }
            double paceSecKm = dur / (dist / 1000.0);
            if (paceSecKm < 120 || paceSecKm > 900) {
                continue; // 剔除异常配速（快于 2:00 或慢于 15:00 每公里）
            }
            double vdot = VdotCalculator.compute(dist, dur);
            if (vdot > bestVdot) {
                bestVdot = vdot;
                best = a;
            }
        }

        if (best == null || bestVdot <= 0) {
            vo.setSource("NONE");
            vo.setMessage("数据不足：完成一次 2 公里以上、8 分钟以上的跑步后，即可基于你的历史数据推算跑力指数。");
            return vo;
        }

        vo.setSource("RACE");
        vo.setVdot(Math.round(bestVdot * 10) / 10.0);
        vo.setMessage(String.format("基于你的全部历史数据（共 %d 次跑步）中最优的有氧表现推算。", activities.size()));

        VdotVO.Race race = new VdotVO.Race();
        race.setActivityId(best.getId());
        race.setActivityName(best.getActivityName() == null ? "跑步记录" : best.getActivityName());
        race.setRaceDate(best.getStartTime() == null ? "" : best.getStartTime().toLocalDate().toString());
        race.setDistanceKm(Math.round(best.getDistanceM() / 100.0) / 10.0);
        race.setDurationSec(best.getDurationSeconds());
        race.setPaceSecKm((int) Math.round(best.getDurationSeconds() / (best.getDistanceM() / 1000.0)));
        vo.setRace(race);

        vo.setEquivalents(Map.of(
                "5K", VdotCalculator.equivalentTimeSec(bestVdot, 5000),
                "10K", VdotCalculator.equivalentTimeSec(bestVdot, 10000),
                "半马", VdotCalculator.equivalentTimeSec(bestVdot, 21097.5),
                "全马", VdotCalculator.equivalentTimeSec(bestVdot, 42195)));

        double vvo2 = VdotCalculator.vvo2maxSpeed(bestVdot);
        List<VdotVO.Zone> zones = new ArrayList<>();
        for (int i = 0; i < VdotCalculator.ZONE_NAMES.length; i++) {
            VdotVO.Zone z = new VdotVO.Zone();
            z.setName(VdotCalculator.ZONE_NAMES[i]);
            z.setPercentLabel(VdotCalculator.ZONE_LABELS[i]);
            z.setSlowPaceSecKm(VdotCalculator.paceSecKm(VdotCalculator.ZONE_PCTS[i][0], vvo2));
            z.setFastPaceSecKm(VdotCalculator.paceSecKm(VdotCalculator.ZONE_PCTS[i][1], vvo2));
            zones.add(z);
        }
        vo.setZones(zones);
        return vo;
    }

    // ==================== 分析维度 ====================

    private Map<String, Object> analyzePace(List<RunningLap> laps, RunningActivity activity) {
        Map<String, Object> result = new LinkedHashMap<>();
        int[] paces = laps.stream()
                .map(RunningLap::getAvgPaceSecKm)
                .filter(p -> p != null && p > 0)
                .mapToInt(Integer::intValue)
                .toArray();
        if (paces.length == 0) {
            result.put("说明", laps.isEmpty() ? "无分段数据，使用整体配速" : "分段缺少配速数据，使用整体配速");
            result.put("平均配速", activity.getAvgPaceSecKm());
            return result;
        }

        double avg = Arrays.stream(paces).average().orElse(0);
        double stdDev = Math.sqrt(Arrays.stream(paces).mapToDouble(p -> Math.pow(p - avg, 2)).average().orElse(0));

        // 配速区间分布
        Map<String, Integer> paceZones = new LinkedHashMap<>();
        paceZones.put("轻松区(>6:00)", (int) Arrays.stream(paces).filter(p -> p > 360).count());
        paceZones.put("有氧区(5:00-6:00)", (int) Arrays.stream(paces).filter(p -> p > 300 && p <= 360).count());
        paceZones.put("节奏区(4:10-5:00)", (int) Arrays.stream(paces).filter(p -> p > 250 && p <= 300).count());
        paceZones.put("乳酸阈(3:30-4:10)", (int) Arrays.stream(paces).filter(p -> p > 210 && p <= 250).count());
        paceZones.put("无氧区(<3:30)", (int) Arrays.stream(paces).filter(p -> p <= 210).count());

        // 正负配速
        String splitVerdict = "数据不足";
        if (laps.size() >= 4) {
            int mid = laps.size() / 2;
            double firstHalf = avgPaceOf(laps.subList(0, mid));
            double secondHalf = avgPaceOf(laps.subList(mid, laps.size()));
            double diff = secondHalf - firstHalf;
            if (firstHalf > 0 && secondHalf > 0) {
                if (diff < -5) splitVerdict = "负配速（后半程更快），策略优秀";
                else if (diff <= 5) splitVerdict = "配速均匀，分配合理";
                else if (diff <= 15) splitVerdict = "轻微正配速，后半程略有掉速";
                else splitVerdict = "明显正配速，体力分配需改善";
                result.put("后半程−前半程(秒)", Math.round(diff));
            }
        }

        result.put("分段平均配速", Math.round(avg));
        result.put("配速波动(秒)", Math.round(stdDev * 10) / 10.0);
        result.put("配速稳定性", stdDev < 10 ? "优秀" : stdDev < 20 ? "良好" : "需改善");
        result.put("配速区间分布", paceZones);
        result.put("正负配速评价", splitVerdict);
        return result;
    }

    private Map<String, Object> analyzeHeartRate(List<RunningLap> laps, RunningActivity activity) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 区间口径统一基于用户档案的储备心率(HRR)：max_heart_rate / rest_heart_rate，缺失时回退 190/60
        int[] profile = loadHrProfile(activity.getUserId());
        int profileMaxHr = profile[0];
        int profileRhr = profile[1];

        int maxHR = activity.getMaxHeartRate() != null ? activity.getMaxHeartRate() : 190; // 活动实测最大心率
        int avgHR = activity.getAvgHeartRate() != null ? activity.getAvgHeartRate() : 0;

        result.put("平均心率", avgHR);
        result.put("最大心率", maxHR);
        result.put("心率储备使用率(%)", hrrUsage(avgHR, profileMaxHr, profileRhr));

        // 1) 优先：COROS 官方心率区间（hr_zone_json 中 type=126 分组，percent/second 为权威值，不再自行分桶）
        if (applyCorosHrZones(result, activity.getHrZoneJson())) {
            return result;
        }

        // 2) 回退：按档案储备心率(HRR)对分段做计数分桶
        boolean hasHrLap = laps.stream().anyMatch(l -> l.getAvgHeartRate() != null);
        if (!hasHrLap) {
            result.put("说明", "无分段心率数据");
            return result;
        }

        int[] zones = new int[5];
        for (RunningLap lap : laps) {
            if (lap.getAvgHeartRate() == null) continue;
            double pct = hrrPercent(lap.getAvgHeartRate(), profileMaxHr, profileRhr); // 储备心率口径
            if (pct < 60) zones[0]++;
            else if (pct < 74) zones[1]++;
            else if (pct < 84) zones[2]++;
            else if (pct < 95) zones[3]++;
            else zones[4]++;
        }
        int total = (int) Arrays.stream(zones).sum();
        Map<String, Object> zoneMap = new LinkedHashMap<>();
        zoneMap.put("Z1热身", zones[0]);
        zoneMap.put("Z2燃脂", zones[1]);
        zoneMap.put("Z3有氧", zones[2]);
        zoneMap.put("Z4乳酸阈", zones[3]);
        zoneMap.put("Z5无氧", zones[4]);

        int aerobicCount = zones[0] + zones[1] + zones[2];   // 阈下有氧（除 Z4乳酸阈/Z5无氧）
        int aerobicPct = total > 0 ? aerobicCount * 100 / total : 0;

        result.put("心率区间分布", zoneMap);
        result.put("有氧占比", aerobicPct + "%");
        result.put("区间口径", "储备心率(HRR)分段计数，档案最大" + profileMaxHr + "/静息" + profileRhr);
        result.put("建议", aerobicPct >= 70
                ? "有氧占比良好，符合80/20训练原则"
                : "高强度占比偏高，建议增加低强度有氧跑");
        return result;
    }

    /**
     * 用 COROS 官方心率区间填充「心率区间分布/有氧占比/建议」。
     * percent、second 直接来自 hr_zone_json（type=126 分组），成功返回 true。
     */
    private boolean applyCorosHrZones(Map<String, Object> result, String hrZoneJson) {
        List<JSONObject> items = parseCorosHrZoneItems(hrZoneJson);
        if (items == null) {
            return false;
        }

        int[] percent = new int[6];
        long[] seconds = new long[6];
        boolean hasItem = false;
        for (JSONObject item : items) {
            Integer idx = item.getInt("zoneIndex");
            if (idx == null || idx < 0 || idx > 5) continue;
            Integer p = item.getInt("percent");
            Long s = item.getLong("second");
            percent[idx] = p == null ? 0 : p;
            seconds[idx] = s == null ? 0 : s;
            hasItem = true;
        }
        // 秒数与占比都为空视为该活动没有心率区间统计，走回退
        if (!hasItem || (Arrays.stream(seconds).sum() == 0 && Arrays.stream(percent).sum() == 0)) {
            return false;
        }

        Map<String, Object> zoneMap = new LinkedHashMap<>();
        for (int i = 0; i < HR_ZONE_KEYS.length; i++) {
            zoneMap.put(HR_ZONE_KEYS[i], percent[i] + "% (" + seconds[i] + "秒)");
        }
        // 阈下有氧 = 除 Z4乳酸阈/Z5无氧 之外的全部（含恢复/Z1热身，均为低强度）
        int aerobicPct = 100 - percent[4] - percent[5];

        result.put("心率区间分布", zoneMap);
        result.put("有氧占比", aerobicPct + "%");
        result.put("区间口径", "COROS 官方心率区间(账号储备心率分区)");
        result.put("建议", aerobicPct >= 70
                ? "有氧占比良好，符合80/20训练原则"
                : "高强度占比偏高，建议增加低强度有氧跑");
        return true;
    }

    /**
     * 从 hr_zone_json 取出 type=126（心率区间）分组的 item 列表（按 zoneIndex 升序）。
     * 兼容三种存法：单个分组对象、zoneList 分组数组、分组对象数组。取不到返回 null。
     */
    private List<JSONObject> parseCorosHrZoneItems(String hrZoneJson) {
        if (hrZoneJson == null || hrZoneJson.isBlank()) {
            return null;
        }
        try {
            String json = hrZoneJson.trim();
            if (json.startsWith("[")) {
                JSONArray groups = JSONUtil.parseArray(json);
                for (Object g : groups) {
                    JSONObject group = toJsonObject(g);
                    Integer type = group.getInt("type");
                    if (type != null && type == 126) {
                        return sortZoneItems(group.getJSONArray("zoneItemList"));
                    }
                }
                return null;
            }

            JSONObject obj = JSONUtil.parseObj(json);
            if (obj.containsKey("zoneItemList")) {
                return sortZoneItems(obj.getJSONArray("zoneItemList"));   // 存的就是 type=126 分组
            }
            JSONArray groups = obj.getJSONArray("zoneList");
            if (groups != null) {
                for (Object g : groups) {
                    JSONObject group = toJsonObject(g);
                    Integer type = group.getInt("type");
                    if (type != null && type == 126) {
                        return sortZoneItems(group.getJSONArray("zoneItemList"));
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;    // JSON 异常一律按“无心率区间”处理，走回退
        }
    }

    private List<JSONObject> sortZoneItems(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return null;
        }
        List<JSONObject> items = new ArrayList<>();
        for (Object o : arr) {
            items.add(toJsonObject(o));
        }
        items.sort(Comparator.comparingInt((JSONObject j) ->
                j.getInt("zoneIndex") != null ? j.getInt("zoneIndex") : Integer.MAX_VALUE));
        return items;
    }

    private JSONObject toJsonObject(Object o) {
        return o instanceof JSONObject j ? j : JSONUtil.parseObj(o);
    }

    /** 心率储备使用率 = (avgHr - 静息) / (最大 - 静息) × 100，限制在 0~100 */
    private int hrrUsage(int avgHr, int maxHr, int rhr) {
        int v = (int) Math.round(hrrPercent(avgHr, maxHr, rhr));
        return Math.max(0, Math.min(100, v));
    }

    /** 储备心率百分比 = (hr - 静息) / (最大 - 静息) × 100 */
    private double hrrPercent(int hr, int maxHr, int rhr) {
        return (hr - rhr) * 100.0 / (maxHr - rhr);
    }

    /** 读取 t_user 档案的最大心率/静息心率，缺失或非法时回退默认 190/60 */
    private int[] loadHrProfile(Long userId) {
        int maxHr = 190;
        int rhr = 60;
        if (userId != null) {
            try {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    if (user.getMaxHeartRate() != null && user.getMaxHeartRate() > 0) {
                        maxHr = user.getMaxHeartRate();
                    }
                    if (user.getRestHeartRate() != null && user.getRestHeartRate() > 0) {
                        rhr = user.getRestHeartRate();
                    }
                }
            } catch (Exception e) {
                // 档案读取失败按默认值处理
            }
        }
        if (maxHr <= rhr) {
            maxHr = rhr + 90;   // 保证储备心率分母为正
        }
        return new int[]{maxHr, rhr};
    }

    private Map<String, Object> analyzeCadence(List<RunningLap> laps, RunningActivity activity) {
        Map<String, Object> result = new LinkedHashMap<>();
        Integer avgCadence = activity.getAvgCadence();

        if (avgCadence != null) {
            result.put("平均步频", avgCadence);
            if (avgCadence >= 180) result.put("步频评价", "步频优秀 (≥180spm)");
            else if (avgCadence >= 170) result.put("步频评价", "步频良好 (170-180spm)");
            else if (avgCadence >= 160) result.put("步频评价", "步频偏低 (160-170spm)，建议提高步频");
            else result.put("步频评价", "步频明显偏低 (<160spm)，建议练习高步频跑法");
            result.put("目标步频", 180);
        } else {
            result.put("说明", "该记录缺少步频数据");
            return result;
        }

        // 步频稳定性
        if (laps.size() >= 2) {
            int[] cadences = laps.stream().map(l -> l.getAvgCadence() != null ? l.getAvgCadence() : 0)
                    .filter(c -> c > 0).mapToInt(Integer::intValue).toArray();
            if (cadences.length >= 2) {
                double avg = Arrays.stream(cadences).average().orElse(0);
                double stdDev = Math.sqrt(Arrays.stream(cadences).mapToDouble(c -> Math.pow(c - avg, 2)).average().orElse(0));
                result.put("步频稳定性", stdDev < 3 ? "非常稳定" : stdDev < 6 ? "较稳定" : "波动较大");
            }
        }
        return result;
    }

    private Map<String, Object> analyzeFatigue(List<RunningLap> laps) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (laps.size() < 6) {
            result.put("疲劳程度", "数据不足");
            result.put("说明", "需要至少6个分段才能评估疲劳度");
            return result;
        }

        int third = Math.max(laps.size() / 3, 1);
        double firstThird = avgPaceOf(laps.subList(0, third));
        double lastThird = avgPaceOf(laps.subList(laps.size() - third, laps.size()));
        if (firstThird <= 0 || lastThird <= 0) {
            result.put("疲劳程度", "数据不足");
            result.put("说明", "分段缺少配速数据，无法评估疲劳度");
            return result;
        }
        double slowdownPct = (lastThird - firstThird) / firstThird * 100;

        result.put("后程掉速(%)", Math.round(slowdownPct * 10) / 10.0);
        result.put("前1/3段平均配速", Math.round(firstThird));
        result.put("后1/3段平均配速", Math.round(lastThird));

        if (slowdownPct <= 2) {
            result.put("疲劳程度", "极佳");
            result.put("建议", "后程掉速极少，体能储备和配速策略都很好");
        } else if (slowdownPct <= 5) {
            result.put("疲劳程度", "良好");
            result.put("建议", "轻度疲劳，整体表现不错");
        } else if (slowdownPct <= 10) {
            result.put("疲劳程度", "中度疲劳");
            result.put("建议", "建议加强有氧基础训练，提高耐力储备");
        } else {
            result.put("疲劳程度", "高度疲劳");
            result.put("建议", "后程明显掉速，建议降低训练强度，增加恢复跑");
        }
        return result;
    }

    private Map<String, Object> analyzeTrainingEffect(RunningActivity activity, List<RunningLap> laps) {
        Map<String, Object> result = new LinkedHashMap<>();

        double distanceKm = activity.getDistanceM() / 1000.0;
        int durationMin = activity.getDurationSeconds() / 60;
        int avgHR = activity.getAvgHeartRate() != null ? activity.getAvgHeartRate() : 0;

        // 有氧训练效果 (基于距离、时长、心率)
        String aerobicTE;
        if (distanceKm < 3 || durationMin < 20) {
            aerobicTE = "低 (活动量不足)";
        } else if (avgHR > 0 && avgHR < 130) {
            aerobicTE = "低强度恢复跑，维持有氧基础";
        } else if (distanceKm >= 8 || durationMin >= 45) {
            aerobicTE = "中高 (有效提升有氧耐力)";
        } else {
            aerobicTE = "中等 (维持有氧能力)";
        }
        result.put("有氧训练效果", aerobicTE);

        // 无氧训练效果
        String anaerobicTE;
        boolean hasPaceLap = laps.stream().anyMatch(l -> l.getAvgPaceSecKm() != null && l.getAvgPaceSecKm() > 0);
        if (!hasPaceLap) {
            anaerobicTE = "无法评估";
        } else {
            long highIntensityLaps = laps.stream()
                    .filter(l -> l.getAvgPaceSecKm() != null && l.getAvgPaceSecKm() > 0 && l.getAvgPaceSecKm() <= 300)
                    .count();
            if (highIntensityLaps >= 3) {
                anaerobicTE = "中高 (有效刺激无氧能力)";
            } else if (highIntensityLaps >= 1) {
                anaerobicTE = "低中 (少量高强度刺激)";
            } else {
                anaerobicTE = "低 (纯有氧训练)";
            }
        }
        result.put("无氧训练效果", anaerobicTE);

        // 训练强度建议
        result.put("强度建议", "建议本周至少包含1次节奏跑或间歇跑以提升速度能力");
        return result;
    }

    private int calculateScore(AnalysisResultVO vo) {
        int score = 60; // 基础分

        // 配速稳定性加分
        Map<String, Object> pace = vo.getPaceAnalysis();
        if ("优秀".equals(pace.get("配速稳定性"))) score += 15;
        else if ("良好".equals(pace.get("配速稳定性"))) score += 8;

        // 心率分析加分
        Map<String, Object> hr = vo.getHeartRateAnalysis();
        Object aerobicPct = hr.get("有氧占比");
        String pct = aerobicPct == null ? null : aerobicPct.toString();
        if (pct != null && pct.startsWith("7")) score += 10;
        else if (pct != null && pct.startsWith("6")) score += 5;

        // 疲劳度加分
        Map<String, Object> fatigue = vo.getFatigueAnalysis();
        if ("极佳".equals(fatigue.get("疲劳程度"))) score += 15;
        else if ("良好".equals(fatigue.get("疲劳程度"))) score += 8;

        return Math.min(score, 100);
    }

    /** 规则版文字摘要（AI 不可用时的兜底） */
    private String generateSummary(AnalysisResultVO vo, RunningActivity activity) {
        double distKm = activity.getDistanceM() / 1000;
        int durMin = activity.getDurationSeconds() / 60;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("本次跑步 %.2f 公里，用时 %d 分钟。", distKm, durMin));

        Map<String, Object> pace = vo.getPaceAnalysis();
        sb.append(String.format("配速稳定性%s", pace.get("配速稳定性")));
        Object verdict = pace.get("正负配速评价");
        if (verdict != null && !"数据不足".equals(verdict)) {
            sb.append(String.format("，%s", verdict));
        }
        sb.append("。");

        Map<String, Object> fatigue = vo.getFatigueAnalysis();
        if (!"数据不足".equals(fatigue.get("疲劳程度"))) {
            sb.append(String.format("疲劳度评估：%s。%s", fatigue.get("疲劳程度"), fatigue.get("建议")));
        }

        Map<String, Object> hr = vo.getHeartRateAnalysis();
        if (hr.get("建议") != null) {
            sb.append(" ").append(hr.get("建议"));
        }

        return sb.toString();
    }

    /** AI 摘要：配置了大模型 key 则由模型生成，否则返回规则文案 */
    private String buildAiSummary(AnalysisResultVO vo, RunningActivity activity) {
        String ruleText = generateSummary(vo, activity);
        try {
            String prompt = String.format(
                    "请为以下跑步训练写一段中文教练点评（3~5 句，纯文本，不要标题和列表）：%n"
                            + "活动：%s%n距离：%.2f 公里%n用时：%d 分钟%n平均配速：%s%n平均心率：%s，最大心率：%s%n"
                            + "综合评分：%d 分%n配速分析：%s%n心率分析：%s%n疲劳评估：%s",
                    activity.getActivityName(),
                    activity.getDistanceM() / 1000.0,
                    activity.getDurationSeconds() / 60,
                    paceStr(activity.getAvgPaceSecKm()),
                    activity.getAvgHeartRate() == null ? "无" : activity.getAvgHeartRate(),
                    activity.getMaxHeartRate() == null ? "无" : activity.getMaxHeartRate(),
                    vo.getScore(),
                    cn.hutool.json.JSONUtil.toJsonStr(vo.getPaceAnalysis()),
                    cn.hutool.json.JSONUtil.toJsonStr(vo.getHeartRateAnalysis()),
                    cn.hutool.json.JSONUtil.toJsonStr(vo.getFatigueAnalysis()));
            String ai = aiClient.chat(
                    "你是经验丰富的跑步教练，用简体中文给出专业、具体、鼓励式的训练点评。", prompt);
            return ai != null ? ai : ruleText;
        } catch (Exception e) {
            return ruleText;
        }
    }

    private String paceStr(Integer secKm) {
        if (secKm == null || secKm <= 0) return "--";
        return String.format("%d'%02d\"/km", secKm / 60, secKm % 60);
    }

    /** 分段列表平均配速（过滤缺失值） */
    private double avgPaceOf(List<RunningLap> laps) {
        return laps.stream()
                .map(RunningLap::getAvgPaceSecKm)
                .filter(p -> p != null && p > 0)
                .mapToInt(Integer::intValue)
                .average().orElse(0);
    }

    private String serializeAnalysis(AnalysisResultVO vo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("paceAnalysis", vo.getPaceAnalysis());
        map.put("heartRateAnalysis", vo.getHeartRateAnalysis());
        map.put("cadenceAnalysis", vo.getCadenceAnalysis());
        map.put("fatigueAnalysis", vo.getFatigueAnalysis());
        map.put("trainingEffect", vo.getTrainingEffect());
        map.put("score", vo.getScore());
        return cn.hutool.json.JSONUtil.toJsonStr(map);
    }

    private AnalysisResultVO deserializeAnalysis(String json) {
        AnalysisResultVO vo = new AnalysisResultVO();
        try {
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(json);
            vo.setPaceAnalysis(obj.getJSONObject("paceAnalysis"));
            vo.setHeartRateAnalysis(obj.getJSONObject("heartRateAnalysis"));
            vo.setCadenceAnalysis(obj.getJSONObject("cadenceAnalysis"));
            vo.setFatigueAnalysis(obj.getJSONObject("fatigueAnalysis"));
            vo.setTrainingEffect(obj.getJSONObject("trainingEffect"));
        } catch (Exception e) {
            // 解析失败则返回空
        }
        return vo;
    }

    /* ---------------- 肌肉热力图（参考高驰 App 肌群负荷口径） ---------------- */

    /** 肌群元数据：key -> {中文名, 人体视图 FRONT/BACK} */
    private static final String[][] MUSCLE_META = {
            {"chest", "胸大肌", "FRONT"},
            {"shoulders", "三角肌", "FRONT"},
            {"biceps", "肱二头肌", "FRONT"},
            {"core", "腹部核心", "FRONT"},
            {"quads", "股四头肌", "FRONT"},
            {"tibialis", "胫骨前肌", "FRONT"},
            {"lats", "背阔肌", "BACK"},
            {"glutes", "臀大肌", "BACK"},
            {"hamstrings", "腘绳肌", "BACK"},
            {"calves", "小腿三头肌", "BACK"}
    };

    /** 满负荷参考值：约等于 28 天规律训练下主肌群的负荷当量（load >= REF 记 100 分，超出封顶） */
    private static final double MUSCLE_LOAD_REF = 300.0;

    @Override
    public MuscleMapVO getMuscleMap(Long userId, Integer days) {
        int window = days == null ? 28 : Math.max(1, Math.min(days, 365));
        LocalDateTime start = LocalDateTime.now().minusDays(window);
        List<RunningActivity> activities = runningStats.listByRange(userId, start, LocalDateTime.now());

        Map<String, Double> load = new LinkedHashMap<>();
        for (String[] meta : MUSCLE_META) {
            load.put(meta[0], 0.0);
        }

        int totalMinutes = 0;
        for (RunningActivity a : activities) {
            Integer durSec = a.getDurationSeconds();
            if (durSec == null || durSec <= 0) {
                continue;
            }
            double minutes = durSec / 60.0;
            totalMinutes += (int) Math.round(minutes);
            double intensity = muscleIntensity(a);
            for (Map.Entry<String, Double> e : muscleWeights(a.getActivityType()).entrySet()) {
                load.merge(e.getKey(), minutes * intensity * e.getValue(), Double::sum);
            }
        }

        List<MuscleMapVO.MuscleItem> muscles = new ArrayList<>();
        for (String[] meta : MUSCLE_META) {
            double v = load.getOrDefault(meta[0], 0.0);
            int score = (int) Math.min(100, Math.round(v / MUSCLE_LOAD_REF * 100));
            MuscleMapVO.MuscleItem item = new MuscleMapVO.MuscleItem();
            item.setKey(meta[0]);
            item.setName(meta[1]);
            item.setView(meta[2]);
            item.setScore(score);
            if (score >= 70) {
                item.setLevel("HIGH");
                item.setLevelText("高负荷");
            } else if (score >= 40) {
                item.setLevel("MEDIUM");
                item.setLevelText("中等");
            } else if (score >= 12) {
                item.setLevel("LOW");
                item.setLevelText("轻度");
            } else {
                item.setLevel("IDLE");
                item.setLevelText("未激活");
            }
            muscles.add(item);
        }
        muscles.sort((x, y) -> y.getScore() - x.getScore());

        MuscleMapVO vo = new MuscleMapVO();
        vo.setDays(window);
        vo.setActivityCount(activities.size());
        vo.setTotalMinutes(totalMinutes);
        vo.setMuscles(muscles);
        return vo;
    }

    /** 单次活动强度 0.50~1.00：优先心率口径（均值相对峰值），无心率按中等强度 0.62 */
    private double muscleIntensity(RunningActivity a) {
        Integer avg = a.getAvgHeartRate();
        if (avg == null || avg <= 0) {
            return 0.62;
        }
        Integer max = a.getMaxHeartRate();
        double hrMax = (max != null && max > avg + 5) ? max : 190;
        double pct = (avg - 70) / Math.max(30.0, hrMax - 70);
        return Math.max(0.50, Math.min(1.0, 0.5 + 0.5 * pct));
    }

    /** 运动类型 -> 肌群负荷权重：覆盖跑/骑/力量/游泳；未知类型按全身轻度参与 */
    private Map<String, Double> muscleWeights(String activityType) {
        String t = activityType == null ? "" : activityType.toUpperCase();
        if (t.contains("RUN") || t.contains("JOG")) {
            return Map.ofEntries(
                    Map.entry("quads", 0.24), Map.entry("glutes", 0.19),
                    Map.entry("calves", 0.16), Map.entry("hamstrings", 0.13),
                    Map.entry("core", 0.10), Map.entry("tibialis", 0.07),
                    Map.entry("lats", 0.03), Map.entry("shoulders", 0.03),
                    Map.entry("biceps", 0.03), Map.entry("chest", 0.02));
        }
        if (t.contains("RIDE") || t.contains("CYCL")) {
            return Map.ofEntries(
                    Map.entry("quads", 0.32), Map.entry("glutes", 0.26),
                    Map.entry("calves", 0.16), Map.entry("hamstrings", 0.12),
                    Map.entry("core", 0.08), Map.entry("tibialis", 0.02),
                    Map.entry("lats", 0.01), Map.entry("shoulders", 0.01),
                    Map.entry("biceps", 0.01), Map.entry("chest", 0.01));
        }
        if (t.contains("STRENGTH") || t.contains("TRAIN") || t.contains("HIIT")
                || t.contains("GYM") || t.contains("WORKOUT")) {
            return Map.ofEntries(
                    Map.entry("chest", 0.16), Map.entry("shoulders", 0.15),
                    Map.entry("lats", 0.15), Map.entry("biceps", 0.10),
                    Map.entry("core", 0.12), Map.entry("quads", 0.12),
                    Map.entry("hamstrings", 0.08), Map.entry("glutes", 0.06),
                    Map.entry("calves", 0.04), Map.entry("tibialis", 0.02));
        }
        if (t.contains("SWIM")) {
            return Map.ofEntries(
                    Map.entry("lats", 0.26), Map.entry("shoulders", 0.20),
                    Map.entry("core", 0.16), Map.entry("chest", 0.12),
                    Map.entry("biceps", 0.10), Map.entry("hamstrings", 0.06),
                    Map.entry("quads", 0.05), Map.entry("glutes", 0.03),
                    Map.entry("calves", 0.01), Map.entry("tibialis", 0.01));
        }
        return Map.ofEntries(
                Map.entry("quads", 0.14), Map.entry("glutes", 0.12),
                Map.entry("hamstrings", 0.11), Map.entry("core", 0.13),
                Map.entry("calves", 0.10), Map.entry("chest", 0.09),
                Map.entry("shoulders", 0.09), Map.entry("lats", 0.09),
                Map.entry("biceps", 0.08), Map.entry("tibialis", 0.05));
    }
}
