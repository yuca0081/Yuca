/**
 * 小助手模块 API 接口
 */
import request from './index'
import type {
  Session,
  Message,
  CreateSessionDto,
  SendMessageDto,
  StreamChunk
} from '@/types/assistant'

/**
 * 获取会话列表
 */
export const getSessions = () => {
  return request.get<Session[]>('/assistant/sessions')
}

/**
 * 创建新会话
 */
export const createSession = (data: CreateSessionDto = {}) => {
  return request.post<Session>('/assistant/sessions', data)
}

/**
 * 重命名会话
 */
export const renameSession = (sessionId: string, title: string) => {
  return request.put<Session>(`/assistant/sessions/${sessionId}`, { title })
}

/**
 * 删除会话
 */
export const deleteSession = (sessionId: string) => {
  return request.delete(`/assistant/sessions/${sessionId}`)
}

/**
 * 获取会话消息
 */
export const getMessages = (sessionId: string) => {
  return request.get<Message[]>(`/assistant/sessions/${sessionId}/messages`)
}

/**
 * 发送消息（流式）
 * @param sessionId 会话ID
 * @param data 消息内容
 * @param onChunk 接收数据块回调
 * @param onComplete 完成回调
 * @param onError 错误回调
 */
export const sendMessage = async (
  sessionId: string,
  data: SendMessageDto,
  onChunk: (chunk: string) => void,
  onComplete: () => void,
  onError: (error: Error) => void
) => {
  const token = localStorage.getItem('token')

  try {
    const response = await fetch(
      `/api/assistant/sessions/${sessionId}/chat`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          onComplete()
          break
        }

        // 解码数据
        buffer += decoder.decode(value, { stream: true })

        // 按行分割处理
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const jsonStr = line.slice(6).trim()
              if (!jsonStr) continue

              const chunk: StreamChunk = JSON.parse(jsonStr)

              if (chunk.type === 'token' && chunk.content) {
                onChunk(chunk.content)
              } else if (chunk.type === 'done') {
                onComplete()
              } else if (chunk.type === 'error') {
                onError(new Error(chunk.content || 'Stream error'))
              }
            } catch (e) {
              console.error('Parse SSE data error:', e)
            }
          }
        }
      }
    } finally {
      reader.releaseLock()
    }
  } catch (error) {
    onError(error as Error)
  }
}
