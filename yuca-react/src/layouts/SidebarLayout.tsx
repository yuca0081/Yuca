import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'

interface SidebarLayoutProps {
  sidebar: React.ReactNode
}

export default function SidebarLayout({ sidebar }: SidebarLayoutProps) {
  return (
    <div className="min-h-screen bg-[#FFFAF0]">
      <Navbar />
      <div className="max-w-6xl mx-auto px-6 pt-28 sm:pt-32 pb-12 flex gap-6 h-[calc(100vh-8rem)]">
        {/* Sidebar */}
        <aside className="sidebar w-64 shrink-0 overflow-hidden flex flex-col">
          {sidebar}
        </aside>
        {/* Main Content */}
        <main className="flex-1 block-card overflow-hidden flex flex-col">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
