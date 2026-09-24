<template>
  <div class="app-shell">
    <!-- Apple 官网式半透明毛玻璃导航栏 -->
    <header class="nav-bar">
      <div class="nav-inner">
        <router-link to="/dashboard" class="nav-logo">
          <span class="nav-logo-mark">P</span>ulverize
        </router-link>

        <nav class="nav-links">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="nav-user">
          <!-- 暗黑模式切换 -->
          <button
            type="button"
            class="nav-icon-btn"
            :title="theme === 'dark' ? '切换为浅色模式' : '切换为暗黑模式'"
            @click="onToggleTheme"
          >
            <!-- 太阳 -->
            <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="12" cy="12" r="4.2" />
              <path d="M12 2.5v2.4M12 19.1v2.4M2.5 12h2.4M19.1 12h2.4M5 5l1.7 1.7M17.3 17.3L19 19M19 5l-1.7 1.7M6.7 17.3L5 19" />
            </svg>
            <!-- 月亮 -->
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.5 14.2A8.5 8.5 0 1 1 9.8 3.5a7 7 0 0 0 10.7 10.7z" />
            </svg>
          </button>

          <!-- 头像下拉 -->
          <div ref="menuRoot" class="a-dropdown">
            <button type="button" class="avatar-btn" @click.stop="menuOpen = !menuOpen">
              <span class="avatar">
                {{ avatarText }}
              </span>
            </button>

              <div v-if="menuOpen" class="a-dropdown__menu" @click.stop>
                <div class="a-dropdown__head">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}
                </div>
                <button type="button" class="a-dropdown__item" @click="go('/profile')">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="3.6" /><path d="M4.5 20c1.4-3.4 4.2-5 7.5-5s6.1 1.6 7.5 5" stroke-linecap="round" /></svg>
                  个人资料
                </button>
                <button type="button" class="a-dropdown__item" @click="go('/connect')">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M9 15l6-6M7.5 6.5l1.8-1.8a4.6 4.6 0 0 1 6.5 6.5l-1.8 1.8M16.5 17.5l-1.8 1.8a4.6 4.6 0 0 1-6.5-6.5l1.8-1.8" /></svg>
                  连接与 AI 工具
                </button>
                <div class="a-dropdown__divider" />
                <button type="button" class="a-dropdown__item a-dropdown__item--danger" @click="logout">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 8V5.8A1.8 1.8 0 0 0 12.7 4H6.3A1.8 1.8 0 0 0 4.5 5.8v12.4A1.8 1.8 0 0 0 6.3 20h6.4a1.8 1.8 0 0 0 1.8-1.8V16M9.5 12H20M17 9l3 3-3 3" /></svg>
                  退出登录
                </button>
              </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="app-main">
      <div class="a-container">
        <router-view v-slot="{ Component }">
          <!--
            不用 mode="out-in"：out-in 在快速连续导航时可能状态错乱，导致新页面
            永不入场（页面空白）。默认模式下新页面无条件立即插入，配合
            .a-container 的 grid 同格叠放（旧页淡出、新页淡入），无布局跳动。
            :duration 用定时器兜底保证过渡必然结束，不依赖 transitionend。
          -->
          <transition name="page-fade" :duration="250">
            <!-- key 隔离各页面实例，避免状态残留互相影响 -->
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getTheme, toggleTheme, type ThemeMode } from '@/utils/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { path: '/dashboard', label: '数据看板' },
  { path: '/activities', label: '跑步记录' },
  { path: '/analysis', label: '跑步分析' },
  { path: '/plan', label: '训练计划' },
  { path: '/goal', label: '运动目标' },
  { path: '/report', label: '训练报告' },
  { path: '/connect', label: '连接' },
]

function isActive(path: string): boolean {
  if (path === '/dashboard') return route.path === '/dashboard'
  return route.path.startsWith(path)
}

/* ---- 暗黑模式 ---- */
const theme = ref<ThemeMode>(getTheme())
function onToggleTheme() {
  theme.value = toggleTheme()
}

/* ---- 头像菜单 ---- */
const menuOpen = ref(false)
const menuRoot = ref<HTMLElement | null>(null)

const avatarText = computed(() => {
  const u = userStore.userInfo
  return (u?.nickname || u?.username || 'U').charAt(0).toUpperCase()
})

function go(path: string) {
  menuOpen.value = false
  router.push(path)
}

function logout() {
  menuOpen.value = false
  userStore.logout()
  router.push('/login')
}

function onDocClick(e: MouseEvent) {
  if (menuOpen.value && menuRoot.value && !menuRoot.value.contains(e.target as Node)) {
    menuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  // 首次进入拉取用户信息（本地可能只有缓存的旧资料）
  if (!userStore.userInfo) userStore.fetchProfile().catch(() => {})
})
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ---- 毛玻璃顶栏 ---- */
.nav-bar {
  position: sticky;
  top: var(--client-tb-h, 0px);
  z-index: 100;
  background: var(--a-nav-bg);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--a-divider);
}

.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-logo {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--a-text);
  text-decoration: none;
  flex: none;
}
.nav-logo-mark {
  color: var(--a-blue);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.nav-links::-webkit-scrollbar {
  display: none;
}

.nav-link {
  padding: 7px 13px;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 500;
  color: var(--a-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.18s ease;
}
.nav-link:hover {
  color: var(--a-text);
  background: var(--a-fill);
}
.nav-link.active {
  /* 浅色模式 = 深底浅字，暗黑模式 = 浅底深字（双向都用变量反色，避免暗黑下浅底白字） */
  color: var(--a-bg);
  background: var(--a-text);
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

.nav-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--a-text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s ease;
}
.nav-icon-btn:hover {
  background: var(--a-fill);
  color: var(--a-text);
}

.avatar-btn {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: inline-flex;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--a-blue);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

/* ---- 内容区 ---- */
.app-main {
  flex: 1;
  width: 100%;
}

/*
  页面切换过渡期间新旧两个 .page 同时存在于 DOM，
  用 grid 让它们叠在同一格：容器高度取两者较大值，
  不会出现「旧页在上、新页在下」的双倍高度跳动；
  旧页淡出时在下层，新页淡入在上层（DOM 顺序靠后）。
*/
.a-container {
  display: grid;
}
.a-container > * {
  grid-area: 1 / 1;
  min-width: 0;
}
</style>
