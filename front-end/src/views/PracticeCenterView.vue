<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const phase = ref('setup') // setup | doing | result
const facets = ref({ subjects: [], knowledgePoints: [], difficulties: [] })
const cfg = ref({ subject: '', knowledgePoint: '', difficulty: '', size: 10, source: 'bank', title: '' })
const questions = ref([])
const answers = ref([])
const sessionId = ref(null)
const idx = ref(0)
const enterAt = ref(0)
const result = ref(null)
const loading = ref(false)
const error = ref('')

const current = computed(() => questions.value[idx.value])
const answeredCount = computed(() => answers.value.filter(a => a.userAnswer).length)
const progress = computed(() => questions.value.length ? Math.round(idx.value / questions.value.length * 100) : 0)

function options(q) {
  try { const v = JSON.parse(q.optionsJson || '[]'); return Array.isArray(v) ? v : [] } catch { return [] }
}
function letterOf(s) { return String(s || '').trim().charAt(0).toUpperCase() }

function tick() {
  if (!answers.value[idx.value]) return
  answers.value[idx.value].durationMs += Math.max(0, Date.now() - enterAt.value)
  enterAt.value = Date.now()
}
function go(n) {
  if (n === idx.value) return
  tick()
  idx.value = n
  enterAt.value = Date.now()
}
function pick(opt) {
  answers.value[idx.value].userAnswer = letterOf(opt)
}

async function loadFacets() {
  try { facets.value = (await feature.learningFacets()) || { subjects: [], knowledgePoints: [], difficulties: [] } }
  catch { /* 字典失败不阻塞 */ }
}

async function start() {
  loading.value = true; error.value = ''
  try {
    const data = await feature.practiceStart({ ...cfg.value })
    questions.value = data.questions || []
    sessionId.value = data.sessionId
    answers.value = questions.value.map(() => ({ userAnswer: '', durationMs: 0 }))
    idx.value = 0; phase.value = 'doing'; result.value = null
    enterAt.value = Date.now()
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function submitAll() {
  const blank = questions.value.length - answeredCount.value
  if (blank > 0 && !window.confirm(`还有 ${blank} 题未作答，未作答会判错并进入错题本。确定交卷吗？`)) return
  tick()
  loading.value = true; error.value = ''
  try {
    const payload = questions.value.map((q, i) => ({
      questionId: q.id,
      userAnswer: answers.value[i].userAnswer,
      durationMs: answers.value[i].durationMs,
    }))
    result.value = await feature.practiceSubmit(sessionId.value, payload)
    phase.value = 'result'
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function reset() { phase.value = 'setup'; result.value = null; questions.value = []; answers.value = []; idx.value = 0 }

onMounted(async () => {
  await loadFacets()
  if (route.query.knowledgePoint) cfg.value.knowledgePoint = String(route.query.knowledgePoint)
  if (route.query.subject) cfg.value.subject = String(route.query.subject)
  if (route.query.source) cfg.value.source = String(route.query.source)
  if (route.query.size) cfg.value.size = Number(route.query.size) || 10
  if (route.query.auto === '1' || route.query.knowledgePoint) await start()
})
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / PRACTICE" title="练习中心"
      description="按学科、知识点、难度组卷，逐题计时作答，交卷后场次、答题记录与错题本一次同步。"
      :actions="[
        { label: '题库管理', icon: '▤', color: 'blue', to: '/learning/questions' },
        { label: '练习场次', icon: '◷', color: 'violet', to: '/learning/sessions' },
        { label: '错题本', icon: '✎', color: 'rose', to: '/learning/mistakes' },
        { label: '重新配置', icon: '↻', color: 'slate', click: reset },
      ]" />

    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>

    <!-- 组卷配置 -->
    <div v-if="phase === 'setup'" class="glass p-6" v-reveal="0">
      <h2 class="text-[15px] font-semibold ink">组卷配置</h2>
      <p class="mt-1 text-[11px] faint">题目从当前账号的题库抽取，返回的题目不含答案与解析。</p>
      <div class="form-grid mt-5">
        <label><span class="field-label">题目来源</span><select v-model="cfg.source" class="field w-full">
          <option value="bank">全部题库</option><option value="mistakes">仅未掌握错题</option></select></label>
        <label><span class="field-label">题数 (1-50)</span><input v-model.number="cfg.size" class="field w-full" type="number" min="1" max="50" /></label>
        <label><span class="field-label">学科</span><select v-model="cfg.subject" class="field w-full">
          <option value="">不限</option><option v-for="s in facets.subjects" :key="s">{{ s }}</option></select></label>
        <label><span class="field-label">知识点</span><select v-model="cfg.knowledgePoint" class="field w-full">
          <option value="">不限</option><option v-for="k in facets.knowledgePoints" :key="k">{{ k }}</option></select></label>
        <label><span class="field-label">难度</span><select v-model="cfg.difficulty" class="field w-full">
          <option value="">不限</option><option v-for="d in facets.difficulties" :key="d">{{ d }}</option></select></label>
        <label><span class="field-label">场次标题（可选）</span><input v-model="cfg.title" class="field w-full" placeholder="默认按时间命名" /></label>
      </div>
      <button class="btn btn-mint mt-5" type="button" :disabled="loading" @click="start">
        <span class="btn-glyph">▶</span> {{ loading ? '组卷中…' : '开始练习' }}
      </button>
    </div>

    <!-- 逐题作答 -->
    <div v-else-if="phase === 'doing'" class="glass p-6" v-reveal="0">
      <div class="flex flex-wrap items-center gap-3">
        <div class="progress-track flex-1"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>
        <span class="text-[11px] faint">第 {{ idx + 1 }} / {{ questions.length }} 题 · 已答 {{ answeredCount }}</span>
      </div>
      <div class="mt-5 flex flex-wrap items-center gap-2">
        <span class="pill pill-sky">{{ current.subject }}</span>
        <span class="pill pill-neutral">{{ current.knowledgePoint }}</span>
        <span v-if="current.difficulty" class="pill pill-violet">{{ current.difficulty }}</span>
        <span class="ml-auto text-[11px] faint">场次 #{{ sessionId }}</span>
      </div>
      <h2 class="mt-4 text-[16px] font-semibold ink">{{ current.title }}</h2>
      <p class="mt-2 text-[13px] leading-7 muted">{{ current.content }}</p>

      <div class="mt-4 grid gap-2 sm:grid-cols-2">
        <button v-for="o in options(current)" :key="o" type="button" class="chip text-left"
          :class="{ 'is-active': answers[idx].userAnswer === letterOf(o) }" @click="pick(o)">{{ o }}</button>
      </div>
      <label class="mt-4 block"><span class="field-label">或直接填写答案（A/B/C/D）</span>
        <input v-model="answers[idx].userAnswer" class="field w-full" maxlength="4" placeholder="例如 A" /></label>

      <div class="mt-5 flex flex-wrap gap-2">
        <button class="btn btn-slate" type="button" :disabled="idx === 0" @click="go(idx - 1)"><span class="btn-glyph">←</span> 上一题</button>
        <button v-if="idx < questions.length - 1" class="btn btn-blue" type="button" @click="go(idx + 1)"><span class="btn-glyph">→</span> 下一题</button>
        <button class="btn btn-mint ml-auto" type="button" :disabled="loading" @click="submitAll">
          <span class="btn-glyph">✓</span> {{ loading ? '判分中…' : '交卷' }}
        </button>
      </div>
      <div class="mt-4 flex flex-wrap gap-1.5">
        <button v-for="(q, i) in questions" :key="q.id" type="button" class="chip"
          :class="{ 'is-active': i === idx }" @click="go(i)">
          {{ i + 1 }}{{ answers[i].userAnswer ? ' ✓' : '' }}
        </button>
      </div>
    </div>

    <!-- 结果 -->
    <div v-else-if="phase === 'result'" v-reveal="0">
      <div class="glass grid grid-cols-2 gap-4 p-6 md:grid-cols-4">
        <div v-for="m in [
          { label: '总题数', value: result.total }, { label: '答对', value: result.correctCount },
          { label: '正确率', value: result.accuracy + '%' }, { label: '场次编号', value: '#' + result.sessionId },
        ]" :key="m.label" class="text-center">
          <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
        </div>
      </div>
      <div class="glass mt-5 p-6">
        <h2 class="mb-3 text-[15px] font-semibold ink">逐题解析</h2>
        <div v-for="(r, i) in result.results" :key="r.questionId" class="border-b border-hairline py-3 last:border-0">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill" :class="r.correct ? 'pill-mint' : 'pill-rose'">{{ r.correct ? '正确' : '错误' }}</span>
            <span class="text-[13px] font-medium ink">{{ i + 1 }}. {{ r.title }}</span>
            <span v-if="r.knowledgePoint" class="pill pill-neutral">{{ r.knowledgePoint }}</span>
          </div>
          <p class="mt-2 text-[12px] muted">你的答案：{{ r.userAnswer || '未作答' }} · 正确答案：{{ r.answer }}</p>
          <p class="mt-1 text-[12px] muted">解析：{{ r.explanation || '暂无解析' }}</p>
        </div>
      </div>
      <div class="mt-5 flex flex-wrap gap-2">
        <button class="btn btn-mint" type="button" @click="reset"><span class="btn-glyph">↻</span> 再来一轮</button>
        <button class="btn btn-violet" type="button" @click="router.push('/learning/sessions')"><span class="btn-glyph">◷</span> 查看场次</button>
        <button class="btn btn-rose" type="button" @click="router.push('/learning/mistakes')"><span class="btn-glyph">✎</span> 去错题本</button>
      </div>
    </div>

    <EmptyState v-if="phase === 'setup' && !facets.knowledgePoints.length" class="mt-5"
      text="题库里还没有题目" hint="先到「题库管理」添加或批量导入几道题，再回来组卷" />
  </section>
</template>
