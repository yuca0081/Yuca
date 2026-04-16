import { useState } from 'react'
import { Plus, Search, Calendar } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'

interface BlogPost {
  id: number
  title: string
  summary: string
  date: string
  tags: string[]
}

const mockPosts: BlogPost[] = []

export default function Blog() {
  const [search, setSearch] = useState('')

  const filtered = mockPosts.filter((p) =>
    p.title.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">博客</h1>
        <button className="btn-primary flex items-center gap-2 text-sm">
          <Plus className="w-4 h-4" />
          写文章
        </button>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#6B5344]" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="搜索文章..."
          className="pl-10 h-11 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
        />
      </div>

      {/* Posts Grid */}
      {filtered.length === 0 ? (
        <div className="block-card p-12 flex flex-col items-center justify-center text-[#6B5344]">
          <Book className="w-12 h-12 mb-4 text-[#E8DDD4]" />
          <p className="text-lg">还没有文章</p>
          <p className="text-sm mt-1">点击「写文章」开始你的创作</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filtered.map((post) => (
            <article key={post.id} className="menu-card p-6 cursor-pointer">
              <h3 className="font-bold text-lg mb-2">
                {post.title}
              </h3>
              <p className="text-sm text-[#6B5344] line-clamp-2 mb-4">{post.summary}</p>
              <div className="flex items-center justify-between">
                <div className="flex gap-2">
                  {post.tags.map((tag) => (
                    <Badge key={tag} variant="secondary" className="bg-[#FFF5E6] text-[#6B5344] rounded-none">
                      {tag}
                    </Badge>
                  ))}
                </div>
                <div className="flex items-center gap-1 text-[#6B5344] text-xs">
                  <Calendar className="w-3 h-3" />
                  {post.date}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}

function Book({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20" />
    </svg>
  )
}
