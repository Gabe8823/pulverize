package com.run.module.report.dto;

import lombok.Data;

/**
 * 报告整体汇总
 */
@Data
public class ReportSummaryVO {

    /** 跑步次数 */
    private int runs;

    /** 总距离（公里，2 位小数） */
    private double distanceKm;

    /** 总时长（分钟） */
    private int durationMin;

    /** 平均配速 s/km（总秒/总公里，无距离时 null） */
    private Integer avgPaceSecKm;

    /** 平均心率（无数据 null） */
    private Integer avgHeartRate;

    /** 消耗热量 kcal（null 记 0） */
    private Integer calories;

    /** 累计爬升 米（null 记 0） */
    private double elevationGainM;
}
