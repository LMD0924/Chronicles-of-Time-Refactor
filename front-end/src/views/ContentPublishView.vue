<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const router = useRouter()
const title = ref(''), category = ref('随笔'), tags = ref(''), body = ref(''), coverUrl = ref('')
const msg = ref(''), error = ref(''), busy = ref(false)
const preview = computed(() => md.render(body.value || '*预览会显示在这里*'))

async function upload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try { const r = await feature.uploadImage(file); coverUrl.value = r.url; msg.value = '封面已上传' } catch (err) { error.value = err.message }
}
async function publish(status) {
  if (!title.value.trim() || !body.value.trim()) { error.value = '标题与正文不能为空'; return }
  busy.value = true; error.value = ''
  try {
    await R.community.contents.save({
      title: title.value, body: body.value, summary: body.value.replace(/[#*`>\-]/g, '').slice(0, 120),
      category: category.value, tags: tags.value, coverUrl: coverUrl.value, status,
      viewCount: 0, likeCount: 0, favoriteCount: 0,
      createdAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
      updatedAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })
    router.push('/community')
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>

<template>
  <section>
    <PageHeader eyebrow="COMMUNITY / PUBLISH" title="写下这一刻"
      description="Markdown 写作，实时预览；文章发布后会作为一个节点出现在时间线上。"
      :actions="[{ label: '返回广场', icon: '←', color: 'slate', to: '/community' }]" />
    <div class="grid gap-5 md:grid-cols-2">
      <div class="glass p-6">
        <div class="form-grid">
          <label class="span-2"><span class="field-label">标题</span><input v-model="title" class="field w-full" placeholder="给这段时光起个名字" /></label>
          <label><span class="field-label">分类</span>
            <select v-model="category" class="field w-full"><option>随笔</option><option>复盘</option><option>学习笔记</option><option>规划</option><option>生活</option></select>
          </label>
          <label><span class="field-label">标签（逗号分隔）</span><input v-model="tags" class="field w-full" placeholder="复盘,时间线" /></label>
          <label class="span-2"><span class="field-label">封面（可选）</span><input class="field w-full" type="file" accept="image/*" @change="upload" /></label>
          <label class="span-2"><span class="field-label">正文（Markdown）</span><textarea v-model="body" class="field w-full font-mono" rows="16" placeholder="# 标题&#10;&#10;写点什么…"></textarea></label>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn btn-mint" :disabled="busy" @click="publish('PUBLISHED')"><span class="btn-glyph">✓</span> 发布</button>
          <button class="btn btn-amber" :disabled="busy" @click="publish('DRAFT')"><span class="btn-glyph">▤</span> 存草稿</button>
        </div>
        <p v-if="msg" class="mt-3 text-[12px] text-mint">{{ msg }}</p>
        <p v-if="error" class="mt-3 text-[12px] text-rose-500">{{ error }}</p>
      </div>
      <div class="glass p-6">
        <h2 class="section-title mb-4">实时预览</h2>
        <h3 class="text-[18px] font-semibold ink">{{ title || '未命名' }}</h3>
        <div class="mt-3 flex flex-wrap gap-1.5"><span class="pill pill-rose">{{ category }}</span><span v-for="t in tags.split(',')" :key="t" class="pill pill-neutral">{{ t }}</span></div>
        <div class="markdown-body mt-4 text-[13px] leading-7 muted" v-html="preview"></div>
      </div>
    </div>
  </section>
</template>
