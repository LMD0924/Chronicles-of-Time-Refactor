<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  text: { type: String, default: '这里还空着' },
  hint: { type: String, default: '' },
  rows: { type: Number, default: 3 }
})
const emit = defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="space-y-3">
    <div v-for="i in rows" :key="i" class="placeholder-bar" style="height:62px"></div>
  </div>
  <div v-else-if="error" class="glass p-8 text-center">
    <span class="pill pill-rose mb-3">加载失败</span>
    <p class="text-[13px] muted">{{ error }}</p>
    <button class="btn btn-slate mt-4" type="button" @click="emit('retry')"><span class="btn-glyph">↻</span> 重试</button>
  </div>
  <EmptyState v-else-if="empty" :text="text" :hint="hint" />
  <slot v-else />
</template>
