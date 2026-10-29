<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { History, LibraryBig, Plus, Tags, Users } from 'lucide-vue-next'
import BrandMark from './BrandMark.vue'
import UserMenu from './UserMenu.vue'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()

const itens = computed(() => [
  { to: '/publicacoes', label: 'Publicações', icon: LibraryBig },
  { to: '/tags', label: 'Tags', icon: Tags },
  { to: '/historico', label: 'Atividade', icon: History },
  ...(auth.isAdmin ? [{ to: '/users', label: 'Utilizadores', icon: Users }] : []),
])

const ativo = (to) => route.path === to || route.path.startsWith(to + '/')
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-4">
    <RouterLink to="/publicacoes" class="flex items-center gap-3 rounded-lg px-1.5 py-1">
      <BrandMark />
      <span class="leading-tight">
        <span class="display block text-[1.2rem]">Publicações</span>
        <span class="text-muted-foreground block text-xs">Centro de Investigação XYZ</span>
      </span>
    </RouterLink>

    <Button as-child size="lg" class="w-full justify-start">
      <RouterLink to="/publicacoes/create"><Plus /> Nova publicação</RouterLink>
    </Button>

    <nav aria-label="Principal" class="flex flex-col gap-0.5">
      <RouterLink
        v-for="item in itens"
        :key="item.to"
        :to="item.to"
        class="text-muted-foreground hover:bg-accent hover:text-foreground flex items-center gap-3 rounded-lg px-3 py-2 text-[15px] font-medium transition-colors"
        :class="ativo(item.to) && 'bg-accent text-foreground'"
        :aria-current="ativo(item.to) ? 'page' : undefined"
      >
        <component :is="item.icon" class="size-[18px]" aria-hidden="true" />
        {{ item.label }}
      </RouterLink>
    </nav>

    <div class="mt-auto">
      <UserMenu />
    </div>
  </div>
</template>
