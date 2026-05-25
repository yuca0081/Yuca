import request from './index'
import type { Session, SessionDetail, CreateSessionDto } from '@/types'

// Session CRUD
export const getSessions = (offset = 0, limit = 20) =>
  request.get<Session[]>('/assistant/sessions', { params: { offset, limit } })

export const createSession = (data?: CreateSessionDto) =>
  request.post<Session>('/assistant/session', data)

export const getSessionDetail = (id: number) =>
  request.get<SessionDetail>(`/assistant/session/${id}`)

export const deleteSession = (id: number) =>
  request.delete<void>(`/assistant/session/${id}`)

// SSE Chat streaming
export interface ChatSseParams {
  sessionId: number
  content: string
  modelName?: string
  enableThinking?: boolean
  enableSearch?: boolean
  onToken: (text: string) => void
  onThinking?: (text: string) => void
  onDone: (data: { messageId: number; fullMessage: string }) => void
  onError: (message: string) => void
}

export const chatStream = async (params: ChatSseParams): Promise<void> => {
  const token = localStorage.getItem('access_token')
  const body = {
    sessionId: params.sessionId,
    content: params.content,
    modelName: params.modelName,
    enableThinking: params.enableThinking,
    enableSearch: params.enableSearch,
  }

  const response = await fetch('/api/assistant/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    params.onError(`请求失败: ${response.status}`)
    return
  }

  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (!line.startsWith('data:')) continue
      const jsonStr = line.slice(5).trim()
      if (!jsonStr) continue

      try {
        const event = JSON.parse(jsonStr)
        switch (event.type) {
          case 'token':
            params.onToken(event.content)
            break
          case 'thinking':
            params.onThinking?.(event.content)
            break
          case 'done':
            params.onDone({
              messageId: event.messageId,
              fullMessage: event.fullMessage,
            })
            break
          case 'error':
            params.onError(event.message || '未知错误')
            break
        }
      } catch {
        // ignore malformed JSON
      }
    }
  }
}
