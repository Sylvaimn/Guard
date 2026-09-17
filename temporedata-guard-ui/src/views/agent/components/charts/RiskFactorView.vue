<template>
  <div class="risk-factor-view">
    <VChart v-if="option" :option="option" autoresize style="height: 200px; width: 100%" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, BarChart, GridComponent])

const props = defineProps({
  // 因素对象 {traffic, contact, gather, fund, watch}
  factors: { type: Object, default: () => ({}) },
  score: { type: Number, default: 0 },
  level: { type: String, default: 'LOW' }
})

const LEVEL_COLOR = { RED: '#ef4444', YELLOW: '#f59e0b', ORANGE: '#fb923c', LOW: '#9ca3af' }

const LABELS = { traffic: '轨迹活跃', contact: '通联度', gather: '聚集', fund: '资金流入', watch: '布控命中' }

const option = computed(() => {
  const names = Object.keys(props.factors || {}).filter(k => LABELS[k])
  return {
    tooltip: {},
    grid: { left: 80, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'value', max: 100 },
    yAxis: { type: 'category', data: names.map(n => LABELS[n]).reverse() },
    series: [{
      type: 'bar',
      data: names.map(n => props.factors[n]).reverse(),
      itemStyle: { color: LEVEL_COLOR[props.level] || '#1a6ff5' },
      label: { show: true, position: 'right' }
    }]
  }
})
</script>

<style scoped>
.risk-factor-view { background: #fafbfc; border-radius: 8px; padding: 8px; }
</style>
