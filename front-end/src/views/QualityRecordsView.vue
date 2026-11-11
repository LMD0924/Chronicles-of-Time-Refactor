<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / QUALITY",
  "title": "综评记录",
  "description": "综合素质评价材料的时间线归档。",
  "color": "violet",
  "fields": [
    {
      "key": "title",
      "label": "标题",
      "type": "text",
      "required": true
    },
    {
      "key": "category",
      "label": "类别",
      "type": "select",
      "options": [
        "思想品德",
        "学业水平",
        "身心健康",
        "艺术素养",
        "劳动实践"
      ]
    },
    {
      "key": "score",
      "label": "分值",
      "type": "number"
    },
    {
      "key": "occurredAt",
      "label": "日期",
      "type": "date"
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
      "key": "score",
      "label": "分值",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.planning.qualityRecords
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
