package com.run.module.plan.service;

import com.run.module.plan.dto.GeneratePlanRequest;
import com.run.module.plan.dto.PlanDetailVO;
import com.run.module.plan.dto.PushWatchResultVO;
import com.run.module.plan.entity.TrainingPlan;
import com.run.module.plan.entity.TrainingPlanDetail;

import java.util.List;

public interface PlanService {

    /** 生成个性化训练计划 */
    GeneratePlanRequest.GeneratePlanResponse generatePlan(Long userId, GeneratePlanRequest request);

    /** 获取用户的训练计划列表 */
    List<TrainingPlan> listPlans(Long userId);

    /** 获取训练计划详情（含每日训练） */
    PlanDetailVO getPlanDetail(Long userId, Long planId);

    /** 更新每日训练状态 */
    void updateDayStatus(Long userId, Long detailId, Integer status);

    /** 删除训练计划 */
    void deletePlan(Long userId, Long planId);

    /** 推送训练计划到高驰手表（逐天串行排期），返回成功/失败天数与失败明细 */
    PushWatchResultVO pushToWatch(Long userId, Long planId);
}
