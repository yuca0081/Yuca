import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'

// 后端错误码 → 中文提示
const errorMessages: Record<number, string> = {
  400: '请求参数错误',
  401: '未登录或登录已过期',
  403: '没有访问权限',
  404: '请求的资源不存在',
  500: '服务器内部错误',
  1000: '请求参数错误',
  1001: '系统错误，请稍后重试',
  2001: '用户不存在',
  2002: '用户名已存在',
  2003: '邮箱已被注册',
  2004: '手机号已被注册',
  2005: '账号或密码错误',
  2006: '密码格式不正确',
  2007: '账号已被禁用',
  2008: '账号已被锁定',
  2009: '登录失败次数过多，账号已锁定',
  3001: '登录凭证无效，请重新登录',
  3002: '登录已过期，请重新登录',
  3003: '刷新凭证无效',
  3004: '刷新凭证已过期',
  3005: '刷新凭证已撤销',
  4001: '验证码无效',
  4002: '验证码已过期',
  4003: '验证码已使用',
  4004: '发送验证码过于频繁',
}

function getErrorMessage(code?: number, fallback?: string): string {
  // 优先根据后端消息内容匹配中文
  if (fallback) {
    const lower = fallback.toLowerCase()
    if (lower.includes('account not found')) return '账号不存在'
    if (lower.includes('incorrect password')) return '密码错误'
    if (lower.includes('invalid account') || lower.includes('invalid account or password')) return '账号或密码错误'
    if (lower.includes('account is locked') || lower.includes('locked')) return '账号已被锁定，请稍后再试'
    if (lower.includes('user not found')) return '用户不存在'
    if (lower.includes('already exists')) return '该账号已被注册'
    if (lower.includes('disabled')) return '账号已被禁用'
  }
  // 再根据错误码兜底
  if (code && errorMessages[code]) return errorMessages[code]
  return fallback || '网络错误，请稍后重试'
}

type RequestInstance = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch'> & {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
}) as RequestInstance

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const { code, data, message } = response.data
    if (code === 200) {
      return data
    }
    // Business-level auth errors — redirect to login
    if (code === 401 || code === 3001 || code === 3002) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('user_info')
      window.location.href = '/login'
      return Promise.reject(new Error(getErrorMessage(code, message)))
    }
    return Promise.reject(new Error(getErrorMessage(code, message)))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('user_info')
      window.location.href = '/login'
    }
    const code = error.response?.data?.code
    const msg = error.response?.data?.message
    return Promise.reject(new Error(getErrorMessage(code, msg)))
  }
)

export default request
