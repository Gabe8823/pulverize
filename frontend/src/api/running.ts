import request from './index'
import type {
  RunningActivity,
  ActivityDetailVO,
  WeeklyStats,
  ActivityCreateForm,
  ActivityQuery,
} from '@/types/running'

interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export const runningApi = {
  /** 新增跑步记录 */
  createActivity: (data: ActivityCreateForm) =>
    request.post<any, RunningActivity>('/running/activities', data),

  /** 跑步记录列表 */
  listActivities: (params: ActivityQuery) =>
    request.get<any, PageResult<RunningActivity>>('/running/activities', { params }),

  /** 跑步详情 */
  getActivityDetail: (id: number) =>
    request.get<any, ActivityDetailVO>(`/running/activities/${id}`),

  /** 删除跑步记录 */
  deleteActivity: (id: number) =>
    request.delete<any, void>(`/running/activities/${id}`),

  /** 本周统计 */
  getWeeklyStats: () =>
    request.get<any, WeeklyStats>('/running/stats/weekly'),

  /** 全量统计 */
  getAllTimeStats: () =>
    request.get<any, WeeklyStats>('/running/stats/all'),
}
