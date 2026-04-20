import request from './index'
import type { NoteBook, NoteItem, CreateNoteBookRequest, CreateItemRequest, UpdateItemRequest, UpdateNoteBookRequest, NoteTreeResponse } from '@/types'

// Notebook CRUD
export const createNoteBook = (data: CreateNoteBookRequest) =>
  request.post<number>('/note/books', data)

export const getNoteBooks = () =>
  request.get<NoteBook[]>('/note/books')

export const getNoteBook = (id: number) =>
  request.get<NoteBook>(`/note/books/${id}`)

export const updateNoteBook = (id: number, data: UpdateNoteBookRequest) =>
  request.put<void>(`/note/books/${id}`, data)

export const deleteNoteBook = (id: number) =>
  request.delete<void>(`/note/books/${id}`)

export const setDefaultNoteBook = (id: number) =>
  request.post<void>(`/note/books/${id}/default`)

// Note Item CRUD
export const createNoteItem = (data: CreateItemRequest) =>
  request.post<number>('/note/items', data)

export const getNoteItem = (id: number) =>
  request.get<NoteItem>(`/note/items/${id}`)

export const updateNoteItem = (id: number, data: UpdateItemRequest) =>
  request.put<void>(`/note/items/${id}`, data)

export const deleteNoteItem = (id: number) =>
  request.delete<void>(`/note/items/${id}`)

// Tree
export const getNoteTree = (bookId: number) =>
  request.get<NoteTreeResponse>(`/note/items/books/${bookId}/tree`)

// Recent & Pinned
export const getRecentItems = (limit = 10) =>
  request.get<NoteItem[]>('/note/items/recent', { params: { limit } })

export const getPinnedItems = () =>
  request.get<NoteItem[]>('/note/items/pinned')
