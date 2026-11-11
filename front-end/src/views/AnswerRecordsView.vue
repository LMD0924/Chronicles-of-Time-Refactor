<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "LEARNING / ANSWERS",
  "title": "答题记录",
  "description": "每一次作答都会留痕，用于生成热力图与学习路径。",
  "color": "mint",
  "fields": [
    {
      "key": "questionId",
      "label": "题目 ID",
      "type": "number"
    },
    {
      "key": "userAnswer",
      "label": "作答",
      "type": "text"
    },
    {
      "key": "correct",
      "label": "是否正确",
      "type": "checkbox"
    },
    {
      "key": "score",
      "label": "得分",
      "type": "number"
    },
    {
      "key": "answeredAt",
      "label": "作答时间",
      "type": "text"
    }
  ],
  "columns": [
    {
      "key": "answeredAt",
      "label": "时间",
      "type": "text"
    },
    {
      "key": "questionId",
      "label": "题目",
      "type": "text"
    },
    {
      "key": "userAnswer",
      "label": "作答",
      "type": "text"
    },
    {
      "key": "correct",
      "label": "正确",
      "type": "text"
    },
    {
      "key": "score",
      "label": "得分",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": true
}
cfg.api = R.learning.answers
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
