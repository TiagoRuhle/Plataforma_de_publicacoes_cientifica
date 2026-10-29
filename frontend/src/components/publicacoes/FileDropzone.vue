<script setup>
import { computed, ref } from 'vue'
import { FileArchive, FileText, UploadCloud, X } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'

const props = defineProps({
  modelValue: { type: Object, default: null }, // File
  error: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const aArrastar = ref(false)
const input = ref(null)

const eZip = computed(() => props.modelValue?.name?.toLowerCase().endsWith('.zip'))
const tamanho = computed(() => {
  const bytes = props.modelValue?.size ?? 0
  return bytes > 1024 * 1024
    ? `${(bytes / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`
})

function escolher(ficheiro) {
  if (ficheiro) emit('update:modelValue', ficheiro)
}
function aoSoltar(e) {
  aArrastar.value = false
  escolher(e.dataTransfer?.files?.[0])
}
function aoSeleccionar(e) {
  escolher(e.target.files?.[0])
  e.target.value = ''
}
</script>

<template>
  <div>
    <div
      v-if="modelValue"
      class="bg-card flex items-center gap-4 rounded-xl border p-4"
      :class="error && 'border-destructive'"
    >
      <span
        class="bg-secondary text-muted-foreground inline-flex size-11 shrink-0 items-center justify-center rounded-lg"
      >
        <FileArchive v-if="eZip" class="size-5" aria-hidden="true" />
        <FileText v-else class="size-5" aria-hidden="true" />
      </span>
      <div class="min-w-0 flex-1">
        <p class="truncate font-medium">{{ modelValue.name }}</p>
        <p class="text-muted-foreground text-sm tabular-nums">{{ tamanho }}</p>
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="Remover ficheiro"
        @click="emit('update:modelValue', null)"
      >
        <X />
      </Button>
    </div>

    <label
      v-else
      class="bg-card hover:bg-accent/40 focus-within:ring-ring/40 flex cursor-pointer flex-col items-center gap-2 rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors focus-within:ring-[3px]"
      :class="[aArrastar && 'border-signal bg-accent/60', error && 'border-destructive']"
      @dragover.prevent="aArrastar = true"
      @dragleave.prevent="aArrastar = false"
      @drop.prevent="aoSoltar"
    >
      <UploadCloud class="text-muted-foreground size-7" aria-hidden="true" />
      <span class="font-medium">Largue aqui o ficheiro ou clique para escolher</span>
      <span class="text-muted-foreground text-sm">PDF ou ZIP</span>
      <input
        ref="input"
        type="file"
        accept=".pdf,.zip,application/pdf,application/zip"
        class="sr-only"
        @change="aoSeleccionar"
      />
    </label>

    <p v-if="error" class="text-destructive mt-2 text-sm" role="alert">{{ error }}</p>
  </div>
</template>
