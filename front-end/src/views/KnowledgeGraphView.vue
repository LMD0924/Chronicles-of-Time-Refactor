<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const graph = ref({ nodes: [], edges: [] })
const heat = ref({})
const option = computed(() => ({
  tooltip: {},
  legend: [{ data: ['知识点'] }],
  series: [{
    type: 'graph', layout: 'force', roam: true, draggable: true,
    label: { show: true, fontSize: 11 }, force: { repulsion: 220, edgeLength: 120 },
    data: graph.value.nodes.map(n => ({ name: String(n.name), value: n.value, symbolSize: Math.max(22, Math.min(60, 20 + (n.value || 0) * 6)), itemStyle: { color: '#4a6cf7' } })),
    edges: graph.value.edges.map(e => ({ source: String(e.source), target: String(e.target) })),
    lineStyle: { color: '#a9bcf3', curveness: .2 }
  }]
}))
onMounted(async () => {
  try { const [g, h] = await Promise.all([feature.knowledgeGraph(), feature.heatmap()]); graph.value = g || { nodes: [], edges: [] }; heat.value = h || {} }
  catch { /* ignore */ }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / GRAPH" title="知识图谱"
      description="按知识点聚合题库与错题，节点大小表示题量；原 3D 图谱改为等价的 ECharts 关系图。"
      :actions="[{ label: '学习路径', icon: '→', color: 'mint', to: '/learning/path' }, { label: '题库', icon: '▤', color: 'blue', to: '/learning/questions' }]" />
    <div class="glass p-4"><Chart :option="option" :height="440" /></div>
    <div class="glass mt-5 p-5">
      <h2 class="section-title mb-3">作答热力（按日期）</h2>
      <div class="flex flex-wrap gap-2">
        <span v-for="(v, d) in heat" :key="d" class="pill pill-sky">{{ d }} · {{ v }} 次</span>
        <span v-if="!Object.keys(heat).length" class="text-[12px] muted">还没有答题记录</span>
      </div>
    </div>
  </section>
</template>
