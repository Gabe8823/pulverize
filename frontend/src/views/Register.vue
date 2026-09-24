<template>
  <div class="page auth-page">
    <div class="auth-aurora" aria-hidden="true">
      <i class="auth-blob auth-blob--1"></i>
      <i class="auth-blob auth-blob--2"></i>
    </div>

    <div class="auth-wrap">
      <div class="auth-brand">
        <span class="auth-brand__pill">P</span>
        <span class="auth-brand__rest">ulverize</span>
      </div>

      <div class="a-card auth-card" :class="{ 'a-shake': shaking }">
        <div class="page-head auth-head">
          <p class="eyebrow">智能跑步数据分析平台</p>
          <h1 class="headline">创建账号</h1>
          <p class="subhead">几步之内完成注册，马上开始科学训练。</p>
        </div>

        <form class="a-form auth-form" @submit.prevent="handleRegister">
          <div class="a-field">
            <label class="a-label" for="reg-username">用户名</label>
            <AInput
              id="reg-username"
              v-model="form.username"
              placeholder="字母、数字、下划线，3-30 个字符"
              :disabled="loading"
              :maxlength="30"
            />
            <p class="a-hint">只能包含字母、数字和下划线，长度 3-30 个字符</p>
          </div>

          <div class="a-field">
            <label class="a-label" for="reg-nickname">昵称（可选）</label>
            <AInput
              id="reg-nickname"
              v-model="form.nickname"
              placeholder="用于展示的昵称"
              :disabled="loading"
              :maxlength="30"
            />
          </div>

          <div class="a-field">
            <label class="a-label" for="reg-password">密码</label>
            <AInput
              id="reg-password"
              v-model="form.password"
              type="password"
              placeholder="密码（至少 8 位）"
              :disabled="loading"
              :maxlength="50"
            />
          </div>

          <div class="a-field">
            <label class="a-label" for="reg-confirm">确认密码</label>
            <AInput
              id="reg-confirm"
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :disabled="loading"
              :maxlength="50"
              @enter="handleRegister"
            />
          </div>

          <AButton html-type="submit" type="primary" size="lg" block :loading="loading">
            注册
          </AButton>
        </form>

        <p class="auth-footer">
          已有账号？
          <router-link class="a-link" to="/login">立即登录</router-link>
        </p>
      </div>

      <p class="caption auth-caption">本地演示环境 · 数据仅保存在你的机器上</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { toast } from '@/utils/toast'
import AButton from '@/components/ui/AButton.vue'
import AInput from '@/components/ui/AInput.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

/** 错误抖动：校验失败或注册出错时给卡片挂 0.36s 抖动反馈 */
const shaking = ref(false)
function shake(): void {
  shaking.value = false
  window.requestAnimationFrame(() => {
    shaking.value = true
  })
  window.setTimeout(() => {
    shaking.value = false
  }, 420)
}

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

function validate(): boolean {
  const username = form.username.trim()

  if (!username) {
    toast.error('请输入用户名')
    return false
  }
  if (username.length < 3 || username.length > 30) {
    toast.error('长度 3-30 个字符')
    return false
  }
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    toast.error('只能包含字母、数字和下划线')
    return false
  }
  if (!form.password) {
    toast.error('请输入密码')
    return false
  }
  if (form.password.length < 8 || form.password.length > 50) {
    toast.error('密码长度 8-50 个字符')
    return false
  }
  if (!form.confirmPassword) {
    toast.error('请确认密码')
    return false
  }
  if (form.confirmPassword !== form.password) {
    toast.error('两次输入的密码不一致')
    return false
  }
  return true
}

async function handleRegister() {
  if (loading.value) return
  if (!validate()) {
    shake()
    return
  }

  loading.value = true
  try {
    await userStore.register(
      form.username.trim(),
      form.password,
      form.nickname.trim() || undefined
    )
    toast.success('注册成功')
    router.push('/dashboard')
  } catch {
    // 错误已由 axios 拦截器处理，这里补一道卡片抖动反馈
    shake()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--a-bg);
  padding: 48px 24px;
  overflow: hidden;
}

/* ---- aurora 氛围背景：两枚品牌红光斑缓移（唯一允许的无限氛围动效） ---- */
.auth-aurora {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.auth-blob {
  position: absolute;
  width: 62vmax;
  height: 62vmax;
  border-radius: 50%;
  filter: blur(64px);
  opacity: 0.6;
}
.auth-blob--1 {
  top: -20%;
  left: -14%;
  background: radial-gradient(circle at 42% 42%, var(--a-blue-ring), transparent 65%);
  animation: auth-drift-1 22s ease-in-out infinite alternate;
}
.auth-blob--2 {
  right: -16%;
  bottom: -24%;
  background: radial-gradient(circle at 58% 58%, var(--a-blue-ring), transparent 65%);
  animation: auth-drift-2 27s ease-in-out infinite alternate;
}
@keyframes auth-drift-1 {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(9%, 7%) scale(1.14);
  }
}
@keyframes auth-drift-2 {
  from {
    transform: translate(0, 0) scale(1.06);
  }
  to {
    transform: translate(-8%, -6%) scale(0.94);
  }
}

.auth-wrap {
  width: 100%;
  max-width: 440px;
  text-align: center;
  animation: auth-rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes auth-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ---- 品牌胶囊入场 ---- */
.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  margin-bottom: 26px;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.auth-brand__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  min-width: 44px;
  padding: 0 12px;
  border-radius: 980px;
  background: var(--a-blue);
  color: #fff;
  font-size: 27px;
  font-weight: 700;
  box-shadow: 0 8px 24px var(--a-blue-ring);
  animation: auth-pill-pop 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.auth-brand__rest {
  color: var(--a-text);
  animation: auth-fade-in 0.5s 0.24s ease both;
}
@keyframes auth-pill-pop {
  0% {
    opacity: 0;
    transform: scale(0.4);
  }
  62% {
    opacity: 1;
    transform: scale(1.07);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
@keyframes auth-fade-in {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.auth-card {
  padding: 40px 36px 32px;
  text-align: left;
}

.auth-head {
  padding: 0 0 8px;
}

.auth-form {
  margin-top: 30px;
  gap: 20px;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14.5px;
  color: var(--a-text-secondary);
}

.auth-caption {
  margin-top: 22px;
}

@media (prefers-reduced-motion: reduce) {
  .auth-blob,
  .auth-wrap,
  .auth-brand__pill,
  .auth-brand__rest {
    animation: none !important;
  }
}
</style>
