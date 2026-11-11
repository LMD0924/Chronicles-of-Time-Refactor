<script setup>
import { onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const tab = ref('my')
const tabs = [
  { key: 'my', label: '我的选科' }, { key: 'intention', label: '选科意向' },
  { key: 'guidance', label: '选科指导' }, { key: 'history', label: '选科历史' },
  { key: 'approval', label: '审批面板' }, { key: 'statistics', label: '统计面板' },
  { key: 'recommend', label: '智能推荐' }, { key: 'grading', label: '等级量表' },
  { key: 'major', label: '专业管理' },
]

const configs = {
  my: { title: '我的选科', color: 'blue', api: R.planning.selections, fields: [
    { key: 'combinationId', label: '组合 ID', type: 'number' }, { key: 'status', label: '状态', type: 'select', options: ['草稿', '待审批', '已确认', '已取消'] },
    { key: 'confirmedAt', label: '确认日期', type: 'date' }, { key: 'note', label: '备注', type: 'textarea', span: 2 }],
    columns: [{ key: 'id', label: '编号' }, { key: 'combinationId', label: '组合' }, { key: 'status', label: '状态' }, { key: 'confirmedAt', label: '确认日期', type: 'date' }] },
  intention: { title: '选科意向', color: 'mint', api: R.planning.intentions, fields: [
    { key: 'targetMajor', label: '目标专业' }, { key: 'targetSchool', label: '目标院校' },
    { key: 'reason', label: '理由', type: 'textarea', span: 2 }, { key: 'status', label: '状态', type: 'select', options: ['草稿', '已提交', '已通过'] }],
    columns: [{ key: 'targetMajor', label: '专业' }, { key: 'targetSchool', label: '院校' }, { key: 'status', label: '状态' }] },
  guidance: { title: '选科指导', color: 'violet', api: R.planning.guidances, fields: [
    { key: 'title', label: '标题', required: true }, { key: 'content', label: '内容', type: 'textarea', span: 2 },
    { key: 'status', label: '状态', type: 'select', options: ['草稿', '已发布'] }],
    columns: [{ key: 'title', label: '标题' }, { key: 'status', label: '状态' }, { key: 'content', label: '内容' }] },
  history: { title: '选科历史', color: 'slate', api: R.planning.selectionHistories, readonly: true, fields: [
    { key: 'selectionId', label: '选科 ID', type: 'number' }, { key: 'action', label: '动作' }, { key: 'operator', label: '操作人' }, { key: 'detail', label: '详情' }],
    columns: [{ key: 'action', label: '动作' }, { key: 'operator', label: '操作人' }, { key: 'detail', label: '详情' }] },
  grading: { title: '等级量表', color: 'amber', api: R.planning.gradingScales, fields: [
    { key: 'subjectId', label: '学科 ID', type: 'number' }, { key: 'level', label: '等级' },
    { key: 'minScore', label: '最低分', type: 'number' }, { key: 'maxScore', label: '最高分', type: 'number' },
    { key: 'ratio', label: '比例', type: 'number' }, { key: 'active', label: '启用', type: 'checkbox' }],
    columns: [{ key: 'level', label: '等级' }, { key: 'minScore', label: '最低' }, { key: 'maxScore', label: '最高' }, { key: 'ratio', label: '比例' }] },
  major: { title: '专业要求', color: 'teal', api: R.planning.majorRequirements, fields: [
    { key: 'majorCode', label: '专业代码' }, { key: 'majorName', label: '专业名称' }, { key: 'category', label: '门类' },
    { key: 'level', label: '层次' }, { key: 'requiredSubjects', label: '选科要求' }],
    columns: [{ key: 'majorCode', label: '代码' }, { key: 'majorName', label: '专业' }, { key: 'requiredSubjects', label: '要求' }] },
}

const approvals = ref([])
const stats = ref({})
const first = ref('')
const recommend = ref(null)
const msg = ref('')

async function loadApprovals() {
  try { approvals.value = (await R.planning.selections.all()) || [] } catch (e) { msg.value = e.message }
}
async function loadStats() {
  try { stats.value = (await feature.selectionStats()) || {} } catch (e) { msg.value = e.message }
}
async function decide(row, status) {
  try { await feature.confirmSelection({ id: row.id, status, operator: 'admin' }); await loadApprovals(); msg.value = '已更新：' + status }
  catch (e) { msg.value = e.message }
}
async function runRecommend() {
  try { recommend.value = await feature.recommendSelection({ firstSubject: first.value }) } catch (e) { msg.value = e.message }
}
onMounted(() => { loadApprovals(); loadStats() })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SELECTION" title="选科中心"
      description="我的选科、意向、指导、历史、审批、统计、等级量表、专业管理与智能推荐集中在一个页面，用标签切换。"
      :actions="[{ label: '学科与组合', icon: '⌘', color: 'sky', to: '/planning/subjects' }, { label: '志愿填报', icon: '→', color: 'amber', to: '/planning/volunteer' }]" />

    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }" @click="tab = t.key">{{ t.label }}</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] muted">{{ msg }}</p>

    <ResourcePage v-if="configs[tab]" v-bind="configs[tab]" :key="tab" :eyebrow="`SELECTION / ${tab.toUpperCase()}`"
                  :description="`${configs[tab].title}的新增、编辑与删除都会写入时间线。`" />

    <div v-else-if="tab === 'approval'" class="glass p-5">
      <h2 class="section-title mb-3">待处理选科</h2>
      <table class="data-table w-full">
        <thead><tr><th>编号</th><th>组合</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in approvals" :key="r.id">
            <td>{{ r.id }}</td><td>{{ r.combinationId }}</td><td>{{ r.status }}</td>
            <td>
              <button class="btn btn-mint" @click="decide(r, '已确认')">通过</button>
              <button class="btn btn-rose ml-1" @click="decide(r, '已取消')">驳回</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!approvals.length" class="mt-3 text-[12px] muted">暂无待处理记录</p>
    </div>

    <div v-else-if="tab === 'statistics'" class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title">选科总数</h2>
        <p class="stat-num mt-3" v-count="stats.total || 0">0</p>
      </div>
      <div class="glass p-6">
        <h2 class="section-title mb-3">按状态分布</h2>
        <span v-for="(v, k) in (stats.byStatus || {})" :key="k" class="pill pill-sky mr-2">{{ k }} · {{ v }}</span>
        <p v-if="!Object.keys(stats.byStatus || {}).length" class="text-[12px] muted">暂无数据</p>
      </div>
    </div>

    <div v-else class="glass p-6">
      <h2 class="section-title mb-3">智能推荐</h2>
      <div class="flex flex-wrap gap-2">
        <select v-model="first" class="field"><option value="">全部首选科目</option><option>物理</option><option>历史</option></select>
        <button class="btn btn-violet" @click="runRecommend"><span class="btn-glyph">◎</span> 生成推荐</button>
      </div>
      <div v-if="recommend" class="mt-5 grid gap-4 md:grid-cols-2">
        <div><h3 class="eyebrow mb-2">匹配组合</h3>
          <div v-for="c in recommend.combinations" :key="c.id" class="glass-flat mb-2 p-3 text-[12px]">{{ c.label || c.firstSubject }} · {{ c.secondSubjects }}</div>
          <p v-if="!recommend.combinations.length" class="text-[12px] muted">暂无匹配组合</p>
        </div>
        <div><h3 class="eyebrow mb-2">可选专业</h3>
          <div v-for="m in recommend.majors" :key="m.id" class="glass-flat mb-2 p-3 text-[12px]">{{ m.majorName }} · {{ m.requiredSubjects }}</div>
          <p v-if="!recommend.majors.length" class="text-[12px] muted">暂无匹配专业</p>
        </div>
      </div>
    </div>
  </section>
</template>
