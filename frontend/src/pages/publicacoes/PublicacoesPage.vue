<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { onKeyStroke, useDebounceFn } from '@vueuse/core'
import { ChevronLeft, ChevronRight, LibraryBig, Plus, Search, SearchX, X } from 'lucide-vue-next'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PublicacaoRow from '@/components/publicacoes/PublicacaoRow.vue'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { usePublicacaoStore } from '@/stores/publicacao'
import { useTagStore } from '@/stores/tag'
import { errorMessage } from '@/lib/errors'
import { plural } from '@/lib/format'

const PAGE_SIZE = 15
const ORDENACOES = [
  { value: 'data:desc', label: 'Mais recentes' },
  { value: 'rating:desc', label: 'Melhor avaliadas' },
  { value: 'comentarios:desc', label: 'Mais comentadas' },
  { value: 'ratings:desc', label: 'Mais avaliações' },
  { value: 'titulo:asc', label: 'Título (A a Z)' },
  { value: 'ano:desc', label: 'Ano da publicação' },
  { value: 'data:asc', label: 'Mais antigas' },
]
const ESCOPOS = [
  { value: 'todas', label: 'Todas' },
  { value: 'minhas', label: 'As minhas' },
  { value: 'ocultas', label: 'Ocultas' },
]

const route = useRoute()
const router = useRouter()
const store = usePublicacaoStore()
const tagStore = useTagStore()

const q = ref(String(route.query.q ?? ''))
const tag = ref(String(route.query.tag ?? ''))
const anoDe = ref(String(route.query.anoDe ?? ''))
const anoAte = ref(String(route.query.anoAte ?? ''))
const escopo = ref(
  ESCOPOS.some((e) => e.value === route.query.escopo) ? route.query.escopo : 'todas',
)
const ordem = ref(
  ORDENACOES.some((o) => o.value === route.query.ordem) ? route.query.ordem : 'data:desc',
)
const page = ref(Math.max(0, Number(route.query.page) || 0))

const resultado = ref(null)
const loading = ref(true)
const erro = ref('')
const campoPesquisa = ref(null)
let pedido = 0

const filtrosAtivos = computed(
  () => !!(q.value || tag.value || anoDe.value || anoAte.value || escopo.value !== 'todas'),
)
const total = computed(() => resultado.value?.total ?? 0)
const totalPaginas = computed(() => resultado.value?.totalPages ?? 0)
const primeiraCarga = computed(() => loading.value && !resultado.value)

async function carregar() {
  const meu = ++pedido
  loading.value = true
  erro.value = ''
  const [sort, dir] = ordem.value.split(':')
  try {
    const dados = await store.search({
      q: q.value.trim() || undefined,
      tag: tag.value || undefined,
      anoDe: anoDe.value || undefined,
      anoAte: anoAte.value || undefined,
      minhas: escopo.value === 'minhas' || undefined,
      ocultas: escopo.value === 'ocultas' || undefined,
      sort,
      dir,
      page: page.value,
      size: PAGE_SIZE,
    })
    if (meu !== pedido) return // chegou uma resposta mais recente
    resultado.value = dados
    sincronizarUrl()
  } catch (e) {
    if (meu === pedido) erro.value = errorMessage(e, 'Não foi possível carregar as publicações.')
  } finally {
    if (meu === pedido) loading.value = false
  }
}

/** O estado da pesquisa vive no URL: dá para partilhar, voltar atrás e recarregar. */
function sincronizarUrl() {
  const query = {}
  if (q.value.trim()) query.q = q.value.trim()
  if (tag.value) query.tag = tag.value
  if (anoDe.value) query.anoDe = anoDe.value
  if (anoAte.value) query.anoAte = anoAte.value
  if (escopo.value !== 'todas') query.escopo = escopo.value
  if (ordem.value !== 'data:desc') query.ordem = ordem.value
  if (page.value > 0) query.page = String(page.value)
  router.replace({ query })
}

const pesquisarDepois = useDebounceFn(() => {
  page.value = 0
  carregar()
}, 300)

watch([q, anoDe, anoAte], pesquisarDepois)
watch([tag, escopo, ordem], () => {
  page.value = 0
  carregar()
})

function irParaPagina(n) {
  page.value = n
  carregar()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function limpar() {
  q.value = ''
  tag.value = ''
  anoDe.value = ''
  anoAte.value = ''
  escopo.value = 'todas'
}

// "/" salta para a pesquisa (como nas ferramentas de que já se está habituado)
onKeyStroke('/', (e) => {
  const alvo = document.activeElement?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(alvo)) return
  e.preventDefault()
  campoPesquisa.value?.focus()
})

onMounted(async () => {
  carregar()
  try {
    await tagStore.load()
  } catch {
    /* o filtro por tag fica vazio, o resto continua a funcionar */
  }
})
</script>

<template>
  <div class="space-y-7">
    <PageHeader
      title="Publicações"
      description="O que o Centro já adquiriu e produziu, com os resumos, comentários e avaliações dos colegas."
    >
      <template #actions>
        <Button as-child class="lg:hidden"
          ><RouterLink :to="{ name: 'publicacao-create' }"
            ><Plus /> Nova publicação</RouterLink
          ></Button
        >
      </template>
    </PageHeader>

    <!-- pesquisa e filtros -->
    <section aria-label="Pesquisa" class="space-y-3">
      <div class="relative">
        <Search
          class="text-muted-foreground pointer-events-none absolute top-1/2 left-4 size-[18px] -translate-y-1/2"
          aria-hidden="true"
        />
        <input
          ref="campoPesquisa"
          v-model="q"
          type="search"
          placeholder="Pesquisar título, autor ou área"
          aria-label="Pesquisar publicações"
          class="border-input bg-card placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/30 h-12 w-full rounded-xl border pr-12 pl-11 text-base outline-none focus-visible:ring-[3px]"
        />
        <kbd
          v-if="!q"
          class="text-muted-foreground bg-secondary pointer-events-none absolute top-1/2 right-3.5 hidden h-6 min-w-6 -translate-y-1/2 items-center justify-center rounded-md px-1.5 text-xs font-medium sm:inline-flex"
        >
          /
        </kbd>
      </div>

      <div class="flex flex-wrap items-center gap-x-4 gap-y-3">
        <div
          class="bg-secondary flex gap-0.5 rounded-lg p-0.5"
          role="radiogroup"
          aria-label="Âmbito"
        >
          <button
            v-for="e in ESCOPOS"
            :key="e.value"
            type="button"
            role="radio"
            :aria-checked="escopo === e.value"
            class="text-muted-foreground rounded-md px-3 py-1.5 text-[13px] font-medium transition-colors"
            :class="
              escopo === e.value ? 'bg-card text-foreground shadow-xs' : 'hover:text-foreground'
            "
            @click="escopo = e.value"
          >
            {{ e.label }}
          </button>
        </div>

        <label class="flex items-center gap-2 text-sm">
          <span class="text-muted-foreground">Tag</span>
          <NativeSelect v-model="tag" size="sm" class="w-44">
            <option value="">Todas</option>
            <option v-for="t in tagStore.tags" :key="t.tag" :value="t.tag">{{ t.tag }}</option>
          </NativeSelect>
        </label>

        <div class="flex items-center gap-2 text-sm">
          <span class="text-muted-foreground">Ano</span>
          <input
            v-model="anoDe"
            type="number"
            inputmode="numeric"
            min="1900"
            max="2100"
            placeholder="De"
            aria-label="Ano desde"
            class="border-input bg-card focus-visible:border-ring focus-visible:ring-ring/30 h-8 w-20 rounded-md border px-2.5 text-[13px] tabular-nums outline-none focus-visible:ring-[3px]"
          />
          <span class="text-muted-foreground">a</span>
          <input
            v-model="anoAte"
            type="number"
            inputmode="numeric"
            min="1900"
            max="2100"
            placeholder="Até"
            aria-label="Ano até"
            class="border-input bg-card focus-visible:border-ring focus-visible:ring-ring/30 h-8 w-20 rounded-md border px-2.5 text-[13px] tabular-nums outline-none focus-visible:ring-[3px]"
          />
        </div>

        <label class="ml-auto flex items-center gap-2 text-sm">
          <span class="text-muted-foreground">Ordenar</span>
          <NativeSelect v-model="ordem" size="sm" class="w-44">
            <option v-for="o in ORDENACOES" :key="o.value" :value="o.value">{{ o.label }}</option>
          </NativeSelect>
        </label>
      </div>
    </section>

    <!-- resultados -->
    <section aria-label="Resultados">
      <div class="mb-3 flex min-h-7 items-center justify-between gap-3">
        <p class="text-muted-foreground text-sm" role="status" aria-live="polite">
          <template v-if="primeiraCarga">A pesquisar…</template>
          <template v-else-if="resultado">
            {{ plural(total, 'publicação', 'publicações') }}
            <template v-if="totalPaginas > 1">
              (página {{ page + 1 }} de {{ totalPaginas }})</template
            >
          </template>
        </p>
        <Button v-if="filtrosAtivos" variant="ghost" size="sm" @click="limpar"
          ><X /> Limpar filtros</Button
        >
      </div>

      <p
        v-if="erro"
        class="bg-destructive/10 text-destructive flex items-center justify-between gap-3 rounded-xl px-4 py-3 text-sm"
        role="alert"
      >
        {{ erro }}
        <Button variant="outline" size="sm" @click="carregar">Tentar de novo</Button>
      </p>

      <div v-else class="bg-card overflow-hidden rounded-2xl border">
        <ul v-if="primeiraCarga" class="divide-y" aria-hidden="true">
          <li v-for="n in 5" :key="n" class="flex gap-4 px-6 py-5">
            <Skeleton class="w-1.5 self-stretch rounded-full" />
            <div class="flex-1 space-y-3">
              <Skeleton class="h-6 w-2/3" />
              <Skeleton class="h-4 w-1/3" />
              <Skeleton class="h-4 w-5/6" />
            </div>
          </li>
        </ul>

        <ul
          v-else-if="resultado?.items.length"
          class="divide-y transition-opacity duration-150"
          :class="loading && 'opacity-55'"
        >
          <li v-for="p in resultado.items" :key="p.id"><PublicacaoRow :publicacao="p" /></li>
        </ul>

        <EmptyState
          v-else-if="filtrosAtivos"
          :icon="SearchX"
          title="Nenhuma publicação corresponde à pesquisa"
          description="Experimente outras palavras, retire algum filtro ou alargue o intervalo de anos."
        >
          <Button variant="outline" @click="limpar">Limpar filtros</Button>
        </EmptyState>

        <EmptyState
          v-else
          :icon="LibraryBig"
          title="Ainda não há publicações"
          description="Carregue o primeiro PDF ou ZIP para começar a construir a biblioteca do Centro."
        >
          <Button as-child
            ><RouterLink :to="{ name: 'publicacao-create' }"
              ><Plus /> Nova publicação</RouterLink
            ></Button
          >
        </EmptyState>
      </div>

      <nav
        v-if="totalPaginas > 1"
        class="mt-5 flex items-center justify-between"
        aria-label="Paginação"
      >
        <Button
          variant="outline"
          size="sm"
          :disabled="page === 0 || loading"
          @click="irParaPagina(page - 1)"
        >
          <ChevronLeft /> Anterior
        </Button>
        <span class="text-muted-foreground text-sm tabular-nums"
          >Página {{ page + 1 }} de {{ totalPaginas }}</span
        >
        <Button
          variant="outline"
          size="sm"
          :disabled="page + 1 >= totalPaginas || loading"
          @click="irParaPagina(page + 1)"
        >
          Seguinte <ChevronRight />
        </Button>
      </nav>
    </section>
  </div>
</template>
