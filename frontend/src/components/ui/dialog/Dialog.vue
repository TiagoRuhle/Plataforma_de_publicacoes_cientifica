<script setup>
import { DialogRoot, useForwardPropsEmits } from 'reka-ui'

defineOptions({ name: 'UiDialog' })

const props = defineProps({
  open: { type: Boolean, required: false },
  defaultOpen: { type: Boolean, required: false },
  modal: { type: Boolean, required: false },
})
const emits = defineEmits(['update:open'])
const forwarded = useForwardPropsEmits(props, emits)
</script>

<template>
  <DialogRoot v-bind="forwarded">
    <slot />
  </DialogRoot>
</template>
