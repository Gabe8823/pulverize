<template>
  <div class="page">
    <header class="page-head">
      <p class="eyebrow">训练计划</p>
      <h1 class="display">每一步，都有方向。</h1>
      <p class="subhead">AI 依据你的目标与近期跑步数据生成周期化计划，每一天都清楚该跑什么。</p>
      <div class="hero-actions">
        <AButton type="primary" @click="router.push('/plan/generate')">生成新计划</AButton>
      </div>
    </header>

    <section class="section">
      <div v-if="loading" class="loading-state">
        <span class="a-spinner"></span>
      </div>

      <div v-else-if="plans.length === 0" class="a-empty">
        <div class="a-empty__icon" aria-hidden="true">🏃</div>
        <div class="a-empty__text">还没有训练计划</div>
        <div class="a-empty__sub">生成一份，从今天开始科学训练</div>
        <div class="empty-actions">
          <AButton type="primary" @click="router.push('/plan/generate')">生成新计划</AButton>
        </div>
      </div>

      <div v-else class="card-grid-2">
        <article
          v-for="plan in plans"
          :key="plan.id"
          class="a-card hoverable plan-card"
          @click="router.push(`/plan/${plan.id}`)"
        >
          <div class="plan-top">
            <span class="chip" :class="statusChip(plan.status)">{{ statusLabel(plan.status) }}</span>
            <span class="chip chip-blue">{{ goalTypeLabel(plan.goalType) }}</span>
          </div>

          <h3 class="plan-name" :title="plan.planName">{{ plan.planName }}</h3>
          <p class="plan-meta">{{ plan.startDate }} ~ {{ plan.endDate }}</p>
          <p v-if="plan.goalValue" class="plan-goal">目标：<strong>{{ plan.goalValue }}</strong></p>

          <div class="plan-foot">
            <div class="plan-stats">
              <span class="stat-count">{{ planStats(plan).weeks }} 周</span>
              <span class="stat-dot">·</span>
              <span class="stat-count">{{ planStats(plan).days }} 天</span>
            </div>
            <div class="plan-actions" @click.stop>
              <AButton size="sm" type="primary" @click="router.push(`/plan/${plan.id}`)">查看</AButton>
              <AButton size="sm" type="ghost-danger" @click="openDelete(plan)">删除</AButton>
            </div>
          </div>
        </article>
      </div>
    </section>

    <AModal
      v-model="deleteVisible"
      title="删除训练计划？"
      danger
      confirm-text="删除"
      cancel-text="取消"
      :loading="deleting"
      @confirm="confirmDelete"
    >
      确定要删除「{{ deleteTarget?.planName }}」吗？该计划下所有每日训练安排会一并删除，此操作不可恢复。
    </AModal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AButton from '@/components/ui/AButton.vue'
import AModal from '@/components/ui/AModal.vue'
import { planApi } from '@/api/plan'
import { toast } from '@/utils/toast'
import type { TrainingPlan } from '@/types/plan'

const router = useRouter()

const plans = ref<TrainingPlan[]>([])
const loading = ref(true)

const deleteVisible = ref(false)
const deleting = ref(false)
const deleteTarget = ref<TrainingPlan | null>(null)

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    plans.value = await planApi.listPlans()
  } catch {
    /* 请求拦截器已统一提示错误 */
  } finally {
    loading.value = false
  }
}

function openDelete(plan: TrainingPlan): void {
  deleteTarget.value = plan
  deleteVisible.value = true
}

async function confirmDelete(): Promise<void> {
  const target = deleteTarget.value
  if (!target) return
  deleting.value = true
  try {
    await planApi.deletePlan(target.id)
    toast.success('删除成功')
    deleteVisible.value = false
    deleteTarget.value = null
    await load()
  } catch {
    /* 请求拦截器已统一提示错误 */
  } finally {
    deleting.value = false
  }
}

function parseDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map((part) => Number(part))
  return new Date(y, (m || 1) - 1, d || 1)
}

function planStats(plan: TrainingPlan): { weeks: number; days: number } {
  const start = parseDate(plan.startDate)
  const end = parseDate(plan.endDate)
  const diff = Math.round((end.getTime() - start.getTime()) / 86400000) + 1
  const days = Math.max(1, Number.isFinite(diff) ? diff : 1)
  return { weeks: Math.max(1, Math.ceil(days / 7)), days }
}

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
</script>

<style scoped lang="scss">
.hero-actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.loading-state {
  padding: 72px 0;
  display: flex;
  justify-content: center;
}

.empty-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.plan-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.plan-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.plan-name {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.014em;
  line-height: 1.3;
  color: var(--a-text);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.plan-meta {
  margin: 8px 0 0;
  font-size: 13.5px;
  color: var(--a-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.plan-goal {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--a-text-secondary);

  strong {
    font-weight: 600;
    color: var(--a-text);
  }
}

.plan-foot {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  border-top: 1px solid var(--a-divider);
}

.plan-stats {
  display: flex;
  align-items: center;
  gap: 7px;
  font-variant-numeric: tabular-nums;
}

.stat-count {
  font-size: 14px;
  font-weight: 600;
  color: var(--a-text);
}

.stat-dot {
  font-size: 14px;
  color: var(--a-text-tertiary);
}

.plan-actions {
  display: flex;
  gap: 8px;
}
</style>
