/**
 * 小助手模块状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '@/api/assistant'
import type { Session, Message } from '@/types/assistant'

export const useAssistantStore = defineStore('assistant', () => {
  // ========== 状态 ==========
  const sessions = ref<Session[]>([])
  const currentSessionId = ref<string>('')
  const messagesMap = ref<Map<string, Message[]>>(new Map())
  const isLoading = ref(false)

  // ========== 计算属性 ==========
  const currentSession = computed(() => {
    return sessions.value.find(s => s.id === currentSessionId.value) || null
  })

  const currentMessages = computed(() => {
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

      // 如果没有当前会话，选择第一个或创建新的
      if (!currentSessionId.value && sessions.value.length > 0) {
        currentSessionId.value = sessions.value[0].id
        await loadMessages(currentSessionId.value)
      } else if (sessions.value.length === 0) {
        await createSession()
      }
    } catch (error) {
      console.error('Failed to load sessions:', error)
      // 如果是开发环境且后端未实现，创建模拟数据
      if (import.meta.env.DEV) {
        await createMockSession()
      }
    }
  }

  /**
   * 创建新会话
   */
  const createSession = async (title = '新对话') => {
    try {
      const newSession = await api.createSession({ title })
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.id
      messagesMap.value.set(newSession.id, [])
    } catch (error) {
      console.error('Failed to create session:', error)
      // 开发环境下使用模拟数据
      if (import.meta.env.DEV) {
        const mockSession: Session = {
          id: `session-${Date.now()}`,
          title,
          createdAt: Date.now(),
          updatedAt: Date.now()
        }
        sessions.value.unshift(mockSession)
        currentSessionId.value = mockSession.id
        messagesMap.value.set(mockSession.id, [])
      }
    }
  }

  /**
   * 切换会话
   */
  const switchSession = async (sessionId: string) => {
    if (currentSessionId.value === sessionId) return

    currentSessionId.value = sessionId

    // 如果消息未加载，则加载
    if (!messagesMap.value.has(sessionId)) {
      await loadMessages(sessionId)
    }
  }

  /**
   * 加载会话消息
   */
  const loadMessages = async (sessionId: string) => {
    try {
      const messages = await api.getMessages(sessionId)
      messagesMap.value.set(sessionId, messages)
    } catch (error) {
      console.error('Failed to load messages:', error)
      // 开发环境下使用空数组
      if (import.meta.env.DEV && !messagesMap.value.has(sessionId)) {
        messagesMap.value.set(sessionId, [])
      }
    }
  }

  /**
   * 重命名会话
   */
  const renameSession = async (sessionId: string, title: string) => {
    try {
      const updated = await api.renameSession(sessionId, title)
      const index = sessions.value.findIndex(s => s.id === sessionId)
      if (index !== -1) {
        sessions.value[index] = updated
      }
    } catch (error) {
      console.error('Failed to rename session:', error)
      // 开发环境下直接更新
      if (import.meta.env.DEV) {
        const session = sessions.value.find(s => s.id === sessionId)
        if (session) {
          session.title = title
          session.updatedAt = Date.now()
        }
      }
    }
  }

  /**
   * 删除会话
   */
  const deleteSession = async (sessionId: string) => {
    try {
      await api.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      messagesMap.value.delete(sessionId)

      // 如果删除的是当前会话，切换到其他会话
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0].id)
        } else {
          await createSession()
        }
      }
    } catch (error) {
      console.error('Failed to delete session:', error)
      // 开发环境下直接删除
      if (import.meta.env.DEV) {
        sessions.value = sessions.value.filter(s => s.id !== sessionId)
        messagesMap.value.delete(sessionId)

        if (currentSessionId.value === sessionId) {
          if (sessions.value.length > 0) {
            await switchSession(sessions.value[0].id)
          } else {
            await createSession()
          }
        }
      }
    }
  }

  /**
   * 发送消息（流式）
   */
  const sendMessage = async (content: string) => {
    if (!currentSessionId.value || isLoading.value) return

    // 添加用户消息
    const userMessage: Message = {
      id: `msg-${Date.now()}-user`,
      sessionId: currentSessionId.value,
      role: 'user',
      content,
      timestamp: Date.now()
    }

    const messages = currentMessages.value
    messages.push(userMessage)

    // 创建 AI 消息占位
    const aiMessage: Message = {
      id: `msg-${Date.now()}-ai`,
      sessionId: currentSessionId.value,
      role: 'assistant',
      content: '',
      timestamp: Date.now()
    }
    messages.push(aiMessage)

    isLoading.value = true

    try {
      await api.sendMessage(
        currentSessionId.value,
        { content },
        // onChunk: 接收流式数据
        (chunk: string) => {
          aiMessage.content += chunk
        },
        // onComplete: 完成
        () => {
          isLoading.value = false
        },
        // onError: 错误
        (error: Error) => {
          isLoading.value = false
          aiMessage.content = '抱歉，出现了错误，请稍后重试。'
          console.error('Stream error:', error)

          // 开发环境下使用模拟响应
          if (import.meta.env.DEV) {
            simulateAIResponse(aiMessage)
          }
        }
      )
    } catch (error) {
      isLoading.value = false
      aiMessage.content = '抱歉，无法连接到服务器。'
      console.error('Send message error:', error)

      // 开发环境下使用模拟响应
      if (import.meta.env.DEV) {
        simulateAIResponse(aiMessage)
      }
    }
  }

  /**
   * 停止流式响应
   */
  const stopStreaming = () => {
    isLoading.value = false
  }

  // ========== 辅助函数 ==========

  /**
   * 创建模拟会话（开发环境）
   */
  const createMockSession = async () => {
    const mockSession: Session = {
      id: `session-${Date.now()}`,
      title: '新对话',
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
    sessions.value.unshift(mockSession)
    currentSessionId.value = mockSession.id
    messagesMap.value.set(mockSession.id, [])
  }

  /**
   * 模拟 AI 响应（开发环境）
   */
  const simulateAIResponse = (message: Message) => {
    const responses = [
      '你好！我是小助手，有什么可以帮助你的吗？',
      '这是一个很好的问题！让我来帮你解答。',
      '我理解你的需求，这里有一些建议...',
      '根据你的描述，我建议你可以尝试以下方法。',
      '这个问题涉及到多个方面，让我逐一为你分析。'
    ]

    const response = responses[Math.floor(Math.random() * responses.length)]

    // 模拟打字机效果
    let index = 0
    const interval = setInterval(() => {
      if (index < response.length) {
        message.content += response[index]
        index++
      } else {
        clearInterval(interval)
        isLoading.value = false
      }
    }, 50)
  }

  return {
    // 状态
    sessions,
    currentSessionId,
    currentSession,
    currentMessages,
    isLoading,

    // Actions
    loadSessions,
    createSession,
    switchSession,
    renameSession,
    deleteSession,
    sendMessage,
    stopStreaming
  }
})
