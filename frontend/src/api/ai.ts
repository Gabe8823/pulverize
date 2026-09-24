import request from './index'
import type { AiConfig, AiConfigPayload, AiTestResult } from '@/types/ai'

export const aiApi = {
  /** 当前生效的 AI 接入配置（密钥仅掩码） */
  getConfig: () => request.get<any, AiConfig>('/ai/config'),

  /** 保存接入配置（apiKey 留空表示不修改），保存后立即生效 */
  saveConfig: (data: AiConfigPayload) => request.post<any, AiConfig>('/ai/config', data),

  /** 连通性测试：用填写值（空白取已保存配置）发起一次真实调用 */
  test: (data?: AiConfigPayload) => request.post<any, AiTestResult>('/ai/test', data ?? {}),
}
