<script setup>
import { onMounted, reactive, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const sections = [
  { key: 'education', label: '教育经历', color: 'sky' },
  { key: 'work', label: '工作 / 实习', color: 'mint' },
  { key: 'project', label: '项目经历', color: 'violet' },
  { key: 'skill', label: '技能', color: 'amber' },
  { key: 'certificate', label: '证书', color: 'rose' },
  { key: 'social', label: '社会实践', color: 'teal' },
]
const grouped = ref({})
const active = ref('education')
const form = reactive({ sectionType: 'education', title: '', subtitle: '', content: '', startDate: '', endDate: '' })
const msg = ref('')

async function load() { try { grouped.value = (await feature.completeResume()) || {} } catch (e) { msg.value = e.message } }
function pick(s) { active.value = s.key; form.sectionType = s.key }
async function add() {
  if (!form.title) { msg.value = '请填写标题'; return }
  try { await R.identity.resumeSections.save({ ...form }); form.title = ''; form.subtitle = ''; form.content = ''; msg.value = '已添加'; await load() }
  catch (e) { msg.value = e.message }
}
async function remove(id) { try { await R.identity.resumeSections.remove(id); await load() } catch (e) { msg.value = e.message } }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / RESUME" title="简历"
      description="教育、工作、项目、技能、证书与社会实践统一由条目构成，按需导出与复用。"
      :actions="[{ label: '证书档案', icon: '★', color: 'amber', to: '/archive/certificates' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />
    <div class="tabs">
      <button v-for="s in sections" :key="s.key" class="tab-btn" :class="{ 'is-active': active === s.key }" @click="pick(s)">{{ s.label }}</button>
    </div>
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title mb-4">新增「{{ sections.find(s => s.key === active)?.label }}」条目</h2>
        <div class="form-grid">
          <label><span class="field-label">标题</span><input v-model="form.title" class="field w-full" /></label>
          <label><span class="field-label">副标题</span><input v-model="form.subtitle" class="field w-full" /></label>
          <label><span class="field-label">开始</span><input v-model="form.startDate" class="field w-full" type="date" /></label>
          <label><span class="field-label">结束</span><input v-model="form.endDate" class="field w-full" type="date" /></label>
          <label class="span-2"><span class="field-label">内容</span><textarea v-model="form.content" class="field w-full" rows="4"></textarea></label>
        </div>
        <button class="btn btn-mint mt-4" @click="add"><span class="btn-glyph">＋</span> 添加条目</button>
        <p v-if="msg" class="mt-3 text-[12px] muted">{{ msg }}</p>
      </div>
      <div class="glass p-6">
        <h2 class="section-title mb-4">已归档条目 ({{ (grouped[active] || []).length }})</h2>
        <div v-for="r in (grouped[active] || [])" :key="r.id" class="glass-flat mb-3 p-4">
          <div class="flex items-center justify-between gap-2">
            <h3 class="text-[14px] font-medium ink">{{ r.title }}</h3>
            <button class="btn btn-rose" @click="remove(r.id)">删除</button>
          </div>
          <p class="mt-1 text-[11px] faint">{{ r.subtitle }} · {{ r.startDate }} ~ {{ r.endDate }}</p>
          <p class="mt-2 text-[12px] muted">{{ r.content }}</p>
        </div>
        <p v-if="!(grouped[active] || []).length" class="text-[12px] muted">这一组还没有条目</p>
      </div>
    </div>
  </section>
</template>
