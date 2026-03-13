/**
 * 小助手模块类型定义
 */

// 会话信息（列表项）
export interface Session {
  id: number
  title: string | null
  modelName: string
  createdAt: string
  updatedAt: string
  lastMessagePreview?: string
}

// 会话详情（含消息）
export interface SessionDetail extends Session {
  messages: Message[]
}

// 消息角色
export type MessageRole = 'user' | 'assistant' | 'system'

// 消息信息
export interface Message {
  id: number
  role: MessageRole
  content: string
  createdAt: string
}

// 创建会话 DTO
export interface CreateSessionDto {
  modelName?: string
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
  content?: string        // token 事件的内容片段
  messageId?: number      // start/done 事件的消息ID
  fullMessage?: string    // done 事件的完整消息
  message?: string        // error 事件的错误信息
}

// 侧边栏会话操作类型
export type SessionActionType = 'delete'
