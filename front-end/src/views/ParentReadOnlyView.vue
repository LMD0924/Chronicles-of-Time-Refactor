<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const links = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const title = ref('我的拾光时间线')
const days = ref(7)

const activeCount = computed(() => links.value.filter(l => !l.revoked && !l.expired).length)

function fullUrl(path) { return window.location.origin + path }

async function load() {
  loading.value = true; error.value = ''
  try { links.value = (await feature.shareLinks()) || [] }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function copy(path, silent = false) {
  try {
    await navigator.clipboard.writeText(fullUrl(path))
    if (!silent) notice.value = '链接已复制到剪贴板'
  } catch {
    // 非 HTTPS 环境下 clipboard API 可能不可用，降级为选中文本复制
    const el = document.createElement('textarea')
    el.value = fullUrl(path); document.body.appendChild(el); el.select()
    try { document.execCommand('copy'); if (!silent) notice.value = '链接已复制到剪贴板' }
    catch { error.value = '复制失败，请手动复制下方链接' }
    document.body.removeChild(el)
  }
}

async function create() {
  saving.value = true; error.value = ''; notice.value = ''
  try {
    const r = await feature.createShareLink({ title: title.value.trim() || '我的拾光时间线', days: Number(days.value) })
    await load()
    await copy(r.url, true)
    notice.value = '已生成分享链接并复制，可直接发给家人'
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

function open(path) { window.open(fullUrl(path), '_blank', 'noopener') }

async function act(fn, label) {
  error.value = ''; notice.value = ''
  try { await fn(); notice.value = label + '成功'; await load() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SHARE" title="只读分享"
      description="生成一条免登录的只读链接，家人打开即可看到汇总与时间线节点；可设有效期、随时撤销，撤销后立即失效。"
      :actions="[{ label: '刷新', icon: '↻', color: 'slate', click: load }, { label: '回到时间线', icon: '←', color: 'blue', to: '/timeline' }]" />

    <div class="grid gap-5 md:grid-cols-[1fr_1.6fr]">
      <div class="glass p-6">
        <p class="eyebrow">NEW LINK</p>
        <h2 class="section-title mt-2 mb-5">生成分享链接</h2>
        <label class="block"><span class="field-label">分享标题</span>
          <input v-model="title" class="field w-full" placeholder="给家人看的名字" /></label>
        <label class="mt-4 block"><span class="field-label">有效期（天）</span>
          <input v-model="days" class="field w-full" type="number" min="1" max="365" /></label>
        <div class="mt-3 flex flex-wrap gap-2">
          <button v-for="d in [1, 7, 30, 90]" :key="d" class="btn btn-slate" type="button" @click="days = d">{{ d }} 天</button>
        </div>
        <button class="btn btn-violet mt-6 w-full" type="button" :disabled="saving" @click="create">
          <span class="btn-glyph">＋</span> {{ saving ? '生成中…' : '生成并复制链接' }}
        </button>
        <p class="mt-4 text-[11px] leading-6 faint">
          链接本身就是访问凭证，任何拿到地址的人都能查看（不含任何编辑权限，也不暴露账号信息）。
          建议只发给信任的人，不需要时点「撤销」。
        </p>
      </div>

      <div class="glass p-6">
        <div class="mb-4 flex items-center justify-between">
          <div><p class="eyebrow">MY LINKS</p><h2 class="section-title mt-2">已生成的链接</h2></div>
          <span class="pill pill-mint">{{ activeCount }} 条生效中 / 共 {{ links.length }} 条</span>
        </div>
        <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
        <p v-if="notice" class="pill pill-mint mb-3">{{ notice }}</p>

        <div v-if="loading" class="grid gap-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:96px"></div></div>
        <EmptyState v-else-if="!links.length" text="还没有分享链接" hint="在左侧填好标题和有效期，点「生成并复制链接」" />
        <div v-else class="grid gap-3">
          <div v-for="l in links" :key="l.id" class="glass-flat p-4" :class="{ 'opacity-60': l.revoked || l.expired }">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <div class="min-w-0">
                <h3 class="text-[14px] font-medium ink">{{ l.title }}</h3>
                <p class="mt-1 truncate font-mono text-[11px] faint">{{ l.url }}</p>
              </div>
              <div class="flex flex-wrap gap-1.5">
                <span v-if="l.revoked" class="pill pill-rose">已撤销</span>
                <span v-else-if="l.expired" class="pill pill-amber">已过期</span>
                <span v-else class="pill pill-mint">生效中</span>
                <span class="pill pill-sky">{{ l.viewCount }} 次查看</span>
              </div>
            </div>
            <p class="mt-2 text-[11px] faint">有效期至 {{ l.expiresAt || '未设置' }} · 创建于 {{ l.createdAt }}<span v-if="l.lastViewedAt"> · 最近查看 {{ l.lastViewedAt }}</span></p>
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="btn btn-blue" type="button" @click="copy(l.url)"><span class="btn-glyph">⧉</span> 复制链接</button>
              <button class="btn btn-mint" type="button" :disabled="l.revoked || l.expired" @click="open(l.url)"><span class="btn-glyph">↗</span> 预览</button>
              <button v-if="!l.revoked" class="btn btn-amber" type="button" @click="act(() => feature.revokeShareLink(l.id), '撤销')">撤销</button>
              <button v-else class="btn btn-violet" type="button" @click="act(() => feature.renewShareLink(l.id, 7), '续期')">重新启用</button>
              <button class="btn btn-slate" type="button" @click="act(() => feature.renewShareLink(l.id, 30), '续期')">续期 30 天</button>
              <button class="btn btn-rose" type="button" @click="act(() => feature.deleteShareLink(l.id), '删除')">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
