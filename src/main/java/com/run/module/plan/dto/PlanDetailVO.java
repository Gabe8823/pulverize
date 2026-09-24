package com.run.module.plan.dto;

import com.run.module.plan.entity.TrainingPlan;
import com.run.module.plan.entity.TrainingPlanDetail;
import lombok.Data;

import java.util.List;

@Data
public class PlanDetailVO {

    private TrainingPlan plan;
    private List<TrainingPlanDetail> dailyDetails;
}
