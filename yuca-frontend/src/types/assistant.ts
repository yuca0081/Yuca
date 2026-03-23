/**
 * 小助手模块类型定义
 */

// 会话信息（列表项）
export interface Session {
  id: number
  title: string | null
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
  modelName?: string       // 使用的模型名称（仅assistant角色有值）
  createdAt: string
  thinkingContent?: string  // 深度思考内容
  inputTokens?: number      // 输入token数
  outputTokens?: number     // 输出token数
  thinkingTokens?: number   // 深度思考token数
  totalTokens?: number      // 总token数
}

// 创建会话 DTO
export interface CreateSessionDto {
  modelName?: string
}

// 发送消息 DTO
export interface SendMessageDto {
  content: string
  modelName?: string       // 指定使用的模型（可选，如果不指定则使用会话默认模型）
  enableThinking?: boolean
  enableSearch?: boolean
}

// 流式数据块类型
export type StreamChunkType = 'start' | 'thinking' | 'token' | 'done' | 'error'

// 流式数据块
export interface StreamChunk {
  type: StreamChunkType
  content?: string        // token/thinking 事件的内容片段
  messageId?: number      // start/done 事件的消息ID
  fullMessage?: string    // done 事件的完整消息
  message?: string        // error 事件的错误信息
  inputTokens?: number    // done 事件的输入token数
  outputTokens?: number   // done 事件的输出token数
  totalTokens?: number    // done 事件的总token数
}

// 侧边栏会话操作类型
export type SessionActionType = 'delete'
