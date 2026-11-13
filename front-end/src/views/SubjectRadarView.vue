<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const radar = ref({})
const weak = ref([])
const combos = ref([])
const error = ref('')

const option = computed(() => ({
  tooltip: {},
  radar: { indicator: Object.keys(radar.value).map(k => ({ name: k, max: 100 })), radius: '66%' },
  series: [{ type: 'radar', data: [{ value: Object.values(radar.value), name: '学科能力', itemStyle: { color: '#7c4de0' }, areaStyle: { opacity: .22 } }] }],
}))
const coverageOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: combos.value.map(c => c.firstSubject || c.label || '组合'), axisLabel: { fontSize: 10, rotate: 18 } },
  yAxis: { type: 'value', name: '专业覆盖率', max: 100 },
  series: [{ type: 'bar', barWidth: '42%', data: combos.value.map(c => c.coverage),
    itemStyle: { color: '#12996a', borderRadius: [6, 6, 0, 0] } }],
}))
const ranked = computed(() => Object.entries(radar.value).map(([subject, average]) => ({ subject, average }))
  .sort((a, b) => b.average - a.average))
const strongest = computed(() => ranked.value[0] || null)
const weakest = computed(() => ranked.value.length ? ranked.value[ranked.value.length - 1] : null)
const suggestion = computed(() => {
  if (!strongest.value) return '还没有成绩数据，先到「成绩分析」录入几次考试。'
  return `当前最强学科是 ${strongest.value.subject}（${Math.round(strongest.value.average)} 分），` +
    (weakest.value ? `最需要补的是 ${weakest.value.subject}（${Math.round(weakest.value.average)} 分）。` : '') +
    '建议首选科目优先选能力强且专业覆盖率高的方向，再选科目用来补齐覆盖率。'
})

async function load() {
  error.value = ''
  try {
    const [r, w, c] = await Promise.all([feature.radar(), feature.weakSubjects(), feature.hotCombinations()])
    radar.value = r || {}; weak.value = w || []; combos.value = c || []
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / RADAR" title="学科雷达"
      description="用真实成绩计算学科能力分布，再对照组合的专业覆盖率，给出选科方向建议。"
      :actions="[
        { label: '成绩分析', icon: '↗', color: 'sky', to: '/learning/scores' },
        { label: '学科与组合', icon: '⌘', color: 'teal', to: '/planning/subjects' },
        { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' },
      ]" />

    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>

    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-4"><h2 class="section-title mb-2">学科能力分布</h2><Chart :option="option" :height="360" /></div>
      <div class="glass p-4"><h2 class="section-title mb-2">组合专业覆盖率</h2><Chart :option="coverageOption" :height="360" /></div>
    </div>

    <div class="mt-5 grid gap-5 md:grid-cols-[1fr_1.4fr]">
      <div class="glass p-6">
        <p class="eyebrow">SUGGESTION</p>
        <h2 class="section-title mt-1 mb-3">选科方向建议</h2>
        <p class="text-[12px] leading-7 muted">{{ suggestion }}</p>
        <div class="mt-5 flex flex-wrap gap-2">
          <span v-if="strongest" class="pill pill-mint">优势 {{ strongest.subject }} · {{ Math.round(strongest.average) }}</span>
          <span v-if="weakest" class="pill pill-rose">待补 {{ weakest.subject }} · {{ Math.round(weakest.average) }}</span>
        </div>
        <button class="btn btn-violet mt-6" type="button" @click="$router.push('/planning/selection')">
          <span class="btn-glyph">◎</span> 去选科中心
        </button>
      </div>

      <div class="glass p-6">
        <h2 class="section-title mb-3">学科明细（按均分降序）</h2>
        <div v-for="r in ranked" :key="r.subject" class="mb-3">
          <div class="mb-1 flex justify-between text-[12px]"><span class="ink">{{ r.subject }}</span><span class="faint">{{ Math.round(r.average) }} 分</span></div>
          <div class="progress-track"><div class="progress-fill" :style="{ width: Math.min(100, r.average) + '%' }"></div></div>
        </div>
        <p v-if="!ranked.length" class="text-[12px] muted">暂无成绩数据</p>
        <div v-if="weak.length" class="mt-5 flex flex-wrap gap-2">
          <span v-for="w in weak.slice(0, 5)" :key="w.subject" class="pill pill-rose">{{ w.subject }} · {{ w.average }}</span>
        </div>
      </div>
    </div>
  </section>
</template>
