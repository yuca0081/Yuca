import { useState } from 'react'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

interface CreateKbDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (name: string, description?: string) => Promise<void>
}

export function CreateKbDialog({ open, onOpenChange, onSubmit }: CreateKbDialogProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!name.trim()) return
    setLoading(true)
    try {
      await onSubmit(name.trim(), description.trim() || undefined)
      setName('')
      setDescription('')
      onOpenChange(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>新建知识库</DialogTitle>
        </DialogHeader>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="知识库名称"
          className="border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
          autoFocus
        />
        <Input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="描述（可选）"
          className="border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
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
