/**
 * 小助手模块状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed, nextTick } from 'vue'
import * as api from '@/api/assistant'
import type { Session, Message, StreamChunk } from '@/types/assistant'

export const useAssistantStore = defineStore('assistant', () => {
  // ========== 状态 ==========
  const sessions = ref<Session[]>([])
  const currentSessionId = ref<number | null>(null)
  const messagesMap = ref<Map<number, Message[]>>(new Map())
  const isLoading = ref(false)

  // 临时会话状态：表示用户新建了会话但还没发送消息（未调用接口创建）
  const isTempSession = ref(false)

  // 打字机效果相关状态
  const typingTimer = ref<number | null>(null)
  const typingQueue = ref<string[]>([])  // 待显示的字符队列（已弃用，保留用于兼容）
  const isTyping = ref(false)  // 是否正在打字中
  const streamingMessageId = ref<number | null>(null)  // 当前正在流式输出的消息ID
  const streamingFullText = ref('')  // 当前流式消息的完整文本

  // ========== 计算属性 ==========
  const currentSession = computed(() => {
    if (!currentSessionId.value) return null
    return sessions.value.find(s => s.id === currentSessionId.value) || null
  })

  const currentMessages = computed(() => {
    if (!currentSessionId.value) return []
    return messagesMap.value.get(currentSessionId.value) || []
  })

  // ========== Actions ==========

  /**
   * 加载会话列表
   */
  const loadSessions = async () => {
    try {
      const data = await api.getSessions()
      sessions.value = data

      // 如果没有当前会话，且有历史会话，选择第一个
      if (!currentSessionId.value && sessions.value.length > 0) {
        const firstSession = sessions.value[0]
        if (firstSession) {
          currentSessionId.value = firstSession.id
          await loadMessages(firstSession.id)
        }
      }
      // 如果没有历史会话，不自动创建，等待用户手动新建
    } catch (error) {
      console.error('Failed to load sessions:', error)
    }
  }

  /**
   * 创建新会话（调用后端接口）
   */
  const createSession = async () => {
    try {
      const newSession = await api.createSession({})
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.id
      isTempSession.value = false
      messagesMap.value.set(newSession.id, [])
      return newSession
    } catch (error) {
      console.error('Failed to create session:', error)
      throw error
    }
  }

  /**
   * 开始新会话（前端临时状态，不调用接口）
   */
  const startNewSession = () => {
    currentSessionId.value = null
    isTempSession.value = true
  }

  /**
   * 切换会话
   */
  const switchSession = async (sessionId: number) => {
    if (currentSessionId.value === sessionId) {
      return
    }

    currentSessionId.value = sessionId
    isTempSession.value = false

    // 总是重新加载消息，确保显示最新内容
    await loadMessages(sessionId)
  }

  /**
   * 加载会话消息
   */
  const loadMessages = async (sessionId: number) => {
    try {
      const sessionDetail = await api.getSessionDetail(sessionId)
      messagesMap.value.set(sessionId, sessionDetail.messages || [])
    } catch (error) {
      console.error('Failed to load messages:', error)
    }
  }

  /**
   * 删除会话
   */
  const deleteSession = async (sessionId: number) => {
    try {
      await api.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      messagesMap.value.delete(sessionId)

      // 如果删除的是当前会话，切换到其他会话或进入临时会话状态
      if (currentSessionId.value === sessionId) {
        const firstSession = sessions.value[0]
        if (firstSession) {
          await switchSession(firstSession.id)
        } else {
          // 没有其他会话了，进入临时会话状态
          startNewSession()
        }
      }
    } catch (error) {
      console.error('Failed to delete session:', error)
      throw error
    }
  }

  /**
   * 打字机效果：启动打字（从完整文本中截取显示）
   * 参考实现：使用两个变量，streamingFullText存储完整文本，显示时截取前N个字符
   * 修复：使用Array.from正确处理emoji等Unicode字符
   * @param targetMessageId 目标消息ID
   */
  const startTyping = (targetMessageId: number) => {
    // 清除之前的定时器，避免重复执行
    if (typingTimer.value !== null) {
      clearTimeout(typingTimer.value)
    }

    const messages = currentMessages.value
    const aiMsgIndex = messages.findIndex(m => m.id === targetMessageId)

    if (aiMsgIndex === -1) {
      console.error('[打字机] 未找到目标消息')
      return
    }

    // ✅ 使用Array.from正确分割Unicode字符（处理emoji、代理对等）
    const fullChars = Array.from(streamingFullText.value)
    const displayChars = Array.from(messages[aiMsgIndex]!.content)

    if (displayChars.length < fullChars.length) {
      // 设置打字状态
      if (!isTyping.value) {
        isTyping.value = true
        streamingMessageId.value = targetMessageId
      }

      // 每次增加一个字符（按完整的Unicode字符）
      messages[aiMsgIndex]!.content = fullChars.slice(0, displayChars.length + 1).join('')

      console.log(`[打字机] 显示字符 #${displayChars.length + 1}: "${fullChars[displayChars.length]}"，进度: ${displayChars.length + 1}/${fullChars.length}`)

      // 控制打字速度（30ms/字符）
      const delay = 30
      typingTimer.value = window.setTimeout(() => startTyping(targetMessageId), delay)
    } else {
      // 打字结束 - 一次性设置完整文本，确保emoji正确显示
      messages[aiMsgIndex]!.content = streamingFullText.value

      isTyping.value = false
      streamingMessageId.value = null
      typingTimer.value = null
      console.log(`[打字机] ✓ 打字完成，共显示 ${fullChars.length} 个字符`)
    }
  }

  /**
   * 打字机效果：将字符加入队列
   * @param chars 字符数组
   */
  const enqueueChars = (chars: string[]) => {
    typingQueue.value.push(...chars)

    // 如果当前没有在打字，启动打字
    if (!isTyping.value && typingQueue.value.length > 0) {
      // 需要知道目标消息ID，这里通过当前消息列表最后一条AI消息来获取
      const messages = currentMessages.value
      const lastAiMessage = messages.filter(m => m.role === 'assistant').pop()
      if (lastAiMessage) {
        startTyping(lastAiMessage.id)
      }
    }
  }

  /**
   * 打字机效果：停止打字
   */
  const stopTyping = () => {
    if (typingTimer.value !== null) {
      clearTimeout(typingTimer.value)
      typingTimer.value = null
    }
    isTyping.value = false
    typingQueue.value = []
  }

  /**
   * 发送消息（流式）
   */
  const sendMessage = async (content: string) => {
    if (isLoading.value) return

    // 如果是临时会话，先创建会话
    if (isTempSession.value) {
      try {
        await createSession()
      } catch (error) {
        console.error('Failed to create session before sending message:', error)
        return
      }
    }

    if (!currentSessionId.value) return

    // 添加用户消息
    const userMessage: Message = {
      id: Date.now(),
      role: 'user',
      content,
      createdAt: new Date().toISOString()
    }

    const messages = currentMessages.value
    messages.push(userMessage)

    // 创建 AI 消息占位
    const aiMessageId = Date.now() + 1
    const aiMessage: Message = {
      id: aiMessageId,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString()
    }
    messages.push(aiMessage)

    isLoading.value = true

    // 重置打字机状态
    streamingFullText.value = ''
    startTyping(aiMessageId)

    try {
      await api.sendMessage(
        currentSessionId.value,
        { content },
        // onMessage: SSE消息回调（累加到完整文本）
        (chunk: StreamChunk) => {
          if (chunk.type === 'token' && chunk.content) {
            // 累加到完整文本
            streamingFullText.value += chunk.content

            // 每次收到新token都触发打字机效果
            startTyping(aiMessageId)

            console.log(`[打字机] 收到token: "${chunk.content}", 完整文本长度: ${streamingFullText.value.length}`)
          }
        },
        // onComplete: 完成
        () => {
          isLoading.value = false
          // 等待打字队列清空
          console.log('[打字机] Stream完成，等待打字队列清空...')
        },
        // onError: 错误
        (error: Error) => {
          isLoading.value = false
          stopTyping()
          const aiMsgIndex = messages.findIndex(m => m.id === aiMessageId)
          if (aiMsgIndex !== -1) {
            messages[aiMsgIndex]!.content = '抱歉，出现了错误，请稍后重试。'
          }
          console.error('Stream error:', error)
        }
      )
    } catch (error) {
      isLoading.value = false
      stopTyping()
      const aiMsgIndex = messages.findIndex(m => m.id === aiMessageId)
      if (aiMsgIndex !== -1) {
        messages[aiMsgIndex]!.content = '抱歉，无法连接到服务器。'
      }
      console.error('Send message error:', error)
    }
  }

  /**
   * 停止流式响应
   */
  const stopStreaming = () => {
    isLoading.value = false
    stopTyping()
  }

  return {
    // 状态
    sessions,
    currentSessionId,
    currentSession,
    currentMessages,
    isLoading,
    isTempSession,
    streamingMessageId,

    // Actions
    loadSessions,
    createSession,
    startNewSession,
    switchSession,
    deleteSession,
    sendMessage,
    stopStreaming
  }
})
