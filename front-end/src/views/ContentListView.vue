<script setup>
import { onMounted, ref, watch } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const list = ref([]), hot = ref([]), cats = ref({})
const keyword = ref(''), category = ref('')
const loading = ref(true), error = ref('')

async function load() {
  loading.value = true; error.value = ''
  try {
    const [l, h, c] = await Promise.all([
      feature.searchContents({ keyword: keyword.value, category: category.value }),
      feature.hotContents(5), feature.categoryStats()
    ])
    list.value = l || []; hot.value = h || []; cats.value = c || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function summary(c) {
  const s = c.summary || c.body || ''
  return s.length > 90 ? s.slice(0, 90) + '…' : s
}
onMounted(load)
watch(category, load)
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / FEED" title="内容广场"
      description="文章、随笔与复盘集中在这里；游客可读，发布与互动需要登录。"
      :actions="[{ label: '写文章', icon: '✎', color: 'mint', to: '/community/publish' }, { label: '文章图谱', icon: '⌘', color: 'violet', to: '/community/graph' }, { label: '标签洞察', icon: '◈', color: 'amber', to: '/community/tags' }]" />

    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[280px]" placeholder="搜索标题、正文或标签…" @keyup.enter="load" />
      <select v-model="category" class="field max-w-[180px]">
        <option value="">全部分类</option>
        <option v-for="(v, k) in cats" :key="k" :value="k">{{ k }} ({{ v }})</option>
      </select>
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      <span class="pill pill-sky">共 {{ list.length }} 篇</span>
    </div>
    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>

    <div class="grid gap-5 md:grid-cols-[1fr_300px]">
      <div>
        <div v-if="loading" class="space-y-3"><div v-for="i in 3" :key="i" class="placeholder-bar" style="height:110px"></div></div>
        <EmptyState v-else-if="!list.length" text="还没有文章" hint="换个关键词，或去写一篇" />
        <div v-else class="space-y-3">
          <RouterLink v-for="c in list" :key="c.id" :to="`/community/${c.id}`" class="glass lift block p-5" v-reveal="1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="pill pill-rose">{{ c.category || '随笔' }}</span>
              <span class="text-[11px] faint">{{ c.createdAt }}</span>
              <span class="ml-auto text-[11px] faint">♡ {{ c.likeCount || 0 }} · ☆ {{ c.favoriteCount || 0 }}</span>
            </div>
            <h2 class="mt-2 text-[16px] font-semibold ink">{{ c.title }}</h2>
            <p class="mt-2 text-[12px] leading-6 muted">{{ summary(c) }}</p>
            <div class="mt-3 flex flex-wrap gap-1.5">
              <span v-for="t in String(c.tags || '').split('[,，;；]')" :key="t" class="pill pill-neutral">{{ t }}</span>
            </div>
          </RouterLink>
        </div>
      </div>
      <aside class="space-y-4">
        <div class="glass p-5">
          <h2 class="section-title mb-3">热门文章</h2>
          <RouterLink v-for="(h, i) in hot" :key="h.id" :to="`/community/${h.id}`" class="flex items-start gap-2 py-2 text-[12px] hover:text-accent">
            <span class="pill pill-amber">{{ i + 1 }}</span><span class="ink">{{ h.title }}</span>
          </RouterLink>
          <p v-if="!hot.length" class="text-[12px] muted">暂无热门</p>
        </div>
        <div class="glass p-5">
          <h2 class="section-title mb-3">分类统计</h2>
          <div class="flex flex-wrap gap-2">
            <span v-for="(v, k) in cats" :key="k" class="pill pill-sky">{{ k }} · {{ v }}</span>
            <span v-if="!Object.keys(cats).length" class="text-[12px] muted">暂无数据</span>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>
