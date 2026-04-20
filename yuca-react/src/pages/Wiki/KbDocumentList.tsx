import { FileText, Trash2, Eye } from 'lucide-react'
import type { KnowledgeDoc } from '@/types'

interface KbDocumentListProps {
  docs: KnowledgeDoc[]
  selectedDocId: number | null
  onSelect: (doc: KnowledgeDoc) => void
  onDelete: (doc: KnowledgeDoc) => void
  loading: boolean
  pagination: { current: number; size: number; total: number }
  onPageChange: (page: number) => void
}

export function KbDocumentList({ docs, selectedDocId, onSelect, onDelete, loading, pagination, onPageChange }: KbDocumentListProps) {
  const totalPages = Math.ceil(pagination.total / pagination.size)

  if (loading) {
    return <div className="text-sm text-[#6B5344] text-center py-8">加载中...</div>
  }

  if (docs.length === 0) {
    return (
      <div className="text-center py-12 text-[#6B5344]">
        <FileText className="w-10 h-10 mx-auto mb-3 text-[#E8DDD4]" />
        <p>暂无文档</p>
        <p className="text-sm text-[#E8DDD4] mt-1">点击上传按钮添加文档</p>
      </div>
    )
  }

  return (
    <div>
      <div className="grid gap-3">
        {docs.map(doc => (
          <div
            key={doc.id}
            className={`flex items-center gap-3 p-3 border-2 cursor-pointer transition-all group ${
              selectedDocId === doc.id
                ? 'border-[#FF6B35] bg-[#FFF5E6]'
                : 'border-foreground bg-white hover:shadow-[3px_3px_0_#2C1810]'
            }`}
            onClick={() => onSelect(doc)}
          >
            <FileText className="w-5 h-5 text-[#FF6B35] shrink-0" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{doc.docName}</p>
              <p className="text-xs text-[#6B5344] mt-0.5">
                {doc.docFormat.toUpperCase()} · {(doc.docSize / 1024).toFixed(1)} KB · {doc.chunkCount} 个分块
              </p>
            </div>
            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={(e) => { e.stopPropagation(); onSelect(doc) }}
                className="p-1.5 hover:text-[#FF6B35] cursor-pointer"
                title="查看分块"
              >
                <Eye className="w-4 h-4" />
              </button>
              <button
                onClick={(e) => { e.stopPropagation(); onDelete(doc) }}
                className="p-1.5 hover:text-red-500 cursor-pointer"
                title="删除"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-4 pt-4 border-t-2 border-[#E8DDD4]">
          <button
            onClick={() => onPageChange(pagination.current - 1)}
            disabled={pagination.current <= 1}
            className="px-3 py-1 text-sm border-2 border-foreground disabled:opacity-50 cursor-pointer hover:bg-[#FFF5E6] disabled:cursor-not-allowed"
          >
            上一页
          </button>
          <span className="text-sm text-[#6B5344]">
            {pagination.current} / {totalPages}
          </span>
          <button
            onClick={() => onPageChange(pagination.current + 1)}
            disabled={pagination.current >= totalPages}
            className="px-3 py-1 text-sm border-2 border-foreground disabled:opacity-50 cursor-pointer hover:bg-[#FFF5E6] disabled:cursor-not-allowed"
          >
            下一页
          </button>
        </div>
      )}
    </div>
  )
}
