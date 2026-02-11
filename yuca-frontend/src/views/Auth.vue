<template>
  <div class="auth-container">
    <div class="auth-card">
      <!-- Logo -->
      <div class="logo">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
      </div>

      <!-- 标题 -->
      <h1 class="title">{{ currentTab === 'login' ? '登录到 Yuca' : '注册 Yuca' }}</h1>

      <!-- 标签切换 -->
      <n-tabs v-model:value="currentTab" type="segment" animated>
        <n-tab-pane name="login" tab="登录">
          <!-- 登录表单 -->
          <n-form ref="loginFormRef" :model="loginForm" class="auth-form">
            <n-form-item path="account">
              <n-input
                v-model:value="loginForm.account"
                placeholder="用户名 / 邮箱 / 手机号"
                size="large"
                :input-props="{ autocomplete: 'username' }"
              />
            </n-form-item>

            <n-form-item path="password">
              <n-input
                v-model:value="loginForm.password"
                :type="showLoginPassword ? 'text' : 'password'"
                placeholder="密码"
                size="large"
                show-password-on="click"
                :input-props="{ autocomplete: 'current-password' }"
              >
                <template #password-visible-icon>
                  <n-icon :component="EyeOutline" />
                </template>
                <template #password-invisible-icon>
                  <n-icon :component="EyeOffOutline" />
                </template>
              </n-input>
            </n-form-item>

            <div class="checkbox-wrap">
              <n-checkbox v-model:checked="loginForm.rememberMe">
                记住我
              </n-checkbox>
            </div>

            <n-button
              type="primary"
              size="large"
              block
              :loading="loginLoading"
              @click="handleLogin"
              class="submit-btn"
            >
              {{ loginLoading ? '登录中...' : '登录' }}
            </n-button>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="register" tab="注册">
          <!-- 注册表单 -->
          <n-form ref="registerFormRef" :model="registerForm" class="auth-form">
            <n-form-item path="username">
              <n-input
                v-model:value="registerForm.username"
                placeholder="用户名"
                size="large"
                :input-props="{ autocomplete: 'username' }"
                @blur="validateUsername"
              />
            </n-form-item>
            <n-text v-if="usernameError" depth="3" style="font-size: 12px; padding-left: 12px; margin-bottom: 8px;">
              {{ usernameError }}
            </n-text>

            <n-form-item path="password">
              <n-input
                v-model:value="registerForm.password"
                placeholder="密码"
                size="large"
                show-password-on="click"
                :input-props="{ autocomplete: 'new-password' }"
                @blur="validatePassword"
              >
                <template #password-visible-icon>
                  <n-icon :component="EyeOutline" />
                </template>
                <template #password-invisible-icon>
                  <n-icon :component="EyeOffOutline" />
                </template>
              </n-input>
            </n-form-item>
            <n-text v-if="passwordError" depth="3" style="font-size: 12px; padding-left: 12px; margin-bottom: 8px;">
              {{ passwordError }}
            </n-text>

            <n-form-item path="confirmPassword">
              <n-input
                v-model:value="confirmPassword"
                placeholder="确认密码"
                size="large"
                show-password-on="click"
                :input-props="{ autocomplete: 'new-password' }"
                @blur="validateConfirmPassword"
              >
                <template #password-visible-icon>
                  <n-icon :component="EyeOutline" />
                </template>
                <template #password-invisible-icon>
                  <n-icon :component="EyeOffOutline" />
                </template>
              </n-input>
            </n-form-item>
            <n-text v-if="confirmPasswordError" depth="3" style="font-size: 12px; padding-left: 12px; margin-bottom: 8px;">
              {{ confirmPasswordError }}
            </n-text>

            <n-form-item path="email">
              <n-input
                v-model:value="registerForm.email"
                placeholder="邮箱（可选）"
                size="large"
                :input-props="{ autocomplete: 'email' }"
              />
            </n-form-item>

            <div class="hint">邮箱和手机号至少需要填写一个</div>

            <n-button
              type="primary"
              size="large"
              block
              :loading="registerLoading"
              @click="handleRegister"
              class="submit-btn"
            >
              {{ registerLoading ? '注册中...' : '注册' }}
            </n-button>
          </n-form>
        </n-tab-pane>
      </n-tabs>

      <!-- 消息提示 -->
      <div v-if="successMessage" class="alert success">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="alert error">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NInput, NButton, NCheckbox, NText, NIcon, NTabs, NTabPane, NForm, NFormItem, useMessage } from 'naive-ui'
import { EyeOutline, EyeOffOutline } from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { login, register } from '@/api/user'
import { hashPassword } from '@/utils/crypto'
import type { LoginRequest, RegisterRequest } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const currentTab = ref<'login' | 'register'>('login')

// 登录表单
const loginForm = ref<LoginRequest>({
  account: '',
  password: '',
  rememberMe: false
})
const loginLoading = ref(false)
const showLoginPassword = ref(false)

// 注册表单
const registerForm = ref<RegisterRequest>({
  username: '',
  password: '',
  email: '',
  phone: '',
  nickname: ''
})
const confirmPassword = ref('')
const registerLoading = ref(false)

// 验证错误
const usernameError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')

// 消息提示
const errorMessage = ref('')
const successMessage = ref('')

onMounted(() => {
  if (route.name === 'Register') {
    currentTab.value = 'register'
  }

  const registered = route.query.registered as string
  const username = route.query.username as string

  if (registered === 'true') {
    currentTab.value = 'login'
    successMessage.value = '注册成功！请登录'
    if (username) {
      loginForm.value.account = username
    }
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  }
})

const validateUsername = () => {
  const username = registerForm.value.username.trim()
  if (!username) {
    usernameError.value = '请输入用户名'
    return false
  }
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(username)) {
    usernameError.value = '用户名必须是4-20字符，仅支持字母、数字、下划线'
    return false
  }
  usernameError.value = ''
  return true
}

const validatePassword = () => {
  const password = registerForm.value.password
  if (!password) {
    passwordError.value = '请输入密码'
    return false
  }
  if (!/^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/.test(password)) {
    passwordError.value = '密码必须是6-20字符，必须包含字母和数字'
    return false
  }
  passwordError.value = ''
  return true
}

const validateConfirmPassword = () => {
  if (!confirmPassword.value) {
    confirmPasswordError.value = '请再次输入密码'
    return false
  }
  if (confirmPassword.value !== registerForm.value.password) {
    confirmPasswordError.value = '两次输入的密码不一致'
    return false
  }
  confirmPasswordError.value = ''
  return true
}

const handleLogin = async () => {
  if (!loginForm.value.account) {
    message.error('请输入账号')
    return
  }
  if (!loginForm.value.password) {
    message.error('请输入密码')
    return
  }

  try {
    loginLoading.value = true
    errorMessage.value = ''

    const hashedPassword = hashPassword(loginForm.value.password)

    const res = await login({
      account: loginForm.value.account,
      password: hashedPassword,
      rememberMe: loginForm.value.rememberMe
    })

    userStore.setTokens(res.accessToken, res.refreshToken)
    userStore.setUserInfo(res.user)

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: any) {
    errorMessage.value = error?.message || '登录失败，请检查账号和密码'
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  errorMessage.value = ''

  if (!validateUsername() || !validatePassword() || !validateConfirmPassword()) {
    return
  }

  try {
    registerLoading.value = true

    const hashedPassword = hashPassword(registerForm.value.password)

    await register({
      ...registerForm.value,
      password: hashedPassword
    })

    // 注册成功，切换到登录标签
    currentTab.value = 'login'
    successMessage.value = '注册成功！请登录'
    loginForm.value.account = registerForm.value.username

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (error: any) {
    errorMessage.value = error?.message || '注册失败，请稍后重试'
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  transition: all 0.3s ease;
}

.auth-card:hover {
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
}

.logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.logo svg {
  width: 48px;
  height: 48px;
  color: #1f2937;
}

.title {
  font-size: 24px;
  font-weight: bold;
  color: #1f2937;
  text-align: center;
  margin: 0 0 24px 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.checkbox-wrap {
  margin: 8px 0;
}

.hint {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  margin: 8px 0;
}

.submit-btn {
  margin-top: 12px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}

.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
}

.alert.success {
  background: rgba(34, 197, 94, 0.2);
  color: #15803d;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.alert.error {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

/* Naive UI 样式覆盖 */
.auth-form :deep(.n-form-item) {
  margin-bottom: 4px;
}

.auth-form :deep(.n-form-item .n-form-item-blank) {
  padding-bottom: 4px;
}

/* 响应式 */
@media (max-width: 480px) {
  .auth-card {
    max-width: 100%;
    padding: 24px;
  }

  .title {
    font-size: 20px;
  }
}
</style>
