package com.run.module.running.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_running_activity")
public class RunningActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String platform;

    private String platformActivityId;

    private String activityName;

    private String activityType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationSeconds;

    private Double distanceM;

    private Integer avgPaceSecKm;

    private Integer maxPaceSecKm;

    private Integer avgHeartRate;

    private Integer maxHeartRate;

    private Integer avgCadence;

    private Double avgStrideM;

    private Integer calories;

    private Double elevationGainM;

    private Double elevationLossM;

    /** COROS 官方心率区间(type=126 分组)JSON，含 zoneIndex/percent/second 等 */
    private String hrZoneJson;

    private String gpxUrl;

    private String mapImageUrl;

    private Double trainingEffectAerobic;

    private Double trainingEffectAnaerobic;

    private Double vo2max;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
