<script setup>
defineProps({
  icon: { type: [Object, Function], default: null },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="flex flex-col items-center px-6 py-16 text-center">
    <span
      v-if="icon"
      class="bg-secondary text-muted-foreground mb-4 inline-flex size-12 items-center justify-center rounded-2xl"
    >
      <component :is="icon" class="size-5" aria-hidden="true" />
    </span>
    <h2 class="display text-xl">{{ title }}</h2>
    <p v-if="description" class="text-muted-foreground mt-2 max-w-[44ch] text-sm">
      {{ description }}
    </p>
    <div v-if="$slots.default" class="mt-5 flex flex-wrap justify-center gap-2">
      <slot />
    </div>
  </div>
</template>
