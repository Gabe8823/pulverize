<template>
  <!--
    单一稳定根节点：路由离场过渡（page-fade out-in）期间根元素不可被替换，
    否则过渡会永久卡死，导致后续所有页面渲染空白。内部状态一律在此根内切换。
  -->
  <div class="page" :class="{ 'page-center': !detail }">
    <!-- 加载中 -->
    <span v-if="loading" class="a-spinner" aria-label="加载中"></span>

    <!-- 加载失败 / 活动不存在 -->
    <div v-else-if="loadError" class="a-empty">
      <div class="a-empty__icon" aria-hidden="true">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.5v5.5" />
          <path d="M12 16.4v.2" />
        </svg>
      </div>
      <div class="a-empty__text">活动加载失败</div>
      <div class="a-empty__sub">活动可能已被删除，或暂时无法连接服务器</div>
      <AButton class="empty-action" type="default" @click="goBack">返回跑步记录</AButton>
    </div>

    <template v-else-if="detail">
    <button class="back-link" type="button" @click="goBack">‹ 返回跑步记录</button>

    <!-- 页头 -->
    <header class="page-head detail-head">
      <h1 class="display">{{ detail.activity.activityName || '跑步详情' }}</h1>
      <div class="meta-row">
        <PlatformIcon :platform="detail.activity.platform" :size="24" />
        <span class="meta-text">{{ platformLabel(detail.activity.platform) }}</span>
        <span class="chip chip-blue">{{ typeLabel(detail.activity.activityType) }}</span>
        <span class="meta-text">{{ formatDateTime(detail.activity.startTime) }}</span>
      </div>
    </header>

    <!-- 核心指标 -->
    <section class="section">
      <div class="card-grid-4">
        <div
          v-for="m in metrics"
          :key="m.label"
          class="a-card metric-card"
          :class="{ 'is-accent': m.accent }"
        >
          <div class="stat-value">{{ m.value }}</div>
          <div class="stat-label">{{ m.label }}</div>
        </div>
      </div>
    </section>

    <!-- 训练效果 / VO2max -->
    <section v-if="scoreChips.length" class="section">
      <div class="a-card score-card">
        <div class="score-head">
          <span class="score-title">训练效果</span>
          <span class="caption">效果评分为 0–5 分，分数越高代表训练刺激越大</span>
        </div>
        <div class="score-row">
          <span
            v-for="c in scoreChips"
            :key="c.label"
            class="chip score-chip"
            :class="c.cls"
          >{{ c.label }} {{ c.value }}</span>
        </div>
      </div>
    </section>

    <!-- 分段详情 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">分段详情</h2>
          <p class="section-desc">按公里或自定义分段统计</p>
        </div>
        <span v-if="detail.laps.length" class="caption">共 {{ detail.laps.length }} 段</span>
      </div>

      <div v-if="detail.laps.length" class="a-table-wrap">
        <table class="a-table">
          <thead>
            <tr>
              <th>分段</th>
              <th>距离</th>
              <th>用时</th>
              <th>平均配速</th>
              <th>平均心率</th>
              <th>累计爬升</th>
              <th>最快</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="lap in detail.laps" :key="lap.id">
              <td>{{ tableLapLabel(lap) }}</td>
              <td>{{ formatDistance(lap.splitDistanceM) }} km</td>
              <td>{{ formatDuration(lap.splitDurationSec) }}</td>
              <td>{{ formatPace(lap.avgPaceSecKm) }} /km</td>
              <td>{{ heartText(lap.avgHeartRate) }}</td>
              <td>{{ elevText(lap.elevationGainM) }}</td>
              <td>
                <span v-if="lap.id === fastestLapId" class="chip chip-green">最快</span>
                <span v-else class="caption">--</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="a-card">
        <div class="a-empty">
          <div class="a-empty__icon" aria-hidden="true">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19V5" />
              <path d="M4 19h16" />
              <path d="M8 15l3.5-5.5L15 13l4-6" />
            </svg>
          </div>
          <div class="a-empty__text">暂无分段数据</div>
          <div class="a-empty__sub">尝试在连接页重新同步该活动</div>
        </div>
      </div>
    </section>

    <!-- 分段配速 / 心率曲线 -->
    <section v-if="detail.laps.length > 0" class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">配速与心率</h2>
          <p class="section-desc">每个分段的平均配速与平均心率变化趋势</p>
        </div>
        <span class="caption">纵轴配速已倒置，越靠上代表越快</span>
      </div>
      <div class="a-card">
        <div ref="chartRef" class="chart-box"></div>
      </div>
    </section>

    <!-- 备注 -->
    <section v-if="detail.activity.remark" class="section">
      <div class="section-head">
        <h2 class="section-title">备注</h2>
      </div>
      <div class="a-card">
        <p class="remark-text">{{ detail.activity.remark }}</p>
      </div>
    </section>
    </template>

    <!-- 最终兜底：loading / loadError / detail 全为空时避免整页空白 -->
    <div v-else class="a-empty">
      <div class="a-empty__icon" aria-hidden="true">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.5v5.5" />
          <path d="M12 16.4v.2" />
        </svg>
      </div>
      <div class="a-empty__text">加载失败</div>
      <div class="a-empty__sub">未能获取这次跑步的数据，请稍后重试</div>
      <AButton class="empty-action" type="primary" @click="loadDetail">重新加载</AButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chartInit } from '@/utils/chart'
import type { ECharts, EChartsOption } from 'echarts'
import { runningApi } from '@/api/running'
import { formatPace, formatDistance, formatDuration, formatDateTime } from '@/utils/format'
import { toast } from '@/utils/toast'
import PlatformIcon from '@/components/icons/PlatformIcon.vue'
import AButton from '@/components/ui/AButton.vue'
import type { RunningActivity, RunningLap } from '@/types/running'

/* ---------------- 局部类型（后端实体中存在、共享 types 未声明的可选字段） ---------------- */

interface DetailActivity extends RunningActivity {
  trainingEffectAerobic?: number | null
  trainingEffectAnaerobic?: number | null
  vo2max?: number | null
}

interface DetailLap extends RunningLap {
  elevationGainM?: number | null
}

interface DetailVO {
  activity: DetailActivity
  laps: DetailLap[]
  distanceText: string
  durationText: string
  paceText: string
  caloriesText: string
}

interface MetricItem {
  label: string
  value: string
  accent?: boolean
}

interface ScoreChip {
  label: string
  value: string
  cls: string
}

/* ---------------- 基础状态 ---------------- */

const route = useRoute()
const router = useRouter()

const detail = ref<DetailVO | null>(null)
const loading = ref(true)
const loadError = ref(false)
const chartRef = ref<HTMLElement | null>(null)

let chart: ECharts | null = null
let themeObserver: MutationObserver | null = null

/* ---------------- 文案映射 ---------------- */

const PLATFORM_LABELS: Record<string, string> = {
  MANUAL: '手动录入',
  STRAVA: 'Strava',
  GARMIN: 'Garmin',
  COROS: 'COROS',
  APPLE: 'Apple',
  HUAWEI: '华为运动健康',
  KEEP: 'Keep',
  NIKE: 'Nike Run Club',
  ZEPP: 'Zepp',
}

const TYPE_LABELS: Record<string, string> = {
  RUN: '跑步',
  RUNNING: '跑步',
  OUTDOOR_RUN: '户外跑',
  ROAD_RUN: '路跑',
  TREADMILL: '跑步机',
  TREADMILL_RUN: '跑步机跑',
  TRACK_RUN: '场地跑',
  TRAIL_RUN: '越野跑',
  WALK: '步行',
  HIKE: '徒步',
  CYCLING: '骑行',
}

function platformLabel(platform: string): string {
  return PLATFORM_LABELS[(platform || '').toUpperCase()] || platform || '--'
}

function typeLabel(type: string): string {
  return TYPE_LABELS[(type || '').toUpperCase()] || type || '跑步'
}

/* ---------------- 核心指标 ---------------- */

const metrics = computed<MetricItem[]>(() => {
  const vo = detail.value
  if (!vo) return []
  const a = vo.activity
  const list: MetricItem[] = [
    { label: '距离', value: `${formatDistance(a.distanceM)} km`, accent: true },
    { label: '时长', value: formatDuration(a.durationSeconds) },
    { label: '平均配速', value: `${formatPace(a.avgPaceSecKm)} /km` },
  ]
  if (a.avgHeartRate != null) list.push({ label: '平均心率', value: `${a.avgHeartRate} bpm` })
  if (a.maxHeartRate != null) list.push({ label: '最大心率', value: `${a.maxHeartRate} bpm` })
  if (a.avgCadence != null) list.push({ label: '平均步频', value: `${a.avgCadence} spm` })
  if (a.elevationGainM != null) {
    list.push({ label: '累计爬升', value: `${Math.round(a.elevationGainM)} m` })
  }
  if (a.calories != null) list.push({ label: '卡路里', value: `${a.calories} kcal` })
  return list
})

/* ---------------- 训练效果 / VO2max ---------------- */

function scoreClass(v: number): string {
  if (v < 2) return 'chip-gray'
  if (v < 3) return 'chip-blue'
  if (v < 4) return 'chip-orange'
  return 'chip-red'
}

const scoreChips = computed<ScoreChip[]>(() => {
  const a = detail.value?.activity
  if (!a) return []
  const out: ScoreChip[] = []
  if (a.trainingEffectAerobic != null) {
    out.push({ label: '有氧效果', value: a.trainingEffectAerobic.toFixed(1), cls: scoreClass(a.trainingEffectAerobic) })
  }
  if (a.trainingEffectAnaerobic != null) {
    out.push({ label: '无氧效果', value: a.trainingEffectAnaerobic.toFixed(1), cls: scoreClass(a.trainingEffectAnaerobic) })
  }
  if (a.vo2max != null) {
    out.push({ label: 'VO2max', value: a.vo2max.toFixed(1), cls: 'chip-green' })
  }
  return out
})

/* ---------------- 分段 ---------------- */

function tableLapLabel(lap: DetailLap): string {
  return lap.splitDistanceM === 1000 ? `第 ${lap.lapIndex} km` : `分段 ${lap.lapIndex}`
}

function axisLapLabel(lap: DetailLap): string {
  return lap.splitDistanceM === 1000 ? `${lap.lapIndex} km` : `段 ${lap.lapIndex}`
}

function heartText(v: number | null | undefined): string {
  return v != null && v > 0 ? `${v} bpm` : '--'
}

function elevText(v: number | null | undefined): string {
  return v != null ? `${Math.round(v)} m` : '--'
}

const fastestLapId = computed<number | null>(() => {
  const laps = detail.value?.laps ?? []
  const valid = laps.filter((l) => l.avgPaceSecKm != null && l.avgPaceSecKm > 0)
  if (valid.length < 2) return null
  let best = valid[0]
  for (const l of valid) {
    if (l.avgPaceSecKm < best.avgPaceSecKm) best = l
  }
  return best.id
})

/* ---------------- 图表 ---------------- */

function buildChartOption(vo: DetailVO): EChartsOption {
  const dark = document.documentElement.dataset.theme === 'dark'
  const axisText = '#86868b'
  const splitLine = dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)'
  const primary = dark ? '#ff3b30' : '#e30613'
  const primaryFill = dark ? 'rgba(255,59,48,0.16)' : 'rgba(227,6,19,0.10)'
  const hrFill = dark ? 'rgba(255,69,58,0.55)' : 'rgba(255,59,48,0.5)'
  const legendText = dark ? '#a1a1a6' : '#6e6e73'
  const tipBg = dark ? '#1d1d1f' : '#ffffff'
  const tipBorder = dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.10)'
  const tipText = dark ? '#f5f5f7' : '#1d1d1f'

  const laps = vo.laps
  const labels = laps.map((l) => axisLapLabel(l))
  const paces = laps.map((l) => l.avgPaceSecKm || 0)
  const hrs = laps.map((l) => l.avgHeartRate || 0)

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: tipBg,
      borderColor: tipBorder,
      borderWidth: 1,
      textStyle: { color: tipText, fontSize: 12 },
      formatter: (params: unknown): string => {
        const rows = Array.isArray(params)
          ? (params as Array<{ axisValue?: string; seriesName?: string; value?: number | string }>)
          : []
        const pace = rows.find((r) => r.seriesName === '配速')
        const hr = rows.find((r) => r.seriesName === '心率')
        const paceText = pace && pace.value != null ? `${formatPace(Number(pace.value))} /km` : '--'
        const hrText = hr && hr.value ? `${hr.value} bpm` : '--'
        const title = rows[0]?.axisValue ?? ''
        return `${title}<br/>配速：${paceText}<br/>心率：${hrText}`
      },
    },
    legend: { data: ['配速', '心率'], bottom: 0, textStyle: { color: legendText } },
    grid: { top: 20, right: 60, bottom: 46, left: 60 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: splitLine } },
      axisLabel: { color: axisText },
    },
    yAxis: [
      {
        type: 'value',
        name: '配速 (s/km)',
        inverse: true,
        axisLabel: { formatter: (v: string | number) => formatPace(Number(v)), color: axisText },
        splitLine: { lineStyle: { color: splitLine } },
        nameTextStyle: { color: axisText },
      },
      {
        type: 'value',
        name: '心率 (bpm)',
        position: 'right',
        axisLabel: { color: axisText },
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
        areaStyle: { color: primaryFill },
      },
      {
        name: '心率',
        type: 'bar',
        yAxisIndex: 1,
        data: hrs,
        barWidth: '30%',
        itemStyle: { color: hrFill, borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
}

function renderChart(): void {
  const vo = detail.value
  const el = chartRef.value
  if (!vo || !el || vo.laps.length === 0) return
  try {
    if (!chart) chart = chartInit(el)
    chart.setOption(buildChartOption(vo), true)
  } catch (e: unknown) {
    // 图表渲染失败不影响页面主体，仅记录日志
    console.error('渲染分段图表失败', e)
  }
}

function disposeChart(): void {
  if (chart) {
    chart.dispose()
    chart = null
  }
}

function handleResize(): void {
  if (chart) chart.resize()
}

/* ---------------- 加载 ---------------- */

async function loadDetail(): Promise<void> {
  disposeChart()

  const id = Number(route.params.id)
  loading.value = true
  loadError.value = false

  if (!Number.isFinite(id) || id <= 0) {
    detail.value = null
    loadError.value = true
    loading.value = false
    return
  }

  try {
    const vo = await runningApi.getActivityDetail(id)
    // 接口成功但 data 为空（code 200 + null）也按失败处理，避免三态全空导致白屏
    if (!vo || !vo.activity) {
      detail.value = null
      loadError.value = true
      return
    }
    detail.value = vo
  } catch (e: unknown) {
    console.error('加载活动详情失败', e)
    detail.value = null
    loadError.value = true
    toast.error('活动加载失败')
  } finally {
    loading.value = false
  }

  if (detail.value) {
    await nextTick()
    renderChart()
  }
}

function goBack(): void {
  router.push('/activities')
}

onMounted(() => {
  void loadDetail()

  themeObserver = new MutationObserver(() => {
    if (detail.value && chartRef.value) renderChart()
  })
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  themeObserver = null
  window.removeEventListener('resize', handleResize)
  disposeChart()
})

watch(
  () => route.params.id,
  (newId, oldId) => {
    // 离开详情页（id 变 undefined）或组件卸载后不再触发加载：
    // 否则 loading 状态会在离场过渡期间把根节点内容清空、甚至中断过渡导致整站白屏
    if (newId == null || newId === '' || newId === oldId) return
    void loadDetail()
  }
)
</script>

<style scoped lang="scss">
.page-center {
  min-height: 46vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-action {
  margin-top: 22px;
}

.detail-head {
  padding-top: 20px;
}

/* 页头 meta */
.meta-row {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-text {
  font-size: 15px;
  color: var(--a-text-secondary);
}

/* 指标卡 */
.metric-card {
  padding: 24px;
}

.metric-card .stat-value {
  font-size: clamp(26px, 2.6vw, 34px);
}

.metric-card.is-accent .stat-value {
  color: var(--a-blue);
}

/* 训练效果 */
.score-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.score-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}

.score-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.score-chip {
  font-size: 15px;
  font-weight: 600;
  padding: 9px 18px;
  font-variant-numeric: tabular-nums;
}

/* 图表 */
.chart-box {
  width: 100%;
  height: 320px;
}

/* 备注 */
.remark-text {
  margin: 0;
  color: var(--a-text-secondary);
  line-height: 1.75;
}
</style>
