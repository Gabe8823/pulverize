<template>
  <div class="page">
    <!-- 页头 -->
    <header class="page-head">
      <p class="eyebrow">运动目标</p>
      <h1 class="headline">为目标，持续奔跑。</h1>
      <p class="subhead">设定周跑量或最佳配速目标，进度会随每一次训练自动更新。</p>
      <div class="head-actions">
        <AButton type="ghost" :loading="refreshing" @click="handleRefresh">刷新进度</AButton>
        <AButton type="primary" @click="openCreate">设定新目标</AButton>
      </div>
    </header>

    <!-- 目标列表 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">我的目标</h2>
          <p class="section-desc">每一个目标都对应真实的训练数据，进度由系统自动计算。</p>
        </div>
        <span v-if="goals.length > 0" class="chip chip-gray">{{ goals.length }} 个目标</span>
      </div>

      <div v-if="loading && goals.length === 0" class="a-empty">
        <span class="a-spinner" />
      </div>

      <div v-else-if="goals.length === 0" class="a-empty">
        <div class="a-empty__text">还没有设定目标</div>
        <div class="a-empty__sub">定个小目标，让每一次训练更有方向</div>
      </div>

      <div v-else class="card-grid">
        <article v-for="goal in goals" :key="goal.id" class="a-card hoverable goal-card">
          <div class="goal-top">
            <span class="goal-type">{{ goalTypeLabel(goal.goalType) }}</span>
            <span class="chip" :class="goalStatusChip(goal.status)">
              {{ goalStatusLabel(goal.status) }}
            </span>
          </div>

          <div class="goal-target">
            <span class="stat-value">{{ formatGoalTarget(goal) }}</span>
            <span class="goal-unit">{{ goalUnitLabel(goal) }}</span>
          </div>

          <!-- 进度条 -->
          <div class="goal-bar">
            <div class="goal-bar-inner" :style="{ width: goalPercent(goal) + '%' }"></div>
          </div>

          <div class="goal-progress">
            <span>{{ progressText(goal) }}</span>
            <span class="goal-percent text-accent">{{ goalPercent(goal) }}%</span>
          </div>

          <div v-if="goal.deadline" class="goal-deadline">截止 {{ goal.deadline }}</div>

          <div class="goal-actions">
            <AButton size="sm" @click="openEdit(goal)">编辑</AButton>
            <AButton size="sm" type="ghost-danger" @click="openDelete(goal)">删除</AButton>
          </div>
        </article>
      </div>
    </section>

    <!-- 新建 / 编辑目标 -->
    <AModal
      v-model="editVisible"
      :title="editingId === null ? '设定新目标' : '编辑目标'"
      size="lg"
      :loading="saving"
      :confirm-text="editingId === null ? '创建目标' : '保存修改'"
      @confirm="handleConfirm"
    >
      <div class="a-form edit-form">
        <div class="a-field">
          <div class="field-head">
            <label class="a-label">目标类型</label>
            <button
              type="button"
              class="ai-suggest-btn"
              :disabled="aiLoading"
              @click="handleAiSuggest"
            >
              <span v-if="aiLoading" class="a-spinner a-spinner--inline" />
              <svg v-else viewBox="0 0 20 20" width="14" height="14" fill="currentColor">
                <path d="M10 1.6l1.9 4.9 4.9 1.9-4.9 1.9L10 15.2 8.1 10.3 3.2 8.4l4.9-1.9L10 1.6zM16.4 12.4l.9 2.3 2.3.9-2.3.9-.9 2.3-.9-2.3-2.3-.9 2.3-.9.9-2.3z" />
              </svg>
              {{ aiLoading ? 'AI 思考中…' : 'AI 推荐目标' }}
            </button>
          </div>
          <ASelect
            v-model="form.goalType"
            :options="goalTypeOptions"
            allow-custom
            custom-label="自定义目标类型…"
            custom-placeholder="如：全马完赛"
            @change="handleTypeChange"
          />
        </div>

        <div class="form-row">
          <div class="a-field">
            <label class="a-label">{{ PACE_TYPES.has(form.goalType) ? '目标成绩' : '目标值' }}</label>
            <AInput
              v-if="PACE_TYPES.has(form.goalType)"
              v-model="form.targetTime"
              type="text"
              :placeholder="targetPlaceholder"
            />
            <AInput v-else v-model="form.targetValue" type="number" :min="0" :placeholder="targetPlaceholder" />
          </div>
          <div class="a-field">
            <label class="a-label">单位</label>
            <ASelect
              v-if="!PACE_TYPES.has(form.goalType)"
              v-model="form.unit"
              :options="unitOptions"
              allow-custom
              custom-label="自定义单位…"
              custom-placeholder="如 min/km"
            />
            <p v-else class="a-hint">s/km · 按比赛距离由成绩自动换算</p>
          </div>
        </div>

        <!-- AI 建议 -->
        <transition name="ai-bubble">
          <div v-if="aiSuggestion" class="ai-suggestion">
            <div class="ai-suggestion__head">
              <span class="chip chip-blue">{{ aiSuggestion.source === 'AI' ? 'AI 教练' : '数据规则' }}</span>
              <span class="ai-suggestion__meta">已为你填入{{ PACE_TYPES.has(form.goalType) ? '目标成绩' : '目标值' }}、单位与截止日期</span>
            </div>
            <p class="ai-suggestion__text">{{ aiSuggestion.suggestion }}</p>
          </div>
        </transition>

        <div class="a-field">
          <label class="a-label">截止日期</label>
          <ADateInput v-model="form.deadline" placeholder="选择日期" />
        </div>

        <div v-if="editingId !== null" class="a-field">
          <label class="a-label">当前值</label>
          <AInput v-model="form.currentValue" type="number" :min="0" placeholder="留空则由系统自动计算" />
          <p class="a-hint">保存后进度仍会随训练数据自动刷新。</p>
        </div>
      </div>
    </AModal>

    <!-- 删除确认 -->
    <AModal
      v-model="deleteVisible"
      title="删除目标"
      danger
      confirm-text="删除"
      :loading="deleting"
      @confirm="handleDelete"
    >
      确定删除「{{ pendingDeleteLabel }}」吗？删除后无法恢复。
    </AModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'
import ASelect from '@/components/ui/ASelect.vue'
import AModal from '@/components/ui/AModal.vue'
import ADateInput from '@/components/ui/ADateInput.vue'
import { goalApi } from '@/api/goal'
import { toast } from '@/utils/toast'
import { formatDuration } from '@/utils/format'
import type { UserGoal, AiGoalSuggestion } from '@/types/goal'

/** 配速类目标（单位 s/km） */
const PACE_TYPES: Set<string> = new Set(['BEST_5K', 'BEST_10K', 'BEST_HALF'])

/** 成绩类目标的比赛距离（km）——输入完赛成绩，保存时自动换算为配速 */
const RACE_KM: Record<string, number> = { BEST_5K: 5, BEST_10K: 10, BEST_HALF: 21.0975 }

/** 成绩类目标卡片上展示的短标签 */
const RACE_LABEL: Record<string, string> = { BEST_5K: '5K', BEST_10K: '10K', BEST_HALF: '半马' }

/** 目标值占位：PB 类提示输入想要的完赛成绩，跑量类提示公里数（可手输，也可点 AI 推荐） */
const targetPlaceholder = computed(() => {
  if (!PACE_TYPES.has(form.goalType)) return '例如 30'
  return form.goalType === 'BEST_HALF' ? '如 2:15:00（时:分:秒）' : '如 24:30（分:秒）'
})

/** 目标类型中文映射（未知类型显示为自定义目标） */
const GOAL_TYPE_MAP: Record<string, string> = {
  WEEKLY_DISTANCE: '周跑量目标',
  MONTHLY_DISTANCE: '月跑量目标',
  BEST_5K: '5K 成绩目标',
  BEST_10K: '10K 成绩目标',
  BEST_HALF: '半马成绩目标',
}

const goalTypeOptions: Array<{ value: string; label: string; hint?: string; group?: string }> = [
  { value: 'WEEKLY_DISTANCE', label: '周跑量目标', hint: '本周累计跑量（km）', group: '跑量目标' },
  { value: 'MONTHLY_DISTANCE', label: '月跑量目标', hint: '本月累计跑量（km）', group: '跑量目标' },
  { value: 'BEST_5K', label: '5K 成绩目标', hint: '5 公里目标成绩（如 24:30）', group: '训练目的 · PB 成绩' },
  { value: 'BEST_10K', label: '10K 成绩目标', hint: '10 公里目标成绩（如 51:00）', group: '训练目的 · PB 成绩' },
  { value: 'BEST_HALF', label: '半马成绩目标', hint: '半程马拉松目标成绩（如 2:15:00）', group: '训练目的 · PB 成绩' },
]

const unitOptions: Array<{ value: string; label: string; hint?: string }> = [
  { value: 'km', label: 'km', hint: '公里，用于跑量目标' },
  { value: 's/km', label: 's/km', hint: '秒/公里，用于配速目标' },
]

const goals = ref<UserGoal[]>([])
const loading = ref(false)
const refreshing = ref(false)

/* ---- 新建 / 编辑 ---- */
const editVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<{
  goalType: string
  targetValue: number | null
  /** 成绩类目标输入的完赛时间文本（如 24:30 / 2:15:00），保存时换算为配速 */
  targetTime: string
  unit: string
  deadline: string
  currentValue: number | null
}>({
  goalType: 'WEEKLY_DISTANCE',
  targetValue: null,
  targetTime: '',
  unit: 'km',
  deadline: '',
  currentValue: null,
})

/** 上次选择的目标类型（成绩类型之间切换时按新距离重算显示用） */
let prevGoalType = 'WEEKLY_DISTANCE'

/* ---- 删除 ---- */
const deleteVisible = ref(false)
const deleting = ref(false)
const pendingDeleteId = ref<number | null>(null)
const pendingDeleteLabel = ref('')

onMounted(() => {
  void loadGoals()
})

async function loadGoals(): Promise<void> {
  loading.value = true
  try {
    goals.value = await goalApi.listGoals()
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    loading.value = false
  }
}

async function handleRefresh(): Promise<void> {
  if (refreshing.value) return
  refreshing.value = true
  try {
    goals.value = await goalApi.refreshGoals()
    toast.success('进度已刷新')
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    refreshing.value = false
  }
}

function openCreate(): void {
  editingId.value = null
  form.goalType = 'WEEKLY_DISTANCE'
  prevGoalType = 'WEEKLY_DISTANCE'
  form.targetValue = 20
  form.targetTime = ''
  form.unit = 'km'
  form.deadline = ''
  form.currentValue = null
  aiSuggestion.value = null
  editVisible.value = true
}

function openEdit(goal: UserGoal): void {
  editingId.value = goal.id
  form.goalType = goal.goalType
  prevGoalType = goal.goalType
  form.targetValue = goal.targetValue
  form.targetTime = PACE_TYPES.has(goal.goalType)
    ? paceToTimeText(goal.targetValue, goal.goalType)
    : ''
  form.unit = goal.unit
  form.deadline = goal.deadline ?? ''
  form.currentValue = goal.currentValue
  aiSuggestion.value = null
  editVisible.value = true
}

/** 切换目标类型时自动带出单位；成绩类型之间切换时按新距离重算显示 */
function handleTypeChange(value: string | number | null): void {
  const v = String(value ?? '')
  const oldType = prevGoalType
  prevGoalType = v
  form.unit = PACE_TYPES.has(v) ? 's/km' : 'km'
  if (PACE_TYPES.has(v)) {
    if (PACE_TYPES.has(oldType) && form.targetTime) {
      // 5K ↔ 10K ↔ 半马：同为成绩，按新距离换算展示
      const pace = timeTextToPace(form.targetTime, oldType)
      form.targetTime = pace !== null ? paceToTimeText(pace, v) : ''
      form.targetValue = pace
    } else {
      form.targetTime = ''
      form.targetValue = null
    }
  } else if (PACE_TYPES.has(oldType)) {
    // 成绩 → 跑量：清掉残留的配速数值，避免被当成公里数提交
    form.targetValue = null
  }
  aiSuggestion.value = null
}

/* ---- AI 推荐目标 ---- */
const aiLoading = ref(false)
const aiSuggestion = ref<AiGoalSuggestion | null>(null)

async function handleAiSuggest(): Promise<void> {
  if (aiLoading.value) return
  aiLoading.value = true
  try {
    const res = await goalApi.aiSuggest(form.goalType)
    aiSuggestion.value = res
    if (res.targetValue && Number.isFinite(res.targetValue)) {
      form.targetValue = Math.round(res.targetValue * 10) / 10
      // 成绩类：推荐的是配速，输入框展示为完赛时间
      if (PACE_TYPES.has(form.goalType)) {
        form.targetTime = paceToTimeText(form.targetValue, form.goalType)
      }
    }
    if (res.unit) form.unit = res.unit
    if (res.deadline) form.deadline = res.deadline
    toast.success(res.source === 'AI' ? 'AI 推荐已生成' : '已按你的历史数据给出推荐')
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    aiLoading.value = false
  }
}

async function handleConfirm(): Promise<void> {
  if (saving.value) return

  // 成绩类：输入的是完赛时间，保存前换算为配速（s/km，后端契约不变）
  let target = form.targetValue
  if (PACE_TYPES.has(form.goalType)) {
    const pace = timeTextToPace(form.targetTime, form.goalType)
    if (pace === null) {
      toast.error(form.goalType === 'BEST_HALF' ? '请输入正确的成绩，如 2:15:00' : '请输入正确的成绩，如 24:30')
      return
    }
    target = pace
    form.targetValue = pace
  }
  if (target === null || !Number.isFinite(target) || target <= 0) {
    toast.error('请输入大于 0 的目标值')
    return
  }

  const payload: Partial<UserGoal> = {
    goalType: form.goalType,
    targetValue: target,
    unit: PACE_TYPES.has(form.goalType) ? 's/km' : form.unit,
    deadline: form.deadline || null,
  }
  if (editingId.value !== null && form.currentValue !== null && Number.isFinite(form.currentValue)) {
    payload.currentValue = form.currentValue
  }

  saving.value = true
  try {
    if (editingId.value === null) {
      await goalApi.createGoal(payload)
      toast.success('目标已创建')
    } else {
      await goalApi.updateGoal(editingId.value, payload)
      toast.success('目标已更新')
    }
    editVisible.value = false
    await loadGoals()
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    saving.value = false
  }
}

function openDelete(goal: UserGoal): void {
  pendingDeleteId.value = goal.id
  pendingDeleteLabel.value = goalTypeLabel(goal.goalType)
  deleteVisible.value = true
}

async function handleDelete(): Promise<void> {
  if (deleting.value || pendingDeleteId.value === null) return
  deleting.value = true
  try {
    await goalApi.deleteGoal(pendingDeleteId.value)
    toast.success('目标已删除')
    deleteVisible.value = false
    pendingDeleteId.value = null
    await loadGoals()
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    deleting.value = false
  }
}

/* ---- 展示辅助 ---- */
function goalTypeLabel(type: string): string {
  return GOAL_TYPE_MAP[type] ?? '自定义目标'
}

function goalStatusLabel(status: number): string {
  if (status === 1) return '已完成'
  if (status === 2) return '已过期'
  return '进行中'
}

function goalStatusChip(status: number): string {
  if (status === 1) return 'chip-blue'
  if (status === 2) return 'chip-gray'
  return 'chip-green'
}

/** 进度百分比：跑量类按 current/target；成绩类（配速）按 target/current，越快越满 */
function goalPercent(goal: UserGoal): number {
  const target = goal.targetValue ?? 0
  if (!target) return 0
  const current = goal.currentValue ?? 0
  if (!current) return 0
  if (PACE_TYPES.has(goal.goalType)) {
    return Math.min(100, Math.max(0, Math.round((target / current) * 100)))
  }
  return Math.min(100, Math.max(0, Math.round((current / target) * 100)))
}

function formatValue(value: number | null | undefined): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return '0'
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}

/** 配速（s/km）→ 完赛时间文本：5K/10K 显示 分:秒，半马显示 时:分:秒 */
function paceToTimeText(paceSecKm: number | null | undefined, goalType: string): string {
  if (paceSecKm === null || paceSecKm === undefined || !Number.isFinite(paceSecKm) || paceSecKm <= 0) {
    return ''
  }
  const km = RACE_KM[goalType] ?? 10
  const total = Math.round(paceSecKm * km)
  return formatDuration(total)
}

/** 完赛时间文本（24:30 / 2:15:00）→ 配速（s/km）；非法输入返回 null */
function timeTextToPace(text: string, goalType: string): number | null {
  const km = RACE_KM[goalType]
  if (!km) return null
  const parts = text.trim().split(':').map((p) => p.trim())
  if (parts.length < 2 || parts.length > 3) return null
  if (!parts.every((p) => /^\d{1,3}$/.test(p))) return null
  let total = 0
  if (parts.length === 3) {
    total = Number(parts[0]) * 3600 + Number(parts[1]) * 60 + Number(parts[2])
  } else {
    total = Number(parts[0]) * 60 + Number(parts[1])
  }
  if (total < 60 || total > 12 * 3600) return null
  const pace = Math.round(total / km)
  return pace >= 60 && pace <= 1200 ? pace : null
}

/** 卡片目标值：成绩类显示完赛时间，跑量类显示数值 */
function formatGoalTarget(goal: UserGoal): string {
  if (PACE_TYPES.has(goal.goalType)) {
    const t = paceToTimeText(goal.targetValue, goal.goalType)
    return t || '--'
  }
  return formatValue(goal.targetValue)
}

/** 卡片单位：成绩类显示短标签（5K/10K/半马），否则原单位 */
function goalUnitLabel(goal: UserGoal): string {
  return PACE_TYPES.has(goal.goalType) ? `目标成绩（${RACE_LABEL[goal.goalType] ?? ''}）` : goal.unit
}

/** 卡片进度行文案 */
function progressText(goal: UserGoal): string {
  if (PACE_TYPES.has(goal.goalType)) {
    const cur = paceToTimeText(goal.currentValue, goal.goalType)
    const tgt = paceToTimeText(goal.targetValue, goal.goalType)
    return `当前 ${cur || '--'} / 目标 ${tgt || '--'}`
  }
  return `当前 ${formatValue(goal.currentValue)} / ${formatValue(goal.targetValue)} ${goal.unit}`
}
</script>

<style scoped lang="scss">
.head-actions {
  margin-top: 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

/* ---- 目标卡片 ---- */
.goal-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.goal-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.goal-type {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--a-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.goal-target {
  display: flex;
  align-items: baseline;
  gap: 6px;

  .stat-value {
    font-size: 30px;
  }
}

.goal-unit {
  font-size: 15px;
  font-weight: 500;
  color: var(--a-text-secondary);
}

/* 进度条：外层背景 var(--a-fill)，内层 var(--a-blue) */
.goal-bar {
  height: 8px;
  border-radius: 980px;
  background: var(--a-fill);
  overflow: hidden;
}

.goal-bar-inner {
  height: 100%;
  border-radius: 980px;
  background: var(--a-blue);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.goal-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--a-text-secondary);
  font-variant-numeric: tabular-nums;
}

.goal-percent {
  font-weight: 600;
}

.goal-deadline {
  font-size: 12.5px;
  color: var(--a-text-tertiary);
}

.goal-actions {
  margin-top: auto;
  padding-top: 6px;
  display: flex;
  gap: 10px;
}

/* ---- 弹窗表单 ---- */
.edit-form {
  padding: 8px 0 22px;
}

/* 按钮内联小号 spinner */
.a-spinner--inline {
  width: 14px;
  height: 14px;
  border-width: 2px;
  border-color: var(--a-blue-ring);
  border-top-color: var(--a-blue);
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

/* AI 推荐按钮（药丸形、描边） */
.ai-suggest-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1.5px solid var(--a-blue);
  background: transparent;
  color: var(--a-blue);
  padding: 6px 14px;
  border-radius: 980px;
  font-family: var(--a-font);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.16s ease, transform 0.16s ease, opacity 0.16s ease;
}
.ai-suggest-btn:hover:not(:disabled) {
  background: var(--a-blue-ring);
  transform: translateY(-1px);
}
.ai-suggest-btn:disabled {
  opacity: 0.65;
  cursor: default;
}

/* AI 建议气泡 */
.ai-suggestion {
  background: linear-gradient(135deg, rgba(227, 6, 19, 0.07), rgba(255, 59, 48, 0.05));
  border: 1px solid rgba(227, 6, 19, 0.22);
  border-radius: 14px;
  padding: 14px 16px;
}
.ai-suggestion__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.ai-suggestion__meta {
  font-size: 12.5px;
  color: var(--a-text-tertiary);
}
.ai-suggestion__text {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.65;
  color: var(--a-text-secondary);
}

.ai-bubble-enter-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}
.ai-bubble-leave-active {
  transition: opacity 0.18s ease;
}
.ai-bubble-enter-from,
.ai-bubble-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
