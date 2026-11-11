<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "TIMELINE / MILESTONES",
  "title": "里程碑",
  "description": "重要时刻会自动出现在时间线上，并可与成长记录互相关联。",
  "color": "amber",
  "fields": [
    {
      "key": "title",
      "label": "标题",
      "type": "text",
      "required": true
    },
    {
      "key": "occurredAt",
      "label": "发生日期",
      "type": "date"
    },
    {
      "key": "icon",
      "label": "图标",
      "type": "select",
      "options": [
        "star",
        "flag",
        "book",
        "trophy"
      ]
    },
    {
      "key": "color",
      "label": "主题色",
      "type": "select",
      "options": [
        "amber",
        "mint",
        "sky",
        "violet",
        "rose",
        "teal"
      ]
    },
    {
      "key": "linkedRecordId",
      "label": "关联记录 ID",
      "type": "number"
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
      "key": "color",
      "label": "色彩",
      "type": "text"
    },
    {
      "key": "description",
      "label": "描述",
      "type": "text"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.timeline.milestones
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
