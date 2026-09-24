<template>
  <header class="client-tb">
    <span class="client-tb-logo">R</span>
    <span class="client-tb-name">RunAI</span>
    <span class="client-tb-tag">桌面版</span>

    <div class="client-tb-bg">
      <button class="client-tb-btn" type="button" title="更换客户端背景" @click.stop="open = !open">
        <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            d="M8 1.6c-3.5 0-6.3 2.6-6.3 6 0 1.9 1.1 3.3 2.7 3.3.9 0 1.5-.5 1.5-1.3 0-.8-.6-1-.6-1.7 0-.6.5-1.1 1.2-1.1h1.4c2.3 0 4.2-1.5 4.2-3.5C12.1 2.3 10.4 1.6 8 1.6Z"
            stroke="currentColor"
            stroke-width="1.3"
          />
          <circle cx="5.1" cy="6.7" r="0.9" fill="currentColor" />
          <circle cx="8.3" cy="5" r="0.9" fill="currentColor" />
          <circle cx="10.9" cy="7" r="0.9" fill="currentColor" />
        </svg>
        <span>背景</span>
      </button>

      <div v-if="open" class="client-tb-pop" @click.stop>
        <div class="client-tb-pop-title">客户端背景</div>
        <button
          v-for="p in presets"
          :key="p.id"
          class="client-tb-item"
          :class="{ on: current === p.id }"
          type="button"
          @click="pick(p.id)"
        >
          <span class="client-tb-sw" :style="{ background: p.swatch }" />
          <span>{{ p.label }}</span>
          <span v-if="current === p.id" class="client-tb-check">✓</span>
        </button>
        <button
          class="client-tb-item"
          :class="{ on: isImageBg }"
          type="button"
          @click="pickImage"
        >
          <span class="client-tb-sw" :style="{ background: checker }" />
          <span>自定义图片…</span>
          <span v-if="isImageBg" class="client-tb-check">✓</span>
        </button>
        <input ref="fileInput" hidden type="file" accept="image/*" @change="onFile" />
      </div>
    </div>

    <span class="client-tb-drag" />
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { applyBg, fileToBg, getBg } from '../../utils/clientTheme'

const open = ref(false)
const current = ref('default')
const fileInput = ref<HTMLInputElement | null>(null)

const presets = [
  { id: 'default', label: '默认（跟随主题）', swatch: 'linear-gradient(135deg, #f5f5f7, #ffffff)' },
  { id: 'red-black', label: '红黑', swatch: 'linear-gradient(135deg, #e30613, #0b0b0d)' },
  { id: 'graphite', label: '墨石灰', swatch: 'linear-gradient(135deg, #3a3a3e, #131315)' },
  { id: 'cloud', label: '云白', swatch: 'linear-gradient(135deg, #f7f7f9, #e0e0e6)' },
]
const checker =
  'repeating-conic-gradient(#3c3c42 0% 25%, #26262b 0% 50%) 0 / 8px 8px'

const isImageBg = computed(() => current.value.startsWith('image:'))

function onDoc(e: MouseEvent) {
  const t = e.target as HTMLElement | null
  if (open.value && !t?.closest?.('.client-tb-bg')) open.value = false
}

onMounted(() => {
  current.value = getBg()
  document.addEventListener('click', onDoc, true)
})
onBeforeUnmount(() => document.removeEventListener('click', onDoc, true))

function pick(id: string) {
  applyBg(id)
  current.value = id
  open.value = false
}

function pickImage() {
  fileInput.value?.click()
}

async function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const value = await fileToBg(file)
    applyBg(value)
    current.value = value
  } catch {
    window.alert('图片处理失败：请换一张不超过 3MB 的常见格式图片（JPG/PNG/WebP）')
  }
  open.value = false
}
</script>

<style scoped>
.client-tb {
  position: relative;
  z-index: 2000;
  box-sizing: border-box;
  height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 150px 0 16px;
  background: #16161a;
  color: #f5f5f7;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  -webkit-app-region: drag;
  user-select: none;
  font-family: var(--a-font, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif);
}
.client-tb-logo {
  width: 20px;
  height: 20px;
  border-radius: 5px;
  background: #e30613;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.client-tb-name {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.2px;
}
.client-tb-tag {
  font-size: 11px;
  color: #9a9aa2;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  padding: 1px 8px;
}
.client-tb-bg {
  position: relative;
  margin-left: 6px;
  -webkit-app-region: no-drag;
}
.client-tb-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #c9c9d1;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}
.client-tb-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.client-tb-pop {
  position: absolute;
  top: 32px;
  left: 0;
  width: 208px;
  padding: 8px;
  background: #202024;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.5);
  -webkit-app-region: no-drag;
}
.client-tb-pop-title {
  font-size: 11px;
  color: #8f8f96;
  padding: 4px 8px 6px;
}
.client-tb-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 7px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #e6e6ea;
  font-size: 12.5px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.client-tb-item:hover {
  background: rgba(255, 255, 255, 0.08);
}
.client-tb-item.on {
  background: rgba(227, 6, 19, 0.22);
}
.client-tb-check {
  margin-left: auto;
  color: #ff6b62;
  font-size: 12px;
}
.client-tb-sw {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex: 0 0 16px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}
.client-tb-drag {
  flex: 1;
  height: 100%;
  -webkit-app-region: drag;
}
</style>
