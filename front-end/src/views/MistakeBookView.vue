<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const mistakes = ref([])
const questions = ref([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const filters = ref({ q: '', knowledgePoint: '', mastered: '' })

const questionIndex = computed(() => {
  const m = new Map()
  questions.value.forEach(q => m.set(q.id, q))
  return m
})
const points = computed(() => [...new Set(mistakes.value.map(m => m.knowledgePoint).filter(Boolean))])
const stats = computed(() => ({
  total: mistakes.value.length,
  pending: mistakes.value.filter(m => !m.mastered).length,
  mastered: mistakes.value.filter(m => m.mastered).length,
  reviews: mistakes.value.reduce((s, m) => s + (Number(m.reviewCount) || 0), 0),
}))
const filtered = computed(() => {
  const kw = filters.value.q.trim().toLowerCase()
  return mistakes.value.filter(m => {
    if (kw && !Object.values(m).some(v => v != null && String(v).toLowerCase().includes(kw))) return false
    if (filters.value.knowledgePoint && m.knowledgePoint !== filters.value.knowledgePoint) return false
    if (filters.value.mastered === 'yes' && !m.mastered) return false
    if (filters.value.mastered === 'no' && m.mastered) return false
    return true
  })
})

async function load() {
  loading.value = true; error.value = ''
  try {
    const [m, q] = await Promise.all([R.learning.mistakes.list(), R.learning.questions.list()])
    mistakes.value = m || []
    questions.value = q || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function qOf(m) { return questionIndex.value.get(m.questionId) || {} }
function options(m) {
  try { const v = JSON.parse(qOf(m).optionsJson || '[]'); return Array.isArray(v) ? v : [] } catch { return [] }
}

async function review(m) {
  try { await feature.reviewMistake(m.id); notice.value = `「${qOf(m).title || m.questionId}」已记录一次复习`; await load() }
  catch (e) { error.value = e.message }
}
async function toggle(m) {
  try {
    await feature.setMistakeMastered(m.id, !m.mastered)
    notice.value = m.mastered ? '已移回待复习' : '已标记为掌握，不再进入错题重练'
    await load()
  } catch (e) { error.value = e.message }
}
function drill(m) {
  router.push({ path: '/learning/practice', query: { auto: '1', source: 'mistakes', knowledgePoint: m.knowledgePoint || '', size: '10' } })
}
function drillAll() {
  router.push({ path: '/learning/practice', query: { auto: '1', source: 'mistakes', size: '20' } })
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / MISTAKES" title="错题本"
      description="答错自动入本、按题目去重并累加复习次数。可逐题复习、标记掌握，或按知识点一键重练。"
      :actions="[
        { label: '错题重练', icon: '▶', color: 'mint', click: drillAll },
        { label: '练习中心', icon: '▤', color: 'blue', to: '/learning/practice' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '错题总数', value: stats.total }, { label: '待复习', value: stats.pending },
        { label: '已掌握', value: stats.mastered }, { label: '累计复习次数', value: stats.reviews },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <div class="glass mt-5 flex flex-wrap items-end gap-3 p-5">
      <label class="min-w-[180px] flex-1"><span class="field-label">搜索</span>
        <input v-model="filters.q" class="field w-full" type="search" placeholder="知识点 / 原因…" /></label>
      <label><span class="field-label">知识点</span><select v-model="filters.knowledgePoint" class="field w-[150px]">
        <option value="">全部</option><option v-for="p in points" :key="p">{{ p }}</option></select></label>
      <label><span class="field-label">掌握状态</span><select v-model="filters.mastered" class="field w-[140px]">
        <option value="">全部</option><option value="no">待复习</option><option value="yes">已掌握</option></select></label>
      <button class="btn btn-slate" type="button" @click="filters = { q: '', knowledgePoint: '', mastered: '' }">
        <span class="btn-glyph">↺</span> 重置</button>
      <span class="text-[11px] faint">{{ filtered.length }} / {{ mistakes.length }} 条</span>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:100px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="没有符合条件的错题" hint="去练习中心做几道题，答错的会自动进来" />

    <div v-else class="crud-grid mt-5">
      <article v-for="m in filtered" :key="m.id" class="crud-card lift" v-reveal="1">
        <div class="flex items-start justify-between gap-2">
          <h3 class="text-[14px] font-semibold ink">{{ qOf(m).title || ('题目 #' + m.questionId) }}</h3>
          <span class="pill" :class="m.mastered ? 'pill-mint' : 'pill-rose'">{{ m.mastered ? '已掌握' : '待复习' }}</span>
        </div>
        <p class="mt-2 text-[12px] leading-6 muted">{{ qOf(m).content || '题目已被删除' }}</p>
        <div v-if="options(m).length" class="mt-2 flex flex-wrap gap-1.5">
          <span v-for="o in options(m)" :key="o" class="pill pill-neutral">{{ o }}</span>
        </div>
        <div class="mt-3 rounded-lg p-2" style="background: var(--surface-2)">
          <p class="text-[11px] muted">正确答案：{{ qOf(m).answer || '—' }}</p>
          <p class="mt-1 text-[11px] muted">解析：{{ qOf(m).explanation || '暂无解析' }}</p>
        </div>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span v-if="m.knowledgePoint" class="pill pill-violet">{{ m.knowledgePoint }}</span>
          <span class="pill pill-neutral">复习 {{ m.reviewCount || 0 }} 次</span>
          <span v-if="m.lastReviewAt" class="pill pill-neutral">{{ String(m.lastReviewAt).slice(0, 10) }}</span>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-blue" type="button" @click="review(m)"><span class="btn-glyph">↻</span> 复习一次</button>
          <button class="btn btn-violet" type="button" @click="toggle(m)">
            <span class="btn-glyph">{{ m.mastered ? '↩' : '✓' }}</span> {{ m.mastered ? '移回待复习' : '标记掌握' }}
          </button>
          <button class="btn btn-mint" type="button" @click="drill(m)"><span class="btn-glyph">▶</span> 重练该知识点</button>
        </div>
      </article>
    </div>
  </section>
</template>
