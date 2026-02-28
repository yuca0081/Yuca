<template>
  <div class="chat-area">
    <!-- 消息列表 -->
    <div class="chat-messages">
      <ChatMessageList :messages="messages" :loading="loading" />
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-container">
      <ChatInput
        :disabled="loading"
        :loading="loading"
        @send="handleSend"
        @stop="handleStop"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Session, Message } from '@/types/assistant'
import ChatMessageList from './ChatMessageList.vue'
import ChatInput from './ChatInput.vue'

interface Props {
  session: Session | null
  messages: Message[]
  loading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  sendMessage: [content: string]
  stopStreaming: []
}>()

const handleSend = (content: string) => {
  emit('sendMessage', content)
}

const handleStop = () => {
  emit('stopStreaming')
}
</script>

<style scoped>
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #ffffff;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.chat-input-container {
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
  background-color: #ffffff;
}
</style>
