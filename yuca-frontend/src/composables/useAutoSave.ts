import { ref, watch, onUnmounted, type Ref } from 'vue'

/**
 * 自动保存 Composable
 * @param content 要保存的内容
 * @param saveFn 保存函数
 * @param delay 防抖延迟（毫秒）
 */
export function useAutoSave<T>(
  content: Ref<T>,
  saveFn: (value: T) => Promise<void>,
  delay = 2000
) {
  const saving = ref(false)
  const lastSavedAt = ref<Date | null>(null)
  const lastSavedValue = ref<T | null>(null)
  let saveTimer: number | null = null

  /**
   * 执行保存
   */
  const doSave = async (value: T) => {
    // 如果值没有变化，跳过保存
    if (lastSavedValue.value === value) return

    saving.value = true
    try {
      await saveFn(value)
      lastSavedAt.value = new Date()
      lastSavedValue.value = value
    } catch (error) {
      console.error('自动保存失败:', error)
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * 防抖保存
   */
  const scheduleSave = (value: T) => {
    if (saveTimer !== null) {
      clearTimeout(saveTimer)
    }
    saveTimer = window.setTimeout(() => {
      doSave(value)
      saveTimer = null
    }, delay)
  }

  /**
   * 立即保存（跳过防抖）
   */
  const saveNow = async () => {
    if (saveTimer !== null) {
      clearTimeout(saveTimer)
      saveTimer = null
    }
    await doSave(content.value)
  }

  /**
   * 监听内容变化
   */
  watch(content, (newValue) => {
    scheduleSave(newValue)
  }, { deep: true })

  /**
   * 清理定时器
   */
  onUnmounted(() => {
    if (saveTimer !== null) {
      clearTimeout(saveTimer)
    }
  })

  return {
    saving,
    lastSavedAt,
    saveNow,
    doSave
  }
}

/**
 * 格式化保存时间显示
 */
export function formatSaveTime(date: Date): string {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)

  if (seconds < 60) return '刚刚'

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`

  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString()
}
