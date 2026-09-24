package com.run.module.analysis.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 跑力指数 VDOT（GET /analysis/vdot）
 *
 * - 基于该用户【全部历史数据】推算：遍历所有满足条件的跑步
 *   （距离 ≥ 2km、时长 ≥ 8 分钟、配速 2:00~15:00/km），取 VDOT 最高的一次，
 *   不限定"近期比赛"。
 * - source=RACE 表示已用最优表现完成推算；NONE 表示数据不足（race/equivalents/zones 为空）。
 */
@Data
public class VdotVO {

    /** VDOT 值（四舍五入到整数位精度），NONE 时为 null */
    private Double vdot;

    /** RACE=已推算（基于最优表现） NONE=数据不足 */
    private String source;

    /** 提示文案（说明推算依据或数据不足原因） */
    private String message;

    /** 参考的最优表现（source=RACE 时有值） */
    private Race race;

    /** 等效完赛时间（秒），key："5K" / "10K" / "半马" / "全马" */
    private Map<String, Long> equivalents;

    /** Daniels 五档训练配速区间 */
    private List<Zone> zones;

    /** 参考的最优表现 */
    @Data
    public static class Race {
        private Long activityId;
        private String activityName;
        /** 活动日期 yyyy-MM-dd */
        private String raceDate;
        private Double distanceKm;
        private Integer durationSec;
        private Integer paceSecKm;
    }

    /** 训练配速区间 */
    @Data
    public static class Zone {
        /** 区间名：轻松跑 / 马拉松配速 / 乳酸阈配速 / 间歇配速 / 重复跑配速 */
        private String name;
        /** 强度标签，如 "59–74%" */
        private String percentLabel;
        /** 区间慢端配速（秒/公里，数值更大） */
        private Integer slowPaceSecKm;
        /** 区间快端配速（秒/公里，数值更小） */
        private Integer fastPaceSecKm;
    }
}
