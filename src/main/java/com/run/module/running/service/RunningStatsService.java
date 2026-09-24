package com.run.module.running.service;

import com.run.module.running.entity.RunningActivity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跑步数据统计服务（只读）——跨模块获取跑步记录的统一出口。
 *
 * 目标（goal）、报告（report）、分析（analysis）等模块通过本服务读取跑步数据，
 * 不再直接注入 RunningActivityMapper，降低模块间耦合。
 */
public interface RunningStatsService {

    /** 用户全量跑步记录（startTime 升序，不含已逻辑删除） */
    List<RunningActivity> listAll(Long userId);

    /** 时间区间 [start, end) 内的跑步记录（startTime 升序） */
    List<RunningActivity> listByRange(Long userId, LocalDateTime start, LocalDateTime end);

    /** 按 ID 读取单条记录并校验归属，不属于该用户或不存在返回 null */
    RunningActivity getById(Long userId, Long activityId);
}
