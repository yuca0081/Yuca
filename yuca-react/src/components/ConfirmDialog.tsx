import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  onConfirm: () => void
  loading?: boolean
}

export function ConfirmDialog({ open, onOpenChange, title, description, onConfirm, loading }: ConfirmDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading} className="rounded-none">
            取消
          </Button>
          <Button onClick={onConfirm} disabled={loading} className="bg-red-600 hover:bg-red-700 text-white rounded-none">
            {loading ? '删除中...' : '确认删除'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
