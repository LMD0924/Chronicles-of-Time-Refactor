<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mode = ref('login')
const username = ref('')
const password = ref('')
const nickname = ref('')
const showPwd = ref(false)
const error = ref('')
const busy = ref(false)

const demos = [
  { label: '用户演示账号', color: 'mint', u: 'user', p: 'Cot@2026user' },
  { label: '管理员演示账号', color: 'violet', u: 'admin', p: 'Cot@2026admin' },
]

function fill(d) { username.value = d.u; password.value = d.p; error.value = '' }
function switchMode() { mode.value = mode.value === 'login' ? 'register' : 'login'; error.value = '' }
const heading = computed(() => mode.value === 'login' ? '欢迎回来' : '开始记录')

async function submit() {
  if (!username.value || !password.value) { error.value = '请填写账号与密码'; return }
  busy.value = true; error.value = ''
  try {
    if (mode.value === 'login') await auth.login({ username: username.value, password: password.value })
    else { await auth.register({ username: username.value, password: password.value, nickname: nickname.value }); await auth.login({ username: username.value, password: password.value }) }
    router.push(route.query.redirect || '/timeline')
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>

<template>
  <section class="mx-auto grid max-w-[940px] gap-8 pt-4 md:grid-cols-2">
    <div class="glass p-8" v-reveal="0">
      <p class="eyebrow">IDENTITY / 双身份共用一套前端</p>
      <h1 class="hand-title display mt-3">{{ heading }}</h1>
      <p class="mt-4 text-[13px] leading-7 muted">
        普通用户与管理员使用同一个前端。登录后会按角色显示对应入口：
        用户管理自己的时间线，管理员额外获得管理后台。
      </p>
      <div class="mt-7 space-y-3">
        <div class="glass-flat flex items-start gap-3 p-3">
          <span class="pill pill-mint">用户</span>
          <span class="text-[12px] leading-6 muted">记录、学习、规划、社区、档案与个人中心；数据按账号隔离。</span>
        </div>
        <div class="glass-flat flex items-start gap-3 p-3">
          <span class="pill pill-violet">管理员</span>
          <span class="text-[12px] leading-6 muted">用户能力之外，增加用户与角色、内容治理、数据字典、操作审计与系统信息。</span>
        </div>
        <div class="glass-flat flex items-start gap-3 p-3">
          <span class="pill pill-neutral">游客</span>
          <span class="text-[12px] leading-6 muted">可浏览首页与内容广场，写操作与时间线需要登录。</span>
        </div>
      </div>
      <p class="mt-6 text-[11px] faint">演示账号仅用于本地体验，生产部署必须删除并覆盖 JWT 密钥。</p>
    </div>

    <div class="glass p-8" v-reveal="1">
      <div class="tabs !mb-6">
        <button class="tab-btn" :class="{ 'is-active': mode === 'login' }" @click="mode = 'login'">登录</button>
        <button class="tab-btn" :class="{ 'is-active': mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <label class="block mb-4">
        <span class="field-label">账号</span>
        <input v-model="username" class="field w-full" type="text" autocomplete="username" placeholder="请输入账号" @keyup.enter="submit" />
      </label>
      <label v-if="mode === 'register'" class="block mb-4">
        <span class="field-label">昵称</span>
        <input v-model="nickname" class="field w-full" type="text" placeholder="展示用昵称（可选）" />
      </label>
      <label class="block mb-4">
        <span class="field-label">密码</span>
        <span class="relative block">
          <input v-model="password" class="field w-full pr-16" :type="showPwd ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" @keyup.enter="submit" />
          <button type="button" class="absolute right-2 top-1/2 -translate-y-1/2 text-[11px] link" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</button>
        </span>
      </label>

      <p v-if="error" class="mb-3 text-[12px] text-rose-500">{{ error }}</p>
      <button class="btn btn-blue w-full justify-center" type="button" :disabled="busy" @click="submit">
        <span class="btn-glyph">→</span> {{ busy ? '处理中…' : (mode === 'login' ? '登录' : '注册并登录') }}
      </button>

      <div class="mt-6 border-t border-hairline pt-5">
        <p class="field-label mb-2">演示账号一键填入</p>
        <div class="flex flex-wrap gap-2">
          <button v-for="d in demos" :key="d.u" class="btn" :class="`btn-${d.color}`" type="button" @click="fill(d)">
            <span class="btn-glyph">◆</span> {{ d.label }}
          </button>
        </div>
        <p class="mt-3 text-[11px] faint">连续失败 5 次将冻结账号 30 分钟；自助注册固定为普通用户。</p>
      </div>
    </div>
  </section>
</template>
