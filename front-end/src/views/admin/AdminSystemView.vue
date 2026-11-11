<script setup>
import { onMounted, ref } from 'vue'
import { feature } from '@/api'

const overview = ref({}), modules = ref([]), dash = ref({})
onMounted(async () => {
  try {
    const [o, m, d] = await Promise.all([feature.systemOverview(), feature.systemModules(), feature.dashboard()])
    overview.value = o || {}; modules.value = m || []; dash.value = d || {}
  } catch { /* ignore */ }
})
</script>

<template>
  <div>
    <h2 class="section-title mb-4">系统信息</h2>
    <div class="glass p-6">
      <div class="grid gap-3 md:grid-cols-2">
        <div v-for="(v, k) in overview" :key="k" class="glass-flat p-3">
          <p class="text-[11px] faint">{{ k }}</p><p class="mt-1 text-[13px] ink">{{ v }}</p>
        </div>
      </div>
    </div>
    <div class="glass mt-5 p-6">
      <h3 class="section-title mb-3">限界上下文模块</h3>
      <div class="flex flex-wrap gap-2">
        <span v-for="m in modules" :key="m.name" class="pill pill-mint">{{ m.name }}</span>
        <span v-if="!modules.length" class="text-[12px] muted">后端未启动</span>
      </div>
    </div>
    <div class="glass mt-5 p-6">
      <h3 class="section-title mb-3">各域资源计数</h3>
      <div v-for="(mods, name) in (dash.modules || {})" :key="name" class="mb-3">
        <p class="eyebrow mb-1">{{ name }}</p>
        <div class="flex flex-wrap gap-1.5">
          <span v-for="(v, k) in mods" :key="k" class="pill pill-neutral">{{ k }} · {{ v }}</span>
        </div>
      </div>
      <p v-if="!Object.keys(dash.modules || {}).length" class="text-[12px] muted">暂无统计数据</p>
    </div>
  </div>
</template>
