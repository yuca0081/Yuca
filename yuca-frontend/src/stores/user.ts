import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentUser } from '@/api/user'
import type { User } from '@/types/api'

// 用户信息接口（匹配后端 UserResponse）
export interface UserInfo {
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

const TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_INFO_KEY = 'user_info'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>('')
  const refreshToken = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // 从 localStorage 初始化
  const initAuth = () => {
    const savedToken = localStorage.getItem(TOKEN_KEY)
    const savedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    const savedUserInfo = localStorage.getItem(USER_INFO_KEY)

    if (savedToken) {
      accessToken.value = savedToken
    }
    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken
    }
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
      } catch {
        userInfo.value = null
      }
    }
  }

  // 设置访问令牌
  function setAccessToken(token: string) {
    accessToken.value = token
    localStorage.setItem(TOKEN_KEY, token)
  }

  // 设置刷新令牌
  function setRefreshToken(token: string) {
    refreshToken.value = token
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  }

  // 同时设置两个令牌
  function setTokens(access: string, refresh: string) {
    setAccessToken(access)
    setRefreshToken(refresh)
  }

  // 设置用户信息
  function setUserInfo(user: UserInfo) {
    userInfo.value = user
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
  }

  // 登出
  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  // 检查是否已登录
  const isLoggedIn = () => {
    return !!accessToken.value
  }

  // 获取用于请求的 token（兼容旧代码）
  const token = () => accessToken.value

  // 获取用户信息（从服务器）
  const fetchUserInfo = async () => {
    const user = await getCurrentUser()
    setUserInfo(user)
    return user
  }

  // 设置用户信息（setUserInfo 的别名，用于 Profile.vue）
  const setUser = setUserInfo

  // 为了兼容 Profile.vue，暴露 user 作为 userInfo 的别名
  const user = userInfo

  // 初始化时从 localStorage 读取
  initAuth()

  return {
    accessToken,
    refreshToken,
    userInfo,
    user,
    token,
    setAccessToken,
    setRefreshToken,
    setTokens,
    setUserInfo,
    setUser,
    fetchUserInfo,
    clearAuth,
    isLoggedIn
  }
})
