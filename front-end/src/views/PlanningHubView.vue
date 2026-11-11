<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'

const stats = ref({})
const error = ref('')
const quick = [
      { to: '/planning/selection', label: '选科中心', desc: '意向 · 指导 · 历史 · 审批', color: 'violet', icon: '◎' },
      { to: '/planning/volunteer', label: '志愿填报', desc: '检索 · 推荐 · 模拟 · 方案', color: 'amber', icon: '▤' },
      { to: '/planning/subjects', label: '学科与组合', desc: '字典与热门组合', color: 'sky', icon: '⌘' },
      { to: '/planning/gpa', label: 'GPA 模拟', desc: '绩点推演', color: 'mint', icon: '◷' },
      { to: '/planning/gap', label: '毕业缺口', desc: '学分核对', color: 'rose', icon: '△' },
      { to: '/planning/review', label: '复习计划', desc: '目标与进度', color: 'teal', icon: '✎' },
    ]
const metrics = computed(() => [
      { label: '选科记录', value: stats.value.selections || 0, color: 'violet' },
      { label: '志愿方案', value: stats.value['volunteer-plans'] || 0, color: 'amber' },
      { label: '课程与学分', value: stats.value['term-courses'] || 0, color: 'sky' },
      { label: '复习计划', value: stats.value['review-plans'] || 0, color: 'mint' },
    ])

onMounted(async () => {
  try { stats.value = (await R.planning.subjectOptions.stats()) || {} } catch (e) { error.value = e.message }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / OVERVIEW" title="规划总览" description="选科、志愿、课程学分、复习与目标都挂在同一个用户身上，用时间线串起来，而不是按阶段切开。" :actions="[
        { label: '选科中心', icon: '◎', color: 'violet', to: '/planning/selection' },
        { label: '志愿填报', icon: '▤', color: 'amber', to: '/planning/volunteer' },
        { label: '学科雷达', icon: '◈', color: 'sky', to: '/planning/radar' },
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
          所有新增与修改都会带上时间标记，自动出现在「拾光时间线」里，
          和记录、学习、社区、档案互相印证，不再彼此孤立。
        </p>
        <div class="mt-5 flex items-center gap-3">
          <div class="progress-track flex-1"><div class="progress-fill" style="width: 74%"></div></div>
          <span class="text-[11px] faint">关联度 74%</span>
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
