<template>
  <div class="page">
    <template v-if="detail">
      <button class="back-link" @click="router.push('/plan')">‹ 训练计划</button>

      <header class="page-head">
        <p class="eyebrow">计划详情</p>
        <h1 class="headline">{{ detail.plan.planName }}</h1>
        <div class="detail-meta">
          <span class="chip chip-blue">
            {{ goalTypeLabel(detail.plan.goalType) }}<template v-if="detail.plan.goalValue"> · {{ detail.plan.goalValue }}</template>
          </span>
          <span class="chip" :class="statusChip(detail.plan.status)">{{ statusLabel(detail.plan.status) }}</span>
          <span class="meta-item">{{ detail.plan.startDate }} ~ {{ detail.plan.endDate }}</span>
          <span v-if="weeklyDays" class="meta-item">每周 {{ weeklyDays }} 天</span>
          <span class="meta-item">生成于 {{ formatDateTime(detail.plan.createTime) }}</span>
        </div>
        <div class="head-actions">
          <AButton type="primary" :loading="pushing" @click="pushVisible = true">
            推送到手表
          </AButton>
        </div>
      </header>

      <section v-if="detail.plan.aiReasoning" class="section reasoning-section">
        <div class="reasoning-toggle">
          <AButton type="ghost" @click="reasoningOpen = !reasoningOpen">
            {{ reasoningOpen ? '收起 AI 生成思路' : '查看 AI 生成思路' }}
          </AButton>
        </div>
        <div v-if="reasoningOpen" class="a-card">
          <pre class="reasoning-text">{{ reasoningText }}</pre>
        </div>
      </section>

      <section v-for="week in weeks" :key="week.key" class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">第 {{ week.index }} 周 · {{ week.rangeLabel }}</h2>
            <p class="section-desc">{{ week.trainCount }} 次训练 · {{ week.restCount }} 天休息</p>
          </div>
          <div class="week-total">
            <span class="stat-value">{{ week.totalKm.toFixed(1) }}</span>
            <span class="stat-label">公里</span>
          </div>
        </div>

        <div class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>类型</th>
                <th>训练内容</th>
                <th>距离</th>
                <th>时长</th>
                <th>配速</th>
                <th>心率</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="day in week.items"
                :key="day.id"
                :class="{ 'is-done': day.status === 1, 'is-skipped': day.status === 2 }"
              >
                <td class="cell-date">{{ formatDate(day.planDate) }}</td>
                <td>
                  <span class="chip" :class="workoutChip(day.workoutType)">
                    {{ workoutLabel(day.workoutType) }}
                  </span>
                </td>
                <td class="cell-desc" :class="{ 'is-done-text': day.status === 1 }">
                  {{ day.workoutDescription }}
                </td>
                <td class="cell-num">{{ day.targetDistanceKm != null ? day.targetDistanceKm + ' km' : '--' }}</td>
                <td class="cell-num">{{ day.targetDurationMin != null ? day.targetDurationMin + ' 分' : '--' }}</td>
                <td class="cell-num">{{ formatPace(day.targetPaceSecKm) }} /km</td>
                <td class="cell-num">{{ hrLabel(day.targetHrZone) }}</td>
                <td class="cell-actions">
                  <div class="day-status">
                    <AButton
                      size="sm"
                      :type="day.status === 0 ? 'primary' : 'ghost'"
                      @click="setStatus(day, 0)"
                    >
                      待完成
                    </AButton>
                    <AButton
                      size="sm"
                      :type="day.status === 1 ? 'primary' : 'ghost'"
                      @click="setStatus(day, 1)"
                    >
                      已完成
                    </AButton>
                    <AButton
                      size="sm"
                      :type="day.status === 2 ? 'primary' : 'ghost'"
                      @click="setStatus(day, 2)"
                    >
                      已跳过
                    </AButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <div v-else-if="loading" class="loading-state">
      <span class="a-spinner"></span>
    </div>

    <div v-else class="a-empty">
      <div class="a-empty__text">训练计划加载失败</div>
      <div class="a-empty__sub">请返回列表后重试</div>
    </div>

    <!-- 推送到手表确认 -->
    <AModal
      v-model="pushVisible"
      title="推送到手表？"
      confirm-text="确认推送"
      cancel-text="取消"
      :loading="pushing"
      @confirm="handlePushToWatch"
    >
      将把计划内非休息日训练逐日推送到高驰训练日历，同步 App 后可下发到手表。
    </AModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AButton from '@/components/ui/AButton.vue'
import AModal from '@/components/ui/AModal.vue'
import { planApi } from '@/api/plan'
import { toast } from '@/utils/toast'
import { formatPace, formatDate, formatDateTime } from '@/utils/format'
import type { PlanDetailVO, TrainingPlanDetail } from '@/types/plan'

const route = useRoute()
const router = useRouter()

const detail = ref<PlanDetailVO | null>(null)
const loading = ref(true)
const reasoningOpen = ref(false)

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const id = Number(route.params.id)
    detail.value = await planApi.getPlanDetail(id)
  } catch {
    /* 请求拦截器已统一提示错误 */
  } finally {
    loading.value = false
  }
}

async function setStatus(day: TrainingPlanDetail, status: number): Promise<void> {
  if (day.status === status) return
  try {
    await planApi.updateDayStatus(day.id, status)
    day.status = status
    toast.success('已更新')
  } catch {
    /* 请求拦截器已统一提示错误 */
  }
}

// ==================== 推送到手表 ====================

const pushVisible = ref(false)
const pushing = ref(false)

async function handlePushToWatch(): Promise<void> {
  const planId = detail.value?.plan.id
  if (!planId || pushing.value) return

  pushing.value = true
  try {
    const res = await planApi.pushToWatch(planId)
    pushVisible.value = false

    const pushed = res?.pushed ?? 0
    const failed = res?.failed ?? 0
    const errors = res?.errors ?? []
    if (failed > 0) {
      // 失败明细截取前几条，避免 toast 过长
      const detailText = errors.length > 0 ? `：${errors.slice(0, 3).join('；')}` : ''
      toast.error(`已推送 ${pushed} 天，失败 ${failed} 天${detailText}`)
    } else {
      toast.success(`已推送 ${pushed} 天，同步 App 后可下发到手表`)
    }
  } catch {
    /* 请求拦截器已统一提示错误 */
  } finally {
    pushing.value = false
  }
}

// ==================== 中文映射 ====================

const GOAL_LABELS: Record<string, string> = {
  '5K_PB': '5 公里 PB',
  '10K_PB': '10 公里 PB',
  HALF_MARATHON: '半程马拉松',
  MARATHON: '全程马拉松',
  LOSE_WEIGHT: '减脂',
  KEEP_FIT: '保持健康',
}

function goalTypeLabel(type: string): string {
  return GOAL_LABELS[type] ?? `未知目标（${type}）`
}

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

const STATUS_LABELS: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已完成',
  3: '已终止',
}

const STATUS_CHIPS: Record<number, string> = {
  0: 'chip-gray',
  1: 'chip-green',
  2: 'chip-blue',
  3: 'chip-orange',
}

function statusLabel(s: number): string {
  return STATUS_LABELS[s] ?? `未知状态（${s}）`
}

function statusChip(s: number): string {
  return STATUS_CHIPS[s] ?? 'chip-gray'
}

const HR_LABELS: Record<string, string> = {
  Z1: 'Z1 恢复区',
  Z2: 'Z2 有氧区',
  Z3: 'Z3 节奏区',
  'Z3-Z4': 'Z3-Z4 乳酸阈区',
  Z4: 'Z4 乳酸阈区',
  Z5: 'Z5 无氧区',
}

function hrLabel(zone: string | null): string {
  if (!zone) return '--'
  return HR_LABELS[zone] ?? `${zone} 区`
}

// ==================== AI 生成思路 ====================

const reasoningText = computed<string>(() =>
  reasoningLines(detail.value?.plan.aiReasoning ?? '')
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

// ==================== 按自然周分组 ====================

interface WeekGroup {
  key: string
  index: number
  rangeLabel: string
  totalKm: number
  trainCount: number
  restCount: number
  items: TrainingPlanDetail[]
}

function parseDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map((part) => Number(part))
  return new Date(y, (m || 1) - 1, d || 1)
}

function formatMD(d: Date): string {
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const weeks = computed<WeekGroup[]>(() => {
  const items = detail.value?.dailyDetails ?? []
  const groups: WeekGroup[] = []
  const byKey = new Map<string, WeekGroup>()

  for (const item of items) {
    const d = parseDate(item.planDate)
    const mondayOffset = (d.getDay() + 6) % 7
    const monday = new Date(d.getFullYear(), d.getMonth(), d.getDate() - mondayOffset)
    const key = `${monday.getFullYear()}-${monday.getMonth() + 1}-${monday.getDate()}`

    let group = byKey.get(key)
    if (!group) {
      const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6)
      group = {
        key,
        index: groups.length + 1,
        rangeLabel: `${formatMD(monday)}-${formatMD(sunday)}`,
        totalKm: 0,
        trainCount: 0,
        restCount: 0,
        items: [],
      }
      groups.push(group)
      byKey.set(key, group)
    }

    group.items.push(item)
    group.totalKm += item.targetDistanceKm ?? 0
    if (item.workoutType === 'REST') {
      group.restCount += 1
    } else {
      group.trainCount += 1
    }
  }

  return groups
})

/** 由每日安排推算每周训练天数 */
const weeklyDays = computed<number | null>(() => {
  const items = detail.value?.dailyDetails ?? []
  if (items.length === 0 || weeks.value.length === 0) return null
  const active = items.filter((i) => i.workoutType !== 'REST').length
  return Math.max(1, Math.round(active / weeks.value.length))
})
</script>

<style scoped lang="scss">
.head-actions {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-meta {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 14px;
  color: var(--a-text-secondary);
  font-variant-numeric: tabular-nums;
}

.reasoning-section {
  max-width: 860px;
  margin-left: auto;
  margin-right: auto;
}

.reasoning-toggle {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.reasoning-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--a-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--a-font);
}

.week-total {
  display: flex;
  align-items: baseline;
  gap: 6px;

  .stat-value {
    font-size: clamp(24px, 2.4vw, 30px);
  }
}

.a-table {
  min-width: 980px;
}

.cell-date {
  white-space: nowrap;
  font-weight: 600;
}

.cell-desc {
  min-width: 220px;
  line-height: 1.55;
  color: var(--a-text-secondary);
}

.cell-num {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.cell-actions {
  white-space: nowrap;
}

.day-status {
  display: flex;
  gap: 6px;
}

tr.is-done td {
  opacity: 0.65;
}

tr.is-skipped td {
  opacity: 0.75;
}

.cell-desc.is-done-text {
  text-decoration: line-through;
}

.loading-state {
  padding: 72px 0;
  display: flex;
  justify-content: center;
}
</style>
