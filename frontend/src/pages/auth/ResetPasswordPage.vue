<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import AuthLayout from '@/components/app/AuthLayout.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAPIStore } from '@/stores/api'
import { errorMessage } from '@/lib/errors'

const api = useAPIStore()
const route = useRoute()
const router = useRouter()

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const nova = ref('')
const confirmacao = ref('')
const erro = ref('')
const loading = ref(false)

async function repor() {
  erro.value = ''
  if (nova.value.length < 8) {
    erro.value = 'A palavra-passe tem de ter pelo menos 8 caracteres.'
    return
  }
  if (nova.value !== confirmacao.value) {
    erro.value = 'As palavras-passe não coincidem.'
    return
  }
  loading.value = true
  try {
    await api.resetPassword(token.value, nova.value)
    toast.success('Palavra-passe alterada. Já pode entrar.')
    router.replace({ name: 'login' })
  } catch (e) {
    erro.value = errorMessage(e, 'O link é inválido ou expirou. Peça um novo.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout title="Escolha uma nova palavra-passe" subtitle="Use pelo menos 8 caracteres.">
    <div v-if="!token" class="space-y-5">
      <p class="bg-destructive/10 text-destructive rounded-lg px-3.5 py-2.5 text-sm" role="alert">
        Este link está incompleto. Peça um novo link de recuperação.
      </p>
      <Button as-child variant="outline" class="w-full">
        <RouterLink :to="{ name: 'forgot-password' }">Pedir novo link</RouterLink>
      </Button>
    </div>

    <form v-else class="space-y-5" novalidate @submit.prevent="repor">
      <div class="space-y-2">
        <Label for="nova">Nova palavra-passe</Label>
        <Input id="nova" v-model="nova" type="password" autocomplete="new-password" autofocus />
      </div>
      <div class="space-y-2">
        <Label for="confirmacao">Confirmar palavra-passe</Label>
        <Input id="confirmacao" v-model="confirmacao" type="password" autocomplete="new-password" />
      </div>
      <p
        v-if="erro"
        class="bg-destructive/10 text-destructive rounded-lg px-3.5 py-2.5 text-sm"
        role="alert"
      >
        {{ erro }}
      </p>
      <Button type="submit" size="lg" class="w-full" :disabled="loading">
        <Loader2 v-if="loading" class="animate-spin" />
        {{ loading ? 'A guardar…' : 'Guardar palavra-passe' }}
      </Button>
    </form>
  </AuthLayout>
</template>
