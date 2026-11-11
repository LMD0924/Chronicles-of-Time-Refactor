<script setup>
import { onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const tab = ref('papers')
const papers = ref([]), suggestions = ref([])
const currentId = ref(''), author = ref(''), content = ref('')
const msg = ref('')
const papersCfg = {
  eyebrow: 'ARCHIVE / PAPERS', title: '论文管理', description: '标题、摘要、关键词、导师与状态。', color: 'violet',
  api: R.archive.papers,
  fields: [{ key: 'title', label: '标题', required: true }, { key: 'keywords', label: '关键词' },
           { key: 'status', label: '状态', type: 'select', options: ['选题', '开题', '初稿', '修改', '定稿', '答辩'] },
           { key: 'advisor', label: '导师' }, { key: 'fileUrl', label: '附件地址' },
           { key: 'abstractText', label: '摘要', type: 'textarea', span: 2 }],
  columns: [{ key: 'title', label: '标题' }, { key: 'status', label: '状态' }, { key: 'advisor', label: '导师' }, { key: 'keywords', label: '关键词' }]
}
const sugCfg = {
  eyebrow: 'ARCHIVE / SUGGESTIONS', title: '修改建议', description: '按论文记录逐条归档导师或同伴的修改意见。', color: 'amber',
  api: R.archive.paperSuggestions,
  fields: [{ key: 'paperId', label: '论文 ID', type: 'number' }, { key: 'author', label: '提出人' },
           { key: 'content', label: '建议内容', type: 'textarea', span: 2 }],
  columns: [{ key: 'paperId', label: '论文' }, { key: 'author', label: '提出人' }, { key: 'content', label: '建议' }]
}
async function load() {
  try {
    papers.value = (await R.archive.papers.list()) || []
    suggestions.value = (await feature.paperSuggestions(currentId.value || null)) || []
  } catch (e) { msg.value = e.message }
}
async function addSuggestion() {
  if (!currentId.value || !content.value) { msg.value = '请填写论文 ID 与建议内容'; return }
  try { await R.archive.paperSuggestions.save({ paperId: Number(currentId.value), author: author.value, content: content.value }); content.value = ''; msg.value = '已添加建议'; await load() }
  catch (e) { msg.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / PAPER" title="论文"
      description="论文写作、状态推进与修改建议在同一处完成。"
      :actions="[{ label: '论文看板', icon: '▤', color: 'teal', to: '/archive/thesis' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />
    <div class="tabs">
      <button class="tab-btn" :class="{ 'is-active': tab === 'papers' }" @click="tab = 'papers'">论文列表</button>
      <button class="tab-btn" :class="{ 'is-active': tab === 'suggestions' }" @click="tab = 'suggestions'">修改建议</button>
    </div>

    <template v-if="tab === 'papers'"><ResourcePage v-bind="papersCfg" /></template>
    <template v-else>
      <div class="grid gap-5 md:grid-cols-2">
        <div class="glass p-6">
          <h2 class="section-title mb-4">新增建议</h2>
          <div class="form-grid">
            <label><span class="field-label">论文 ID</span><input v-model="currentId" class="field w-full" type="number" @change="load" /></label>
            <label><span class="field-label">提出人</span><input v-model="author" class="field w-full" /></label>
            <label class="span-2"><span class="field-label">建议内容</span><textarea v-model="content" class="field w-full" rows="4"></textarea></label>
          </div>
          <button class="btn btn-amber mt-4" @click="addSuggestion"><span class="btn-glyph">＋</span> 添加</button>
          <p v-if="msg" class="mt-3 text-[12px] muted">{{ msg }}</p>
        </div>
        <div class="glass p-6">
          <h2 class="section-title mb-4">建议列表 ({{ suggestions.length }})</h2>
          <div v-for="s in suggestions" :key="s.id" class="glass-flat mb-2 p-3">
            <p class="text-[12px] ink">{{ s.content }}</p><p class="mt-1 text-[11px] faint">{{ s.author }} · 论文 #{{ s.paperId }}</p>
          </div>
          <p v-if="!suggestions.length" class="text-[12px] muted">暂无建议</p>
        </div>
      </div>
      <div class="mt-7"><ResourcePage v-bind="sugCfg" /></div>
    </template>
  </section>
</template>
