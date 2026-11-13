<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const probeScore = ref(85)
const probe = ref(null)
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const activeRows = computed(() => rows.value.filter(r => r.active !== false))
const palette = ['#4a8cf7', '#12996a', '#8a6cf1', '#e0a23c', '#ef7a9b', '#3fb6c4']

async function load() {
  loading.value = true; error.value = ''
  try { rows.value = (await R.planning.gradingScales.list()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function convert() {
  error.value = ''; probe.value = null
  try { probe.value = await feature.convertGrade({ score: Number(probeScore.value) }) }
  catch (e) { error.value = e.message }
}

function defaults() { return { level: '', minScore: 0, maxScore: 100, ratio: 0, active: true, subjectId: null } }
function openCreate() { editing.value = null; form.value = defaults(); error.value = ''; showForm.value = true }
function openEdit(r) { editing.value = r.id; form.value = { ...r }; error.value = ''; showForm.value = true }

async function submit() {
  saving.value = true; error.value = ''
  try {
    const payload = { ...form.value }
    ;['minScore', 'maxScore', 'ratio'].forEach(k => { payload[k] = Number(payload[k]) || 0 })
    payload.active = !!payload.active
    if (editing.value) await R.planning.gradingScales.update(editing.value, payload)
    else await R.planning.gradingScales.save(payload)
    showForm.value = false; notice.value = '量表已保存，换算器立即生效'; await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除等级「${r.level}」吗？`)) return
  try { await R.planning.gradingScales.remove(r.id); await load() }
  catch (e) { error.value = e.message }
}

onMounted(async () => { await load(); await convert() })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / GRADING" title="等级量表"
      description="维护分数区间、等级与比例，右侧换算器实时按区间匹配；停用的量表不会参与换算。"
      :actions="[
        { label: '新增等级', icon: '＋', color: 'slate', click: openCreate },
        { label: '专业要求', icon: '◎', color: 'teal', to: '/planning/majors' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="grid gap-5 md:grid-cols-[1.6fr_1fr]">
      <div class="glass p-6">
        <div class="mb-4 flex items-center justify-between">
          <div><p class="eyebrow">SCALE</p><h2 class="section-title mt-1">分数区间</h2></div>
          <span class="pill pill-sky">{{ activeRows.length }} 条启用 / 共 {{ rows.length }} 条</span>
        </div>
        <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
        <p v-if="notice" class="pill pill-mint mb-3">{{ notice }}</p>
        <div v-if="loading" class="grid gap-3"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:64px"></div></div>
        <EmptyState v-else-if="!rows.length" text="还没有等级量表" hint="点「新增等级」配置分数区间" />
        <div v-else class="grid gap-3">
          <div v-for="(r, i) in rows" :key="r.id" class="glass-flat p-4" :class="{ 'opacity-55': r.active === false }">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span class="pill" :style="{ background: palette[i % palette.length] + '22', color: palette[i % palette.length] }">{{ r.level }}</span>
              <span class="text-[12px] muted">{{ r.minScore }} – {{ r.maxScore }} 分</span>
              <span class="pill pill-neutral">比例 {{ r.ratio }}</span>
              <span v-if="r.active === false" class="pill pill-rose">已停用</span>
            </div>
            <div class="progress-track mt-3">
              <div class="progress-fill" :style="{
                width: Math.min(100, Math.max(0, (Number(r.maxScore) - Number(r.minScore)))) + '%',
                background: palette[i % palette.length] }"></div>
            </div>
            <div class="mt-3 flex gap-2">
              <button class="btn btn-slate" type="button" @click="openEdit(r)">编辑</button>
              <button class="btn btn-rose" type="button" @click="remove(r)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <div class="glass h-fit p-6">
        <p class="eyebrow">CONVERTER</p>
        <h2 class="section-title mt-1 mb-5">分数换算</h2>
        <label class="block"><span class="field-label">输入分数</span>
          <input v-model="probeScore" class="field w-full" type="number" step="0.5" @keyup.enter="convert" /></label>
        <button class="btn btn-violet mt-4 w-full" type="button" @click="convert"><span class="btn-glyph">◷</span> 换算等级</button>
        <div v-if="probe" class="mt-6 text-center">
          <p class="text-[11px] faint">{{ probe.score }} 分对应</p>
          <p class="mt-2 text-[32px] font-semibold ink">{{ probe.level }}</p>
          <p class="mt-1 text-[12px] muted">比例 {{ probe.ratio }}</p>
          <p v-if="!probe.matched" class="mt-3 text-[11px] text-rose-500">没有命中的启用区间，请检查量表配置</p>
        </div>
        <p class="mt-6 text-[11px] leading-6 faint">
          换算按最低分从高到低逐条匹配，命中第一条即返回；多条区间重叠时优先高分档。
        </p>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
        <div class="modal-card">
          <h2 class="mb-1 text-[17px] font-semibold ink">{{ editing ? '编辑' : '新增' }}等级</h2>
          <p class="mb-5 text-[12px] muted">区间为闭区间，含最低分与最高分。</p>
          <div class="form-grid">
            <label><span class="field-label">等级</span><input v-model="form.level" class="field w-full" /></label>
            <label><span class="field-label">学科 ID（可空）</span><input v-model="form.subjectId" class="field w-full" type="number" /></label>
            <label><span class="field-label">最低分</span><input v-model="form.minScore" class="field w-full" type="number" /></label>
            <label><span class="field-label">最高分</span><input v-model="form.maxScore" class="field w-full" type="number" /></label>
            <label><span class="field-label">比例</span><input v-model="form.ratio" class="field w-full" type="number" step="0.01" /></label>
            <label><span class="field-label">启用</span>
              <label class="inline-flex items-center gap-2 text-[13px] muted"><input v-model="form.active" type="checkbox" /> 参与换算</label></label>
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
