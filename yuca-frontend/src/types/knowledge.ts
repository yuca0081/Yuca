/**
 * 知识库模块类型定义
 */

/**
 * 知识库
 */
export interface KnowledgeBase {
  id: number
  userId: number
  name: string
  description?: string
  fileFormat: string[]
  docCount?: number
  createdAt: string
  updatedAt: string
  // UI 扩展字段
  isFrequentlyUsed?: boolean // 是否常用（最近访问）
  isOwner?: boolean // 是否是所有者（用于 Tab 切换）
  lastAccessTime?: string // 最后访问时间
}

/**
 * 文档
 */
export interface KnowledgeDoc {
  id: number
  kbId: number
  kbName?: string
  docName: string
  docFormat: string
  docSize: number
  filePath?: string
  chunkCount: number
  createdAt: string
}

/**
 * 文档切片
 */
export interface KnowledgeChunk {
  id: number
  docId: number
  kbId: number
  chunkIndex: number
  content: string
  embedding?: number[]
  isActive: boolean
  createdAt: string
  updatedAt: string
}

/**
 * 搜索结果
 */
export interface SearchResult {
  chunkId: number
  docId: number
  docName: string
  content: string
  similarity: number
  chunkIndex: number
}

/**
 * 创建知识库请求
 */
export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
  fileFormat?: string[]
}

/**
 * 更新知识库请求
 */
export interface UpdateKnowledgeBaseRequest {
  id: number
  name?: string
  description?: string
  fileFormat?: string
}

/**
 * 语义搜索请求
 */
export interface SemanticSearchRequest {
  kbId: number
  query: string
  topK?: number
  threshold?: number
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
