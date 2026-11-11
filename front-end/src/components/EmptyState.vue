<script setup>
defineProps({ text: { type: String, default: '这里还空着' }, hint: { type: String, default: '' } })
</script>

<template>
  <div class="empty-state">
    <div class="text-[28px] mb-2">◷</div>
    <p class="text-[13px] muted">{{ text }}</p>
    <p v-if="hint" class="mt-1 text-[11px] faint">{{ hint }}</p>
  </div>
</template>
