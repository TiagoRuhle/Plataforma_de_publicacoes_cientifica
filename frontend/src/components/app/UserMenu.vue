<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { onClickOutside } from '@vueuse/core'
import { ChevronsUpDown, LogOut, UserRound } from 'lucide-vue-next'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ThemeSwitch from './ThemeSwitch.vue'
import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/lib/format'
import { toast } from 'vue-sonner'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const aberto = ref(false)
const raiz = ref(null)
onClickOutside(raiz, () => (aberto.value = false))
watch(
  () => route.fullPath,
  () => (aberto.value = false),
)

function terminarSessao() {
  aberto.value = false
  auth.logout()
  router.push({ name: 'login' })
  toast.success('Sessão terminada.')
}
</script>

<template>
  <div v-if="auth.currentUser" ref="raiz" class="relative" @keydown.esc="aberto = false">
    <div
      v-if="aberto"
      class="bg-popover absolute right-0 bottom-full left-0 mb-2 space-y-3 rounded-xl border p-3 shadow-[0_18px_40px_-20px_color-mix(in_oklch,var(--foreground)_40%,transparent)]"
      role="menu"
    >
      <ThemeSwitch />
      <div class="space-y-0.5">
        <RouterLink
          :to="{ name: 'profile' }"
          class="hover:bg-accent flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm transition-colors"
          role="menuitem"
        >
          <UserRound class="size-4" aria-hidden="true" /> O meu perfil
        </RouterLink>
        <button
          type="button"
          class="hover:bg-accent flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm transition-colors"
          role="menuitem"
          @click="terminarSessao"
        >
          <LogOut class="size-4" aria-hidden="true" /> Terminar sessão
        </button>
      </div>
    </div>

    <button
      type="button"
      class="hover:bg-accent flex w-full items-center gap-3 rounded-xl p-2 text-left transition-colors"
      :aria-expanded="aberto"
      aria-haspopup="menu"
      @click="aberto = !aberto"
    >
      <UserAvatar :name="auth.currentUser.name" />
      <span class="min-w-0 flex-1">
        <span class="block truncate text-sm font-medium">{{ auth.currentUser.name }}</span>
        <span class="text-muted-foreground block truncate text-xs">{{
          roleLabel(auth.currentUser.role)
        }}</span>
      </span>
      <ChevronsUpDown class="text-muted-foreground size-4 shrink-0" aria-hidden="true" />
    </button>
  </div>
</template>
