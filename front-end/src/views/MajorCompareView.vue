<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const data = ref({ plans: [], details: [] })
const picked = ref([null, null])
const loading = ref(true)
const error = ref('')

const side = (i) => data.value.details.filter(d => String(d.planId) === String(picked.value[i]))
const dist = (list) => {
  const m = { 冲: 0, 稳: 0, 保: 0 }
  list.forEach(d => { const k = d.chanceLevel || '稳'; m[k] = (m[k] || 0) + 1 })
  return m
}
const left = computed(() => side(0))
const right = computed(() => side(1))
const onlyLeft = computed(() => left.value.filter(a => !right.value.some(b => b.universityName === a.universityName && b.majorName === a.majorName)))
const onlyRight = computed(() => right.value.filter(b => !left.value.some(a => a.universityName === b.universityName && a.majorName === b.majorName)))
const inBoth = computed(() => left.value.filter(a => right.value.some(b => b.universityName === a.universityName && b.majorName === a.majorName)))

const distOption = computed(() => {
  const a = dist(left.value), b = dist(right.value)
  return {
    tooltip: { trigger: 'axis' }, legend: { data: ['方案 A', '方案 B'] },
    xAxis: { type: 'category', data: ['冲', '稳', '保'] },
    yAxis: { type: 'value', name: '院校数', minInterval: 1 },
    series: [
      { name: '方案 A', type: 'bar', data: [a['冲'], a['稳'], a['保']], itemStyle: { color: '#4a8cf7' } },
      { name: '方案 B', type: 'bar', data: [b['冲'], b['稳'], b['保']], itemStyle: { color: '#8a6cf1' } },
    ],
  }
})
const planTitle = (i) => data.value.plans.find(p => String(p.id) === String(picked.value[i]))?.title || `方案 ${i === 0 ? 'A' : 'B'}`

async function load() {
  loading.value = true; error.value = ''
  try {
    data.value = (await feature.comparePlans(picked.value[0], picked.value[1])) || { plans: [], details: [] }
    if (picked.value[0] == null && data.value.plans.length) picked.value[0] = data.value.plans[0].id
    if (picked.value[1] == null && data.value.plans.length > 1) picked.value[1] = data.value.plans[1].id
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / COMPARE" title="方案对比"
      description="并排比较两套志愿方案：冲稳保分布、院校差异与重合项一目了然，切换下拉即时重算。"
      :actions="[{ label: '志愿填报', icon: '←', color: 'blue', to: '/planning/volunteer' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>
    <div v-if="loading" class="grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:180px"></div></div>
    <template v-else>
      <div class="grid gap-5 md:grid-cols-2">
        <div v-for="(s, idx) in [0, 1]" :key="idx" class="glass p-5">
          <label class="block">
            <span class="field-label">方案 {{ idx === 0 ? 'A' : 'B' }}</span>
            <select v-model="picked[idx]" class="field w-full">
              <option :value="null">请选择方案</option>
              <option v-for="p in data.plans" :key="p.id" :value="p.id">{{ p.title }}</option>
            </select>
          </label>
          <div class="mt-3 flex flex-wrap gap-2">
            <span v-for="(v, k) in dist(idx === 0 ? left : right)" :key="k" class="pill pill-sky">{{ k }} · {{ v }}</span>
          </div>
          <div class="mt-3 space-y-2">
            <div v-for="d in (idx === 0 ? left : right)" :key="d.id" class="glass-flat p-3 text-[12px]">
              {{ d.priority }}. {{ d.universityName }} · {{ d.majorName }}
              <span class="pill pill-amber ml-1">{{ d.chanceLevel }}</span>
              <span v-if="d.scoreLine" class="ml-1 faint">{{ d.scoreLine }} 分</span>
            </div>
            <p v-if="!(idx === 0 ? left : right).length" class="text-[12px] muted">选择一个方案查看明细</p>
          </div>
        </div>
      </div>

      <div class="mt-5 grid gap-5 md:grid-cols-[1.3fr_1fr]">
        <div class="glass p-5"><h2 class="section-title mb-2">冲稳保分布</h2><Chart :option="distOption" :height="260" /></div>
        <div class="glass p-5">
          <h2 class="section-title mb-3">差异速览</h2>
          <div class="grid grid-cols-3 gap-3 text-center">
            <div v-for="m in [
              { label: '仅 A', value: onlyLeft.length }, { label: '两边都有', value: inBoth.length }, { label: '仅 B', value: onlyRight.length },
            ]" :key="m.label" class="glass-flat p-3">
              <p class="stat-num">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
            </div>
          </div>
          <div class="mt-4 space-y-2 text-[12px]">
            <p class="muted">A：{{ planTitle(0) }}</p>
            <p class="muted">B：{{ planTitle(1) }}</p>
            <p class="faint">院校 + 专业名称完全一致时才算重合项。</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
