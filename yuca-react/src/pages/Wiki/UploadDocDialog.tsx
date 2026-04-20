import { useState, useRef } from 'react'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Upload, FileText } from 'lucide-react'

interface UploadDocDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (file: File) => Promise<void>
}

export function UploadDocDialog({ open, onOpenChange, onSubmit }: UploadDocDialogProps) {
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleSubmit = async () => {
    if (!file) return
    setLoading(true)
    try {
      await onSubmit(file)
      setFile(null)
      onOpenChange(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => {
      onOpenChange(v)
      if (!v) setFile(null)
    }}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>上传文档</DialogTitle>
        </DialogHeader>
        <div
          className="border-2 border-dashed border-foreground p-8 text-center cursor-pointer hover:bg-[#FFF5E6] transition-colors"
          onClick={() => inputRef.current?.click()}
        >
          <input
            ref={inputRef}
            type="file"
            className="hidden"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
            accept=".txt,.md,.pdf,.doc,.docx,.csv"
          />
          {file ? (
            <div className="flex items-center justify-center gap-2 text-sm">
              <FileText className="w-5 h-5 text-[#FF6B35]" />
              <span className="font-medium">{file.name}</span>
              <span className="text-[#6B5344]">({(file.size / 1024).toFixed(1)} KB)</span>
            </div>
          ) : (
            <div>
              <Upload className="w-8 h-8 mx-auto mb-2 text-[#E8DDD4]" />
              <p className="text-sm text-[#6B5344]">点击选择文件</p>
              <p className="text-xs text-[#E8DDD4] mt-1">支持 TXT, MD, PDF, DOC, DOCX, CSV</p>
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading} className="rounded-none">
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={loading || !file} className="rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white">
            {loading ? '上传中...' : '上传'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
