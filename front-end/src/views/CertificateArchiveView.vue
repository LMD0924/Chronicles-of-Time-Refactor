<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const rows = ref([]), stats = ref({}), keyword = ref(''), category = ref('')
const loading = ref(true), saving = ref(false), uploading = ref(false)
const msg = ref(''), error = ref('')
const showForm = ref(false), editing = ref(null), pendingFile = ref(null)
const form = reactive({ name: '', issuer: '', issuedAt: '', category: '', fileUrl: '' })

const categories = computed(() => [...new Set(rows.value.map(r => r.category).filter(Boolean))].sort())
const filtered = computed(() => rows.value.filter(r => {
  const hit = !keyword.value || [r.name, r.issuer, r.category].some(v => String(v || '').toLowerCase().includes(keyword.value.toLowerCase()))
  return hit && (!category.value || r.category === category.value)
}))
const withFile = computed(() => rows.value.filter(r => r.fileUrl).length)

function reset() {
  editing.value = null; pendingFile.value = null
  Object.assign(form, { name: '', issuer: '', issuedAt: '', category: '', fileUrl: '' })
}
function edit(r) { editing.value = r.id; Object.assign(form, { ...r }); showForm.value = true }

async function load() {
  loading.value = true; error.value = ''
  try {
    const [list, s] = await Promise.all([R.archive.certificates.list(), feature.archiveStats()])
    rows.value = list || []; stats.value = s || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function bindFile(id) {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    const uploaded = await feature.uploadFile(pendingFile.value)
    await feature.bindAttachment({ resource: 'certificates', id, url: uploaded.url })
    msg.value = `附件「${uploaded.fileName}」已上传并绑定`
    pendingFile.value = null
  } finally { uploading.value = false }
}

async function save() {
  if (!form.name.trim()) { error.value = '请填写证书名称'; return }
  saving.value = true; error.value = ''; msg.value = ''
  try {
    const payload = { ...form }
    let id = editing.value
    if (editing.value) await R.archive.certificates.update(editing.value, payload)
    else {
      const saved = await R.archive.certificates.save(payload)
      id = saved?.id
    }
    if (id) await bindFile(id)
    msg.value = editing.value ? '证书已更新' : '证书已归档'
    showForm.value = false; reset(); await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

async function remove(r) {
  if (!window.confirm(`确定删除「${r.name}」吗？`)) return
  try { await R.archive.certificates.remove(r.id); await load() } catch (e) { error.value = e.message }
}
async function clearFile(r) {
  try { await R.archive.certificates.update(r.id, { ...r, fileUrl: '' }); msg.value = '附件引用已移除'; await load() }
  catch (e) { error.value = e.message }
}
function onFile(e) { pendingFile.value = e.target.files?.[0] || null }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / CERTIFICATES" title="证书档案"
      description="证书、颁发机构与附件统一归档；上传的文件会写入本服务静态资源目录并绑定到记录。"
      :actions="[{ label: '简历', icon: '▤', color: 'blue', to: '/archive/resume' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="glass mb-5 grid grid-cols-2 gap-4 p-5 md:grid-cols-4">
      <div><p class="eyebrow">证书总数</p><p class="mt-1 text-[22px] font-semibold ink">{{ rows.length }}</p></div>
      <div><p class="eyebrow">已附附件</p><p class="mt-1 text-[22px] font-semibold ink">{{ withFile }}</p></div>
      <div><p class="eyebrow">分类数</p><p class="mt-1 text-[22px] font-semibold ink">{{ categories.length }}</p></div>
      <div><p class="eyebrow">档案总附件</p><p class="mt-1 text-[22px] font-semibold ink">{{ stats.attachments ?? 0 }}</p></div>
    </div>

    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[240px]" placeholder="搜索名称 / 机构 / 类别…" />
      <select v-model="category" class="field max-w-[180px]">
        <option value="">全部类别</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <button class="btn btn-mint" @click="showForm = !showForm; reset()"><span class="btn-glyph">＋</span> 新增证书</button>
      <span class="pill pill-sky">{{ filtered.length }} / {{ rows.length }} 条</span>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <div v-if="showForm" class="glass mb-5 p-6">
      <h2 class="section-title mb-4">{{ editing ? '编辑证书' : '新增证书' }}</h2>
      <div class="form-grid">
        <label><span class="field-label">证书名称 *</span><input v-model="form.name" class="field w-full" /></label>
        <label><span class="field-label">颁发机构</span><input v-model="form.issuer" class="field w-full" /></label>
        <label><span class="field-label">取得日期</span><input v-model="form.issuedAt" class="field w-full" type="date" /></label>
        <label><span class="field-label">类别</span><input v-model="form.category" class="field w-full" placeholder="如：技能 / 竞赛 / 语言" /></label>
        <label class="span-2"><span class="field-label">附件（图片 / PDF，保存后自动上传并绑定）</span>
          <input class="field w-full" type="file" accept="image/*,.pdf,.doc,.docx" @change="onFile" />
        </label>
      </div>
      <div class="mt-4 flex gap-2">
        <button class="btn btn-blue" :disabled="saving || uploading" @click="save">
          <span class="btn-glyph">✓</span> {{ uploading ? '上传中…' : '保存' }}
        </button>
        <button class="btn btn-slate" @click="showForm = false; reset()">取消</button>
      </div>
    </div>

    <div v-if="loading" class="grid gap-4 md:grid-cols-2"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:130px"></div></div>
    <div v-else-if="!filtered.length" class="glass p-10 text-center text-[13px] muted">暂无证书，点「新增证书」开始归档。</div>
    <div v-else class="grid gap-4 md:grid-cols-2">
      <div v-for="r in filtered" :key="r.id" class="glass lift p-5">
        <div class="flex items-start gap-3">
          <span class="pill pill-amber">★</span>
          <div class="min-w-0 flex-1">
            <h3 class="text-[15px] font-semibold ink">{{ r.name }}</h3>
            <p class="mt-1 text-[11px] faint">{{ r.issuer || '未填写机构' }} · {{ r.issuedAt || '未填写日期' }}</p>
          </div>
          <span v-if="r.category" class="pill pill-sky">{{ r.category }}</span>
        </div>
        <div class="mt-4 flex flex-wrap items-center gap-2 border-t border-hairline pt-3">
          <a v-if="r.fileUrl" :href="r.fileUrl" target="_blank" rel="noopener" class="btn btn-violet">
            <span class="btn-glyph">↗</span> 查看附件
          </a>
          <span v-else class="text-[11px] faint">暂无附件</span>
          <button v-if="r.fileUrl" class="btn btn-slate ml-auto" @click="clearFile(r)">移除引用</button>
          <button class="btn btn-slate" @click="edit(r)">编辑</button>
          <button class="btn btn-rose" @click="remove(r)">删除</button>
        </div>
      </div>
    </div>
  </section>
</template>
