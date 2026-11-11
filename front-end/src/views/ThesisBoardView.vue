<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "ARCHIVE / THESIS",
  "title": "论文看板",
  "description": "按阶段推进论文任务，截止日期进入时间线提醒。",
  "color": "violet",
  "fields": [
    {
      "key": "title",
      "label": "任务",
      "type": "text",
      "required": true
    },
    {
      "key": "stage",
      "label": "阶段",
      "type": "select",
      "options": [
        "选题",
        "开题",
        "初稿",
        "修改",
        "定稿",
        "答辩"
      ]
    },
    {
      "key": "deadline",
      "label": "截止日期",
      "type": "date"
    },
    {
      "key": "status",
      "label": "状态",
      "type": "select",
      "options": [
        "待办",
        "进行中",
        "已完成"
      ]
    },
    {
      "key": "note",
      "label": "备注",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "deadline",
      "label": "截止",
      "type": "date"
    },
    {
      "key": "title",
      "label": "任务",
      "type": "text"
    },
    {
      "key": "stage",
      "label": "阶段",
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
cfg.api = R.archive.thesisBoard
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
