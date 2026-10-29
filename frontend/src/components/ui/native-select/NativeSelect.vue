<script setup>
import { ChevronDown } from 'lucide-vue-next'
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'

// inheritAttrs: false -> id, aria-label, disabled... vão para o <select> (e não para o <div> de fora)
defineOptions({ name: 'UiNativeSelect', inheritAttrs: false })

/** <select> nativo com o estilo do sistema: melhor acessibilidade e UX em telemóvel do que um select custom. */
const props = defineProps({
  modelValue: { type: [String, Number, Boolean], required: false },
  class: { type: null, required: false },
  size: { type: String, required: false, default: 'default' },
})

const emits = defineEmits(['update:modelValue'])
const modelValue = useVModel(props, 'modelValue', emits, { passive: true })
</script>

<template>
  <div :class="cn('relative inline-flex w-full', props.class)">
    <select
      v-bind="$attrs"
      v-model="modelValue"
      data-slot="native-select"
      :class="
        cn(
          'border-input bg-card text-foreground w-full appearance-none rounded-lg border pr-9 pl-3.5 text-[15px] transition-[color,box-shadow,border-color] outline-none',
          'focus-visible:border-ring focus-visible:ring-ring/30 focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50',
          size === 'sm' ? 'h-8 rounded-md text-[13px]' : 'h-10',
        )
      "
    >
      <slot />
    </select>
    <ChevronDown
      class="text-muted-foreground pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2"
    />
  </div>
</template>
