<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/utils/request'

const listUsers = (keyword) => http.get('/api/v1/admin/users', { params: { keyword } })
const createUser = (data) => http.post('/api/v1/admin/users', data)
const setStatus = (id, status) => http.post(`/api/v1/admin/users/${id}/status`, null, { params: { status } })
const setRole = (id, role) => http.put(`/api/v1/admin/users/${id}/role`, null, { params: { role } })
const resetPassword = (id, data) => http.post(`/api/v1/admin/users/${id}/reset-password`, data)

const me = JSON.parse(localStorage.getItem('cot-user') || 'null')
const myId = me ? Number(me.userId) : null
const users = ref([]), keyword = ref(''), msg = ref(''), error = ref('')
const form = reactive({ username: '', nickname: '', password: '', role: 'USER' })
const show = ref(false)

const isSelf = (u) => myId !== null && Number(u.id) === myId

async function load() {
  error.value = ''
  try { users.value = (await listUsers(keyword.value)) || [] } catch (e) { error.value = e.message }
}
async function create() {
  if (!form.username || !form.password) { error.value = '账号与密码必填'; return }
  try {
    await createUser({ ...form })
    show.value = false; msg.value = '已创建账号（操作已记入审计）'
    await load()
  } catch (e) { error.value = e.message }
}
async function changeStatus(u) {
  msg.value = ''; error.value = ''
  if (isSelf(u) && u.status === 'ACTIVE') { error.value = '不能停用当前登录的账号'; return }
  const status = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await setStatus(u.id, status)
    msg.value = `已${status === 'ACTIVE' ? '启用' : '停用'}「${u.username}」，旧令牌已失效`
    await load()
  } catch (e) { error.value = e.message }
}
async function changeRole(u, e) {
  const role = e.target.value
  msg.value = ''; error.value = ''
  if (isSelf(u) && role !== 'ADMIN') {
    error.value = '不能将自己的管理员权限降权'
    e.target.value = u.role
    return
  }
  try {
    await setRole(u.id, role)
    msg.value = '角色已更新（操作已记入审计）'
    await load()
  } catch (err) { error.value = err.message; e.target.value = u.role }
}
async function reset(u) {
  if (!window.confirm(`确定重置「${u.username}」的密码吗？旧令牌会立即失效。`)) return
  try {
    await resetPassword(u.id, { password: 'Cot@2026reset' })
    msg.value = '密码已重置为 Cot@2026reset'
    await load()
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h2 class="section-title">用户与角色</h2>
      <button class="btn btn-mint" @click="show = !show"><span class="btn-glyph">＋</span> 新建账号</button>
    </div>
    <div class="crud-toolbar">
      <input v-model="keyword" class="field max-w-[240px]" placeholder="按账号 / 昵称搜索" @keyup.enter="load" />
      <button class="btn btn-blue" @click="load"><span class="btn-glyph">⌕</span> 检索</button>
      <button class="btn btn-slate" @click="load"><span class="btn-glyph">↻</span> 刷新</button>
      <span class="pill pill-sky">{{ users.length }} 个账号</span>
    </div>
    <p v-if="msg" class="mb-2 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="mb-2 text-[12px] text-rose-500">{{ error }}</p>

    <div v-if="show" class="glass mb-4 p-5">
      <div class="form-grid">
        <label><span class="field-label">账号</span><input v-model="form.username" class="field w-full" /></label>
        <label><span class="field-label">昵称</span><input v-model="form.nickname" class="field w-full" /></label>
        <label><span class="field-label">初始密码</span><input v-model="form.password" class="field w-full" /></label>
        <label><span class="field-label">角色</span><select v-model="form.role" class="field w-full"><option value="USER">用户</option><option value="ADMIN">管理员</option></select></label>
      </div>
      <button class="btn btn-blue mt-4" @click="create"><span class="btn-glyph">✓</span> 创建</button>
    </div>

    <div class="glass overflow-x-auto p-1">
      <table class="data-table w-full">
        <thead><tr><th>ID</th><th>账号</th><th>昵称</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}<span v-if="isSelf(u)" class="pill pill-violet ml-1">当前账号</span></td>
            <td>{{ u.nickname }}</td>
            <td>
              <select class="field max-w-[120px]" :value="u.role" @change="changeRole(u, $event)">
                <option value="USER">用户</option>
                <option value="ADMIN">管理员</option>
              </select>
            </td>
            <td><span class="pill" :class="u.status === 'ACTIVE' ? 'pill-sky' : 'pill-rose'">{{ u.status }}</span></td>
            <td class="whitespace-nowrap">
              <button class="btn" :class="u.status === 'ACTIVE' ? 'btn-amber' : 'btn-mint'" @click="changeStatus(u)">
                {{ u.status === 'ACTIVE' ? '停用' : '启用' }}
              </button>
              <button class="btn btn-rose ml-1" @click="reset(u)">重置密码</button>
            </td>
          </tr>
          <tr v-if="!users.length"><td colspan="6" class="text-center muted">暂无账号</td></tr>
        </tbody>
      </table>
    </div>
    <p class="mt-3 text-[11px] faint">停用会使该账号的旧令牌立即失效；启用会清零登录失败计数。不能停用或降权当前登录的账号。</p>
  </div>
</template>
