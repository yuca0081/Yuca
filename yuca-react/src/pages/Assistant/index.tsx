import { useState, useRef, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Bot, Send, Plus, MessageSquare, Trash2, User } from 'lucide-react'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

interface ChatSession {
  id: number
  title: string | null
  updatedAt: string
}

export default function Assistant() {
  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [activeSessionId, setActiveSessionId] = useState<number | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }

  useEffect(() => { scrollToBottom() }, [messages])

  const handleSend = async () => {
    if (!input.trim() || loading) return

    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: input.trim(),
      createdAt: new Date().toISOString(),
    }

    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)

    setTimeout(() => {
      const aiMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '你好！我是你的 AI 助手。这个功能正在开发中，敬请期待。',
        createdAt: new Date().toISOString(),
      }
      setMessages(prev => [...prev, aiMsg])
      setLoading(false)
    }, 1000)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const deleteSession = (id: number) => {
    setSessions(prev => prev.filter(s => s.id !== id))
    if (activeSessionId === id) {
      setActiveSessionId(null)
      setMessages([])
    }
  }

  return (
    <div className="flex gap-6 h-[calc(100vh-8rem)]">
      {/* Sidebar */}
      <aside className="sidebar w-64 shrink-0 overflow-hidden flex flex-col">
        <div className="p-4">
          <button
            onClick={() => { setActiveSessionId(null); setMessages([]) }}
            className="w-full btn-primary text-sm py-2.5 flex items-center justify-center gap-2"
          >
            <Plus className="w-4 h-4" />
            新对话
          </button>
        </div>
        <div className="border-t-2 border-foreground" />
        <ScrollArea className="flex-1 p-2">
          {sessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-[#6B5344] text-xs">
              <MessageSquare className="w-6 h-6 mb-2 text-[#E8DDD4]" />
              <p>暂无对话</p>
            </div>
          ) : (
            sessions.map(session => (
              <div
                key={session.id}
                className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm transition-colors group ${
                  activeSessionId === session.id ? 'bg-[#FF6B35] text-white font-medium' : 'text-[#6B5344] hover:bg-[#FFF5E6]'
                }`}
                onClick={() => setActiveSessionId(session.id)}
              >
                <MessageSquare className="w-4 h-4 shrink-0" />
                <span className="flex-1 truncate">{session.title || '新对话'}</span>
                <button
                  onClick={(e) => { e.stopPropagation(); deleteSession(session.id) }}
                  className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition-all cursor-pointer"
                >
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>
            ))
          )}
        </ScrollArea>
      </aside>

      {/* Main Chat Area */}
      <main className="flex-1 block-card overflow-hidden flex flex-col">
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.length === 0 ? (
            <div className="flex-1 flex items-center justify-center h-full text-[#6B5344]">
              <div className="text-center">
                <div className="w-16 h-16 bg-[#FF6B35] flex items-center justify-center mx-auto mb-4 border-2 border-foreground">
                  <Bot className="w-8 h-8 text-white" />
                </div>
                <h3 className="font-bold text-lg">Yuca AI 助手</h3>
                <p className="text-[#6B5344] mt-1 text-sm">有什么我可以帮助你的？</p>
              </div>
            </div>
          ) : (
            messages.map(msg => (
              <div key={msg.id} className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                {msg.role === 'assistant' && (
                  <div className="w-8 h-8 bg-foreground flex items-center justify-center shrink-0">
                    <Bot className="w-4 h-4 text-white" />
                  </div>
                )}
                <div className={`max-w-[70%] px-4 py-3 text-sm leading-relaxed border-2 ${
                  msg.role === 'user'
                    ? 'bg-[#FF6B35] text-white border-foreground'
                    : 'bg-white text-foreground border-foreground'
                }`}>
                  {msg.content}
                </div>
                {msg.role === 'user' && (
                  <div className="w-8 h-8 bg-[#FFF5E6] flex items-center justify-center shrink-0 border-2 border-foreground">
                    <User className="w-4 h-4 text-foreground" />
                  </div>
                )}
              </div>
            ))
          )}
          {loading && (
            <div className="flex gap-3">
              <div className="w-8 h-8 bg-foreground flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4 text-white" />
              </div>
              <div className="bg-white px-4 py-3 border-2 border-foreground">
                <div className="flex gap-1">
                  <span className="w-2 h-2 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                  <span className="w-2 h-2 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                  <span className="w-2 h-2 bg-[#6B5344] rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Input */}
        <div className="p-4 border-t-2 border-foreground">
          <div className="flex gap-3">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入消息..."
              className="h-11 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
              disabled={loading}
            />
            <button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              className="btn-primary h-11 w-11 flex items-center justify-center p-0 disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}
