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

      <h3 class="page-title">趋势统计</h3>

      <!-- 切换周/月 -->
      <div class="view-toggle">
        <button class="toggle-btn" :class="{ active: viewMode === 'weekly' }" @click="switchView('weekly')">周视图</button>
        <button class="toggle-btn" :class="{ active: viewMode === 'monthly' }" @click="switchView('monthly')">月视图</button>
      </div>

      <div class="placeholder">
        <p>趋势图表功能将在后续版本中上线</p>
        <p class="hint">将使用 ECharts 展示热量趋势折线图和日历视图</p>
      </div>

      <!-- 统计摘要（使用已有的API数据） -->
      <div v-if="trendData" class="trend-summary">
        <div class="summary-item">
          <span class="summary-label">日均热量</span>
          <span class="summary-value">{{ Math.round(trendData.averageCalories) }} kcal</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">最高</span>
          <span class="summary-value">{{ Math.round(trendData.maxCalories) }} kcal</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">最低</span>
          <span class="summary-value">{{ Math.round(trendData.minCalories) }} kcal</span>
        </div>
        <div v-if="trendData.targetDays !== undefined" class="summary-item">
          <span class="summary-label">达标天数</span>
          <span class="summary-value">{{ trendData.targetDays }} 天</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon } from 'naive-ui'
import { ArrowBackOutline, RestaurantOutline, StatsChartOutline, TrendingUpOutline, SettingsOutline } from '@vicons/ionicons5'
import { useDietStore } from '@/stores/diet'
import type { TrendData } from '@/types/diet'

const router = useRouter()
const store = useDietStore()

const tabs = [
  { key: 'record', label: '饮食记录', path: '/diet/record', icon: RestaurantOutline },
  { key: 'daily', label: '每日统计', path: '/diet/daily', icon: StatsChartOutline },
  { key: 'trend', label: '趋势', path: '/diet/trend', icon: TrendingUpOutline },
  { key: 'goal', label: '目标', path: '/diet/goal', icon: SettingsOutline }
]
const currentTab = 'trend'

const viewMode = ref<'weekly' | 'monthly'>('weekly')

const trendData = computed<TrendData | null>(() =>
  viewMode.value === 'weekly' ? store.weeklyTrend : store.monthlyTrend
)

async function switchView(mode: 'weekly' | 'monthly') {
  viewMode.value = mode
  if (mode === 'weekly') {
    await store.loadWeeklyTrend()
  } else {
    await store.loadMonthlyTrend()
  }
}

function goBackHome() { router.push('/') }

onMounted(() => {
  store.loadWeeklyTrend()
})
</script>

<style scoped>
.diet-page-container {
  min-height: 100vh; padding: 20px;
  background: linear-gradient(135deg, rgba(0,0,0,0.7) 0%, rgba(30,30,30,0.8) 100%);
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

.page-title { font-size: 18px; font-weight: bold; margin-bottom: 20px; color: #1f2937; }

.view-toggle { display: flex; gap: 4px; margin-bottom: 20px; }
.toggle-btn { padding: 8px 20px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.1); background: transparent; cursor: pointer; font-size: 13px; color: #6b7280; transition: all 0.2s; }
.toggle-btn:hover { background: rgba(0,0,0,0.05); }
.toggle-btn.active { background: #14b8a6; color: white; border-color: #14b8a6; }

.placeholder { text-align: center; padding: 40px 20px; color: #6b7280; }
.hint { font-size: 13px; margin-top: 8px; opacity: 0.7; }

.trend-summary { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-top: 20px; }
.summary-item { padding: 14px; background: rgba(255,255,255,0.6); border-radius: 10px; border: 1px solid rgba(0,0,0,0.05); text-align: center; }
.summary-label { display: block; font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.summary-value { font-size: 18px; font-weight: 700; color: #1f2937; }
</style>
