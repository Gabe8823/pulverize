package com.run.module.report.dto;

import lombok.Data;

import java.util.List;

/**
 * 训练报告
 */
@Data
public class ReportVO {

    /** 统计粒度：DAY / WEEK / MONTH / QUARTER / YEAR / CUSTOM */
    private String granularity;

    /** 实际起始日期 yyyy-MM-dd */
    private String startDate;

    /** 实际结束日期 yyyy-MM-dd */
    private String endDate;

    /** 周期展示标签，如「本周（09-21 ~ 09-23）」 */
    private String periodLabel;

    /** 汇总统计 */
    private ReportSummaryVO summary;

    /** 分桶趋势（含空桶，label 升序） */
    private List<ReportBucketVO> buckets;

    /** 报告正文（AI 或规则兜底） */
    private String aiReport;

    /** 是否由 AI 生成 */
    private boolean aiGenerated;

    /** 是否已配置可用的 AI 服务（false 时前端提示去「个人资料 → AI 服务接入」配置密钥） */
    private boolean aiAvailable;
}
