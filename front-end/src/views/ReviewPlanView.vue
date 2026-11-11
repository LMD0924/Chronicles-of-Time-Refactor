<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / REVIEW",
  "title": "复习计划",
  "description": "按学科制定计划并跟踪进度，到期自动出现在时间线上。",
  "color": "blue",
  "fields": [
    {
      "key": "subject",
      "label": "学科",
      "type": "text",
      "required": true
    },
    {
      "key": "title",
      "label": "计划标题",
      "type": "text",
      "required": true
    },
    {
      "key": "content",
      "label": "计划内容",
      "type": "textarea",
      "span": 2
    },
    {
      "key": "targetDate",
      "label": "目标日期",
      "type": "date"
    },
    {
      "key": "progress",
      "label": "进度(%)",
      "type": "number"
    },
    {
      "key": "status",
      "label": "状态",
      "type": "select",
      "options": [
        "未开始",
        "进行中",
        "已完成",
        "已搁置"
      ]
    }
  ],
  "columns": [
    {
      "key": "targetDate",
      "label": "目标日期",
      "type": "date"
    },
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
      "key": "progress",
      "label": "进度",
      "type": "text"
    },
    {
      "key": "status",
      "label": "状态",
      "type": "text"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.planning.reviewPlans
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
