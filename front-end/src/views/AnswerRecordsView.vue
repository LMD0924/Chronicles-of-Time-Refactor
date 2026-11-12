<script setup>
import { computed, onMounted, ref } from 'vue'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const questions = ref([])
const sessions = ref([])
const loading = ref(true)
const error = ref('')
const filters = ref({ q: '', sessionId: '', correct: '' })

const questionIndex = computed(() => new Map(questions.value.map(q => [q.id, q])))
const stats = computed(() => {
  const correct = rows.value.filter(r => r.correct).length
  const totalMs = rows.value.reduce((s, r) => s + (Number(r.durationMs) || 0), 0)
  return {
    total: rows.value.length,
    correct,
    accuracy: rows.value.length ? Math.round(correct * 1000 / rows.value.length) / 10 : 0,
    avgSeconds: rows.value.length ? Math.round(totalMs / rows.value.length / 1000) : 0,
  }
})
const filtered = computed(() => {
  const kw = filters.value.q.trim().toLowerCase()
  return rows.value.filter(r => {
    if (kw && !Object.values(r).some(v => v != null && String(v).toLowerCase().includes(kw))) return false
    if (filters.value.sessionId && String(r.sessionId) !== String(filters.value.sessionId)) return false
    if (filters.value.correct === 'yes' && !r.correct) return false
    if (filters.value.correct === 'no' && r.correct) return false
    return true
  })
})

async function load() {
  loading.value = true; error.value = ''
  try {
    const [a, q, s] = await Promise.all([
      R.learning.answers.list(), R.learning.questions.list(), feature.practiceSessions(),
    ])
    rows.value = a || []
    questions.value = q || []
    sessions.value = s || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function titleOf(r) { return questionIndex.value.get(r.questionId)?.title || ('题目 #' + r.questionId) }

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / ANSWERS" title="答题记录"
      description="每一次作答都留痕：所属场次、答案、正误、得分与用时。可按场次回看整轮作答。"
      :actions="[
        { label: '练习中心', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '练习场次', icon: '◷', color: 'violet', to: '/learning/sessions' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '累计答题', value: stats.total }, { label: '答对', value: stats.correct },
        { label: '正确率', value: stats.accuracy + '%' }, { label: '平均用时(秒)', value: stats.avgSeconds },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <div class="glass mt-5 flex flex-wrap items-end gap-3 p-5">
      <label class="min-w-[180px] flex-1"><span class="field-label">搜索</span>
        <input v-model="filters.q" class="field w-full" type="search" placeholder="题目 / 答案…" /></label>
      <label><span class="field-label">所属场次</span><select v-model="filters.sessionId" class="field w-[190px]">
        <option value="">全部</option>
        <option v-for="s in sessions" :key="s.id" :value="s.id">#{{ s.id }} {{ s.title }}</option></select></label>
      <label><span class="field-label">正误</span><select v-model="filters.correct" class="field w-[120px]">
        <option value="">全部</option><option value="yes">正确</option><option value="no">错误</option></select></label>
      <button class="btn btn-slate" type="button" @click="filters = { q: '', sessionId: '', correct: '' }">
        <span class="btn-glyph">↺</span> 重置</button>
      <span class="text-[11px] faint">{{ filtered.length }} / {{ rows.length }} 条</span>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:56px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="还没有答题记录" hint="去练习中心做一轮，记录会自动出现在这里" />

    <div v-else class="glass mt-5 overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr>
          <th>题目</th><th>你的答案</th><th>结果</th><th>得分</th><th>用时</th><th>场次</th><th>作答时间</th>
        </tr></thead>
        <tbody>
          <tr v-for="r in filtered" :key="r.id">
            <td>{{ titleOf(r) }}</td>
            <td>{{ r.userAnswer || '未作答' }}</td>
            <td><span class="pill" :class="r.correct ? 'pill-mint' : 'pill-rose'">{{ r.correct ? '正确' : '错误' }}</span></td>
            <td>{{ r.score ?? 0 }}</td>
            <td>{{ Math.round((Number(r.durationMs) || 0) / 1000) }}s</td>
            <td>{{ r.sessionId ? '#' + r.sessionId : '单题练习' }}</td>
            <td>{{ r.answeredAt || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
