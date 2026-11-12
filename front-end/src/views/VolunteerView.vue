<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'

const BASE = '/api/v1/planning/volunteer'
const api = {
  plans: () => http.get(`${BASE}/plans`),
  addPlan: (d) => http.post(`${BASE}/plans`, d),
  deletePlan: (id) => http.delete(`${BASE}/plans/${id}`),
  planDetails: (id) => http.get(`${BASE}/plans/${id}/details`),
  addDetail: (d) => http.post(`${BASE}/details`, d),
  deleteDetail: (id) => http.delete(`${BASE}/details/${id}`),
  batchDetails: (arr) => http.post(`${BASE}/details/batch`, arr),
  fromRecommend: (d) => http.post(`${BASE}/details/from-recommend`, d),
  matchingCheck: (id) => http.post(`${BASE}/matching/check/${id}`, {}),
  matchingReport: (id) => http.get(`${BASE}/matching/report/${id}`),
  simulateSingle: (id, score) => http.post(`${BASE}/simulate/single/${id}`, score === '' || score == null ? {} : { score: Number(score) }),
  statistics: () => http.get(`${BASE}/statistics`),
  searchMajors: (kw) => http.get(`${BASE}/search/majors`, { params: { keyword: kw } }),
  recommend: (d) => http.post('/api/v1/planning/volunteer/recommend', d),
}

const tab = ref('search')
const tabs = [
  { key: 'search', label: '院校检索' }, { key: 'recommend', label: '智能推荐' },
  { key: 'simulate', label: '模拟填报' }, { key: 'plans', label: '方案管理' },
  { key: 'details', label: '志愿明细' }, { key: 'matching', label: '匹配报告' },
]
const notice = ref('')
const plans = ref([])
const activePlanId = ref('')
const details = ref([])
const volStats = ref({})
const report = ref(null)
const majors = ref([])
const recommendList = ref([])
const simResults = ref([])
const matchResults = reactive({})

const keyword = ref('')
const majorKw = ref(''), score = ref(600)
const recUniversity = ref('')
const simScore = ref('')
const planForm = reactive({ title: '', planYear: '', province: '', status: 'DRAFT' })
const detailForm = reactive({ universityName: '', majorName: '', priority: '', chanceLevel: '稳', scoreLine: '', note: '' })
const batchText = ref('')

const activePlanTitle = computed(() => (plans.value.find(p => String(p.id) === String(activePlanId.value)) || {}).title || '')

function toast(m) { notice.value = m }
function fail(e) { notice.value = e?.message || '操作失败' }
function levelPill(l) { return l === '保' ? 'pill-mint' : l === '冲' ? 'pill-rose' : 'pill-amber' }
function resultPill(r) { return r === 'ADMITTED' || r === 'MATCHED' ? 'pill-mint' : r === 'REJECTED' ? 'pill-rose' : 'pill-amber' }
function pct(v) { return Math.round((Number(v) || 0) * 100) }

async function loadPlans() {
  try {
    plans.value = await api.plans() || []
    if (!activePlanId.value && plans.value.length) activePlanId.value = plans.value[0].id
  } catch (e) { fail(e) }
}
async function loadStats() { try { volStats.value = await api.statistics() || {} } catch (e) { fail(e) } }
async function loadDetails() {
  if (!activePlanId.value) { details.value = []; return }
  try { details.value = await api.planDetails(activePlanId.value) || [] } catch (e) { fail(e) }
}
async function loadReport() {
  if (!activePlanId.value) { report.value = null; return }
  try { report.value = await api.matchingReport(activePlanId.value) } catch (e) { report.value = null; fail(e) }
}
async function search() { try { majors.value = await api.searchMajors(keyword.value) || [] } catch (e) { fail(e) } }
async function runRecommend() {
  try { recommendList.value = await api.recommend({ major: majorKw.value, score: Number(score.value) }) || [] }
  catch (e) { fail(e) }
}
async function onPlanChange() { await loadDetails(); await loadReport() }
function focusPlan(p) { activePlanId.value = p.id; loadDetails(); loadReport() }
async function refresh() { await loadPlans(); await loadStats(); await loadDetails(); await loadReport() }

function selectTab(k) {
  tab.value = k
  notice.value = ''
  const loaders = {
    search: search, recommend: loadPlans, simulate: onPlanChange,
    plans: () => { loadPlans(); loadStats() }, details: () => { loadPlans(); loadDetails() },
    matching: () => { loadPlans(); loadReport() },
  }
  loaders[k]?.()
}

async function addPlan() {
  if (!planForm.title.trim()) { toast('请填写方案名称'); return }
  try {
    await api.addPlan({ ...planForm })
    Object.assign(planForm, { title: '', planYear: '', province: '', status: 'DRAFT' })
    toast('方案已创建'); await refresh()
  } catch (e) { fail(e) }
}
async function deletePlan(id) {
  if (!window.confirm('删除方案会同时删除其下志愿与模拟结果，确定吗？')) return
  try {
    await api.deletePlan(id)
    if (String(activePlanId.value) === String(id)) { activePlanId.value = ''; details.value = []; report.value = null; simResults.value = [] }
    toast('方案已删除'); await refresh()
  } catch (e) { fail(e) }
}

async function addToPlan(r) {
  if (!activePlanId.value) { toast('请先在「方案管理」创建并选择目标方案'); return }
  try {
    await api.fromRecommend({
      planId: Number(activePlanId.value),
      universityName: r.universityName || recUniversity.value || r.majorName,
      majorName: r.majorName,
      chanceLevel: r.chanceLevel || '稳',
      scoreLine: r.suggestedScore || r.scoreLine || 0,
    })
    toast('已添加到志愿方案'); await refresh()
  } catch (e) { fail(e) }
}

async function addDetail() {
  if (!activePlanId.value) { toast('请先选择方案'); return }
  if (!detailForm.majorName.trim()) { toast('请填写专业名称'); return }
  try {
    await api.addDetail({
      planId: Number(activePlanId.value),
      universityName: detailForm.universityName,
      majorName: detailForm.majorName,
      priority: detailForm.priority === '' ? 0 : Number(detailForm.priority),
      chanceLevel: detailForm.chanceLevel,
      scoreLine: detailForm.scoreLine === '' ? 0 : Number(detailForm.scoreLine),
      note: detailForm.note,
    })
    Object.assign(detailForm, { universityName: '', majorName: '', priority: '', chanceLevel: '稳', scoreLine: '', note: '' })
    toast('志愿已新增'); await refresh()
  } catch (e) { fail(e) }
}
async function deleteDetail(id) {
  try { await api.deleteDetail(id); toast('志愿已删除'); await refresh() } catch (e) { fail(e) }
}
async function runBatch() {
  if (!activePlanId.value) { toast('请先选择方案'); return }
  const lines = batchText.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (!lines.length) { toast('请填写批量数据'); return }
  const arr = lines.map(line => {
    const [universityName, majorName, chanceLevel, scoreLine, note] = line.split(/[,，\t]/).map(s => (s || '').trim())
    return {
      planId: Number(activePlanId.value),
      universityName: universityName || '', majorName: majorName || '',
      chanceLevel: chanceLevel || '稳', scoreLine: Number(scoreLine) || 0, note: note || '',
      priority: 0,
    }
  })
  try { await api.batchDetails(arr); batchText.value = ''; toast(`已批量新增 ${arr.length} 条`); await refresh() }
  catch (e) { fail(e) }
}

function setSim(r) {
  const i = simResults.value.findIndex(x => String(x.detailId) === String(r.detailId))
  if (i >= 0) simResults.value[i] = r; else simResults.value.push(r)
}
async function simulateOne(d) {
  try { const r = await api.simulateSingle(d.id, simScore.value); setSim(r); toast(`模拟完成：${r.result}`) }
  catch (e) { fail(e) }
}
async function simulateAll() {
  for (const d of details.value) await simulateOne(d)
}
async function checkMatch(d) {
  try { const r = await api.matchingCheck(d.id); matchResults[d.id] = r; toast(`匹配检查完成：命中 ${r.hitCount}/${r.requiredCount}`) }
  catch (e) { fail(e) }
}

onMounted(() => { loadPlans(); loadStats(); search() })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / VOLUNTEER" title="志愿填报"
      description="院校检索、智能推荐、模拟填报、方案管理、志愿明细与选科匹配报告，全部基于同一份真实数据。"
      :actions="[{ label: '选科中心', icon: '◎', color: 'blue', to: '/planning/selection' }]" />
    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }" @click="selectTab(t.key)">{{ t.label }}</button>
    </div>
    <p v-if="notice" class="mb-3 text-[12px] text-rose-500">{{ notice }}</p>

    <div v-if="tab === 'search'" class="glass p-6">
      <div class="crud-toolbar">
        <input v-model="keyword" class="field max-w-[280px]" placeholder="专业 / 代码 / 门类 / 选科要求…" @keyup.enter="search" />
        <button class="btn btn-blue" @click="search"><span class="btn-glyph">⌕</span> 检索</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>代码</th><th>专业</th><th>门类</th><th>层次</th><th>选科要求</th></tr></thead>
        <tbody><tr v-for="m in majors" :key="m.id"><td>{{ m.majorCode || '—' }}</td><td>{{ m.majorName || '—' }}</td><td>{{ m.category || '—' }}</td><td>{{ m.level || '—' }}</td><td>{{ m.requiredSubjects || '—' }}</td></tr></tbody>
      </table>
      <p v-if="!majors.length" class="mt-3 text-[12px] muted">暂无匹配数据</p>
    </div>

    <div v-else-if="tab === 'recommend'" class="glass p-6">
      <div class="crud-toolbar">
        <input v-model="majorKw" class="field max-w-[200px]" placeholder="意向专业（可空）" />
        <input v-model="score" class="field max-w-[140px]" type="number" placeholder="分数" />
        <input v-model="recUniversity" class="field max-w-[200px]" placeholder="目标院校（可空）" />
        <select v-model="activePlanId" class="field max-w-[220px]">
          <option value="">目标方案</option>
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.title }} (#{{ p.id }})</option>
        </select>
        <button class="btn btn-violet" @click="runRecommend"><span class="btn-glyph">◎</span> 生成推荐</button>
      </div>
      <p class="mb-3 text-[12px] muted">当前目标方案：<span class="pill pill-sky">{{ activePlanTitle || '未选择' }}</span></p>
      <div class="crud-grid">
        <div v-for="r in recommendList" :key="r.id" class="crud-card">
          <span class="pill" :class="levelPill(r.chanceLevel)">{{ r.chanceLevel }}</span>
          <h3 class="mt-2 text-[14px] font-semibold ink">{{ r.majorName }}</h3>
          <p class="mt-1 text-[12px] muted">{{ r.category }} · {{ r.requiredSubjects }}</p>
          <p class="mt-2 text-[11px] faint">参考分 {{ r.suggestedScore }}</p>
          <button class="btn btn-mint mt-3" @click="addToPlan(r)"><span class="btn-glyph">＋</span> 添加到志愿</button>
        </div>
      </div>
      <p v-if="!recommendList.length" class="mt-3 text-[12px] muted">点击「生成推荐」查看结果</p>
    </div>

    <div v-else-if="tab === 'simulate'" class="glass p-6">
      <div class="crud-toolbar">
        <select v-model="activePlanId" class="field max-w-[240px]" @change="onPlanChange">
          <option value="">选择方案</option>
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.title }} (#{{ p.id }})</option>
        </select>
        <input v-model="simScore" class="field max-w-[160px]" type="number" placeholder="本人分数（可空）" />
        <button class="btn btn-amber" @click="simulateAll"><span class="btn-glyph">▶</span> 批量模拟</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>序</th><th>院校</th><th>专业</th><th>档位</th><th>参考分</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="d in details" :key="d.id">
            <td>{{ d.priority }}</td><td>{{ d.universityName || '—' }}</td><td>{{ d.majorName || '—' }}</td>
            <td><span class="pill" :class="levelPill(d.chanceLevel)">{{ d.chanceLevel || '—' }}</span></td>
            <td>{{ d.scoreLine || 0 }}</td>
            <td>
              <button class="btn btn-violet" @click="simulateOne(d)">单条模拟</button>
              <button class="btn btn-blue ml-1" @click="checkMatch(d)">匹配检查</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!details.length" class="mt-3 text-[12px] muted">选择方案后查看志愿明细</p>

      <div v-if="simResults.length" class="mt-5">
        <h3 class="eyebrow mb-2">单条模拟结果</h3>
        <div class="crud-grid">
          <div v-for="s in simResults" :key="'s' + s.detailId" class="crud-card">
            <span class="pill" :class="resultPill(s.result)">{{ s.result }}</span>
            <p class="mt-2 text-[12px] muted">{{ s.analysis }}</p>
            <p class="mt-1 text-[11px] faint">志愿 #{{ s.detailId }} · 概率 {{ pct(s.probability) }}%</p>
          </div>
        </div>
      </div>
      <div v-if="Object.keys(matchResults).length" class="mt-5">
        <h3 class="eyebrow mb-2">选科匹配检查</h3>
        <div class="crud-grid">
          <div v-for="(m, k) in matchResults" :key="'m' + k" class="crud-card">
            <span class="pill" :class="resultPill(m.result)">{{ m.result }}</span>
            <p class="mt-2 text-[12px] muted">要求：{{ (m.requiredSubjects || []).join('、') || '—' }}</p>
            <p class="mt-1 text-[12px] muted">命中 {{ m.hitCount }}/{{ m.requiredCount }} · 匹配分 {{ m.matchingScore }}</p>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="tab === 'plans'" class="glass p-6">
      <h2 class="section-title mb-3">方案统计</h2>
      <div class="grid gap-3 md:grid-cols-4 mb-5">
        <div class="glass-flat p-3"><p class="text-[11px] faint">方案数</p><p class="stat-num mt-1">{{ volStats.planCount || 0 }}</p></div>
        <div class="glass-flat p-3"><p class="text-[11px] faint">志愿数</p><p class="stat-num mt-1">{{ volStats.detailCount || 0 }}</p></div>
        <div class="glass-flat p-3"><p class="text-[11px] faint">平均参考分</p><p class="stat-num mt-1">{{ volStats.averageScoreLine || 0 }}</p></div>
        <div class="glass-flat p-3">
          <p class="text-[11px] faint">档位分布</p>
          <div class="mt-2">
            <span v-for="(v, k) in (volStats.chanceLevelDistribution || {})" :key="k" class="pill" :class="levelPill(k)">{{ k }} · {{ v }}</span>
            <span v-if="!Object.keys(volStats.chanceLevelDistribution || {}).length" class="text-[12px] muted">暂无</span>
          </div>
        </div>
      </div>
      <h2 class="section-title mb-3">方案管理</h2>
      <div class="crud-toolbar flex-wrap">
        <input v-model="planForm.title" class="field max-w-[180px]" placeholder="方案名称 *" />
        <input v-model="planForm.planYear" class="field max-w-[120px]" placeholder="年份" />
        <input v-model="planForm.province" class="field max-w-[120px]" placeholder="省份" />
        <select v-model="planForm.status" class="field max-w-[140px]">
          <option value="DRAFT">草稿</option><option value="ACTIVE">启用</option><option value="ARCHIVED">归档</option>
        </select>
        <button class="btn btn-blue" @click="addPlan">新增方案</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>编号</th><th>名称</th><th>年份</th><th>省份</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="p in plans" :key="p.id">
            <td>{{ p.id }}</td><td>{{ p.title }}</td><td>{{ p.planYear || '—' }}</td><td>{{ p.province || '—' }}</td>
            <td><span class="pill pill-sky">{{ p.status || '—' }}</span></td>
            <td>
              <button class="btn btn-violet" @click="focusPlan(p)">查看明细</button>
              <button class="btn btn-rose ml-1" @click="deletePlan(p.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!plans.length" class="mt-3 text-[12px] muted">暂无方案，请先新增</p>
    </div>

    <div v-else-if="tab === 'details'" class="glass p-6">
      <div class="crud-toolbar">
        <select v-model="activePlanId" class="field max-w-[240px]" @change="loadDetails">
          <option value="">选择方案</option>
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.title }} (#{{ p.id }})</option>
        </select>
        <span class="pill pill-sky">共 {{ details.length }} 条</span>
      </div>
      <div class="crud-toolbar flex-wrap">
        <input v-model="detailForm.universityName" class="field max-w-[160px]" placeholder="院校" />
        <input v-model="detailForm.majorName" class="field max-w-[160px]" placeholder="专业 *" />
        <input v-model="detailForm.priority" class="field max-w-[100px]" type="number" placeholder="序" />
        <select v-model="detailForm.chanceLevel" class="field max-w-[110px]"><option>冲</option><option>稳</option><option>保</option></select>
        <input v-model="detailForm.scoreLine" class="field max-w-[120px]" type="number" placeholder="参考分" />
        <input v-model="detailForm.note" class="field max-w-[160px]" placeholder="备注" />
        <button class="btn btn-blue" @click="addDetail">新增志愿</button>
      </div>
      <div class="glass-flat mb-4 p-3">
        <span class="field-label">批量新增（每行：院校,专业,档位,参考分,备注）</span>
        <textarea v-model="batchText" class="field w-full" rows="3" placeholder="北京大学,计算机科学与技术,冲,680,第一志愿"></textarea>
        <button class="btn btn-violet mt-2" @click="runBatch">批量新增</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>序</th><th>院校</th><th>专业</th><th>档位</th><th>参考分</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="d in details" :key="d.id">
            <td>{{ d.priority }}</td><td>{{ d.universityName || '—' }}</td><td>{{ d.majorName || '—' }}</td>
            <td><span class="pill" :class="levelPill(d.chanceLevel)">{{ d.chanceLevel || '—' }}</span></td>
            <td>{{ d.scoreLine || 0 }}</td><td>{{ d.note || '—' }}</td>
            <td><button class="btn btn-rose" @click="deleteDetail(d.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!details.length" class="mt-3 text-[12px] muted">选择方案后查看并维护志愿明细</p>
    </div>

    <div v-else class="glass p-6">
      <h2 class="section-title mb-3">选科匹配报告</h2>
      <div class="crud-toolbar">
        <select v-model="activePlanId" class="field max-w-[240px]" @change="loadReport">
          <option value="">选择方案</option>
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.title }} (#{{ p.id }})</option>
        </select>
        <button class="btn btn-blue" @click="loadReport">刷新报告</button>
      </div>
      <div v-if="report" class="grid gap-3 md:grid-cols-4 mb-5">
        <div class="glass-flat p-3"><p class="text-[11px] faint">志愿总数</p><p class="stat-num mt-1">{{ report.total || 0 }}</p></div>
        <div class="glass-flat p-3"><p class="text-[11px] faint">完全命中</p><p class="stat-num mt-1">{{ report.matched || 0 }}</p></div>
        <div class="glass-flat p-3"><p class="text-[11px] faint">未完全匹配</p><p class="stat-num mt-1">{{ report.unmatched || 0 }}</p></div>
        <div class="glass-flat p-3"><p class="text-[11px] faint">平均匹配分</p><p class="stat-num mt-1">{{ report.averageMatchingScore || 0 }}</p></div>
      </div>
      <p v-if="report" class="mb-3 text-[12px] muted">已确认选科组合：<span v-for="s in (report.chosenSubjects || [])" :key="s" class="pill pill-mint mr-1">{{ s }}</span><span v-if="!(report.chosenSubjects || []).length" class="muted">尚未确认选科</span></p>
      <table v-if="report" class="data-table w-full">
        <thead><tr><th>院校</th><th>专业</th><th>选科要求</th><th>命中</th><th>匹配分</th><th>结论</th></tr></thead>
        <tbody>
          <tr v-for="d in (report.details || [])" :key="d.detailId">
            <td>{{ d.universityName || '—' }}</td><td>{{ d.majorName || '—' }}</td>
            <td>{{ (d.requiredSubjects || []).join('、') || '—' }}</td>
            <td>{{ d.hitCount }}/{{ d.requiredCount }}</td>
            <td>{{ d.matchingScore }}</td>
            <td><span class="pill" :class="d.matched ? 'pill-mint' : 'pill-amber'">{{ d.matched ? '匹配' : '未完全匹配' }}</span></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!report" class="text-[12px] muted">选择方案后查看匹配报告</p>
    </div>
  </section>
</template>
