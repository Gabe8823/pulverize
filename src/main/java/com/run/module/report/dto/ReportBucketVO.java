package com.run.module.report.dto;

import lombok.Data;

/**
 * 报告分桶（趋势图数据点，空桶也返回）
 */
@Data
public class ReportBucketVO {

    /** 桶标签，如「6点」「周一」「9/1」「09-01」「1月」 */
    private String label;

    /** 桶内跑步次数 */
    private int runs;

    /** 桶内距离（公里） */
    private double distanceKm;

    /** 桶内时长（分钟） */
    private int durationMin;

    /** 桶内平均配速 s/km（距离>0 否则 null） */
    private Integer avgPaceSecKm;

    /** 桶内平均心率（无数据 null） */
    private Integer avgHeartRate;
}
