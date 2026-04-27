import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  DietRecord,
  DailySummary,
  DietGoal,
  TrendData,
  CreateRecordRequest,
  UpdateRecordRequest,
  UpdateGoalRequest
} from '@/types/diet'
import * as dietApi from '@/api/diet'

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export const useDietStore = defineStore('diet', () => {
  // ========== 状态 ==========
  const records = ref<DietRecord[]>([])
  const currentDate = ref(formatDate(new Date()))
  const dailySummary = ref<DailySummary | null>(null)
  const goal = ref<DietGoal | null>(null)
  const weeklyTrend = ref<TrendData | null>(null)
  const monthlyTrend = ref<TrendData | null>(null)
  const loading = ref(false)

  // ========== 记录操作 ==========
  const loadRecords = async (date?: string) => {
    try {
      loading.value = true
      const d = date || currentDate.value
      records.value = await dietApi.getRecordList(d)
    } catch (error) {
      console.error('加载记录失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const loadDailySummary = async (date?: string) => {
    try {
      const d = date || currentDate.value
      dailySummary.value = await dietApi.getDailySummary(d)
    } catch (error) {
      console.error('加载每日汇总失败:', error)
      throw error
    }
  }

  const createRecord = async (data: CreateRecordRequest) => {
    try {
      const id = await dietApi.createRecord(data)
      await loadRecords()
      await loadDailySummary()
      return id
    } catch (error) {
      console.error('创建记录失败:', error)
      throw error
    }
  }

  const updateRecord = async (id: number, data: UpdateRecordRequest) => {
    try {
      await dietApi.updateRecord(id, data)
      await loadRecords()
      await loadDailySummary()
    } catch (error) {
      console.error('更新记录失败:', error)
      throw error
    }
  }

  const deleteRecord = async (id: number) => {
    try {
      await dietApi.deleteRecord(id)
      await loadRecords()
      await loadDailySummary()
    } catch (error) {
      console.error('删除记录失败:', error)
      throw error
    }
  }

  // ========== 目标操作 ==========
  const loadGoal = async () => {
    try {
      goal.value = await dietApi.getDietGoal()
    } catch (error) {
      console.error('加载目标失败:', error)
      throw error
    }
  }

  const updateGoal = async (data: UpdateGoalRequest) => {
    try {
      goal.value = await dietApi.updateDietGoal(data)
    } catch (error) {
      console.error('更新目标失败:', error)
      throw error
    }
  }

  // ========== 趋势操作 ==========
  const loadWeeklyTrend = async (date?: string) => {
    try {
      weeklyTrend.value = await dietApi.getWeeklyTrend(date)
    } catch (error) {
      console.error('加载周趋势失败:', error)
      throw error
    }
  }

  const loadMonthlyTrend = async (date?: string) => {
    try {
      monthlyTrend.value = await dietApi.getMonthlyTrend(date)
    } catch (error) {
      console.error('加载月趋势失败:', error)
      throw error
    }
  }

  // ========== 日期切换 ==========
  const setDate = async (date: string) => {
    currentDate.value = date
    await Promise.all([loadRecords(date), loadDailySummary(date)])
  }

  return {
    records,
    currentDate,
    dailySummary,
    goal,
    weeklyTrend,
    monthlyTrend,
    loading,
    loadRecords,
    loadDailySummary,
    createRecord,
    updateRecord,
    deleteRecord,
    loadGoal,
    updateGoal,
    loadWeeklyTrend,
    loadMonthlyTrend,
    setDate
  }
})
