<script setup>
import { R } from '@/api'
import ResourcePage from '@/components/ResourcePage.vue'

const cfg = {
  "eyebrow": "ARCHIVE / CERTIFICATES",
  "title": "证书档案",
  "description": "证书、颁发机构与附件统一归档，可同步到简历。",
  "color": "amber",
  "fields": [
    {
      "key": "name",
      "label": "证书名称",
      "type": "text",
      "required": true
    },
    {
      "key": "issuer",
      "label": "颁发机构",
      "type": "text"
    },
    {
      "key": "issuedAt",
      "label": "取得日期",
      "type": "date"
    },
    {
      "key": "category",
      "label": "类别",
      "type": "text"
    },
    {
      "key": "fileUrl",
      "label": "附件地址",
      "type": "text"
    }
  ],
  "columns": [
    {
      "key": "issuedAt",
      "label": "取得日期",
      "type": "date"
    },
    {
      "key": "name",
      "label": "名称",
      "type": "text"
    },
    {
      "key": "issuer",
      "label": "机构",
      "type": "text"
    },
    {
      "key": "category",
      "label": "类别",
      "type": "text"
    }
  ],
  "cardView": true,
  "readonly": false
}
cfg.api = R.archive.certificates
</script>

<template>
  <ResourcePage v-bind="cfg" />
</template>
