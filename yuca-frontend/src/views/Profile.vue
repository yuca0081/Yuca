<template>
  <div class="profile-page">
    <!-- 返回按钮 -->
    <n-button class="back-button" @click="goBack">
      <template #icon>
        <n-icon><ArrowBackIcon /></n-icon>
      </template>
      <span>返回</span>
    </n-button>

    <div class="profile-card">
      <h2 class="card-title">个人资料</h2>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <n-spin size="large" />
      </div>

      <!-- 内容区域 -->
      <div v-else class="card-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <label class="avatar-wrapper">
            <input
              type="file"
              accept="image/*"
              @change="handleFileChange"
              class="file-input"
            />
            <img
              :src="formData.avatarUrl ? `${getFullAvatarUrl(formData.avatarUrl, userInfo.id)}?t=${avatarTimestamp}` : defaultAvatar"
              alt="Avatar"
              class="avatar-image"
              @error="handleAvatarError"
            />
            <div v-if="uploading" class="upload-overlay">
              <n-spin size="large" />
            </div>
            <div class="avatar-hint">
              支持 JPG、PNG 格式，文件大小不超过 5MB
            </div>
          </label>
        </div>

        <!-- 表单区域 -->
        <div class="form-section">
          <!-- 用户名 -->
          <div class="form-field">
            <label class="field-label">用户名</label>
            <input
              v-model="userInfo.username"
              type="text"
              class="text-input"
              disabled
            />
          </div>

          <!-- 邮箱 -->
          <div class="form-field">
            <label class="field-label">邮箱</label>
            <input
              v-model="formData.email"
              type="email"
              placeholder="请输入邮箱"
              class="text-input"
            />
          </div>

          <!-- 手机号 -->
          <div class="form-field">
            <label class="field-label">手机号</label>
            <input
              v-model="formData.phone"
              type="tel"
              placeholder="请输入手机号"
              class="text-input"
            />
          </div>

          <!-- 昵称 -->
          <div class="form-field">
            <label class="field-label">昵称</label>
            <input
              v-model="formData.nickname"
              type="text"
              placeholder="请输入昵称"
              maxlength="50"
              class="text-input"
            />
            <div class="char-count">{{ formData.nickname.length }}/50</div>
          </div>

          <!-- 保存按钮 -->
          <div class="button-section">
            <n-button
              type="primary"
              @click="handleSave"
              :loading="saving"
              class="save-button"
            >
              保存更改
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NSpin, useMessage } from 'naive-ui'
import { ArrowBack as ArrowBackIcon } from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { uploadAvatar, updateProfile } from '@/api/user'
import type { UpdateProfileRequest } from '@/types/api'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

// 返回主页
const goBack = () => {
  router.push('/')
}

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

// 状态
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const avatarTimestamp = ref(Date.now()) // 用于破坏头像缓存

// 默认头像
const defaultAvatar = 'https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg'

// 用户信息（只读）
const userInfo = reactive({
  id: 0,
  username: ''
})

// 表单数据（可编辑）
const formData = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatarUrl: ''
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    loading.value = true
    const user = await userStore.fetchUserInfo()

    // 设置只读字段
    userInfo.id = user.id
    userInfo.username = user.username

    // 设置可编辑字段
    formData.nickname = user.nickname || ''
    formData.email = user.email || ''
    formData.phone = user.phone || ''
    formData.avatarUrl = user.avatarUrl || ''

    // 更新时间戳，确保图片重新加载
    avatarTimestamp.value = Date.now()
  } catch (error: any) {
    message.error(error.message || '加载用户信息失败')
    if (error.message?.includes('401')) {
      router.push('/login')
    }
  } finally {
    loading.value = false
  }
}

// 头像加载错误处理
const handleAvatarError = () => {
  console.error('头像加载失败:', formData.avatarUrl)
}

// 处理文件选择
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 验证文件
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size <= 5 * 1024 * 1024

  if (!isImage) {
    message.error('只能上传图片文件')
    return
  }
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB')
    return
  }

  // 上传头像
  try {
    uploading.value = true

    // 上传文件（后端会自动更新用户的 avatar_url 字段）
    await uploadAvatar(file)

    // 重新获取用户信息，获取数据库中的最新头像路径
    const user = await userStore.fetchUserInfo()
    formData.avatarUrl = user.avatarUrl || ''

    // 更新时间戳，强制刷新图片
    avatarTimestamp.value = Date.now()

    message.success('头像上传成功')
  } catch (error: any) {
    message.error(error.message || '头像上传失败')
  } finally {
    uploading.value = false
    // 清空 input，允许重复上传同一文件
    target.value = ''
  }
}

// 保存更改
const handleSave = async () => {
  if (formData.nickname && formData.nickname.length > 50) {
    message.error('昵称长度不能超过50个字符')
    return
  }

  if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    message.error('请输入有效的邮箱地址')
    return
  }

  try {
    saving.value = true
    const data: UpdateProfileRequest = {
      nickname: formData.nickname,
      email: formData.email,
      phone: formData.phone,
      avatarUrl: formData.avatarUrl
    }

    const updatedUser = await updateProfile(data)
    userStore.setUser(updatedUser)
    message.success('个人资料更新成功')

    // 返回主页
    setTimeout(() => {
      router.push('/')
    }, 500)
  } catch (error: any) {
    message.error(error.message || '更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn()) {
    router.push('/login')
    return
  }
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 40px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
  position: relative;
}

/* 返回按钮 */
.back-button {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
  padding: 8px 14px !important;
  font-size: 13px !important;
  height: 36px !important;
  background: rgba(255, 255, 255, 0.75) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2) !important;
  color: var(--color-text-primary) !important;
  transition: all 0.3s ease;
}

.back-button:hover {
  background: rgba(255, 255, 255, 0.85) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25) !important;
}

.profile-card {
  width: 100%;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: slideUp 0.4s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-title {
  margin: 0;
  padding: 28px 24px 20px;
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.loading-container {
  padding: 60px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.card-content {
  padding: 32px 24px;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.file-input {
  display: none;
}

.avatar-image {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  border: 4px solid white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  object-fit: cover;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  transition: all 0.3s ease;
}

.avatar-wrapper:hover .avatar-image {
  transform: scale(1.05);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.avatar-hint {
  margin-top: 12px;
  color: #6b7280;
  font-size: 13px;
  text-align: center;
}

/* 表单区域 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-left: 4px;
}

.disabled-input {
  padding: 12px 16px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  color: #6b7280;
  font-size: 15px;
  cursor: not-allowed;
}

.text-input {
  width: 100%;
  padding: 12px 16px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  color: var(--color-text-primary);
  font-size: 15px;
  transition: all 0.2s ease;
  outline: none;
}

.text-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1);
}

.text-input::placeholder {
  color: #9ca3af;
}

.char-count {
  position: absolute;
  right: 16px;
  bottom: -20px;
  font-size: 12px;
  color: #9ca3af;
}

/* 按钮区域 */
.button-section {
  margin-top: 24px;
}

.save-button {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 6px;
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: white !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.save-button:hover {
  opacity: 0.9;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-page {
    padding: 20px 16px;
  }

  .back-button {
    top: 12px;
    left: 12px;
    padding: 6px 12px !important;
    font-size: 12px !important;
    height: 32px !important;
  }

  .profile-card {
    max-width: 100%;
  }

  .card-title {
    padding: 24px 20px 16px;
    font-size: 20px;
  }

  .card-content {
    padding: 24px 20px;
  }

  .avatar-image {
    width: 80px;
    height: 80px;
  }
}
</style>
