<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const posts = ref([]), summary = ref([])
const loading = ref(true), saving = ref(false)
const msg = ref(''), error = ref(''), showForm = ref(false)
const course = ref(''), keyword = ref('')
const form = reactive({ courseName: '', rating: 5, content: '' })

const courses = computed(() => [...new Set(posts.value.map(p => p.courseName).filter(Boolean))].sort())
const filtered = computed(() => posts.value.filter(p => {
  const hit = !keyword.value || [p.courseName, p.content].some(v => String(v || '').toLowerCase().includes(keyword.value.toLowerCase()))
  return hit && (!course.value || p.courseName === course.value)
}))
const avg = computed(() => posts.value.length
  ? Math.round(posts.value.reduce((s, p) => s + (Number(p.rating) || 0), 0) / posts.value.length * 10) / 10 : 0)
function stars(n) { return '★'.repeat(Math.max(0, Math.min(5, Math.round(Number(n) || 0)))) + '☆'.repeat(5 - Math.max(0, Math.min(5, Math.round(Number(n) || 0)))) }

async function load() {
  loading.value = true; error.value = ''
  try {
    const [p, s] = await Promise.all([R.archive.electivePosts.list(), feature.electiveSummary()])
    posts.value = p || []; summary.value = s?.items || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
async function add() {
  if (!form.courseName.trim() || !form.content.trim()) { error.value = '请填写课程名称与评价内容'; return }
  saving.value = true; error.value = ''; msg.value = ''
  try {
    await R.archive.electivePosts.save({
      courseName: form.courseName.trim(), rating: Number(form.rating) || 0, content: form.content.trim(),
      createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })
    Object.assign(form, { courseName: '', rating: 5, content: '' })
    showForm.value = false; msg.value = '评价已发布'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}
async function remove(p) {
  if (!window.confirm(`确定删除对「${p.courseName}」的评价吗？`)) return
  try { await R.archive.electivePosts.remove(p.id); await load() } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / ELECTIVE" title="选课参考"
      description="课程评价与经验帖，按课程自动聚合平均分，供后续选课决策参考。"
      :actions="[{ label: '课程树', icon: '⌘', color: 'teal', to: '/archive/course-tree' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="glass mb-5 grid grid-cols-2 gap-4 p-5 md:grid-cols-3">
      <div><p class="eyebrow">评价总数</p><p class="mt-1 text-[22px] font-semibold ink">{{ posts.length }}</p></div>
      <div><p class="eyebrow">覆盖课程</p><p class="mt-1 text-[22px] font-semibold ink">{{ courses.length }}</p></div>
      <div><p class="eyebrow">整体均分</p><p class="mt-1 text-[22px] font-semibold ink">{{ avg }}</p></div>
    </div>

    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[220px]" placeholder="搜索课程或评价…" />
      <select v-model="course" class="field max-w-[180px]">
        <option value="">全部课程</option>
        <option v-for="c in courses" :key="c" :value="c">{{ c }}</option>
      </select>
      <button class="btn btn-mint" @click="showForm = !showForm"><span class="btn-glyph">＋</span> 写评价</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <div v-if="showForm" class="glass mb-5 p-6">
      <h2 class="section-title mb-4">新增课程评价</h2>
      <div class="form-grid">
        <label><span class="field-label">课程名称 *</span><input v-model="form.courseName" class="field w-full" /></label>
        <label><span class="field-label">评分（1–5）</span><input v-model="form.rating" class="field w-full" type="number" min="1" max="5" /></label>
        <label class="span-2"><span class="field-label">评价内容 *</span><textarea v-model="form.content" class="field w-full" rows="3"></textarea></label>
      </div>
      <button class="btn btn-blue mt-4" :disabled="saving" @click="add"><span class="btn-glyph">✓</span> 发布</button>
    </div>

    <div class="grid gap-5 md:grid-cols-[1fr_280px]">
      <div>
        <div v-if="loading" class="space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:100px"></div></div>
        <div v-else-if="!filtered.length" class="glass p-10 text-center text-[13px] muted">暂无评价。</div>
        <div v-else class="space-y-3">
          <div v-for="p in filtered" :key="p.id" class="glass p-4">
            <div class="flex flex-wrap items-center gap-2">
              <span class="pill pill-mint">{{ p.courseName }}</span>
              <span class="text-[13px] text-amber">{{ stars(p.rating) }}</span>
              <span class="text-[11px] faint">{{ p.rating }} / 5</span>
              <span class="ml-auto text-[11px] faint">{{ p.createdAt }}</span>
            </div>
            <p class="mt-2 text-[12px] leading-6 muted">{{ p.content }}</p>
            <button class="btn btn-rose mt-3" @click="remove(p)">删除</button>
          </div>
        </div>
      </div>
      <aside class="glass h-fit p-5">
        <h2 class="section-title mb-3">课程均分排行</h2>
        <div v-for="(s, i) in summary" :key="s.courseName" class="mb-2 flex items-center gap-2">
          <span class="pill pill-amber">{{ i + 1 }}</span>
          <span class="text-[12px] ink">{{ s.courseName }}</span>
          <span class="ml-auto text-[12px] font-medium ink">{{ s.avgRating }}</span>
          <span class="text-[10px] faint">({{ s.reviews }})</span>
        </div>
        <p v-if="!summary.length" class="text-[12px] muted">暂无汇总</p>
      </aside>
    </div>
  </section>
</template>
