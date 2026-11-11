<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "PLANNING / GRADING",
  "title": "等级量表",
  "description": "分数区间、等级与比例配置，供选科与成绩换算使用。",
  "color": "slate",
  "fields": [
    {
      "key": "subjectId",
      "label": "学科 ID",
      "type": "number"
    },
    {
      "key": "level",
      "label": "等级",
      "type": "text"
    },
    {
      "key": "minScore",
      "label": "最低分",
      "type": "number"
    },
    {
      "key": "maxScore",
      "label": "最高分",
      "type": "number"
    },
    {
      "key": "ratio",
      "label": "比例",
      "type": "number"
    },
    {
      "key": "active",
      "label": "启用",
      "type": "checkbox"
    }
  ],
  "columns": [
    {
      "key": "subjectId",
      "label": "学科",
      "type": "text"
    },
    {
      "key": "level",
      "label": "等级",
      "type": "text"
    },
    {
      "key": "minScore",
      "label": "最低",
      "type": "text"
    },
    {
      "key": "maxScore",
      "label": "最高",
      "type": "text"
    },
    {
      "key": "ratio",
      "label": "比例",
      "type": "text"
    }
  ],
  "cardView": false,
  "readonly": false
}
cfg.api = R.planning.gradingScales
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
