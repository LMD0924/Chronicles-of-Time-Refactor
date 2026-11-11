<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi, feature } from '@/api'
import MetricCircle from '@/components/MetricCircle.vue'

const stats = ref({}), dash = ref({}), modules = ref([])
const metrics = computed(() => [
  { label: '账号总数', value: stats.value.accounts || 0, color: 'sky' },
  { label: '审计记录', value: stats.value.auditLogs || 0, color: 'violet' },
  { label: '时间线记录', value: dash.value.records || 0, color: 'mint' },
  { label: '文章内容', value: dash.value.contents || 0, color: 'amber' },
])
onMounted(async () => {
  try {
    const [s, d, m] = await Promise.all([adminApi.stats(), feature.dashboard(), feature.systemModules()])
    stats.value = s || {}; dash.value = d || {}; modules.value = m || []
  } catch { /* ignore */ }
})
</script>

<template>
  <div>
    <h2 class="section-title mb-4">系统概览</h2>
    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>
    <div class="glass mt-5 p-5">
      <h3 class="section-title mb-3">已装配的限界上下文 ({{ modules.length }})</h3>
      <div class="crud-grid">
        <div v-for="m in modules" :key="m.name" class="crud-card">
          <span class="pill pill-mint">{{ m.name }}</span>
          <p class="mt-2 text-[12px] muted">{{ m.description }}</p>
        </div>
      </div>
      <p v-if="!modules.length" class="text-[12px] muted">后端未启动或暂无模块数据</p>
    </div>
  </div>
</template>
