<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "LEARNING / BANK",
  "title": "题库管理",
  "description": "题干、选项、答案与知识点统一维护，供练习中心随机抽题。",
  "color": "sky",
  "fields": [
    {
      "key": "subject",
      "label": "学科",
      "type": "text",
      "required": true
    },
    {
      "key": "knowledgePoint",
      "label": "知识点",
      "type": "text"
    },
    {
      "key": "title",
      "label": "题目标题",
      "type": "text",
      "required": true
    },
    {
      "key": "content",
      "label": "题干",
      "type": "textarea",
      "span": 2
    },
    {
      "key": "optionsJson",
      "label": "选项(JSON)",
      "type": "textarea",
      "span": 2
    },
    {
      "key": "answer",
      "label": "答案",
      "type": "text"
    },
    {
      "key": "explanation",
      "label": "解析",
      "type": "textarea",
      "span": 2
    },
    {
      "key": "difficulty",
      "label": "难度",
      "type": "select",
      "options": [
        "容易",
        "中等",
        "困难"
      ]
    },
    {
      "key": "source",
      "label": "来源",
      "type": "text"
    }
  ],
  "columns": [
    {
      "key": "subject",
      "label": "学科",
      "type": "text"
    },
    {
      "key": "title",
      "label": "标题",
      "type": "text"
    },
    {
      "key": "knowledgePoint",
      "label": "知识点",
      "type": "text"
    },
    {
      "key": "difficulty",
      "label": "难度",
      "type": "text"
    },
    {
      "key": "answer",
      "label": "答案",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.learning.questions
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
