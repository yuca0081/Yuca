import { createBrowserRouter, Navigate } from 'react-router-dom'
import MainLayout from '@/layouts/MainLayout'
import Home from '@/pages/Home'
import Login from '@/pages/Login'
import ResetPassword from '@/pages/ResetPassword'
import Profile from '@/pages/Profile'
import Blog from '@/pages/Blog'
import Tools from '@/pages/Tools'
import Notes from '@/pages/Notes'
import Wiki from '@/pages/Wiki'
import Assistant from '@/pages/Assistant'
import Diet from '@/pages/Diet'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/reset-password',
    element: <ResetPassword />,
  },
  {
    element: <MainLayout />,
    children: [
      { path: '/', element: <Home /> },
      { path: '/profile', element: <Profile /> },
      { path: '/blog', element: <Blog /> },
      { path: '/tools', element: <Tools /> },
      { path: '/notes', element: <Notes /> },
      { path: '/wiki', element: <Wiki /> },
      { path: '/assistant', element: <Assistant /> },
      { path: '/diet', element: <Diet /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

export default router
