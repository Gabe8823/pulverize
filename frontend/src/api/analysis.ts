import request from './index'
import type { AnalysisResultVO, MuscleMap, VdotResult } from '@/types/analysis'

export const analysisApi = {
  /** 重新分析跑步活动（POST） */
  analyzeActivity: (id: number) =>
    request.post<any, AnalysisResultVO>(`/analysis/activities/${id}`),

  /** 获取分析缓存（GET，无缓存时后端会实时计算） */
  getAnalysis: (id: number) =>
    request.get<any, AnalysisResultVO>(`/analysis/activities/${id}`),

  /** 跑力指数 VDOT（基于全部历史数据中的最优有氧表现推算） */
  getVdot: () => request.get<any, VdotResult>('/analysis/vdot'),

  /** 肌肉热力图：近 N 天肌群负荷估算（参考高驰 App） */
  getMuscleMap: (days: number) =>
    request.get<any, MuscleMap>('/analysis/muscle-map', { params: { days } }),
}
