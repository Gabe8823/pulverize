/** 报告统计粒度：天/周/月/季度/年/自定义区间 */
export type ReportGranularity = 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR' | 'CUSTOM'

/** 报告汇总 */
export interface ReportSummary {
  runs: number
  distanceKm: number
  durationMin: number
  avgPaceSecKm: number | null
  avgHeartRate: number | null
  calories: number | null
  elevationGainM: number
}
/** 报告分桶（趋势数据点） */
export interface ReportBucket {
  label: string
  runs: number
  distanceKm: number
  durationMin: number
  avgPaceSecKm: number | null
  avgHeartRate: number | null
}

/** 训练报告 */
export interface TrainingReport {
  granularity: ReportGranularity
  startDate: string
  endDate: string
  periodLabel: string
  summary: ReportSummary
  buckets: ReportBucket[]
  aiReport: string
  aiGenerated: boolean
  /** 后端 AI 通道是否可用（未配置密钥时为 false，此时为规则兜底文案） */
  aiAvailable: boolean
}

/** 报告生成请求 */
export interface ReportQuery {
  granularity: ReportGranularity
  startDate?: string
  endDate?: string
}
