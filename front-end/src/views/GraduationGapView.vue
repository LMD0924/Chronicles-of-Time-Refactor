<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const gaps = ref([])
const option = computed(() => ({
  tooltip: {}, xAxis: { type: 'category', data: gaps.value.map(g => g.requirementName) }, yAxis: { type: 'value' },
  series: [
    { name: '已修', type: 'bar', stack: 'credit', data: gaps.value.map(g => g.earnedCredit), itemStyle: { color: '#12996a' } },
    { name: '缺口', type: 'bar', stack: 'credit', data: gaps.value.map(g => g.remaining || 0), itemStyle: { color: '#d9464e' } }
  ]
}))
onMounted(async () => { try { gaps.value = (await feature.gapAnalysis()) || [] } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / GAP" title="毕业缺口"
      description="按毕业要求核对已修学分，未达标项给出剩余学分。"
      :actions="[{ label: 'GPA 模拟', icon: '◷', color: 'mint', to: '/planning/gpa' }, { label: '学期课表', icon: '▤', color: 'sky', to: '/planning/schedule' }]" />
    <div class="glass p-5"><Chart :option="option" :height="300" /></div>
    <div class="mt-5 grid gap-3 md:grid-cols-2">
      <div v-for="g in gaps" :key="g.id" class="glass p-5">
        <div class="flex items-center justify-between"><h3 class="text-[14px] font-medium ink">{{ g.requirementName }}</h3><span class="pill pill-rose">还差 {{ g.remaining || 0 }}</span></div>
        <div class="progress-track mt-3"><div class="progress-fill" :style="{ width: Math.min(100, (g.earnedCredit / (g.requiredCredit || 1)) * 100) + '%' }"></div></div>
        <p class="mt-2 text-[11px] faint">{{ g.earnedCredit }} / {{ g.requiredCredit }} 学分 · {{ g.note }}</p>
      </div>
      <p v-if="!gaps.length" class="text-[12px] muted">暂无毕业要求数据</p>
    </div>
  </section>
</template>
