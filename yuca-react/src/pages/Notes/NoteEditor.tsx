import { useState, useRef, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkBreaks from 'remark-breaks'
import rehypeHighlight from 'rehype-highlight'

interface NoteEditorProps {
  title: string
  content: string
  onTitleChange: (title: string) => void
  onContentChange: (content: string) => void
  saving: boolean
}

export function NoteEditor({ title, content, onTitleChange, onContentChange, saving }: NoteEditorProps) {
  const [editing, setEditing] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (editing && textareaRef.current) {
      textareaRef.current.focus()
    }
  }, [editing])

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

      <div className="flex-1 overflow-hidden relative">
        {editing ? (
          <div className="absolute inset-0 flex flex-col p-6">
            <textarea
              ref={textareaRef}
              value={content}
              onChange={(e) => onContentChange(e.target.value)}
              onBlur={() => setEditing(false)}
              onKeyDown={(e) => {
                if (e.key === 'Escape') setEditing(false)
              }}
              placeholder="开始写作..."
              className="flex-1 bg-transparent resize-none outline-none text-foreground leading-relaxed font-mono text-sm w-full"
            />
            <p className="text-xs text-[#B09A87] py-1">点击外部区域或按 Esc 预览 Markdown 样式</p>
          </div>
        ) : (
          <div
            className="absolute inset-0 overflow-y-auto p-6 cursor-text"
            onClick={() => setEditing(true)}
          >
            {content ? (
              <div className="md-content">
                <ReactMarkdown remarkPlugins={[remarkGfm, remarkBreaks]} rehypePlugins={[rehypeHighlight]}>
                  {content}
                </ReactMarkdown>
              </div>
            ) : (
              <p className="text-[#E8DDD4]">点击开始写作...</p>
            )}
          </div>
        )}
      </div>
    </>
  )
}
