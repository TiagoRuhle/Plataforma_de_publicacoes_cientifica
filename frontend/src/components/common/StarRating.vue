<script setup>
import { computed, ref } from 'vue'
import { Star } from 'lucide-vue-next'
import { formatRating } from '@/lib/format'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  size: { type: String, default: 'md' }, // sm | md | lg
  label: { type: String, default: 'Avaliação' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'rate'])

// Só leitura: suporta valores fracionários (média). Interativo: 1 a 5.
const hover = ref(0)
const tamanho = computed(() => ({ sm: 'size-3.5', md: 'size-4.5', lg: 'size-6' })[props.size])
const percentagem = computed(() => Math.max(0, Math.min(100, (props.modelValue / 5) * 100)))
const preenchidas = computed(() => hover.value || props.modelValue)

function escolher(n) {
  if (props.readonly || props.disabled) return
  emit('update:modelValue', n)
  emit('rate', n)
}
</script>

<template>
  <span
    v-if="readonly"
    class="relative inline-flex"
    role="img"
    :aria-label="`${label}: ${formatRating(modelValue)} em 5`"
  >
    <span class="text-border flex gap-0.5" aria-hidden="true">
      <Star v-for="n in 5" :key="n" :class="tamanho" fill="currentColor" stroke-width="1.5" />
    </span>
    <span
      class="text-star absolute top-0 left-0 overflow-hidden"
      :style="{ width: percentagem + '%' }"
      aria-hidden="true"
    >
      <span class="flex w-max gap-0.5">
        <Star v-for="n in 5" :key="n" :class="tamanho" fill="currentColor" stroke-width="1.5" />
      </span>
    </span>
  </span>

  <span
    v-else
    class="inline-flex gap-0.5"
    role="radiogroup"
    :aria-label="label"
    @mouseleave="hover = 0"
  >
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      role="radio"
      :aria-checked="modelValue === n"
      :aria-label="`${n} ${n === 1 ? 'estrela' : 'estrelas'}`"
      :disabled="disabled"
      class="hover:text-star focus-visible:ring-ring/50 rounded-sm p-0.5 transition-transform duration-100 hover:scale-110 focus-visible:ring-2 disabled:opacity-50"
      :class="n <= preenchidas ? 'text-star' : 'text-border'"
      @mouseenter="hover = n"
      @focus="hover = n"
      @blur="hover = 0"
      @click="escolher(n)"
    >
      <Star :class="tamanho" fill="currentColor" stroke-width="1.5" />
    </button>
  </span>
</template>
