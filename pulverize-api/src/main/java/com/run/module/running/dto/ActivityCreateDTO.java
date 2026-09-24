package com.run.module.running.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityCreateDTO {

    private String activityName;

    private String activityType = "RUNNING";

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "运动时长不能为空")
    @Positive(message = "运动时长必须大于0")
    private Integer durationSeconds;

    @NotNull(message = "运动距离不能为空")
    @Positive(message = "运动距离必须大于0")
    private Double distanceM;

    private Integer avgHeartRate;

    private Integer maxHeartRate;

    private Integer avgCadence;

    private Integer calories;

    private Double elevationGainM;

    private String remark;

    /** 分段数据（可选） */
    private List<LapDTO> laps;

    @Data
    public static class LapDTO {
        private Integer lapIndex;
        private Double splitDistanceM;
        private Integer splitDurationSec;
        private Integer avgPaceSecKm;
        private Integer avgHeartRate;
    }
}
