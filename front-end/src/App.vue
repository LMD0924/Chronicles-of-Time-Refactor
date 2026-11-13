<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import DefaultLayout from './layouts/DefaultLayout.vue'

const toast = ref('')
let timer = null
function onToast(e) {
  toast.value = e.detail?.message || '请求失败'
  clearTimeout(timer)
  timer = setTimeout(() => { toast.value = '' }, 4200)
}
onMounted(() => window.addEventListener('cot:toast', onToast))
onUnmounted(() => { window.removeEventListener('cot:toast', onToast); clearTimeout(timer) })
</script>

<template>
  <DefaultLayout />
  <Teleport to="body">
    <div v-if="toast" class="cot-toast" role="alert" @click="toast = ''">
      <span class="pill pill-rose">提示</span>
      <span class="text-[12px] muted">{{ toast }}</span>
      <span class="ml-auto text-[11px] faint">点击关闭</span>
    </div>
  </Teleport>
</template>
