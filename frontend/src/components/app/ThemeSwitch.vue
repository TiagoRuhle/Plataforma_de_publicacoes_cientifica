<script setup>
import { Monitor, Moon, Sun } from 'lucide-vue-next'
import { useTheme } from '@/composables/useTheme'

const { modo, setModo } = useTheme()
const opcoes = [
  { valor: 'light', label: 'Claro', icon: Sun },
  { valor: 'system', label: 'Sistema', icon: Monitor },
  { valor: 'dark', label: 'Escuro', icon: Moon },
]
</script>

<template>
  <div class="bg-secondary flex gap-0.5 rounded-lg p-0.5" role="radiogroup" aria-label="Tema">
    <button
      v-for="o in opcoes"
      :key="o.valor"
      type="button"
      role="radio"
      :aria-checked="modo === o.valor"
      class="text-muted-foreground flex flex-1 items-center justify-center gap-1.5 rounded-md py-1.5 text-xs font-medium transition-colors"
      :class="modo === o.valor ? 'bg-card text-foreground shadow-xs' : 'hover:text-foreground'"
      @click="setModo(o.valor)"
    >
      <component :is="o.icon" class="size-3.5" aria-hidden="true" />
      {{ o.label }}
    </button>
  </div>
</template>
