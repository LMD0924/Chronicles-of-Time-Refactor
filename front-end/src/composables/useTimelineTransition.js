import { ref } from 'vue'

/** 时间轴卷轴过渡：路由器在 beforeEach 递增 key，DefaultLayout 用它重建脊线。 */
export const seamKey = ref(0)
export const seamActive = ref(false)
let timer = null

export function pulseTimeline() {
  seamKey.value += 1
  seamActive.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { seamActive.value = false }, 760)
}
