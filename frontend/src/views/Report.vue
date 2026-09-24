<template>
  <div class="page report-page">
    <!-- 页头 -->
    <header class="page-head">
      <p class="eyebrow">训练报告</p>
      <h1 class="headline">一段周期，一份总结。</h1>
      <p class="subhead">选择时间周期，汇总跑量、配速与心率趋势，AI 教练给出周期复盘与下阶段建议。</p>
    </header>

    <!-- 周期选择 -->
    <section class="section">
      <div class="period-bar">
        <div class="segmented" role="tablist" aria-label="统计周期">
          <button
            v-for="p in PRESETS"
            :key="p.value"
            type="button"
            role="tab"
            class="segmented__item"
            :class="{ 'is-active': granularity === p.value }"
            :aria-selected="granularity === p.value"
            @click="selectPreset(p.value)"
          >
            {{ p.label }}
          </button>
        </div>

        <transition name="custom-range">
          <div v-if="granularity === 'CUSTOM'" class="custom-range">
            <div class="custom-range__field">
              <span class="custom-range__label">开始</span>
              <ADateInput v-model="customStart" placeholder="开始日期" :max="customEnd || undefined" />
            </div>
            <span class="custom-range__dash">→</span>
            <div class="custom-range__field">
              <span class="custom-range__label">结束</span>
              <ADateInput v-model="customEnd" placeholder="结束日期" :min="customStart || undefined" />
            </div>
            <AButton type="primary" :loading="generating" :disabled="!canGenerateCustom" @click="generate">
              生成报告
            </AButton>
          </div>
        </transition>
      </div>
    </section>

    <!-- 加载中 -->
    <section v-if="generating && !report" class="section loading-block">
      <span class="a-spinner"></span>
      <p class="loading-text">正在汇总数据并生成 AI 报告…</p>
    </section>

    <!-- 空态 -->
    <section v-else-if="report && report.summary.runs === 0" class="section">
      <div class="a-card">
        <div class="a-empty">
          <div class="a-empty__icon">🗓</div>
          <div class="a-empty__text">该周期内没有跑步记录</div>
          <div class="a-empty__sub">换一个时间周期，或完成一次跑步后再来生成报告。</div>
        </div>
      </div>
    </section>

    <template v-else-if="report">
      <!-- 汇总统计 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">周期汇总</h2>
            <p class="section-desc">{{ report.periodLabel }}</p>
          </div>
          <span class="chip chip-gray">共 {{ report.summary.runs }} 次跑步</span>
        </div>

        <div class="summary-grid">
          <article v-for="card in summaryCards" :key="card.label" class="a-card stat-card">
            <div class="stat-value">
              {{ card.value }}<span v-if="card.unit" class="stat-unit">{{ card.unit }}</span>
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </article>
        </div>
      </section>

      <!-- 趋势图 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">周期趋势</h2>
            <p class="section-desc">按周期分桶的跑量与平均配速，节奏变化一眼看清。</p>
          </div>
          <span class="caption">配速轴已反向：越靠上，配速越快</span>
        </div>
        <div class="a-card">
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </section>

      <!-- AI 报告 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">AI 教练报告</h2>
            <p class="section-desc">基于本周期全部训练数据生成的复盘与建议。</p>
            <p v-if="!report.aiAvailable" class="section-desc ai-unavailable">
              未检测到可用的 AI 密钥，当前显示规则摘要 —— 配置 DeepSeek Key 后这里会自动切换为 AI 生成。
            </p>
          </div>
          <div class="ai-actions">
            <span class="chip" :class="report.aiGenerated ? 'chip-blue' : 'chip-gray'">
              {{ report.aiGenerated ? 'AI 生成' : '规则摘要' }}
            </span>
            <AButton size="sm" :loading="regenerating" @click="regenerate">
              {{ report.aiGenerated ? '重新生成' : '用 AI 重新生成' }}
            </AButton>
          </div>
        </div>

        <div class="a-card ai-report-card">
          <div class="ai-report-card__head">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3z" />
              <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z" />
            </svg>
            <span>教练复盘</span>
          </div>
          <p class="ai-report-card__text">{{ report.aiReport }}</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { chartInit } from '@/utils/chart'
import type { ECharts, EChartsOption, SeriesOption } from 'echarts'
import AButton from '@/components/ui/AButton.vue'
import ADateInput from '@/components/ui/ADateInput.vue'
import { reportApi } from '@/api/report'
import { toast } from '@/utils/toast'
import { formatPace } from '@/utils/format'
import type { ReportGranularity, TrainingReport } from '@/types/report'

const PRESETS: Array<{ value: ReportGranularity; label: string }> = [
  { value: 'DAY', label: '天' },
  { value: 'WEEK', label: '周' },
  { value: 'MONTH', label: '月' },
  { value: 'QUARTER', label: '季度' },
  { value: 'YEAR', label: '年' },
  { value: 'CUSTOM', label: '自定义' },
]

const granularity = ref<ReportGranularity>('WEEK')
const customStart = ref('')
const customEnd = ref('')
const report = ref<TrainingReport | null>(null)
const generating = ref(false)
const regenerating = ref(false)

const canGenerateCustom = computed(
  () => Boolean(customStart.value && customEnd.value && customStart.value <= customEnd.value)
)

function selectPreset(g: ReportGranularity): void {
  if (granularity.value === g) return
  granularity.value = g
  if (g === 'CUSTOM') {
    // 自定义模式下等用户选好日期再生成
    return
  }
  void generate()
}

async function generate(): Promise<void> {
  if (generating.value) return
  if (granularity.value === 'CUSTOM' && !canGenerateCustom.value) return
  generating.value = true
  try {
    report.value = await reportApi.generateReport({
      granularity: granularity.value,
      startDate: granularity.value === 'CUSTOM' ? customStart.value : undefined,
      endDate: granularity.value === 'CUSTOM' ? customEnd.value : undefined,
    })
    await nextTick()
    renderTrendChart()
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    generating.value = false
  }
}

/** 仅重新生成 AI 文字部分（后端全量重算，前端保持滚动位置） */
async function regenerate(): Promise<void> {
  if (regenerating.value) return
  regenerating.value = true
  try {
    const keep = report.value
    const data = await reportApi.generateReport({
      granularity: granularity.value,
      startDate: granularity.value === 'CUSTOM' ? customStart.value : undefined,
      endDate: granularity.value === 'CUSTOM' ? customEnd.value : undefined,
    })
    if (keep) report.value = data
    if (!data.aiGenerated) {
      toast.info(
        data.aiAvailable
          ? 'AI 本次生成失败，已展示规则摘要'
          : '未配置 AI 密钥，已展示规则摘要（配置后可生成 AI 报告）'
      )
    }
    await nextTick()
    renderTrendChart()
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    regenerating.value = false
  }
}

/* ---------- 汇总卡片 ---------- */
const summaryCards = computed(() => {
  const s = report.value?.summary
  if (!s) return []
  const hours = Math.floor(s.durationMin / 60)
  const mins = s.durationMin % 60
  return [
    { label: '累计跑量', value: s.distanceKm.toFixed(1), unit: 'km' },
    { label: '总时长', value: hours > 0 ? `${hours}:${String(mins).padStart(2, '0')}` : String(mins), unit: hours > 0 ? 'h' : 'min' },
    { label: '平均配速', value: formatPace(s.avgPaceSecKm), unit: s.avgPaceSecKm ? '/km' : '' },
    { label: '平均心率', value: s.avgHeartRate ?? '--', unit: s.avgHeartRate ? 'bpm' : '' },
    { label: '消耗热量', value: s.calories ?? '--', unit: s.calories ? 'kcal' : '' },
    { label: '累计爬升', value: Math.round(s.elevationGainM), unit: 'm' },
  ]
})

/* ---------- 趋势图 ---------- */
const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null
let themeObserver: MutationObserver | null = null

function isDark(): boolean {
  return document.documentElement.dataset.theme === 'dark'
}

function buildTrendOption(): EChartsOption | null {
  const r = report.value
  if (!r || r.buckets.length === 0) return null

  const dark = isDark()
  const axisText = '#86868b'
  const splitLine = dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)'

  const labels = r.buckets.map((b) => b.label)
  const distances = r.buckets.map((b) => +b.distanceKm.toFixed(2))
  const paces = r.buckets.map((b) => b.avgPaceSecKm ?? null)
  const hasPace = paces.some((p) => p !== null)

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: dark ? '#2c2c2e' : '#ffffff',
      borderColor: splitLine,
      borderWidth: 1,
      textStyle: { color: dark ? '#f5f5f7' : '#1d1d1f', fontSize: 12 },
      formatter(params: unknown) {
        const list = params as Array<{ axisValue?: string; value?: number | null; seriesName?: string }>
        const bar = list.find((i) => i.seriesName === '跑量')
        const line = list.find((i) => i.seriesName === '平均配速')
        let html = `${list[0]?.axisValue ?? ''}<br/>跑量：${bar?.value ?? 0} km`
        if (line && line.value != null) {
          html += `<br/>平均配速：${formatPace(line.value)} /km`
        }
        return html
      },
    },
    legend: { data: hasPace ? ['跑量', '平均配速'] : ['跑量'], bottom: 0, textStyle: { color: axisText } },
    grid: { top: 24, right: hasPace ? 56 : 24, bottom: 44, left: 64 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: splitLine } },
      axisTick: { show: false },
      axisLabel: { color: axisText },
    },
    yAxis: hasPace
      ? [
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
        ]
      : [
          {
            type: 'value',
            name: '距离 (km)',
            axisLabel: { color: axisText },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: splitLine } },
            nameTextStyle: { color: axisText },
          },
        ],
    series: buildSeries(hasPace, distances, paces),
  }
}

function buildSeries(
  hasPace: boolean,
  distances: number[],
  paces: (number | null)[]
): SeriesOption[] {
  const dark = isDark()
  const primary = dark ? '#ff3b30' : '#e30613'
  const primarySoft = dark ? 'rgba(255,59,48,0.16)' : 'rgba(227,6,19,0.08)'
  const barColor = dark ? 'rgba(255,255,255,0.26)' : 'rgba(0,0,0,0.3)'

  const series: SeriesOption[] = [
    {
      name: '跑量',
      type: 'bar',
      yAxisIndex: hasPace ? 1 : 0,
      data: distances,
      barWidth: '46%',
      itemStyle: { color: barColor, borderRadius: [4, 4, 0, 0] },
    },
  ]
  if (hasPace) {
    series.push({
      name: '平均配速',
      type: 'line',
      yAxisIndex: 0,
      data: paces,
      connectNulls: true,
      smooth: true,
      lineStyle: { color: primary, width: 2.5 },
      itemStyle: { color: primary },
      areaStyle: { color: primarySoft },
    })
  }
  return series
}

function renderTrendChart(): void {
  if (!trendChartRef.value) return
  const option = buildTrendOption()
  if (!option) return

  if (trendChart && trendChart.getDom() !== trendChartRef.value) {
    trendChart.dispose()
    trendChart = null
  }
  if (!trendChart) {
    trendChart = chartInit(trendChartRef.value)
  }
  trendChart.setOption(option, true)
}

function handleResize(): void {
  trendChart?.resize()
}

onMounted(() => {
  themeObserver = new MutationObserver(() => renderTrendChart())
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  window.addEventListener('resize', handleResize)
  void generate()
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  themeObserver = null
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped lang="scss">
/* ---- 周期选择条 ---- */
.period-bar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.segmented {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  padding: 5px;
  background: var(--a-fill);
  border-radius: 980px;
}
.segmented__item {
  border: none;
  background: transparent;
  padding: 9px 22px;
  border-radius: 980px;
  font-family: var(--a-font);
  font-size: 14.5px;
  font-weight: 550;
  color: var(--a-text-secondary);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}
.segmented__item:hover {
  color: var(--a-text);
}
.segmented__item.is-active {
  background: var(--a-surface);
  color: var(--a-text);
  font-weight: 650;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* ---- 自定义区间 ---- */
.custom-range {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}
.custom-range__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 190px;
  flex: 0 1 220px;
}
.custom-range__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--a-text-secondary);
}
.custom-range__dash {
  padding-bottom: 15px;
  color: var(--a-text-tertiary);
}

.custom-range-enter-active,
.custom-range-leave-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}
.custom-range-enter-from,
.custom-range-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ---- 汇总卡：控制字号防溢出（单位为小号，数值不换行） ---- */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 26px 24px;
  min-width: 0;
  overflow: hidden;
}
.summary-grid .stat-value {
  font-size: clamp(26px, 2.4vw, 34px);
  white-space: nowrap;
}
.stat-unit {
  font-size: 0.46em;
  font-weight: 600;
  color: var(--a-text-tertiary);
  margin-left: 4px;
  letter-spacing: 0;
}
.summary-grid .stat-label {
  margin-top: 6px;
  font-size: 13.5px;
  color: var(--a-text-secondary);
}

/* ---- AI 报告 ---- */
.ai-unavailable {
  margin-top: 6px;
  font-size: 13px;
  color: var(--a-text-secondary);
}

.ai-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-report-card {
  border-left: 3px solid var(--a-blue);
}
.ai-report-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--a-blue);
  font-size: 14.5px;
  font-weight: 650;
  margin-bottom: 12px;
}
.ai-report-card__text {
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: var(--a-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---- 通用 ---- */
.chart-box {
  width: 100%;
  height: 340px;
}
.loading-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 64px 0;
}
.loading-text {
  margin: 0;
  font-size: 14.5px;
  color: var(--a-text-tertiary);
}

@media (max-width: 640px) {
  .segmented__item {
    padding: 8px 15px;
  }
  .chart-box {
    height: 280px;
  }
}
</style>
