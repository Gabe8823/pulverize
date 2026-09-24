package com.run.module.running.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.mapper.RunningActivityMapper;
import com.run.module.running.service.RunningStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 跑步数据统计服务实现（只读，跨模块统一出口） */
@Service
@RequiredArgsConstructor
public class RunningStatsServiceImpl implements RunningStatsService {

    private final RunningActivityMapper activityMapper;

    @Override
    public List<RunningActivity> listAll(Long userId) {
        return activityMapper.selectList(
                new LambdaQueryWrapper<RunningActivity>()
                        .eq(RunningActivity::getUserId, userId)
                        .orderByAsc(RunningActivity::getStartTime));
    }

    @Override
    public List<RunningActivity> listByRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return activityMapper.selectList(
                new LambdaQueryWrapper<RunningActivity>()
                        .eq(RunningActivity::getUserId, userId)
                        .ge(RunningActivity::getStartTime, start)
                        .lt(RunningActivity::getStartTime, end)
                        .orderByAsc(RunningActivity::getStartTime));
    }

    @Override
    public RunningActivity getById(Long userId, Long activityId) {
        RunningActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !userId.equals(activity.getUserId())) {
            return null;
        }
        return activity;
    }
}
