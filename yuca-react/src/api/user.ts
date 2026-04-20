import request from './index'
import type { LoginRequest, LoginResponse, User, RefreshTokenRequest, UpdateProfileRequest, ResetPasswordRequest } from '@/types'

export const login = (data: LoginRequest) =>
  request.post<LoginResponse>('/user/login', data)

export const getCurrentUser = () =>
  request.get<User>('/user/current')

export const refreshToken = (data: RefreshTokenRequest) =>
  request.post('/user/refresh-token', data)

export const register = (data: { username: string; password: string; email?: string; nickname?: string }) =>
  request.post<User>('/user/register', data)

export const logout = (refreshToken?: string) => {
  const headers: Record<string, string> = {}
  if (refreshToken) headers['X-Refresh-Token'] = refreshToken
  return request.post<void>('/user/logout', {}, { headers })
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateProfile = (data: UpdateProfileRequest) =>
  request.put<User>('/user/profile', data)

export const resetPassword = (data: ResetPasswordRequest) =>
  request.post<void>('/user/reset-password', data)
