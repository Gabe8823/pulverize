package com.run.module.plan.controller;

import com.run.common.result.R;
import com.run.module.plan.dto.GeneratePlanRequest;
import com.run.module.plan.dto.PlanDetailVO;
import com.run.module.plan.dto.PushWatchResultVO;
import com.run.module.plan.entity.TrainingPlan;
import com.run.module.plan.service.PlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /** 生成训练计划 */
    @PostMapping("/generate")
    public R<GeneratePlanRequest.GeneratePlanResponse> generatePlan(
            HttpServletRequest request, @Valid @RequestBody GeneratePlanRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        GeneratePlanRequest.GeneratePlanResponse resp = planService.generatePlan(userId, req);
        return R.ok("计划生成成功", resp);
    }

    /** 训练计划列表 */
    @GetMapping("/list")
    public R<List<TrainingPlan>> listPlans(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(planService.listPlans(userId));
    }

    /** 训练计划详情 */
    @GetMapping("/{id}")
    public R<PlanDetailVO> getPlanDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(planService.getPlanDetail(userId, id));
    }

    /** 更新每日训练状态 */
    @PutMapping("/detail/{id}/status")
    public R<Void> updateDayStatus(HttpServletRequest request,
                                    @PathVariable Long id, @RequestParam Integer status) {
        Long userId = (Long) request.getAttribute("userId");
        planService.updateDayStatus(userId, id, status);
        return R.ok("更新成功", null);
    }

    /** 删除训练计划 */
    @DeleteMapping("/{id}")
    public R<Void> deletePlan(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        planService.deletePlan(userId, id);
        return R.ok("删除成功", null);
    }

    /** 推送训练计划到高驰手表（逐天串行排期，REST 日跳过） */
    @PostMapping("/{id}/push-watch")
    public R<PushWatchResultVO> pushWatch(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok("推送完成", planService.pushToWatch(userId, id));
    }
}
