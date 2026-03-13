import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { useUserStore } from '@/stores/user'

// 创建自定义请求类型（响应拦截器会提取 data）
type RequestInstance = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch'> & {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
}

// 创建 Axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000, // 文档上传可能需要较长时间（向量生成），增加到60秒
  headers: {
    'Content-Type': 'application/json'
  }
}) as RequestInstance

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 添加调试日志
    console.log('🚀 发送请求:', config.method?.toUpperCase(), config.url)

    // 从 localStorage 获取 access token
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 添加调试日志
    console.log('✅ 收到响应:', response.config.url, response.data)

    const { code, data, message } = response.data

    // 后端 ApiResponse 结构: { code: number, data: any, message: string }
    if (code === 200) {
      return data
    } else {
      // 这里可以使用 Naive UI 的消息提示
      console.error('请求失败:', message)
      return Promise.reject(new Error(message || '请求失败'))
    }
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      const { status } = error.response

      if (status === 401) {
        // Token 过期或无效，清除所有认证信息并跳转登录
        const userStore = useUserStore()
        userStore.clearAuth()
        window.location.href = '/login'
      } else if (status === 403) {
        console.error('没有权限访问')
      } else if (status === 404) {
        console.error('请求的资源不存在')
      } else if (status === 500) {
        console.error('服务器错误')
      }
    }

    return Promise.reject(error)
  }
)

export default request
