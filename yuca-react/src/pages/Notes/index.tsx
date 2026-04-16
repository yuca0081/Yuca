import { useState } from 'react'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Plus, Folder, FolderOpen, FileText, Search, ChevronRight, ChevronDown } from 'lucide-react'

interface TreeItem {
  id: number
  title: string
  type: 'folder' | 'document'
  children?: TreeItem[]
  expanded?: boolean
}

const mockTree: TreeItem[] = []

export default function Notes() {
  const [tree, setTree] = useState<TreeItem[]>(mockTree)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [search, setSearch] = useState('')
  const [content, setContent] = useState('')

  const toggleExpand = (id: number, items: TreeItem[]): TreeItem[] =>
    items.map(item => {
      if (item.id === id) return { ...item, expanded: !item.expanded }
      if (item.children) return { ...item, children: toggleExpand(id, item.children) }
      return item
    })

  const renderItem = (item: TreeItem, depth = 0) => (
    <div key={item.id}>
      <div
        className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm transition-colors ${
          selectedId === item.id ? 'bg-[#FF6B35] text-white font-medium' : 'text-[#6B5344] hover:bg-[#FFF5E6]'
        }`}
        style={{ paddingLeft: `${depth * 16 + 12}px` }}
        onClick={() => {
          setSelectedId(item.id)
          if (item.type === 'folder') setTree(prev => toggleExpand(item.id, prev))
        }}
      >
        {item.type === 'folder' ? (
          <>
            {item.expanded ? <ChevronDown className="w-3 h-3 shrink-0" /> : <ChevronRight className="w-3 h-3 shrink-0" />}
            {item.expanded ? <FolderOpen className="w-4 h-4 text-[#FF6B35] shrink-0" /> : <Folder className="w-4 h-4 text-[#FF6B35] shrink-0" />}
          </>
        ) : (
          <>
            <span className="w-3 shrink-0" />
            <FileText className="w-4 h-4 shrink-0" />
          </>
        )}
        <span className="truncate">{item.title}</span>
      </div>
      {item.expanded && item.children?.map(child => renderItem(child, depth + 1))}
    </div>
  )

  return (
    <div className="flex gap-6 h-[calc(100vh-8rem)]">
      {/* Sidebar */}
      <aside className="sidebar w-64 shrink-0 overflow-hidden flex flex-col">
        <div className="p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-bold text-sm">笔记本</h2>
            <button className="p-1.5 cursor-pointer hover:text-[#FF6B35] transition-colors">
              <Plus className="w-4 h-4" />
            </button>
          </div>
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#6B5344]" />
            <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="搜索笔记..." className={`pl-8 h-9 text-xs ${"bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"}`} />
          </div>
        </div>
        <div className="border-t-2 border-foreground" />
        <ScrollArea className="flex-1 p-2">
          {tree.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-[#6B5344] text-xs">
              <FileText className="w-6 h-6 mb-2 text-[#E8DDD4]" />
              <p>暂无笔记</p>
              <p className="text-[#E8DDD4]">点击 + 创建</p>
            </div>
          ) : (
            tree.map(item => renderItem(item))
          )}
        </ScrollArea>
      </aside>

      {/* Main Content */}
      <main className="flex-1 block-card overflow-hidden flex flex-col">
        {selectedId ? (
          <>
            <div className="p-4 border-b-2 border-foreground">
              <Input className="text-lg font-bold border-0 bg-transparent focus:ring-0 px-0 h-8 shadow-none" placeholder="无标题" />
            </div>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="开始写作..."
              className="flex-1 p-4 bg-transparent resize-none outline-none text-foreground leading-relaxed"
            />
          </>
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
    </div>
  )
}
