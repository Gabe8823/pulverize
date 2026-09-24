import request from './index'
import type { ReportQuery, TrainingReport } from '@/types/report'

export const reportApi = {
  /** 生成训练报告（统计 + AI 文字报告） */
  generateReport: (data: ReportQuery) =>
    request.post<any, TrainingReport>('/report/generate', data),
}
