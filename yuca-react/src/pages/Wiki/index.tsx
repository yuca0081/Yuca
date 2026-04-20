import { useState, useEffect, useCallback } from 'react'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Upload, BookOpen } from 'lucide-react'
import { WikiSidebar } from './WikiSidebar'
import { KbDocumentList } from './KbDocumentList'
import { DocumentDetail } from './DocumentDetail'
import { CreateKbDialog } from './CreateKbDialog'
import { UploadDocDialog } from './UploadDocDialog'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import {
  getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase,
  getDocPageList, uploadDocument, deleteDocuments, getDocChunks,
} from '@/api/knowledge'
import type { KnowledgeBase, KnowledgeDoc, KnowledgeChunk } from '@/types'

export default function Wiki() {
  // KB state
  const [kbList, setKbList] = useState<KnowledgeBase[]>([])
  const [activeKbId, setActiveKbId] = useState<number | null>(null)

  // Document state
  const [docs, setDocs] = useState<KnowledgeDoc[]>([])
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 })
  const [selectedDoc, setSelectedDoc] = useState<KnowledgeDoc | null>(null)
  const [chunks, setChunks] = useState<KnowledgeChunk[]>([])

  // UI state
  const [kbLoading, setKbLoading] = useState(false)
  const [docLoading, setDocLoading] = useState(false)
  const [chunkLoading, setChunkLoading] = useState(false)
  const [createKbOpen, setCreateKbOpen] = useState(false)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'kb' | 'doc'; data: KnowledgeBase | KnowledgeDoc } | null>(null)

  const deleteLabel = deleteTarget?.type === 'kb'
    ? `确定要删除知识库「${(deleteTarget.data as KnowledgeBase).name}」吗？其中的所有文档将被删除。`
    : `确定要删除文档「${(deleteTarget?.data as KnowledgeDoc)?.docName}」吗？`
  const [deleteLoading, setDeleteLoading] = useState(false)

  // Load KB list
  const loadKbList = useCallback(async () => {
    try {
      setKbLoading(true)
      const list = await getKnowledgeBases()
      setKbList(list)
      if (list.length > 0 && !activeKbId) {
        setActiveKbId(list[0].id)
      }
    } catch {
      console.error('Failed to load knowledge bases')
    } finally {
      setKbLoading(false)
    }
  }, [activeKbId])

  // Load documents
  const loadDocs = useCallback(async (kbId: number, page = 1) => {
    try {
      setDocLoading(true)
      const res = await getDocPageList(kbId, page, 10)
      setDocs(res.records || [])
      setPagination({ current: res.current, size: res.size, total: res.total })
    } catch {
      console.error('Failed to load documents')
    } finally {
      setDocLoading(false)
    }
  }, [])

  // Load chunks
  const loadChunks = async (docId: number) => {
    try {
      setChunkLoading(true)
      const data = await getDocChunks(docId)
      setChunks(data)
    } catch {
      console.error('Failed to load chunks')
    } finally {
      setChunkLoading(false)
    }
  }

  // Initial load
  useEffect(() => {
    loadKbList()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Load docs when active KB changes
  useEffect(() => {
    if (activeKbId) {
      loadDocs(activeKbId)
      setSelectedDoc(null)
      setChunks([])
    }
  }, [activeKbId, loadDocs])

  // Handlers
  const handleSelectKb = (id: number) => {
    setActiveKbId(id)
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  const handleCreateKb = async (name: string, description?: string) => {
    const id = await createKnowledgeBase({ name, description })
    await loadKbList()
    setActiveKbId(id)
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleteLoading(true)
    try {
      if (deleteTarget.type === 'kb') {
        await deleteKnowledgeBase(deleteTarget.data.id)
        const remaining = kbList.filter(k => k.id !== deleteTarget.data.id)
        setKbList(remaining)
        if (activeKbId === deleteTarget.data.id) {
          setActiveKbId(remaining.length > 0 ? remaining[0].id : null)
        }
      } else {
        await deleteDocuments([deleteTarget.data.id])
        if (activeKbId) await loadDocs(activeKbId, pagination.current)
        if (selectedDoc?.id === deleteTarget.data.id) {
          setSelectedDoc(null)
          setChunks([])
        }
      }
    } finally {
      setDeleteLoading(false)
      setDeleteTarget(null)
    }
  }

  const handleUpload = async (file: File) => {
    if (!activeKbId) return
    await uploadDocument(activeKbId, file)
    await loadDocs(activeKbId)
  }

  const handleSelectDoc = async (doc: KnowledgeDoc) => {
    setSelectedDoc(doc)
    await loadChunks(doc.id)
  }

  const handlePageChange = (page: number) => {
    if (activeKbId) loadDocs(activeKbId, page)
  }

  return (
    <div className="flex gap-6 h-[calc(100vh-8rem)]">
      {/* Sidebar */}
      <aside className="sidebar w-64 shrink-0 overflow-hidden flex flex-col">
        <ScrollArea className="flex-1">
          <WikiSidebar
            kbList={kbList}
            activeKbId={activeKbId}
            onSelect={handleSelectKb}
            onDelete={(kb) => setDeleteTarget({ type: 'kb', data: kb })}
            onCreate={() => setCreateKbOpen(true)}
            loading={kbLoading}
          />
        </ScrollArea>
      </aside>

      {/* Main Content */}
      <main className="flex-1 block-card overflow-hidden flex flex-col">
        {activeKbId ? (
          <div className="flex-1 overflow-y-auto p-6">
            {/* Toolbar */}
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-bold text-lg">{kbList.find(k => k.id === activeKbId)?.name || '知识库'}</h2>
              <button
                onClick={() => setUploadOpen(true)}
                className="btn-primary text-sm py-2 flex items-center gap-2"
              >
                <Upload className="w-4 h-4" />
                上传文档
              </button>
            </div>

            {selectedDoc ? (
              <div>
                <button
                  onClick={() => { setSelectedDoc(null); setChunks([]) }}
                  className="text-sm text-[#FF6B35] hover:underline mb-4 cursor-pointer"
                >
                  &larr; 返回文档列表
                </button>
                <DocumentDetail doc={selectedDoc} chunks={chunks} chunkLoading={chunkLoading} />
              </div>
            ) : (
              <KbDocumentList
                docs={docs}
                selectedDocId={null}
                onSelect={handleSelectDoc}
                onDelete={(doc) => setDeleteTarget({ type: 'doc', data: doc })}
                loading={docLoading}
                pagination={pagination}
                onPageChange={handlePageChange}
              />
            )}
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center text-[#6B5344]">
            <div className="text-center">
              <BookOpen className="w-12 h-12 mx-auto mb-3 text-[#E8DDD4]" />
              <p>选择一个知识库</p>
              <p className="text-sm text-[#E8DDD4] mt-1">或从左侧创建新知识库</p>
            </div>
          </div>
        )}
      </main>

      {/* Dialogs */}
      <CreateKbDialog
        open={createKbOpen}
        onOpenChange={setCreateKbOpen}
        onSubmit={handleCreateKb}
      />
      <UploadDocDialog
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        onSubmit={handleUpload}
      />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title={deleteTarget?.type === 'kb' ? '删除知识库' : '删除文档'}
        description={deleteLabel}
        onConfirm={handleDelete}
        loading={deleteLoading}
      />
    </div>
  )
}
