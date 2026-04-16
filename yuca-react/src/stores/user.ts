import { create } from 'zustand'
import { getCurrentUser } from '@/api/user'
import type { User } from '@/types'

interface UserInfo extends User {}

interface UserState {
  accessToken: string
  refreshToken: string
  userInfo: UserInfo | null
  setAccessToken: (token: string) => void
  setRefreshToken: (token: string) => void
  setTokens: (access: string, refresh: string) => void
  setUserInfo: (user: UserInfo) => void
  fetchUserInfo: () => Promise<UserInfo>
  clearAuth: () => void
  isLoggedIn: () => boolean
  initAuth: () => void
}

const TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_INFO_KEY = 'user_info'

export const useUserStore = create<UserState>((set, get) => ({
  accessToken: localStorage.getItem(TOKEN_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) || '',
  userInfo: (() => {
    try {
      const saved = localStorage.getItem(USER_INFO_KEY)
      return saved ? JSON.parse(saved) : null
    } catch { return null }
  })(),

  setAccessToken: (token) => {
    localStorage.setItem(TOKEN_KEY, token)
    set({ accessToken: token })
  },

  setRefreshToken: (token) => {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
    set({ refreshToken: token })
  },

  setTokens: (access, refresh) => {
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
    set({ accessToken: access, refreshToken: refresh })
  },

  setUserInfo: (user) => {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
    set({ userInfo: user })
  },

  fetchUserInfo: async () => {
    const user = await getCurrentUser()
    get().setUserInfo(user)
    return user
  },

  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
    set({ accessToken: '', refreshToken: '', userInfo: null })
  },

  isLoggedIn: () => !!get().accessToken,

  initAuth: () => {
    const token = localStorage.getItem(TOKEN_KEY)
    const refresh = localStorage.getItem(REFRESH_TOKEN_KEY)
    let userInfo: UserInfo | null = null
    try {
      const saved = localStorage.getItem(USER_INFO_KEY)
      if (saved) userInfo = JSON.parse(saved)
    } catch { /* ignore */ }
    set({
      accessToken: token || '',
      refreshToken: refresh || '',
      userInfo,
    })
  },
}))
