<template>
  <button
    type="button"
    :class="['a-switch', modelValue ? 'is-on' : '']"
    :disabled="disabled"
    role="switch"
    :aria-checked="modelValue"
    @click="!disabled && $emit('update:modelValue', !modelValue)"
  />
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{ modelValue: boolean; disabled?: boolean }>(),
  { disabled: false }
)
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>
