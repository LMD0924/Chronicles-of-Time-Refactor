<script setup>
import { onMounted, ref } from 'vue'
import http from '@/utils/request'

const getAuditLogs = (params) => http.get('/api/v1/admin/audit-logs', { params })

const logs = ref([]), limit = ref(20), action = ref(''), error = ref(''), msg = ref('')
const actions = [
  'CREATE_USER', 'UPDATE_USER', 'RESET_PASSWORD', 'UPDATE_USER_STATUS', 'UPDATE_USER_ROLE',
  'UPDATE_CONTENT_STATUS', 'DELETE_CONTENT',
  'CREATE_DICTIONARY', 'UPDATE_DICTIONARY', 'DELETE_DICTIONARY',
]

async function load() {
  error.value = ''
  try {
    const data = await getAuditLogs({ limit: limit.value, action: action.value })
    logs.value = data?.items || []
    msg.value = `当前显示 ${logs.value.length} 条`
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h2 class="section-title">操作审计</h2>
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">↻</span> 刷新</button>
    </div>
    <div class="crud-toolbar">
      <select v-model.number="limit" class="field max-w-[160px]" @change="load">
        <option :value="20">最近 20 条</option>
        <option :value="50">最近 50 条</option>
        <option :value="100">最近 100 条</option>
      </select>
      <select v-model="action" class="field max-w-[220px]" @change="load">
        <option value="">全部动作</option>
        <option v-for="a in actions" :key="a" :value="a">{{ a }}</option>
      </select>
      <span class="pill pill-sky">{{ logs.length }} 条</span>
    </div>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
    <p class="mb-3 text-[12px] muted">管理端所有写操作都会记录操作人、时间、动作、目标与前后值摘要。</p>

    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr><th>时间</th><th>操作人</th><th>动作</th><th>目标</th><th>前值</th><th>后值</th></tr></thead>
        <tbody>
          <tr v-for="l in logs" :key="l.id">
            <td class="whitespace-nowrap text-[11px]">{{ l.createdAt || '—' }}</td>
            <td>{{ l.operatorName || '—' }}</td>
            <td><span class="pill pill-violet">{{ l.action }}</span></td>
            <td>{{ l.targetType }} #{{ l.targetId }}</td>
            <td class="max-w-[240px] whitespace-pre-wrap break-all text-[11px]">{{ l.beforeValue || '—' }}</td>
            <td class="max-w-[240px] whitespace-pre-wrap break-all text-[11px]">{{ l.afterValue || '—' }}</td>
          </tr>
          <tr v-if="!logs.length"><td colspan="6" class="text-center muted">暂无审计记录，可在其他后台页面操作后返回查看</td></tr>
        </tbody>
      </table>
    </div>
    <p class="mt-3 text-[11px] faint">{{ msg }}</p>
  </div>
</template>
