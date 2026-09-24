package com.run.module.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GeneratePlanRequest {

    @NotNull(message = "目标类型不能为空")
    private String goalType;       // 5K_PB / 10K_PB / HALF_MARATHON / MARATHON / LOSE_WEIGHT / KEEP_FIT

    private String goalValue;      // 例如 "25:00" 或 "完赛"

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private Integer weeklyTrainingDays = 4;   // 每周训练天数

    private Integer trainingDurationMinutes = 60; // 每次训练时长(分钟)

    /** 参考的职业运动员训练风格: KIPCHOGE / OSAKO / HASAN / 空(通用) */
    private String athleteStyle;

    @Data
    public static class DailyPlanItem {
        private LocalDate planDate;
        private Integer dayOfWeek;
        private String workoutType;
        private Double targetDistanceKm;
        private Integer targetDurationMin;
        private Integer targetPaceSecKm;
        private String targetHrZone;
        private String workoutDescription;
    }

    @Data
    public static class GeneratePlanResponse {
        private Long planId;
        private String planName;
        private String aiReasoning;
        private List<DailyPlanItem> dailyPlan;
    }
}
