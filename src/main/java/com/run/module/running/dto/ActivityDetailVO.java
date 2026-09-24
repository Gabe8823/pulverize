package com.run.module.running.dto;

import com.run.module.running.entity.RunningActivity;
import com.run.module.running.entity.RunningLap;
import lombok.Data;

import java.util.List;

@Data
public class ActivityDetailVO {

    private RunningActivity activity;
    private List<RunningLap> laps;

    /** 格式化后的便捷字段 */
    private String distanceText;      // "5.23 km"
    private String durationText;      // "32:15"
    private String paceText;          // "6'10""
    private String caloriesText;      // "320 kcal"
}
