<script setup>
import { reactive, ref } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/lib/errors'
import { roleLabel } from '@/lib/format'

const auth = useAuthStore()
const userStore = useUserStore()

const dados = reactive({ name: auth.currentUser.name, email: auth.currentUser.email })
const aGuardar = ref(false)

async function guardarDados() {
  if (!dados.name.trim() || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(dados.email.trim())) {
    return toast.error('Indique um nome e um e-mail válidos.')
  }
  aGuardar.value = true
  try {
    await userStore.update(auth.currentUser.username, {
      name: dados.name.trim(),
      email: dados.email.trim(),
    })
    await auth.getUser()
    toast.success('Dados guardados.')
  } catch (e) {
    toast.error(e.response?.status === 409 ? 'Este e-mail já está em uso.' : errorMessage(e))
  } finally {
    aGuardar.value = false
  }
}

const pw = reactive({ atual: '', nova: '', confirmacao: '' })
const erroPw = ref('')
const aAlterar = ref(false)

async function alterarPassword() {
  erroPw.value = ''
  if (!pw.atual) erroPw.value = 'Indique a palavra-passe atual.'
  else if (pw.nova.length < 8)
    erroPw.value = 'A nova palavra-passe tem de ter pelo menos 8 caracteres.'
  else if (pw.nova !== pw.confirmacao) erroPw.value = 'As palavras-passe novas não coincidem.'
  if (erroPw.value) return

  aAlterar.value = true
  try {
    await userStore.changePassword(auth.currentUser.username, pw.atual, pw.nova)
    Object.assign(pw, { atual: '', nova: '', confirmacao: '' })
    toast.success('Palavra-passe alterada.')
  } catch (e) {
    erroPw.value = errorMessage(e)
  } finally {
    aAlterar.value = false
  }
}
</script>

<template>
  <div class="max-w-2xl space-y-12">
    <div class="flex items-center gap-5">
      <UserAvatar :name="auth.currentUser.name" size="lg" />
      <PageHeader
        :title="auth.currentUser.name"
        :description="`${auth.currentUser.username}, ${roleLabel(auth.currentUser.role)}`"
      />
    </div>

    <section class="space-y-5" aria-labelledby="h-dados">
      <h2 id="h-dados" class="display text-xl">Dados pessoais</h2>
      <form class="space-y-5" novalidate @submit.prevent="guardarDados">
        <div class="grid gap-5 sm:grid-cols-2">
          <div class="space-y-2">
            <Label for="nome">Nome</Label
            ><Input id="nome" v-model="dados.name" autocomplete="name" />
          </div>
          <div class="space-y-2">
            <Label for="email">E-mail</Label>
            <Input id="email" v-model="dados.email" type="email" autocomplete="email" />
          </div>
        </div>
        <p class="text-muted-foreground text-sm">
          O e-mail é usado para os alertas de tags e para recuperar a palavra-passe.
        </p>
        <Button type="submit" :disabled="aGuardar"
          ><Loader2 v-if="aGuardar" class="animate-spin" /> Guardar dados</Button
        >
      </form>
    </section>

    <section class="space-y-5 border-t pt-10" aria-labelledby="h-password">
      <h2 id="h-password" class="display text-xl">Palavra-passe</h2>
      <form class="max-w-sm space-y-5" novalidate @submit.prevent="alterarPassword">
        <div class="space-y-2">
          <Label for="atual">Palavra-passe atual</Label
          ><Input id="atual" v-model="pw.atual" type="password" autocomplete="current-password" />
        </div>
        <div class="space-y-2">
          <Label for="nova">Nova palavra-passe</Label
          ><Input id="nova" v-model="pw.nova" type="password" autocomplete="new-password" />
        </div>
        <div class="space-y-2">
          <Label for="confirmacao">Confirmar nova palavra-passe</Label
          ><Input
            id="confirmacao"
            v-model="pw.confirmacao"
            type="password"
            autocomplete="new-password"
          />
        </div>
        <p v-if="erroPw" class="text-destructive text-sm" role="alert">{{ erroPw }}</p>
        <Button type="submit" variant="outline" :disabled="aAlterar"
          ><Loader2 v-if="aAlterar" class="animate-spin" /> Alterar palavra-passe</Button
        >
      </form>
    </section>
  </div>
</template>
