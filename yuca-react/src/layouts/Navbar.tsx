import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { useUserStore } from '@/stores/user'
import { LogOut, User } from 'lucide-react'

const navItems = [
  { path: '/', label: '首页' },
  { path: '/blog', label: '博客' },
  { path: '/tools', label: '工具' },
  { path: '/notes', label: '笔记' },
  { path: '/wiki', label: '知识库' },
  { path: '/assistant', label: 'AI 助手' },
]

export default function Navbar() {
  const location = useLocation()
  const { userInfo, clearAuth } = useUserStore()
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    clearAuth()
    window.location.href = '/login'
  }

  return (
    <nav className="fixed top-4 left-4 right-4 z-50">
      <div className="max-w-6xl mx-auto bg-[#FFFAF0]/60 backdrop-blur-md border-2 border-foreground px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Logo — Serif font */}
          <Link to="/" className="cursor-pointer no-underline">
            <span className="text-2xl font-bold tracking-wide text-foreground" style={{ fontFamily: "'Playfair Display SC', serif" }}>Yuca</span>
          </Link>

          {/* Desktop Nav Links — centered text only */}
          <div className="hidden md:flex items-center gap-6 sm:gap-8">
            {navItems.map(({ path, label }) => {
              const isActive = location.pathname === path || (path !== '/' && location.pathname.startsWith(path))
              return (
                <Link
                  key={path}
                  to={path}
                  className={`text-sm font-medium no-underline transition-colors cursor-pointer ${
                    isActive
                      ? 'text-[#FF6B35]'
                      : 'text-[#6B5344] hover:text-[#FF6B35]'
                  }`}
                >
                  {label}
                </Link>
              )
            })}
          </div>

          {/* Desktop Right — Avatar */}
          <div className="hidden md:block">
            <DropdownMenu>
              <DropdownMenuTrigger className="outline-none cursor-pointer">
                <Avatar className="w-9 h-9 border-2 border-foreground">
                  <AvatarImage src={userInfo?.avatarUrl} alt={userInfo?.nickname || userInfo?.username} />
                  <AvatarFallback className="bg-[#FFF5E6] text-foreground text-sm font-medium">
                    {(userInfo?.nickname || userInfo?.username || 'U')[0].toUpperCase()}
                  </AvatarFallback>
                </Avatar>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <div className="px-2 py-1.5">
                  <p className="text-sm font-medium">{userInfo?.nickname || userInfo?.username}</p>
                  <p className="text-xs text-[#6B5344]">{userInfo?.email}</p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="cursor-pointer" onClick={() => window.location.href = '/profile'}>
                  <User className="w-4 h-4 mr-2" />
                  个人资料
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="cursor-pointer text-red-600" onClick={handleLogout}>
                  <LogOut className="w-4 h-4 mr-2" />
                  退出登录
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Mobile Hamburger */}
          <button
            className="md:hidden p-2 cursor-pointer"
            onClick={() => setMobileOpen(!mobileOpen)}
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              {mobileOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileOpen && (
          <div className="md:hidden pt-6 mt-4 border-t-2 border-foreground">
            <div className="flex flex-col gap-4">
              {navItems.map(({ path, label }) => (
                <Link
                  key={path}
                  to={path}
                  className={`text-sm font-medium no-underline cursor-pointer ${
                    location.pathname === path ? 'text-[#FF6B35]' : 'text-[#6B5344] hover:text-[#FF6B35]'
                  }`}
                  onClick={() => setMobileOpen(false)}
                >
                  {label}
                </Link>
              ))}
            </div>
          </div>
        )}
      </div>
    </nav>
  )
}
