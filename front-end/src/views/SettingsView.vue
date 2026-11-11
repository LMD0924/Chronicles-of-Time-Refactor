<script setup>
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const { theme, setTheme, toggleTheme } = useTheme()
const auth = useAuthStore()
const router = useRouter()

async function logout() { await auth.logout(); router.push('/') }
</script>

<template>
  <section>
    <PageHeader eyebrow="SYSTEM / SETTINGS" title="设置"
      description="主题、账号与会话。主题偏好会记忆在浏览器，切换时全站有 0.55s 过渡。" />
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <h2 class="section-title mb-4">外观</h2>
        <p class="text-[12px] muted mb-4">默认浅色纸面风格；暗色主题使用同一组 token。</p>
        <div class="flex flex-wrap gap-2">
          <button class="btn btn-blue" :class="{ 'is-active': theme === 'light' }" @click="setTheme('light')"><span class="btn-glyph">☀</span> 浅色</button>
          <button class="btn btn-violet" :class="{ 'is-active': theme === 'dark' }" @click="setTheme('dark')"><span class="btn-glyph">☾</span> 暗色</button>
          <button class="btn btn-teal" @click="toggleTheme"><span class="btn-glyph">⇄</span> 切换</button>
        </div>
        <div class="glass-flat mt-5 p-4 text-[12px] muted">
          当前主题：<span class="pill pill-sky">{{ theme === 'dark' ? '暗色' : '浅色' }}</span>
        </div>
      </div>
      <div class="glass p-6">
        <h2 class="section-title mb-4">账号与会话</h2>
        <div class="space-y-3 text-[12px] muted">
          <p>登录账号：<span class="ink">{{ auth.displayName }}</span></p>
          <p>角色：<span class="pill" :class="auth.isAdmin ? 'pill-violet' : 'pill-mint'">{{ auth.isAdmin ? '管理员' : '用户' }}</span></p>
          <p>令牌：本地保存，退出或改密码后立即失效</p>
        </div>
        <div class="mt-5 flex flex-wrap gap-2">
          <RouterLink to="/profile" class="btn btn-mint"><span class="btn-glyph">☺</span> 个人中心</RouterLink>
          <button class="btn btn-rose" @click="logout"><span class="btn-glyph">↪</span> 退出登录</button>
        </div>
      </div>
    </div>
    <div class="glass mt-5 p-6">
      <h2 class="section-title mb-3">关于本项目</h2>
      <p class="text-[12px] leading-7 muted">
        拾光记以一条个人时间线串联记录、学习、规划、社区与档案；后端为混合 DDD 的模块化单体，
        前端使用 Vue 3 + JavaScript + TailwindCSS，路由过渡为时间轴卷轴。
      </p>
    </div>
  </section>
</template>
