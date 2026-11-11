<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'
import Chart from '@/components/Chart.vue'

const stats = ref({})
const trend = ref([])
const weak = ref([])
const radar = ref({})
const error = ref('')

const metrics = computed(() => [
  { label: '题库题目', value: stats.value.questions || 0, color: 'sky' },
  { label: '答题记录', value: stats.value.answers || 0, color: 'mint' },
  { label: '错题', value: stats.value.mistakes || 0, color: 'rose' },
  { label: '成绩记录', value: stats.value.scores || 0, color: 'violet' },
])
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: trend.value.map(t => t.exam) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, data: trend.value.map(t => t.average), areaStyle: { opacity: .12 }, itemStyle: { color: '#4a6cf7' } }]
}))
const weakOption = computed(() => ({
  tooltip: {},
  xAxis: { type: 'category', data: weak.value.map(w => w.subject) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: weak.value.map(w => w.average), itemStyle: { color: '#d9464e', borderRadius: [6, 6, 0, 0] } }]
}))

onMounted(async () => {
  try {
    const [s, t, w, r] = await Promise.all([
      R.learning.questions.stats(), feature.scoreTrend(), feature.weakSubjects(), feature.radar()
    ])
    stats.value = s || {}; trend.value = t || []; weak.value = w || []; radar.value = r || {}
  } catch (e) { error.value = e.message }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / OVERVIEW" title="学习总览"
      description="题库、练习、错题、成绩与知识图谱共享同一份数据，结果会回写到时间线。"
      :actions="[
        { label: '开始练习', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '题库管理', icon: '▤', color: 'blue', to: '/learning/questions' },
        { label: '知识图谱', icon: '⌘', color: 'violet', to: '/learning/graph' },
      ]" />
    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>
    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>
    <div class="mt-5 grid gap-5 md:grid-cols-2">
      <div class="glass p-5"><h2 class="section-title mb-2">考试均分趋势</h2><Chart :option="trendOption" :height="280" /></div>
      <div class="glass p-5"><h2 class="section-title mb-2">学科平均分（由低到高）</h2><Chart :option="weakOption" :height="280" /></div>
    </div>
    <div class="glass mt-5 p-5">
      <h2 class="section-title mb-3">能力雷达</h2>
      <div class="flex flex-wrap gap-2">
        <span v-for="(v, k) in radar" :key="k" class="pill pill-sky">{{ k }} · {{ Math.round(v) }}</span>
        <span v-if="!Object.keys(radar).length" class="text-[12px] muted">暂无成绩数据</span>
      </div>
    </div>
  </section>
</template>
