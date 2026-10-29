<script setup>
import { cn } from '@/lib/utils'

defineOptions({ name: 'UiDialogHeader' })
const props = defineProps({ class: { type: null, required: false } })
</script>

<template>
  <div :class="cn('flex flex-col gap-1.5 pr-8', props.class)">
    <slot />
  </div>
</template>
