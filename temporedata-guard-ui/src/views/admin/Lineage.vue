<template>
  <div class="lineage-page">
    <!-- Toolbar -->
    <div class="lineage-toolbar">
      <h2 class="page-title">数据血缘分析</h2>
      <div class="toolbar-center">
        <el-select
          v-model="store.searchKeyword"
          filterable
          remote
          :remote-method="handleSearch"
          :loading="searching"
          placeholder="搜索表 / 字段 / 任务..."
          clearable
          size="default"
          style="width: 300px"
          @change="selectNode"
        >
          <el-option
            v-for="t in searchResults"
            :key="t.id"
            :label="t.name"
            :value="t.id"
          >
            <div class="search-option">
              <el-tag :type="nodeTypeTag(t.nodeType)" size="small">{{ t.nodeType }}</el-tag>
              <span>{{ t.name }}</span>
              <span class="search-degree">↓{{ t.inDegree }} ↑{{ t.outDegree }}</span>
            </div>
          </el-option>
        </el-select>
        <el-radio-group v-model="store.graphLevel" size="small">
          <el-radio-button value="table">表级</el-radio-button>
          <el-radio-button value="column">字段级</el-radio-button>
        </el-radio-group>
        <el-select v-model="store.graphDirection" size="small" style="width: 110px">
          <el-option label="双向" value="both" />
          <el-option label="仅上游" value="up" />
          <el-option label="仅下游" value="down" />
        </el-select>
        <el-select v-model="store.graphDepth" size="small" style="width: 90px">
          <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="`深度 ${d}`" :value="d" />
        </el-select>
        <el-select v-model="store.nodeTypeFilter" size="small" clearable placeholder="节点类型" style="width: 120px">
          <el-option label="表" value="TABLE" />
          <el-option label="字段" value="COLUMN" />
          <el-option label="任务" value="TASK" />
          <el-option label="接口" value="API" />
          <el-option label="文件" value="FILE" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="store.loadGraph()" :loading="store.graphLoading" :disabled="!store.rootNodeId">
          分析血缘
        </el-button>
        <el-button :icon="WarningFilled" @click="openImpact" :loading="impactLoading" :disabled="!store.rootNodeId">
          影响分析
        </el-button>
        <el-button :icon="Share" :type="store.isOverview ? 'primary' : ''" @click="loadOverview" :disabled="store.isOverview">
          血缘总览
        </el-button>
        <el-radio-group v-model="store.layoutMode" size="small">
          <el-radio-button value="force">力导向</el-radio-button>
          <el-radio-button value="layer">分层</el-radio-button>
          <el-radio-button value="radial">径向</el-radio-button>
        </el-radio-group>
        <el-button :icon="DataAnalysis" @click="runGraphAnalysis" :disabled="!store.graphData">环检测</el-button>
        <el-tag v-if="wsConnected" size="small" type="success">实时连接</el-tag>
        <el-tag v-else size="small" type="info">离线</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-divider direction="vertical" />
        <el-button :icon="Guide" @click="openPathDialog" :disabled="!store.graphData">路径分析</el-button>
        <el-button :icon="Odometer" @click="loadHeat">热度排行</el-button>
        <el-button :icon="Document" @click="openSqlDialog">SQL 解析</el-button>
        <el-button :icon="Download" @click="exportPng" :disabled="!store.graphData">导出 PNG</el-button>
        <el-button :icon="RefreshRight" @click="rebuildGraph">重建图数据</el-button>
      </div>
    </div>

    <div class="lineage-body">
      <!-- Graph Canvas (G6) -->
      <div class="graph-container" ref="graphRef">
        <div v-if="!store.graphData && !store.graphLoading" class="empty-state">
          <el-icon class="empty-icon"><Share /></el-icon>
          <p>搜索并选择表 / 字段 / 任务，点击"分析血缘"查看图谱</p>
        </div>
        <div v-if="store.graphLoading" class="loading-state">
          <el-icon class="loading-icon is-loading"><Loading /></el-icon>
          <p>正在分析血缘关系...</p>
        </div>
        <div v-if="store.graphData" class="graph-stats">
          <span>根节点：<strong>{{ store.graphData.rootTableName }}</strong></span>
          <el-tag v-if="!store.isOverview" size="small" type="success">上游 {{ store.graphData.totalUpstream }}</el-tag>
          <el-tag v-if="!store.isOverview" size="small" type="warning">下游 {{ store.graphData.totalDownstream }}</el-tag>
          <el-tag size="small" type="info">最大深度 {{ store.graphData.maxDepth }}</el-tag>
          <el-tag size="small">节点 {{ store.graphData.nodes?.length || 0 }}</el-tag>
          <el-tag v-if="(store.graphData.nodes || []).filter(n => n.nodeType === 'API').length" size="small" type="danger" effect="light">
            接口出口 {{ store.graphData.nodes.filter(n => n.nodeType === 'API').length }}
          </el-tag>
          <el-tag size="small">连线 {{ store.graphData.links?.length || 0 }}</el-tag>
          <el-tag v-if="store.graphData.nodes?.length > 50" size="small" type="danger" effect="light">
            大图已聚合（{{ store.graphData.nodes.length }} → {{ aggregatedRenderCount }}）
          </el-tag>
        </div>
        <LineageGraph
          v-if="store.graphData"
          :data="store.graphData"
          :layout="store.layoutMode"
          :cycle-edge-keys="cycleEdgeKeys"
          @node-click="onNodeClick"
          @node-dblclick="onNodeRefocus"
          ref="g6Ref"
        />
      </div>

      <!-- Detail Panel -->
      <div v-if="store.selectedNode" class="detail-panel">
        <h3>
          <el-tag :type="nodeTypeTag(store.selectedNode.nodeType)" size="small">{{ store.selectedNode.nodeType }}</el-tag>
          {{ store.selectedNode.name }}
        </h3>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="类型">{{ nodeTypeLabel(store.selectedNode.nodeType) }}</el-descriptions-item>
          <el-descriptions-item label="数据源/库">{{ store.selectedNode.datasourceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag :type="categoryTag(store.selectedNode.category)" size="small">{{ categoryLabel(store.selectedNode.category) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="距根深度">{{ store.selectedNode.depth }}</el-descriptions-item>
          <el-descriptions-item label="层级">{{ store.selectedNode.layer }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button type="primary" size="small" @click="store.refocus(store.selectedNode.id)">以此节点为中心</el-button>
          <el-button size="small" @click="store.clearSelection()">关闭</el-button>
        </div>
        <!-- Field Lineage -->
        <div v-if="fieldLinks.length" class="field-lineage">
          <h4>字段级血缘</h4>
          <div v-for="(fl, i) in fieldLinks" :key="i" class="field-link-item">
            <span class="fl-source">{{ fl.sourceColumn }}</span>
            <el-icon class="fl-arrow"><ArrowRight /></el-icon>
            <span class="fl-target">{{ fl.targetColumn }}</span>
            <el-tag v-if="fl.transformationLogic" size="small" type="info" class="fl-logic">{{ fl.transformationLogic }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- Path Analysis Dialog -->
    <el-dialog v-model="pathDialogVisible" title="路径分析" width="620px" destroy-on-close>
      <div class="path-dialog-body">
        <div class="path-selects">
          <div class="path-field">
            <span class="path-label">起点</span>
            <el-select v-model="pathSource" filterable remote :remote-method="(k) => searchFor(k, 'pathSource')"
                       :loading="searching" placeholder="搜索起点节点" style="width: 100%">
              <el-option v-for="t in pathSourceResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
            </el-select>
          </div>
          <div class="path-field">
            <span class="path-label">终点</span>
            <el-select v-model="pathTarget" filterable remote :remote-method="(k) => searchFor(k, 'pathTarget')"
                       :loading="searching" placeholder="搜索终点节点" style="width: 100%">
              <el-option v-for="t in pathTargetResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
            </el-select>
          </div>
        </div>
        <div class="path-dialog-actions">
          <el-button type="primary" :loading="pathLoading" @click="runPathAnalysis">查找最短路径</el-button>
        </div>
        <div v-if="pathResult" class="path-result">
          <template v-if="pathResult.found">
            <el-alert type="success" :closable="false" :title="`找到路径（${pathResult.path.length} 个节点）`" />
            <div class="path-chain">
              <template v-for="(id, i) in pathResult.path" :key="i">
                <span v-if="i > 0" class="chain-arrow">→</span>
                <el-tag :type="i === 0 ? 'primary' : i === pathResult.path.length - 1 ? 'danger' : ''" size="small">
                  {{ pathName(id) }}
                </el-tag>
              </template>
            </div>
            <div v-if="pathResult.hops.length" class="path-hops">
              <h4>路径详情</h4>
              <el-table :data="pathResult.hops" size="small" border>
                <el-table-column label="起点" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ pathName(row.source) }}</template>
                </el-table-column>
                <el-table-column label="终点" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ pathName(row.target) }}</template>
                </el-table-column>
                <el-table-column prop="edgeType" label="边类型" width="110" />
                <el-table-column prop="taskName" label="任务" min-width="140" show-overflow-tooltip />
              </el-table>
            </div>
          </template>
          <el-alert v-else type="warning" :closable="false" title="未找到连通路径（两者之间不存在血缘链路）" />
        </div>
      </div>
    </el-dialog>

    <!-- Heat Dialog -->
    <el-dialog v-model="heatDialogVisible" title="热度排行（出入度 TOP20）" width="560px" destroy-on-close>
      <el-table :data="heatData" size="small" border max-height="480" v-loading="heatLoading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="nodeType" label="类型" width="90">
          <template #default="{ row }"><el-tag :type="nodeTypeTag(row.nodeType)" size="small">{{ row.nodeType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="name" label="节点" min-width="160" show-overflow-tooltip />
        <el-table-column prop="inDegree" label="入度" width="70" align="center" />
        <el-table-column prop="outDegree" label="出度" width="70" align="center" />
        <el-table-column prop="total" label="热度" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" type="danger">{{ row.total }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- SQL Analysis Dialog -->
    <el-dialog v-model="impactVisible" title="影响分析" width="760px" destroy-on-close>
      <div v-if="impactData" class="impact-summary">
        <span>根节点：<strong>{{ impactData.tableName || store.rootNode?.name }}</strong></span>
        <el-tag type="danger" effect="light" style="margin-left: 8px">受影响 {{ impactData.totalImpacted }} 个下游</el-tag>
        <el-tag style="margin-left: 8px">最大深度 {{ impactData.maxDepth }}</el-tag>
        <span class="impact-hint">修改该表/节点，将影响以下下游对象（含接口出口）</span>
      </div>
      <el-table :data="impactData?.paths || []" v-loading="impactLoading" stripe max-height="420" empty-text="无受影响下游">
        <el-table-column label="受影响对象" min-width="160">
          <template #default="{ row }">
            <span>{{ row.tableName }}</span>
            <el-tag v-if="row.relationType === 'API'" size="small" type="danger" effect="light" style="margin-left: 6px">接口</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="datasourceName" label="数据源" width="130" />
        <el-table-column label="深度" width="70" align="center">
          <template #default="{ row }">{{ row.depth }}</template>
        </el-table-column>
        <el-table-column label="关联" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ impactRelationLabel(row.relationType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="链路" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ (row.pathChain || []).join(' → ') }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="sqlDialogVisible" title="SQL 血缘解析" width="680px" destroy-on-close>
      <div class="sql-dialog-body">
        <p class="sql-dialog-hint">输入 INSERT...SELECT / CREATE TABLE AS SELECT，解析表间血缘与字段映射</p>
        <el-input v-model="sqlText" type="textarea" :rows="6" placeholder="例如：INSERT INTO dws_sales_daily (sale_date, total_amount) SELECT sale_date, SUM(amount) FROM dwd_sales_detail GROUP BY sale_date" />
        <div class="sql-dialog-actions">
          <el-button type="primary" :loading="sqlAnalyzing" @click="runSqlAnalysis">解析血缘</el-button>
        </div>
        <div v-if="sqlResult" class="sql-result">
          <div class="sql-result-head">
            <el-tag type="primary">{{ sqlResult.sqlType }}</el-tag>
            <span class="sql-result-arrow">{{ (sqlResult.inputTables || []).join(', ') || '-' }} → {{ sqlResult.outputTable || '(查询)' }}</span>
          </div>
          <template v-if="sqlResult.fieldMappings && sqlResult.fieldMappings.length">
            <h4>字段级血缘</h4>
            <el-table :data="sqlResult.fieldMappings" size="small" border max-height="220">
              <el-table-column prop="sourceTable" label="来源表" width="150" show-overflow-tooltip />
              <el-table-column prop="sourceColumn" label="来源列" width="130" show-overflow-tooltip />
              <el-table-column prop="targetColumn" label="目标列" width="130" show-overflow-tooltip />
              <el-table-column prop="transformation" label="转换逻辑" min-width="160" show-overflow-tooltip />
            </el-table>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {Search, Share, Loading, ArrowRight, DataAnalysis, Guide, Odometer, Document, Download, RefreshRight, WarningFilled } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useLineageStore } from '@/stores/lineage'
import { lineageApi } from '@/api/modules/lineage'
import { metaApi } from '@/api/modules/meta'
import LineageGraph from '@/components/lineage/LineageGraph.vue'

const store = useLineageStore()
const route = useRoute()
const { graphData, graphLoading, selectedNode, layoutMode, isOverview, nodeNameMap, fieldLinks } = storeToRefs(store)

const aggregatedRenderCount = computed(() => {
  const nodes = store.graphData?.nodes || []
  if (nodes.length <= 50) return nodes.length
  // 与 LineageGraph 聚合逻辑一致：仅统计 L0 与每层聚合节点
  const byLayer = {}
  for (const n of nodes) {
    const layer = n.layer ?? 0
    ;(byLayer[layer] = byLayer[layer] || []).push(n)
  }
  let count = 0
  for (const [layer, list] of Object.entries(byLayer)) {
    if (Number(layer) === 0 || list.length <= 3) count += list.length
    else count += 1
  }
  return count
})

// ---- Local state ----
const searching = ref(false)
const searchResults = ref([])
const g6Ref = ref(null)
const cycleEdgeKeys = ref([])

// Path analysis
const pathDialogVisible = ref(false)
const pathSource = ref('')
const pathTarget = ref('')
const pathSourceResults = ref([])
const pathTargetResults = ref([])
const pathLoading = ref(false)
const pathResult = ref(null)

// Heat
const heatDialogVisible = ref(false)
const heatLoading = ref(false)
const heatData = ref([])

// SQL analysis
const sqlDialogVisible = ref(false)
const impactVisible = ref(false)
const impactLoading = ref(false)
const impactData = ref(null)

function impactRelationLabel(t) {
  const map = { CONSUMES: '消费', PRODUCES: '产出', FLOWS_TO: '流向', API: '接口消费', QUALITY: '质量校验', SYNC: '同步' }
  return map[t] || t || '-'
}

async function openImpact() {
  if (!store.rootNodeId) return
  impactVisible.value = true
  impactLoading.value = true
  impactData.value = null
  try {
    const data = await metaApi.getImpactAnalysis(store.rootNodeId, 5)
    impactData.value = data
  } catch (e) {
    ElMessage.error(e.message || '影响分析失败')
  } finally {
    impactLoading.value = false
  }
}
const sqlText = ref('')
const sqlAnalyzing = ref(false)
const sqlResult = ref(null)

// WebSocket
const wsConnected = ref(false)
let ws = null

// ---- Watchers ----
watch(() => [store.graphLevel, store.graphDirection, store.graphDepth, store.nodeTypeFilter], () => {
  if (store.rootNodeId && !store.graphLoading) store.loadGraph()
})

// ---- Helpers ----
function nodeTypeTag(t) {
  const map = { TABLE: 'primary', COLUMN: 'success', TASK: 'warning', API: 'danger', REPORT: 'info' }
  return map[t] || 'info'
}
function nodeTypeLabel(t) {
  const map = { TABLE: '数据表', COLUMN: '字段', TASK: '任务', API: '接口', REPORT: '报表' }
  return map[t] || t || '节点'
}
function categoryTag(c) {
  return c === 'root' ? '' : c === 'upstream' ? 'success' : c === 'downstream' ? 'warning' : 'info'
}
function categoryLabel(c) {
  return c === 'root' ? '根节点' : c === 'upstream' ? '上游' : c === 'downstream' ? '下游' : '外部'
}
function pathName(id) {
  return nodeNameMap.value[id] || id
}

// ---- Search ----
async function handleSearch(kw) {
  if (!kw || kw.length < 1) { searchResults.value = []; return }
  searching.value = true
  try {
    searchResults.value = await lineageApi.search(kw)
  } catch (e) {
    searchResults.value = []
  } finally { searching.value = false }
}

async function searchFor(kw, target) {
  if (!kw || kw.length < 1) { target === 'pathSource' ? pathSourceResults.value = [] : pathTargetResults.value = []; return }
  searching.value = true
  try {
    const res = await lineageApi.search(kw)
    if (target === 'pathSource') pathSourceResults.value = res
    else pathTargetResults.value = res
  } catch { /* ignore */ } finally { searching.value = false }
}

function selectNode(id) {
  store.selectNode(id)
}

// ---- Graph actions ----
async function loadOverview() {
  await store.loadOverview()
}

function onNodeClick(node) {
  store.selectedNode = node
}

function onNodeRefocus(node) {
  store.refocus(node.id)
  ElMessage.success(`已聚焦节点：${node.name}`)
}

async function exportPng() {
  const dataUrl = await g6Ref.value?.exportPng()
  if (!dataUrl) {
    ElMessage.warning('导出失败')
    return
  }
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = `lineage_${store.graphData?.rootTableName || 'graph'}.png`
  a.click()
}

async function runGraphAnalysis() {
  if (!store.graphData) return
  try {
    const res = await lineageApi.findCycles()
    // highlight cycle edges on the canvas (red)
    const keys = new Set()
    for (const cycle of res.cycles || []) {
      for (let i = 0; i < cycle.length - 1; i++) {
        const srcName = cycle[i]
        const tgtName = cycle[i + 1]
        // resolve names back to ids via name map
        const srcId = Object.keys(nodeNameMap.value).find(k => nodeNameMap.value[k] === srcName)
        const tgtId = Object.keys(nodeNameMap.value).find(k => nodeNameMap.value[k] === tgtName)
        if (srcId && tgtId) keys.add(`${srcId}|${tgtId}`)
      }
    }
    cycleEdgeKeys.value = [...keys]
    ElMessageBox.alert(
      res.summary + (res.cycles.length ? '\n环：' + res.cycles.map(c => c.join(' → ')).join('\n') : ''),
      '环检测结果',
      { confirmButtonText: '知道了', type: res.hasCycle ? 'warning' : 'success' }
    )
    if (!res.hasCycle) cycleEdgeKeys.value = []
  } catch (e) {
    ElMessage.error('环检测失败: ' + (e.message || '未知错误'))
  }
}

// ---- Path analysis ----
function openPathDialog() {
  pathSource.value = store.rootNodeId || ''
  pathTarget.value = ''
  pathResult.value = null
  pathSourceResults.value = []
  pathTargetResults.value = []
  pathDialogVisible.value = true
}

async function runPathAnalysis() {
  if (!pathSource.value || !pathTarget.value) {
    ElMessage.warning('请选择起点和终点')
    return
  }
  pathLoading.value = true
  try {
    pathResult.value = await lineageApi.findPath({ sourceId: pathSource.value, targetId: pathTarget.value })
  } catch (e) {
    ElMessage.error('路径分析失败: ' + (e.message || '未知错误'))
  } finally { pathLoading.value = false }
}

// ---- Heat ----
async function loadHeat() {
  heatDialogVisible.value = true
  heatLoading.value = true
  try {
    heatData.value = await lineageApi.getHeat()
  } catch (e) {
    ElMessage.error('获取热度失败: ' + (e.message || '未知错误'))
  } finally { heatLoading.value = false }
}

// ---- Rebuild ----
async function rebuildGraph() {
  try {
    await ElMessageBox.confirm('将根据元数据、同步任务、采集任务、质量规则、工作流 SQL 全量重建血缘图数据（约数十秒）。确认继续？', '重建图数据', { type: 'warning' })
  } catch { return }
  store.graphLoading = true
  try {
    const res = await lineageApi.rebuild()
    ElMessage.success(`重建完成：${res.totalNodes} 节点 / ${res.totalEdges} 边`)
    if (store.rootNodeId) await store.loadGraph()
    else await store.loadOverview()
  } catch (e) {
    ElMessage.error('重建失败: ' + (e.message || '未知错误'))
  } finally { store.graphLoading = false }
}

// ---- SQL analysis ----
function openSqlDialog() {
  sqlText.value = ''
  sqlResult.value = null
  sqlDialogVisible.value = true
}

async function runSqlAnalysis() {
  if (!sqlText.value || !sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }
  sqlAnalyzing.value = true
  try {
    sqlResult.value = await metaApi.analyzeSql(sqlText.value)
    if (!sqlResult.value.nodes || !sqlResult.value.nodes.length) {
      ElMessage.info('未解析到血缘关系')
    }
  } catch (e) {
    ElMessage.error('SQL 解析失败: ' + (e.message || '未知错误'))
  } finally { sqlAnalyzing.value = false }
}

// ---- WebSocket realtime ----
function connectWs() {
  try {
    const token = localStorage.getItem('zqy_token')
    if (!token) return
    const base = import.meta.env.DEV ? 'ws://localhost:8080' : `ws://${location.host}`
    ws = new WebSocket(`${base}/ws/lineage?token=${token}`)
    ws.onopen = () => { wsConnected.value = true }
    ws.onclose = () => {
      wsConnected.value = false
      setTimeout(connectWs, 5000)
    }
    ws.onerror = () => { wsConnected.value = false }
    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'lineage.updated') {
          ElMessage.success(`血缘数据已更新（${msg.source || '采集'}）`)
          cycleEdgeKeys.value = []
          if (store.isOverview) store.loadOverview()
          else if (store.rootNodeId) store.loadGraph()
        }
      } catch (e) { /* ignore */ }
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  const nodeQuery = route.query.node
  if (nodeQuery) {
    store.rootNodeId = String(nodeQuery)
    store.isOverview = false
    store.loadGraph().catch(() => {
      ElMessage.error('定位节点失败，已切换至总览')
      store.loadOverview()
    })
  } else {
    store.loadOverview()
  }
  connectWs()
})

onBeforeUnmount(() => {
  ws?.close()
})
</script>

<style scoped>
.impact-summary { margin-bottom: 12px; font-size: 13px; color: #1f2937; }
.impact-hint { margin-left: 12px; font-size: 12px; color: #8492a6; }
.lineage-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.lineage-toolbar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  white-space: nowrap;
}
.toolbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  flex-wrap: wrap;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-degree {
  color: #9ca3af;
  font-size: 12px;
  margin-left: auto;
}

.lineage-body {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.graph-container {
  flex: 1;
  background: #111827;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  min-height: 0;
}
.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
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
  flex-wrap: wrap;
  color: #e5e7eb;
  font-size: 13px;
  z-index: 10;
  pointer-events: none;
}

.detail-panel {
  width: 300px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  overflow-y: auto;
  flex-shrink: 0;
}
.detail-panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
  word-break: break-all;
  display: flex;
  align-items: center;
  gap: 6px;
}
.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.field-lineage {
  margin-top: 16px;
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
}
.field-lineage h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7280;
}
.field-link-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  font-size: 12px;
  border-bottom: 1px solid #f3f4f6;
}
.fl-source { color: #10b981; }
.fl-target { color: #f59e0b; }
.fl-arrow { color: #9ca3af; flex-shrink: 0; }
.fl-logic { margin-left: auto; }

.path-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.path-selects {
  display: flex;
  gap: 12px;
}
.path-field {
  flex: 1;
}
.path-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}
.path-dialog-actions {
  display: flex;
  justify-content: flex-end;
}
.path-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.path-chain {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.chain-arrow {
  color: #9ca3af;
  font-size: 12px;
}
.path-hops h4 {
  margin: 8px 0;
  font-size: 13px;
  color: #6b7280;
}

.sql-dialog-hint {
  color: #6b7280;
  font-size: 12px;
  margin: 0 0 8px;
}
.sql-dialog-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.sql-result {
  margin-top: 14px;
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
}
.sql-result-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.sql-result-arrow {
  font-size: 13px;
  color: #374151;
}
.sql-result h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7280;
}
</style>