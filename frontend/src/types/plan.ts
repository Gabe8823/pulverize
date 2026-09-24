/** 训练计划 */
export interface TrainingPlan {
  id: number
  userId: number
  planName: string
  goalType: string
  goalValue: string
  startDate: string
  endDate: string
  status: number
  aiReasoning: string
  createTime: string
}

/** 训练计划每日详情（数据库记录，含 id 与完成状态） */
export interface TrainingPlanDetail {
  id: number
  planId: number
  planDate: string
  dayOfWeek: number
  workoutType: string
  targetDistanceKm: number | null
  targetDurationMin: number | null
  targetPaceSecKm: number | null
  targetHrZone: string | null
  workoutDescription: string
  status: number
}

/** 生成计划响应中的每日安排（尚未入库，无 id/status） */
export interface DailyPlanItem {
  planDate: string
  dayOfWeek: number
  workoutType: string
  targetDistanceKm: number | null
  targetDurationMin: number | null
  targetPaceSecKm: number | null
  targetHrZone: string | null
  workoutDescription: string
}

/** 计划详情VO */
export interface PlanDetailVO {
  plan: TrainingPlan
  dailyDetails: TrainingPlanDetail[]
}

/** 职业运动员训练风格：'' 通用 / KIPCHOGE / OSAKO / HASAN */
export type AthleteStyle = '' | 'KIPCHOGE' | 'OSAKO' | 'HASAN'

/** 生成计划请求 */
export interface GeneratePlanRequest {
  goalType: string
  goalValue?: string
  startDate: string
  endDate: string
  weeklyTrainingDays?: number
  trainingDurationMinutes?: number
  /** 参考的职业运动员训练风格，空字符串表示通用科学训练 */
  athleteStyle?: AthleteStyle | string
}

export interface GeneratePlanResponse {
  planId: number
  planName: string
  aiReasoning: string
  dailyPlan: DailyPlanItem[]
}
