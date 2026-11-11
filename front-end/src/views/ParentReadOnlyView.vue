<script setup>
import { onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const feed = ref([]), stats = ref({})
onMounted(async () => {
  try { const [f, s] = await Promise.all([feature.timelineFeed(), feature.timelineStats()]); feed.value = (f || []).slice(0, 20); stats.value = s || {} } catch { /* ignore */ }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SHARE" title="只读视图"
      description="适合投屏或分享给家人：只展示汇总与时间线节点，隐藏所有编辑入口。"
      :actions="[{ label: '打印 / 存 PDF', icon: '⎙', color: 'teal', click: () => window.print() }, { label: '回到时间线', icon: '←', color: 'blue', to: '/timeline' }]" />
    <div class="glass p-6">
      <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div v-for="(v, k) in { 记录: stats.totalRecords || 0, 里程碑: stats.milestones || 0, 关联: stats.links || 0, 学习时长: Math.round(stats.studyHours || 0) }" :key="k" class="glass-flat p-4 text-center">
          <p class="stat-num" v-count="v">0</p><p class="mt-1 text-[11px] muted">{{ k }}</p>
        </div>
      </div>
    </div>
    <div class="tl-rail mt-6">
      <div v-for="e in feed" :key="e.id" class="tl-node mb-4">
        <div class="glass p-4">
          <span class="pill pill-sky">{{ e.type }}</span><span class="ml-2 text-[11px] faint">{{ e.occurredAt }}</span>
          <h3 class="mt-2 text-[14px] font-medium ink">{{ e.title }}</h3>
        </div>
      </div>
      <p v-if="!feed.length" class="text-[12px] muted">暂无可展示的节点</p>
    </div>
  </section>
</template>
