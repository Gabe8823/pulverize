<template>
  <div class="page dashboard">
    <!-- 页头 -->
    <header class="page-head">
      <p class="eyebrow">数据看板</p>
      <h1 class="headline">你的跑步，一目了然。</h1>
      <p class="subhead">本周跑量、配速与训练计划，一屏掌握每一次进步。</p>
    </header>

    <!-- 顶部统计卡组 -->
    <section class="section">
      <div class="card-grid-4">
        <article v-for="card in statCards" :key="card.label" class="a-card hoverable stat-card">
          <template v-if="loaded">
            <div class="stat-value">
              {{ card.value }}<span v-if="card.unit" class="stat-unit">{{ card.unit }}</span>
            </div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-hint">{{ card.hint }}</div>
          </template>
          <template v-else>
            <div class="a-skeleton skel-value"></div>
            <div class="a-skeleton skel-label"></div>
          </template>
        </article>
      </div>
    </section>

    <!-- 图表区 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">配速与跑量趋势</h2>
          <p class="section-desc">最近几次跑步的配速曲线与单次距离，节奏变化一眼看清。</p>
        </div>
        <span class="caption">配速轴已反向：越靠上，配速越快</span>
      </div>
      <div class="a-card">
        <div v-if="!loaded" class="loading-block"><span class="a-spinner"></span></div>
        <div v-else-if="recentActivities.length === 0" class="a-empty">
          <div class="a-empty__icon">📈</div>
          <div class="a-empty__text">还没有可展示的趋势数据</div>
          <div class="a-empty__sub">完成第一次跑步后，这里会自动生成配速与跑量曲线。</div>
        </div>
        <div v-else ref="paceChartRef" class="chart-box"></div>
      </div>
    </section>

    <!-- 最近活动 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">最近活动</h2>
          <p class="section-desc">点击任意一行，查看这次跑步的完整数据。</p>
        </div>
        <router-link to="/activities" class="a-link">查看全部 →</router-link>
      </div>

      <div v-if="loading && recentActivities.length === 0" class="loading-block">
        <span class="a-spinner"></span>
      </div>

      <div v-else-if="recentActivities.length === 0" class="a-card">
        <div class="a-empty">
          <div class="a-empty__icon">🏃</div>
          <div class="a-empty__text">还没有跑步数据</div>
          <div class="a-empty__sub">手动添加一条，或在「连接」页同步运动手表数据。</div>
          <div class="empty-actions">
            <AButton type="primary" size="sm" @click="openAdd">手动添加</AButton>
            <router-link to="/connect" class="a-link">连接运动 App →</router-link>
          </div>
        </div>
      </div>

      <div v-else class="a-table-wrap">
        <table class="a-table">
          <thead>
            <tr>
              <th>平台</th>
              <th>名称</th>
              <th>日期</th>
              <th>距离</th>
              <th>时长</th>
              <th>平均配速</th>
              <th>平均心率</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in recentActivities"
              :key="item.id"
              @click="goDetail(item)"
            >
              <td class="cell-platform"><PlatformIcon :platform="item.platform" :size="28" /></td>
              <td class="cell-name">{{ item.activityName || '跑步' }}</td>
              <td class="cell-date">{{ formatDate(item.startTime) }}</td>
              <td class="cell-accent">{{ formatDistance(item.distanceM) }} km</td>
              <td>{{ formatDuration(item.durationSeconds) }}</td>
              <td>{{ formatPace(item.avgPaceSecKm) }} /km</td>
              <td>{{ item.avgHeartRate ? `${item.avgHeartRate} bpm` : '--' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 训练计划速览 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">训练计划</h2>
          <p class="section-desc">AI 教练围绕你的目标，安排好每一周。</p>
        </div>
        <router-link to="/plan" class="a-link">全部计划 →</router-link>
      </div>

      <div v-if="loading && plans.length === 0" class="loading-block">
        <span class="a-spinner"></span>
      </div>

      <div
        v-for="plan in activePlans"
        v-else-if="activePlans.length > 0"
        :key="plan.id"
        class="a-card hoverable plan-card"
        @click="router.push(`/plan/${plan.id}`)"
      >
        <div class="plan-head">
          <div class="plan-head__main">
            <h3 class="plan-name">{{ plan.planName }}</h3>
            <p class="caption plan-meta">
              {{ plan.startDate }} ~ {{ plan.endDate }} ·
              {{ goalTypeLabel(plan.goalType) }}
            </p>
          </div>
          <span class="chip chip-blue">进行中</span>
        </div>
        <div class="plan-progress">
          <div class="plan-progress__track">
            <div class="plan-progress__bar" :style="{ width: `${planProgress}%` }"></div>
          </div>
          <span class="plan-progress__pct">{{ planProgress }}%</span>
        </div>
        <p class="caption">按计划周期推进的进度，点击查看每日训练安排。</p>
      </div>

      <div v-else class="a-card plan-cta">
        <div class="plan-cta__text">
          <h3 class="plan-cta__title">生成你的第一份训练计划</h3>
          <p class="plan-cta__desc">
            告诉 AI 你的目标与可用时间，几分钟内得到一份周期化训练安排。
          </p>
        </div>
        <AButton type="primary" size="lg" @click="router.push('/plan/generate')">
          立即生成
        </AButton>
      </div>
    </section>

    <!-- 悬浮添加按钮 -->
    <div class="fab-wrap">
      <AButton type="primary" size="lg" @click="openAdd">＋ 添加记录</AButton>
    </div>

    <!-- 手动添加跑步记录弹窗 -->
    <AModal
      v-model="showAddDialog"
      title="添加跑步记录"
      size="lg"
      confirm-text="保存"
      cancel-text="取消"
      :loading="addLoading"
      @confirm="handleAddActivity"
    >
      <div class="a-form add-form">
        <div class="field-row">
          <div class="a-field">
            <label class="a-label">开始时间</label>
            <input v-model="addForm.startTime" type="datetime-local" class="a-input" />
          </div>
          <div class="a-field">
            <label class="a-label">结束时间</label>
            <input v-model="addForm.endTime" type="datetime-local" class="a-input" />
          </div>
        </div>
        <div class="field-row">
          <div class="a-field">
            <label class="a-label">距离（米）</label>
            <AInput v-model="addForm.distanceM" type="number" :min="1" :step="100" placeholder="5000" />
          </div>
          <div class="a-field">
            <label class="a-label">时长（秒）</label>
            <AInput v-model="addForm.durationSeconds" type="number" :min="1" :step="60" placeholder="1800" />
          </div>
        </div>
        <div class="field-row">
          <div class="a-field">
            <label class="a-label">平均心率</label>
            <AInput v-model="addForm.avgHeartRate" type="number" :min="50" :max="230" placeholder="选填" />
          </div>
          <div class="a-field">
            <label class="a-label">卡路里（kcal）</label>
            <AInput v-model="addForm.calories" type="number" :min="0" :step="10" placeholder="选填" />
          </div>
        </div>
        <div class="a-field">
          <label class="a-label">备注</label>
          <AInput v-model="addForm.remark" type="textarea" :rows="3" placeholder="选填" />
        </div>
      </div>
    </AModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { chartInit } from '@/utils/chart'
import type { ECharts, EChartsOption } from 'echarts'
import { runningApi } from '@/api/running'
import { planApi } from '@/api/plan'
import { toast } from '@/utils/toast'
import { formatPace, formatDistance, formatDuration, formatDate } from '@/utils/format'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'
import AModal from '@/components/ui/AModal.vue'
import PlatformIcon from '@/components/icons/PlatformIcon.vue'
import type { RunningActivity, WeeklyStats } from '@/types/running'
import type { TrainingPlan } from '@/types/plan'

const router = useRouter()

const loading = ref(false)
const loaded = ref(false)
const weeklyStats = ref<WeeklyStats | null>(null)
const recentActivities = ref<RunningActivity[]>([])
const plans = ref<TrainingPlan[]>([])

/* ---------- 顶部统计 ---------- */
interface StatCard {
  label: string
  value: string
  unit: string
  hint: string
}

const statCards = computed<StatCard[]>(() => {
  const s = weeklyStats.value
  const hasPace = !!s && s.avgPace > 0
  return [
    {
      label: '本周距离',
      value: s ? formatDistance(s.totalDistanceM) : '--',
      unit: 'km',
      hint: '本周累计里程',
    },
    {
      label: '本周次数',
      value: s ? String(s.totalActivities) : '--',
      unit: '次',
      hint: '完成的跑步次数',
    },
    {
      label: '本周时长',
      value: s ? formatDuration(s.totalDurationSec) : '--',
      unit: '',
      hint: '累计运动时间',
    },
    {
      label: '平均配速',
      value: hasPace ? formatPace(s!.avgPace) : '--',
      unit: hasPace ? '/km' : '',
      hint: '每公里平均用时',
    },
  ]
})

/* ---------- 训练计划 ---------- */
const activePlan = computed<TrainingPlan | null>(
  () => plans.value.find((p) => p.status === 1) || null
)
const activePlans = computed<TrainingPlan[]>(() =>
  activePlan.value ? [activePlan.value] : []
)

const planProgress = computed(() => {
  const p = activePlan.value
  if (!p) return 0
  const start = new Date(p.startDate).getTime()
  const end = new Date(p.endDate).getTime()
  if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return 0
  const pct = ((Date.now() - start) / (end - start)) * 100
  return Math.min(100, Math.max(0, Math.round(pct)))
})

function goalTypeLabel(type: string) {
  const map: Record<string, string> = {
    '5K_PB': '5K 提速',
    '10K_PB': '10K 突破',
    HALF_MARATHON: '半程马拉松',
    MARATHON: '全程马拉松',
    LOSE_WEIGHT: '减脂',
    KEEP_FIT: '健身',
  }
  return map[type] || type
}

/** 点击行进入详情；id 缺失时不跳转，避免出现 /activities/undefined 白屏 */
function goDetail(item: RunningActivity) {
  const id: number | null | undefined = item.id
  if (id == null || !Number.isFinite(id)) return
  router.push(`/activities/${id}`)
}

/* ---------- 数据加载 ---------- */
async function loadData() {
  loading.value = true
  try {
    const [stats, activities, planList] = await Promise.all([
      runningApi.getWeeklyStats(),
      runningApi.listActivities({ pageNum: 1, pageSize: 8 }),
      planApi.listPlans(),
    ])
    weeklyStats.value = stats
    recentActivities.value = activities.records || []
    plans.value = planList || []
  } catch {
    // 无数据时首次加载失败属于正常情况（拦截器已 toast）
  } finally {
    loading.value = false
    loaded.value = true
  }
  await nextTick()
  renderPaceChart()
}

/* ---------- 图表（深浅色自适应） ---------- */
const paceChartRef = ref<HTMLElement>()
let paceChart: ECharts | null = null
let themeObserver: MutationObserver | null = null

function isDark() {
  return document.documentElement.dataset.theme === 'dark'
}

function buildOption(dates: string[], paces: number[], distances: number[]): EChartsOption {
  const dark = isDark()
  const axisText = '#86868b'
  const splitLine = dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)'
  const primary = dark ? '#ff3b30' : '#e30613'
  const primarySoft = dark ? 'rgba(255,59,48,0.16)' : 'rgba(227,6,19,0.08)'
  const barColor = dark ? 'rgba(255,255,255,0.26)' : 'rgba(0,0,0,0.3)'

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: dark ? '#2c2c2e' : '#ffffff',
      borderColor: splitLine,
      borderWidth: 1,
      textStyle: { color: dark ? '#f5f5f7' : '#1d1d1f', fontSize: 12 },
      formatter(params: unknown) {
        const list = params as Array<{ axisValue?: string; value?: number }>
        const p = list[0]
        const dist = list[1]
        if (!p) return ''
        return `${p.axisValue ?? ''}<br/>配速：${formatPace(p.value ?? 0)} /km<br/>距离：${dist?.value ?? '--'} km`
      },
    },
    legend: { data: ['配速', '跑量'], bottom: 0, textStyle: { color: axisText } },
    grid: { top: 24, right: 56, bottom: 44, left: 64 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: splitLine } },
      axisTick: { show: false },
      axisLabel: { color: axisText },
    },
    yAxis: [
      {
        type: 'value',
        name: '配速 (s/km)',
        inverse: true,
        axisLabel: { formatter: (v: number) => formatPace(v), color: axisText },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: splitLine } },
        nameTextStyle: { color: axisText },
      },
      {
        type: 'value',
        name: '距离 (km)',
        position: 'right',
        axisLabel: { color: axisText },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        nameTextStyle: { color: axisText },
      },
    ],
    series: [
      {
        name: '配速',
        type: 'line',
        data: paces,
        smooth: true,
        lineStyle: { color: primary, width: 2.5 },
        itemStyle: { color: primary },
        areaStyle: { color: primarySoft },
      },
      {
        name: '跑量',
        type: 'bar',
        yAxisIndex: 1,
        data: distances,
        barWidth: '40%',
        itemStyle: { color: barColor, borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
}

function renderPaceChart() {
  if (!paceChartRef.value || recentActivities.value.length === 0) return

  if (paceChart && paceChart.getDom() !== paceChartRef.value) {
    paceChart.dispose()
    paceChart = null
  }
  if (!paceChart) {
    paceChart = chartInit(paceChartRef.value)
  }

  const data = [...recentActivities.value].reverse()
  const dates = data.map((d) => {
    const dt = new Date(d.startTime)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  })
  const paces = data.map((d) => d.avgPaceSecKm || 0)
  const distances = data.map((d) => +(d.distanceM / 1000).toFixed(2))

  paceChart.setOption(buildOption(dates, paces, distances), true)
}

function handleResize() {
  paceChart?.resize()
}

/* ---------- 手动添加 ---------- */
interface AddFormState {
  startTime: string
  endTime: string
  distanceM: string | number | null
  durationSeconds: string | number | null
  avgHeartRate: string | number | null
  calories: string | number | null
  remark: string | number | null
}

const showAddDialog = ref(false)
const addLoading = ref(false)
const addForm = reactive<AddFormState>({
  startTime: '',
  endTime: '',
  distanceM: 5000,
  durationSeconds: 1800,
  avgHeartRate: null,
  calories: null,
  remark: '',
})

function toLocalInput(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toDateTime(v: string) {
  return v && v.length === 16 ? `${v}:00` : v
}

function toNum(v: string | number | null) {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function openAdd() {
  const now = new Date()
  addForm.startTime = toLocalInput(new Date(now.getTime() - 30 * 60 * 1000))
  addForm.endTime = toLocalInput(now)
  addForm.distanceM = 5000
  addForm.durationSeconds = 1800
  addForm.avgHeartRate = null
  addForm.calories = null
  addForm.remark = ''
  showAddDialog.value = true
}

async function handleAddActivity() {
  if (!addForm.startTime || !addForm.endTime || !toNum(addForm.distanceM) || !toNum(addForm.durationSeconds)) {
    toast('请填写开始时间、结束时间、距离和时长', 'info')
    return
  }
  addLoading.value = true
  try {
    await runningApi.createActivity({
      startTime: toDateTime(addForm.startTime),
      endTime: toDateTime(addForm.endTime),
      durationSeconds: toNum(addForm.durationSeconds),
      distanceM: toNum(addForm.distanceM),
      avgHeartRate: toNum(addForm.avgHeartRate) || undefined,
      calories: toNum(addForm.calories) || undefined,
      remark: addForm.remark ? String(addForm.remark) : undefined,
    })
    toast.success('添加成功')
    showAddDialog.value = false
    loadData()
  } catch {
    // 由拦截器处理
  } finally {
    addLoading.value = false
  }
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  themeObserver = new MutationObserver(() => renderPaceChart())
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
  window.addEventListener('resize', handleResize)
  loadData()
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  themeObserver = null
  window.removeEventListener('resize', handleResize)
  paceChart?.dispose()
  paceChart = null
})
</script>

<style scoped lang="scss">
/* 统计卡 */
.stat-card {
  min-height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-unit {
  margin-left: 7px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0;
  color: var(--a-text-tertiary);
}

.skel-value {
  height: 42px;
  width: 65%;
  border-radius: 12px;
}
.skel-label {
  height: 15px;
  width: 45%;
  margin-top: 12px;
}

/* 图表 */
.chart-box {
  width: 100%;
  height: 300px;
}

/* 加载 */
.loading-block {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 0;
}

/* 空状态操作 */
.empty-actions {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  flex-wrap: wrap;
}

/* 表格单元格 */
.cell-platform {
  width: 56px;
}
.cell-name {
  font-weight: 600;
}
.cell-date {
  color: var(--a-text-secondary);
  white-space: nowrap;
}
.cell-accent {
  color: var(--a-blue);
  font-weight: 600;
}

/* 训练计划卡 */
.plan-card {
  cursor: pointer;
}

.plan-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.plan-head__main {
  min-width: 0;
}
.plan-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.014em;
  line-height: 1.25;
  color: var(--a-text);
}
.plan-meta {
  margin-top: 7px;
  font-variant-numeric: tabular-nums;
}

.plan-progress {
  margin: 20px 0 12px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.plan-progress__track {
  flex: 1;
  height: 8px;
  border-radius: 980px;
  background: var(--a-fill);
  overflow: hidden;
}
.plan-progress__bar {
  height: 100%;
  border-radius: 980px;
  background: var(--a-blue);
  transition: width 0.35s ease;
}
.plan-progress__pct {
  font-size: 15px;
  font-weight: 700;
  color: var(--a-blue);
  font-variant-numeric: tabular-nums;
  min-width: 46px;
  text-align: right;
}

/* 计划引导卡 */
.plan-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}
.plan-cta__title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}
.plan-cta__desc {
  margin: 8px 0 0;
  font-size: 14.5px;
  line-height: 1.55;
  color: var(--a-text-secondary);
  max-width: 560px;
}

/* 弹窗表单 */
.add-form {
  padding: 6px 0 10px;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
/* datetime-local 原生控件对齐整体字号，避免显示不全 */
.field-row .a-input[type='datetime-local'] {
  font-size: 15.5px;
  min-width: 0;
}

/* 悬浮添加 */
.fab-wrap {
  position: fixed;
  right: 36px;
  bottom: 36px;
  z-index: 90;
}
.fab-wrap .a-btn {
  box-shadow: 0 12px 32px var(--a-blue-ring), var(--a-shadow-card-hover);
}

@media (max-width: 640px) {
  .field-row {
    grid-template-columns: 1fr;
  }
  .fab-wrap {
    right: 20px;
    bottom: 20px;
  }
}
</style>
