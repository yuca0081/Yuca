import { useState, useRef, useEffect, useCallback } from 'react'
import { Input } from '@/components/ui/input'
import { Bot, Send, User, Loader2, X } from 'lucide-react'
import { noteAssistantChat } from '@/api/noteAi'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
}

interface NoteAiSidebarProps {
  onClose: () => void
  noteItemId: number | null
  title: string
  content: string
}

export function NoteAiSidebar({ onClose, noteItemId, title }: NoteAiSidebarProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [sessionId, setSessionId] = useState<string | undefined>(undefined)
  const scrollRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = useCallback(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [])

  useEffect(() => { scrollToBottom() }, [messages, scrollToBottom])

  const handleSend = async () => {
    if (!input.trim() || loading) return

    const userMsg: Message = {
      id: Date.now(),
      role: 'user',
      content: input.trim(),
    }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      let messageContent = userMsg.content
      if (noteItemId && title) {
        messageContent = `[当前文档: ${title}]\n\n${userMsg.content}`
      }

      const res = await noteAssistantChat(sessionId, messageContent)
      if (!sessionId) {
        setSessionId(`note-${Date.now()}`)
      }
      const aiMsg: Message = {
        id: Date.now() + 1,
        role: 'assistant',
        content: res.content,
      }
      setMessages(prev => [...prev, aiMsg])
    } catch (e: any) {
      const errorMsg: Message = {
        id: Date.now() + 1,
        role: 'assistant',
        content: `出错了: ${e.message || '请求失败'}`,
      }
      setMessages(prev => [...prev, errorMsg])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <aside className="w-80 shrink-0 block-card overflow-hidden flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b-2 border-foreground">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 bg-foreground flex items-center justify-center">
            <Bot className="w-3.5 h-3.5 text-white" />
          </div>
          <span className="font-bold text-sm text-foreground">笔记助手</span>
        </div>
        <button
          onClick={onClose}
          className="p-1 text-[#6B5344] hover:text-foreground transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 space-y-3">
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-[#6B5344]">
            <div className="text-center">
              <div className="w-12 h-12 bg-[#FF6B35] flex items-center justify-center mx-auto mb-3 border-2 border-foreground">
                <Bot className="w-6 h-6 text-white" />
              </div>
              <p className="text-sm font-medium">笔记 AI 助手</p>
              <p className="text-xs mt-1 text-[#E8DDD4]">问我关于笔记的任何问题</p>
            </div>
          </div>
        ) : (
          messages.map(msg => (
            <div key={msg.id} className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              {msg.role === 'assistant' && (
                <div className="w-6 h-6 bg-foreground flex items-center justify-center shrink-0">
                  <Bot className="w-3 h-3 text-white" />
                </div>
              )}
              <div className={`max-w-[80%] px-3 py-2 text-xs leading-relaxed border-2 ${
                msg.role === 'user'
                  ? 'bg-[#FF6B35] text-white border-foreground'
                  : 'bg-white text-foreground border-foreground'
              }`}>
                <div className="whitespace-pre-wrap break-words">{msg.content}</div>
              </div>
              {msg.role === 'user' && (
                <div className="w-6 h-6 bg-[#FFF5E6] flex items-center justify-center shrink-0 border-2 border-foreground">
                  <User className="w-3 h-3 text-foreground" />
                </div>
              )}
            </div>
          ))
        )}
        {loading && (
          <div className="flex gap-2">
            <div className="w-6 h-6 bg-foreground flex items-center justify-center shrink-0">
              <Bot className="w-3 h-3 text-white" />
            </div>
            <div className="bg-white px-3 py-2 border-2 border-foreground">
              <div className="flex gap-1">
                <span className="w-1.5 h-1.5 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-1.5 h-1.5 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-1.5 h-1.5 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="p-3 border-t-2 border-foreground">
        <div className="flex gap-2">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="问我关于笔记的问题..."
            className="h-9 text-xs bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            disabled={loading}
          />
          <button
            onClick={handleSend}
            disabled={loading || !input.trim()}
            className="btn-primary h-9 w-9 flex items-center justify-center p-0 disabled:opacity-50 shrink-0"
          >
            {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>
    </aside>
  )
}
