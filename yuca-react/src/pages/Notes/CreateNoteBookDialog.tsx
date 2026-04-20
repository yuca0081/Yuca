import { useState } from 'react'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

interface CreateNoteBookDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (name: string) => Promise<void>
}

export function CreateNoteBookDialog({ open, onOpenChange, onSubmit }: CreateNoteBookDialogProps) {
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!name.trim()) return
    setLoading(true)
    try {
      await onSubmit(name.trim())
      setName('')
      onOpenChange(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>新建笔记本</DialogTitle>
        </DialogHeader>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="笔记本名称"
          className="border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
          autoFocus
        />
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading} className="rounded-none">
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={loading || !name.trim()} className="rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white">
            {loading ? '创建中...' : '创建'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
