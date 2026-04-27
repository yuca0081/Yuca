import { create } from 'zustand'
import type {
  DietRecord,
  DailySummary,
  DietGoal,
  TrendData,
  CreateRecordRequest,
  UpdateRecordRequest,
  UpdateGoalRequest,
} from '@/types/diet'
import * as dietApi from '@/api/diet'

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

interface DietState {
  records: DietRecord[]
  currentDate: string
  dailySummary: DailySummary | null
  goal: DietGoal | null
  weeklyTrend: TrendData | null
  monthlyTrend: TrendData | null
  loading: boolean

  loadRecords: (date?: string) => Promise<void>
  loadDailySummary: (date?: string) => Promise<void>
  createRecord: (data: CreateRecordRequest) => Promise<number>
  updateRecord: (id: number, data: UpdateRecordRequest) => Promise<void>
  deleteRecord: (id: number) => Promise<void>
  loadGoal: () => Promise<void>
  updateGoal: (data: UpdateGoalRequest) => Promise<void>
  loadWeeklyTrend: (date?: string) => Promise<void>
  loadMonthlyTrend: (date?: string) => Promise<void>
  setDate: (date: string) => Promise<void>
}

export const useDietStore = create<DietState>((set, get) => ({
  records: [],
  currentDate: formatDate(new Date()),
  dailySummary: null,
  goal: null,
  weeklyTrend: null,
  monthlyTrend: null,
  loading: false,

  loadRecords: async (date?: string) => {
    set({ loading: true })
    try {
      const d = date || get().currentDate
      const records = await dietApi.getRecordList(d)
      set({ records })
    } finally {
      set({ loading: false })
    }
  },

  loadDailySummary: async (date?: string) => {
    const d = date || get().currentDate
    const dailySummary = await dietApi.getDailySummary(d)
    set({ dailySummary })
  },

  createRecord: async (data) => {
    const id = await dietApi.createRecord(data)
    await get().loadRecords()
    await get().loadDailySummary()
    return id
  },

  updateRecord: async (id, data) => {
    await dietApi.updateRecord(id, data)
    await get().loadRecords()
    await get().loadDailySummary()
  },

  deleteRecord: async (id) => {
    await dietApi.deleteRecord(id)
    await get().loadRecords()
    await get().loadDailySummary()
  },

  loadGoal: async () => {
    const goal = await dietApi.getDietGoal()
    set({ goal })
  },

  updateGoal: async (data) => {
    const goal = await dietApi.updateDietGoal(data)
    set({ goal })
  },

  loadWeeklyTrend: async (date?: string) => {
    const weeklyTrend = await dietApi.getWeeklyTrend(date)
    set({ weeklyTrend })
  },

  loadMonthlyTrend: async (date?: string) => {
    const monthlyTrend = await dietApi.getMonthlyTrend(date)
    set({ monthlyTrend })
  },

  setDate: async (date: string) => {
    set({ currentDate: date })
    await Promise.all([get().loadRecords(date), get().loadDailySummary(date)])
  },
}))
