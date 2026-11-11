<script setup>
import { onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const path = ref([])
onMounted(async () => { try { path.value = (await feature.learningPath()) || [] } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / PATH" title="学习路径"
      description="按错题掌握度从低到高给出建议顺序，先补最薄弱的知识点。"
      :actions="[{ label: '错题本', icon: '✎', color: 'rose', to: '/learning/mistakes' }, { label: '开始练习', icon: '▶', color: 'mint', to: '/learning/practice' }]" />
    <EmptyState v-if="!path.length" text="暂时没有需要补强的知识点" />
    <div v-else class="grid gap-3 md:grid-cols-2">
      <div v-for="(p, i) in path" :key="p.knowledgePoint" class="glass lift p-5" v-reveal="1">
        <div class="flex items-center justify-between gap-2">
          <span class="pill pill-violet">第 {{ i + 1 }} 步</span>
          <span v-if="p.suggested" class="pill pill-rose">建议优先</span>
        </div>
        <h3 class="mt-3 text-[15px] font-semibold ink">{{ p.knowledgePoint }}</h3>
        <div class="mt-3 flex items-center gap-3">
          <div class="progress-track flex-1"><div class="progress-fill" :style="{ width: Math.min(100, (p.mastery || 0) * 50) + '%' }"></div></div>
          <span class="text-[11px] faint">掌握度 {{ Math.round((p.mastery || 0) * 50) }}%</span>
        </div>
      </div>
    </div>
  </section>
</template>
