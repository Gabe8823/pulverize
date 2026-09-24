<template>
  <div class="page auth-page">
    <div class="auth-wrap">
      <div class="auth-brand">
        <span class="auth-brand__mark">P</span><span class="auth-brand__rest">ulverize</span>
      </div>

      <div class="a-card auth-card">
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
  if (!validate()) return

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
