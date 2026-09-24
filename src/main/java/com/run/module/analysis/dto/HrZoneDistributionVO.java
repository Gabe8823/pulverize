package com.run.module.analysis.dto;

import lombok.Data;

/** 心率区间分布（口径：储备心率 HRR；已同步 COROS 心率区间的活动优先用 hr_zone_json 官方 percent） */
@Data
public class HrZoneDistributionVO {

    private Integer z1Count;   // <60% 储备心率
    private Integer z2Count;   // 60-74%
    private Integer z3Count;   // 74-84%
    private Integer z4Count;   // 84-95%
    private Integer z5Count;   // >=95%

    private Integer z1Percentage;
    private Integer z2Percentage;
    private Integer z3Percentage;
    private Integer z4Percentage;
    private Integer z5Percentage;

    private String dominantZone;
    private String aerobicPercentage;
}
