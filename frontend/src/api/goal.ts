import request from './index'
import type { UserGoal, AiGoalSuggestion } from '@/types/goal'

export const goalApi = {
  /** 创建目标 */
  createGoal: (data: Partial<UserGoal>) =>
    request.post<any, UserGoal>('/goal', data),

  /** 目标列表 */
  listGoals: () =>
    request.get<any, UserGoal[]>('/goal/list'),

  /** AI 推荐目标值（结合近期跑量/最佳成绩，给出建议目标与理由） */
  aiSuggest: (goalType: string) =>
    request.post<any, AiGoalSuggestion>('/goal/ai-suggest', { goalType }),

  /** 更新目标 */
  updateGoal: (id: number, data: Partial<UserGoal>) =>
    request.put<any, void>(`/goal/${id}`, data),

  /** 删除目标 */
  deleteGoal: (id: number) =>
    request.delete<any, void>(`/goal/${id}`),

  /** 刷新进度 */
  refreshGoals: () =>
    request.post<any, UserGoal[]>('/goal/refresh'),
}
