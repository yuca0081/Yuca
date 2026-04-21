import { useState, useRef, useEffect } from 'react'
import type { TreeNode } from '@/types'
import { ChevronRight, ChevronDown, Folder, FolderOpen, FileText, Trash2, MoreHorizontal, Plus, Pencil } from 'lucide-react'

interface NoteTreeProps {
  nodes: TreeNode[]
  selectedId: number | null
  expandedIds: Set<number>
  onSelect: (node: TreeNode) => void
  onDelete: (node: TreeNode) => void
  onAddChild: (parentNode: TreeNode) => void
  onRename: (node: TreeNode) => void
}

export function NoteTree({ nodes, selectedId, expandedIds, onSelect, onDelete, onAddChild, onRename }: NoteTreeProps) {
  return <>{nodes.map(node => renderNode(node, 0))}</>

  function renderNode(node: TreeNode, depth: number) {
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

          {/* Action buttons — visible on hover */}
          {isFolder && (
            <FolderMenu node={node} onAddChild={onAddChild} onRename={onRename} onDelete={onDelete} />
          )}
          {!isFolder && (
            <button
              onClick={(e) => { e.stopPropagation(); onDelete(node) }}
              className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition-all cursor-pointer"
            >
              <Trash2 className="w-3 h-3" />
            </button>
          )}
        </div>
        {isFolder && isExpanded && node.children?.map(child => renderNode(child, depth + 1))}
      </div>
    )
  }
}

/** Three-dot menu for folder nodes */
function FolderMenu({ node, onAddChild, onRename, onDelete }: {
  node: TreeNode
  onAddChild: (node: TreeNode) => void
  onRename: (node: TreeNode) => void
  onDelete: (node: TreeNode) => void
}) {
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Close on outside click
  useEffect(() => {
    if (!open) return
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  return (
    <div ref={menuRef} className="relative opacity-0 group-hover:opacity-100 transition-opacity">
      <button
        onClick={(e) => { e.stopPropagation(); setOpen(!open) }}
        className="p-1 hover:text-[#FF6B35] transition-colors cursor-pointer"
      >
        <MoreHorizontal className="w-3.5 h-3.5" />
      </button>
      {open && (
        <div
          className="absolute right-0 top-full mt-1 bg-white border-2 border-foreground shadow-[4px_4px_0_#2C1810] z-20 min-w-[120px]"
          onClick={(e) => e.stopPropagation()}
        >
          <button
            className="w-full flex items-center gap-2 px-3 py-2 text-xs text-[#6B5344] hover:bg-[#FFF5E6] transition-colors cursor-pointer"
            onClick={() => { onAddChild(node); setOpen(false) }}
          >
            <Plus className="w-3.5 h-3.5" />
            创建
          </button>
          <button
            className="w-full flex items-center gap-2 px-3 py-2 text-xs text-[#6B5344] hover:bg-[#FFF5E6] transition-colors cursor-pointer"
            onClick={() => { onRename(node); setOpen(false) }}
          >
            <Pencil className="w-3.5 h-3.5" />
            重命名
          </button>
          <div className="border-t border-foreground/20" />
          <button
            className="w-full flex items-center gap-2 px-3 py-2 text-xs text-red-500 hover:bg-red-50 transition-colors cursor-pointer"
            onClick={() => { onDelete(node); setOpen(false) }}
          >
            <Trash2 className="w-3.5 h-3.5" />
            删除
          </button>
        </div>
      )}
    </div>
  )
}
