<template>
  <div class="chat-message" :class="`message-${message.role}`">
    <div class="message-avatar">
      <n-avatar v-if="message.role === 'user'" round size="small" :component="UserIcon" />
      <n-avatar v-else round size="small" color="#666" :component="RobotIcon" />
    </div>

    <div class="message-content">
      <div class="message-header">
        <span class="message-role">
          {{ message.role === 'user' ? '你' : '小助手' }}
        </span>
        <span class="message-time">
          {{ formatTime(message.createdAt) }}
        </span>
      </div>

      <div class="message-text">
        <!-- 深度思考内容 -->
        <div v-if="message.thinkingContent" class="thinking-content">
          <div class="thinking-header">
            <n-icon :component="SparklesIcon" size="14" />
            <span>深度思考</span>
          </div>
          <div class="thinking-text">{{ message.thinkingContent }}</div>
        </div>

        <!-- 流式输出时显示纯文本，避免频繁markdown渲染 -->
        <div v-if="message.role === 'assistant'" class="markdown-content">
          <div v-if="streaming" class="streaming-content">{{ message.content }}</div>
          <div v-else v-html="renderedMarkdown" />
        </div>
        <div v-else class="user-content">
          {{ message.content }}
        </div>

        <!-- Token使用统计 -->
        <div v-if="message.totalTokens" class="token-usage">
          <span class="token-item">输入: {{ message.inputTokens }}</span>
          <span class="token-item">输出: {{ message.outputTokens }}</span>
          <span class="token-item total">总计: {{ message.totalTokens }}</span>
        </div>
      </div>

      <!-- 消息操作 -->
      <div v-if="message.role === 'assistant' && message.content" class="message-actions">
        <n-button size="tiny" text @click="copyMessage">
          <template #icon>
            <n-icon :component="CopyIcon" />
          </template>
          复制
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NAvatar, NIcon, NButton } from 'naive-ui'
import {
  Person as UserIcon,
  ChatboxEllipses as RobotIcon,
  Copy as CopyIcon,
  Sparkles as SparklesIcon
} from '@vicons/ionicons5'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js'
import type { Message } from '@/types/assistant'

interface Props {
  message: Message
  streaming?: boolean
}

const props = defineProps<Props>()

// 配置 markdown-it
const md: any = markdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedMarkdown = computed(() => {
  if (!props.message.content) return ''
  return md.render(props.message.content)
})

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const copyMessage = () => {
  navigator.clipboard.writeText(props.message.content)
  // 可以添加复制成功的提示
}
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

/* 助手消息在左边 */
.message-assistant {
  flex-direction: row;
}

/* 用户消息在右边 */
.message-user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  max-width: 80%;
  overflow: hidden;
}

/* 用户消息的内容靠右 */
.message-user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-user .message-header {
  flex-direction: row-reverse;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.message-role {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-text {
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.markdown-content {
  color: #333;
}

.streaming-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #333;
  line-height: 1.6;
}

/* Markdown 样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-content :deep(p) {
  margin-bottom: 12px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-bottom: 12px;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin-bottom: 4px;
}

.markdown-content :deep(code) {
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.markdown-content :deep(pre) {
  background-color: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 12px;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: inherit;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #999;
  padding-left: 16px;
  margin: 12px 0;
  color: #666;
}

.markdown-content :deep(a) {
  color: #333;
  text-decoration: none;
  border-bottom: 1px solid #333;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 12px;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.user-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #333;
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-message:hover .message-actions {
  opacity: 1;
}

/* 深度思考内容样式 */
.thinking-content {
  background: #f3f4f6;
  border-left: 3px solid #9ca3af;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.thinking-text {
  color: #4b5563;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Token使用统计样式 */
.token-usage {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
  font-size: 12px;
  color: #6b7280;
  flex-wrap: wrap;
}

.token-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.token-item.total {
  color: #374151;
  font-weight: 600;
}
</style>

<!-- 引入 highlight.js 样式 -->
<style>
@import 'highlight.js/styles/github-dark.css';
</style>
