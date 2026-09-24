package com.run.module.analysis.dto;

import lombok.Data;

import java.util.List;

/**
 * 肌群明细 VO：单个肌群在统计窗口内的负荷构成、贡献 Top 活动与建议
 * （GET /analysis/muscle-map/{key}/detail）。名称与建议均为中文，前端直接渲染。
 */
@Data
public class MuscleDetailVO {

    /** 肌群 key */
    private String key;

    /** 中文名 */
    private String name;

    /** 人体视图：FRONT 正面 / BACK 背面 */
    private String view;

    /** 负荷分数 0-100 */
    private Integer score;

    /** 等级：HIGH / MEDIUM / LOW / IDLE */
    private String level;

    /** 等级中文 */
    private String levelText;

    /** 统计窗口（天） */
    private Integer days;

    /** 窗口内活动次数 */
    private Integer activityCount;

    /** 窗口内总时长（分钟） */
    private Integer totalMinutes;

    /** 刺激该肌群的活动次数（肌群权重 > 0） */
    private Integer sessionCount;

    /** 刺激该肌群的累计时长（分钟） */
    private Integer muscleMinutes;

    /** 参与活动的平均强度百分比 0-100（心率口径），无数据为 null */
    private Integer intensityPercent;

    /** 贡献 Top 活动（按贡献降序，最多 5 条） */
    private List<Contribution> contributions;

    /** 恢复 / 训练建议（中文，按等级生成） */
    private String advice;

    @Data
    public static class Contribution {

        /** 活动 ID */
        private Long activityId;

        /** 活动名称 */
        private String activityName;

        /** 运动日期 yyyy-MM-dd */
        private String date;

        /** 该活动时长（分钟） */
        private Integer minutes;

        /** 对该肌群负荷的贡献占比 0-100（一位小数） */
        private Double percent;
    }
}
