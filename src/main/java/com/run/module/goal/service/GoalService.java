package com.run.module.goal.service;

import com.run.module.goal.dto.AiGoalSuggestionVO;
import com.run.module.goal.entity.UserGoal;

import java.util.List;

public interface GoalService {

    UserGoal createGoal(Long userId, UserGoal goal);

    List<UserGoal> listGoals(Long userId);

    void updateGoal(Long userId, Long goalId, UserGoal goal);

    void deleteGoal(Long userId, Long goalId);

    /** 根据跑步数据刷新所有目标进度 */
    List<UserGoal> refreshGoalProgress(Long userId);

    /**
     * 根据近 8 周跑步数据生成目标建议（规则基线 + AI 覆盖，绝不抛异常）
     *
     * @param userId   用户 ID
     * @param goalType 目标类型，空则按 WEEKLY_DISTANCE 处理
     */
    AiGoalSuggestionVO aiSuggest(Long userId, String goalType);
}
