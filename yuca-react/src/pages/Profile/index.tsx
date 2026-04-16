import { useState } from 'react'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api/user'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { User, Mail, Phone, Save, Camera } from 'lucide-react'

export default function Profile() {
  const { userInfo, setUserInfo } = useUserStore()
  const [nickname, setNickname] = useState(userInfo?.nickname || '')
  const [email, setEmail] = useState(userInfo?.email || '')
  const [phone, setPhone] = useState(userInfo?.phone || '')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')

  const handleSave = async () => {
    setSaving(true)
    setMessage('')
    try {
      const updated = await updateProfile({ nickname, email, phone }) as any
      setUserInfo(updated)
      setMessage('保存成功')
    } catch {
      setMessage('保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-8 max-w-2xl">
      {/* Profile Header */}
      <div className="block-card p-8">
        <div className="flex items-center gap-6">
          <div className="relative group">
            <Avatar className="w-20 h-20 border-2 border-foreground">
              <AvatarImage src={userInfo?.avatarUrl} />
              <AvatarFallback className="bg-[#FFF5E6] text-foreground text-2xl" style={{ fontFamily: "'Playfair Display SC', serif" }}>
                {(userInfo?.nickname || userInfo?.username || 'U')[0].toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <div className="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
              <Camera className="w-6 h-6 text-white" />
            </div>
          </div>
          <div>
            <h1 className="text-2xl font-bold">
              {userInfo?.nickname || userInfo?.username}
            </h1>
            <p className="text-[#6B5344] mt-1">@{userInfo?.username}</p>
          </div>
        </div>
      </div>

      {/* Edit Form */}
      <div className="bg-white p-6 sm:p-8 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        <h2 className="text-lg font-bold mb-6">编辑资料</h2>

        <div className="space-y-5">
          <div>
            <label className="flex items-center gap-2 text-sm font-medium mb-2">
              <User className="w-4 h-4" /> 昵称
            </label>
            <Input
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="h-11 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            />
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-medium mb-2">
              <Mail className="w-4 h-4" /> 邮箱
            </label>
            <Input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              className="h-11 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            />
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-medium mb-2">
              <Phone className="w-4 h-4" /> 手机号
            </label>
            <Input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="h-11 bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
            />
          </div>

          <div className="border-t-2 border-[#E8DDD4] pt-4" />

          <div className="flex items-center justify-between">
            {message && (
              <span className={`text-sm ${message.includes('成功') ? 'text-[#4A7C59]' : 'text-red-600'}`}>
                {message}
              </span>
            )}
            <button
              onClick={handleSave}
              disabled={saving}
              className="btn-primary flex items-center gap-2 ml-auto disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {saving ? '保存中...' : '保存修改'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
