package com.run.module.analysis.dto;

import lombok.Data;

import java.util.List;

/**
 * 肌肉热力图 VO：按运动类型、时长与强度估算的近 N 天肌群负荷（参考高驰 App 肌肉负荷口径）。
 * 所有展示名称为中文，前端直接渲染。
 */
@Data
public class MuscleMapVO {

    /** 统计窗口（天） */
    private Integer days;

    /** 窗口内活动次数 */
    private Integer activityCount;

    /** 窗口内总时长（分钟） */
    private Integer totalMinutes;

    /** 各肌群负荷，按分数降序 */
    private List<MuscleItem> muscles;

    @Data
    public static class MuscleItem {

        /** 肌群 key，前端映射人体 SVG 区域 */
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
    }
}
