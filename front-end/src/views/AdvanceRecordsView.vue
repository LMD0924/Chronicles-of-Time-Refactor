<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const selected = ref([])
const syncStatus = ref({ work: 0, advance: 0 })
const loading = ref(true)
const saving = ref(false)
const syncing = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})
const keyword = ref('')
const filterCategory = ref('')

const categories = ['晋升', '获奖', '认证', '突破', '其他']
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rows.value
    .filter(r => !filterCategory.value || r.category === filterCategory.value)
    .filter(r => !kw || Object.values(r).some(v => v != null && String(v).toLowerCase().includes(kw)))
    .sort((a, b) => String(b.occurredAt || '').localeCompare(String(a.occurredAt || '')))
})

async function load() {
  loading.value = true; error.value = ''
  try {
    const [list, status] = await Promise.all([
      R.timeline.advanceRecords.list(), feature.resumeSyncStatus(),
    ])
    rows.value = list || []
    syncStatus.value = status || { work: 0, advance: 0 }
    selected.value = []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date()
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return { title: '', occurredAt: today, category: '突破', description: '' }
}
function openCreate() { editing.value = null; form.value = defaults(); showForm.value = true; notice.value = '' }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; showForm.value = true; notice.value = '' }

async function submit() {
  if (!form.value.title?.trim()) { error.value = '请填写标题'; return }
  saving.value = true; error.value = ''
  try {
    if (editing.value) await R.timeline.advanceRecords.update(editing.value, { ...form.value })
    else await R.timeline.advanceRecords.save({ ...form.value })
    showForm.value = false
    notice.value = '进阶记录已保存，并作为高亮节点汇入时间线'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.title}」吗？`)) return
  try { await R.timeline.advanceRecords.remove(r.id); notice.value = '进阶记录已删除'; await load() }
  catch (e) { error.value = e.message }
}

async function sync() {
  if (syncing.value) return
  syncing.value = true; error.value = ''
  try {
    const res = await feature.syncResume('advance', selected.value)
    notice.value = `已写入简历 ${res.created} 段经历，跳过已存在 ${res.skipped} 段`
    await load()
  } catch (e) { error.value = e.message }
  finally { syncing.value = false }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="TIMELINE / ADVANCES" title="进阶记录"
      description="晋升、获奖、突破与能力跃迁，作为时间线上的高亮节点，同样可一键写进个人简历。"
      :actions="[
        { label: '新增进阶', icon: '＋', color: 'violet', click: openCreate },
        { label: '同步到简历', icon: '⇥', color: 'amber', click: sync },
        { label: '简历', icon: '▤', color: 'teal', to: '/archive/resume' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '进阶记录', value: rows.length },
        { label: '已同步简历', value: syncStatus.advance },
        { label: '已选中', value: selected.length },
        { label: '时间线来源', value: '进阶' },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p>
        <p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <div class="glass mt-5 flex flex-wrap items-center gap-3 p-4" v-reveal="1">
      <input v-model="keyword" class="field max-w-[240px]" type="search" placeholder="搜索标题 / 描述…" />
      <select v-model="filterCategory" class="field w-[130px]">
        <option value="">全部类别</option><option v-for="c in categories" :key="c">{{ c }}</option>
      </select>
      <span class="text-[11px] faint">勾选后可只同步选中的几段；不勾选则同步全部。</span>
      <RouterLink to="/timeline" class="btn btn-slate ml-auto"><span class="btn-glyph">⌁</span> 查看统一时间线</RouterLink>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-violet mt-4">{{ notice }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:96px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="还没有符合条件的进阶记录" hint="点击「新增进阶」开始" />

    <div v-else class="tl-rail mt-7">
      <div v-for="r in filtered" :key="r.id" class="tl-node mb-5" :class="{ 'tl-milestone': r.category === '获奖' }" v-reveal="2">
        <div class="glass lift p-5">
          <div class="flex flex-wrap items-center gap-2">
            <input v-model="selected" type="checkbox" :value="r.id" :aria-label="`选择 ${r.title}`" />
            <span class="pill pill-violet">{{ r.category || '进阶' }}</span>
            <span class="text-[11px] faint">{{ r.occurredAt || '未标日期' }}</span>
            <span class="ml-auto text-[11px] faint">#{{ r.id }}</span>
          </div>
          <h3 class="mt-2 text-[15px] font-semibold ink">{{ r.title }}</h3>
          <p class="mt-1 text-[12px] leading-6 muted">{{ r.description || '暂无描述' }}</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <button class="btn btn-violet" type="button" @click="openEdit(r)"><span class="btn-glyph">✎</span> 编辑</button>
            <button class="btn btn-rose" type="button" @click="remove(r)"><span class="btn-glyph">×</span> 删除</button>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }} · 进阶记录</h2>
          <p class="mb-5 text-[12px] muted">保存后会自动出现在拾光时间线，可在上方一键同步到简历。</p>
          <div class="form-grid">
            <label><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" /></label>
            <label><span class="field-label">日期</span><input v-model="form.occurredAt" class="field w-full" type="date" /></label>
            <label><span class="field-label">类别</span><select v-model="form.category" class="field w-full"><option v-for="c in categories" :key="c">{{ c }}</option></select></label>
            <label class="span-2"><span class="field-label">描述</span><textarea v-model="form.description" class="field w-full" rows="4"></textarea></label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-violet" type="button" :disabled="saving" @click="submit"><span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
