<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="a-modal-overlay"
      :class="{ 'is-open': visible }"
      @click.self="onOverlay"
    >
      <div :class="['a-modal', size === 'lg' ? 'a-modal--lg' : '']" role="dialog">
        <div class="a-modal__head">
          <h3 class="a-modal__title">{{ title }}</h3>
          <button type="button" class="a-modal__close" aria-label="关闭" @click="close">✕</button>
        </div>
        <div class="a-modal__body">
          <slot />
        </div>
        <div v-if="showFooter" class="a-modal__foot">
          <slot name="footer">
            <a-button @click="close">{{ cancelText }}</a-button>
            <a-button :type="danger ? 'danger' : 'primary'" :loading="loading" @click="$emit('confirm')">
              {{ confirmText }}
            </a-button>
          </slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import AButton from './AButton.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    size?: 'md' | 'lg'
    showFooter?: boolean
    confirmText?: string
    cancelText?: string
    danger?: boolean
    loading?: boolean
    closeOnOverlay?: boolean
  }>(),
  {
    title: '',
    size: 'md',
    showFooter: true,
    confirmText: '确定',
    cancelText: '取消',
    danger: false,
    loading: false,
    closeOnOverlay: true,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

const visible = ref(false)

watch(
  () => props.modelValue,
  async (show) => {
    if (show) {
      await nextTick()
      visible.value = true
      document.body.style.overflow = 'hidden'
    } else {
      visible.value = false
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

function close() {
  emit('update:modelValue', false)
  emit('cancel')
}
function onOverlay() {
  if (props.closeOnOverlay !== false) close()
}
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.modelValue) close()
}

window.addEventListener('keydown', onKey)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>
