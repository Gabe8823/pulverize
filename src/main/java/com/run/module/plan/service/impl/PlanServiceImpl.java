package com.run.module.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.run.common.ai.AiClient;
import com.run.common.exception.BizException;
import com.run.module.plan.dto.GeneratePlanRequest;
import com.run.module.plan.dto.PlanDetailVO;
import com.run.module.plan.dto.PushWatchResultVO;
import com.run.module.plan.entity.TrainingPlan;
import com.run.module.plan.entity.TrainingPlanDetail;
import com.run.module.plan.mapper.TrainingPlanDetailMapper;
import com.run.module.plan.mapper.TrainingPlanMapper;
import com.run.module.plan.service.PlanService;
import com.run.module.platform.client.CorosApiClient;
import com.run.module.platform.entity.PlatformAuth;
import com.run.module.platform.mapper.PlatformAuthMapper;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.mapper.RunningActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 训练计划生成与管理
 *
 * - 规则引擎按目标 + 近30天数据生成周期化计划
 * - 支持选择职业运动员训练风格（athleteStyle）：以个人能力为基准，套用其训练结构
 * - aiReasoning 优先由大模型生成（AiClient），未配置 key 时降级为规则文案
 * - pushToWatch: 把计划逐天推送到高驰手表训练日历（串行，不同日并发）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private static final String PLATFORM_COROS = "COROS";

    private final TrainingPlanMapper planMapper;
    private final TrainingPlanDetailMapper detailMapper;
    private final RunningActivityMapper activityMapper;
    private final AiClient aiClient;
    private final PlatformAuthMapper platformAuthMapper;
    private final CorosApiClient corosApiClient;

    @Override
    @Transactional
    public GeneratePlanRequest.GeneratePlanResponse generatePlan(Long userId, GeneratePlanRequest request) {
        // 收集用户近30天跑步数据
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<RunningActivity> recentActivities = activityMapper.selectList(
                new LambdaQueryWrapper<RunningActivity>()
                        .eq(RunningActivity::getUserId, userId)
                        .ge(RunningActivity::getStartTime, thirtyDaysAgo)
                        .orderByDesc(RunningActivity::getStartTime));

        double avgDistanceKm = recentActivities.stream()
                .map(RunningActivity::getDistanceM)
                .filter(Objects::nonNull)
                .mapToDouble(d -> d / 1000.0).average().orElse(5.0);
        int avgPace = (int) recentActivities.stream()
                .map(RunningActivity::getAvgPaceSecKm)
                .filter(p -> p != null && p > 0)
                .mapToInt(Integer::intValue)
                .average().orElse(360);
        double longestRunKm = recentActivities.stream()
                .map(RunningActivity::getDistanceM)
                .filter(Objects::nonNull)
                .mapToDouble(d -> d / 1000.0).max().orElse(5.0);

        // 基于规则生成训练计划（含职业运动员风格）
        List<GeneratePlanRequest.DailyPlanItem> dailyPlan = generateDailyPlan(
                request, avgDistanceKm, avgPace, longestRunKm);

        // 保存到数据库
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setPlanName(buildPlanName(request.getGoalType(), request.getStartDate(), request.getAthleteStyle()));
        plan.setGoalType(request.getGoalType());
        plan.setGoalValue(request.getGoalValue());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setStatus(0);
        String ruleReasoning = buildReasoning(request, recentActivities.size(), avgDistanceKm, avgPace);
        plan.setAiReasoning(buildAiReasoning(request, recentActivities.size(),
                avgDistanceKm, avgPace, longestRunKm, ruleReasoning));
        plan.setWeeklyPlan("{}");
        plan.setDeleted(0);
        planMapper.insert(plan);

        List<TrainingPlanDetail> details = dailyPlan.stream().map(d -> {
            TrainingPlanDetail detail = new TrainingPlanDetail();
            detail.setPlanId(plan.getId());
            detail.setPlanDate(d.getPlanDate());
            detail.setDayOfWeek(d.getDayOfWeek());
            detail.setWorkoutType(d.getWorkoutType());
            detail.setTargetDistanceKm(d.getTargetDistanceKm());
            detail.setTargetDurationMin(d.getTargetDurationMin());
            detail.setTargetPaceSecKm(d.getTargetPaceSecKm());
            detail.setTargetHrZone(d.getTargetHrZone());
            detail.setWorkoutDescription(d.getWorkoutDescription());
            detail.setStatus(0);
            return detail;
        }).collect(Collectors.toList());
        details.forEach(detailMapper::insert);

        GeneratePlanRequest.GeneratePlanResponse resp = new GeneratePlanRequest.GeneratePlanResponse();
        resp.setPlanId(plan.getId());
        resp.setPlanName(plan.getPlanName());
        resp.setAiReasoning(plan.getAiReasoning());
        resp.setDailyPlan(dailyPlan);
        return resp;
    }

    @Override
    public List<TrainingPlan> listPlans(Long userId) {
        return planMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>()
                        .eq(TrainingPlan::getUserId, userId)
                        .orderByDesc(TrainingPlan::getCreateTime));
    }

    @Override
    public PlanDetailVO getPlanDetail(Long userId, Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BizException("训练计划不存在");
        }

        List<TrainingPlanDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanDetail>()
                        .eq(TrainingPlanDetail::getPlanId, planId)
                        .orderByAsc(TrainingPlanDetail::getPlanDate));

        PlanDetailVO vo = new PlanDetailVO();
        vo.setPlan(plan);
        vo.setDailyDetails(details);
        return vo;
    }

    @Override
    public void updateDayStatus(Long userId, Long detailId, Integer status) {
        TrainingPlanDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BizException("训练日详情不存在");
        }
        TrainingPlan plan = planMapper.selectById(detail.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BizException("训练计划不存在");
        }
        detail.setStatus(status);
        detailMapper.updateById(detail);
    }

    @Override
    @Transactional
    public void deletePlan(Long userId, Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BizException("训练计划不存在");
        }
        planMapper.deleteById(planId);
        detailMapper.delete(new LambdaQueryWrapper<TrainingPlanDetail>()
                .eq(TrainingPlanDetail::getPlanId, planId));
    }

    // ==================== 推送高驰手表 ====================

    @Override
    public PushWatchResultVO pushToWatch(Long userId, Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BizException("训练计划不存在");
        }

        // 取平台鉴权（accessToken + 高驰 userId，yfheader 用）
        PlatformAuth auth = platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM_COROS));
        if (auth == null || auth.getAccessToken() == null || auth.getAccessToken().isBlank()
                || auth.getPlatformUserId() == null || auth.getPlatformUserId().isBlank()) {
            throw new BizException("请先连接高驰账号");
        }

        List<TrainingPlanDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanDetail>()
                        .eq(TrainingPlanDetail::getPlanId, planId)
                        .orderByAsc(TrainingPlanDetail::getPlanDate));

        PushWatchResultVO result = new PushWatchResultVO();
        // 逐天串行推送：idInPlan 按"当天日历 maxIdInPlan+1"解析，并发会互相覆盖
        for (TrainingPlanDetail detail : details) {
            if (detail.getPlanDate() == null || "REST".equals(detail.getWorkoutType())) {
                continue; // 休息日不上表
            }
            String day = detail.getPlanDate().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
            try {
                pushDay(auth, detail, day);
                result.setPushed(result.getPushed() + 1);
            } catch (Exception e) {
                log.warn("推送训练日到高驰失败: planId={}, day={}", planId, day, e);
                result.setFailed(result.getFailed() + 1);
                result.getErrors().add(day + ": " + e.getMessage());
            }
        }
        return result;
    }

    /** 单天推送：GET 当天日历取 maxIdInPlan -> idInPlan+1 -> POST schedule/update（内联课表） */
    private void pushDay(PlatformAuth auth, TrainingPlanDetail detail, String day) {
        String token = auth.getAccessToken();
        String corosUserId = auth.getPlatformUserId();

        // 1. 解析当天日历，取下一个 idInPlan
        Map<String, Object> schedule = corosApiClient.getSchedule(token, corosUserId, day, day);
        long idInPlan = 1;
        Object maxId = schedule.get("maxIdInPlan");
        try {
            if (maxId != null) {
                idInPlan = Long.parseLong(maxId.toString().trim()) + 1;
            }
        } catch (NumberFormatException e) {
            idInPlan = 1;
        }

        // 2. 构造当天课表（跑步），并附加 idInPlan
        Map<String, Object> program = buildRunningProgram(detail);
        program.put("idInPlan", idInPlan);

        // 3. 排期（结构照抄 coros-mcp _post_schedule_inline）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entities", List.of(Map.of(
                "happenDay", day,
                "idInPlan", idInPlan,
                "sortNoInSchedule", 1)));
        body.put("programs", List.of(program));
        body.put("versionObjects", List.of(Map.of("id", idInPlan, "status", 1)));
        body.put("pbVersion", 2);

        corosApiClient.updateSchedule(token, corosUserId, body);
    }

    /**
     * 按每日明细构造跑步课表 program（coros-mcp _build_workout_program_payload 的 Java 简化版）
     *
     * 关键约定（来自缓存文件 L841-1195 实测结论）:
     * - sportType 走 activity 侧 100(跑步) -> wire 侧恒为 1，且必须附带跑步 metadata 块，
     *   否则 App 解析失败 / 手表渲染成力量训练；直接传 1 会被服务端拒绝。
     * - 时长步: targetType=2, targetValue=秒, intensityMultiplier=0, intensity 原值。
     * - 距离步: targetType=5, targetValue=米x100, intensityMultiplier=1000, intensity 值 x1000。
     * - intensityType=3 配速目标，强度单位 = 秒/公里(如 300 = 5'00"/km)，时长步原值、
     *   距离步 x1000；无配速时退回 intensityType=2 心率且 0/0 开放目标。
     * - exerciseType: 首个顶层步=1(热身)，末个=3(放松)，中间=2(主课)。
     */
    private Map<String, Object> buildRunningProgram(TrainingPlanDetail detail) {
        Integer pace = detail.getTargetPaceSecKm();
        boolean paceTarget = pace != null && pace > 0;
        int intensityType = paceTarget ? 3 : 2;          // 3=配速(秒/公里) 2=心率
        // 主课目标强度: targetPaceSecKm 上下浮动约 ±15 秒（低值=更快）
        int mainLow = paceTarget ? Math.max(60, pace - 15) : 0;
        int mainHigh = paceTarget ? pace + 15 : 0;

        // 主课时长/距离: 距离优先 -> duration_meters；否则时长 -> duration_minutes（缺省 30 分钟）
        Double distanceKm = detail.getTargetDistanceKm();
        boolean distanceBased = distanceKm != null && distanceKm > 0;
        int mainMinutes = detail.getTargetDurationMin() != null && detail.getTargetDurationMin() > 0
                ? detail.getTargetDurationMin() : 30;

        List<Map<String, Object>> exercises = new ArrayList<>();
        // 热身 10 分钟（开放目标）
        exercises.add(buildExercise(1, "热身", 1, 1, intensityType, 0, 0, null, 600, 0));
        // 主课
        if (distanceBased) {
            int meters = (int) Math.round(distanceKm * 1000);
            exercises.add(buildExercise(2, "主课", 2, 2, intensityType,
                    mainLow * 1000, mainHigh * 1000, meters, 0, 1000));
        } else {
            exercises.add(buildExercise(2, "主课", 2, 2, intensityType,
                    mainLow, mainHigh, null, mainMinutes * 60, 0));
        }
        // 放松 10 分钟（开放目标）
        exercises.add(buildExercise(3, "放松", 3, 3, intensityType, 0, 0, null, 600, 0));

        // estimatedTime/duration 与参考实现一致: 距离步秒数按 0 计
        int totalSeconds = 600 + (distanceBased ? 0 : mainMinutes * 60) + 600;

        Map<String, Object> program = new LinkedHashMap<>();
        String name = detail.getWorkoutDescription() != null && !detail.getWorkoutDescription().isBlank()
                ? detail.getWorkoutDescription() : "跑步训练";
        program.put("name", name.length() > 30 ? name.substring(0, 30) : name);
        program.put("sportType", 1); // wire ID（跑步）
        program.put("estimatedTime", totalSeconds);
        program.put("access", 1);
        program.put("exercises", exercises);

        // 跑步课表必备扩展块（缺失则 App 解析失败/手表渲染异常）
        program.put("duration", totalSeconds);
        program.put("exerciseNum", exercises.size());
        program.put("gradeSystemVersion", 0);
        program.put("hybridTotalSets", 0);
        program.put("overview", "");
        program.put("poolLength", 0);
        program.put("poolLengthId", 0);
        program.put("poolLengthUnit", 0);
        program.put("referExercise", Map.of(
                "gradeSystem", 0,
                "hrType", intensityType == 2 ? 3 : 0,
                "intensityType", 0,
                "valueType", 1));
        program.put("sourceUrl", "");
        program.put("subType", 65535); // 65535 = 结构化课表
        program.put("totalSets", exercises.size());
        program.put("trainingLoad", 0);
        program.put("type", 0);
        program.put("videoCoverUrl", "");
        program.put("videoUrl", "");
        return program;
    }

    /** 构造单个 exercise（平铺步，无 repeat 组）。durationMeters 非空走距离步，否则按秒时长步 */
    private Map<String, Object> buildExercise(int id, String name, int exerciseType, int sortTop,
                                              int intensityType, int low, int high,
                                              Integer durationMeters, int seconds, int intensityMultiplier) {
        Map<String, Object> ex = new LinkedHashMap<>();
        ex.put("id", id);
        ex.put("name", name);
        ex.put("exerciseType", exerciseType);   // 1=热身 2=主课 3=放松
        ex.put("sportType", 1);                 // 跑步 wire ID
        ex.put("intensityType", intensityType);
        ex.put("intensityValue", low);
        ex.put("intensityValueExtend", high);
        ex.put("intensityMultiplier", intensityMultiplier);
        if (durationMeters != null) {
            ex.put("targetType", 5);                    // 距离
            ex.put("targetValue", durationMeters * 100); // 米 x100
        } else {
            ex.put("targetType", 2);                    // 时长
            ex.put("targetValue", seconds);             // 秒
        }
        ex.put("sets", 1);
        ex.put("sortNo", 16777216 * sortTop);
        ex.put("restType", 3);
        ex.put("restValue", 0);
        ex.put("groupId", "0");
        ex.put("isGroup", false);
        ex.put("originId", "0");
        // 跑步 metadata 块（对每个非 group 步必填）
        ex.put("exerciseKind", 0);
        ex.put("gradeSystem", 0);
        ex.put("hrType", intensityType == 2 ? 2 : 0);
        ex.put("intensityPercent", 0);
        ex.put("intensityPercentExtend", 0);
        ex.put("onsightGradeOffset", 0);
        ex.put("overview", "");
        ex.put("packageTime", 0);
        ex.put("sourceId", "0");
        ex.put("subType", 0);
        ex.put("targetDisplayUnit", 0);
        return ex;
    }

    // ==================== 计划生成核心逻辑 ====================

    private List<GeneratePlanRequest.DailyPlanItem> generateDailyPlan(
            GeneratePlanRequest request, double avgDistanceKm, int avgPace, double longestRunKm) {

        List<GeneratePlanRequest.DailyPlanItem> plan = new ArrayList<>();
        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int weeklyDays = request.getWeeklyTrainingDays() != null ? request.getWeeklyTrainingDays() : 4;

        // 职业运动员风格参数（配速增量以个人当前能力为基准，不照搬职业配速）
        AthleteStyle style = AthleteStyle.of(request.getAthleteStyle());

        // 基础配速：目标配速根据当前水平逐步提升
        int easyPace = avgPace + style.easyDelta();        // 轻松跑比平均慢
        int tempoPace = avgPace - style.tempoDelta();      // 节奏跑比平均快
        int intervalPace = avgPace - style.intervalDelta();// 间歇跑比平均快

        // 目标距离基础值
        double easyDistance = Math.max(avgDistanceKm * 0.8, 3.0);
        double longDistance = Math.max(longestRunKm * style.longRunFactor(), 5.0);
        double tempoDistance = Math.max(avgDistanceKm * 0.7, 3.0);

        for (int day = 0; day < totalDays; day++) {
            LocalDate date = request.getStartDate().plusDays(day);
            DayOfWeek dow = date.getDayOfWeek();
            int weekIndex = day / 7;
            double weekMultiplier = 1.0 + weekIndex * 0.08; // 每周递增8%

            GeneratePlanRequest.DailyPlanItem item = new GeneratePlanRequest.DailyPlanItem();
            item.setPlanDate(date);
            item.setDayOfWeek(dow.getValue());

            // 训练安排策略
            int trainingDayInWeek = getTrainingDayInWeek(dow, weeklyDays);
            boolean isTrainingDay = trainingDayInWeek > 0;

            if (!isTrainingDay) {
                item.setWorkoutType("REST");
                item.setWorkoutDescription("完全休息日，促进恢复");
            } else if (dow == DayOfWeek.SATURDAY && weeklyDays >= 3) {
                // 周六：长距离跑（上限按运动员风格）
                double dist = Math.min(longDistance * weekMultiplier, style.longRunCap());
                item.setWorkoutType("LONG_RUN");
                item.setTargetDistanceKm(Math.round(dist * 10) / 10.0);
                item.setTargetDurationMin((int) (dist * (easyPace / 60.0)) + 5);
                item.setTargetPaceSecKm(easyPace);
                item.setTargetHrZone("Z2");
                item.setWorkoutDescription(String.format(
                        "长距离有氧跑 %.1f 公里，轻松配速完成，心率保持在Z2区间", dist));
            } else if (dow == DayOfWeek.WEDNESDAY && weeklyDays >= 4) {
                // 周三：质量课（间歇/节奏），由目标或运动员风格决定
                String goalType = request.getGoalType();
                boolean intervalDay = "AUTO".equals(style.qualityHint())
                        ? ("5K_PB".equals(goalType) || "10K_PB".equals(goalType))
                        : "INTERVAL".equals(style.qualityHint());
                if (intervalDay) {
                    double dist = Math.min(tempoDistance * weekMultiplier, 10.0);
                    item.setWorkoutType("INTERVAL");
                    item.setTargetDistanceKm(Math.round(dist * 10) / 10.0);
                    item.setTargetDurationMin((int) (dist * (intervalPace / 60.0)) + 5);
                    item.setTargetPaceSecKm(intervalPace);
                    item.setTargetHrZone("Z4");
                    item.setWorkoutDescription(String.format(
                            "间歇训练 %.1f 公里，包含%d组快速跑与慢跑间歇", dist, 6));
                } else {
                    double dist = Math.min(tempoDistance * style.tempoFactor() * weekMultiplier, 12.0);
                    item.setWorkoutType("TEMPO");
                    item.setTargetDistanceKm(Math.round(dist * 10) / 10.0);
                    item.setTargetDurationMin((int) (dist * (tempoPace / 60.0)) + 5);
                    item.setTargetPaceSecKm(tempoPace);
                    item.setTargetHrZone("Z3-Z4");
                    item.setWorkoutDescription(String.format(
                            "节奏跑 %.1f 公里，中高配速稳定输出", dist));
                }
            } else {
                // 轻松跑
                double dist = Math.min(easyDistance * weekMultiplier, 12.0);
                item.setWorkoutType("EASY_RUN");
                item.setTargetDistanceKm(Math.round(dist * 10) / 10.0);
                item.setTargetDurationMin((int) (dist * (easyPace / 60.0)) + 5);
                item.setTargetPaceSecKm(easyPace);
                item.setTargetHrZone("Z2");
                item.setWorkoutDescription(String.format(
                        "轻松有氧跑 %.1f 公里，关注跑姿放松，保持Z2心率", dist));
            }

            plan.add(item);
        }
        return plan;
    }

    /** 根据星期和每周训练天数，返回是第几个训练日(1-based)，非训练日返回0 */
    private int getTrainingDayInWeek(DayOfWeek dow, int weeklyDays) {
        if (weeklyDays >= 5) {
            return (dow != DayOfWeek.SUNDAY) ? 1 : 0;
        } else if (weeklyDays == 4) {
            return switch (dow) {
                case MONDAY, WEDNESDAY, FRIDAY, SATURDAY -> 1;
                default -> 0;
            };
        } else if (weeklyDays == 3) {
            return switch (dow) {
                case MONDAY, WEDNESDAY, SATURDAY -> 1;
                default -> 0;
            };
        } else {
            return switch (dow) {
                case MONDAY, SATURDAY -> 1;
                default -> 0;
            };
        }
    }

    // ==================== 职业运动员风格 ====================

    /**
     * 职业运动员训练风格参数。
     * 所有配速增量都叠加在用户自身能力（avgPace）之上，结构（长距离比例、质量课类型）按运动员特点调整。
     */
    private record AthleteStyle(String displayName, String desc,
                                int easyDelta, int tempoDelta, int intervalDelta,
                                double longRunFactor, double longRunCap,
                                double tempoFactor, String qualityHint) {

        static final AthleteStyle DEFAULT = new AthleteStyle(null,
                "通用 80/20 周期化训练风格", 30, 20, 40, 1.1, 25.0, 0.7, "AUTO");

        static AthleteStyle of(String code) {
            if (code == null || code.isBlank()) return DEFAULT;
            return switch (code) {
                case "KIPCHOGE" -> new AthleteStyle("埃鲁德·基普乔格",
                        "以马拉松节奏跑和超长距离有氧为核心的均衡耐力风格",
                        40, 25, 45, 1.6, 35.0, 0.8, "TEMPO");
                case "OSAKO" -> new AthleteStyle("大迫杰",
                        "节奏跑特化、重点提升乳酸阈的速度耐力风格",
                        30, 35, 50, 1.2, 28.0, 1.0, "TEMPO");
                case "HASAN" -> new AthleteStyle("希凡·哈桑",
                        "间歇与速度课占比高、以速度见长的风格",
                        25, 20, 55, 1.0, 22.0, 0.6, "INTERVAL");
                default -> DEFAULT;
            };
        }
    }

    // ==================== 计划命名与推理说明 ====================

    private String goalLabel(String goalType) {
        return switch (goalType == null ? "" : goalType) {
            case "5K_PB" -> "5K提速计划";
            case "10K_PB" -> "10K突破计划";
            case "HALF_MARATHON" -> "半程马拉松训练计划";
            case "MARATHON" -> "全程马拉松训练计划";
            case "LOSE_WEIGHT" -> "跑步减脂计划";
            default -> "综合跑步训练计划";
        };
    }

    private String buildPlanName(String goalType, LocalDate startDate, String athleteStyle) {
        AthleteStyle style = AthleteStyle.of(athleteStyle);
        String styleSuffix = style.displayName() != null ? " · " + style.displayName() + "风格" : "";
        return String.format("%s%s (%s开始)", goalLabel(goalType), styleSuffix,
                startDate.format(DateTimeFormatter.ofPattern("M月d日")));
    }

    /** 规则版计划推理（AI 不可用时的兜底） */
    private String buildReasoning(GeneratePlanRequest request, int activityCount,
                                  double avgDistanceKm, int avgPace) {
        StringBuilder sb = new StringBuilder();
        sb.append("基于你近30天的运动数据制定此计划：\n");
        sb.append(String.format("· 跑了 %d 次，平均单次 %.1f 公里，平均配速 %d'%02d\" /km\n",
                activityCount, avgDistanceKm, avgPace / 60, avgPace % 60));
        sb.append("· 训练安排遵循80/20原则（80%低强度有氧 + 20%高强度）\n");
        sb.append("· 每周递增8%跑量，避免过度训练\n");
        AthleteStyle style = AthleteStyle.of(request.getAthleteStyle());
        if (style.displayName() != null) {
            sb.append(String.format("· 参考职业运动员 %s：%s\n", style.displayName(), style.desc()));
        }
        sb.append(String.format("· 每周安排 %d 次训练，每次约 %d 分钟\n",
                request.getWeeklyTrainingDays(), request.getTrainingDurationMinutes()));
        sb.append("· 长距离跑安排在周六，节奏/间歇安排在周三");
        return sb.toString();
    }

    /** AI 版计划推理：配置了大模型 key 则由模型生成，否则返回规则文案 */
    private String buildAiReasoning(GeneratePlanRequest request, int activityCount,
                                    double avgDistanceKm, int avgPace, double longestRunKm,
                                    String fallback) {
        try {
            AthleteStyle style = AthleteStyle.of(request.getAthleteStyle());
            String styleText = style.displayName() != null
                    ? style.displayName() + "（" + style.desc() + "）"
                    : "通用 80/20 周期化训练风格";

            String prompt = String.format(
                    "请用简体中文写一段‘计划制定思路’（4~6 行，每行以·开头，不要标题）：%n"
                            + "目标：%s，目标值：%s%n周期：%s 至 %s，每周 %d 次训练，每次约 %d 分钟%n"
                            + "用户近30天数据：共 %d 次跑步，平均 %.1f 公里/次，最长 %.1f 公里，平均配速 %d 分 %02d 秒/公里%n"
                            + "参考训练风格：%s%n"
                            + "要求：结合 80/20 原则、每周递增与该运动员风格，说明为什么这样安排，不要编造用户数据。",
                    goalLabel(request.getGoalType()),
                    request.getGoalValue() == null || request.getGoalValue().isBlank() ? "未设置" : request.getGoalValue(),
                    request.getStartDate(), request.getEndDate(),
                    request.getWeeklyTrainingDays(), request.getTrainingDurationMinutes(),
                    activityCount, avgDistanceKm, longestRunKm, avgPace / 60, avgPace % 60,
                    styleText);
            String ai = aiClient.chat("你是国家队级别的马拉松教练，制定计划严谨、表达简洁。", prompt);
            return ai != null ? ai : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
