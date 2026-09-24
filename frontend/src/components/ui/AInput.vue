<template>
  <div v-if="type === 'password'" class="a-input-wrap">
    <input
      :class="['a-input', extraClass]"
      :type="revealed ? 'text' : 'password'"
      :value="modelValue ?? ''"
      :placeholder="placeholder"
      :disabled="disabled"
      :maxlength="maxlength"
      @input="onInput"
      @blur="$emit('blur', $event)"
      @keyup.enter="$emit('enter', $event)"
    />
    <button
      type="button"
      class="a-input-affix"
      :aria-label="revealed ? '隐藏密码' : '显示密码'"
      @click="revealed = !revealed"
    >
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    </button>
  </div>
  <textarea
    v-else-if="type === 'textarea'"
    :class="['a-input', extraClass]"
    :value="modelValue ?? ''"
    :placeholder="placeholder"
    :disabled="disabled"
    :maxlength="maxlength"
    :rows="rows"
    @input="onInput"
    @blur="$emit('blur', $event)"
  />
  <input
    v-else
    :class="['a-input', extraClass]"
    :type="type"
    :value="modelValue ?? ''"
    :placeholder="placeholder"
    :disabled="disabled"
    :maxlength="maxlength"
    :min="min"
    :max="max"
    :step="step"
    @input="onInput"
    @blur="$emit('blur', $event)"
    @keyup.enter="$emit('enter', $event)"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null
    type?: 'text' | 'password' | 'number' | 'date' | 'time' | 'textarea' | 'email' | 'tel'
    placeholder?: string
    disabled?: boolean
    maxlength?: number
    rows?: number
    min?: number | string
    max?: number | string
    step?: number | string
    extraClass?: string
  }>(),
  {
    modelValue: '',
    type: 'text',
    placeholder: '',
    disabled: false,
    maxlength: undefined,
    rows: 4,
    min: undefined,
    max: undefined,
    step: undefined,
    extraClass: '',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
  blur: [e: FocusEvent]
  enter: [e: KeyboardEvent]
}>()

const revealed = ref(false)

function onInput(e: Event) {
  const el = e.target as HTMLInputElement | HTMLTextAreaElement
  if (props.type === 'number') {
    emit('update:modelValue', el.value === '' ? null : Number(el.value))
  } else {
    emit('update:modelValue', el.value)
  }
}
</script>
