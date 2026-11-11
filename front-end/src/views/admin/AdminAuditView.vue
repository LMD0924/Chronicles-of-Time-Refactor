<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api'

const logs = ref([]), error = ref('')
onMounted(async () => { try { logs.value = (await adminApi.audit()) || [] } catch (e) { error.value = e.message } })
</script>

<template>
  <div>
    <h2 class="section-title mb-4">操作审计</h2>
    <p class="mb-4 text-[12px] muted">管理端所有写操作都会记录操作人、时间、动作、目标与前后值摘要。</p>
    <p v-if="error" class="pill pill-rose mb-3">{{ error }}</p>
    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr><th>时间</th><th>操作人</th><th>动作</th><th>目标</th><th>前值</th><th>后值</th></tr></thead>
        <tbody>
          <tr v-for="l in logs" :key="l.id">
            <td>{{ l.createdAt }}</td><td>{{ l.operatorName }}</td>
            <td><span class="pill pill-violet">{{ l.action }}</span></td>
            <td>{{ l.targetType }} #{{ l.targetId }}</td><td>{{ l.beforeValue || '—' }}</td><td>{{ l.afterValue || '—' }}</td>
          </tr>
          <tr v-if="!logs.length"><td colspan="6" class="text-center muted">暂无审计记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
