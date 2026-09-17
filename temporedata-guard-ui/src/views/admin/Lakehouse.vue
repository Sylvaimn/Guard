<template>
  <div class="lakehouse-page">
    <div class="page-header">
      <h2 class="page-title">湖仓管理</h2>
      <div class="page-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="CircleCheck" :loading="initLoading" @click="handleInit">初始化表</el-button>
        <el-button :icon="Link" :loading="registering" @click="handleRegister">注册为数据源</el-button>
      </div>
    </div>

    <!-- Overview -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6">
        <div class="ov-card">
          <div class="ov-label">Warehouse 路径</div>
          <div class="ov-value sm" :title="warehouse">{{ warehouse || '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ov-card">
          <div class="ov-label">湖仓表</div>
          <div class="ov-value sm">{{ tableName }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ov-card">
          <div class="ov-label">快照数</div>
          <div class="ov-value">{{ snapshots.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ov-card">
          <div class="ov-label">数据行数</div>
          <div class="ov-value">{{ rows.length }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- Snapshot list -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">快照 (时间旅行)</span>
          <span class="section-sub">Iceberg 每次写入生成快照，可回溯历史版本</span>
        </div>
      </template>
      <el-table :data="snapshots" v-loading="loading" size="small" empty-text="暂无快照（先初始化表）">
        <el-table-column prop="snapshotId" label="快照 ID" min-width="200" />
        <el-table-column label="快照时间" width="180">
          <template #default="{ row }">
            {{ fmtTs(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :disabled="row.snapshotId === currentSnapshotId" @click="loadSnapshot(row.snapshotId)">
              时间旅行读取
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Data preview -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">数据预览</span>
          <span class="section-sub">{{ viewingSnapshot ? '快照 ' + viewingSnapshot : '当前快照' }}</span>
          <el-button v-if="viewingSnapshot" size="small" @click="loadRows">回到当前</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="rowsLoading" stripe height="360" empty-text="暂无数据（可追加写入）">
        <el-table-column v-for="col in columns" :key="col" :prop="col" :label="col" min-width="130" />
      </el-table>
    </el-card>

    <!-- Append -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">追加写入</span>
          <span class="section-sub">JSON 数组格式，写入后生成新快照</span>
        </div>
      </template>
      <el-input
        v-model="appendJson"
        type="textarea"
        :rows="4"
        placeholder='[{"region":"昌平区","stat_date":"2026-08-25","renci":80,"guiji":600}]'
      />
      <div class="append-actions">
        <el-button type="primary" :loading="appending" @click="handleAppend">写入湖仓</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { lakehouseApi } from '@/api/modules/lakehouse'
import { datasourceApi } from '@/api/modules/datasource'
import { ElMessage } from 'element-plus'
import { Refresh, CircleCheck, Link } from '@element-plus/icons-vue'

const warehouse = ref('')
const tableName = ref('police.track_flow_daily')
const loading = ref(false)
const initLoading = ref(false)
const rowsLoading = ref(false)
const appending = ref(false)
const snapshots = ref([])
const rows = ref([])
const columns = ref([])
const currentSnapshotId = ref(null)
const viewingSnapshot = ref('')
const appendJson = ref('')
const registering = ref(false)

async function loadAll() {
  loading.value = true
  try {
    await loadSnapshots()
    await loadRows()
    const info = await lakehouseApi.init()
    if (info) {
      warehouse.value = info.warehouse || warehouse.value
      tableName.value = info.table || tableName.value
    }
  } catch (e) {
    ElMessage.error(e.message || '湖仓加载失败')
  } finally {
    loading.value = false
  }
}

async function loadSnapshots() {
  const list = await lakehouseApi.snapshots() || []
  snapshots.value = list
  if (list.length) {
    currentSnapshotId.value = list[list.length - 1].snapshotId
  }
}

async function loadRows() {
  rowsLoading.value = true
  viewingSnapshot.value = ''
  try {
    const data = await lakehouseApi.read() || []
    rows.value = data
    columns.value = data.length ? Object.keys(data[0]) : []
  } catch (e) {
    ElMessage.error(e.message || '读取失败')
  } finally {
    rowsLoading.value = false
  }
}

async function loadSnapshot(snapshotId) {
  rowsLoading.value = true
  viewingSnapshot.value = snapshotId
  try {
    const data = await lakehouseApi.readSnapshot(snapshotId) || []
    rows.value = data
    columns.value = data.length ? Object.keys(data[0]) : []
  } catch (e) {
    ElMessage.error(e.message || '快照读取失败（该快照不支持时间旅行）')
  } finally {
    rowsLoading.value = false
  }
}

async function handleRegister() {
  if (!warehouse.value) { ElMessage.warning('请先初始化湖仓表'); return }
  registering.value = true
  try {
    const ds = await datasourceApi.create({
      name: 'track_flow_daily',
      type: 'ICEBERG',
      host: warehouse.value,
      port: 9000,
      database: 'police',
      username: '',
      password: ''
    })
    ElMessage.success('已注册为 Iceberg 数据源: ' + (ds?.name || 'track_flow_daily'))
  } catch (err) {
    ElMessage.error(err.message || '注册失败(可能已存在)')
  } finally {
    registering.value = false
  }
}

async function handleInit() {
  initLoading.value = true
  try {
    const info = await lakehouseApi.init()
    warehouse.value = info.warehouse
    tableName.value = info.table
    ElMessage.success('湖仓表就绪: ' + info.table)
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message || '初始化失败')
  } finally {
    initLoading.value = false
  }
}

async function fmtTs(ms) {
  if (!ms) return '-'
  const d = new Date(Number(ms))
  return d.toLocaleString('zh-CN', { hour12: false })
}

async function handleAppend() {
  let parsed
  try {
    parsed = JSON.parse(appendJson.value)
  } catch (e) {
    ElMessage.warning('JSON 格式错误')
    return
  }
  if (!Array.isArray(parsed) || !parsed.length) {
    ElMessage.warning('请输入 JSON 数组')
    return
  }
  appending.value = true
  try {
    const n = await lakehouseApi.append(parsed)
    ElMessage.success(`写入成功: ${n} 行`)
    appendJson.value = ''
    await loadSnapshots()
    await loadRows()
  } catch (e) {
    ElMessage.error(e.message || '写入失败')
  } finally {
    appending.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.lakehouse-page { max-width: 1200px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.overview-row { margin-bottom: 16px; }
.ov-card { background: #fff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.ov-label { font-size: 12px; color: #8492a6; margin-bottom: 6px; }
.ov-value { font-size: 22px; font-weight: 600; color: #1f2937; }
.ov-value.sm { font-size: 13px; font-weight: 500; word-break: break-all; }
.section-card { margin-bottom: 16px; border-radius: 8px; }
.section-header { display: flex; align-items: center; gap: 10px; }
.section-title { font-weight: 600; color: #1f2937; }
.section-sub { font-size: 12px; color: #8492a6; flex: 1; }
.append-actions { margin-top: 12px; }
</style>
