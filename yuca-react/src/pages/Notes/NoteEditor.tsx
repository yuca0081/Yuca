import { Input } from '@/components/ui/input'

interface NoteEditorProps {
  title: string
  content: string
  onTitleChange: (title: string) => void
  onContentChange: (content: string) => void
  saving: boolean
}

export function NoteEditor({ title, content, onTitleChange, onContentChange, saving }: NoteEditorProps) {
  return (
    <>
      <div className="p-4 border-b-2 border-foreground flex items-center gap-3">
        <Input
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          className="text-lg font-bold border-0 bg-transparent focus:ring-0 px-0 h-8 shadow-none"
          placeholder="无标题"
        />
        {saving && <span className="text-xs text-[#6B5344] shrink-0">保存中...</span>}
      </div>
      <textarea
        value={content}
        onChange={(e) => onContentChange(e.target.value)}
        placeholder="开始写作..."
        className="flex-1 p-4 bg-transparent resize-none outline-none text-foreground leading-relaxed"
      />
    </>
  )
}
