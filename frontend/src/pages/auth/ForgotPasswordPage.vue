<script setup>
import { ref } from 'vue'
import { ArrowLeft, Loader2, MailCheck } from 'lucide-vue-next'
import AuthLayout from '@/components/app/AuthLayout.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAPIStore } from '@/stores/api'
import { errorMessage } from '@/lib/errors'

const api = useAPIStore()
const email = ref('')
const loading = ref(false)
const enviado = ref(false)
const erro = ref('')

async function enviar() {
  erro.value = ''
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value.trim())) {
    erro.value = 'Indique um e-mail válido.'
    return
  }
  loading.value = true
  try {
    await api.forgotPassword(email.value.trim())
    enviado.value = true // o backend responde sempre 202, exista ou não a conta
  } catch (e) {
    erro.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="Recuperar a palavra-passe"
    subtitle="Indique o e-mail da sua conta e enviamos-lhe um link para escolher uma nova."
  >
    <div v-if="enviado" class="space-y-5">
      <div class="bg-success/10 flex gap-3 rounded-xl p-4">
        <MailCheck class="text-success mt-0.5 size-5 shrink-0" aria-hidden="true" />
        <p class="text-sm">
          Se existir uma conta com este e-mail, vai receber instruções dentro de instantes. O link é
          válido durante 30 minutos.
        </p>
      </div>
      <Button as-child variant="outline" class="w-full">
        <RouterLink :to="{ name: 'login' }"><ArrowLeft /> Voltar a entrar</RouterLink>
      </Button>
    </div>

    <form v-else class="space-y-5" novalidate @submit.prevent="enviar">
      <div class="space-y-2">
        <Label for="email">E-mail</Label>
        <Input
          id="email"
          v-model="email"
          type="email"
          autocomplete="email"
          autofocus
          :aria-invalid="!!erro"
        />
      </div>
      <p v-if="erro" class="text-destructive text-sm" role="alert">{{ erro }}</p>
      <Button type="submit" size="lg" class="w-full" :disabled="loading">
        <Loader2 v-if="loading" class="animate-spin" />
        {{ loading ? 'A enviar…' : 'Enviar link' }}
      </Button>
      <p class="text-center text-sm">
        <RouterLink :to="{ name: 'login' }" class="text-signal underline-offset-4 hover:underline"
          >Voltar a entrar</RouterLink
        >
      </p>
    </form>
  </AuthLayout>
</template>
