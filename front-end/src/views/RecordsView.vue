<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "TIMELINE / RECORDS",
  "title": "成长记录",
  "description": "每一天都可以是一条时间线节点：记录学习时长、心情、睡眠与里程碑。",
  "color": "mint",
  "fields": [
    {
      "key": "recordDate",
      "label": "记录日期",
      "type": "date",
      "required": true
    },
    {
      "key": "title",
      "label": "标题",
      "type": "text",
      "required": true
    },
    {
      "key": "category",
      "label": "分类",
      "type": "select",
      "options": [
        "学习",
        "规划",
        "生活",
        "工作",
        "复盘"
      ]
    },
    {
      "key": "mood",
      "label": "心情",
      "type": "text"
    },
    {
      "key": "tags",
      "label": "标签",
      "type": "text"
    },
    {
      "key": "studyHours",
      "label": "学习时长(h)",
      "type": "number"
    },
    {
      "key": "sleepHours",
      "label": "睡眠时长(h)",
      "type": "number"
    },
    {
      "key": "exerciseHours",
      "label": "运动时长(h)",
      "type": "number"
    },
    {
      "key": "happinessIndex",
      "label": "幸福指数(0-10)",
      "type": "number"
    },
    {
      "key": "isMilestone",
      "label": "标为里程碑",
      "type": "checkbox"
    },
    {
      "key": "content",
      "label": "正文",
      "type": "textarea",
      "span": 2
    }
  ],
  "columns": [
    {
      "key": "recordDate",
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
      "label": "分类",
      "type": "text"
    },
    {
      "key": "studyHours",
      "label": "学习(h)",
      "type": "text"
    },
    {
      "key": "mood",
      "label": "心情",
      "type": "text"
    },
    {
      "key": "isMilestone",
      "label": "里程碑",
      "type": "text"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.timeline.records
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
