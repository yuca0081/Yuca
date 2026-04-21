import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'

export default function MainLayout() {
  return (
    <div className="min-h-screen bg-[#FFFAF0]">
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 pt-28 sm:pt-32 pb-12">
        <Outlet />
      </main>
    </div>
  )
}
