<script setup>
import { onMounted, ref } from 'vue'
import http from '@/utils/request'

const fetchDictionaries = () => http.get('/api/v1/admin/dictionaries')
const addDict = (type, data) => http.post(`/api/v1/admin/dictionaries/${type}`, data)
const updateDict = (type, id, data) => http.put(`/api/v1/admin/dictionaries/${type}/${id}`, data)
const deleteDict = (type, id) => http.delete(`/api/v1/admin/dictionaries/${type}/${id}`)

const tabs = [
  { key: 'subject_option', type: 'subject-option', label: '学科字典', color: 'sky', fields: [
    { key: 'name', label: '名称' }, { key: 'category', label: '类别' },
    { key: 'isFirst', label: '首选', type: 'checkbox' }, { key: 'isRequired', label: '必修', type: 'checkbox' },
    { key: 'description', label: '说明' }] },
  { key: 'grading_scale', type: 'grading-scale', label: '等级量表', color: 'amber', fields: [
    { key: 'subjectId', label: '学科 ID', type: 'number' }, { key: 'level', label: '等级' },
    { key: 'minScore', label: '最低分', type: 'number' }, { key: 'maxScore', label: '最高分', type: 'number' },
    { key: 'ratio', label: '比例', type: 'number' }, { key: 'active', label: '启用', type: 'checkbox' }] },
  { key: 'major_requirement', type: 'major-requirement', label: '专业要求', color: 'teal', fields: [
    { key: 'majorCode', label: '专业代码' }, { key: 'majorName', label: '专业名称' },
    { key: 'category', label: '门类' }, { key: 'level', label: '层次' },
    { key: 'requiredSubjects', label: '选科要求' }] },
]

const tab = ref('subject_option')
const data = ref({}), form = ref({}), msg = ref(''), error = ref('')
const editing = ref(null), editForm = ref({})
const current = () => tabs.find(t => t.key === tab.value)
const rows = () => data.value[tab.value] || []

function display(r, f) {
  const v = r[f.key]
  if (f.type === 'checkbox') return v ? '是' : '否'
  return v === null || v === undefined || v === '' ? '—' : v
}
function reset() {
  form.value = {}
  current().fields.forEach(f => { form.value[f.key] = f.type === 'checkbox' ? false : '' })
}
function switchTab(k) { tab.value = k; editing.value = null; error.value = ''; reset() }

async function load() {
  error.value = ''
  try { data.value = (await fetchDictionaries()) || {} } catch (e) { error.value = e.message }
}
async function save() {
  msg.value = ''; error.value = ''
  try {
    await addDict(current().type, { ...form.value })
    msg.value = '字典条目已新增（操作已记入审计）'
    reset(); await load()
  } catch (e) { error.value = e.message }
}
function startEdit(r) { editing.value = r.id; editForm.value = { ...r } }
function cancelEdit() { editing.value = null; editForm.value = {} }
async function saveEdit() {
  msg.value = ''; error.value = ''
  try {
    await updateDict(current().type, editing.value, { ...editForm.value })
    msg.value = '字典条目已更新（操作已记入审计）'
    cancelEdit(); await load()
  } catch (e) { error.value = e.message }
}
async function remove(id) {
  msg.value = ''; error.value = ''
  if (!window.confirm('确定删除该字典条目吗？')) return
  try {
    await deleteDict(current().type, id)
    msg.value = '字典条目已删除（操作已记入审计）'
    await load()
  } catch (e) { error.value = e.message }
}
onMounted(() => { load(); reset() })
</script>

<template>
  <div>
    <h2 class="section-title mb-4">数据字典</h2>
    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }"
              @click="switchTab(t.key)">{{ t.label }}</button>
    </div>
    <p v-if="msg" class="mb-2 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="mb-2 text-[12px] text-rose-500">{{ error }}</p>

    <div class="glass mb-5 p-5">
      <h3 class="section-title mb-3">新增 · {{ current().label }}</h3>
      <div class="form-grid">
        <label v-for="f in current().fields" :key="f.key">
          <span class="field-label">{{ f.label }}</span>
          <input v-if="f.type === 'checkbox'" v-model="form[f.key]" type="checkbox" />
          <input v-else v-model="form[f.key]" class="field w-full" :type="f.type || 'text'" />
        </label>
      </div>
      <button class="btn mt-4" :class="`btn-${current().color}`" @click="save"><span class="btn-glyph">＋</span> 新增</button>
    </div>

    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr>
          <th>ID</th>
          <th v-for="f in current().fields" :key="f.key">{{ f.label }}</th>
          <th>操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="r in rows()" :key="r.id">
            <template v-if="editing === r.id">
              <td>{{ r.id }}</td>
              <td v-for="f in current().fields" :key="f.key">
                <input v-if="f.type === 'checkbox'" v-model="editForm[f.key]" type="checkbox" />
                <input v-else v-model="editForm[f.key]" class="field w-full" :type="f.type || 'text'" />
              </td>
              <td class="whitespace-nowrap">
                <button class="btn btn-mint" @click="saveEdit">保存</button>
                <button class="btn btn-slate ml-1" @click="cancelEdit">取消</button>
              </td>
            </template>
            <template v-else>
              <td>{{ r.id }}</td>
              <td v-for="f in current().fields" :key="f.key" class="max-w-[180px] truncate">{{ display(r, f) }}</td>
              <td class="whitespace-nowrap">
                <button class="btn btn-blue" @click="startEdit(r)">编辑</button>
                <button class="btn btn-rose ml-1" @click="remove(r.id)">删除</button>
              </td>
            </template>
          </tr>
          <tr v-if="!rows().length"><td :colspan="current().fields.length + 2" class="text-center muted">暂无条目</td></tr>
        </tbody>
      </table>
      <p class="p-2 text-[11px] faint">共 {{ rows().length }} 条 · 行内编辑后点击保存，所有写操作均记入审计。</p>
    </div>
  </div>
</template>
