<template>
  <div class="scheduler-page">
    <div class="page-header">
      <h2 class="page-title">调度管理</h2>
      <div class="page-header-actions">
        <el-button @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="工作流名称" min-width="160" />
        <el-table-column prop="id" label="工作流ID" min-width="200" show-overflow-tooltip />
        <el-table-column label="调度状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.scheduleStatus ? 'success' : 'info'" effect="dark">
              {{ row.scheduleStatus ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron 表达式" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.cronExpression || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleViewExecutions(row)">
              <el-icon><Clock /></el-icon>
              执行历史
            </el-button>
            <el-button
              size="small"
              :type="row.scheduleStatus ? 'warning' : 'success'"
              link
              @click="handleToggleSchedule(row)"
            >
              <el-icon>
                <VideoPause v-if="row.scheduleStatus" />
                <VideoPlay v-else />
              </el-icon>
              {{ row.scheduleStatus ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </div>

    <!-- 执行历史弹窗 -->
    <el-dialog
      v-model="execDialogVisible"
      title="执行历史"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-table :data="executions" v-loading="execLoading" stripe max-height="400">
        <el-table-column prop="executionId" label="执行ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="execStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160" />
        <el-table-column prop="endTime" label="结束时间" min-width="160" />
      </el-table>
    </el-dialog>

    <!-- 设置调度弹窗 -->
    <el-dialog
      v-model="scheduleDialogVisible"
      title="设置调度"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="scheduleFormRef" :model="scheduleForm" label-width="110px" label-position="right">
        <el-form-item label="Cron 表达式" prop="cronExpression">
          <el-input v-model="scheduleForm.cronExpression" placeholder="例如：0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="快捷设置">
          <div class="cron-presets">
            <el-tag
              v-for="preset in cronPresets"
              :key="preset.label"
              :type="scheduleForm.cronExpression === preset.value ? 'primary' : 'info'"
              effect="plain"
              class="cron-preset-tag"
              @click="scheduleForm.cronExpression = preset.value"
            >
              {{ preset.label }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scheduleSubmitting" @click="handleScheduleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Clock, VideoPause, VideoPlay, Refresh } from '@element-plus/icons-vue'
import { workflowApi } from '@/api/modules/workflow'
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

// ---- 执行历史弹窗 ----
const execDialogVisible = ref(false)
const executions = ref([])
const execLoading = ref(false)

// ---- 调度弹窗 ----
const scheduleDialogVisible = ref(false)
const scheduleSubmitting = ref(false)
const scheduleFormRef = ref(null)
const scheduleTargetId = ref(null)
const scheduleForm = ref({ cronExpression: '' })

const cronPresets = [
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每天 8 点', value: '0 0 8 * * ?' },
  { label: '每周一 0 点', value: '0 0 0 ? * MON' },
  { label: '每月 1 号 0 点', value: '0 0 0 1 * ?' }
]

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    const workflows = await workflowApi.list()
    tableData.value = Array.isArray(workflows) ? workflows : []
  } catch (err) {
    ElMessage.error('加载调度列表失败')
  } finally {
    loading.value = false
  }
}

// ---- 查看执行历史 ----
async function handleViewExecutions(row) {
  execDialogVisible.value = true
  execLoading.value = true
  try {
    const data = await workflowApi.executions(row.id)
    executions.value = Array.isArray(data) ? data : []
  } catch {
    ElMessage.error('加载执行历史失败')
  } finally {
    execLoading.value = false
  }
}

// ---- 启用/停用调度 ----
async function handleToggleSchedule(row) {
  if (row.scheduleStatus) {
    // 停用
    try {
      await workflowApi.disableSchedule(row.id)
      ElMessage.success('调度已停用')
      await fetchData()
    } catch {
      ElMessage.error('停用调度失败')
    }
  } else {
    // 启用 - 打开设置弹窗
    scheduleTargetId.value = row.id
    scheduleForm.value.cronExpression = row.cronExpression || ''
    scheduleDialogVisible.value = true
  }
}

async function handleScheduleSubmit() {
  scheduleSubmitting.value = true
  try {
    await workflowApi.schedule(scheduleTargetId.value, {
      enabled: 1,
      cronExpression: scheduleForm.value.cronExpression
    })
    ElMessage.success('调度已启用')
    scheduleDialogVisible.value = false
    await fetchData()
  } catch {
    ElMessage.error('启用调度失败')
  } finally {
    scheduleSubmitting.value = false
  }
}

function execStatusTag(status) {
  const map = { SUCCESS: 'success', RUNNING: 'warning', FAILED: 'danger', PENDING: 'info' }
  return map[status] || 'info'
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.scheduler-page {
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

.page-header-actions {
  display: flex;
  gap: 8px;
}

.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cron-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cron-preset-tag {
  cursor: pointer;
  user-select: none;
}
</style>