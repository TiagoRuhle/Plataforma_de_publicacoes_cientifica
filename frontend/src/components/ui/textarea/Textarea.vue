<script setup>
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'

defineOptions({ name: 'UiTextarea' })

const props = defineProps({
  defaultValue: { type: [String, Number], required: false },
  modelValue: { type: [String, Number], required: false },
  class: { type: null, required: false },
})

const emits = defineEmits(['update:modelValue'])

const modelValue = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: props.defaultValue,
})
</script>

<template>
  <textarea
    v-model="modelValue"
    data-slot="textarea"
    :class="
      cn(
        'placeholder:text-muted-foreground border-input bg-card flex min-h-24 w-full rounded-lg border px-3.5 py-2.5 text-[15px] leading-relaxed transition-[color,box-shadow,border-color] outline-none',
        'focus-visible:border-ring focus-visible:ring-ring/30 focus-visible:ring-[3px] focus-visible:outline-none',
        'aria-invalid:ring-destructive/25 aria-invalid:border-destructive disabled:cursor-not-allowed disabled:opacity-50',
        props.class,
      )
    "
  />
</template>
