import axios from 'axios'
import { toast } from '@/utils/toast'
import { getToken, removeToken } from '@/utils/storage'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器（统一 R 结构：{ code, msg, data }）
request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data ?? {}
    if (code === 200) {
      return data
    }
    toast.error(msg || '请求失败')
    if (code === 401) {
      removeToken()
      router.push('/login')
    }
    return Promise.reject(new Error(msg))
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      router.push('/login')
    }
    const msg = error.response?.data?.msg || error.message || '网络错误'
    toast.error(msg)
    return Promise.reject(error)
  }
)

export default request
