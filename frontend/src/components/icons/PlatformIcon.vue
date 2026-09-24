<template>
  <span
    class="platform-icon"
    :style="{ width: `${size}px`, height: `${size}px`, borderRadius: `${size * 0.225}px` }"
    :title="platform"
  >
    <svg viewBox="0 0 40 40" :width="size" :height="size" aria-hidden="true">
      <!-- COROS App：黑底白字标 -->
      <template v-if="is('COROS')">
        <rect width="40" height="40" rx="9" fill="#111111" />
        <text
          x="20"
          y="24.6"
          text-anchor="middle"
          fill="#fff"
          font-size="8.6"
          font-weight="800"
          letter-spacing="0.2"
          font-family="inherit"
        >
          COROS
        </text>
      </template>

      <!-- Strava App：橙底白色折线标 -->
      <template v-else-if="is('STRAVA')">
        <rect width="40" height="40" rx="9" fill="#FC4C02" />
        <path
          d="M23.6 30.2l-4.1-8.1h-6l7.3 14.4 7.3-14.4h-6l-4.1 8.1z M11.8 18.7h6l2.8 5.5 2.8-5.5h6L18.7 4l-6.9 14.7z"
          fill="#fff"
          transform="scale(0.86) translate(3.2 0.5)"
        />
      </template>

      <!-- Garmin Connect App：白底蓝色三角标 -->
      <template v-else-if="is('GARMIN')">
        <rect width="40" height="40" rx="9" fill="#ffffff" />
        <path d="M20 8.5L31.5 30.5H8.5L20 8.5z" fill="#0b5cad" />
        <path d="M20 16.5l6.4 12H13.6l6.4-12z" fill="#ffffff" />
      </template>

      <!-- Apple 健身 App：黑底三色活动圆环 -->
      <template v-else-if="is('APPLE')">
        <rect width="40" height="40" rx="9" fill="#000000" />
        <g transform="rotate(-90 20 20)" fill="none" stroke-linecap="round">
          <circle cx="20" cy="20" r="11" stroke="#fa114f" stroke-width="4" stroke-dasharray="48 21" />
          <circle cx="20" cy="20" r="8" stroke="#92e82a" stroke-width="4" stroke-dasharray="36 14" />
          <circle cx="20" cy="20" r="5" stroke="#1ad5fa" stroke-width="4" stroke-dasharray="22 9" />
        </g>
      </template>

      <!-- 华为运动健康 App：浅底红花瓣标 -->
      <template v-else-if="is('HUAWEI')">
        <rect width="40" height="40" rx="9" fill="#f2f2f4" />
        <g fill="#C7000B" transform="translate(20 20)">
          <ellipse cx="0" cy="-8.4" rx="4.4" ry="7.6" transform="rotate(0)" />
          <ellipse cx="0" cy="-8.4" rx="4.4" ry="7.6" transform="rotate(72)" />
          <ellipse cx="0" cy="-8.4" rx="4.4" ry="7.6" transform="rotate(144)" />
          <ellipse cx="0" cy="-8.4" rx="4.4" ry="7.6" transform="rotate(216)" />
          <ellipse cx="0" cy="-8.4" rx="4.4" ry="7.6" transform="rotate(288)" />
        </g>
      </template>

      <!-- 其它平台：灰底首字母 -->
      <template v-else>
        <rect width="40" height="40" rx="9" fill="#8e8e93" />
        <text x="20" y="26.4" text-anchor="middle" fill="#fff" font-size="18" font-weight="700" font-family="inherit">
          {{ initial }}
        </text>
      </template>

      <!-- iOS 风格顶部光泽（固定低透明白，避免渐变 id 多实例冲突） -->
      <path d="M0,0 H40 V13.5 C30.5,21.5 9.5,21.5 0,13.5 Z" fill="rgba(255, 255, 255, 0.1)" />
    </svg>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{ platform: string; size?: number }>(),
  { size: 36 }
)

function is(name: string) {
  return (props.platform || '').toUpperCase().replace(/[^A-Z]/g, '') === name
}

const initial = computed(() => (props.platform || '?').charAt(0).toUpperCase())
</script>

<style scoped>
.platform-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex: none;
  box-shadow: inset 0 0 0 0.5px rgba(0, 0, 0, 0.1);
  vertical-align: middle;
}
.platform-icon svg {
  display: block;
}
</style>
