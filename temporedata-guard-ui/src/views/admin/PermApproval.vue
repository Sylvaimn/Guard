<template>
  <div class="pa-page">
    <el-card shadow="never" class="apply-card">
      <template #header>
        <div class="apply-head">
          <span class="apply-title">数据申请审批（用户端发起）</span>
          <el-button size="small" @click="loadApplies">刷新</el-button>
        </div>
      </template>
      <el-table :data="applies" v-loading="applyLoading" stripe empty-text="暂无待审批申请">
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="target" label="数据对象" min-width="150" />
        <el-table-column prop="applicant" label="申请人" width="100" />
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column prop="project" label="专案" width="120" />
        <el-table-column prop="time" label="申请时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link @click="decide(row, 'approve')">通过</el-button>
            <el-button size="small" type="danger" link @click="decide(row, 'reject')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <div class="approval-tabs">
    <el-radio-group v-model="typeFilter" size="small">
      <el-radio-button label="all">全部</el-radio-button>
      <el-radio-button label="PERM">临时权限</el-radio-button>
      <el-radio-button label="EXPORT">导出审批</el-radio-button>
      <el-radio-button label="QUOTA">配额扩容</el-radio-button>
      <el-radio-button label="MODEL">模型调整</el-radio-button>
      <el-radio-button label="CROSS">跨单位协查</el-radio-button>
      <el-radio-button label="ROLLBACK">回滚申请</el-radio-button>
    </el-radio-group>
  </div>
  <SearchTable
    title="权限审批"
    :loading="table.loading"
    :total="table.total"
    v-model:current-page="table.currentPage"
    v-model:page-size="table.pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="filteredData" v-loading="table.loading" stripe style="width: 100%">
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="resourceName" label="资源名称" min-width="150" />
        <el-table-column label="审批类型" width="130">
          <template #default="{ row }">
            <el-tag :type="approvalTypeTag(approvalType(row))" effect="light">{{ approvalTypeLabel(approvalType(row)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessType" label="权限类型" width="100">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.accessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请事由" min-width="200" />
        <el-table-column label="期限/到期" width="140">
          <template #default="{ row }">
            <span v-if="row.expireTime" :style="{ color: isExpiring(row) ? '#d97706' : '#6b7280' }">{{ row.expireTime }}</span>
            <span v-else style="color:#c3c8d1">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" link @click="handleApprove(row)">
                <el-icon><Select /></el-icon>通过
              </el-button>
              <el-button size="small" type="danger" link @click="handleReject(row)">
                <el-icon><CloseBold /></el-icon>拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { userApi } from '@/api/modules/user'
import { permapprovalApi } from '@/api/modules/permapproval'
import { useTable } from '@/composables/useTable'
import { ElMessage } from 'element-plus'
import SearchTable from '@/components/common/SearchTable.vue'
import { Select, CloseBold } from '@element-plus/icons-vue'

const table = useTable(() => permapprovalApi.pending())
const typeFilter = ref('all')
const filteredData = computed(() => {
  const rows = table.pagedData.value || []
  if (typeFilter.value === 'all') return rows
  return rows.filter(r => approvalType(r) === typeFilter.value)
})

// 统一审批类型识别(临时权限/导出/扩容/模型调整/协查/回滚)
function approvalType(row) {
  const t = ((row.accessType || '') + '|' + (row.resourceName || '') + '|' + (row.reason || '')).toUpperCase()
  if (/EXPORT|导出|报表/.test(t)) return 'EXPORT'
  if (/QUOTA|扩容/.test(t)) return 'QUOTA'
  if (/MODEL|模型/.test(t)) return 'MODEL'
  if (/协查|CROSS|跨单位/.test(t)) return 'CROSS'
  if (/ROLLBACK|回滚/.test(t)) return 'ROLLBACK'
  return 'PERM'
}
const approvalTypeLabel = t => ({ PERM: '临时权限', EXPORT: '导出审批', QUOTA: '配额扩容', MODEL: '模型调整', CROSS: '跨单位协查', ROLLBACK: '回滚申请' }[t] || t)
const approvalTypeTag = t => ({ PERM: 'primary', EXPORT: 'warning', QUOTA: 'danger', MODEL: 'success', CROSS: 'info', ROLLBACK: 'danger' }[t] || 'info')
const statusLabel = v => ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }[v] || v)
function isExpiring(row) {
  if (!row.expireTime) return false
  const days = Math.ceil((new Date(row.expireTime).getTime() - Date.now()) / 864e5)
  return days <= 7 && days >= 0
}
function statusTag(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

async function handleApprove(row) {
  try {
    await permapprovalApi.approve(row.id)
    ElMessage.success('已通过审批（限时权限自动生效，到期自动回收）')
    await table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function handleReject(row) {
  try {
    await permapprovalApi.reject(row.id)
    ElMessage.success('已拒绝审批')
    await table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => { table.fetch() })
const applies = ref([])
const applyLoading = ref(false)

async function loadApplies() {
  applyLoading.value = true
  try {
    applies.value = await userApi.pendingApplies() || []
  } catch (e) { /* ignore */ } finally { applyLoading.value = false }
}

async function decide(row, action) {
  try {
    if (action === 'approve') await userApi.approveApply(row.id)
    else await userApi.rejectApply(row.id)
    ElMessage.success(action === 'approve' ? '已通过' : '已驳回')
    loadApplies()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

onMounted(loadApplies)
</script>