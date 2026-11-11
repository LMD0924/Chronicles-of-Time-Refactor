<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'
import EmptyState from '@/components/EmptyState.vue'

const entries = ref([])
const stats = ref({})
const loading = ref(true)
const activeType = ref('')
const error = ref('')

const types = ['记录', '里程碑', '工作', '进阶', '成绩', '练习', '规划', '目标', '文章', '证书', '论文']
const filtered = computed(() => activeType.value ? entries.value.filter(e => e.type === activeType.value) : entries.value)
const metrics = computed(() => [
  { label: '全部节点', value: entries.value.length, color: 'sky' },
  { label: '记录', value: stats.value.totalRecords || 0, color: 'mint' },
  { label: '里程碑', value: stats.value.milestones || 0, color: 'amber' },
  { label: '学习时长', value: Math.round(stats.value.studyHours || 0), color: 'violet' },
])

async function load() {
  loading.value = true; error.value = ''
  try {
    const [feed, s] = await Promise.all([feature.timelineFeed(), feature.timelineStats()])
    entries.value = feed || []; stats.value = s || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="CHRONICLE / ONE TIMELINE" title="拾光时间线"
      description="记录、学习、规划、社区与档案汇聚于此。节点按时间倒序排列，点击可跳回来源功能。"
      :actions="[
        { label: '写记录', icon: '＋', color: 'mint', to: '/timeline/records' },
        { label: '里程碑', icon: '★', color: 'amber', to: '/timeline/milestones' },
        { label: '贯通关联', icon: '⌘', color: 'violet', to: '/timeline/connections' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>

    <div class="mt-6 flex flex-wrap gap-1.5">
      <button class="chip" :class="{ 'is-active': !activeType }" @click="activeType = ''">全部</button>
      <button v-for="t in types" :key="t" class="chip" :class="{ 'is-active': activeType === t }" @click="activeType = t">{{ t }}</button>
    </div>

    <p v-if="error" class="mt-4 pill pill-rose">{{ error }}</p>
    <div v-if="loading" class="mt-6 space-y-3"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:78px"></div></div>
    <EmptyState v-else-if="!filtered.length" class="mt-6" text="时间线上还没有节点" hint="从「写记录」或任一功能开始，数据会自动汇入这里" />

    <div v-else class="tl-rail mt-7">
      <div v-for="e in filtered" :key="e.id" class="tl-node mb-5" :class="{ 'tl-milestone': e.type === '里程碑' }" v-reveal="1">
        <div class="glass lift p-5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill" :class="`pill-${e.color}`">{{ e.type }}</span>
            <span class="text-[11px] faint">{{ e.occurredAt }}</span>
            <span class="ml-auto text-[11px] faint">#{{ e.sourceId }}</span>
          </div>
          <h3 class="mt-2 text-[15px] font-semibold ink">{{ e.title }}</h3>
          <p v-if="e.summary" class="mt-1 text-[12px] leading-6 muted">{{ e.summary }}</p>
          <RouterLink :to="e.link" class="btn btn-teal mt-3"><span class="btn-glyph">→</span> 查看来源</RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>
