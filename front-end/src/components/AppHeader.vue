<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import { NAV_GROUPS } from '@/config/navigation'

const { theme, toggleTheme } = useTheme()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const scrolled = ref(false)
const initial = computed(() => (auth.displayName || '拾').slice(0, 1).toUpperCase())

async function doLogout() {
  await auth.logout()
  router.push('/')
}

function isGroupActive(group) {
  return group.items.some(i => i.to === route.path || (i.to !== '/' && route.path.startsWith(i.to + '/')))
}

function onScroll() { scrolled.value = window.scrollY > 28 }
onMounted(() => { onScroll(); window.addEventListener('scroll', onScroll, { passive: true }) })
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="site-header" :class="{ 'is-scrolled': scrolled }">
    <div class="nav-shell">
      <RouterLink to="/" class="flex flex-none items-center gap-2.5" aria-label="拾光记首页">
        <span class="brand-mark">拾</span>
        <span>
          <span class="brand-name">拾光记</span>
          <span class="brand-sub">One timeline, every moment</span>
        </span>
      </RouterLink>

      <nav class="nav-links" aria-label="主导航">
        <div v-for="group in NAV_GROUPS" :key="group.key" class="nav-link mega-wrap"
             :class="{ 'is-active': isGroupActive(group) }" tabindex="0">
          <span class="mega-dot-inline" :class="`pill-${group.color}`"></span>
          {{ group.label }}
          <div class="mega-panel" @click.stop>
            <div class="mega-head">
              <span class="mega-title">{{ group.label }} · 全部功能</span>
              <span class="eyebrow">{{ group.items.length }} 个入口</span>
            </div>
            <div class="mega-grid">
              <RouterLink v-for="item in group.items" :key="item.to" :to="item.to" class="mega-item">
                <span class="mega-dot"></span>
                <span>{{ item.label }}<small>{{ item.desc }}</small></span>
              </RouterLink>
            </div>
          </div>
        </div>
        <RouterLink v-if="auth.isAdmin" to="/admin" class="nav-link is-admin-link">管理后台</RouterLink>
      </nav>

      <div class="nav-actions">
        <button class="btn-icon" type="button"
                :aria-label="theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
                :title="theme === 'dark' ? '浅色主题' : '深色主题'" @click="toggleTheme">
          <svg v-if="theme === 'dark'" viewBox="0 0 24 24" fill="none" class="icon-swap" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" class="icon-swap" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.5 14.2A8.5 8.5 0 0 1 9.8 3.5a8.5 8.5 0 1 0 10.7 10.7Z" />
          </svg>
        </button>
        <span class="nav-divider"></span>

        <template v-if="auth.isLoggedIn">
          <span class="pill" :class="auth.isAdmin ? 'pill-violet' : 'pill-mint'">{{ auth.isAdmin ? '管理员' : '用户' }}</span>
          <span class="avatar">{{ initial }}</span>
          <RouterLink to="/profile" class="nav-user nav-user-link" title="个人中心">{{ auth.displayName }}</RouterLink>
          <RouterLink to="/settings" class="btn btn-teal"><span class="btn-glyph">⚙</span> 设置</RouterLink>
          <button type="button" class="btn btn-slate" @click="doLogout"><span class="btn-glyph">↪</span> 退出</button>
        </template>
        <template v-else>
          <span class="pill pill-neutral">游客</span>
          <RouterLink to="/login" class="btn btn-blue"><span class="btn-glyph">→</span> 登录</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.mega-dot-inline { width: 6px; height: 6px; border-radius: 999px; background: var(--accent); opacity: .8; }
.nav-link.mega-wrap { cursor: pointer; outline: none; }
</style>
