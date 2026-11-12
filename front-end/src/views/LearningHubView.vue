<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const stats = ref({})
const sessions = ref([])
const path = ref([])
const loading = ref(true)
const error = ref('')

const metrics = computed(() => [
  { label: '题库题目', value: stats.value.questions || 0, color: 'sky' },
  { label: '累计答题', value: stats.value.answers || 0, color: 'mint' },
  { label: '正确率', value: (stats.value.accuracy || 0) + '%', color: 'violet' },
  { label: '待复习错题', value: stats.value.pendingMistakes || 0, color: 'rose' },
])
const recent = computed(() => sessions.value.filter(s => s.finished).slice(0, 8).reverse())
const sessionOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: recent.value.map(s => '#' + s.id) },
  yAxis: { type: 'value', max: 100, name: '正确率%' },
  series: [{ type: 'line', smooth: true, data: recent.value.map(s => s.accuracy || 0), itemStyle: { color: '#3fb99a' }, areaStyle: { opacity: 0.12 } }],
}))
const weakPoints = computed(() => path.value.filter(p => p.suggested).slice(0, 4))

async function load() {
  loading.value = true; error.value = ''
  try {
    const [s, ss, p] = await Promise.all([feature.learningStats(), feature.practiceSessions(), feature.learningPath()])
    stats.value = s || {}; sessions.value = ss || []; path.value = p || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / HUB" title="学习总览"
      description="题库、练习、答题、错题与成绩的一条入口：看指标、看趋势、直接跳到下一步该做的事。"
      :actions="[
        { label: '开始练习', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '题库管理', icon: '▤', color: 'blue', to: '/learning/questions' },
        { label: '错题本', icon: '✎', color: 'rose', to: '/learning/mistakes' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>

    <div class="mt-5 grid gap-5 md:grid-cols-2">
      <div class="glass p-5" v-reveal="1">
        <h2 class="text-[14px] font-semibold ink">最近场次正确率</h2>
        <p v-if="!recent.length" class="mt-3 text-[12px] muted">还没有已交卷的场次，先去练习一轮。</p>
        <Chart v-else :option="sessionOption" :height="240" />
      </div>
      <div class="glass p-5" v-reveal="1">
        <h2 class="text-[14px] font-semibold ink">建议优先补的知识点</h2>
        <EmptyState v-if="!weakPoints.length" class="mt-3" text="暂无薄弱知识点" hint="去做几轮练习后这里会出现建议" />
        <div v-else class="mt-3 space-y-2">
          <div v-for="p in weakPoints" :key="p.knowledgePoint" class="flex flex-wrap items-center gap-2">
            <span class="pill pill-rose">{{ p.mastery }}%</span>
            <span class="text-[13px] ink">{{ p.knowledgePoint }}</span>
            <RouterLink :to="{ path: '/learning/practice', query: { auto: '1', knowledgePoint: p.knowledgePoint, size: '10' } }"
              class="btn btn-mint ml-auto"><span class="btn-glyph">▶</span> 去练</RouterLink>
          </div>
        </div>
        <RouterLink to="/learning/path" class="btn btn-violet mt-4"><span class="btn-glyph">⌁</span> 查看完整学习路径</RouterLink>
      </div>
    </div>

    <div class="glass mt-5 p-5" v-reveal="2">
      <h2 class="mb-3 text-[14px] font-semibold ink">最近练习场次</h2>
      <div v-if="loading" class="space-y-2"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:44px"></div></div>
      <EmptyState v-else-if="!sessions.length" text="还没有练习场次" hint="去练习中心组卷开始" />
      <div v-else>
        <div v-for="s in sessions.slice(0, 6)" :key="s.id" class="flex flex-wrap items-center gap-2 border-b border-hairline py-2 text-[12px] last:border-0">
          <span class="pill" :class="s.finished ? 'pill-mint' : 'pill-amber'">{{ s.finished ? '已交卷' : '进行中' }}</span>
          <span class="ink">{{ s.title }}</span>
          <span class="pill pill-sky">{{ s.questionCount || 0 }} 题</span>
          <span class="pill pill-violet">{{ s.accuracy || 0 }}%</span>
          <span class="ml-auto faint">{{ s.startedAt }}</span>
          <RouterLink to="/learning/sessions" class="btn btn-blue"><span class="btn-glyph">→</span> 回看</RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>
