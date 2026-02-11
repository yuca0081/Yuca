import request from './index'
import type {
  KnowledgeBase,
  KnowledgeDoc,
  KnowledgeChunk,
  SearchResult,
  CreateKnowledgeBaseRequest,
  UpdateKnowledgeBaseRequest,
  SemanticSearchRequest,
  PageResponse
} from '@/types/knowledge'

/**
 * 知识库API
 */

// 创建知识库
export const createKnowledgeBase = (data: CreateKnowledgeBaseRequest) =>
  request.post<number>('/knowledge-base/create', data)

// 更新知识库
export const updateKnowledgeBase = (id: number, data: Partial<UpdateKnowledgeBaseRequest>) =>
  request.post<void>(`/knowledge-base/${id}/update`, { id, ...data })

// 删除知识库
export const deleteKnowledgeBase = (id: number) =>
  request.post<void>(`/knowledge-base/${id}/delete`)

// 获取知识库详情
export const getKnowledgeBase = (id: number) =>
  request.get<KnowledgeBase>(`/knowledge-base/${id}`)

// 获取知识库列表
export const getKnowledgeBaseList = () =>
  request.get<KnowledgeBase[]>('/knowledge-base/list')

/**
 * 文档API
 */

// 上传文档
export const uploadDocument = (kbId: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', kbId.toString())
  return request.post<number>('/knowledge-doc/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 分页查询文档列表
export const getDocumentList = (kbId: number, current: number = 1, size: number = 10) =>
  request.get<PageResponse<KnowledgeDoc>>('/knowledge-doc/pageList', {
    params: { kbId, current, size }
  })

// 批量删除文档
export const deleteDocuments = (docIds: number[]) =>
  request.post<void>('/knowledge-doc/delete', docIds)

// 获取文档切片列表
export const getChunkList = (docId: number) =>
  request.get<KnowledgeChunk[]>(`/knowledge-doc/${docId}/chunks`)

/**
 * 搜索API
 */

// 语义搜索
export const semanticSearch = (data: SemanticSearchRequest) =>
  request.post<SearchResult[]>('/knowledge-search/semantic', data)

// 全局语义搜索
export const globalSearch = (query: string, topK: number = 5, threshold: number = 0.7) =>
  request.get<SearchResult[]>('/knowledge-search/global', {
    params: { query, topK, threshold }
  })
