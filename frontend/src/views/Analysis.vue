<template>
  <div class="page">
    <header class="page-head">
      <p class="eyebrow">跑步分析</p>
      <h1 class="display">AI 教练，读懂你的每一步</h1>
      <p class="subhead">
        选择一条跑步记录，从配速、心率、步频到疲劳与训练效果，多维分析给出综合评分与教练级点评。
      </p>
    </header>

    <!-- 活动选择器 -->
    <section class="section selector-bar">
      <div class="select-wrap">
        <ASelect
          :model-value="selectedActivityId"
          :options="selectOptions"
          placeholder="选择一条跑步记录"
          @update:model-value="onSelectInput"
          @change="handleSelectChange"
        />
      </div>
      <AButton
        type="primary"
        size="lg"
        :loading="analyzing"
        :disabled="!selectedActivityId"
        @click="handleAnalyze"
      >
        {{ result ? '重新分析' : '开始分析' }}
      </AButton>
    </section>

    <!-- 跑力指数 VDOT -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">跑力指数 · VDOT</h2>
          <p class="section-desc">基于你的全部跑步数据推算的有氧能力指数，并给出等效完赛时间与训练配速区间。</p>
        </div>
        <span
          class="chip"
          :class="vdotResult && vdotResult.source === 'RACE' ? 'chip-blue' : 'chip-gray'"
        >
          {{ vdotLoading ? '计算中…' : vdotResult && vdotResult.source === 'RACE' ? '基于全部数据' : '数据不足' }}
        </span>
      </div>

      <div v-if="vdotLoading" class="a-empty">
        <span class="a-spinner"></span>
      </div>

      <div v-else-if="vdotResult && vdotResult.source === 'RACE'" class="vdot-grid">
        <!-- 指数与参考成绩 -->
        <div class="a-card vdot-score-card">
          <div class="vdot-score">
            <span class="vdot-number">{{ vdotDisplay }}</span>
            <span class="vdot-label">VDOT</span>
          </div>
          <p v-if="vdotResult.race" class="vdot-ref">
            参考：{{ vdotResult.race.activityName }} · {{ formatDate(vdotResult.race.raceDate) }} ·
            {{ vdotResult.race.distanceKm.toFixed(2) }} 公里 · {{ formatDuration(vdotResult.race.durationSec) }}
          </p>
        </div>

        <!-- 等效完赛时间 -->
        <div class="a-card vdot-equiv-card">
          <div class="vdot-card-title">等效完赛时间</div>
          <div class="vdot-equiv-grid">
            <div v-for="e in vdotEquivList" :key="e.label" class="vdot-equiv-item">
              <span class="vdot-equiv-time">{{ e.time }}</span>
              <span class="vdot-equiv-label">{{ e.label }}</span>
            </div>
          </div>
        </div>

        <!-- 训练配速区间 -->
        <div class="a-card vdot-zone-card">
          <div class="vdot-card-title">训练配速区间</div>
          <div class="vdot-zone-row vdot-zone-head">
            <span>类型</span>
            <span>强度</span>
            <span class="vdot-zone-pace">配速区间（快–慢）</span>
          </div>
          <div v-for="z in vdotZones" :key="z.name" class="vdot-zone-row">
            <span class="vdot-zone-name">{{ z.name }}</span>
            <span class="vdot-zone-pct">{{ z.percentLabel }}</span>
            <span class="vdot-zone-pace">{{ formatPace(z.fastPaceSecKm) }}–{{ formatPace(z.slowPaceSecKm) }}/km</span>
          </div>
        </div>
      </div>

      <div v-else class="a-empty">
        <div class="a-empty__text">{{ (vdotResult && vdotResult.message) || '数据不足' }}</div>
        <div class="a-empty__sub">完成一次 2 公里以上、8 分钟以上的跑步后再来看看</div>
      </div>
    </section>

    <!-- 加载中 -->
    <section v-if="loading || analyzing" class="section loading-block">
      <span class="a-spinner"></span>
    </section>

    <template v-else-if="result">
      <!-- 总览卡 + AI 教练点评卡 -->
      <section class="section overview-grid">
        <div class="a-card overview-card">
          <div class="score-col">
            <div class="stat-value score-value">
              {{ scoreText }}<span v-if="scoreText !== '--'" class="score-unit">/100</span>
            </div>
            <div class="stat-label">综合评分</div>
          </div>
          <div class="overview-meta">
            <p class="overview-name">{{ result.activityName || '本次跑步' }}</p>
            <p class="caption">{{ activityDate }}</p>
            <span v-if="scoreLevel" class="chip" :class="scoreLevel.chip">{{ scoreLevel.text }}</span>
          </div>
        </div>

        <div class="a-card coach-card" style="border-left: 3px solid var(--a-blue)">
          <div class="coach-head">
            <svg
              class="coach-icon"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3z" />
              <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z" />
            </svg>
            <h2 class="coach-title">AI 教练点评</h2>
          </div>
          <p class="coach-text">{{ result.aiSummary || '完成分析后，教练会在这里给出本次训练的完整点评。' }}</p>
        </div>
      </section>

      <!-- 维度分析 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">多维分析</h2>
            <p class="section-desc">配速、心率、步频、疲劳与训练效果，逐项给出结论与建议。</p>
          </div>
        </div>

        <div class="card-grid-2">
          <div v-for="dim in dims" :key="dim.title" class="a-card dim-card">
            <div class="dim-head">
              <h3 class="dim-title">{{ dim.title }}</h3>
              <span class="caption">{{ dim.items.length }} 项指标</span>
            </div>

            <!-- 训练效果数值块 -->
            <div v-if="dim.title === '训练效果' && trainingTe" class="te-block">
              <span class="te-text">{{ trainingTe.text }}</span>
              <span class="chip" :class="trainingTe.chip">{{ trainingTe.chipText }}</span>
            </div>

            <template v-for="item in dim.items" :key="item.key">
              <!-- 嵌套对象 / 数组 → 表格 -->
              <div v-if="item.table" class="table-block">
                <p class="caption table-caption">{{ item.key }}</p>
                <div class="a-table-wrap inner-table">
                  <table class="a-table">
                    <thead>
                      <tr>
                        <th v-for="h in item.table.headers" :key="h">{{ h }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, ri) in item.table.rows" :key="ri">
                        <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <!-- 标量 → 键值行 -->
              <div v-else class="detail-item">
                <span class="detail-label">{{ item.key }}</span>
                <span class="detail-value">{{ item.text }}</span>
              </div>
            </template>
          </div>
        </div>
      </section>
    </template>

    <!-- 空状态 -->
    <section v-else class="section">
      <div class="a-empty">
        <div class="a-empty__text">还没有分析结果</div>
        <div class="a-empty__sub">选择上方任意一条跑步记录，点击「开始分析」。</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AButton from '@/components/ui/AButton.vue'
import ASelect from '@/components/ui/ASelect.vue'
import { runningApi } from '@/api/running'
import { analysisApi } from '@/api/analysis'
import { toast } from '@/utils/toast'
import { formatPace, formatDistance, formatDuration, formatDate } from '@/utils/format'
import type { RunningActivity } from '@/types/running'
import type { AnalysisResultVO, VdotResult } from '@/types/analysis'

/* ---------- 状态 ---------- */
const activities = ref<RunningActivity[]>([])
const selectedActivityId = ref<number | null>(null)
const result = ref<AnalysisResultVO | null>(null)
const loading = ref(false)
const analyzing = ref(false)

/* ---------- 活动选择器 ---------- */
const selectOptions = computed(() =>
  activities.value.map((a) => ({
    value: a.id,
    label: `${a.activityName || '跑步记录'}（${formatDate(a.startTime)}）`,
    hint: `${formatDistance(a.distanceM)} 公里 · ${formatPace(a.avgPaceSecKm)}/公里`,
  }))
)

function onSelectInput(v: string | number | null): void {
  selectedActivityId.value = typeof v === 'number' ? v : v === null ? null : Number(v)
}

function handleSelectChange(): void {
  result.value = null
  if (selectedActivityId.value !== null) void loadCache()
}

const selectedActivity = computed<RunningActivity | null>(
  () => activities.value.find((a) => a.id === selectedActivityId.value) ?? null
)

const activityDate = computed<string>(() => {
  const a = selectedActivity.value
  return a ? formatDate(a.startTime) : '--'
})

/* ---------- 总览 ---------- */
const scoreText = computed<string>(() => {
  const s = result.value?.score
  return s === null || s === undefined ? '--' : String(s)
})

const scoreLevel = computed<{ text: string; chip: string } | null>(() => {
  const s = result.value?.score
  if (s === null || s === undefined) return null
  if (s >= 80) return { text: '表现出色', chip: 'chip-green' }
  if (s >= 60) return { text: '稳定发挥', chip: 'chip-blue' }
  return { text: '仍有提升空间', chip: 'chip-orange' }
})

/* ---------- 维度数据 ---------- */
interface NestedTable {
  headers: string[]
  rows: string[][]
}

interface DimItem {
  key: string
  text: string
  table: NestedTable | null
}

interface DimCard {
  title: string
  items: DimItem[]
}

/** 英文枚举值兜底中文映射（未知值原样展示） */
const ENUM_ZH: Record<string, string> = {
  MANUAL: '手动录入',
  EASY_RUN: '轻松跑',
  TEMPO_RUN: '节奏跑',
  INTERVAL_RUN: '间歇跑',
  LONG_RUN: '长距离跑',
  RECOVERY_RUN: '恢复跑',
  RACE: '比赛',
  EASY: '轻松',
  TEMPO: '节奏',
  INTERVAL: '间歇',
  RECOVERY: '恢复',
}

/** 标量值格式化：配速秒值 → formatPace、距离 → formatDistance、百分比带 %，其余原样 */
function formatValue(key: string, v: unknown): string {
  if (v === null || v === undefined || v === '') return '--'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (typeof v === 'number') {
    if (key.includes('(%)') || key.includes('%')) return `${v}%`
    if (key.includes('配速') && !key.includes('秒') && v > 0) return formatPace(v)
    if (key.includes('距离') || key.includes('跑量')) return `${formatDistance(v)} 公里`
    if (key.includes('时长') || key.includes('用时')) return formatDuration(v)
    if (key.includes('心率')) return `${v} 次/分`
    if (key.includes('步频')) return `${v} 步/分`
    return Number.isInteger(v) ? String(v) : String(Math.round(v * 10) / 10)
  }
  if (typeof v === 'string') return ENUM_ZH[v] ?? v
  return String(v)
}

/** 对象/数组 → 全中文表头嵌套表（表头取内层中文 key） */
function buildTable(v: unknown): NestedTable | null {
  if (Array.isArray(v)) {
    if (v.length === 0) return null
    const first = v[0]
    if (first !== null && typeof first === 'object') {
      const headers = Object.keys(first)
      return {
        headers,
        rows: v.map((item) =>
          headers.map((h) =>
            formatValue(h, (item as Record<string, unknown>)[h])
          )
        ),
      }
    }
    return {
      headers: ['数值'],
      rows: v.map((item) => [formatValue('数值', item)]),
    }
  }
  if (v !== null && typeof v === 'object') {
    const entries = Object.entries(v as Record<string, unknown>)
    if (entries.length === 0) return null
    return {
      headers: entries.map(([k]) => k),
      rows: [entries.map(([k, val]) => formatValue(k, val))],
    }
  }
  return null
}

function toItems(map: Record<string, unknown>, skipKeys: string[]): DimItem[] {
  return Object.entries(map)
    .filter(([k]) => !skipKeys.includes(k))
    .map(([k, v]) => {
      const table = v !== null && typeof v === 'object' ? buildTable(v) : null
      return { key: k, text: table ? '' : formatValue(k, v), table }
    })
}

/** 训练效果数值（0-5）解析 */
function teNumber(v: unknown): number | null {
  if (typeof v === 'number' && v >= 0 && v <= 5) return v
  if (typeof v === 'string' && v.trim() !== '') {
    const n = Number(v)
    if (!Number.isNaN(n) && n >= 0 && n <= 5) return n
  }
  return null
}

interface TeBlock {
  text: string
  chip: string
  chipText: string
}

const trainingTe = computed<TeBlock | null>(() => {
  const map = result.value?.trainingEffect
  if (!map) return null
  const aerobic = teNumber(map['有氧训练效果'])
  const anaerobic = teNumber(map['无氧训练效果'])
  if (aerobic === null && anaerobic === null) return null
  const level = aerobic ?? anaerobic ?? 0
  let chip = 'chip-gray'
  let chipText = '恢复为主'
  if (level >= 4) {
    chip = 'chip-red'
    chipText = '高负荷刺激'
  } else if (level >= 3) {
    chip = 'chip-orange'
    chipText = '高强度刺激'
  } else if (level >= 2) {
    chip = 'chip-blue'
    chipText = '有氧提升'
  }
  const fmt = (n: number | null): string => (n === null ? '--' : n.toFixed(1))
  return {
    text: `有氧 ${fmt(aerobic)} / 无氧 ${fmt(anaerobic)}`,
    chip,
    chipText,
  }
})

const dims = computed<DimCard[]>(() => {
  const r = result.value
  if (!r) return []
  const defs: Array<{ title: string; map: Record<string, unknown> | null }> = [
    { title: '配速分析', map: r.paceAnalysis },
    { title: '心率分析', map: r.heartRateAnalysis },
    { title: '步频分析', map: r.cadenceAnalysis },
    { title: '疲劳分析', map: r.fatigueAnalysis },
    { title: '训练效果', map: r.trainingEffect },
  ]
  const skipTe =
    trainingTe.value !== null ? ['有氧训练效果', '无氧训练效果'] : []
  return defs
    .filter((d): d is { title: string; map: Record<string, unknown> } => {
      return !!d.map && Object.keys(d.map).length > 0
    })
    .map((d) => ({
      title: d.title,
      items: toItems(d.map, d.title === '训练效果' ? skipTe : []),
    }))
})

/* ---------- 跑力指数 VDOT ---------- */
const vdotResult = ref<VdotResult | null>(null)
const vdotLoading = ref(true)

const vdotDisplay = computed(() => {
  const v = vdotResult.value?.vdot
  return v != null ? v.toFixed(1) : '--'
})

const vdotEquivList = computed(() => {
  const eq = vdotResult.value?.equivalents
  if (!eq) return []
  return (['5K', '10K', '半马', '全马'] as const)
    .filter((k) => typeof eq[k] === 'number')
    .map((k) => ({ label: k, time: formatDuration(eq[k]) }))
})

const vdotZones = computed(() => vdotResult.value?.zones ?? [])

async function loadVdot(): Promise<void> {
  vdotLoading.value = true
  try {
    vdotResult.value = await analysisApi.getVdot()
  } catch {
    // 拦截器已统一提示
  } finally {
    vdotLoading.value = false
  }
}

/* ---------- 数据流 ---------- */
onMounted(async () => {
  void loadVdot()
  try {
    const res = await runningApi.listActivities({ pageNum: 1, pageSize: 50 })
    activities.value = res.records || []
  } catch {
    // 拦截器已统一提示
  }
})

async function loadCache(): Promise<void> {
  if (!selectedActivityId.value) return
  loading.value = true
  try {
    result.value = await analysisApi.getAnalysis(selectedActivityId.value)
  } catch {
    // 拦截器已统一提示
  } finally {
    loading.value = false
  }
}

async function handleAnalyze(): Promise<void> {
  if (!selectedActivityId.value) return
  analyzing.value = true
  try {
    result.value = await analysisApi.analyzeActivity(selectedActivityId.value)
    toast.success('分析完成')
  } catch {
    // 拦截器已统一提示
  } finally {
    analyzing.value = false
  }
}
</script>

<style scoped lang="scss">
/* 选择器 */
.selector-bar {
  display: flex;
  gap: 14px;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.select-wrap {
  width: min(520px, 100%);
}

/* 加载 */
.loading-block {
  display: flex;
  justify-content: center;
  padding: 64px 0;
}

/* 总览 + AI 点评 */
.overview-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(300px, 400px) 1fr;
}

@media (max-width: 860px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-col {
  flex: none;
}

.score-value {
  color: var(--a-text);
  white-space: nowrap;
}

.score-unit {
  margin-left: 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--a-text-tertiary);
  letter-spacing: -0.01em;
}

.overview-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.overview-name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}

/* AI 教练点评卡 */
.coach-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.coach-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.coach-icon {
  color: var(--a-blue);
  flex: none;
}

.coach-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}

.coach-text {
  margin: 12px 0 0;
  font-size: 15.5px;
  line-height: 1.6;
  color: var(--a-text);
  white-space: pre-line;
}

/* 维度卡 */
.dim-card {
  min-width: 0;
}

.dim-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dim-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 9px 0;
  border-bottom: 1px solid var(--a-divider);

  &:last-child {
    border-bottom: none;
  }
}

.detail-label {
  font-size: 13.5px;
  color: var(--a-text-secondary);
}

.detail-value {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--a-text);
  text-align: right;
  max-width: 62%;
  word-break: break-word;
}

/* 嵌套表格 */
.table-block {
  padding-top: 4px;
  margin-bottom: 6px;

  &:last-child {
    margin-bottom: 0;
  }
}

.table-caption {
  margin: 6px 0 8px;
  font-weight: 600;
  color: var(--a-text-secondary);
}

.inner-table {
  background: var(--a-fill);
  box-shadow: none;
  border-radius: 12px;
}

.inner-table .a-table {
  min-width: 0;
  font-size: 13.5px;
}

.inner-table .a-table th {
  padding: 10px 12px;
  font-size: 12.5px;
  white-space: normal;
}

.inner-table .a-table td {
  padding: 10px 12px;
}

/* 训练效果数值块 */
.te-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 14px;
  margin-bottom: 8px;
  background: var(--a-fill);
  border-radius: 12px;
}

.te-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--a-text);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

/* ---- 跑力指数 VDOT ---- */
.vdot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}
.vdot-score-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}
.vdot-score {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.vdot-number {
  font-size: clamp(42px, 6vw, 58px);
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--a-text);
}
.vdot-label {
  font-size: 15px;
  font-weight: 650;
  color: var(--a-text-secondary);
}
.vdot-ref {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--a-text-secondary);
}
.vdot-card-title {
  font-size: 13px;
  font-weight: 650;
  color: var(--a-text-secondary);
  margin-bottom: 12px;
}
.vdot-equiv-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.vdot-equiv-item {
  background: var(--a-fill);
  border-radius: 12px;
  padding: 13px 15px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.vdot-equiv-time {
  font-size: 22px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
  color: var(--a-text);
}
.vdot-equiv-label {
  font-size: 12.5px;
  color: var(--a-text-secondary);
}
.vdot-zone-row {
  display: grid;
  grid-template-columns: 1.1fr 0.8fr 1.3fr;
  gap: 10px;
  align-items: center;
  padding: 11px 0;
  font-size: 14px;
  border-top: 1px solid rgba(128, 128, 128, 0.22);
}
.vdot-zone-head {
  border-top: none;
  padding-top: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--a-text-secondary);
}
.vdot-zone-name {
  font-weight: 600;
}
.vdot-zone-pace {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
@media (max-width: 640px) {
  .vdot-zone-row {
    grid-template-columns: 1fr 1fr;
  }
  .vdot-zone-row .vdot-zone-pace {
    grid-column: 1 / -1;
    text-align: left;
    font-size: 13px;
    color: var(--a-text-secondary);
  }
}
</style>
