export interface Session {
  id: number
  title: string | null
  createdAt: string
  updatedAt: string
  lastMessagePreview?: string
}

export interface SessionDetail extends Session {
  messages: Message[]
}

export type MessageRole = 'user' | 'assistant' | 'system'

export interface Message {
  id: number
  role: MessageRole
  content: string
  modelName?: string
  createdAt: string
  thinkingContent?: string
  inputTokens?: number
  outputTokens?: number
  thinkingTokens?: number
  totalTokens?: number
}

export interface CreateSessionDto {
  modelName?: string
}

export interface SendMessageDto {
  content: string
  modelName?: string
  enableThinking?: boolean
  enableSearch?: boolean
}

export type StreamChunkType = 'start' | 'thinking' | 'token' | 'done' | 'error'

export interface StreamChunk {
  type: StreamChunkType
  content?: string
  messageId?: number
  fullMessage?: string
  message?: string
  inputTokens?: number
  outputTokens?: number
  totalTokens?: number
}
