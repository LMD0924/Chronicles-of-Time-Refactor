<script setup>
import { onMounted, ref } from 'vue'
import http from '@/utils/request'

const listContents = (params) => http.get('/api/v1/admin/contents', { params })
const updateStatus = (id, status) => http.put(`/api/v1/admin/contents/${id}/status`, null, { params: { status } })
const removeContent = (id) => http.delete(`/api/v1/admin/contents/${id}`)

const list = ref([]), keyword = ref(''), status = ref(''), msg = ref(''), error = ref('')
const statuses = ['PUBLISHED', 'HIDDEN', 'ARCHIVED', 'DRAFT']

async function load() {
  error.value = ''
  try {
    const data = await listContents({ status: status.value, keyword: keyword.value })
    list.value = data?.items || []
  } catch (e) { error.value = e.message }
}
async function setStatus(c, s) {
  msg.value = ''; error.value = ''
  try {
    await updateStatus(c.id, s)
    msg.value = `「${c.title}」状态已更新为 ${s}（操作已记入审计）`
    await load()
  } catch (e) { error.value = e.message }
}
async function remove(c) {
  msg.value = ''; error.value = ''
  if (!window.confirm(`确定删除「${c.title}」吗？该文章的评论、点赞、收藏会一并删除，且不可恢复。`)) return
  try {
    await removeContent(c.id)
    msg.value = '文章及其关联数据已删除（操作已记入审计）'
    await load()
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title mb-4">内容治理</h2>
    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[240px]" placeholder="按标题 / 作者搜索" @keyup.enter="load" />
      <select v-model="status" class="field max-w-[180px]" @change="load">
        <option value="">全部状态</option>
        <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
      </select>
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      <button class="btn btn-slate" @click="load"><span class="btn-glyph">↻</span> 刷新</button>
      <span class="pill pill-sky">跨账号 {{ list.length }} 篇</span>
    </div>
    <p v-if="msg" class="mb-2 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="mb-2 text-[12px] text-rose-500">{{ error }}</p>

    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr><th>ID</th><th>标题</th><th>作者</th><th>分类</th><th>状态</th><th>浏览量</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in list" :key="c.id">
            <td>{{ c.id }}</td>
            <td class="max-w-[200px] truncate">{{ c.title || '—' }}</td>
            <td>{{ c.authorName || '—' }}</td>
            <td>{{ c.category || '—' }}</td>
            <td><span class="pill" :class="c.status === 'PUBLISHED' ? 'pill-mint' : 'pill-amber'">{{ c.status || '—' }}</span></td>
            <td>{{ c.viewCount ?? 0 }}</td>
            <td class="whitespace-nowrap">
              <button class="btn btn-mint" @click="setStatus(c, 'PUBLISHED')">发布</button>
              <button class="btn btn-amber ml-1" @click="setStatus(c, 'HIDDEN')">下架</button>
              <button class="btn btn-slate ml-1" @click="setStatus(c, 'ARCHIVED')">归档</button>
              <button class="btn btn-rose ml-1" @click="remove(c)">删除</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="7" class="text-center muted">暂无内容</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
