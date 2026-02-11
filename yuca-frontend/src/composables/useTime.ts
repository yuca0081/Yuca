import { ref, onMounted, onUnmounted } from 'vue'
import { formatDateTime, getWeekday } from '@/utils/format'

export function useTime() {
  const currentTime = ref('')
  const currentDate = ref('')
  const weekday = ref('')

  let timer: number | null = null

  const updateTime = () => {
    const now = new Date()
    currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
    currentDate.value = now.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
    weekday.value = getWeekday(now.toISOString())
  }

  const startTimer = () => {
    updateTime()
    timer = window.setInterval(() => {
      updateTime()
    }, 1000)
  }

  const stopTimer = () => {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(() => {
    startTimer()
  })

  onUnmounted(() => {
    stopTimer()
  })

  return {
    currentTime,
    currentDate,
    weekday
  }
}
