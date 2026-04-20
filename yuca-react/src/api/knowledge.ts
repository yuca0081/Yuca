import request from './index'
import type { KnowledgeBase, KnowledgeDoc, KnowledgeChunk, CreateKnowledgeBaseRequest, UpdateKnowledgeBaseRequest, PageResponse } from '@/types'

// Knowledge Base CRUD
export const createKnowledgeBase = (data: CreateKnowledgeBaseRequest) =>
  request.post<number>('/knowledge-base/create', data)

export const getKnowledgeBases = () =>
  request.get<KnowledgeBase[]>('/knowledge-base/list')

export const getKnowledgeBase = (id: number) =>
  request.get<KnowledgeBase>(`/knowledge-base/${id}`)

export const updateKnowledgeBase = (id: number, data: UpdateKnowledgeBaseRequest) =>
  request.post<void>(`/knowledge-base/${id}/update`, data)

export const deleteKnowledgeBase = (id: number) =>
  request.post<void>(`/knowledge-base/${id}/delete`)

// Documents
export const uploadDocument = (kbId: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<number>(`/knowledge-doc/upload`, formData, {
    params: { kbId },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
  })
}

export const getDocPageList = (kbId: number, current = 1, size = 10) =>
  request.get<PageResponse<KnowledgeDoc>>('/knowledge-doc/pageList', {
    params: { kbId, current, size },
  })

export const deleteDocuments = (ids: number[]) =>
  request.post<void>('/knowledge-doc/delete', ids)

// Chunks
export const getDocChunks = (docId: number) =>
  request.get<KnowledgeChunk[]>(`/knowledge-doc/${docId}/chunks`)
