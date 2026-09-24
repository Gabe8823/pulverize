package com.run.module.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_training_plan_detail")
public class TrainingPlanDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private LocalDate planDate;

    private Integer dayOfWeek;

    private String workoutType;

    private Double targetDistanceKm;

    private Integer targetDurationMin;

    private Integer targetPaceSecKm;

    private String targetHrZone;

    private String workoutDescription;

    private Integer status;

    private Long actualActivityId;

    private LocalDateTime createTime;
}
