<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const stages = ['选题', '开题', '初稿', '修改', '定稿', '答辩']
const tab = ref('papers')
const papers = ref([]), suggestions = ref([])
const loading = ref(true), saving = ref(false), uploading = ref(false)
const msg = ref(''), error = ref('')
const showForm = ref(false), editing = ref(null), pendingFile = ref(null)
const form = reactive({ title: '', keywords: '', status: '选题', advisor: '', abstractText: '', fileUrl: '' })
const suggestionForm = reactive({ paperId: '', author: '', content: '' })
const filteredId = ref('')

const stageCount = computed(() => {
  const m = {}
  stages.forEach(s => { m[s] = papers.value.filter(p => p.status === s).length })
  return m
})
const visibleSuggestions = computed(() => filteredId.value
  ? suggestions.value.filter(s => String(s.paperId) === String(filteredId.value))
  : suggestions.value)

function reset() {
  editing.value = null; pendingFile.value = null
  Object.assign(form, { title: '', keywords: '', status: '选题', advisor: '', abstractText: '', fileUrl: '' })
}
function edit(p) { editing.value = p.id; Object.assign(form, { ...p }); showForm.value = true }

async function load() {
  loading.value = true; error.value = ''
  try {
    const [p, s] = await Promise.all([R.archive.papers.list(), feature.paperSuggestions(null)])
    papers.value = p || []; suggestions.value = s || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function bindFile(id) {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    const uploaded = await feature.uploadFile(pendingFile.value)
    await feature.bindAttachment({ resource: 'papers', id, url: uploaded.url })
    msg.value = `论文文件「${uploaded.fileName}」已上传并绑定`
    pendingFile.value = null
  } finally { uploading.value = false }
}

async function save() {
  if (!form.title.trim()) { error.value = '请填写论文标题'; return }
  saving.value = true; error.value = ''; msg.value = ''
  try {
    const payload = { ...form }
    let id = editing.value
    if (editing.value) await R.archive.papers.update(editing.value, payload)
    else {
      const saved = await R.archive.papers.save(payload)
      id = saved?.id
    }
    if (id) await bindFile(id)
    msg.value = editing.value ? '论文已更新' : '论文已创建'
    showForm.value = false; reset(); await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function advance(p) {
  const next = stages[Math.min(stages.indexOf(p.status) + 1, stages.length - 1)]
  try { await R.archive.papers.update(p.id, { ...p, status: next }); msg.value = `「${p.title}」推进到 ${next}`; await load() }
  catch (e) { error.value = e.message }
}
async function remove(p) {
  if (!window.confirm(`确定删除「${p.title}」吗？`)) return
  try { await R.archive.papers.remove(p.id); await load() } catch (e) { error.value = e.message }
}
async function addSuggestion() {
  if (!suggestionForm.paperId || !suggestionForm.content.trim()) { error.value = '请选择论文并填写建议'; return }
  try {
    await R.archive.paperSuggestions.save({
      paperId: Number(suggestionForm.paperId), author: suggestionForm.author, content: suggestionForm.content,
      createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })
    suggestionForm.content = ''
    msg.value = '建议已归档'; await load()
  } catch (e) { error.value = e.message }
}
async function removeSuggestion(s) {
  try { await R.archive.paperSuggestions.remove(s.id); await load() } catch (e) { error.value = e.message }
}
function onFile(e) { pendingFile.value = e.target.files?.[0] || null }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / PAPERS" title="论文"
      description="论文写作、状态推进、附件与修改建议在同一处完成；文件走本服务的上传与绑定。"
      :actions="[{ label: '论文看板', icon: '▤', color: 'teal', to: '/archive/thesis' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="tabs">
      <button class="tab-btn" :class="{ 'is-active': tab === 'papers' }" @click="tab = 'papers'">论文列表 ({{ papers.length }})</button>
      <button class="tab-btn" :class="{ 'is-active': tab === 'suggestions' }" @click="tab = 'suggestions'">修改建议 ({{ suggestions.length }})</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <template v-if="tab === 'papers'">
      <div class="glass mb-5 flex flex-wrap gap-2 p-4">
        <span v-for="s in stages" :key="s" class="pill" :class="stageCount[s] ? 'pill-violet' : 'pill-neutral'">{{ s }} · {{ stageCount[s] }}</span>
        <button class="btn btn-mint ml-auto" @click="showForm = !showForm; reset()"><span class="btn-glyph">＋</span> 新建论文</button>
      </div>

      <div v-if="showForm" class="glass mb-5 p-6">
        <h2 class="section-title mb-4">{{ editing ? '编辑论文' : '新建论文' }}</h2>
        <div class="form-grid">
          <label class="span-2"><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" /></label>
          <label><span class="field-label">关键词</span><input v-model="form.keywords" class="field w-full" /></label>
          <label><span class="field-label">导师</span><input v-model="form.advisor" class="field w-full" /></label>
          <label><span class="field-label">阶段</span><select v-model="form.status" class="field w-full"><option v-for="s in stages" :key="s">{{ s }}</option></select></label>
          <label><span class="field-label">附件（保存后自动上传并绑定）</span><input class="field w-full" type="file" accept="image/*,.pdf,.doc,.docx" @change="onFile" /></label>
          <label class="span-2"><span class="field-label">摘要</span><textarea v-model="form.abstractText" class="field w-full" rows="3"></textarea></label>
        </div>
        <div class="mt-4 flex gap-2">
          <button class="btn btn-blue" :disabled="saving || uploading" @click="save"><span class="btn-glyph">✓</span> {{ uploading ? '上传中…' : '保存' }}</button>
          <button class="btn btn-slate" @click="showForm = false; reset()">取消</button>
        </div>
      </div>

      <div v-if="loading" class="space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:120px"></div></div>
      <div v-else-if="!papers.length" class="glass p-10 text-center text-[13px] muted">还没有论文记录。</div>
      <div v-else class="space-y-3">
        <div v-for="p in papers" :key="p.id" class="glass p-5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill pill-violet">{{ p.status || '选题' }}</span>
            <h3 class="text-[15px] font-semibold ink">{{ p.title }}</h3>
            <span class="ml-auto text-[11px] faint">导师 {{ p.advisor || '—' }}</span>
          </div>
          <p class="mt-2 text-[12px] leading-6 muted">{{ p.abstractText || '暂无摘要' }}</p>
          <p v-if="p.keywords" class="mt-2 text-[11px] faint">关键词：{{ p.keywords }}</p>
          <div class="mt-3 flex flex-wrap items-center gap-2 border-t border-hairline pt-3">
            <a v-if="p.fileUrl" :href="p.fileUrl" target="_blank" rel="noopener" class="btn btn-violet"><span class="btn-glyph">↗</span> 查看文件</a>
            <span v-else class="text-[11px] faint">暂无附件</span>
            <button class="btn btn-mint ml-auto" :disabled="p.status === '答辩'" @click="advance(p)">推进阶段 →</button>
            <button class="btn btn-slate" @click="edit(p)">编辑</button>
            <button class="btn btn-rose" @click="remove(p)">删除</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="grid gap-5 md:grid-cols-2">
        <div class="glass p-6">
          <h2 class="section-title mb-4">新增建议</h2>
          <div class="form-grid">
            <label class="span-2"><span class="field-label">所属论文</span>
              <select v-model="suggestionForm.paperId" class="field w-full">
                <option value="">选择论文</option>
                <option v-for="p in papers" :key="p.id" :value="p.id">{{ p.title }}</option>
              </select>
            </label>
            <label class="span-2"><span class="field-label">提出人</span><input v-model="suggestionForm.author" class="field w-full" placeholder="导师 / 同伴" /></label>
            <label class="span-2"><span class="field-label">建议内容</span><textarea v-model="suggestionForm.content" class="field w-full" rows="4"></textarea></label>
          </div>
          <button class="btn btn-amber mt-4" @click="addSuggestion"><span class="btn-glyph">＋</span> 归档建议</button>
        </div>
        <div class="glass p-6">
          <div class="mb-3 flex items-center gap-2">
            <h2 class="section-title">建议列表</h2>
            <select v-model="filteredId" class="field ml-auto max-w-[170px]">
              <option value="">全部论文</option>
              <option v-for="p in papers" :key="p.id" :value="p.id">{{ p.title }}</option>
            </select>
          </div>
          <div v-for="s in visibleSuggestions" :key="s.id" class="glass-flat mb-2 p-3">
            <p class="text-[12px] ink">{{ s.content }}</p>
            <div class="mt-1 flex items-center gap-2">
              <span class="text-[11px] faint">{{ s.author || '匿名' }} · 论文 #{{ s.paperId }} · {{ s.createdAt }}</span>
              <button class="btn btn-slate ml-auto" @click="removeSuggestion(s)">删除</button>
            </div>
          </div>
          <p v-if="!visibleSuggestions.length" class="text-[12px] muted">暂无建议</p>
        </div>
      </div>
    </template>
  </section>
</template>
