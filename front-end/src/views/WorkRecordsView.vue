<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "TIMELINE / WORK",
  "title": "工作记录",
  "description": "岗位、经历与阶段性产出，统一挂到同一条时间线上。",
  "color": "teal",
  "fields": [
    {
      "key": "company",
      "label": "单位/组织",
      "type": "text",
      "required": true
    },
    {
      "key": "position",
      "label": "岗位/角色",
      "type": "text"
    },
    {
      "key": "startDate",
      "label": "开始日期",
      "type": "date"
    },
    {
      "key": "endDate",
      "label": "结束日期",
      "type": "date"
    },
    {
      "key": "category",
      "label": "类型",
      "type": "select",
      "options": [
        "全职",
        "实习",
        "项目",
        "兼职",
        "志愿"
      ]
    },
    {
      "key": "content",
      "label": "工作内容",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "startDate",
      "label": "开始",
      "type": "date"
    },
    {
      "key": "company",
      "label": "单位",
      "type": "text"
    },
    {
      "key": "position",
      "label": "岗位",
      "type": "text"
    },
    {
      "key": "category",
      "label": "类型",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.timeline.workRecords
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
