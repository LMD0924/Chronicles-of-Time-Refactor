<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StateBlock from '@/components/StateBlock.vue'

const route = useRoute(), router = useRouter()
const q = ref(String(route.query.q || ''))
const result = ref({ keyword: '', total: 0, groups: [] })
const loading = ref(false), error = ref(''), searched = ref(false)
const quick = ['时间线', '复盘', '函数', '高等数学', '论文', '志愿', '证书']
const groups = computed(() => result.value.groups || [])

async function search(keyword) {
  if (keyword !== undefined) q.value = keyword
  if (!q.value.trim()) { result.value = { keyword: '', total: 0, groups: [] }; searched.value = false; return }
  loading.value = true; error.value = ''; searched.value = true
  try {
    result.value = (await feature.globalSearch(q.value.trim(), 10)) || { groups: [], total: 0 }
    router.replace({ path: '/search', query: { q: q.value.trim() } })
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function go(url) { if (url) router.push(url) }
watch(() => route.query.q, v => { const next = String(v || ''); if (next && next !== q.value) { q.value = next; search() } })
onMounted(() => { if (q.value) search() })
</script>

<template>
  <section>
    <PageHeader eyebrow="GLOBAL / SEARCH" title="全局搜索"
      description="一条关键词检索时间线、题库、规划、档案与社区文章；私有数据只搜当前账号，广场只搜已发布文章。"
      :actions="[{ label: '拾光时间线', icon: '⌁', color: 'mint', to: '/timeline' }, { label: '内容广场', icon: '◈', color: 'blue', to: '/community' }]" />

    <div class="glass mb-5 p-5">
      <div class="crud-toolbar">
        <input v-model="q" class="field max-w-[420px]" type="search" placeholder="搜索标题、标签、正文…"
               @keyup.enter="search()" />
        <button class="btn btn-blue" type="button" :disabled="loading" @click="search()">
          <span class="btn-glyph">⌕</span> {{ loading ? '搜索中…' : '搜索' }}
        </button>
        <button v-if="q" class="btn btn-slate" type="button" @click="q = ''; result = { groups: [], total: 0 }; searched = false">清空</button>
        <span v-if="searched && !loading && !error" class="pill pill-sky">命中 {{ result.total }} 条</span>
      </div>
      <div class="mt-3 flex flex-wrap items-center gap-1.5">
        <span class="text-[11px] faint">试试：</span>
        <button v-for="k in quick" :key="k" class="pill pill-neutral" @click="search(k)">{{ k }}</button>
      </div>
    </div>

    <StateBlock :loading="loading" :error="error" :empty="!searched" :rows="3"
      text="输入关键词开始检索" hint="支持标题、标签与正文内容" @retry="search()" >
      <StateBlock :empty="searched && !groups.length" text="没有找到相关内容" hint="换个关键词，或去对应模块新增一条记录">
        <div class="space-y-5">
          <div v-for="g in groups" :key="g.label" class="glass p-5">
            <div class="mb-3 flex items-center gap-2">
              <h2 class="section-title">{{ g.label }}</h2>
              <span class="pill pill-sky ml-auto">{{ g.items.length }} 条</span>
            </div>
            <div class="grid gap-2 md:grid-cols-2">
              <button v-for="item in g.items" :key="g.label + ':' + item.id" type="button"
                      class="glass-flat flex flex-wrap items-center gap-2 p-3 text-left hover:text-accent" @click="go(item.url)">
                <span class="text-[13px] font-medium ink">{{ item.title }}</span>
                <span v-if="item.subtitle && item.subtitle !== '未命名'" class="pill pill-neutral">{{ item.subtitle }}</span>
                <span class="ml-auto text-[11px] faint">{{ item.date || '' }} →</span>
              </button>
            </div>
          </div>
        </div>
      </StateBlock>
    </StateBlock>
  </section>
</template>
