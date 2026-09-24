package com.run.module.platform.connector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.exception.BizException;
import com.run.module.platform.client.CorosApiClient;
import com.run.module.platform.dto.*;
import com.run.module.platform.entity.PlatformAuth;
import com.run.module.platform.mapper.PlatformAuthMapper;
import com.run.module.platform.spi.PlatformConnector;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.entity.RunningLap;
import com.run.module.running.mapper.RunningActivityMapper;
import com.run.module.running.mapper.RunningLapMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 高驰(COROS)平台连接器
 *
 * 单位换算依据（列表值与详情值交叉验证，见 CorosActivityResponse 注释）:
 *   列表: 距离米 / 时长秒 / 时间戳秒 / 爬升下降米
 *   详情 summary: 距离厘米(÷100) / 时间戳与总时长厘秒(÷100) / 爬升下降米
 *   详情 lap: 距离厘米(÷100) / 时长厘秒(÷100) / 爬升米
 *   卡路里一律为千卡×1000(÷1000)；avgSpeed 实为配速(秒/公里)
 *
 * 分段选取优先级: 用户自定义分段(≥2段) > 1公里自动分段(lapDistance=100000) > 其它分组
 * 回填: 已同步但没有分段、或没有心率区间(hr_zone_json 为空)的历史记录，
 *       同步时自动重新拉取详情补齐（并修正历史单位换算错误）。
 * 心率区间: 详情 zoneList 中 type=126 的分组为 COROS 官方心率区间统计，
 *           序列化存入 t_running_activity.hr_zone_json，供分析直接使用 percent。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorosConnector implements PlatformConnector {

    public static final String PLATFORM = "COROS";

    private final PlatformAuthMapper platformAuthMapper;
    private final CorosApiClient corosApiClient;
    private final RunningActivityMapper activityMapper;
    private final RunningLapMapper lapMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String displayName() {
        return "高驰 COROS";
    }

    @Override
    public String description() {
        return "绑定高驰账号，自动同步手表记录的跑步活动、分段与心率数据。";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    @Transactional
    public SyncStatusVO bind(Long userId, PlatformBindRequest request) {
        // 1. 调用高驰 API 登录
        CorosLoginResponse.LoginResult loginResult = corosApiClient.login(
                request.getAccount(), request.getPassword(), request.getRegion());

        // 2. 保存/更新平台授权
        PlatformAuth auth = platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM));

        if (auth == null) {
            auth = new PlatformAuth();
            auth.setUserId(userId);
            auth.setPlatform(PLATFORM);
        }

        auth.setPlatformUserId(loginResult.getUserId() != null ? loginResult.getUserId() : loginResult.getOpenId());
        auth.setAccessToken(loginResult.getAccessToken());
        auth.setSyncStatus(0);
        auth.setTokenExpireTime(LocalDateTime.now().plusDays(30));

        if (auth.getId() != null) {
            platformAuthMapper.updateById(auth);
        } else {
            platformAuthMapper.insert(auth);
        }

        SyncStatusVO vo = new SyncStatusVO();
        vo.setStatus(0);
        vo.setBound(true);
        vo.setMessage("绑定成功，请点击同步数据");
        return vo;
    }

    @Override
    @Transactional
    public SyncStatusVO sync(Long userId) {
        PlatformAuth auth = getAuth(userId);

        try {
            // 更新状态为同步中
            auth.setSyncStatus(1);
            platformAuthMapper.updateById(auth);

            int syncedCount = 0;
            int backfilledCount = 0;
            int pageNumber = 1;
            int pageSize = 20;

            // 分页拉取活动列表（只取跑步类）
            while (pageNumber <= 200) {
                CorosActivityResponse.CorosActivityData activityData = corosApiClient.queryActivities(
                        auth.getAccessToken(), "cn", "100,101,102,103", pageNumber, pageSize);

                if (activityData == null || activityData.getDataList() == null
                        || activityData.getDataList().isEmpty()) {
                    break;
                }

                for (CorosActivityResponse.CorosActivityItem item : activityData.getDataList()) {
                    if (item.getLabelId() == null) {
                        continue;
                    }

                    RunningActivity existing = findByPlatformActivityId(userId, item.getLabelId());

                    // 已同步、有分段且已有心率区间：仅刷新列表字段（起止时间/距离/名称等以平台列表为准，
                    // 活动可能在平台侧被修正）；没有分段或缺心率区间的（历史数据）回填详情
                    if (existing != null && lapCount(existing.getId()) > 0 && hasHrZone(existing)) {
                        applyListFields(existing, item);
                        activityMapper.updateById(existing);
                        continue;
                    }

                    // 拉取活动详情（summary + 分段）
                    CorosActivityResponse.CorosActivityDetailData detail = null;
                    try {
                        detail = corosApiClient.queryActivityDetail(
                                auth.getAccessToken(), auth.getPlatformUserId(), "cn",
                                item.getLabelId(), item.getSportType());
                    } catch (Exception e) {
                        log.warn("获取活动详情失败: {}", item.getLabelId(), e);
                    }

                    if (existing == null) {
                        saveActivity(userId, item, detail);
                        syncedCount++;
                    } else if (detail != null) {
                        // 回填：修正历史单位换算（爬升、时长等）并补最大心率与分段
                        updateActivity(existing, item, detail);
                        backfilledCount++;
                    }
                }

                boolean lastPage = activityData.getTotalPage() != null
                        ? pageNumber >= activityData.getTotalPage()
                        : activityData.getDataList().size() < pageSize;
                if (lastPage) {
                    break;
                }
                pageNumber++;
            }

            // 更新状态为同步完成
            auth.setSyncStatus(2);
            auth.setLastSyncTime(LocalDateTime.now());
            platformAuthMapper.updateById(auth);

            SyncStatusVO vo = new SyncStatusVO();
            vo.setStatus(2);
            vo.setBound(true);
            String msg = "同步完成，共新增 " + syncedCount + " 条记录";
            if (backfilledCount > 0) {
                msg += "，回填详情 " + backfilledCount + " 条";
            }
            vo.setMessage(msg);
            vo.setSyncedCount(syncedCount + backfilledCount);
            vo.setLastSyncTime(LocalDateTime.now().toString());
            return vo;

        } catch (Exception e) {
            log.error("同步高驰数据失败: userId={}", userId, e);
            auth.setSyncStatus(3);
            platformAuthMapper.updateById(auth);

            SyncStatusVO vo = new SyncStatusVO();
            vo.setStatus(3);
            vo.setBound(true);
            vo.setMessage("同步失败: " + e.getMessage());
            return vo;
        }
    }

    @Override
    public SyncStatusVO status(Long userId) {
        PlatformAuth auth = platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM));

        SyncStatusVO vo = new SyncStatusVO();
        if (auth == null) {
            vo.setStatus(0);
            vo.setBound(false);
            vo.setMessage("未绑定高驰账号");
            vo.setSyncedCount(0);
            return vo;
        }

        vo.setBound(true);
        vo.setStatus(auth.getSyncStatus());
        vo.setLastSyncTime(auth.getLastSyncTime() != null ? auth.getLastSyncTime().toString() : null);

        switch (auth.getSyncStatus()) {
            case 0 -> vo.setMessage("已绑定，待同步");
            case 1 -> vo.setMessage("同步中...");
            case 2 -> vo.setMessage("同步完成");
            case 3 -> vo.setMessage("同步失败");
            default -> vo.setMessage("未知状态");
        }

        vo.setSyncedCount(Math.toIntExact(activityMapper.selectCount(
                new LambdaQueryWrapper<RunningActivity>()
                        .eq(RunningActivity::getUserId, userId)
                        .eq(RunningActivity::getPlatform, PLATFORM))));
        return vo;
    }

    @Override
    public void unbind(Long userId) {
        platformAuthMapper.delete(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM));
    }

    // ==================== 内部方法 ====================

    private PlatformAuth getAuth(Long userId) {
        PlatformAuth auth = platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM));
        if (auth == null) {
            throw new BizException("未绑定高驰账号");
        }
        return auth;
    }

    private RunningActivity findByPlatformActivityId(Long userId, String platformActivityId) {
        return activityMapper.selectOne(
                new LambdaQueryWrapper<RunningActivity>()
                        .eq(RunningActivity::getUserId, userId)
                        .eq(RunningActivity::getPlatform, PLATFORM)
                        .eq(RunningActivity::getPlatformActivityId, platformActivityId));
    }

    /** 是否已有 COROS 心率区间数据（没有则同步时重新拉详情回填） */
    private boolean hasHrZone(RunningActivity activity) {
        return activity.getHrZoneJson() != null && !activity.getHrZoneJson().isBlank();
    }

    private long lapCount(Long activityId) {
        return lapMapper.selectCount(
                new LambdaQueryWrapper<RunningLap>().eq(RunningLap::getActivityId, activityId));
    }

    /** 新增活动 */
    private void saveActivity(Long userId, CorosActivityResponse.CorosActivityItem item,
                              CorosActivityResponse.CorosActivityDetailData detail) {
        RunningActivity activity = new RunningActivity();
        activity.setUserId(userId);
        activity.setPlatform(PLATFORM);
        activity.setPlatformActivityId(item.getLabelId());
        activity.setDeleted(0);

        applyListFields(activity, item);
        applyDetailFields(activity, detail);
        activityMapper.insert(activity);

        insertLaps(activity.getId(), detail);
    }

    /** 回填/更新已有活动（修正单位 + 补详情字段与分段） */
    private void updateActivity(RunningActivity activity,
                                CorosActivityResponse.CorosActivityItem item,
                                CorosActivityResponse.CorosActivityDetailData detail) {
        applyListFields(activity, item);
        applyDetailFields(activity, detail);
        activityMapper.updateById(activity);

        if (hasLapData(detail)) {
            lapMapper.delete(new LambdaQueryWrapper<RunningLap>()
                    .eq(RunningLap::getActivityId, activity.getId()));
            insertLaps(activity.getId(), detail);
        }
    }

    /** 详情里是否有可用分段（没有则不能删旧分段，避免回填时把已有分段清空） */
    private boolean hasLapData(CorosActivityResponse.CorosActivityDetailData detail) {
        return detail != null && detail.getLapList() != null
                && detail.getLapList().stream()
                .anyMatch(g -> g.getLapItemList() != null && !g.getLapItemList().isEmpty());
    }

    /** 列表字段（单位已是目标单位） */
    private void applyListFields(RunningActivity activity, CorosActivityResponse.CorosActivityItem item) {
        if (item.getName() != null && !item.getName().isBlank()) {
            activity.setActivityName(item.getName());
        } else if (activity.getActivityName() == null) {
            activity.setActivityName("COROS " + item.getSportType() + " 运动");
        }
        activity.setActivityType(mapActivityType(item.getSportType()));

        LocalDateTime start = fromEpochSeconds(item.getStartTime());
        if (start == null && item.getDate() != null) {
            start = fromCorosDate(item.getDate());
        }
        activity.setStartTime(start);
        LocalDateTime end = fromEpochSeconds(item.getEndTime());
        if (end == null && start != null && item.getTotalTime() != null) {
            end = start.plusSeconds(item.getTotalTime());
        }
        activity.setEndTime(end);

        activity.setDurationSeconds(item.getTotalTime());
        activity.setDistanceM(item.getDistance());
        activity.setAvgPaceSecKm(toInt(item.getAvgSpeed()));      // avgSpeed 实为配速(秒/公里)
        activity.setAvgHeartRate(item.getAvgHr());
        activity.setMaxHeartRate(item.getMaxHr());
        activity.setAvgCadence(item.getAvgCadence());
        activity.setCalories(toCalories(item.getCalorie()));      // 千卡 x1000 -> 千卡
        activity.setElevationGainM(item.getAscent());             // 米
        activity.setElevationLossM(item.getTotalDescent() != null
                ? item.getTotalDescent() : item.getDescent());    // 米
        activity.setRemark(item.getDevice());
    }

    /** 详情摘要覆盖/补充列表缺失字段（最大心率、训练效果、VO2max 等），注意单位换算 */
    private void applyDetailFields(RunningActivity activity,
                                   CorosActivityResponse.CorosActivityDetailData detail) {
        if (detail == null) {
            return;
        }
        // 心率区间（COROS 官方统计，与 summary 是否存在无关）：只存 type=126 的分组
        applyHrZone(activity, detail);
        if (detail.getSummary() == null) {
            return;
        }
        CorosActivityResponse.CorosDetailSummary s = detail.getSummary();
        // 起止时间以列表字段为准：详情 start/endTimestamp 单位不可靠（曾换算错误把全部
        // 活动 start/end 写成 1970 年，导致按日期的统计/计划查询全部落空），
        // 列表 startTime/endTime 为 epoch 秒且已验证正确，此处不再覆盖
        if (s.getTotalTime() != null) {
            activity.setDurationSeconds((int) Math.round(s.getTotalTime() / 100.0)); // 厘秒 -> 秒
        }
        if (s.getDistance() != null) {
            activity.setDistanceM(Math.round(s.getDistance()) / 100.0);       // 厘米 -> 米
        }
        if (s.getAvgSpeed() != null) activity.setAvgPaceSecKm(toInt(s.getAvgSpeed()));
        if (s.getAvgHr() != null) activity.setAvgHeartRate(s.getAvgHr());
        if (s.getMaxHr() != null) activity.setMaxHeartRate(s.getMaxHr());
        if (s.getAvgCadence() != null) activity.setAvgCadence(s.getAvgCadence());
        if (s.getCalories() != null) activity.setCalories(toCalories(s.getCalories()));
        if (s.getElevGain() != null) activity.setElevationGainM(s.getElevGain());         // 米
        if (s.getTotalDescent() != null) activity.setElevationLossM(s.getTotalDescent()); // 米
        if (s.getAerobicEffect() != null) activity.setTrainingEffectAerobic(s.getAerobicEffect());
        if (s.getAnaerobicEffect() != null) activity.setTrainingEffectAnaerobic(s.getAnaerobicEffect());
        if (s.getCurrentVo2Max() != null) activity.setVo2max(s.getCurrentVo2Max());
    }

    /**
     * 提取详情 zoneList 中 type=126（心率区间）的分组，序列化存入 hr_zone_json。
     * 详情确实没有心率区间时写入 "[]"，避免下次同步反复拉详情回填。
     */
    private void applyHrZone(RunningActivity activity,
                             CorosActivityResponse.CorosActivityDetailData detail) {
        var zones = detail.getZoneList();
        if (zones == null || zones.isEmpty()) {
            if (activity.getHrZoneJson() == null) {
                activity.setHrZoneJson("[]");
            }
            return;
        }
        var hrGroup = zones.stream()
                .filter(g -> g.getType() != null && g.getType() == 126
                        && g.getZoneItemList() != null && !g.getZoneItemList().isEmpty())
                .findFirst()
                .orElse(null);
        if (hrGroup == null) {
            if (activity.getHrZoneJson() == null) {
                activity.setHrZoneJson("[]");
            }
            return;
        }
        try {
            activity.setHrZoneJson(objectMapper.writeValueAsString(hrGroup));
        } catch (Exception e) {
            log.warn("序列化心率区间失败: activityId={}", activity.getId(), e);
            activity.setHrZoneJson("[]");
        }
    }

    /** 保存分段：用户自定义分段(≥2段) > 1公里自动分段 > 其它分组 */
    private void insertLaps(Long activityId, CorosActivityResponse.CorosActivityDetailData detail) {
        if (detail == null || detail.getLapList() == null || detail.getLapList().isEmpty()) {
            return;
        }

        var groups = detail.getLapList().stream()
                .filter(g -> g.getLapItemList() != null && !g.getLapItemList().isEmpty())
                .toList();
        if (groups.isEmpty()) {
            return;
        }

        var userGroups = groups.stream()
                .filter(g -> g.getType() != null && g.getType() == -1)
                .toList();

        CorosActivityResponse.CorosLapGroup chosen;
        if (!userGroups.isEmpty() && userGroups.get(0).getLapItemList().size() >= 2) {
            chosen = userGroups.get(0);   // 用户自定义分段（间歇/组合课表）
        } else {
            chosen = groups.stream()
                    .filter(g -> g.getLapDistance() != null && g.getLapDistance() == 100_000L) // 1 公里自动分段
                    .findFirst()
                    .orElseGet(() -> !userGroups.isEmpty() ? userGroups.get(0) : groups.get(0));
        }

        int index = 1;
        for (CorosActivityResponse.CorosLapItem lapItem : chosen.getLapItemList()) {
            RunningLap lap = new RunningLap();
            lap.setActivityId(activityId);
            lap.setLapIndex(lapItem.getLapIndex() != null ? lapItem.getLapIndex() : index);

            Double distM = lapItem.getDistance() != null
                    ? Math.round(lapItem.getDistance()) / 100.0 : null;                 // 厘米 -> 米
            Integer durSec = lapItem.getTime() != null
                    ? (int) Math.round(lapItem.getTime() / 100.0) : null;               // 厘秒 -> 秒

            Integer pace = lapItem.getAvgPace();
            if ((pace == null || pace <= 0) && distM != null && distM > 0 && durSec != null) {
                pace = (int) Math.round(durSec / (distM / 1000.0));                     // 缺配速按时距推算
            }

            lap.setSplitDistanceM(distM);
            lap.setSplitDurationSec(durSec);
            lap.setAvgPaceSecKm(pace);
            lap.setAvgHeartRate(lapItem.getAvgHr());
            lap.setMaxHeartRate(lapItem.getMaxHr());
            lap.setAvgCadence(lapItem.getAvgCadence());
            lap.setElevationGainM(lapItem.getElevGain());                               // 米
            lapMapper.insert(lap);
            index++;
        }
    }

    private String mapActivityType(Integer sportType) {
        if (sportType == null) return "RUNNING";
        return switch (sportType) {
            case 101 -> "INDOOR_RUN";
            case 102 -> "TRAIL_RUN";
            case 103 -> "VIRTUAL_RUN";
            case 100, 104 -> "RUNNING";
            default -> "RUNNING";
        };
    }

    private Integer toInt(Double v) {
        return v == null ? null : (int) Math.round(v);
    }

    /** 千卡 x1000 -> 千卡 */
    private Integer toCalories(Double v) {
        return v == null ? null : (int) Math.round(v / 1000.0);
    }

    private LocalDateTime fromEpochSeconds(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return null;
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }

    /** COROS date 字段 20220205 -> LocalDateTime */
    private LocalDateTime fromCorosDate(Integer date) {
        int d = date;
        return java.time.LocalDate.of(d / 10000, (d / 100) % 100, d % 100).atStartOfDay();
    }
}
