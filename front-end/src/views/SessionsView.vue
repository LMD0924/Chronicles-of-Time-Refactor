<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const sessions = ref([])
const detail = ref(null)
const expanded = ref(null)
const loading = ref(true)
const loadingDetail = ref(false)
const error = ref('')

const stats = computed(() => {
  const finished = sessions.value.filter(s => s.finished)
  const total = finished.reduce((s, x) => s + (Number(x.questionCount) || 0), 0)
  const correct = finished.reduce((s, x) => s + (Number(x.correctCount) || 0), 0)
  return {
    total: sessions.value.length,
    finished: finished.length,
    accuracy: total ? Math.round(correct * 1000 / total) / 10 : 0,
    questions: total,
  }
})

async function load() {
  loading.value = true; error.value = ''
  try { sessions.value = (await feature.practiceSessions()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function toggle(s) {
  if (expanded.value === s.id) { expanded.value = null; detail.value = null; return }
  expanded.value = s.id
  loadingDetail.value = true; error.value = ''
  try { detail.value = await feature.practiceSessionDetail(s.id) }
  catch (e) { error.value = e.message }
  finally { loadingDetail.value = false }
}
function options(item) {
  try { const v = JSON.parse(item.optionsJson || '[]'); return Array.isArray(v) ? v : [] } catch { return [] }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / SESSIONS" title="练习场次"
      description="每一轮组卷都是一个独立场次，记录题数、正确数与交卷时间，可展开逐题回看。"
      :actions="[
        { label: '开始练习', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '答题记录', icon: '▤', color: 'blue', to: '/learning/answers' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '场次总数', value: stats.total }, { label: '已交卷', value: stats.finished },
        { label: '累计题目', value: stats.questions }, { label: '整体正确率', value: stats.accuracy + '%' },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:90px"></div></div>
    <EmptyState v-else-if="!sessions.length" class="mt-5" text="还没有练习场次" hint="去练习中心组卷并交卷，场次会出现在这里" />

    <div v-else class="mt-5 space-y-3">
      <article v-for="s in sessions" :key="s.id" class="glass lift p-5" v-reveal="1">
        <div class="flex flex-wrap items-center gap-2">
          <span class="pill" :class="s.finished ? 'pill-mint' : 'pill-amber'">{{ s.finished ? '已交卷' : '进行中' }}</span>
          <h3 class="text-[14px] font-semibold ink">{{ s.title }}</h3>
          <span class="ml-auto text-[11px] faint">#{{ s.id }} · {{ s.startedAt }}</span>
        </div>
        <div class="mt-3 flex flex-wrap gap-2">
          <span class="pill pill-sky">题目 {{ s.questionCount || 0 }}</span>
          <span class="pill pill-mint">答对 {{ s.correctCount || 0 }}</span>
          <span class="pill pill-violet">正确率 {{ s.accuracy || 0 }}%</span>
          <span v-if="s.finishedAt" class="pill pill-neutral">交卷 {{ s.finishedAt }}</span>
        </div>
        <div class="mt-3 progress-track"><div class="progress-fill" :style="{ width: (s.accuracy || 0) + '%' }"></div></div>
        <button class="btn btn-violet mt-4" type="button" :disabled="loadingDetail && expanded === s.id" @click="toggle(s)">
          <span class="btn-glyph">{{ expanded === s.id ? '⌃' : '⌄' }}</span>
          {{ expanded === s.id ? '收起逐题' : '逐题回看' }}
        </button>

        <div v-if="expanded === s.id" class="mt-4">
          <div v-if="loadingDetail" class="placeholder-bar" style="height:60px"></div>
          <p v-else-if="!detail?.items?.length" class="text-[12px] muted">这个场次还没有答题记录（可能尚未交卷）。</p>
          <div v-else>
            <div v-for="(item, i) in detail.items" :key="item.id || i" class="border-b border-hairline py-3 last:border-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="pill" :class="item.correct ? 'pill-mint' : 'pill-rose'">{{ item.correct ? '正确' : '错误' }}</span>
                <span class="text-[13px] font-medium ink">{{ i + 1 }}. {{ item.title || ('题目 #' + item.questionId) }}</span>
                <span v-if="item.knowledgePoint" class="pill pill-neutral">{{ item.knowledgePoint }}</span>
              </div>
              <p class="mt-1 text-[12px] muted">{{ item.content || '题干已不可用' }}</p>
              <div v-if="options(item).length" class="mt-2 flex flex-wrap gap-1.5">
                <span v-for="o in options(item)" :key="o" class="pill pill-neutral">{{ o }}</span>
              </div>
              <p class="mt-2 text-[12px] muted">你的答案：{{ item.userAnswer || '未作答' }} · 正确答案：{{ item.answer }}</p>
              <p class="mt-1 text-[12px] muted">解析：{{ item.explanation || '暂无解析' }} · 用时 {{ Math.round((Number(item.durationMs) || 0) / 1000) }}s</p>
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
