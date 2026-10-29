<script setup>
import BrandMark from './BrandMark.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})

/** Prateleira decorativa: lombadas de alturas e cores diferentes (as mesmas cores das tags). */
const lombadas = [
  { h: 205, alt: 62, larg: 34 },
  { h: 18, alt: 84, larg: 28 },
  { h: 140, alt: 52, larg: 40 },
  { h: 275, alt: 74, larg: 30 },
  { h: 52, alt: 90, larg: 36 },
  { h: 350, alt: 58, larg: 28 },
  { h: 172, alt: 78, larg: 42 },
  { h: 240, alt: 66, larg: 30 },
  { h: 88, alt: 86, larg: 34 },
  { h: 315, alt: 56, larg: 38 },
]
</script>

<template>
  <div class="grid min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
    <main
      id="conteudo"
      class="flex flex-col justify-center px-6 py-10 sm:px-12 lg:px-20"
      tabindex="-1"
    >
      <div class="mx-auto w-full max-w-[26rem]">
        <div class="mb-10 flex items-center gap-3">
          <BrandMark :size="32" />
          <span class="leading-tight">
            <span class="display block text-xl">Publicações</span>
            <span class="text-muted-foreground block text-xs">Centro de Investigação XYZ</span>
          </span>
        </div>

        <h1 class="display text-[clamp(2rem,1.5rem+1.6vw,2.75rem)]">{{ title }}</h1>
        <p v-if="subtitle" class="text-muted-foreground mt-3 text-[15px]">{{ subtitle }}</p>

        <div class="mt-8">
          <slot />
        </div>
      </div>
    </main>

    <aside
      class="bg-sidebar border-sidebar-border relative hidden overflow-hidden border-l lg:block"
      aria-hidden="true"
    >
      <div class="absolute inset-x-0 bottom-0 flex items-end justify-center gap-2.5 px-10 pb-0">
        <span
          v-for="(l, i) in lombadas"
          :key="i"
          class="tag-hue rounded-t-xl bg-[var(--tag-spine)]"
          :style="{
            '--h': l.h,
            height: l.alt * 0.78 + 'vh',
            width: l.larg + 'px',
            opacity: 0.92,
          }"
        />
      </div>
      <div class="bg-border absolute inset-x-0 bottom-0 h-3" />
    </aside>
  </div>
</template>
