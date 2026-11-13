<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const types = ['分类', '课程', '知识点']
const tree = ref([]), loading = ref(true), saving = ref(false)
const msg = ref(''), error = ref(''), showForm = ref(false), editing = ref(null)
const form = reactive({ name: '', nodeType: '分类', parentId: 0, credit: 0, sortNo: 0, description: '' })

function flatten(nodes, depth = 0, out = []) {
  for (const n of nodes || []) {
    out.push({ ...n, depth })
    flatten(n.children || [], depth + 1, out)
  }
  return out
}
const flat = computed(() => flatten(tree.value))
const totalCredit = computed(() => Math.round(flat.value.reduce((s, n) => s + (Number(n.credit) || 0), 0) * 10) / 10)
const typeCount = computed(() => {
  const m = {}
  types.forEach(t => { m[t] = flat.value.filter(n => n.nodeType === t).length })
  return m
})

function reset() { editing.value = null; Object.assign(form, { name: '', nodeType: '分类', parentId: 0, credit: 0, sortNo: 0, description: '' }) }
function edit(n) {
  editing.value = n.id
  Object.assign(form, { name: n.name, nodeType: n.nodeType, parentId: n.parentId || 0, credit: n.credit || 0, sortNo: n.sortNo || 0, description: n.description || '' })
  showForm.value = true
}
async function load() {
  loading.value = true; error.value = ''
  try { tree.value = (await feature.courseTree()) || [] } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
async function save() {
  if (!form.name.trim()) { error.value = '请填写节点名称'; return }
  saving.value = true; error.value = ''; msg.value = ''
  try {
    const payload = { ...form, parentId: Number(form.parentId) || 0, credit: Number(form.credit) || 0, sortNo: Number(form.sortNo) || 0 }
    if (editing.value) await feature.updateCourseNode(editing.value, payload)
    else await feature.addCourseNode(payload)
    msg.value = editing.value ? '节点已更新' : '节点已新增'
    showForm.value = false; reset(); await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}
async function remove(n) {
  if (!window.confirm(`确定删除「${n.name}」吗？其子节点会提升为根节点。`)) return
  try {
    const r = await feature.deleteCourseNode(n.id)
    msg.value = `已删除，${r.promotedChildren || 0} 个子节点提升为根节点`
    await load()
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / COURSE TREE" title="课程树"
      description="分类 → 课程 → 知识点层级维护；删除节点时子节点自动提升为根节点，不会误删整棵子树。"
      :actions="[{ label: '知识图谱', icon: '⌘', color: 'violet', to: '/learning/graph' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="glass mb-5 grid grid-cols-2 gap-4 p-5 md:grid-cols-5">
      <div><p class="eyebrow">节点总数</p><p class="mt-1 text-[20px] font-semibold ink">{{ flat.length }}</p></div>
      <div v-for="t in types" :key="t"><p class="eyebrow">{{ t }}</p><p class="mt-1 text-[20px] font-semibold ink">{{ typeCount[t] }}</p></div>
      <div><p class="eyebrow">总学分</p><p class="mt-1 text-[20px] font-semibold ink">{{ totalCredit }}</p></div>
    </div>

    <div class="crud-toolbar">
      <button class="btn btn-mint" @click="showForm = !showForm; reset()"><span class="btn-glyph">＋</span> 新增节点</button>
      <span class="pill pill-sky">{{ flat.length }} 个节点</span>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <div v-if="showForm" class="glass mb-5 p-6">
      <h2 class="section-title mb-4">{{ editing ? '编辑节点' : '新增节点' }}</h2>
      <div class="form-grid">
        <label><span class="field-label">名称 *</span><input v-model="form.name" class="field w-full" /></label>
        <label><span class="field-label">类型</span><select v-model="form.nodeType" class="field w-full"><option v-for="t in types" :key="t">{{ t }}</option></select></label>
        <label><span class="field-label">父节点</span>
          <select v-model="form.parentId" class="field w-full">
            <option :value="0">（根节点）</option>
            <option v-for="n in flat.filter(x => x.id !== editing)" :key="n.id" :value="n.id">{{ '　'.repeat(n.depth) + n.name }}</option>
          </select>
        </label>
        <label><span class="field-label">学分</span><input v-model="form.credit" class="field w-full" type="number" step="0.5" /></label>
        <label><span class="field-label">排序</span><input v-model="form.sortNo" class="field w-full" type="number" /></label>
        <label><span class="field-label">说明</span><input v-model="form.description" class="field w-full" /></label>
      </div>
      <button class="btn btn-blue mt-4" :disabled="saving" @click="save"><span class="btn-glyph">✓</span> 保存</button>
    </div>

    <div v-if="loading" class="placeholder-bar" style="height:180px"></div>
    <div v-else-if="!flat.length" class="glass p-10 text-center text-[13px] muted">还没有课程节点，点「新增节点」开始搭建。</div>
    <div v-else class="glass p-4">
      <div v-for="n in flat" :key="n.id" class="glass-flat mb-2 flex flex-wrap items-center gap-2 p-3" :style="{ marginLeft: (n.depth * 24) + 'px' }">
        <span class="pill" :class="n.nodeType === '知识点' ? 'pill-mint' : n.nodeType === '课程' ? 'pill-sky' : 'pill-violet'">{{ n.nodeType }}</span>
        <span class="text-[13px] font-medium ink">{{ n.name }}</span>
        <span v-if="n.credit" class="text-[11px] faint">{{ n.credit }} 学分</span>
        <span v-if="n.description" class="text-[11px] muted">{{ n.description }}</span>
        <button class="btn btn-slate ml-auto" @click="edit(n)">编辑</button>
        <button class="btn btn-rose" @click="remove(n)">删除</button>
      </div>
    </div>
  </section>
</template>
