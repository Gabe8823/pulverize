package com.run.module.running.dto;

import lombok.Data;

@Data
public class WeeklyStatsVO {

    private Integer totalActivities;
    private Double totalDistanceM;
    private Integer totalDurationSec;
    private Integer totalCalories;
    private Integer avgPace;
    private Integer avgHeartRate;
}
