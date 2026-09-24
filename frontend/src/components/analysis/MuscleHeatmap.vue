<template>
  <section class="section">
    <div class="section-head">
      <div>
        <h2 class="section-title">肌肉热力图</h2>
        <p class="section-desc">
          基于近 {{ days }} 天的运动类型、时长与强度估算肌群负荷（参考高驰 App 肌肉负荷口径），
          蓝黄红色阶从冷到热表示刺激由轻到重，点击肌群可查看明细。
        </p>
      </div>
      <div class="mm-seg">
        <button
          v-for="d in periodOptions"
          :key="d"
          type="button"
          class="mm-seg__btn"
          :class="{ on: days === d }"
          @click="switchDays(d)"
        >
          {{ d }} 天
        </button>
      </div>
    </div>

    <div v-if="loading" class="mm-loading">
      <div class="mm-load-fig"><ASkeleton :lines="1" :height="240" :radius="18" /></div>
      <div class="mm-load-side"><ASkeleton :lines="6" :height="14" /></div>
    </div>
    <div v-else-if="error" class="a-empty">
      <div class="a-empty__text">{{ error }}</div>
    </div>

    <template v-else-if="map">
      <div class="mm-grid">
        <!-- 人体视图：正面 / 背面（肌群为可交互解剖路径） -->
        <div class="mm-figures" ref="figRef">
          <figure class="mm-fig">
            <svg viewBox="0 0 180 372" class="mm-svg" role="img" aria-label="正面肌肉热力图">
              <!-- 底层轮廓 -->
              <path class="mm-limb mm-arm" d="M58,66 L45,132 L40,190" />
              <path class="mm-limb mm-arm" d="M122,66 L135,132 L140,190" />
              <path class="mm-limb mm-leg" d="M75,190 L71,270 L69,346" />
              <path class="mm-limb mm-leg" d="M105,190 L109,270 L111,346" />
              <path
                class="mm-base"
                d="M56,64 Q90,51 124,64 L127,112 Q124,152 119,192 L61,192 Q56,152 53,112 Z"
              />
              <rect class="mm-base" x="82" y="44" width="16" height="15" rx="6" />
              <ellipse class="mm-base" cx="90" cy="26" rx="18" ry="22" />

              <!-- 三角肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'shoulders' }"
                @mousemove="onHover($event, 'shoulders')"
                @mouseleave="hideHover"
                @click="select('shoulders')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('shoulders')"
                  d="M47,57 Q55,46 66,52 Q72,60 67,71 Q57,78 49,71 Q44,63 47,57 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('shoulders')"
                  d="M133,57 Q125,46 114,52 Q108,60 113,71 Q123,78 131,71 Q136,63 133,57 Z"
                />
              </g>

              <!-- 胸大肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'chest' }"
                @mousemove="onHover($event, 'chest')"
                @mouseleave="hideHover"
                @click="select('chest')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('chest')"
                  d="M60,77 Q74,69 88,77 Q89,92 81,100 Q66,99 60,89 Q58,81 60,77 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('chest')"
                  d="M120,77 Q106,69 92,77 Q91,92 99,100 Q114,99 120,89 Q122,81 120,77 Z"
                />
              </g>

              <!-- 肱二头肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'biceps' }"
                @mousemove="onHover($event, 'biceps')"
                @mouseleave="hideHover"
                @click="select('biceps')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('biceps')"
                  d="M55,79 Q47,88 45,103 Q47,114 53,113 Q57,99 58,86 Q57,79 55,79 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('biceps')"
                  d="M125,79 Q133,88 135,103 Q133,114 127,113 Q123,99 122,86 Q123,79 125,79 Z"
                />
              </g>

              <!-- 腹部核心（含肌腱线） -->
              <g
                class="mm-g"
                :class="{ on: selected === 'core' }"
                @mousemove="onHover($event, 'core')"
                @mouseleave="hideHover"
                @click="select('core')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('core')"
                  d="M76,104 Q90,99 104,104 Q106,124 105,146 Q102,163 90,166 Q78,163 75,146 Q74,124 76,104 Z"
                />
                <path class="mm-cut" d="M77,120 H103 M77,137 H103 M90,106 V164" />
              </g>

              <!-- 股四头肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'quads' }"
                @mousemove="onHover($event, 'quads')"
                @mouseleave="hideHover"
                @click="select('quads')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('quads')"
                  d="M71,197 Q63,224 66,258 Q69,277 78,277 Q84,270 84,244 Q83,216 80,197 Q76,191 71,197 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('quads')"
                  d="M109,197 Q117,224 114,258 Q111,277 102,277 Q96,270 96,244 Q97,216 100,197 Q104,191 109,197 Z"
                />
              </g>

              <!-- 胫骨前肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'tibialis' }"
                @mousemove="onHover($event, 'tibialis')"
                @mouseleave="hideHover"
                @click="select('tibialis')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('tibialis')"
                  d="M71,283 Q67,304 68,328 Q70,339 75,337 Q78,330 79,308 Q78,291 76,283 Q73,280 71,283 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('tibialis')"
                  d="M109,283 Q113,304 112,328 Q110,339 105,337 Q102,330 101,308 Q102,291 104,283 Q107,280 109,283 Z"
                />
              </g>
            </svg>
            <figcaption>正面</figcaption>
          </figure>

          <figure class="mm-fig">
            <svg viewBox="0 0 180 372" class="mm-svg" role="img" aria-label="背面肌肉热力图">
              <path class="mm-limb mm-arm" d="M58,66 L45,132 L40,190" />
              <path class="mm-limb mm-arm" d="M122,66 L135,132 L140,190" />
              <path class="mm-limb mm-leg" d="M75,190 L71,270 L69,346" />
              <path class="mm-limb mm-leg" d="M105,190 L109,270 L111,346" />
              <path
                class="mm-base"
                d="M56,64 Q90,51 124,64 L127,112 Q124,152 119,192 L61,192 Q56,152 53,112 Z"
              />
              <rect class="mm-base" x="82" y="44" width="16" height="15" rx="6" />
              <ellipse class="mm-base" cx="90" cy="26" rx="18" ry="22" />

              <!-- 背阔肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'lats' }"
                @mousemove="onHover($event, 'lats')"
                @mouseleave="hideHover"
                @click="select('lats')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('lats')"
                  d="M66,84 Q54,96 52,118 Q56,142 66,153 Q77,147 78,126 Q77,104 72,88 Q69,82 66,84 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('lats')"
                  d="M114,84 Q126,96 128,118 Q124,142 114,153 Q103,147 102,126 Q103,104 108,88 Q111,82 114,84 Z"
                />
              </g>

              <!-- 臀大肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'glutes' }"
                @mousemove="onHover($event, 'glutes')"
                @mouseleave="hideHover"
                @click="select('glutes')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('glutes')"
                  d="M74,186 Q63,190 62,202 Q66,215 78,216 Q88,212 88,199 Q85,188 74,186 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('glutes')"
                  d="M106,186 Q117,190 118,202 Q114,215 102,216 Q92,212 92,199 Q95,188 106,186 Z"
                />
              </g>

              <!-- 腘绳肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'hamstrings' }"
                @mousemove="onHover($event, 'hamstrings')"
                @mouseleave="hideHover"
                @click="select('hamstrings')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('hamstrings')"
                  d="M70,221 Q64,243 66,265 Q70,277 78,275 Q84,267 84,245 Q82,225 78,217 Q72,215 70,221 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('hamstrings')"
                  d="M110,221 Q116,243 114,265 Q110,277 102,275 Q96,267 96,245 Q98,225 102,217 Q108,215 110,221 Z"
                />
              </g>

              <!-- 小腿三头肌 -->
              <g
                class="mm-g"
                :class="{ on: selected === 'calves' }"
                @mousemove="onHover($event, 'calves')"
                @mouseleave="hideHover"
                @click="select('calves')"
              >
                <path
                  class="mm-mus"
                  :fill="heatOf('calves')"
                  d="M72,286 Q66,301 68,317 Q72,331 78,329 Q83,319 82,303 Q80,289 77,284 Q73,282 72,286 Z"
                />
                <path
                  class="mm-mus"
                  :fill="heatOf('calves')"
                  d="M108,286 Q114,301 112,317 Q108,331 102,329 Q97,319 98,303 Q100,289 103,284 Q107,282 108,286 Z"
                />
              </g>
            </svg>
            <figcaption>背面</figcaption>
          </figure>

          <!-- 悬浮提示 -->
          <div
            v-if="tip"
            class="mm-tip"
            :style="{ left: tip.x + 'px', top: tip.y + 'px' }"
            aria-hidden="true"
          >
            {{ labelOf(tip.key) }}
          </div>
        </div>

        <!-- 右侧：概览 + 肌群排行 -->
        <div class="mm-side">
          <div class="mm-summary">
            <div class="mm-sum-item">
              <b>{{ map.activityCount }}</b>
              <span>次运动</span>
            </div>
            <div class="mm-sum-item">
              <b>{{ map.totalMinutes }}</b>
              <span>分钟</span>
            </div>
            <div class="mm-sum-item">
              <b>{{ topMuscle ? topMuscle.name : '—' }}</b>
              <span>主要刺激</span>
            </div>
          </div>

          <ul class="mm-list">
            <li
              v-for="m in map.muscles"
              :key="m.key"
              class="mm-item"
              :class="{ on: selected === m.key }"
              :title="m.levelText"
              @click="select(m.key)"
            >
              <span class="mm-name">{{ m.name }}</span>
              <span class="mm-bar">
                <i :style="{ width: m.score + '%', background: heat(m.score) }" />
              </span>
              <span class="mm-score">{{ m.score }}</span>
            </li>
          </ul>

          <div class="mm-legend">
            <span class="mm-lg"
              ><i style="background: rgba(150, 150, 158, 0.45)" />未激活</span
            >
            <span class="mm-lg"><i :style="{ background: heat(25) }" />轻度</span>
            <span class="mm-lg"><i :style="{ background: heat(55) }" />中等</span>
            <span class="mm-lg"><i :style="{ background: heat(85) }" />高负荷</span>
            <span class="mm-scale" :style="{ background: scaleGradient }" aria-hidden="true" />
          </div>
        </div>
      </div>

      <!-- 肌群明细面板 -->
      <div v-if="selected" class="mm-panel a-card">
        <div class="mm-panel-head">
          <div class="mm-panel-title-row">
            <h3 class="mm-panel-title">{{ detail ? detail.name : '…' }}</h3>
            <span v-if="detail" class="mm-panel-level">
              <i :style="{ background: heat(detail.score) }" />
              {{ detail.levelText }}
            </span>
          </div>
          <button type="button" class="mm-panel-close" aria-label="关闭明细" @click="closeDetail">
            ×
          </button>
        </div>

        <div v-if="detailLoading" class="a-empty">
          <span class="a-spinner"></span>
        </div>

        <template v-else-if="detail">
          <div class="mm-stats">
            <div class="mm-stat">
              <b>{{ detail.score }}</b>
              <span>负荷分</span>
            </div>
            <div class="mm-stat">
              <b>{{ detail.sessionCount }}</b>
              <span>次刺激</span>
            </div>
            <div class="mm-stat">
              <b>{{ detail.muscleMinutes }}</b>
              <span>参与分钟</span>
            </div>
            <div class="mm-stat">
              <b>{{ detail.intensityPercent ?? '—' }}</b>
              <span>平均强度 %</span>
            </div>
          </div>

          <p class="mm-advice">{{ detail.advice }}</p>

          <h4 class="mm-sub">贡献 Top 活动（近 {{ detail.days }} 天）</h4>
          <ul v-if="detail.contributions.length" class="mm-contrib">
            <li v-for="c in detail.contributions" :key="c.activityId">
              <span class="mm-c-name">{{ c.activityName }}</span>
              <span class="mm-c-date">{{ c.date }}</span>
              <span class="mm-c-min">{{ c.minutes }} 分钟</span>
              <span class="mm-c-bar">
                <i :style="{ width: c.percent + '%', background: heat(detail.score) }" />
              </span>
              <span class="mm-c-pct">{{ c.percent }}%</span>
            </li>
          </ul>
          <p v-else class="mm-c-empty">窗口内暂无刺激该肌群的活动。</p>
        </template>

        <div v-else class="a-empty">
          <div class="a-empty__text">明细加载失败，请稍后重试</div>
        </div>
      </div>

      <p class="mm-note">
        负荷为估算值：按运动类型的肌群参与度加权，再乘以时长与强度（心率口径，缺心率时取中等强度），
        仅作恢复与交叉训练参考。
      </p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ASkeleton from '@/components/ui/ASkeleton.vue'
import { analysisApi } from '@/api/analysis'
import type { MuscleDetail, MuscleMap } from '@/types/analysis'

const periodOptions = [7, 28, 90]
const days = ref(28)
const map = ref<MuscleMap | null>(null)
const loading = ref(true)
const error = ref('')

const topMuscle = computed(() => (map.value?.muscles?.length ? map.value.muscles[0] : null))

function find(key: string) {
  return map.value?.muscles?.find((m) => m.key === key) ?? null
}

/* ---------------- 蓝黄红热力色阶（DESIGN_SYSTEM §7 唯一口径） ---------------- */

const BLUE: [number, number, number] = [74, 158, 255] // #4a9eff
const YELLOW: [number, number, number] = [255, 214, 10] // #ffd60a
const RED: [number, number, number] = [255, 59, 48] // #ff3b30

function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}

function mix(a: [number, number, number], b: [number, number, number], t: number): string {
  return `rgb(${Math.round(lerp(a[0], b[0], t))}, ${Math.round(lerp(a[1], b[1], t))}, ${Math.round(
    lerp(a[2], b[2], t)
  )})`
}

/** 负荷分 -> 颜色：0 分中性灰；0-45 蓝→黄；45-100 黄→红 */
function heat(score: number): string {
  if (score <= 0) return 'rgba(150, 150, 158, 0.45)'
  const s = Math.min(score, 100)
  if (s <= 45) return mix(BLUE, YELLOW, s / 45)
  return mix(YELLOW, RED, (s - 45) / 55)
}

/** 图例渐变条：0 → 100 平滑采样 */
const scaleGradient = computed(() => {
  const stops: string[] = []
  for (let s = 0; s <= 100; s += 5) {
    stops.push(`${heat(s)} ${s}%`)
  }
  return `linear-gradient(90deg, ${stops.join(', ')})`
})

function heatOf(key: string): string {
  return heat(find(key)?.score ?? 0)
}

function labelOf(key: string): string {
  const m = find(key)
  if (!m) return ''
  return `${m.name} · ${m.score} 分（${m.levelText}）`
}

/* ---------------- 悬浮提示 ---------------- */

const figRef = ref<HTMLElement | null>(null)
const tip = ref<{ x: number; y: number; key: string } | null>(null)

function onHover(e: MouseEvent, key: string): void {
  const box = figRef.value?.getBoundingClientRect()
  if (!box) return
  tip.value = { x: e.clientX - box.left + 14, y: e.clientY - box.top + 14, key }
}

function hideHover(): void {
  tip.value = null
}

/* ---------------- 明细面板 ---------------- */

const selected = ref<string | null>(null)
const detail = ref<MuscleDetail | null>(null)
const detailLoading = ref(false)

async function loadDetail(key: string): Promise<void> {
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await analysisApi.getMuscleDetail(key, days.value)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function select(key: string): void {
  tip.value = null
  if (selected.value === key) {
    closeDetail()
    return
  }
  selected.value = key
  loadDetail(key)
}

function closeDetail(): void {
  selected.value = null
  detail.value = null
  detailLoading.value = false
}

/* ---------------- 加载与切换 ---------------- */

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    map.value = await analysisApi.getMuscleMap(days.value)
  } catch {
    map.value = null
    error.value = '肌肉热力图加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function switchDays(d: number): void {
  if (days.value === d) return
  days.value = d
  load()
  if (selected.value) {
    loadDetail(selected.value)
  }
}

onMounted(load)
</script>

<style scoped>
.mm-loading {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 32px;
  align-items: start;
}

.mm-seg {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(128, 128, 138, 0.14);
}
.mm-seg__btn {
  border: 0;
  background: transparent;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-family: inherit;
  color: var(--a-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}
.mm-seg__btn.on {
  background: var(--a-surface);
  color: var(--a-text);
  font-weight: 600;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.14);
}

.mm-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 32px;
  align-items: center;
}
.mm-figures {
  position: relative;
  display: flex;
  gap: 18px;
  justify-content: center;
}
.mm-fig {
  margin: 0;
  text-align: center;
}
.mm-svg {
  width: clamp(140px, 20vw, 190px);
  height: auto;
  display: block;
}
.mm-fig figcaption {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--a-text-tertiary);
  letter-spacing: 0.2em;
}
.mm-base {
  fill: rgba(128, 128, 138, 0.2);
}
.mm-limb {
  fill: none;
  stroke: rgba(128, 128, 138, 0.2);
  stroke-linecap: round;
}
.mm-arm {
  stroke-width: 13;
}
.mm-leg {
  stroke-width: 19;
}
.mm-mus {
  stroke: rgba(0, 0, 0, 0.1);
  stroke-width: 0.8;
  cursor: pointer;
  transition: filter 0.2s ease, stroke 0.2s ease;
}
.mm-g:hover .mm-mus {
  filter: brightness(1.12) drop-shadow(0 0 5px rgba(227, 6, 19, 0.4));
}
.mm-g.on .mm-mus {
  stroke: var(--a-blue);
  stroke-width: 1.8;
  filter: drop-shadow(0 0 6px var(--a-blue-ring));
}
.mm-cut {
  fill: none;
  stroke: rgba(0, 0, 0, 0.16);
  stroke-width: 1.2;
  stroke-linecap: round;
  pointer-events: none;
}

/* 悬浮提示 */
.mm-tip {
  position: absolute;
  z-index: 4;
  transform: translateY(-4px);
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--a-toast-bg);
  border: 1px solid var(--a-divider);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(12px);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--a-text);
  white-space: nowrap;
  pointer-events: none;
}

.mm-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}
.mm-sum-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(128, 128, 138, 0.1);
  text-align: center;
}
.mm-sum-item b {
  display: block;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
  color: var(--a-text);
}
.mm-sum-item span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--a-text-secondary);
}

.mm-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 3px 6px;
  margin: 0 -6px;
  border-radius: 10px;
  transition: background 0.18s ease;
}
.mm-item:hover {
  background: rgba(128, 128, 138, 0.1);
}
.mm-item.on {
  background: var(--a-blue-ring);
}
.mm-item.on .mm-name {
  color: var(--a-blue);
  font-weight: 600;
}
.mm-name {
  width: 78px;
  flex: 0 0 78px;
  font-size: 13.5px;
  color: var(--a-text);
  white-space: nowrap;
}
.mm-bar {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: rgba(128, 128, 138, 0.18);
  overflow: hidden;
}
.mm-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s cubic-bezier(0.2, 0.7, 0.3, 1);
}
.mm-score {
  width: 30px;
  flex: 0 0 30px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--a-text-secondary);
}

.mm-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  font-size: 12.5px;
  color: var(--a-text-secondary);
}
.mm-lg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.mm-lg i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.mm-scale {
  flex: 1;
  min-width: 120px;
  height: 8px;
  border-radius: 999px;
}

/* ---- 明细面板 ---- */
.mm-panel {
  margin-top: 24px;
  padding: 22px 26px 24px;
  text-align: left;
  animation: mm-panel-in 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes mm-panel-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.mm-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.mm-panel-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mm-panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--a-text);
}
.mm-panel-level {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border-radius: 999px;
  background: rgba(128, 128, 138, 0.14);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--a-text-secondary);
}
.mm-panel-level i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}
.mm-panel-close {
  border: 0;
  background: rgba(128, 128, 138, 0.14);
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 18px;
  line-height: 1;
  color: var(--a-text-secondary);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}
.mm-panel-close:hover {
  background: rgba(128, 128, 138, 0.26);
  color: var(--a-text);
}

.mm-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 16px;
}
.mm-stat {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(128, 128, 138, 0.1);
  text-align: center;
}
.mm-stat b {
  display: block;
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--a-text);
}
.mm-stat span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--a-text-secondary);
}

.mm-advice {
  margin: 16px 0 0;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--a-blue-ring);
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--a-text);
}

.mm-sub {
  margin: 20px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--a-text-secondary);
}

.mm-contrib {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mm-contrib li {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mm-c-name {
  width: 150px;
  flex: 0 0 150px;
  font-size: 13.5px;
  color: var(--a-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mm-c-date {
  width: 84px;
  flex: 0 0 84px;
  font-size: 12.5px;
  font-variant-numeric: tabular-nums;
  color: var(--a-text-tertiary);
}
.mm-c-min {
  width: 64px;
  flex: 0 0 64px;
  font-size: 12.5px;
  font-variant-numeric: tabular-nums;
  color: var(--a-text-secondary);
}
.mm-c-bar {
  flex: 1;
  height: 7px;
  border-radius: 999px;
  background: rgba(128, 128, 138, 0.18);
  overflow: hidden;
}
.mm-c-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s cubic-bezier(0.2, 0.7, 0.3, 1);
}
.mm-c-pct {
  width: 48px;
  flex: 0 0 48px;
  text-align: right;
  font-size: 12.5px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--a-text-secondary);
}
.mm-c-empty {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--a-text-tertiary);
}

.mm-note {
  margin-top: 20px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--a-text-tertiary);
}

@media (max-width: 960px) {
  .mm-grid {
    grid-template-columns: 1fr;
  }
  .mm-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .mm-c-name {
    width: 110px;
    flex: 0 0 110px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mm-panel {
    animation: none;
  }
}
</style>
