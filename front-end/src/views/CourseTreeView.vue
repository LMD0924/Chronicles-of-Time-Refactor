<script setup>
import { onMounted, ref } from 'vue'
import { feature, R } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ResourcePage from '@/components/ResourcePage.vue'

const tree = ref([])
const cfg = {
  eyebrow: 'ARCHIVE / COURSE TREE', title: '课程节点', description: '通过 parentId 组织层级，前端自动拼装成树。', color: 'teal',
  api: R.archive.courseTree,
  fields: [{ key: 'parentId', label: '父节点 ID', type: 'number' }, { key: 'name', label: '名称', required: true },
           { key: 'nodeType', label: '类型', type: 'select', options: ['分类', '课程', '知识点'] },
           { key: 'credit', label: '学分', type: 'number' }, { key: 'sortNo', label: '排序', type: 'number' },
           { key: 'description', label: '说明', type: 'textarea', span: 2 }],
  columns: [{ key: 'name', label: '名称' }, { key: 'nodeType', label: '类型' }, { key: 'credit', label: '学分' }, { key: 'parentId', label: '父节点' }]
}
async function load() { try { tree.value = (await feature.courseTree()) || [] } catch { /* ignore */ } }
onMounted(load)
</script>

<template>
  <section>
    <PageHeader eyebrow="ARCHIVE / COURSE TREE" title="课程树"
      description="课程分类、课程与知识点的层级结构，可与学习图谱相互跳转。"
      :actions="[{ label: '知识图谱', icon: '⌘', color: 'violet', to: '/learning/graph' }, { label: '刷新', icon: '↻', color: 'slate', click: load }]" />
    <div class="glass p-6">
      <div v-for="root in tree" :key="root.id" class="mb-4">
        <div class="glass-flat flex items-center gap-3 p-3">
          <span class="pill pill-teal">{{ root.nodeType || '分类' }}</span>
          <span class="text-[14px] font-medium ink">{{ root.name }}</span>
          <span class="ml-auto text-[11px] faint">{{ root.credit || 0 }} 学分</span>
        </div>
        <div v-for="child in (root.children || [])" :key="child.id" class="ml-6 mt-2 border-l-2 border-hairline pl-4">
          <div class="flex items-center gap-2 py-1 text-[12px]">
            <span class="pill pill-sky">{{ child.nodeType || '课程' }}</span>
            <span class="ink">{{ child.name }}</span>
            <span class="ml-auto faint">{{ child.credit || 0 }} 学分</span>
          </div>
        </div>
        <p v-if="!(root.children || []).length" class="ml-6 mt-2 text-[11px] faint">暂无子节点</p>
      </div>
      <p v-if="!tree.length" class="text-[12px] muted">还没有课程节点，可在下方新增（父节点 ID 留空即为根节点）</p>
    </div>
    <div class="mt-7"><ResourcePage v-bind="cfg" /></div>
  </section>
</template>
