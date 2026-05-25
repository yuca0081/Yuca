import { useState } from 'react'
import { Bot, FileText, Languages, Sparkles, Maximize2, List, MessageSquare } from 'lucide-react'
import { noteDocAction, type DocActionType } from '@/api/noteAi'
import { DocActionPreview } from './DocActionPreview'

interface NoteAiToolbarProps {
  noteItemId: number | null
  title: string
  content: string
  onApplyResult: (result: string, action: string) => void
  onToggleSidebar: () => void
  showSidebar: boolean
}

const actions: { type: DocActionType; label: string; icon: React.ReactNode }[] = [
  { type: 'SUMMARIZE', label: '总结', icon: <FileText className="w-4 h-4" /> },
  { type: 'TRANSLATE', label: '翻译', icon: <Languages className="w-4 h-4" /> },
  { type: 'POLISH', label: '润色', icon: <Sparkles className="w-4 h-4" /> },
  { type: 'EXPAND', label: '扩写', icon: <Maximize2 className="w-4 h-4" /> },
  { type: 'OUTLINE', label: '大纲', icon: <List className="w-4 h-4" /> },
]

export function NoteAiToolbar({
  noteItemId,
  title,
  content,
  onApplyResult,
  onToggleSidebar,
  showSidebar,
}: NoteAiToolbarProps) {
  const [loading, setLoading] = useState(false)
  const [previewResult, setPreviewResult] = useState<string | null>(null)
  const [previewAction, setPreviewAction] = useState<string>('')

  const handleAction = async (type: DocActionType) => {
    if (!noteItemId || !content) return
    setLoading(true)
    try {
      const res = await noteDocAction(noteItemId, type, title, content)
      setPreviewResult(res.result)
      setPreviewAction(res.action)
    } catch (e: any) {
      alert(e.message || '操作失败')
    } finally {
      setLoading(false)
    }
  }

  const handleApply = (result: string, action: string) => {
    onApplyResult(result, action)
    setPreviewResult(null)
  }

  return (
    <>
      <div className="w-10 shrink-0 bg-white border-l-2 border-foreground flex flex-col items-center py-2 gap-1">
        {/* AI actions */}
        {actions.map(({ type, label, icon }) => (
          <button
            key={type}
            onClick={() => handleAction(type)}
            disabled={loading || !noteItemId || !content}
            title={label}
            className="w-8 h-8 flex items-center justify-center text-[#6B5344] hover:bg-[#FF6B35] hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer"
          >
            {icon}
          </button>
        ))}

        <div className="w-6 border-t-2 border-foreground my-1" />

        {/* Chat toggle */}
        <button
          onClick={onToggleSidebar}
          title="AI 对话"
          className={`w-8 h-8 flex items-center justify-center transition-colors cursor-pointer ${
            showSidebar
              ? 'bg-[#FF6B35] text-white'
              : 'text-[#6B5344] hover:bg-[#FF6B35] hover:text-white'
          }`}
        >
          <MessageSquare className="w-4 h-4" />
        </button>
      </div>

      <DocActionPreview
        open={!!previewResult}
        onClose={() => setPreviewResult(null)}
        result={previewResult || ''}
        action={previewAction}
        onApply={handleApply}
      />
    </>
  )
}
