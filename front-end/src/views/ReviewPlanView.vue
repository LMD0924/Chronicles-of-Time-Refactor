<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const board = ref({ items: [], byStatus: {}, bySubject: [], averageProgress: 0 })
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const filter = ref('全部')
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const filters = computed(() => ['全部', '逾期', '未开始', '进行中', '已完成', '已搁置'])
const visible = computed(() => {
  const list = board.value.items || []
  if (filter.value === '全部') return list
  if (filter.value === '逾期') return list.filter(p => p.overdue)
  return list.filter(p => (p.status || '未开始') === filter.value)
})
const overdueCount = computed(() => (board.value.items || []).filter(p => p.overdue).length)
const doneCount = computed(() => (board.value.items || []).filter(p => p.progress >= 100 || p.status === '已完成').length)
const subjectOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (board.value.bySubject || []).map(s => s.subject) },
  yAxis: { type: 'value', name: '平均进度', max: 100 },
  series: [{ type: 'bar', barWidth: '46%', data: (board.value.bySubject || []).map(s => s.averageProgress),
    itemStyle: { color: '#4a8cf7', borderRadius: [6, 6, 0, 0] } }],
}))

async function load() {
  loading.value = true; error.value = ''
  try { board.value = (await feature.reviewBoard()) || { items: [], byStatus: {}, bySubject: [] } }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  const d = new Date(); d.setDate(d.getDate() + 14)
  return {
    subject: '', title: '', content: '',
    targetDate: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    progress: 0, status: '未开始',
  }
}
function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(p) { editing.value = p.id; form.value = { ...p }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    payload.progress = Number(payload.progress) || 0
    if (editing.value) await R.planning.reviewPlans.update(editing.value, payload)
    else await R.planning.reviewPlans.save(payload)
    showForm.value = false; notice.value = '计划已保存，看板与统计同步刷新'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function setProgress(p) {
  error.value = ''
  try { await feature.setReviewProgress(p.id, Number(p.progress)); notice.value = '进度已更新'; await load() }
  catch (e) { error.value = e.message; await load() }
}

async function decompose(p) {
  error.value = ''
  try {
    const r = await feature.decomposeReview(p.id, [])
    notice.value = `已把「${p.title}」拆成 ${r.created} 个阶段，正在前往目标路径`
    setTimeout(() => router.push('/planning/target'), 600)
  } catch (e) { error.value = e.message }
}

async function remove(p) {
  if (!window.confirm(`确定删除「${p.title}」吗？`)) return
  try { await R.planning.reviewPlans.remove(p.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / REVIEW" title="复习计划"
      description="按学科制定计划、拖动进度并拆成里程碑；100% 自动标记完成，逾期项会优先提醒，到期节点同步进时间线。"
      :actions="[
        { label: '新增计划', icon: '＋', color: 'blue', click: openCreate },
        { label: '目标路径', icon: '◎', color: 'violet', to: '/planning/target' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '计划总数', value: board.items?.length || 0 },
        { label: '平均进度', value: `${board.averageProgress || 0}%` },
        { label: '已完成', value: doneCount },
        { label: '已逾期', value: overdueCount },
      ]" :key="m.label" class="text-center">
        <p class="text-[20px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>

    <div class="crud-toolbar mt-5">
      <button v-for="f in filters" :key="f" class="btn" :class="filter === f ? 'btn-violet' : 'btn-slate'" type="button" @click="filter = f">
        {{ f }}<span v-if="f === '逾期' && overdueCount" class="ml-1">· {{ overdueCount }}</span>
      </button>
    </div>

    <div v-if="loading" class="grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:170px"></div></div>
    <template v-else>
      <div class="mt-5 grid gap-5 md:grid-cols-[1.7fr_1fr]">
        <div>
          <EmptyState v-if="!visible.length" text="这个分类下还没有计划" hint="点右上角「新增计划」开始" />
          <div v-for="p in visible" :key="p.id" class="glass mb-4 p-5" v-reveal="1">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="pill pill-sky">{{ p.subject || '未分类' }}</span>
                  <span class="pill" :class="p.overdue ? 'pill-rose' : p.status === '已完成' ? 'pill-mint' : 'pill-neutral'">{{ p.status || '未开始' }}</span>
                  <span v-if="p.overdue" class="pill pill-rose">已逾期</span>
                  <span v-else-if="p.remainingDays !== null" class="text-[11px] faint">还剩 {{ p.remainingDays }} 天</span>
                </div>
                <h3 class="mt-2 text-[15px] font-semibold ink">{{ p.title }}</h3>
                <p class="mt-1 text-[12px] leading-6 muted">{{ p.content || '暂无计划内容' }}</p>
                <p class="mt-1 text-[11px] faint">目标日期 {{ p.targetDate || '未设置' }}</p>
              </div>
              <div class="text-right"><p class="stat-num">{{ p.progress }}%</p><p class="text-[11px] faint">完成度</p></div>
            </div>
            <input v-model.number="p.progress" class="mt-4 w-full accent-violet-500" type="range" min="0" max="100" step="5"
                   @change="setProgress(p)" />
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="btn btn-violet" type="button" @click="decompose(p)"><span class="btn-glyph">⌘</span> 拆成里程碑</button>
              <button class="btn btn-slate" type="button" @click="openEdit(p)"><span class="btn-glyph">✎</span> 编辑</button>
              <button class="btn btn-rose" type="button" @click="remove(p)"><span class="btn-glyph">×</span> 删除</button>
            </div>
          </div>
        </div>

        <div class="glass h-fit p-5">
          <h2 class="section-title mb-2">各学科平均进度</h2>
          <Chart :option="subjectOption" :height="260" />
          <div class="mt-4 space-y-2">
            <div v-for="(v, k) in board.byStatus" :key="k" class="flex items-center justify-between text-[12px]">
              <span class="muted">{{ k }}</span><span class="pill pill-neutral">{{ v }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}复习计划</h2>
          <p class="mb-5 text-[12px] muted">进度可保存后在看板上拖动，100% 会自动置为「已完成」。</p>
          <div class="form-grid">
            <label><span class="field-label">学科</span><input v-model="form.subject" class="field w-full" list="review-subjects" />
              <datalist id="review-subjects"><option v-for="s in board.bySubject" :key="s.subject" :value="s.subject" /></datalist></label>
            <label><span class="field-label">状态</span><select v-model="form.status" class="field w-full">
              <option>未开始</option><option>进行中</option><option>已完成</option><option>已搁置</option></select></label>
            <label class="span-2"><span class="field-label">计划标题</span><input v-model="form.title" class="field w-full" /></label>
            <label class="span-2"><span class="field-label">计划内容</span><textarea v-model="form.content" class="field w-full" rows="3"></textarea></label>
            <label><span class="field-label">目标日期</span><input v-model="form.targetDate" class="field w-full" type="date" /></label>
            <label><span class="field-label">进度(%)</span><input v-model="form.progress" class="field w-full" type="number" min="0" max="100" /></label>
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
