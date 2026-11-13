<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const groups = computed(() => {
  const m = new Map()
  for (const r of rows.value) {
    const key = r.targetName || '未命名目标'
    if (!m.has(key)) m.set(key, [])
    m.get(key).push(r)
  }
  return [...m.entries()].map(([name, stages]) => {
    const done = stages.filter(s => s.status === '已完成').length
    return { name, stages, done, total: stages.length, rate: stages.length ? Math.round(done * 100 / stages.length) : 0 }
  })
})
const doneCount = computed(() => rows.value.filter(r => r.status === '已完成').length)
const rate = computed(() => rows.value.length ? Math.round(doneCount.value * 100 / rows.value.length) : 0)
const nextStatus = (s) => s === '未开始' ? '进行中' : s === '进行中' ? '已完成' : '未开始'
const statusClass = (s) => s === '已完成' ? 'pill-mint' : s === '进行中' ? 'pill-sky' : 'pill-neutral'

async function load() {
  loading.value = true; error.value = ''
  try { rows.value = (await R.planning.targetPaths.list()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date(); d.setMonth(d.getMonth() + 1)
  return {
    targetName: '', stage: '启动', milestone: '',
    deadline: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    status: '未开始', note: '',
  }
}
function openCreate(name = '') {
  editing.value = null; form.value = defaults()
  if (name && name !== '未命名目标') form.value.targetName = name
  error.value = ''; showForm.value = true
}
function openEdit(r) { editing.value = r.id; form.value = { ...r }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    if (editing.value) await R.planning.targetPaths.update(editing.value, form.value)
    else await R.planning.targetPaths.save(form.value)
    showForm.value = false; notice.value = '阶段已保存'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function advance(s) {
  error.value = ''
  try { await feature.setTargetStatus(s.id, nextStatus(s.status || '未开始')); notice.value = '阶段状态已推进'; await load() }
  catch (e) { error.value = e.message }
}

async function remove(s) {
  if (!window.confirm(`确定删除阶段「${s.milestone || s.stage}」吗？`)) return
  try { await R.planning.targetPaths.remove(s.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / TARGET" title="目标路径"
      description="把一个大目标拆成可推进的阶段；复习计划可一键分解成四段里程碑，点状态即可从未开始推进到完成。"
      :actions="[
        { label: '新增阶段', icon: '＋', color: 'violet', click: () => openCreate() },
        { label: '复习计划', icon: '✎', color: 'blue', to: '/planning/review' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '目标数', value: groups.length }, { label: '阶段总数', value: rows.length },
        { label: '已完成', value: doneCount }, { label: '总完成率', value: `${rate}%` },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div v-if="loading" class="mt-5 grid gap-4"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:150px"></div></div>
    <EmptyState v-else-if="!groups.length" text="还没有目标路径" hint="新增阶段，或到复习计划页把一条计划拆成里程碑" />

    <div v-else class="mt-5 grid gap-5">
      <div v-for="g in groups" :key="g.name" class="glass p-6" v-reveal="1">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div><p class="eyebrow">TARGET</p><h2 class="section-title mt-1">{{ g.name }}</h2></div>
          <div class="flex items-center gap-3">
            <span class="pill pill-mint">{{ g.done }}/{{ g.total }} 阶段完成</span>
            <button class="btn btn-slate" type="button" @click="openCreate(g.name)">＋ 加阶段</button>
          </div>
        </div>
        <div class="progress-track mb-5"><div class="progress-fill" :style="{ width: g.rate + '%' }"></div></div>

        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <div v-for="s in g.stages" :key="s.id" class="glass-flat flex flex-col p-4">
            <div class="flex items-center justify-between gap-2">
              <span class="pill" :class="statusClass(s.status)">{{ s.status || '未开始' }}</span>
              <span class="text-[11px] faint">{{ s.deadline || '无期限' }}</span>
            </div>
            <p class="mt-3 text-[11px] faint">{{ s.stage || '自定义阶段' }}</p>
            <h3 class="mt-1 flex-1 text-[14px] font-medium ink">{{ s.milestone || '未命名里程碑' }}</h3>
            <p v-if="s.note" class="mt-2 text-[11px] leading-5 muted">{{ s.note }}</p>
            <div class="mt-4 flex flex-wrap gap-2">
              <button class="btn btn-violet" type="button" @click="advance(s)"><span class="btn-glyph">→</span> 推进</button>
              <button class="btn btn-slate" type="button" @click="openEdit(s)">编辑</button>
              <button class="btn btn-rose" type="button" @click="remove(s)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}阶段</h2>
          <p class="mb-5 text-[12px] muted">同一「目标名称」的阶段会归到同一条路径里。</p>
          <div class="form-grid">
            <label><span class="field-label">目标名称</span><input v-model="form.targetName" class="field w-full" list="target-names" />
              <datalist id="target-names"><option v-for="g in groups" :key="g.name" :value="g.name" /></datalist></label>
            <label><span class="field-label">阶段</span><input v-model="form.stage" class="field w-full" /></label>
            <label class="span-2"><span class="field-label">里程碑</span><input v-model="form.milestone" class="field w-full" /></label>
            <label><span class="field-label">截止日期</span><input v-model="form.deadline" class="field w-full" type="date" /></label>
            <label><span class="field-label">状态</span><select v-model="form.status" class="field w-full">
              <option>未开始</option><option>进行中</option><option>已完成</option></select></label>
            <label class="span-2"><span class="field-label">备注</span><textarea v-model="form.note" class="field w-full" rows="2"></textarea></label>
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
