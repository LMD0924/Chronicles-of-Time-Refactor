<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const links = ref([])
const from = ref(''), to = ref(''), relation = ref('关联'), note = ref('')
const msg = ref('')
const groups = ['记录', '学习', '规划', '社区', '档案', '目标']

const option = computed(() => {
  const set = new Set()
  links.value.forEach(l => { set.add(l.fromNode); set.add(l.toNode) })
  return {
    tooltip: {},
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      label: { show: true, fontSize: 12 },
      force: { repulsion: 180, edgeLength: 110 },
      data: [...set].map(n => ({ name: n, symbolSize: 30, itemStyle: { color: '#4a6cf7' } })),
      edges: links.value.map(l => ({ source: l.fromNode, target: l.toNode, label: { show: false } })),
      lineStyle: { color: '#9db4f5', curveness: 0.2 }
    }]
  }
})

async function load() { try { links.value = (await feature.connections()) || [] } catch (e) { msg.value = e.message } }
async function add() {
  if (!from.value || !to.value) { msg.value = '请填写两端节点'; return }
  try { await feature.connect({ fromNode: from.value, toNode: to.value, relation: relation.value, note: note.value, score: 1 }); from.value = ''; to.value = ''; note.value = ''; msg.value = '已建立关联'; await load() }
  catch (e) { msg.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="TIMELINE / CONNECTIONS" title="贯通关联"
      description="把原本孤立的功能用关系连起来：一条记录可以指向一道错题、一个目标或一篇文章。" />

    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title">新建关联</h2>
        <div class="form-grid mt-4">
          <label><span class="field-label">起点</span><select v-model="from" class="field w-full"><option value="">请选择</option><option v-for="g in groups" :key="g">{{ g }}</option></select></label>
          <label><span class="field-label">终点</span><select v-model="to" class="field w-full"><option value="">请选择</option><option v-for="g in groups" :key="g">{{ g }}</option></select></label>
          <label><span class="field-label">关系</span><input v-model="relation" class="field w-full" /></label>
          <label><span class="field-label">备注</span><input v-model="note" class="field w-full" /></label>
        </div>
        <button class="btn btn-violet mt-4" type="button" @click="add"><span class="btn-glyph">⌘</span> 建立关联</button>
        <p v-if="msg" class="mt-3 text-[12px] muted">{{ msg }}</p>
      </div>
      <div class="glass p-4"><Chart :option="option" :height="300" /></div>
    </div>

    <div class="glass mt-5 p-5">
      <h2 class="section-title mb-3">已有关联 ({{ links.length }})</h2>
      <div v-if="!links.length" class="text-[12px] muted">还没有关联，先在左侧创建一条。</div>
      <div v-for="l in links" :key="l.id" class="flex flex-wrap items-center gap-2 border-b border-hairline py-2 text-[12px] last:border-0">
        <span class="pill pill-mint">{{ l.fromNode }}</span><span class="faint">→ {{ l.relation }} →</span>
        <span class="pill pill-sky">{{ l.toNode }}</span>
        <span class="muted">{{ l.note }}</span>
      </div>
    </div>
  </section>
</template>
