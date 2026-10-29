<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/app/AppShell.vue'
import { Toaster } from '@/components/ui/sonner'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const { modo } = useTheme()
const semMoldura = computed(() => route.meta.layout === 'bare')
</script>

<template>
  <a
    href="#conteudo"
    class="bg-primary text-primary-foreground sr-only z-50 rounded-lg px-4 py-2 focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
  >
    Saltar para o conteúdo
  </a>

  <RouterView v-if="semMoldura" />
  <AppShell v-else>
    <RouterView v-slot="{ Component }">
      <Transition name="pagina" mode="out-in">
        <component :is="Component" :key="route.path" />
      </Transition>
    </RouterView>
  </AppShell>

  <Toaster :theme="modo" position="bottom-right" rich-colors close-button />
</template>

<style>
.pagina-enter-active,
.pagina-leave-active {
  transition:
    opacity 0.14s ease,
    transform 0.14s ease;
}
.pagina-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.pagina-leave-to {
  opacity: 0;
}
.gaveta-enter-active,
.gaveta-leave-active {
  transition: opacity 0.18s ease;
}
.gaveta-enter-active aside,
.gaveta-leave-active aside {
  transition: transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.gaveta-enter-from,
.gaveta-leave-to {
  opacity: 0;
}
.gaveta-enter-from aside,
.gaveta-leave-to aside {
  transform: translateX(-100%);
}
</style>
