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
            <n-icon :component="SearchOutline" size="28" />
          </div>
          <h1 class="tool-title">正则表达式测试工具</h1>
        </div>
      </div>

      <div class="regex-workspace">
        <!-- 正则表达式输入 -->
        <div class="regex-input-section">
          <div class="input-row">
            <div class="input-prefix">/</div>
            <n-input
              v-model:value="regexPattern"
              placeholder="正则表达式"
              size="large"
              class="pattern-input"
            />
            <div class="input-prefix">/</div>
            <n-input
              v-model:value="regexFlags"
              placeholder="gim"
              size="large"
              class="flags-input"
              maxlength="10"
            />
          </div>
          <div class="quick-patterns">
            <span class="quick-label">常用模式：</span>
            <n-button size="small" @click="setPattern('^\\d+$')">数字</n-button>
            <n-button size="small" @click="setPattern('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$')">邮箱</n-button>
            <n-button size="small" @click="setPattern('^1[3-9]\\d{9}$')">手机号</n-button>
            <n-button size="small" @click="setPattern('^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$')">URL</n-button>
          </div>
        </div>

        <!-- 测试文本输入 -->
        <div class="test-text-section">
          <div class="section-header">
            <span class="section-title">测试文本</span>
            <div class="section-actions">
              <n-button size="small" @click="clearAll">清空</n-button>
              <n-button size="small" @click="loadExample">示例</n-button>
            </div>
          </div>
          <n-input
            v-model:value="testText"
            type="textarea"
            placeholder="请输入要测试的文本..."
            :autosize="{ minRows: 8, maxRows: 15 }"
            :input-props="{ spellcheck: false }"
            @input="testRegex"
          />
        </div>

        <!-- 匹配结果 -->
        <div class="result-section">
          <div class="section-header">
            <span class="section-title">匹配结果</span>
            <n-tag v-if="matchCount !== null" :type="matchCount > 0 ? 'success' : 'warning'">
              {{ matchCount }} 个匹配
            </n-tag>
          </div>
          <div v-if="errorMessage" class="error-message">
            <n-icon :component="AlertCircleOutline" size="16" />
            <span>{{ errorMessage }}</span>
          </div>
          <div v-else-if="matches.length > 0" class="matches-list">
            <div v-for="(match, index) in matches" :key="index" class="match-item">
              <span class="match-index">{{ index + 1 }}.</span>
              <span class="match-value">{{ match }}</span>
              <n-button size="tiny" @click="copyMatch(match)">
                <template #icon>
                  <n-icon :component="CopyOutline" size="12" />
                </template>
              </n-button>
            </div>
          </div>
          <div v-else class="empty-result">
            {{ testText ? '未找到匹配项' : '等待输入...' }}
          </div>
        </div>

        <!-- 匹配详情 -->
        <div v-if="matchDetails.length > 0" class="details-section">
          <span class="section-title">匹配详情</span>
          <div v-for="(detail, index) in matchDetails" :key="index" class="detail-item">
            <div class="detail-header">匹配 {{ index + 1 }}</div>
            <div class="detail-content">
              <span class="detail-label">完整匹配：</span>
              <span class="detail-value">{{ detail.match }}</span>
            </div>
            <div v-if="detail.groups && detail.groups.length > 0" class="detail-groups">
              <div v-for="(group, gIndex) in detail.groups" :key="gIndex" class="group-item">
                <span class="group-label">捕获组 {{ gIndex + 1 }}：</span>
                <span class="group-value">{{ group }}</span>
              </div>
            </div>
            <div class="detail-position">
              <span class="detail-label">位置：</span>
              <span class="detail-value">{{ detail.index }} - {{ detail.index + detail.match.length - 1 }}</span>
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
import { NButton, NIcon, NInput, NTag, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  SearchOutline,
  CopyOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const regexPattern = ref('')
const regexFlags = ref('g')
const testText = ref('')
const matches = ref<string[]>([])
const matchCount = ref<number | null>(null)
const errorMessage = ref('')
const matchDetails = ref<Array<{ match: string; index: number; groups?: string[] }>>([])

const goBack = () => router.push('/tools')

const clearAll = () => {
  regexPattern.value = ''
  regexFlags.value = 'g'
  testText.value = ''
  matches.value = []
  matchCount.value = null
  errorMessage.value = ''
  matchDetails.value = []
}

const loadExample = () => {
  regexPattern.value = '\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b'
  regexFlags.value = 'gi'
  testText.value = '联系我们：support@example.com 或 sales@test.org\n无效邮箱：invalid@, @test.com, test@test'
  testRegex()
}

const setPattern = (pattern: string) => {
  regexPattern.value = pattern
}

const testRegex = () => {
  if (!regexPattern.value) {
    matches.value = []
    matchCount.value = null
    matchDetails.value = []
    errorMessage.value = ''
    return
  }

  try {
    const regex = new RegExp(regexPattern.value, regexFlags.value)
    errorMessage.value = ''

    if (regexFlags.value.includes('g')) {
      // 全局匹配
      const found = testText.value.match(regex)
      matches.value = found || []
      matchCount.value = matches.value.length

      // 获取详细匹配信息
      matchDetails.value = []
      let match
      const regexWithoutG = new RegExp(regexPattern.value, regexFlags.value.replace('g', ''))
      while ((match = regexWithoutG.exec(testText.value)) !== null) {
        matchDetails.value.push({
          match: match[0],
          index: match.index,
          groups: match.slice(1)
        })
        if (!matchFlags.value.includes('g')) break
      }
    } else {
      // 单次匹配
      const match = testText.value.match(regex)
      matches.value = match ? [match[0]] : []
      matchCount.value = matches.value.length

      matchDetails.value = []
      if (match) {
        matchDetails.value.push({
          match: match[0],
          index: match.index || 0,
          groups: match.slice(1)
        })
      }
    }
  } catch (e) {
    errorMessage.value = `正则表达式错误: ${(e as Error).message}`
    matches.value = []
    matchCount.value = null
    matchDetails.value = []
  }
}

const copyMatch = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
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

.regex-workspace {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.regex-input-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-prefix {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: 'Courier New', Consolas, monospace;
}

.pattern-input {
  flex: 1;
}

.flags-input {
  width: 100px;
  font-family: 'Courier New', Consolas, monospace;
}

.quick-patterns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.test-text-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-text-section :deep(.n-input__textarea-el) {
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

.test-text-section :deep(.n-input__textarea-el:focus) {
  background: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.08) !important;
}

.test-text-section :deep(.n-input-wrapper) {
  padding: 0 !important;
}

.test-text-section :deep(.n-input-wrapper:focus-within) {
  box-shadow: none !important;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.section-actions {
  display: flex;
  gap: 8px;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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

.matches-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.match-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 6px;
}

.match-index {
  font-size: 12px;
  color: var(--color-text-secondary);
  min-width: 30px;
}

.match-value {
  flex: 1;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.empty-result {
  padding: 32px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
}

.details-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
}

.detail-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 6px;
}

.detail-header {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-content {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.detail-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  min-width: 80px;
}

.detail-value {
  flex: 1;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.detail-groups {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
  padding-left: 12px;
}

.group-item {
  display: flex;
  gap: 8px;
}

.group-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.group-value {
  flex: 1;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.detail-position {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .tool-main-card {
    width: 95%;
    padding: 24px;
  }

  .input-row {
    flex-wrap: wrap;
  }

  .pattern-input,
  .flags-input {
    width: 100%;
  }

  .quick-patterns {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* 覆盖 primary 按钮颜色 - 改为浅色背景深色边框 */
.quick-patterns :deep(.n-button--primary) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

.quick-patterns :deep(.n-button--primary:hover) {
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

.quick-patterns :deep(.n-button) {
  background-color: #f9fafb !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

.quick-patterns :deep(.n-button:hover) {
  background-color: #f3f4f6 !important;
  border-color: #6b7280 !important;
  color: #374151 !important;
}

/* 覆盖 tiny 按钮样式 */
.match-item :deep(.n-button--tiny) {
  background-color: #f9fafb !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
}

.match-item :deep(.n-button--tiny:hover) {
  background-color: #f3f4f6 !important;
  border-color: #9ca3af !important;
  color: #4b5563 !important;
}

/* 覆盖 Tag 颜色 */
.section-header :deep(.n-tag--success) {
  background-color: rgba(107, 114, 128, 0.1) !important;
  color: #6b7280 !important;
  border-color: rgba(107, 114, 128, 0.3) !important;
}

.section-header :deep(.n-tag--warning) {
  background-color: rgba(156, 163, 175, 0.1) !important;
  color: #9ca3af !important;
  border-color: rgba(156, 163, 175, 0.3) !important;
}
</style>
