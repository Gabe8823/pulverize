<template>
  <nav class="a-pagination" aria-label="分页">
    <span v-if="showTotal" class="a-pagination__total">共 {{ total }} 条</span>
    <button type="button" class="a-page-btn" :disabled="page <= 1" @click="go(page - 1)">‹</button>
    <template v-for="(item, i) in pages" :key="i">
      <span v-if="item === '…'" class="a-page-ellipsis">…</span>
      <button
        v-else
        type="button"
        class="a-page-btn"
        :class="{ 'is-active': item === page }"
        @click="go(item as number)"
      >
        {{ item }}
      </button>
    </template>
    <button type="button" class="a-page-btn" :disabled="page >= pageCount" @click="go(page + 1)">›</button>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: number
    total: number
    pageSize?: number
    showTotal?: boolean
  }>(),
  { pageSize: 10, showTotal: true }
)

const emit = defineEmits<{ 'update:modelValue': [page: number] }>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const page = computed(() => props.modelValue)

const pages = computed<Array<number | '…'>>(() => {
  const n = pageCount.value
  const cur = page.value
  if (n <= 7) return Array.from({ length: n }, (_, i) => i + 1)
  const items: Array<number | '…'> = [1]
  const start = Math.max(2, cur - 1)
  const end = Math.min(n - 1, cur + 1)
  if (start > 2) items.push('…')
  for (let i = start; i <= end; i++) items.push(i)
  if (end < n - 1) items.push('…')
  items.push(n)
  return items
})

function go(p: number) {
  if (p < 1 || p > pageCount.value || p === page.value) return
  emit('update:modelValue', p)
}
</script>
