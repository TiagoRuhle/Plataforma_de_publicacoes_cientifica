<script setup>
import { SearchX } from 'lucide-vue-next'
import EmptyState from '@/components/common/EmptyState.vue'
import { Button } from '@/components/ui/button'
</script>

<template>
  <main id="conteudo" class="flex min-h-dvh items-center justify-center p-6" tabindex="-1">
    <EmptyState
      :icon="SearchX"
      title="Página não encontrada"
      description="O endereço não existe ou a publicação foi movida."
    >
      <Button as-child><RouterLink to="/publicacoes">Ir para as publicações</RouterLink></Button>
    </EmptyState>
  </main>
</template>
