<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const auth = useAuthStore()
const router = useRouter()
const me = ref({}), overview = ref({})
const form = reactive({ nickname: '', bio: '', region: '', realName: '', phone: '', email: '', school: '', birthday: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })
const msg = ref(''), error = ref('')

async function load() {
  try {
    const [m, o] = await Promise.all([authApi.me(), authApi.overview()])
    me.value = m || {}; overview.value = o || {}
    const a = m?.account || {}, p = m?.profile || {}
    Object.assign(form, { nickname: a.nickname || '', bio: a.bio || '', region: a.region || '',
      realName: p.realName || '', phone: p.phone || '', email: p.email || '', school: p.school || '', birthday: p.birthday || '' })
  } catch (e) { error.value = e.message }
}
async function saveProfile() {
  try { await authApi.updateMe({ ...form }); msg.value = '资料已保存'; await load() } catch (e) { error.value = e.message }
}
async function changePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) { error.value = '请填写原密码与新密码'; return }
  try {
    await authApi.changePassword({ ...pwd })
    msg.value = '密码已修改，旧会话已失效，请重新登录'
    setTimeout(async () => { await auth.logout(); router.push('/login') }, 1200)
  } catch (e) { error.value = e.message }
}
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="IDENTITY / PROFILE" title="个人中心"
      description="身份卡、资料编辑与安全设置。管理员也无法在这里查看他人档案。"
      :actions="[{ label: '设置', icon: '⚙', color: 'slate', to: '/settings' }, { label: '时间线', icon: '◷', color: 'mint', to: '/timeline' }]" />

    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6" v-reveal="0">
        <div class="flex items-center gap-4">
          <span class="avatar !h-14 !w-14 !text-[20px]">{{ (auth.displayName || '拾').slice(0, 1) }}</span>
          <div>
            <h2 class="text-[17px] font-semibold ink">{{ auth.displayName }}</h2>
            <span class="pill" :class="auth.isAdmin ? 'pill-violet' : 'pill-mint'">{{ auth.isAdmin ? '管理员' : '用户' }}</span>
          </div>
        </div>
        <div class="mt-5 grid grid-cols-2 gap-3">
          <div class="glass-flat p-3"><p class="text-[11px] faint">账号</p><p class="mt-1 text-[13px] ink">{{ me.account?.username }}</p></div>
          <div class="glass-flat p-3"><p class="text-[11px] faint">生源地</p><p class="mt-1 text-[13px] ink">{{ me.account?.region || '—' }}</p></div>
          <div class="glass-flat p-3 col-span-2"><p class="text-[11px] faint">简介</p><p class="mt-1 text-[13px] ink">{{ me.account?.bio || '还没有填写简介' }}</p></div>
        </div>
        <h3 class="section-title mt-6 mb-2">我的数据</h3>
        <p class="text-[12px] muted">角色 {{ overview.role || 'USER' }} · 用户 ID {{ overview.userId || '—' }}</p>
      </div>

      <div class="glass p-6" v-reveal="1">
        <h2 class="section-title mb-4">资料编辑</h2>
        <div class="form-grid">
          <label><span class="field-label">昵称</span><input v-model="form.nickname" class="field w-full" /></label>
          <label><span class="field-label">生源地</span><input v-model="form.region" class="field w-full" /></label>
          <label><span class="field-label">真实姓名</span><input v-model="form.realName" class="field w-full" /></label>
          <label><span class="field-label">电话</span><input v-model="form.phone" class="field w-full" /></label>
          <label><span class="field-label">邮箱</span><input v-model="form.email" class="field w-full" /></label>
          <label><span class="field-label">学校 / 单位</span><input v-model="form.school" class="field w-full" /></label>
          <label class="span-2"><span class="field-label">简介</span><textarea v-model="form.bio" class="field w-full" rows="3"></textarea></label>
        </div>
        <button class="btn btn-mint mt-4" @click="saveProfile"><span class="btn-glyph">✓</span> 保存资料</button>
      </div>
    </div>

    <div class="glass mt-5 p-6">
      <h2 class="section-title mb-4">安全设置</h2>
      <div class="grid gap-4 md:grid-cols-2">
        <label><span class="field-label">原密码</span><input v-model="pwd.oldPassword" class="field w-full" type="password" /></label>
        <label><span class="field-label">新密码（至少 6 位）</span><input v-model="pwd.newPassword" class="field w-full" type="password" /></label>
      </div>
      <button class="btn btn-rose mt-4" @click="changePassword"><span class="btn-glyph">⚿</span> 修改密码并吊销旧会话</button>
      <p v-if="msg" class="mt-3 text-[12px] text-mint">{{ msg }}</p>
      <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
    </div>
  </section>
</template>
