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
            <n-icon :component="BarcodeOutline" size="28" />
          </div>
          <h1 class="tool-title">UUID 生成器工具</h1>
        </div>
      </div>

      <div class="uuid-workspace">
        <!-- 生成配置 -->
        <div class="config-section">
          <h3 class="section-title">生成配置</h3>
          <div class="config-options">
            <div class="option-row">
              <span class="option-label">UUID 版本：</span>
              <n-radio-group v-model:value="uuidVersion">
                <n-radio-button value="v4">UUID v4 (随机)</n-radio-button>
                <n-radio-button value="v1">UUID v1 (时间)</n-radio-button>
              </n-radio-group>
            </div>
            <div class="option-row">
              <span class="option-label">生成数量：</span>
              <n-input-number
                v-model:value="generateCount"
                :min="1"
                :max="100"
                size="large"
                style="width: 150px"
              />
            </div>
            <div class="option-row">
              <span class="option-label">格式：</span>
              <n-checkbox v-model:checked="uppercase">大写</n-checkbox>
              <n-checkbox v-model:checked="withHyphens">带连字符</n-checkbox>
            </div>
          </div>
          <n-button type="primary" size="large" @click="generateUuids" block>
            生成 UUID
          </n-button>
        </div>

        <!-- 生成结果 -->
        <div class="result-section">
          <div class="section-header">
            <span class="section-title">生成结果 ({{ generatedUuids.length }} 个)</span>
            <div class="section-actions">
              <n-button size="small" @click="clearResults" :disabled="generatedUuids.length === 0">
                清空
              </n-button>
              <n-button size="small" @click="copyAll" :disabled="generatedUuids.length === 0">
                <template #icon>
                  <n-icon :component="CopyOutline" size="14" />
                </template>
                复制全部
              </n-button>
            </div>
          </div>

          <div v-if="generatedUuids.length > 0" class="uuid-list">
            <div v-for="(uuid, index) in generatedUuids" :key="index" class="uuid-item">
              <span class="uuid-index">{{ index + 1 }}</span>
              <span class="uuid-value">{{ uuid }}</span>
              <n-button size="tiny" @click="copyUuid(uuid)">
                <template #icon>
                  <n-icon :component="CopyOutline" size="12" />
                </template>
              </n-button>
            </div>
          </div>

          <div v-else class="empty-result">
            <n-icon :component="BarcodeOutline" size="48" color="#999" />
            <p>点击"生成 UUID" 开始生成</p>
          </div>
        </div>

        <!-- UUID 验证 -->
        <div class="validate-section">
          <h3 class="section-title">UUID 验证</h3>
          <div class="validate-input">
            <n-input
              v-model:value="validateInput"
              placeholder="请输入 UUID 进行验证..."
              size="large"
            >
              <template #suffix>
                <n-button text @click="validateUuid">
                  <template #icon>
                    <n-icon :component="SearchOutline" size="20" />
                  </template>
                </n-button>
              </template>
            </n-input>
          </div>
          <div v-if="validateResult" class="validate-result">
            <div v-if="validateResult.valid" class="valid-result">
              <n-icon :component="CheckmarkCircleOutline" size="20" color="#10b981" />
              <span>有效的 UUID {{ validateResult.version }}</span>
            </div>
            <div v-else class="invalid-result">
              <n-icon :component="AlertCircleOutline" size="20" color="#ef4444" />
              <span>无效的 UUID</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInput, NInputNumber, NRadioGroup, NRadioButton, NCheckbox, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  BarcodeOutline,
  CopyOutline,
  SearchOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const uuidVersion = ref<'v1' | 'v4'>('v4')
const generateCount = ref(1)
const uppercase = ref(false)
const withHyphens = ref(true)
const generatedUuids = ref<string[]>([])

const validateInput = ref('')
const validateResult = ref<{ valid: boolean; version?: string } | null>(null)

const goBack = () => router.push('/tools')

// 生成 UUID v4 (随机)
const generateUUIDv4 = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 生成 UUID v1 (基于时间戳的简化版本)
const generateUUIDv1 = (): string => {
  const timestamp = Date.now()
  const timeHex = timestamp.toString(16).padStart(12, '0')
  const randomHex = Math.random().toString(16).substr(2, 4)
  const clockSeq = Math.random().toString(16).substr(2, 4)
  const node = Math.random().toString(16).substr(2, 12)

  return `${timeHex.substr(0, 8)}-${timeHex.substr(8, 4)}-1${randomHex}-${clockSeq}-${node}`
}

const generateUuids = () => {
  const uuids: string[] = []

  for (let i = 0; i < generateCount.value; i++) {
    let uuid = uuidVersion.value === 'v4' ? generateUUIDv4() : generateUUIDv1()

    if (uppercase.value) {
      uuid = uuid.toUpperCase()
    }

    if (!withHyphens.value) {
      uuid = uuid.replace(/-/g, '')
    }

    uuids.push(uuid)
  }

  generatedUuids.value = uuids
  message.success(`成功生成 ${uuids.length} 个 UUID`)
}

const clearResults = () => {
  generatedUuids.value = []
}

const copyUuid = async (uuid: string) => {
  try {
    await navigator.clipboard.writeText(uuid)
    message.success('已复制到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}

const copyAll = async () => {
  try {
    const text = generatedUuids.value.join('\n')
    await navigator.clipboard.writeText(text)
    message.success('已复制全部到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}

const validateUuid = () => {
  const input = validateInput.value.trim()

  if (!input) {
    message.warning('请输入 UUID')
    return
  }

  // UUID 正则表达式 (支持带连字符和不带连字符)
  const uuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$|^[0-9a-fA-F]{32}$/

  if (uuidRegex.test(input)) {
    // 确定版本
    const parts = input.split('-')
    let version = ''

    if (parts.length === 5) {
      const versionChar = parts[2].charAt(0)
      version = `v${versionChar}`
    } else if (parts.length === 1 && parts[0].length === 32) {
      // 不带连字符的 UUID，检查第 14 位字符
      const versionChar = input.charAt(12)
      version = `v${versionChar}`
    }

    validateResult.value = {
      valid: true,
      version
    }
  } else {
    validateResult.value = {
      valid: false
    }
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
  max-width: 1000px;
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

.uuid-workspace {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.config-section,
.result-section,
.validate-section {
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

.config-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  min-width: 100px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.uuid-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.uuid-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.uuid-item:hover {
  background: rgba(255, 255, 255, 0.6);
  transform: translateX(4px);
}

.uuid-index {
  font-size: 12px;
  color: var(--color-text-secondary);
  min-width: 30px;
  font-weight: 500;
}

.uuid-value {
  flex: 1;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
  color: var(--color-text-secondary);
}

.validate-input {
  width: 100%;
}

.validate-input :deep(.n-input__input-el) {
  background: rgba(255, 255, 255, 0.5) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 8px !important;
  padding: 12px 16px !important;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.validate-input :deep(.n-input__input-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.validate-input :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.validate-input :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
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

.validate-result {
  padding: 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.valid-result {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}

.invalid-result {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.uuid-list::-webkit-scrollbar {
  width: 8px;
}

.uuid-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.uuid-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.uuid-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

@media (max-width: 768px) {
  .tool-main-card {
    width: 95%;
    padding: 24px;
  }

  .option-row {
    flex-wrap: wrap;
  }

  .option-label {
    min-width: auto;
    width: 100%;
  }
}

/* 覆盖 primary 按钮颜色 - 改为浅色背景深色边框 */
.config-section :deep(.n-button--primary) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.config-section :deep(.n-button--primary:hover) {
  background-color: #e5e7eb !important;
  border-color: #4b5563 !important;
  color: #1f2937 !important;
}

/* 覆盖默认按钮边框颜色 */
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
.uuid-item :deep(.n-button--tiny) {
  background-color: #f9fafb !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
}

.uuid-item :deep(.n-button--tiny:hover) {
  background-color: #f3f4f6 !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

/* 覆盖 RadioGroup 颜色 - 浅色背景深色边框 */
.option-row :deep(.n-radio-button--checked) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.option-row :deep(.n-radio-button--checked:hover) {
  background-color: #e5e7eb !important;
  border-color: #4b5563 !important;
}

/* 覆盖 Checkbox 颜色 */
.option-row :deep(.n-checkbox--checked .n-checkbox-box) {
  background-color: #6b7280 !important;
  border-color: #6b7280 !important;
}
</style>
