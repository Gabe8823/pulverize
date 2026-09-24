/** AI 服务接入配置（与后端 AiConfigVO 对齐，密钥仅回传掩码） */
export interface AiConfig {
  /** 生效的接口地址 */
  baseUrl: string
  /** 生效的模型名 */
  model: string
  /** 是否已配置可用密钥 */
  keySet: boolean
  /** 密钥掩码，如 sk-a1****z9f2；未配置为 null */
  keyMasked: string | null
  /** 配置来源：DB=界面配置 YML=配置文件 NONE=未配置 */
  source: 'DB' | 'YML' | 'NONE'
}

/** 保存/测试入参（全部可选，空白字段保持原值） */
export interface AiConfigPayload {
  baseUrl?: string
  apiKey?: string
  model?: string
}

/** 连通性测试结果（POST /api/ai/test） */
export interface AiTestResult {
  /** 往返耗时（毫秒） */
  latencyMs: number
  /** 模型回复（截断） */
  reply: string
  /** 实际使用的模型名 */
  model: string
}
