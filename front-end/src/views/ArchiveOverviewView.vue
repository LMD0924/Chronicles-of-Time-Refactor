<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import MetricCircle from '@/components/MetricCircle.vue'

const stats = ref({})
const error = ref('')
const quick = [
      { to: '/profile', label: '个人中心', desc: '资料与安全', color: 'mint', icon: '☺' },
      { to: '/archive/resume', label: '简历', desc: '分组编辑', color: 'blue', icon: '▤' },
      { to: '/archive/certificates', label: '证书档案', desc: '证书与附件', color: 'amber', icon: '★' },
      { to: '/archive/papers', label: '论文', desc: '写作与建议', color: 'violet', icon: '✎' },
      { to: '/archive/course-tree', label: '课程树', desc: '课程分类结构', color: 'sky', icon: '⌘' },
      { to: '/settings', label: '设置', desc: '主题与偏好', color: 'slate', icon: '⚙' },
    ]
const metrics = computed(() => [
      { label: '证书', value: stats.value.certificates || 0, color: 'amber' },
      { label: '论文', value: stats.value.papers || 0, color: 'violet' },
      { label: '课程节点', value: stats.value.courseNodes || 0, color: 'sky' },
      { label: '看板任务', value: stats.value.thesisItems || 0, color: 'mint' },
    ])

onMounted(async () => {
  try { stats.value = (await feature.archiveStats()) || {} } catch (e) { error.value = e.message }
})
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / OVERVIEW" title="档案总览" description="个人资料、简历、证书、课程树与论文统一归档，所有新增都会同步到时间线。" :actions="[
        { label: '个人中心', icon: '☺', color: 'mint', to: '/profile' },
        { label: '简历', icon: '▤', color: 'blue', to: '/archive/resume' },
        { label: '设置', icon: '⚙', color: 'slate', to: '/settings' },
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
