<template>
  <div class="network-graph-view">
    <VChart v-if="option" :option="option" autoresize style="height: 320px; width: 100%" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import VChart from 'vue-echarts'

use([CanvasRenderer, GraphChart])

const props = defineProps({
  // 边: [{from, to, label?, weight?}]
  edges: { type: Array, default: () => [] },
  // 节点可选: [{id, name, category?}], 缺省由边推断
  nodes: { type: Array, default: () => [] },
  // 边类型配色
  colorMap: { type: Object, default: () => ({}) }
})

const option = computed(() => {
  const nodeMap = {}
  const edges = (props.edges || []).map((e, i) => {
    if (!nodeMap[e.from]) nodeMap[e.from] = { id: e.from, name: e.from, symbolSize: 34, category: e.type || 'default' }
    if (!nodeMap[e.to]) nodeMap[e.to] = { id: e.to, name: e.to, symbolSize: 34, category: e.type || 'default' }
    return {
      source: e.from,
      target: e.to,
      label: { show: e.weight != null, formatter: String(e.weight) },
      lineStyle: { width: e.weight ? Math.min(6, 1 + e.weight / 10000) : 2, color: props.colorMap[e.type] || '#1a6ff5' }
    }
  })
  ;(props.nodes || []).forEach(n => { nodeMap[n.id] = nodeMap[n.id] || { id: n.id, name: n.name, symbolSize: 34, category: n.category || 'default' } })
  const nodes = Object.values(nodeMap)
  const cats = [...new Set(nodes.map(n => n.category))]
  return {
    tooltip: { formatter: p => p.dataType === 'edge' ? `${p.data.source} → ${p.data.target}` : `${p.name}` },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      data: nodes,
      links: edges,
      categories: cats.map(c => ({ name: c })),
      label: { show: true, position: 'bottom', fontSize: 10 },
      force: { repulsion: 220, edgeLength: [60, 120] }
    }]
  }
})
</script>

<style scoped>
.network-graph-view { background: #fafbfc; border-radius: 8px; padding: 8px; }
</style>
