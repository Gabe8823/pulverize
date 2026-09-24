<template>
  <div class="page">
    <header class="page-head">
      <p class="eyebrow">跑步记录</p>
      <h1 class="headline">每一次奔跑，都有迹可循</h1>
      <p class="subhead">共 {{ total }} 条记录，按时间倒序排列。</p>

      <!-- 列表 / 卡片 视图切换 -->
      <div class="head-tools">
        <div class="a-seg" role="tablist" aria-label="视图切换">
          <button
            type="button"
            class="a-seg__item"
            :class="{ 'is-active': viewMode === 'list' }"
            :aria-pressed="viewMode === 'list'"
            @click="setViewMode('list')"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" aria-hidden="true">
              <path d="M2.5 4h11M2.5 8h11M2.5 12h11" />
            </svg>
            列表
          </button>
          <button
            type="button"
            class="a-seg__item"
            :class="{ 'is-active': viewMode === 'card' }"
            :aria-pressed="viewMode === 'card'"
            @click="setViewMode('card')"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" aria-hidden="true">
              <rect x="2.4" y="2.4" width="4.7" height="4.7" rx="1.2" />
              <rect x="8.9" y="2.4" width="4.7" height="4.7" rx="1.2" />
              <rect x="2.4" y="8.9" width="4.7" height="4.7" rx="1.2" />
              <rect x="8.9" y="8.9" width="4.7" height="4.7" rx="1.2" />
            </svg>
            卡片
          </button>
        </div>
      </div>
    </header>

    <section class="section">
      <!-- 筛选工具条 -->
      <div class="a-card filter-bar">
        <div class="filter-field">
          <span class="filter-label">开始日期</span>
          <ADateInput
            :model-value="startDate"
            placeholder="开始日期"
            @update:model-value="onStartDateChange"
            @enter="handleSearch"
          />
        </div>
        <div class="filter-field">
          <span class="filter-label">结束日期</span>
          <ADateInput
            :model-value="endDate"
            placeholder="结束日期"
            @update:model-value="onEndDateChange"
            @enter="handleSearch"
          />
        </div>
        <div class="filter-field">
          <span class="filter-label">活动类型</span>
          <ASelect v-model="typeFilter" :options="typeOptions" placeholder="全部类型" @change="handleSearch" />
        </div>
        <div class="filter-actions">
          <AButton type="primary" @click="handleSearch">查询</AButton>
          <AButton type="default" @click="handleReset">重置</AButton>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrap">
        <span class="a-spinner" role="status" aria-label="加载中"></span>
      </div>

      <!-- 空态 -->
      <div v-else-if="activities.length === 0" class="a-card">
        <div class="a-empty">
          <div class="a-empty__icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 21s-6.6-5.7-6.6-10.6a6.6 6.6 0 1 1 13.2 0C18.6 15.3 12 21 12 21z" />
              <circle cx="12" cy="10.4" r="2.4" />
            </svg>
          </div>
          <div class="a-empty__text">暂无跑步记录</div>
          <div class="a-empty__sub">连接运动平台并同步后，这里会显示你的每一次跑步</div>
          <AButton type="primary" @click="router.push('/connect')">去连接运动平台</AButton>
        </div>
      </div>

      <template v-else>
        <!-- 列表视图 -->
        <div v-if="viewMode === 'list'" class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>活动名称</th>
                <th>平台</th>
                <th>日期</th>
                <th>距离</th>
                <th>时长</th>
                <th>平均配速</th>
                <th>平均心率</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in activities" :key="row.id" @click="goDetail(row.id)">
                <td>
                  <div class="name-cell">
                    <PlatformIcon :platform="row.platform" :size="28" />
                    <span class="name-text">{{ row.activityName || '未命名跑步' }}</span>
                    <span class="chip" :class="typeChip(row.activityType)">{{ typeLabel(row.activityType) }}</span>
                  </div>
                </td>
                <td class="cell-muted">{{ platformLabel(row.platform) }}</td>
                <td class="cell-nowrap">{{ formatDateTime(row.startTime) }}</td>
                <td class="cell-num cell-distance">{{ formatDistance(row.distanceM) }}<span class="unit">km</span></td>
                <td class="cell-num cell-nowrap">{{ formatDuration(row.durationSeconds) }}</td>
                <td class="cell-num cell-nowrap">{{ formatPace(row.avgPaceSecKm) }}<span class="unit">/km</span></td>
                <td class="cell-num cell-nowrap">{{ row.avgHeartRate ? `${row.avgHeartRate} bpm` : '--' }}</td>
                <td>
                  <div class="actions-cell">
                    <AButton size="sm" type="ghost" @click="goDetail(row.id, $event)">详情 ›</AButton>
                    <AButton size="sm" type="ghost-danger" @click="handleDelete(row.id, $event)">
                      {{ deleteTargetId === row.id ? '确认删除' : '删除' }}
                    </AButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 卡片视图 -->
        <div v-else class="card-grid activity-grid">
          <article
            v-for="item in activities"
            :key="item.id"
            class="a-card hoverable activity-card"
            @click="goDetail(item.id)"
          >
            <div class="card-top">
              <PlatformIcon :platform="item.platform" :size="40" />
              <span class="chip" :class="typeChip(item.activityType)">{{ typeLabel(item.activityType) }}</span>
              <span class="card-date">{{ formatDate(item.startTime) }}</span>
            </div>

            <h3 class="card-name">{{ item.activityName || '未命名跑步' }}</h3>

            <div class="card-stats">
              <div class="card-stat">
                <div class="stat-value stat-sm">
                  {{ formatDistance(item.distanceM) }}<span class="stat-unit">km</span>
                </div>
                <div class="stat-label">距离</div>
              </div>
              <div class="card-stat">
                <div class="stat-value stat-sm">{{ formatDuration(item.durationSeconds) }}</div>
                <div class="stat-label">时长</div>
              </div>
              <div class="card-stat">
                <div class="stat-value stat-sm">{{ formatPace(item.avgPaceSecKm) }}</div>
                <div class="stat-label">平均配速 /km</div>
              </div>
            </div>

            <div class="card-foot">
              <span class="foot-item">
                平均心率
                <strong>{{ item.avgHeartRate ? `${item.avgHeartRate} bpm` : '--' }}</strong>
              </span>
              <span v-if="item.elevationGainM" class="foot-item">
                爬升 <strong>+{{ item.elevationGainM }} m</strong>
              </span>
              <span class="foot-item foot-platform">{{ platformLabel(item.platform) }}</span>
            </div>
          </article>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-bar">
          <APagination
            v-model="query.pageNum"
            :total="total"
            :page-size="query.pageSize"
            @update:model-value="handlePageChange"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import AButton from '@/components/ui/AButton.vue'
import ADateInput from '@/components/ui/ADateInput.vue'
import ASelect from '@/components/ui/ASelect.vue'
import APagination from '@/components/ui/APagination.vue'
import PlatformIcon from '@/components/icons/PlatformIcon.vue'
import { runningApi } from '@/api/running'
import { toast } from '@/utils/toast'
import { formatPace, formatDistance, formatDuration, formatDate, formatDateTime } from '@/utils/format'
import type { RunningActivity, ActivityQuery } from '@/types/running'

const router = useRouter()

const activities = ref<RunningActivity[]>([])
const total = ref(0)
const loading = ref(true)

const startDate = ref('')
const endDate = ref('')
const typeFilter = ref<string | number | null>(null)
const deleteTargetId = ref<number | null>(null)
let deleteTimer: ReturnType<typeof setTimeout> | null = null

const query = reactive<ActivityQuery>({
  pageNum: 1,
  pageSize: 10,
})

/* ---------- 视图切换（列表 / 卡片），持久化到 localStorage ---------- */
type ViewMode = 'list' | 'card'
const VIEW_KEY = 'runai-activity-view'

function readViewMode(): ViewMode {
  try {
    return localStorage.getItem(VIEW_KEY) === 'card' ? 'card' : 'list'
  } catch {
    return 'list'
  }
}

const viewMode = ref<ViewMode>(readViewMode())

function setViewMode(mode: ViewMode) {
  viewMode.value = mode
  try {
    localStorage.setItem(VIEW_KEY, mode)
  } catch {
    /* 忽略存储失败 */
  }
}

/* ---------- 筛选 ---------- */
const typeOptions = [
  { value: 'RUNNING', label: '跑步', hint: '路跑与日常训练' },
  { value: 'TRAIL_RUN', label: '越野跑', hint: '山地与越野路线' },
]

function onStartDateChange(v: string | number | null) {
  startDate.value = v === null ? '' : String(v)
}
function onEndDateChange(v: string | number | null) {
  endDate.value = v === null ? '' : String(v)
}

async function loadActivities() {
  loading.value = true
  try {
    const params: ActivityQuery = { ...query }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    if (typeFilter.value !== null && typeFilter.value !== '') {
      params.activityType = String(typeFilter.value)
    }
    const res = await runningApi.listActivities(params)
    activities.value = res.records || []
    total.value = res.total || 0
  } catch {
    toast.error('跑步记录加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadActivities()
}

function handleReset() {
  startDate.value = ''
  endDate.value = ''
  typeFilter.value = null
  query.pageNum = 1
  loadActivities()
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadActivities()
}

/* ---------- 跳转与删除 ---------- */
function goDetail(id: number, e?: MouseEvent) {
  e?.stopPropagation()
  router.push(`/activities/${id}`)
}

function handleDelete(id: number, e: MouseEvent) {
  e.stopPropagation()
  if (deleteTargetId.value !== id) {
    deleteTargetId.value = id
    if (deleteTimer) clearTimeout(deleteTimer)
    deleteTimer = setTimeout(() => {
      deleteTargetId.value = null
    }, 3000)
    return
  }
  confirmDelete(id)
}

async function confirmDelete(id: number) {
  if (deleteTimer) {
    clearTimeout(deleteTimer)
    deleteTimer = null
  }
  deleteTargetId.value = null
  try {
    await runningApi.deleteActivity(id)
    toast.success('已删除')
    await loadActivities()
  } catch {
    toast.error('删除失败，请稍后重试')
  }
}

/* ---------- 展示文案 ---------- */
const TYPE_LABELS: Record<string, string> = {
  RUNNING: '跑步',
  TRAIL_RUN: '越野跑',
  TREADMILL: '室内跑',
}

function typeLabel(type?: string) {
  if (!type) return '其他'
  return TYPE_LABELS[type.toUpperCase()] || '其他'
}

function typeChip(type?: string) {
  const key = (type || '').toUpperCase()
  if (key === 'RUNNING') return 'chip-blue'
  if (key === 'TRAIL_RUN') return 'chip-green'
  if (key === 'TREADMILL') return 'chip-orange'
  return 'chip-gray'
}

const PLATFORM_LABELS: Record<string, string> = {
  MANUAL: '手动录入',
  STRAVA: 'Strava',
  GARMIN: 'Garmin',
  COROS: 'COROS',
  APPLE: 'Apple',
  HUAWEI: '华为运动健康',
  ZEPP: 'Zepp',
  KEEP: 'Keep',
}

function platformLabel(platform?: string) {
  if (!platform) return '未知来源'
  return PLATFORM_LABELS[platform.toUpperCase()] || platform
}

onMounted(loadActivities)

onBeforeUnmount(() => {
  if (deleteTimer) clearTimeout(deleteTimer)
})
</script>

<style scoped lang="scss">
/* ---------- 页头视图切换 ---------- */
.head-tools {
  display: flex;
  justify-content: flex-end;
  margin-top: 26px;
}

/* ---------- 筛选工具条 ---------- */
.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  /* 三个控件等宽自适应，避免大小不一 */
  flex: 1 1 180px;
  min-width: 176px;
  max-width: 260px;
}

.filter-label {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--a-text-secondary);
  letter-spacing: 0.01em;
}

/* 日期触发器与下拉触发器统一：44px 高 / 圆角 12 / 字号 15 */
.filter-field :deep(.a-date-trigger) {
  height: 44px;
  min-height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
  padding: 0 14px;
}

.filter-field :deep(.a-select__trigger) {
  height: 44px;
  min-height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
  padding: 0 14px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

/* ---------- 加载 ---------- */
.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 72px 0;
}

/* ---------- 空态 ---------- */
.a-empty .a-btn {
  margin-top: 22px;
}
.a-empty__icon {
  color: var(--a-blue);
}

/* ---------- 列表视图 ---------- */
.a-table {
  min-width: 920px;
}

.col-actions {
  text-align: right;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.name-text {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.cell-muted {
  color: var(--a-text-secondary);
}

.cell-nowrap {
  white-space: nowrap;
}

.cell-num {
  font-variant-numeric: tabular-nums;
}

.cell-distance {
  font-weight: 600;
}

.unit {
  font-size: 12px;
  font-weight: 500;
  color: var(--a-text-tertiary);
  margin-left: 4px;
}

.actions-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
}

/* ---------- 卡片视图 ---------- */
.card-grid.activity-grid {
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.activity-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-date {
  margin-left: auto;
  font-size: 13px;
  color: var(--a-text-tertiary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.card-name {
  margin: 16px 0 0;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--a-text);
}

.card-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--a-divider);
}

.card-stat {
  min-width: 0;
}

.stat-sm {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.1;
  color: var(--a-text);
  font-variant-numeric: tabular-nums;
}

.card-stat .stat-label {
  margin-top: 6px;
  font-size: 13px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 600;
  color: var(--a-text-tertiary);
  margin-left: 4px;
}

.card-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--a-divider);
  font-size: 13.5px;
  color: var(--a-text-secondary);
}

.foot-item strong {
  font-weight: 600;
  color: var(--a-text);
}

.foot-platform {
  margin-left: auto;
  color: var(--a-text-tertiary);
}

/* ---------- 分页 ---------- */
.pagination-bar {
  margin-top: 26px;
  display: flex;
  justify-content: center;
}
</style>
