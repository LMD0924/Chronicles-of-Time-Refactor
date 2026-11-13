<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const statuses = ['待办', '进行中', '已完成']
const board = ref({ groups: {}, stages: [], total: 0, done: 0, overdue: 0, completionRate: 0 })
const loading = ref(true), saving = ref(false), msg = ref(''), error = ref('')
const showForm = ref(false)
const form = reactive({ title: '', stage: '选题', deadline: '', status: '待办', note: '' })

const stages = computed(() => board.value.stages?.length ? board.value.stages : ['选题', '开题', '初稿', '修改', '定稿', '答辩'])
const groups = computed(() => board.value.groups || {})

async function load() {
  loading.value = true; error.value = ''
  try { board.value = (await feature.thesisBoard()) || board.value } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
async function add() {
  if (!form.title.trim()) { error.value = '请填写任务名称'; return }
  saving.value = true; error.value = ''
  try {
    await R.archive.thesisBoard.save({ ...form, createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ') })
    Object.assign(form, { title: '', stage: '选题', deadline: '', status: '待办', note: '' })
    showForm.value = false; msg.value = '任务已加入看板'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}
async function advance(item) {
  try { await feature.thesisStage(item.id, { next: true, done: stages.value.indexOf(item.stage) === stages.value.length - 1 }); msg.value = `「${item.title}」已推进`; await load() }
  catch (e) { error.value = e.message }
}
async function setStatus(item, status) {
  try { await feature.thesisStatus(item.id, status); await load() } catch (e) { error.value = e.message }
}
async function remove(item) {
  if (!window.confirm(`确定删除「${item.title}」吗？`)) return
  try { await R.archive.thesisBoard.remove(item.id); await load() } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / THESIS" title="论文看板"
      description="按选题 → 开题 → 初稿 → 修改 → 定稿 → 答辩推进，逾期任务会高亮提醒。"
      :actions="[{ label: '论文', icon: '✎', color: 'violet', to: '/archive/papers' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="glass mb-5 p-5">
      <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div><p class="eyebrow">任务总数</p><p class="mt-1 text-[22px] font-semibold ink">{{ board.total }}</p></div>
        <div><p class="eyebrow">已完成</p><p class="mt-1 text-[22px] font-semibold ink">{{ board.done }}</p></div>
        <div><p class="eyebrow">逾期</p><p class="mt-1 text-[22px] font-semibold ink">{{ board.overdue }}</p></div>
        <div>
          <p class="eyebrow">完成率 {{ board.completionRate }}%</p>
          <div class="progress-track mt-3"><div class="progress-fill" :style="{ width: (board.completionRate || 0) + '%' }"></div></div>
        </div>
      </div>
    </div>

    <div class="crud-toolbar">
      <button class="btn btn-mint" @click="showForm = !showForm"><span class="btn-glyph">＋</span> 新增任务</button>
      <span class="pill pill-rose" v-if="board.overdue">{{ board.overdue }} 项逾期</span>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <div v-if="showForm" class="glass mb-5 p-6">
      <h2 class="section-title mb-4">新增看板任务</h2>
      <div class="form-grid">
        <label class="span-2"><span class="field-label">任务 *</span><input v-model="form.title" class="field w-full" /></label>
        <label><span class="field-label">阶段</span><select v-model="form.stage" class="field w-full"><option v-for="s in stages" :key="s">{{ s }}</option></select></label>
        <label><span class="field-label">截止日期</span><input v-model="form.deadline" class="field w-full" type="date" /></label>
        <label><span class="field-label">状态</span><select v-model="form.status" class="field w-full"><option v-for="s in statuses" :key="s">{{ s }}</option></select></label>
        <label><span class="field-label">备注</span><input v-model="form.note" class="field w-full" /></label>
      </div>
      <button class="btn btn-blue mt-4" :disabled="saving" @click="add"><span class="btn-glyph">✓</span> 保存</button>
    </div>

    <div v-if="loading" class="placeholder-bar" style="height:240px"></div>
    <div v-else class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="s in stages" :key="s" class="glass p-4">
        <div class="mb-3 flex items-center gap-2">
          <h2 class="section-title">{{ s }}</h2>
          <span class="pill pill-sky ml-auto">{{ (groups[s] || []).length }}</span>
        </div>
        <div v-for="item in (groups[s] || [])" :key="item.id" class="glass-flat mb-2 p-3">
          <div class="flex items-start gap-2">
            <span class="text-[12px] font-medium ink">{{ item.title }}</span>
            <span class="pill ml-auto" :class="item.status === '已完成' ? 'pill-mint' : item.status === '进行中' ? 'pill-amber' : 'pill-neutral'">{{ item.status }}</span>
          </div>
          <p class="mt-1 text-[11px] faint">截止 {{ item.deadline || '未设置' }}</p>
          <p v-if="item.note" class="mt-1 text-[11px] muted">{{ item.note }}</p>
          <div class="mt-2 flex flex-wrap gap-1">
            <button class="btn btn-mint" @click="advance(item)">推进 →</button>
            <select class="field max-w-[110px]" :value="item.status" @change="setStatus(item, $event.target.value)">
              <option v-for="st in statuses" :key="st">{{ st }}</option>
            </select>
            <button class="btn btn-rose ml-auto" @click="remove(item)">删除</button>
          </div>
        </div>
        <p v-if="!(groups[s] || []).length" class="text-[11px] muted">本阶段暂无任务</p>
      </div>
    </div>
  </section>
</template>
