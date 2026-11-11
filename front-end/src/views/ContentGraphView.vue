<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const graph = ref({ nodes: [], edges: [] })
const option = computed(() => ({
  tooltip: {},
  series: [{
    type: 'graph', layout: 'force', roam: true, draggable: true,
    label: { show: true, fontSize: 11 }, force: { repulsion: 200, edgeLength: 100 },
    categories: [{ name: '文章' }, { name: '标签' }],
    data: graph.value.nodes.map(n => ({
      name: String(n.name), category: n.category === '标签' ? 1 : 0,
      symbolSize: n.category === '标签' ? 22 : 34,
      itemStyle: { color: n.category === '标签' ? '#c98713' : '#4a6cf7' }
    })),
    edges: graph.value.edges.map(e => ({ source: String(e.source), target: String(e.target) })),
    lineStyle: { color: '#a9bcf3', curveness: .2 }
  }]
}))
onMounted(async () => { try { graph.value = (await feature.contentGraph()) || { nodes: [], edges: [] } } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / GRAPH" title="文章图谱"
      description="文章与标签组成关系网，用来发现内容之间的关联，而不是把功能按阶段切开。"
      :actions="[{ label: '标签洞察', icon: '◈', color: 'amber', to: '/community/tags' }, { label: '内容广场', icon: '←', color: 'blue', to: '/community' }]" />
    <div class="glass p-4"><Chart :option="option" :height="480" /></div>
  </section>
</template>
