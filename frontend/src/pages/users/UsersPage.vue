<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChevronRight, Plus, Search, Users } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/lib/errors'
import { plural, roleLabel } from '@/lib/format'

const store = useUserStore()
const loading = ref(true)
const pesquisa = ref('')

const filtrados = computed(() => {
  const termo = pesquisa.value.trim().toLowerCase()
  return [...store.users]
    .filter(
      (u) => !termo || [u.name, u.username, u.email].some((v) => v?.toLowerCase().includes(termo)),
    )
    .sort((a, b) => a.name.localeCompare(b.name, 'pt'))
})

onMounted(async () => {
  try {
    await store.load()
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível carregar os utilizadores.'))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-7">
    <PageHeader
      title="Utilizadores"
      description="Crie contas, altere perfis e suspenda o acesso de quem já não colabora com o Centro."
    >
      <template #actions>
        <Button as-child
          ><RouterLink :to="{ name: 'user-create' }"><Plus /> Novo utilizador</RouterLink></Button
        >
      </template>
    </PageHeader>

    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="relative min-w-56 flex-1 sm:max-w-sm">
        <Search
          class="text-muted-foreground pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2"
          aria-hidden="true"
        />
        <input
          v-model="pesquisa"
          type="search"
          placeholder="Nome, utilizador ou e-mail"
          aria-label="Pesquisar utilizadores"
          class="border-input bg-card placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/30 h-10 w-full rounded-lg border pr-3 pl-10 text-[15px] outline-none focus-visible:ring-[3px]"
        />
      </div>
      <p v-if="!loading" class="text-muted-foreground text-sm" role="status">
        {{ plural(filtrados.length, 'utilizador', 'utilizadores') }}
      </p>
    </div>

    <div class="bg-card overflow-hidden rounded-2xl border">
      <ul v-if="loading" class="divide-y" aria-hidden="true">
        <li v-for="n in 5" :key="n" class="flex items-center gap-4 px-5 py-4">
          <Skeleton class="size-9 rounded-full" /><Skeleton class="h-5 w-56" />
        </li>
      </ul>

      <ul v-else-if="filtrados.length" class="divide-y">
        <li v-for="u in filtrados" :key="u.username">
          <RouterLink
            :to="{ name: 'user', params: { username: u.username } }"
            class="hover:bg-accent/55 focus-visible:bg-accent/55 flex items-center gap-4 px-5 py-3.5 transition-colors"
          >
            <UserAvatar :name="u.name" />
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium">{{ u.name }}</span>
              <span class="text-muted-foreground block truncate text-sm"
                >{{ u.username }}, {{ u.email }}</span
              >
            </span>
            <span
              class="bg-secondary hidden w-28 rounded-md px-2 py-0.5 text-center text-[13px] font-medium sm:inline-block"
              >{{ roleLabel(u.role) }}</span
            >
            <span
              class="inline-flex w-20 items-center gap-1.5 text-[13px] font-medium"
              :class="u.active === false ? 'text-warning' : 'text-success'"
            >
              <span class="size-1.5 rounded-full bg-current" aria-hidden="true" />
              {{ u.active === false ? 'Suspenso' : 'Ativo' }}
            </span>
            <ChevronRight class="text-muted-foreground size-4" aria-hidden="true" />
          </RouterLink>
        </li>
      </ul>

      <EmptyState
        v-else
        :icon="Users"
        title="Nenhum utilizador encontrado"
        description="Experimente outra pesquisa."
      />
    </div>
  </div>
</template>
