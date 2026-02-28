<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">登录</h1>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <input
            v-model="loginForm.account"
            type="text"
            placeholder="用户名 / 邮箱 / 手机号"
            class="form-input"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <div class="password-input">
            <input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="密码"
              class="form-input"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
            </button>
          </div>
        </div>

        <div class="form-group checkbox-group">
          <label class="checkbox-label">
            <input v-model="loginForm.rememberMe" type="checkbox" class="checkbox" />
            <span>记住我</span>
          </label>
        </div>

        <button type="submit" class="submit-button" :disabled="loginLoading">
          {{ loginLoading ? '登录中...' : '登录' }}
        </button>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        <div v-if="successMessage" class="success-message">
          {{ successMessage }}
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import { hashPassword } from '@/utils/crypto'
import type { LoginRequest } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = ref<LoginRequest>({
  account: '',
  password: '',
  rememberMe: false
})

const loginLoading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

onMounted(() => {
  const registered = route.query.registered as string
  const username = route.query.username as string

  if (registered === 'true') {
    successMessage.value = '注册成功！请登录'
    if (username) {
      loginForm.value.account = username
    }
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  }
})

const handleLogin = async () => {
  if (!loginForm.value.account) {
    errorMessage.value = '请输入账号'
    return
  }
  if (!loginForm.value.password) {
    errorMessage.value = '请输入密码'
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
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
  background: #F5F5F5;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #000000;
  text-align: center;
  margin: 0 0 32px 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  font-size: 15px;
  color: #333333;
  background: transparent !important;
  border: 1px solid #E0E0E0;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

/* 移除浏览器自动填充的蓝色背景 */
.form-input:-webkit-autofill,
.form-input:-webkit-autofill:hover,
.form-input:-webkit-autofill:focus,
.form-input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 30px #FFFFFF inset !important;
  -webkit-text-fill-color: #333333 !important;
  transition: background-color 5000s ease-in-out 0s;
}

.form-input::placeholder {
  color: #999999;
}

.form-input:hover {
  border-color: #1890FF;
}

.form-input:focus {
  border-color: #1890FF;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.password-input {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input .form-input {
  padding-right: 48px;
}

.toggle-password {
  position: absolute;
  right: 12px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: #999999;
  transition: color 0.2s;
}

.toggle-password:hover {
  color: #666666;
}

.toggle-password svg {
  width: 20px;
  height: 20px;
}

.checkbox-group {
  margin-top: 4px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1890FF;
}

.checkbox-label span {
  font-size: 14px;
  color: #666666;
}

.submit-button {
  width: 100%;
  height: 48px;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #FFFFFF;
  background: #333333;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-button:hover {
  background: #555555;
}

.submit-button:active {
  transform: scale(0.98);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  padding: 12px 16px;
  font-size: 14px;
  color: #F56C6C;
  text-align: center;
  background: #FEF0F0;
  border: 1px solid #FDE2E2;
  border-radius: 8px;
}

.success-message {
  padding: 12px 16px;
  font-size: 14px;
  color: #67C23A;
  text-align: center;
  background: #F0F9EB;
  border: 1px solid #E1F3D8;
  border-radius: 8px;
}

@media (max-width: 480px) {
  .login-card {
    max-width: 100%;
    padding: 24px;
  }

  .login-title {
    font-size: 20px;
  }
}
</style>
