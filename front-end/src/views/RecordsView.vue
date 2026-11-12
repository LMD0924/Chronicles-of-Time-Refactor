<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const heat = ref([])
const facets = ref({ categories: [], periods: [], tags: [] })
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const filters = reactive({ q: '', category: '', period: '', tag: '', dateFrom: '', dateTo: '' })
const categories = ['学习', '规划', '生活', '工作', '复盘']
const periods = ['早晨', '上午', '下午', '晚上', '深夜']
const moods = ['充实', '平静', '开心', '疲惫', '焦虑', '专注', '其他']
const numberFields = ['studyHours', 'sleepHours', 'exerciseHours', 'happinessIndex']

const facetOptions = computed(() => ({
  categories: [...new Set([...categories, ...facets.value.categories])],
  periods: [...new Set([...periods, ...facets.value.periods])],
  tags: facets.value.tags || [],
}))

const stats = computed(() => {
  const month = ymd(new Date()).slice(0, 7)
  return {
    total: rows.value.length,
    month: rows.value.filter(r => String(r.recordDate || '').startsWith(month)).length,
    hours: Math.round(rows.value.reduce((s, r) => s + (Number(r.studyHours) || 0), 0) * 10) / 10,
    happiness: rows.value.length
      ? Math.round(rows.value.reduce((s, r) => s + (Number(r.happinessIndex) || 0), 0) / rows.value.length * 10) / 10 : 0,
  }
})

const filtered = computed(() => {
  const kw = filters.q.trim().toLowerCase()
  return rows.value.filter(r => {
    if (kw && !Object.values(r).some(v => v != null && String(v).toLowerCase().includes(kw))) return false
    if (filters.category && r.category !== filters.category) return false
    if (filters.period && r.period !== filters.period) return false
    if (filters.tag && !String(r.tags || '').split(/[,，;；\s]+/).includes(filters.tag)) return false
    const d = String(r.recordDate || '').slice(0, 10)
    if (filters.dateFrom && d < filters.dateFrom) return false
    if (filters.dateTo && d > filters.dateTo) return false
    return true
  })
})

function ymd(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
const heatDays = computed(() => {
  const map = new Map(heat.value.map(h => [String(h.date).slice(0, 10), h]))
  const out = []
  const today = new Date()
  for (let i = 89; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i)
    const key = ymd(d)
    out.push({ date: key, count: map.get(key)?.count || 0, studyHours: map.get(key)?.studyHours || 0 })
  }
  return out
})
const heatLevel = (c) => c === 0 ? 0 : Math.min(4, c)

async function load() {
  loading.value = true; error.value = ''
  try {
    const [list, h, f] = await Promise.all([
      R.timeline.records.list(), feature.recordHeatmap(90), feature.timelineFacets(),
    ])
    rows.value = list || []
    heat.value = h || []
    facets.value = f || { categories: [], periods: [], tags: [] }
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  return {
    recordDate: ymd(new Date()), title: '', category: '学习', period: '', mood: '平静', tags: '',
    studyHours: '', sleepHours: '', exerciseHours: '', happinessIndex: 7, isMilestone: false, content: '',
  }
}
function openCreate() { editing.value = null; form.value = defaults(); showForm.value = true; notice.value = '' }
function openEdit(row) { editing.value = row.id; form.value = { ...row }; showForm.value = true; notice.value = '' }

async function submit() {
  if (!form.value.title?.trim()) { error.value = '请填写标题'; return }
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    numberFields.forEach(k => { payload[k] = payload[k] === '' || payload[k] == null ? null : Number(payload[k]) })
    if (editing.value) await R.timeline.records.update(editing.value, payload)
    else await R.timeline.records.save(payload)
    showForm.value = false
    notice.value = payload.isMilestone ? '已保存，并自动在时间线上生成一条里程碑' : '记录已保存'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(row) {
  if (!window.confirm(`确定删除「${row.title}」吗？由它自动生成的里程碑也会一并移除。`)) return
  try { await R.timeline.records.remove(row.id); notice.value = '记录已删除'; await load() }
  catch (e) { error.value = e.message }
}
function resetFilters() {
  Object.assign(filters, { q: '', category: '', period: '', tag: '', dateFrom: '', dateTo: '' })
}
function tagsOf(r) { return String(r.tags || '').split(/[,，;；\s]+/).filter(Boolean) }

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="TIMELINE / RECORDS" title="成长记录"
      description="每一天都可以是一条时间线节点。勾选「标为里程碑」会自动生成里程碑，取消勾选会回收它。"
      :actions="[
        { label: '写一条记录', icon: '＋', color: 'mint', click: openCreate },
        { label: '里程碑', icon: '★', color: 'amber', to: '/timeline/milestones' },
        { label: '时间线', icon: '⌁', color: 'violet', to: '/timeline' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '全部记录', value: stats.total }, { label: '本月记录', value: stats.month },
        { label: '累计学习(h)', value: stats.hours }, { label: '平均幸福指数', value: stats.happiness },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p>
        <p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <div class="glass mt-5 p-5" v-reveal="1">
      <h2 class="text-[14px] font-semibold ink">近 90 天记录热力</h2>
      <p class="mt-1 text-[11px] faint">色块越深，当天记录越多；悬停可看日期与学习时长。</p>
      <div class="mt-4 grid grid-cols-[repeat(15,minmax(0,1fr))] gap-1.5">
        <span v-for="d in heatDays" :key="d.date" class="block h-[14px] rounded-[3px] border border-hairline"
          :style="{ background: d.count ? `color-mix(in srgb, var(--mint) ${18 + heatLevel(d.count) * 18}%, var(--surface-2))` : 'var(--surface-2)' }"
          :title="`${d.date} · ${d.count} 条 · 学习 ${Math.round(d.studyHours * 10) / 10}h`" />
      </div>
    </div>

    <div class="glass mt-5 p-5" v-reveal="1">
      <div class="flex flex-wrap items-end gap-3">
        <label class="min-w-[180px] flex-1"><span class="field-label">搜索</span>
          <input v-model="filters.q" class="field w-full" type="search" placeholder="标题 / 正文 / 心情…" /></label>
        <label><span class="field-label">分类</span><select v-model="filters.category" class="field w-[130px]">
          <option value="">全部</option><option v-for="c in facetOptions.categories" :key="c">{{ c }}</option></select></label>
        <label><span class="field-label">时段</span><select v-model="filters.period" class="field w-[120px]">
          <option value="">全部</option><option v-for="p in facetOptions.periods" :key="p">{{ p }}</option></select></label>
        <label><span class="field-label">标签</span><select v-model="filters.tag" class="field w-[130px]">
          <option value="">全部</option><option v-for="t in facetOptions.tags" :key="t">{{ t }}</option></select></label>
        <label><span class="field-label">从</span><input v-model="filters.dateFrom" class="field w-[145px]" type="date" /></label>
        <label><span class="field-label">到</span><input v-model="filters.dateTo" class="field w-[145px]" type="date" /></label>
        <button class="btn btn-slate" type="button" @click="resetFilters"><span class="btn-glyph">↺</span> 重置</button>
      </div>
      <p class="mt-3 text-[11px] faint">显示 {{ filtered.length }} / {{ rows.length }} 条</p>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:90px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="还没有符合条件的记录" hint="点击「写一条记录」开始" />

    <div v-else class="crud-grid mt-5">
      <article v-for="r in filtered" :key="r.id" class="crud-card lift" v-reveal="2">
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="text-[11px] faint">{{ r.recordDate || '未标日期' }}</p>
            <h3 class="mt-1 text-[14px] font-semibold ink">{{ r.title }}</h3>
          </div>
          <span v-if="r.isMilestone" class="pill pill-amber">★ 里程碑</span>
        </div>
        <p class="mt-2 text-[12px] leading-6 muted">{{ r.content || '暂无正文' }}</p>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span v-if="r.category" class="pill pill-mint">{{ r.category }}</span>
          <span v-if="r.period" class="pill pill-neutral">{{ r.period }}</span>
          <span v-if="r.mood" class="pill pill-sky">{{ r.mood }}</span>
          <span v-for="t in tagsOf(r)" :key="t" class="pill pill-violet"># {{ t }}</span>
        </div>
        <p class="mt-3 text-[11px] faint">
          学习 {{ r.studyHours ?? 0 }}h · 睡眠 {{ r.sleepHours ?? 0 }}h · 运动 {{ r.exerciseHours ?? 0 }}h · 幸福 {{ r.happinessIndex ?? 0 }}/10
        </p>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-violet" type="button" @click="openEdit(r)"><span class="btn-glyph">✎</span> 编辑</button>
          <button class="btn btn-rose" type="button" @click="remove(r)"><span class="btn-glyph">×</span> 删除</button>
          <RouterLink v-if="r.isMilestone" to="/timeline/milestones" class="btn btn-amber"><span class="btn-glyph">★</span> 里程碑</RouterLink>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }} · 成长记录</h2>
          <p class="mb-5 text-[12px] muted">带 * 为必填；勾选「标为里程碑」将自动创建里程碑节点。</p>
          <div class="form-grid">
            <label><span class="field-label">记录日期 *</span><input v-model="form.recordDate" class="field w-full" type="date" /></label>
            <label><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" placeholder="今天发生了什么" /></label>
            <label><span class="field-label">分类</span><select v-model="form.category" class="field w-full">
              <option v-for="c in facetOptions.categories" :key="c">{{ c }}</option></select></label>
            <label><span class="field-label">时段</span><select v-model="form.period" class="field w-full">
              <option value="">不填写</option><option v-for="p in facetOptions.periods" :key="p">{{ p }}</option></select></label>
            <label><span class="field-label">心情</span><select v-model="form.mood" class="field w-full">
              <option v-for="m in moods" :key="m">{{ m }}</option></select></label>
            <label><span class="field-label">标签（逗号分隔）</span><input v-model="form.tags" class="field w-full" placeholder="学习,复盘" /></label>
            <label><span class="field-label">学习时长(h)</span><input v-model="form.studyHours" class="field w-full" type="number" step="0.5" min="0" /></label>
            <label><span class="field-label">睡眠时长(h)</span><input v-model="form.sleepHours" class="field w-full" type="number" step="0.5" min="0" /></label>
            <label><span class="field-label">运动时长(h)</span><input v-model="form.exerciseHours" class="field w-full" type="number" step="0.5" min="0" /></label>
            <label><span class="field-label">幸福指数(0-10)</span><input v-model="form.happinessIndex" class="field w-full" type="number" min="0" max="10" /></label>
            <label class="span-2"><span class="field-label">正文</span><textarea v-model="form.content" class="field w-full" rows="4" placeholder="把这一刻写下来…"></textarea></label>
            <label class="span-2 inline-flex items-center gap-2 text-[13px] muted">
              <input v-model="form.isMilestone" type="checkbox" /> 标为里程碑（自动生成里程碑节点，取消勾选会回收）
            </label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-mint" type="button" :disabled="saving" @click="submit">
              <span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
