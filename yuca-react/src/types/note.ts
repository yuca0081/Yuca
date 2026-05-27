export interface NoteBook {
  id: number
  userId: number
  name: string
  description?: string
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
  sortOrder: number
  content?: string
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  isTemplate?: boolean
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
  color?: string
}

export interface CreateItemRequest {
  bookId: number
  parentId: number | null
  type: 'FOLDER' | 'DOCUMENT'
  title: string
  content?: string
}

export interface UpdateItemRequest {
  title?: string
  content?: string
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

export interface UpdateNoteBookRequest {
  name?: string
  description?: string
  color?: string
}

export interface NoteTreeResponse {
  id: number
  name: string
  nodes: TreeNode[]
}
