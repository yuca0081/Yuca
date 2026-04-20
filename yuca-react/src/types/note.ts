export interface NoteBook {
  id: number
  userId: number
  name: string
  description?: string
  icon?: string
  sortOrder: number
  isDefault: boolean
  color?: string
  documentCount: number
  createdAt: string
  updatedAt: string
  isFrequentlyUsed?: boolean
  lastAccessTime?: string
  recentDocs?: NoteItem[]
}

export interface NoteItem {
  id: number
  userId: number
  bookId: number
  parentId: number | null
  type: 'FOLDER' | 'DOCUMENT'
  title: string
  icon?: string
  sortOrder: number
  isPinned: boolean
  content?: string
  contentType?: 'MARKDOWN' | 'RICH_TEXT'
  summary?: string
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  isTemplate?: boolean
  viewCount?: number
  wordCount?: number
  childCount?: number
  createdAt: string
  updatedAt: string
  publishedAt?: string
  children?: NoteItem[]
  isExpanded?: boolean
  isLoading?: boolean
  level?: number
}

export interface TreeNode extends NoteItem {
  children: TreeNode[]
  level: number
}

export interface CreateNoteBookRequest {
  name: string
  description?: string
  icon?: string
  color?: string
}

export interface CreateItemRequest {
  bookId: number
  parentId: number | null
  type: 'FOLDER' | 'DOCUMENT'
  title: string
  content?: string
  contentType?: 'MARKDOWN' | 'RICH_TEXT'
}

export interface UpdateItemRequest {
  title?: string
  content?: string
  icon?: string
  isPinned?: boolean
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

export interface UpdateNoteBookRequest {
  name?: string
  description?: string
  icon?: string
  color?: string
}

export interface NoteTreeResponse {
  id: number
  name: string
  nodes: TreeNode[]
}
