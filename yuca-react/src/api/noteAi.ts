import request from './index'

export type DocActionType = 'SUMMARIZE' | 'TRANSLATE' | 'POLISH' | 'EXPAND' | 'OUTLINE'

export interface NoteChatResponse {
  content: string
}

export interface DocActionResponse {
  result: string
  action: string
}

/**
 * 笔记 AI 对话
 */
export const noteAssistantChat = async (
  sessionId: string | undefined,
  content: string
): Promise<NoteChatResponse> => {
  return request.post('/note/assistant/chat', { sessionId, content })
}

/**
 * 文档一键操作
 */
export const noteDocAction = async (
  noteItemId: number,
  action: DocActionType,
  title: string,
  content: string,
  targetLanguage?: string
): Promise<DocActionResponse> => {
  return request.post('/note/assistant/doc-action', {
    noteItemId,
    action,
    title,
    content,
    targetLanguage,
  })
}
