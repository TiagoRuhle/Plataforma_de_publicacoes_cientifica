<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, History, Loader2, SearchX } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/lib/errors'
import { ROLES, roleLabel } from '@/lib/format'

const props = defineProps({ username: { type: String, required: true } })

const router = useRouter()
const auth = useAuthStore()
const store = useUserStore()

const user = ref(null)
const loading = ref(true)
const naoEncontrado = ref(false)

const dados = reactive({ name: '', email: '' })
const role = ref('COLABORADOR')
const aGuardarDados = ref(false)
const aGuardarRole = ref(false)

const eEuProprio = computed(() => auth.currentUser?.username === props.username)
const suspenso = computed(() => user.value?.active === false)

function preencher(u) {
  user.value = u
  dados.name = u.name
  dados.email = u.email
  role.value = u.role
}

onMounted(async () => {
  try {
    preencher(await store.get(props.username))
  } catch (e) {
    if (e.response?.status === 404) naoEncontrado.value = true
    else toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
})

async function guardarDados() {
  if (!dados.name.trim() || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(dados.email.trim())) {
    return toast.error('Indique um nome e um e-mail válidos.')
  }
  aGuardarDados.value = true
  try {
    preencher(
      await store.update(props.username, { name: dados.name.trim(), email: dados.email.trim() }),
    )
    toast.success('Dados guardados.')
  } catch (e) {
    toast.error(e.response?.status === 409 ? 'Este e-mail já está em uso.' : errorMessage(e))
  } finally {
    aGuardarDados.value = false
  }
}

async function guardarRole() {
  aGuardarRole.value = true
  try {
    await store.setRole(props.username, role.value)
    user.value = { ...user.value, role: role.value }
    toast.success(`Perfil alterado para ${roleLabel(role.value)}.`)
  } catch (e) {
    role.value = user.value.role
    toast.error(errorMessage(e))
  } finally {
    aGuardarRole.value = false
  }
}

// ---- estado e remoção, com confirmação
const confirmacao = reactive({ aberto: false, tipo: '', loading: false })
const textos = computed(() =>
  confirmacao.tipo === 'apagar'
    ? {
        titulo: `Apagar ${user.value?.name}?`,
        descricao:
          'A conta é removida definitivamente. Se tiver publicações, comentários ou avaliações, suspenda-a em vez de a apagar.',
        rotulo: 'Apagar conta',
        destrutivo: true,
      }
    : suspenso.value
      ? {
          titulo: `Reativar ${user.value?.name}?`,
          descricao: 'A pessoa volta a poder entrar na plataforma.',
          rotulo: 'Reativar conta',
          destrutivo: false,
        }
      : {
          titulo: `Suspender ${user.value?.name}?`,
          descricao: 'A pessoa deixa de poder entrar. O que já publicou mantém-se.',
          rotulo: 'Suspender conta',
          destrutivo: true,
        },
)
function pedir(tipo) {
  Object.assign(confirmacao, { aberto: true, tipo, loading: false })
}
async function confirmar() {
  confirmacao.loading = true
  try {
    if (confirmacao.tipo === 'apagar') {
      await store.remove(props.username)
      toast.success('Conta apagada.')
      router.replace({ name: 'users' })
    } else {
      const ativar = suspenso.value
      await store.setStatus(props.username, ativar)
      user.value = { ...user.value, active: ativar }
      toast.success(ativar ? 'Conta reativada.' : 'Conta suspensa.')
    }
    confirmacao.aberto = false
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    confirmacao.loading = false
  }
}
</script>

<template>
  <div class="max-w-2xl">
    <RouterLink
      :to="{ name: 'users' }"
      class="text-muted-foreground hover:text-foreground mb-6 inline-flex items-center gap-1.5 text-sm transition-colors"
    >
      <ArrowLeft class="size-4" aria-hidden="true" /> Utilizadores
    </RouterLink>

    <div v-if="loading" class="space-y-5" aria-hidden="true">
      <Skeleton class="h-16 w-72" /><Skeleton class="h-48 w-full" />
    </div>

    <EmptyState
      v-else-if="naoEncontrado"
      :icon="SearchX"
      title="Utilizador não encontrado"
      description="A conta pode ter sido apagada."
    >
      <Button as-child><RouterLink :to="{ name: 'users' }">Ver utilizadores</RouterLink></Button>
    </EmptyState>

    <div v-else-if="user" class="space-y-10">
      <header class="flex items-center gap-4">
        <UserAvatar :name="user.name" size="lg" />
        <div class="min-w-0">
          <h1 class="display truncate text-[clamp(1.75rem,1.4rem+1.2vw,2.35rem)]">
            {{ user.name }}
          </h1>
          <p class="text-muted-foreground flex flex-wrap items-center gap-x-4 text-sm">
            <span>{{ user.username }}</span>
            <span>{{ roleLabel(user.role) }}</span>
            <span
              class="inline-flex items-center gap-1.5 font-medium"
              :class="suspenso ? 'text-warning' : 'text-success'"
            >
              <span class="size-1.5 rounded-full bg-current" aria-hidden="true" />{{
                suspenso ? 'Suspenso' : 'Ativo'
              }}
            </span>
          </p>
        </div>
      </header>

      <section class="space-y-5" aria-labelledby="h-dados">
        <h2 id="h-dados" class="display text-xl">Dados</h2>
        <form class="space-y-5" novalidate @submit.prevent="guardarDados">
          <div class="grid gap-5 sm:grid-cols-2">
            <div class="space-y-2">
              <Label for="nome">Nome</Label><Input id="nome" v-model="dados.name" />
            </div>
            <div class="space-y-2">
              <Label for="email">E-mail</Label
              ><Input id="email" v-model="dados.email" type="email" />
            </div>
          </div>
          <Button type="submit" :disabled="aGuardarDados"
            ><Loader2 v-if="aGuardarDados" class="animate-spin" /> Guardar dados</Button
          >
        </form>
      </section>

      <section class="space-y-4 border-t pt-8" aria-labelledby="h-perfil">
        <h2 id="h-perfil" class="display text-xl">Perfil de acesso</h2>
        <div class="flex flex-wrap items-center gap-3">
          <NativeSelect v-model="role" class="w-56" :disabled="eEuProprio" aria-label="Perfil">
            <option v-for="(rotulo, valor) in ROLES" :key="valor" :value="valor">
              {{ rotulo }}
            </option>
          </NativeSelect>
          <Button
            variant="outline"
            :disabled="eEuProprio || role === user.role || aGuardarRole"
            @click="guardarRole"
          >
            <Loader2 v-if="aGuardarRole" class="animate-spin" /> Alterar perfil
          </Button>
        </div>
        <p v-if="eEuProprio" class="text-muted-foreground text-sm">
          Não pode alterar o seu próprio perfil, suspender ou apagar a sua conta.
        </p>
      </section>

      <section class="space-y-4 border-t pt-8" aria-labelledby="h-acoes">
        <h2 id="h-acoes" class="display text-xl">Conta</h2>
        <div class="flex flex-wrap gap-3">
          <Button as-child variant="outline"
            ><RouterLink :to="{ name: 'historico', query: { user: user.username } }"
              ><History /> Ver atividade</RouterLink
            ></Button
          >
          <Button variant="outline" :disabled="eEuProprio" @click="pedir('estado')">{{
            suspenso ? 'Reativar conta' : 'Suspender conta'
          }}</Button>
          <Button
            variant="ghost"
            class="text-destructive hover:bg-destructive/10 hover:text-destructive"
            :disabled="eEuProprio"
            @click="pedir('apagar')"
          >
            Apagar conta
          </Button>
        </div>
      </section>
    </div>

    <ConfirmDialog
      v-model:open="confirmacao.aberto"
      :title="textos.titulo"
      :description="textos.descricao"
      :confirm-label="textos.rotulo"
      :destructive="textos.destrutivo"
      :loading="confirmacao.loading"
      @confirm="confirmar"
    />
  </div>
</template>
