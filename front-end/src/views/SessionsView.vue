<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "LEARNING / SESSIONS",
  "title": "练习场次",
  "description": "每轮练习的题量与正确率，汇入时间线。",
  "color": "teal",
  "fields": [
    {
      "key": "title",
      "label": "场次名称",
      "type": "text",
      "required": true
    },
    {
      "key": "questionCount",
      "label": "题量",
      "type": "number"
    },
    {
      "key": "correctCount",
      "label": "正确数",
      "type": "number"
    },
    {
      "key": "startedAt",
      "label": "开始时间",
      "type": "text"
    },
    {
      "key": "finishedAt",
      "label": "结束时间",
      "type": "text"
    }
  ],
  "columns": [
    {
      "key": "startedAt",
      "label": "开始",
      "type": "text"
    },
    {
      "key": "title",
      "label": "名称",
      "type": "text"
    },
    {
      "key": "questionCount",
      "label": "题量",
      "type": "text"
    },
    {
      "key": "correctCount",
      "label": "正确数",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.learning.sessions
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
