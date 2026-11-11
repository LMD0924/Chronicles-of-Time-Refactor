<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "LEARNING / MISTAKES",
  "title": "错题本",
  "description": "错题自动归集，可标记掌握、累计复习次数。",
  "color": "rose",
  "fields": [
    {
      "key": "questionId",
      "label": "题目 ID",
      "type": "number"
    },
    {
      "key": "knowledgePoint",
      "label": "知识点",
      "type": "text"
    },
    {
      "key": "reason",
      "label": "错因",
      "type": "text"
    },
    {
      "key": "mastered",
      "label": "已掌握",
      "type": "checkbox"
    },
    {
      "key": "reviewCount",
      "label": "复习次数",
      "type": "number"
    }
  ],
  "columns": [
    {
      "key": "knowledgePoint",
      "label": "知识点",
      "type": "text"
    },
    {
      "key": "reason",
      "label": "错因",
      "type": "text"
    },
    {
      "key": "mastered",
      "label": "已掌握",
      "type": "text"
    },
    {
      "key": "reviewCount",
      "label": "复习次数",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.learning.mistakes
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
