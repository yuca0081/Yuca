import request from './index'
import type {
  NoteBook,
  NoteItem,
  NoteTag,
  CreateNoteBookRequest,
  CreateItemRequest,
  UpdateItemRequest,
  MoveItemRequest
} from '@/types/note'

/**
 * 笔记本 API
 */

// 创建笔记本
export const createNoteBook = (data: CreateNoteBookRequest) =>
  request.post<number>('/note/books', data)

// 获取笔记本列表
export const getNoteBookList = () =>
  request.get<NoteBook[]>('/note/books')

// 获取笔记本详情
export const getNoteBook = (id: number) =>
  request.get<NoteBook>(`/note/books/${id}`)

// 更新笔记本
export const updateNoteBook = (id: number, data: Partial<CreateNoteBookRequest>) =>
  request.put<void>(`/note/books/${id}`, data)

// 删除笔记本
export const deleteNoteBook = (id: number) =>
  request.delete<void>(`/note/books/${id}`)

// 设置默认笔记本
export const setDefaultNoteBook = (id: number) =>
  request.post<void>(`/note/books/${id}/default`)

/**
 * 节点 API（单表统一管理）
 */

// 创建节点（文件夹或文档）
export const createItem = (data: CreateItemRequest) =>
  request.post<number>('/note/items', data)

// 获取节点详情
export const getItem = (id: number) =>
  request.get<NoteItem>(`/note/items/${id}`)

// 更新节点
export const updateItem = (id: number, data: UpdateItemRequest) =>
  request.put<void>(`/note/items/${id}`, data)

// 删除节点
export const deleteItem = (id: number) =>
  request.delete<void>(`/note/items/${id}`)

// 移动节点
export const moveItem = (id: number, data: MoveItemRequest) =>
  request.post<void>(`/note/items/${id}/move`, data)

// 获取笔记本完整树形结构
export const getNoteTree = (bookId: number) =>
  request.get<NoteItem[]>(`/note/items/books/${bookId}/tree`)

// 获取直接子节点列表
export const getChildren = (parentId: number | null, bookId: number) =>
  request.get<NoteItem[]>(`/note/items/${parentId || 'root'}/children`, {
    params: { bookId }
  })

// 获取最近编辑的文档
export const getRecentItems = (limit: number = 10) =>
  request.get<NoteItem[]>('/note/items/recent', { params: { limit } })

// 获取置顶文档
export const getPinnedItems = () =>
  request.get<NoteItem[]>('/note/items/pinned')

/**
 * 标签 API
 */

// 创建标签
export const createTag = (name: string, color?: string) =>
  request.post<number>('/note/tags', { name, color })

// 获取用户标签列表
export const getTagList = () =>
  request.get<NoteTag[]>('/note/tags')

// 更新标签
export const updateTag = (id: number, data: { name?: string; color?: string }) =>
  request.put<void>(`/note/tags/${id}`, data)

// 删除标签
export const deleteTag = (id: number) =>
  request.delete<void>(`/note/tags/${id}`)

// 给文档添加标签（后端期望 { tagIds: number[] }）
export const addTagToItem = (itemId: number, tagIds: number[]) =>
  request.post<void>(`/note/items/${itemId}/tags`, { tagIds })

// 移除文档标签
export const removeTagFromItem = (itemId: number, tagId: number) =>
  request.delete<void>(`/note/items/${itemId}/tags/${tagId}`)

// 获取文档的标签列表
export const getItemTags = (itemId: number) =>
  request.get<NoteTag[]>(`/note/items/${itemId}/tags`)

// 获取标签下的文档
export const getItemsByTag = (tagId: number) =>
  request.get<NoteItem[]>(`/note/tags/${tagId}/items`)
