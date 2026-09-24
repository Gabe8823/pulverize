package com.run.module.running.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_running_lap")
public class RunningLap {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Integer lapIndex;

    private Double splitDistanceM;

    private Integer splitDurationSec;

    private Integer avgPaceSecKm;

    private Integer avgHeartRate;

    private Integer maxHeartRate;

    private Integer avgCadence;

    private Double elevationGainM;
}
