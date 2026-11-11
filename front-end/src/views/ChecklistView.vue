<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / CHECKLIST",
  "title": "强基清单",
  "description": "逐项勾选准备事项，进度一目了然。",
  "color": "amber",
  "fields": [
    {
      "key": "title",
      "label": "事项",
      "type": "text",
      "required": true
    },
    {
      "key": "category",
      "label": "分组",
      "type": "text"
    },
    {
      "key": "done",
      "label": "已完成",
      "type": "checkbox"
    },
    {
      "key": "deadline",
      "label": "截止日期",
      "type": "date"
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
      "key": "done",
      "label": "完成",
      "type": "text"
    },
    {
      "key": "title",
      "label": "事项",
      "type": "text"
    },
    {
      "key": "category",
      "label": "分组",
      "type": "text"
    },
    {
      "key": "deadline",
      "label": "截止",
      "type": "date"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.planning.checklistItems
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
