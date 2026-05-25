import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { FileEdit, ArrowDownToLine } from 'lucide-react'

interface DocActionPreviewProps {
  open: boolean
  onClose: () => void
  result: string
  action: string
  onApply: (result: string, action: string) => void
}

const actionLabels: Record<string, string> = {
  SUMMARIZE: '总结',
  TRANSLATE: '翻译',
  POLISH: '润色',
  EXPAND: '扩写',
  OUTLINE: '大纲',
}

export function DocActionPreview({ open, onClose, result, action, onApply }: DocActionPreviewProps) {
  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-2xl max-h-[80vh] border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none flex flex-col">
        <DialogHeader>
          <DialogTitle>AI {actionLabels[action] || action}结果</DialogTitle>
        </DialogHeader>
        <ScrollArea className="flex-1 max-h-[50vh]">
          <div className="p-4 bg-[#FFFAF0] border-2 border-foreground">
            <div className="whitespace-pre-wrap break-words text-sm leading-relaxed text-foreground">
              {result}
            </div>
          </div>
        </ScrollArea>
        <div className="flex gap-3 pt-4">
          <Button
            onClick={() => onApply(result, action)}
            className="flex-1 rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white flex items-center justify-center gap-2"
          >
            <FileEdit className="w-4 h-4" />
            替换文档内容
          </Button>
          <Button
            onClick={() => onApply(result, 'APPEND')}
            className="flex-1 rounded-none bg-foreground hover:bg-[#1A0F08] text-white flex items-center justify-center gap-2"
          >
            <ArrowDownToLine className="w-4 h-4" />
            追加到文末
          </Button>
          <Button
            variant="outline"
            onClick={onClose}
            className="rounded-none"
          >
            取消
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
