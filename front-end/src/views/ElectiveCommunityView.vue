<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "ARCHIVE / ELECTIVE",
  "title": "选课参考",
  "description": "课程评价与经验帖，供后续选课决策参考。",
  "color": "mint",
  "fields": [
    {
      "key": "courseName",
      "label": "课程名称",
      "type": "text",
      "required": true
    },
    {
      "key": "rating",
      "label": "评分",
      "type": "number"
    },
    {
      "key": "content",
      "label": "评价内容",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "courseName",
      "label": "课程",
      "type": "text"
    },
    {
      "key": "rating",
      "label": "评分",
      "type": "text"
    },
    {
      "key": "content",
      "label": "评价",
      "type": "text"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.archive.electivePosts
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
