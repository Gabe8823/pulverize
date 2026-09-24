<template>
  <div class="page auth-page">
    <div class="auth-wrap">
      <div class="auth-brand">
        <span class="auth-brand__mark">P</span><span class="auth-brand__rest">ulverize</span>
      </div>

      <div class="a-card auth-card">
        <div class="page-head auth-head">
          <p class="eyebrow">智能跑步数据分析平台</p>
          <h1 class="headline">欢迎回来</h1>
          <p class="subhead">登录你的账号，继续查看今天的训练数据。</p>
        </div>

        <form class="a-form auth-form" @submit.prevent="handleLogin">
          <div class="a-field">
            <label class="a-label" for="login-username">账号</label>
            <AInput
              id="login-username"
              v-model="form.username"
              placeholder="请输入账号"
              :disabled="loading"
              :maxlength="30"
              @enter="handleLogin"
            />
          </div>

          <div class="a-field">
            <label class="a-label" for="login-password">密码</label>
            <AInput
              id="login-password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :disabled="loading"
              @enter="handleLogin"
            />
          </div>

          <AButton html-type="submit" type="primary" size="lg" block :loading="loading">
            登录
          </AButton>
        </form>

        <p class="auth-footer">
          还没有账号？
          <router-link class="a-link" to="/register">立即注册</router-link>
        </p>
      </div>

      <p class="caption auth-caption">演示账号：123 / [REDACTED]</p>
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

const form = reactive({
  username: '',
  password: '',
})

async function handleLogin() {
  if (loading.value) return

  if (!form.username.trim()) {
    toast.error('请输入用户名')
    return
  }
  if (!form.password) {
    toast.error('请输入密码')
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username.trim(), form.password)
    toast.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误已由 axios 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--a-bg);
  padding: 48px 24px;
}

.auth-wrap {
  width: 100%;
  max-width: 440px;
  text-align: center;
}

.auth-brand {
  margin-bottom: 26px;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--a-text);
}
.auth-brand__mark {
  color: var(--a-blue);
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
</style>
