<template>
  <!--
    Apple 风格日期控件：触发器 + 自绘日历弹层（不再依赖原生 date input）。
    - 触发器：与 .a-input 同款式样，显示「2026年9月23日」或占位文案 + 日历图标
    - 弹层：Teleport 到 body、fixed 定位、z-index 高于弹窗遮罩(120)，
      头部月份切换 + 今天按钮，周一开头的 7 列日期网格，蓝色圆形选中态
    - 支持 min/max 限制、点击外部/Esc 关闭
  -->
  <button
    :id="id"
    ref="triggerEl"
    type="button"
    class="a-date-trigger"
    :class="{ 'is-placeholder': !modelValue, 'is-open': open }"
    @click="toggle"
  >
    <span class="a-date-trigger__text">{{ display }}</span>
    <svg class="a-date-trigger__icon" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
      <rect x="3.2" y="5" width="17.6" height="16" rx="3.4" />
      <path d="M3.2 9.6h17.6M8 3v3.4M16 3v3.4" />
      <path d="M7.6 13.4h.01M12 13.4h.01M16.4 13.4h.01M7.6 17h.01M12 17h.01" stroke-width="2.2" />
    </svg>
  </button>

  <Teleport to="body">
    <div
      v-if="open"
      ref="popEl"
      class="a-date-pop"
      :style="popStyle"
      role="dialog"
      aria-label="选择日期"
      @click.stop
    >
      <div class="a-date-pop__head">
        <div class="a-date-pop__nav">
          <button type="button" class="a-date-pop__navbtn" aria-label="上个月" @click="shiftMonth(-1)">
            <svg viewBox="0 0 20 20" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.5 4L6.5 10l6 6" /></svg>
          </button>
          <button type="button" class="a-date-pop__navbtn" aria-label="下个月" @click="shiftMonth(1)">
            <svg viewBox="0 0 20 20" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.5 4l6 6-6 6" /></svg>
          </button>
        </div>
        <div class="a-date-pop__title">{{ viewYear }}年{{ viewMonth }}月</div>
        <button type="button" class="a-date-pop__today" @click="pickToday">今天</button>
      </div>

      <div class="a-date-pop__weekdays">
        <span v-for="w in WEEKDAYS" :key="w">{{ w }}</span>
      </div>

      <div class="a-date-pop__grid">
        <button
          v-for="(cell, i) in monthCells"
          :key="i"
          type="button"
          class="a-date-pop__day"
          :class="{
            'is-out': !cell.inMonth,
            'is-selected': cell.value === modelValue,
            'is-today': cell.value === todayStr,
            'is-disabled': cell.disabled,
          }"
          :disabled="cell.disabled"
          @click="choose(cell)"
        >
          {{ cell.day }}
        </button>
      </div>

      <div class="a-date-pop__foot">
        <button type="button" class="a-date-pop__clear" :disabled="!modelValue" @click="clear">
          清除
        </button>
        <button type="button" class="a-date-pop__confirm" @click="open = false">完成</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    id?: string
    modelValue?: string | null
    placeholder?: string
    min?: string
    max?: string
  }>(),
  {
    id: undefined,
    modelValue: '',
    placeholder: '选择日期',
    min: undefined,
    max: undefined,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  enter: []
}>()

/** 周一开头（与国内日历习惯一致） */
const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日']

const open = ref(false)
const triggerEl = ref<HTMLButtonElement | null>(null)
const popEl = ref<HTMLElement | null>(null)
const popStyle = ref<Record<string, string>>({})

const todayStr = formatDateISO(new Date())

/* ---------- 视图月份 ---------- */
const viewYear = ref(0)
const viewMonth = ref(0) // 1-12

function initView(): void {
  const base = props.modelValue ? parseDate(props.modelValue) : new Date()
  viewYear.value = base.getFullYear()
  viewMonth.value = base.getMonth() + 1
}

watch(
  () => props.modelValue,
  (v) => {
    if (v && open.value) {
      const d = parseDate(v)
      viewYear.value = d.getFullYear()
      viewMonth.value = d.getMonth() + 1
    }
  }
)

const display = computed(() =>
  props.modelValue ? formatReadable(props.modelValue) : props.placeholder
)

/* ---------- 日历网格 ---------- */
interface MonthCell {
  day: number
  value: string
  inMonth: boolean
  disabled: boolean
}

const monthCells = computed<MonthCell[]>(() => {
  const y = viewYear.value
  const m = viewMonth.value
  const first = new Date(y, m - 1, 1)
  // 周一开头的偏移：周日(0)视为 7
  const lead = ((first.getDay() + 6) % 7)
  const daysInMonth = new Date(y, m, 0).getDate()

  const cells: MonthCell[] = []
  // 前导（上月）
  for (let i = lead; i > 0; i--) {
    const d = new Date(y, m - 1, 1 - i)
    cells.push(buildCell(d, false))
  }
  // 本月
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push(buildCell(new Date(y, m - 1, d), true))
  }
  // 补尾至整周（下月）
  const tail = (7 - (cells.length % 7)) % 7
  for (let i = 1; i <= tail; i++) {
    cells.push(buildCell(new Date(y, m - 1, daysInMonth + i), false))
  }
  return cells
})

function buildCell(d: Date, inMonth: boolean): MonthCell {
  const value = formatDateISO(d)
  const disabled =
    (props.min !== undefined && props.min !== '' && value < props.min) ||
    (props.max !== undefined && props.max !== '' && value > props.max)
  return { day: d.getDate(), value, inMonth, disabled }
}

function choose(cell: MonthCell): void {
  if (cell.disabled) return
  emit('update:modelValue', cell.value)
  open.value = false
}

function pickToday(): void {
  if (buildCell(new Date(), true).disabled) return
  emit('update:modelValue', todayStr)
  open.value = false
}

function clear(): void {
  emit('update:modelValue', '')
  open.value = false
}

function shiftMonth(delta: number): void {
  const m = viewMonth.value + delta
  if (m < 1) {
    viewMonth.value = 12
    viewYear.value -= 1
  } else if (m > 12) {
    viewMonth.value = 1
    viewYear.value += 1
  } else {
    viewMonth.value = m
  }
}

/* ---------- 开关与定位 ---------- */
function toggle(): void {
  open.value ? close() : openCalendar()
}

function openCalendar(): void {
  initView()
  open.value = true
  // 等 DOM 提交后再逐帧测量，避免首开时布局未就绪导致定位落空
  void nextTick(() => schedulePlace())
}

function close(): void {
  open.value = false
}

let popObserver: ResizeObserver | null = null

/**
 * 弹层定位（修复：首开定位落空/错位、弹窗内被视口裁剪）：
 * - 贴触发器下方；下方放不下且上方放得下 → 翻转到上方
 * - 四边统一钳入视口（12px 边距），保证完整可见
 * - 高度超过可用视口时弹层内部滚动，「完成」按钮永远可点
 * - 宽度贴触发器，但至少 316px、至多视口可用宽
 * 返回 false 表示 popEl 尚未挂载/可测量，需要重试。
 */
function place(): boolean {
  const t = triggerEl.value
  const p = popEl.value
  if (!t || !p) return false
  const r = t.getBoundingClientRect()
  const vw = window.innerWidth
  const vh = window.innerHeight
  const gap = 8
  const margin = 12
  const width = Math.min(Math.max(r.width, 316), vw - margin * 2)

  let ph = p.offsetHeight
  const maxH = vh - margin * 2
  const style: Record<string, string> = {
    position: 'fixed',
    width: `${Math.round(width)}px`,
  }
  if (ph > maxH) {
    style.maxHeight = `${maxH}px`
    style.overflowY = 'auto'
    ph = maxH
  }

  let top = r.bottom + gap
  const above = r.top - ph - gap
  if (top + ph + margin > vh && above >= margin) {
    top = above
  }
  top = Math.min(Math.max(top, margin), vh - ph - margin)

  const left = Math.min(Math.max(r.left, margin), vw - width - margin)

  style.top = `${Math.round(top)}px`
  style.left = `${Math.round(left)}px`
  popStyle.value = style

  // 弹层尺寸变化（月份 5/6 行切换、字体加载完成）时自动重新贴合
  if (!popObserver) {
    popObserver = new ResizeObserver(() => {
      if (open.value) place()
    })
    popObserver.observe(p)
  }
  return true
}

/** 打开后的定位调度：Teleport 内容首帧可能尚未就绪（popEl 为空），用 rAF 逐帧重试直到测到为止 */
function schedulePlace(): void {
  let tries = 0
  const step = () => {
    if (!open.value) return
    if (place() || tries++ >= 10) return
    requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

function onDocClick(e: MouseEvent): void {
  if (!open.value) return
  const target = e.target as Node
  if (triggerEl.value?.contains(target)) return
  if (popEl.value?.contains(target)) return
  open.value = false
}

function onKey(e: KeyboardEvent): void {
  if (e.key === 'Escape' && open.value) open.value = false
}

function onViewportChange(): void {
  if (open.value) place()
}

// 任何关闭路径（选择/清除/完成/外部点击/Esc）都停止尺寸观察
watch(open, (v) => {
  if (!v) {
    popObserver?.disconnect()
    popObserver = null
  }
})

onBeforeUnmount(() => {
  popObserver?.disconnect()
  popObserver = null
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', onViewportChange)
  window.removeEventListener('scroll', onViewportChange, true)
})
document.addEventListener('click', onDocClick)
document.addEventListener('keydown', onKey)
window.addEventListener('resize', onViewportChange)
window.addEventListener('scroll', onViewportChange, true)

/* ---------- 日期工具（本地时区，无 UTC 偏移问题） ---------- */
function parseDate(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, (m || 1) - 1, d || 1)
}

function formatDateISO(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 2026-09-23 → 2026年9月23日（去前导零） */
function formatReadable(v: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v)
  if (!m) return v
  return `${m[1]}年${Number(m[2])}月${Number(m[3])}日`
}
</script>

<style scoped lang="scss">
/* ---- 触发器：与 .a-input 同款 ---- */
.a-date-trigger {
  width: 100%;
  min-height: 52px;
  padding: 0 16px 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-family: var(--a-font);
  font-size: 15.5px;
  font-weight: 500;
  color: var(--a-text);
  background: var(--a-input-bg);
  border: 1.5px solid var(--a-border-input);
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.a-date-trigger:hover {
  border-color: var(--a-text-tertiary);
}
.a-date-trigger.is-open,
.a-date-trigger:focus-visible {
  outline: none;
  border-color: var(--a-blue);
  box-shadow: 0 0 0 4px var(--a-blue-ring);
}
.a-date-trigger__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.a-date-trigger.is-placeholder .a-date-trigger__text {
  color: var(--a-text-tertiary);
  font-weight: 400;
}
.a-date-trigger__icon {
  flex: none;
  color: var(--a-text-tertiary);
}
.a-date-trigger:hover .a-date-trigger__icon {
  color: var(--a-blue);
}

/* ---- 日历弹层（Apple 风格） ---- */
.a-date-pop {
  z-index: 160; /* 高于 .a-modal-overlay(120) */
  background: var(--a-surface);
  border: 1px solid var(--a-divider);
  border-radius: 18px;
  padding: 14px 14px 12px;
  box-shadow: 0 18px 56px rgba(0, 0, 0, 0.22);
  animation: date-pop-in 0.18s cubic-bezier(0.2, 0.8, 0.2, 1);
  user-select: none;
}

@keyframes date-pop-in {
  from {
    opacity: 0;
    transform: translateY(-4px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.a-date-pop__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.a-date-pop__nav {
  display: flex;
  gap: 4px;
}
.a-date-pop__navbtn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--a-text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, color 0.15s ease;
}
.a-date-pop__navbtn:hover {
  background: var(--a-fill);
  color: var(--a-text);
}
.a-date-pop__title {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: -0.01em;
  color: var(--a-text);
  font-variant-numeric: tabular-nums;
}
.a-date-pop__today {
  border: none;
  background: transparent;
  padding: 5px 11px;
  border-radius: 980px;
  font-family: var(--a-font);
  font-size: 13px;
  font-weight: 550;
  color: var(--a-blue);
  cursor: pointer;
  transition: background 0.15s ease;
}
.a-date-pop__today:hover {
  background: var(--a-blue-ring);
}

.a-date-pop__weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 2px;
  span {
    text-align: center;
    font-size: 11.5px;
    font-weight: 600;
    color: var(--a-text-tertiary);
    padding: 4px 0;
  }
}

.a-date-pop__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.a-date-pop__day {
  aspect-ratio: 1;
  min-height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  font-family: var(--a-font);
  font-size: 14.5px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--a-text);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.14s ease, color 0.14s ease;
}
.a-date-pop__day:hover:not(:disabled) {
  background: var(--a-fill);
}
.a-date-pop__day.is-out {
  color: var(--a-text-tertiary);
  opacity: 0.45;
}
.a-date-pop__day.is-today:not(.is-selected) {
  color: var(--a-blue);
  font-weight: 700;
  box-shadow: inset 0 0 0 1.5px var(--a-blue);
}
.a-date-pop__day.is-selected {
  background: var(--a-blue);
  color: #fff;
  font-weight: 650;
  box-shadow: none;
}
.a-date-pop__day:disabled {
  color: var(--a-text-tertiary);
  opacity: 0.3;
  cursor: not-allowed;
}

.a-date-pop__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--a-divider);
}
.a-date-pop__clear {
  border: none;
  background: transparent;
  padding: 6px 12px;
  border-radius: 980px;
  font-family: var(--a-font);
  font-size: 13px;
  font-weight: 500;
  color: var(--a-text-secondary);
  cursor: pointer;
  transition: background 0.15s ease;
}
.a-date-pop__clear:hover:not(:disabled) {
  background: var(--a-fill);
}
.a-date-pop__clear:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.a-date-pop__confirm {
  border: none;
  padding: 7px 18px;
  border-radius: 980px;
  background: var(--a-blue);
  color: #fff;
  font-family: var(--a-font);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}
.a-date-pop__confirm:hover {
  background: var(--a-blue-hover);
}

@media (prefers-reduced-motion: reduce) {
  .a-date-pop {
    animation: none;
  }
}
</style>
