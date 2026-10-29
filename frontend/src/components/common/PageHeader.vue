<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <header class="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
    <div class="min-w-0">
      <h1 class="display text-[clamp(1.85rem,1.45rem+1.3vw,2.5rem)]">{{ title }}</h1>
      <p v-if="description" class="text-muted-foreground mt-2 max-w-[62ch] text-[15px]">
        {{ description }}
      </p>
    </div>
    <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </header>
</template>
