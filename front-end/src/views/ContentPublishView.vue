<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const labels = { DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', HIDDEN: '已下架', ARCHIVED: '已归档' }
const pills = { DRAFT: 'pill-neutral', PENDING: 'pill-amber', PUBLISHED: 'pill-mint', REJECTED: 'pill-rose', HIDDEN: 'pill-neutral', ARCHIVED: 'pill-neutral' }
const categories = ['随笔', '复盘', '学习笔记', '规划', '生活']

const tab = ref('write'), publishing = ref(false), uploading = ref(false)
const msg = ref(''), error = ref('')
const mine = ref([]), overview = ref({ counts: {}, total: 0 })
const editing = ref(null)
const form = reactive({ title: '', category: '随笔', tags: '', body: '', coverUrl: '' })
const preview = computed(() => md.render(form.body || '*预览会显示在这里*'))

function reset() { editing.value = null; Object.assign(form, { title: '', category: '随笔', tags: '', body: '', coverUrl: '' }) }

async function loadMine() {
  try {
    const [list, ov] = await Promise.all([feature.myContents(), feature.contentOverview()])
    mine.value = list?.items || []; overview.value = ov || overview.value
  } catch (e) { error.value = e.message }
}
async function upload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true; error.value = ''
  try { const r = await feature.uploadImage(file); form.coverUrl = r.url; msg.value = `封面「${r.fileName}」已上传` }
  catch (err) { error.value = err.message }
  finally { uploading.value = false }
}
async function save(status) {
  if (!form.title.trim() || !form.body.trim()) { error.value = '标题与正文不能为空'; return }
  publishing.value = true; error.value = ''; msg.value = ''
  try {
    const payload = { title: form.title, category: form.category, tags: form.tags, body: form.body, coverUrl: form.coverUrl }
    if (editing.value) {
      await feature.updateContent(editing.value, { ...payload, submit: status === 'PENDING' })
      msg.value = status === 'PENDING' ? '已保存并重新送审' : '稿件已保存为草稿'
    } else {
      await feature.createContent({ ...payload, status })
      msg.value = status === 'PENDING' ? '已提交审核，等待管理员处理' : '草稿已保存'
    }
    reset(); tab.value = 'mine'; await loadMine()
  } catch (e) { error.value = e.message }
  finally { publishing.value = false }
}
function edit(c) {
  editing.value = c.id
  Object.assign(form, { title: c.title || '', category: c.category || '随笔', tags: c.tags || '', body: c.body || '', coverUrl: c.coverUrl || '' })
  tab.value = 'write'; msg.value = '正在编辑稿件，保存后可重新送审'
}
async function submit(c) { await act(() => feature.submitContent(c.id), '已提交审核') }
async function withdraw(c) { await act(() => feature.withdrawContent(c.id), '已撤回送审') }
async function remove(c) {
  if (!window.confirm(`确定删除「${c.title}」吗？相关评论、点赞、收藏会一并清理。`)) return
  await act(() => feature.removeContent(c.id), '稿件已删除')
}
async function act(fn, text) {
  error.value = ''; msg.value = ''
  try { await fn(); msg.value = text; await loadMine() } catch (e) { error.value = e.message }
}
onMounted(loadMine)
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / PUBLISH" title="写下这一刻"
      description="Markdown 写作与实时预览；按 D4 流程走草稿 → 提交审核 → 管理员发布，驳回理由可在「我的投稿」查看。"
      :actions="[{ label: '返回广场', icon: '←', color: 'slate', to: '/community' }]" />

    <div class="tabs">
      <button class="tab-btn" :class="{ 'is-active': tab === 'write' }" @click="tab = 'write'">{{ editing ? '编辑稿件' : '写文章' }}</button>
      <button class="tab-btn" :class="{ 'is-active': tab === 'mine' }" @click="tab = 'mine'; loadMine()">我的投稿 ({{ overview.total || mine.length }})</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <template v-if="tab === 'write'">
      <div class="grid gap-5 md:grid-cols-2">
        <div class="glass p-6">
          <div class="form-grid">
            <label class="span-2"><span class="field-label">标题 *</span><input v-model="form.title" class="field w-full" placeholder="给这段时光起个名字" /></label>
            <label><span class="field-label">分类</span><select v-model="form.category" class="field w-full"><option v-for="c in categories" :key="c">{{ c }}</option></select></label>
            <label><span class="field-label">标签（逗号分隔）</span><input v-model="form.tags" class="field w-full" placeholder="复盘,时间线" /></label>
            <label class="span-2"><span class="field-label">封面（可选）</span><input class="field w-full" type="file" accept="image/*" @change="upload" /></label>
            <label class="span-2"><span class="field-label">正文（Markdown）*</span><textarea v-model="form.body" class="field w-full font-mono" rows="16" placeholder="# 标题&#10;&#10;写点什么…"></textarea></label>
          </div>
          <div class="mt-4 flex flex-wrap gap-2">
            <button class="btn btn-mint" :disabled="publishing || uploading" @click="save('PENDING')"><span class="btn-glyph">✓</span> 提交审核</button>
            <button class="btn btn-amber" :disabled="publishing || uploading" @click="save('DRAFT')"><span class="btn-glyph">▤</span> 存草稿</button>
            <button v-if="editing" class="btn btn-slate" @click="reset()">取消编辑</button>
            <span v-if="form.coverUrl" class="pill pill-sky ml-auto">封面已就绪</span>
          </div>
        </div>
        <div class="glass p-6">
          <h2 class="section-title mb-4">实时预览</h2>
          <img v-if="form.coverUrl" :src="form.coverUrl" alt="封面" class="mb-4 max-h-[180px] w-full rounded-xl object-cover" />
          <h3 class="text-[18px] font-semibold ink">{{ form.title || '未命名' }}</h3>
          <div class="mt-3 flex flex-wrap gap-1.5">
            <span class="pill pill-rose">{{ form.category }}</span>
            <span v-for="t in form.tags.split(/[,，;；]/).filter(Boolean)" :key="t" class="pill pill-neutral">{{ t }}</span>
          </div>
          <div class="markdown-body mt-4 text-[13px] leading-7 muted" v-html="preview"></div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="glass mb-5 flex flex-wrap gap-2 p-4">
        <span v-for="(v, k) in (overview.counts || {})" :key="k" class="pill" :class="pills[k] || 'pill-neutral'">{{ labels[k] || k }} · {{ v }}</span>
      </div>
      <div v-if="!mine.length" class="glass p-10 text-center text-[13px] muted">还没有投稿，去「写文章」开始吧。</div>
      <div v-else class="space-y-3">
        <div v-for="c in mine" :key="c.id" class="glass p-5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill" :class="pills[c.status] || 'pill-neutral'">{{ labels[c.status] || c.status }}</span>
            <h3 class="text-[15px] font-semibold ink">{{ c.title }}</h3>
            <span class="ml-auto text-[11px] faint">{{ c.updatedAt || c.createdAt }}</span>
          </div>
          <p class="mt-2 text-[12px] muted">{{ c.summary || (c.body || '').slice(0, 90) }}</p>
          <div v-if="c.status === 'REJECTED' && c.reviewNote" class="glass-flat mt-3 p-3">
            <p class="text-[11px] faint">驳回理由（{{ c.reviewedAt || '管理员' }}）</p>
            <p class="mt-1 text-[12px] ink">{{ c.reviewNote }}</p>
          </div>
          <div class="mt-3 flex flex-wrap gap-2 border-t border-hairline pt-3">
            <RouterLink v-if="c.status === 'PUBLISHED'" :to="`/community/${c.id}`" class="btn btn-violet"><span class="btn-glyph">↗</span> 查看</RouterLink>
            <button v-if="c.status === 'DRAFT' || c.status === 'REJECTED'" class="btn btn-mint" @click="submit(c)">提交审核</button>
            <button v-if="c.status === 'DRAFT' || c.status === 'REJECTED'" class="btn btn-blue" @click="edit(c)">编辑改稿</button>
            <button v-if="c.status === 'PENDING'" class="btn btn-amber" @click="withdraw(c)">撤回</button>
            <button class="btn btn-rose ml-auto" @click="remove(c)">删除</button>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
