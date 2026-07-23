import type { PageResponse } from './api'

export interface KnowledgeBase {
  id: number
  userId: number
  name: string
  description?: string
  fileFormat: string[]
  docCount?: number
  createdAt: string
  updatedAt: string
  isFrequentlyUsed?: boolean
  isOwner?: boolean
  lastAccessTime?: string
}

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

export interface SearchResult {
  chunkId: number
  docId: number
  docName: string
  content: string
  similarity: number
  chunkIndex: number
}

export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
  fileFormat?: string[]
}

export interface UpdateKnowledgeBaseRequest {
  name?: string
  description?: string
}

export interface KnowledgeChunk {
  id: number
  docId: number
  kbId: number
  content: string
  chunkIndex: number
  isActive: boolean
  createdAt: string
  updatedAt: string
  // 章节树字段（md 文档有标题层级时填充，非 md 平切片为 null）
  title?: string | null
  headingLevel?: number | null
  breadcrumb?: string | null
  parentId?: number | null
  summary?: string | null
  lineStart?: number | null
  lineEnd?: number | null
}

export type { PageResponse }
