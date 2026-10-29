<script setup>
import { computed } from 'vue'
import { EyeOff, X } from 'lucide-vue-next'
import { tagStyle } from '@/lib/tagColor'

const props = defineProps({
  name: { type: String, required: true },
  hidden: { type: Boolean, default: false },
  removable: { type: Boolean, default: false },
  interactive: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  size: { type: String, default: 'sm' }, // sm | md
})
const emit = defineEmits(['click', 'remove'])

const estilo = computed(() => tagStyle(props.name))
const tamanho = computed(() =>
  props.size === 'md' ? 'h-7 px-2.5 text-[13.5px]' : 'h-6 px-2 text-[12.5px]',
)
</script>

<template>
  <component
    :is="interactive ? 'button' : 'span'"
    :type="interactive ? 'button' : undefined"
    :aria-pressed="interactive ? selected : undefined"
    class="tag-hue inline-flex max-w-full items-center gap-1.5 rounded-md bg-[var(--tag-bg)] font-medium text-[var(--tag-fg)] ring-1 ring-[var(--tag-line)] ring-inset"
    :class="[
      tamanho,
      interactive && 'cursor-pointer transition-[box-shadow,filter] hover:brightness-[0.97]',
      selected && 'ring-2 ring-[var(--tag-spine)]',
    ]"
    :style="estilo"
    @click="interactive && emit('click')"
  >
    <span class="size-1.5 shrink-0 rounded-full bg-[var(--tag-spine)]" aria-hidden="true" />
    <span class="truncate">{{ name }}</span>
    <EyeOff v-if="hidden" class="size-3 shrink-0 opacity-70" aria-label="Tag oculta" />
    <button
      v-if="removable"
      type="button"
      class="-mr-1 ml-0.5 inline-flex size-4 items-center justify-center rounded-sm opacity-70 transition-opacity hover:bg-black/10 hover:opacity-100 dark:hover:bg-white/15"
      :aria-label="`Desassociar a tag ${name}`"
      @click.stop.prevent="emit('remove')"
    >
      <X class="size-3" />
    </button>
  </component>
</template>
