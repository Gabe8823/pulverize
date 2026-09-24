import request from './index'
import type { LoginForm, RegisterForm, LoginResponse, UserInfo } from '@/types/user'

export const userApi = {
  /** 注册 */
  register: (data: RegisterForm) =>
    request.post<any, LoginResponse>('/auth/register', data),

  /** 登录 */
  login: (data: LoginForm) =>
    request.post<any, LoginResponse>('/auth/login', data),

  /** 获取当前用户信息 */
  getProfile: () =>
    request.get<any, UserInfo>('/user/profile'),

  /** 更新用户信息 */
  updateProfile: (data: Partial<UserInfo>) =>
    request.put<any, UserInfo>('/user/profile', data),

  /** 从高驰同步身体数据（身高 / 体重 / 最大心率 / 静息心率 / 生日，不同步性别） */
  syncCorosProfile: () =>
    request.post<any, UserInfo>('/profile/sync-coros'),
}
