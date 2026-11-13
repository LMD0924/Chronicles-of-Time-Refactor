<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const subjects = ref([])
const picked = ref([])
const matched = ref({ items: [], total: 0, matched: 0 })
const keyword = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const visible = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return matched.value.items || []
  return (matched.value.items || []).filter(m => ['majorName', 'majorCode', 'category', 'requiredSubjects']
    .some(k => String(m[k] || '').toLowerCase().includes(kw)))
})

function toggle(s) {
  const i = picked.value.indexOf(s)
  if (i >= 0) picked.value.splice(i, 1); else picked.value.push(s)
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [subs, result] = await Promise.all([
      feature.subjects(), feature.matchMajors(picked.value.join(',')),
    ])
    subjects.value = (subs || []).map(s => s.name).filter(Boolean)
    matched.value = result || { items: [], total: 0, matched: 0 }
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

watch(picked, () => { load() }, { deep: true })

function defaults() { return { majorCode: '', majorName: '', category: '', level: '常规', requiredSubjects: '', description: '' } }
function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    if (editing.value) await R.planning.majorRequirements.update(editing.value, form.value)
    else await R.planning.majorRequirements.save(form.value)
    showForm.value = false; notice.value = '专业要求已保存，匹配结果同步刷新'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.majorName}」吗？`)) return
  try { await R.planning.majorRequirements.remove(r.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / MAJORS" title="专业要求"
      description="勾选已选科目，实时检查每个专业的选科要求是否满足；缺哪科直接标出，结果按缺口从少到多排序。"
      :actions="[
        { label: '新增专业', icon: '＋', color: 'teal', click: openCreate },
        { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div><p class="eyebrow">MY SUBJECTS</p><h2 class="section-title mt-1">我的选科组合</h2></div>
        <span class="pill pill-mint">{{ matched.matched || 0 }} / {{ matched.total || 0 }} 个专业满足要求</span>
      </div>
      <div class="mt-4 flex flex-wrap gap-2">
        <button v-for="s in subjects" :key="s" class="btn" :class="picked.includes(s) ? 'btn-violet' : 'btn-slate'" type="button" @click="toggle(s)">
          {{ picked.includes(s) ? '✓ ' : '' }}{{ s }}
        </button>
        <button v-if="picked.length" class="btn btn-rose" type="button" @click="picked = []">清空</button>
        <span v-if="!subjects.length" class="text-[12px] muted">学科字典为空，可到「学科与组合」维护</span>
      </div>
      <p class="mt-3 text-[11px] faint">勾选后自动重新计算匹配结果；未勾选任何科目时会列出全部专业及其要求。</p>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div class="crud-toolbar mt-5">
      <input v-model="keyword" class="field max-w-[280px]" type="search" placeholder="搜索专业 / 代码 / 门类…" />
    </div>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:110px"></div></div>
    <EmptyState v-else-if="!visible.length" text="没有匹配的专业" hint="换个关键词，或新增一条专业要求" />
    <div v-else class="mt-5 grid gap-3 md:grid-cols-2">
      <div v-for="m in visible" :key="m.id" class="glass p-4">
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div class="min-w-0">
            <span class="pill pill-sky">{{ m.majorCode || '—' }}</span>
            <h3 class="mt-2 text-[14px] font-semibold ink">{{ m.majorName }}</h3>
            <p class="mt-1 text-[11px] faint">{{ m.category }} · {{ m.level }}</p>
          </div>
          <span class="pill" :class="m.satisfied ? 'pill-mint' : 'pill-rose'">{{ m.satisfied ? '满足要求' : `缺 ${m.missing?.length || 0} 科` }}</span>
        </div>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span class="pill pill-neutral">要求：{{ m.requiredSubjects || '不限' }}</span>
          <span v-for="x in (m.missing || [])" :key="x" class="pill pill-rose">缺 {{ x }}</span>
        </div>
        <p v-if="m.description" class="mt-2 text-[11px] leading-5 muted">{{ m.description }}</p>
        <div class="mt-3 flex gap-2">
          <button class="btn btn-slate" type="button" @click="openEdit(m)">编辑</button>
          <button class="btn btn-rose" type="button" @click="remove(m)">删除</button>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}专业要求</h2>
          <p class="mb-5 text-[12px] muted">选科要求用逗号分隔，如「物理,化学」，留空表示不限。</p>
          <div class="form-grid">
            <label><span class="field-label">专业代码</span><input v-model="form.majorCode" class="field w-full" /></label>
            <label><span class="field-label">专业名称</span><input v-model="form.majorName" class="field w-full" /></label>
            <label><span class="field-label">门类</span><input v-model="form.category" class="field w-full" /></label>
            <label><span class="field-label">层次</span><select v-model="form.level" class="field w-full">
              <option>热门</option><option>常规</option><option>冷门</option></select></label>
            <label class="span-2"><span class="field-label">选科要求</span><input v-model="form.requiredSubjects" class="field w-full" list="major-subjects" />
              <datalist id="major-subjects"><option v-for="s in subjects" :key="s" :value="s" /></datalist></label>
            <label class="span-2"><span class="field-label">说明</span><textarea v-model="form.description" class="field w-full" rows="2"></textarea></label>
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
