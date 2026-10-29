<script setup>
import { computed } from 'vue'
import { MessageSquare, Star, EyeOff } from 'lucide-vue-next'
import TagChip from './TagChip.vue'
import TagSpine from './TagSpine.vue'
import { formatRating } from '@/lib/format'
import { plainText } from '@/lib/markdownLite'

const props = defineProps({
  publicacao: { type: Object, required: true },
})

const MAX_TAGS = 4
const tagsVisiveis = computed(() => props.publicacao.tags.slice(0, MAX_TAGS))
const tagsExtra = computed(() => Math.max(0, props.publicacao.tags.length - MAX_TAGS))
const previa = computed(() =>
  ['CONCLUIDO', 'EDITADO'].includes(props.publicacao.resumoEstado)
    ? plainText(props.publicacao.resumoIa)
    : '',
)
const temRatings = computed(() => props.publicacao.totalRatings > 0)
</script>

<template>
  <RouterLink
    :to="{ name: 'publicacao', params: { id: publicacao.id } }"
    class="group hover:bg-accent/55 focus-visible:bg-accent/55 relative flex gap-4 px-4 py-5 transition-colors duration-150 sm:px-6"
  >
    <TagSpine :tags="publicacao.tags" />

    <div class="min-w-0 flex-1">
      <div class="flex items-start gap-3">
        <h3
          class="display decoration-foreground/40 line-clamp-2 text-[1.2rem] underline-offset-4 group-hover:underline"
        >
          {{ publicacao.titulo }}
        </h3>
        <span
          v-if="!publicacao.visivel"
          class="bg-warning/15 text-warning mt-1 inline-flex shrink-0 items-center gap-1 rounded-md px-1.5 py-0.5 text-xs font-medium"
        >
          <EyeOff class="size-3" aria-hidden="true" /> Oculta
        </span>
      </div>

      <p class="text-muted-foreground mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-sm">
        <span class="text-foreground/80">{{ publicacao.autor }}</span>
        <span>{{ publicacao.areaCientifica }}</span>
        <span class="tabular-nums">{{ publicacao.ano }}</span>
        <span>{{ publicacao.tipo }}</span>
      </p>

      <p
        v-if="previa"
        class="text-muted-foreground mt-2 line-clamp-2 max-w-[78ch] text-[14px] leading-relaxed"
      >
        {{ previa }}
      </p>

      <div v-if="publicacao.tags.length" class="mt-3 flex flex-wrap gap-1.5">
        <TagChip
          v-for="t in tagsVisiveis"
          :key="t.tag"
          :name="t.tag"
          :hidden="t.visivel === false"
        />
        <span v-if="tagsExtra" class="text-muted-foreground inline-flex h-6 items-center text-xs">
          +{{ tagsExtra }}
        </span>
      </div>

      <!-- métricas em linha no telemóvel -->
      <p class="text-muted-foreground mt-3 flex items-center gap-4 text-sm tabular-nums sm:hidden">
        <span class="flex items-center gap-1">
          <Star
            class="size-4"
            :class="temRatings ? 'text-star' : 'text-border'"
            fill="currentColor"
            stroke-width="1.5"
            aria-hidden="true"
          />
          <span :class="temRatings && 'text-foreground font-medium'">
            {{ temRatings ? formatRating(publicacao.ratingMedio) : 'Sem avaliações' }}
          </span>
        </span>
        <span class="flex items-center gap-1">
          <MessageSquare class="size-4" aria-hidden="true" /> {{ publicacao.totalComentarios }}
        </span>
      </p>
    </div>

    <dl
      class="text-muted-foreground hidden shrink-0 flex-col items-end gap-1.5 pt-1 text-sm tabular-nums sm:flex"
    >
      <div class="flex items-center gap-1.5" :title="`${publicacao.totalRatings} avaliações`">
        <dt class="sr-only">Avaliação média</dt>
        <dd class="text-foreground flex items-center gap-1 font-medium">
          <Star
            class="size-4"
            :class="temRatings ? 'text-star' : 'text-border'"
            fill="currentColor"
            stroke-width="1.5"
            aria-hidden="true"
          />
          {{ temRatings ? formatRating(publicacao.ratingMedio) : '—' }}
          <span class="text-muted-foreground text-xs font-normal"
            >({{ publicacao.totalRatings }})</span
          >
        </dd>
      </div>
      <div class="flex items-center gap-1.5" :title="`${publicacao.totalComentarios} comentários`">
        <dt class="sr-only">Comentários</dt>
        <dd class="flex items-center gap-1">
          <MessageSquare class="size-4" aria-hidden="true" />
          {{ publicacao.totalComentarios }}
        </dd>
      </div>
    </dl>
  </RouterLink>
</template>
