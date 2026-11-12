<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { R, feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const milestones = ref([])
const records = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const showLink = ref(false)
const editing = ref(null)
const linking = ref(null)
const form = ref({})
const linkRecordId = ref('')

const filters = reactive({ q: '', origin: '', linked: '' })
const icons = ['star', 'flag', 'book', 'trophy', 'bolt', 'heart']
const colors = ['amber', 'mint', 'sky', 'violet', 'rose', 'teal']

const counts = computed(() => ({
  total: milestones.value.length,
  auto: milestones.value.filter(m => m.origin === 'AUTO').length,
  manual: milestones.value.filter(m => m.origin !== 'AUTO').length,
  linked: milestones.value.filter(m => m.linkedRecordId).length,
}))

const filtered = computed(() => {
  const kw = filters.q.trim().toLowerCase()
  return milestones.value.filter(m => {
    if (kw && !Object.values(m).some(v => v != null && String(v).toLowerCase().includes(kw))) return false
    if (filters.origin === 'AUTO' && m.origin !== 'AUTO') return false
    if (filters.origin === 'MANUAL' && m.origin === 'AUTO') return false
    if (filters.linked === 'yes' && !m.linkedRecordId) return false
    if (filters.linked === 'no' && m.linkedRecordId) return false
    return true
  })
})

function ymd() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [ms, rs] = await Promise.all([feature.milestoneDetails(), R.timeline.records.list()])
    milestones.value = ms || []
    records.value = rs || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

function defaults() {
  return { title: '', occurredAt: ymd(), icon: 'star', color: 'amber', description: '', origin: 'MANUAL' }
}
function openCreate() { editing.value = null; form.value = defaults(); showForm.value = true; notice.value = '' }
function openEdit(m) { editing.value = m.id; form.value = { ...m }; showForm.value = true; notice.value = '' }
function openLink(m) { linking.value = m; linkRecordId.value = m.linkedRecordId || ''; showLink.value = true; notice.value = '' }

async function submit() {
  if (!form.value.title?.trim()) { error.value = '请填写里程碑标题'; return }
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    delete payload.linkedRecordTitle
    delete payload.linkedRecordDate
    if (editing.value) await R.timeline.milestones.update(editing.value, payload)
    else await R.timeline.milestones.save(payload)
    showForm.value = false
    notice.value = '里程碑已保存'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function doLink() {
  if (!linkRecordId.value) { error.value = '请选择一条成长记录'; return }
  saving.value = true; error.value = ''
  try {
    await feature.linkMilestone(linking.value.id, Number(linkRecordId.value))
    showLink.value = false
    notice.value = '已关联成长记录，该里程碑现由你手动管理'
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function doUnlink(m) {
  try { await feature.unlinkMilestone(m.id); notice.value = '已解除关联'; await load() }
  catch (e) { error.value = e.message }
}

async function remove(m) {
  const extra = m.origin === 'AUTO' ? '它由成长记录自动生成，若记录仍勾选「标为里程碑」，下次编辑记录时会再次生成。' : ''
  if (!window.confirm(`确定删除「${m.title}」吗？${extra}`)) return
  try { await R.timeline.milestones.remove(m.id); notice.value = '里程碑已删除'; await load() }
  catch (e) { error.value = e.message }
}
function resetFilters() { Object.assign(filters, { q: '', origin: '', linked: '' }) }

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="TIMELINE / MILESTONES" title="里程碑"
      description="重要时刻有两条来源：记录勾选「标为里程碑」自动生成，或在这里手动创建并关联到某条记录。"
      :actions="[
        { label: '新建里程碑', icon: '＋', color: 'amber', click: openCreate },
        { label: '成长记录', icon: '✎', color: 'mint', to: '/timeline/records' },
        { label: '时间线', icon: '⌁', color: 'violet', to: '/timeline' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '全部里程碑', value: counts.total }, { label: '记录自动生成', value: counts.auto },
        { label: '手动创建 / 关联', value: counts.manual }, { label: '已关联记录', value: counts.linked },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p>
        <p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <div class="glass mt-5 p-5" v-reveal="1">
      <div class="flex flex-wrap items-end gap-3">
        <label class="min-w-[180px] flex-1"><span class="field-label">搜索</span>
          <input v-model="filters.q" class="field w-full" type="search" placeholder="标题 / 描述…" /></label>
        <label><span class="field-label">来源</span><select v-model="filters.origin" class="field w-[170px]">
          <option value="">全部</option><option value="AUTO">记录自动生成</option><option value="MANUAL">手动创建 / 关联</option></select></label>
        <label><span class="field-label">关联状态</span><select v-model="filters.linked" class="field w-[140px]">
          <option value="">全部</option><option value="yes">已关联记录</option><option value="no">未关联</option></select></label>
        <button class="btn btn-slate" type="button" @click="resetFilters"><span class="btn-glyph">↺</span> 重置</button>
      </div>
      <p class="mt-3 text-[11px] faint">显示 {{ filtered.length }} / {{ milestones.length }} 条</p>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <p v-if="notice" class="pill pill-mint mt-4">{{ notice }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:96px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-5" text="还没有符合条件的里程碑"
      hint="新建一条，或到成长记录里勾选「标为里程碑」自动生成" />

    <div v-else class="crud-grid mt-5">
      <article v-for="m in filtered" :key="m.id" class="crud-card lift" v-reveal="2">
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="text-[11px] faint">{{ m.occurredAt || '未标日期' }}</p>
            <h3 class="mt-1 text-[14px] font-semibold ink">{{ m.icon === 'star' ? '★ ' : '' }}{{ m.title }}</h3>
          </div>
          <span class="pill" :class="m.origin === 'AUTO' ? 'pill-mint' : 'pill-amber'">
            {{ m.origin === 'AUTO' ? '自动生成' : '手动' }}
          </span>
        </div>
        <p class="mt-2 text-[12px] leading-6 muted">{{ m.description || '暂无描述' }}</p>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span class="pill" :class="`pill-${m.color || 'neutral'}`">{{ m.color || '默认' }}</span>
          <span class="pill pill-neutral">{{ m.icon || 'star' }}</span>
        </div>
        <div class="mt-3 rounded-lg p-2" style="background: var(--surface-2)">
          <p v-if="m.linkedRecordId" class="text-[11px] muted">
            关联记录：<RouterLink to="/timeline/records" class="link">{{ m.linkedRecordTitle || ('#' + m.linkedRecordId) }}</RouterLink>
            <span v-if="m.linkedRecordDate" class="faint"> · {{ m.linkedRecordDate }}</span>
          </p>
          <p v-else class="text-[11px] faint">尚未关联成长记录</p>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-violet" type="button" @click="openEdit(m)"><span class="btn-glyph">✎</span> 编辑</button>
          <button class="btn btn-teal" type="button" @click="openLink(m)"><span class="btn-glyph">⌘</span> 关联记录</button>
          <button v-if="m.linkedRecordId" class="btn btn-slate" type="button" @click="doUnlink(m)"><span class="btn-glyph">↩</span> 解除</button>
          <button class="btn btn-rose" type="button" @click="remove(m)"><span class="btn-glyph">×</span> 删除</button>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新建' }} · 里程碑</h2>
          <p class="mb-5 text-[12px] muted">手动创建的里程碑可以在下方「关联记录」里绑定到某条成长记录。</p>
          <div class="form-grid">
            <label><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" placeholder="例如：第一次完整复盘" /></label>
            <label><span class="field-label">发生日期</span><input v-model="form.occurredAt" class="field w-full" type="date" /></label>
            <label><span class="field-label">图标</span><select v-model="form.icon" class="field w-full"><option v-for="i in icons" :key="i">{{ i }}</option></select></label>
            <label><span class="field-label">主题色</span><select v-model="form.color" class="field w-full"><option v-for="c in colors" :key="c">{{ c }}</option></select></label>
            <label class="span-2"><span class="field-label">描述</span><textarea v-model="form.description" class="field w-full" rows="4"></textarea></label>
          </div>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showForm = false">取消</button>
            <button class="btn btn-amber" type="button" :disabled="saving" @click="submit"><span class="btn-glyph">✓</span> {{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showLink" class="modal-mask" @click.self="showLink = false">
        <div class="modal-card" style="width: min(480px, 100%)">
          <h2 class="mb-1 text-[17px] font-semibold ink">关联成长记录</h2>
          <p class="mb-5 text-[12px] muted">把「{{ linking?.title }}」关联到一条记录；关联后该里程碑转为你手动管理，不再随记录勾选状态自动回收。</p>
          <label><span class="field-label">选择记录</span>
            <select v-model="linkRecordId" class="field w-full">
              <option value="">请选择</option>
              <option v-for="r in records" :key="r.id" :value="r.id">{{ r.recordDate }} · {{ r.title }}</option>
            </select>
          </label>
          <p v-if="!records.length" class="mt-3 text-[12px] faint">还没有成长记录，先去写一条吧。</p>
          <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-slate" type="button" @click="showLink = false">取消</button>
            <button class="btn btn-teal" type="button" :disabled="saving || !records.length" @click="doLink"><span class="btn-glyph">⌘</span> 关联</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
