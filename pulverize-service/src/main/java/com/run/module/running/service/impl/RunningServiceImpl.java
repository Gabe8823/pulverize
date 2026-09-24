package com.run.module.running.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.run.common.exception.BizException;
import com.run.module.running.dto.*;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.entity.RunningLap;
import com.run.module.running.mapper.RunningActivityMapper;
import com.run.module.running.mapper.RunningLapMapper;
import com.run.module.running.service.RunningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RunningServiceImpl implements RunningService {

    private final RunningActivityMapper activityMapper;
    private final RunningLapMapper lapMapper;

    @Override
    @Transactional
    public RunningActivity createActivity(Long userId, ActivityCreateDTO dto) {
        RunningActivity activity = new RunningActivity();
        activity.setUserId(userId);
        activity.setPlatform("MANUAL");
        activity.setActivityName(dto.getActivityName() != null ? dto.getActivityName()
                : buildDefaultName(dto.getStartTime()));
        activity.setActivityType(dto.getActivityType());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setDurationSeconds(dto.getDurationSeconds());
        activity.setDistanceM(dto.getDistanceM());
        activity.setAvgHeartRate(dto.getAvgHeartRate());
        activity.setMaxHeartRate(dto.getMaxHeartRate());
        activity.setAvgCadence(dto.getAvgCadence());
        activity.setCalories(dto.getCalories());
        activity.setElevationGainM(dto.getElevationGainM());
        activity.setRemark(dto.getRemark());
        activity.setDeleted(0);

        // 自动计算配速: 秒/公里
        if (dto.getDistanceM() > 0) {
            activity.setAvgPaceSecKm((int) (dto.getDurationSeconds() / (dto.getDistanceM() / 1000.0)));
        }

        activityMapper.insert(activity);

        // 保存分段数据
        if (!CollectionUtils.isEmpty(dto.getLaps())) {
            for (ActivityCreateDTO.LapDTO lapDTO : dto.getLaps()) {
                RunningLap lap = new RunningLap();
                lap.setActivityId(activity.getId());
                lap.setLapIndex(lapDTO.getLapIndex());
                lap.setSplitDistanceM(lapDTO.getSplitDistanceM());
                lap.setSplitDurationSec(lapDTO.getSplitDurationSec());
                lap.setAvgPaceSecKm(lapDTO.getAvgPaceSecKm());
                lap.setAvgHeartRate(lapDTO.getAvgHeartRate());
                lapMapper.insert(lap);
            }
        }

        return activity;
    }

    @Override
    public IPage<RunningActivity> listActivities(Long userId, ActivityQueryDTO query) {
        Page<RunningActivity> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<RunningActivity> wrapper = new LambdaQueryWrapper<RunningActivity>()
                .eq(RunningActivity::getUserId, userId)
                .eq(RunningActivity::getDeleted, 0)
                .ge(query.getStartDate() != null,
                        RunningActivity::getStartTime, query.getStartDate() != null ? query.getStartDate().atStartOfDay() : null)
                .lt(query.getEndDate() != null,
                        RunningActivity::getStartTime, query.getEndDate() != null ? query.getEndDate().plusDays(1).atStartOfDay() : null)
                .eq(query.getActivityType() != null,
                        RunningActivity::getActivityType, query.getActivityType())
                .orderByDesc(RunningActivity::getStartTime);

        return activityMapper.selectPage(page, wrapper);
    }

    @Override
    public ActivityDetailVO getActivityDetail(Long userId, Long activityId) {
        RunningActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getUserId().equals(userId)) {
            throw new BizException("跑步记录不存在");
        }

        List<RunningLap> laps = lapMapper.selectList(
                new LambdaQueryWrapper<RunningLap>()
                        .eq(RunningLap::getActivityId, activityId)
                        .orderByAsc(RunningLap::getLapIndex));

        ActivityDetailVO vo = new ActivityDetailVO();
        vo.setActivity(activity);
        vo.setLaps(laps);

        // 格式化字段
        vo.setDistanceText(String.format("%.2f km", activity.getDistanceM() / 1000));
        vo.setDurationText(formatDuration(activity.getDurationSeconds()));
        vo.setPaceText(activity.getAvgPaceSecKm() != null ? formatPace(activity.getAvgPaceSecKm()) : "--");
        vo.setCaloriesText(activity.getCalories() != null ? activity.getCalories() + " kcal" : "--");

        return vo;
    }

    @Override
    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        RunningActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getUserId().equals(userId)) {
            throw new BizException("跑步记录不存在");
        }
        activityMapper.deleteById(activityId);

        // 删除关联的分段数据
        lapMapper.delete(new LambdaQueryWrapper<RunningLap>()
                .eq(RunningLap::getActivityId, activityId));
    }

    @Override
    public WeeklyStatsVO getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);
        return activityMapper.selectWeeklyStats(userId, weekStart, weekEnd);
    }

    @Override
    public WeeklyStatsVO getAllTimeStats(Long userId) {
        return activityMapper.selectAllTimeStats(userId);
    }

    private String buildDefaultName(LocalDateTime startTime) {
        return String.format("%d月%d日跑步", startTime.getMonthValue(), startTime.getDayOfMonth());
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    private String formatPace(int paceSecKm) {
        return String.format("%d'%02d\"", paceSecKm / 60, paceSecKm % 60);
    }
}
