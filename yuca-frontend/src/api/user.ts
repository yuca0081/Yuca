import request from './index'
import axios from 'axios'
import type {
  LoginRequest,
  LoginResponse,
  User,
  RefreshTokenRequest,
  TokenInfo,
  RegisterRequest,
  UpdateProfileRequest
} from '@/types/api'

/**
 * 用户登录
 * POST /user/login
 */
export const login = (data: LoginRequest) => {
  return request.post<LoginResponse>('/user/login', data)
}

/**
 * 获取当前用户信息
 * GET /user/current
 */
export const getCurrentUser = () => {
  return request.get<User>('/user/current')
}

/**
 * 刷新令牌
 * POST /user/refresh-token
 */
export const refreshToken = (data: RefreshTokenRequest) => {
  return request.post<TokenInfo>('/user/refresh-token', data)
}

/**
 * 用户注册
 * POST /user/register
 */
export const register = (data: RegisterRequest) => {
  return request.post<User>('/user/register', data)
}

/**
 * 退出登录
 * POST /user/logout
 * 需要在请求头中传递 X-Refresh-Token
 */
export const logout = (refreshToken?: string) => {
  const headers: Record<string, string> = {}
  if (refreshToken) {
    headers['X-Refresh-Token'] = refreshToken
  }
  return request.post<void>('/user/logout', {}, { headers })
}

/**
 * 上传用户头像
 * POST /user/avatar
 */
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取用户头像
 * GET /user/avatar/{userId}
 * 返回 Blob 类型，可以直接转换为 base64
 */
export const getUserAvatar = (userId: number) => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('access_token')

  return axios.get(`${baseURL}/user/avatar/${userId}`, {
    responseType: 'blob',
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  }).then(response => response.data)
}

/**
 * 更新个人资料
 * PUT /user/profile
 */
export const updateProfile = (data: UpdateProfileRequest) => {
  return request.put<User>('/user/profile', data)
}
