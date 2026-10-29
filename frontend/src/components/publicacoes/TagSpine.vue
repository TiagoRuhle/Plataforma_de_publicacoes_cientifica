<script setup>
import { computed } from 'vue'
import { tagStyle } from '@/lib/tagColor'

/** A "lombada" da publicação: um segmento colorido por tag (as mesmas cores dos chips). */
const props = defineProps({
  tags: { type: Array, default: () => [] },
  max: { type: Number, default: 5 },
})

const segmentos = computed(() => props.tags.slice(0, props.max).map((t) => t.tag))
</script>

<template>
  <span
    class="flex w-1.5 shrink-0 flex-col self-stretch overflow-hidden rounded-full"
    aria-hidden="true"
  >
    <span v-if="segmentos.length === 0" class="bg-border flex-1" />
    <span
      v-for="nome in segmentos"
      :key="nome"
      class="tag-hue min-h-1 flex-1 bg-[var(--tag-spine)]"
      :style="tagStyle(nome)"
    />
  </span>
</template>
