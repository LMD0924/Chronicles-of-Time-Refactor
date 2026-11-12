<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const BASE = '/api/v1/planning/selection'
const api = {
  mine: () => http.get(`${BASE}/mine`),
  submit: (data) => http.post(`${BASE}/submit`, data),
  confirm: (id) => http.put(`${BASE}/confirm/${id}`, {}),
  modify: (id, data) => http.put(`${BASE}/modify/${id}`, data),
  cancel: (id, reason) => http.delete(`${BASE}/cancel/${id}`, { params: { reason } }),
  intentions: () => http.get(`${BASE}/intentions`),
  addIntention: (data) => http.post(`${BASE}/intentions`, data),
  intentionStatus: (id, status) => http.put(`${BASE}/intentions/${id}/status`, { status }),
  guidances: () => http.get(`${BASE}/guidances`),
  addGuidance: (data) => http.post(`${BASE}/guidances`, data),
  guidanceStatus: (id, status) => http.put(`${BASE}/guidances/${id}/status`, { status }),
  histories: (params) => http.get(`${BASE}/histories`, { params }),
  historyStats: () => http.get(`${BASE}/histories/statistics`),
  approvals: () => http.get(`${BASE}/approvals/pending`),
  decide: (id, decision, comment) => http.put(`${BASE}/approvals/${id}`, {}, { params: { decision, comment } }),
  statistics: () => http.get(`${BASE}/statistics`),
  advice: () => http.get(`${BASE}/advice`),
  subjects: () => http.get(`${BASE}/subjects`),
  majorMatch: (params) => http.get(`${BASE}/major-match`, { params }),
}

const STATUS_LABEL = {
  PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CONFIRMED: '已确认', CANCELLED: '已退选',
  DRAFT: '草稿', EVALUATED: '已评估', ADOPTED: '已采纳', ONGOING: '进行中', COMPLETED: '已完成'
}
const INTENTION_FLOW = ['DRAFT', 'EVALUATED', 'CONFIRMED', 'ADOPTED']
const GUIDANCE_FLOW = ['ONGOING', 'COMPLETED', 'ADOPTED']

const tab = ref('my')
const notice = ref('')
const busy = ref(false)
const tabs = computed(() => {
  const list = [
    { key: 'my', label: '我的选科' }, { key: 'submit', label: '提交选科' },
    { key: 'intention', label: '选科意向' }, { key: 'guidance', label: '选科指导' },
    { key: 'history', label: '变更历史' }, { key: 'statistics', label: '统计分析' },
    { key: 'major', label: '专业推荐' }, { key: 'advice', label: '智能建议' },
  ]
  if (auth.isAdmin) list.splice(5, 0, { key: 'approval', label: '审批管理' })
  return list
})

const subjects = ref({ first: [], second: [] })
const mine = ref([])
const subForm = reactive({ firstSubject: '', secondSubjects: [], note: '' })
const editingId = ref(null)
const editForm = reactive({ firstSubject: '', secondSubjects: '', note: '' })
const intentions = ref([])
const intForm = reactive({ targetMajor: '', targetSchool: '', reason: '', status: 'DRAFT' })
const guidances = ref([])
const guiForm = reactive({ title: '', content: '', strength: '', weakness: '', opportunity: '', threat: '', status: 'ONGOING' })
const historyFilter = reactive({ action: '', limit: 100 })
const histories = ref([])
const historyStats = ref({})
const approvals = ref([])
const approvalComments = reactive({})
const stats = ref({})
const matchForm = reactive({ firstSubject: '', subjects: '' })
const majorMatches = ref([])
const advice = ref([])

function toast(m) { notice.value = m }
function fail(e) { notice.value = e?.message || '操作失败' }
function statusText(s) { return STATUS_LABEL[s] || s || '—' }
function comboName(r) {
  return r.label || ((r.firstSubject || '') + (r.secondSubjects ? ' + ' + r.secondSubjects : '')) || ('组合 #' + r.combinationId)
}
function statusClass(s) {
  const v = String(s || '')
  if (v === 'PENDING') return 'pill-amber'
  if (['APPROVED', 'CONFIRMED', 'ADOPTED', 'COMPLETED'].includes(v)) return 'pill-mint'
  if (['REJECTED', 'CANCELLED'].includes(v)) return 'pill-rose'
  if (v === 'DRAFT') return 'pill-neutral'
  if (['ONGOING', 'EVALUATED'].includes(v)) return 'pill-sky'
  return 'pill-violet'
}
function barWidth(v, max) {
  const n = Number(v) || 0, m = Number(max) || 1
  return m > 0 ? Math.round(n / m * 100) : 0
}
function maxOf(o) {
  const vals = Object.values(o || {}).map(Number)
  return vals.length ? Math.max(1, ...vals) : 1
}
const maxHot = computed(() => Math.max(1, ...(stats.value.hotCombinations || []).map(x => Number(x.count) || 0)))
const adviceSummary = computed(() => (advice.value || []).find(x => x.type === 'SUMMARY') || null)
const adviceCombination = computed(() => (advice.value || []).find(x => x.type === 'COMBINATION') || null)
const adviceSuggestions = computed(() => (advice.value || []).filter(x => x.type === 'SUGGESTION'))

async function loadSubjects() {
  try {
    const s = await api.subjects()
    subjects.value = {
      first: (s?.first || []).length ? s.first : ['物理', '历史'],
      second: (s?.second || []).length ? s.second : ['化学', '生物', '政治', '地理', '技术'],
    }
  } catch (e) {
    subjects.value = { first: ['物理', '历史'], second: ['化学', '生物', '政治', '地理', '技术'] }
    fail(e)
  }
}
async function loadMine() { try { mine.value = await api.mine() || [] } catch (e) { fail(e) } }
async function loadIntentions() { try { intentions.value = await api.intentions() || [] } catch (e) { fail(e) } }
async function loadGuidances() { try { guidances.value = await api.guidances() || [] } catch (e) { fail(e) } }
async function loadHistory() {
  try {
    histories.value = await api.histories({ action: historyFilter.action, limit: historyFilter.limit }) || []
    historyStats.value = await api.historyStats() || {}
  } catch (e) { fail(e) }
}
async function loadApprovals() { try { approvals.value = await api.approvals() || [] } catch (e) { fail(e) } }
async function loadStats() { try { stats.value = await api.statistics() || {} } catch (e) { fail(e) } }
async function loadAdvice() { try { advice.value = await api.advice() || [] } catch (e) { fail(e) } }

function selectTab(k) {
  tab.value = k
  notice.value = ''
  const loaders = {
    my: loadMine, submit: loadSubjects, intention: loadIntentions, guidance: loadGuidances,
    history: loadHistory, approval: loadApprovals, statistics: loadStats,
    major: loadSubjects, advice: loadAdvice,
  }
  loaders[k]?.()
}

async function submitSelection() {
  if (!subForm.firstSubject) { toast('请选择首选科目'); return }
  if (!subForm.secondSubjects.length) { toast('请至少选择一门再选科目'); return }
  busy.value = true
  try {
    await api.submit({ firstSubject: subForm.firstSubject, secondSubjects: subForm.secondSubjects.join(','), note: subForm.note })
    subForm.firstSubject = ''; subForm.secondSubjects = []; subForm.note = ''
    toast('选科已提交，状态为待审批')
    await loadMine()
  } catch (e) { fail(e) } finally { busy.value = false }
}
async function confirmSel(id) {
  try { await api.confirm(id); toast('已确认选科'); await loadMine() } catch (e) { fail(e) }
}
function openEdit(r) {
  editingId.value = r.id
  editForm.firstSubject = r.firstSubject || ''
  editForm.secondSubjects = r.secondSubjects || ''
  editForm.note = r.note || ''
  if (!subjects.value.first.includes(editForm.firstSubject) && editForm.firstSubject) {
    subjects.value.first = [editForm.firstSubject, ...subjects.value.first]
  }
}
async function saveEdit() {
  busy.value = true
  try {
    await api.modify(editingId.value, { firstSubject: editForm.firstSubject, secondSubjects: editForm.secondSubjects, note: editForm.note })
    editingId.value = null
    toast('已修改，状态回到待审批')
    await loadMine()
  } catch (e) { fail(e) } finally { busy.value = false }
}
async function cancelSel(r) {
  const reason = window.prompt('请输入退选原因', '个人规划调整')
  if (reason === null) return
  try { await api.cancel(r.id, reason); toast('已退选'); await loadMine() } catch (e) { fail(e) }
}

async function addIntention() {
  try {
    await api.addIntention({ ...intForm })
    Object.assign(intForm, { targetMajor: '', targetSchool: '', reason: '', status: 'DRAFT' })
    toast('意向已添加'); await loadIntentions()
  } catch (e) { fail(e) }
}
async function setIntentionStatus(id, status) {
  try { await api.intentionStatus(id, status); toast('意向状态已流转'); await loadIntentions() } catch (e) { fail(e) }
}

async function addGuidance() {
  try {
    await api.addGuidance({ ...guiForm })
    Object.assign(guiForm, { title: '', content: '', strength: '', weakness: '', opportunity: '', threat: '', status: 'ONGOING' })
    toast('指导记录已添加'); await loadGuidances()
  } catch (e) { fail(e) }
}
async function setGuidanceStatus(id, status) {
  try { await api.guidanceStatus(id, status); toast('指导状态已流转'); await loadGuidances() } catch (e) { fail(e) }
}

async function decide(id, decision) {
  try {
    await api.decide(id, decision, approvalComments[id] || '')
    toast(decision === 'APPROVE' ? '已通过审批' : '已驳回申请')
    await loadApprovals(); await loadMine(); await loadHistory()
  } catch (e) { fail(e) }
}

async function runMatch() {
  try { majorMatches.value = await api.majorMatch({ firstSubject: matchForm.firstSubject, subjects: matchForm.subjects }) || [] }
  catch (e) { fail(e) }
}

onMounted(() => { loadSubjects(); loadMine() })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SELECTION" title="选科中心"
      description="我的选科、提交、意向、指导、变更历史、审批、统计分析、专业匹配与智能建议，全部基于真实数据聚合。"
      :actions="[{ label: '志愿填报', icon: '→', color: 'amber', to: '/planning/volunteer' }]" />

    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }" @click="selectTab(t.key)">{{ t.label }}</button>
    </div>
    <p v-if="notice" class="mb-3 text-[12px] muted">{{ notice }}</p>

    <div v-if="tab === 'my'" class="glass p-5">
      <h2 class="section-title mb-3">我的选科</h2>
      <p v-if="!mine.length" class="text-[12px] muted">暂无选科记录，请到「提交选科」发起。</p>
      <div v-for="r in mine" :key="r.id" class="glass-flat mb-3 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div>
            <span class="pill" :class="statusClass(r.status)">{{ statusText(r.status) }}</span>
            <span class="ml-2 text-[14px] font-semibold ink">{{ comboName(r) }}</span>
          </div>
          <span class="text-[11px] faint">编号 {{ r.id }}</span>
        </div>
        <p class="mt-2 text-[12px] muted">首选 {{ r.firstSubject || '—' }} · 再选 {{ r.secondSubjects || '—' }}</p>
        <p v-if="r.note" class="mt-1 text-[12px] muted">理由：{{ r.note }}</p>
        <p v-if="r.confirmedAt" class="mt-1 text-[11px] faint">确认时间：{{ r.confirmedAt }}</p>
        <p v-if="r.latestHistory" class="mt-1 text-[11px] faint">最近变更：{{ r.latestHistory.action }} · {{ r.latestHistory.detail }}</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button class="btn btn-mint" :disabled="busy" @click="confirmSel(r.id)">确认</button>
          <button class="btn btn-violet" @click="openEdit(r)">修改</button>
          <button class="btn btn-rose" @click="cancelSel(r)">退选</button>
        </div>
        <div v-if="editingId === r.id" class="mt-4 form-grid">
          <label class="block"><span class="field-label">首选科目</span>
            <select v-model="editForm.firstSubject" class="field w-full">
              <option v-for="s in subjects.first" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
          <label class="block"><span class="field-label">再选科目（逗号分隔）</span>
            <input v-model="editForm.secondSubjects" class="field w-full" placeholder="化学,生物" />
          </label>
          <label class="block span-2"><span class="field-label">理由</span>
            <textarea v-model="editForm.note" class="field w-full" rows="2"></textarea>
          </label>
          <div class="span-2 flex gap-2">
            <button class="btn btn-blue" :disabled="busy" @click="saveEdit">保存修改</button>
            <button class="btn btn-slate" @click="editingId = null">取消</button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="tab === 'submit'" class="glass p-5">
      <h2 class="section-title mb-3">提交选科</h2>
      <div class="form-grid">
        <label class="block"><span class="field-label">首选科目 *</span>
          <select v-model="subForm.firstSubject" class="field w-full">
            <option value="">请选择</option>
            <option v-for="s in subjects.first" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
        <div class="block"><span class="field-label">再选科目 *（可多选）</span>
          <div class="mt-2">
            <label v-for="s in subjects.second" :key="s" class="inline-flex items-center gap-1 mr-3 mb-1 text-[12px] muted">
              <input type="checkbox" :value="s" v-model="subForm.secondSubjects" /> {{ s }}
            </label>
          </div>
        </div>
        <label class="block span-2"><span class="field-label">选科理由</span>
          <textarea v-model="subForm.note" class="field w-full" rows="3" placeholder="兴趣、优势学科、目标专业等"></textarea>
        </label>
      </div>
      <div class="mt-4"><button class="btn btn-blue" :disabled="busy" @click="submitSelection">提交选科</button></div>
    </div>

    <div v-else-if="tab === 'intention'" class="glass p-5">
      <h2 class="section-title mb-3">选科意向</h2>
      <div class="crud-toolbar flex-wrap">
        <input v-model="intForm.targetMajor" class="field max-w-[180px]" placeholder="目标专业" />
        <input v-model="intForm.targetSchool" class="field max-w-[180px]" placeholder="目标院校" />
        <input v-model="intForm.reason" class="field max-w-[220px]" placeholder="理由" />
        <select v-model="intForm.status" class="field max-w-[140px]">
          <option v-for="s in INTENTION_FLOW" :key="s" :value="s">{{ statusText(s) }}</option>
        </select>
        <button class="btn btn-blue" @click="addIntention">新增意向</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>编号</th><th>目标专业</th><th>目标院校</th><th>状态</th><th>状态流转</th></tr></thead>
        <tbody>
          <tr v-for="r in intentions" :key="r.id">
            <td>{{ r.id }}</td><td>{{ r.targetMajor || '—' }}</td><td>{{ r.targetSchool || '—' }}</td>
            <td><span class="pill" :class="statusClass(r.status)">{{ statusText(r.status) }}</span></td>
            <td>
              <button v-for="s in INTENTION_FLOW" :key="s" class="btn btn-slate mr-1 mb-1" @click="setIntentionStatus(r.id, s)">{{ statusText(s) }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!intentions.length" class="mt-3 text-[12px] muted">暂无意向数据</p>
    </div>

    <div v-else-if="tab === 'guidance'" class="glass p-5">
      <h2 class="section-title mb-3">选科指导（SWOT）</h2>
      <div class="form-grid">
        <label class="block"><span class="field-label">标题 *</span><input v-model="guiForm.title" class="field w-full" placeholder="指导主题" /></label>
        <label class="block"><span class="field-label">状态</span>
          <select v-model="guiForm.status" class="field w-full">
            <option v-for="s in GUIDANCE_FLOW" :key="s" :value="s">{{ statusText(s) }}</option>
          </select>
        </label>
        <label class="block span-2"><span class="field-label">指导内容</span><textarea v-model="guiForm.content" class="field w-full" rows="2"></textarea></label>
        <label class="block"><span class="field-label">优势 S</span><input v-model="guiForm.strength" class="field w-full" /></label>
        <label class="block"><span class="field-label">劣势 W</span><input v-model="guiForm.weakness" class="field w-full" /></label>
        <label class="block"><span class="field-label">机会 O</span><input v-model="guiForm.opportunity" class="field w-full" /></label>
        <label class="block"><span class="field-label">威胁 T</span><input v-model="guiForm.threat" class="field w-full" /></label>
      </div>
      <div class="mt-4"><button class="btn btn-violet" @click="addGuidance">新增指导记录</button></div>
      <div class="mt-5 crud-grid">
        <div v-for="r in guidances" :key="r.id" class="crud-card">
          <div class="flex items-center justify-between gap-2">
            <h3 class="text-[14px] font-semibold ink">{{ r.title }}</h3>
            <span class="pill" :class="statusClass(r.status)">{{ statusText(r.status) }}</span>
          </div>
          <p class="mt-2 whitespace-pre-line text-[12px] leading-6 muted">{{ r.content || '—' }}</p>
          <div class="mt-3 flex flex-wrap gap-1">
            <button v-for="s in GUIDANCE_FLOW" :key="s" class="btn btn-slate" @click="setGuidanceStatus(r.id, s)">{{ statusText(s) }}</button>
          </div>
        </div>
      </div>
      <p v-if="!guidances.length" class="mt-3 text-[12px] muted">暂无指导记录</p>
    </div>

    <div v-else-if="tab === 'history'" class="glass p-5">
      <h2 class="section-title mb-3">变更历史</h2>
      <div class="grid gap-3 md:grid-cols-5 mb-5">
        <div v-for="c in [{ k: 'total', l: '总计' }, { k: 'pending', l: '待审批' }, { k: 'approved', l: '已通过' }, { k: 'rejected', l: '已驳回' }, { k: 'confirmed', l: '已确认' }]" :key="c.k" class="glass-flat p-3">
          <p class="text-[11px] faint">{{ c.l }}</p>
          <p class="stat-num mt-1">{{ historyStats[c.k] || 0 }}</p>
        </div>
      </div>
      <div class="crud-toolbar">
        <select v-model="historyFilter.action" class="field max-w-[180px]" @change="loadHistory">
          <option value="">全部动作</option>
          <option>SUBMIT</option><option>CONFIRM</option><option>MODIFY</option><option>CANCEL</option><option>APPROVE</option><option>REJECT</option>
        </select>
        <input v-model.number="historyFilter.limit" class="field max-w-[120px]" type="number" min="1" placeholder="条数" @change="loadHistory" />
        <button class="btn btn-blue" @click="loadHistory">刷新</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>时间</th><th>动作</th><th>操作人</th><th>选科 ID</th><th>详情</th></tr></thead>
        <tbody>
          <tr v-for="h in histories" :key="h.id">
            <td>{{ h.createdAt || '—' }}</td><td><span class="pill pill-sky">{{ h.action }}</span></td>
            <td>{{ h.operator || '—' }}</td><td>{{ h.selectionId || '—' }}</td><td>{{ h.detail || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!histories.length" class="mt-3 text-[12px] muted">暂无变更记录</p>
    </div>

    <div v-else-if="tab === 'approval'" class="glass p-5">
      <h2 class="section-title mb-3">审批管理（管理员）</h2>
      <p class="mb-4 text-[12px] muted">仅列出待审批(PENDING)申请；通过 / 驳回会写入变更历史与审计日志。</p>
      <div v-for="r in approvals" :key="r.id" class="glass-flat mb-3 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="text-[14px] font-semibold ink">{{ comboName(r) }}</span>
          <span class="text-[11px] faint">编号 {{ r.id }} · 归属用户 {{ r.userId }}</span>
        </div>
        <p class="mt-1 text-[12px] muted">首选 {{ r.firstSubject || '—' }} · 再选 {{ r.secondSubjects || '—' }} · 备注 {{ r.note || '—' }}</p>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <input v-model="approvalComments[r.id]" class="field max-w-[260px]" placeholder="审批意见 / 原因" />
          <button class="btn btn-mint" @click="decide(r.id, 'APPROVE')">通过</button>
          <button class="btn btn-rose" @click="decide(r.id, 'REJECT')">驳回</button>
        </div>
      </div>
      <p v-if="!approvals.length" class="text-[12px] muted">暂无待审批申请</p>
    </div>

    <div v-else-if="tab === 'statistics'" class="glass p-5">
      <h2 class="section-title mb-3">统计分析</h2>
      <p class="mb-4 text-[12px] muted">选科总数 <span class="pill pill-sky">{{ stats.total || 0 }}</span></p>
      <div class="grid gap-5 md:grid-cols-2">
        <div class="glass-flat p-4">
          <h3 class="eyebrow mb-3">热门组合 TOP5</h3>
          <div v-for="(c, i) in (stats.hotCombinations || [])" :key="i" class="flex items-center gap-3 mb-2">
            <span class="text-[12px] ink w-28 truncate">{{ c.label }}</span>
            <div class="flex-1 rounded" style="height:10px;background:var(--surface-2);overflow:hidden">
              <div :style="{ width: barWidth(c.count, maxHot) + '%', height: '100%', background: 'var(--btn-blue)' }"></div>
            </div>
            <span class="text-[12px] muted w-8 text-right">{{ c.count }}</span>
          </div>
          <p v-if="!(stats.hotCombinations || []).length" class="text-[12px] muted">暂无数据</p>
        </div>
        <div class="glass-flat p-4">
          <h3 class="eyebrow mb-3">首选科目分布</h3>
          <span v-for="(v, k) in (stats.firstSubjectDistribution || {})" :key="k" class="pill pill-mint mr-2 mb-2">{{ k }} · {{ v }}</span>
          <p v-if="!Object.keys(stats.firstSubjectDistribution || {}).length" class="text-[12px] muted">暂无数据</p>
        </div>
        <div class="glass-flat p-4">
          <h3 class="eyebrow mb-3">按月提交趋势</h3>
          <div v-for="(v, k) in (stats.monthlyTrend || {})" :key="k" class="flex items-center gap-3 mb-2">
            <span class="text-[12px] ink w-20">{{ k }}</span>
            <div class="flex-1 rounded" style="height:10px;background:var(--surface-2);overflow:hidden">
              <div :style="{ width: barWidth(v, maxOf(stats.monthlyTrend)) + '%', height: '100%', background: 'var(--btn-violet)' }"></div>
            </div>
            <span class="text-[12px] muted w-8 text-right">{{ v }}</span>
          </div>
          <p v-if="!Object.keys(stats.monthlyTrend || {}).length" class="text-[12px] muted">暂无数据</p>
        </div>
        <div class="glass-flat p-4">
          <h3 class="eyebrow mb-3">状态分布</h3>
          <span v-for="(v, k) in (stats.statusDistribution || {})" :key="k" class="pill pill-amber mr-2 mb-2">{{ k }} · {{ v }}</span>
          <p v-if="!Object.keys(stats.statusDistribution || {}).length" class="text-[12px] muted">暂无数据</p>
        </div>
      </div>
    </div>

    <div v-else-if="tab === 'major'" class="glass p-5">
      <h2 class="section-title mb-3">专业推荐（按选科匹配）</h2>
      <div class="crud-toolbar">
        <select v-model="matchForm.firstSubject" class="field max-w-[180px]">
          <option value="">首选科目</option>
          <option v-for="s in subjects.first" :key="s" :value="s">{{ s }}</option>
        </select>
        <input v-model="matchForm.subjects" class="field max-w-[260px]" placeholder="其他科目，逗号分隔，如：化学,生物" />
        <button class="btn btn-violet" @click="runMatch">计算匹配</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>代码</th><th>专业</th><th>选科要求</th><th>命中</th><th>命中率</th></tr></thead>
        <tbody>
          <tr v-for="m in majorMatches" :key="m.id">
            <td>{{ m.majorCode || '—' }}</td><td>{{ m.majorName || '—' }}</td>
            <td>{{ (m.requiredList || []).join('、') || m.requiredSubjects || '—' }}</td>
            <td>{{ m.hitCount }}/{{ m.requiredCount }}</td>
            <td>
              <div class="flex items-center gap-2">
                <div class="rounded" style="height:8px;width:90px;background:var(--surface-2);overflow:hidden">
                  <div :style="{ width: Math.round((m.matchRate || 0) * 100) + '%', height: '100%', background: 'var(--btn-mint)' }"></div>
                </div>
                <span class="text-[12px] muted">{{ Math.round((m.matchRate || 0) * 100) }}%</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!majorMatches.length" class="mt-3 text-[12px] muted">选择科目后点击「计算匹配」查看结果</p>
    </div>

    <div v-else class="glass p-5">
      <h2 class="section-title mb-3">智能建议</h2>
      <p class="mb-4 text-[12px] muted">基于成绩记录各科平均分找出薄弱 / 强项学科，结合专业选科要求给出组合建议（结构化数据，非固定文案）。</p>
      <div v-if="adviceSummary" class="glass-flat mb-5 p-4">
        <div class="flex flex-wrap gap-4">
          <span class="pill pill-sky">成绩科目数 {{ adviceSummary.scoreCount || 0 }}</span>
          <span class="pill pill-violet">总平均分 {{ adviceSummary.overallAverage }}</span>
        </div>
        <p class="mt-3 text-[12px] ink">薄弱学科</p>
        <div class="mt-1">
          <span v-for="w in (adviceSummary.weakSubjects || [])" :key="w.subject" class="pill pill-rose mr-2 mb-2">{{ w.subject }} · 均分 {{ w.avgScore }}</span>
          <span v-if="!(adviceSummary.weakSubjects || []).length" class="text-[12px] muted">暂无明显薄弱学科</span>
        </div>
        <p class="mt-3 text-[12px] ink">强项学科</p>
        <div class="mt-1">
          <span v-for="s in (adviceSummary.strongSubjects || [])" :key="s" class="pill pill-mint mr-2 mb-2">{{ s }}</span>
          <span v-if="!(adviceSummary.strongSubjects || []).length" class="text-[12px] muted">暂无成绩数据</span>
        </div>
      </div>
      <div v-if="adviceCombination" class="glass-flat mb-5 p-4">
        <h3 class="eyebrow mb-2">推荐组合（按专业需求度排序，规避薄弱学科）</h3>
        <p class="text-[13px] ink">首选 {{ adviceCombination.firstSubject || '—' }} · 再选 {{ adviceCombination.secondSubjects || '—' }}</p>
        <div class="mt-2 flex flex-wrap gap-2">
          <span class="pill pill-mint">可覆盖专业 {{ adviceCombination.coveredMajorCount || 0 }}</span>
          <span class="pill pill-sky">候选学科 {{ adviceCombination.candidateCount || 0 }}</span>
          <span v-for="s in (adviceCombination.weakSubjectsToAvoid || [])" :key="s" class="pill pill-rose">建议规避 {{ s }}</span>
        </div>
        <div class="mt-3">
          <p class="mb-1 text-[11px] faint">学科需求度（被多少专业要求）</p>
          <span v-for="(v, k) in (adviceCombination.subjectDemand || {})" :key="k" class="pill pill-neutral mr-2 mb-2">{{ k }} · {{ v }}</span>
        </div>
      </div>
      <div class="crud-grid">
        <div v-for="(g, i) in adviceSuggestions" :key="i" class="crud-card">
          <div class="flex items-start justify-between gap-2">
            <h3 class="text-[14px] font-semibold ink">{{ g.majorName }}</h3>
            <span class="pill pill-sky">{{ g.matchingScore }}</span>
          </div>
          <p class="mt-1 text-[11px] faint">{{ g.majorCode }}</p>
          <p class="mt-2 text-[12px] muted">要求：{{ (g.requiredSubjects || []).join('、') }}</p>
          <p class="mt-1 text-[12px] muted">强项命中：{{ (g.strongSubjects || []).join('、') || '—' }}（{{ g.hitCount }}/{{ g.requiredCount }}）</p>
          <p class="mt-1 text-[12px] muted">薄弱风险：{{ (g.weakSubjects || []).join('、') || '无' }}（{{ g.riskCount }}）</p>
          <div class="mt-3">
            <span class="pill pill-mint">建议组合：{{ g.recommendedFirstSubject }} + {{ g.recommendedSecondSubjects || '—' }}</span>
          </div>
        </div>
      </div>
      <p v-if="!adviceSuggestions.length" class="mt-3 text-[12px] muted">录入成绩或补充专业选科要求后即可生成组合建议</p>
    </div>
  </section>
</template>
