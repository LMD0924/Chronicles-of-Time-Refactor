<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const filter = ref('全部')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const categories = computed(() => ['全部', ...new Set(rows.value.map(r => r.category).filter(Boolean))])
const visible = computed(() => filter.value === '全部' ? rows.value : rows.value.filter(r => r.category === filter.value))
const done = computed(() => rows.value.filter(r => r.done).length)
const rate = computed(() => rows.value.length ? Math.round(done.value * 100 / rows.value.length) : 0)
const byCategory = computed(() => {
  const m = new Map()
  for (const r of rows.value) {
    const k = r.category || '未分类'
    if (!m.has(k)) m.set(k, { total: 0, done: 0 })
    const v = m.get(k); v.total++; if (r.done) v.done++
  }
  return [...m.entries()].map(([name, v]) => ({ name, ...v }))
})
const categoryOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: byCategory.value.map(c => c.name) },
  yAxis: { type: 'value', name: '项数' },
  series: [
    { name: '已完成', type: 'bar', stack: 'c', data: byCategory.value.map(c => c.done), itemStyle: { color: '#12996a' } },
    { name: '待办', type: 'bar', stack: 'c', data: byCategory.value.map(c => c.total - c.done), itemStyle: { color: '#d9464e' } },
  ],
}))

async function load() {
  loading.value = true; error.value = ''
  try { rows.value = (await R.planning.checklistItems.list()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date(); d.setDate(d.getDate() + 30)
  return {
    title: '', category: '强基计划', done: false,
    deadline: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    note: '',
  }
}
function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value, done: !!form.value.done }
    if (editing.value) await R.planning.checklistItems.update(editing.value, payload)
    else await R.planning.checklistItems.save(payload)
    showForm.value = false; notice.value = '清单项已保存'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function toggle(r) {
  error.value = ''
  try { const out = await feature.toggleChecklist(r.id); notice.value = `完成率 ${out.completionRate}%`; await load() }
  catch (e) { error.value = e.message }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.title}」吗？`)) return
  try { await R.planning.checklistItems.remove(r.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / CHECKLIST" title="强基清单"
      description="把报名材料、竞赛、面试准备逐项勾掉；每次勾选都会重算完成率并按分类汇总。"
      :actions="[
        { label: '新增清单项', icon: '＋', color: 'rose', click: openCreate },
        { label: '毕业缺口', icon: '△', color: 'amber', to: '/planning/gap' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="grid gap-5 md:grid-cols-[1fr_1.6fr]">
      <div class="glass p-6 text-center">
        <p class="eyebrow">COMPLETION</p>
        <div class="mx-auto mt-4 grid h-[132px] w-[132px] place-items-center rounded-full border-[10px] border-hairline"
             :style="{ background: `conic-gradient(var(--mint, #12996a) ${rate}%, var(--surface-2) 0)` }">
          <div class="grid h-[100px] w-[100px] place-items-center rounded-full" :style="{ background: 'var(--surface-2)' }">
            <span class="text-[26px] font-semibold ink">{{ rate }}%</span>
          </div>
        </div>
        <p class="mt-4 text-[13px] muted">{{ done }} / {{ rows.length }} 项已完成</p>
        <div class="mt-4 space-y-2">
          <div v-for="c in byCategory" :key="c.name" class="flex items-center justify-between text-[12px]">
            <span class="muted">{{ c.name }}</span><span class="pill pill-neutral">{{ c.done }}/{{ c.total }}</span>
          </div>
        </div>
      </div>

      <div class="glass p-5">
        <h2 class="section-title mb-2">分类完成度</h2>
        <Chart :option="categoryOption" :height="260" />
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div class="crud-toolbar mt-5">
      <button v-for="c in categories" :key="c" class="btn" :class="filter === c ? 'btn-violet' : 'btn-slate'" type="button" @click="filter = c">{{ c }}</button>
    </div>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:100px"></div></div>
    <EmptyState v-else-if="!visible.length" text="这个分类还没有清单项" hint="点右上角「新增清单项」添加" />
    <div v-else class="mt-5 grid gap-3 md:grid-cols-2">
      <div v-for="r in visible" :key="r.id" class="glass flex items-start gap-3 p-4" :class="{ 'opacity-60': r.done }">
        <input type="checkbox" class="mt-1 h-[18px] w-[18px]" :checked="!!r.done" @change="toggle(r)" />
        <div class="min-w-0 flex-1">
          <h3 class="text-[14px] font-medium ink" :class="{ 'line-through': r.done }">{{ r.title }}</h3>
          <div class="mt-1 flex flex-wrap gap-2 text-[11px] faint">
            <span class="pill pill-neutral">{{ r.category || '未分类' }}</span>
            <span>截止 {{ r.deadline || '未设置' }}</span>
          </div>
          <p v-if="r.note" class="mt-2 text-[11px] leading-5 muted">{{ r.note }}</p>
          <div class="mt-3 flex gap-2">
            <button class="btn btn-slate" type="button" @click="openEdit(r)">编辑</button>
            <button class="btn btn-rose" type="button" @click="remove(r)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}清单项</h2>
          <p class="mb-5 text-[12px] muted">保存后可在列表里直接勾选完成。</p>
          <div class="form-grid">
            <label class="span-2"><span class="field-label">标题</span><input v-model="form.title" class="field w-full" /></label>
            <label><span class="field-label">分类</span><input v-model="form.category" class="field w-full" list="check-cats" />
              <datalist id="check-cats"><option v-for="c in categories.slice(1)" :key="c" :value="c" /></datalist></label>
            <label><span class="field-label">截止日期</span><input v-model="form.deadline" class="field w-full" type="date" /></label>
            <label class="span-2"><span class="field-label">备注</span><textarea v-model="form.note" class="field w-full" rows="2"></textarea></label>
            <label class="span-2"><span class="field-label">完成状态</span>
              <label class="inline-flex items-center gap-2 text-[13px] muted"><input v-model="form.done" type="checkbox" /> 已完成</label></label>
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
