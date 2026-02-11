// API 响应类型定义

// 对应后端 ApiResponse 结构
export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

// 用户相关类型（匹配后端 UserResponse）
export interface User {
  id: number
  username: string
  email: string
  phone?: string
  nickname?: string
  avatarUrl?: string
  status: number
  createTime: string
  lastLoginTime?: string
}

// 登录请求（匹配后端 LoginRequest）
export interface LoginRequest {
  account: string      // 支持用户名、邮箱、手机号
  password: string
  rememberMe?: boolean
}

// Token 信息（匹配后端 TokenResponse）
export interface TokenInfo {
  accessToken: string
  refreshToken: string
  tokenType: string    // "Bearer"
  expiresIn: number    // 秒数
}

// 登录响应（匹配后端 LoginResponse）
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

// 刷新令牌请求
export interface RefreshTokenRequest {
  refreshToken: string
}

// 获取当前用户响应
export interface UserResponse {
  id: number
  username: string
  email: string
  phone?: string
  nickname?: string
  avatarUrl?: string
  status: number
  createTime: string
  lastLoginTime?: string
}

// 注册请求（匹配后端 RegisterRequest）
export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

// 更新个人资料请求（匹配后端 UpdateProfileRequest）
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  phone?: string
  avatarUrl?: string
}
