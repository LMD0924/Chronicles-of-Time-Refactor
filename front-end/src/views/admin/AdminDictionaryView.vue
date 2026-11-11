<script setup>
import { onMounted, ref } from 'vue'
import { R } from '@/api'

const tab = ref('subjects')
const tabs = [
  { key: 'subjects', label: '学科字典', api: R.planning.subjectOptions, fields: [
    { key: 'name', label: '名称' }, { key: 'category', label: '类别' }, { key: 'isFirst', label: '首选', type: 'checkbox' },
    { key: 'isRequired', label: '必修', type: 'checkbox' }, { key: 'description', label: '说明' }], color: 'sky' },
  { key: 'grading', label: '等级量表', api: R.planning.gradingScales, fields: [
    { key: 'subjectId', label: '学科 ID', type: 'number' }, { key: 'level', label: '等级' },
    { key: 'minScore', label: '最低分', type: 'number' }, { key: 'maxScore', label: '最高分', type: 'number' },
    { key: 'ratio', label: '比例', type: 'number' }, { key: 'active', label: '启用', type: 'checkbox' }], color: 'amber' },
  { key: 'majors', label: '专业要求', api: R.planning.majorRequirements, fields: [
    { key: 'majorCode', label: '专业代码' }, { key: 'majorName', label: '专业名称' }, { key: 'category', label: '门类' },
    { key: 'level', label: '层次' }, { key: 'requiredSubjects', label: '选科要求' }], color: 'teal' },
]
const rows = ref([]), form = ref({}), msg = ref('')
const current = () => tabs.find(t => t.key === tab.value)

async function load() { try { rows.value = (await current().api.all()) || [] } catch (e) { msg.value = e.message } }
function reset() { form.value = {}; current().fields.forEach(f => { form.value[f.key] = f.type === 'checkbox' ? false : '' }) }
async function save() {
  try { await current().api.save({ ...form.value }); msg.value = '已保存'; reset(); await load() } catch (e) { msg.value = e.message }
}
async function remove(id) { try { await current().api.remove(id); await load() } catch (e) { msg.value = e.message } }
onMounted(() => { load(); reset() })
</script>

<template>
  <div>
    <h2 class="section-title mb-4">数据字典</h2>
    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }"
              @click="tab = t.key; reset(); load()">{{ t.label }}</button>
    </div>
    <p v-if="msg" class="mb-2 text-[12px] muted">{{ msg }}</p>
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-5">
        <h3 class="section-title mb-3">新增字典项</h3>
        <div class="form-grid">
          <label v-for="f in current().fields" :key="f.key">
            <span class="field-label">{{ f.label }}</span>
            <input v-if="['text', 'number'].includes(f.type || 'text')" v-model="form[f.key]" class="field w-full" :type="f.type || 'text'" />
            <label v-else class="inline-flex items-center gap-2 text-[12px] muted"><input v-model="form[f.key]" type="checkbox" /> 是</label>
          </label>
        </div>
        <button class="btn mt-4" :class="`btn-${current().color}`" @click="save"><span class="btn-glyph">＋</span> 新增</button>
      </div>
      <div class="glass p-5">
        <h3 class="section-title mb-3">现有条目 ({{ rows.length }})</h3>
        <div v-for="r in rows" :key="r.id" class="flex items-center gap-2 border-b border-hairline py-2 text-[12px] last:border-0">
          <span class="ink">{{ r.name || r.level || r.majorName || ('#' + r.id) }}</span>
          <span class="ml-auto faint">{{ r.category || r.requiredSubjects || r.description }}</span>
          <button class="btn btn-rose" @click="remove(r.id)">删除</button>
        </div>
        <p v-if="!rows.length" class="text-[12px] muted">暂无条目</p>
      </div>
    </div>
  </div>
</template>
