// API Response
export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

// User
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

export interface LoginRequest {
  account: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  phone?: string
  avatarUrl?: string
}

// Pagination
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
