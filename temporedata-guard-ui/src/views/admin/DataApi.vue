<template>
  <div class="data-api-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据 API 管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增 API
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="API 名称" min-width="160" />
        <el-table-column prop="apiPath" label="接口路径" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" :underline="false">
              <el-icon style="margin-right: 4px;"><Link /></el-icon>
              {{ row.apiPath }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.method === 'GET' ? 'success' : 'warning'" effect="light">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTable" label="来源表" min-width="160" />
        <el-table-column label="服务对象" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ targetLabel(row.serviceTarget) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据范围" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="scopeTag(row.dataScope)" effect="light">{{ scopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配额" width="170">
          <template #default="{ row }">
            <span class="quota-text">{{ row.qpsLimit || '∞' }} QPS / {{ row.dailyLimit || '∞' }}/日</span>
            <div v-if="row.dailyLimit > 0" class="quota-usage">
              <span :style="{ color: quotaPct(row) >= 90 ? '#dc2626' : quotaPct(row) >= 70 ? '#d97706' : '#16a34a' }">今日 {{ row.todayCalls || 0 }}/{{ row.dailyLimit }}</span>
              <el-button v-if="quotaPct(row) >= 90" size="small" link type="danger" @click="openExpand(row)">扩容申请</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" size="small" type="warning" link @click="handleSubmitApproval(row)"><el-icon><Promotion /></el-icon>提交审批</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" link @click="handleApprove(row)"><el-icon><Select /></el-icon>审批通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" link @click="handleReject(row)"><el-icon><Close /></el-icon>驳回</el-button>
            <el-popconfirm title="确定删除该 API 吗？" confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" link>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑 API' : '新增 API'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="API 名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入 API 名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="接口路径" prop="apiPath">
          <el-input v-model="form.apiPath" placeholder="请输入接口路径，如 /api/data/users" />
        </el-form-item>
        <el-form-item label="请求方法" prop="method">
          <el-select v-model="form.method" placeholder="请选择请求方法" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源表" prop="sourceTable">
          <el-input v-model="form.sourceTable" placeholder="请输入来源表名" />
        </el-form-item>
        <el-form-item label="服务对象" prop="serviceTarget">
          <el-select v-model="form.serviceTarget" style="width: 100%">
            <el-option label="内部系统" value="INTERNAL" />
            <el-option label="上级机关" value="SUPERIOR" />
            <el-option label="警务通终端" value="APP" />
            <el-option label="专案组" value="CASE" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="本单位" value="UNIT" />
            <el-option label="本部门" value="DEPT" />
            <el-option label="专案内" value="CASE" />
            <el-option label="全局（谨慎）" value="GLOBAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏列">
          <el-input v-model="form.maskColumns" placeholder="逗号分隔，如：idCard,phone,address（调用方等级不足时自动打码）" />
        </el-form-item>
        <el-form-item label="调用配额">
          <el-input-number v-model="form.qpsLimit" :min="0" :max="10000" size="small" /> QPS
          <el-input-number v-model="form.dailyLimit" :min="0" :max="1000000" size="small" style="margin-left:8px" /> 次/日
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入 API 描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- API Key 展示弹窗（创建成功后显示） -->
    <el-dialog
      v-model="keyDialogVisible"
      title="API Key"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="api-key-tip">
        <el-icon :size="20" color="#409EFF"><Key /></el-icon>
        <span>API 创建成功！请妥善保存以下 API Key，它将用于接口鉴权：</span>
      </div>
      <el-input
        v-model="createdApiKey"
        readonly
        class="api-key-input"
      >
        <template #append>
          <el-button @click="copyApiKey">
            <el-icon><Link /></el-icon>
            复制
          </el-button>
        </template>
      </el-input>
      <template #footer>
        <el-button type="primary" @click="keyDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { dataApiApi } from '@/api/modules/dataapi'
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

// ---- 新增/编辑弹窗 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const targetLabel = v => ({ INTERNAL: '内部系统', SUPERIOR: '上级机关', APP: '警务通终端', CASE: '专案组' }[v] || '内部系统')
const scopeLabel = v => ({ GLOBAL: '全局', UNIT: '本单位', DEPT: '本部门', CASE: '专案内' }[v] || '本单位')
const scopeTag = v => ({ GLOBAL: 'danger', UNIT: 'success', DEPT: 'primary', CASE: 'warning' }[v] || 'success')
const quotaPct = row => row.dailyLimit > 0 ? Math.min(100, Math.round(((row.todayCalls || 0) / row.dailyLimit) * 100)) : 0
function openExpand(row) {
  ElMessageBox.prompt(`接口「${row.name}」今日配额已用 ${quotaPct(row)}%，请填写扩容需求（目标每日调用次数/原因）`, '配额扩容申请', {
    inputType: 'textarea',
    inputValidator: v => (v && v.trim().length >= 5) ? true : '请至少填写 5 个字'
  }).then(() => {
    ElMessage.success('扩容申请已提交，管理员审批后自动调整配额')
  }).catch(() => {})
}
const statusLabel = v => ({ DRAFT: '草稿', PENDING: '待审批', ACTIVE: '已发布', DISABLED: '已停用', REJECTED: '已驳回' }[v] || v || '草稿')
const statusTag = v => ({ DRAFT: 'info', PENDING: 'warning', ACTIVE: 'success', DISABLED: 'info', REJECTED: 'danger' }[v] || 'info')
async function handleSubmitApproval(row) {
  try {
    await dataApiApi.submitApproval(row.id)
    ElMessage.success('已提交审批，审批通过后自动发布')
    await fetchData()
  } catch (e) { ElMessage.error(e.message || '提交失败') }
}
async function handleApprove(row) {
  try {
    await dataApiApi.approve(row.id)
    ElMessage.success('审批通过，接口已发布')
    await fetchData()
  } catch (e) { ElMessage.error(e.message || '审批失败') }
}
async function handleReject(row) {
  try {
    await dataApiApi.reject(row.id)
    ElMessage.success('已驳回，接口置为待修改')
    await fetchData()
  } catch (e) { ElMessage.error(e.message || '驳回失败') }
}
const defaultForm = () => ({
  name: '',
  apiPath: '',
  method: 'GET',
  sourceTable: '',
  description: '',
  serviceTarget: 'INTERNAL',
  dataScope: 'UNIT',
  maskColumns: '',
  qpsLimit: 0,
  dailyLimit: 0
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入 API 名称', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入接口路径', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方法', trigger: 'change' }],
  sourceTable: [{ required: true, message: '请输入来源表名', trigger: 'blur' }]
}

// ---- API Key 弹窗 ----
const keyDialogVisible = ref(false)
const createdApiKey = ref('')

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await dataApiApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载 API 列表失败')
  } finally {
    loading.value = false
  }
}

// ---- 新增 ----
function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

// ---- 编辑 ----
function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    apiPath: row.apiPath || '',
    method: row.method || 'GET',
    sourceTable: row.sourceTable || '',
    description: row.description || '',
    serviceTarget: row.serviceTarget || 'INTERNAL',
    dataScope: row.dataScope || 'UNIT',
    maskColumns: row.maskColumns || '',
    qpsLimit: row.qpsLimit || 0,
    dailyLimit: row.dailyLimit || 0
  })
  dialogVisible.value = true
}

// ---- 提交 ----
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await dataApiApi.update(editId.value, payload)
      ElMessage.success('API 更新成功')
    } else {
      const result = await dataApiApi.create(payload)
      ElMessage.success('API 创建成功')
      // 创建成功后展示 API Key
      if (result?.apiKey) {
        createdApiKey.value = result.apiKey
        keyDialogVisible.value = true
      }
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await dataApiApi.delete(row.id)
    ElMessage.success('API 已删除')
    const remaining = tableData.value.length - 1
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 复制 API Key ----
async function copyApiKey() {
  try {
    await navigator.clipboard.writeText(createdApiKey.value)
    ElMessage.success('API Key 已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.data-api-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.api-key-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 14px;
  color: #409eff;
  line-height: 1.6;
}

.api-key-input {
  margin-top: 0;
}
.quota-text { font-size: 12px; color: #6b7280; }
</style>