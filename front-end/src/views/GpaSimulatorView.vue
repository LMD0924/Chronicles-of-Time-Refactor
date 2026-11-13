<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const target = ref(3.6)
const result = ref(null)
const courses = ref([])
const draft = ref({})
const error = ref('')
const notice = ref('')
const calculating = ref(false)

const cfg = {
  eyebrow: 'GPA / COURSES', title: '课程与学分', description: '课程学分与绩点是 GPA 模拟与毕业缺口核对的数据源。', color: 'mint',
  api: R.planning.termCourses,
  fields: [{ key: 'term', label: '学期' }, { key: 'courseName', label: '课程名称', required: true },
           { key: 'credit', label: '学分', type: 'number' }, { key: 'score', label: '分数', type: 'number' },
           { key: 'gpaPoint', label: '绩点', type: 'number' }, { key: 'category', label: '类别', type: 'select', options: ['必修', '选修', '通识'] }],
  columns: [{ key: 'term', label: '学期' }, { key: 'courseName', label: '课程' }, { key: 'credit', label: '学分' },
            { key: 'score', label: '分数' }, { key: 'gpaPoint', label: '绩点' }],
}

const compareOption = computed(() => {
  const list = result.value?.courses || []
  return {
    tooltip: { trigger: 'axis' }, legend: { data: ['调整前', '调整后'] },
    xAxis: { type: 'category', data: list.map(c => c.courseName) },
    yAxis: { type: 'value', max: 4.5, name: '绩点' },
    series: [
      { name: '调整前', type: 'bar', data: list.map(c => c.before), itemStyle: { color: '#b9c7e4' } },
      { name: '调整后', type: 'bar', data: list.map(c => c.after), itemStyle: { color: '#12996a', borderRadius: [5, 5, 0, 0] } },
    ],
  }
})
const adjustedCount = computed(() => Object.keys(draft.value).filter(k => {
  const c = courses.value.find(x => x.courseName === k)
  return c && Number(draft.value[k]) !== Number(c.gpaPoint || 0)
}).length)

async function loadCourses() {
  courses.value = (await R.planning.termCourses.list()) || []
  const d = {}
  courses.value.forEach(c => { d[c.courseName] = Number(c.gpaPoint || 0) })
  draft.value = d
}

async function run() {
  calculating.value = true; error.value = ''
  try {
    const adjustments = Object.entries(draft.value).map(([courseName, gpaPoint]) => ({ courseName, gpaPoint: Number(gpaPoint) || 0 }))
    result.value = await feature.gpaWhatIf({ targetGpa: Number(target.value), adjustments })
    await loadCourses()
    // loadCourses 会重置 draft 为库里的真实值，需要把未落库的推演值重新贴回去
    adjustments.forEach(a => { draft.value[a.courseName] = a.gpaPoint })
  } catch (e) { error.value = e.message }
  finally { calculating.value = false }
}

function reset() {
  courses.value.forEach(c => { draft.value[c.courseName] = Number(c.gpaPoint || 0) })
  notice.value = '已恢复为当前真实绩点'
  run()
}

onMounted(async () => { await loadCourses(); await run() })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / GPA" title="GPA 模拟"
      description="按课程学分加权计算当前绩点；直接改下面任意一门课的绩点，即可推演「如果这科考好一点」对总绩点的影响，不写入数据库。"
      :actions="[
        { label: '毕业缺口', icon: '△', color: 'rose', to: '/planning/gap' },
        { label: '学期课表', icon: '▤', color: 'sky', to: '/planning/schedule' },
        { label: '刷新', icon: '↻', color: 'slate', click: run },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '当前 GPA', value: result?.currentGpa ?? '—' },
        { label: '推演 GPA', value: result?.simulatedGpa ?? '—' },
        { label: '变化', value: result ? `${result.delta > 0 ? '+' : ''}${result.delta}` : '—' },
        { label: '距目标', value: result?.gapToTarget ?? '—' },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div class="mt-5 grid gap-5 md:grid-cols-[1fr_1.3fr]">
      <div class="glass p-6">
        <label class="block"><span class="field-label">目标绩点</span>
          <input v-model="target" class="field w-full" type="number" step="0.1" min="0" max="4.5" @keyup.enter="run" /></label>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-violet" type="button" :disabled="calculating" @click="run"><span class="btn-glyph">◷</span> {{ calculating ? '计算中…' : '重新推演' }}</button>
          <button class="btn btn-slate" type="button" @click="reset">恢复真实绩点</button>
        </div>
        <p class="mt-3 text-[11px] faint">已调整 {{ adjustedCount }} 门课程 · 总学分 {{ result?.totalCredits || 0 }}</p>

        <h2 class="section-title mb-3 mt-6">逐科调整</h2>
        <div v-for="c in courses" :key="c.id" class="glass-flat mb-2 flex items-center gap-3 p-3">
          <div class="min-w-0 flex-1">
            <p class="truncate text-[13px] font-medium ink">{{ c.courseName }}</p>
            <p class="text-[11px] faint">{{ c.term }} · {{ c.credit }} 学分 · 实际 {{ c.gpaPoint }}</p>
          </div>
          <input v-model.number="draft[c.courseName]" class="field w-[84px]" type="number" step="0.1" min="0" max="4.5" />
        </div>
        <p v-if="!courses.length" class="text-[12px] muted">还没有课程，先在下方添加。</p>
      </div>

      <div class="glass h-fit p-5">
        <h2 class="section-title mb-2">调整前后对比</h2>
        <Chart :option="compareOption" :height="330" />
        <p class="mt-3 text-[11px] leading-6 faint">
          加权口径：Σ(绩点 × 学分) ÷ Σ学分。推演只覆盖你修改过的课程，原始课程数据保持不变。
        </p>
      </div>
    </div>

    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
