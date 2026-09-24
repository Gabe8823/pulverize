<template>
  <div class="a-skeleton-wrap" aria-hidden="true">
    <div
      v-for="i in lines"
      :key="i"
      class="a-skeleton"
      :style="{
        height: height + 'px',
        width: i === lines ? lastWidth : '100%',
        borderRadius: radius + 'px',
        marginTop: i === 1 ? '0' : gap + 'px',
      }"
    />
  </div>
</template>

<script setup lang="ts">
/** 骨架屏占位条（复用 theme.scss 的 .a-skeleton shimmer 动效，reduced-motion 已关停） */
withDefaults(
  defineProps<{
    /** 骨架条数量 */
    lines?: number
    /** 每条高度（px） */
    height?: number
    /** 最后一条宽度（收尾视觉，避免整齐呆板） */
    lastWidth?: string
    /** 条间距（px） */
    gap?: number
    /** 圆角（px） */
    radius?: number
  }>(),
  {
    lines: 3,
    height: 14,
    lastWidth: '62%',
    gap: 10,
    radius: 8,
  }
)
</script>

<style scoped>
.a-skeleton-wrap {
  display: flex;
  flex-direction: column;
}
</style>
