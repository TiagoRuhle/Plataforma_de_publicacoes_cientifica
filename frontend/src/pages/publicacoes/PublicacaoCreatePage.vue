<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2, Sparkles } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import PageHeader from '@/components/common/PageHeader.vue'
import FileDropzone from '@/components/publicacoes/FileDropzone.vue'
import TagChip from '@/components/publicacoes/TagChip.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { usePublicacaoStore } from '@/stores/publicacao'
import { useTagStore } from '@/stores/tag'
import { errorMessage } from '@/lib/errors'
import { MAX_UPLOAD_MB } from '@/lib/config'

const TIPOS = [
  'Artigo científico',
  'Comunicação em conferência',
  'Capítulo de livro',
  'Livro científico',
  'Relatório técnico',
  'Patente',
  'Dataset',
  'Software',
  'Modelo de IA',
  'Base de dados',
  'Tese de mestrado',
  'Tese de doutoramento',
  'Artigo de divulgação científica',
]
const AREAS = [
  'Ciência de Dados',
  'Ciência dos Materiais',
  'Inteligência Artificial',
  'Machine Learning',
  'Engenharia Informática',
]

const router = useRouter()
const store = usePublicacaoStore()
const tagStore = useTagStore()

const anoAtual = new Date().getFullYear()
const form = reactive({
  titulo: '',
  autor: '',
  tipo: TIPOS[0],
  areaCientifica: '',
  ano: String(anoAtual),
})
const ficheiro = ref(null)
const tagsEscolhidas = ref([])
const erros = reactive({})
const erroGeral = ref('')
const loading = ref(false)
const progresso = ref(0)

const ePdf = computed(() => ficheiro.value?.name?.toLowerCase().endsWith('.pdf'))

function alternarTag(nome) {
  const i = tagsEscolhidas.value.indexOf(nome)
  if (i >= 0) tagsEscolhidas.value.splice(i, 1)
  else tagsEscolhidas.value.push(nome)
}

function validar() {
  Object.keys(erros).forEach((k) => delete erros[k])
  if (!ficheiro.value) {
    erros.ficheiro = 'Escolha um ficheiro PDF ou ZIP.'
  } else if (!/\.(pdf|zip)$/i.test(ficheiro.value.name)) {
    erros.ficheiro = 'Só são aceites ficheiros PDF ou ZIP.'
  } else if (ficheiro.value.size > MAX_UPLOAD_MB * 1024 * 1024) {
    erros.ficheiro = `O ficheiro excede o limite de ${MAX_UPLOAD_MB} MB.`
  } else if (ficheiro.value.size === 0) {
    erros.ficheiro = 'O ficheiro está vazio.'
  }
  if (!form.titulo.trim()) erros.titulo = 'Indique o título.'
  if (!form.autor.trim()) erros.autor = 'Indique o(s) autor(es).'
  if (!form.areaCientifica.trim()) erros.areaCientifica = 'Indique a área científica.'
  const ano = Number(form.ano)
  if (!Number.isInteger(ano) || ano < 1000 || ano > anoAtual + 1)
    erros.ano = `O ano tem de estar entre 1000 e ${anoAtual + 1}.`
  return Object.keys(erros).length === 0
}

async function submeter() {
  erroGeral.value = ''
  if (!validar()) return
  const dados = new FormData()
  dados.append('titulo', form.titulo.trim())
  dados.append('tipo', form.tipo)
  dados.append('areaCientifica', form.areaCientifica.trim())
  dados.append('autor', form.autor.trim())
  dados.append('ano', form.ano)
  dados.append('file', ficheiro.value)
  tagsEscolhidas.value.forEach((t) => dados.append('tags', t))

  loading.value = true
  progresso.value = 0
  try {
    const criada = await store.create(dados, (e) => {
      if (e.total) progresso.value = Math.round((e.loaded / e.total) * 100)
    })
    toast.success('Publicação carregada.')
    router.push({ name: 'publicacao', params: { id: criada.id } })
  } catch (e) {
    erroGeral.value = errorMessage(e, 'Não foi possível carregar a publicação.')
  } finally {
    loading.value = false
  }
}

onMounted(() => tagStore.load().catch(() => {}))
</script>

<template>
  <div class="max-w-2xl space-y-8">
    <PageHeader
      title="Nova publicação"
      description="Carregue um PDF ou ZIP e descreva-o para que os colegas o encontrem."
    />

    <form class="space-y-7" novalidate @submit.prevent="submeter">
      <div class="space-y-2">
        <Label>Ficheiro</Label>
        <FileDropzone v-model="ficheiro" :error="erros.ficheiro" />
        <p v-if="ePdf" class="text-muted-foreground flex items-start gap-2 text-sm">
          <Sparkles class="text-signal mt-0.5 size-4 shrink-0" aria-hidden="true" />
          O resumo é gerado automaticamente por IA depois do carregamento. Pode corrigi-lo a
          qualquer momento.
        </p>
      </div>

      <div class="space-y-2">
        <Label for="titulo">Título</Label>
        <Input id="titulo" v-model="form.titulo" :aria-invalid="!!erros.titulo" maxlength="255" />
        <p v-if="erros.titulo" class="text-destructive text-sm" role="alert">{{ erros.titulo }}</p>
      </div>

      <div class="space-y-2">
        <Label for="autor">Autor(es)</Label>
        <Input
          id="autor"
          v-model="form.autor"
          placeholder="Ex.: Maria Santos, João Pereira"
          :aria-invalid="!!erros.autor"
          maxlength="255"
        />
        <p v-if="erros.autor" class="text-destructive text-sm" role="alert">{{ erros.autor }}</p>
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <div class="space-y-2">
          <Label for="tipo">Tipo</Label>
          <NativeSelect id="tipo" v-model="form.tipo">
            <option v-for="t in TIPOS" :key="t" :value="t">{{ t }}</option>
          </NativeSelect>
        </div>
        <div class="space-y-2">
          <Label for="ano">Ano de publicação</Label>
          <Input
            id="ano"
            v-model="form.ano"
            type="number"
            inputmode="numeric"
            :aria-invalid="!!erros.ano"
          />
          <p v-if="erros.ano" class="text-destructive text-sm" role="alert">{{ erros.ano }}</p>
        </div>
      </div>

      <div class="space-y-2">
        <Label for="area">Área científica</Label>
        <Input
          id="area"
          v-model="form.areaCientifica"
          list="areas"
          :aria-invalid="!!erros.areaCientifica"
          maxlength="100"
        />
        <datalist id="areas"><option v-for="a in AREAS" :key="a" :value="a" /></datalist>
        <p v-if="erros.areaCientifica" class="text-destructive text-sm" role="alert">
          {{ erros.areaCientifica }}
        </p>
      </div>

      <fieldset class="space-y-3">
        <legend class="text-sm leading-none font-medium">Tags</legend>
        <p class="text-muted-foreground text-sm">
          Quem subscrever uma tag recebe um e-mail quando lhe associarem publicações.
        </p>
        <div v-if="tagStore.tags.length" class="flex flex-wrap gap-2">
          <TagChip
            v-for="t in tagStore.tags"
            :key="t.tag"
            :name="t.tag"
            :hidden="t.visivel === false"
            size="md"
            interactive
            :selected="tagsEscolhidas.includes(t.tag)"
            @click="alternarTag(t.tag)"
          />
        </div>
        <p v-else class="text-muted-foreground text-sm">
          Ainda não há tags. Um responsável pode criá-las na página Tags.
        </p>
      </fieldset>

      <p
        v-if="erroGeral"
        class="bg-destructive/10 text-destructive rounded-lg px-3.5 py-2.5 text-sm"
        role="alert"
      >
        {{ erroGeral }}
      </p>

      <div
        v-if="loading"
        class="bg-secondary h-1.5 overflow-hidden rounded-full"
        role="progressbar"
        :aria-valuenow="progresso"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <div
          class="bg-signal h-full rounded-full transition-[width] duration-200"
          :style="{ width: progresso + '%' }"
        />
      </div>

      <div class="flex justify-end gap-2">
        <Button as-child variant="ghost" type="button"
          ><RouterLink :to="{ name: 'publicacoes' }">Cancelar</RouterLink></Button
        >
        <Button type="submit" :disabled="loading">
          <Loader2 v-if="loading" class="animate-spin" />
          {{ loading ? `A carregar ${progresso}%` : 'Carregar publicação' }}
        </Button>
      </div>
    </form>
  </div>
</template>
