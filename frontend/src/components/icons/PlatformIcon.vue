<template>
  <span
    class="platform-icon"
    :style="{ width: `${size}px`, height: `${size}px`, borderRadius: `${size * 0.24}px` }"
    :title="platform"
  >
    <svg :viewBox="'0 0 40 40'" :width="size" :height="size" aria-hidden="true">
      <!-- COROS：黑底白字 -->
      <template v-if="is('COROS')">
        <rect width="40" height="40" :rx="size * 0.24" fill="#111111" />
        <text x="20" y="24.6" text-anchor="middle" fill="#fff" font-size="8.6" font-weight="800" letter-spacing="0.2" font-family="inherit">COROS</text>
      </template>

      <!-- Strava：橙底白双向折线 -->
      <template v-else-if="is('STRAVA')">
        <rect width="40" height="40" :rx="size * 0.24" fill="#FC4C02" />
        <path
          d="M23.6 30.2l-4.1-8.1h-6l7.3 14.4 7.3-14.4h-6l-4.1 8.1z M11.8 18.7h6l2.8 5.5 2.8-5.5h6L18.7 4l-6.9 14.7z"
          fill="#fff"
          transform="scale(0.86) translate(3.2 0.5)"
        />
      </template>

      <!-- Garmin：蓝底白三角 -->
      <template v-else-if="is('GARMIN')">
        <rect width="40" height="40" :rx="size * 0.24" fill="#007cc3" />
        <path d="M20 8.5L31.5 30.5H8.5L20 8.5z" fill="#fff" />
        <path d="M20 16.5l6.4 12H13.6l6.4-12z" fill="#007cc3" />
      </template>

      <!-- Apple：黑底白苹果 -->
      <template v-else-if="is('APPLE')">
        <rect width="40" height="40" :rx="size * 0.24" fill="#000000" />
        <path
          d="M26.4 20.9c0-3.1 2.5-4.6 2.6-4.7-1.4-2.1-3.7-2.4-4.5-2.4-1.9-.2-3.7 1.1-4.6 1.1-1 0-2.4-1.1-4-1-2 0-3.9 1.2-5 3-2.1 3.7-.5 9.2 1.5 12.2 1 1.5 2.2 3.1 3.8 3 1.5-.1 2.1-1 3.9-1s2.3 1 4 1c1.6 0 2.7-1.5 3.7-3 1.2-1.7 1.6-3.4 1.7-3.5-.1 0-3.2-1.2-3.1-4.7z M23.3 11.8c.8-1 1.4-2.4 1.2-3.8-1.2.1-2.7.8-3.5 1.8-.8.9-1.5 2.3-1.3 3.7 1.4.1 2.8-.7 3.6-1.7z"
          fill="#fff"
        />
      </template>

      <!-- 华为：浅底红花瓣 -->
      <template v-else-if="is('HUAWEI')">
        <rect width="40" height="40" :rx="size * 0.24" fill="#f2f2f4" />
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
        <rect width="40" height="40" :rx="size * 0.24" fill="#8e8e93" />
        <text x="20" y="26.4" text-anchor="middle" fill="#fff" font-size="18" font-weight="700" font-family="inherit">
          {{ initial }}
        </text>
      </template>
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
  box-shadow: inset 0 0 0 0.5px rgba(0, 0, 0, 0.08);
  vertical-align: middle;
}
.platform-icon svg {
  display: block;
}
</style>
