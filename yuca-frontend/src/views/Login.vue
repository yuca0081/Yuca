<template>
  <div class="login-container">
    <div class="login-box">
      <!-- Logo -->
      <div class="logo">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
      </div>

      <!-- 标题 -->
      <h1 class="title">登录到 Yuca</h1>

      <!-- 注册成功提示 -->
      <n-alert
        v-if="showRegisterSuccess"
        type="success"
        :show-icon="false"
        class="success-alert"
      >
        注册成功！请登录
      </n-alert>

      <!-- 登录表单 -->
      <div class="form">
        <n-input
          v-model:value="formData.account"
          placeholder="用户名 / 邮箱 / 手机号"
          size="large"
          class="input-item"
          :input-props="{ autocomplete: 'username' }"
        />

        <n-input
          v-model:value="formData.password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="密码"
          size="large"
          class="input-item"
          :input-props="{ autocomplete: 'current-password' }"
        >
          <template #suffix>
            <n-icon
              :component="showPassword ? EyeOutline : EyeOffOutline"
              size="18"
              class="eye-icon"
              @click="showPassword = !showPassword"
            />
          </template>
        </n-input>

        <div class="checkbox-wrap">
          <n-checkbox v-model:checked="formData.rememberMe">
            记住我
          </n-checkbox>
        </div>

        <n-button
          type="primary"
          size="large"
          block
          :loading="loading"
          @click="handleLogin"
          class="submit-btn"
        >
          {{ loading ? '登录中...' : '登录' }}
        </n-button>
      </div>

      <!-- 底部链接 -->
      <div class="footer">
        还没有账号？
        <router-link to="/register" class="link">注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NInput, NButton, NCheckbox, NAlert, NIcon, useMessage } from 'naive-ui'
import { EyeOutline, EyeOffOutline } from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import { hashPassword } from '@/utils/crypto'
import type { LoginRequest } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const formData = ref<LoginRequest>({
  account: '',
  password: '',
  rememberMe: false
})

const loading = ref(false)
const showRegisterSuccess = ref(false)
const showPassword = ref(false)

onMounted(() => {
  const registered = route.query.registered as string
  const username = route.query.username as string

  if (registered === 'true') {
    showRegisterSuccess.value = true
    if (username) {
      formData.value.account = username
    }
    setTimeout(() => {
      showRegisterSuccess.value = false
    }, 3000)
  }
})

const handleLogin = async () => {
  if (!formData.value.account) {
    message.error('请输入账号')
    return
  }
  if (!formData.value.password) {
    message.error('请输入密码')
    return
  }

  try {
    loading.value = true
    const hashedPassword = hashPassword(formData.value.password)

    const res = await login({
      account: formData.value.account,
      password: hashedPassword,
      rememberMe: formData.value.rememberMe
    })

    userStore.setTokens(res.accessToken, res.refreshToken)
    userStore.setUserInfo(res.user)
    message.success('登录成功')

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: any) {
    message.error(error?.message || '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
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
  background: #f6f8fa;
}

.login-box {
  width: 100%;
  max-width: 340px;
}

.logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.logo svg {
  width: 48px;
  height: 48px;
  color: #24292f;
}

.title {
  font-size: 24px;
  font-weight: 300;
  color: #24292f;
  text-align: center;
  margin: 0 0 20px 0;
}

.success-alert {
  margin-bottom: 16px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-item {
  width: 100%;
}

.checkbox-wrap {
  margin: 4px 0;
}

.eye-icon {
  cursor: pointer;
  color: #57606a;
  transition: color 0.2s;
}

.eye-icon:hover {
  color: #24292f;
}

.submit-btn {
  margin-top: 8px;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
}

.footer {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #57606a;
}

.footer .link {
  color: #0969da;
  text-decoration: none;
  font-weight: 500;
}

.footer .link:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-box {
    max-width: 100%;
  }
}
</style>
