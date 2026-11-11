<script setup>
import { computed, onMounted, ref } from 'vue'
import PageHeader from './PageHeader.vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  api: { type: Object, required: true },
  title: { type: String, required: true },
  eyebrow: { type: String, default: '' },
  description: { type: String, default: '' },
  color: { type: String, default: 'blue' },
  fields: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  cardView: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  extraActions: { type: Array, default: () => [] }
})

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const selected = ref([])
const showForm = ref(false)
const editing = ref(null)
const form = ref({})
const saving = ref(false)
const error = ref('')
const stats = ref(null)

const cols = computed(() => props.columns.length ? props.columns : props.fields.slice(0, 5))
const filtered = computed(() => {
  if (!keyword.value.trim()) return rows.value
  const kw = keyword.value.toLowerCase()
  return rows.value.filter(r => Object.values(r).some(v => v != null && String(v).toLowerCase().includes(kw)))
})

function defaults() {
  const d = {}
  props.fields.forEach(f => { d[f.key] = f.type === 'checkbox' ? false : (f.default ?? '') })
  return d
}

async function load() {
  loading.value = true; error.value = ''
  try {
    rows.value = (await props.api.list({ keyword: keyword.value })) || []
    selected.value = []
    try { stats.value = await props.api.stats() } catch { /* 可选 */ }
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(row) { editing.value = row.id; form.value = { ...row }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    props.fields.forEach(f => {
      if (f.type === 'number' && payload[f.key] !== '' && payload[f.key] != null) payload[f.key] = Number(payload[f.key])
    })
    if (editing.value) await props.api.update(editing.value, payload)
    else await props.api.save(payload)
    showForm.value = false
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(row) {
  if (!window.confirm(`确定删除「${row.title || row.name || row.id}」吗？`)) return
  try { await props.api.remove(row.id); await load() } catch (e) { error.value = e.message }
}

async function batchRemove() {
  if (!selected.value.length || !window.confirm(`确定删除选中的 ${selected.value.length} 项吗？`)) return
  try { await props.api.batchRemove(selected.value); await load() } catch (e) { error.value = e.message }
}

function display(row, col) {
  const v = row[col.key]
  if (v == null || v === '') return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (col.type === 'date' && typeof v === 'string') return v.slice(0, 10)
  return String(v).length > 46 ? String(v).slice(0, 46) + '…' : String(v)
}

const actions = computed(() => {
  const a = [...props.extraActions]
  if (!props.readonly) a.unshift({ label: '新增', icon: '＋', color: props.color, click: openCreate })
  a.push({ label: '刷新', icon: '↻', color: 'slate', click: load })
  return a
})

onMounted(load)
</script>

<template>
  <section>
    <PageHeader :eyebrow="eyebrow" :title="title" :description="description" :actions="actions" />

    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[280px]" type="search" placeholder="搜索关键词…" @keyup.enter="load" />
      <button class="btn btn-blue" type="button" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      <button v-if="selected.length && !readonly" class="btn btn-rose" type="button" @click="batchRemove">
        <span class="btn-glyph">×</span> 删除选中 ({{ selected.length }})
      </button>
      <span v-if="stats" class="pill pill-sky">共 {{ rows.length }} 条</span>
      <span v-if="error" class="pill pill-rose">{{ error }}</span>
    </div>

    <div v-if="loading" class="grid gap-3">
      <div v-for="i in 3" :key="i" class="placeholder-bar" style="height: 62px"></div>
    </div>

    <EmptyState v-else-if="!filtered.length" text="暂无数据" hint="点击右上角「新增」开始记录" />

    <!-- 卡片视图 -->
    <div v-else-if="cardView" class="crud-grid">
      <div v-for="row in filtered" :key="row.id" class="crud-card lift" v-reveal="1">
        <div class="flex items-start justify-between gap-2">
          <h3 class="text-[14px] font-semibold ink">{{ row.title || row.name || '未命名' }}</h3>
          <input v-if="!readonly" v-model="selected" type="checkbox" :value="row.id" class="mt-1" />
        </div>
        <p class="mt-2 text-[12px] leading-6 muted">{{ display(row, { key: 'content' }) !== '—' ? display(row, { key: 'content' }) : display(row, { key: 'description' }) }}</p>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span v-for="c in cols.slice(0, 3)" :key="c.key" class="pill pill-neutral">{{ c.label }}: {{ display(row, c) }}</span>
        </div>
        <div v-if="!readonly" class="mt-4 flex gap-2">
          <button class="btn btn-violet" type="button" @click="openEdit(row)"><span class="btn-glyph">✎</span> 编辑</button>
          <button class="btn btn-rose" type="button" @click="remove(row)"><span class="btn-glyph">×</span> 删除</button>
        </div>
      </div>
    </div>

    <!-- 表格视图 -->
    <div v-else class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead>
          <tr>
            <th v-if="!readonly" style="width:38px"><input type="checkbox" :checked="selected.length === filtered.length && filtered.length > 0"
              @change="selected = $event.target.checked ? filtered.map(r => r.id) : []" /></th>
            <th v-for="c in cols" :key="c.key">{{ c.label }}</th>
            <th v-if="!readonly" style="width:130px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in filtered" :key="row.id">
            <td v-if="!readonly"><input v-model="selected" type="checkbox" :value="row.id" /></td>
            <td v-for="c in cols" :key="c.key">{{ display(row, c) }}</td>
            <td v-if="!readonly">
              <button class="btn btn-violet" type="button" @click="openEdit(row)">编辑</button>
              <button class="btn btn-rose ml-1" type="button" @click="remove(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="text-[17px] font-semibold ink mb-1">{{ editing ? '编辑' : '新增' }} · {{ title }}</h2>
          <p class="text-[12px] muted mb-5">带 * 为必填项，保存后自动归到当前账号</p>
          <div class="form-grid">
            <label v-for="f in fields" :key="f.key" class="block" :class="{ 'span-2': f.span === 2 || f.type === 'textarea' }">
              <span class="field-label">{{ f.label }}<span v-if="f.required" class="text-rose-500"> *</span></span>
              <input v-if="['text', 'number', 'date', 'month', 'time'].includes(f.type)" v-model="form[f.key]"
                     class="field w-full" :type="f.type" :placeholder="f.placeholder || ''" />
              <select v-else-if="f.type === 'select'" v-model="form[f.key]" class="field w-full">
                <option value="">请选择</option>
                <option v-for="o in (f.options || [])" :key="typeof o === 'string' ? o : o.value"
                        :value="typeof o === 'string' ? o : o.value">{{ typeof o === 'string' ? o : o.label }}</option>
              </select>
              <textarea v-else-if="f.type === 'textarea'" v-model="form[f.key]" class="field w-full" rows="4" :placeholder="f.placeholder || ''"></textarea>
              <label v-else-if="f.type === 'checkbox'" class="inline-flex items-center gap-2 text-[13px] muted">
                <input v-model="form[f.key]" type="checkbox" /> 是
              </label>
            </label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-blue" type="button" :disabled="saving" @click="submit">
              <span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
