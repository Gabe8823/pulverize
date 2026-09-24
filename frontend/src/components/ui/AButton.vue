<template>
  <button
    :class="[
      'a-btn',
      `a-btn--${type}`,
      size !== 'md' ? `a-btn--${size}` : '',
      block ? 'a-btn--block' : '',
      loading ? 'is-loading' : '',
    ]"
    :type="htmlType"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'primary' | 'default' | 'ghost' | 'danger' | 'ghost-danger'
    size?: 'lg' | 'md' | 'sm'
    block?: boolean
    loading?: boolean
    disabled?: boolean
    htmlType?: 'button' | 'submit' | 'reset'
  }>(),
  {
    type: 'default',
    size: 'md',
    block: false,
    loading: false,
    disabled: false,
    htmlType: 'button',
  }
)

defineEmits<{ click: [e: MouseEvent] }>()
</script>
