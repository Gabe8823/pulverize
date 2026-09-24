package com.run.module.goal.controller;

import com.run.common.result.R;
import com.run.module.goal.dto.AiGoalSuggestionVO;
import com.run.module.goal.entity.UserGoal;
import com.run.module.goal.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /** 创建目标 */
    @PostMapping
    public R<UserGoal> createGoal(HttpServletRequest request, @RequestBody UserGoal goal) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok("创建成功", goalService.createGoal(userId, goal));
    }

    /** 获取所有目标 */
    @GetMapping("/list")
    public R<List<UserGoal>> listGoals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(goalService.listGoals(userId));
    }

    /** 更新目标 */
    @PutMapping("/{id}")
    public R<Void> updateGoal(HttpServletRequest request,
                                @PathVariable Long id, @RequestBody UserGoal goal) {
        Long userId = (Long) request.getAttribute("userId");
        goalService.updateGoal(userId, id, goal);
        return R.ok("更新成功", null);
    }

    /** 删除目标 */
    @DeleteMapping("/{id}")
    public R<Void> deleteGoal(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        goalService.deleteGoal(userId, id);
        return R.ok("删除成功", null);
    }

    /** 刷新目标进度（根据跑步数据自动更新） */
    @PostMapping("/refresh")
    public R<List<UserGoal>> refreshGoals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok("进度已更新", goalService.refreshGoalProgress(userId));
    }

    /** AI 目标推荐：规则基线 + 大模型覆盖，body 如 {"goalType":"WEEKLY_DISTANCE"}（goalType 缺省 WEEKLY_DISTANCE） */
    @PostMapping("/ai-suggest")
    public R<AiGoalSuggestionVO> aiSuggest(HttpServletRequest request,
                                            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Object rawType = body == null ? null : body.get("goalType");
        String goalType = rawType == null ? null : String.valueOf(rawType);
        return R.ok(goalService.aiSuggest(userId, goalType));
    }
}
