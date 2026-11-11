<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import Chart from '@/components/Chart.vue'

const tags = ref({}), cats = ref({}), related = ref([])
const active = ref('')
const option = computed(() => ({
  tooltip: {}, xAxis: { type: 'category', data: Object.keys(cats.value) }, yAxis: { type: 'value' },
  series: [{ type: 'bar', data: Object.values(cats.value), itemStyle: { color: '#7c4de0', borderRadius: [6, 6, 0, 0] } }]
}))
async function pick(t) { active.value = t; try { related.value = (await feature.related(t)) || [] } catch { /* ignore */ } }
onMounted(async () => {
  try { const [a, b] = await Promise.all([feature.tagCloud(), feature.categoryStats()]); tags.value = a || {}; cats.value = b || {} } catch { /* ignore */ }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / INSIGHTS" title="标签洞察"
      description="标签云与分类分布；点选标签可查看使用该标签的文章。"
      :actions="[{ label: '文章图谱', icon: '⌘', color: 'violet', to: '/community/graph' }, { label: '内容广场', icon: '←', color: 'blue', to: '/community' }]" />
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title mb-4">标签云</h2>
        <div class="flex flex-wrap gap-2">
          <button v-for="(v, k) in tags" :key="k" class="pill" :class="active === k ? 'pill-rose' : 'pill-sky'" @click="pick(k)">
            {{ k }} <span class="faint">×{{ v }}</span>
          </button>
          <span v-if="!Object.keys(tags).length" class="text-[12px] muted">暂无标签</span>
        </div>
        <div v-if="active" class="mt-5">
          <h3 class="eyebrow mb-2">「{{ active }}」下的文章</h3>
          <RouterLink v-for="r in related" :key="r.id" :to="`/community/${r.id}`" class="block py-1 text-[12px] hover:text-accent">{{ r.title }}</RouterLink>
          <p v-if="!related.length" class="text-[12px] muted">暂无文章</p>
        </div>
      </div>
      <div class="glass p-5"><h2 class="section-title mb-2">分类分布</h2><Chart :option="option" :height="280" /></div>
    </div>
  </section>
</template>
