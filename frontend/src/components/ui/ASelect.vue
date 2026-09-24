<template>
  <div ref="root" :class="['a-select', open ? 'is-open' : '']">
    <button
      type="button"
      class="a-select__trigger"
      :class="{ 'is-placeholder': !displayValue }"
      :disabled="disabled"
      @click="toggle"
    >
      <span class="a-select__value">{{ displayValue || placeholder }}</span>
      <svg class="a-select__caret" viewBox="0 0 20 20" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <path d="M5 8l5 5 5-5" />
      </svg>
    </button>

    <Teleport to="body">
      <div
        v-if="open"
        ref="menu"
        class="a-select__menu"
        :style="menuStyle"
        @click.stop
      >
        <!-- 自定义输入模式 -->
        <div v-if="customMode" class="a-select__custom">
          <input
            ref="customInput"
            v-model="customText"
            class="a-select__custom-input"
            :placeholder="customPlaceholder"
            maxlength="40"
            @keydown.enter.prevent="confirmCustom"
            @keydown.esc="closeMenu"
          />
          <div class="a-select__custom-actions">
            <button type="button" class="a-select__custom-btn" @click="customMode = false">返回</button>
            <button
              type="button"
              class="a-select__custom-btn is-primary"
              :disabled="!customText.trim()"
              @click="confirmCustom"
            >
              确定
            </button>
          </div>
        </div>

        <template v-else>
          <template v-for="(row, i) in menuRows" :key="i">
            <div v-if="row.type === 'group'" class="a-select__group">{{ row.label }}</div>
            <button
              v-else
              type="button"
              class="a-select__option"
              :class="{ 'is-active': row.option!.value === modelValue }"
              @click="choose(row.option!)"
            >
              <span>
                <span class="a-select__label">{{ row.option!.label }}</span>
                <span v-if="row.option!.hint" class="a-select__hint">{{ row.option!.hint }}</span>
              </span>
              <svg class="a-select__check" viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4.5 10.5l3.6 3.6L15.5 6.5" />
              </svg>
            </button>
          </template>

          <!-- 自定义选项入口 -->
          <button
            v-if="allowCustom"
            type="button"
            class="a-select__option a-select__option--custom"
            @click="enterCustomMode"
          >
            <span class="a-select__label">
              <svg viewBox="0 0 20 20" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" style="vertical-align: -2px; margin-right: 6px">
                <path d="M10 4v12M4 10h12" />
              </svg>{{ customLabel }}
            </span>
          </button>

          <div v-if="!options.length && !allowCustom" class="a-empty" style="padding: 24px 12px">
            <div class="a-empty__text">暂无选项</div>
          </div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export interface SelectOption {
  value: string | number
  label: string
  hint?: string
  /** 分组标题：相同 group 的选项会被归入同一组，组前渲染小标题 */
  group?: string
}

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null
    options: SelectOption[]
    placeholder?: string
    disabled?: boolean
    /** 追加一条「自定义…」选项，用户可输入任意值 */
    allowCustom?: boolean
    customLabel?: string
    customPlaceholder?: string
  }>(),
  {
    modelValue: null,
    placeholder: '请选择',
    disabled: false,
    allowCustom: false,
    customLabel: '自定义…',
    customPlaceholder: '输入自定义内容',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
  change: [value: string | number | null]
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const menuStyle = ref<Record<string, string>>({})
const customMode = ref(false)
const customText = ref('')
const customInput = ref<HTMLInputElement | null>(null)

/** 命中预设选项显示 label；自定义值直接显示原始文本 */
const displayValue = computed(() => {
  const hit = props.options.find((o) => o.value === props.modelValue)
  if (hit) return hit.label
  if (props.modelValue !== null && props.modelValue !== '') return String(props.modelValue)
  return ''
})

/** 菜单行：分组标题 + 选项混排（未分组的选项不插入标题） */
interface MenuRow {
  type: 'group' | 'option'
  label?: string
  option?: SelectOption
}

const menuRows = computed<MenuRow[]>(() => {
  const rows: MenuRow[] = []
  let lastGroup: string | undefined
  for (const opt of props.options) {
    if (opt.group !== undefined && opt.group !== lastGroup) {
      rows.push({ type: 'group', label: opt.group })
      lastGroup = opt.group
    }
    rows.push({ type: 'option', option: opt })
  }
  return rows
})

function toggle() {
  if (props.disabled) return
  open.value ? closeMenu() : openMenu()
}

function openMenu() {
  customMode.value = false
  customText.value = ''
  open.value = true
  void nextTick(placeMenu)
}

function closeMenu() {
  open.value = false
  customMode.value = false
}

/**
 * 自适应定位：优先下方展示；下方放不下则翻到上方；
 * 始终钳制在视口内，且不出现内部滑条（高度随内容自适应）。
 */
function placeMenu() {
  const el = root.value
  const m = menu.value
  if (!el || !m) return

  const r = el.getBoundingClientRect()
  const mh = m.offsetHeight
  const vw = window.innerWidth
  const vh = window.innerHeight
  const gap = 8
  const edge = 12

  let top = r.bottom + gap
  if (top + mh + edge > vh) {
    const above = r.top - gap - mh
    if (above >= edge) {
      top = above
    } else {
      // 上下都放不下：贴下方并限制高度（隐藏滑条，仅可滚轮/键盘触达）
      top = Math.max(edge, r.bottom + gap)
      const cap = vh - top - edge
      if (cap > 0 && mh > cap) {
        menuStyle.value = { ...menuStyle.value, maxHeight: `${cap}px` }
      }
    }
  }

  let left = r.left
  if (left + r.width > vw - edge) left = Math.max(edge, vw - r.width - edge)

  menuStyle.value = {
    ...menuStyle.value,
    position: 'fixed',
    top: `${Math.round(top)}px`,
    left: `${Math.round(left)}px`,
    width: `${r.width}px`,
    maxHeight: 'none',
  }
}

function choose(opt: SelectOption) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  closeMenu()
}

function enterCustomMode() {
  customMode.value = true
  customText.value = typeof props.modelValue === 'string' ? props.modelValue : ''
  void nextTick(() => {
    customInput.value?.focus()
    placeMenu()
  })
}

function confirmCustom() {
  const v = customText.value.trim()
  if (!v) return
  emit('update:modelValue', v)
  emit('change', v)
  closeMenu()
}

function onDocClick(e: MouseEvent) {
  if (!open.value) return
  const target = e.target as Node
  if (root.value?.contains(target)) return
  if (menu.value?.contains(target)) return
  closeMenu()
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) closeMenu()
}

/** 滚动/缩放时重新定位而不是直接关闭（弹窗内滚动不打断选择） */
function reposition() {
  if (open.value) placeMenu()
}

watch(open, (v) => {
  if (v) {
    document.addEventListener('keydown', onKey)
  } else {
    document.removeEventListener('keydown', onKey)
  }
})

onMounted(() => {
  document.addEventListener('click', onDocClick)
  window.addEventListener('resize', reposition)
  window.addEventListener('scroll', reposition, true)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', reposition)
  window.removeEventListener('scroll', reposition, true)
})
</script>
