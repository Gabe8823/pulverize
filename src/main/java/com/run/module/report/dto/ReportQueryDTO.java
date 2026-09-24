package com.run.module.report.dto;

import lombok.Data;

/**
 * 训练报告查询参数
 */
@Data
public class ReportQueryDTO {

    /** 统计粒度（必填）：DAY / WEEK / MONTH / QUARTER / YEAR / CUSTOM */
    private String granularity;

    /** 起始日期 yyyy-MM-dd（CUSTOM 使用） */
    private String startDate;

    /** 结束日期 yyyy-MM-dd（CUSTOM 使用） */
    private String endDate;
}
