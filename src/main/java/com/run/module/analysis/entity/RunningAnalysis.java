package com.run.module.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_running_analysis")
public class RunningAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private String analysisType;

    private String analysisData;

    private String aiSummary;

    private Integer score;

    private LocalDateTime createTime;
}
