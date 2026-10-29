<script setup>
import { computed } from 'vue'
import { initials } from '@/lib/format'
import { tagHue } from '@/lib/tagColor'

const props = defineProps({
  name: { type: String, default: '' },
  size: { type: String, default: 'md' }, // sm | md | lg
})

const iniciais = computed(() => initials(props.name))
const tamanho = computed(
  () => ({ sm: 'size-7 text-[11px]', md: 'size-9 text-[13px]', lg: 'size-14 text-xl' })[props.size],
)
</script>

<template>
  <span
    class="tag-hue inline-flex shrink-0 items-center justify-center rounded-full bg-[var(--tag-bg)] font-semibold text-[var(--tag-fg)] ring-1 ring-[var(--tag-line)] ring-inset select-none"
    :class="tamanho"
    :style="{ '--h': tagHue(name) }"
    aria-hidden="true"
  >
    {{ iniciais }}
  </span>
</template>
