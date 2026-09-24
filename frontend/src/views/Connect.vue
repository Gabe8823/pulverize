<template>
  <div class="page">
    <!-- ============ 页头 ============ -->
    <header class="page-head">
      <p class="eyebrow">连接</p>
      <h1 class="headline">连接你的运动世界</h1>
      <p class="subhead">
        绑定运动 App 与手表，自动同步每一次奔跑；或接入 MCP，
        <span class="text-accent">让 AI 教练直接读懂你的训练数据。</span>
      </p>
    </header>

    <!-- ============ A. 平台连接区 ============ -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">运动 App 与设备</h2>
          <p class="section-desc">
            授权后自动同步活动、分段与心率数据。解绑不会删除已同步到本地的记录。
          </p>
        </div>
        <AButton type="ghost" :loading="listLoading" @click="loadPlatforms">刷新状态</AButton>
      </div>

      <div v-if="listLoading && platforms.length === 0" class="loading-row">
        <span class="a-spinner"></span>
        <span class="caption">正在加载平台状态…</span>
      </div>

      <div v-else-if="platforms.length === 0" class="a-empty">
        <div class="a-empty__text">暂无可连接的平台</div>
        <div class="a-empty__sub">稍后刷新试试</div>
      </div>

      <div v-else class="card-grid-2">
        <article
          v-for="p in platforms"
          :key="p.platform"
          class="a-card hoverable platform-card"
          :class="{ disabled: !p.available }"
        >
          <div class="platform-head">
            <PlatformIcon :platform="iconPlatform(p.platform)" :size="56" />
            <div class="platform-meta">
              <h3 class="platform-name">{{ p.name }}</h3>
              <div class="chip-row">
                <span v-if="!p.available" class="chip chip-gray">敬请期待</span>
                <template v-else-if="p.bound">
                  <span class="chip chip-green">已连接</span>
                  <span v-if="syncing === p.platform" class="chip chip-blue">同步中</span>
                  <span v-else-if="p.syncStatus === 3" class="chip chip-red">
                    {{ statusLabel(p.syncStatus) }}
                  </span>
                  <span v-if="p.syncedCount" class="chip chip-gray">已同步 {{ p.syncedCount }} 条</span>
                </template>
                <span v-else class="chip chip-gray">未绑定</span>
              </div>
            </div>
            <span v-if="syncing === p.platform" class="a-spinner platform-spinner"></span>
          </div>

          <p class="platform-desc">{{ p.description }}</p>
          <p v-if="p.bound && p.lastSyncTime" class="caption platform-sync-time">
            上次同步：{{ formatDateTime(p.lastSyncTime) }}
          </p>
          <p v-else-if="p.bound && p.message" class="caption platform-sync-time">
            {{ p.message }}
          </p>

          <!-- 操作区 -->
          <div class="platform-actions">
            <AButton v-if="!p.available" disabled>敬请期待</AButton>
            <AButton v-else-if="!p.bound" type="primary" @click="openBind(p)">连接账号</AButton>
            <template v-else>
              <AButton
                type="primary"
                :loading="syncing === p.platform"
                @click="handleSync(p)"
              >
                {{ syncing === p.platform ? '同步中…（首次同步耗时较长）' : '立即同步' }}
              </AButton>
              <AButton type="ghost-danger" :disabled="syncing === p.platform" @click="openUnbind(p)">
                解绑
              </AButton>
            </template>
          </div>
        </article>
      </div>
    </section>

    <!-- ============ B. AI 教练接入（MCP） ============ -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">AI 教练接入（MCP）</h2>
          <p class="section-desc">
            通过 MCP 协议把你的跑步数据安全地交给 AI 助手（如 Claude、Cursor 等）调用。
          </p>
        </div>
        <RouterLink
          class="text-accent"
          style="text-decoration: underline; font-size: 13px; white-space: nowrap"
          to="/profile#ai-config"
        >
          配置 AI 服务密钥 →
        </RouterLink>
      </div>

      <div class="card-grid-2">
        <!-- 令牌卡 -->
        <div class="a-card">
          <div class="card-title-row">
            <h3 class="card-title">访问令牌</h3>
            <span v-if="tokenMeta" class="chip chip-green">已生成</span>
            <span v-else class="chip chip-gray">尚未生成令牌</span>
          </div>

          <div v-if="tokenMeta" class="token-display">
            <p class="token-line">当前令牌：{{ tokenMeta.masked }}</p>
            <p class="caption token-expire">
              有效期至 {{ tokenMeta.expiresAt }}（{{ tokenMeta.expiresInDays }} 天）
            </p>
          </div>
          <div v-else class="token-display">
            <p class="caption">
              尚未生成令牌，点击下方按钮生成。生成后完整令牌只会出现在你的剪贴板中一次。
            </p>
          </div>

          <div class="token-actions">
            <AButton type="primary" size="lg" :loading="generatingToken" @click="generateToken">
              生成访问令牌
            </AButton>
            <AButton type="ghost" :disabled="generatingToken" @click="generateToken">
              重新生成
            </AButton>
          </div>

          <p class="caption token-note">
            完整令牌只在生成瞬间出现在剪贴板，页面不再保存明文；请自行妥善保管。
          </p>
        </div>

        <!-- 接入配置卡 -->
        <div class="a-card">
          <div class="card-title-row">
            <h3 class="card-title">接入配置</h3>
            <AButton type="ghost" size="sm" @click="copyConfig">复制配置</AButton>
          </div>

          <pre class="code-block">{{ mcpConfigSnippet }}</pre>

          <p class="caption config-note">
            将配置中的 YOUR_MCP_TOKEN 替换为剪贴板中的完整令牌，保存后重启 AI 客户端即可生效。
          </p>
        </div>
      </div>

      <!-- 可用工具列表 -->
      <div class="tools-block">
        <div class="tools-head">
          <div class="tools-title">
            <h3 class="card-title">可用工具</h3>
            <span class="chip chip-blue">MCP 工具</span>
          </div>
          <span class="caption">共 {{ tools.length }} 个工具</span>
        </div>

        <div v-if="toolsLoading" class="loading-row">
          <span class="a-spinner"></span>
          <span class="caption">正在加载工具列表…</span>
        </div>

        <div v-else-if="toolsError || tools.length === 0" class="a-empty">
          <div class="a-empty__text">工具列表加载失败</div>
          <div class="a-empty__sub">请稍后重试，或检查 MCP 服务是否可用</div>
        </div>

        <div v-else class="card-grid">
          <div v-for="(t, i) in tools" :key="i" class="a-card tool-card">
            <p class="tool-title">{{ t.title }}</p>
            <p v-if="t.detail" class="tool-desc">{{ t.detail }}</p>
          </div>
        </div>
      </div>

      <!-- 免责 / 帮助 -->
      <p class="caption disclaimer">
        安全提示：访问令牌等同于你的账号凭证，泄露可能导致训练数据被读取或被他人触发同步，请勿提交到公开仓库、聊天群或截图分享。
      </p>
    </section>

    <!-- ============ 绑定弹窗 ============ -->
    <AModal
      v-model="bindVisible"
      :title="bindTarget ? `连接 ${bindTarget.name}` : '连接账号'"
      size="md"
      :loading="binding"
      confirm-text="授权连接"
      cancel-text="取消"
      @confirm="handleBind"
      @cancel="bindVisible = false"
    >
      <div class="a-form">
        <div class="a-field">
          <label class="a-label">账号</label>
          <AInput
            v-model="bindForm.account"
            type="text"
            placeholder="邮箱 / 手机号"
            :disabled="binding"
            @enter="handleBind"
          />
          <p class="a-hint">用于登录 {{ bindTarget ? bindTarget.name : '' }} 的邮箱或手机号。</p>
        </div>

        <div class="a-field">
          <label class="a-label">密码</label>
          <AInput
            v-model="bindForm.password"
            type="password"
            placeholder="密码"
            :disabled="binding"
            @enter="handleBind"
          />
          <p class="a-hint">凭据仅用于换取平台令牌，不会明文长期存储。</p>
        </div>

        <div class="a-field">
          <label class="a-label">区域</label>
          <ASelect
            v-model="bindForm.region"
            :options="regionOptions"
            placeholder="请选择区域"
            :disabled="binding"
          />
          <p class="a-hint">选择账号所属的数据区域，选错会导致授权失败。</p>
        </div>
      </div>

      <template #footer>
        <div class="modal-foot">
          <AButton type="primary" size="lg" block :loading="binding" @click="handleBind">
            授权连接
          </AButton>
          <AButton block :disabled="binding" @click="bindVisible = false">取消</AButton>
        </div>
      </template>
    </AModal>

    <!-- ============ 解绑确认弹窗 ============ -->
    <AModal
      v-model="unbindVisible"
      title="确认解绑"
      size="md"
      danger
      :loading="unbinding"
      confirm-text="解绑"
      cancel-text="取消"
      @confirm="confirmUnbind"
      @cancel="unbindVisible = false"
    >
      <p class="unbind-text">
        确定解绑 {{ unbindTarget ? unbindTarget.name : '' }}？已同步的数据会保留，之后将不再自动同步。
      </p>
    </AModal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'
import ASelect from '@/components/ui/ASelect.vue'
import AModal from '@/components/ui/AModal.vue'
import PlatformIcon from '@/components/icons/PlatformIcon.vue'
import { platformApi } from '@/api/platform'
import { toast } from '@/utils/toast'
import { formatDateTime } from '@/utils/format'
import { getToken } from '@/utils/storage'
import type {
  McpR,
  McpTokenData,
  McpTokenMeta,
  McpToolItem,
  PlatformStatus,
} from '@/types/platform'

/* ============================================================
   A. 平台连接
   ============================================================ */
const platforms = ref<PlatformStatus[]>([])
const listLoading = ref(false)
const syncing = ref<string | null>(null)

const bindVisible = ref(false)
const binding = ref(false)
const bindTarget = ref<PlatformStatus | null>(null)
const bindForm = reactive({ account: '', password: '', region: 'cn' })

const unbindVisible = ref(false)
const unbinding = ref(false)
const unbindTarget = ref<PlatformStatus | null>(null)

const regionOptions: Array<{ value: string; label: string; hint?: string }> = [
  { value: 'cn', label: '中国', hint: '账号数据存储于中国区' },
  { value: 'eu', label: '欧洲' },
  { value: 'us', label: '美洲' },
]

onMounted(() => {
  void loadPlatforms()
  void loadTools()
})

async function loadPlatforms(): Promise<void> {
  listLoading.value = true
  try {
    platforms.value = await platformApi.listPlatforms()
  } catch {
    /* 失败已由 request 统一 toast */
  } finally {
    listLoading.value = false
  }
}

function openBind(p: PlatformStatus): void {
  bindForm.account = ''
  bindForm.password = ''
  bindForm.region = 'cn'
  bindTarget.value = p
  bindVisible.value = true
}

async function handleBind(): Promise<void> {
  if (binding.value || !bindTarget.value) return
  if (!bindForm.account || !bindForm.password) {
    toast.info('请填写账号和密码')
    return
  }
  binding.value = true
  try {
    await platformApi.bind(bindTarget.value.platform, { ...bindForm })
    toast.success('绑定成功，现在可以点击立即同步')
    bindVisible.value = false
    await loadPlatforms()
  } catch {
    /* 失败已由 request 统一 toast */
  } finally {
    binding.value = false
  }
}

async function handleSync(p: PlatformStatus): Promise<void> {
  if (syncing.value) return
  syncing.value = p.platform
  try {
    const res = await platformApi.sync(p.platform)
    toast.success(res?.message || '同步完成')
    await loadPlatforms()
  } catch {
    /* 失败已由 request 统一 toast */
  } finally {
    syncing.value = null
  }
}

function openUnbind(p: PlatformStatus): void {
  unbindTarget.value = p
  unbindVisible.value = true
}

async function confirmUnbind(): Promise<void> {
  if (unbinding.value || !unbindTarget.value) return
  unbinding.value = true
  try {
    await platformApi.unbind(unbindTarget.value.platform)
    toast.success('已解绑')
    unbindVisible.value = false
    await loadPlatforms()
  } catch {
    /* 失败已由 request 统一 toast */
  } finally {
    unbinding.value = false
  }
}

/** 平台 id → 图标变体（苹果健康 → 苹果） */
function iconPlatform(id: string): string {
  return id === 'APPLE_HEALTH' ? 'APPLE' : id
}

function statusLabel(s: number): string {
  return ['待同步', '同步中', '已同步', '同步失败'][s] || '未知状态'
}

/* ============================================================
   B. MCP 令牌安全流程（完整 token 不落盘、不进响应式状态、不渲染）
   ============================================================ */
const MCP_META_KEY = 'runai-mcp-meta'

const tokenMeta = ref<McpTokenMeta | null>(readTokenMeta())
const generatingToken = ref(false)

function readTokenMeta(): McpTokenMeta | null {
  try {
    const raw = localStorage.getItem(MCP_META_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<McpTokenMeta>
    if (typeof parsed.masked !== 'string' || typeof parsed.expiresAt !== 'string') return null
    return {
      masked: parsed.masked,
      expiresAt: parsed.expiresAt,
      expiresInDays: typeof parsed.expiresInDays === 'number' ? parsed.expiresInDays : 0,
    }
  } catch {
    return null
  }
}

async function generateToken(): Promise<void> {
  if (generatingToken.value) return
  generatingToken.value = true
  try {
    const res = await fetch('/api/mcp/token', {
      method: 'POST',
      headers: { Authorization: `Bearer ${getToken() ?? ''}` },
    })
    const body = (await res.json()) as McpR<McpTokenData | null>
    if (!res.ok || body.code !== 200 || !body.data || typeof body.data.token !== 'string') {
      throw new Error(body.msg || '令牌生成失败')
    }

    // 用完即弃的局部变量：只用于复制与生成掩码，绝不写入 ref / localStorage / DOM
    const rawToken = body.data.token

    try {
      await navigator.clipboard.writeText(rawToken)
    } catch {
      toast.error('复制失败，请手动复制')
    }

    const meta: McpTokenMeta = {
      masked: rawToken.slice(0, 8) + '••••••••' + rawToken.slice(-6),
      expiresAt: body.data.expiresAt,
      expiresInDays: body.data.expiresInDays,
    }
    localStorage.setItem(MCP_META_KEY, JSON.stringify(meta))
    tokenMeta.value = meta

    toast.success('访问令牌已生成，完整令牌已复制到剪贴板（仅显示这一次的机会已结束）')
  } catch (e) {
    toast.error(e instanceof Error && e.message ? e.message : '令牌生成失败')
  } finally {
    generatingToken.value = false
  }
}

/* ---- 接入配置（永远使用占位符，不放真实令牌） ---- */
const mcpConfigSnippet = `{
  "mcpServers": {
    "run-ai": {
      "url": "http://localhost:2021/api/mcp",
      "headers": { "Authorization": "Bearer YOUR_MCP_TOKEN" }
    }
  }
}`

async function copyConfig(): Promise<void> {
  try {
    await navigator.clipboard.writeText(mcpConfigSnippet)
    toast.success('配置已复制')
  } catch {
    toast.error('复制失败，请手动复制')
  }
}

/* ---- 可用工具（只展示 description 的中文短名，禁止出现方法名） ---- */
const tools = ref<McpToolItem[]>([])
const toolsLoading = ref(true)
const toolsError = ref(false)

async function loadTools(): Promise<void> {
  toolsLoading.value = true
  toolsError.value = false
  try {
    const res = await fetch('/api/mcp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'tools/list' }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const body = (await res.json()) as { result?: { tools?: Array<{ description?: string }> } }
    const list = body.result?.tools ?? []
    tools.value = list.map((item) => splitToolDesc(item.description ?? ''))
  } catch {
    tools.value = []
    toolsError.value = true
  } finally {
    toolsLoading.value = false
  }
}

/** 中文短名 = 第一个 '：' 或 '。' 之前的部分；无分隔符则整句为短名、正文省略 */
function splitToolDesc(desc: string): McpToolItem {
  const text = desc.trim()
  if (!text) return { title: '未命名工具', detail: '' }

  let idx = -1
  for (const sep of ['：', '。']) {
    const i = text.indexOf(sep)
    if (i >= 0 && (idx < 0 || i < idx)) idx = i
  }
  if (idx < 0) return { title: text, detail: '' }
  return { title: text.slice(0, idx).trim(), detail: text.slice(idx + 1).trim() }
}
</script>

<style scoped lang="scss">
/* ---- 加载行 ---- */
.loading-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 56px 0;
}

/* ---- 平台卡片 ---- */
.platform-card {
  display: flex;
  flex-direction: column;
}

.platform-card.disabled {
  opacity: 0.72;
}

.platform-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.platform-meta {
  min-width: 0;
  flex: 1;
}

.platform-name {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--a-text);
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.platform-spinner {
  flex: none;
}

.platform-desc {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.55;
  color: var(--a-text-secondary);
}

.platform-sync-time {
  margin-top: 10px;
}

.platform-actions {
  margin-top: auto;
  padding-top: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ---- 卡片标题行 ---- */
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.card-title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.012em;
  color: var(--a-text);
}

/* ---- 令牌卡 ---- */
.token-display {
  background: var(--a-fill);
  border-radius: 14px;
  padding: 16px 18px;
}

.token-line {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--a-text);
  word-break: break-all;
}

.token-expire {
  margin: 8px 0 0;
}

.token-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.token-note {
  margin: 14px 0 0;
  line-height: 1.6;
}

.config-note {
  margin: 12px 0 0;
  line-height: 1.6;
}

/* ---- 工具列表 ---- */
.tools-block {
  margin-top: 26px;
}

.tools-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.tools-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tool-card {
  padding: 18px 20px;
}

.tool-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--a-text);
}

.tool-desc {
  margin: 8px 0 0;
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--a-text-secondary);
}

/* ---- 免责说明 ---- */
.disclaimer {
  margin: 26px auto 0;
  max-width: 720px;
  text-align: center;
  line-height: 1.7;
}

/* ---- 弹窗 ---- */
.modal-foot {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.unbind-text {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--a-text-secondary);
}
</style>
