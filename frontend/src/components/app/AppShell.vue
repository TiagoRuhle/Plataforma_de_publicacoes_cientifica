<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Menu, X } from 'lucide-vue-next'
import BrandMark from './BrandMark.vue'
import SidebarContent from './SidebarContent.vue'
import { Button } from '@/components/ui/button'

const route = useRoute()
const gaveta = ref(false)
watch(
  () => route.fullPath,
  () => (gaveta.value = false),
)
</script>

<template>
  <div class="min-h-dvh">
    <!-- barra lateral (desktop) -->
    <aside
      class="bg-sidebar border-sidebar-border fixed inset-y-0 left-0 z-30 hidden w-64 border-r lg:block"
    >
      <SidebarContent />
    </aside>

    <!-- barra superior (telemóvel/tablet) -->
    <div
      class="bg-sidebar/90 border-sidebar-border sticky top-0 z-30 flex h-14 items-center justify-between border-b px-4 backdrop-blur lg:hidden"
    >
      <RouterLink to="/publicacoes" class="flex items-center gap-2.5">
        <BrandMark :size="24" />
        <span class="display text-lg">Publicações</span>
      </RouterLink>
      <Button variant="ghost" size="icon" aria-label="Abrir menu" @click="gaveta = true"
        ><Menu
      /></Button>
    </div>

    <Transition name="gaveta">
      <div v-if="gaveta" class="fixed inset-0 z-40 lg:hidden" @keydown.esc="gaveta = false">
        <div
          class="absolute inset-0 bg-[color-mix(in_oklch,var(--foreground)_38%,transparent)]"
          @click="gaveta = false"
        />
        <aside
          class="bg-sidebar absolute inset-y-0 left-0 w-72 max-w-[85vw] shadow-2xl"
          role="dialog"
          aria-label="Menu"
        >
          <Button
            class="absolute top-3 right-3"
            variant="ghost"
            size="icon-sm"
            aria-label="Fechar menu"
            @click="gaveta = false"
          >
            <X />
          </Button>
          <SidebarContent />
        </aside>
      </div>
    </Transition>

    <main id="conteudo" class="lg:pl-64" tabindex="-1">
      <div class="mx-auto w-full max-w-[1100px] px-4 py-8 sm:px-8 lg:py-12">
        <slot />
      </div>
    </main>
  </div>
</template>
