import request from './index'
import type {
  DietRecord,
  DailySummary,
  DietGoal,
  TrendData,
  CreateRecordRequest,
  UpdateRecordRequest,
  UpdateGoalRequest
} from '@/types/diet'

// 饮食记录
export const createRecord = (data: CreateRecordRequest) =>
  request.post<number>('/diet/record', data)

export const updateRecord = (id: number, data: UpdateRecordRequest) =>
  request.put<void>(`/diet/record/${id}`, data)

export const deleteRecord = (id: number) =>
  request.delete<void>(`/diet/record/${id}`)

export const getRecordList = (date: string) =>
  request.get<DietRecord[]>('/diet/record/list', { params: { date } })

export const getDailySummary = (date: string) =>
  request.get<DailySummary>('/diet/record/daily-summary', { params: { date } })

export const getRecommendedMealType = () =>
  request.get<number>('/diet/record/recommend-meal')

// 饮食目标
export const getDietGoal = () =>
  request.get<DietGoal>('/diet/goal')

export const updateDietGoal = (data: UpdateGoalRequest) =>
  request.put<DietGoal>('/diet/goal', data)

// 趋势统计
export const getWeeklyTrend = (date?: string) =>
  request.get<TrendData>('/diet/trend/weekly', { params: date ? { date } : {} })

export const getMonthlyTrend = (date?: string) =>
  request.get<TrendData>('/diet/trend/monthly', { params: date ? { date } : {} })
