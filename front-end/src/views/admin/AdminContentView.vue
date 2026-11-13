<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const tab = ref('pending')
const pending = ref([]), list = ref([])
const keyword = ref(''), status = ref(''), msg = ref(''), error = ref('')
const rejecting = ref(null), rejectNote = ref(''), busy = ref(false)
const statuses = ['PENDING', 'DRAFT', 'PUBLISHED', 'REJECTED', 'HIDDEN', 'ARCHIVED']
const labels = { PENDING: '待审核', DRAFT: '草稿', PUBLISHED: '已发布', REJECTED: '已驳回', HIDDEN: '已下架', ARCHIVED: '已归档' }

async function load() {
  error.value = ''
  try {
    if (tab.value === 'pending') pending.value = (await adminApi.pendingContents())?.items || []
    else list.value = (await adminApi.contents({ status: status.value, keyword: keyword.value }))?.items || []
  } catch (e) { error.value = e.message }
}
async function review(c, action, note = '') {
  busy.value = true; msg.value = ''; error.value = ''
  try {
    await adminApi.reviewContent(c.id, action, note)
    msg.value = action === 'APPROVE'
      ? `「${c.title}」已通过审核并发布` : `「${c.title}」已驳回，作者可见理由`
    rejecting.value = null; rejectNote.value = ''
    await load()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
async function setStatus(c, s) {
  msg.value = ''; error.value = ''
  try { await adminApi.setContentStatus(c.id, s); msg.value = `「${c.title}」状态已更新为 ${labels[s] || s}`; await load() }
  catch (e) { error.value = e.message }
}
async function remove(c) {
  if (!window.confirm(`确定删除「${c.title}」吗？该文章的评论、点赞、收藏会一并删除，且不可恢复。`)) return
  msg.value = ''; error.value = ''
  try { await adminApi.deleteContent(c.id); msg.value = '文章及其关联数据已删除（操作已记入审计）'; await load() }
  catch (e) { error.value = e.message }
}
function switchTab(t) { tab.value = t; load() }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ADMIN / CONTENT" title="内容治理"
      description="按 D4 流程审核待发布稿件；通过、驳回（带理由）、下架、归档与删除都会写入审计日志。"
      :actions="[{ label: '内容广场', icon: '◈', color: 'blue', to: '/community' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />

    <div class="tabs">
      <button class="tab-btn" :class="{ 'is-active': tab === 'pending' }" @click="switchTab('pending')">待审核 ({{ pending.length }})</button>
      <button class="tab-btn" :class="{ 'is-active': tab === 'all' }" @click="switchTab('all')">全部内容 ({{ list.length }})</button>
    </div>
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <template v-if="tab === 'pending'">
      <div v-if="!pending.length" class="glass p-10 text-center text-[13px] muted">没有待审核的稿件。</div>
      <div v-else class="space-y-3">
        <div v-for="c in pending" :key="c.id" class="glass p-5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill pill-amber">待审核</span>
            <h3 class="text-[15px] font-semibold ink">{{ c.title }}</h3>
            <span class="ml-auto text-[11px] faint">{{ c.authorName }} · {{ c.updatedAt || c.createdAt }}</span>
          </div>
          <p class="mt-2 text-[12px] leading-6 muted">{{ c.summary || '（无摘要）' }}</p>
          <div class="mt-2 flex flex-wrap gap-1.5">
            <span v-if="c.category" class="pill pill-sky">{{ c.category }}</span>
            <span v-for="t in String(c.tags || '').split(/[,，;；]/).filter(Boolean)" :key="t" class="pill pill-neutral">{{ t }}</span>
          </div>
          <div v-if="rejecting === c.id" class="glass-flat mt-3 p-3">
            <label class="block"><span class="field-label">驳回理由（作者可见）*</span>
              <textarea v-model="rejectNote" class="field mt-1 w-full" rows="2" placeholder="请说明需要修改的地方…"></textarea>
            </label>
            <div class="mt-2 flex gap-2">
              <button class="btn btn-rose" :disabled="busy || !rejectNote.trim()" @click="review(c, 'REJECT', rejectNote)">确认驳回</button>
              <button class="btn btn-slate" @click="rejecting = null; rejectNote = ''">取消</button>
            </div>
          </div>
          <div v-else class="mt-3 flex flex-wrap gap-2 border-t border-hairline pt-3">
            <button class="btn btn-mint" :disabled="busy" @click="review(c, 'APPROVE')"><span class="btn-glyph">✓</span> 通过并发布</button>
            <button class="btn btn-amber" :disabled="busy" @click="rejecting = c.id; rejectNote = ''">驳回</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="crud-toolbar">
        <input v-model="keyword" class="field max-w-[240px]" placeholder="按标题 / 作者搜索" @keyup.enter="load" />
        <select v-model="status" class="field max-w-[180px]" @change="load">
          <option value="">全部状态</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ labels[s] }}</option>
        </select>
        <button class="btn btn-blue" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      </div>
      <div class="glass overflow-x-auto p-1">
        <table class="data-table w-full">
          <thead><tr><th>ID</th><th>标题</th><th>作者</th><th>分类</th><th>状态</th><th>浏览</th><th>审核意见</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="c in list" :key="c.id">
              <td>{{ c.id }}</td>
              <td class="max-w-[180px] truncate">{{ c.title || '—' }}</td>
              <td>{{ c.authorName || '—' }}</td>
              <td>{{ c.category || '—' }}</td>
              <td><span class="pill" :class="c.status === 'PUBLISHED' ? 'pill-mint' : c.status === 'PENDING' ? 'pill-amber' : 'pill-neutral'">{{ labels[c.status] || c.status || '—' }}</span></td>
              <td>{{ c.viewCount ?? 0 }}</td>
              <td class="max-w-[180px] truncate text-[11px] faint">{{ c.reviewNote || '—' }}<span v-if="c.reviewedAt" class="block">{{ c.reviewedAt }}</span></td>
              <td class="whitespace-nowrap">
                <button class="btn btn-mint" @click="setStatus(c, 'PUBLISHED')">发布</button>
                <button class="btn btn-amber ml-1" @click="setStatus(c, 'HIDDEN')">下架</button>
                <button class="btn btn-slate ml-1" @click="setStatus(c, 'ARCHIVED')">归档</button>
                <button class="btn btn-rose ml-1" @click="remove(c)">删除</button>
              </td>
            </tr>
            <tr v-if="!list.length"><td colspan="8" class="text-center muted">暂无内容</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
