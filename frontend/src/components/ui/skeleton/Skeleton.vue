<script setup>
import { cn } from '@/lib/utils'

defineOptions({ name: 'UiSkeleton' })

const props = defineProps({
  class: { type: null, required: false },
})
</script>

<template>
  <div aria-hidden="true" :class="cn('skeleton', props.class)" />
</template>
