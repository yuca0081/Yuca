import { useState, useEffect } from 'react'
import { useDietStore } from '@/stores/diet'
import { getRecommendedMealType } from '@/api/diet'
import { MEAL_TYPE_OPTIONS } from '@/types/diet'
import type { DietRecord, CreateRecordRequest, UpdateRecordRequest } from '@/types/diet'
import { UtensilsCrossed, BarChart3, TrendingUp, Target, Plus, Pencil, Trash2, ChevronLeft, ChevronRight, X } from 'lucide-react'

type TabKey = 'record' | 'daily' | 'trend' | 'goal'

const tabs: { key: TabKey; label: string; icon: typeof UtensilsCrossed }[] = [
  { key: 'record', label: '饮食记录', icon: UtensilsCrossed },
  { key: 'daily', label: '每日统计', icon: BarChart3 },
  { key: 'trend', label: '趋势', icon: TrendingUp },
  { key: 'goal', label: '目标', icon: Target },
]

function formatTs(ts: number): string {
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function round(val: number | undefined): string {
  if (val === undefined || val === null) return '0'
  return Math.round(val).toString()
}

/* ── Date Navigator ── */
function DateNav() {
  const { currentDate, setDate } = useDietStore()

  const change = (delta: number) => {
    const d = new Date(currentDate)
    d.setDate(d.getDate() + delta)
    setDate(formatTs(d.getTime()))
  }

  return (
    <div className="flex items-center justify-center gap-3 mb-6">
      <button onClick={() => change(-1)} className="p-2 border-2 border-foreground hover:bg-[#FF6B35] hover:text-white transition-colors cursor-pointer">
        <ChevronLeft className="w-4 h-4" />
      </button>
      <input
        type="date"
        value={currentDate}
        onChange={(e) => {
          const val = e.target.value
          if (val) { setDate(val) }
        }}
        className="border-2 border-foreground px-3 py-1.5 text-sm bg-transparent text-center font-medium cursor-pointer"
      />
      <button onClick={() => change(1)} className="p-2 border-2 border-foreground hover:bg-[#FF6B35] hover:text-white transition-colors cursor-pointer">
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  )
}

/* ── Record List ── */
function RecordList({
  modal, setModal, editing, setEditing,
}: {
  modal: 'create' | 'edit' | null
  setModal: (m: 'create' | 'edit' | null) => void
  editing: DietRecord | null
  setEditing: (r: DietRecord | null) => void
}) {
  const { records, deleteRecord, currentDate, createRecord, updateRecord } = useDietStore()

  const labels: Record<number, string> = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '加餐' }
  const meals = [1, 2, 3, 4]
    .map((t) => ({
      type: t,
      label: labels[t] ?? '',
      records: records.filter((r) => r.mealType === t),
    }))
    .filter((m) => m.records.length > 0)

  return (
    <div>
      <DateNav />

      {records.length === 0 && (
        <div className="text-center py-16 text-[#6B5344]">
          <p className="text-lg">今天还没有饮食记录</p>
          <p className="text-sm mt-2 opacity-70">点击右上角 + 按钮添加记录</p>
        </div>
      )}

      {meals.map((meal) => (
        <div key={meal.type} className="mb-6">
          <div className="flex justify-between items-center mb-2 px-1">
            <span className="font-semibold text-sm">{meal.label}</span>
            <span className="text-xs text-[#6B5344]">
              {round(meal.records.reduce((s, r) => s + (r.calories || 0), 0))} kcal
            </span>
          </div>
          <div className="space-y-2">
            {meal.records.map((r) => (
              <div key={r.id} className="block-card p-3 flex justify-between items-center">
                <div>
                  <div className="font-medium text-sm">{r.foodName}</div>
                  <div className="text-xs text-[#6B5344]">
                    {r.amount}{r.unit} · {round(r.calories)} kcal
                  </div>
                  {r.remark && <div className="text-xs text-[#6B5344] mt-1 italic">{r.remark}</div>}
                </div>
                <div className="flex gap-1">
                  <button onClick={() => { setEditing(r); setModal('edit') }} className="p-1.5 hover:bg-[#FFF5E6] transition-colors cursor-pointer">
                    <Pencil className="w-4 h-4 text-[#6B5344]" />
                  </button>
                  <button onClick={() => { if (confirm('确定删除？')) deleteRecord(r.id) }} className="p-1.5 hover:bg-red-50 transition-colors cursor-pointer">
                    <Trash2 className="w-4 h-4 text-red-400" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {modal && (
        <RecordForm
          mode={modal}
          record={editing}
          currentDate={currentDate}
          onClose={() => { setModal(null); setEditing(null) }}
          onSubmit={async (data) => {
            if (modal === 'create') await createRecord(data as CreateRecordRequest)
            else if (editing) await updateRecord(editing.id, data as UpdateRecordRequest)
            setModal(null)
            setEditing(null)
          }}
        />
      )}
    </div>
  )
}

/* ── Record Form Modal ── */
function RecordForm({
  mode, record, currentDate, onClose, onSubmit,
}: {
  mode: 'create' | 'edit'
  record: DietRecord | null
  currentDate: string
  onClose: () => void
  onSubmit: (data: CreateRecordRequest | UpdateRecordRequest) => Promise<void>
}) {
  const [form, setForm] = useState<{
    recordDate: string
    mealType: number
    foodName: string
    amount: string
    unit: string
    calories: string
    protein: string
    fat: string
    carbs: string
    remark: string
  }>({
    recordDate: mode === 'edit' && record ? record.recordDate : currentDate,
    mealType: record?.mealType ?? 1,
    foodName: record?.foodName ?? '',
    amount: record?.amount != null ? String(record.amount) : '',
    unit: record?.unit ?? 'g',
    calories: record?.calories != null ? String(record.calories) : '',
    protein: record?.protein != null ? String(record.protein) : '',
    fat: record?.fat != null ? String(record.fat) : '',
    carbs: record?.carbs != null ? String(record.carbs) : '',
    remark: record?.remark ?? '',
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (mode === 'create') {
      getRecommendedMealType().then((t) => setForm((f) => ({ ...f, mealType: t }))).catch(() => {})
    }
  }, [mode])

  const updateForm = (partial: Partial<typeof form>) => setForm((f) => ({ ...f, ...partial }))

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center">
      <div className="block-card p-6 w-[90%] max-w-md max-h-[85vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-5">
          <h3 className="font-bold text-lg">{mode === 'create' ? '添加记录' : '编辑记录'}</h3>
          <button onClick={onClose} className="p-1 cursor-pointer"><X className="w-5 h-5" /></button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="text-xs font-medium text-[#6B5344] block mb-1">日期</label>
            <input type="date" value={form.recordDate} onChange={(e) => updateForm({ recordDate: e.target.value })}
              className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
          </div>
          <div>
            <label className="text-xs font-medium text-[#6B5344] block mb-1">餐次</label>
            <select value={form.mealType} onChange={(e) => updateForm({ mealType: Number(e.target.value) as 1|2|3|4 })}
              className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent cursor-pointer">
              {MEAL_TYPE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs font-medium text-[#6B5344] block mb-1">食物名称</label>
            <input value={form.foodName} onChange={(e) => updateForm({ foodName: e.target.value })} placeholder="请输入食物名称"
              className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
          </div>
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="text-xs font-medium text-[#6B5344] block mb-1">食用量</label>
              <input type="number" min="0" placeholder="0" value={form.amount} onChange={(e) => updateForm({ amount: e.target.value })}
                className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
            </div>
            <div className="w-20">
              <label className="text-xs font-medium text-[#6B5344] block mb-1">单位</label>
              <select value={form.unit} onChange={(e) => updateForm({ unit: e.target.value })}
                className="w-full border-2 border-foreground px-2 py-2 text-sm bg-transparent cursor-pointer">
                <option value="g">g</option>
                <option value="份">份</option>
                <option value="ml">ml</option>
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-[#6B5344] block mb-1">热量 (kcal)</label>
            <input type="number" min="0" placeholder="0" value={form.calories} onChange={(e) => updateForm({ calories: e.target.value })}
              className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
          </div>
          <div className="flex gap-3">
            {(['protein', 'fat', 'carbs'] as const).map((key) => (
              <div key={key} className="flex-1">
                <label className="text-xs font-medium text-[#6B5344] block mb-1">
                  {{ protein: '蛋白质', fat: '脂肪', carbs: '碳水' }[key]} (g)
                </label>
                <input type="number" min="0" placeholder="0" value={form[key]} onChange={(e) => updateForm({ [key]: e.target.value })}
                  className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
              </div>
            ))}
          </div>
          <div>
            <label className="text-xs font-medium text-[#6B5344] block mb-1">备注</label>
            <textarea value={form.remark} onChange={(e) => updateForm({ remark: e.target.value })} rows={2} placeholder="可选"
              className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent resize-none" />
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-5">
          <button onClick={onClose} className="btn-secondary">取消</button>
          <button
            onClick={async () => {
              if (!form.foodName || !form.amount || !form.calories) return
              setLoading(true)
              try { await onSubmit({
                    ...form,
                    amount: Number(form.amount),
                    calories: Number(form.calories),
                    protein: Number(form.protein) || 0,
                    fat: Number(form.fat) || 0,
                    carbs: Number(form.carbs) || 0,
                  }) } finally { setLoading(false) }
            }}
            disabled={loading}
            className="btn-primary disabled:opacity-50"
          >
            {loading ? '保存中...' : '保存'}
          </button>
        </div>
      </div>
    </div>
  )
}

/* ── Daily Summary ── */
function DailySummary() {
  const { dailySummary, goal, loadDailySummary, loadGoal } = useDietStore()

  useEffect(() => { loadDailySummary(); loadGoal() }, [])

  if (!dailySummary) return <div className="text-center py-16 text-[#6B5344]">加载中...</div>

  const goalCal = goal?.dailyCalories || 2000
  const percent = Math.min((dailySummary.totalCalories / goalCal) * 100, 100)
  const circumference = 2 * Math.PI * 50
  const offset = circumference - (percent / 100) * circumference

  return (
    <div>
      <DateNav />
      <div className="space-y-8">
        {/* Stat cards */}
        <div className="grid grid-cols-2 gap-4">
          {[
            { label: '总热量 kcal', value: round(dailySummary.totalCalories) },
            { label: '蛋白质 g', value: round(dailySummary.totalProtein) },
            { label: '脂肪 g', value: round(dailySummary.totalFat) },
            { label: '碳水 g', value: round(dailySummary.totalCarbs) },
          ].map((s) => (
            <div key={s.label} className="block-card p-4 text-center">
              <div className="text-2xl font-bold">{s.value}</div>
              <div className="text-xs text-[#6B5344] mt-1">{s.label}</div>
            </div>
          ))}
        </div>

        {/* Progress ring */}
        <div className="flex flex-col items-center">
          <div className="relative w-36 h-36">
            <svg viewBox="0 0 120 120" className="w-full h-full">
              <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(0,0,0,0.06)" strokeWidth="10" />
              <circle cx="60" cy="60" r="50" fill="none"
                stroke={percent >= 100 ? '#ef4444' : '#FF6B35'}
                strokeWidth="10" strokeLinecap="round"
                strokeDasharray={circumference} strokeDashoffset={offset}
                transform="rotate(-90 60 60)" className="transition-all duration-500" />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <div className="text-xl font-bold">{round(dailySummary.totalCalories)}</div>
              <div className="text-xs text-[#6B5344]">/ {goalCal} kcal</div>
            </div>
          </div>
          <div className="text-sm text-[#6B5344] mt-3">
            {percent >= 100 ? '已达到目标' : `还需 ${round(goalCal - dailySummary.totalCalories)} kcal`}
          </div>
        </div>

        {/* Meal distribution */}
        {dailySummary.mealSummaries.length > 0 && (
          <div>
            <h4 className="font-semibold text-sm mb-3">餐次分布</h4>
            <div className="space-y-3">
              {dailySummary.mealSummaries.map((m) => (
                <div key={m.mealType} className="flex items-center gap-3">
                  <span className="w-10 text-right text-sm">{m.mealTypeLabel}</span>
                  <div className="flex-1 h-3 bg-black/5 overflow-hidden">
                    <div className="h-full bg-[#FF6B35] transition-all duration-300" style={{ width: `${dailySummary.totalCalories ? (m.calories / dailySummary.totalCalories) * 100 : 0}%`, minWidth: '4px' }} />
                  </div>
                  <span className="w-20 text-xs text-[#6B5344]">{round(m.calories)} kcal</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

/* ── Trend (P2 skeleton) ── */
function Trend() {
  const [mode, setMode] = useState<'weekly' | 'monthly'>('weekly')
  const { weeklyTrend, monthlyTrend, loadWeeklyTrend, loadMonthlyTrend } = useDietStore()
  const data = mode === 'weekly' ? weeklyTrend : monthlyTrend

  useEffect(() => { loadWeeklyTrend() }, [])

  const switchMode = (m: 'weekly' | 'monthly') => {
    setMode(m)
    if (m === 'weekly') loadWeeklyTrend()
    else loadMonthlyTrend()
  }

  return (
    <div>
      <h3 className="font-bold text-lg mb-4">趋势统计</h3>
      <div className="flex gap-2 mb-6">
        <button onClick={() => switchMode('weekly')}
          className={`px-5 py-2 text-sm border-2 border-foreground cursor-pointer transition-colors ${mode === 'weekly' ? 'bg-[#FF6B35] text-white' : 'hover:bg-[#FFF5E6]'}`}>
          周视图
        </button>
        <button onClick={() => switchMode('monthly')}
          className={`px-5 py-2 text-sm border-2 border-foreground cursor-pointer transition-colors ${mode === 'monthly' ? 'bg-[#FF6B35] text-white' : 'hover:bg-[#FFF5E6]'}`}>
          月视图
        </button>
      </div>

      <div className="text-center py-10 text-[#6B5344]">
        <p>趋势图表功能将在后续版本中上线</p>
        <p className="text-sm mt-1 opacity-70">将使用 ECharts 展示热量趋势折线图和日历视图</p>
      </div>

      {data && (
        <div className="grid grid-cols-2 gap-4 mt-4">
          <div className="block-card p-4 text-center">
            <div className="text-xs text-[#6B5344] mb-1">日均热量</div>
            <div className="text-lg font-bold">{round(data.averageCalories)} kcal</div>
          </div>
          <div className="block-card p-4 text-center">
            <div className="text-xs text-[#6B5344] mb-1">最高</div>
            <div className="text-lg font-bold">{round(data.maxCalories)} kcal</div>
          </div>
          <div className="block-card p-4 text-center">
            <div className="text-xs text-[#6B5344] mb-1">最低</div>
            <div className="text-lg font-bold">{round(data.minCalories)} kcal</div>
          </div>
          {data.targetDays !== undefined && (
            <div className="block-card p-4 text-center">
              <div className="text-xs text-[#6B5344] mb-1">达标天数</div>
              <div className="text-lg font-bold">{data.targetDays} 天</div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/* ── Goal Settings ── */
function GoalSettings() {
  const { goal, loadGoal, updateGoal } = useDietStore()
  const [form, setForm] = useState({ dailyCalories: 2000, proteinRatio: 20, fatRatio: 30, carbsRatio: 50 })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadGoal().then(() => {
      if (goal) setForm({ dailyCalories: goal.dailyCalories, proteinRatio: goal.proteinRatio, fatRatio: goal.fatRatio, carbsRatio: goal.carbsRatio })
    })
  }, [])

  // Sync form when goal loads
  useEffect(() => {
    if (goal) setForm({ dailyCalories: goal.dailyCalories, proteinRatio: goal.proteinRatio, fatRatio: goal.fatRatio, carbsRatio: goal.carbsRatio })
  }, [goal])

  const ratioSum = form.proteinRatio + form.fatRatio + form.carbsRatio

  const templates = [
    { name: '减脂', dailyCalories: 1500, proteinRatio: 30, fatRatio: 30, carbsRatio: 40 },
    { name: '增肌', dailyCalories: 2500, proteinRatio: 30, fatRatio: 20, carbsRatio: 50 },
    { name: '维持', dailyCalories: 2000, proteinRatio: 20, fatRatio: 30, carbsRatio: 50 },
  ]

  return (
    <div>
      <h3 className="font-bold text-lg mb-5">目标设置</h3>

      {/* Quick templates */}
      <div className="mb-6">
        <p className="text-xs font-medium text-[#6B5344] mb-2">快捷模板</p>
        <div className="flex gap-3">
          {templates.map((t) => (
            <button key={t.name} onClick={() => setForm(t)}
              className="px-5 py-2 text-sm border-2 border-foreground hover:bg-[#FF6B35] hover:text-white transition-colors cursor-pointer">
              {t.name}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-5">
        <div>
          <label className="text-xs font-medium text-[#6B5344] block mb-2">每日热量目标 (kcal)</label>
          <input type="number" min={500} max={10000} step={50} value={form.dailyCalories}
            onChange={(e) => setForm({ ...form, dailyCalories: Number(e.target.value) })}
            className="w-full border-2 border-foreground px-3 py-2 text-sm bg-transparent" />
        </div>

        {(['proteinRatio', 'fatRatio', 'carbsRatio'] as const).map((key) => {
          const label = { proteinRatio: '蛋白质', fatRatio: '脂肪', carbsRatio: '碳水' }[key]
          return (
            <div key={key}>
              <label className="text-xs font-medium text-[#6B5344] block mb-2">{label}占比 {form[key]}%</label>
              <input type="range" min={0} max={100} value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: Number(e.target.value) })}
                className="w-full accent-[#FF6B35]" />
            </div>
          )
        })}

        {/* Ratio bar */}
        <div className="flex items-center gap-3">
          <div className="flex-1 h-6 bg-black/5 overflow-hidden flex">
            <div className="h-full bg-[#14b8a6]" style={{ width: `${form.proteinRatio}%` }} />
            <div className="h-full bg-[#f59e0b]" style={{ width: `${form.fatRatio}%` }} />
            <div className="h-full bg-[#6366f1]" style={{ width: `${form.carbsRatio}%` }} />
          </div>
          <span className={`text-sm font-medium whitespace-nowrap ${ratioSum !== 100 ? 'text-red-500' : ''}`}>
            合计: {ratioSum}%
          </span>
        </div>
        <div className="flex gap-4 text-xs text-[#6B5344]">
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#14b8a6] inline-block" />蛋白质</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#f59e0b] inline-block" />脂肪</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#6366f1] inline-block" />碳水</span>
        </div>
      </div>

      <div className="flex justify-end mt-6">
        <button
          onClick={async () => {
            if (ratioSum !== 100) return
            setSaving(true)
            try { await updateGoal(form) } finally { setSaving(false) }
          }}
          disabled={ratioSum !== 100 || saving}
          className="btn-primary disabled:opacity-50"
        >
          {saving ? '保存中...' : '保存目标'}
        </button>
      </div>
    </div>
  )
}

/* ── Main Diet Page ── */
export default function Diet() {
  const [activeTab, setActiveTab] = useState<TabKey>('record')
  const [modal, setModal] = useState<'create' | 'edit' | null>(null)
  const [editing, setEditing] = useState<DietRecord | null>(null)

  return (
    <div className="max-w-2xl mx-auto">
      {/* Tabs */}
      <div className="flex gap-1 mb-6 overflow-x-auto">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium border-2 border-foreground cursor-pointer transition-colors whitespace-nowrap ${
              activeTab === key ? 'bg-[#FF6B35] text-white' : 'hover:bg-[#FFF5E6]'
            }`}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="block-card p-6 relative">
        {activeTab === 'record' && (
          <button
            onClick={() => setModal('create')}
            className="absolute top-4 right-4 w-10 h-10 bg-[#FF6B35] text-white border-2 border-foreground shadow-[4px_4px_0_0_#1a1a1a] hover:shadow-[2px_2px_0_0_#1a1a1a] hover:translate-x-[2px] hover:translate-y-[2px] transition-all flex items-center justify-center cursor-pointer"
          >
            <Plus className="w-5 h-5" />
          </button>
        )}
        {activeTab === 'record' && <RecordList modal={modal} setModal={setModal} editing={editing} setEditing={setEditing} />}
        {activeTab === 'daily' && <DailySummary />}
        {activeTab === 'trend' && <Trend />}
        {activeTab === 'goal' && <GoalSettings />}
      </div>
    </div>
  )
}
