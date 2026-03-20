<template>
  <div class="chat-input">
    <!-- 输入框 -->
    <div class="input-field-container">
      <textarea
        ref="inputRef"
        v-model="inputText"
        class="input-field"
        :placeholder="placeholder"
        :disabled="disabled"
        rows="1"
        @keydown="handleKeydown"
        @input="autoResize"
      ></textarea>
    </div>

    <!-- 按钮栏 -->
    <div class="input-actions-bar">
      <!-- 左侧功能按钮 -->
      <div class="input-left-buttons">
        <button class="feature-btn" @click="toggleDeepThinking" :class="{ active: deepThinkingEnabled }">
          <n-icon :component="SparklesOutline" size="16" />
          <span>深度思考</span>
        </button>
        <button class="feature-btn" @click="toggleWebSearch" :class="{ active: webSearchEnabled }">
          <n-icon :component="GlobeOutline" size="16" />
          <span>联网搜索</span>
          <n-icon :component="ChevronDownOutline" size="12" />
        </button>
        <button class="feature-btn" @click="toggleTools" :class="{ active: toolsEnabled }">
          <n-icon :component="ConstructOutline" size="16" />
          <span>工具</span>
          <n-icon :component="ChevronDownOutline" size="12" />
        </button>
      </div>

      <!-- 右侧操作按钮 -->
      <div class="input-right-buttons">
        <button v-if="loading" class="action-btn stop-btn" @click="$emit('stop')">
          <n-icon :component="StopCircleOutline" size="20" />
        </button>
        <button v-else class="action-btn add-btn" title="添加附件">
          <n-icon :component="AddOutline" size="20" />
        </button>
        <button
          class="action-btn send-btn"
          :class="{ disabled: !canSend }"
          :disabled="!canSend"
          @click="handleSend"
        >
          <n-icon :component="SendOutline" size="18" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { NIcon } from 'naive-ui'
import {
  SparklesOutline,
  GlobeOutline,
  ConstructOutline,
  ChevronDownOutline,
  AddOutline,
  SendOutline,
  StopCircleOutline
} from '@vicons/ionicons5'

interface Props {
  disabled: boolean
  loading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  send: [content: string, options: { deepThinking: boolean; webSearch: boolean }]
  stop: []
}>()

const inputRef = ref()
const inputText = ref('')

// 功能开关状态
const deepThinkingEnabled = ref(false)
const webSearchEnabled = ref(false)
const toolsEnabled = ref(false)

const placeholder = computed(() => {
  return props.loading ? '正在生成中...' : '输入你的问题...'
})

const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !props.disabled
})

// 切换功能
const toggleDeepThinking = () => {
  deepThinkingEnabled.value = !deepThinkingEnabled.value
}

const toggleWebSearch = () => {
  webSearchEnabled.value = !webSearchEnabled.value
}

const toggleTools = () => {
  toolsEnabled.value = !toolsEnabled.value
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!canSend.value) return
  emit('send', inputText.value.trim(), {
    deepThinking: deepThinkingEnabled.value,
    webSearch: webSearchEnabled.value
  })
  inputText.value = ''
}

// 自动调整输入框高度
const autoResize = () => {
  const textarea = inputRef.value as HTMLTextAreaElement
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }
}

const focus = () => {
  nextTick(() => {
    inputRef.value?.focus()
  })
}

defineExpose({
  focus
})
</script>

<style scoped>
.chat-input {
  position: relative;
  padding: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 输入框容器 */
.input-field-container {
  width: 100%;
  padding: 12px 14px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.input-field-container:hover {
  border-color: #d1d5db;
  background: #f5f6f7;
}

.input-field-container:focus-within {
  border-color: #333;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.08);
}

.input-field {
  width: 100%;
  min-height: 24px;
  max-height: 200px;
  padding: 0;
  background: transparent;
  border: none;
  outline: none;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  font-family: inherit;
}

.input-field::placeholder {
  color: #9ca3af;
}

.input-field:disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

/* 按钮栏 */
.input-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

/* 左侧功能按钮 */
.input-left-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.feature-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.feature-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #333;
}

.feature-btn.active {
  background: rgba(0, 0, 0, 0.1);
  color: #333;
}

.feature-btn span {
  line-height: 1;
}

/* 右侧操作按钮 */
.input-right-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #1f2937;
  transform: scale(1.05);
}

.action-btn:active {
  transform: scale(0.95);
}

.action-btn.send-btn {
  background: #333;
  border-color: #333;
  color: #fff;
}

.action-btn.send-btn:hover {
  background: #1f2937;
  border-color: #1f2937;
}

.action-btn.send-btn.disabled {
  background: #e5e7eb;
  border-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  transform: none;
}

.action-btn.stop-btn {
  background: #fef3c7;
  border-color: #fbbf24;
  color: #d97706;
}

.action-btn.stop-btn:hover {
  background: #fde68a;
  border-color: #f59e0b;
}
</style>
