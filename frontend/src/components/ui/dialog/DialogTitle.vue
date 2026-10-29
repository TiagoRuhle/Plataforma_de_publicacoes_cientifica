<script setup>
import { reactiveOmit } from '@vueuse/core'
import { DialogTitle, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

defineOptions({ name: 'UiDialogTitle' })
const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
})
const delegatedProps = reactiveOmit(props, 'class')
const forwardedProps = useForwardProps(delegatedProps)
</script>

<template>
  <DialogTitle v-bind="forwardedProps" :class="cn('display text-[1.35rem]', props.class)">
    <slot />
  </DialogTitle>
</template>
