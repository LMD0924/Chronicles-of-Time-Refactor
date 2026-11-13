<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { seamKey, seamActive, pulseTimeline } from '@/composables/useTimelineTransition'

const build = ref('P1 · 全量交付')
const route = useRoute()
</script>

<template>
  <div class="bg-layer" aria-hidden="true">
    <div class="bg-grid"></div>
    <div class="halo halo-a"></div>
    <div class="halo halo-b"></div>
    <div class="halo halo-c"></div>
  </div>

  <div class="relative z-10 flex min-h-[100dvh] flex-col">
    <AppHeader v-if="!route.meta.public" />

    <main class="mx-auto w-full max-w-[1200px] flex-1 px-5 pb-16 md:px-8"
          :class="route.meta.public ? 'pt-10' : 'pt-[110px] md:pt-[124px]'">
      <RouterView v-slot="{ Component, route }">
        <Transition name="page" mode="out-in" @before-enter="pulseTimeline">
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </main>

    <footer class="mx-auto mb-5 w-[calc(100%-2.5rem)] max-w-[1200px] border-t border-hairline px-2 py-5 md:w-[calc(100%-4rem)]">
      <div class="flex flex-wrap items-center justify-between gap-3 text-[12px] leading-6 muted">
        <span class="flex flex-wrap items-center gap-2">
          <span class="pulse-dot inline-block h-1.5 w-1.5 rounded-full bg-accent"></span>
          拾光记 · Chronicles of Time
          <span class="pill pill-mint">时间线主轴</span>
          <span class="faint">{{ build }}</span>
        </span>
        <span class="faint">一条时间线串起记录、学习、规划与档案 · MIT © 2026 LMD0924</span>
      </div>
    </footer>
  </div>

  <!-- 时间轴卷轴过渡：切换时沿内容区左侧掠过的一条脊线（替代参考项目的 HUD 遮罩） -->
  <div v-if="seamActive" :key="seamKey" class="timeline-seam" aria-hidden="true"></div>
</template>
