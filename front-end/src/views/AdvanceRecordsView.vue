<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "TIMELINE / ADVANCES",
  "title": "进阶记录",
  "description": "晋升、获奖、突破与能力跃迁，作为时间线上的高亮节点。",
  "color": "violet",
  "fields": [
    {
      "key": "title",
      "label": "标题",
      "type": "text",
      "required": true
    },
    {
      "key": "occurredAt",
      "label": "日期",
      "type": "date"
    },
    {
      "key": "category",
      "label": "类别",
      "type": "select",
      "options": [
        "晋升",
        "获奖",
        "认证",
        "突破",
        "其他"
      ]
    },
    {
      "key": "description",
      "label": "描述",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "occurredAt",
      "label": "日期",
      "type": "date"
    },
    {
      "key": "title",
      "label": "标题",
      "type": "text"
    },
    {
      "key": "category",
      "label": "类别",
      "type": "text"
    },
    {
      "key": "description",
      "label": "描述",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.timeline.advanceRecords
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
