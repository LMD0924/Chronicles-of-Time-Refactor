<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / TARGETS",
  "title": "目标路径",
  "description": "把长期目标拆成阶段里程碑，与记录、学习数据互相印证。",
  "color": "rose",
  "fields": [
    {
      "key": "targetName",
      "label": "目标",
      "type": "text",
      "required": true
    },
    {
      "key": "stage",
      "label": "阶段",
      "type": "text"
    },
    {
      "key": "milestone",
      "label": "里程碑",
      "type": "text"
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
        "规划中",
        "进行中",
        "已达成"
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
      "key": "targetName",
      "label": "目标",
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
cfg.api = R.planning.targetPaths
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
