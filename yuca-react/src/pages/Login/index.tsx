import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import { Input } from '@/components/ui/input'
import { LogIn, Eye, EyeOff } from 'lucide-react'

export default function Login() {
  const [account, setAccount] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { setTokens, setUserInfo } = useUserStore()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // SHA-256 哈希密码，避免明文传输
      const encoder = new TextEncoder()
      const data = encoder.encode(password)
      const hashBuffer = await crypto.subtle.digest('SHA-256', data)
      const hashArray = Array.from(new Uint8Array(hashBuffer))
      const hashedPassword = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

      const res = await login({ account, password: hashedPassword }) as any
      setTokens(res.accessToken, res.refreshToken)
      setUserInfo(res.user)
      const redirect = searchParams.get('redirect') || '/'
      navigate(redirect)
    } catch (err: any) {
      setError(err.message || '登录失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#FFFAF0] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white p-6 sm:p-8 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8">
          <span className="text-3xl font-bold tracking-wide text-foreground mb-2" style={{ fontFamily: "'Playfair Display SC', serif" }}>Yuca</span>
          <div className="w-16 h-[2px] bg-[#FF6B35] mb-4" />
          <h1 className="text-xl font-bold">欢迎回来</h1>
          <p className="text-sm text-[#6B5344] mt-1">登录到你的个人空间</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && (
            <div className="bg-red-50 border-2 border-red-300 text-red-600 px-4 py-3 text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium mb-2">账号</label>
            <Input
              value={account}
              onChange={(e) => setAccount(e.target.value)}
              placeholder="用户名 / 邮箱 / 手机号"
              className="h-12 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">密码</label>
            <div className="relative">
              <Input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                className="h-12 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none pr-10"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#6B5344] hover:text-foreground cursor-pointer"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full h-12 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <LogIn className="w-4 h-4" />
                登录
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  )
}
