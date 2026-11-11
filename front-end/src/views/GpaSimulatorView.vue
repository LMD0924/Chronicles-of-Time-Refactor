<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const target = ref(3.6)
const result = ref(null)
const msg = ref('')
const cfg = {
  eyebrow: 'GPA / COURSES', title: '课程与成绩', description: '课程学分与绩点是 GPA 模拟的数据源。', color: 'mint',
  api: R.planning.termCourses,
  fields: [{ key: 'term', label: '学期' }, { key: 'courseName', label: '课程名称', required: true },
           { key: 'credit', label: '学分', type: 'number' }, { key: 'score', label: '分数', type: 'number' },
           { key: 'gpaPoint', label: '绩点', type: 'number' }, { key: 'category', label: '类别', type: 'select', options: ['必修', '选修', '通识'] }],
  columns: [{ key: 'term', label: '学期' }, { key: 'courseName', label: '课程' }, { key: 'credit', label: '学分' }, { key: 'gpaPoint', label: '绩点' }]
}
const option = computed(() => ({
  tooltip: {}, xAxis: { type: 'category', data: (result.value?.courses || []).map(c => c.courseName) },
  yAxis: { type: 'value', max: 4.5 },
  series: [{ type: 'bar', data: (result.value?.courses || []).map(c => c.gpaPoint), itemStyle: { color: '#12996a', borderRadius: [6, 6, 0, 0] } }]
}))
async function run() { try { result.value = await feature.simulateGpa({ targetGpa: Number(target.value) }) } catch (e) { msg.value = e.message } }
onMounted(run)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / GPA" title="GPA 模拟"
      description="按课程学分加权计算当前绩点，并给出与目标绩点的差距。"
      :actions="[{ label: '毕业缺口', icon: '△', color: 'rose', to: '/planning/gap' }, { label: '学期课表', icon: '▤', color: 'sky', to: '/planning/schedule' }]" />
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <label class="block"><span class="field-label">目标绩点</span><input v-model="target" class="field w-full" type="number" step="0.1" /></label>
        <button class="btn btn-violet mt-4" @click="run"><span class="btn-glyph">◷</span> 重新计算</button>
        <div v-if="result" class="mt-6 grid grid-cols-3 gap-3 text-center">
          <div class="glass-flat p-3"><p class="stat-num">{{ result.currentGpa }}</p><p class="text-[11px] muted">当前 GPA</p></div>
          <div class="glass-flat p-3"><p class="stat-num">{{ result.targetGpa }}</p><p class="text-[11px] muted">目标 GPA</p></div>
          <div class="glass-flat p-3"><p class="stat-num">{{ result.gap }}</p><p class="text-[11px] muted">差距</p></div>
        </div>
        <p v-if="msg" class="mt-3 text-[12px] text-rose-500">{{ msg }}</p>
      </div>
      <div class="glass p-5"><h2 class="section-title mb-2">各课程绩点</h2><Chart :option="option" :height="270" /></div>
    </div>
    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
