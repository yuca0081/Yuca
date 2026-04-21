import { useState, useEffect, useCallback } from 'react'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Plus, FileText, Search, ChevronDown, BookOpen, RefreshCw } from 'lucide-react'
import { NoteTree } from './NoteTree'
import { NoteEditor } from './NoteEditor'
import { CreateNoteBookDialog } from './CreateNoteBookDialog'
import { CreateNoteItemDialog } from './CreateNoteItemDialog'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { useAutoSave } from '@/hooks/useAutoSave'
import { getNoteBooks, getNoteTree, getNoteItem, createNoteItem, updateNoteItem, deleteNoteItem, createNoteBook, deleteNoteBook } from '@/api/note'
import type { NoteBook, TreeNode, NoteItem } from '@/types'

export default function Notes() {
  // Notebook state
  const [noteBooks, setNoteBooks] = useState<NoteBook[]>([])
  const [activeBookId, setActiveBookId] = useState<number | null>(null)
  const [bookMenuOpen, setBookMenuOpen] = useState(false)

  // Tree state
  const [treeNodes, setTreeNodes] = useState<TreeNode[]>([])
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set())
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedItem, setSelectedItem] = useState<NoteItem | null>(null)

  // Editor state
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)

  // UI state
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [createBookOpen, setCreateBookOpen] = useState(false)
  const [createItemOpen, setCreateItemOpen] = useState(false)
  const [addChildParentId, setAddChildParentId] = useState<number | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'item' | 'book'; node?: TreeNode; book?: NoteBook } | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [renameTarget, setRenameTarget] = useState<TreeNode | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [renameLoading, setRenameLoading] = useState(false)

  const activeBook = noteBooks.find(b => b.id === activeBookId)

  // Load notebooks
  const loadNoteBooks = useCallback(async () => {
    try {
      const books = await getNoteBooks()
      setNoteBooks(books)
      if (books.length > 0 && !activeBookId) {
        const defaultBook = books.find(b => b.isDefault) || books[0]
        setActiveBookId(defaultBook.id)
      }
    } catch {
      console.error('Failed to load notebooks')
    }
  }, [activeBookId])

  // Load tree
  const loadTree = useCallback(async (bookId: number) => {
    try {
      setLoading(true)
      const res = await getNoteTree(bookId)
      setTreeNodes(res.nodes || [])
    } catch {
      console.error('Failed to load tree')
    } finally {
      setLoading(false)
    }
  }, [])

  // Initial load
  useEffect(() => {
    loadNoteBooks()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Load tree when activeBookId changes
  useEffect(() => {
    if (activeBookId) {
      loadTree(activeBookId)
      setSelectedId(null)
      setSelectedItem(null)
      setTitle('')
      setContent('')
    }
  }, [activeBookId, loadTree])

  // Auto-save
  const hasChanges = selectedItem && (
    title !== (selectedItem.title || '') ||
    content !== (selectedItem.content || '')
  )

  useAutoSave(
    { title, content },
    {
      onSave: async () => {
        if (!selectedItem) return
        setSaving(true)
        try {
          await updateNoteItem(selectedItem.id, { title, content })
          setSelectedItem(prev => prev ? { ...prev, title, content } : null)
          // Sync sidebar tree title
          setTreeNodes(prev => {
            const update = (nodes: TreeNode[]): TreeNode[] =>
              nodes.map(n => n.id === selectedItem.id
                ? { ...n, title }
                : n.children ? { ...n, children: update(n.children) } : n
              )
            return update(prev)
          })
        } finally {
          setSaving(false)
        }
      },
      enabled: !!hasChanges,
      interval: 1500,
    }
  )

  // Select a tree node
  const handleSelectNode = async (node: TreeNode) => {
    if (node.type === 'FOLDER') {
      setExpandedIds(prev => {
        const next = new Set(prev)
        if (next.has(node.id)) next.delete(node.id)
        else next.add(node.id)
        return next
      })
      return
    }
    // Document
    setSelectedId(node.id)
    try {
      const item = await getNoteItem(node.id)
      setSelectedItem(item)
      setTitle(item.title || '')
      setContent(item.content || '')
    } catch {
      console.error('Failed to load note item')
    }
  }

  // Delete item
  const handleDeleteItem = (node: TreeNode) => {
    setDeleteTarget({ type: 'item', node })
  }

  const handleConfirmDelete = async () => {
    if (!deleteTarget) return
    setDeleteLoading(true)
    try {
      if (deleteTarget.type === 'item' && deleteTarget.node) {
        await deleteNoteItem(deleteTarget.node.id)
        if (selectedId === deleteTarget.node.id) {
          setSelectedId(null)
          setSelectedItem(null)
          setTitle('')
          setContent('')
        }
        if (activeBookId) await loadTree(activeBookId)
      } else if (deleteTarget.type === 'book' && deleteTarget.book) {
        await deleteNoteBook(deleteTarget.book.id)
        const remaining = noteBooks.filter(b => b.id !== deleteTarget.book!.id)
        setNoteBooks(remaining)
        if (activeBookId === deleteTarget.book.id) {
          setActiveBookId(remaining.length > 0 ? (remaining.find(b => b.isDefault) || remaining[0]).id : null)
        }
      }
    } finally {
      setDeleteLoading(false)
      setDeleteTarget(null)
    }
  }

  // Create item
  const handleCreateItem = async (type: 'FOLDER' | 'DOCUMENT', itemTitle: string) => {
    if (!activeBookId) return
    try {
      const parentId = addChildParentId ?? (selectedId && selectedItem?.type === 'FOLDER' ? selectedId : null)
      await createNoteItem({
        bookId: activeBookId,
        parentId,
        type,
        title: itemTitle,
      })
      await loadTree(activeBookId)
      setAddChildParentId(null)
    } catch (err) {
      console.error('Failed to create item:', err)
      setAddChildParentId(null)
    }
  }

  // Add child to folder via three-dot menu
  const handleAddChild = (parentNode: TreeNode) => {
    setAddChildParentId(parentNode.id)
    setCreateItemOpen(true)
  }

  // Rename
  const handleRename = (node: TreeNode) => {
    setRenameTarget(node)
    setRenameValue(node.title)
  }

  const handleConfirmRename = async () => {
    if (!renameTarget || !renameValue.trim()) return
    setRenameLoading(true)
    try {
      await updateNoteItem(renameTarget.id, { title: renameValue.trim() })
      setTreeNodes(prev => {
        const update = (nodes: TreeNode[]): TreeNode[] =>
          nodes.map(n => n.id === renameTarget!.id
            ? { ...n, title: renameValue.trim() }
            : n.children ? { ...n, children: update(n.children) } : n
          )
        return update(prev)
      })
      if (selectedItem?.id === renameTarget.id) {
        setSelectedItem(prev => prev ? { ...prev, title: renameValue.trim() } : null)
        setTitle(renameValue.trim())
      }
    } finally {
      setRenameLoading(false)
      setRenameTarget(null)
    }
  }

  // Create notebook
  const handleCreateBook = async (name: string) => {
    try {
      const id = await createNoteBook({ name })
      await loadNoteBooks()
      setActiveBookId(id)
    } catch (err) {
      console.error('Failed to create notebook:', err)
    }
  }

  // Filter tree by search
  const filterTree = (nodes: TreeNode[], query: string): TreeNode[] => {
    if (!query) return nodes
    return nodes.reduce<TreeNode[]>((acc, node) => {
      const childMatch = node.children ? filterTree(node.children, query) : []
      if (node.title.toLowerCase().includes(query.toLowerCase()) || childMatch.length > 0) {
        acc.push({ ...node, children: childMatch })
        if (childMatch.length > 0) setExpandedIds(prev => new Set(prev).add(node.id))
      }
      return acc
    }, [])
  }

  const displayedNodes = search ? filterTree(treeNodes, search) : treeNodes

  return (
    <div className="flex gap-6 h-[calc(100vh-8rem)]">
      {/* Sidebar */}
      <aside className="sidebar w-64 shrink-0 overflow-hidden flex flex-col">
        <div className="p-4">
          <div className="flex items-center justify-between mb-4">
            {/* Notebook selector */}
            <div className="flex-1 mr-2 relative">
              <button
                onClick={() => setBookMenuOpen(!bookMenuOpen)}
                className="w-full flex items-center justify-between px-2 py-1.5 text-sm font-bold bg-white border-2 border-foreground cursor-pointer hover:bg-[#FFF5E6] transition-colors"
              >
                <span className="truncate">{activeBook?.name || '选择笔记本'}</span>
                <ChevronDown className="w-4 h-4 shrink-0" />
              </button>
              {bookMenuOpen && (
                <div className="absolute top-full left-0 right-0 mt-1 bg-white border-2 border-foreground shadow-[4px_4px_0_#2C1810] z-10 max-h-48 overflow-y-auto">
                  {noteBooks.map(book => (
                    <div
                      key={book.id}
                      className={`px-3 py-2 text-sm cursor-pointer transition-colors flex items-center justify-between group ${
                        activeBookId === book.id ? 'bg-[#FF6B35] text-white font-medium' : 'text-[#6B5344] hover:bg-[#FFF5E6]'
                      }`}
                      onClick={() => {
                        setActiveBookId(book.id)
                        setBookMenuOpen(false)
                      }}
                    >
                      <BookOpen className="w-3.5 h-3.5 mr-2 shrink-0" />
                      <span className="flex-1 truncate">{book.name}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setDeleteTarget({ type: 'book', book })
                          setBookMenuOpen(false)
                        }}
                        className="opacity-0 group-hover:opacity-100 text-red-500 cursor-pointer ml-1"
                      >
                        &times;
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="flex gap-1">
              <button
                onClick={() => setCreateBookOpen(true)}
                className="p-1.5 cursor-pointer hover:text-[#FF6B35] transition-colors"
                title="新建笔记本"
              >
                <BookOpen className="w-4 h-4" />
              </button>
              <button
                onClick={() => setCreateItemOpen(true)}
                className="p-1.5 cursor-pointer hover:text-[#FF6B35] transition-colors"
                title="新建条目"
              >
                <Plus className="w-4 h-4" />
              </button>
              <button
                onClick={() => activeBookId && loadTree(activeBookId)}
                className="p-1.5 cursor-pointer hover:text-[#FF6B35] transition-colors"
                title="刷新"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>
          </div>
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#6B5344]" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="搜索笔记..."
              className="pl-8 h-9 text-xs bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            />
          </div>
        </div>
        <div className="border-t-2 border-foreground" />
        <ScrollArea className="flex-1 p-2">
          {loading ? (
            <div className="flex items-center justify-center h-32 text-[#6B5344] text-xs">
              加载中...
            </div>
          ) : displayedNodes.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-[#6B5344] text-xs">
              <FileText className="w-6 h-6 mb-2 text-[#E8DDD4]" />
              <p>{activeBookId ? '暂无笔记' : '请先创建笔记本'}</p>
              <p className="text-[#E8DDD4]">点击 + 创建</p>
            </div>
          ) : (
            <NoteTree
              nodes={displayedNodes}
              selectedId={selectedId}
              expandedIds={expandedIds}
              onSelect={handleSelectNode}
              onDelete={handleDeleteItem}
              onAddChild={handleAddChild}
              onRename={handleRename}
            />
          )}
        </ScrollArea>
      </aside>

      {/* Main Content */}
      <main className="flex-1 block-card overflow-hidden flex flex-col">
        {selectedItem ? (
          <NoteEditor
            title={title}
            content={content}
            onTitleChange={setTitle}
            onContentChange={setContent}
            saving={saving}
          />
        ) : (
          <div className="flex-1 flex items-center justify-center text-[#6B5344]">
            <div className="text-center">
              <FileText className="w-12 h-12 mx-auto mb-3 text-[#E8DDD4]" />
              <p>选择一篇笔记开始编辑</p>
              <p className="text-sm text-[#E8DDD4] mt-1">或从左侧创建新笔记</p>
            </div>
          </div>
        )}
      </main>

      {/* Dialogs */}
      <CreateNoteBookDialog
        open={createBookOpen}
        onOpenChange={setCreateBookOpen}
        onSubmit={handleCreateBook}
      />
      <CreateNoteItemDialog
        open={createItemOpen}
        onOpenChange={setCreateItemOpen}
        onSubmit={handleCreateItem}
      />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title={deleteTarget?.type === 'book' ? '删除笔记本' : '删除条目'}
        description={
          deleteTarget?.type === 'book'
            ? `确定要删除笔记本「${deleteTarget.book?.name}」吗？其中的所有内容将被删除。`
            : deleteTarget?.node?.type === 'FOLDER'
              ? `确定要删除文件夹「${deleteTarget.node?.title}」吗？其中的所有子文件和子文件夹也将被删除。`
              : `确定要删除「${deleteTarget?.node?.title}」吗？`
        }
        onConfirm={handleConfirmDelete}
        loading={deleteLoading}
      />

      {/* Rename Dialog */}
      <Dialog open={!!renameTarget} onOpenChange={(open) => !open && setRenameTarget(null)}>
        <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
          <DialogHeader>
            <DialogTitle>重命名</DialogTitle>
          </DialogHeader>
          <Input
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            className="border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            onKeyDown={(e) => e.key === 'Enter' && handleConfirmRename()}
            autoFocus
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setRenameTarget(null)} disabled={renameLoading} className="rounded-none">
              取消
            </Button>
            <Button onClick={handleConfirmRename} disabled={renameLoading || !renameValue.trim()} className="rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white">
              {renameLoading ? '保存中...' : '确定'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
