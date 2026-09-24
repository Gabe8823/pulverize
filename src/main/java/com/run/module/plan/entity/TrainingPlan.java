package com.run.module.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_training_plan")
public class TrainingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String planName;

    private String goalType;

    private String goalValue;

    private LocalDate startDate;

    private LocalDate endDate;

    private String weeklyPlan;

    private Integer status;

    private String aiReasoning;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
