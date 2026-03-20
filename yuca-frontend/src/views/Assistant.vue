<template>
  <!-- 第1层：页面容器（暗色遮罩背景） -->
  <div class="assistant-page-container">
    <!-- 返回主页按钮 -->
    <n-button class="back-home-btn" @click="goBackHome">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="18" />
      </template>
      <span>返回主页</span>
    </n-button>

    <!-- 第2层：页面主体容器（毛玻璃卡片） -->
    <div class="assistant-main-card">
      <!-- 左侧：会话侧栏 (可调整宽度，可折叠) -->
      <div class="assistant-sidebar" :class="{ collapsed: isCollapsed }" :style="{ width: sidebarWidth + '%' }">
        <!-- 用户区 -->
        <div class="sidebar-top" v-show="!isCollapsed">
          <div class="user-section">
            <div class="user-avatar">
              <img v-if="userAvatar" :src="userAvatar" alt="用户头像" class="avatar-img" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="create-icon" @click="handleNewSession">
              <n-icon :component="AddCircleOutline" size="24" />
            </div>
          </div>
        </div>

        <!-- 搜索栏 -->
        <div class="sidebar-search" v-show="!isCollapsed">
          <div class="search-box">
            <n-icon :component="SearchOutline" class="search-icon" size="16" />
            <input
              ref="searchInputRef"
              type="text"
              placeholder="搜索对话"
              class="search-input"
              v-model="searchQuery"
            />
          </div>
        </div>

        <!-- 会话列表 -->
        <div class="sidebar-nav">
          <div class="session-list" v-show="!isCollapsed">
            <div
              v-for="session in filteredSessions"
              :key="session.id"
              class="session-item"
              :class="{ active: session.id === currentSessionId }"
              @click="handleSwitchSession(session.id)"
              @click.capture="() => console.log('Session item clicked:', session.id)"
            >
              <div class="session-content">
                <n-icon class="session-icon" :component="ChatboxEllipsesOutline" size="18" />
                <span class="session-title">{{ session.title || '新对话' }}</span>
              </div>
              <div class="session-actions" @click.stop>
                <n-dropdown :options="getSessionOptions(session.id)" @select="handleSessionAction">
                  <n-button size="tiny" text>
                    <template #icon>
                      <n-icon :component="EllipsisVerticalOutline" size="16" />
                    </template>
                  </n-button>
                </n-dropdown>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredSessions.length === 0" class="empty-sessions">
              <n-empty description="暂无对话" size="small" />
            </div>
          </div>
        </div>
      </div>

      <!-- 可拖动分隔条（带折叠按钮） -->
      <div
        class="resize-divider"
        @mousedown="startResize"
        @dblclick="resetWidth"
      >
        <div class="divider-line"></div>
        <div class="collapse-toggle-btn" @click.stop="toggleSidebar">
          <n-icon :component="isCollapsed ? ChevronForwardOutline : ChevronBackOutline" size="14" />
        </div>
      </div>

      <!-- 右侧：聊天区域 -->
      <div class="assistant-content">
        <ChatArea
          :session="currentSession"
          :messages="currentMessages"
          :loading="isLoading"
          @send-message="handleSendMessage"
          @stop-streaming="handleStopStreaming"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NEmpty, NDropdown, useDialog } from 'naive-ui'
import {
  ArrowBackOutline,
  AddCircleOutline,
  SearchOutline,
  ChatboxEllipsesOutline,
  EllipsisVerticalOutline,
  ChevronForwardOutline,
  ChevronBackOutline
} from '@vicons/ionicons5'
import { useAssistantStore } from '@/stores/assistant'
import ChatArea from '@/components/assistant/ChatArea.vue'

const router = useRouter()
const assistantStore = useAssistantStore()
const dialog = useDialog()

// ========== 侧栏相关 ==========
const sidebarWidth = ref(20) // 默认20%
const isResizing = ref(false)
const startX = ref(0)
const startWidth = ref(20)
const isCollapsed = ref(false)
const savedWidth = ref(20)

// 用户信息
const userInitial = computed(() => {
  const userStr = localStorage.getItem('user_info')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      return user.username ? user.username.charAt(0).toUpperCase() : 'U'
    } catch {
      return 'U'
    }
  }
  return 'U'
})

const userAvatar = computed(() => {
  const cachedAvatar = localStorage.getItem('user_avatar')
  if (cachedAvatar) {
    return cachedAvatar
  }
  return null
})

// 搜索
const searchInputRef = ref<HTMLInputElement>()
const searchQuery = ref('')

// ========== 数据相关 ==========
const sessions = computed(() => assistantStore.sessions)
const currentSessionId = computed(() => assistantStore.currentSessionId)
const currentSession = computed(() => assistantStore.currentSession)
const currentMessages = computed(() => assistantStore.currentMessages)
const isLoading = ref(false)

// 过滤后的会话列表
const filteredSessions = computed(() => {
  if (!searchQuery.value) return sessions.value
  return sessions.value.filter((session) =>
    (session.title || '').toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// ========== 方法 ==========
// 侧栏折叠
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  if (isCollapsed.value) {
    savedWidth.value = sidebarWidth.value
  } else {
    sidebarWidth.value = savedWidth.value || 20
  }
}

// 开始拖动调整宽度
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  startX.value = e.clientX
  startWidth.value = sidebarWidth.value

  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

// 拖动中
const onResize = (e: MouseEvent) => {
  if (!isResizing.value) return

  const mainCard = document.querySelector('.assistant-main-card') as HTMLElement
  if (!mainCard) return

  const containerWidth = mainCard.offsetWidth
  const deltaX = e.clientX - startX.value
  const deltaPercent = (deltaX / containerWidth) * 100

  let newWidth = startWidth.value + deltaPercent
  newWidth = Math.max(15, Math.min(60, newWidth))

  sidebarWidth.value = newWidth
}

// 停止拖动
const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// 重置宽度（双击分隔条）
const resetWidth = () => {
  sidebarWidth.value = 20
}

// 会话操作选项
const getSessionOptions = (sessionId: number) => [
  {
    label: '删除',
    key: 'delete',
    sessionId
  }
]

// 处理会话操作
const handleSessionAction = (key: string, option: any) => {
  const sessionId = option.sessionId
  if (key === 'delete') {
    dialog.warning({
      title: '确认删除',
      content: '确定要删除这个对话吗？删除后无法恢复。',
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: () => {
        handleDeleteSession(sessionId)
      }
    })
  }
}

// 返回主页
const goBackHome = () => {
  router.push('/')
}

// 新建对话（临时会话，不调用接口）
const handleNewSession = () => {
  assistantStore.startNewSession()
}

// 切换对话
const handleSwitchSession = async (sessionId: number) => {
  console.log('handleSwitchSession called with sessionId:', sessionId)
  try {
    await assistantStore.switchSession(sessionId)
    console.log('switchSession completed')
  } catch (error) {
    console.error('Error in switchSession:', error)
  }
}

// 删除对话
const handleDeleteSession = async (sessionId: number) => {
  await assistantStore.deleteSession(sessionId)
}

// 发送消息
const handleSendMessage = async (content: string, options: { deepThinking: boolean; webSearch: boolean }) => {
  isLoading.value = true
  await assistantStore.sendMessage(content, options)
  isLoading.value = false
}

// 停止流式响应
const handleStopStreaming = () => {
  assistantStore.stopStreaming()
}

// 快捷键：Ctrl+J 聚焦搜索
const handleKeyDown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'j') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  assistantStore.loadSessions()
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
/* ========== 第1层：页面容器 ========== */
.assistant-page-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1;
}

/* ========== 第2层：页面主体容器 ========== */
.assistant-main-card {
  width: 93%;
  height: 85vh;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  display: flex;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out;
  user-select: none;
}

/* ========== 返回主页按钮 ========== */
.back-home-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
  padding: 8px 14px !important;
  font-size: 13px !important;
  height: 36px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2) !important;
  color: var(--color-text-primary) !important;
  transition: all 0.3s ease;
}

.back-home-btn:hover {
  background: rgba(255, 255, 255, 1) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25) !important;
}

/* ========== 左侧侧栏 ========== */
.assistant-sidebar {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  min-width: 60px;
  max-width: 35%;
}

/* 折叠状态 */
.assistant-sidebar.collapsed {
  width: 60px !important;
}

.assistant-sidebar.collapsed .sidebar-top,
.assistant-sidebar.collapsed .sidebar-search {
  opacity: 0;
  pointer-events: none;
}

/* 顶部用户区 */
.sidebar-top {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: opacity 0.3s ease;
}

.user-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.create-icon:hover {
  background: rgba(0, 0, 0, 0.05);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
}

.user-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 搜索栏 */
.sidebar-search {
  padding: 12px;
  transition: opacity 0.3s ease;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: var(--color-text-secondary);
  z-index: 1;
}

.search-input {
  width: 100%;
  height: 36px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 0 12px 0 36px;
  font-size: 13px;
  color: var(--color-text-primary);
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: var(--color-text-secondary);
}

.search-input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.7);
  border-color: #333;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05);
}

/* 会话列表 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 4px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.session-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

.session-item.active {
  background: rgba(0, 0, 0, 0.06);
}

.session-item.active .session-title {
  color: #333;
  font-weight: 600;
}

.session-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  overflow: hidden;
}

.session-icon {
  color: #666;
  flex-shrink: 0;
}

.session-item.active .session-icon {
  color: #333;
}

.session-title {
  font-size: 14px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.empty-sessions {
  padding: 40px 20px;
  text-align: center;
}

/* ========== 可拖动分隔条 ========== */
.resize-divider {
  width: 8px;
  background: transparent;
  cursor: col-resize;
  position: relative;
  transition: all 0.3s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.resize-divider:hover {
  background: rgba(0, 0, 0, 0.03);
}

.resize-divider:active {
  background: rgba(0, 0, 0, 0.05);
}

.divider-line {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 1px;
  height: 40px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.resize-divider:hover .divider-line {
  opacity: 1;
}

/* 折叠按钮 */
.collapse-toggle-btn {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.15);
  color: var(--color-text-secondary);
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: scale(0.9);
  pointer-events: none;
}

.resize-divider:hover .collapse-toggle-btn {
  opacity: 1;
  transform: scale(1);
  pointer-events: auto;
}

.collapse-toggle-btn:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(0, 0, 0, 0.3);
  color: #333;
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.collapse-toggle-btn:active {
  transform: scale(0.95);
}

/* ========== 右侧聊天区域 ========== */
.assistant-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  user-select: text;
}

/* ========== 动画 ========== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 滚动条样式 ========== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* ========== 按钮圆角 ========== */
:deep(.n-button) {
  border-radius: 6px !important;
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .assistant-main-card {
    width: 98%;
    height: 90vh;
    flex-direction: column;
  }

  .back-home-btn {
    top: 12px;
    left: 12px;
    padding: 6px 12px !important;
    font-size: 12px !important;
    height: 32px !important;
  }

  .resize-divider {
    width: 6px;
  }

  .assistant-sidebar {
    width: 100% !important;
    height: 200px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }

  .assistant-sidebar.collapsed {
    display: none;
  }

  .assistant-content {
    width: 100%;
    flex: 1;
  }
}
</style>
