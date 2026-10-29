<script setup>
/** Marca: três lombadas de alturas e cores diferentes, tal como as publicações na lista. */
defineProps({ size: { type: Number, default: 28 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 28 28" fill="none" aria-hidden="true">
    <rect x="3" y="8" width="5.5" height="17" rx="2.75" style="fill: oklch(0.7 0.14 205)" />
    <rect x="11.25" y="3" width="5.5" height="22" rx="2.75" style="fill: oklch(0.7 0.14 18)" />
    <rect x="19.5" y="11" width="5.5" height="14" rx="2.75" style="fill: oklch(0.7 0.14 140)" />
  </svg>
</template>
