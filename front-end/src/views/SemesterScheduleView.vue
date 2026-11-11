<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const courses = ref([])
const groups = computed(() => {
  const m = {}
  courses.value.forEach(c => { (m[c.term || '未分学期'] ||= []).push(c) })
  return m
})
onMounted(async () => { try { courses.value = (await feature.schedule()) || [] } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / SCHEDULE" title="学期课表"
      description="按学期展示课程、学分与成绩，数据同时供 GPA 模拟与毕业缺口使用。"
      :actions="[{ label: 'GPA 模拟', icon: '◷', color: 'violet', to: '/planning/gpa' }, { label: '课程树', icon: '⌘', color: 'teal', to: '/archive/course-tree' }]" />
    <EmptyState v-if="!courses.length" text="课表还是空的" hint="到「GPA 模拟」里添加课程与学分" />
    <div v-for="(list, term) in groups" :key="term" class="glass mb-5 p-5">
      <h2 class="section-title mb-3">{{ term }} <span class="text-[11px] faint">· {{ list.length }} 门</span></h2>
      <div class="crud-grid">
        <div v-for="c in list" :key="c.id" class="crud-card">
          <span class="pill pill-sky">{{ c.category || '课程' }}</span>
          <h3 class="mt-2 text-[14px] font-semibold ink">{{ c.courseName }}</h3>
          <p class="mt-1 text-[12px] muted">{{ c.credit }} 学分 · {{ c.score }} 分 · 绩点 {{ c.gpaPoint }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
