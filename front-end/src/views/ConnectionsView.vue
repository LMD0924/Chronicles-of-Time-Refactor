<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const router = useRouter()
const links = ref([])
const from = ref(''), to = ref(''), relation = ref('关联'), note = ref(''), score = ref(1)
const editing = ref(null)
const msg = ref('')
const error = ref('')
const loading = ref(true)

const groups = ['记录', '学习', '规划', '社区', '档案', '目标']
const groupRoutes = {
  记录: '/timeline/records', 学习: '/learning', 规划: '/planning',
  社区: '/community', 档案: '/archive', 目标: '/planning/target',
}
const groupColors = {
  记录: '#3fb99a', 学习: '#4a8cf7', 规划: '#8a6cf1',
  社区: '#ef7a9b', 档案: '#e5a54b', 目标: '#ec6a6a',
}
function colorOf(name) { return groupColors[name] || '#7b8aa8' }

const option = computed(() => {
  const set = new Set()
  links.value.forEach(l => { set.add(l.fromNode); set.add(l.toNode) })
  return {
    tooltip: { formatter: (p) => p.dataType === 'edge' ? (p.data?.relation || '关联') : (p.name || '') },
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      label: { show: true, fontSize: 12 },
      force: { repulsion: 200, edgeLength: 120 },
      data: [...set].map(n => ({ name: n, symbolSize: 34, itemStyle: { color: colorOf(n) } })),
      edges: links.value.map(l => ({
        source: l.fromNode, target: l.toNode, relation: l.relation,
        label: { show: true, formatter: l.relation || '关联', fontSize: 10 },
      })),
      lineStyle: { color: '#9db4f5', curveness: 0.2, width: 1.5 },
      emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
    }],
  }
})

function onChartClick(p) {
  const name = p?.name || p?.data?.name
  if (!name) return
  const route = groupRoutes[name]
  if (route) { router.push(route); msg.value = `已跳转到「${name}」模块` }
  else msg.value = `「${name}」是自定义节点，暂无对应模块`
}

async function load() {
  loading.value = true; error.value = ''
  try { links.value = (await feature.connections()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function startEdit(l) {
  editing.value = l.id
  from.value = l.fromNode; to.value = l.toNode
  relation.value = l.relation || '关联'; note.value = l.note || ''; score.value = l.score || 1
  msg.value = '正在编辑这条关联，保存后生效'
}
function resetForm() {
  editing.value = null; from.value = ''; to.value = ''
  relation.value = '关联'; note.value = ''; score.value = 1
}

async function save() {
  if (!from.value.trim() || !to.value.trim()) { error.value = '请填写两端节点'; return }
  error.value = ''
  try {
    const payload = {
      fromNode: from.value.trim(), toNode: to.value.trim(),
      relation: relation.value.trim() || '关联', note: note.value.trim(), score: Number(score.value) || 1,
    }
    if (editing.value) await feature.updateConnection(editing.value, payload)
    else await feature.connect(payload)
    msg.value = editing.value ? '关联已更新' : '已建立关联'
    resetForm()
    await load()
  } catch (e) { error.value = e.message }
}

async function remove(l) {
  if (!window.confirm(`确定删除「${l.fromNode} → ${l.toNode}」这条关联吗？`)) return
  try { await feature.deleteConnection(l.id); msg.value = '关联已删除'; await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="TIMELINE / CONNECTIONS" title="贯通关联"
      description="把原本孤立的功能用关系连起来。点击图谱节点可直接跳到对应模块，连线可增删改。"
      :actions="[
        { label: '时间线', icon: '⌁', color: 'violet', to: '/timeline' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="text-[14px] font-semibold ink">{{ editing ? '编辑关联' : '新建关联' }}</h2>
        <p class="mt-1 text-[11px] faint">可以选预设模块，也可以直接输入自定义节点名。</p>
        <div class="form-grid mt-4">
          <label><span class="field-label">起点</span>
            <input v-model="from" class="field w-full" list="cot-groups" placeholder="例如：记录" />
            <datalist id="cot-groups"><option v-for="g in groups" :key="g" :value="g" /></datalist>
          </label>
          <label><span class="field-label">终点</span>
            <input v-model="to" class="field w-full" list="cot-groups" placeholder="例如：学习" />
          </label>
          <label><span class="field-label">关系</span><input v-model="relation" class="field w-full" placeholder="关联 / 来源 / 促成" /></label>
          <label><span class="field-label">强度 (0-5)</span><input v-model="score" class="field w-full" type="number" min="0" max="5" step="0.5" /></label>
          <label class="span-2"><span class="field-label">备注</span><input v-model="note" class="field w-full" placeholder="这条关联说明了什么" /></label>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-violet" type="button" @click="save"><span class="btn-glyph">⌘</span> {{ editing ? '保存修改' : '建立关联' }}</button>
          <button v-if="editing" class="btn btn-slate" type="button" @click="resetForm"><span class="btn-glyph">↺</span> 取消编辑</button>
        </div>
        <p v-if="msg" class="mt-3 text-[12px] muted">{{ msg }}</p>
        <p v-if="error" class="pill pill-rose mt-3">{{ error }}</p>
      </div>
      <div class="glass p-4">
        <h2 class="mb-2 text-[14px] font-semibold ink">关系图谱</h2>
        <Chart :option="option" :height="300" @point-click="onChartClick" />
        <p class="mt-1 text-[11px] faint">提示：点击预设模块节点可直接跳转；拖拽节点可调整布局。</p>
      </div>
    </div>

    <div class="glass mt-5 p-5">
      <h2 class="mb-3 text-[14px] font-semibold ink">已有关联 ({{ links.length }})</h2>
      <div v-if="loading" class="space-y-2"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:40px"></div></div>
      <p v-else-if="!links.length" class="text-[12px] muted">还没有关联，先在上方创建一条。</p>
      <div v-else>
        <div v-for="l in links" :key="l.id"
             class="flex flex-wrap items-center gap-2 border-b border-hairline py-2 text-[12px] last:border-0">
          <span class="pill pill-mint">{{ l.fromNode }}</span>
          <span class="faint">→ {{ l.relation || '关联' }} →</span>
          <span class="pill pill-sky">{{ l.toNode }}</span>
          <span class="pill pill-neutral">强度 {{ l.score ?? 0 }}</span>
          <span class="muted">{{ l.note }}</span>
          <span class="ml-auto flex gap-2">
            <button v-if="groupRoutes[l.toNode]" class="btn btn-teal" type="button" @click="router.push(groupRoutes[l.toNode])">
              <span class="btn-glyph">→</span> 跳转
            </button>
            <button class="btn btn-violet" type="button" @click="startEdit(l)"><span class="btn-glyph">✎</span> 编辑</button>
            <button class="btn btn-rose" type="button" @click="remove(l)"><span class="btn-glyph">×</span> 删除</button>
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
