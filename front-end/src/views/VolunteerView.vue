<script setup>
import { onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const tab = ref('search')
const tabs = [
  { key: 'search', label: '院校检索' }, { key: 'recommend', label: '智能推荐' },
  { key: 'simulate', label: '模拟填报' }, { key: 'plans', label: '方案管理' },
  { key: 'details', label: '志愿明细' },
]
const plansCfg = {
  eyebrow: 'VOLUNTEER / PLANS', title: '志愿方案', description: '方案按账号隔离，可保存多套用于对比。', color: 'amber',
  api: R.planning.volunteerPlans,
  fields: [{ key: 'title', label: '方案名称', required: true }, { key: 'planYear', label: '年份' },
           { key: 'province', label: '省份' }, { key: 'status', label: '状态', type: 'select', options: ['草稿', '已提交', '已归档'] }],
  columns: [{ key: 'title', label: '名称' }, { key: 'planYear', label: '年份' }, { key: 'province', label: '省份' }, { key: 'status', label: '状态' }]
}
const detailsCfg = {
  eyebrow: 'VOLUNTEER / DETAILS', title: '志愿明细', description: '逐条维护院校、专业、优先级与冲稳保档位。', color: 'blue',
  api: R.planning.volunteerDetails,
  fields: [{ key: 'planId', label: '方案 ID', type: 'number' }, { key: 'universityName', label: '院校' },
           { key: 'majorName', label: '专业' }, { key: 'priority', label: '优先级', type: 'number' },
           { key: 'chanceLevel', label: '档位', type: 'select', options: ['冲', '稳', '保'] },
           { key: 'scoreLine', label: '参考分', type: 'number' }, { key: 'note', label: '备注', type: 'textarea', span: 2 }],
  columns: [{ key: 'priority', label: '序' }, { key: 'universityName', label: '院校' }, { key: 'majorName', label: '专业' }, { key: 'chanceLevel', label: '档位' }]
}

const keyword = ref(''), score = ref(600), major = ref('')
const majors = ref([]), recommend = ref([]), sims = ref([]), report = ref(null)
const planId = ref(''), msg = ref('')

async function search() {
  try { majors.value = (await R.planning.majorRequirements.list({ keyword: keyword.value })) || [] } catch (e) { msg.value = e.message }
}
async function runRecommend() {
  try { recommend.value = (await feature.recommendVolunteer({ major: major.value, score: Number(score.value) })) || [] } catch (e) { msg.value = e.message }
}
async function runSimulate() {
  if (!planId.value) { msg.value = '请先填写方案 ID'; return }
  try { sims.value = (await feature.simulateVolunteer({ planId: Number(planId.value) })) || []; report.value = await feature.volunteerReport(Number(planId.value)) }
  catch (e) { msg.value = e.message }
}
onMounted(search)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / VOLUNTEER" title="志愿填报"
      description="院校检索、智能推荐、模拟填报、方案管理与志愿明细，全部基于同一份数据。"
      :actions="[{ label: '方案对比', icon: '◫', color: 'violet', to: '/planning/compare' }, { label: '选科中心', icon: '◎', color: 'blue', to: '/planning/selection' }]" />
    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ 'is-active': tab === t.key }" @click="tab = t.key">{{ t.label }}</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-rose-500">{{ msg }}</p>

    <div v-if="tab === 'search'" class="glass p-6">
      <div class="crud-toolbar">
        <input v-model="keyword" class="field max-w-[280px]" placeholder="院校 / 专业关键词…" @keyup.enter="search" />
        <button class="btn btn-blue" @click="search"><span class="btn-glyph">⌕</span> 检索</button>
      </div>
      <table class="data-table w-full">
        <thead><tr><th>代码</th><th>专业</th><th>门类</th><th>层次</th><th>选科要求</th></tr></thead>
        <tbody><tr v-for="m in majors" :key="m.id"><td>{{ m.majorCode }}</td><td>{{ m.majorName }}</td><td>{{ m.category }}</td><td>{{ m.level }}</td><td>{{ m.requiredSubjects }}</td></tr></tbody>
      </table>
      <p v-if="!majors.length" class="mt-3 text-[12px] muted">暂无匹配数据</p>
    </div>

    <div v-else-if="tab === 'recommend'" class="glass p-6">
      <div class="crud-toolbar">
        <input v-model="major" class="field max-w-[200px]" placeholder="意向专业（可空）" />
        <input v-model="score" class="field max-w-[140px]" type="number" placeholder="分数" />
        <button class="btn btn-violet" @click="runRecommend"><span class="btn-glyph">◎</span> 生成推荐</button>
      </div>
      <div class="crud-grid">
        <div v-for="r in recommend" :key="r.id" class="crud-card">
          <span class="pill" :class="r.chanceLevel === '保' ? 'pill-mint' : r.chanceLevel === '冲' ? 'pill-rose' : 'pill-amber'">{{ r.chanceLevel }}</span>
          <h3 class="mt-2 text-[14px] font-semibold ink">{{ r.majorName }}</h3>
          <p class="mt-1 text-[12px] muted">{{ r.category }} · {{ r.requiredSubjects }}</p>
          <p class="mt-2 text-[11px] faint">参考分 {{ r.suggestedScore }}</p>
        </div>
      </div>
      <p v-if="!recommend.length" class="mt-3 text-[12px] muted">点击「生成推荐」查看结果</p>
    </div>

    <div v-else-if="tab === 'simulate'" class="glass p-6">
      <div class="crud-toolbar">
        <input v-model="planId" class="field max-w-[180px]" type="number" placeholder="方案 ID" />
        <button class="btn btn-amber" @click="runSimulate"><span class="btn-glyph">▶</span> 批量模拟</button>
      </div>
      <div v-if="report" class="mb-4 flex flex-wrap gap-2">
        <span v-for="(v, k) in (report.distribution || {})" :key="k" class="pill pill-sky">{{ k }}档 · {{ v }} 条</span>
      </div>
      <div v-for="s in sims" :key="s.id" class="glass-flat mb-2 p-3">
        <span class="pill pill-mint">{{ s.result }}</span>
        <span class="ml-2 text-[12px] muted">{{ s.analysis }}</span>
        <span class="ml-2 text-[11px] faint">概率 {{ Math.round((s.probability || 0) * 100) }}%</span>
      </div>
      <p v-if="!sims.length" class="text-[12px] muted">填写方案 ID 后运行模拟</p>
    </div>

    <ResourcePage v-else-if="tab === 'plans'" v-bind="plansCfg" :key="tab" />
    <ResourcePage v-else v-bind="detailsCfg" :key="tab" />
  </section>
</template>
