<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const scores = ref([]), trend = ref([]), weak = ref([]), radar = ref({})
const cfg = {
  eyebrow: '', title: '成绩明细', description: '', color: 'sky',
  fields: [
    { key: 'subject', label: '学科', required: true }, { key: 'examName', label: '考试名称', required: true },
    { key: 'examDate', label: '考试日期', type: 'date' }, { key: 'score', label: '得分', type: 'number' },
    { key: 'fullScore', label: '满分', type: 'number' }, { key: 'classAvg', label: '班级均分', type: 'number' }
  ],
  columns: [
    { key: 'examDate', label: '日期', type: 'date' }, { key: 'examName', label: '考试' },
    { key: 'subject', label: '学科' }, { key: 'score', label: '得分' }, { key: 'classAvg', label: '班均' }
  ]
}
cfg.api = R.learning.scores

const line = computed(() => ({
  tooltip: { trigger: 'axis' }, legend: { data: ['平均分'] },
  xAxis: { type: 'category', data: trend.value.map(t => t.exam) }, yAxis: { type: 'value' },
  series: [{ name: '平均分', type: 'line', smooth: true, data: trend.value.map(t => t.average), itemStyle: { color: '#4a6cf7' } }]
}))
const radarOption = computed(() => ({
  tooltip: {},
  radar: { indicator: Object.keys(radar.value).map(k => ({ name: k, max: 100 })) },
  series: [{ type: 'radar', data: [{ value: Object.values(radar.value), name: '学科能力' }], itemStyle: { color: '#7c4de0' }, areaStyle: { opacity: .2 } }]
}))
const bars = computed(() => ({
  tooltip: {}, xAxis: { type: 'category', data: weak.value.map(w => w.subject) }, yAxis: { type: 'value' },
  series: [{ type: 'bar', data: weak.value.map(w => w.average), itemStyle: { color: '#2f9d6a', borderRadius: [6, 6, 0, 0] } }]
}))

async function load() {
  try {
    const [s, t, w, r] = await Promise.all([R.learning.scores.list(), feature.scoreTrend(), feature.weakSubjects(), feature.radar()])
    scores.value = s || []; trend.value = t || []; weak.value = w || []; radar.value = r || {}
  } catch { /* 后端未启动时降级 */ }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / SCORES" title="成绩分析"
      description="趋势、学科雷达与薄弱学科都来自同一份成绩记录，新增后自动重算。"
      :actions="[{ label: '刷新', icon: '↻', color: 'slate', click: load }, { label: '知识图谱', icon: '⌘', color: 'violet', to: '/learning/graph' }]" />
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-5"><h2 class="section-title mb-2">考试趋势</h2><Chart :option="line" :height="270" /></div>
      <div class="glass p-5"><h2 class="section-title mb-2">学科雷达</h2><Chart :option="radarOption" :height="270" /></div>
    </div>
    <div class="glass mt-5 p-5"><h2 class="section-title mb-2">薄弱学科（均分升序）</h2><Chart :option="bars" :height="240" /></div>
    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
