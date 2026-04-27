<template>
  <div class="diet-page-container">
    <n-button class="back-home-btn" @click="goBackHome">
      <template #icon>
        <n-icon :component="ArrowBackOutline" size="18" />
      </template>
      <span>返回主页</span>
    </n-button>

    <div class="diet-main-card">
      <!-- 子导航 -->
      <div class="diet-tabs">
        <div v-for="tab in tabs" :key="tab.path" class="diet-tab" :class="{ active: currentTab === tab.key }" @click="$router.push(tab.path)">
          <n-icon :component="tab.icon" size="18" />
          <span>{{ tab.label }}</span>
        </div>
      </div>

      <!-- 日期导航 -->
      <div class="date-nav">
        <button class="date-arrow" @click="changeDate(-1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <n-date-picker v-model:value="selectedTimestamp" type="date" :bordered="false" @update:value="onDateChange" style="width:140px" />
        <button class="date-arrow" @click="changeDate(1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>

      <div v-if="summary" class="daily-content">
        <!-- 四项数据卡片 -->
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-value">{{ round(summary.totalCalories) }}</div>
            <div class="stat-label">总热量 kcal</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ round(summary.totalProtein) }}</div>
            <div class="stat-label">蛋白质 g</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ round(summary.totalFat) }}</div>
            <div class="stat-label">脂肪 g</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ round(summary.totalCarbs) }}</div>
            <div class="stat-label">碳水 g</div>
          </div>
        </div>

        <!-- 热量进度环 -->
        <div class="progress-section">
          <div class="progress-ring-wrapper">
            <svg class="progress-ring" viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="10"/>
              <circle
                cx="60" cy="60" r="50" fill="none"
                :stroke="progressColor"
                stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="progressOffset"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="progress-text">
              <div class="progress-value">{{ round(summary.totalCalories) }}</div>
              <div class="progress-target">/ {{ goalCalories }} kcal</div>
            </div>
          </div>
          <div class="progress-label">
            {{ progressPercent >= 100 ? '已达到目标' : `还需 ${round(goalCalories - summary.totalCalories)} kcal` }}
          </div>
        </div>

        <!-- 餐次分布 -->
        <div v-if="summary.mealSummaries.length > 0" class="meal-section">
          <h4 class="section-title">餐次分布</h4>
          <div class="meal-bars">
            <div v-for="meal in summary.mealSummaries" :key="meal.mealType" class="meal-bar-item">
              <div class="meal-bar-label">{{ meal.mealTypeLabel }}</div>
              <div class="meal-bar-track">
                <div class="meal-bar-fill" :style="{ width: barWidth(meal.calories) + '%' }"></div>
              </div>
              <div class="meal-bar-value">{{ round(meal.calories) }} kcal</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>暂无数据</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NDatePicker } from 'naive-ui'
import { ArrowBackOutline, RestaurantOutline, StatsChartOutline, TrendingUpOutline, SettingsOutline } from '@vicons/ionicons5'
import { useDietStore } from '@/stores/diet'

const router = useRouter()
const store = useDietStore()

const tabs = [
  { key: 'record', label: '饮食记录', path: '/diet/record', icon: RestaurantOutline },
  { key: 'daily', label: '每日统计', path: '/diet/daily', icon: StatsChartOutline },
  { key: 'trend', label: '趋势', path: '/diet/trend', icon: TrendingUpOutline },
  { key: 'goal', label: '目标', path: '/diet/goal', icon: SettingsOutline }
]
const currentTab = 'daily'

const selectedTimestamp = ref(Date.now())

const summary = computed(() => store.dailySummary)
const goalCalories = computed(() => store.goal?.dailyCalories || 2000)

const circumference = 2 * Math.PI * 50
const progressPercent = computed(() => {
  if (!summary.value) return 0
  return Math.min((summary.value.totalCalories / goalCalories.value) * 100, 100)
})
const progressOffset = computed(() => circumference - (progressPercent.value / 100) * circumference)
const progressColor = computed(() => progressPercent.value >= 100 ? '#ef4444' : '#14b8a6')

function round(val: number | undefined): string {
  if (val === undefined || val === null) return '0'
  return Math.round(val).toString()
}

function barWidth(calories: number): number {
  if (!summary.value || summary.value.totalCalories === 0) return 0
  return Math.min((calories / summary.value.totalCalories) * 100, 100)
}

function onDateChange(val: number | null) {
  if (val) store.setDate(formatTs(val))
}

function changeDate(delta: number) {
  const d = new Date(store.currentDate)
  d.setDate(d.getDate() + delta)
  selectedTimestamp.value = d.getTime()
  store.setDate(formatTs(d.getTime()))
}

function formatTs(ts: number): string {
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function goBackHome() { router.push('/') }

onMounted(async () => {
  await Promise.all([store.loadDailySummary(), store.loadGoal()])
})
</script>

<style scoped>
.diet-page-container {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
  position: relative;
}
.back-home-btn { position: absolute; top: 20px; left: 20px; z-index: 10; background: rgba(245,245,245,0.9); border: none; }
.diet-main-card {
  max-width: 640px; margin: 0 auto; padding: 24px;
  background: rgba(245,245,245,0.9); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.5); border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0,0,0,0.37); min-height: calc(100vh - 40px);
}
.diet-tabs { display: flex; gap: 4px; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid rgba(0,0,0,0.08); }
.diet-tab { display: flex; align-items: center; gap: 4px; padding: 8px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; color: #6b7280; transition: all 0.2s; }
.diet-tab:hover { background: rgba(0,0,0,0.05); }
.diet-tab.active { background: #14b8a6; color: white; }
.date-nav { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 20px; }
.date-arrow { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 8px; border: 1px solid rgba(0,0,0,0.1); background: transparent; cursor: pointer; color: #1f2937; transition: background 0.2s; }
.date-arrow:hover { background: rgba(0,0,0,0.05); }

.stat-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 24px; }
.stat-card { padding: 14px; background: rgba(255,255,255,0.6); border-radius: 10px; border: 1px solid rgba(0,0,0,0.05); text-align: center; }
.stat-value { font-size: 22px; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 2px; }

.progress-section { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; }
.progress-ring-wrapper { position: relative; width: 140px; height: 140px; }
.progress-ring { width: 100%; height: 100%; }
.progress-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); text-align: center; }
.progress-value { font-size: 20px; font-weight: 700; color: #1f2937; }
.progress-target { font-size: 11px; color: #6b7280; }
.progress-label { margin-top: 8px; font-size: 13px; color: #6b7280; }

.meal-section { margin-top: 8px; }
.section-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #1f2937; }
.meal-bars { display: flex; flex-direction: column; gap: 10px; }
.meal-bar-item { display: flex; align-items: center; gap: 10px; }
.meal-bar-label { width: 40px; font-size: 13px; color: #1f2937; text-align: right; }
.meal-bar-track { flex: 1; height: 12px; background: rgba(0,0,0,0.06); border-radius: 6px; overflow: hidden; }
.meal-bar-fill { height: 100%; background: #14b8a6; border-radius: 6px; transition: width 0.3s; min-width: 4px; }
.meal-bar-value { width: 80px; font-size: 12px; color: #6b7280; }

.empty-state { text-align: center; padding: 60px 20px; color: #6b7280; }
</style>
