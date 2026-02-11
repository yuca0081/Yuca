<template>
  <div class="tool-page-container">
    <n-button class="back-btn" @click="goBack">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="16" />
      </template>
      <span>返回</span>
    </n-button>

    <div class="tool-main-card">
      <div class="tool-header">
        <div class="tool-title-section">
          <div class="tool-icon">
            <n-icon :component="ShieldCheckmarkOutline" size="28" />
          </div>
          <h1 class="tool-title">JWT 加解密工具</h1>
        </div>
      </div>

      <div class="jwt-workspace">
        <!-- JWT 解析 -->
        <div class="jwt-section">
          <h3 class="section-title">JWT 解析</h3>
          <div class="input-area">
            <n-input
              v-model:value="jwtInput"
              type="textarea"
              placeholder="请输入 JWT 令牌..."
              :autosize="{ minRows: 6, maxRows: 10 }"
              :input-props="{ spellcheck: false }"
            />
            <div class="action-buttons">
              <n-button type="primary" @click="parseJwt">解析</n-button>
              <n-button @click="clearJwt">清空</n-button>
            </div>
          </div>
          <div v-if="jwtResult" class="jwt-result">
            <div class="jwt-result-section">
              <h4 class="jwt-result-title">Header</h4>
              <pre class="jwt-result-content">{{ formatJson(jwtResult.header) }}</pre>
            </div>
            <div class="jwt-result-section">
              <h4 class="jwt-result-title">Payload</h4>
              <pre class="jwt-result-content">{{ formatJson(jwtResult.payload) }}</pre>
            </div>
            <div class="jwt-result-section">
              <h4 class="jwt-result-title">Signature</h4>
              <div class="jwt-signature">{{ jwtResult.signature }}</div>
            </div>
            <div v-if="jwtResult.error" class="jwt-error">
              <n-icon :component="AlertCircleOutline" size="16" />
              <span>{{ jwtResult.error }}</span>
            </div>
          </div>
        </div>

        <!-- JWT 生成 -->
        <div class="jwt-section">
          <h3 class="section-title">JWT 生成</h3>
          <div class="generate-inputs">
            <div class="input-row">
              <label class="input-label">Payload (JSON)</label>
              <n-input
                v-model:value="payloadInput"
                type="textarea"
                placeholder='{"userId": 123, "username": "test"}'
                :autosize="{ minRows: 4, maxRows: 8 }"
              />
            </div>
            <div class="input-row">
              <label class="input-label">Secret (可选)</label>
              <n-input
                v-model:value="secretInput"
                type="password"
                placeholder="输入密钥..."
                show-password-on="click"
              />
            </div>
            <div class="action-buttons">
              <n-button type="primary" @click="generateJwt">生成</n-button>
              <n-button @click="loadPayloadExample">示例</n-button>
            </div>
          </div>
          <div v-if="generatedToken" class="token-result">
            <div class="token-label">生成的 JWT：</div>
            <div class="token-value">{{ generatedToken }}</div>
            <n-button size="small" @click="copyToken" class="copy-token-btn">
              <template #icon>
                <n-icon :component="CopyOutline" size="14" />
              </template>
              复制
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInput, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  ShieldCheckmarkOutline,
  CopyOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'
import type { JwtInfo } from '@/types/tools'

const router = useRouter()
const message = useMessage()

const jwtInput = ref('')
const jwtResult = ref<JwtInfo | null>(null)
const payloadInput = ref('')
const secretInput = ref('')
const generatedToken = ref('')

const goBack = () => router.push('/tools')

const clearJwt = () => {
  jwtInput.value = ''
  jwtResult.value = null
}

const parseJwt = () => {
  const token = jwtInput.value.trim()
  if (!token) {
    message.warning('请输入 JWT 令牌')
    return
  }

  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      throw new Error('JWT 格式错误：应包含 3 部分')
    }

    const header = JSON.parse(atob(parts[0]))
    const payload = JSON.parse(atob(parts[1]))
    const signature = parts[2]

    jwtResult.value = {
      header,
      payload,
      signature,
      valid: true
    }

    message.success('解析成功')
  } catch (e) {
    jwtResult.value = {
      header: {},
      payload: {},
      signature: '',
      valid: false,
      error: `解析失败: ${(e as Error).message}`
    }
    message.error('解析失败')
  }
}

const formatJson = (obj: Record<string, any>) => {
  return JSON.stringify(obj, null, 2)
}

const loadPayloadExample = () => {
  payloadInput.value = JSON.stringify(
    {
      userId: 123,
      username: 'test',
      role: 'admin',
      exp: Math.floor(Date.now() / 1000) + 3600
    },
    null,
    2
  )
}

const generateJwt = () => {
  if (!payloadInput.value.trim()) {
    message.warning('请输入 Payload')
    return
  }

  try {
    const payload = JSON.parse(payloadInput.value)
    const header = { alg: 'HS256', typ: 'JWT' }

    const headerEncoded = btoa(JSON.stringify(header))
    const payloadEncoded = btoa(JSON.stringify(payload))

    // 简单生成（实际应用中需要使用真实签名）
    const signature = btoa(`${headerEncoded}.${payloadEncoded}.${secretInput.value || 'secret'}`)
    generatedToken.value = `${headerEncoded}.${payloadEncoded}.${signature}`

    message.success('生成成功')
  } catch (e) {
    message.error('生成失败：请检查 Payload 格式')
  }
}

const copyToken = async () => {
  try {
    await navigator.clipboard.writeText(generatedToken.value)
    message.success('已复制到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}
</script>

<style scoped>
.tool-page-container {
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
  overflow-y: auto;
}

.back-btn {
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

.back-btn:hover {
  background: rgba(255, 255, 255, 0.85) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25) !important;
}

.tool-main-card {
  width: 90%;
  max-width: 1200px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  padding: 32px;
  animation: fadeInUp 0.5s ease-out;
}

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

.tool-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.tool-title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tool-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(156, 163, 175, 0.15);
  border: 2px solid rgba(156, 163, 175, 0.4);
  border-radius: 12px;
  color: #6b7280;
}

.tool-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.jwt-workspace {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.jwt-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin: 0;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-area :deep(.n-input__textarea-el) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.5) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 8px !important;
  padding: 16px !important;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.input-area :deep(.n-input__textarea-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.input-area :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.input-area :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.jwt-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.jwt-result-section {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 12px;
}

.jwt-result-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

.jwt-result-content {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-primary);
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.jwt-signature {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.jwt-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
}

.generate-inputs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.generate-inputs :deep(.n-input__textarea-el) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.5) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 8px !important;
  padding: 16px !important;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.generate-inputs :deep(.n-input__textarea-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.generate-inputs :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.generate-inputs :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
}

.input-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.token-result {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 16px;
  position: relative;
}

.token-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.token-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-primary);
  word-break: break-all;
  margin-bottom: 12px;
}

.copy-token-btn {
  position: absolute;
  top: 16px;
  right: 16px;
}

@media (max-width: 1024px) {
  .jwt-workspace {
    grid-template-columns: 1fr;
  }
  .tool-main-card {
    width: 95%;
    padding: 24px;
  }
}

/* 覆盖 primary 按钮颜色 - 改为浅色背景深色边框 */
.action-buttons :deep(.n-button--primary) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.action-buttons :deep(.n-button--primary:hover) {
  background-color: #e5e7eb !important;
  border-color: #4b5563 !important;
  color: #1f2937 !important;
}

/* 覆盖默认按钮边框颜色 */
.action-buttons :deep(.n-button) {
  background-color: #f9fafb !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

.action-buttons :deep(.n-button:hover) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

/* 覆盖 small 按钮样式 */
.section-actions :deep(.n-button) {
  background-color: #f9fafb !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

.section-actions :deep(.n-button:hover) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

/* 覆盖 tiny 按钮样式 */
.copy-token-btn :deep(.n-button) {
  background-color: #f9fafb !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
}

.copy-token-btn :deep(.n-button:hover) {
  background-color: #f3f4f6 !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}
</style>
