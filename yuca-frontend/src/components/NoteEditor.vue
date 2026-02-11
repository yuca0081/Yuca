<template>
  <div class="note-editor-container">
    <!-- 编辑器头部 -->
    <div class="editor-header">
      <div class="header-left">
        <span class="doc-status">{{ statusText }}</span>
        <span v-if="saving" class="saving-indicator">保存中...</span>
        <span v-else-if="lastSavedAt" class="saved-indicator">
          已保存 {{ formatTime(lastSavedAt) }}
        </span>
      </div>
      <div class="header-right">
        <span class="word-count">{{ wordCount }} 字</span>
      </div>
    </div>

    <!-- 编辑器内容区 -->
    <div class="editor-content">
      <textarea
        ref="textareaRef"
        v-model="content"
        class="markdown-editor"
        placeholder="开始输入..."
        @input="handleInput"
      ></textarea>
      <!-- 预览区（可切换） -->
      <div v-if="showPreview" class="markdown-preview" v-html="previewHtml"></div>
    </div>

    <!-- 编辑器底部工具栏 -->
    <div class="editor-footer">
      <div class="toolbar">
        <n-button text size="small" @click="togglePreview">
          <template #icon>
            <n-icon :component="showPreview ? EyeOffOutline : EyeOutline" />
          </template>
          {{ showPreview ? '隐藏预览' : '预览' }}
        </n-button>
        <n-button text size="small" @click="handleSave">
          <template #icon>
            <n-icon :component="SaveOutline" />
          </template>
          保存
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { NButton, NIcon } from 'naive-ui'
import { EyeOutline, EyeOffOutline, SaveOutline } from '@vicons/ionicons5'

interface Props {
  modelValue: string
  itemId?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'save': [value: string]
}>()

const content = ref(props.modelValue || '')
const textareaRef = ref<HTMLTextAreaElement>()
const showPreview = ref(false)
const saving = ref(false)
const lastSavedAt = ref<Date | null>(null)
const autoSaveTimer = ref<number | null>(null)

// 计算字数
const wordCount = computed(() => {
  return content.value.length
})

// 状态文本
const statusText = computed(() => {
  return '草稿'
})

// 预览 HTML（简单的 Markdown 转 HTML）
const previewHtml = computed(() => {
  // 简单的 Markdown 转换（后续可以引入 marked.js）
  let html = content.value
  // 标题
  html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
  html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
  html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
  // 粗体
  html = html.replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
  // 斜体
  html = html.replace(/\*(.*?)\*/gim, '<em>$1</em>')
  // 代码块
  html = html.replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')
  // 链接
  html = html.replace(/\[(.*?)\]\((.*?)\)/gim, '<a href="$2" target="_blank">$1</a>')
  // 换行
  html = html.replace(/\n/gim, '<br>')
  return html
})

// 监听 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  if (newVal !== content.value) {
    content.value = newVal || ''
  }
})

// 输入处理
const handleInput = () => {
  emit('update:modelValue', content.value)
  scheduleAutoSave()
}

// 自动保存（防抖）
const scheduleAutoSave = () => {
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  autoSaveTimer.value = window.setTimeout(() => {
    handleSave()
  }, 2000)
}

// 手动保存
const handleSave = async () => {
  if (!content.value.trim()) return
  saving.value = true
  try {
    await emit('save', content.value)
    lastSavedAt.value = new Date()
  } finally {
    saving.value = false
  }
}

// 切换预览
const togglePreview = () => {
  showPreview.value = !showPreview.value
}

// 格式化时间
const formatTime = (date: Date): string => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)

  if (seconds < 60) return '刚刚'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return date.toLocaleTimeString()
}

onMounted(() => {
  // 自动聚焦
  textareaRef.value?.focus()
})
</script>

<style scoped>
.note-editor-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  overflow: hidden;
}

/* 编辑器头部 */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.3);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-status {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.saving-indicator {
  font-size: 12px;
  color: #f59e0b;
}

.saved-indicator {
  font-size: 12px;
  color: #10b981;
}

.word-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 编辑器内容区 */
.editor-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.markdown-editor {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-primary);
  background: transparent;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.markdown-editor::placeholder {
  color: var(--color-text-secondary);
}

.markdown-preview {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  border-left: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.3);
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-primary);
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}

.markdown-preview :deep(h1) {
  font-size: 24px;
}

.markdown-preview :deep(h2) {
  font-size: 20px;
}

.markdown-preview :deep(h3) {
  font-size: 18px;
}

.markdown-preview :deep(pre) {
  background: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-preview :deep(code) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
}

.markdown-preview :deep(a) {
  color: #14b8a6;
  text-decoration: none;
}

.markdown-preview :deep(a:hover) {
  text-decoration: underline;
}

/* 编辑器底部 */
.editor-footer {
  padding: 8px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.3);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
