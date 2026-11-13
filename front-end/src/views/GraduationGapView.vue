<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const check = ref({ items: [], totalRemaining: 0, creditsByCategory: {}, totalCourseCredits: 0 })
const loading = ref(true)
const error = ref('')
const notice = ref('')

const gapOption = computed(() => ({
  tooltip: { trigger: 'axis' }, legend: { data: ['已修', '缺口'] },
  xAxis: { type: 'category', data: check.value.items.map(g => g.requirementName) },
  yAxis: { type: 'value', name: '学分' },
  series: [
    { name: '已修', type: 'bar', stack: 'credit', data: check.value.items.map(g => g.earnedCredit), itemStyle: { color: '#12996a' } },
    { name: '缺口', type: 'bar', stack: 'credit', data: check.value.items.map(g => g.remaining), itemStyle: { color: '#d9464e' } },
  ],
}))
const categoryOption = computed(() => {
  const entries = Object.entries(check.value.creditsByCategory || {})
  return {
    tooltip: {}, xAxis: { type: 'category', data: entries.map(e => e[0]) }, yAxis: { type: 'value', name: '学分' },
    series: [{ type: 'bar', barWidth: '44%', data: entries.map(e => e[1]), itemStyle: { color: '#4a8cf7', borderRadius: [6, 6, 0, 0] } }],
  }
})
const satisfied = computed(() => check.value.items.filter(g => g.satisfied).length)

async function load() {
  loading.value = true; error.value = ''
  try { check.value = (await feature.gapCheck()) || { items: [], creditsByCategory: {} } }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function saveEarned(g) {
  error.value = ''
  try {
    await R.planning.graduationGaps.update(g.id, { ...g, earnedCredit: Number(g.earnedCredit) || 0 })
    notice.value = `「${g.requirementName}」已修学分已更新`; await load()
  } catch (e) { error.value = e.message }
}

async function markDone(g) {
  error.value = ''
  try {
    await R.planning.graduationGaps.update(g.id, { ...g, earnedCredit: Number(g.requiredCredit) || 0, status: '已达标' })
    notice.value = `「${g.requirementName}」已标记达标`; await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / GAP" title="毕业缺口"
      description="按毕业要求逐项核对已修学分，未达标项给出剩余学分；可直接改已修学分或一键标记达标，图表与统计同步刷新。"
      :actions="[
        { label: 'GPA 模拟', icon: '◷', color: 'mint', to: '/planning/gpa' },
        { label: '学期课表', icon: '▤', color: 'sky', to: '/planning/schedule' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '毕业要求', value: check.items.length },
        { label: '已达标', value: satisfied },
        { label: '合计缺口', value: check.totalRemaining },
        { label: '课程总学分', value: check.totalCourseCredits },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div v-if="loading" class="mt-5 grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:260px"></div></div>
    <template v-else>
      <div class="mt-5 grid gap-5 md:grid-cols-2">
        <div class="glass p-5"><h2 class="section-title mb-2">要求 vs 已修</h2><Chart :option="gapOption" :height="280" /></div>
        <div class="glass p-5"><h2 class="section-title mb-2">课程学分按类别分布</h2><Chart :option="categoryOption" :height="280" /></div>
      </div>

      <div class="mt-5 grid gap-3 md:grid-cols-2">
        <div v-for="g in check.items" :key="g.id" class="glass p-5">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <h3 class="text-[14px] font-medium ink">{{ g.requirementName }}</h3>
            <span class="pill" :class="g.satisfied ? 'pill-mint' : 'pill-rose'">{{ g.satisfied ? '已达标' : `还差 ${g.remaining}` }}</span>
          </div>
          <div class="progress-track mt-3">
            <div class="progress-fill" :style="{ width: Math.min(100, Number(g.earnedCredit) / (Number(g.requiredCredit) || 1) * 100) + '%' }"></div>
          </div>
          <p class="mt-2 text-[11px] faint">{{ g.earnedCredit }} / {{ g.requiredCredit }} 学分 · {{ g.status || '未核对' }} · {{ g.note || '' }}</p>
          <div class="mt-4 flex flex-wrap items-center gap-2">
            <label class="flex items-center gap-2 text-[12px] muted">已修
              <input v-model.number="g.earnedCredit" class="field w-[88px]" type="number" step="0.5" />
            </label>
            <button class="btn btn-blue" type="button" @click="saveEarned(g)">保存</button>
            <button class="btn btn-mint" type="button" @click="markDone(g)">标记达标</button>
          </div>
        </div>
        <p v-if="!check.items.length" class="text-[12px] muted">暂无毕业要求数据</p>
      </div>
    </template>
  </section>
</template>
