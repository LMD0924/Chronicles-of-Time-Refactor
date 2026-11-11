<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const questions = ref([])
const index = ref(0)
const answer = ref('')
const result = ref(null)
const done = ref(0)
const correct = ref(0)
const msg = ref('')
const current = computed(() => questions.value[index.value])

async function load() {
  try { questions.value = (await feature.randomQuestions(10)) || []; index.value = 0; result.value = null; done.value = 0; correct.value = 0; answer.value = '' }
  catch (e) { msg.value = e.message }
}
function options(q) {
  try { return JSON.parse(q.optionsJson || '[]') } catch { return [] }
}
async function submit() {
  if (!current.value || !answer.value) { msg.value = '请选择或填写答案'; return }
  try {
    result.value = await feature.submitAnswer({ questionId: current.value.id, userAnswer: answer.value.replace(/^[A-D][.、]?\s*/, ''), durationMs: 0 })
    done.value++; if (result.value.correct) correct.value++
  } catch (e) { msg.value = e.message }
}
function next() { index.value++; answer.value = ''; result.value = null; msg.value = '' }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="LEARNING / PRACTICE" title="练习中心"
      description="从题库随机抽题，提交后自动判分、写入答题记录，错题自动进入错题本。"
      :actions="[{ label: '重新抽题', icon: '↻', color: 'blue', click: load }, { label: '错题本', icon: '✎', color: 'rose', to: '/learning/mistakes' }]" />

    <div class="mb-4 flex items-center gap-3">
      <div class="progress-track flex-1"><div class="progress-fill" :style="{ width: (questions.length ? index / questions.length * 100 : 0) + '%' }"></div></div>
      <span class="text-[11px] faint">已完成 {{ done }}/{{ questions.length }} · 正确 {{ correct }}</span>
    </div>

    <EmptyState v-if="!questions.length" text="题库里还没有题目" hint="先到「题库管理」添加几道题" />
    <div v-else-if="index >= questions.length" class="glass p-10 text-center">
      <h2 class="hand-title display">本轮完成</h2>
      <p class="mt-3 text-[13px] muted">共 {{ done }} 题，正确 {{ correct }} 题，正确率 {{ done ? Math.round(correct / done * 100) : 0 }}%</p>
      <button class="btn btn-mint mt-5" @click="load"><span class="btn-glyph">↻</span> 再来一轮</button>
    </div>
    <div v-else class="glass p-7">
      <div class="flex flex-wrap items-center gap-2">
        <span class="pill pill-sky">{{ current.subject }}</span>
        <span class="pill pill-neutral">{{ current.knowledgePoint }}</span>
        <span class="ml-auto text-[11px] faint">第 {{ index + 1 }} / {{ questions.length }} 题</span>
      </div>
      <h2 class="mt-4 text-[16px] font-semibold ink">{{ current.title }}</h2>
      <p class="mt-2 text-[13px] leading-7 muted">{{ current.content }}</p>
      <div class="mt-4 grid gap-2 sm:grid-cols-2">
        <button v-for="o in options(current)" :key="o" class="chip text-left" :class="{ 'is-active': answer === o }" @click="answer = o">{{ o }}</button>
      </div>
      <input v-model="answer" class="field mt-4 w-full" placeholder="也可直接输入答案…" />
      <div class="mt-4 flex flex-wrap gap-2">
        <button class="btn btn-violet" @click="submit"><span class="btn-glyph">✓</span> 提交答案</button>
        <button v-if="result" class="btn btn-blue" @click="next"><span class="btn-glyph">→</span> 下一题</button>
      </div>
      <div v-if="result" class="glass-flat mt-4 p-4">
        <p class="text-[13px]" :class="result.correct ? 'text-mint' : 'text-rose-500'">{{ result.correct ? '回答正确' : '回答错误，已加入错题本' }}</p>
        <p class="mt-1 text-[12px] muted">正确答案：{{ result.answer }}</p>
        <p class="mt-1 text-[12px] muted">解析：{{ result.explanation }}</p>
      </div>
      <p v-if="msg" class="mt-3 text-[12px] text-rose-500">{{ msg }}</p>
    </div>
  </section>
</template>
