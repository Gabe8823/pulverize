package com.run.module.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * COROS 活动查询响应
 *
 * 列表: GET /activity/query?size=&pageNumber=&modeList=
 *   -> data.dataList[]
 * 详情: POST /activity/detail/query (form: labelId, userId, sportType)
 *   -> data.summary + data.lapList[]
 *
 * 单位约定（依据 Training Hub 实测比对，同一条活动列表值 vs 详情值交叉验证）:
 *   列表: distance 米 / totalTime 秒 / startTime 秒级时间戳 / ascent、descent 米
 *   详情 summary: distance 厘米 / totalTime、startTimestamp、endTimestamp 厘秒(0.01秒)
 *                 elevGain、totalDescent 米（与列表一致）
 *   详情 lap: distance 厘米 / time 厘秒 / elevGain 米
 *   通用: calorie(s) 千卡 x1000 / avgSpeed 字段名虽叫 speed，实际是平均配速(秒/公里)
 *   详情接口响应为 gzip 压缩且不带 Content-Type，需按字节接收后手动解压（见 CorosApiClient）
 */
@Data
public class CorosActivityResponse {

    /** 结果码，成功为 "0000"（高驰没有 code 字段） */
    private String result;
    private String message;
    private String apiCode;
    private CorosActivityData data;

    @Data
    public static class CorosActivityData {
        private Integer count;
        private Integer pageNumber;
        private Integer totalPage;
        private List<CorosActivityItem> dataList;
    }

    @Data
    public static class CorosActivityItem {
        private String labelId;        // 活动唯一 ID
        private String name;           // 活动名称
        private Integer sportType;     // 100=跑步 101=跑步机 102=越野跑 103=虚拟跑
        private Integer date;          // 20220205
        private Long startTime;        // 秒级时间戳
        private Long endTime;          // 秒级时间戳
        private Integer totalTime;     // 总时长(秒)
        private Double distance;       // 距离(米)
        private Double avgSpeed;       // 实为平均配速(秒/公里)
        private Integer avgHr;
        private Integer maxHr;
        private Integer avgCadence;
        private Double calorie;        // 千卡 x1000
        private Double ascent;         // 爬升(米)
        private Double descent;        // 下降(米)
        private Double totalDescent;   // 总下降(米)
        private String device;
    }

    /** COROS 活动详情响应 */
    @Data
    public static class CorosActivityDetail {
        private String result;
        private String message;
        private CorosActivityDetailData data;
    }

    @Data
    public static class CorosActivityDetailData {
        private CorosDetailSummary summary;
        /** 分段分组：type=-1 用户分段；lapDistance 为该组单段距离(厘米)，100000 即 1 公里自动分段 */
        private List<CorosLapGroup> lapList;
        /** 区间统计分组：type=126/zoneType=2 心率区间；type=130/173 配速区间 */
        private List<CorosZoneGroup> zoneList;
    }

    /** zoneList 分组（详情响应末尾） */
    @Data
    public static class CorosZoneGroup {
        private Integer type;          // 126=心率区间；130/173=配速区间
        private Integer zoneType;      // 心率区间为 2（账号 rhrZone 设置）
        private List<CorosZoneItem> zoneItemList;
    }

    @Data
    public static class CorosZoneItem {
        private Integer zoneIndex;     // 0=恢复 1=Z1热身 2=Z2燃脂 3=Z3有氧 4=Z4乳酸阈 5=Z5无氧
        private Integer leftScope;     // 区间下界(bpm)，API 对 zoneIndex=0 回显疑似有 bug，边界仅供参考
        private Integer rightScope;    // 区间上界(bpm)
        private Long second;           // 该区间秒数（权威值，总和≈活动总时长）
        private Integer percent;       // 该区间时间占比(%)，COROS 官方统计（权威值）
    }

    @Data
    public static class CorosDetailSummary {
        private String name;
        private Integer sportType;
        private Long startTimestamp;   // 厘秒(0.01秒)
        private Long endTimestamp;     // 厘秒
        private Integer totalTime;     // 厘秒
        private Double distance;       // 厘米
        private Double elevGain;       // 米
        private Double totalDescent;   // 米
        private Integer avgHr;
        private Integer maxHr;
        private Integer avgCadence;
        private Double avgSpeed;       // 实为平均配速(秒/公里)
        private Double calories;       // 千卡 x1000
        private Double aerobicEffect;  // 有氧训练效果
        private Double anaerobicEffect;
        private Double currentVo2Max;
    }

    @Data
    public static class CorosLapGroup {
        private Integer type;          // -1=用户分段，2=1公里自动分段 等
        private Long lapDistance;      // 单段距离(厘米)，100000 = 1 公里
        private List<CorosLapItem> lapItemList;
    }

    @Data
    public static class CorosLapItem {
        private Integer lapIndex;
        private Long time;             // 厘秒(0.01秒)
        private Double distance;       // 厘米
        private Integer avgHr;
        private Integer maxHr;
        private Integer avgCadence;
        private Integer avgPace;       // 秒/公里（原值可能是小数，Jackson 截断）
        private Double elevGain;       // 米
        private Double totalDescent;   // 米
    }
}
