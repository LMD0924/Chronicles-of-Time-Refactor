<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const summary = ref({ categories: [], items: [], totalScore: 0 })
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const filter = ref('全部')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const filters = computed(() => ['全部', ...summary.value.categories.map(c => c.category)])
const visible = computed(() => filter.value === '全部'
  ? summary.value.items : summary.value.items.filter(i => (i.category || '未分类') === filter.value))
const categoryOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: summary.value.categories.map(c => c.category) },
  yAxis: { type: 'value', name: '分值' },
  series: [{ type: 'bar', barWidth: '44%', data: summary.value.categories.map(c => c.totalScore),
    itemStyle: { color: '#8a6cf1', borderRadius: [6, 6, 0, 0] } }],
}))

async function load() {
  loading.value = true; error.value = ''
  try { summary.value = (await feature.qualitySummary()) || { categories: [], items: [] } }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date()
  return {
    title: '', category: '学业水平', score: 0,
    occurredAt: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    description: '',
  }
}
function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value, score: Number(form.value.score) || 0 }
    if (editing.value) await R.planning.qualityRecords.update(editing.value, payload)
    else await R.planning.qualityRecords.save(payload)
    showForm.value = false; notice.value = '记录已归档，分类统计同步刷新'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.title}」吗？`)) return
  try { await R.planning.qualityRecords.remove(r.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / QUALITY" title="综评记录"
      description="综合素质材料按五大类归档，分类统计与分值总和实时更新，时间线倒序展示每一条记录。"
      :actions="[
        { label: '新增记录', icon: '＋', color: 'violet', click: openCreate },
        { label: '只读分享', icon: '↗', color: 'mint', to: '/planning/share' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '记录总数', value: summary.items?.length || 0 },
        { label: '累计分值', value: summary.totalScore || 0 },
        { label: '覆盖类别', value: summary.categories?.length || 0 },
        { label: '最高类别', value: [...(summary.categories || [])].sort((a, b) => b.totalScore - a.totalScore)[0]?.category || '—' },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div class="crud-toolbar mt-5">
      <button v-for="f in filters" :key="f" class="btn" :class="filter === f ? 'btn-violet' : 'btn-slate'" type="button" @click="filter = f">{{ f }}</button>
    </div>

    <div v-if="loading" class="grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:160px"></div></div>
    <template v-else>
      <div class="mt-5 grid gap-5 md:grid-cols-[1fr_1.5fr]">
        <div class="glass h-fit p-5">
          <h2 class="section-title mb-2">分类分值</h2>
          <Chart :option="categoryOption" :height="260" />
          <div class="mt-4 space-y-2">
            <div v-for="c in summary.categories" :key="c.category" class="flex items-center justify-between text-[12px]">
              <span class="muted">{{ c.category }}</span>
              <span class="pill pill-neutral">{{ c.count }} 条 · 均分 {{ c.average }}</span>
            </div>
          </div>
        </div>

        <div>
          <EmptyState v-if="!visible.length" text="这个分类还没有记录" hint="点右上角「新增记录」归档一份材料" />
          <div class="tl-rail">
            <div v-for="r in visible" :key="r.id" class="tl-node mb-4">
              <div class="glass p-4">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="pill pill-violet">{{ r.category || '未分类' }}</span>
                  <span class="text-[11px] faint">{{ r.occurredAt }}</span>
                  <span class="ml-auto pill pill-mint">{{ r.score }} 分</span>
                </div>
                <h3 class="mt-2 text-[14px] font-medium ink">{{ r.title }}</h3>
                <p v-if="r.description" class="mt-1 text-[12px] leading-6 muted">{{ r.description }}</p>
                <div class="mt-3 flex gap-2">
                  <button class="btn btn-slate" type="button" @click="openEdit(r)">编辑</button>
                  <button class="btn btn-rose" type="button" @click="remove(r)">删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}综评记录</h2>
          <p class="mb-5 text-[12px] muted">五大类分值会自动汇总到左侧图表。</p>
          <div class="form-grid">
            <label class="span-2"><span class="field-label">标题</span><input v-model="form.title" class="field w-full" /></label>
            <label><span class="field-label">类别</span><select v-model="form.category" class="field w-full">
              <option>思想品德</option><option>学业水平</option><option>身心健康</option><option>艺术素养</option><option>劳动实践</option></select></label>
            <label><span class="field-label">分值</span><input v-model="form.score" class="field w-full" type="number" step="0.5" /></label>
            <label><span class="field-label">日期</span><input v-model="form.occurredAt" class="field w-full" type="date" /></label>
            <label class="span-2"><span class="field-label">说明</span><textarea v-model="form.description" class="field w-full" rows="3"></textarea></label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-blue" type="button" :disabled="saving" @click="submit"><span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
