import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { NoteBook, NoteItem, NoteTag, CreateNoteBookRequest, CreateItemRequest, UpdateItemRequest, MoveItemRequest } from '@/types/note'
import * as noteApi from '@/api/note'

export const useNoteStore = defineStore('note', () => {
  // ========== 状态 ==========
  const noteBooks = ref<NoteBook[]>([])
  const currentBookId = ref<number | null>(null)
  const noteTree = ref<NoteItem[]>([])
  const currentItem = ref<NoteItem | null>(null)
  const tags = ref<NoteTag[]>([])
  const recentItems = ref<NoteItem[]>([])
  const pinnedItems = ref<NoteItem[]>([])

  // 加载状态
  const loading = ref(false)
  const saving = ref(false)

  // ========== 计算属性 ==========
  const currentBook = computed(() =>
    noteBooks.value.find((b: NoteBook) => b.id === currentBookId.value)
  )

  // ========== 笔记本操作 ==========
  const loadNoteBooks = async () => {
    try {
      loading.value = true
      const data = await noteApi.getNoteBookList()
      noteBooks.value = data.map((book: NoteBook) => ({
        ...book,
        isFrequentlyUsed: false,
        lastAccessTime: book.updatedAt
      }))

      // 如果有默认笔记本，自动选中
      const defaultBook = data.find((b: NoteBook) => b.isDefault)
      if (defaultBook) {
        currentBookId.value = defaultBook.id
      } else if (data.length > 0 && !currentBookId.value && data[0]) {
        currentBookId.value = data[0]!.id
      }
    } catch (error) {
      console.error('加载笔记本列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const createNoteBook = async (data: CreateNoteBookRequest) => {
    try {
      const id = await noteApi.createNoteBook(data)
      await loadNoteBooks()
      return id
    } catch (error) {
      console.error('创建笔记本失败:', error)
      throw error
    }
  }

  const updateNoteBook = async (id: number, data: Partial<CreateNoteBookRequest>) => {
    try {
      await noteApi.updateNoteBook(id, data)
      await loadNoteBooks()
    } catch (error) {
      console.error('更新笔记本失败:', error)
      throw error
    }
  }

  const deleteNoteBook = async (id: number) => {
    try {
      await noteApi.deleteNoteBook(id)
      await loadNoteBooks()
      if (currentBookId.value === id) {
        currentBookId.value = noteBooks.value[0]?.id || null
      }
    } catch (error) {
      console.error('删除笔记本失败:', error)
      throw error
    }
  }

  // ========== 节点操作 ==========
  const loadNoteTree = async (bookId: number) => {
    try {
      loading.value = true
      const data = await noteApi.getNoteTree(bookId)
      noteTree.value = buildTree(data)
    } catch (error) {
      console.error('加载笔记树失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const loadItem = async (id: number) => {
    try {
      loading.value = true
      const data = await noteApi.getItem(id)
      currentItem.value = data
      return data
    } catch (error) {
      console.error('加载节点失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const createItem = async (data: CreateItemRequest) => {
    try {
      const id = await noteApi.createItem(data)
      if (currentBookId.value) {
        await loadNoteTree(currentBookId.value)
      }
      return id
    } catch (error) {
      console.error('创建节点失败:', error)
      throw error
    }
  }

  const updateItem = async (id: number, data: UpdateItemRequest) => {
    try {
      saving.value = true
      await noteApi.updateItem(id, data)
      // 更新当前编辑的文档
      if (currentItem.value && currentItem.value.id === id) {
        currentItem.value = { ...currentItem.value, ...data }
      }
      // 更新树中的节点
      updateTreeItem(noteTree.value, id, data)
    } catch (error) {
      console.error('更新节点失败:', error)
      throw error
    } finally {
      saving.value = false
    }
  }

  const deleteItem = async (id: number) => {
    try {
      await noteApi.deleteItem(id)
      if (currentBookId.value) {
        await loadNoteTree(currentBookId.value)
      }
      if (currentItem.value?.id === id) {
        currentItem.value = null
      }
    } catch (error) {
      console.error('删除节点失败:', error)
      throw error
    }
  }

  const moveItem = async (id: number, data: MoveItemRequest) => {
    try {
      await noteApi.moveItem(id, data)
      if (currentBookId.value) {
        await loadNoteTree(currentBookId.value)
      }
    } catch (error) {
      console.error('移动节点失败:', error)
      throw error
    }
  }

  const togglePin = async (id: number) => {
    try {
      // 获取当前节点的置顶状态
      const item = await loadItem(id)
      if (!item) return

      // 使用 updateItem 切换置顶状态
      await noteApi.updateItem(id, { isPinned: !item.isPinned })
      if (currentBookId.value) {
        await loadNoteTree(currentBookId.value)
      }
      await loadPinnedItems()
    } catch (error) {
      console.error('切换置顶失败:', error)
      throw error
    }
  }

  // ========== 标签操作 ==========
  const loadTags = async () => {
    try {
      const data = await noteApi.getTagList()
      tags.value = data
    } catch (error) {
      console.error('加载标签列表失败:', error)
      throw error
    }
  }

  const createTag = async (name: string, color?: string) => {
    try {
      await noteApi.createTag(name, color)
      await loadTags()
    } catch (error) {
      console.error('创建标签失败:', error)
      throw error
    }
  }

  const deleteTag = async (id: number) => {
    try {
      await noteApi.deleteTag(id)
      await loadTags()
    } catch (error) {
      console.error('删除标签失败:', error)
      throw error
    }
  }

  // ========== 最近/置顶 ==========
  const loadRecentItems = async (limit: number = 10) => {
    try {
      const data = await noteApi.getRecentItems(limit)
      recentItems.value = data
    } catch (error) {
      console.error('加载最近文档失败:', error)
      throw error
    }
  }

  const loadPinnedItems = async () => {
    try {
      const data = await noteApi.getPinnedItems()
      pinnedItems.value = data
    } catch (error) {
      console.error('加载置顶列表失败:', error)
      throw error
    }
  }

  // ========== 辅助函数 ==========

  // 构建树形结构
  const buildTree = (items: NoteItem[], parentId: number | null = null, level: number = 0): NoteItem[] => {
    return items
      .filter(item => item.parentId === parentId)
      .map(item => ({
        ...item,
        level,
        isExpanded: false,
        isLoading: false,
        children: buildTree(items, item.id, level + 1)
      }))
  }

  // 更新树中的节点
  const updateTreeItem = (items: NoteItem[], id: number, data: Partial<NoteItem>): boolean => {
    for (const item of items) {
      if (item.id === id) {
        Object.assign(item, data)
        return true
      }
      if (item.children && item.children.length > 0) {
        if (updateTreeItem(item.children, id, data)) {
          return true
        }
      }
    }
    return false
  }

  // 切换节点展开状态
  const toggleExpand = (itemId: number) => {
    const toggle = (items: NoteItem[]): boolean => {
      for (const item of items) {
        if (item.id === itemId) {
          item.isExpanded = !item.isExpanded
          return true
        }
        if (item.children && item.children.length > 0) {
          if (toggle(item.children)) {
            return true
          }
        }
      }
      return false
    }
    toggle(noteTree.value)
  }

  return {
    // 状态
    noteBooks,
    currentBookId,
    currentBook,
    noteTree,
    currentItem,
    tags,
    recentItems,
    pinnedItems,
    loading,
    saving,
    // 笔记本操作
    loadNoteBooks,
    createNoteBook,
    updateNoteBook,
    deleteNoteBook,
    // 节点操作
    loadNoteTree,
    loadItem,
    createItem,
    updateItem,
    deleteItem,
    moveItem,
    togglePin,
    toggleExpand,
    // 标签操作
    loadTags,
    createTag,
    deleteTag,
    // 最近/置顶
    loadRecentItems,
    loadPinnedItems
  }
})
