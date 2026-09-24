/** 用户运动目标 */
export interface UserGoal {
  id: number
  userId: number
  goalType: string
  targetValue: number
  currentValue: number
  unit: string
  deadline: string | null
  status: number
  createTime: string
}

/** AI 目标推荐 */
export interface AiGoalSuggestion {
  targetValue: number
  unit: string
  deadline: string | null
  /** 中文建议文案 2~4 句 */
  suggestion: string
  /** AI = 大模型生成，RULE = 本地规则 */
  source: 'AI' | 'RULE'
}
