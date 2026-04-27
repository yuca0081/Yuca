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

      <h3 class="page-title">目标设置</h3>

      <!-- 快捷模板 -->
      <div class="template-section">
        <p class="section-label">快捷模板</p>
        <div class="template-btns">
          <button v-for="t in templates" :key="t.name" class="template-btn" @click="applyTemplate(t)">
            {{ t.name }}
          </button>
        </div>
      </div>

      <!-- 热量目标 -->
      <div class="form-group">
        <label>每日热量目标 (kcal)</label>
        <n-input-number v-model:value="form.dailyCalories" :min="500" :max="10000" :step="50" style="width: 100%" />
      </div>

      <!-- 营养比例 -->
      <div class="form-group">
        <label>蛋白质占比 {{ form.proteinRatio }}%</label>
        <n-slider v-model:value="form.proteinRatio" :min="0" :max="100" :step="1" />
      </div>
      <div class="form-group">
        <label>脂肪占比 {{ form.fatRatio }}%</label>
        <n-slider v-model:value="form.fatRatio" :min="0" :max="100" :step="1" />
      </div>
      <div class="form-group">
        <label>碳水占比 {{ form.carbsRatio }}%</label>
        <n-slider v-model:value="form.carbsRatio" :min="0" :max="100" :step="1" />
      </div>

      <!-- 比例总和提示 -->
      <div class="ratio-bar">
        <div class="ratio-fill" :style="ratioBarStyle">
          <div class="ratio-segment protein" :style="{ width: form.proteinRatio + '%' }"></div>
          <div class="ratio-segment fat" :style="{ width: form.fatRatio + '%' }"></div>
          <div class="ratio-segment carbs" :style="{ width: form.carbsRatio + '%' }"></div>
        </div>
        <span class="ratio-sum" :class="{ error: ratioSum !== 100 }">
          合计: {{ ratioSum }}%
        </span>
      </div>

      <div class="ratio-legend">
        <span class="legend-item"><span class="dot protein"></span>蛋白质</span>
        <span class="legend-item"><span class="dot fat"></span>脂肪</span>
        <span class="legend-item"><span class="dot carbs"></span>碳水</span>
      </div>

      <div class="form-actions">
        <n-button type="primary" @click="saveGoal" :loading="saving" :disabled="ratioSum !== 100">
          保存目标
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NInputNumber, NSlider, useMessage } from 'naive-ui'
import { ArrowBackOutline, RestaurantOutline, StatsChartOutline, TrendingUpOutline, SettingsOutline } from '@vicons/ionicons5'
import { useDietStore } from '@/stores/diet'

const router = useRouter()
const store = useDietStore()
const message = useMessage()

const tabs = [
  { key: 'record', label: '饮食记录', path: '/diet/record', icon: RestaurantOutline },
  { key: 'daily', label: '每日统计', path: '/diet/daily', icon: StatsChartOutline },
  { key: 'trend', label: '趋势', path: '/diet/trend', icon: TrendingUpOutline },
  { key: 'goal', label: '目标', path: '/diet/goal', icon: SettingsOutline }
]
const currentTab = 'goal'

const templates = [
  { name: '减脂', dailyCalories: 1500, proteinRatio: 30, fatRatio: 30, carbsRatio: 40 },
  { name: '增肌', dailyCalories: 2500, proteinRatio: 30, fatRatio: 20, carbsRatio: 50 },
  { name: '维持', dailyCalories: 2000, proteinRatio: 20, fatRatio: 30, carbsRatio: 50 }
]

const form = reactive({
  dailyCalories: 2000,
  proteinRatio: 20,
  fatRatio: 30,
  carbsRatio: 50
})

const saving = ref(false)

const ratioSum = computed(() => form.proteinRatio + form.fatRatio + form.carbsRatio)

const ratioBarStyle = computed(() => ({
  width: Math.min(ratioSum.value, 100) + '%'
}))

function applyTemplate(t: typeof templates[0]) {
  form.dailyCalories = t.dailyCalories
  form.proteinRatio = t.proteinRatio
  form.fatRatio = t.fatRatio
  form.carbsRatio = t.carbsRatio
}

async function saveGoal() {
  if (ratioSum.value !== 100) {
    message.warning('蛋白质、脂肪、碳水占比之和必须为100%')
    return
  }
  saving.value = true
  try {
    await store.updateGoal({
      dailyCalories: form.dailyCalories,
      proteinRatio: form.proteinRatio,
      fatRatio: form.fatRatio,
      carbsRatio: form.carbsRatio
    })
    message.success('目标已保存')
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function goBackHome() { router.push('/') }

onMounted(async () => {
  await store.loadGoal()
  if (store.goal) {
    form.dailyCalories = store.goal.dailyCalories
    form.proteinRatio = store.goal.proteinRatio
    form.fatRatio = store.goal.fatRatio
    form.carbsRatio = store.goal.carbsRatio
  }
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

.template-section { margin-bottom: 20px; }
.section-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.template-btns { display: flex; gap: 8px; }
.template-btn { padding: 8px 20px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.1); background: rgba(255,255,255,0.6); cursor: pointer; font-size: 13px; color: #1f2937; transition: all 0.2s; }
.template-btn:hover { background: #14b8a6; color: white; border-color: #14b8a6; }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 6px; color: #6b7280; }

.ratio-bar { display: flex; align-items: center; gap: 12px; margin: 16px 0; }
.ratio-fill { height: 24px; border-radius: 12px; overflow: hidden; display: flex; background: rgba(0,0,0,0.04); min-width: 200px; }
.ratio-segment { height: 100%; transition: width 0.2s; }
.ratio-segment.protein { background: #14b8a6; }
.ratio-segment.fat { background: #f59e0b; }
.ratio-segment.carbs { background: #6366f1; }
.ratio-sum { font-size: 13px; color: #1f2937; font-weight: 500; white-space: nowrap; }
.ratio-sum.error { color: #ef4444; }

.ratio-legend { display: flex; gap: 16px; margin-bottom: 20px; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #6b7280; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.protein { background: #14b8a6; }
.dot.fat { background: #f59e0b; }
.dot.carbs { background: #6366f1; }

.form-actions { display: flex; justify-content: flex-end; }
</style>
