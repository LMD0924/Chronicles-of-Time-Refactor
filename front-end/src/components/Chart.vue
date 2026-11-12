<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, RadarChart, GraphChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useTheme } from '@/composables/useTheme'

echarts.use([LineChart, BarChart, PieChart, RadarChart, GraphChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 300 }
})
/** 可选：把 ECharts 的点击事件透传给业务页面（不监听时行为与原来完全一致）。 */
const emit = defineEmits(['point-click'])
const el = ref(null)
const { theme } = useTheme()
let chart = null

function render() {
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value)
    chart.on('click', (p) => emit('point-click', p))
  }
  const dark = theme.value === 'dark'
  const opt = JSON.parse(JSON.stringify(props.option))
  opt.backgroundColor = 'transparent'
  opt.textStyle = { color: dark ? '#c8d2e6' : '#4b5568', fontFamily: 'inherit' }
  if (opt.xAxis) {
    const axes = Array.isArray(opt.xAxis) ? opt.xAxis : [opt.xAxis]
    axes.forEach(a => { a.axisLine = { lineStyle: { color: dark ? '#39435a' : '#dce3f0' } }; a.axisLabel = { color: dark ? '#9aa8c0' : '#7a869c' } })
  }
  if (opt.yAxis) {
    const axes = Array.isArray(opt.yAxis) ? opt.yAxis : [opt.yAxis]
    axes.forEach(a => { a.splitLine = { lineStyle: { color: dark ? '#252d3e' : '#eef1f8' } }; a.axisLabel = { color: dark ? '#9aa8c0' : '#7a869c' } })
  }
  chart.setOption(opt, true)
}

function resize() { chart?.resize() }
onMounted(async () => { await nextTick(); render(); window.addEventListener('resize', resize) })
watch(() => props.option, render, { deep: true })
watch(theme, render)
onBeforeUnmount(() => { window.removeEventListener('resize', resize); chart?.dispose(); chart = null })
</script>

<template>
  <div ref="el" :style="{ height: height + 'px', width: '100%' }"></div>
</template>
