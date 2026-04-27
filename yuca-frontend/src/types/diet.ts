/** 餐次类型 */
export type MealType = 1 | 2 | 3 | 4

/** 餐次选项 */
export const MEAL_TYPE_OPTIONS: { label: string; value: MealType }[] = [
  { label: '早餐', value: 1 },
  { label: '午餐', value: 2 },
  { label: '晚餐', value: 3 },
  { label: '加餐', value: 4 }
]

/** 饮食记录 */
export interface DietRecord {
  id: number
  recordDate: string
  mealType: MealType
  mealTypeLabel: string
  foodName: string
  amount: number
  unit: string
  calories: number
  protein?: number
  fat?: number
  carbs?: number
  remark?: string
  createdAt: string
  updatedAt: string
}

/** 每日统计-餐次汇总 */
export interface MealSummary {
  mealType: number
  mealTypeLabel: string
  calories: number
  protein: number
  fat: number
  carbs: number
  recordCount: number
}

/** 每日统计 */
export interface DailySummary {
  date: string
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  mealSummaries: MealSummary[]
}

/** 饮食目标 */
export interface DietGoal {
  dailyCalories: number
  proteinRatio: number
  fatRatio: number
  carbsRatio: number
  updatedAt: string
}

/** 趋势-每日数据 */
export interface DailyTrendItem {
  date: string
  calories: number
  protein: number
  fat: number
  carbs: number
}

/** 趋势统计 */
export interface TrendData {
  items: DailyTrendItem[]
  averageCalories: number
  maxCalories: number
  minCalories: number
  targetDays?: number
}

/** 创建记录请求 */
export interface CreateRecordRequest {
  recordDate: string
  mealType: number
  foodName: string
  amount: number
  unit?: string
  calories: number
  protein?: number
  fat?: number
  carbs?: number
  remark?: string
}

/** 更新记录请求 */
export type UpdateRecordRequest = Partial<CreateRecordRequest>

/** 更新目标请求 */
export interface UpdateGoalRequest {
  dailyCalories: number
  proteinRatio: number
  fatRatio: number
  carbsRatio: number
}
