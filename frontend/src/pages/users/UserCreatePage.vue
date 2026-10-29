<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/lib/errors'
import { ROLES } from '@/lib/format'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', name: '', email: '', password: '', role: 'COLABORADOR' })
const erros = reactive({})
const erroGeral = ref('')
const mostrar = ref(false)
const loading = ref(false)

function validar() {
  Object.keys(erros).forEach((k) => delete erros[k])
  if (!/^[A-Za-z0-9._-]{3,50}$/.test(form.username))
    erros.username = 'Use 3 a 50 caracteres: letras, números, ponto, hífen ou _.'
  if (!form.name.trim()) erros.name = 'Indique o nome.'
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim()))
    erros.email = 'Indique um e-mail válido.'
  if (form.password.length < 8)
    erros.password = 'A palavra-passe tem de ter pelo menos 8 caracteres.'
  return Object.keys(erros).length === 0
}

async function criar() {
  erroGeral.value = ''
  if (!validar()) return
  loading.value = true
  try {
    const user = await store.create({ ...form, name: form.name.trim(), email: form.email.trim() })
    toast.success(`Conta de ${user.name} criada.`)
    router.push({ name: 'user', params: { username: user.username } })
  } catch (e) {
    erroGeral.value =
      e.response?.status === 409
        ? 'Já existe um utilizador com este nome ou e-mail.'
        : errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="max-w-xl space-y-8">
    <PageHeader
      title="Novo utilizador"
      description="A pessoa pode alterar a palavra-passe depois de entrar."
    />

    <form class="space-y-6" novalidate @submit.prevent="criar">
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="space-y-2">
          <Label for="username">Utilizador</Label>
          <Input
            id="username"
            v-model="form.username"
            autocapitalize="none"
            spellcheck="false"
            :aria-invalid="!!erros.username"
          />
          <p v-if="erros.username" class="text-destructive text-sm" role="alert">
            {{ erros.username }}
          </p>
        </div>
        <div class="space-y-2">
          <Label for="nome">Nome</Label>
          <Input id="nome" v-model="form.name" :aria-invalid="!!erros.name" />
          <p v-if="erros.name" class="text-destructive text-sm" role="alert">{{ erros.name }}</p>
        </div>
      </div>

      <div class="space-y-2">
        <Label for="email">E-mail</Label>
        <Input id="email" v-model="form.email" type="email" :aria-invalid="!!erros.email" />
        <p v-if="erros.email" class="text-destructive text-sm" role="alert">{{ erros.email }}</p>
      </div>

      <div class="space-y-2">
        <Label for="password">Palavra-passe inicial</Label>
        <div class="relative">
          <Input
            id="password"
            v-model="form.password"
            :type="mostrar ? 'text' : 'password'"
            autocomplete="new-password"
            class="pr-11"
            :aria-invalid="!!erros.password"
          />
          <button
            type="button"
            class="text-muted-foreground hover:text-foreground absolute top-1/2 right-2 inline-flex size-7 -translate-y-1/2 items-center justify-center rounded-md"
            :aria-label="mostrar ? 'Esconder palavra-passe' : 'Mostrar palavra-passe'"
            @click="mostrar = !mostrar"
          >
            <EyeOff v-if="mostrar" class="size-4" /><Eye v-else class="size-4" />
          </button>
        </div>
        <p v-if="erros.password" class="text-destructive text-sm" role="alert">
          {{ erros.password }}
        </p>
      </div>

      <div class="space-y-2">
        <Label for="role">Perfil</Label>
        <NativeSelect id="role" v-model="form.role">
          <option v-for="(rotulo, valor) in ROLES" :key="valor" :value="valor">{{ rotulo }}</option>
        </NativeSelect>
        <p class="text-muted-foreground text-sm">
          Colaborador: carrega, comenta e avalia. Responsável: também modera e gere tags.
          Administrador: também gere utilizadores.
        </p>
      </div>

      <p
        v-if="erroGeral"
        class="bg-destructive/10 text-destructive rounded-lg px-3.5 py-2.5 text-sm"
        role="alert"
      >
        {{ erroGeral }}
      </p>

      <div class="flex justify-end gap-2">
        <Button as-child variant="ghost" type="button"
          ><RouterLink :to="{ name: 'users' }">Cancelar</RouterLink></Button
        >
        <Button type="submit" :disabled="loading"
          ><Loader2 v-if="loading" class="animate-spin" /> Criar utilizador</Button
        >
      </div>
    </form>
  </div>
</template>
