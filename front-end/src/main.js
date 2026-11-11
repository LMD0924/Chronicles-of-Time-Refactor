import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { reveal, spotlight, count } from '@/directives'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import './style.css'

useTheme().initTheme()

const app = createApp(App)
app.directive('reveal', reveal)
app.directive('spotlight', spotlight)
app.directive('count', count)

const pinia = createPinia()
app.use(pinia)
const auth = useAuthStore(pinia)
window.addEventListener('cot:unauthorized', () => auth.clear())

auth.bootstrap().finally(() => {
  app.use(router)
  app.mount('#app')
})
