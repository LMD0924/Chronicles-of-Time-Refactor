<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const path = ref([])
const stats = ref({})
const loading = ref(true)
const error = ref('')

const weakCount = computed(() => path.value.filter(p => p.suggested).length)
const avgMastery = computed(() => path.value.length
  ? Math.round(path.value.reduce((s, p) => s + (Number(p.mastery) || 0), 0) / path.value.length * 10) / 10 : 0)

function level(m) { return m >= 80 ? 'pill-mint' : m >= 60 ? 'pill-amber' : 'pill-rose' }
function drill(p) {
  router.push({ path: '/learning/practice', query: { auto: '1', knowledgePoint: p.knowledgePoint, size: '10' } })
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [p, s] = await Promise.all([feature.learningPath(), feature.learningStats()])
    path.value = p || []
    stats.value = s || {}
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / PATH" title="学习路径"
      description="按知识点的真实答题正确率排优先级：掌握度低于 60% 的建议优先重练。"
      :actions="[
        { label: '开始练习', icon: '▶', color: 'mint', to: '/learning/practice' },
        { label: '知识图谱', icon: '◈', color: 'violet', to: '/learning/graph' },
        { label: '刷新', icon: '↻', color: 'slate', click: load },
      ]" />

    <div class="glass grid grid-cols-2 gap-4 p-5 md:grid-cols-4" v-reveal="0">
      <div v-for="m in [
        { label: '知识点', value: path.length }, { label: '建议优先', value: weakCount },
        { label: '平均掌握度', value: avgMastery + '%' }, { label: '累计答题', value: stats.answers || 0 },
      ]" :key="m.label" class="text-center">
        <p class="text-[22px] font-semibold ink">{{ m.value }}</p><p class="mt-1 text-[11px] faint">{{ m.label }}</p>
      </div>
    </div>

    <p v-if="error" class="pill pill-rose mt-4">{{ error }}</p>
    <div v-if="loading" class="mt-5 space-y-3"><div v-for="i in 4" :key="i" class="placeholder-bar" style="height:80px"></div></div>
    <EmptyState v-else-if="!path.length" class="mt-5" text="还没有学习路径数据"
      hint="先到练习中心做一轮题目，系统会按知识点统计掌握度" />

    <div v-else class="mt-5 space-y-3">
      <article v-for="(p, i) in path" :key="p.knowledgePoint" class="glass lift p-5" v-reveal="1">
        <div class="flex flex-wrap items-center gap-2">
          <span class="pill pill-neutral">#{{ i + 1 }}</span>
          <h3 class="text-[14px] font-semibold ink">{{ p.knowledgePoint }}</h3>
          <span class="pill" :class="level(p.mastery)">掌握度 {{ p.mastery }}%</span>
          <span v-if="p.suggested" class="pill pill-rose">建议优先</span>
          <span class="ml-auto text-[11px] faint">作答 {{ p.attempts }} 次 · 答对 {{ p.correct }}</span>
        </div>
        <div class="progress-track mt-3"><div class="progress-fill" :style="{ width: (p.mastery || 0) + '%' }"></div></div>
        <p class="mt-3 text-[12px] muted">
          {{ p.suggested ? '这个知识点的正确率偏低，建议先做一轮针对性练习，再回错题本逐题复习。' : '掌握情况稳定，可以偶尔用错题重练保持手感。' }}
        </p>
        <button class="btn btn-mint mt-3" type="button" @click="drill(p)"><span class="btn-glyph">▶</span> 练这个知识点</button>
      </article>
    </div>
  </section>
</template>
