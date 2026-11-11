<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { feature } from '@/api'
import { useAuthStore } from '@/stores/auth'
import MetricCircle from '@/components/MetricCircle.vue'

const auth = useAuthStore()
const dash = ref({})
const feed = ref([])
const typed = ref('')
const full = '把散落的日子，连成一条看得见的线。'

const quick = [
  { to: '/timeline', label: '拾光时间线', desc: '所有记录汇成一条轴', color: 'mint', icon: '◷' },
  { to: '/learning', label: '学习', desc: '题库 · 错题 · 图谱', color: 'sky', icon: '✎' },
  { to: '/planning', label: '规划', desc: '选科 · 志愿 · 目标', color: 'violet', icon: '◎' },
  { to: '/community', label: '社区', desc: '写下并分享', color: 'rose', icon: '❋' },
  { to: '/archive', label: '档案', desc: '简历 · 证书 · 论文', color: 'amber', icon: '▤' },
]

onMounted(async () => {
  let i = 0
  const timer = setInterval(() => { typed.value = full.slice(0, ++i); if (i >= full.length) clearInterval(timer) }, 95)
  try { dash.value = await feature.dashboard() } catch { /* 后端未启动时降级 */ }
  if (auth.isLoggedIn) {
    try { feed.value = (await feature.timelineFeed()).slice(0, 5) } catch { /* ignore */ }
  }
})

const metrics = computed(() => [
  { label: '时间线记录', value: dash.value.records || 0, color: 'mint' },
  { label: '里程碑', value: dash.value.milestones || 0, color: 'amber' },
  { label: '题库题目', value: dash.value.questions || 0, color: 'sky' },
  { label: '文章内容', value: dash.value.contents || 0, color: 'violet' },
])
</script>

<template>
  <section>
    <div class="mx-auto max-w-[760px] pt-6 text-center" v-reveal="0">
      <p class="eyebrow">CHRONICLES OF TIME · 拾光记</p>
      <h1 class="hand-title display mt-4 text-[clamp(46px,8vw,84px)]">拾光记</h1>
      <p class="mt-4 min-h-[28px] text-[15px] muted">
        {{ typed }}<span class="type-caret">|</span>
      </p>
      <p class="mx-auto mt-4 max-w-[560px] text-[13px] leading-7 muted">
        不再把人生切成互不相干的阶段。记录、学习、规划、社区与档案都挂在同一条时间轴上，
        彼此关联、彼此印证。
      </p>
      <div class="mt-7 flex flex-wrap justify-center gap-2">
        <RouterLink :to="auth.isLoggedIn ? '/timeline' : '/login'" class="btn btn-blue"><span class="btn-glyph">→</span> 进入时间线</RouterLink>
        <RouterLink to="/community" class="btn btn-mint"><span class="btn-glyph">❋</span> 逛逛社区</RouterLink>
        <RouterLink to="/timeline/records" class="btn btn-violet"><span class="btn-glyph">＋</span> 写一条记录</RouterLink>
        <RouterLink to="/settings" class="btn btn-amber"><span class="btn-glyph">⚙</span> 偏好设置</RouterLink>
      </div>
    </div>

    <div class="glass mt-12 grid grid-cols-2 gap-4 p-6 md:grid-cols-4" v-reveal="1">
      <MetricCircle v-for="m in metrics" :key="m.label" :label="m.label" :value="m.value" :color="m.color" />
    </div>

    <div class="mt-10 grid gap-5 md:grid-cols-2">
      <div class="glass p-6" v-reveal="2">
        <p class="eyebrow">ONE TIMELINE</p>
        <h2 class="section-title mt-2">一条时间线，串起所有功能</h2>
        <div class="tl-rail mt-5" v-if="feed.length">
          <div v-for="e in feed" :key="e.id" class="tl-node mb-4">
            <div class="glass-flat p-3">
              <div class="flex items-center justify-between gap-2">
                <span class="pill pill-sky">{{ e.type }}</span>
                <span class="text-[11px] faint">{{ e.occurredAt }}</span>
              </div>
              <RouterLink :to="e.link" class="mt-1 block text-[13px] font-medium ink hover:text-accent">{{ e.title }}</RouterLink>
            </div>
          </div>
        </div>
        <p v-else class="mt-4 text-[12px] leading-6 muted">
          登录后，这里会按时间倒序展示来自记录、学习、规划、社区与档案的全部节点。
        </p>
        <div class="mt-5 flex items-center gap-3">
          <div class="progress-track flex-1"><div class="progress-fill" style="width: 68%"></div></div>
          <span class="text-[11px] faint">已完成 3/4</span>
        </div>
      </div>

      <div class="grid gap-3" v-reveal="3">
        <RouterLink v-for="q in quick" :key="q.to" :to="q.to" class="glass lift flex items-center gap-4 p-4">
          <span class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[18px]" :class="`pill-${q.color}`">{{ q.icon }}</span>
          <span class="min-w-0">
            <span class="block text-[14px] font-medium ink">{{ q.label }}</span>
            <span class="block text-[11px] muted">{{ q.desc }}</span>
          </span>
          <span class="ml-auto text-[16px] faint">→</span>
        </RouterLink>
      </div>
    </div>
  </section>
</template>
