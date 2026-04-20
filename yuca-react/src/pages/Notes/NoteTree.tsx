import type { TreeNode } from '@/types'
import { ChevronRight, ChevronDown, Folder, FolderOpen, FileText, Trash2 } from 'lucide-react'

interface NoteTreeProps {
  nodes: TreeNode[]
  selectedId: number | null
  expandedIds: Set<number>
  onSelect: (node: TreeNode) => void
  onDelete: (node: TreeNode) => void
}

export function NoteTree({ nodes, selectedId, expandedIds, onSelect, onDelete }: NoteTreeProps) {
  const renderNode = (node: TreeNode, depth = 0) => {
    const isFolder = node.type === 'FOLDER'
    const isExpanded = expandedIds.has(node.id)

    return (
      <div key={node.id}>
        <div
          className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm transition-colors group ${
            selectedId === node.id ? 'bg-[#FF6B35] text-white font-medium' : 'text-[#6B5344] hover:bg-[#FFF5E6]'
          }`}
          style={{ paddingLeft: `${depth * 16 + 12}px` }}
          onClick={() => onSelect(node)}
        >
          {isFolder ? (
            <>
              {isExpanded ? <ChevronDown className="w-3 h-3 shrink-0" /> : <ChevronRight className="w-3 h-3 shrink-0" />}
              {isExpanded ? <FolderOpen className="w-4 h-4 text-[#FF6B35] shrink-0" /> : <Folder className="w-4 h-4 text-[#FF6B35] shrink-0" />}
            </>
          ) : (
            <>
              <span className="w-3 shrink-0" />
              <FileText className="w-4 h-4 shrink-0" />
            </>
          )}
          <span className="flex-1 truncate">{node.title}</span>
          <button
            onClick={(e) => { e.stopPropagation(); onDelete(node) }}
            className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition-all cursor-pointer"
          >
            <Trash2 className="w-3 h-3" />
          </button>
        </div>
        {isFolder && isExpanded && node.children?.map(child => renderNode(child, depth + 1))}
      </div>
    )
  }

  return <>{nodes.map(node => renderNode(node))}</>
}
