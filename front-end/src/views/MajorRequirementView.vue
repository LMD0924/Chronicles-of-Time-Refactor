<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / MAJORS",
  "title": "专业要求",
  "description": "专业代码、层次与选科要求，驱动选科推荐与志愿匹配。",
  "color": "teal",
  "fields": [
    {
      "key": "majorCode",
      "label": "专业代码",
      "type": "text",
      "required": true
    },
    {
      "key": "majorName",
      "label": "专业名称",
      "type": "text",
      "required": true
    },
    {
      "key": "category",
      "label": "门类",
      "type": "text"
    },
    {
      "key": "level",
      "label": "层次",
      "type": "select",
      "options": [
        "热门",
        "常规",
        "冷门"
      ]
    },
    {
      "key": "requiredSubjects",
      "label": "选科要求",
      "type": "text"
    },
    {
      "key": "description",
      "label": "说明",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "majorCode",
      "label": "代码",
      "type": "text"
    },
    {
      "key": "majorName",
      "label": "专业",
      "type": "text"
    },
    {
      "key": "category",
      "label": "门类",
      "type": "text"
    },
    {
      "key": "level",
      "label": "层次",
      "type": "text"
    },
    {
      "key": "requiredSubjects",
      "label": "选科要求",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.planning.majorRequirements
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
