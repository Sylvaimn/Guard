<template>
  <div class="eg-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Share /></el-icon>实体关联图谱</h2>
      <p>一查多：以归一化实体为中心的关联关系图（同案 / 同源 / 使用）</p>
    </div>

    <div class="toolbar">
      <el-select
        v-model="selectedEntityId"
        filterable
        remote
        :remote-method="searchEntities"
        :loading="searching"
        placeholder="搜索实体（姓名 / 身份证 / 车牌...）"
        style="width: 320px"
        clearable
      >
        <el-option v-for="e in entityOptions" :key="e.id" :label="`${e.displayName} (${e.primaryKey})`" :value="e.id" />
      </el-select>
      <el-select v-model="degree" size="small" style="width: 110px">
        <el-option v-for="d in [1, 2, 3]" :key="d" :label="`${d} 度关联`" :value="d" />
      </el-select>
      <el-select v-model="edgeType" size="small" clearable placeholder="边类型" style="width: 130px">
        <el-option label="同案" value="SAME_CASE" />
        <el-option label="同源记录" value="SHARED_SOURCE" />
        <el-option label="使用" value="USES" />
      </el-select>
      <el-button type="primary" :icon="Search" :disabled="!selectedEntityId" :loading="loading" @click="loadGraph">
        分析关联
      </el-button>
      <div v-if="graph" class="graph-meta">
        <el-tag size="small">节点 {{ graph.nodes?.length || 0 }}</el-tag>
        <el-tag size="small">关联 {{ graph.edges?.length || 0 }}</el-tag>
        <el-tag v-if="graph.trimmed" size="small" type="danger">已裁剪</el-tag>
      </div>
    </div>

    <div class="graph-container" ref="graphRef">
      <div v-if="!graph && !loading" class="empty-state">
        <el-icon class="empty-icon"><Share /></el-icon>
        <p>搜索并选择实体，点击"分析关联"查看关系图</p>
      </div>
      <div v-if="loading" class="loading-state">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <p>正在分析关联关系...</p>
      </div>
      <div v-if="graph && !loading" class="graph-stats">
        <span>根实体：<strong>{{ graph.root?.name }}</strong></span>
        <el-tag size="small" type="info">{{ graph.root?.entityType }}</el-tag>
        <el-tag size="small" :type="graph.trimmed ? 'danger' : 'success'">{{ graph.trimmed ? '已裁剪' : '完整子图' }}</el-tag>
      </div>

      <!-- 图例 -->
      <div class="eg-legend">
        <div class="eg-legend-title">图例</div>
        <div class="eg-legend-item" v-for="t in legendTypes" :key="t.type">
          <span class="eg-swatch" :style="{ background: t.color }"></span>
          <span>{{ t.label }}</span>
        </div>
        <div class="eg-legend-divider"></div>
        <div class="eg-legend-item" v-for="e in legendEdges" :key="e.type">
          <span class="eg-line" :style="{ borderColor: e.color }"></span>
          <span>{{ e.label }}</span>
        </div>
      </div>

      <!-- 节点详情面板 -->
      <div v-if="selectedNode" class="eg-node-panel">
        <div class="eg-panel-head">
          <el-tag :type="typeTag(selectedNode.entityType)" size="small">{{ typeLabel(selectedNode.entityType) }}</el-tag>
          <span class="eg-panel-name">{{ selectedNode.name }}</span>
          <button class="eg-panel-close" @click="selectedNode = null">×</button>
        </div>
        <div class="eg-panel-body">
          <div class="eg-row"><span class="eg-k">主标识</span><span class="eg-v masked">{{ selectedNode.primaryKey || '-' }}</span></div>
          <div class="eg-row"><span class="eg-k">密级</span><span class="eg-v">{{ densityLabel(selectedNode.density) }}</span></div>
          <div class="eg-row"><span class="eg-k">关联度</span><span class="eg-v">{{ selectedNode.degree || 0 }}</span></div>
          <div v-if="selectedNode.labels?.length" class="eg-labels">
            <el-tag v-for="l in selectedNode.labels" :key="l" size="small" effect="light" style="margin-right: 4px">{{ l }}</el-tag>
          </div>
        </div>
        <div class="eg-panel-actions">
          <el-button size="small" type="primary" @click="refocus(selectedNode)">以此为中心</el-button>
          <el-button size="small" @click="selectedNode = null">关闭</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Share, Loading } from '@element-plus/icons-vue'
import { Graph } from '@antv/g6'
import { entityMasterApi } from '@/api/modules/entitymaster'
import { entityGraphApi } from '@/api/modules/entitygraph'
import { ElMessage } from 'element-plus'

const graphRef = ref(null)
const selectedEntityId = ref('')
const entityOptions = ref([])
const route = useRoute()
const searching = ref(false)
const loading = ref(false)
const degree = ref(1)
const edgeType = ref('')
const graph = ref(null)
const selectedNode = ref(null)
let g6Graph = null

const legendTypes = [
  { type: 'PERSON', color: '#3b82f6', label: '人员' },
  { type: 'VEHICLE', color: '#f59e0b', label: '车辆' },
  { type: 'CASE', color: '#ef4444', label: '案件' },
  { type: 'ITEM', color: '#8b5cf6', label: '物品' },
  { type: 'ORG', color: '#10b981', label: '组织' }
]
const legendEdges = [
  { type: 'SAME_CASE', color: '#ef4444', label: '同案' },
  { type: 'SHARED_SOURCE', color: '#10b981', label: '同源记录' },
  { type: 'USES', color: '#f59e0b', label: '使用' },
  { type: 'CONTACT', color: '#3b82f6', label: '关联' }
]

function densityLabel(d) {
  return { PUBLIC: '公开', INTERNAL: '内部', CONFIDENTIAL: '秘密', SECRET: '机密' }[d] || d
}

function typeColor(type) {
  return { PERSON: '#3b82f6', VEHICLE: '#f59e0b', CASE: '#ef4444', ITEM: '#8b5cf6', ORG: '#10b981' }[type] || '#64748b'
}
function typeLabel(t) {
  return { PERSON: '人员', VEHICLE: '车辆', CASE: '案件', ITEM: '物品', ORG: '组织' }[t] || t
}
function edgeColor(t) {
  return { SAME_CASE: '#ef4444', SHARED_SOURCE: '#10b981', USES: '#f59e0b', CONTACT: '#3b82f6' }[t] || '#94a3b8'
}

async function searchEntities(kw) {
  if (!kw || kw.length < 1) { entityOptions.value = []; return }
  searching.value = true
  try {
    entityOptions.value = await entityMasterApi.list({ entityType: '' })
    entityOptions.value = entityOptions.value.filter(e =>
      (e.displayName || '').includes(kw) || (e.primaryKey || '').includes(kw)
    ).slice(0, 20)
  } catch (e) {
    entityOptions.value = []
  } finally {
    searching.value = false
  }
}

function buildGraphData(g) {
  const degreeMap = {}
  for (const e of g.edges || []) {
    degreeMap[e.source] = (degreeMap[e.source] || 0) + 1
    degreeMap[e.target] = (degreeMap[e.target] || 0) + 1
  }
  return {
    nodes: (g.nodes || []).map(n => {
      const color = typeColor(n.entityType)
      return {
        id: n.id,
        data: n,
        style: {
          size: [Math.max(90, (n.name || '').length * 9 + 40), 40],
          shape: 'rect',
          radius: 6,
          fill: color,
          stroke: '#ffffff',
          lineWidth: 1.5,
          labelText: `${typeLabel(n.entityType)} · ${n.name}`,
          labelFill: '#fff',
          labelFontSize: 11,
          labelFontWeight: 600,
          labelPosition: 'center',
          labelPlacement: 'center',
          shadowColor: color,
          shadowBlur: 10
        }
      }
    }),
    edges: (g.edges || []).map((e, i) => ({
      id: 'e' + i,
      source: e.source,
      target: e.target,
      data: e,
      style: {
        stroke: edgeColor(e.relationType),
        lineWidth: 2.5,
        lineCap: 'round',
        endArrow: true,
        labelText: e.label,
        labelFill: '#cbd5e1',
        labelFontSize: 10,
        labelBackground: true,
        labelBackgroundFill: '#1e293b',
        labelBackgroundRadius: 3
      }
    }))
  }
}

async function loadGraph() {
  if (!selectedEntityId.value) return
  loading.value = true
  try {
    const params = { degree: degree.value }
    if (edgeType.value) params.edgeType = edgeType.value
    const res = await entityGraphApi.graph(selectedEntityId.value, params)
    graph.value = res
    if (g6Graph) {
      g6Graph.setData(buildGraphData(res))
      g6Graph.render()
    }
  } catch (e) {
    ElMessage.error(e.message || '分析失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  g6Graph = new Graph({
    container: graphRef.value,
    autoFit: 'view',
    data: { nodes: [], edges: [] },
    layout: { type: 'force', linkDistance: 160, nodeStrength: -150 },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element', 'click-select', 'hover-activate']
  })

  g6Graph.on('node:click', (event) => {
    const id = event.target.id
    if (!id) return
    const nodeData = g6Graph.getNodeData(id)
    selectedNode.value = nodeData?.data || null
  })

  g6Graph.render()

  // 支持从情报检索/布控预警跳转直达（query 参数）
  if (route.query.entityId) {
    selectedEntityId.value = String(route.query.entityId)
    degree.value = 2
    await loadGraph()
  }
})

async function refocus(node) {
  selectedEntityId.value = node.id
  degree.value = 2
  await loadGraph()
  ElMessage.success(`已切换为中心：${node.name}`)
}

onBeforeUnmount(() => {
  g6Graph?.destroy()
  g6Graph = null
})
</script>

<style scoped>
.eg-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.page-header h2 {
  font-size: 19px;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.page-header p {
  margin: 0;
  color: #9ca3af;
  font-size: 13px;
}
.header-icon {
  color: #6366f1;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.graph-meta {
  display: flex;
  gap: 6px;
  margin-left: auto;
}
.graph-container {
  flex: 1;
  background: #0f172a;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
  min-height: 400px;
  background:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px);
  background-size: 24px 24px;
}
.empty-state, .loading-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}
.empty-icon, .loading-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.graph-stats {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e5e7eb;
  font-size: 13px;
  z-index: 10;
  pointer-events: none;
}

/* 图例 */
.eg-legend {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 11;
  min-width: 140px;
  background: rgba(15, 23, 42, 0.92);
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 11px;
  color: #cbd5e1;
}
.eg-legend-title {
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 6px;
}
.eg-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.eg-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}
.eg-line {
  width: 22px;
  border-top: 2px solid;
}
.eg-legend-divider {
  height: 1px;
  background: #334155;
  margin: 6px 0;
}

/* 节点详情面板 */
.eg-node-panel {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 12;
  width: 280px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.55);
  overflow: hidden;
}
.eg-panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}
.eg-panel-name {
  flex: 1;
  font-weight: 600;
  color: #f8fafc;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.eg-panel-close {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 16px;
  cursor: pointer;
}
.eg-panel-close:hover {
  color: #f8fafc;
}
.eg-panel-body {
  padding: 10px 12px;
  font-size: 12px;
}
.eg-row {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.eg-k {
  flex-shrink: 0;
  color: #64748b;
  min-width: 44px;
}
.eg-v {
  color: #e2e8f0;
}
.eg-v.masked {
  font-family: Menlo, Consolas, monospace;
}
.eg-labels {
  margin-top: 8px;
}
.eg-panel-actions {
  display: flex;
  gap: 8px;
  padding: 8px 12px 12px;
}
</style>
