<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="glass mx-auto max-w-[620px] p-10 text-center">
    <p class="eyebrow">404 / NOT FOUND</p>
    <h1 class="hand-title display mt-3">这段时光还没被记录</h1>
    <p class="mt-3 text-[13px] muted">页面不存在，或已经被移到时间线的另一处。</p>
    <RouterLink to="/" class="btn btn-blue mt-6 inline-flex"><span class="btn-glyph">←</span> 回到首页</RouterLink>
  </section>
</template>
