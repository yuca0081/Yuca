<template>
  <!-- 第1层：页面容器（暗色遮罩背景） -->
  <div class="tool-page-container">
    <!-- 返回按钮 -->
    <n-button class="back-btn" @click="goBack">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="16" />
      </template>
      <span>返回</span>
    </n-button>

    <!-- 主体卡片 -->
    <div class="tool-main-card">
      <!-- 标题栏 -->
      <div class="tool-header">
        <div class="tool-title-section">
          <div class="tool-icon">
            <n-icon :component="CodeWorkingOutline" size="28" />
          </div>
          <h1 class="tool-title">JSON 格式化工具</h1>
        </div>
      </div>

      <!-- 工作区 -->
      <div class="tool-workspace">
        <!-- 输入区 -->
        <div class="tool-input-area">
          <div class="area-header">
            <span class="area-title">输入</span>
            <div class="area-actions">
              <n-button size="small" @click="clearInput">清空</n-button>
              <n-button size="small" @click="loadExample">示例</n-button>
            </div>
          </div>
          <n-input
            v-model:value="input"
            type="textarea"
            class="area-content"
            placeholder="请输入 JSON 字符串..."
            :autosize="{ minRows: 15, maxRows: 25 }"
            :input-props="{ spellcheck: false }"
          />
          <div class="action-buttons">
            <n-button type="primary" @click="formatJson">格式化</n-button>
            <n-button @click="compressJson">压缩</n-button>
            <n-button @click="validateJson">验证</n-button>
          </div>
        </div>

        <!-- 输出区 -->
        <div class="tool-output-area">
          <div class="area-header">
            <span class="area-title">输出</span>
            <div class="area-actions">
              <n-button size="small" @click="copyOutput" :disabled="!output">
                <template #icon>
                  <n-icon :component="CopyOutline" size="14" />
                </template>
                复制
              </n-button>
            </div>
          </div>
          <div class="output-content">{{ output || '等待处理...' }}</div>
          <div v-if="errorMessage" class="error-message">
            <n-icon :component="AlertCircleOutline" size="16" />
            <span>{{ errorMessage }}</span>
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
  CodeWorkingOutline,
  CopyOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const input = ref('')
const output = ref('')
const errorMessage = ref('')

// 示例 JSON
const exampleJson = {
  name: 'Yuca',
  version: '1.0.0',
  features: ['知识库', '博客', '笔记'],
  config: { debug: true, maxUsers: 100 }
}

// 返回工具列表
const goBack = () => {
  router.push('/tools')
}

// 清空输入
const clearInput = () => {
  input.value = ''
  output.value = ''
  errorMessage.value = ''
}

// 加载示例
const loadExample = () => {
  input.value = JSON.stringify(exampleJson)
  errorMessage.value = ''
}

// 格式化 JSON
const formatJson = () => {
  try {
    const parsed = JSON.parse(input.value)
    output.value = JSON.stringify(parsed, null, 2)
    errorMessage.value = ''
    message.success('格式化成功')
  } catch (e) {
    errorMessage.value = `JSON 格式错误: ${(e as Error).message}`
    output.value = ''
  }
}

// 压缩 JSON
const compressJson = () => {
  try {
    const parsed = JSON.parse(input.value)
    output.value = JSON.stringify(parsed)
    errorMessage.value = ''
    message.success('压缩成功')
  } catch (e) {
    errorMessage.value = `JSON 格式错误: ${(e as Error).message}`
    output.value = ''
  }
}

// 验证 JSON
const validateJson = () => {
  try {
    JSON.parse(input.value)
    output.value = '✅ JSON 格式正确'
    errorMessage.value = ''
    message.success('验证通过')
  } catch (e) {
    errorMessage.value = `JSON 格式错误: ${(e as Error).message}`
    output.value = '❌ JSON 格式不正确'
  }
}

// 复制输出
const copyOutput = async () => {
  try {
    await navigator.clipboard.writeText(output.value)
    message.success('已复制到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}
</script>

<style scoped>
/* 第1层：页面容器 - 暗色遮罩背景 */
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

/* 返回按钮 */
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

/* 主体卡片 */
.tool-main-card {
  width: 90%;
  max-width: 1400px;
  min-height: 80vh;
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

/* 标题栏 */
.tool-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
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

/* 工作区 */
.tool-workspace {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: 500px;
}

.tool-input-area,
.tool-output-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 区域头部 */
.area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.area-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.area-actions {
  display: flex;
  gap: 8px;
}

/* 内容区 */
.area-content {
  flex: 1;
}

.area-content :deep(.n-input__textarea-el) {
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

.area-content :deep(.n-input__textarea-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.area-content :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.area-content :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
}

.output-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 16px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  min-height: 300px;
}

/* 错误信息 */
.error-message {
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

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .tool-workspace {
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
.area-actions :deep(.n-button) {
  background-color: #f9fafb !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

.area-actions :deep(.n-button:hover) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}
</style>
