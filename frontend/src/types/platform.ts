/** 运动平台连接状态 */
export interface PlatformStatus {
  platform: string
  name: string
  description: string
  available: boolean
  bound: boolean
  syncStatus: number
  message: string | null
  syncedCount: number
  lastSyncTime: string | null
}

/** 绑定请求 */
export interface PlatformBindForm {
  account: string
  password: string
  region: string
}

/** 同步状态 */
export interface SyncStatus {
  status: number
  message: string | null
  syncedCount: number | null
  lastSyncTime: string | null
  bound: boolean | null
}

/** 后端统一响应结构（fetch 原始请求使用，不经 request 拦截器解包） */
export interface McpR<T> {
  code: number
  msg: string
  data: T
}

/** POST /api/mcp/token 返回的令牌数据（完整 token 仅作局部变量用完即弃） */
export interface McpTokenData {
  token: string
  expiresAt: string
  expiresInDays: number
}

/** 令牌元信息（仅掩码，可安全写入 localStorage 并渲染到页面） */
export interface McpTokenMeta {
  masked: string
  expiresAt: string
  expiresInDays: number
}

/** MCP 工具展示项：只有中文短名与说明，绝不包含方法名（name） */
export interface McpToolItem {
  title: string
  detail: string
}
