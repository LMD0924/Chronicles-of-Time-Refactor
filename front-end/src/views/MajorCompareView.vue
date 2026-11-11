<script setup>
import { computed, onMounted, ref } from 'vue'
import { feature } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const data = ref({ plans: [], details: [] })
const picked = ref(['', ''])
const left = computed(() => data.value.details.filter(d => String(d.planId) === String(picked.value[0])))
const right = computed(() => data.value.details.filter(d => String(d.planId) === String(picked.value[1])))
const dist = (list) => { const m = {}; list.forEach(d => { m[d.chanceLevel || '未分档'] = (m[d.chanceLevel || '未分档'] || 0) + 1 }); return m }
onMounted(async () => { try { data.value = (await feature.comparePlans(null, null)) || { plans: [], details: [] } } catch { /* ignore */ } })
</script>

<template>
  <section>
    <PageHeader eyebrow="PLANNING / COMPARE" title="方案对比"
      description="选择两套志愿方案并排比较，查看院校差异与冲稳保分布。"
      :actions="[{ label: '志愿填报', icon: '←', color: 'blue', to: '/planning/volunteer' }]" />
    <div class="grid gap-5 md:grid-cols-2">
      <div v-for="(side, idx) in ['left', 'right']" :key="side" class="glass p-5">
        <label class="block">
          <span class="field-label">方案 {{ idx === 0 ? 'A' : 'B' }}</span>
          <select v-model="picked[idx]" class="field w-full">
            <option value="">请选择</option>
            <option v-for="p in data.plans" :key="p.id" :value="p.id">{{ p.title }}</option>
          </select>
        </label>
        <div class="mt-3 flex flex-wrap gap-2">
          <span v-for="(v, k) in dist(idx === 0 ? left : right)" :key="k" class="pill pill-sky">{{ k }} · {{ v }}</span>
        </div>
        <div class="mt-3 space-y-2">
          <div v-for="d in (idx === 0 ? left : right)" :key="d.id" class="glass-flat p-3 text-[12px]">
            {{ d.priority }}. {{ d.universityName }} · {{ d.majorName }} <span class="pill pill-amber ml-1">{{ d.chanceLevel }}</span>
          </div>
          <p v-if="!(idx === 0 ? left : right).length" class="text-[12px] muted">选择一个方案查看明细</p>
        </div>
      </div>
    </div>
  </section>
</template>
