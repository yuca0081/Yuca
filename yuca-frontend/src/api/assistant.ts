/**
 * 小助手模块 API 接口
 */
import request from './index'
import type {
  Session,
  CreateSessionDto,
  SendMessageDto,
  StreamChunk,
  SessionDetail
} from '@/types/assistant'

/**
 * 获取会话列表
 * @param offset 偏移量
 * @param limit 每页数量
 */
export const getSessions = (offset = 0, limit = 20) => {
  return request.get<Session[]>('/assistant/sessions', {
    params: { offset, limit }
  })
}

/**
 * 创建新会话
 * @param data 创建会话参数
 */
export const createSession = (data: CreateSessionDto = {}) => {
  return request.post<Session>('/assistant/session', data)
}

/**
 * 获取会话详情（含消息）
 * @param sessionId 会话ID
 */
export const getSessionDetail = (sessionId: number) => {
  return request.get<SessionDetail>(`/assistant/session/${sessionId}`)
}

/**
 * 删除会话
 * @param sessionId 会话ID
 */
export const deleteSession = (sessionId: number) => {
  return request.delete(`/assistant/session/${sessionId}`)
}

/**
 * 发送消息（SSE流式）
 * @param sessionId 会话ID
 * @param data 消息内容
 * @param onMessage SSE消息回调
 * @param onComplete 完成回调
 * @param onError 错误回调
 */
export const sendMessage = async (
  sessionId: number,
  data: SendMessageDto,
  onMessage: (chunk: StreamChunk) => void,
  onComplete: () => void,
  onError: (error: Error) => void
) => {
  const token = localStorage.getItem('access_token')

  try {
    // 直接访问后端，绕过Vite proxy（SSE需要）
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8500'
    const response = await fetch(`${baseUrl}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId,
        content: data.content,
        modelName: data.modelName,
        enableThinking: data.enableThinking,
        enableSearch: data.enableSearch
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    // ✅ 显式指定UTF-8编码，确保emoji等Unicode字符正确解码
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let eventCount = 0
    const startTime = Date.now()

    try {
      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          console.log(`[SSE] Stream完成，总耗时: ${Date.now() - startTime}ms，共接收 ${eventCount} 个事件`)
          break
        }

        // 记录每次接收的时间戳
        const receiveTime = Date.now() - startTime
        console.log(`[SSE +${receiveTime}ms] 接收原始数据块，大小: ${value.length} 字节`)

        // 解码数据
        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk

        // 按行分割处理
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          // 跳过空行和 event 行
          if (!line.trim() || line.startsWith('event:')) {
            continue
          }

          // 处理 data 行（注意：data:后面可能没有空格）
          if (line.startsWith('data:')) {
            try {
              const jsonStr = line.slice(5).trim()  // "data:" 是5个字符
              if (!jsonStr) continue

              const parsedChunk: StreamChunk = JSON.parse(jsonStr)
              eventCount++
              console.log(`[SSE +${Date.now() - startTime}ms] 事件#${eventCount} 类型: ${parsedChunk.type}, 内容: "${parsedChunk.content || ''}"`)

              onMessage(parsedChunk)

              if (parsedChunk.type === 'done') {
                onComplete()
              } else if (parsedChunk.type === 'error') {
                onError(new Error(parsedChunk.message || 'Stream error'))
              }
            } catch (e) {
              console.error('解析SSE数据失败:', e, 'Line:', line)
            }
          }
        }
      }
    } finally {
      reader.releaseLock()
    }
  } catch (error) {
    console.error('❌ 发送消息失败:', error)
    onError(error as Error)
  }
}
