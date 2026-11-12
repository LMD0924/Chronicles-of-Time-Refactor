<script setup>
import { computed, onMounted, ref } from 'vue'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const scores = ref([])
const trend = ref([])
const weak = ref([])
const radar = ref({})
const heat = ref({})
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const form = ref({})

const subjects = computed(() => [...new Set(scores.value.map(s => s.subject).filter(Boolean))])
const avgScore = computed(() => scores.value.length
  ? Math.round(scores.value.reduce((s, x) => s + (Number(x.score) || 0), 0) / scores.value.length * 10) / 10 : 0)

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: trend.value.map(t => t.exam) },
  yAxis: { type: 'value', name: '平均分' },
  series: [{ type: 'line', smooth: true, data: trend.value.map(t => t.average), itemStyle: { color: '#4a8cf7' }, areaStyle: { opacity: 0.12 } }],
}))
const weakOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: weak.value.map(w => w.subject) },
  yAxis: { type: 'value', name: '平均分' },
  series: [{ type: 'bar', data: weak.value.map(w => w.average), itemStyle: { color: '#ef7a9b' }, barWidth: '42%' }],
}))
const radarOption = computed(() => ({
  tooltip: {},
  radar: { indicator: Object.keys(radar.value).map(k => ({ name: k, max: 100 })), radius: '65%' },
  series: [{ type: 'radar', data: [{ value: Object.values(radar.value), name: '学科均分', itemStyle: { color: '#8a6cf1' } }] }],
}))
const heatDays = computed(() => {
  const out = []
  const today = new Date()
  for (let i = 89; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    out.push({ date: key, count: heat.value[key] || 0 })
  }
  return out
})

async function load() {
  loading.value = true; error.value = ''
  try {
    const [s, t, w, r, h] = await Promise.all([
      R.learning.scores.list(), feature.scoreTrend(), feature.weakSubjects(), feature.radar(), feature.heatmap(),
    ])
    scores.value = s || []; trend.value = t || []; weak.value = w || []
    radar.value = r || {}; heat.value = h || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date()
  return {
    subject: subjects.value[0] || '', examName: '阶段测评',
    examDate: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    score: 0, fullScore: 100, classAvg: 0,
  }
}
function openCreate() { form.value = defaults(); showForm.value = true; notice.value = '' }
async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    ;['score', 'fullScore', 'classAvg'].forEach(k => { payload[k] = payload[k] === '' ? null : Number(payload[k]) })
    await R.learning.scores.save(payload)
    showForm.value = false; notice.value = '成绩已录入，图表已同步刷新'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}
async function remove(s) {
  if (!window.confirm(`确定删除「${s.examName} · ${s.subject}」吗？`)) return
  try { await R.learning.scores.remove(s.id); await load() } catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / SCORES" title="成绩分析"
      description="趋势、学科雷达、薄弱学科与答题热力四张图联动；录入新成绩后所有图表实时刷新。"
      :actions="[
        { label: '录入成绩', icon: '＋', color: 'blue', click: openCreate },
        { label: '练习中心', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '成绩记录', value: scores.length }, { label: '总体均分', value: avgScore },
        { label: '学科数', value: subjects.length }, { label: '最弱学科', value: weak[0]?.subject || '—' },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div v-if="loading" class="mt-5 grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:260px"></div></div>
    <template v-else>
      <div class="mt-5 grid gap-5 md:grid-cols-2">
        <div class="glass p-5"><h2 class="mb-2 text-[14px] font-semibold ink">考试趋势</h2><Chart :option="trendOption" :height="260" /></div>
        <div class="glass p-5"><h2 class="mb-2 text-[14px] font-semibold ink">学科雷达</h2><Chart :option="radarOption" :height="260" /></div>
        <div class="glass p-5"><h2 class="mb-2 text-[14px] font-semibold ink">薄弱学科（均分升序）</h2><Chart :option="weakOption" :height="260" /></div>
        <div class="glass p-5">
          <h2 class="text-[14px] font-semibold ink">近 90 天答题热力</h2>
          <p class="mt-1 text-[11px] faint">色块越深，当天作答越多。</p>
          <div class="mt-4 grid grid-cols-[repeat(15,minmax(0,1fr))] gap-1.5">
            <span v-for="d in heatDays" :key="d.date" class="block h-[13px] rounded-[3px] border border-hairline"
              :style="{ background: d.count ? `color-mix(in srgb, var(--sky) ${18 + Math.min(4, d.count) * 18}%, var(--surface-2))` : 'var(--surface-2)' }"
              :title="`${d.date} · ${d.count} 题`" />
          </div>
        </div>
      </div>

      <div class="glass mt-5 p-5">
        <h2 class="mb-3 text-[14px] font-semibold ink">成绩明细 ({{ scores.length }})</h2>
        <EmptyState v-if="!scores.length" text="还没有成绩记录" hint="点击「录入成绩」添加" />
        <div v-else class="overflow-x-auto">
          <table class="data-table w-full">
            <thead><tr><th>考试</th><th>学科</th><th>日期</th><th>得分</th><th>满分</th><th>班级均分</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="s in scores" :key="s.id">
                <td>{{ s.examName }}</td><td>{{ s.subject }}</td><td>{{ s.examDate }}</td>
                <td>{{ s.score }}</td><td>{{ s.fullScore }}</td><td>{{ s.classAvg }}</td>
                <td><button class="btn btn-rose" type="button" @click="remove(s)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">录入成绩</h2>
          <p class="mb-5 text-[12px] muted">保存后趋势、雷达、薄弱学科三张图会一起刷新。</p>
          <div class="form-grid">
            <label><span class="field-label">考试名称</span><input v-model="form.examName" class="field w-full" /></label>
            <label><span class="field-label">学科</span><input v-model="form.subject" class="field w-full" list="score-subjects" />
              <datalist id="score-subjects"><option v-for="s in subjects" :key="s" :value="s" /></datalist></label>
            <label><span class="field-label">考试日期</span><input v-model="form.examDate" class="field w-full" type="date" /></label>
            <label><span class="field-label">得分</span><input v-model="form.score" class="field w-full" type="number" step="0.5" /></label>
            <label><span class="field-label">满分</span><input v-model="form.fullScore" class="field w-full" type="number" /></label>
            <label><span class="field-label">班级均分</span><input v-model="form.classAvg" class="field w-full" type="number" step="0.5" /></label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-blue" type="button" :disabled="saving" @click="submit"><span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
