import { useState, useRef } from 'react'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Upload, FileText, X, Check, Loader2 } from 'lucide-react'

type UploadStatus = 'pending' | 'uploading' | 'success' | 'failed'

interface UploadItem {
  id: string
  file: File
  status: UploadStatus
  error?: string
}

interface UploadDocDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (file: File) => Promise<void>
}

const makeId = (f: File) => `${f.name}__${f.size}__${f.lastModified}`

export function UploadDocDialog({ open, onOpenChange, onSubmit }: UploadDocDialogProps) {
  const [items, setItems] = useState<UploadItem[]>([])
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const pendingCount = items.filter(i => i.status === 'pending').length
  const failedCount = items.filter(i => i.status === 'failed').length
  const doneCount = items.filter(i => i.status === 'success' || i.status === 'failed').length
  const hasPending = pendingCount > 0
  const hasFailed = failedCount > 0

  const addFiles = (incoming: FileList | null) => {
    if (!incoming || incoming.length === 0) return
    const next: UploadItem[] = []
    Array.from(incoming).forEach(file => {
      const id = makeId(file)
      if (items.some(it => it.id === id) || next.some(it => it.id === id)) return
      next.push({ id, file, status: 'pending' })
    })
    if (next.length > 0) setItems(prev => [...prev, ...next])
  }

  const removeItem = (id: string) => {
    if (uploading) return
    setItems(prev => prev.filter(it => it.id !== id))
  }

  const updateItem = (id: string, patch: Partial<UploadItem>) => {
    setItems(prev => prev.map(it => (it.id === id ? { ...it, ...patch } : it)))
  }

  const handleUpload = async () => {
    const queue = items.filter(i => i.status === 'pending' || i.status === 'failed')
    if (queue.length === 0) return
    setUploading(true)
    let failed = 0
    try {
      for (const item of queue) {
        updateItem(item.id, { status: 'uploading', error: undefined })
        try {
          await onSubmit(item.file)
          updateItem(item.id, { status: 'success' })
        } catch (err) {
          failed++
          updateItem(item.id, {
            status: 'failed',
            error: err instanceof Error ? err.message : '上传失败',
          })
        }
      }
      if (failed === 0) {
        setItems([])
        onOpenChange(false)
      } else {
        // 保留失败项让用户重试，移除已成功项
        setItems(prev => prev.filter(it => it.status !== 'success'))
      }
    } finally {
      setUploading(false)
    }
  }

  const handleOpenChange = (next: boolean) => {
    if (uploading && !next) return
    onOpenChange(next)
    if (!next) setItems([])
  }

  const buttonLabel = uploading
    ? `上传中 ${doneCount}/${items.length}...`
    : hasFailed
      ? `重试失败项 (${failedCount})`
      : `上传${pendingCount > 0 ? ` (${pendingCount})` : ''}`

  const buttonDisabled = uploading || (!hasPending && !hasFailed)

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none">
        <DialogHeader>
          <DialogTitle>上传文档</DialogTitle>
        </DialogHeader>

        <div
          className="border-2 border-dashed border-foreground p-6 text-center cursor-pointer hover:bg-[#FFF5E6] transition-colors"
          onClick={() => !uploading && inputRef.current?.click()}
        >
          <input
            ref={inputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => {
              addFiles(e.target.files)
              e.target.value = ''
            }}
            accept=".txt,.md,.pdf,.doc,.docx,.csv"
          />
          <Upload className="w-7 h-7 mx-auto mb-2 text-[#E8DDD4]" />
          <p className="text-sm text-[#6B5344]">点击选择文件（可多选）</p>
          <p className="text-xs text-[#E8DDD4] mt-1">支持 TXT, MD, PDF, DOC, DOCX, CSV</p>
        </div>

        {items.length > 0 && (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {items.map(item => (
              <div
                key={item.id}
                className="flex items-center gap-2 p-2 border-2 border-foreground bg-white"
              >
                <FileText className="w-4 h-4 text-[#FF6B35] shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm truncate">{item.file.name}</p>
                  <p className="text-xs text-[#6B5344]">
                    {(item.file.size / 1024).toFixed(1)} KB
                  </p>
                  {item.status === 'failed' && item.error && (
                    <p className="text-xs text-red-500 truncate">{item.error}</p>
                  )}
                </div>
                {item.status === 'pending' && !uploading && (
                  <button
                    onClick={() => removeItem(item.id)}
                    className="p-1 hover:text-red-500 cursor-pointer"
                    title="移除"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
                {item.status === 'uploading' && (
                  <Loader2 className="w-4 h-4 animate-spin text-[#FF6B35]" />
                )}
                {item.status === 'success' && <Check className="w-4 h-4 text-green-600" />}
                {item.status === 'failed' && <X className="w-4 h-4 text-red-500" />}
              </div>
            ))}
          </div>
        )}

        {uploading && items.length > 0 && (
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-[#6B5344]">
              <span>上传中... {doneCount}/{items.length}</span>
              <span>{Math.round((doneCount / items.length) * 100)}%</span>
            </div>
            <div className="border-2 border-foreground h-3 bg-white relative overflow-hidden">
              <div
                className="absolute inset-y-0 left-0 bg-[#FF6B35] transition-all"
                style={{ width: `${(doneCount / items.length) * 100}%` }}
              />
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={uploading}
            className="rounded-none"
          >
            取消
          </Button>
          <Button
            onClick={handleUpload}
            disabled={buttonDisabled}
            className="rounded-none bg-[#FF6B35] hover:bg-[#E55A2B] text-white"
          >
            {buttonLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
