<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const terms = ref([])
const active = ref('')
const loading = ref(true)
const error = ref('')

const current = computed(() => terms.value.find(t => t.term === active.value) || terms.value[0] || null)
const categories = computed(() => {
  const m = new Map()
  for (const c of current.value?.courses || []) {
    const k = c.category || '未分类'
    if (!m.has(k)) m.set(k, { name: k, count: 0, credit: 0 })
    const v = m.get(k); v.count++; v.credit += Number(c.credit || 0)
  }
  return [...m.values()]
})
const termOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: terms.value.map(t => t.term) },
  yAxis: { type: 'value', name: '学分' },
  series: [{ type: 'bar', barWidth: '42%', data: terms.value.map(t => t.totalCredit),
    itemStyle: { color: '#4a8cf7', borderRadius: [6, 6, 0, 0] } }],
}))

async function load() {
  loading.value = true; error.value = ''
  try {
    terms.value = (await feature.scheduleGrid()) || []
    if (!terms.value.some(t => t.term === active.value)) active.value = terms.value[0]?.term || ''
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SCHEDULE" title="学期课表"
      description="按学期聚合并统计学分、均分与平均绩点；同一份课程数据同时供 GPA 推演与毕业缺口核对使用。"
      :actions="[
        { label: '添加课程', icon: '＋', color: 'violet', to: '/planning/gpa' },
        { label: '毕业缺口', icon: '△', color: 'rose', to: '/planning/gap' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div v-if="loading" class="grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:150px"></div></div>
    <template v-else>
      <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>
      <EmptyState v-if="!terms.length" text="课表还是空的" hint="到「GPA 模拟」页添加课程与学分" />

      <template v-else>
        <div class="glass p-5">
          <h2 class="section-title mb-2">各学期学分</h2>
          <Chart :option="termOption" :height="220" />
        </div>

        <div class="crud-toolbar mt-5">
          <button v-for="t in terms" :key="t.term" class="btn" :class="active === t.term ? 'btn-violet' : 'btn-slate'" type="button" @click="active = t.term">
            {{ t.term }} <span class="ml-1 text-[11px]">· {{ t.count }} 门</span>
          </button>
        </div>

        <div v-if="current" class="mt-5 grid gap-5 md:grid-cols-[1fr_1.7fr]">
          <div class="glass h-fit p-6">
            <p class="eyebrow">TERM SUMMARY</p>
            <h2 class="section-title mt-1 mb-5">{{ current.term }}</h2>
            <div class="grid grid-cols-2 gap-4 text-center">
              <div v-for="m in [
                { label: '课程数', value: current.count }, { label: '总学分', value: current.totalCredit },
                { label: '平均分', value: current.averageScore }, { label: '平均绩点', value: current.averageGpa },
              ]" :key="m.label" class="glass-flat p-4">
                <p class="stat-num">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
              </div>
            </div>
            <h3 class="section-title mb-3 mt-6">类别分布</h3>
            <div v-for="c in categories" :key="c.name" class="mb-3">
              <div class="mb-1 flex justify-between text-[12px]"><span class="muted">{{ c.name }}</span><span class="faint">{{ c.credit }} 学分 / {{ c.count }} 门</span></div>
              <div class="progress-track"><div class="progress-fill" :style="{ width: Math.min(100, c.credit / (current.totalCredit || 1) * 100) + '%' }"></div></div>
            </div>
          </div>

          <div class="glass overflow-x-auto p-5">
            <h2 class="section-title mb-4">课程网格</h2>
            <table class="data-table w-full">
              <thead><tr><th>课程</th><th>类别</th><th>学分</th><th>分数</th><th>绩点</th></tr></thead>
              <tbody>
                <tr v-for="c in current.courses" :key="c.id">
                  <td class="font-medium ink">{{ c.courseName }}</td>
                  <td><span class="pill pill-sky">{{ c.category || '课程' }}</span></td>
                  <td>{{ c.credit }}</td><td>{{ c.score }}</td><td>{{ c.gpaPoint }}</td>
                </tr>
              </tbody>
            </table>
            <div class="mt-5 grid gap-3 md:grid-cols-3">
              <div v-for="c in current.courses" :key="'card-' + c.id" class="crud-card">
                <span class="pill pill-neutral">{{ c.category || '课程' }}</span>
                <h3 class="mt-2 text-[13px] font-semibold ink">{{ c.courseName }}</h3>
                <p class="mt-1 text-[11px] muted">{{ c.credit }} 学分 · {{ c.score }} 分 · 绩点 {{ c.gpaPoint }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </section>
</template>
