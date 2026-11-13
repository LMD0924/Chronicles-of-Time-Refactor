<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const router = useRouter()
const subjects = ref([])
const hot = ref([])
const majors = ref([])
const tree = ref([])
const error = ref('')

const cfg = {
  eyebrow: '', title: '组合台账', description: '维护选科组合与专业覆盖率，图表与组合树会同步刷新。', color: 'sky',
  fields: [
    { key: 'firstSubject', label: '首选科目' }, { key: 'secondSubjects', label: '再选科目' },
    { key: 'label', label: '组合名称' }, { key: 'studentCount', label: '选择人数', type: 'number' },
    { key: 'coverage', label: '专业覆盖率', type: 'number' },
  ],
  columns: [
    { key: 'firstSubject', label: '首选' }, { key: 'secondSubjects', label: '再选' },
    { key: 'studentCount', label: '人数' }, { key: 'coverage', label: '覆盖率' },
  ],
}
cfg.api = R.planning.subjectCombinations

const hotOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: hot.value.map(c => c.firstSubject + '/' + (c.secondSubjects || c.label || '')),
    axisLabel: { rotate: 20, fontSize: 10 } },
  yAxis: [{ type: 'value', name: '人数' }, { type: 'value', name: '覆盖率', max: 100 }],
  series: [
    { name: '选择人数', type: 'bar', data: hot.value.map(c => c.studentCount), itemStyle: { color: '#4a8cf7', borderRadius: [5, 5, 0, 0] } },
    { name: '覆盖率', type: 'line', yAxisIndex: 1, smooth: true, data: hot.value.map(c => c.coverage), itemStyle: { color: '#e0a23c' } },
  ],
}))

async function load() {
  error.value = ''
  try {
    const [s, h, m, t] = await Promise.all([
      feature.subjects(), feature.hotCombinations(),
      R.planning.majorRequirements.list(), feature.combinationTree(),
    ])
    subjects.value = s || []; hot.value = h || []; majors.value = m || []; tree.value = t || []
  } catch (e) { error.value = e.message }
}
function goSelection(first) { router.push({ path: '/planning/selection', query: first ? { first } : {} }) }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SUBJECTS" title="学科与组合"
      description="学科字典、组合树、热门组合与专业选科要求放在一起；点任一组合直接带条件跳到选科中心。"
      :actions="[
        { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' },
        { label: '专业要求', icon: '⌘', color: 'teal', to: '/planning/majors' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>

    <div class="glass p-5">
      <h2 class="section-title mb-3">学科字典</h2>
      <div class="flex flex-wrap gap-2">
        <span v-for="s in subjects" :key="s.id" class="pill" :class="s.isFirst ? 'pill-violet' : 'pill-sky'">
          {{ s.name }} <small class="faint">{{ s.category }}</small>
        </span>
        <span v-if="!subjects.length" class="text-[12px] muted">暂无学科数据</span>
      </div>
    </div>

    <div class="mt-5 grid gap-5 md:grid-cols-[1.4fr_1fr]">
      <div class="glass p-5">
        <div class="mb-3 flex items-center justify-between">
          <h2 class="section-title">组合树</h2>
          <span class="pill pill-sky">{{ tree.length }} 个首选科目分支</span>
        </div>
        <div v-for="node in tree" :key="node.firstSubject" class="glass-flat mb-3 p-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <button class="btn btn-violet" type="button" @click="goSelection(node.firstSubject)">{{ node.firstSubject }} →</button>
            <span class="text-[11px] faint">{{ node.count }} 种组合 · {{ node.students }} 人</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <button v-for="c in node.children" :key="c.id" class="btn btn-slate" type="button" @click="goSelection(c.firstSubject)">
              {{ c.secondSubjects || c.label || '组合' }}
              <span class="ml-1 faint">{{ c.studentCount }} 人 · {{ c.coverage }}%</span>
            </button>
          </div>
        </div>
        <p v-if="!tree.length" class="text-[12px] muted">还没有组合记录，可在下方台账里新增</p>
      </div>

      <div class="glass h-fit p-5">
        <h2 class="section-title mb-2">热门组合与覆盖率</h2>
        <Chart :option="hotOption" :height="280" />
      </div>
    </div>

    <div class="mt-5 glass p-5">
      <h2 class="section-title mb-3">专业选科要求</h2>
      <div class="grid gap-2 md:grid-cols-2">
        <div v-for="m in majors" :key="m.id" class="glass-flat flex flex-wrap items-center gap-2 p-3">
          <span class="pill pill-amber">{{ m.category }}</span>
          <span class="text-[12px] ink">{{ m.majorName }}</span>
          <span class="ml-auto text-[11px] faint">{{ m.requiredSubjects || '不限' }}</span>
        </div>
      </div>
      <p v-if="!majors.length" class="text-[12px] muted">暂无专业要求，到「专业要求」页维护</p>
    </div>

    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
