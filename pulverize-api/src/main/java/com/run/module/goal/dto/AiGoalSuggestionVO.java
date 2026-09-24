package com.run.module.goal.dto;

import lombok.Data;

/**
 * 目标 AI 推荐结果
 *
 * source = "AI" 表示至少一个字段被大模型建议覆盖；
 * source = "RULE" 表示完全由规则基线给出（AI 未启用或全部字段校验未通过）。
 */
@Data
public class AiGoalSuggestionVO {

    /** 建议目标值（必有） */
    private Double targetValue;

    /** 单位：km / s/km 等 */
    private String unit;

    /** 截止日期 yyyy-MM-dd，可为 null */
    private String deadline;

    /** 中文建议（2~4 句，必有） */
    private String suggestion;

    /** AI 或 RULE */
    private String source;
}
