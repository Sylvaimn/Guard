<template>
  <div class="workflow-page">
    <!-- Left: Workflow list -->
    <div class="workflow-sidebar">
      <div class="sidebar-header">
        <h3>作业流列表</h3>
        <el-button type="primary" size="small" :icon="Plus" @click="openCreateDialog">新建</el-button>
      </div>
      <el-input v-model="searchKey" placeholder="搜索作业流..." size="small" clearable class="sidebar-search" />
      <div class="workflow-list" v-loading="loading">
        <div
          v-for="wf in filteredWorkflows"
          :key="wf.id"
          class="workflow-item"
          :class="{ active: currentWorkflow?.id === wf.id }"
          @click="selectWorkflow(wf)"
        >
          <div class="wf-name">{{ wf.name }}</div>
          <div class="wf-meta">
            <el-tag :type="statusTag(wf.status)" size="small">{{ wf.status === 'ONLINE' ? '在线' : '离线' }}</el-tag>
          </div>
        </div>
        <el-empty v-if="!filteredWorkflows.length" description="暂无作业流" :image-size="60" />
      </div>
    </div>

    <!-- Right: DAG Editor -->
    <div class="workflow-main">
      <template v-if="currentWorkflow">
        <!-- Header -->
        <div class="main-header">
          <div class="header-info">
            <h2 class="wf-title">{{ currentWorkflow.name }}</h2>
            <span class="wf-desc" v-if="currentWorkflow.description">{{ currentWorkflow.description }}</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" :icon="VideoPlay" @click="handleExecute" :loading="executing">执行</el-button>
            <el-button :icon="Edit" @click="openEditDialog">编辑信息</el-button>
            <el-button :icon="Clock" @click="scheduleVisible = true">调度</el-button>
            <el-button type="danger" :icon="Delete" @click="handleDeleteWorkflow">删除</el-button>
          </div>
        </div>

        <!-- DAG Editor -->
        <div class="dag-wrapper">
          <WorkflowDagEditor
            ref="dagEditorRef"
            :workflowNodes="dagNodes"
            :workflowEdges="dagEdges"
            :datasources="datasources"
            @change="onDagChange"
            @reload="reloadWorkflow"
          />
        </div>

        <!-- Save bar -->
        <div class="save-bar" v-if="dagDirty">
          <el-alert title="DAG 已修改，请保存" type="warning" :closable="false" show-icon />
          <el-button type="primary" :loading="saving" @click="saveDag">保存 DAG</el-button>
        </div>
      </template>

      <div v-else class="no-workflow">
        <el-empty description="请选择或创建一个作业流" :image-size="80" />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业流' : '新建作业流'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入作业流名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="handleDialogSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Schedule Dialog -->
    <el-dialog v-model="scheduleVisible" title="调度配置" width="420px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="Cron 表达式">
          <el-input v-model="cronExpression" placeholder="0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="scheduleEnabled"
            active-text="启用"
            inactive-text="停用"
            @change="handleSchedule"
            :loading="scheduleLoading"
          />
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- Execution History Dialog -->
    <el-dialog v-model="historyVisible" title="执行历史" width="600px">
      <el-table :data="executions" v-loading="historyLoading" stripe>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="execStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showExecDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, Clock } from '@element-plus/icons-vue'
import { workflowApi } from '@/api/modules/workflow'
import { datasourceApi } from '@/api/modules/datasource'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkflowDagEditor from '@/components/workflow/WorkflowDagEditor.vue'

// ---- Workflow list ----
const workflows = ref([])
const loading = ref(false)
const currentWorkflow = ref(null)
const searchKey = ref('')
const datasources = ref([])

const filteredWorkflows = computed(() => {
  if (!searchKey.value) return workflows.value
  const kw = searchKey.value.toLowerCase()
  return workflows.value.filter(w => w.name?.toLowerCase().includes(kw))
})

// ---- DAG state ----
const dagNodes = ref([])
const dagEdges = ref([])
const dagDirty = ref(false)
const dagChangeData = ref(null)
const saving = ref(false)
const dagEditorRef = ref(null)

// ---- Dialog ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogSubmitting = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', description: '' })
const rules = {
  name: [{ required: true, message: '请输入作业流名称', trigger: 'blur' }]
}

// ---- Schedule ----
const scheduleVisible = ref(false)
const scheduleEnabled = ref(false)
const scheduleLoading = ref(false)
const cronExpression = ref('')

// ---- Execution ----
const executing = ref(false)
const historyVisible = ref(false)
const historyLoading = ref(false)
const executions = ref([])

// ---- Status helpers ----
function statusTag(status) {
  return status === 'ONLINE' ? 'success' : 'info'
}
function execStatusTag(status) {
  const map = { RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', ABORTED: 'info', SKIPPED: 'info' }
  return map[status] || 'info'
}

// ---- Fetch ----
onMounted(async () => {
  await fetchWorkflows()
  await fetchDatasources()
})

async function fetchWorkflows() {
  loading.value = true
  try {
    workflows.value = await workflowApi.list() || []
  } catch (e) {
    ElMessage.error('获取作业流列表失败')
  } finally { loading.value = false }
}

async function fetchDatasources() {
  try {
    datasources.value = await datasourceApi.list() || []
  } catch (e) { /* ignore */ }
}

// ---- Select workflow ----
async function selectWorkflow(wf) {
  currentWorkflow.value = wf
  try {
    const detail = await workflowApi.detail(wf.id)
    currentWorkflow.value = detail
    dagNodes.value = detail.nodes || []
    dagEdges.value = detail.edges || []
    dagDirty.value = false
    scheduleEnabled.value = detail.status === 'ONLINE'
    cronExpression.value = detail.cronExpression || ''
  } catch (e) {
    ElMessage.error('加载作业流详情失败')
  }
}

async function reloadWorkflow() {
  if (currentWorkflow.value) {
    await selectWorkflow(currentWorkflow.value)
  }
}

// ---- DAG changes ----
function onDagChange({ nodes, edges }) {
  dagChangeData.value = { nodes, edges }
  dagDirty.value = true
}

async function saveDag() {
  if (!currentWorkflow.value || !dagChangeData.value) return
  saving.value = true
  try {
    const payload = {
      name: currentWorkflow.value.name,
      description: currentWorkflow.value.description,
      nodes: dagChangeData.value.nodes,
      edges: dagChangeData.value.edges
    }
    await workflowApi.update(currentWorkflow.value.id, payload)
    dagDirty.value = false
    dagChangeData.value = null
    ElMessage.success('DAG 保存成功')
    await reloadWorkflow()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally { saving.value = false }
}

// ---- CRUD ----
function openCreateDialog() {
  isEdit.value = false
  form.name = ''
  form.description = ''
  formRef.value?.resetFields()
  dialogVisible.value = true
}

function openEditDialog() {
  if (!currentWorkflow.value) return
  isEdit.value = true
  form.name = currentWorkflow.value.name
  form.description = currentWorkflow.value.description || ''
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialogSubmitting.value = true
  try {
    if (isEdit.value) {
      await workflowApi.update(currentWorkflow.value.id, { name: form.name, description: form.description, nodes: dagNodes.value, edges: dagEdges.value })
      ElMessage.success('作业流已更新')
    } else {
      await workflowApi.create({ name: form.name, description: form.description, nodes: [], edges: [] })
      ElMessage.success('作业流已创建')
    }
    dialogVisible.value = false
    await fetchWorkflows()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally { dialogSubmitting.value = false }
}

async function handleDeleteWorkflow() {
  if (!currentWorkflow.value) return
  try {
    await ElMessageBox.confirm('确定删除该作业流？', '确认', { type: 'warning' })
    await workflowApi.delete(currentWorkflow.value.id)
    ElMessage.success('已删除')
    currentWorkflow.value = null
    dagNodes.value = []
    dagEdges.value = []
    dagDirty.value = false
    await fetchWorkflows()
  } catch (e) { /* cancelled */ }
}

// ---- Execute ----
async function handleExecute() {
  if (!currentWorkflow.value) return
  executing.value = true
  try {
    await workflowApi.execute(currentWorkflow.value.id)
    ElMessage.success('作业流已触发执行')
  } catch (e) {
    ElMessage.error('执行失败')
  } finally { executing.value = false }
}

async function handleSchedule() {
  if (!currentWorkflow.value) return
  scheduleLoading.value = true
  try {
    if (scheduleEnabled.value) {
      await workflowApi.schedule(currentWorkflow.value.id, { enabled: 1, cronExpression: cronExpression.value })
      ElMessage.success('调度已启用')
    } else {
      await workflowApi.disableSchedule(currentWorkflow.value.id)
      ElMessage.success('调度已停用')
    }
    await reloadWorkflow()
  } catch (e) {
    ElMessage.error('调度配置失败')
    scheduleEnabled.value = !scheduleEnabled.value
  } finally { scheduleLoading.value = false }
}

async function showExecDetail(row) {
  // TODO: show execution detail with node logs
  ElMessage.info('执行ID: ' + row.executeId)
}
</script>

<style scoped>
.workflow-page {
  display: flex;
  height: calc(100vh - 60px);
  gap: 0;
  margin: -20px -24px;
}

/* Sidebar */
.workflow-sidebar {
  width: 240px;
  min-width: 240px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-bottom: 1px solid #e5e7eb;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.sidebar-search {
  padding: 8px 12px;
}
.workflow-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}
.workflow-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background .15s;
}
.workflow-item:hover { background: #f3f4f6; }
.workflow-item.active {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}
.wf-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}
.wf-meta { display: flex; gap: 4px; }

/* Main */
.workflow-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafbfc;
}
.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.header-info { display: flex; flex-direction: column; gap: 2px; }
.wf-title { margin: 0; font-size: 16px; font-weight: 600; color: #1f2937; }
.wf-desc { font-size: 12px; color: #9ca3af; }
.header-actions { display: flex; gap: 8px; }
.dag-and-monitor {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}
.dag-wrapper {
  flex: 1;
  padding: 12px;
  overflow: hidden;
  min-width: 0;
}
.save-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 16px;
  background: #fffbeb;
  border-top: 1px solid #fde68a;
}
.no-workflow {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>