<script setup>
import { RouterLink, useRoute } from 'vue-router'
import { ADMIN_NAV } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
</script>

<template>
  <section>
    <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="eyebrow">ADMIN / 共用同一套前端</p>
        <h1 class="hand-title display mt-1">管理后台</h1>
        <p class="mt-2 text-[12px] muted">左侧细导航 + 半透明磨砂数据卡片，不另做一套深色重 UI。</p>
      </div>
      <span class="pill pill-violet">{{ auth.displayName }} · 管理员</span>
    </div>

    <div class="grid gap-5 md:grid-cols-[210px_1fr]">
      <nav class="glass h-fit p-3">
        <RouterLink v-for="n in ADMIN_NAV" :key="n.to" :to="n.to"
          class="mb-1 flex items-center gap-2 rounded-lg px-3 py-2 text-[13px] muted transition-colors hover:text-accent"
          :class="{ '!bg-[var(--accent-soft)] !text-[var(--link)]': route.path === n.to }">
          <span class="h-1.5 w-1.5 rounded-full bg-accent"></span> {{ n.label }}
        </RouterLink>
        <div class="mt-4 border-t border-hairline pt-3">
          <RouterLink to="/timeline" class="btn btn-slate w-full justify-center"><span class="btn-glyph">←</span> 返回前台</RouterLink>
        </div>
      </nav>
      <div class="min-w-0"><RouterView /></div>
    </div>
  </section>
</template>
