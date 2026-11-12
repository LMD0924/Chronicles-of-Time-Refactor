<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const router = useRouter()
const graph = ref({ nodes: [], edges: [], pendingMistakes: 0 })
const heat = ref({})
const msg = ref('')
const error = ref('')
const loading = ref(true)

const colors = ['#4a8cf7', '#3fb99a', '#8a6cf1', '#e5a54b', '#ef7a9b', '#ec6a6a']
const subjectColor = computed(() => {
  const map = new Map(), names = [...new Set(graph.value.nodes.map(n => n.subject).filter(Boolean))]
  names.forEach((s, i) => map.set(s, colors[i % colors.length]))
  return map
})
const option = computed(() => ({
  tooltip: { formatter: p => p.dataType === 'edge' ? '同学科关联' : `${p.name} · ${p.data?.value || 0} 题` },
  series: [{
    type: 'graph', layout: 'force', roam: true, draggable: true,
    label: { show: true, fontSize: 12 }, force: { repulsion: 220, edgeLength: 130 },
    data: graph.value.nodes.map(n => ({
      name: n.name, value: n.value, subject: n.subject, symbolSize: 26 + Math.min(28, (n.value || 0) * 8),
      itemStyle: { color: subjectColor.value.get(n.subject) || '#7b8aa8' },
    })),
    edges: graph.value.edges.map(e => ({ source: e.source, target: e.target })),
    lineStyle: { color: '#9db4f5', curveness: 0.2 },
    emphasis: { focus: 'adjacency' },
  }],
}))
const heatDays = computed(() => {
  const out = [], today = new Date()
  for (let i = 89; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    out.push({ date: key, count: heat.value[key] || 0 })
  }
  return out
})

function onChartClick(p) {
  const name = p?.name || p?.data?.name
  if (!name) return
  msg.value = `已按知识点「${name}」组卷`
  router.push({ path: '/learning/practice', query: { auto: '1', knowledgePoint: name, size: '10' } })
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [g, h] = await Promise.all([feature.knowledgeGraph(), feature.heatmap()])
    graph.value = g || { nodes: [], edges: [] }
    heat.value = h || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / GRAPH" title="知识图谱"
      description="知识点按学科着色、按题目数量放大；点击任一节点可直接按该知识点组卷练习。"
      :actions="[
        { label: '练习中心', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '学习路径', icon: '⌁', color: 'violet', to: '/learning/path' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-3" v-reveal="0">
      <div v-for="m in [
        { label: '知识点', value: graph.nodes.length }, { label: '关联边', value: graph.edges.length },
        { label: '待复习错题', value: graph.pendingMistakes || 0 },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="msg" class="pill pill-mint mt-4">{{ msg }}</p>

    <div v-if="loading" class="mt-5 placeholder-bar" style="height:340px"></div>
    <div v-else class="glass mt-5 p-5">
      <h2 class="text-[14px] font-semibold ink">知识点关系网</h2>
      <p class="mt-1 text-[11px] faint">同一学科的知识点自动连线；节点越大表示题目越多。点击节点开始针对性练习。</p>
      <Chart :option="option" :height="340" @point-click="onChartClick" />
      <div class="mt-2 flex flex-wrap gap-2">
        <span v-for="[s, c] in subjectColor" :key="s" class="pill pill-neutral">
          <span class="inline-block h-2 w-2 rounded-full" :style="{ background: c }"></span> {{ s }}
        </span>
      </div>
    </div>

    <div class="glass mt-5 p-5">
      <h2 class="text-[14px] font-semibold ink">近 90 天答题热力</h2>
      <div class="mt-4 grid grid-cols-[repeat(15,minmax(0,1fr))] gap-1.5">
        <span v-for="d in heatDays" :key="d.date" class="block h-[13px] rounded-[3px] border border-hairline"
          :style="{ background: d.count ? `color-mix(in srgb, var(--violet) ${18 + Math.min(4, d.count) * 18}%, var(--surface-2))` : 'var(--surface-2)' }"
          :title="`${d.date} · ${d.count} 题`" />
      </div>
    </div>
  </section>
</template>
