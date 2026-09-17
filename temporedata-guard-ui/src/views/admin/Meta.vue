<template>
  <div class="meta-page">
    <div class="page-header">
      <h2 class="page-title">元数据管理</h2>
      <div class="page-actions">
        <el-button type="primary" :icon="Refresh" :loading="syncing" @click="handleSyncAll">全量同步</el-button>
      </div>
    </div>

    <!-- Overview Stats -->
    <div class="overview-stats">
      <div class="ov-card">
        <div class="ov-value">{{ overview?.totalTables ?? 0 }}</div>
        <div class="ov-label">已采集表</div>
      </div>
      <div class="ov-card">
        <div class="ov-value">{{ overview?.totalColumns ?? 0 }}</div>
        <div class="ov-label">字段总数</div>
      </div>
      <div class="ov-card">
        <div class="ov-value">{{ fmtRows(overview?.totalRows ?? 0) }}</div>
        <div class="ov-label">数据总量(行)</div>
      </div>
      <div class="ov-card">
        <div class="ov-value">{{ overview?.unCollectedCount ?? 0 }}</div>
        <div class="ov-label">未采集数据源</div>
      </div>
      <div class="ov-card">
        <div class="ov-value">{{ (overview?.datasources || []).length }}</div>
        <div class="ov-label">数据源总数</div>
      </div>
    </div>

    <!-- Datasource collection status cards -->
    <div class="ds-cards">
      <div
        v-for="ds in overview?.datasources || []"
        :key="ds.datasourceId"
        class="ds-card"
        :class="{ 'is-collected': ds.collected, 'is-selected': selectedDs === ds.datasourceId }"
        @click="filterByDatasource(ds.datasourceId)"
      >
        <div class="ds-card-head">
          <span class="ds-name" :title="ds.datasourceName">{{ ds.datasourceName }}</span>
          <el-tag size="small" :type="ds.collected ? 'success' : 'warning'" effect="light">
            {{ ds.collected ? '已采集' : '未采集' }}
          </el-tag>
        </div>
        <div class="ds-meta">
          <span>{{ ds.tableCount }} 表</span>
          <span>{{ ds.columnCount }} 字段</span>
          <span>{{ fmtRows(ds.rowCount) }} 行</span>
        </div>
        <div class="ds-sync-time">
          <el-icon v-if="ds.collected" :size="12"><Clock /></el-icon>
          {{ ds.collected ? '最近同步: ' + (ds.lastSyncTime || '-') : '待执行结构采集' }}
        </div>
        <div v-if="ds.datasourceType === 'H2'" class="ds-hint" title="平台 H2 数据源为进程内内存库，服务重启后数据与元数据需重新采集">
          <el-icon :size="12"><WarningFilled /></el-icon>
          内存库 · 重启后需重新导入数据
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="meta-tabs">
      <el-tab-pane label="已采集元数据" name="tables">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="selectedDs" placeholder="按数据源筛选" clearable style="width: 200px" @change="fetchTables">
        <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
      </el-select>
      <el-input v-model="searchKey" placeholder="搜索表名..." clearable style="width: 240px" @input="doSearch" />
      <el-button :icon="Search" @click="fetchTables">查询</el-button>
    </div>

    <!-- Table List -->
    <el-table :data="pagedData" v-loading="loading" stripe @row-click="selectTable" highlight-current-row>
      <el-table-column prop="tableName" label="表名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="datasourceName" label="数据源" width="120" />
      <el-table-column prop="schemaName" label="Schema" width="100" />
      <el-table-column prop="comment" label="注释" min-width="200" show-overflow-tooltip />
      <el-table-column prop="rowCount" label="记录数" width="100" align="right" />
      <el-table-column prop="syncTime" label="同步时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Connection" @click.stop="showLineage(row)">血缘</el-button>
          <el-button type="success" link :icon="List" @click.stop="showColumns(row)">字段</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage" v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]" :total="filteredData.length"
      layout="total, sizes, prev, pager, next" class="pagination"
    />
      </el-tab-pane>

      <!-- 统一元数据目录 -->
      <el-tab-pane label="统一元数据目录" name="unified">
        <div class="toolbar">
          <el-select v-model="unifiedType" placeholder="按来源类型筛选" clearable style="width: 200px" @change="fetchUnified">
            <el-option v-for="t in unifiedTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
          <span class="unified-tip">跨 MySQL / ClickHouse / Iceberg / Kafka 元数据聚合目录</span>
          <el-button type="primary" :icon="Refresh" :loading="unifiedSyncing" @click="handleUnifiedSync">同步目录</el-button>
        </div>
        <el-table :data="unifiedRows" v-loading="unifiedLoading" stripe style="width: 100%">
          <el-table-column prop="datasource_name" label="数据源" min-width="140" show-overflow-tooltip />
          <el-table-column prop="source_type" label="来源类型" width="140">
            <template #default="{ row }">
              <el-tag :type="unifiedTypeTag(row.source_type)" size="small" effect="light">{{ dsTypeLabel(row.source_type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="item_name" label="对象名" min-width="180" show-overflow-tooltip />
          <el-table-column prop="item_type" label="对象类型" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.item_type" size="small" effect="plain">{{ row.item_type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="extra" label="附加信息" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sync_time" label="同步时间" width="160" />
        </el-table>
        <div v-if="!unifiedRows.length && !unifiedLoading" class="empty-tip">暂无统一目录数据（点击「同步目录」采集各数据源元数据）</div>
      </el-tab-pane>
    </el-tabs>

    <!-- Column Detail Dialog -->
    <el-dialog v-model="columnsVisible" title="表字段详情" width="700px" destroy-on-close>
      <div class="dialog-table-name" v-if="selectedTable">
        <strong>{{ selectedTable.datasourceName }} / {{ selectedTable.tableName }}</strong>
      </div>
      <el-table :data="columns" v-loading="columnsLoading" stripe max-height="400">
        <el-table-column prop="ordinalPosition" label="#" width="50" />
        <el-table-column prop="columnName" label="字段名" width="160" />
        <el-table-column prop="columnType" label="类型" width="120" />
        <el-table-column prop="columnSize" label="长度" width="70" />
        <el-table-column prop="nullable" label="可空" width="70">
          <template #default="{ row }">
            <el-tag :type="row.nullable ? 'warning' : 'info'" size="small">{{ row.nullable ? 'YES' : 'NO' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="primaryKey" label="主键" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.primaryKey" type="danger" size="small">PK</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="注释" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- Lineage Dialog -->
    <el-dialog v-model="lineageVisible" :title="'血缘分析 - ' + (selectedTable?.tableName || '')" width="900px" fullscreen destroy-on-close>
      <el-form inline class="lineage-controls">
        <el-form-item label="追溯深度">
          <el-slider v-model="lineageDepth" :min="1" :max="5" style="width: 150px" @change="fetchLineage" show-stops />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="lineageLoading" @click="fetchLineage">刷新</el-button>
        </el-form-item>
      </el-form>
      <LineageGraph
        :nodes="allLineageNodes"
        :edges="lineageEdges"
        :centerTableId="selectedTable?.id"
        :centerTableName="selectedTable?.tableName"
      />
      <el-divider />
      <el-tabs v-model="lineageTab">
        <el-tab-pane label="上游依赖" name="upstream">
          <el-table :data="lineageData?.upstream" stripe max-height="300">
            <el-table-column prop="tableName" label="表名" width="180" />
            <el-table-column prop="relationType" label="关系类型" width="100">
              <template #default="{ row }">
                <el-tag :type="relationTag(row.relationType)" size="small">{{ row.relationType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relationName" label="关系名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="depth" label="深度" width="60" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="下游依赖" name="downstream">
          <el-table :data="lineageData?.downstream" stripe max-height="300">
            <el-table-column prop="tableName" label="表名" width="180" />
            <el-table-column prop="relationType" label="关系类型" width="100">
              <template #default="{ row }">
                <el-tag :type="relationTag(row.relationType)" size="small">{{ row.relationType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relationName" label="关系名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="depth" label="深度" width="60" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`影响分析 (${impactData?.totalImpacted ?? 0})`" name="impact">
          <el-alert
            v-if="impactData?.totalImpacted === 0"
            title="此表无下游依赖，修改不会影响其他数据对象"
            type="success"
            :closable="false"
            show-icon
          />
          <el-table v-else :data="impactData?.paths" stripe max-height="300" :default-sort="{ prop: 'depth', order: 'ascending' }">
            <el-table-column prop="tableName" label="受影响表" width="180" show-overflow-tooltip />
            <el-table-column prop="datasourceName" label="所属数据源" width="140" show-overflow-tooltip />
            <el-table-column prop="relationType" label="关系类型" width="100">
              <template #default="{ row }">
                <el-tag :type="relationTag(row.relationType)" size="small">{{ row.relationType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="depth" label="影响深度" width="90" align="center" />
            <el-table-column prop="pathChain" label="影响路径" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="path-chain">{{ (row.pathChain || []).join(' → ') }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search, Connection, List, Clock, WarningFilled } from '@element-plus/icons-vue'
import { dsTypeLabel } from '@/utils/dsLabels'
import { ElMessage } from 'element-plus'
import { metaApi } from '@/api/modules/meta'
import { datasourceApi } from '@/api/modules/datasource'
import LineageGraph from '@/components/lineage/GraphLite.vue'

const loading = ref(false)
const syncing = ref(false)
const activeTab = ref('tables')
const unifiedRows = ref([])
const unifiedLoading = ref(false)
const unifiedSyncing = ref(false)
const unifiedType = ref('')
const unifiedTypeOptions = ['JDBC', 'CLICKHOUSE', 'ICEBERG', 'KAFKA']
const tables = ref([])
const datasources = ref([])
const selectedDs = ref('')
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredData = computed(() => {
  let data = tables.value
  if (selectedDs.value) data = data.filter(t => t.datasourceId === selectedDs.value)
  if (searchKey.value) {
    const kw = searchKey.value.toLowerCase()
    data = data.filter(t => t.tableName?.toLowerCase().includes(kw) || t.comment?.toLowerCase().includes(kw))
  }
  return data
})
const pagedData = computed(() => {
  const s = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(s, s + pageSize.value)
})

// Columns
const columnsVisible = ref(false)
const columnsLoading = ref(false)
const columns = ref([])
const selectedTable = ref(null)

// Lineage
const lineageVisible = ref(false)
const lineageLoading = ref(false)
const lineageData = ref(null)
const lineageDepth = ref(3)
const lineageTab = ref('upstream')

// Impact analysis
const impactData = ref(null)

// Overview
const overview = ref(null)

const allLineageNodes = computed(() => {
  if (!lineageData.value) return []
  return [...(lineageData.value.upstream || []), ...(lineageData.value.downstream || [])]
})
const lineageEdges = computed(() => lineageData.value?.edges || [])

onMounted(() => {
  fetchDatasources()
  fetchTables()
  fetchOverview()
  fetchUnified()
})

async function fetchOverview() {
  try { overview.value = await metaApi.overview() || null } catch (e) { /* ignore */ }
}

function filterByDatasource(dsId) {
  selectedDs.value = selectedDs.value === dsId ? '' : dsId
  currentPage.value = 1
  fetchTables()
}

function fmtRows(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

async function fetchDatasources() {
  try { datasources.value = await datasourceApi.list() || [] } catch (e) { /* ignore */ }
}

async function fetchTables() {
  loading.value = true
  try {
    if (selectedDs.value) {
      tables.value = await metaApi.listTablesByDatasource(selectedDs.value) || []
    } else {
      tables.value = await metaApi.listTables() || []
    }
  } catch (e) {
    ElMessage.error('获取元数据失败')
  } finally { loading.value = false }
}

function doSearch() { currentPage.value = 1 }

async function fetchUnified() {
  unifiedLoading.value = true
  try {
    const rows = await metaApi.unifiedList(unifiedType.value || undefined)
    unifiedRows.value = rows || []
  } catch (e) {
    ElMessage.error(e.message || '获取统一目录失败')
  } finally { unifiedLoading.value = false }
}

async function handleUnifiedSync() {
  unifiedSyncing.value = true
  try {
    const n = await metaApi.unifiedSync()
    ElMessage.success(`统一目录同步完成: ${n} 条元数据`)
    await fetchUnified()
  } catch (e) {
    ElMessage.error(e.message || '统一目录同步失败')
  } finally { unifiedSyncing.value = false }
}

function unifiedTypeTag(t) {
  const map = { JDBC: '', CLICKHOUSE: 'warning', ICEBERG: 'success', KAFKA: 'danger' }
  return map[t] || 'info'
}

async function handleSyncAll() {
  syncing.value = true
  try {
    const res = await metaApi.syncAll()
    if (res.skippedCount > 0) {
      ElMessage.warning(`跳过 ${res.skippedCount} 个数据源(空库保护): ${(res.skippedReasons || []).join('; ')}`)
    }
    ElMessage.success(`同步完成: ${res.successCount}个数据源, ${res.totalTables}张表, ${res.totalColumns}个字段`)
    await fetchTables()
    await fetchOverview()
  } catch (e) {
    ElMessage.error('同步失败')
  } finally { syncing.value = false }
}

function selectTable(row) { selectedTable.value = row }

async function showColumns(row) {
  selectedTable.value = row
  columnsVisible.value = true
  columnsLoading.value = true
  try {
    columns.value = await metaApi.getColumns(row.id) || []
  } catch (e) {
    ElMessage.error('获取字段失败')
  } finally { columnsLoading.value = false }
}

async function showLineage(row) {
  selectedTable.value = row
  impactData.value = null
  lineageTab.value = 'upstream'
  lineageVisible.value = true
  await fetchLineage()
  try {
    impactData.value = await metaApi.getImpactAnalysis(row.id, 5) || null
  } catch (e) { /* ignore */ }
}

async function fetchLineage() {
  if (!selectedTable.value) return
  lineageLoading.value = true
  try {
    lineageData.value = await metaApi.getLineage(selectedTable.value.id, lineageDepth.value)
  } catch (e) {
    ElMessage.error('获取血缘失败')
  } finally { lineageLoading.value = false }
}

function relationTag(type) {
  const map = { SYNC: 'success', INGESTION: 'warning', WORKFLOW: '', QUERY: 'info', QUALITY: 'danger' }
  return map[type] || 'info'
}
</script>

<style scoped>
.meta-tabs { margin-top: 16px; }
.unified-tip { font-size: 12px; color: #8492a6; margin-right: auto; }
.empty-tip { text-align: center; color: #909399; padding: 24px 0; font-size: 13px; }
.meta-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-actions { display: flex; gap: 8px; }

.overview-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.ov-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.ov-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}

.ov-label {
  font-size: 12px;
  color: #9ca3af;
}

.ds-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.ds-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.ds-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.ds-card.is-collected:hover {
  border-color: #10b981;
}

.ds-card.is-selected {
  border-color: #1a6ff5;
  background: #f0f6ff;
}

.ds-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ds-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ds-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}

.ds-sync-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #9ca3af;
}

.ds-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #f59e0b;
  margin-top: 2px;
}

.toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.dialog-table-name { margin-bottom: 12px; color: #6b7280; }
.lineage-controls { display: flex; align-items: center; gap: 12px; }
.path-chain { color: #6b7280; font-size: 12px; }

@media (max-width: 1200px) {
  .overview-stats { grid-template-columns: repeat(3, 1fr); }
}
</style>