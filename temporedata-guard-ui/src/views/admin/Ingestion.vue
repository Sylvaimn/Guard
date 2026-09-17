<template>
  <div class="ingestion-page">
    <div class="page-header">
      <h2 class="page-title">数据接入</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新建接入任务</el-button>
    </div>

    <!-- CDC 实时采集链路 -->
    <el-card class="cdc-card" shadow="never">
      <template #header>
        <div class="cdc-header">
          <span class="cdc-title"><el-icon style="vertical-align: -2px; margin-right: 4px"><Odometer /></el-icon>CDC 实时采集链路</span>
          <span class="cdc-sub">binlog 变更 → Kafka(zy-cdc) → Iceberg 湖仓 + 实时质量 + 断点位点</span>
          <div class="cdc-actions">
            <el-button size="small" type="primary" :loading="cdcDemoLoading" @click="handleCdcDemo">
              <el-icon style="margin-right: 4px"><VideoPlay /></el-icon>发布演示事件
            </el-button>
            <el-button size="small" :loading="reconcileLoading" @click="handleReconcile">采集对账</el-button>
            <el-button size="small" @click="loadCdc">刷新</el-button>
          </div>
        </div>
      </template>
      <el-row :gutter="16" class="cdc-stats">
        <el-col :span="8">
          <div class="cdc-stat">
            <div class="cdc-stat-label">链路状态</div>
            <el-tag :type="cdcLinkOk ? 'success' : 'warning'" effect="dark">{{ cdcStatus }}</el-tag>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="cdc-stat">
            <div class="cdc-stat-label">断点位点</div>
            <div class="cdc-stat-value">{{ offsets.length }}</div>
            <div class="cdc-stat-sub">个采集位点（失败从位点续传）</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="cdc-stat">
            <div class="cdc-stat-label">实时质量</div>
            <div class="cdc-stat-value">{{ qualityCount }}</div>
            <div class="cdc-stat-sub">条流式校验记录（PASS/FAIL）</div>
          </div>
        </el-col>
      </el-row>
      <el-table :data="offsets" v-loading="cdcLoading" size="small" max-height="200" empty-text="暂无采集位点（发布演示事件后产生）">
        <el-table-column label="采集位点" min-width="240">
          <template #default="{ row }">
            <div>{{ cdcSourceLabel(row.source_key) }}</div>
            <div class="cdc-key-sub">{{ row.source_key }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="offset_value" label="位点值(事件时间戳)" min-width="160" />
        <el-table-column prop="last_event_time" label="最近事件时间" min-width="160" />
        <el-table-column prop="update_time" label="更新时间" min-width="160" />
      </el-table>
    </el-card>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="任务名称" min-width="160" />
        <el-table-column prop="sourceType" label="数据源类型" width="120">
          <template #default="{ row }">
            <el-tag :type="sourceTypeTagType(row.sourceType)" size="small">{{ dsTypeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetTable" label="目标表" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="success" link :icon="VideoPlay" @click="handleExecute(row)">执行</el-button>
              <el-popconfirm title="确定要删除该接入任务吗？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="currentPage = 1"
        />
      </div>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑接入任务' : '新建接入任务'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择数据源类型" style="width: 100%">
            <el-option label="CSV" value="CSV" />
            <el-option label="API" value="API" />
            <el-option label="DB" value="DB" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标表" prop="targetTable">
          <el-input v-model="form.targetTable" placeholder="请输入目标表名" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
            maxlength="256"
          />
        </el-form-item>
        <el-form-item label="CSV 文件" v-if="form.sourceType === 'CSV'">
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".csv"
          >
            <el-icon class="upload-icon"><Upload /></el-icon>
            <div class="upload-text">将 CSV 文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .csv 格式文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, Upload, Odometer, Refresh } from '@element-plus/icons-vue'
import { ingestionApi } from '@/api/modules/ingestion'
import api from '@/api'
import { ElMessage } from 'element-plus'
import { dsTypeLabel, cdcSourceLabel } from '@/utils/dsLabels'

// ---- Data ----
const tasks = ref([])
const loading = ref(false)
const cdcStatus = ref('加载中...')
const cdcLinkOk = ref(false)
const offsets = ref([])
const qualityCount = ref(0)
const cdcLoading = ref(false)
const cdcDemoLoading = ref(false)
const reconcileLoading = ref(false)
const reconcileResult = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = computed(() => tasks.value.length)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tasks.value.slice(start, start + pageSize.value)
})

// ---- Dialog ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const editId = ref(null)
const fileList = ref([])
const csvFile = ref(null)

const form = reactive({
  name: '',
  sourceType: '',
  targetTable: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  targetTable: [{ required: true, message: '请输入目标表名', trigger: 'blur' }]
}

// ---- Status helpers ----
function statusTagType(status) {
  const map = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { PENDING: '待执行', RUNNING: '执行中', SUCCESS: '已成功', FAILED: '已失败' }
  return map[status] || status
}

function sourceTypeTagType(type) {
  const map = { CSV: 'info', API: 'success', DB: 'warning' }
  return map[type] || 'info'
}

// ---- File upload ----
function handleFileChange(file) {
  csvFile.value = file.raw
  fileList.value = [file]
}

function handleFileRemove() {
  csvFile.value = null
  fileList.value = []
}

// ---- Fetch ----
async function fetchTasks() {
  loading.value = true
  try {
    tasks.value = await ingestionApi.list()
  } catch (err) {
    ElMessage.error(err.message || '获取接入任务列表失败')
  } finally {
    loading.value = false
  }
}

// ---- CRUD ----
function openAddDialog() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.sourceType = ''
  form.targetTable = ''
  form.description = ''
  csvFile.value = null
  fileList.value = []
  formRef.value?.resetFields()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.sourceType = row.sourceType
  form.targetTable = row.targetTable
  form.description = row.description || ''
  csvFile.value = null
  fileList.value = []
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      sourceType: form.sourceType,
      targetTable: form.targetTable,
      description: form.description
    }
    if (isEdit.value) {
      await ingestionApi.update(editId.value, payload)
      ElMessage.success('接入任务更新成功')
    } else {
      await ingestionApi.create(payload)
      ElMessage.success('接入任务创建成功')
    }
    dialogVisible.value = false
    await fetchTasks()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleExecute(row) {
  try {
    await ingestionApi.execute(row.id)
    ElMessage.success(`接入任务 "${row.name}" 已触发执行`)
    await fetchTasks()
  } catch (err) {
    ElMessage.error(err.message || '执行失败')
  }
}

async function handleDelete(id) {
  try {
    await ingestionApi.delete(id)
    ElMessage.success('接入任务已删除')
    await fetchTasks()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- Init ----
async function loadCdc() {
  cdcLoading.value = true
  try {
    const st = await ingestionApi.cdcStatus()
    cdcStatus.value = st || '就绪'
    cdcLinkOk.value = !!st
    const offs = await ingestionApi.cdcOffsets()
    offsets.value = offs || []
    qualityCount.value = (await loadQualityCount())
  } catch (e) {
    cdcStatus.value = '链路不可用（后端/Kafka 未就绪）'
    cdcLinkOk.value = false
  } finally {
    cdcLoading.value = false
  }
}

async function loadQualityCount() {
  try {
    const resp = await api.get('/api/quality/check/realtime')
    return (resp.data.data && resp.data.data.length) ? resp.data.data.length : 0
  } catch (e) {
    return 0
  }
}

async function handleReconcile() {
  reconcileLoading.value = true
  try {
    const r = await ingestionApi.cdcReconcile()
    reconcileResult.value = r
    if (r.status === 'SYNCED') {
      ElMessage.success(`对账一致: 源库 ${r.sourceRows} 行 = 湖仓 ${r.lakeRows} 行`)
    } else if (r.status === 'MISMATCH') {
      ElMessage.warning(`对账不一致: 源库 ${r.sourceRows} 行 vs 湖仓 ${r.lakeRows} 行, 差 ${r.diff} 行(待补采)`)
    } else {
      ElMessage.error('对账失败: ' + (r.error || r.status))
    }
  } catch (err) {
    ElMessage.error(err.message || '对账失败')
  } finally {
    reconcileLoading.value = false
  }
}

async function handleCdcDemo() {
  cdcDemoLoading.value = true
  try {
    const msg = await ingestionApi.cdcDemo()
    ElMessage.success('演示事件已发布: ' + (msg || 'binlog -> Kafka -> 湖仓'))
    setTimeout(loadCdc, 1500)
  } catch (err) {
    ElMessage.error(err.message || '发布失败')
  } finally {
    cdcDemoLoading.value = false
  }
}

onMounted(() => {
  fetchTasks()
  loadCdc()
})
</script>

<style scoped>
.ingestion-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.cdc-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.cdc-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cdc-title {
  font-weight: 600;
  color: #1f2937;
}

.cdc-sub {
  font-size: 12px;
  color: #8492a6;
  flex: 1;
}

.cdc-actions {
  display: flex;
  gap: 8px;
}

.cdc-stats {
  margin-bottom: 12px;
}

.cdc-stat {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 16px;
}

.cdc-stat-label {
  font-size: 12px;
  color: #8492a6;
  margin-bottom: 6px;
}

.cdc-stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.cdc-key-sub { font-size: 11px; color: #b0b8c4; margin-top: 2px; }
.cdc-stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-dragger {
  width: 100%;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  margin-top: 8px;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
}
</style>