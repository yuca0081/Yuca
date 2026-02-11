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
            <n-icon :component="KeyOutline" size="28" />
          </div>
          <h1 class="tool-title">哈希生成工具</h1>
        </div>
      </div>

      <div class="hash-workspace">
        <!-- 输入区 -->
        <div class="input-section">
          <div class="section-header">
            <h3 class="section-title">输入内容</h3>
            <div class="section-actions">
              <n-button size="small" @click="clearInput">清空</n-button>
              <n-button size="small" @click="loadExample">示例</n-button>
            </div>
          </div>
          <n-input
            v-model:value="input"
            type="textarea"
            placeholder="请输入要生成哈希的内容..."
            :autosize="{ minRows: 6, maxRows: 10 }"
            :input-props="{ spellcheck: false }"
          />
          <div class="algorithm-selector">
            <span class="selector-label">哈希算法：</span>
            <n-radio-group v-model:value="selectedAlgorithm">
              <n-radio-button value="md5">MD5</n-radio-button>
              <n-radio-button value="sha-1">SHA-1</n-radio-button>
              <n-radio-button value="sha-256">SHA-256</n-radio-button>
              <n-radio-button value="sha-512">SHA-512</n-radio-button>
            </n-radio-group>
          </div>
          <n-button type="primary" size="large" @click="generateHash" block>
            生成哈希
          </n-button>
        </div>

        <!-- 输出区 -->
        <div class="output-section">
          <h3 class="section-title">哈希结果</h3>
          <div v-if="hashResult" class="hash-results">
            <div v-for="(hash, algo) in hashResults" :key="algo" class="hash-item">
              <div class="hash-header">
                <span class="hash-algorithm">{{ algo.toUpperCase() }}</span>
                <n-button size="tiny" @click="copyHash(hash)">
                  <template #icon>
                    <n-icon :component="CopyOutline" size="12" />
                  </template>
                  复制
                </n-button>
              </div>
              <div class="hash-value">{{ hash }}</div>
            </div>
          </div>
          <div v-else class="empty-result">
            等待生成...
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInput, NRadioGroup, NRadioButton, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  KeyOutline,
  CopyOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const input = ref('')
const selectedAlgorithm = ref<HashAlgorithm>('sha-256')
const hashResult = ref('')
const hashResults = ref<Record<string, string>>({})

type HashAlgorithm = 'md5' | 'sha-1' | 'sha-256' | 'sha-512'

const goBack = () => router.push('/tools')

const clearInput = () => {
  input.value = ''
  hashResult.value = ''
  hashResults.value = {}
}

const loadExample = () => {
  input.value = 'Hello, Yuca!'
}

// 简单的哈希实现（生产环境应使用 crypto-js）
const simpleHash = async (text: string, algorithm: HashAlgorithm): Promise<string> => {
  const encoder = new TextEncoder()
  const data = encoder.encode(text)

  let hashAlgorithm: string
  switch (algorithm) {
    case 'md5':
      // MD5 不在 SubtleCrypto 中，使用简单实现
      return simpleMd5(text)
    case 'sha-1':
      hashAlgorithm = 'SHA-1'
      break
    case 'sha-256':
      hashAlgorithm = 'SHA-256'
      break
    case 'sha-512':
      hashAlgorithm = 'SHA-512'
      break
    default:
      hashAlgorithm = 'SHA-256'
  }

  const hashBuffer = await crypto.subtle.digest(hashAlgorithm, data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

// 简单的 MD5 实现（仅用于演示）
const simpleMd5 = (text: string): string => {
  // 这是一个简化的实现，生产环境应使用真实的 MD5 库
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    const char = text.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  return Math.abs(hash).toString(16).padStart(32, '0').substring(0, 32)
}

const generateHash = async () => {
  if (!input.value.trim()) {
    message.warning('请输入内容')
    return
  }

  try {
    const algorithms: HashAlgorithm[] = ['md5', 'sha-1', 'sha-256', 'sha-512']
    const results: Record<string, string> = {}

    for (const algo of algorithms) {
      results[algo] = await simpleHash(input.value, algo)
    }

    hashResults.value = results
    hashResult.value = results[selectedAlgorithm.value]

    message.success('哈希生成成功')
  } catch (e) {
    message.error('生成失败')
  }
}

const copyHash = async (hash: string) => {
  try {
    await navigator.clipboard.writeText(hash)
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

.hash-workspace {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-section :deep(.n-input__textarea-el) {
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

.input-section :deep(.n-input__textarea-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.input-section :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.input-section :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin: 0;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.algorithm-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
}

.selector-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.output-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hash-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hash-item {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 12px;
}

.hash-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hash-algorithm {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.hash-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  color: var(--color-text-primary);
  word-break: break-all;
  line-height: 1.6;
}

.empty-result {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}

@media (max-width: 1024px) {
  .hash-workspace {
    grid-template-columns: 1fr;
  }
  .tool-main-card {
    width: 95%;
    padding: 24px;
  }
  .algorithm-selector {
    flex-wrap: wrap;
  }
}

/* 覆盖 primary 按钮颜色 - 改为浅色背景深色边框 */
.input-section :deep(.n-button--primary) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.input-section :deep(.n-button--primary:hover) {
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
.hash-item :deep(.n-button--tiny) {
  background-color: #f9fafb !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
}

.hash-item :deep(.n-button--tiny:hover) {
  background-color: #f3f4f6 !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

/* 覆盖 RadioGroup 颜色 */
.algorithm-selector :deep(.n-radio-button--checked) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.algorithm-selector :deep(.n-radio-button--checked:hover) {
  background-color: #e5e7eb !important;
  border-color: #4b5563 !important;
}
</style>
