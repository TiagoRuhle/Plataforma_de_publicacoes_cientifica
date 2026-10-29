<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bell, BellRing, Loader2, Plus, Search, Tags as TagsIcon, Trash2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import TagChip from '@/components/publicacoes/TagChip.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/stores/auth'
import { useTagStore } from '@/stores/tag'
import { errorMessage } from '@/lib/errors'

const auth = useAuthStore()
const tagStore = useTagStore()

const loading = ref(true)
const pesquisa = ref('')
const filtro = ref('todas') // todas | subscritas | ocultas
const aTratar = ref(null) // nome da tag em processamento

const subscritas = computed(() => new Set(tagStore.subscricoes.map((s) => s.nome)))
const filtros = computed(() => [
  { value: 'todas', label: 'Todas' },
  { value: 'subscritas', label: 'Subscritas' },
  ...(auth.isGestor ? [{ value: 'ocultas', label: 'Ocultas' }] : []),
])
const visiveis = computed(() =>
  tagStore.tags
    .filter((t) => t.tag.toLowerCase().includes(pesquisa.value.trim().toLowerCase()))
    .filter((t) => {
      if (filtro.value === 'subscritas') return subscritas.value.has(t.tag)
      if (filtro.value === 'ocultas') return t.visivel === false
      return true
    })
    .sort((a, b) => a.tag.localeCompare(b.tag, 'pt')),
)

onMounted(async () => {
  try {
    await Promise.all([tagStore.load(), tagStore.loadSubscricoes()])
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível carregar as tags.'))
  } finally {
    loading.value = false
  }
})

async function alternarSubscricao(nome) {
  aTratar.value = nome
  const subscrita = subscritas.value.has(nome)
  try {
    if (subscrita) await tagStore.unsubscribe(nome)
    else await tagStore.subscribe(nome)
    toast.success(
      subscrita
        ? `Subscrição de "${nome}" cancelada.`
        : `Vai receber um e-mail sobre novidades em "${nome}".`,
    )
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    aTratar.value = null
  }
}

// ---- nova tag (responsável/admin)
const dialogoNova = ref(false)
const nova = reactive({ nome: '', visivel: true })
const erroNova = ref('')
const aCriar = ref(false)

function abrirNova() {
  Object.assign(nova, { nome: '', visivel: true })
  erroNova.value = ''
  dialogoNova.value = true
}
async function criar() {
  erroNova.value = ''
  const nome = nova.nome.trim()
  if (!nome) {
    erroNova.value = 'Indique o nome da tag.'
    return
  }
  if (/[\\/]/.test(nome)) {
    erroNova.value = 'O nome não pode conter / nem \\.'
    return
  }
  aCriar.value = true
  try {
    await tagStore.create({ tag: nome, visivel: nova.visivel })
    dialogoNova.value = false
    toast.success(`Tag "${nome}" criada.`)
  } catch (e) {
    erroNova.value =
      e.response?.status === 409 ? 'Já existe uma tag com este nome.' : errorMessage(e)
  } finally {
    aCriar.value = false
  }
}

// ---- apagar tag
const apagar = reactive({ aberto: false, nome: '', loading: false })
function pedirApagar(nome) {
  Object.assign(apagar, { aberto: true, nome, loading: false })
}
async function confirmarApagar() {
  apagar.loading = true
  try {
    await tagStore.remove(apagar.nome)
    toast.success(`Tag "${apagar.nome}" apagada.`)
    apagar.aberto = false
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    apagar.loading = false
  }
}
</script>

<template>
  <div class="space-y-7">
    <PageHeader
      title="Tags"
      description="Subscreva uma tag para receber um e-mail sempre que alguém lhe associar uma publicação ou acrescentar conteúdo a uma publicação com essa tag."
    >
      <template #actions>
        <Button v-if="auth.isGestor" @click="abrirNova"><Plus /> Nova tag</Button>
      </template>
    </PageHeader>

    <div class="flex flex-wrap items-center gap-3">
      <div class="relative min-w-56 flex-1 sm:max-w-xs">
        <Search
          class="text-muted-foreground pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2"
          aria-hidden="true"
        />
        <input
          v-model="pesquisa"
          type="search"
          placeholder="Filtrar tags"
          aria-label="Filtrar tags"
          class="border-input bg-card placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/30 h-10 w-full rounded-lg border pr-3 pl-10 text-[15px] outline-none focus-visible:ring-[3px]"
        />
      </div>
      <div
        class="bg-secondary flex gap-0.5 rounded-lg p-0.5"
        role="radiogroup"
        aria-label="Mostrar"
      >
        <button
          v-for="f in filtros"
          :key="f.value"
          type="button"
          role="radio"
          :aria-checked="filtro === f.value"
          class="text-muted-foreground rounded-md px-3 py-1.5 text-[13px] font-medium transition-colors"
          :class="
            filtro === f.value ? 'bg-card text-foreground shadow-xs' : 'hover:text-foreground'
          "
          @click="filtro = f.value"
        >
          {{ f.label }}
        </button>
      </div>
    </div>

    <div class="bg-card overflow-hidden rounded-2xl border">
      <ul v-if="loading" class="divide-y" aria-hidden="true">
        <li v-for="n in 5" :key="n" class="flex items-center justify-between px-5 py-4">
          <Skeleton class="h-7 w-40" /><Skeleton class="h-9 w-32" />
        </li>
      </ul>

      <ul v-else-if="visiveis.length" class="divide-y">
        <li
          v-for="t in visiveis"
          :key="t.tag"
          class="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5"
        >
          <RouterLink
            :to="{ name: 'publicacoes', query: { tag: t.tag } }"
            class="group flex min-w-0 items-center gap-3"
            :title="`Ver publicações com a tag ${t.tag}`"
          >
            <TagChip :name="t.tag" size="md" :hidden="t.visivel === false" />
            <span
              class="text-muted-foreground text-sm opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100"
              >Ver publicações</span
            >
          </RouterLink>

          <div class="flex items-center gap-2">
            <Button
              :variant="subscritas.has(t.tag) ? 'secondary' : 'outline'"
              size="sm"
              :aria-pressed="subscritas.has(t.tag)"
              :disabled="aTratar === t.tag"
              @click="alternarSubscricao(t.tag)"
            >
              <Loader2 v-if="aTratar === t.tag" class="animate-spin" />
              <BellRing v-else-if="subscritas.has(t.tag)" class="text-signal" />
              <Bell v-else />
              {{ subscritas.has(t.tag) ? 'Subscrita' : 'Subscrever' }}
            </Button>
            <Button
              v-if="auth.isGestor"
              variant="ghost"
              size="icon-sm"
              class="text-muted-foreground hover:text-destructive"
              :aria-label="`Apagar a tag ${t.tag}`"
              @click="pedirApagar(t.tag)"
            >
              <Trash2 />
            </Button>
          </div>
        </li>
      </ul>

      <EmptyState
        v-else
        :icon="TagsIcon"
        :title="
          pesquisa || filtro !== 'todas' ? 'Nenhuma tag corresponde ao filtro' : 'Ainda não há tags'
        "
        :description="
          pesquisa || filtro !== 'todas'
            ? 'Experimente outro nome ou mostre todas as tags.'
            : auth.isGestor
              ? 'Crie a primeira tag para começar a organizar as publicações.'
              : 'Um responsável do Centro pode criar tags para organizar as publicações.'
        "
      >
        <Button v-if="!pesquisa && filtro === 'todas' && auth.isGestor" @click="abrirNova"
          ><Plus /> Nova tag</Button
        >
      </EmptyState>
    </div>

    <!-- nova tag -->
    <Dialog v-model:open="dialogoNova">
      <DialogContent>
        <form class="grid gap-5" novalidate @submit.prevent="criar">
          <DialogHeader>
            <DialogTitle>Nova tag</DialogTitle>
            <DialogDescription
              >Use um nome curto, por exemplo o de um projeto ou tema.</DialogDescription
            >
          </DialogHeader>
          <div class="space-y-2">
            <Label for="nome-tag">Nome</Label>
            <Input
              id="nome-tag"
              v-model="nova.nome"
              maxlength="100"
              autofocus
              :aria-invalid="!!erroNova"
            />
            <p v-if="erroNova" class="text-destructive text-sm" role="alert">{{ erroNova }}</p>
          </div>
          <label class="flex items-start gap-3 text-sm">
            <input v-model="nova.visivel" type="checkbox" class="accent-primary mt-0.5 size-4" />
            <span>
              <span class="font-medium">Visível para todos</span>
              <span class="text-muted-foreground block"
                >Se desmarcar, só responsáveis e administradores a veem.</span
              >
            </span>
          </label>
          <DialogFooter>
            <Button type="button" variant="ghost" :disabled="aCriar" @click="dialogoNova = false"
              >Cancelar</Button
            >
            <Button type="submit" :disabled="aCriar"
              ><Loader2 v-if="aCriar" class="animate-spin" /> Criar tag</Button
            >
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <ConfirmDialog
      v-model:open="apagar.aberto"
      :title="`Apagar a tag &quot;${apagar.nome}&quot;?`"
      description="A tag é removida de todas as publicações e as subscrições dos colegas são canceladas. Esta ação não pode ser desfeita."
      confirm-label="Apagar tag"
      destructive
      :loading="apagar.loading"
      @confirm="confirmarApagar"
    />
  </div>
</template>
