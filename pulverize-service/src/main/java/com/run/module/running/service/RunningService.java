package com.run.module.running.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.run.module.running.dto.*;
import com.run.module.running.entity.RunningActivity;

public interface RunningService {

    /** 新增跑步记录（手动录入） */
    RunningActivity createActivity(Long userId, ActivityCreateDTO dto);

    /** 分页查询跑步记录 */
    IPage<RunningActivity> listActivities(Long userId, ActivityQueryDTO query);

    /** 获取跑步详情（含分段数据 + 格式化字段） */
    ActivityDetailVO getActivityDetail(Long userId, Long activityId);

    /** 删除跑步记录 */
    void deleteActivity(Long userId, Long activityId);

    /** 本周统计 */
    WeeklyStatsVO getWeeklyStats(Long userId);

    /** 全量统计 */
    WeeklyStatsVO getAllTimeStats(Long userId);
}
