<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { History, MessageSquare, FileText, Star, Tag, UserRound } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/stores/auth'
import { useHistoricoStore } from '@/stores/historico'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/lib/errors'
import { GRUPOS_ACAO, acaoInfo, dayKey, formatDayLabel, formatTime } from '@/lib/format'

const ICONES = {
  publicacoes: FileText,
  comentarios: MessageSquare,
  tags: Tag,
  avaliacoes: Star,
  conta: UserRound,
}
const PAGINA = 80

const route = useRoute()
const auth = useAuthStore()
const store = useHistoricoStore()
const userStore = useUserStore()

const alvo = ref(typeof route.query.user === 'string' && auth.isAdmin ? route.query.user : 'me')
const grupo = ref('todos')
const limite = ref(PAGINA)
const verOutros = computed(() => alvo.value !== 'me')

async function carregar() {
  limite.value = PAGINA
  try {
    if (alvo.value === 'me') await store.getMyHistorico()
    else if (alvo.value === 'all') await store.getAllHistorico()
    else await store.getUserHistorico(alvo.value)
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível carregar a atividade.'))
  }
}

onMounted(async () => {
  if (auth.isAdmin) userStore.load().catch(() => {}) // lista para o seletor
  carregar()
})
watch(alvo, carregar)

const ordenados = computed(() =>
  [...store.historico]
    .filter((h) => grupo.value === 'todos' || acaoInfo(h.tipoAcao).grupo === grupo.value)
    .sort((a, b) => new Date(b.dataHora) - new Date(a.dataHora)),
)
const dias = computed(() => {
  const mapa = new Map()
  for (const h of ordenados.value.slice(0, limite.value)) {
    const chave = dayKey(h.dataHora)
    if (!mapa.has(chave)) mapa.set(chave, { chave, rotulo: formatDayLabel(h.dataHora), itens: [] })
    mapa.get(chave).itens.push(h)
  }
  return [...mapa.values()]
})
/** A descrição só aparece quando acrescenta informação (não repete o rótulo nem o título). */
function descricaoUtil(h) {
  const d = (h.descricao || '').trim()
  if (!d) return ''
  const rotulo = acaoInfo(h.tipoAcao).label.toLowerCase()
  if (d.toLowerCase().startsWith(rotulo)) return ''
  if (
    h.tituloPublicacao &&
    d.includes(h.tituloPublicacao) &&
    d.length < h.tituloPublicacao.length + 40
  )
    return ''
  return d
}
const grupos = computed(() => [['todos', 'Tudo'], ...Object.entries(GRUPOS_ACAO)])
</script>

<template>
  <div class="space-y-7">
    <PageHeader
      title="Atividade"
      :description="
        verOutros
          ? 'Tudo o que foi feito na plataforma.'
          : 'O que fez na plataforma: carregamentos, edições, comentários e avaliações.'
      "
    />

    <div class="flex flex-wrap items-center gap-x-5 gap-y-3">
      <label v-if="auth.isAdmin" class="flex items-center gap-2 text-sm">
        <span class="text-muted-foreground">Ver</span>
        <NativeSelect v-model="alvo" size="sm" class="w-56">
          <option value="me">O meu histórico</option>
          <option value="all">Todos os utilizadores</option>
          <option v-for="u in userStore.users" :key="u.username" :value="u.username">
            {{ u.name }} ({{ u.username }})
          </option>
        </NativeSelect>
      </label>

      <div class="flex flex-wrap gap-1.5" role="radiogroup" aria-label="Tipo de atividade">
        <button
          v-for="[valor, rotulo] in grupos"
          :key="valor"
          type="button"
          role="radio"
          :aria-checked="grupo === valor"
          class="rounded-full border px-3.5 py-1 text-[13px] font-medium transition-colors"
          :class="
            grupo === valor
              ? 'bg-primary text-primary-foreground border-primary'
              : 'text-muted-foreground hover:bg-accent hover:text-foreground'
          "
          @click="grupo = valor"
        >
          {{ rotulo }}
        </button>
      </div>
    </div>

    <div v-if="store.loading && !store.historico.length" class="space-y-4" aria-hidden="true">
      <Skeleton v-for="n in 6" :key="n" class="h-14 w-full" />
    </div>

    <EmptyState
      v-else-if="!dias.length"
      :icon="History"
      title="Sem atividade para mostrar"
      :description="
        grupo !== 'todos'
          ? 'Não há registos deste tipo. Experimente mostrar tudo.'
          : 'Quando houver ações, aparecem aqui.'
      "
    >
      <Button v-if="grupo !== 'todos'" variant="outline" @click="grupo = 'todos'"
        >Mostrar tudo</Button
      >
    </EmptyState>

    <div v-else class="space-y-9" :class="store.loading && 'opacity-60'">
      <section v-for="dia in dias" :key="dia.chave" :aria-label="dia.rotulo">
        <h2 class="display text-muted-foreground mb-3 text-lg">{{ dia.rotulo }}</h2>
        <ol class="bg-card divide-y overflow-hidden rounded-2xl border">
          <li v-for="h in dia.itens" :key="h.id" class="flex gap-4 px-5 py-3.5">
            <span
              class="bg-secondary text-muted-foreground mt-0.5 inline-flex size-8 shrink-0 items-center justify-center rounded-full"
              aria-hidden="true"
            >
              <component :is="ICONES[acaoInfo(h.tipoAcao).grupo]" class="size-4" />
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-[15px]">
                <span class="font-medium">{{ acaoInfo(h.tipoAcao).label }}</span>
                <RouterLink
                  v-if="h.publicacaoId"
                  :to="{ name: 'publicacao', params: { id: h.publicacaoId } }"
                  class="text-signal ml-1.5 underline-offset-4 hover:underline"
                >
                  {{ h.tituloPublicacao || `Publicação ${h.publicacaoId}` }}
                </RouterLink>
              </p>
              <p v-if="descricaoUtil(h)" class="text-muted-foreground mt-0.5 line-clamp-2 text-sm">
                {{ descricaoUtil(h) }}
              </p>
            </div>
            <div class="text-muted-foreground shrink-0 text-right text-[13px] tabular-nums">
              <time :datetime="h.dataHora">{{ formatTime(h.dataHora) }}</time>
              <p v-if="verOutros" class="mt-0.5">{{ h.nomeUser || h.username }}</p>
            </div>
          </li>
        </ol>
      </section>

      <div v-if="ordenados.length > limite" class="flex justify-center">
        <Button variant="outline" @click="limite += PAGINA"
          >Mostrar mais ({{ ordenados.length - limite }})</Button
        >
      </div>
    </div>
  </div>
</template>
