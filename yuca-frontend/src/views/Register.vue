<template>
  <div class="register-container">
    <div class="register-box">
      <!-- Logo -->
      <div class="logo">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
      </div>

      <!-- 标题 -->
      <h1 class="title">注册 Yuca</h1>

      <!-- 注册表单 -->
      <n-form ref="formRef" :model="formData" class="register-form">
        <n-form-item path="username" :show-label="false">
          <n-input
            v-model:value="formData.username"
            placeholder="用户名"
            size="large"
            :input-props="{ autocomplete: 'username' }"
            :status="usernameError ? 'error' : undefined"
            @blur="validateUsername"
            @input="usernameError = ''"
          />
        </n-form-item>
        <n-text v-if="usernameError" depth="3" style="font-size: 12px; padding-left: 4px; margin-top: -8px;">
          {{ usernameError }}
        </n-text>

        <n-form-item path="password" :show-label="false">
          <n-input
            v-model:value="formData.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="密码"
            size="large"
            :input-props="{ autocomplete: 'new-password' }"
            :status="passwordError ? 'error' : undefined"
            @blur="validatePassword"
            @input="passwordError = ''"
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
        </n-form-item>
        <n-text v-if="passwordError" depth="3" style="font-size: 12px; padding-left: 4px; margin-top: -8px;">
          {{ passwordError }}
        </n-text>

        <n-form-item path="confirmPassword" :show-label="false">
          <n-input
            v-model:value="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="确认密码"
            size="large"
            :input-props="{ autocomplete: 'new-password' }"
            :status="confirmPasswordError ? 'error' : undefined"
            @blur="validateConfirmPassword"
            @input="confirmPasswordError = ''"
          >
            <template #suffix>
              <n-icon
                :component="showConfirmPassword ? EyeOutline : EyeOffOutline"
                size="18"
                class="eye-icon"
                @click="showConfirmPassword = !showConfirmPassword"
              />
            </template>
          </n-input>
        </n-form-item>
        <n-text v-if="confirmPasswordError" depth="3" style="font-size: 12px; padding-left: 4px; margin-top: -8px;">
          {{ confirmPasswordError }}
        </n-text>

        <n-form-item path="email" :show-label="false">
          <n-input
            v-model:value="formData.email"
            placeholder="邮箱（可选）"
            size="large"
            :input-props="{ autocomplete: 'email' }"
            :status="emailError ? 'error' : undefined"
            @blur="validateEmail"
            @input="emailError = ''"
          />
        </n-form-item>
        <n-text v-if="emailError" depth="3" style="font-size: 12px; padding-left: 4px; margin-top: -8px;">
          {{ emailError }}
        </n-text>

        <n-form-item path="phone" :show-label="false">
          <n-input
            v-model:value="formData.phone"
            placeholder="手机号（可选）"
            size="large"
            :input-props="{ autocomplete: 'tel' }"
            :status="phoneError ? 'error' : undefined"
            @blur="validatePhone"
            @input="phoneError = ''"
          />
        </n-form-item>
        <n-text v-if="phoneError" depth="3" style="font-size: 12px; padding-left: 4px; margin-top: -8px;">
          {{ phoneError }}
        </n-text>

        <n-form-item path="nickname" :show-label="false">
          <n-input
            v-model:value="formData.nickname"
            placeholder="昵称（可选）"
            size="large"
            :input-props="{ autocomplete: 'nickname' }"
          />
        </n-form-item>

        <div class="hint">
          邮箱和手机号至少需要填写一个
        </div>

        <n-button
          type="primary"
          size="large"
          block
          :loading="loading"
          @click="handleRegister"
          class="submit-btn"
        >
          {{ loading ? '注册中...' : '注册' }}
        </n-button>
      </n-form>

      <!-- 底部链接 -->
      <div class="footer">
        已有账号？
        <router-link to="/login" class="link">登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NText, NIcon, useMessage } from 'naive-ui'
import { EyeOutline, EyeOffOutline } from '@vicons/ionicons5'
import { register } from '@/api/user'
import { hashPassword } from '@/utils/crypto'
import type { RegisterRequest } from '@/types/api'

const router = useRouter()
const message = useMessage()

const formData = ref<RegisterRequest>({
  username: '',
  password: '',
  email: '',
  phone: '',
  nickname: ''
})

const confirmPassword = ref('')
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const usernameError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')
const emailError = ref('')
const phoneError = ref('')

const validateUsername = () => {
  const username = formData.value.username.trim()
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
  const password = formData.value.password
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
  if (confirmPassword.value !== formData.value.password) {
    confirmPasswordError.value = '两次输入的密码不一致'
    return false
  }
  confirmPasswordError.value = ''
  return true
}

const validateEmail = () => {
  const email = formData.value.email?.trim()
  if (!email) {
    emailError.value = ''
    return true
  }
  const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
  if (!emailRegex.test(email)) {
    emailError.value = '请输入正确的邮箱格式'
    return false
  }
  emailError.value = ''
  return true
}

const validatePhone = () => {
  const phone = formData.value.phone?.trim()
  if (!phone) {
    phoneError.value = ''
    return true
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    phoneError.value = '请输入正确的手机号格式'
    return false
  }
  phoneError.value = ''
  return true
}

const validateEmailOrPhone = () => {
  const email = formData.value.email?.trim()
  const phone = formData.value.phone?.trim()
  if (!email && !phone) {
    message.error('邮箱和手机号至少需要填写一个')
    return false
  }
  return true
}

const validateForm = () => {
  const isUsernameValid = validateUsername()
  const isPasswordValid = validatePassword()
  const isConfirmPasswordValid = validateConfirmPassword()
  const isEmailValid = validateEmail()
  const isPhoneValid = validatePhone()
  const hasEmailOrPhone = validateEmailOrPhone()

  return isUsernameValid && isPasswordValid && isConfirmPasswordValid &&
         isEmailValid && isPhoneValid && hasEmailOrPhone
}

const handleRegister = async () => {
  if (!validateForm()) {
    return
  }

  try {
    loading.value = true
    const hashedPassword = hashPassword(formData.value.password)

    await register({
      ...formData.value,
      password: hashedPassword
    })

    message.success('注册成功')

    router.push({
      path: '/login',
      query: { registered: 'true', username: formData.value.username }
    })
  } catch (error: any) {
    message.error(error?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #f6f8fa;
}

.register-box {
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

.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 去除所有 Naive UI 表单项的边框 */
.register-form :deep(.n-form-item) {
  border: none !important;
}

.register-form :deep(.n-form-item .n-form-item-blank) {
  border: none !important;
}

.register-form :deep(.n-form-item .n-form-item-label) {
  border: none !important;
}

.eye-icon {
  cursor: pointer;
  color: #57606a;
  transition: color 0.2s;
}

.eye-icon:hover {
  color: #24292f;
}

.hint {
  font-size: 12px;
  color: #57606a;
  text-align: center;
  margin: -4px 0 8px 0;
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
  .register-box {
    max-width: 100%;
  }
}
</style>
