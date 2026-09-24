package com.run.module.goal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_user_goal")
public class UserGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String goalType;

    private Double targetValue;

    private Double currentValue;

    private String unit;

    private LocalDate deadline;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
