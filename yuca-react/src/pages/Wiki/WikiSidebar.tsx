import { Plus, BookOpen, Trash2 } from 'lucide-react'
import type { KnowledgeBase } from '@/types'

interface WikiSidebarProps {
  kbList: KnowledgeBase[]
  activeKbId: number | null
  onSelect: (id: number) => void
  onDelete: (kb: KnowledgeBase) => void
  onCreate: () => void
  loading: boolean
}

export function WikiSidebar({ kbList, activeKbId, onSelect, onDelete, onCreate, loading }: WikiSidebarProps) {
  return (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="font-bold text-sm">知识库</h2>
        <button onClick={onCreate} className="p-1.5 cursor-pointer hover:text-[#FF6B35] transition-colors" title="新建知识库">
          <Plus className="w-4 h-4" />
        </button>
      </div>
      {loading ? (
        <div className="text-xs text-[#6B5344] text-center py-4">加载中...</div>
      ) : kbList.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-32 text-[#6B5344] text-xs">
          <BookOpen className="w-6 h-6 mb-2 text-[#E8DDD4]" />
          <p>暂无知识库</p>
          <p className="text-[#E8DDD4]">点击 + 创建</p>
        </div>
      ) : (
        <div className="space-y-1">
          {kbList.map(kb => (
            <div
              key={kb.id}
              className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm transition-colors group ${
                activeKbId === kb.id ? 'bg-[#FF6B35] text-white font-medium' : 'text-[#6B5344] hover:bg-[#FFF5E6]'
              }`}
              onClick={() => onSelect(kb.id)}
            >
              <BookOpen className="w-4 h-4 shrink-0" />
              <div className="flex-1 min-w-0">
                <span className="truncate block">{kb.name}</span>
                {kb.docCount !== undefined && (
                  <span className={`text-xs ${activeKbId === kb.id ? 'text-white/70' : 'text-[#E8DDD4]'}`}>
                    {kb.docCount} 个文档
                  </span>
                )}
              </div>
              <button
                onClick={(e) => { e.stopPropagation(); onDelete(kb) }}
                className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition-all cursor-pointer"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
