<script setup>
import { onMounted, ref } from 'vue'
import http from '@/utils/request'

const getOverview = () => http.get('/api/v1/admin/overview')

const data = ref(null), error = ref(''), loading = ref(false)

const countCards = [
  { label: '账号', key: 'account' },
  { label: '时间线记录', key: 'life_record' },
  { label: '里程碑', key: 'milestone' },
  { label: '题库', key: 'question_bank' },
  { label: '成绩', key: 'score_record' },
  { label: '文章', key: 'content' },
  { label: '评论', key: 'content_comment' },
  { label: '志愿方案', key: 'volunteer_plan' },
  { label: '志愿明细', key: 'volunteer_detail' },
  { label: '模拟结果', key: 'admission_simulation' },
  { label: '选科申请', key: 'course_selection' },
  { label: '审计日志', key: 'audit_log' },
]

async function load() {
  loading.value = true; error.value = ''
  try { data.value = await getOverview() } catch (e) { error.value = e.message } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h2 class="section-title">系统概览</h2>
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">↻</span> {{ loading ? '刷新中…' : '刷新' }}</button>
    </div>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
    <p class="mb-4 text-[12px] muted">各业务表计数、账号构成与最近的跨账号业务动态，全部来自真实数据聚合。</p>

    <div class="glass mb-5 grid grid-cols-2 gap-4 p-5 md:grid-cols-4">
      <div class="crud-card">
        <p class="text-[12px] muted">普通用户 / 管理员</p>
        <p class="mt-1 text-2xl font-semibold ink">{{ data?.userCount ?? 0 }}
          <span class="text-[13px] muted">/ {{ data?.adminCount ?? 0 }}</span></p>
      </div>
      <div v-for="c in countCards" :key="c.key" class="crud-card">
        <p class="text-[12px] muted">{{ c.label }}</p>
        <p class="mt-1 text-2xl font-semibold ink">{{ data?.counts?.[c.key] ?? 0 }}</p>
      </div>
    </div>

    <div class="grid gap-5 lg:grid-cols-2">
      <div class="glass overflow-x-auto p-5">
        <h3 class="section-title mb-3">最近操作审计 ({{ data?.recentAudit?.length || 0 }})</h3>
        <table class="data-table w-full">
          <thead><tr><th>时间</th><th>操作人</th><th>动作</th><th>目标</th></tr></thead>
          <tbody>
            <tr v-for="a in (data?.recentAudit || [])" :key="a.id">
              <td class="whitespace-nowrap text-[11px]">{{ a.createdAt || '—' }}</td>
              <td>{{ a.operatorName || '—' }}</td>
              <td><span class="pill pill-violet">{{ a.action }}</span></td>
              <td>{{ a.targetType }} #{{ a.targetId }}</td>
            </tr>
            <tr v-if="!(data?.recentAudit || []).length"><td colspan="4" class="text-center muted">暂无审计记录</td></tr>
          </tbody>
        </table>
      </div>

      <div class="glass overflow-x-auto p-5">
        <h3 class="section-title mb-3">跨账号志愿方案 ({{ data?.recentPlans?.length || 0 }})</h3>
        <table class="data-table w-full">
          <thead><tr><th>用户</th><th>标题</th><th>状态</th><th>明细数</th></tr></thead>
          <tbody>
            <tr v-for="p in (data?.recentPlans || [])" :key="p.id">
              <td>{{ p.userId }}</td>
              <td class="max-w-[160px] truncate">{{ p.title || '—' }}</td>
              <td><span class="pill pill-sky">{{ p.status || '—' }}</span></td>
              <td>{{ p.detailCount ?? 0 }}</td>
            </tr>
            <tr v-if="!(data?.recentPlans || []).length"><td colspan="4" class="text-center muted">暂无方案</td></tr>
          </tbody>
        </table>

        <h3 class="section-title mb-3 mt-6">最近选科申请 ({{ data?.recentSelections?.length || 0 }})</h3>
        <table class="data-table w-full">
          <thead><tr><th>用户</th><th>组合</th><th>状态</th><th>说明</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="s in (data?.recentSelections || [])" :key="s.id">
              <td>{{ s.userId }}</td>
              <td>{{ s.combinationId ?? '—' }}</td>
              <td><span class="pill pill-mint">{{ s.status || '—' }}</span></td>
              <td class="max-w-[140px] truncate">{{ s.note || '—' }}</td>
              <td class="whitespace-nowrap text-[11px]">{{ s.createdAt || '—' }}</td>
            </tr>
            <tr v-if="!(data?.recentSelections || []).length"><td colspan="5" class="text-center muted">暂无申请</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
