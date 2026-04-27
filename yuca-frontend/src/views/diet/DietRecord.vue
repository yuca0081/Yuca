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
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="diet-tab"
          :class="{ active: currentTab === tab.key }"
          @click="$router.push(tab.path)"
        >
          <n-icon :component="tab.icon" size="18" />
          <span>{{ tab.label }}</span>
        </div>
      </div>

      <!-- 日期导航 -->
      <div class="date-nav">
        <button class="date-arrow" @click="changeDate(-1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <n-date-picker
          v-model:value="selectedTimestamp"
          type="date"
          :bordered="false"
          clearable
          @update:value="onDateChange"
          style="width: 140px"
        />
        <button class="date-arrow" @click="changeDate(1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>

      <!-- 记录列表 -->
      <div class="record-list">
        <div v-if="store.records.length === 0" class="empty-state">
          <p>今天还没有饮食记录</p>
          <p class="empty-hint">点击右下角按钮添加记录</p>
        </div>

        <div v-for="meal in mealsWithRecords" :key="meal.type" class="meal-group">
          <div class="meal-header">
            <span class="meal-label">{{ meal.label }}</span>
            <span class="meal-cal">{{ meal.totalCal }} kcal</span>
          </div>
          <div
            v-for="record in meal.records"
            :key="record.id"
            class="record-card"
          >
            <div class="record-info">
              <div class="record-name">{{ record.foodName }}</div>
              <div class="record-detail">
                {{ record.amount }}{{ record.unit }} · {{ record.calories }} kcal
              </div>
              <div v-if="record.remark" class="record-remark">{{ record.remark }}</div>
            </div>
            <div class="record-actions">
              <button class="action-btn" @click="openEdit(record)" title="编辑">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </button>
              <button class="action-btn delete" @click="confirmDelete(record.id)" title="删除">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加按钮 -->
      <button class="add-btn" @click="openCreate">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>
    </div>

    <!-- 新增/编辑 Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-card">
        <h3 class="modal-title">{{ editingId ? '编辑记录' : '添加记录' }}</h3>
        <div class="form-group">
          <label>日期</label>
          <n-date-picker v-model:value="formTimestamp" type="date" style="width: 100%" @update:value="onFormDateChange" />
        </div>
        <div class="form-group">
          <label>餐次</label>
          <n-select v-model:value="form.mealType" :options="mealOptions" />
        </div>
        <div class="form-group">
          <label>食物名称</label>
          <n-input v-model:value="form.foodName" placeholder="请输入食物名称" />
        </div>
        <div class="form-row">
          <div class="form-group" style="flex:1">
            <label>食用量</label>
            <n-input-number v-model:value="form.amount" :min="0" placeholder="数量" style="width:100%" />
          </div>
          <div class="form-group" style="width:90px">
            <label>单位</label>
            <n-select v-model:value="form.unit" :options="unitOptions" />
          </div>
        </div>
        <div class="form-group">
          <label>热量 (kcal)</label>
          <n-input-number v-model:value="form.calories" :min="0" placeholder="热量" style="width:100%" />
        </div>
        <div class="form-row">
          <div class="form-group" style="flex:1">
            <label>蛋白质 (g)</label>
            <n-input-number v-model:value="form.protein" :min="0" placeholder="可选" style="width:100%" />
          </div>
          <div class="form-group" style="flex:1">
            <label>脂肪 (g)</label>
            <n-input-number v-model:value="form.fat" :min="0" placeholder="可选" style="width:100%" />
          </div>
          <div class="form-group" style="flex:1">
            <label>碳水 (g)</label>
            <n-input-number v-model:value="form.carbs" :min="0" placeholder="可选" style="width:100%" />
          </div>
        </div>
        <div class="form-group">
          <label>备注</label>
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </div>
        <div class="modal-actions">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" @click="submitForm" :loading="submitting">保存</n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NDatePicker, NSelect, NInput, NInputNumber, useMessage, useDialog } from 'naive-ui'
import { ArrowBackOutline, RestaurantOutline, StatsChartOutline, TrendingUpOutline, SettingsOutline } from '@vicons/ionicons5'
import { useDietStore } from '@/stores/diet'
import { getRecommendedMealType } from '@/api/diet'
import { MEAL_TYPE_OPTIONS } from '@/types/diet'
import type { DietRecord, CreateRecordRequest, UpdateRecordRequest } from '@/types/diet'

const router = useRouter()
const store = useDietStore()
const message = useMessage()
const dialog = useDialog()

const tabs = [
  { key: 'record', label: '饮食记录', path: '/diet/record', icon: RestaurantOutline },
  { key: 'daily', label: '每日统计', path: '/diet/daily', icon: StatsChartOutline },
  { key: 'trend', label: '趋势', path: '/diet/trend', icon: TrendingUpOutline },
  { key: 'goal', label: '目标', path: '/diet/goal', icon: SettingsOutline }
]
const currentTab = 'record'

const mealOptions = MEAL_TYPE_OPTIONS
const unitOptions = [
  { label: 'g', value: 'g' },
  { label: '份', value: '份' },
  { label: 'ml', value: 'ml' }
]

// 日期相关
const selectedTimestamp = ref(Date.now())
function onDateChange(val: number | null) {
  if (val) {
    store.setDate(formatTs(val))
  }
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

// 按餐次分组
const mealsWithRecords = computed(() => {
  const groups: { type: number; label: string; records: DietRecord[]; totalCal: number }[] = []
  const labels: Record<number, string> = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '加餐' }
  for (let t = 1; t <= 4; t++) {
    const recs = store.records.filter(r => r.mealType === t)
    if (recs.length > 0) {
      groups.push({
        type: t,
        label: labels[t] ?? '',
        records: recs,
        totalCal: recs.reduce((sum, r) => sum + (r.calories || 0), 0)
      })
    }
  }
  return groups
})

// Modal
const showModal = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formTimestamp = ref(Date.now())

const form = ref<{
  recordDate: string
  mealType: number
  foodName: string
  amount: number | null
  unit: string
  calories: number | null
  protein: number | null
  fat: number | null
  carbs: number | null
  remark: string
}>({
  recordDate: '',
  mealType: 1,
  foodName: '',
  amount: null,
  unit: 'g',
  calories: null,
  protein: null,
  fat: null,
  carbs: null,
  remark: ''
})

function onFormDateChange(val: number | null) {
  if (val) form.value.recordDate = formatTs(val)
}

async function openCreate() {
  editingId.value = null
  form.value = {
    recordDate: store.currentDate,
    mealType: 1,
    foodName: '',
    amount: null,
    unit: 'g',
    calories: null,
    protein: null,
    fat: null,
    carbs: null,
    remark: ''
  }
  formTimestamp.value = new Date(store.currentDate).getTime()
  try {
    const recommended = await getRecommendedMealType()
    form.value.mealType = recommended
  } catch {}
  showModal.value = true
}

function openEdit(record: DietRecord) {
  editingId.value = record.id
  form.value = {
    recordDate: record.recordDate,
    mealType: record.mealType,
    foodName: record.foodName,
    amount: record.amount,
    unit: record.unit,
    calories: record.calories,
    protein: record.protein || null,
    fat: record.fat || null,
    carbs: record.carbs || null,
    remark: record.remark || ''
  }
  formTimestamp.value = new Date(record.recordDate).getTime()
  showModal.value = true
}

async function submitForm() {
  if (!form.value.foodName || form.value.amount === null || form.value.calories === null) {
    message.warning('请填写食物名称、食用量和热量')
    return
  }

  submitting.value = true
  try {
    const data = {
      recordDate: form.value.recordDate,
      mealType: form.value.mealType,
      foodName: form.value.foodName,
      amount: form.value.amount,
      unit: form.value.unit,
      calories: form.value.calories,
      protein: form.value.protein ?? undefined,
      fat: form.value.fat ?? undefined,
      carbs: form.value.carbs ?? undefined,
      remark: form.value.remark || ''
    }

    if (editingId.value) {
      await store.updateRecord(editingId.value, data as UpdateRecordRequest)
      message.success('更新成功')
    } else {
      await store.createRecord(data as CreateRecordRequest)
      message.success('添加成功')
    }
    showModal.value = false
  } catch {
    message.error('操作失败')
  } finally {
    submitting.value = false
  }
}

function confirmDelete(id: number) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这条记录吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await store.deleteRecord(id)
        message.success('删除成功')
      } catch {
        message.error('删除失败')
      }
    }
  })
}

function goBackHome() {
  router.push('/')
}

onMounted(() => {
  store.setDate(store.currentDate)
})
</script>

<style scoped>
.diet-page-container {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7) 0%, rgba(30, 30, 30, 0.8) 100%);
  position: relative;
}

.back-home-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
  background: rgba(245, 245, 245, 0.9);
  border: none;
}

.diet-main-card {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
  background: rgba(245, 245, 245, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md, 16px);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  min-height: calc(100vh - 40px);
}

.diet-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.diet-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
  transition: all 0.2s;
}

.diet-tab:hover {
  background: rgba(0, 0, 0, 0.05);
}

.diet-tab.active {
  background: var(--color-primary, #14b8a6);
  color: white;
}

.date-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.date-arrow {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: transparent;
  cursor: pointer;
  color: var(--color-text-primary, #1f2937);
  transition: background 0.2s;
}

.date-arrow:hover {
  background: rgba(0, 0, 0, 0.05);
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary, #6b7280);
}

.empty-hint {
  font-size: 13px;
  margin-top: 8px;
  opacity: 0.7;
}

.meal-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.meal-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary, #1f2937);
}

.meal-cal {
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

.record-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: background 0.2s;
}

.record-card:hover {
  background: rgba(255, 255, 255, 0.85);
}

.record-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 2px;
}

.record-detail {
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}

.record-remark {
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
  margin-top: 4px;
  font-style: italic;
}

.record-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: var(--color-text-secondary, #6b7280);
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: var(--color-text-primary, #1f2937);
}

.action-btn.delete:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.add-btn {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: var(--color-primary, #14b8a6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.4);
  transition: transform 0.2s;
}

.add-btn:hover {
  transform: scale(1.1);
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-card {
  background: rgba(245, 245, 245, 0.97);
  border-radius: var(--radius-md, 16px);
  padding: 24px;
  width: 90%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.modal-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: var(--color-text-primary, #1f2937);
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--color-text-secondary, #6b7280);
}

.form-row {
  display: flex;
  gap: 10px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>
