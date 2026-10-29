<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowLeft,
  Download,
  Eye,
  EyeOff,
  FileArchive,
  FileText,
  History,
  Loader2,
  MessageSquare,
  PencilLine,
  Plus,
  RefreshCw,
  SearchX,
  Sparkles,
  Trash2,
  TriangleAlert,
} from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import StarRating from '@/components/common/StarRating.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import TagChip from '@/components/publicacoes/TagChip.vue'
import TagSpine from '@/components/publicacoes/TagSpine.vue'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { useAPIStore } from '@/stores/api'
import { useAuthStore } from '@/stores/auth'
import { usePublicacaoStore } from '@/stores/publicacao'
import { useTagStore } from '@/stores/tag'
import { errorMessage } from '@/lib/errors'
import { filenameFromDisposition, saveBlob } from '@/lib/download'
import {
  acaoInfo,
  formatDate,
  formatDateTime,
  formatRating,
  formatRelative,
  plural,
} from '@/lib/format'
import { renderResumo } from '@/lib/markdownLite'

const props = defineProps({ id: { type: String, required: true } })

const api = useAPIStore()
const auth = useAuthStore()
const store = usePublicacaoStore()
const tagStore = useTagStore()

const p = ref(null)
const loading = ref(true)
const naoEncontrada = ref(false)
const erro = ref('')

const username = computed(() => auth.currentUser?.username)
const isOwner = computed(() => !!p.value && p.value.submetidoPor === username.value)
const canEdit = computed(() => isOwner.value || auth.isGestor)
const meuRating = computed(
  () => p.value?.ratings?.find((r) => r.autor === username.value)?.rating ?? 0,
)
const eZip = computed(() => p.value?.ficheiro?.toLowerCase().endsWith('.zip'))
const estado = computed(() => p.value?.resumoEstado)
const podeRegenerar = computed(
  () => canEdit.value && !!p.value && !eZip.value && estado.value !== 'PENDENTE',
)
const resumoHtml = computed(() => renderResumo(p.value?.resumoIa))
const tagsDisponiveis = computed(() =>
  tagStore.tags.filter((t) => !p.value?.tags.some((x) => x.tag === t.tag)),
)

// ------------------------------------------------------------------ carregar
async function carregar() {
  try {
    p.value = await store.get(props.id)
    naoEncontrada.value = false
    erro.value = ''
  } catch (e) {
    if (e.response?.status === 404) naoEncontrada.value = true
    else erro.value = errorMessage(e, 'Não foi possível carregar a publicação.')
  } finally {
    loading.value = false
  }
}

// Enquanto a IA trabalha, vai-se perguntando ao servidor (para ao fim de 10 minutos).
let temporizador = null
let inicioEspera = 0
function pararEspera() {
  clearInterval(temporizador)
  temporizador = null
}
function iniciarEspera() {
  if (temporizador) return
  inicioEspera = Date.now()
  temporizador = setInterval(async () => {
    if (Date.now() - inicioEspera > 10 * 60 * 1000) return pararEspera()
    try {
      p.value = await store.get(props.id)
    } catch {
      /* tenta de novo no próximo ciclo */
    }
  }, 4000)
}
watch(estado, (e) => (e === 'PENDENTE' ? iniciarEspera() : pararEspera()), { immediate: true })

onMounted(async () => {
  await carregar()
  tagStore.load().catch(() => {})
})
onBeforeUnmount(pararEspera)

// ------------------------------------------------------------------ confirmações
const confirmacao = reactive({
  aberto: false,
  titulo: '',
  descricao: '',
  rotulo: 'Confirmar',
  destrutivo: false,
  acao: null,
  loading: false,
})
function pedirConfirmacao({
  titulo,
  descricao = '',
  rotulo = 'Confirmar',
  destrutivo = false,
  acao,
}) {
  Object.assign(confirmacao, {
    aberto: true,
    titulo,
    descricao,
    rotulo,
    destrutivo,
    acao,
    loading: false,
  })
}
async function executarConfirmacao() {
  confirmacao.loading = true
  try {
    await confirmacao.acao()
    confirmacao.aberto = false
  } finally {
    confirmacao.loading = false
  }
}

/** Executa uma ação da página com toast de erro, e recarrega a publicação. */
async function correr(acao, sucesso) {
  try {
    await acao()
    await carregar()
    if (sucesso) toast.success(sucesso)
    return true
  } catch (e) {
    toast.error(errorMessage(e))
    return false
  }
}

// ------------------------------------------------------------------ ficheiro
const aDescarregar = ref(false)
async function descarregar() {
  aDescarregar.value = true
  try {
    const r = await api.downloadPublicacao(props.id)
    saveBlob(r.data, filenameFromDisposition(r.headers?.['content-disposition'], p.value.ficheiro))
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível descarregar o ficheiro.'))
  } finally {
    aDescarregar.value = false
  }
}

// ------------------------------------------------------------------ resumo
const editando = ref(false)
const rascunho = ref('')
const aGuardar = ref(false)
// Controlo de concorrência: guarda-se a versão que a pessoa viu ao abrir a edição. Se, ao guardar, o servidor
// tiver outra (alguém alterou entretanto), responde 409 e o texto escrito NÃO se perde.
const versaoEdicao = ref(null)
const conflito = ref(false)

function editarResumo() {
  rascunho.value = estado.value === 'ERRO' ? '' : (p.value.resumoIa ?? '')
  versaoEdicao.value = p.value.version
  conflito.value = false
  editando.value = true
}
function cancelarEdicao() {
  editando.value = false
  conflito.value = false
}
async function guardarResumo() {
  const texto = rascunho.value.trim()
  if (!texto) return toast.error('O resumo não pode estar vazio.')
  aGuardar.value = true
  try {
    await store.update(props.id, { resumo: texto, version: versaoEdicao.value })
    await carregar()
    toast.success('Resumo guardado.')
    editando.value = false
    conflito.value = false
  } catch (e) {
    if (e.response?.status === 409) {
      conflito.value = true
      await carregar() // traz a versão atual, para a pessoa ver o que mudou
      versaoEdicao.value = p.value?.version ?? null // se voltar a guardar, substitui de forma consciente
    } else {
      toast.error(errorMessage(e))
    }
  } finally {
    aGuardar.value = false
  }
}
function regenerar() {
  const pedir = () => correr(() => store.regenerarResumo(props.id), 'A gerar um novo resumo…')
  if (estado.value === 'EDITADO') {
    pedirConfirmacao({
      titulo: 'Substituir o resumo corrigido?',
      descricao:
        'O resumo atual foi corrigido manualmente. Gerar um novo resumo com IA vai substituí-lo.',
      rotulo: 'Gerar novo resumo',
      acao: pedir,
    })
  } else {
    pedir()
  }
}

// ------------------------------------------------------------------ avaliação
const aAvaliar = ref(false)
async function avaliar(n) {
  aAvaliar.value = true
  await correr(() => store.rate(props.id, n), 'Avaliação guardada.')
  aAvaliar.value = false
}

// ------------------------------------------------------------------ tags
const adicionarTag = ref(false)
const tagEscolhida = ref('')
async function associarTag() {
  if (!tagEscolhida.value) return
  const ok = await correr(() => store.addTag(props.id, tagEscolhida.value), 'Tag associada.')
  if (ok) {
    tagEscolhida.value = ''
    adicionarTag.value = false
  }
}
function desassociarTag(nome) {
  pedirConfirmacao({
    titulo: `Desassociar a tag "${nome}"?`,
    descricao: 'A publicação deixa de aparecer nas pesquisas por esta tag.',
    rotulo: 'Desassociar',
    destrutivo: true,
    acao: () => correr(() => store.removeTag(props.id, nome), 'Tag desassociada.'),
  })
}

// ------------------------------------------------------------------ visibilidade
function alternarVisibilidade() {
  const ocultar = p.value.visivel
  correr(
    () => (ocultar ? store.ocultar(props.id) : store.mostrar(props.id)),
    ocultar ? 'Publicação ocultada.' : 'Publicação visível para todos.',
  )
}

// ------------------------------------------------------------------ comentários
const novoComentario = ref('')
const aComentar = ref(false)
const LIMITE_COMENTARIO = 2000
async function comentar() {
  const texto = novoComentario.value.trim()
  if (!texto || aComentar.value) return
  aComentar.value = true
  const ok = await correr(() => store.addComentario(props.id, texto))
  aComentar.value = false
  if (ok) novoComentario.value = ''
}
function apagarComentario(c) {
  pedirConfirmacao({
    titulo: 'Apagar o comentário?',
    descricao: 'Esta ação não pode ser desfeita.',
    rotulo: 'Apagar',
    destrutivo: true,
    acao: () => correr(() => store.deleteComentario(c.id), 'Comentário apagado.'),
  })
}
const moderar = (c) =>
  correr(() => (c.visivel ? store.ocultarComentario(c.id) : store.mostrarComentario(c.id)))
const podeApagar = (c) => c.autor === username.value || auth.isGestor

// ------------------------------------------------------------------ histórico
const historicoAberto = ref(false)
const historico = ref([])
const aCarregarHistorico = ref(false)
async function alternarHistorico() {
  historicoAberto.value = !historicoAberto.value
  if (!historicoAberto.value || historico.value.length) return
  aCarregarHistorico.value = true
  try {
    historico.value = [...(await store.getHistorico(props.id))].sort(
      (a, b) => new Date(b.dataHora) - new Date(a.dataHora),
    )
  } catch (e) {
    toast.error(errorMessage(e))
    historicoAberto.value = false
  } finally {
    aCarregarHistorico.value = false
  }
}
</script>

<template>
  <div>
    <RouterLink
      :to="{ name: 'publicacoes' }"
      class="text-muted-foreground hover:text-foreground mb-6 inline-flex items-center gap-1.5 text-sm transition-colors"
    >
      <ArrowLeft class="size-4" aria-hidden="true" /> Publicações
    </RouterLink>

    <!-- a carregar -->
    <div v-if="loading" class="space-y-5" aria-hidden="true">
      <Skeleton class="h-12 w-3/4" />
      <Skeleton class="h-5 w-1/3" />
      <Skeleton class="mt-10 h-40 w-full max-w-[68ch]" />
    </div>

    <EmptyState
      v-else-if="naoEncontrada"
      :icon="SearchX"
      title="Publicação não encontrada"
      description="O endereço pode estar errado ou a publicação estar oculta."
    >
      <Button as-child
        ><RouterLink :to="{ name: 'publicacoes' }">Ver todas as publicações</RouterLink></Button
      >
    </EmptyState>

    <p
      v-else-if="erro && !p"
      class="bg-destructive/10 text-destructive rounded-xl px-4 py-3 text-sm"
      role="alert"
    >
      {{ erro }}
    </p>

    <article v-else-if="p" class="space-y-10">
      <!-- cabeçalho -->
      <header class="flex gap-5">
        <TagSpine :tags="p.tags" class="hidden sm:flex" />
        <div class="min-w-0 flex-1 space-y-4">
          <div class="flex flex-wrap items-center gap-2 text-sm">
            <span
              class="bg-secondary text-secondary-foreground rounded-md px-2 py-0.5 text-[13px] font-medium"
              >{{ p.tipo }}</span
            >
            <span
              v-if="!p.visivel"
              class="bg-warning/15 text-warning inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[13px] font-medium"
            >
              <EyeOff class="size-3.5" aria-hidden="true" /> Oculta
            </span>
          </div>
          <h1
            class="display max-w-[32ch]"
            :class="
              p.titulo.length > 70
                ? 'text-[clamp(1.75rem,1.3rem+1.6vw,2.5rem)]'
                : 'text-[clamp(2rem,1.4rem+2.4vw,3.25rem)]'
            "
          >
            {{ p.titulo }}
          </h1>
          <p class="text-muted-foreground flex flex-wrap gap-x-5 gap-y-1 text-[15px]">
            <span class="text-foreground font-medium">{{ p.autor }}</span>
            <span>{{ p.areaCientifica }}</span>
            <span class="tabular-nums">{{ p.ano }}</span>
          </p>

          <div class="flex flex-wrap items-center gap-2">
            <TagChip
              v-for="t in p.tags"
              :key="t.tag"
              :name="t.tag"
              size="md"
              :hidden="t.visivel === false"
              :removable="auth.isGestor"
              @remove="desassociarTag(t.tag)"
            />
            <span v-if="!p.tags.length" class="text-muted-foreground text-sm">Sem tags.</span>

            <template v-if="!adicionarTag">
              <Button
                variant="ghost"
                size="sm"
                :disabled="!tagsDisponiveis.length"
                @click="adicionarTag = true"
              >
                <Plus /> Tag
              </Button>
            </template>
            <form v-else class="flex items-center gap-2" @submit.prevent="associarTag">
              <NativeSelect v-model="tagEscolhida" size="sm" class="w-44" aria-label="Escolher tag">
                <option value="" disabled>Escolher tag…</option>
                <option v-for="t in tagsDisponiveis" :key="t.tag" :value="t.tag">
                  {{ t.tag }}
                </option>
              </NativeSelect>
              <Button type="submit" size="sm" :disabled="!tagEscolhida">Associar</Button>
              <Button type="button" variant="ghost" size="sm" @click="adicionarTag = false"
                >Cancelar</Button
              >
            </form>
          </div>
        </div>
      </header>

      <div class="grid gap-12 lg:grid-cols-[minmax(0,1fr)_19rem]">
        <!-- coluna principal -->
        <div class="min-w-0 space-y-14">
          <!-- resumo -->
          <section aria-labelledby="h-resumo">
            <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <h2 id="h-resumo" class="display text-2xl">Resumo</h2>
                <span
                  v-if="estado === 'CONCLUIDO'"
                  class="text-muted-foreground inline-flex items-center gap-1.5 text-[13px]"
                >
                  <Sparkles class="text-signal size-3.5" aria-hidden="true" /> Gerado por IA
                </span>
                <span
                  v-else-if="estado === 'EDITADO'"
                  class="text-muted-foreground inline-flex items-center gap-1.5 text-[13px]"
                >
                  <PencilLine class="size-3.5" aria-hidden="true" /> Corrigido manualmente
                </span>
                <span
                  v-else-if="estado === 'PENDENTE'"
                  class="text-muted-foreground inline-flex items-center gap-1.5 text-[13px]"
                >
                  <Loader2 class="size-3.5 animate-spin" aria-hidden="true" /> A gerar
                </span>
                <span
                  v-else-if="estado === 'ERRO'"
                  class="text-warning inline-flex items-center gap-1.5 text-[13px]"
                >
                  <TriangleAlert class="size-3.5" aria-hidden="true" /> Falhou
                </span>
              </div>
              <div v-if="canEdit && !editando" class="flex gap-1">
                <Button variant="ghost" size="sm" @click="editarResumo"
                  ><PencilLine /> Editar</Button
                >
                <Button v-if="podeRegenerar" variant="ghost" size="sm" @click="regenerar"
                  ><RefreshCw /> Gerar de novo</Button
                >
              </div>
            </div>

            <div v-if="editando" class="space-y-3">
              <div
                v-if="conflito"
                class="bg-warning/10 space-y-2 rounded-xl p-4 text-sm"
                role="alert"
              >
                <p class="font-medium">
                  Esta publicação foi alterada por outra pessoa enquanto editava.
                </p>
                <p>
                  O seu texto foi mantido. Abaixo está o resumo atual; se voltar a guardar, o seu
                  texto substitui-o.
                </p>
                <p
                  v-if="p.resumoIa"
                  class="bg-card max-h-40 overflow-auto rounded-lg border p-3 whitespace-pre-line"
                >
                  {{ p.resumoIa }}
                </p>
              </div>
              <Textarea
                v-model="rascunho"
                class="reading min-h-56 max-w-none"
                aria-label="Resumo"
              />
              <div class="flex justify-end gap-2">
                <Button variant="ghost" :disabled="aGuardar" @click="cancelarEdicao"
                  >Cancelar</Button
                >
                <Button :disabled="aGuardar || !rascunho.trim()" @click="guardarResumo">
                  <Loader2 v-if="aGuardar" class="animate-spin" />
                  {{ conflito ? 'Guardar mesmo assim' : 'Guardar resumo' }}
                </Button>
              </div>
            </div>

            <div v-else-if="estado === 'PENDENTE'" class="max-w-[68ch] space-y-3" role="status">
              <p class="text-muted-foreground text-[15px]">
                A IA está a ler o documento. Pode demorar alguns minutos e esta página atualiza
                sozinha.
              </p>
              <Skeleton class="h-4 w-full" />
              <Skeleton class="h-4 w-11/12" />
              <Skeleton class="h-4 w-4/5" />
            </div>

            <div
              v-else-if="estado === 'ERRO'"
              class="bg-warning/10 max-w-[68ch] space-y-3 rounded-xl p-4"
            >
              <p class="text-sm">{{ p.resumoIa }}</p>
              <div v-if="canEdit && podeRegenerar" class="flex flex-wrap gap-2">
                <Button size="sm" @click="regenerar"><RefreshCw /> Tentar novamente</Button>
                <Button size="sm" variant="outline" @click="editarResumo">Escrever o resumo</Button>
              </div>
            </div>

            <div
              v-else-if="estado === 'NAO_APLICAVEL'"
              class="text-muted-foreground max-w-[68ch] text-[15px]"
            >
              <p>Os ficheiros ZIP não têm resumo automático.</p>
              <p v-if="canEdit" class="mt-1">Pode escrever um resumo manualmente.</p>
            </div>

            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-else class="reading" v-html="resumoHtml" />

            <p
              v-if="estado === 'CONCLUIDO' && !editando"
              class="text-muted-foreground mt-4 max-w-[68ch] text-[13px]"
            >
              Os resumos gerados por IA podem conter imprecisões. Confirme no documento original.
            </p>
          </section>

          <!-- comentários -->
          <section aria-labelledby="h-comentarios">
            <h2 id="h-comentarios" class="display mb-5 text-2xl">
              Comentários
              <span class="text-muted-foreground text-lg tabular-nums">{{
                p.comentarios.length
              }}</span>
            </h2>

            <ul v-if="p.comentarios.length" class="divide-y">
              <li
                v-for="c in p.comentarios"
                :key="c.id"
                class="flex gap-3.5 py-4 first:pt-0"
                :class="!c.visivel && 'opacity-60'"
              >
                <UserAvatar :name="c.autor" />
                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-baseline gap-x-3 gap-y-0.5">
                    <span class="font-medium">{{ c.autor }}</span>
                    <time
                      class="text-muted-foreground text-[13px]"
                      :datetime="c.data"
                      :title="formatDateTime(c.data)"
                      >{{ formatRelative(c.data) }}</time
                    >
                    <span
                      v-if="!c.visivel"
                      class="bg-warning/15 text-warning rounded px-1.5 text-xs font-medium"
                      >Oculto</span
                    >
                  </div>
                  <p class="mt-1 text-[15px] leading-relaxed whitespace-pre-line">{{ c.texto }}</p>
                  <div v-if="podeApagar(c) || auth.isGestor" class="mt-1.5 flex gap-1">
                    <Button
                      v-if="auth.isGestor"
                      variant="ghost"
                      size="sm"
                      class="text-muted-foreground -ml-2"
                      @click="moderar(c)"
                    >
                      <EyeOff v-if="c.visivel" /><Eye v-else />
                      {{ c.visivel ? 'Ocultar' : 'Mostrar' }}
                    </Button>
                    <Button
                      v-if="podeApagar(c)"
                      variant="ghost"
                      size="sm"
                      class="text-muted-foreground hover:text-destructive"
                      @click="apagarComentario(c)"
                    >
                      <Trash2 /> Apagar
                    </Button>
                  </div>
                </div>
              </li>
            </ul>
            <p v-else class="text-muted-foreground mb-5 text-[15px]">
              Ainda ninguém comentou. Partilhe com os colegas porque é (ou não) relevante.
            </p>

            <form class="mt-6 space-y-3" @submit.prevent="comentar">
              <Textarea
                v-model="novoComentario"
                placeholder="Escreva um comentário para os colegas…"
                aria-label="Novo comentário"
                :maxlength="LIMITE_COMENTARIO"
                @keydown.ctrl.enter.prevent="comentar"
                @keydown.meta.enter.prevent="comentar"
              />
              <div class="flex items-center justify-between gap-3">
                <span
                  class="text-muted-foreground text-xs tabular-nums"
                  :class="novoComentario.length > LIMITE_COMENTARIO - 100 && 'text-warning'"
                >
                  {{ novoComentario.length }} / {{ LIMITE_COMENTARIO }}
                </span>
                <Button type="submit" :disabled="!novoComentario.trim() || aComentar">
                  <Loader2 v-if="aComentar" class="animate-spin" /><MessageSquare v-else /> Comentar
                </Button>
              </div>
            </form>
          </section>

          <!-- histórico -->
          <section v-if="canEdit" aria-labelledby="h-historico">
            <h2 id="h-historico" class="sr-only">Histórico de edições</h2>
            <Button
              variant="outline"
              size="sm"
              :aria-expanded="historicoAberto"
              @click="alternarHistorico"
            >
              <History /> {{ historicoAberto ? 'Esconder histórico' : 'Ver histórico de edições' }}
            </Button>
            <div v-if="historicoAberto" class="mt-4">
              <Skeleton v-if="aCarregarHistorico" class="h-24 w-full max-w-xl" />
              <ol v-else class="max-w-xl space-y-3">
                <li v-for="h in historico" :key="h.id" class="flex gap-3 text-sm">
                  <span class="bg-border mt-2 size-1.5 shrink-0 rounded-full" aria-hidden="true" />
                  <div>
                    <p>
                      <span class="font-medium">{{ acaoInfo(h.tipoAcao).label }}</span>
                      <span class="text-muted-foreground"> por {{ h.nomeUser || h.username }}</span>
                    </p>
                    <p v-if="h.descricao" class="text-muted-foreground">{{ h.descricao }}</p>
                    <time class="text-muted-foreground text-xs" :datetime="h.dataHora">{{
                      formatDateTime(h.dataHora)
                    }}</time>
                  </div>
                </li>
              </ol>
            </div>
          </section>
        </div>

        <!-- margem -->
        <aside class="space-y-5 lg:sticky lg:top-8 lg:self-start" aria-label="Detalhes">
          <section class="bg-card space-y-5 rounded-2xl border p-5" aria-labelledby="h-avaliacao">
            <h2 id="h-avaliacao" class="sr-only">Avaliação</h2>
            <div class="flex items-end gap-4">
              <span class="display text-5xl tabular-nums">{{
                p.totalRatings ? formatRating(p.ratingMedio) : '—'
              }}</span>
              <div class="pb-1.5">
                <StarRating readonly :model-value="p.ratingMedio" />
                <p class="text-muted-foreground mt-0.5 text-xs">
                  {{ plural(p.totalRatings, 'avaliação', 'avaliações') }}
                </p>
              </div>
            </div>
            <div class="border-t pt-4">
              <p class="mb-1.5 text-sm font-medium">
                {{ meuRating ? 'A sua avaliação' : 'Avalie esta publicação' }}
              </p>
              <StarRating
                :model-value="meuRating"
                size="lg"
                :disabled="aAvaliar"
                label="A sua avaliação"
                @rate="avaliar"
              />
            </div>
          </section>

          <section class="bg-card space-y-4 rounded-2xl border p-5" aria-labelledby="h-ficheiro">
            <h2 id="h-ficheiro" class="sr-only">Ficheiro</h2>
            <div class="flex items-center gap-3">
              <span
                class="bg-secondary text-muted-foreground inline-flex size-10 shrink-0 items-center justify-center rounded-lg"
              >
                <FileArchive v-if="eZip" class="size-5" aria-hidden="true" />
                <FileText v-else class="size-5" aria-hidden="true" />
              </span>
              <p class="min-w-0 truncate text-sm font-medium" :title="p.ficheiro">
                {{ p.ficheiro }}
              </p>
            </div>
            <Button variant="outline" class="w-full" :disabled="aDescarregar" @click="descarregar">
              <Loader2 v-if="aDescarregar" class="animate-spin" /><Download v-else /> Descarregar
            </Button>

            <dl
              class="grid grid-cols-[auto_1fr] items-center gap-x-4 gap-y-2.5 border-t pt-4 text-sm"
            >
              <dt class="text-muted-foreground">Submetida por</dt>
              <dd class="flex items-center justify-end gap-2 text-right font-medium">
                <UserAvatar :name="p.submetidoPor" size="sm" /> {{ p.submetidoPor }}
              </dd>
              <dt class="text-muted-foreground">Em</dt>
              <dd class="text-right tabular-nums">{{ formatDate(p.dataSubmissao) }}</dd>
            </dl>
          </section>

          <Button
            v-if="auth.isGestor"
            variant="outline"
            class="w-full"
            @click="alternarVisibilidade"
          >
            <EyeOff v-if="p.visivel" /><Eye v-else />
            {{ p.visivel ? 'Ocultar publicação' : 'Tornar visível a todos' }}
          </Button>
        </aside>
      </div>
    </article>

    <ConfirmDialog
      v-model:open="confirmacao.aberto"
      :title="confirmacao.titulo"
      :description="confirmacao.descricao"
      :confirm-label="confirmacao.rotulo"
      :destructive="confirmacao.destrutivo"
      :loading="confirmacao.loading"
      @confirm="executarConfirmacao"
    />
  </div>
</template>
