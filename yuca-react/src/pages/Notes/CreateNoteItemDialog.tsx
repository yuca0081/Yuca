import { useState } from 'react'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

interface CreateNoteItemDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (type: 'FOLDER' | 'DOCUMENT', title: string) => Promise<void>
  defaultType?: 'FOLDER' | 'DOCUMENT'
}

export function CreateNoteItemDialog({ open, onOpenChange, onSubmit, defaultType = 'DOCUMENT' }: CreateNoteItemDialogProps) {
  const [type, setType] = useState<'FOLDER' | 'DOCUMENT'>(defaultType)
  const [title, setTitle] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!title.trim()) return
    setLoading(true)
    try {
      await onSubmit(type, title.trim())
      setTitle('')
      onOpenChange(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => {
      onOpenChange(v)
      if (v) setType(defaultType)
    }}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>新建条目</DialogTitle>
        </DialogHeader>
        <div className="flex gap-2">
          <button
            onClick={() => setType('DOCUMENT')}
            className={`flex-1 py-2 text-sm font-medium border-2 transition-colors cursor-pointer ${
              type === 'DOCUMENT' ? 'bg-[#FF6B35] text-white border-foreground' : 'bg-white text-[#6B5344] border-foreground hover:bg-[#FFF5E6]'
            }`}
          >
            文档
          </button>
          <button
            onClick={() => setType('FOLDER')}
            className={`flex-1 py-2 text-sm font-medium border-2 transition-colors cursor-pointer ${
              type === 'FOLDER' ? 'bg-[#FF6B35] text-white border-foreground' : 'bg-white text-[#6B5344] border-foreground hover:bg-[#FFF5E6]'
            }`}
          >
            文件夹
          </button>
        </div>
        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={type === 'FOLDER' ? '文件夹名称' : '文档标题'}
          className="border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
          autoFocus
        />
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading} className="rounded-none">
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={loading || !title.trim()} className="rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white">
            {loading ? '创建中...' : '创建'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
