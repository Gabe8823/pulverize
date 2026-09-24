import request from './index'
import type { PlatformStatus, PlatformBindForm, SyncStatus } from '@/types/platform'

export const platformApi = {
  /** 所有平台的连接状态 */
  listPlatforms: () => request.get<any, PlatformStatus[]>('/platform/list'),

  /** 绑定平台账号 */
  bind: (platform: string, data: PlatformBindForm) =>
    request.post<any, SyncStatus>(`/platform/${platform}/bind`, data),

  /** 同步平台数据（后端会回填历史详情，可能耗时数分钟，放宽超时） */
  sync: (platform: string) =>
    request.post<any, SyncStatus>(`/platform/${platform}/sync`, null, { timeout: 600000 }),

  /** 查询平台同步状态 */
  status: (platform: string) =>
    request.get<any, SyncStatus>(`/platform/${platform}/status`),

  /** 解绑平台 */
  unbind: (platform: string) =>
    request.delete<any, void>(`/platform/${platform}/unbind`),
}
