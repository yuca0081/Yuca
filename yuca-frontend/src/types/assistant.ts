/**
 * 小助手模块类型定义
 */

// 会话信息
export interface Session {
  id: string
  title: string
  createdAt: number
  updatedAt: number
}

// 消息角色
export type MessageRole = 'user' | 'assistant' | 'system'

// 消息信息
export interface Message {
  id: string
  sessionId: string
  role: MessageRole
  content: string
  timestamp: number
}

// 创建会话 DTO
export interface CreateSessionDto {
  title?: string
}

// 发送消息 DTO
export interface SendMessageDto {
  content: string
}

// 流式数据块类型
export type StreamChunkType = 'start' | 'token' | 'done' | 'error'

// 流式数据块
export interface StreamChunk {
  type: StreamChunkType
  content?: string
  messageId?: string
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

// 侧边栏会话操作类型
export type SessionActionType = 'rename' | 'delete'
