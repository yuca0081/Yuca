import { Navigate, Outlet, useLocation } from 'react-router-dom'
import Navbar from './Navbar'
import { useUserStore } from '@/stores/user'

export default function MainLayout() {
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const location = useLocation()

  if (!isLoggedIn()) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  return (
    <div className="min-h-screen bg-[#FFFAF0]">
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 pt-28 sm:pt-32 pb-12">
        <Outlet />
      </main>
    </div>
  )
}
