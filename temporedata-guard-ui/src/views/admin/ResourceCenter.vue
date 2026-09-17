<template>
  <div class="resource-page">
    <div class="page-header">
      <h2 class="page-title">资源中心</h2>
      <el-button type="primary" :icon="Plus" @click="uploadDialogVisible = true">上传文件</el-button>
    </div>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="文件名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="originalName" label="原始名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="120" align="right">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="View" @click="openPreview(row)">打开</el-button>
              <el-button type="primary" link :icon="Download" @click="handleDownload(row)">下载</el-button>
              <el-button type="info" link :icon="Share" @click="openLineage(row)">血缘</el-button>
              <el-button type="success" link :icon="Connection" @click="openRegister(row)">登记血缘</el-button>
              <el-popconfirm title="确定要删除该文件吗？" @confirm="handleDelete(row.id)">
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
          :total="tableData.length"
          layout="total, sizes, prev, pager, next"
          @size-change="currentPage = 1"
        />
      </div>
    </div>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" :title="'预览 - ' + previewName" width="720px" top="6vh" destroy-on-close>
      <div v-if="previewData">
        <template v-if="previewData.type === 'text'">
          <el-alert v-if="previewData.ext === 'csv'" type="info" :closable="false" title="CSV 表格预览" style="margin-bottom: 10px" />
          <pre class="preview-text">{{ previewData.content }}</pre>
        </template>
        <img v-else-if="previewData.type === 'image'" :src="previewData.content" class="preview-img" />
        <iframe v-else-if="previewData.type === 'pdf'" :src="previewData.content" class="preview-pdf" />
        <el-empty v-else description="该格式暂不支持在线预览，请下载后查看" />
      </div>
      <div v-else v-loading="previewLoading" style="min-height: 200px" />
    </el-dialog>

    <!-- 血缘链路弹窗 -->
    <el-dialog v-model="lineageVisible" :title="'血缘链路 - ' + lineageName" width="680px" destroy-on-close>
      <el-alert v-if="!lineageEdges.length" type="info" :closable="false" title="该资源暂未参与血缘，点击「登记血缘」关联目标表" />
      <el-table :data="lineageEdges" v-loading="lineageLoading" stripe max-height="380">
        <el-table-column label="链路" min-width="260">
          <template #default="{ row }">
            <span class="node-chip">{{ row.sourceId.split(':').pop() }}</span>
            <el-tag size="small" effect="plain" style="margin: 0 6px">{{ edgeTypeLabel(row.edgeType) }}</el-tag>
            <span class="node-chip">{{ row.targetId.split(':').pop() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联对象" min-width="140">
          <template #default="{ row }">{{ row.taskName || '-' }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="140">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 登记血缘弹窗 -->
    <el-dialog v-model="registerVisible" :title="'登记血缘 - ' + registerName" width="480px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="目标表">
          <el-input v-model="registerForm.tableName" placeholder="如 track_flow_daily（湖仓/分析表名）" />
        </el-form-item>
        <el-form-item label="关系类型">
          <el-radio-group v-model="registerForm.relationType">
            <el-radio value="LOAD">文件 → 表（入湖加载）</el-radio>
            <el-radio value="EXPORT">表 → 文件（导出产物）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">登记</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件弹窗 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <p>将文件拖到此处，或 <em>点击上传</em></p>
          <p class="upload-hint">支持 jar、zip、tar.gz 等格式</p>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" :disabled="!uploadFile" @click="handleUpload">确定上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Delete, Download, UploadFilled, Share, Connection, View } from '@element-plus/icons-vue'
import { fileApi } from '@/api/modules/file'
import { resourceApi } from '@/api/modules/resource'
import { ElMessage } from 'element-plus'

// ---- 表格数据 ----
const tableData = ref([])
const loading = ref(false)

// ---- 分页 ----
const currentPage = ref(1)
const pageSize = ref(10)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

// ---- 上传弹窗 ----
const uploadDialogVisible = ref(false)
const uploadLoading = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)

function handleFileChange(file) {
  uploadFile.value = file.raw
}

function handleFileRemove() {
  uploadFile.value = null
}

// ---- 血缘 ----
const lineageVisible = ref(false)
const lineageLoading = ref(false)
const lineageName = ref('')
const lineageEdges = ref([])
const registerVisible = ref(false)
const registering = ref(false)
const registerName = ref('')
const registerId = ref('')
const registerForm = ref({ tableName: '', relationType: 'LOAD' })

function edgeTypeLabel(t) {
  const map = { LOAD: '加载', EXPORT: '导出', CONSUMES: '消费', PRODUCES: '产出', FLOWS_TO: '流向' }
  return map[t] || t || '-'
}

async function openLineage(row) {
  lineageName.value = row.name || row.originalName || ''
  lineageVisible.value = true
  lineageLoading.value = true
  lineageEdges.value = []
  try {
    const data = await resourceApi.lineage(row.id)
    lineageEdges.value = (data && data.edges) || []
  } catch (e) {
    ElMessage.error(e.message || '血缘查询失败')
  } finally {
    lineageLoading.value = false
  }
}

function openRegister(row) {
  registerName.value = row.name || row.originalName || ''
  registerId.value = row.id
  registerForm.value = { tableName: '', relationType: 'LOAD' }
  registerVisible.value = true
}

async function handleRegister() {
  if (!registerForm.value.tableName.trim()) { ElMessage.warning('请输入目标表名'); return }
  registering.value = true
  try {
    const r = await resourceApi.registerLineage(registerId.value, registerForm.value)
    ElMessage.success('血缘已登记: ' + (r.nodeId || '').split(':').pop())
    registerVisible.value = false
    if (lineageVisible.value) await openLineage({ id: registerId.value })
  } catch (e) {
    ElMessage.error(e.message || '登记失败')
  } finally {
    registering.value = false
  }
}

// ---- 预览/打开 ----
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewName = ref('')
const previewData = ref(null)

async function openPreview(row) {
  previewName.value = row.originalName || row.name || ''
  previewVisible.value = true
  previewLoading.value = true
  previewData.value = null
  try {
    const data = await fileApi.preview(row.id)
    previewData.value = data
  } catch (e) {
    ElMessage.error(e.message || '预览失败')
  } finally {
    previewLoading.value = false
  }
}

// ---- 下载(带认证, 修复 window.open 401) ----
async function handleDownload(row) {
  try {
    const blob = await fileApi.downloadBlob(row.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName || row.name || 'file'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e.message || '下载失败')
  }
}

// ---- 工具函数 ----
function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await fileApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// ---- 上传 ----
async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    await fileApi.upload(formData)
    ElMessage.success('文件上传成功，已自动生成血缘关联（文件名/表头匹配目标表，未命中仅建文件节点）')
    uploadDialogVisible.value = false
    uploadFile.value = null
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// ---- 下载 ----
// ---- 删除 ----
async function handleDelete(id) {
  try {
    await fileApi.delete(id)
    ElMessage.success('文件已删除')
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.preview-text { background: #f5f7fa; border-radius: 6px; padding: 14px; max-height: 480px; overflow: auto; font-size: 13px; white-space: pre-wrap; word-break: break-all; }
.preview-img { max-width: 100%; max-height: 480px; display: block; margin: 0 auto; }
.preview-pdf { width: 100%; height: 520px; border: none; }
.node-chip { font-family: monospace; font-size: 12px; color: #1f2937; background: #f3f4f6; border-radius: 4px; padding: 2px 8px; }
.resource-page {
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

.upload-area {
  width: 100%;
}

.upload-icon {
  font-size: 48px;
  color: var(--spark-text-muted);
  margin-bottom: 8px;
}

.upload-text {
  color: var(--spark-text-secondary);
  font-size: 14px;
}

.upload-text em {
  color: var(--spark-primary);
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: var(--spark-text-muted);
  margin-top: 4px;
}
</style>