/**
 * 分析维度数据：内层 Map 的 key 均为中文（如 "配速稳定性"、"心率区间分布"、"疲劳程度"），
 * value 可能是标量（数字/中文字符串），也可能是嵌套对象（如 "配速区间分布"）。
 */
export type AnalysisMap = Record<string, unknown>

/** 分析结果（与后端 AnalysisResultVO 对齐） */
export interface AnalysisResultVO {
  activityId: number
  activityName: string
  /** 综合评分（0-100） */
  score: number | null
  /** AI 教练点评（AI 不可用时为规则兜底文案，始终有值） */
  aiSummary: string | null
  /** 配速分析 */
  paceAnalysis: AnalysisMap | null
  /** 心率分析 */
  heartRateAnalysis: AnalysisMap | null
  /** 步频分析 */
  cadenceAnalysis: AnalysisMap | null
  /** 疲劳度分析 */
  fatigueAnalysis: AnalysisMap | null
  /** 训练效果评估 */
  trainingEffect: AnalysisMap | null
}

/** VDOT 参考的最优表现（source=RACE 时有值，取自全部历史数据） */
export interface VdotRaceInfo {
  activityId: number
  activityName: string
  raceDate: string
  distanceKm: number
  durationSec: number
  paceSecKm: number
}

/** 训练配速区间（基于 VDOT 推算，Daniels 五区间） */
export interface VdotZone {
  /** 区间名：轻松跑 / 马拉松配速 / 乳酸阈配速 / 间歇配速 / 重复跑配速 */
  name: string
  /** 强度百分比标签，如 "59–74%" */
  percentLabel: string
  /** 区间慢端配速（秒/公里，数值更大） */
  slowPaceSecKm: number
  /** 区间快端配速（秒/公里，数值更小） */
  fastPaceSecKm: number
}

/** 跑力指数结果（GET /api/analysis/vdot） */
export interface VdotResult {
  /** VDOT 值，source=NONE 时为 null */
  vdot: number | null
  /** RACE=已基于全部历史数据的最优表现推算，NONE=数据不足 */
  source: 'RACE' | 'NONE'
  /** 提示文案（说明推算依据或数据不足原因） */
  message: string | null
  race: VdotRaceInfo | null
  /** 等效完赛时间（秒），key 为 "5K" | "10K" | "半马" | "全马" */
  equivalents: Record<string, number> | null
  zones: VdotZone[] | null
}

/** 肌群负荷条目（GET /api/analysis/muscle-map） */
export interface MuscleItem {
  /** 肌群 key：chest / shoulders / biceps / core / quads / tibialis / lats / glutes / hamstrings / calves */
  key: string
  /** 中文名 */
  name: string
  /** 人体视图：FRONT 正面 / BACK 背面 */
  view: string
  /** 负荷分数 0-100 */
  score: number
  /** 等级：HIGH / MEDIUM / LOW / IDLE */
  level: string
  /** 等级中文：高负荷 / 中等 / 轻度 / 未激活 */
  levelText: string
}

/** 肌肉热力图（GET /api/analysis/muscle-map?days=N） */
export interface MuscleMap {
  days: number
  activityCount: number
  totalMinutes: number
  /** 按分数降序 */
  muscles: MuscleItem[]
}
