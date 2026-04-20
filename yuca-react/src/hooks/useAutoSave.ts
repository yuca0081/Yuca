import { useRef, useCallback, useEffect } from 'react'

interface UseAutoSaveOptions<T> {
  interval?: number
  onSave: (data: T) => Promise<void>
  enabled?: boolean
}

export function useAutoSave<T>(data: T, options: UseAutoSaveOptions<T>) {
  const { interval = 1500, onSave, enabled = true } = options
  const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined)
  const latestDataRef = useRef<T>(data)
  const savingRef = useRef(false)

  latestDataRef.current = data

  const save = useCallback(async () => {
    if (savingRef.current || !enabled) return
    savingRef.current = true
    try {
      await onSave(latestDataRef.current)
    } finally {
      savingRef.current = false
    }
  }, [onSave, enabled])

  useEffect(() => {
    if (!enabled) return
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(save, interval)
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [data, interval, save, enabled])

  // Flush on unmount
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        save()
      }
    }
  }, [save])
}
