<template>
  <div class="page">
    <!-- 页头 -->
    <header class="page-head">
      <p class="eyebrow">个人资料</p>
      <h1 class="headline">你的训练档案。</h1>
      <p class="subhead">
        这些数据用于计算心率区间与个性化训练强度，保持更新，AI 教练的建议会更准确。
      </p>
      <div class="head-actions">
        <AButton type="primary" size="lg" :loading="loading" @click="handleSave">保存修改</AButton>
      </div>
    </header>

    <!-- 顶部身份卡 -->
    <section class="section">
      <div class="a-card identity-card">
        <div class="avatar-lg">{{ avatarText }}</div>
        <div class="identity-meta">
          <div class="identity-name">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}
          </div>
          <p class="caption">@{{ userStore.userInfo?.username || '—' }}</p>
        </div>
        <span class="chip chip-green">已注册</span>
      </div>
    </section>

    <!-- 基本信息 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">基本信息</h2>
          <p class="section-desc">昵称与基础个人信息，用于页面展示与统计。</p>
        </div>
      </div>

      <div class="a-card">
        <div class="row-list">
          <div class="row">
            <label class="row-label" for="profile-nickname">昵称</label>
            <div class="row-control">
              <AInput
                id="profile-nickname"
                v-model="form.nickname"
                placeholder="请输入昵称"
                :maxlength="20"
              />
            </div>
          </div>

          <div class="row">
            <span class="row-label">性别</span>
            <div class="row-control">
              <ASelect v-model="form.gender" :options="genderOptions" />
            </div>
          </div>

          <div class="row">
            <label class="row-label" for="profile-birthday">生日</label>
            <div class="row-control">
              <ADateInput id="profile-birthday" v-model="form.birthday" placeholder="选择生日" />
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 身体数据 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">身体数据</h2>
          <p class="section-desc">用于计算心率区间与训练强度建议，建议如实填写。</p>
        </div>
        <AButton size="sm" :loading="syncing" @click="handleSyncCoros">从高驰同步</AButton>
      </div>

      <div class="a-card">
        <div class="field-grid">
          <div class="a-field">
            <label class="a-label" for="profile-height">身高 (cm)</label>
            <AInput
              id="profile-height"
              v-model="form.heightCm"
              type="number"
              :min="100"
              :max="250"
              placeholder="例如 175"
            />
            <p class="a-hint">用于评估跑步经济性与体重管理建议。</p>
          </div>

          <div class="a-field">
            <label class="a-label" for="profile-weight">体重 (kg)</label>
            <AInput
              id="profile-weight"
              v-model="form.weightKg"
              type="number"
              :min="30"
              :max="200"
              placeholder="例如 65"
            />
            <p class="a-hint">用于估算每公里热量消耗与训练负荷。</p>
          </div>

          <div class="a-field">
            <label class="a-label" for="profile-max-hr">最大心率 (bpm)</label>
            <AInput
              id="profile-max-hr"
              v-model="form.maxHeartRate"
              type="number"
              :min="100"
              :max="230"
              placeholder="例如 190"
            />
            <p class="a-hint">用于计算心率区间，建议实测或用 220-年龄估算。</p>
          </div>

          <div class="a-field">
            <label class="a-label" for="profile-rest-hr">静息心率 (bpm)</label>
            <AInput
              id="profile-rest-hr"
              v-model="form.restHeartRate"
              type="number"
              :min="30"
              :max="120"
              placeholder="例如 58"
            />
            <p class="a-hint">晨起静卧时测量，反映身体恢复状态。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 服务接入 -->
    <section id="ai-config" class="section" style="scroll-margin-top: 76px">
      <div class="section-head">
        <div>
          <h2 class="section-title">AI 服务接入</h2>
          <p class="section-desc">配置 OpenAI 兼容的大模型接口（如 DeepSeek），驱动目标推荐、训练报告与教练点评；密钥仅保存在服务端。</p>
        </div>
        <div class="ai-actions">
          <span class="chip" :class="aiSourceChip">{{ aiSourceLabel }}</span>
          <AButton size="sm" :loading="aiTesting" @click="handleAiTest">测试连接</AButton>
          <AButton size="sm" type="primary" :loading="aiSaving" @click="handleAiSave">保存配置</AButton>
        </div>
      </div>

      <div class="a-card">
        <div class="row-list">
          <div class="row">
            <label class="row-label" for="ai-url">接口地址</label>
            <div class="row-control">
              <AInput
                id="ai-url"
                v-model="aiForm.baseUrl"
                placeholder="https://api.deepseek.com/chat/completions"
              />
            </div>
          </div>

          <div class="row">
            <label class="row-label" for="ai-model">模型名称</label>
            <div class="row-control">
              <AInput id="ai-model" v-model="aiForm.model" placeholder="deepseek-chat" />
            </div>
          </div>

          <div class="row">
            <label class="row-label" for="ai-key">API 密钥</label>
            <div class="row-control">
              <AInput
                id="ai-key"
                v-model="aiForm.apiKey"
                type="password"
                :placeholder="aiKeyPlaceholder"
                autocomplete="new-password"
              />
            </div>
          </div>
        </div>
        <p class="ai-hint">
          密钥保存在服务端数据库，界面只显示掩码；「API 密钥」留空保存表示不修改已配置的密钥。
          「测试连接」会直接用当前填写的值发起一次真实调用。
        </p>
      </div>
    </section>

    <!-- 账号信息 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">账号信息</h2>
          <p class="section-desc">由系统维护，不可修改。</p>
        </div>
      </div>

      <div class="a-card">
        <div class="row-list">
          <div class="row">
            <span class="row-label">用户名</span>
            <span class="row-value">{{ userStore.userInfo?.username || '—' }}</span>
          </div>
          <div class="row">
            <span class="row-label">用户 ID</span>
            <span class="row-value">{{ userStore.userInfo?.id ?? '—' }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'
import ASelect from '@/components/ui/ASelect.vue'
import ADateInput from '@/components/ui/ADateInput.vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/user'
import { aiApi } from '@/api/ai'
import { toast } from '@/utils/toast'
import type { UserInfo } from '@/types/user'
import type { AiConfig } from '@/types/ai'

const userStore = useUserStore()
const loading = ref(false)
const syncing = ref(false)

/* ---- AI 服务接入 ---- */
const aiSaving = ref(false)
const aiTesting = ref(false)
const aiSource = ref<'DB' | 'YML' | 'NONE'>('NONE')
const aiKeySet = ref(false)
const aiKeyMasked = ref<string | null>(null)
const aiForm = reactive({
  baseUrl: '',
  model: '',
  apiKey: '',
})

const aiKeyPlaceholder = computed<string>(() =>
  aiKeySet.value && aiKeyMasked.value
    ? `已配置（${aiKeyMasked.value}），留空不修改`
    : '未配置，请填入 API 密钥'
)

const aiSourceLabel = computed<string>(() => {
  if (aiSource.value === 'DB') return '已配置（界面）'
  if (aiSource.value === 'YML') return '配置文件密钥'
  return '未配置密钥'
})

const aiSourceChip = computed<string>(() => {
  if (aiSource.value === 'DB') return 'chip-green'
  if (aiSource.value === 'YML') return 'chip-blue'
  return 'chip-gray'
})

/** 将接口返回的配置回填到界面（密钥只回传掩码，输入框清空） */
function applyAiConfig(c: AiConfig): void {
  aiForm.baseUrl = c.baseUrl || ''
  aiForm.model = c.model || ''
  aiForm.apiKey = ''
  aiSource.value = c.source
  aiKeySet.value = c.keySet
  aiKeyMasked.value = c.keyMasked ?? null
}

async function loadAiConfig(): Promise<void> {
  try {
    applyAiConfig(await aiApi.getConfig())
  } catch {
    // 拦截器已统一提示
  }
}

/** 性别：与后端字段保持一致（0 保密 / 1 男 / 2 女） */
const genderOptions: Array<{ value: number; label: string }> = [
  { value: 1, label: '男' },
  { value: 2, label: '女' },
  { value: 0, label: '保密' },
]

const form = reactive({
  nickname: '',
  gender: 0,
  birthday: '',
  heightCm: null as number | null,
  weightKg: null as number | null,
  maxHeartRate: null as number | null,
  restHeartRate: null as number | null,
})

const avatarText = computed<string>(() => {
  const u = userStore.userInfo
  return (u?.nickname || u?.username || 'U').charAt(0).toUpperCase()
})

function syncFromStore(): void {
  const u = userStore.userInfo
  if (!u) return
  form.nickname = u.nickname || ''
  form.gender = u.gender ?? 0
  form.birthday = u.birthday || ''
  form.heightCm = u.heightCm ?? null
  form.weightKg = u.weightKg ?? null
  form.maxHeartRate = u.maxHeartRate ?? null
  form.restHeartRate = u.restHeartRate ?? null
}

onMounted(async () => {
  void loadAiConfig()
  if (userStore.userInfo) {
    syncFromStore()
    return
  }
  try {
    await userStore.fetchProfile()
    syncFromStore()
  } catch {
    // 错误已由 axios 拦截器提示
  }
})

/** 保存 AI 接入配置（密钥留空表示不修改），保存后对后续 AI 调用立即生效 */
async function handleAiSave(): Promise<void> {
  if (aiSaving.value) return
  if (!aiForm.baseUrl.trim()) {
    toast.error('请填写接口地址')
    return
  }
  if (!aiForm.model.trim()) {
    toast.error('请填写模型名称')
    return
  }
  aiSaving.value = true
  try {
    const saved = await aiApi.saveConfig({
      baseUrl: aiForm.baseUrl.trim(),
      model: aiForm.model.trim(),
      apiKey: aiForm.apiKey.trim() || undefined,
    })
    applyAiConfig(saved)
    toast.success('AI 接入配置已保存，立即生效')
  } catch {
    // 拦截器已统一提示
  } finally {
    aiSaving.value = false
  }
}

/** 用当前填写的值发起一次真实连通性测试（空白字段取已保存配置） */
async function handleAiTest(): Promise<void> {
  if (aiTesting.value) return
  aiTesting.value = true
  try {
    const res = await aiApi.test({
      baseUrl: aiForm.baseUrl.trim() || undefined,
      model: aiForm.model.trim() || undefined,
      apiKey: aiForm.apiKey.trim() || undefined,
    })
    const ms = res?.latencyMs
    toast.success(ms != null ? `AI 连接成功（${ms} ms）` : 'AI 连接成功')
  } catch {
    // 拦截器已统一提示失败原因
  } finally {
    aiTesting.value = false
  }
}

function outOfRange(value: number | null, min: number, max: number): boolean {
  return value !== null && (!Number.isFinite(value) || value < min || value > max)
}

function validate(): boolean {
  if (!form.nickname.trim()) {
    toast.error('请输入昵称')
    return false
  }
  if (outOfRange(form.heightCm, 100, 250)) {
    toast.error('请输入合理的身高（100-250 cm）')
    return false
  }
  if (outOfRange(form.weightKg, 30, 200)) {
    toast.error('请输入合理的体重（30-200 kg）')
    return false
  }
  if (outOfRange(form.maxHeartRate, 100, 230)) {
    toast.error('请输入合理的最大心率（100-230 bpm）')
    return false
  }
  if (outOfRange(form.restHeartRate, 30, 120)) {
    toast.error('请输入合理的静息心率（30-120 bpm）')
    return false
  }
  return true
}

/** 从高驰同步身体数据（身高 / 体重 / 最大心率 / 静息心率 / 生日，性别不同步） */
async function handleSyncCoros(): Promise<void> {
  if (syncing.value) return

  syncing.value = true
  try {
    const u = await userApi.syncCorosProfile()
    if (!u) {
      toast.error('高驰未返回身体数据')
      return
    }
    form.heightCm = u.heightCm ?? null
    form.weightKg = u.weightKg ?? null
    form.maxHeartRate = u.maxHeartRate ?? null
    form.restHeartRate = u.restHeartRate ?? null
    form.birthday = u.birthday || ''
    toast.success('已从高驰同步身体数据')
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    syncing.value = false
  }
}

async function handleSave(): Promise<void> {
  if (loading.value) return
  if (!validate()) return

  const payload: Partial<UserInfo> = {
    nickname: form.nickname.trim(),
    gender: form.gender,
    heightCm: form.heightCm,
    weightKg: form.weightKg,
    maxHeartRate: form.maxHeartRate === null ? null : Math.round(form.maxHeartRate),
    restHeartRate: form.restHeartRate === null ? null : Math.round(form.restHeartRate),
  }
  if (form.birthday) payload.birthday = form.birthday

  loading.value = true
  try {
    await userApi.updateProfile(payload)
    await userStore.fetchProfile()
    syncFromStore()
    toast.success('资料已保存')
  } catch {
    // 错误已由 axios 拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.head-actions {
  margin-top: 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

/* ---- 身份卡 ---- */
.identity-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 26px 28px;
  flex-wrap: wrap;
}

.avatar-lg {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  flex: none;
  background: var(--a-blue);
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -0.02em;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.identity-meta {
  min-width: 0;
  flex: 1;
}

.identity-name {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--a-text);
}

.identity-meta .caption {
  margin: 4px 0 0;
}

/* ---- 行式布局（Apple 设置风格） ---- */
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid var(--a-divider);
}

.row:first-child {
  padding-top: 4px;
}

.row:last-child {
  border-bottom: none;
  padding-bottom: 4px;
}

.row-label {
  font-size: 15px;
  font-weight: 500;
  color: var(--a-text);
  flex: none;
}

.row-control {
  width: min(320px, 58%);
}

.row-value {
  font-size: 15px;
  color: var(--a-text-secondary);
  font-variant-numeric: tabular-nums;
  text-align: right;
  word-break: break-all;
}

/* ---- 身体数据网格 ---- */
.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px 26px;
}

@media (max-width: 640px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .row-control {
    width: 100%;
  }

  .row-value {
    text-align: left;
  }
}

/* ---- AI 服务接入 ---- */
.ai-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-hint {
  margin: 14px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--a-text-secondary);
}
</style>
