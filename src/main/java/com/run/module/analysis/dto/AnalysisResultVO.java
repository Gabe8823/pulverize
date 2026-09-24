package com.run.module.analysis.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AnalysisResultVO {

    private Long activityId;
    private String activityName;
    private Integer score;
    private String aiSummary;

    /** 配速分析 */
    private Map<String, Object> paceAnalysis;

    /** 心率分析 */
    private Map<String, Object> heartRateAnalysis;

    /** 步频分析 */
    private Map<String, Object> cadenceAnalysis;

    /** 疲劳度分析 */
    private Map<String, Object> fatigueAnalysis;

    /** 训练效果评估 */
    private Map<String, Object> trainingEffect;
}
