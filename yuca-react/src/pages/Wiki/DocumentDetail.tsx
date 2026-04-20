import { FileText, Layers, Clock } from 'lucide-react'
import type { KnowledgeDoc, KnowledgeChunk } from '@/types'

interface DocumentDetailProps {
  doc: KnowledgeDoc
  chunks: KnowledgeChunk[]
  chunkLoading: boolean
}

export function DocumentDetail({ doc, chunks, chunkLoading }: DocumentDetailProps) {
  return (
    <div className="space-y-6">
      {/* Doc metadata */}
      <div className="block-card p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 bg-[#FF6B35] flex items-center justify-center border-2 border-foreground shrink-0">
            <FileText className="w-5 h-5 text-white" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-bold text-lg truncate">{doc.docName}</h3>
            <div className="flex flex-wrap gap-4 mt-2 text-sm text-[#6B5344]">
              <span className="flex items-center gap-1">
                <Layers className="w-3.5 h-3.5" />
                {doc.chunkCount} 个分块
              </span>
              <span className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                {new Date(doc.createdAt).toLocaleDateString('zh-CN')}
              </span>
              <span>{doc.docFormat.toUpperCase()} · {(doc.docSize / 1024).toFixed(1)} KB</span>
            </div>
          </div>
        </div>
      </div>

      {/* Chunks */}
      <div>
        <h4 className="font-bold text-sm mb-3 flex items-center gap-2">
          <Layers className="w-4 h-4 text-[#FF6B35]" />
          文档分块
          <span className="text-[#6B5344] font-normal">({chunks.length})</span>
        </h4>
        {chunkLoading ? (
          <div className="text-sm text-[#6B5344] text-center py-4">加载分块中...</div>
        ) : chunks.length === 0 ? (
          <div className="text-sm text-[#6B5344] text-center py-4">暂无分块数据</div>
        ) : (
          <div className="space-y-3">
            {chunks.map((chunk) => (
              <div key={chunk.id} className="bg-white border-2 border-foreground p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-medium text-[#FF6B35]">分块 #{chunk.chunkIndex + 1}</span>
                  <span className={`text-xs px-2 py-0.5 border ${chunk.isActive ? 'text-green-700 border-green-300 bg-green-50' : 'text-[#6B5344] border-[#E8DDD4] bg-[#FFF5E6]'}`}>
                    {chunk.isActive ? '活跃' : '未激活'}
                  </span>
                </div>
                <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">{chunk.content}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
