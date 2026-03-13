<template>
  <div class="chat-container">
    <!-- 对话列表容器 -->
    <div class="chat-list" ref="chatListRef">
      <div
        v-for="(item, index) in chatList"
        :key="index"
        class="chat-item"
        :class="{ 'user-message': item.isUser }"
      >
        <div class="avatar">{{ item.isUser ? '我' : 'Bot' }}</div>
        <div class="message">{{ item.content }}</div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <input
        v-model="userInput"
        type="text"
        placeholder="输入消息..."
        @keyup.enter="sendMessage"
      >
      <button @click="sendMessage">发送</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, nextTick } from 'vue'

// 对话列表数据结构
interface ChatItem {
  isUser: boolean
  content: string
}

const chatList = ref<ChatItem[]>([])
const userInput = ref('')
const typingTimer = ref<number | null>(null)
const chatListRef = ref<HTMLElement>()

// 发送用户消息
const sendMessage = () => {
  if (!userInput.value.trim()) return

  // 添加用户消息
  chatList.value.push({
    isUser: true,
    content: userInput.value
  })

  const userQuestion = userInput.value
  userInput.value = ''

  // 模拟Bot回复（延迟500ms后开始）
  setTimeout(() => {
    // 添加Bot消息占位（空内容）
    chatList.value.push({
      isUser: false,
      content: ''
    })

    // 模拟Bot回复内容（将回复内容拆分成字符数组）
    const reply = `我收到了你的消息："${userQuestion}"。这是一个打字机效果的演示示例。你可以看到文字逐字符显示，就像有人正在实时打字一样。这个效果使用Vue 3的Composition API实现，核心逻辑是通过递归的setTimeout来控制每个字符的显示时间。`

    const replyChars = reply.split('')

    // 获取最后一条消息的索引（Bot消息）
    const targetIndex = chatList.value.length - 1
    startTyping(targetIndex, replyChars)
  }, 500)
}

// 核心：打字效果实现
const startTyping = (targetIndex: number, chars: string[]) => {
  let currentIndex = 0 // 当前要显示的字符索引

  // 清除可能存在的旧定时器（避免冲突）
  if (typingTimer.value !== null) {
    clearTimeout(typingTimer.value)
  }

  // 递归函数：逐字符展示
  const typeNext = () => {
    if (currentIndex < chars.length) {
      // 拼接当前字符（更新对话内容）
      chatList.value[targetIndex].content += chars[currentIndex]
      currentIndex++

      // 继续定时调用（控制打字速度，50ms/字符）
      typingTimer.value = window.setTimeout(typeNext, 50)

      // 每次更新后滚动到底部
      scrollToBottom()
    } else {
      // 打字结束，清除定时器
      if (typingTimer.value !== null) {
        clearTimeout(typingTimer.value)
        typingTimer.value = null
      }
    }
  }

  // 启动打字
  typeNext()
}

// 自动滚动到最新消息
const scrollToBottom = () => {
  // 等待DOM更新后执行滚动（关键：确保内容已渲染）
  nextTick(() => {
    if (chatListRef.value) {
      chatListRef.value.scrollTop = chatListRef.value.scrollHeight
    }
  })
}

// 组件销毁前清除定时器，避免内存泄漏
onBeforeUnmount(() => {
  if (typingTimer.value !== null) {
    clearTimeout(typingTimer.value)
  }
})
</script>

<style scoped>
.chat-container {
  width: 600px;
  margin: 40px auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  background: white;
}

.chat-list {
  height: 500px;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9fafb;
}

.chat-item {
  display: flex;
  margin-bottom: 20px;
  max-width: 80%;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 区分用户和机器人消息的对齐方式 */
.chat-item:not(.user-message) {
  align-self: flex-start;
}

.chat-item.user-message {
  margin-left: auto;
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  margin-right: 12px;
  color: #374151;
  flex-shrink: 0;
}

.chat-item.user-message .avatar {
  margin-right: 0;
  margin-left: 12px;
  background-color: #3b82f6;
  color: white;
}

.message {
  padding: 12px 16px;
  border-radius: 18px;
  background-color: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.5;
}

.chat-item.user-message .message {
  background-color: #3b82f6;
  color: white;
}

.input-area {
  display: flex;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: white;
  gap: 12px;
}

.input-area input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  transition: border-color 0.2s;
}

.input-area input:focus {
  border-color: #3b82f6;
}

.input-area button {
  padding: 10px 24px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.input-area button:hover {
  background-color: #2563eb;
}

.input-area button:active {
  transform: scale(0.98);
}

/* 滚动条样式 */
.chat-list::-webkit-scrollbar {
  width: 6px;
}

.chat-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.chat-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.chat-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
