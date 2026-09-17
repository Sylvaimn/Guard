<template>
  <div ref="containerRef" class="lineage-g6-container">
    <div class="canvas-actions">
      <button class="ca-btn" title="放大" @click="zoomBy(1.25)">+</button>
      <button class="ca-btn" title="缩小" @click="zoomBy(0.8)">−</button>
      <div class="ca-sep"></div>
      <button class="ca-btn" title="适应画布" @click="fitView">⤢</button>
      <button class="ca-btn" title="重置缩放 100%" @click="resetView">1:1</button>
      <div class="ca-sep"></div>
      <button class="ca-btn" :class="{ active: minimapVisible }" title="小地图" @click="toggleMinimap">🗺</button>
      <button class="ca-btn" :class="{ active: legendVisible }" title="图例" @click="toggleLegend">ℹ</button>
    </div>

    <!-- 自定义小地图 -->
    <div v-show="minimapVisible" class="lineage-minimap">
      <canvas ref="minimapCanvasRef" width="180" height="120"></canvas>
    </div>

    <!-- 图例 -->
    <div v-if="legendVisible" class="legend-panel">
      <div class="legend-title">图例</div>
      <div class="legend-section">
        <div class="legend-sub">节点类型</div>
        <div class="legend-item" v-for="n in legendNodes" :key="n.label">
          <span class="legend-swatch" :style="{ background: n.color }"></span>
          <span class="legend-label">{{ n.label }}</span>
        </div>
      </div>
      <div class="legend-section">
        <div class="legend-sub">边类型</div>
        <div class="legend-item" v-for="e in legendEdges" :key="e.label">
          <span class="legend-line" :style="{ borderColor: e.color, borderStyle: e.dash ? 'dashed' : 'solid' }"></span>
          <span class="legend-label">{{ e.label }}</span>
        </div>
      </div>
    </div>

    <!-- 边详情浮层 -->
    <div v-if="edgeInfo.visible" class="edge-info" :style="{ left: edgeInfo.x + 'px', top: edgeInfo.y + 'px' }">
      <div class="edge-info-head">
        <el-tag size="small" :type="edgeInfo.tagType" effect="dark">{{ edgeInfo.relationLabel }}</el-tag>
        <button class="edge-info-close" @click="edgeInfo.visible = false">×</button>
      </div>
      <div class="edge-info-body">
        <div class="edge-info-row"><span class="ei-k">来源</span><span class="ei-v">{{ edgeInfo.sourceName }}</span></div>
        <div class="edge-info-row"><span class="ei-k">目标</span><span class="ei-v">{{ edgeInfo.targetName }}</span></div>
        <div v-if="edgeInfo.taskName" class="edge-info-row"><span class="ei-k">任务</span><span class="ei-v">{{ edgeInfo.taskName }}</span></div>
        <div v-if="edgeInfo.sql" class="edge-info-row"><span class="ei-k">SQL</span><span class="ei-v ei-sql">{{ edgeInfo.sql }}</span></div>
        <div v-if="edgeInfo.fieldLinks && edgeInfo.fieldLinks.length" class="edge-info-fields">
          <div class="edge-info-sub">字段映射</div>
          <div v-for="(f, i) in edgeInfo.fieldLinks.slice(0, 8)" :key="i" class="edge-info-field">
            <span class="eif-s">{{ f.sourceColumn }}</span> → <span class="eif-t">{{ f.targetColumn }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="contextMenu.visible" class="ctx-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div v-if="contextMenu.isAgg" class="ctx-item" @click="ctxExpand">展开该层节点</div>
      <template v-else>
        <div class="ctx-item" @click="ctxDetail">查看详情</div>
        <div class="ctx-item" @click="ctxRefocus">以此节点为中心</div>
      </template>
    </div>
    <div v-show="tooltip.visible" class="lineage-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
      <div class="lt-head">
        <span class="lt-type">{{ tooltip.typeTag }}</span>
        <span class="lt-name">{{ tooltip.name }}</span>
      </div>
      <div class="lt-row" v-if="tooltip.datasourceName"><span class="lt-k">数据源</span><span class="lt-v">{{ tooltip.datasourceName }}</span></div>
      <div class="lt-row" v-if="tooltip.relations.length"><span class="lt-k">关联任务</span><span class="lt-v">{{ tooltip.relations.join('、') }}</span></div>
      <div class="lt-row" v-if="tooltip.sql"><span class="lt-k">转换 SQL</span><span class="lt-v lt-sql">{{ tooltip.sql }}</span></div>
      <div class="lt-divider"></div>
      <div class="lt-row"><span class="lt-k">层级</span><span class="lt-v">L{{ tooltip.layer }}</span></div>
      <div class="lt-row"><span class="lt-k">热度</span><span class="lt-v">↑{{ tooltip.deg }} 条连线</span></div>
      <div class="lt-row" v-if="tooltip.rowCount != null"><span class="lt-k">行数</span><span class="lt-v">{{ tooltip.rowCount }}</span></div>
      <div class="lt-row" v-if="tooltip.columnCount != null"><span class="lt-k">列数</span><span class="lt-v">{{ tooltip.columnCount }}</span></div>
      <div class="lt-row" v-if="tooltip.category"><span class="lt-k">位置</span><span class="lt-v">{{ tooltip.category === 'root' ? '根节点' : tooltip.category === 'upstream' ? '上游' : tooltip.category === 'downstream' ? '下游' : tooltip.category }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { Graph } from '@antv/g6'

const props = defineProps({
  data: { type: Object, default: null },
  layout: { type: String, default: 'layer' },
  cycleEdgeKeys: { type: Array, default: () => [] }
})

const emit = defineEmits(['node-click', 'node-dblclick'])

const containerRef = ref(null)
const graphRef = shallowRef(null)
const minimapCanvasRef = ref(null)
const rawNodes = ref([])
const rawEdges = ref([])
const expandedLayers = ref(new Set())
const minimapVisible = ref(true)
const legendVisible = ref(false)
const edgeInfo = reactive({ visible: false, x: 0, y: 0, sourceName: '', targetName: '', taskName: '', sql: '', fieldLinks: [], relationLabel: '', tagType: '' })
const tooltip = reactive({
  visible: false, x: 0, y: 0, name: '', typeTag: '', datasourceName: '',
  relations: [], sql: '', layer: 0, deg: 0, rowCount: null, columnCount: null, category: ''
})
const contextMenu = reactive({ visible: false, x: 0, y: 0, node: null, isAgg: false })
let animFrame = null
let flowOffset = 0
let clickTimer = null
let minimapPlugin = null

const AGG_THRESHOLD = 50

const legendNodes = [
  { label: '根节点', color: '#fbbf24' },
  { label: '数据表', color: '#22d3ee' },
  { label: '字段', color: '#c4b5fd' },
  { label: '任务', color: '#fb7185' },
  { label: '数据源', color: '#fb923c' },
  { label: '报表', color: '#a78bfa' },
  { label: '聚合节点', color: '#64748b' }
]

const legendEdges = [
  { label: '数据流转 FLOWS_TO', color: '#10b981', dash: false },
  { label: '衍生 DERIVED_FROM', color: '#a78bfa', dash: false },
  { label: '产出 PRODUCES', color: '#f59e0b', dash: false },
  { label: '消费 CONSUMES', color: '#38bdf8', dash: false }
]

function edgeRelationLabel(t) {
  return t === 'FLOWS_TO' ? '数据流转' : t === 'DERIVED_FROM' ? '衍生' : t === 'PRODUCES' ? '产出' : t === 'CONSUMES' ? '消费' : (t || '关联')
}

function edgeTagType(t) {
  return t === 'FLOWS_TO' ? 'success' : t === 'DERIVED_FROM' ? 'warning' : t === 'PRODUCES' ? 'danger' : t === 'CONSUMES' ? 'primary' : 'info'
}

function toggleMinimap() {
  minimapVisible.value = !minimapVisible.value
  if (minimapVisible.value) drawMinimap()
}

// 小地图持续跟随：RAF 节流重绘（缩放/平移/布局动画期间保持同步）
let minimapTick = null
function startMinimapTicker() {
  stopMinimapTicker()
  const tick = () => {
    if (minimapVisible.value) {
      drawMinimap()
      minimapTick = setTimeout(tick, 300)
    } else {
      minimapTick = null
    }
  }
  minimapTick = setTimeout(tick, 300)
}
function stopMinimapTicker() {
  if (minimapTick) {
    clearTimeout(minimapTick)
    minimapTick = null
  }
}

function drawMinimap() {
  const graph = graphRef.value
  const canvas = minimapCanvasRef.value
  if (!graph || !canvas) return
  try {
    const W = 180, H = 120, pad = 8
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, W, H)
    ctx.fillStyle = 'rgba(15, 23, 42, 0.92)'
    ctx.fillRect(0, 0, W, H)
    const nodes = graph.getNodeData()
    if (!nodes.length) return
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const n of nodes) {
      const p = graph.getElementPosition(n.id)
      if (!p) continue
      if (p[0] < minX) minX = p[0]
      if (p[1] < minY) minY = p[1]
      if (p[0] > maxX) maxX = p[0]
      if (p[1] > maxY) maxY = p[1]
    }
    if (maxX <= minX) maxX = minX + 1
    if (maxY <= minY) maxY = minY + 1
    const scale = Math.min((W - pad * 2) / (maxX - minX), (H - pad * 2) / (maxY - minY))
    const ox = (W - (maxX - minX) * scale) / 2 - minX * scale
    const oy = (H - (maxY - minY) * scale) / 2 - minY * scale
    // edges
    ctx.strokeStyle = 'rgba(52, 211, 153, 0.35)'
    ctx.lineWidth = 1
    for (const e of graph.getEdgeData()) {
      const s = graph.getElementPosition(e.source)
      const t = graph.getElementPosition(e.target)
      if (!s || !t) continue
      ctx.beginPath()
      ctx.moveTo(s[0] * scale + ox, s[1] * scale + oy)
      ctx.lineTo(t[0] * scale + ox, t[1] * scale + oy)
      ctx.stroke()
    }
    // nodes
    for (const n of nodes) {
      const p = graph.getElementPosition(n.id)
      if (!p) continue
      ctx.fillStyle = '#38bdf8'
      ctx.beginPath()
      ctx.arc(p[0] * scale + ox, p[1] * scale + oy, 2, 0, Math.PI * 2)
      ctx.fill()
    }
    // viewport rect
    try {
      const zoom = graph.getZoom()
      const vw = containerRef.value?.clientWidth || 0
      const vh = containerRef.value?.clientHeight || 0
      // 视口中心对应的画布坐标（视口中心 = [vw/2, vh/2] 的视口坐标）
      const vc = graph.getCanvasByViewport([vw / 2, vh / 2])
      const w = vw / zoom * scale
      const h = vh / zoom * scale
      const cx = vc[0] * scale + ox - w / 2
      const cy = vc[1] * scale + oy - h / 2
      ctx.strokeStyle = '#fbbf24'
      ctx.lineWidth = 1.5
      ctx.strokeRect(cx, cy, w, h)
    } catch (e) { /* ignore */ }
  } catch (e) { /* ignore */ }
}

function toggleLegend() {
  legendVisible.value = !legendVisible.value
}

function nodePalette(d) {
  if (d.nodeType === 'AGG') return { fill: '#475569', stroke: '#94a3b8' }
  if (d.nodeType === 'COLUMN') return { fill: '#5b21b6', stroke: '#a78bfa' }
  if (d.nodeType === 'TASK') return { fill: '#9f1239', stroke: '#fb7185' }
  if (d.nodeType === 'REPORT') return { fill: '#4c1d95', stroke: '#a78bfa' }
  if (d.nodeType === 'DATA_SOURCE') return { fill: '#9a3412', stroke: '#fb923c' }
  if (d.category === 'root') return { fill: '#92400e', stroke: '#fbbf24' }
  if (d.category === 'upstream') return { fill: '#065f46', stroke: '#34d399' }
  return { fill: '#155e75', stroke: '#22d3ee' }
}

function nodeShape(d) {
  if (d.nodeType === 'AGG') return 'diamond'
  return 'rect'
}

function degreeMap() {
  const map = {}
  for (const e of rawEdges.value) {
    map[e.source] = (map[e.source] || 0) + 1
    map[e.target] = (map[e.target] || 0) + 1
  }
  return map
}

function nodeSize(d) {
  if (d.nodeType === 'AGG') return [96, 44]
  if (d.nodeType === 'COLUMN') return [Math.max(64, (d.name || '').length * 7 + 26), 24]
  const deg = degreeMap()[d.id] || 0
  const w = Math.min(220, Math.max(150, (d.name || '').length * 9 + 36 + Math.min(deg * 5, 40)))
  return [w, 52]
}

function typeTag(n) {
  if (n.nodeType === 'TASK') return 'JOB'
  if (n.nodeType === 'TABLE' || n.nodeType === 'DATA_SOURCE') return 'DATASET'
  if (n.nodeType === 'COLUMN') return 'FIELD'
  if (n.nodeType === 'AGG') return 'AGG'
  return n.nodeType || 'NODE'
}

function subLabel(d) {
  if (d.nodeType === 'AGG') return `${d.aggregatedCount ?? ''}节点聚合`
  if (d.nodeType === 'COLUMN') return ''
  const deg = degreeMap()[d.id] || 0
  const rows = d.rowCount != null ? ` · ${d.rowCount}行` : ''
  return `${typeTag(d)} · L${d.layer ?? 0} · ↑${deg}${rows}`
}

function buildData() {
  const degrees = degreeMap()
  let nodes = rawNodes.value
  let edges = rawEdges.value
  const isAggregated = rawNodes.value.length > AGG_THRESHOLD

  // Node aggregation (+N) when the graph exceeds the threshold
  if (rawNodes.value.length > AGG_THRESHOLD) {
    const byLayer = {}
    for (const n of rawNodes.value) {
      const layer = n.layer ?? 0
      ;(byLayer[layer] = byLayer[layer] || []).push(n)
    }
    const aggregated = {}
    const realNodes = []
    for (const [layer, list] of Object.entries(byLayer)) {
      if (Number(layer) === 0 || expandedLayers.value.has(Number(layer)) || list.length <= 3) {
        realNodes.push(...list)
      } else {
        const aggId = 'AGG:L' + layer
        aggregated[aggId] = list.map(n => n.id)
        realNodes.push({
          id: aggId,
          name: `L${layer} · ${list.length} 节点`,
          nodeType: 'AGG',
          category: 'aggregate',
          layer: Number(layer),
          aggregatedCount: list.length,
          aggregated: true
        })
      }
    }
    nodes = realNodes
    edges = edges.map(e => {
      const srcAgg = Object.keys(aggregated).find(k => aggregated[k].includes(e.source))
      const tgtAgg = Object.keys(aggregated).find(k => aggregated[k].includes(e.target))
      if (!srcAgg && !tgtAgg) return e
      return {
        ...e,
        source: srcAgg || e.source,
        target: tgtAgg || e.target,
        data: { ...e.data, aggregated: true }
      }
    })
  }

  return {
    nodes: nodes.map(n => {
      const pal = nodePalette(n)
      const size = nodeSize(n)
      const deg = degrees[n.id] || 0
      const isCard = !['AGG', 'COLUMN'].includes(n.nodeType)
      const isCol = n.nodeType === 'COLUMN'
      const sub = subLabel(n)
      return {
        id: n.id,
        data: { ...n },
        style: {
          size,
          shape: nodeShape(n),
          radius: isCol ? 3 : (isCard ? 6 : 4),
          fill: pal.fill,
          stroke: pal.stroke,
          lineWidth: isCol ? 1.5 : 2.2,
          shadowColor: 'rgba(0,0,0,0.35)',
          shadowBlur: isCard ? 5 : 2,
          labelText: isCol ? n.name : (sub ? `${n.name}\n${sub}` : n.name),
          labelFill: n.nodeType === 'AGG' ? '#ffffff' : (isCol ? '#ede9fe' : '#ffffff'),
          labelFontSize: isCol ? 10.5 : (n.nodeType === 'AGG' ? 11.5 : 11.5),
          labelFontFamily: 'Menlo, Consolas, monospace',
          labelFontWeight: isCard ? 700 : 400,
          labelPosition: 'center',
          labelPlacement: 'center',
          labelBackground: isCard,
          labelBackgroundFill: '#020617',
          labelBackgroundRadius: 4
        }
      }
    }),
    edges: edges.map((e, i) => {
      const isCycle = props.cycleEdgeKeys.includes(`${e.source}|${e.target}`)
      const mainFlow = e.relationType === 'FLOWS_TO'
      return {
        id: e.id || 'e' + i,
        source: e.source,
        target: e.target,
        data: { ...e },
        style: {
          stroke: isCycle ? '#ef4444' : edgeColor(e.relationType),
          lineWidth: isCycle ? 3.5 : (mainFlow ? (isAggregated ? 2.5 : 3) : 2),
          lineCap: 'round',
          opacity: 1,
          endArrow: true,
          endArrowSize: mainFlow ? 12 : 10,
          lineDash: undefined,
          // 大图聚合模式下隐藏次要边标签，避免文字密集重叠
          labelText: isAggregated || e.relationType === 'FLOWS_TO' ? '' : edgeRelationLabel(e.relationType),
          labelFill: '#e2e8f0',
          labelFontSize: 9.5,
          labelBackground: true,
          labelBackgroundFill: '#020617',
          labelBackgroundRadius: 3
        }
      }
    })
  }
}

function edgeColor(t) {
  return t === 'FLOWS_TO' ? '#10b981'
    : t === 'DERIVED_FROM' ? '#a78bfa'
    : t === 'PRODUCES' ? '#f59e0b'
    : t === 'CONSUMES' ? '#38bdf8'
    : '#94a3b8'
}

function layoutOption() {
  if (props.layout === 'force') {
    return { type: 'force', linkDistance: 150, nodeStrength: -200 }
  }
  if (props.layout === 'radial') {
    return { type: 'radial', unitRadius: 160, linkDistance: 200, nodeSpacing: 24 }
  }
  return { type: 'dagre', rankdir: 'LR', nodesep: 32, ranksep: 120 }
}

function render() {
  const graph = graphRef.value
  if (!graph) return
  graph.setData(buildData())
  graph.render()
  requestAnimationFrame(() => {
    if (minimapVisible.value) drawMinimap()
  })
}

function zoomBy(factor) {
  const graph = graphRef.value
  if (!graph) return
  try { graph.zoomBy(factor) } catch (e) { /* ignore */ }
}

function fitView() {
  const graph = graphRef.value
  if (!graph) return
  try { graph.fitView() } catch (e) { /* ignore */ }
}

function resetView() {
  const graph = graphRef.value
  if (!graph) return
  try { graph.zoomTo(1) } catch (e) { /* ignore */ }
}

function ctxDetail() {
  contextMenu.visible = false
  if (contextMenu.node && !contextMenu.isAgg) emit('node-click', contextMenu.node)
}
function ctxRefocus() {
  contextMenu.visible = false
  if (contextMenu.node && !contextMenu.isAgg) emit('node-dblclick', contextMenu.node)
}
function ctxExpand() {
  contextMenu.visible = false
  if (contextMenu.node?.layer != null) expandLayer(contextMenu.node.layer)
}

function expandLayer(layer) {
  expandedLayers.value.add(Number(layer))
  render()
}

// ---- Subtle pulse animation for FLOWS_TO edges (glow breathing, no dash) ----
function startFlowAnimation() {
  let layoutBusy = false
  const g = graphRef.value
  if (g) {
    g.on('beforelayout', () => { layoutBusy = true })
    g.on('afterlayout', () => { layoutBusy = false })
  }
  const tick = () => {
    const graph = graphRef.value
    if (graph && !layoutBusy) {
      try {
        const data = graph.getEdgeData()
        const pulse = []
        for (const e of data) {
          if (e.data?.relationType === 'FLOWS_TO') {
            const base = 0.65 + 0.35 * (0.5 + 0.5 * Math.sin(flowOffset / 18))
            pulse.push({ id: e.id, style: { opacity: base } })
          }
        }
        if (pulse.length) {
          graph.updateEdgeData(pulse)
          graph.draw({ silence: true })
        }
      } catch (e) { /* skip frame during transient layout race */ }
    }
    flowOffset = (flowOffset + 1) % 120
    animFrame = requestAnimationFrame(tick)
  }
  animFrame = requestAnimationFrame(tick)
}

function stopFlowAnimation() {
  if (animFrame) cancelAnimationFrame(animFrame)
  animFrame = null
}

// ---- Graph lifecycle ----
onMounted(() => {
  const graph = new Graph({
    container: containerRef.value,
    autoFit: 'view',
    data: { nodes: [], edges: [] },
    layout: layoutOption(),
    behaviors: [
      'drag-canvas', 'zoom-canvas', 'drag-element', 'click-select',
      { type: 'hover-activate', degree: 1, activeState: 'active', inactiveState: 'inactive' }
    ],
    node: {
      state: {
        selected: { stroke: '#fbbf24', lineWidth: 4, shadowColor: '#fbbf24', shadowBlur: 14 },
        active: { stroke: '#fde68a', lineWidth: 3 },
        inactive: { opacity: 0.65 }
      }
    },
    edge: {
      state: {
        active: { stroke: '#fde68a', lineWidth: 3.5 },
        inactive: { opacity: 0.4 }
      }
    }
  })
  graphRef.value = graph
  try { window.__lineageGraph = graph } catch (e) { /* ignore */ }

  // ---- Custom minimap interactions ----
  let minimapPanTimer = null
  minimapCanvasRef.value?.addEventListener('pointerdown', (e) => {
    const graph2 = graphRef.value
    const canvas = minimapCanvasRef.value
    if (!graph2 || !canvas) return
    const rect = canvas.getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    const nodes = graph2.getNodeData()
    if (!nodes.length) return
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const n of nodes) {
      const p = graph2.getElementPosition(n.id)
      if (!p) continue
      if (p[0] < minX) minX = p[0]
      if (p[1] < minY) minY = p[1]
      if (p[0] > maxX) maxX = p[0]
      if (p[1] > maxY) maxY = p[1]
    }
    if (maxX <= minX) maxX = minX + 1
    if (maxY <= minY) maxY = minY + 1
    const scale = Math.min((180 - 16) / (maxX - minX), (120 - 16) / (maxY - minY))
    const ox = (180 - (maxX - minX) * scale) / 2 - minX * scale
    const oy = (120 - (maxY - minY) * scale) / 2 - minY * scale
    const wx = (px - ox) / scale
    const wy = (py - oy) / scale
    try {
      graph2.translateTo([wx, wy])
      requestAnimationFrame(() => { if (minimapVisible.value) drawMinimap() })
    } catch (err) { /* ignore */ }
  })

  // redraw minimap while panning/zooming
  graph.on('canvas:pointermove', (e) => {
    if (!minimapVisible.value) return
    if (e.targetType === 'canvas') {
      clearTimeout(minimapPanTimer)
      minimapPanTimer = setTimeout(() => drawMinimap(), 200)
    }
  })
  graph.on('wheel', () => {
    if (!minimapVisible.value) return
    clearTimeout(minimapPanTimer)
    minimapPanTimer = setTimeout(() => drawMinimap(), 200)
  })

  function onNodeClick(event) {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    const data = nodeData?.data || {}
    if (data.nodeType === 'AGG') {
      expandLayer(data.layer)
      return
    }
    emit('node-click', { ...data, id })
  }

  graph.on('node:click', (event) => {
    const data = event.target.id ? (graph.getNodeData(event.target.id)?.data || {}) : {}
    if (data.nodeType === 'AGG') {
      onNodeClick(event)
      return
    }
    clearTimeout(clickTimer)
    clickTimer = setTimeout(() => onNodeClick(event), 260)
  })

  graph.on('node:dblclick', (event) => {
    clearTimeout(clickTimer)
    const id = event.target.id
    if (!id) return
    const data = graph.getNodeData(id)?.data || {}
    if (data.nodeType === 'AGG') {
      expandLayer(data.layer)
      return
    }
    emit('node-dblclick', { ...data, id })
  })

  graph.on('node:contextmenu', (event) => {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    if (!nodeData) return
    const data = nodeData.data || {}
    contextMenu.node = { ...data, id }
    contextMenu.isAgg = data.nodeType === 'AGG'
    const rect = containerRef.value?.getBoundingClientRect()
    const x = event.client.x - rect.left + 8
    const y = event.client.y - rect.top + 8
    contextMenu.x = Math.min(x, (rect?.width || 400) - 180)
    contextMenu.y = Math.min(y, (rect?.height || 300) - 100)
    contextMenu.visible = true
  })

  function ctxDetail() {
    contextMenu.visible = false
    if (contextMenu.node && !contextMenu.isAgg) emit('node-click', contextMenu.node)
  }
  function ctxRefocus() {
    contextMenu.visible = false
    if (contextMenu.node && !contextMenu.isAgg) emit('node-dblclick', contextMenu.node)
  }
  function ctxExpand() {
    contextMenu.visible = false
    if (contextMenu.node?.layer != null) expandLayer(contextMenu.node.layer)
  }

  // ---- Edge interactions (hover highlight + click detail) ----
  function edgeData(id) {
    return rawEdges.value.find(e => e.id === id) || null
  }
  function nodeName(id) {
    const n = rawNodes.value.find(x => x.id === id)
    return n ? n.name : id
  }

  graph.on('edge:pointerenter', (event) => {
    const id = event.target.id
    if (!id) return
    try {
      graph.setElementState(id, 'active', true)
    } catch (e) { /* ignore */ }
  })

  graph.on('edge:pointerleave', (event) => {
    const id = event.target.id
    if (!id) return
    try {
      graph.setElementState(id, 'active', false)
    } catch (e) { /* ignore */ }
  })

  graph.on('edge:click', (event) => {
    const id = event.target.id
    if (!id) return
    const e = edgeData(id)
    if (!e) return
    const rect = containerRef.value?.getBoundingClientRect()
    edgeInfo.sourceName = nodeName(e.source)
    edgeInfo.targetName = nodeName(e.target)
    edgeInfo.taskName = e.taskName || ''
    edgeInfo.sql = e.sql || ''
    edgeInfo.fieldLinks = e.fieldLinks || []
    edgeInfo.relationLabel = edgeRelationLabel(e.relationType)
    edgeInfo.tagType = edgeTagType(e.relationType)
    edgeInfo.x = Math.min((event.client?.x ?? 0) - rect.left + 12, (rect?.width || 400) - 340)
    edgeInfo.y = Math.min((event.client?.y ?? 0) - rect.top + 12, (rect?.height || 300) - 260)
    edgeInfo.visible = true
  })

  containerRef.value?.addEventListener('pointerdown', (e) => {
    if (e.button === 0 && !(e.target.closest && e.target.closest('.edge-info'))) edgeInfo.visible = false
  })

  containerRef.value?.addEventListener('pointerdown', (e) => {
    if (e.button === 0 && !(e.target.closest && e.target.closest('.ctx-menu'))) contextMenu.visible = false
  })

  graph.on('node:pointerenter', (event) => {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    if (!nodeData) return
    const d = nodeData.data || {}
    if (d.nodeType === 'AGG') return
    const rels = new Set()
    let sql = ''
    for (const e of rawEdges.value) {
      if (e.source === id || e.target === id) {
        if (e.taskName) rels.add(e.taskName)
        if (!sql && e.sql) sql = e.sql
      }
    }
    tooltip.name = d.name || id
    tooltip.typeTag = typeTag(d)
    tooltip.datasourceName = d.datasourceName || ''
    tooltip.relations = [...rels].slice(0, 3)
    tooltip.sql = sql.length > 90 ? sql.slice(0, 90) + '…' : sql
    tooltip.layer = d.layer ?? 0
    tooltip.deg = degreeMap()[id] || 0
    tooltip.rowCount = d.rowCount != null ? d.rowCount : null
    tooltip.columnCount = d.columnCount != null ? d.columnCount : null
    tooltip.category = d.category || ''
    tooltip.visible = true
    placeTooltip(event)
  })

  graph.on('node:pointermove', placeTooltip)

  graph.on('node:pointerleave', () => {
    tooltip.visible = false
  })

  containerRef.value?.addEventListener('pointerleave', () => {
    tooltip.visible = false
  })

  function placeTooltip(event) {
    if (!tooltip.visible) return
    const rect = containerRef.value?.getBoundingClientRect()
    if (!rect) return
    const x = event.client.x - rect.left + 14
    const y = event.client.y - rect.top + 14
    tooltip.x = Math.min(x, rect.width - 300)
    tooltip.y = Math.min(y, rect.height - 180)
  }

  watch(() => props.data, (d) => {
    if (!d) return
    rawNodes.value = d.nodes || []
    rawEdges.value = (d.links || []).map((l, i) => ({ id: l.id || 'e' + i, source: l.source, target: l.target, relationType: l.relationType, taskName: l.taskName, sql: l.sql, transform: l.transform, fieldLinks: l.fieldLinks || [] }))
    expandedLayers.value = new Set()
    render()
  }, { deep: false })

  watch(() => props.layout, () => {
    graph.setLayout(layoutOption())
    render()
  })

  if (props.data) {
    rawNodes.value = props.data.nodes || []
    rawEdges.value = (props.data.links || []).map((l, i) => ({ id: l.id || 'e' + i, source: l.source, target: l.target, relationType: l.relationType, taskName: l.taskName, sql: l.sql, transform: l.transform, fieldLinks: l.fieldLinks || [] }))
    render()
  }

  startFlowAnimation()
  startMinimapTicker()
})

onBeforeUnmount(() => {
  stopFlowAnimation()
  stopMinimapTicker()
  const graph = graphRef.value
  graphRef.value = null
  if (graph) {
    try { graph.stopLayout() } catch (e) { /* ignore */ }
    graph.destroy()
  }
})

defineExpose({
  getRenderedNodeCount() {
    const graph = graphRef.value
    if (!graph) return 0
    try { return graph.getNodeData().length } catch (e) { return 0 }
  },
  getRenderedEdgeCount() {
    const graph = graphRef.value
    if (!graph) return 0
    try { return graph.getEdgeData().length } catch (e) { return 0 }
  },
  async exportPng() {
    const graph = graphRef.value
    if (!graph) return null
    try {
      const layers = Array.from(containerRef.value?.querySelectorAll('canvas') || [])
        .filter(c => !(c.parentElement && c.parentElement.className && String(c.parentElement.className).includes('minimap')))
      if (!layers.length) return null
      const w = layers[0].width
      const h = layers[0].height
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#0b1120'
      ctx.fillRect(0, 0, w, h)
      for (const layer of layers) {
        ctx.drawImage(layer, 0, 0, w, h)
      }
      return canvas.toDataURL('image/png')
    } catch (e) {
      try {
        return await graph.toDataURL()
      } catch (e2) {
        return null
      }
    }
  }
})
</script>

<style scoped>
.lineage-g6-container {
  width: 100%;
  height: 100%;
  background:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    radial-gradient(ellipse at 30% 20%, #1e293b 0%, #0b1120 60%, #0b1120 100%);
  background-size: 24px 24px, 24px 24px, 100% 100%;
  position: relative;
  overflow: hidden;
}

.canvas-actions {
  position: absolute;
  bottom: 16px;
  left: 16px;
  z-index: 12;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 4px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}
.ca-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #e2e8f0;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: Menlo, Consolas, monospace;
}
.ca-btn:hover {
  background: #334155;
  color: #f8fafc;
}
.ca-btn.active {
  background: #334155;
  color: #38bdf8;
}
.ca-sep {
  width: 1px;
  height: 18px;
  background: #334155;
  margin: 0 2px;
}

.ctx-menu {
  position: absolute;
  z-index: 30;
  min-width: 140px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 6px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.5);
  padding: 4px;
  font-size: 12px;
  color: #e2e8f0;
}
.ctx-item {
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}
.ctx-item:hover {
  background: #1e293b;
  color: #f8fafc;
}

.lineage-tooltip {
  position: absolute;
  z-index: 20;
  pointer-events: none;
  width: 280px;
  background: #0f172a;
  border: 1px solid #334155;
  border-left: 3px solid #38bdf8;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.55);
  padding: 10px 12px;
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.55;
  color: #e2e8f0;
}

.lt-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.lt-type {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #0b1120;
  background: #38bdf8;
  border-radius: 3px;
  padding: 1px 5px;
}

.lt-name {
  font-weight: 700;
  color: #f8fafc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lt-row {
  display: flex;
  gap: 8px;
  margin-top: 3px;
}

.lt-k {
  flex-shrink: 0;
  color: #cbd5e1;
}

.lt-v {
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lt-sql {
  white-space: normal;
  word-break: break-all;
  max-height: 56px;
  overflow: hidden;
}

.lt-divider {
  height: 1px;
  background: #1e293b;
  margin: 6px 0;
}

/* ---- 小地图 ---- */
.lineage-minimap {
  position: absolute;
  bottom: 16px;
  right: 16px;
  z-index: 12;
  border: 1px solid #334155;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  cursor: crosshair;
  background: #0f172a;
}
.lineage-minimap canvas {
  display: block;
}

/* ---- 图例 ---- */
.legend-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 12;
  min-width: 190px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 10px 12px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: #e2e8f0;
}
.legend-title {
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 8px;
  font-size: 12px;
}
.legend-section {
  margin-bottom: 8px;
}
.legend-section:last-child {
  margin-bottom: 0;
}
.legend-sub {
  color: #cbd5e1;
  font-size: 10px;
  margin-bottom: 4px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.legend-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}
.legend-line {
  width: 22px;
  border-top-width: 2px;
  border-top-style: solid;
  flex-shrink: 0;
}
.legend-label {
  color: #e2e8f0;
}

/* ---- 边详情浮层 ---- */
.edge-info {
  position: absolute;
  z-index: 25;
  width: 320px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.55);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: #e2e8f0;
  overflow: hidden;
}
.edge-info-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}
.edge-info-close {
  border: none;
  background: transparent;
  color: #cbd5e1;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
}
.edge-info-close:hover {
  color: #f8fafc;
}
.edge-info-body {
  padding: 10px 12px;
}
.edge-info-row {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.ei-k {
  flex-shrink: 0;
  color: #cbd5e1;
  min-width: 32px;
}
.ei-v {
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ei-sql {
  white-space: normal;
  word-break: break-all;
  max-height: 60px;
  overflow: hidden;
}
.edge-info-fields {
  margin-top: 8px;
  border-top: 1px solid #1e293b;
  padding-top: 6px;
}
.edge-info-sub {
  color: #cbd5e1;
  margin-bottom: 4px;
}
.edge-info-field {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 10px;
}
.eif-s {
  color: #10b981;
}
.eif-t {
  color: #f59e0b;
}
</style>