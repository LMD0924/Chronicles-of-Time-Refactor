<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'
import EmptyState from '@/components/EmptyState.vue'

const entries = ref([])
const stats = ref({})
const facets = ref({ categories: [], periods: [], tags: [] })
const loading = ref(true)
const loadingMore = ref(false)
const error = ref('')
const page = ref(1)
const pages = ref(1)
const total = ref(0)
const expanded = reactive({})
const size = 20

const filters = reactive({ q: '', type: '', category: '', period: '', dateFrom: '', dateTo: '' })
const types = ['记录', '里程碑', '工作', '进阶', '成绩', '练习', '规划', '目标', '文章', '证书', '论文']

const metrics = computed(() => [
  { label: '时间线节点', value: total.value, color: 'sky' },
  { label: '成长记录', value: stats.value.totalRecords || 0, color: 'mint' },
  { label: '里程碑', value: stats.value.milestones || 0, color: 'amber' },
  { label: '学习时长', value: Math.round(stats.value.studyHours || 0), color: 'violet' },
])

const hasFilter = computed(() => Object.values(filters).some(v => !!v))
const grouped = computed(() => {
  const map = new Map()
  for (const e of entries.value) {
    const d = String(e.occurredAt || '').slice(0, 10) || '未标日期'
    if (!map.has(d)) map.set(d, [])
    map.get(d).push(e)
  }
  return [...map.entries()].map(([date, items]) => ({ date, items }))
})

function query(p) {
  return {
    page: p, size,
    q: filters.q || undefined,
    type: filters.type || undefined,
    category: filters.category || undefined,
    period: filters.period || undefined,
    dateFrom: filters.dateFrom || undefined,
    dateTo: filters.dateTo || undefined,
  }
}

async function load(reset = true) {
  if (reset) { page.value = 1; entries.value = []; loading.value = true }
  else loadingMore.value = true
  error.value = ''
  try {
    const data = await feature.timelineFeedPage(query(page.value))
    entries.value = reset ? (data.items || []) : [...entries.value, ...(data.items || [])]
    pages.value = data.pages || 1
    total.value = data.total || 0
  } catch (e) { error.value = e.message }
  finally { loading.value = false; loadingMore.value = false }
}

async function loadMore() {
  if (page.value >= pages.value || loadingMore.value) return
  page.value += 1
  await load(false)
}

async function loadMeta() {
  try {
    const [s, f] = await Promise.all([feature.timelineStats(), feature.timelineFacets()])
    stats.value = s || {}
    facets.value = f || { categories: [], periods: [], tags: [] }
  } catch { /* 筛选字典失败不阻塞时间线 */ }
}

function apply() { load(true) }
function resetAll() {
  Object.assign(filters, { q: '', type: '', category: '', period: '', dateFrom: '', dateTo: '' })
  load(true)
}
function tagsOf(e) {
  return String(e.tags || '').split(/[,，;；\s]+/).filter(Boolean).slice(0, 4)
}

onMounted(() => { load(true); loadMeta() })
</script>

<template>
  <section>
    <PageHeader eyebrow="CHRONICLE / ONE TIMELINE" title="拾光时间线"
      description="记录、学习、规划、社区与档案汇成一条轴。按日期倒序排列，可筛选、展开详情并跳回来源功能。"
      :actions="[
        { label: '写记录', icon: '＋', color: 'mint', to: '/timeline/records' },
        { label: '里程碑', icon: '★', color: 'amber', to: '/timeline/milestones' },
        { label: '贯通关联', icon: '⌘', color: 'violet', to: '/timeline/connections' },
        { label: '刷新', icon: '↻', color: 'slate', click: () => { load(true); loadMeta() } },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>

    <div class="glass mt-5 p-5" v-reveal="1">
      <div class="flex flex-wrap items-end gap-3">
        <label class="min-w-[200px] flex-1">
          <span class="field-label">关键词</span>
          <input v-model="filters.q" class="field w-full" type="search" placeholder="标题或摘要…" @keyup.enter="apply" />
        </label>
        <label>
          <span class="field-label">分类</span>
          <select v-model="filters.category" class="field w-[140px]" @change="apply">
            <option value="">全部</option>
            <option v-for="c in facets.categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
        <label>
          <span class="field-label">时段</span>
          <select v-model="filters.period" class="field w-[130px]" @change="apply">
            <option value="">全部</option>
            <option v-for="p in facets.periods" :key="p" :value="p">{{ p }}</option>
          </select>
        </label>
        <label>
          <span class="field-label">起始日期</span>
          <input v-model="filters.dateFrom" class="field w-[150px]" type="date" @change="apply" />
        </label>
        <label>
          <span class="field-label">结束日期</span>
          <input v-model="filters.dateTo" class="field w-[150px]" type="date" @change="apply" />
        </label>
        <button class="btn btn-blue" type="button" @click="apply"><span class="btn-glyph">⌕</span> 筛选</button>
        <button v-if="hasFilter" class="btn btn-slate" type="button" @click="resetAll"><span class="btn-glyph">↺</span> 重置</button>
      </div>
      <div class="mt-4 flex flex-wrap gap-1.5">
        <button class="chip" :class="{ 'is-active': !filters.type }" @click="filters.type = ''; apply()">全部类型</button>
        <button v-for="t in types" :key="t" class="chip" :class="{ 'is-active': filters.type === t }"
                @click="filters.type = t; apply()">{{ t }}</button>
      </div>
      <p class="mt-3 text-[11px] faint">共 {{ total }} 个节点 · 已加载 {{ entries.length }} 个{{ hasFilter ? ' · 分类与时段筛选仅作用于带该字段的记录节点' : '' }}</p>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <div v-if="loading" class="mt-6 space-y-3"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:78px"></div></div>
    <EmptyState v-else-if="!grouped.length" class="mt-6" text="没有符合条件的节点"
      hint="换个筛选条件，或从「写记录」开始让时间线长起来" />

    <div v-else class="mt-6">
      <div v-for="g in grouped" :key="g.date" class="mb-7">
        <div class="mb-3 flex items-center gap-3">
          <span class="pill pill-neutral">{{ g.date }}</span>
          <span class="h-px flex-1" style="background: var(--line-soft)"></span>
          <span class="text-[11px] faint">{{ g.items.length }} 个节点</span>
        </div>
        <div class="tl-rail">
          <div v-for="e in g.items" :key="e.id" class="tl-node mb-4" :class="{ 'tl-milestone': e.type === '里程碑' }">
            <div class="glass lift p-5">
              <div class="flex flex-wrap items-center gap-2">
                <span class="pill" :class="`pill-${e.color}`">{{ e.type }}</span>
                <span class="text-[11px] faint">{{ e.occurredAt }}</span>
                <span v-if="e.category" class="pill pill-neutral">{{ e.category }}</span>
                <span v-if="e.period" class="pill pill-neutral">{{ e.period }}</span>
                <span class="ml-auto text-[11px] faint">#{{ e.sourceId }}</span>
              </div>
              <h3 class="mt-2 text-[15px] font-semibold ink">{{ e.title || '未命名节点' }}</h3>
              <p class="mt-1 text-[12px] leading-6 muted">{{ expanded[e.id] ? (e.summary || '暂无摘要') : String(e.summary || '暂无摘要').slice(0, 90) }}{{ !expanded[e.id] && String(e.summary || '').length > 90 ? '…' : '' }}</p>
              <div v-if="expanded[e.id]" class="mt-3 flex flex-wrap gap-1.5">
                <span v-for="t in tagsOf(e)" :key="t" class="pill pill-sky"># {{ t }}</span>
                <span v-if="!tagsOf(e).length" class="text-[11px] faint">这条节点没有标签</span>
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <button class="btn btn-slate" type="button" @click="expanded[e.id] = !expanded[e.id]">
                  <span class="btn-glyph">{{ expanded[e.id] ? '⌃' : '⌄' }}</span> {{ expanded[e.id] ? '收起' : '详情' }}
                </button>
                <RouterLink :to="e.link" class="btn btn-teal"><span class="btn-glyph">→</span> 查看来源</RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="page < pages" class="mt-4 text-center">
        <button class="btn btn-blue" type="button" :disabled="loadingMore" @click="loadMore">
          <span class="btn-glyph">⌄</span> {{ loadingMore ? '加载中…' : `加载更多（第 ${page} / ${pages} 页）` }}
        </button>
      </div>
    </div>
  </section>
</template>
