<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { feature } from '@/api'

const route = useRoute()
const data = ref(null)
const loading = ref(true)
const error = ref('')

const statCards = computed(() => {
  const s = data.value?.stats || {}
  return [
    { label: '成长记录', value: s.records || 0, hint: '条时间线节点' },
    { label: '里程碑', value: s.milestones || 0, hint: '个高光时刻' },
    { label: '学习时长', value: s.studyHours || 0, hint: '小时累计' },
    { label: '复习完成', value: `${s.reviewDone || 0}/${s.reviewPlans || 0}`, hint: '计划进度' },
    { label: '清单勾选', value: `${s.checklistDone || 0}/${s.checklistTotal || 0}`, hint: '强基清单' },
    { label: '答题正确率', value: `${s.correctRate || 0}%`, hint: `${s.answers || 0} 次作答` },
  ]
})

async function load() {
  loading.value = true; error.value = ''
  try { data.value = await feature.shareView(route.params.token) }
  catch (e) { error.value = e.message || '分享链接不可用' }
  finally { loading.value = false }
}
function printPage() { window.print() }
onMounted(load)
</script>

<template>
  <section>
    <div class="glass mb-6 p-6 md:p-8">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p class="eyebrow">SHARED / READ ONLY</p>
          <h1 class="mt-2 text-[26px] font-semibold ink">{{ data?.title || '拾光时间线 · 只读分享' }}</h1>
          <p class="mt-2 text-[12px] leading-6 muted">
            这是一份免登录的只读视图，仅展示汇总与时间线节点，隐藏全部编辑入口。
            链接由分享者生成，可随时撤销或设置有效期。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <span class="pill pill-mint">只读</span>
          <span v-if="data" class="pill pill-sky">已被查看 {{ data.viewCount }} 次</span>
          <button class="btn btn-slate" type="button" @click="printPage"><span class="btn-glyph">⎙</span> 打印 / 存 PDF</button>
        </div>
      </div>
      <p v-if="data?.expiresAt" class="mt-4 text-[11px] faint">有效期至 {{ data.expiresAt }} · 生成于 {{ data.createdAt }}</p>
    </div>

    <div v-if="loading" class="grid gap-4 md:grid-cols-3"><div v-for="i in 6" :key="i" class="placeholder-bar" style="height:90px"></div></div>

    <div v-else-if="error" class="glass p-10 text-center">
      <p class="text-[38px]">🔒</p>
      <h2 class="mt-3 text-[18px] font-semibold ink">这份分享暂时无法查看</h2>
      <p class="mt-2 text-[13px] muted">{{ error }}</p>
      <p class="mt-4 text-[11px] faint">可能链接已过期、已被撤销，或地址不完整。请联系分享者重新生成。</p>
      <button class="btn btn-slate mt-6" type="button" @click="load"><span class="btn-glyph">↻</span> 重试</button>
    </div>

    <template v-else-if="data">
      <div class="grid grid-cols-2 gap-4 md:grid-cols-3" v-reveal="0">
        <div v-for="c in statCards" :key="c.label" class="glass p-5 text-center">
          <p class="stat-num">{{ c.value }}</p>
          <p class="mt-1 text-[13px] font-medium ink">{{ c.label }}</p>
          <p class="mt-1 text-[11px] faint">{{ c.hint }}</p>
        </div>
      </div>

      <div class="mt-6 grid gap-5 md:grid-cols-[1.6fr_1fr]">
        <div class="glass p-6">
          <p class="eyebrow">TIMELINE</p>
          <h2 class="section-title mt-2 mb-5">时间线节点</h2>
          <div class="tl-rail">
            <div v-for="(n, i) in data.timeline" :key="`${n.type}-${i}`" class="tl-node mb-4">
              <div class="glass-flat p-4">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="pill pill-sky">{{ n.type }}</span>
                  <span v-if="n.category" class="pill pill-neutral">{{ n.category }}</span>
                  <span class="ml-auto text-[11px] faint">{{ n.occurredAt }}</span>
                </div>
                <h3 class="mt-2 text-[14px] font-medium ink">{{ n.title }}</h3>
                <p v-if="n.content" class="mt-1 text-[12px] leading-6 muted">{{ n.content }}</p>
              </div>
            </div>
            <p v-if="!data.timeline.length" class="text-[12px] muted">分享者还没有记录时间线节点</p>
          </div>
        </div>

        <div class="grid gap-5">
          <div class="glass p-6">
            <p class="eyebrow">MILESTONES</p>
            <h2 class="section-title mt-2 mb-4">里程碑</h2>
            <div v-for="(m, i) in data.milestones" :key="i" class="glass-flat mb-2 flex items-center gap-3 p-3">
              <span class="grid h-8 w-8 flex-none place-items-center rounded-lg text-[15px]" :class="`pill-${m.color}`">★</span>
              <span class="min-w-0"><span class="block truncate text-[13px] font-medium ink">{{ m.title }}</span>
                <span class="block text-[11px] faint">{{ m.occurredAt }}</span></span>
            </div>
            <p v-if="!data.milestones.length" class="text-[12px] muted">暂无里程碑</p>
          </div>

          <div class="glass p-6">
            <p class="eyebrow">PROGRESS</p>
            <h2 class="section-title mt-2 mb-4">复习计划进度</h2>
            <div v-for="(p, i) in data.reviewProgress" :key="i" class="mb-4">
              <div class="flex items-center justify-between gap-2 text-[12px]">
                <span class="truncate ink">{{ p.title }}</span><span class="faint">{{ p.progress }}%</span>
              </div>
              <div class="progress-track mt-2"><div class="progress-fill" :style="{ width: Math.min(100, p.progress || 0) + '%' }"></div></div>
              <p class="mt-1 text-[11px] faint">{{ p.subject }} · {{ p.status }} · 目标 {{ p.targetDate || '未设置' }}</p>
            </div>
            <p v-if="!data.reviewProgress.length" class="text-[12px] muted">暂无复习计划</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
