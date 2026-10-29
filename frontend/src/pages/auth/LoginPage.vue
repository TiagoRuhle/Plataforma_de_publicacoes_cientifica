<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Eye, EyeOff, Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import AuthLayout from '@/components/app/AuthLayout.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/lib/errors'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const mostrar = ref(false)
const erro = ref('')
const loading = ref(false)

/** Só aceita redirecionamentos internos (evita open redirect). */
function destinoSeguro(valor) {
  return typeof valor === 'string' && valor.startsWith('/') && !valor.startsWith('//')
    ? valor
    : '/publicacoes'
}

async function entrar() {
  erro.value = ''
  if (!username.value.trim() || !password.value) {
    erro.value = 'Indique o utilizador e a palavra-passe.'
    return
  }
  loading.value = true
  try {
    const user = await auth.login({ username: username.value.trim(), password: password.value })
    await router.replace(destinoSeguro(route.query.redirect))
    toast.success(`Bem-vindo, ${user.name}.`)
  } catch (e) {
    erro.value =
      e.response?.status === 401
        ? 'Utilizador ou palavra-passe incorretos, ou conta suspensa.'
        : errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="Encontre o que o Centro já leu."
    subtitle="Entre com a sua conta do Centro de Investigação."
  >
    <form class="space-y-5" novalidate @submit.prevent="entrar">
      <div class="space-y-2">
        <Label for="username">Utilizador</Label>
        <Input
          id="username"
          v-model="username"
          autocomplete="username"
          autocapitalize="none"
          spellcheck="false"
          autofocus
          :aria-invalid="!!erro"
        />
      </div>

      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <Label for="password">Palavra-passe</Label>
          <RouterLink
            :to="{ name: 'forgot-password' }"
            class="text-signal text-sm underline-offset-4 hover:underline"
          >
            Esqueceu-se?
          </RouterLink>
        </div>
        <div class="relative">
          <Input
            id="password"
            v-model="password"
            :type="mostrar ? 'text' : 'password'"
            autocomplete="current-password"
            class="pr-11"
            :aria-invalid="!!erro"
          />
          <button
            type="button"
            class="text-muted-foreground hover:text-foreground absolute top-1/2 right-2 inline-flex size-7 -translate-y-1/2 items-center justify-center rounded-md"
            :aria-label="mostrar ? 'Esconder palavra-passe' : 'Mostrar palavra-passe'"
            @click="mostrar = !mostrar"
          >
            <EyeOff v-if="mostrar" class="size-4" />
            <Eye v-else class="size-4" />
          </button>
        </div>
      </div>

      <p v-if="route.query.expired" class="text-muted-foreground text-sm">A sua sessão terminou.</p>
      <p
        v-if="erro"
        class="bg-destructive/10 text-destructive rounded-lg px-3.5 py-2.5 text-sm"
        role="alert"
      >
        {{ erro }}
      </p>

      <Button type="submit" size="lg" class="w-full" :disabled="loading">
        <Loader2 v-if="loading" class="animate-spin" />
        {{ loading ? 'A entrar…' : 'Entrar' }}
      </Button>
    </form>
  </AuthLayout>
</template>
