package com.run.module.running.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.dto.WeeklyStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface RunningActivityMapper extends BaseMapper<RunningActivity> {

    @Select("""
        SELECT
            COUNT(*) AS totalActivities,
            COALESCE(SUM(distance_m), 0) AS totalDistanceM,
            COALESCE(SUM(duration_seconds), 0) AS totalDurationSec,
            COALESCE(SUM(calories), 0) AS totalCalories,
            COALESCE(ROUND(AVG(avg_pace_sec_km)), 0) AS avgPace,
            COALESCE(ROUND(AVG(avg_heart_rate)), 0) AS avgHeartRate
        FROM t_running_activity
        WHERE user_id = #{userId}
          AND start_time >= #{weekStart}
          AND start_time < #{weekEnd}
          AND deleted = 0
    """)
    WeeklyStatsVO selectWeeklyStats(@Param("userId") Long userId,
                                     @Param("weekStart") LocalDate weekStart,
                                     @Param("weekEnd") LocalDate weekEnd);

    @Select("""
        SELECT
            COUNT(*) AS totalActivities,
            COALESCE(SUM(distance_m), 0) AS totalDistanceM,
            COALESCE(SUM(duration_seconds), 0) AS totalDurationSec,
            COALESCE(SUM(calories), 0) AS totalCalories,
            COALESCE(ROUND(AVG(avg_pace_sec_km)), 0) AS avgPace,
            COALESCE(ROUND(AVG(avg_heart_rate)), 0) AS avgHeartRate
        FROM t_running_activity
        WHERE user_id = #{userId} AND deleted = 0
    """)
    WeeklyStatsVO selectAllTimeStats(@Param("userId") Long userId);
}
