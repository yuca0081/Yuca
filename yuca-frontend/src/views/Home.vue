<template>
  <div class="home-container">
    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧：个人信息 + 时间天气 -->
      <div class="left-section">
        <!-- 个人信息卡片 -->
        <div class="card profile-card">
          <!-- 右上角操作按钮 -->
          <div v-if="!loading && userInfo" class="card-actions">
            <button class="icon-btn" @click="handleEditProfile" title="编辑资料">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <button class="icon-btn" @click="handleLogout" title="退出登录">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
            </button>
          </div>

          <div v-if="loading" class="loading-state">
            <p>加载中...</p>
          </div>
          <div v-else-if="userInfo" class="profile-content">
            <div class="avatar-container">
              <img :src="getFullAvatarUrl(userInfo.avatarUrl, userInfo.id) || '/avatar.jpg'" alt="Avatar" class="avatar" />
            </div>
            <h1 class="username">{{ userInfo.nickname || userInfo.username }}</h1>
            <p class="email">{{ userInfo.email }}</p>

            <!-- 社交链接 -->
            <div class="social-links">
              <a href="https://github.com" target="_blank" class="social-link" title="GitHub">
                <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
              </a>
              <a :href="`mailto:${userInfo.email}`" class="social-link" title="Email">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
              </a>
            </div>
          </div>
        </div>

        <!-- 时间天气卡片 -->
        <div class="card time-card" @click="showLocationSelector = true">
          <div class="time-content">
            <p class="date-text">{{ currentDate }} {{ weekday }}</p>
            <p class="time-text">{{ currentTime }}</p>
            <p class="location-text">{{ weatherText }}</p>
            <p class="location-hint">点击切换地区</p>
          </div>
        </div>

        <!-- 地区选择器弹窗 -->
        <div v-if="showLocationSelector" class="location-selector-overlay" @click="showLocationSelector = false">
          <div class="location-selector" @click.stop>
            <h3 class="selector-title">选择地区</h3>
            <div class="location-list">
              <div
                v-for="location in locations"
                :key="location.name"
                class="location-item"
                :class="{ active: location.name === selectedLocation.name }"
                @click="selectLocation(location)"
              >
                <div class="location-name">{{ location.name }}</div>
                <div class="location-info">
                  {{ location.weather }} {{ location.temperature }}°C
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：功能卡片 -->
      <div class="right-section">
        <div class="function-cards">
          <div v-for="item in functionItems" :key="item.name" class="function-card" @click="navigateTo(item.path)">
            <n-icon :component="item.icon" class="function-icon" size="32" color="#1f2937" />
            <span class="function-name">{{ item.name }}</span>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon } from 'naive-ui'
import { useTime } from '@/composables/useTime'
import { useUserStore } from '@/stores/user'
import { getCurrentUser, getUserAvatar } from '@/api/user'
import type { User } from '@/types/api'
import {
  ConstructOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  ChatbubbleEllipsesOutline,
  BookOutline,
  PersonOutline,
  RestaurantOutline
} from '@vicons/ionicons5'

const router = useRouter()
const { currentTime, currentDate, weekday } = useTime()
const userStore = useUserStore()

// 获取完整的头像URL
const getFullAvatarUrl = (url: string | undefined, userId?: number): string => {
  // 如果有 userId，使用专门的头像接口
  if (userId) {
    return `/api/user/avatar/${userId}`
  }

  // 如果已经是完整URL，直接返回
  if (url && (url.startsWith('http://') || url.startsWith('https://'))) {
    return url
  }

  // 如果是相对路径（如 user/avatar/1/xxx.jpeg），添加 /api 前缀通过代理访问
  if (url) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
    return `${baseUrl}/${url.startsWith('/') ? url.slice(1) : url}`
  }

  return ''
}

const userInfo = ref<User | null>(null)
const loading = ref(true)

// 地区相关
const LOCATIONS_KEY = 'selected_location'

interface Location {
  name: string
  weather: string
  temperature: number
  wind: string
}

const locations: Location[] = [
  { name: '北京', weather: '晴', temperature: 8, wind: '西北风2级' },
  { name: '上海', weather: '多云', temperature: 12, wind: '东南风1级' },
  { name: '广州', weather: '阴', temperature: 18, wind: '东风2级' },
  { name: '深圳', weather: '多云', temperature: 20, wind: '南风1级' },
  { name: '杭州', weather: '小雨', temperature: 11, wind: '东北风2级' },
  { name: '南京', weather: '晴', temperature: 9, wind: '东风1级' },
  { name: '武汉', weather: '多云', temperature: 10, wind: '西南风1级' },
  { name: '成都', weather: '阴', temperature: 13, wind: '东风2级' }
]

const selectedLocation = ref<Location>(locations[0])
const showLocationSelector = ref(false)

// 从 localStorage 加载保存的地区
const loadSavedLocation = () => {
  const savedName = localStorage.getItem(LOCATIONS_KEY)
  if (savedName) {
    const found = locations.find(loc => loc.name === savedName)
    if (found) {
      selectedLocation.value = found
    }
  }
}

// 选择地区
const selectLocation = (location: Location) => {
  selectedLocation.value = location
  localStorage.setItem(LOCATIONS_KEY, location.name)
  showLocationSelector.value = false
}

// 天气显示文本
const weatherText = computed(() => {
  return `${selectedLocation.value.name} ${selectedLocation.value.weather} ${selectedLocation.value.temperature}°C ${selectedLocation.value.wind}`
})

// 功能卡片数据
interface FunctionItem {
  name: string
  path: string
  icon: any
}

const functionItems: FunctionItem[] = [
  {
    name: '工具',
    path: '/tools',
    icon: ConstructOutline
  },
  {
    name: '笔记',
    path: '/notes',
    icon: DocumentTextOutline
  },
  {
    name: '知识库',
    path: '/wiki',
    icon: FolderOpenOutline
  },
  {
    name: '小助手',
    path: '/assistant',
    icon: ChatbubbleEllipsesOutline
  },
  {
    name: '测试',
    path: '/blog',
    icon: BookOutline
  },
  {
    name: '饮食',
    path: '/diet/record',
    icon: RestaurantOutline
  },
  {
    name: '个人资料',
    path: '/profile',
    icon: PersonOutline
  }
]

// 加载用户信息
const loadUserInfo = async () => {
  try {
    loading.value = true
    const data = await getCurrentUser()
    userInfo.value = data
    userStore.setUserInfo(data)

    // 获取并缓存用户头像
    await loadAndCacheAvatar(data.id)
  } catch (error) {
    console.error('加载用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取并缓存用户头像
const loadAndCacheAvatar = async (userId: number) => {
  try {
    console.log('开始获取用户头像，userId:', userId)
    const blob = await getUserAvatar(userId)
    console.log('头像 Blob 对象:', blob)

    // 将 Blob 转换为 base64
    const reader = new FileReader()
    reader.onloadend = () => {
      const base64data = reader.result as string
      console.log('头像 base64 长度:', base64data.length)

      // 缓存到 localStorage
      localStorage.setItem('user_avatar', base64data)
      console.log('头像已缓存到 localStorage')
    }
    reader.readAsDataURL(blob)
  } catch (error) {
    console.error('获取用户头像失败:', error)
    // 失败时清除旧的头像缓存
    localStorage.removeItem('user_avatar')
  }
}

// 导航到指定页面
const navigateTo = (path: string) => {
  if (path === router.currentRoute.value.path) return
  router.push(path)
}

// 编辑资料
const handleEditProfile = () => {
  router.push('/profile')
}

// 退出登录
const handleLogout = () => {
  userStore.clearAuth()
  router.push('/login')
}

// 组件挂载时加载用户信息和地区设置
onMounted(async () => {
  // 加载保存的地区
  loadSavedLocation()

  // 如果 store 中有用户信息，直接使用
  if (userStore.userInfo) {
    userInfo.value = userStore.userInfo
    loading.value = false

    // 即使有缓存的用户信息，也要获取并缓存头像
    if (userInfo.value.id) {
      await loadAndCacheAvatar(userInfo.value.id)
    }
  } else {
    loadUserInfo()
  }
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
}

.main-content {
  width: fit-content;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 32px;
  align-items: center;
}

/* 左侧区域 */
.left-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  position: relative;
  padding: 24px;
  background: rgba(245, 245, 245, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

.loading-state {
  text-align: center;
  padding: 20px;
  color: var(--color-text-primary);
}

.profile-content {
  text-align: center;
}

.avatar-container {
  margin-bottom: 16px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid rgba(0, 0, 0, 0.1);
  object-fit: cover;
}

.username {
  font-size: 18px;
  font-weight: bold;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.email {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.social-links {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-link {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: var(--color-text-primary);
  text-decoration: none;
  transition: all 0.3s ease;
}

.social-link:hover {
  background: rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.social-link .icon {
  width: 16px;
  height: 16px;
}

/* 右上角操作按钮 */
.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn svg {
  width: 18px;
  height: 18px;
  color: var(--color-text-primary);
  opacity: 0.6;
  transition: opacity 0.2s ease;
}

.icon-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.icon-btn:hover svg {
  opacity: 1;
}

.icon-btn:active {
  transform: scale(0.95);
}

/* 时间天气卡片 */
.time-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(245, 245, 245, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

.time-card:hover {
  transform: translateY(-2px);
  background: rgba(245, 245, 245, 0.95);
  box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
}

.time-header {
  margin-bottom: 12px;
}

.card-icon {
  width: 20px;
  height: 20px;
  color: var(--color-text-primary);
}

.time-text {
  font-size: 32px;
  font-weight: bold;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.date-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.location-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.location-hint {
  font-size: 10px;
  color: var(--color-text-secondary);
  opacity: 0.7;
}

/* 地区选择器弹窗 */
.location-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.location-selector {
  background: rgba(245, 245, 245, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md);
  padding: 24px;
  max-width: 400px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: var(--shadow-lg);
}

.selector-title {
  font-size: 18px;
  font-weight: bold;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.location-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.location-item {
  padding: 12px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(245, 245, 245, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.location-item:hover {
  background: rgba(245, 245, 245, 0.9);
  transform: translateY(-2px);
}

.location-item.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.location-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.location-info {
  font-size: 12px;
  opacity: 0.8;
}

/* 右侧功能卡片 */
.right-section {
  display: flex;
  align-items: center;
}

.function-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.function-card {
  background: rgba(245, 245, 245, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 20px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.3s ease;
  aspect-ratio: 1;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

.function-card:hover {
  transform: translateY(-4px);
  background: rgba(245, 245, 245, 0.95);
  box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
}

.function-icon {
  outline: none;
}

.function-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .function-cards {
    grid-template-columns: repeat(3, 1fr);
  }

  .time-text {
    font-size: 28px;
  }
}

@media (max-width: 480px) {
  .function-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
