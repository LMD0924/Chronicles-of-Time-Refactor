<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const tags = ref({}), cats = ref({}), related = ref([]), articles = ref([])
const active = ref(''), activeCategory = ref(''), loading = ref(true), error = ref('')
const option = computed(() => ({
  tooltip: {}, xAxis: { type: 'category', data: Object.keys(cats.value) }, yAxis: { type: 'value' },
  series: [{ type: 'bar', data: Object.values(cats.value), itemStyle: { color: '#7c4de0', borderRadius: [6, 6, 0, 0] } }]
}))
const tagList = computed(() => Object.entries(tags.value).sort((a, b) => b[1] - a[1]))
const maxTag = computed(() => tagList.value.length ? tagList.value[0][1] : 1)

async function pick(t) {
  active.value = t; activeCategory.value = ''; error.value = ''
  try {
    const [r, l] = await Promise.all([feature.related(t), feature.searchContents({ tag: t })])
    related.value = r || []; articles.value = l || []
  } catch (e) { error.value = e.message }
}
async function pickCategory(c) {
  activeCategory.value = c; active.value = ''; error.value = ''
  try { articles.value = (await feature.searchContents({ category: c })) || [] } catch (e) { error.value = e.message }
}
function onChartClick(p) { if (p?.name) pickCategory(p.name) }
onMounted(async () => {
  loading.value = true
  try {
    const [a, b] = await Promise.all([feature.tagCloud(), feature.categoryStats()])
    tags.value = a || {}; cats.value = b || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / INSIGHTS" title="标签洞察"
      description="标签与分类只统计已发布文章；点选标签或分类可立即筛出对应文章。"
      :actions="[{ label: '文章图谱', icon: '⌘', color: 'violet', to: '/community/graph' }, { label: '内容广场', icon: '←', color: 'blue', to: '/community' }]" />
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>

    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title mb-4">标签云</h2>
        <div v-if="loading" class="placeholder-bar" style="height:140px"></div>
        <div v-else class="flex flex-wrap gap-2">
          <button v-for="[k, v] in tagList" :key="k" class="pill" :class="active === k ? 'pill-rose' : 'pill-sky'"
            :style="{ fontSize: (11 + Math.round(v / maxTag * 5)) + 'px' }" @click="pick(k)">
            {{ k }} <span class="faint">×{{ v }}</span>
          </button>
          <span v-if="!tagList.length" class="text-[12px] muted">暂无标签</span>
        </div>
        <p class="mt-4 text-[11px] faint">字越大表示该标签下已发布文章越多。</p>
      </div>
      <div class="glass p-5"><h2 class="section-title mb-2">分类分布</h2><Chart :option="option" :height="280" @point-click="onChartClick" /></div>
    </div>

    <div v-if="active || activeCategory" class="glass mt-5 p-6">
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <h2 class="section-title">{{ active ? `标签「${active}」` : `分类「${activeCategory}」` }}下的文章</h2>
        <span class="pill pill-sky">{{ articles.length }} 篇</span>
        <button class="btn btn-slate ml-auto" @click="active = ''; activeCategory = ''; articles = []">清除筛选</button>
      </div>
      <RouterLink v-for="a in articles" :key="a.id" :to="`/community/${a.id}`"
        class="glass-flat mb-2 flex flex-wrap items-center gap-2 p-3 hover:text-accent">
        <span class="text-[13px] ink">{{ a.title }}</span>
        <span class="pill pill-neutral">{{ a.category }}</span>
        <span class="ml-auto text-[11px] faint">浏览 {{ a.viewCount || 0 }} · {{ a.createdAt }}</span>
      </RouterLink>
      <p v-if="!articles.length" class="text-[12px] muted">暂无文章</p>
      <div v-if="active && related.length" class="mt-3 text-[11px] faint">按共同标签关联到 {{ related.length }} 篇文章</div>
    </div>
  </section>
</template>
