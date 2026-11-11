<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const radar = ref({})
const option = computed(() => ({
  tooltip: {},
  radar: { indicator: Object.keys(radar.value).map(k => ({ name: k, max: 100 })), radius: '66%' },
  series: [{ type: 'radar', data: [{ value: Object.values(radar.value), name: '学科能力' }], itemStyle: { color: '#7c4de0' }, areaStyle: { opacity: .22 } }]
}))
onMounted(async () => { try { radar.value = (await feature.radar()) || {} } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / RADAR" title="学科雷达"
      description="基于成绩记录计算的学科能力分布，用于选科与目标设定的参考。"
      :actions="[{ label: '成绩分析', icon: '↗', color: 'sky', to: '/learning/scores' }, { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' }]" />
    <div class="glass p-4"><Chart :option="option" :height="420" /></div>
    <div class="glass mt-5 p-5">
      <div class="flex flex-wrap gap-2">
        <span v-for="(v, k) in radar" :key="k" class="pill pill-violet">{{ k }} · {{ Math.round(v) }}</span>
        <span v-if="!Object.keys(radar).length" class="text-[12px] muted">暂无成绩数据，先到「题库 / 成绩分析」录入</span>
      </div>
    </div>
  </section>
</template>
