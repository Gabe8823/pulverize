import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginResponse } from '@/types/user'
import { userApi } from '@/api/user'
import { setToken, setRefreshToken, setUser, removeToken, getToken, getUser } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(getUser<UserInfo>())

  /** 登录 */
  async function login(username: string, password: string) {
    const res = await userApi.login({ username, password })
    handleLoginSuccess(res)
  }

  /** 注册 */
  async function register(username: string, password: string, nickname?: string) {
    const res = await userApi.register({ username, password, nickname })
    handleLoginSuccess(res)
  }

  function handleLoginSuccess(res: LoginResponse) {
    token.value = res.token
    setToken(res.token)
    setRefreshToken(res.refreshToken)
    // 立即拉取用户信息
    fetchProfile()
  }

  /** 拉取用户信息 */
  async function fetchProfile() {
    const res = await userApi.getProfile()
    userInfo.value = res
    setUser(res)
  }

  /** 登出 */
  function logout() {
    token.value = null
    userInfo.value = null
    removeToken()
  }

  return {
    token,
    userInfo,
    login,
    register,
    fetchProfile,
    logout,
  }
})
