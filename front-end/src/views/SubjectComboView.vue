<script setup>
import { onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const subjects = ref([]), hot = ref([]), majors = ref([])
const cfg = {
  eyebrow: '', title: '组合台账', description: '', color: 'sky',
  fields: [
    { key: 'firstSubject', label: '首选科目' }, { key: 'secondSubjects', label: '再选科目' },
    { key: 'label', label: '组合名称' }, { key: 'studentCount', label: '选择人数', type: 'number' },
    { key: 'coverage', label: '专业覆盖率', type: 'number' }
  ],
  columns: [
    { key: 'firstSubject', label: '首选' }, { key: 'secondSubjects', label: '再选' },
    { key: 'studentCount', label: '人数' }, { key: 'coverage', label: '覆盖率' }
  ]
}
cfg.api = R.planning.subjectCombinations

async function load() {
  try {
    const [s, h, m] = await Promise.all([feature.subjects(), feature.hotCombinations(), R.planning.majorRequirements.all()])
    subjects.value = s || []; hot.value = h || []; majors.value = m || []
  } catch { /* ignore */ }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SUBJECTS" title="学科与组合"
      description="学科字典、热门组合与专业选科要求放在一起，选科时可直接对照。"
      :actions="[{ label: '学科雷达', icon: '◈', color: 'violet', to: '/planning/radar' }, { label: '选科中心', icon: '◎', color: 'blue', to: '/planning/selection' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />
    <div class="glass p-5">
      <h2 class="section-title mb-3">学科字典</h2>
      <div class="flex flex-wrap gap-2">
        <span v-for="s in subjects" :key="s.id" class="pill" :class="s.isFirst ? 'pill-violet' : 'pill-sky'">
          {{ s.name }} <small class="faint">{{ s.category }}</small>
        </span>
        <span v-if="!subjects.length" class="text-[12px] muted">暂无学科数据</span>
      </div>
    </div>
    <div class="mt-5 grid gap-5 md:grid-cols-2">
      <div class="glass p-5">
        <h2 class="section-title mb-3">热门组合</h2>
        <div v-for="c in hot" :key="c.id" class="flex items-center gap-3 border-b border-hairline py-2 last:border-0">
          <span class="pill pill-mint">{{ c.firstSubject }}</span>
          <span class="text-[12px] ink">{{ c.secondSubjects || c.label }}</span>
          <span class="ml-auto text-[11px] faint">{{ c.studentCount }} 人</span>
        </div>
        <p v-if="!hot.length" class="text-[12px] muted">还没有组合记录</p>
      </div>
      <div class="glass p-5">
        <h2 class="section-title mb-3">专业选科要求</h2>
        <div v-for="m in majors" :key="m.id" class="flex flex-wrap items-center gap-2 border-b border-hairline py-2 last:border-0">
          <span class="pill pill-amber">{{ m.category }}</span>
          <span class="text-[12px] ink">{{ m.majorName }}</span>
          <span class="ml-auto text-[11px] faint">{{ m.requiredSubjects }}</span>
        </div>
        <p v-if="!majors.length" class="text-[12px] muted">暂无专业要求</p>
      </div>
    </div>
    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
