<template>
  <section class="section">
    <div class="section-head">
      <div>
        <h2 class="section-title">肌肉热力图</h2>
        <p class="section-desc">
          基于近 {{ days }} 天的运动类型、时长与强度估算肌群负荷（参考高驰 App 肌肉负荷口径），
          红得越深表示刺激越充分，帮助你安排恢复与交叉训练。
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

    <div v-if="loading" class="a-empty">
      <span class="a-spinner"></span>
    </div>
    <div v-else-if="error" class="a-empty">
      <div class="a-empty__text">{{ error }}</div>
    </div>

    <template v-else-if="map">
      <div class="mm-grid">
        <!-- 人体视图：正面 / 背面 -->
        <div class="mm-figures">
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

              <!-- 肌群 -->
              <ellipse class="mm-mus" cx="58" cy="63" rx="14" ry="11" :fill="heatOf('shoulders')">
                <title>{{ labelOf('shoulders') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="122" cy="63" rx="14" ry="11" :fill="heatOf('shoulders')">
                <title>{{ labelOf('shoulders') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="74" cy="87" rx="17" ry="13" :fill="heatOf('chest')">
                <title>{{ labelOf('chest') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="106" cy="87" rx="17" ry="13" :fill="heatOf('chest')">
                <title>{{ labelOf('chest') }}</title>
              </ellipse>
              <ellipse
                class="mm-mus"
                cx="50"
                cy="107"
                rx="8"
                ry="17"
                transform="rotate(12 50 107)"
                :fill="heatOf('biceps')"
              >
                <title>{{ labelOf('biceps') }}</title>
              </ellipse>
              <ellipse
                class="mm-mus"
                cx="130"
                cy="107"
                rx="8"
                ry="17"
                transform="rotate(-12 130 107)"
                :fill="heatOf('biceps')"
              >
                <title>{{ labelOf('biceps') }}</title>
              </ellipse>
              <rect class="mm-mus" x="76" y="104" width="28" height="56" rx="11" :fill="heatOf('core')">
                <title>{{ labelOf('core') }}</title>
              </rect>
              <ellipse class="mm-mus" cx="77" cy="238" rx="14" ry="38" :fill="heatOf('quads')">
                <title>{{ labelOf('quads') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="103" cy="238" rx="14" ry="38" :fill="heatOf('quads')">
                <title>{{ labelOf('quads') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="73" cy="312" rx="8" ry="27" :fill="heatOf('tibialis')">
                <title>{{ labelOf('tibialis') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="107" cy="312" rx="8" ry="27" :fill="heatOf('tibialis')">
                <title>{{ labelOf('tibialis') }}</title>
              </ellipse>
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

              <ellipse
                class="mm-mus"
                cx="70"
                cy="112"
                rx="15"
                ry="27"
                transform="rotate(18 70 112)"
                :fill="heatOf('lats')"
              >
                <title>{{ labelOf('lats') }}</title>
              </ellipse>
              <ellipse
                class="mm-mus"
                cx="110"
                cy="112"
                rx="15"
                ry="27"
                transform="rotate(-18 110 112)"
                :fill="heatOf('lats')"
              >
                <title>{{ labelOf('lats') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="79" cy="197" rx="15" ry="13" :fill="heatOf('glutes')">
                <title>{{ labelOf('glutes') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="101" cy="197" rx="15" ry="13" :fill="heatOf('glutes')">
                <title>{{ labelOf('glutes') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="77" cy="240" rx="13" ry="34" :fill="heatOf('hamstrings')">
                <title>{{ labelOf('hamstrings') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="103" cy="240" rx="13" ry="34" :fill="heatOf('hamstrings')">
                <title>{{ labelOf('hamstrings') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="76" cy="313" rx="10" ry="24" :fill="heatOf('calves')">
                <title>{{ labelOf('calves') }}</title>
              </ellipse>
              <ellipse class="mm-mus" cx="104" cy="313" rx="10" ry="24" :fill="heatOf('calves')">
                <title>{{ labelOf('calves') }}</title>
              </ellipse>
            </svg>
            <figcaption>背面</figcaption>
          </figure>
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
            <li v-for="m in map.muscles" :key="m.key" class="mm-item" :title="m.levelText">
              <span class="mm-name">{{ m.name }}</span>
              <span class="mm-bar">
                <i :style="{ width: m.score + '%', background: heat(m.score) }" />
              </span>
              <span class="mm-score">{{ m.score }}</span>
            </li>
          </ul>

          <div class="mm-legend">
            <span class="mm-lg"><i :style="{ background: heat(85) }" />高负荷</span>
            <span class="mm-lg"><i :style="{ background: heat(55) }" />中等</span>
            <span class="mm-lg"><i :style="{ background: heat(25) }" />轻度</span>
            <span class="mm-lg"><i :style="{ background: heat(0) }" />未激活</span>
          </div>
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
import { analysisApi } from '@/api/analysis'
import type { MuscleMap } from '@/types/analysis'

const periodOptions = [7, 28, 90]
const days = ref(28)
const map = ref<MuscleMap | null>(null)
const loading = ref(true)
const error = ref('')

const topMuscle = computed(() => (map.value?.muscles?.length ? map.value.muscles[0] : null))

function find(key: string) {
  return map.value?.muscles?.find((m) => m.key === key) ?? null
}

/** 热力色：品牌红透明度随分数增强；0 分为中性灰 */
function heat(score: number): string {
  if (score <= 0) return 'rgba(150, 150, 158, 0.45)'
  const a = 0.2 + (Math.min(score, 100) / 100) * 0.8
  return `rgba(227, 6, 19, ${a.toFixed(2)})`
}

function heatOf(key: string): string {
  return heat(find(key)?.score ?? 0)
}

function labelOf(key: string): string {
  const m = find(key)
  if (!m) return ''
  return `${m.name} · ${m.score} 分（${m.levelText}）`
}

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
}

onMounted(load)
</script>

<style scoped>
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
  transition: filter 0.2s ease;
  cursor: help;
}
.mm-mus:hover {
  filter: brightness(1.25) drop-shadow(0 0 6px rgba(227, 6, 19, 0.55));
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
}
</style>
