import request from './index'
import type { TrainingPlan, PlanDetailVO, GeneratePlanRequest, GeneratePlanResponse } from '@/types/plan'

export const planApi = {
  /** 生成训练计划 */
  generatePlan: (data: GeneratePlanRequest) =>
    request.post<any, GeneratePlanResponse>('/plan/generate', data),

  /** 训练计划列表 */
  listPlans: () =>
    request.get<any, TrainingPlan[]>('/plan/list'),

  /** 训练计划详情 */
  getPlanDetail: (id: number) =>
    request.get<any, PlanDetailVO>(`/plan/${id}`),

  /** 更新每日训练状态 */
  updateDayStatus: (id: number, status: number) =>
    request.put<any, void>(`/plan/detail/${id}/status?status=${status}`),

  /** 删除训练计划 */
  deletePlan: (id: number) =>
    request.delete<any, void>(`/plan/${id}`),

  /** 推送计划到手表（高驰训练日历） */
  pushToWatch: (id: number) =>
    request.post<any, { pushed: number; failed: number; errors: string[] }>(
      `/plan/${id}/push-watch`
    ),
}
