<script setup>
defineProps({ label: String, value: [Number, String], color: { type: String, default: 'sky' } })
</script>

<template>
  <div class="metric-item" v-reveal="1">
    <div class="metric-circle" :class="`pill-${color}`">
      <span v-count="value">0</span>
    </div>
    <span class="mt-2 text-[12px] muted">{{ label }}</span>
  </div>
</template>
