<template>
  <div class="message-list" ref="containerRef">
    <!-- 欢迎消息 -->
    <div v-if="messages.length === 0" class="welcome-message">
      <n-icon size="64" color="#666">
        <RobotIcon />
      </n-icon>
      <h2>你好！我是小助手</h2>
      <p>有什么可以帮助你的吗？</p>
    </div>

    <!-- 消息列表 -->
    <template v-else>
      <ChatMessage
        v-for="message in messages"
        :key="message.id"
        :message="message"
        :streaming="message.id === streamingMessageId"
      />
    </template>

    <!-- 加载指示器 -->
    <div v-if="loading" class="typing-indicator">
      <span></span>
      <span></span>
      <span></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { NIcon } from 'naive-ui'
import { ChatboxEllipses as RobotIcon } from '@vicons/ionicons5'
import type { Message } from '@/types/assistant'
import ChatMessage from './ChatMessage.vue'
import { useAssistantStore } from '@/stores/assistant'

interface Props {
  messages: Message[]
  loading: boolean
}

const props = defineProps<Props>()

const assistantStore = useAssistantStore()
const streamingMessageId = computed(() => assistantStore.streamingMessageId)

const containerRef = ref<HTMLElement>()

const lastMessage = computed(() => {
  return props.messages[props.messages.length - 1]
})

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (containerRef.value) {
      containerRef.value.scrollTop = containerRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(
  () => props.messages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)

// 监听 loading 状态，自动滚动
watch(() => props.loading, () => {
  scrollToBottom()
})
</script>

<style scoped>
.message-list {
  height: 100%;
  overflow-y: auto;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.welcome-message h2 {
  margin: 20px 0 10px;
  font-size: 24px;
  font-weight: 600;
}

.welcome-message p {
  font-size: 16px;
  color: #999;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  margin-left: 52px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}
</style>
