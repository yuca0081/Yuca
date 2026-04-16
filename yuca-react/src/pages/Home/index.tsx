import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useUserStore } from '@/stores/user'
import { BookOpen, Wrench, FileText, BookMarked, Bot, ArrowRight, Sun, Cloud, CloudRain, Clock } from 'lucide-react'

const quickLinks = [
  { path: '/tools', label: '开发者工具', desc: 'JSON、JWT、Base64...', icon: Wrench },
  { path: '/notes', label: '我的笔记', desc: '随时记录想法', icon: FileText },
  { path: '/blog', label: '博客文章', desc: '分享知识与经验', icon: BookOpen },
  { path: '/wiki', label: '知识库', desc: '构建知识体系', icon: BookMarked },
  { path: '/assistant', label: 'AI 助手', desc: '智能对话助手', icon: Bot },
]

/* ── Clock Widget ── */
function ClockWidget() {
  const [now, setNow] = useState(new Date())

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  const hours = now.getHours()
  const greeting = hours < 6 ? '夜深了' : hours < 12 ? '早上好' : hours < 18 ? '下午好' : '晚上好'
  const timeStr = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
  const dateStr = now.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

  return { now, greeting, timeStr, dateStr, hours }
}

/* ── Weather Widget (mock) ── */
const weatherMap: Record<string, { icon: typeof Sun; label: string }> = {
  sunny: { icon: Sun, label: '晴天' },
  cloudy: { icon: Cloud, label: '多云' },
  rainy: { icon: CloudRain, label: '小雨' },
}

function WeatherCard() {
  // Mock data — 后续可接入天气 API
  const weather = weatherMap.sunny
  const WeatherIcon = weather.icon

  return (
    <div className="block-card p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-medium text-[#6B5344] uppercase tracking-wider">天气</span>
        <WeatherIcon className="w-5 h-5 text-[#FF6B35]" />
      </div>
      <div className="flex items-end gap-2">
        <span className="text-3xl font-bold">26°</span>
        <span className="text-sm text-[#6B5344] mb-1">晴天</span>
      </div>
      <div className="text-xs text-[#6B5344] mt-2">湿度 45% · 微风</div>
    </div>
  )
}

/* ── News Widget (mock) ── */
const mockNews = [
  { title: 'React 19 正式发布，带来全新并发特性', source: '技术前沿', time: '2小时前' },
  { title: 'Spring Boot 4.0 预览版推出', source: 'Java生态', time: '5小时前' },
  { title: 'AI 编程助手对比：谁才是最强搭档？', source: '开发者资讯', time: '昨天' },
]

function NewsCard() {
  return (
    <div className="block-card p-5 lg:col-span-2">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-medium text-[#6B5344] uppercase tracking-wider">资讯</span>
        <span className="text-xs text-[#FF6B35] cursor-pointer hover:underline">更多</span>
      </div>
      <div className="space-y-3">
        {mockNews.map((item, i) => (
          <div key={i} className="flex items-start gap-3 cursor-pointer group">
            <span className="text-xs font-bold text-[#FF6B35] mt-0.5 w-4 shrink-0">{String(i + 1).padStart(2, '0')}</span>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-foreground group-hover:text-[#FF6B35] transition-colors truncate">{item.title}</p>
              <p className="text-xs text-[#6B5344] mt-0.5">{item.source} · {item.time}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

/* ── Quick Memo Widget ── */
function MemoCard() {
  const [memo, setMemo] = useState(() => localStorage.getItem('yuca_memo') || '')

  const save = (val: string) => {
    setMemo(val)
    localStorage.setItem('yuca_memo', val)
  }

  return (
    <div className="block-card p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-medium text-[#6B5344] uppercase tracking-wider">快捷备忘</span>
        <Link to="/notes" className="text-xs text-[#FF6B35] no-underline hover:underline cursor-pointer">打开笔记</Link>
      </div>
      <textarea
        value={memo}
        onChange={(e) => save(e.target.value)}
        placeholder="记点什么..."
        className="w-full h-20 text-sm bg-transparent resize-none outline-none text-foreground placeholder:text-[#E8DDD4] leading-relaxed"
      />
    </div>
  )
}

/* ── Main Home Page ── */
export default function Home() {
  const { userInfo } = useUserStore()
  const { greeting, timeStr, dateStr } = ClockWidget()

  return (
    <div className="space-y-12">
      {/* Welcome Section */}
      <div className="grid lg:grid-cols-2 gap-8 sm:gap-12 items-center">
        <div>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-[#FFF5E6] border-2 border-foreground text-sm font-medium mb-6">
            <span className="text-[#FF6B35]">&#9733;</span>
            <span>个人生产力空间</span>
          </div>
          <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold mb-6 leading-tight">
            {greeting}，<br />
            <span className="text-[#FF6B35]">{userInfo?.nickname || userInfo?.username || '朋友'}</span>
          </h1>
          <p className="text-lg text-[#6B5344] mb-8 max-w-md">
            欢迎回到 Yuca。探索工具、记录笔记、构建知识体系，让每一天都更高效。
          </p>
          <div className="flex flex-wrap gap-4 mb-10">
            <Link to="/tools" className="btn-primary no-underline">开始使用</Link>
            <Link to="/notes" className="btn-secondary no-underline">我的笔记</Link>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <Clock className="w-5 h-5 text-[#FF6B35]" />
            <span className="text-[#6B5344]">{dateStr}</span>
            <span className="text-[#E8DDD4] mx-1">·</span>
            <span className="text-foreground font-medium font-mono">{timeStr}</span>
          </div>
        </div>

        {/* Right side — Widget Grid */}
        <div className="grid grid-cols-2 gap-4">
          {/* Clock — spans full width */}
          <div className="block-card p-5 col-span-2">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-medium text-[#6B5344] uppercase tracking-wider">时钟</span>
                <div className="text-4xl sm:text-5xl font-bold font-mono mt-2 tracking-wider">{timeStr}</div>
                <div className="text-sm text-[#6B5344] mt-1">{dateStr}</div>
              </div>
              <Clock className="w-12 h-12 text-[#E8DDD4]" />
            </div>
          </div>

          <WeatherCard />
          <MemoCard />
          <NewsCard />
        </div>
      </div>

      {/* Quick Access Grid */}
      <div>
        <div className="divider max-w-xs mx-auto mb-4">
          <svg className="w-6 h-6 text-[#FF6B35]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
          </svg>
        </div>
        <h2 className="text-2xl sm:text-3xl font-bold text-center mb-4">快捷入口</h2>
        <p className="text-[#6B5344] text-center max-w-md mx-auto mb-10">探索你的个人工具集</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {quickLinks.map(({ path, label, desc, icon: Icon }) => (
            <Link
              key={path}
              to={path}
              className="block-card p-6 no-underline group"
            >
              <div className="w-10 h-10 bg-[#FF6B35] flex items-center justify-center mb-4 border-2 border-foreground">
                <Icon className="w-5 h-5 text-white" />
              </div>
              <h3 className="font-bold text-lg text-foreground group-hover:text-[#FF6B35] transition-colors">
                {label}
              </h3>
              <p className="text-sm text-[#6B5344] mt-1">{desc}</p>
              <ArrowRight className="w-4 h-4 text-[#E8DDD4] mt-4 group-hover:text-[#FF6B35] group-hover:translate-x-1 transition-all" />
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
