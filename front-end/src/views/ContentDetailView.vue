<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { feature, R } from '@/api'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const route = useRoute(), router = useRouter(), auth = useAuthStore()
const detail = ref(null), related = ref([])
const comment = ref(''), msg = ref(''), error = ref('')
const rendered = computed(() => md.render(detail.value?.body || ''))
const id = computed(() => route.params.id)

async function load() {
  try {
    detail.value = await feature.contentDetail(id.value)
    related.value = (await feature.related(detail.value?.tags || '')) || []
  } catch (e) { error.value = e.message }
}
async function toggle(kind) {
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  try {
    const f = kind === 'like' ? (detail.value.liked ? feature.unlike : feature.like) : (detail.value.favorited ? feature.unfavorite : feature.favorite)
    await f(id.value); await load()
  } catch (e) { error.value = e.message }
}
async function addComment() {
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  if (!comment.value.trim()) return
  try {
    await R.community.comments.save({ contentId: Number(id.value), body: comment.value, authorName: auth.displayName, createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ') })
    comment.value = ''; msg.value = '评论已发布'; await load()
  } catch (e) { error.value = e.message }
}
onMounted(load)
watch(id, load)
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / ARTICLE" :title="detail?.title || '文章详情'"
      :description="detail?.summary || '正在载入…'"
      :actions="[{ label: '返回广场', icon: '←', color: 'slate', to: '/community' }, { label: '文章图谱', icon: '⌘', color: 'violet', to: '/community/graph' }]" />
    <p v-if="error" class="pill pill-rose mb-4">{{ error }}</p>
    <div v-if="detail" class="grid gap-5 md:grid-cols-[1fr_280px]">
      <article class="glass p-7">
        <div class="flex flex-wrap items-center gap-2">
          <span class="pill pill-rose">{{ detail.category }}</span>
          <span class="text-[11px] faint">{{ detail.createdAt }} · {{ detail.authorName }}</span>
          <span class="ml-auto text-[11px] faint">浏览 {{ detail.viewCount || 0 }}</span>
        </div>
        <div class="markdown-body mt-5 text-[14px] leading-8 ink" v-html="rendered"></div>
        <div class="mt-7 flex flex-wrap gap-2 border-t border-hairline pt-5">
          <button class="btn" :class="detail.liked ? 'btn-rose' : 'btn-mint'" @click="toggle('like')">
            <span class="btn-glyph">♡</span> {{ detail.liked ? '已点赞' : '点赞' }} ({{ detail.likeCount || 0 }})
          </button>
          <button class="btn" :class="detail.favorited ? 'btn-amber' : 'btn-violet'" @click="toggle('favorite')">
            <span class="btn-glyph">☆</span> {{ detail.favorited ? '已收藏' : '收藏' }} ({{ detail.favoriteCount || 0 }})
          </button>
        </div>

        <div class="mt-7">
          <h2 class="section-title mb-3">评论 ({{ (detail.comments || []).length }})</h2>
          <div v-for="c in (detail.comments || [])" :key="c.id" class="glass-flat mb-2 p-3">
            <p class="text-[12px] ink">{{ c.body }}</p><p class="mt-1 text-[11px] faint">{{ c.authorName }} · {{ c.createdAt }}</p>
          </div>
          <textarea v-model="comment" class="field mt-3 w-full" rows="3" placeholder="写下你的想法…"></textarea>
          <button class="btn btn-blue mt-2" @click="addComment"><span class="btn-glyph">→</span> 发表评论</button>
          <p v-if="msg" class="mt-2 text-[12px] text-mint">{{ msg }}</p>
        </div>
      </article>
      <aside class="glass p-5 h-fit">
        <h2 class="section-title mb-3">相关文章</h2>
        <RouterLink v-for="r in related.filter(x => String(x.id) !== id).slice(0, 5)" :key="r.id" :to="`/community/${r.id}`" class="block py-2 text-[12px] hover:text-accent">
          {{ r.title }} <span class="faint">· 共同标签 {{ r.overlap }}</span>
        </RouterLink>
        <p v-if="!related.length" class="text-[12px] muted">暂无关联文章</p>
      </aside>
    </div>
    <div v-else class="glass p-10 text-center text-[13px] muted">正在载入文章…</div>
  </section>
</template>
