<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'

const stats = ref({})
const error = ref('')
const quick = [
  { to: '/planning/selection', label: '选科中心', desc: '意向 · 指导 · 历史 · 审批', color: 'violet', icon: '◎' },
  { to: '/planning/volunteer', label: '志愿填报', desc: '检索 · 推荐 · 模拟 · 方案', color: 'amber', icon: '▤' },
  { to: '/planning/review', label: '复习计划', desc: '拖动进度 · 拆里程碑', color: 'teal', icon: '✎' },
  { to: '/planning/target', label: '目标路径', desc: '阶段推进', color: 'sky', icon: '⌘' },
  { to: '/planning/gpa', label: 'GPA 模拟', desc: '可调参推演', color: 'mint', icon: '◷' },
  { to: '/planning/gap', label: '毕业缺口', desc: '学分核对', color: 'rose', icon: '△' },
  { to: '/planning/checklist', label: '强基清单', desc: '逐项勾选', color: 'blue', icon: '✓' },
  { to: '/planning/share', label: '只读分享', desc: '免登录链接', color: 'violet', icon: '↗' },
]
const metrics = computed(() => [
  { label: '复习计划', value: stats.value.reviewPlans || 0, color: 'teal' },
  { label: '目标阶段', value: stats.value.targetStages || 0, color: 'violet' },
  { label: '志愿方案', value: stats.value.volunteerPlans || 0, color: 'amber' },
  { label: '课程学分', value: stats.value.totalCredits || 0, color: 'mint' },
])
const reviewRate = computed(() => stats.value.reviewPlans
  ? Math.round((stats.value.reviewDone || 0) * 100 / stats.value.reviewPlans) : 0)
const checkRate = computed(() => stats.value.checklistTotal
  ? Math.round((stats.value.checklistDone || 0) * 100 / stats.value.checklistTotal) : 0)

async function load() {
  error.value = ''
  try { stats.value = (await feature.planningOverview()) || {} }
  catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / OVERVIEW" title="规划总览"
      description="选科、志愿、复习、目标、学分与分享都挂在同一个用户身上，用一条时间线串起来，而不是按阶段切开。"
      :actions="[
        { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' },
        { label: '志愿填报', icon: '▤', color: 'amber', to: '/planning/volunteer' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>
    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>

    <div class="mt-6 grid gap-5 md:grid-cols-2">
      <div class="glass p-6" v-reveal="1">
        <p class="eyebrow">CONNECTED BY TIME</p>
        <h2 class="section-title mt-2">这一域的数据也会回到时间线</h2>
        <p class="mt-3 text-[12px] leading-7 muted">
          复习计划到期、目标阶段完成、清单勾选都会带上时间标记，和成长记录、学习场次、社区文章、档案材料互相印证。
        </p>
        <div class="mt-5 space-y-4">
          <div>
            <div class="mb-2 flex justify-between text-[12px]"><span class="muted">复习计划完成度</span><span class="faint">{{ reviewRate }}%</span></div>
            <div class="progress-track"><div class="progress-fill" :style="{ width: reviewRate + '%' }"></div></div>
          </div>
          <div>
            <div class="mb-2 flex justify-between text-[12px]"><span class="muted">强基清单勾选</span><span class="faint">{{ checkRate }}%</span></div>
            <div class="progress-track"><div class="progress-fill" :style="{ width: checkRate + '%' }"></div></div>
          </div>
        </div>
        <div class="mt-6 flex flex-wrap gap-2">
          <span class="pill pill-rose">逾期计划 {{ stats.reviewOverdue || 0 }}</span>
          <span class="pill pill-sky">已完成阶段 {{ stats.targetDone || 0 }}</span>
          <span class="pill pill-mint">综评材料 {{ stats.qualityRecords || 0 }}</span>
          <span class="pill pill-neutral">专业库 {{ stats.majorRequirements || 0 }}</span>
        </div>
      </div>

      <div class="grid gap-3" v-reveal="2">
        <a v-for="q in quick" :key="q.to" :href="q.to" class="glass lift flex items-center gap-4 p-4" @click.prevent="$router.push(q.to)">
          <span class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[18px]" :class="`pill-${q.color}`">{{ q.icon }}</span>
          <span class="min-w-0">
            <span class="block text-[14px] font-medium ink">{{ q.label }}</span>
            <span class="block text-[11px] muted">{{ q.desc }}</span>
          </span>
          <span class="ml-auto text-[16px] faint">→</span>
        </a>
      </div>
    </div>
  </section>
</template>
