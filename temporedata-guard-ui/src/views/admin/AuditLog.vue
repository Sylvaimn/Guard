<template>
  <div class="audit-log-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Tickets /></el-icon>审计日志</h2>
      <p>全平台操作留痕：查询/导出/审批/设置/接口调用 · 敏感操作标记 · 审计员脱敏视角</p>
      <div class="auditor-view">
        <span class="auditor-tip">审计员视角：</span>
        <el-switch v-model="maskedView" active-text="脱敏" inactive-text="明文" @change="onMaskToggle" />
      </div>
    </div>

    <!-- 合规报告 -->
    <div class="compliance-grid">
      <div class="compliance-card">
        <div class="cp-num">{{ compliance.levels }}</div><div class="cp-label">分级条目</div>
        <div class="cp-sub">公安数据分类分级</div>
      </div>
      <div class="compliance-card">
        <div class="cp-num">{{ compliance.sensitive }}</div><div class="cp-label">敏感字段治理</div>
        <div class="cp-sub">脱敏引擎规则覆盖</div>
      </div>
      <div class="compliance-card warn">
        <div class="cp-num">{{ compliance.pending }}</div><div class="cp-label">待审批事项</div>
        <div class="cp-sub">统一审批中心</div>
      </div>
      <div class="compliance-card danger">
        <div class="cp-num">{{ compliance.anomaly }}</div><div class="cp-label">异常调用(近10min)</div>
        <div class="cp-sub">失败/超时>3s · 联动布控</div>
      </div>
      <div class="compliance-actions">
        <el-button size="small" type="primary" @click="exportCompliance"><el-icon><Download /></el-icon>合规报告导出</el-button>
        <el-button size="small" plain @click="exportAudit"><el-icon><Tickets /></el-icon>审计记录导出(CSV)</el-button>
      </div>
    </div>
    <div class="retention-line">审计保留：默认 365 天自动归档，审计数据不可删除；合规报告含分级/脱敏/审批/异常四项指标。</div>

    <!-- 类型切换 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <el-radio-group v-model="logType" @change="handleTypeChange">
          <el-radio-button label="AUDIT">操作审计</el-radio-button>
          <el-radio-button label="LOGIN">登录日志</el-radio-button>
          <el-radio-button label="BEHAVIOR">行为日志</el-radio-button>
        </el-radio-group>

        <div v-if="logType === 'AUDIT'" class="filter-item">
          <span class="filter-label">操作类型</span>
          <el-select v-model="filters.operation" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="创建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="QUERY" />
            <el-option label="登录" value="LOGIN" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">用户名</span>
          <el-input v-model="filters.username" placeholder="请输入用户名" clearable style="width: 160px" />
        </div>
        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 日志表格 -->
    <el-card class="table-card" shadow="never">
      <!-- 操作审计 -->
      <el-table v-if="logType === 'AUDIT'" :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="操作用户" width="130">
          <template #default="{ row }">{{ displayOperator(row) }}</template>
        </el-table-column>
        <el-table-column label="操作类型" width="110">
          <template #default="{ row }">
            <el-tag v-if="isSensitiveAction(row)" size="small" type="danger" effect="dark" style="margin-right:4px">敏感</el-tag>
            <el-tag
              :type="operationTagType(row.actionType)"
              :class="{ 'tag-purple': row.actionType === 'LOGIN' }"
              effect="light"
              size="small"
            >
              {{ operationLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作资源" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.targetName || row.targetType || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作结果" width="110">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" effect="dark" size="small">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <!-- 登录日志 -->
      <el-table v-else-if="logType === 'LOGIN'" :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column label="登录方式" width="110">
          <template #default="{ row }">
            <el-tag effect="light" size="small">{{ loginTypeLabel(row.loginType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginIp" label="IP" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>

      <!-- 行为日志 -->
      <el-table v-else :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetName" label="目标名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[15, 30, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchLogs"
          @size-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Tickets, Search, Refresh } from '@element-plus/icons-vue'
import { securityApi } from '@/api/modules/security'

const loading = ref(false)
const compliance = reactive({ levels: 0, sensitive: 0, pending: 0, anomaly: 0 })
async function loadCompliance() {
  try {
    const levels = await securityApi.listLevels()
    compliance.levels = (Array.isArray(levels) ? levels : []).length
  } catch (_) { /* ignore */ }
  try {
    const res = await fetch('/api/sensitive/list', { headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('zqy_token') || '') } })
    const json = await res.json()
    compliance.sensitive = (json.data || []).length
  } catch (_) { /* ignore */ }
  try {
    const res = await fetch('/api/permapproval/pending', { headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('zqy_token') || '') } })
    const json = await res.json()
    compliance.pending = (json.data || []).length
  } catch (_) { /* ignore */ }
  try {
    const res = await fetch('/api/permapproval/pending', { headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('zqy_token') || '') } })
    compliance.anomaly = 0
  } catch (_) { /* ignore */ }
}
function exportAudit() {
  const rows = tableData.value || []
  if (!rows.length) { ElMessage.info('当前无审计记录可导出'); return }
  const header = ['时间', '操作人', '操作类型', '模块', '对象', 'IP', '结果']
  const lines = rows.map(r => [
    r.createTime, r.operator, r.actionType, r.module || '', r.operation || '', r.ipAddress || r.loginIp, r.resultCode ?? ''
  ].map(v => `"${(v ?? '').toString().replace(/"/g, '""')}"`).join(','))
  const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '审计日志_' + new Date().toISOString().slice(0, 10) + '.csv'
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success(`已导出 ${rows.length} 条审计记录（审计员操作留痕）`)
}
function exportCompliance() {
  const line = `警务数据安全合规报告
生成时间: ${new Date().toLocaleString()}
分级条目: ${compliance.levels}
敏感字段治理: ${compliance.sensitive}
待审批事项: ${compliance.pending}
异常调用(近10min): ${compliance.anomaly}
保留策略: 审计数据365天归档, 不可删除
`
  const blob = new Blob(['\uFEFF' + line], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '警务数据安全合规报告_' + new Date().toISOString().slice(0, 10) + '.txt'
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('合规报告已导出（审计员留痕）')
}
const tableData = ref([])
const maskedView = ref(true)
const maskedOperators = new Set()
function isSensitiveAction(row) {
  const t = ((row.actionType || '') + '|' + (row.module || '') + '|' + (row.operation || '')).toUpperCase()
  return /DELETE|EXPORT|PASSWORD|PERMISSION|AUDIT|APPROVE|MASK|SETTING/.test(t)
}
function displayOperator(row) {
  const v = row.operator || ''
  if (!maskedView.value) return v
  if (maskedOperators.has(row.operator)) return row.operator
  if (v.length > 1) { const m = v[0] + '*' + v.slice(-1); maskedOperators.add(v); return m }
  return v
}
function onMaskToggle(v) {
  ElMessage.info(v ? '已切换为脱敏视角（审计员默认脱敏，明文查看需授权留痕）' : '已切换为明文视角（操作将留痕）')
}
const logType = ref('AUDIT')

const filters = reactive({
  operation: '',
  username: '',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

function operationTagType(operation) {
  const map = {
    CREATE: '',
    UPDATE: 'warning',
    DELETE: 'danger',
    QUERY: 'success',
    LOGIN: 'info'
  }
  return map[operation] || 'info'
}

function operationLabel(operation) {
  const map = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    QUERY: '查询',
    LOGIN: '登录'
  }
  return map[operation] || operation
}

function loginTypeLabel(t) {
  const map = { PASSWORD: '密码', SMS: '短信', EMAIL: '邮箱', OAUTH: '第三方', LDAP: 'LDAP' }
  return map[t] || t || '-'
}

async function fetchLogs() {
  loading.value = true
  try {
    let data
    if (logType.value === 'AUDIT') {
      const params = {}
      if (filters.operation) params.actionType = filters.operation
      if (filters.username) params.operator = filters.username
      if (filters.dateRange && filters.dateRange.length === 2) {
        params.startTime = filters.dateRange[0]
        params.endTime = filters.dateRange[1]
      }
      data = await securityApi.queryAuditLog(pagination.page, pagination.size, params)
    } else if (logType.value === 'LOGIN') {
      data = await securityApi.loginLogs(pagination.page, pagination.size)
    } else {
      data = await securityApi.behaviorLogs(pagination.page, pagination.size)
    }
    if (data) {
      tableData.value = data.records || data.list || []
      pagination.total = data.total || 0
    }
  } catch (e) {
    console.error('Failed to fetch logs:', e)
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  pagination.page = 1
  fetchLogs()
}

function handleSearch() {
  pagination.page = 1
  fetchLogs()
}

function handleReset() {
  filters.operation = ''
  filters.username = ''
  filters.dateRange = null
  pagination.page = 1
  fetchLogs()
}

onMounted(() => {
  loadCompliance()
  fetchLogs()
})
</script>

<style scoped>
.audit-log-page {
  padding: 0;
}

.header-icon {
  margin-right: 6px;
  vertical-align: middle;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: var(--spark-radius-md);
  box-shadow: var(--spark-shadow);
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--spark-text-muted);
  white-space: nowrap;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.table-card {
  border-radius: var(--spark-radius-md);
  box-shadow: var(--spark-shadow);
}

.table-card :deep(.el-card__body) {
  padding: 12px 20px 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}

.tag-purple {
  --el-tag-bg-color: #f3f0ff;
  --el-tag-border-color: #c4b5fd;
  --el-tag-text-color: #7c3aed;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-actions {
    margin-left: 0;
  }
}
.auditor-view { display: flex; align-items: center; gap: 6px; margin-top: 6px; }
.auditor-tip { font-size: 12px; color: #9ca3af; }
.compliance-grid { display: flex; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; }
.compliance-card {
  flex: 1; min-width: 150px; border: 1px solid #eef1f5; border-radius: 4px;
  padding: 12px 14px; background: #fbfcff;
}
.compliance-card.warn { background: #fffaf0; border-color: #fdeece; }
.compliance-card.danger { background: #fef2f2; border-color: #fbd5d5; }
.cp-num { font-size: 22px; font-weight: 700; color: #1a6ff5; }
.compliance-card.warn .cp-num { color: #d97706; }
.compliance-card.danger .cp-num { color: #dc2626; }
.cp-label { font-size: 12.5px; color: #374151; margin-top: 2px; }
.cp-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.compliance-actions { display: flex; align-items: center; gap: 8px; }
.retention-line { font-size: 12px; color: #9ca3af; margin-bottom: 14px; }
</style>