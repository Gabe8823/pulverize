/** 统一返回体 */
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 登录/注册请求 */
export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  nickname?: string
}

/** 登录响应 */
export interface LoginResponse {
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
  token: string
  refreshToken: string
}

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string | null
  phone: string | null
  avatarUrl: string | null
  gender: number
  birthday: string | null
  heightCm: number | null
  weightKg: number | null
  maxHeartRate: number | null
  restHeartRate: number | null
}
