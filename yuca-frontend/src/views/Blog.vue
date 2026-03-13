<template>
  <div class="test-container">
    <div class="test-card">
      <h1>SSE打字机效果测试</h1>
      <p class="subtitle">测试后端/chat接口的流式输出</p>

      <!-- 测试按钮区域 -->
      <div class="button-area">
        <input
          v-model="testMessage"
          type="text"
          placeholder="输入测试消息..."
          class="test-input"
          @keyup.enter="createSessionAndTest"
          :disabled="loading"
        >
        <button @click="createSessionAndTest" :disabled="loading" class="btn-primary">
          {{ loading ? '测试中...' : '开始测试' }}
        </button>
        <button @click="clearMessages" class="btn-secondary">清空消息</button>
      </div>

      <!-- 日志区域 -->
      <div class="log-section">
        <div class="section-header">
          <h3>实时日志</h3>
          <button @click="copyLogs" class="copy-btn" :disabled="logs.length === 0">
            📋 复制全部日志
          </button>
        </div>
        <div class="log-container" ref="logContainer">
          <div v-for="(log, index) in logs" :key="index" class="log-entry" :class="log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>

      <!-- 对话区域 -->
      <div class="chat-section">
        <h3>对话内容</h3>
        <div class="chat-list" ref="chatListRef">
          <div
            v-for="(item, index) in chatList"
            :key="index"
            class="chat-item"
            :class="{ 'user-message': item.isUser }"
          >
            <div class="avatar">{{ item.isUser ? '用户' : 'AI' }}</div>
            <div class="message">{{ item.content }}</div>
          </div>
        </div>
      </div>

      <!-- 打字机状态 -->
      <div class="status-section">
        <h3>打字机状态</h3>
        <div class="status-info">
          <div class="status-item">
            <span class="label">是否正在打字:</span>
            <span class="value">{{ isTyping ? '是' : '否' }}</span>
          </div>
          <div class="status-item">
            <span class="label">队列长度:</span>
            <span class="value">{{ typingQueue.length }}</span>
          </div>
          <div class="status-item">
            <span class="label">当前消息ID:</span>
            <span class="value">{{ currentMessageId || '无' }}</span>
          </div>
          <div class="status-item">
            <span class="label">接收Token数:</span>
            <span class="value">{{ tokenCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, nextTick } from 'vue'
import { createSession as apiCreateSession } from '@/api/assistant'

// 对话列表数据结构
interface ChatItem {
  id?: number
  isUser: boolean
  content: string
}

// 日志数据结构
interface LogEntry {
  time: string
  message: string
  type: 'info' | 'success' | 'error' | 'warning'
}

// 状态
const chatList = ref<ChatItem[]>([])
const logs = ref<LogEntry[]>([])
const loading = ref(false)
const isTyping = ref(false)
const typingQueue = ref<string[]>([])
const currentMessageId = ref<number | null>(null)
const tokenCount = ref(0)
const testMessage = ref('你好，请用一句话介绍你自己') // 默认测试消息
const fullText = ref('') // 完整的流式接收文本

// Refs
const chatListRef = ref<HTMLElement>()
const logContainerRef = ref<HTMLElement>()
const typingTimer = ref<number | null>(null)

// 会话ID
let sessionId: number | null = null

/**
 * 添加日志
 */
const addLog = (message: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') => {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}.${now.getMilliseconds().toString().padStart(3, '0')}`

  logs.value.push({
    time,
    message,
    type
  })

  // 自动滚动到底部
  nextTick(() => {
    if (logContainerRef.value) {
      logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
    }
  })
}

/**
 * 清空消息
 */
const clearMessages = () => {
  chatList.value = []
  logs.value = []
  tokenCount.value = 0
  currentMessageId.value = null
  isTyping.value = false
  typingQueue.value = []
  addLog('消息已清空', 'info')
}

/**
 * 复制全部日志
 */
const copyLogs = () => {
  const logText = logs.value
    .map(log => `${log.time} ${log.message}`)
    .join('\n')

  navigator.clipboard.writeText(logText).then(() => {
    addLog('✓ 日志已复制到剪贴板', 'success')
  }).catch(err => {
    addLog(`✗ 复制失败: ${err}`, 'error')
  })
}

/**
 * 创建会话并测试
 */
const createSessionAndTest = async () => {
  try {
    loading.value = true
    addLog('========== 开始测试 ==========', 'info')

    // 1. 创建会话
    addLog('步骤1: 创建会话...', 'info')
    const session = await apiCreateSession({})
    sessionId = session.id
    addLog(`✓ 会话创建成功，ID: ${sessionId}`, 'success')

    // 2. 发送测试消息
    addLog('步骤2: 发送测试消息...', 'info')
    await sendTestMessage()

  } catch (error: any) {
    addLog(`✗ 测试失败: ${error.message}`, 'error')
    console.error('测试失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 发送测试消息
 */
const sendTestMessage = async () => {
  if (!sessionId) {
    addLog('✗ 会话ID不存在', 'error')
    return
  }

  if (!testMessage.value.trim()) {
    addLog('✗ 请输入测试消息', 'error')
    return
  }

  const messageToSend = testMessage.value.trim()
  addLog(`发送消息: "${messageToSend}"`, 'info')

  // 添加用户消息
  chatList.value.push({
    isUser: true,
    content: messageToSend
  })
  scrollToBottom()

  // 创建AI消息占位
  const aiMessageId = Date.now()
  currentMessageId.value = aiMessageId
  chatList.value.push({
    id: aiMessageId,
    isUser: false,
    content: ''
  })
  scrollToBottom()

  // 重置状态
  tokenCount.value = 0
  typingQueue.value = []
  fullText.value = '' // 重置完整文本

  try {
    const token = localStorage.getItem('access_token')
    if (!token) {
      addLog('✗ 未找到access_token', 'error')
      return
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8500'
    const startTime = Date.now()

    addLog(`发起请求: ${baseUrl}/assistant/chat`, 'info')

    const response = await fetch(`${baseUrl}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId: sessionId,
        content: messageToSend
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    addLog(`✓ 连接建立成功，耗时: ${Date.now() - startTime}ms`, 'success')

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    // ✅ 显式指定UTF-8编码，确保emoji等Unicode字符正确解码
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let eventCount = 0
    let firstTokenTime = 0

    addLog('开始接收SSE事件...', 'info')

    // 启动打字机效果
    startTyping(aiMessageId)

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        const totalDuration = Date.now() - startTime
        addLog(`✓ Stream完成，总耗时: ${totalDuration}ms，共接收 ${eventCount} 个事件`, 'success')
        break
      }

      // 记录接收时间
      const receiveTime = Date.now() - startTime
      if (eventCount === 0) {
        firstTokenTime = receiveTime
      }

      addLog(`[SSE +${receiveTime}ms] 接收数据块，大小: ${value.length} 字节`, 'info')

      // 解码数据
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk

      addLog(`[SSE] 解码后内容长度: ${chunk.length}，缓冲区总长度: ${buffer.length}`, 'info')

      // 按行分割处理
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      addLog(`[SSE] 分割成 ${lines.length} 行`, 'info')

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]

        // 跳过空行和 event 行
        if (!line.trim() || line.startsWith('event:')) {
          continue
        }

        // 处理 data 行
        if (line.startsWith('data:')) {
          try {
            const jsonStr = line.slice(5).trim()
            if (!jsonStr) continue

            const parsedChunk = JSON.parse(jsonStr)
            eventCount++

            const tokenTime = Date.now() - startTime
            addLog(`[SSE +${tokenTime}ms] 事件#${eventCount} 类型: ${parsedChunk.type}, 内容: "${parsedChunk.content || ''}"`, 'info')

            if (parsedChunk.type === 'token' && parsedChunk.content) {
              // 累加到完整文本
              fullText.value += parsedChunk.content
              tokenCount.value += parsedChunk.content.length

              // 启动打字机效果
              startTyping(aiMessageId)

              addLog(`[打字机] 收到token: "${parsedChunk.content}", 完整文本长度: ${fullText.value.length}`, 'info')
            } else if (parsedChunk.type === 'done') {
              addLog('✓ 收到done事件', 'success')
            } else if (parsedChunk.type === 'error') {
              addLog(`✗ 收到error事件: ${parsedChunk.message}`, 'error')
            }
          } catch (e) {
            addLog(`✗ 解析SSE数据失败: ${e}`, 'error')
            console.error('解析失败:', e, 'Line:', line)
          }
        }
      }
    }

    reader.releaseLock()

    // 等待打字队列清空
    addLog(`等待打字队列清空，当前队列长度: ${typingQueue.value.length}`, 'info')

  } catch (error: any) {
    addLog(`✗ 发送消息失败: ${error.message}`, 'error')
    console.error('发送消息失败:', error)
  }
}

/**
 * 启动打字机效果
 * 参考实现：使用两个变量，fullText存储完整文本，显示时截取前N个字符
 * 修复：使用Array.from正确处理emoji等Unicode字符
 */
const startTyping = (targetMessageId: number) => {
  // 清除之前的定时器，避免重复执行
  if (typingTimer.value !== null) {
    clearTimeout(typingTimer.value)
  }

  const messages = chatList.value
  const aiMsgIndex = messages.findIndex(m => m.id === targetMessageId)

  if (aiMsgIndex === -1) {
    addLog('[打字机] ✗ 未找到目标消息', 'error')
    return
  }

  // ✅ 使用Array.from正确分割Unicode字符（处理emoji、代理对等）
  const fullChars = Array.from(fullText.value)
  const displayChars = Array.from(messages[aiMsgIndex]!.content)

  if (displayChars.length < fullChars.length) {
    // 每次增加一个字符（按完整的Unicode字符）
    messages[aiMsgIndex]!.content = fullChars.slice(0, displayChars.length + 1).join('')

    addLog(`[打字机] 显示字符 #${displayChars.length + 1}: "${fullChars[displayChars.length]}"，进度: ${displayChars.length + 1}/${fullChars.length}`, 'info')

    // 控制打字速度（30ms/字符）
    const delay = 30
    typingTimer.value = window.setTimeout(() => startTyping(targetMessageId), delay)

    scrollToBottom()
  } else {
    // 打字结束 - 一次性设置完整文本，确保emoji正确显示
    messages[aiMsgIndex]!.content = fullText.value

    isTyping.value = false
    currentMessageId.value = null
    typingTimer.value = null
    addLog(`[打字机] ✓ 打字完成，共显示 ${fullChars.length} 个字符`, 'success')
  }
}

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (chatListRef.value) {
      chatListRef.value.scrollTop = chatListRef.value.scrollHeight
    }
  })
}

/**
 * 组件销毁前清理
 */
onBeforeUnmount(() => {
  if (typingTimer.value !== null) {
    clearTimeout(typingTimer.value)
  }
})
</script>

<style scoped>
.test-container {
  min-height: 100vh;
  padding: 40px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
}

.test-card {
  max-width: 1200px;
  width: 100%;
  background: rgba(245, 245, 245, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

h1 {
  font-size: 28px;
  color: #1f2937;
  margin-bottom: 8px;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 24px;
}

.button-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: center;
}

.test-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.test-input:focus {
  border-color: #3b82f6;
}

.test-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary, .btn-secondary {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover {
  background-color: #d1d5db;
}

.log-section, .chat-section, .status-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.log-section h3, .chat-section h3, .status-section h3 {
  font-size: 18px;
  color: #1f2937;
  margin: 0;
}

.copy-btn {
  padding: 6px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover:not(:disabled) {
  background: #2563eb;
}

.copy-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.log-container {
  height: 300px;
  overflow-y: auto;
  background: #1f2937;
  border-radius: 8px;
  padding: 16px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.log-entry {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.log-time {
  color: #9ca3af;
  flex-shrink: 0;
}

.log-message {
  flex: 1;
}

.log-entry.info .log-message {
  color: #e5e7eb;
}

.log-entry.success .log-message {
  color: #34d399;
}

.log-entry.error .log-message {
  color: #f87171;
}

.log-entry.warning .log-message {
  color: #fbbf24;
}

.chat-list {
  height: 300px;
  overflow-y: auto;
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.chat-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.chat-item.user-message {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  flex-shrink: 0;
}

.user-message .avatar {
  background: #3b82f6;
  color: white;
}

.message {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f3f4f6;
  word-break: break-word;
  white-space: pre-wrap;
}

.user-message .message {
  background: #3b82f6;
  color: white;
}

.status-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.status-item {
  display: flex;
  gap: 8px;
}

.label {
  font-weight: 500;
  color: #6b7280;
}

.value {
  color: #1f2937;
  font-family: 'Consolas', 'Monaco', monospace;
}

/* 滚动条样式 */
.log-container::-webkit-scrollbar,
.chat-list::-webkit-scrollbar {
  width: 8px;
}

.log-container::-webkit-scrollbar-track,
.chat-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.log-container::-webkit-scrollbar-thumb,
.chat-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

.log-container::-webkit-scrollbar-thumb:hover,
.chat-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
