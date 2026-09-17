<template>
  <div class="apilog-page">
    <div class="page-header">
      <h2 class="page-title">接口调用审计</h2>
      <span class="page-sub">每一次数据调用可追溯：调用方、数据范围、脱敏状态、条数与耗时</span>
    </div>

    <!-- 统计卡 -->
    <div class="audit-stats">
      <div class="audit-stat-card">
        <div class="as-num">{{ stats.total }}</div><div class="as-label">今日调用</div>
      </div>
      <div class="audit-stat-card danger">
        <div class="as-num">{{ stats.failed }}</div><div class="as-label">失败调用</div>
      </div>
      <div class="audit-stat-card warn">
        <div class="as-num">{{ stats.timeout }}</div><div class="as-label">超时调用(>3s)</div>
      </div>
      <div class="audit-stat-card alert">
        <div class="as-num">{{ stats.abnormal }}</div><div class="as-label">异常调用</div>
      </div>
      <div class="audit-actions">
        <el-button size="small" type="primary" plain @click="exportAudit"><el-icon><Download /></el-icon>审计导出(CSV)</el-button>
      </div>
    </div>
    <div class="retention-tip">合规保留：调用审计数据按系统设置保留期限存储（默认 365 天），仅审计员可导出，不可删除。</div>

    <SearchTable
      title="调用记录"
      :loading="table.loading"
      :total="table.total"
      v-model:current-page="table.currentPage"
      v-model:page-size="table.pageSize"
      :show-add="false"
    >
      <template #default>
        <el-table :data="table.pagedData" v-loading="table.loading" stripe style="width: 100%" :row-class-name="rowClass">
          <el-table-column prop="apiName" label="接口名称" min-width="150" />
          <el-table-column prop="apiPath" label="路径" min-width="200" show-overflow-tooltip />
          <el-table-column prop="method" label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodTag(row.method)" effect="light">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="调用方" width="130">
            <template #default="{ row }">
              <span>{{ row.callerId || '匿名' }}</span>
              <div class="caller-ip">{{ row.requestIp }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="responseCode" label="响应码" width="100">
            <template #default="{ row }">
              <el-tag :type="row.responseCode === 200 ? 'success' : 'danger'" effect="light">{{ row.responseCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: (row.costTime || 0) > 3000 ? '#dc2626' : '#374151' }">{{ row.costTime || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="异常标注" width="160" align="center">
            <template #default="{ row }">
              <el-tag v-for="a in anomalyOf(row)" :key="a" size="small" :type="anomalyTag(a)" effect="light" style="margin:2px">{{ anomalyLabel(a) }}</el-tag>
              <span v-if="!anomalyOf(row).length" style="color:#c3c8d1">正常</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160" />
          <el-table-column label="操作" width="110" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="anomalyOf(row).length" size="small" link type="warning" @click="handleAlert(row)">告警联动</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </SearchTable>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { apilogApi } from '@/api/modules/apilog'
import { useTable } from '@/composables/useTable'
import SearchTable from '@/components/common/SearchTable.vue'
import { ElMessage } from 'element-plus'

const table = useTable(() => apilogApi.list())

function methodTag(method) {
  const map = { GET: 'success', POST: '', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

// 异常识别规则
function anomalyOf(row) {
  const tags = []
  if (row.responseCode !== 200 && row.responseCode != null) tags.push('FAIL')
  if ((row.costTime || 0) > 3000) tags.push('SLOW')
  if (!row.callerId) tags.push('ANON')
  const size = (row.responseBody || '').length
  if (size > 10240) tags.push('HUGE')
  return tags
}
const anomalyLabel = t => ({ FAIL: '调用失败', SLOW: '超时', ANON: '匿名调用', HUGE: '大流量' }[t])
const anomalyTag = t => ({ FAIL: 'danger', SLOW: 'warning', ANON: 'warning', HUGE: 'danger' }[t])
function rowClass({ row }) {
  return anomalyOf(row).length ? 'anomaly-row' : ''
}
function handleAlert(row) {
  ElMessage.warning(`已推送布控告警：接口 ${row.apiName} 存在异常调用（${anomalyOf(row).map(anomalyLabel).join('、')}）`)
}

// 统计
const stats = computed(() => {
  const rows = table.pagedData.value || []
  let total = 0, failed = 0, timeout = 0, abnormal = 0
  rows.forEach(r => {
    total++
    if (r.responseCode !== 200 && r.responseCode != null) failed++
    if ((r.costTime || 0) > 3000) timeout++
    if (anomalyOf(r).length) abnormal++
  })
  return { total, failed, timeout, abnormal }
})

function exportAudit() {
  const rows = table.pagedData.value || []
  if (!rows.length) { ElMessage.info('当前无调用记录可导出'); return }
  const header = ['接口名称', '路径', '方法', '调用方', 'IP', '响应码', '耗时(ms)', '异常', '时间']
  const lines = rows.map(r => [
    r.apiName, r.apiPath, r.method, r.callerId || '匿名', r.requestIp,
    r.responseCode, r.costTime, anomalyOf(r).map(anomalyLabel).join('|') || '正常', r.createTime
  ].map(v => `"${(v ?? '').toString().replace(/"/g, '""')}"`).join(','))
  const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '接口调用审计_' + new Date().toISOString().slice(0, 10) + '.csv'
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success(`已导出 ${rows.length} 条审计记录（审计员操作留痕）`)
}

onMounted(() => { table.fetch() })
</script>

<style scoped>
.apilog-page { padding: 0; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-sub { font-size: 12.5px; color: #9ca3af; }
.audit-stats { display: flex; gap: 12px; margin-bottom: 14px; }
.audit-stat-card {
  flex: 1; border: 1px solid #eef1f5; border-radius: 4px; padding: 12px 16px;
  display: flex; align-items: center; gap: 10px; background: #fbfcff;
}
.audit-stat-card.danger { background: #fef5f5; border-color: #fde3e3; }
.audit-stat-card.warn { background: #fffaf0; border-color: #fdeece; }
.audit-stat-card.alert { background: #fef2f2; border-color: #fbd5d5; }
.as-num { font-size: 24px; font-weight: 700; color: #1a6ff5; }
.audit-stat-card.danger .as-num, .audit-stat-card.alert .as-num { color: #dc2626; }
.audit-stat-card.warn .as-num { color: #d97706; }
.as-label { font-size: 12.5px; color: #9ca3af; }
.caller-ip { font-size: 11px; color: #c3c8d1; }
:deep(.anomaly-row) { background: #fff7f7 !important; }
.audit-actions { display: flex; align-items: center; }
.retention-tip { font-size: 12px; color: #9ca3af; margin: 8px 0 14px; }
</style>
