<script setup>
import { computed, onMounted, ref } from 'vue'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const facets = ref({ subjects: [], knowledgePoints: [], difficulties: [], sources: [] })
const loading = ref(true)
const saving = ref(false)
const importing = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const showImport = ref(false)
const editing = ref(null)
const form = ref({})
const importText = ref('')
const importResult = ref(null)
const filters = ref({ q: '', subject: '', knowledgePoint: '', difficulty: '' })

const difficulties = ['简单', '中等', '较难', '困难']
const sample = `[
  {
    "subject": "数学",
    "knowledgePoint": "函数",
    "title": "函数的单调性",
    "content": "下列说法正确的是？",
    "optionsJson": ["A. 增函数", "B. 减函数", "C. 常数", "D. 不确定"],
    "answer": "A",
    "explanation": "由导数符号判断",
    "difficulty": "中等",
    "source": "导入"
  }
]`

const filtered = computed(() => {
  const kw = filters.value.q.trim().toLowerCase()
  return rows.value.filter(r => {
    if (kw && !Object.values(r).some(v => v != null && String(v).toLowerCase().includes(kw))) return false
    if (filters.value.subject && r.subject !== filters.value.subject) return false
    if (filters.value.knowledgePoint && r.knowledgePoint !== filters.value.knowledgePoint) return false
    if (filters.value.difficulty && r.difficulty !== filters.value.difficulty) return false
    return true
  })
})

function options(q) {
  try { const v = JSON.parse(q.optionsJson || '[]'); return Array.isArray(v) ? v : [] } catch { return [] }
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [list, f] = await Promise.all([R.learning.questions.list(), feature.learningFacets()])
    rows.value = list || []
    facets.value = f || { subjects: [], knowledgePoints: [], difficulties: [], sources: [] }
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  return {
    subject: '', knowledgePoint: '', title: '', content: '',
    optionsJson: '["A. ","B. ","C. ","D. "]', answer: 'A', explanation: '',
    difficulty: '中等', source: '自建',
  }
}
function openCreate() { editing.value = null; form.value = defaults(); showForm.value = true; notice.value = '' }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; showForm.value = true; notice.value = '' }

async function submit() {
  if (!form.value.title?.trim() || !form.value.answer?.trim()) { error.value = '标题和答案必填'; return }
  try { JSON.parse(form.value.optionsJson || '[]') } catch { error.value = '选项必须是合法的 JSON 数组'; return }
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    if (editing.value) await R.learning.questions.update(editing.value, payload)
    else await R.learning.questions.save(payload)
    showForm.value = false
    notice.value = '题目已保存'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.title}」吗？`)) return
  try { await R.learning.questions.remove(r.id); notice.value = '题目已删除'; await load() }
  catch (e) { error.value = e.message }
}

async function doImport() {
  let parsed
  try { parsed = JSON.parse(importText.value) } catch (e) { error.value = 'JSON 解析失败：' + e.message; return }
  if (!Array.isArray(parsed)) { error.value = '请粘贴一个 JSON 数组'; return }
  importing.value = true; error.value = ''
  try {
    importResult.value = await feature.importQuestions(parsed)
    notice.value = `导入完成：成功 ${importResult.value.created} 条，失败 ${importResult.value.failed} 条`
    await load()
  } catch (e) { error.value = e.message }
  finally { importing.value = false }
}
function resetFilters() { filters.value = { q: '', subject: '', knowledgePoint: '', difficulty: '' } }

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / BANK" title="题库管理"
      description="题目、选项、答案与解析统一维护；支持粘贴 JSON 数组批量导入，导入后即可用于组卷。"
      :actions="[
        { label: '新增题目', icon: '＋', color: 'blue', click: openCreate },
        { label: '批量导入', icon: '⇥', color: 'violet', click: () => { showImport = true; importResult = null } },
        { label: '去练习', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass flex flex-wrap items-end gap-3 p-5">
      <label class="min-w-[180px] flex-1"><span class="field-label">搜索</span>
        <input v-model="filters.q" class="field w-full" type="search" placeholder="题干 / 知识点 / 解析…" /></label>
      <label><span class="field-label">学科</span><select v-model="filters.subject" class="field w-[130px]">
        <option value="">全部</option><option v-for="s in facets.subjects" :key="s">{{ s }}</option></select></label>
      <label><span class="field-label">知识点</span><select v-model="filters.knowledgePoint" class="field w-[140px]">
        <option value="">全部</option><option v-for="k in facets.knowledgePoints" :key="k">{{ k }}</option></select></label>
      <label><span class="field-label">难度</span><select v-model="filters.difficulty" class="field w-[120px]">
        <option value="">全部</option><option v-for="d in difficulties" :key="d">{{ d }}</option></select></label>
      <button class="btn btn-slate" type="button" @click="resetFilters"><span class="btn-glyph">↺</span> 重置</button>
      <span class="text-[11px] faint">{{ filtered.length }} / {{ rows.length }} 题</span>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:90px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="还没有符合条件的题目" hint="新增一道，或用「批量导入」一次导入多道" />

    <div v-else class="crud-grid mt-5">
      <article v-for="q in filtered" :key="q.id" class="crud-card lift" v-reveal="1">
        <div class="flex items-start justify-between gap-2">
          <h3 class="text-[14px] font-semibold ink">{{ q.title }}</h3>
          <span class="pill pill-sky">{{ q.difficulty || '中等' }}</span>
        </div>
        <p class="mt-2 text-[12px] leading-6 muted">{{ q.content || '暂无题干' }}</p>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span v-if="q.subject" class="pill pill-mint">{{ q.subject }}</span>
          <span v-if="q.knowledgePoint" class="pill pill-violet">{{ q.knowledgePoint }}</span>
          <span v-if="q.source" class="pill pill-neutral">{{ q.source }}</span>
        </div>
        <p class="mt-3 text-[11px] faint">答案 {{ q.answer }} · 选项 {{ options(q).length }} 个</p>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-violet" type="button" @click="openEdit(q)"><span class="btn-glyph">✎</span> 编辑</button>
          <button class="btn btn-rose" type="button" @click="remove(q)"><span class="btn-glyph">×</span> 删除</button>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }} · 题目</h2>
          <p class="mb-5 text-[12px] muted">答案为 A/B/C/D 单字符；选项填 JSON 数组字符串。</p>
          <div class="form-grid">
            <label><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" /></label>
            <label><span class="field-label">学科</span><input v-model="form.subject" class="field w-full" list="cot-subjects" />
              <datalist id="cot-subjects"><option v-for="s in facets.subjects" :key="s" :value="s" /></datalist></label>
            <label><span class="field-label">知识点</span><input v-model="form.knowledgePoint" class="field w-full" list="cot-points" />
              <datalist id="cot-points"><option v-for="k in facets.knowledgePoints" :key="k" :value="k" /></datalist></label>
            <label><span class="field-label">难度</span><select v-model="form.difficulty" class="field w-full"><option v-for="d in difficulties" :key="d">{{ d }}</option></select></label>
            <label class="span-2"><span class="field-label">题干</span><textarea v-model="form.content" class="field w-full" rows="2"></textarea></label>
            <label class="span-2"><span class="field-label">选项（JSON 数组）</span><textarea v-model="form.optionsJson" class="field w-full" rows="2"></textarea></label>
            <label><span class="field-label">正确答案 *</span><input v-model="form.answer" class="field w-full" maxlength="4" placeholder="A" /></label>
            <label><span class="field-label">来源</span><input v-model="form.source" class="field w-full" /></label>
            <label class="span-2"><span class="field-label">解析</span><textarea v-model="form.explanation" class="field w-full" rows="2"></textarea></label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-blue" type="button" :disabled="saving" @click="submit"><span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showImport" class="modal-mask" @click.self="showImport = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">批量导入题目</h2>
          <p class="mb-4 text-[12px] muted">粘贴 JSON 数组，逐条校验后写入；title 与 answer 必填，其余字段可省略。</p>
          <textarea v-model="importText" class="field w-full font-mono" rows="10" :placeholder="sample"></textarea>
          <button class="btn btn-quiet mt-2" type="button" @click="importText = sample">填入示例</button>
          <div v-if="importResult && importResult.errors?.length" class="mt-3 rounded-lg p-3" style="background: var(--surface-2)">
            <p class="text-[12px] font-medium ink">失败明细</p>
            <p v-for="e in importResult.errors" :key="e.index" class="mt-1 text-[11px] muted">第 {{ e.index }} 项：{{ e.reason }}</p>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showImport = false">关闭</button>
            <button class="btn btn-violet" type="button" :disabled="importing" @click="doImport">
              <span class="btn-glyph">⇥</span> {{ importing ? '导入中…' : '校验并导入' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
