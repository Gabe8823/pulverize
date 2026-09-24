<template>
  <div class="page">
    <button class="back-link" @click="router.push('/plan')">‹ 训练计划</button>

    <header class="page-head">
      <p class="eyebrow">生成计划</p>
      <h1 class="headline">像职业选手一样训练</h1>
      <p class="subhead">AI 结合你近期的跑步数据，参考职业运动员训练风格，生成专属周计划。</p>
    </header>

    <section class="section generate-wrap">
      <div class="a-card generate-card">
        <div class="a-form">
          <div class="a-field">
            <span class="a-label">训练目标</span>
            <ASelect
              :model-value="form.goalType"
              :options="GOAL_OPTIONS"
              @update:model-value="onGoalTypeChange"
            />
          </div>

          <div class="a-field">
            <label class="a-label" for="goal-value">目标值（可选）</label>
            <AInput
              id="goal-value"
              :model-value="form.goalValue"
              :placeholder="goalValuePlaceholder"
              :maxlength="30"
              @update:model-value="onGoalValueChange"
            />
          </div>

          <div class="form-row">
            <div class="a-field">
              <label class="a-label" for="start-date">开始日期</label>
              <ADateInput
                id="start-date"
                :model-value="form.startDate"
                :min="today"
                placeholder="选择开始日期"
                @update:model-value="onStartDateChange"
              />
            </div>
            <div class="a-field">
              <label class="a-label" for="end-date">结束日期</label>
              <ADateInput
                id="end-date"
                :model-value="form.endDate"
                :min="form.startDate || today"
                placeholder="选择结束日期"
                @update:model-value="onEndDateChange"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="a-field">
              <span class="a-label">每周训练天数</span>
              <ASelect
                :model-value="form.weeklyTrainingDays"
                :options="WEEKLY_DAY_OPTIONS"
                @update:model-value="onWeeklyDaysChange"
              />
            </div>
            <div class="a-field">
              <label class="a-label" for="duration">单次时长（分钟）</label>
              <AInput
                id="duration"
                type="number"
                :model-value="form.trainingDurationMinutes"
                :min="20"
                :max="240"
                placeholder="60"
                @update:model-value="onDurationChange"
              />
            </div>
          </div>

          <div class="a-field">
            <span class="a-label">参考职业运动员风格</span>
            <p class="a-hint">在你的个人能力基础上，叠加所选选手的训练重点。</p>

            <div class="a-choice-grid style-grid">
              <button
                type="button"
                class="a-choice"
                :class="{ 'is-active': form.athleteStyle === '' }"
                @click="form.athleteStyle = ''"
              >
                <span class="a-choice__title">
                  <svg class="choice-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
                    <path d="M4 20V12" />
                    <path d="M10 20V6" />
                    <path d="M16 20V10" />
                    <path d="M3 20h18" />
                  </svg>
                  通用科学训练
                </span>
                <span class="a-choice__desc">80/20 极化训练，均衡发展耐力与速度</span>
              </button>

              <button
                type="button"
                class="a-choice"
                :class="{ 'is-active': form.athleteStyle === 'KIPCHOGE' }"
                @click="form.athleteStyle = 'KIPCHOGE'"
              >
                <span class="a-choice__title">
                  <svg class="choice-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M8 4h8v5a4 4 0 0 1-8 0V4z" />
                    <path d="M8 5H5.5a3 3 0 0 0 2.5 3" />
                    <path d="M16 5h2.5a3 3 0 0 1-2.5 3" />
                    <path d="M12 13v4" />
                    <path d="M9 20h6" />
                    <path d="M10 17h4" />
                  </svg>
                  埃鲁德·基普乔格
                </span>
                <span class="a-choice__desc">马拉松节奏跑 + 超长距离有氧，稳态耐力核心</span>
              </button>

              <button
                type="button"
                class="a-choice"
                :class="{ 'is-active': form.athleteStyle === 'OSAKO' }"
                @click="form.athleteStyle = 'OSAKO'"
              >
                <span class="a-choice__title">
                  <svg class="choice-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
                  </svg>
                  大迫杰
                </span>
                <span class="a-choice__desc">节奏跑与间歇并重，速度耐力兼修</span>
              </button>

              <button
                type="button"
                class="a-choice"
                :class="{ 'is-active': form.athleteStyle === 'HASAN' }"
                @click="form.athleteStyle = 'HASAN'"
              >
                <span class="a-choice__title">
                  <svg class="choice-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M12 3l2.4 5.2 5.6.7-4.1 3.9 1.1 5.6L12 15.8 6.9 18.4 8 12.8 4 8.9l5.6-.7L12 3z" />
                  </svg>
                  费斯·基普耶格·哈桑
                </span>
                <span class="a-choice__desc">高强度间歇 + 长距离跑，爆发力与耐力兼顾</span>
              </button>
            </div>
          </div>

          <AButton type="primary" size="lg" block :loading="loading" @click="handleGenerate">
            生成训练计划
          </AButton>

          <p class="side-guide">
            已经生成过？
            <button type="button" class="text-accent link-btn" @click="router.push('/plan')">查看我的计划</button>
          </p>
        </div>
      </div>

      <!-- 生成结果 -->
      <div v-if="result" class="a-card result-card">
        <p class="eyebrow">已生成</p>
        <h2 class="result-name">{{ result.planName }}</h2>

        <div v-if="result.aiReasoning" class="reasoning-block">
          <span class="a-label">AI 生成思路</span>
          <pre class="reasoning-text">{{ reasoningText }}</pre>
        </div>

        <div class="a-table-wrap result-table">
          <table class="a-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>训练类型</th>
                <th>距离</th>
                <th>时长</th>
                <th>配速</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, i) in previewPlan" :key="i">
                <td>{{ formatDate(item.planDate) }}</td>
                <td>
                  <span class="chip" :class="workoutChip(item.workoutType)">
                    {{ workoutLabel(item.workoutType) }}
                  </span>
                </td>
                <td>{{ item.targetDistanceKm != null ? item.targetDistanceKm + ' km' : '--' }}</td>
                <td>{{ item.targetDurationMin != null ? item.targetDurationMin + ' 分' : '--' }}</td>
                <td>{{ formatPace(item.targetPaceSecKm) }} /km</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="result.dailyPlan.length > PREVIEW_COUNT" class="caption result-more">
          仅预览前 {{ PREVIEW_COUNT }} 天，完整 {{ result.dailyPlan.length }} 天安排请查看完整计划。
        </p>

        <div class="result-actions">
          <AButton type="primary" size="lg" @click="router.push(`/plan/${result.planId}`)">
            查看完整计划
          </AButton>
          <AButton type="ghost" size="lg" @click="router.push('/plan')">查看我的计划</AButton>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'
import ADateInput from '@/components/ui/ADateInput.vue'
import ASelect from '@/components/ui/ASelect.vue'
import { planApi } from '@/api/plan'
import { toast } from '@/utils/toast'
import { formatPace, formatDate } from '@/utils/format'
import type { GeneratePlanRequest, GeneratePlanResponse, DailyPlanItem } from '@/types/plan'

const router = useRouter()

interface SelectOption {
  value: string | number
  label: string
  hint?: string
}

const GOAL_OPTIONS: SelectOption[] = [
  { value: '5K_PB', label: '5 公里 PB' },
  { value: '10K_PB', label: '10 公里 PB' },
  { value: 'HALF_MARATHON', label: '半程马拉松' },
  { value: 'MARATHON', label: '全程马拉松' },
  { value: 'LOSE_WEIGHT', label: '减脂' },
  { value: 'KEEP_FIT', label: '保持健康' },
]

const WEEKLY_DAY_OPTIONS: SelectOption[] = [3, 4, 5, 6, 7].map((n) => ({
  value: n,
  label: `${n} 天`,
}))

const PREVIEW_COUNT = 10

const WORKOUT_LABELS: Record<string, string> = {
  EASY_RUN: '轻松跑',
  RECOVERY: '恢复跑',
  LONG_RUN: '长距离跑',
  TEMPO: '节奏跑',
  INTERVAL: '间歇跑',
  STRIDES: '加速跑',
  CROSS_TRAIN: '交叉训练',
  REST: '休息',
}

const WORKOUT_CHIPS: Record<string, string> = {
  EASY_RUN: 'chip-green',
  RECOVERY: 'chip-green',
  LONG_RUN: 'chip-blue',
  TEMPO: 'chip-blue',
  INTERVAL: 'chip-orange',
  STRIDES: 'chip-orange',
  CROSS_TRAIN: 'chip-blue',
  REST: 'chip-gray',
}

function workoutLabel(type: string): string {
  return WORKOUT_LABELS[type] ?? `未知类型（${type}）`
}

function workoutChip(type: string): string {
  return WORKOUT_CHIPS[type] ?? 'chip-gray'
}

interface GenerateForm {
  goalType: string
  goalValue: string
  startDate: string
  endDate: string
  weeklyTrainingDays: number
  trainingDurationMinutes: number | null
  athleteStyle: string
}

function toISODate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const today = toISODate(new Date())
const defaultEndDate = toISODate(new Date(Date.now() + 28 * 86400000))

const loading = ref(false)
const result = ref<GeneratePlanResponse | null>(null)

const form = reactive<GenerateForm>({
  goalType: 'KEEP_FIT',
  goalValue: '',
  startDate: today,
  endDate: defaultEndDate,
  weeklyTrainingDays: 4,
  trainingDurationMinutes: 60,
  athleteStyle: '',
})

const goalValuePlaceholder = computed<string>(() => {
  switch (form.goalType) {
    case '5K_PB':
    case '10K_PB':
      return '目标成绩 25:00'
    case 'HALF_MARATHON':
    case 'MARATHON':
      return '目标成绩 4:30:00，或「完赛」'
    case 'LOSE_WEIGHT':
    case 'KEEP_FIT':
      return '如「完赛」（可选）'
    default:
      return '目标成绩，如 25:00'
  }
})

const previewPlan = computed<DailyPlanItem[]>(() =>
  (result.value?.dailyPlan ?? []).slice(0, PREVIEW_COUNT)
)

const reasoningText = computed<string>(() =>
  reasoningLines(result.value?.aiReasoning ?? '')
)

/** aiReasoning 多行文本风格化：每行以「· 」开头 */
function reasoningLines(text: string): string {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => (line.startsWith('·') ? line : `· ${line}`))
    .join('\n')
}

function onGoalTypeChange(v: string | number | null): void {
  if (typeof v === 'string' && GOAL_OPTIONS.some((o) => o.value === v)) {
    form.goalType = v
  }
}

function onGoalValueChange(v: string | number | null): void {
  form.goalValue = v == null ? '' : String(v)
}

function onStartDateChange(v: string | number | null): void {
  form.startDate = v == null ? '' : String(v)
}

function onEndDateChange(v: string | number | null): void {
  form.endDate = v == null ? '' : String(v)
}

function onWeeklyDaysChange(v: string | number | null): void {
  const n = Number(v)
  if (Number.isInteger(n) && n >= 3 && n <= 7) {
    form.weeklyTrainingDays = n
  }
}

function onDurationChange(v: string | number | null): void {
  if (v == null || v === '') {
    form.trainingDurationMinutes = null
    return
  }
  const n = Number(v)
  form.trainingDurationMinutes = Number.isFinite(n) ? n : null
}

async function handleGenerate(): Promise<void> {
  if (!form.startDate || !form.endDate) {
    toast.error('请选择开始与结束日期')
    return
  }
  if (form.endDate < form.startDate) {
    toast.error('结束日期不能早于开始日期')
    return
  }
  if (form.trainingDurationMinutes != null && (form.trainingDurationMinutes < 20 || form.trainingDurationMinutes > 240)) {
    toast.error('单次时长需在 20-240 分钟之间')
    return
  }

  const payload: GeneratePlanRequest = {
    goalType: form.goalType,
    goalValue: form.goalValue.trim(),
    startDate: form.startDate,
    endDate: form.endDate,
    weeklyTrainingDays: form.weeklyTrainingDays,
    trainingDurationMinutes: form.trainingDurationMinutes ?? undefined,
    athleteStyle: form.athleteStyle,
  }

  loading.value = true
  try {
    result.value = await planApi.generatePlan(payload)
    toast.success('计划生成成功')
  } catch {
    /* 请求拦截器已统一提示错误 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.generate-wrap {
  max-width: 760px;
  margin-left: auto;
  margin-right: auto;
}

.generate-card {
  padding: 32px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.style-grid {
  margin-top: 4px;
}

.choice-icon {
  flex: none;
  color: var(--a-text-secondary);
  transition: color 0.18s ease;
}

.a-choice.is-active .choice-icon {
  color: var(--a-blue);
}

.side-guide {
  margin: 0;
  text-align: center;
  font-size: 14px;
  color: var(--a-text-secondary);
}

.link-btn {
  border: none;
  background: none;
  padding: 0;
  font-size: 14px;
  font-weight: 500;
  font-family: var(--a-font);
  cursor: pointer;
}

.result-card {
  margin-top: 24px;
  text-align: left;
}

.result-name {
  margin: 0;
  font-size: clamp(22px, 2.6vw, 28px);
  font-weight: 700;
  letter-spacing: -0.016em;
  line-height: 1.2;
  color: var(--a-text);
}

.reasoning-block {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reasoning-text {
  margin: 0;
  padding: 18px 20px;
  background: var(--a-fill);
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.7;
  color: var(--a-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--a-font);
}

.result-table {
  margin-top: 22px;
}

.result-more {
  margin: 10px 0 0;
}

.result-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .result-actions {
    flex-direction: column;

    .a-btn {
      width: 100%;
    }
  }
}
</style>
