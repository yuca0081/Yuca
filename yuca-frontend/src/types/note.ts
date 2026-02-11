/**
 * 笔记模块类型定义
 */

/**
 * 笔记本
 */
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
  // UI 扩展字段
  isFrequentlyUsed?: boolean  // 是否常用（最近访问）
  lastAccessTime?: string     // 最后访问时间
  recentDocs?: NoteItem[]     // 最近编辑的文档
}

/**
 * 笔记节点（单表设计：文件夹/文档统一管理）
 */
export interface NoteItem {
  id: number
  userId: number
  bookId: number
  parentId: number | null
  type: 'FOLDER' | 'DOCUMENT'
  // 通用字段
  title: string
  icon?: string
  sortOrder: number
  isPinned: boolean
  // 文档专用字段
  content?: string
  contentType?: 'MARKDOWN' | 'RICH_TEXT'
  summary?: string
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  isTemplate?: boolean
  viewCount?: number
  wordCount?: number
  // 文件夹统计字段
  childCount?: number
  // 时间字段
  createdAt: string
  updatedAt: string
  publishedAt?: string
  // UI 扩展字段
  children?: NoteItem[]           // 子节点列表（树形结构）
  isExpanded?: boolean            // 是否展开（UI 状态）
  isLoading?: boolean             // 是否正在加载子节点
  level?: number                  // 层级深度（用于缩进显示）
}

/**
 * 树形节点（带层级关系）
 */
export interface TreeNode extends NoteItem {
  children: TreeNode[]
  level: number
}

/**
 * 标签
 */
export interface NoteTag {
  id: number
  userId: number
  name: string
  color?: string
  useCount: number
  createdAt: string
}

/**
 * 版本历史
 */
export interface NoteVersion {
  id: number
  itemId: number
  versionNumber: number
  title: string
  content: string
  changeNote?: string
  createdAt: string
}

/**
 * 回收站项目
 */
export interface RecycleBinItem {
  id: number
  userId: number
  itemType: 'DOCUMENT' | 'FOLDER' | 'BOOK'
  itemId: number
  itemName: string
  parentId?: number
  extraData?: any
  deletedAt: string
  expireAt: string
}

/**
 * 创建笔记本请求
 */
export interface CreateNoteBookRequest {
  name: string
  description?: string
  icon?: string
  color?: string
}

/**
 * 创建节点请求
 */
export interface CreateItemRequest {
  bookId: number
  parentId: number | null
  type: 'FOLDER' | 'DOCUMENT'
  title: string
  content?: string
  contentType?: 'MARKDOWN' | 'RICH_TEXT'
}

/**
 * 更新节点请求
 */
export interface UpdateItemRequest {
  title?: string
  content?: string
  icon?: string
  isPinned?: boolean
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

/**
 * 移动节点请求
 */
export interface MoveItemRequest {
  newParentId: number | null
  newSortOrder: number
}

/**
 * 搜索请求
 */
export interface SearchRequest {
  keyword: string
  bookId?: number
  limit?: number
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
