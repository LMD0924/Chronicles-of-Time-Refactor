<script setup>
import { onMounted, ref } from 'vue'
import { R } from '@/api'

const list = ref([]), keyword = ref(''), msg = ref('')
async function load() { try { list.value = (await R.community.contents.all(keyword.value)) || [] } catch (e) { msg.value = e.message } }
async function setStatus(c, status) { try { await R.community.contents.update(c.id, { ...c, status }); msg.value = '状态已更新：' + status; await load() } catch (e) { msg.value = e.message } }
async function remove(c) { if (!window.confirm(`删除「${c.title}」？`)) return; try { await R.community.contents.remove(c.id); await load() } catch (e) { msg.value = e.message } }
onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title mb-4">内容治理</h2>
    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[240px]" placeholder="搜索文章…" @keyup.enter="load" />
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      <span class="pill pill-sky">跨账号 {{ list.length }} 篇</span>
    </div>
    <p v-if="msg" class="mb-2 text-[12px] muted">{{ msg }}</p>
    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr><th>ID</th><th>标题</th><th>作者</th><th>分类</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in list" :key="c.id">
            <td>{{ c.id }}</td><td>{{ c.title }}</td><td>{{ c.authorName }}</td><td>{{ c.category }}</td>
            <td><span class="pill" :class="c.status === 'PUBLISHED' ? 'pill-mint' : 'pill-amber'">{{ c.status }}</span></td>
            <td class="whitespace-nowrap">
              <button class="btn btn-mint" @click="setStatus(c, 'PUBLISHED')">发布</button>
              <button class="btn btn-amber ml-1" @click="setStatus(c, 'HIDDEN')">下架</button>
              <button class="btn btn-rose ml-1" @click="remove(c)">删除</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="6" class="text-center muted">暂无内容</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
