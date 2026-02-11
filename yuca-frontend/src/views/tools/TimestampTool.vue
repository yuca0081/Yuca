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
            <n-icon :component="TimeOutline" size="28" />
          </div>
          <h1 class="tool-title">时间戳转换工具</h1>
        </div>
      </div>

      <div class="timestamp-workspace">
        <!-- 时间戳转日期 -->
        <div class="conversion-section">
          <h3 class="section-title">时间戳 → 日期时间</h3>
          <div class="input-group">
            <n-input
              v-model:value="timestampInput"
              placeholder="请输入时间戳（秒或毫秒）"
              size="large"
              class="input-field"
            >
              <template #suffix>
                <span class="input-hint">秒/毫秒</span>
              </template>
            </n-input>
            <n-button type="primary" @click="convertToDate" size="large">转换</n-button>
          </div>
          <div v-if="dateResult" class="result-display">
            <div class="result-label">转换结果：</div>
            <div class="result-value">{{ dateResult }}</div>
          </div>
        </div>

        <!-- 日期转时间戳 -->
        <div class="conversion-section">
          <h3 class="section-title">日期时间 → 时间戳</h3>
          <div class="input-group">
            <n-date-picker
              v-model:value="dateInput"
              type="datetime"
              placeholder="选择日期时间"
              size="large"
              class="date-picker"
              format="yyyy-MM-dd HH:mm:ss"
            />
            <n-button type="primary" @click="convertToTimestamp" size="large">转换</n-button>
          </div>
          <div v-if="timestampResult" class="result-display">
            <div class="result-label">转换结果：</div>
            <div class="result-value-group">
              <div class="result-value-item">
                <span class="result-item-label">秒：</span>
                <span class="result-item-value">{{ timestampResult.seconds }}</span>
              </div>
              <div class="result-value-item">
                <span class="result-item-label">毫秒：</span>
                <span class="result-item-value">{{ timestampResult.milliseconds }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 当前时间戳 -->
        <div class="current-time-section">
          <h3 class="section-title">当前时间</h3>
          <div class="current-time-display">
            <div class="current-time-item">
              <span class="current-time-label">时间戳（秒）：</span>
              <span class="current-time-value">{{ currentTimestamp.seconds }}</span>
              <n-button text @click="copyToClipboard(currentTimestamp.seconds)">
                <template #icon>
                  <n-icon :component="CopyOutline" size="16" />
                </template>
              </n-button>
            </div>
            <div class="current-time-item">
              <span class="current-time-label">时间戳（毫秒）：</span>
              <span class="current-time-value">{{ currentTimestamp.milliseconds }}</span>
              <n-button text @click="copyToClipboard(currentTimestamp.milliseconds)">
                <template #icon>
                  <n-icon :component="CopyOutline" size="16" />
                </template>
              </n-button>
            </div>
            <div class="current-time-item">
              <span class="current-time-label">日期时间：</span>
              <span class="current-time-value">{{ currentTimestamp.datetime }}</span>
              <n-button text @click="copyToClipboard(currentTimestamp.datetime)">
                <template #icon>
                  <n-icon :component="CopyOutline" size="16" />
                </template>
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInput, NDatePicker, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  TimeOutline,
  CopyOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const timestampInput = ref('')
const dateResult = ref('')
const dateInput = ref<number | null>(null)
const timestampResult = ref<{ seconds: string; milliseconds: string } | null>(null)
const currentTimestamp = ref({
  seconds: '',
  milliseconds: '',
  datetime: ''
})

let timer: number | null = null

const goBack = () => router.push('/tools')

const updateCurrentTime = () => {
  const now = new Date()
  currentTimestamp.value = {
    seconds: Math.floor(now.getTime() / 1000).toString(),
    milliseconds: now.getTime().toString(),
    datetime: now.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  }
}

const convertToDate = () => {
  const input = timestampInput.value.trim()
  if (!input) {
    message.warning('请输入时间戳')
    return
  }

  try {
    const num = parseInt(input)
    if (isNaN(num)) {
      throw new Error('无效的数字')
    }

    // 判断是秒还是毫秒（毫秒时间戳通常是13位）
    const timestamp = num.toString().length <= 10 ? num * 1000 : num
    const date = new Date(timestamp)

    if (isNaN(date.getTime())) {
      throw new Error('无效的时间戳')
    }

    dateResult.value = date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })

    message.success('转换成功')
  } catch (e) {
    message.error('转换失败：请输入有效的时间戳')
    dateResult.value = ''
  }
}

const convertToTimestamp = () => {
  if (dateInput.value === null) {
    message.warning('请选择日期时间')
    return
  }

  try {
    const date = new Date(dateInput.value)
    timestampResult.value = {
      seconds: Math.floor(date.getTime() / 1000).toString(),
      milliseconds: date.getTime().toString()
    }
    message.success('转换成功')
  } catch (e) {
    message.error('转换失败')
    timestampResult.value = null
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}

onMounted(() => {
  updateCurrentTime()
  timer = window.setInterval(updateCurrentTime, 1000)
})

onUnmounted(() => {
  if (timer !== null) {
    clearInterval(timer)
  }
})
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
  max-width: 900px;
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

.timestamp-workspace {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.conversion-section {
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

.input-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-field {
  flex: 1;
}

.date-picker {
  flex: 1;
}

.input-hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.result-display {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 16px;
}

.result-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.result-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.result-value-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-value-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-item-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  min-width: 60px;
}

.result-item-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.current-time-section {
  background: rgba(156, 163, 175, 0.1);
  border: 1px solid rgba(156, 163, 175, 0.2);
  border-radius: 12px;
  padding: 20px;
}

.current-time-display {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.current-time-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-time-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  min-width: 120px;
}

.current-time-value {
  flex: 1;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .tool-main-card {
    width: 95%;
    padding: 24px;
  }

  .input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .current-time-label {
    min-width: auto;
  }
}

/* 覆盖 primary 按钮颜色 - 改为浅色背景深色边框 */
.input-group :deep(.n-button--primary) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.input-group :deep(.n-button--primary:hover) {
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

/* 覆盖 text 按钮样式 */
.current-time-item :deep(.n-button--text) {
  color: #6b7280 !important;
}

.current-time-item :deep(.n-button--text:hover) {
  color: #4b5563 !important;
}
</style>
