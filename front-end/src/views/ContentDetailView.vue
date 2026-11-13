<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { feature } from '@/api'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const route = useRoute(), router = useRouter(), auth = useAuthStore()
const detail = ref(null), related = ref([]), comments = ref([])
const comment = ref(''), msg = ref(''), error = ref(''), busy = ref(false)
const rendered = computed(() => md.render(detail.value?.body || ''))
const id = computed(() => route.params.id)
const published = computed(() => detail.value?.status === 'PUBLISHED')
function canDelete(c) { return auth.isAdmin || (!!auth.displayName && c.authorName === auth.displayName) }

async function load() {
  error.value = ''
  try {
    const d = await feature.contentDetail(id.value)
    detail.value = d
    comments.value = d?.comments || []
    related.value = (await feature.related(d?.tags || '', Number(id.value))) || []
  } catch (e) { error.value = e.message; detail.value = null }
}
function needLogin() {
  if (auth.isLoggedIn) return false
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return true
}
async function toggle(kind) {
  if (needLogin()) return
  error.value = ''; msg.value = ''
  try {
    const f = kind === 'like'
      ? (detail.value.liked ? feature.unlike : feature.like)
      : (detail.value.favorited ? feature.unfavorite : feature.favorite)
    await f(id.value)
    msg.value = kind === 'like' ? (detail.value.liked ? '已取消点赞' : '已点赞') : (detail.value.favorited ? '已取消收藏' : '已收藏')
    await load()
  } catch (e) { error.value = e.message }
}
async function addComment() {
  if (needLogin()) return
  if (!comment.value.trim()) return
  busy.value = true; error.value = ''; msg.value = ''
  try {
    await feature.addComment(id.value, comment.value.trim())
    comment.value = ''; msg.value = '评论已发布'; await load()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
async function removeComment(c) {
  if (!window.confirm('确定删除这条评论吗？')) return
  error.value = ''; msg.value = ''
  try { await feature.deleteComment(c.id); msg.value = '评论已删除'; await load() }
  catch (e) { error.value = e.message }
}
onMounted(load)
watch(id, load)
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / ARTICLE" :title="detail?.title || '文章详情'"
      :description="detail?.summary || '正在载入…'"
      :actions="[{ label: '返回广场', icon: '←', color: 'slate', to: '/community' }, { label: '文章图谱', icon: '⌘', color: 'violet', to: '/community/graph' }]" />
    <p v-if="msg" class="mb-3 text-[12px] text-mint">{{ msg }}</p>
    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>

    <div v-if="detail" class="grid gap-5 md:grid-cols-[1fr_280px]">
      <article class="glass p-7">
        <div v-if="!published" class="glass-flat mb-4 p-3">
          <p class="text-[12px] ink">这是你自己的{{ detail.status === 'DRAFT' ? '草稿' : '未公开稿件' }}预览，只有你与管理员可见。</p>
        </div>
        <img v-if="detail.coverUrl" :src="detail.coverUrl" alt="封面" class="mb-5 max-h-[300px] w-full rounded-xl object-cover" />
        <div class="flex flex-wrap items-center gap-2">
          <span class="pill pill-rose">{{ detail.category }}</span>
          <span class="text-[11px] faint">{{ detail.createdAt }} · {{ detail.authorName }}</span>
          <span class="ml-auto text-[11px] faint">浏览 {{ detail.viewCount || 0 }}</span>
        </div>
        <div class="markdown-body mt-5 text-[14px] leading-8 ink" v-html="rendered"></div>
        <div class="mt-4 flex flex-wrap gap-1.5">
          <span v-for="t in String(detail.tags || '').split(/[,，;；]/).filter(Boolean)" :key="t" class="pill pill-neutral">{{ t }}</span>
        </div>

        <div class="mt-7 flex flex-wrap gap-2 border-t border-hairline pt-5">
          <button class="btn" :class="detail.liked ? 'btn-rose' : 'btn-mint'" :disabled="!published" @click="toggle('like')">
            <span class="btn-glyph">♡</span> {{ detail.liked ? '已点赞' : '点赞' }} ({{ detail.likeCount || 0 }})
          </button>
          <button class="btn" :class="detail.favorited ? 'btn-amber' : 'btn-violet'" :disabled="!published" @click="toggle('favorite')">
            <span class="btn-glyph">☆</span> {{ detail.favorited ? '已收藏' : '收藏' }} ({{ detail.favoriteCount || 0 }})
          </button>
        </div>

        <div class="mt-7">
          <h2 class="section-title mb-3">评论 ({{ comments.length }})</h2>
          <div v-for="c in comments" :key="c.id" class="glass-flat mb-2 p-3">
            <div class="flex items-start gap-2">
              <p class="flex-1 text-[12px] ink">{{ c.body }}</p>
              <button v-if="canDelete(c)" class="btn btn-slate" @click="removeComment(c)">删除</button>
            </div>
            <p class="mt-1 text-[11px] faint">{{ c.authorName }} · {{ c.createdAt }}</p>
          </div>
          <p v-if="!comments.length" class="text-[12px] muted">还没有评论，来说两句吧。</p>
          <textarea v-model="comment" class="field mt-3 w-full" rows="3" placeholder="写下你的想法…" :disabled="!published"></textarea>
          <button class="btn btn-blue mt-2" :disabled="busy || !published" @click="addComment">
            <span class="btn-glyph">→</span> {{ auth.isLoggedIn ? '发表评论' : '登录后评论' }}
          </button>
        </div>
      </article>
      <aside class="glass h-fit p-5">
        <h2 class="section-title mb-3">相关文章</h2>
        <RouterLink v-for="r in related.slice(0, 6)" :key="r.id" :to="`/community/${r.id}`" class="block py-2 text-[12px] hover:text-accent">
          {{ r.title }} <span class="faint">· 共同标签 {{ r.overlap }}</span>
        </RouterLink>
        <p v-if="!related.length" class="text-[12px] muted">暂无关联文章</p>
      </aside>
    </div>
    <div v-else class="glass p-10 text-center text-[13px] muted">文章不存在或尚未发布。</div>
  </section>
</template>
