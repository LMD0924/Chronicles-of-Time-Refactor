import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_KEY } from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem('cot-user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function persist(t, u) {
    token.value = t || ''
    user.value = u || null
    if (t) localStorage.setItem(TOKEN_KEY, t); else localStorage.removeItem(TOKEN_KEY)
    if (u) localStorage.setItem('cot-user', JSON.stringify(u)); else localStorage.removeItem('cot-user')
  }

  async function bootstrap() {
    if (!token.value) return
    try { await authApi.verify() } catch { clear() }
  }

  async function login(payload) {
    const data = await authApi.login(payload)
    persist(data.token, { userId: data.userId, username: data.username, nickname: data.nickname, role: data.role })
    return data
  }

  async function register(payload) { return authApi.register(payload) }

  async function logout() {
    try { await authApi.logout() } catch { /* 服务端不可达时也清本地 */ }
    clear()
  }

  function clear() { persist('', null) }

  return { token, user, isLoggedIn, isAdmin, displayName, bootstrap, login, register, logout, clear }
})
