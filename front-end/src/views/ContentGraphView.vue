<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const router = useRouter()
const graph = ref({ nodes: [], edges: [] })
const activeTag = ref(''), tagged = ref([]), error = ref(''), loading = ref(true)
const option = computed(() => ({
  tooltip: {},
  series: [{
    type: 'graph', layout: 'force', roam: true, draggable: true,
    label: { show: true, fontSize: 11 }, force: { repulsion: 220, edgeLength: 110 },
    categories: [{ name: '文章' }, { name: '标签' }],
    data: graph.value.nodes.map(n => ({
      name: String(n.name), contentId: n.contentId, category: n.category === '标签' ? 1 : 0,
      symbolSize: n.category === '标签' ? 22 : 34,
      itemStyle: { color: n.category === '标签' ? '#c98713' : '#4a6cf7' }
    })),
    edges: graph.value.edges.map(e => ({ source: String(e.source), target: String(e.target) })),
    lineStyle: { color: '#a9bcf3', curveness: .2 }
  }]
}))
const articleCount = computed(() => graph.value.nodes.filter(n => n.category === '文章').length)
const tagCount = computed(() => graph.value.nodes.filter(n => n.category === '标签').length)

async function onClick(p) {
  const data = p?.data || {}
  if (data.contentId) { router.push(`/community/${data.contentId}`); return }
  const name = p?.name || data.name
  if (!name) return
  activeTag.value = name
  try { tagged.value = (await feature.related(name)) || [] } catch (e) { error.value = e.message }
}
onMounted(async () => {
  loading.value = true
  try { graph.value = (await feature.contentGraph()) || { nodes: [], edges: [] } }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / GRAPH" title="文章图谱"
      description="只汇总已发布文章：点文章节点直接进详情，点标签节点查看同标签文章。"
      :actions="[{ label: '标签洞察', icon: '◈', color: 'amber', to: '/community/tags' }, { label: '内容广场', icon: '←', color: 'blue', to: '/community' }]" />

    <div class="crud-toolbar">
      <span class="pill pill-sky">{{ articleCount }} 篇文章</span>
      <span class="pill pill-amber">{{ tagCount }} 个标签</span>
      <span class="pill pill-neutral">{{ graph.edges.length }} 条关联</span>
    </div>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
    <div v-if="loading" class="placeholder-bar" style="height:480px"></div>
    <div v-else class="glass p-4"><Chart :option="option" :height="480" @point-click="onClick" /></div>

    <div v-if="activeTag" class="glass mt-5 p-5">
      <div class="mb-3 flex items-center gap-2">
        <h2 class="section-title">「{{ activeTag }}」下的文章</h2>
        <button class="btn btn-slate ml-auto" @click="activeTag = ''; tagged = []">收起</button>
      </div>
      <RouterLink v-for="r in tagged" :key="r.id" :to="`/community/${r.id}`" class="block py-1.5 text-[12px] hover:text-accent">
        {{ r.title }} <span class="faint">· 共同标签 {{ r.overlap }}</span>
      </RouterLink>
      <p v-if="!tagged.length" class="text-[12px] muted">暂无文章</p>
    </div>
  </section>
</template>
