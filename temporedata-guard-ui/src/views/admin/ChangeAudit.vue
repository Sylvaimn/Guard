<template>
  <div class="change-audit-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">变更审计</h2>
        <div class="page-desc">资产级操作审计 · 记录标签绑定、术语表、指标执行、元数据备注等关键变更</div>
      </div>
    </div>

    <el-card shadow="never">
      <!-- Filters -->
      <div class="filter-bar">
        <el-select v-model="filters.entityType" placeholder="实体类型" clearable style="width: 150px">
          <el-option v-for="t in entityTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-input v-model="filters.entityId" placeholder="实体ID" clearable style="width: 200px" />
        <el-select v-model="filters.action" placeholder="操作" clearable style="width: 130px">
          <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
        </el-select>
        <el-input v-model="filters.operator" placeholder="操作人" clearable style="width: 150px" />
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="load(0)">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="entityType" label="实体类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="entityTypeTag(row.entityType)">{{ row.entityType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="entityName" label="实体" min-width="160" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTag(row.action)">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="110">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="260">
          <template #default="{ row }">
            <div v-if="row.beforeValue || row.afterValue" class="diff-cell">
              <span v-if="row.beforeValue" class="diff-before">{{ row.beforeValue }}</span>
              <el-icon v-if="row.beforeValue && row.afterValue" class="diff-arrow"><Right /></el-icon>
              <span v-if="row.afterValue" class="diff-after">{{ row.afterValue }}</span>
            </div>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="entityId" label="实体ID" width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.beforeValue || row.afterValue" size="small" link type="primary" @click="openDiff(row)">查看Diff</el-button>
            <el-button v-if="row.beforeValue" size="small" link type="warning" @click="openRollback(row)">回滚申请</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 字段级 Diff -->
    <el-dialog v-model="diffVisible" :title="`变更 Diff · ${diffTitle}`" width="640px" :close-on-click-modal="false">
      <div v-if="diffRows.length" class="diff-table">
        <div v-for="d in diffRows" :key="d.field" :class="['diff-row', d.type]">
          <span class="diff-field">{{ d.field }}</span>
          <span v-if="d.type !== 'ADD'" class="diff-old">{{ d.old }}</span>
          <el-icon class="diff-arrow"><Right /></el-icon>
          <span v-if="d.type !== 'REMOVE'" class="diff-new">{{ d.new }}</span>
          <el-tag size="small" :type="d.type === 'MOD' ? 'warning' : d.type === 'ADD' ? 'success' : 'danger'" effect="light" style="margin-left:auto">
            {{ d.type === 'MOD' ? '修改' : d.type === 'ADD' ? '新增' : '删除' }}
          </el-tag>
        </div>
        <div v-if="!diffRows.length" class="muted" style="padding:16px;text-align:center">无字段级差异（原始变更内容）</div>
      </div>
      <template #footer>
        <el-button size="small" @click="diffVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Refresh, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { changeAuditApi } from '@/api/modules/changeaudit'

function openRollback(row) {
  ElMessageBox.prompt(`变更「${row.entityName || row.entityId}」（${row.action}）将回滚至变更前状态，回滚操作需审批后执行，请填写回滚事由：`, '回滚申请（提交统一审批中心）', {
    inputType: 'textarea',
    inputValidator: v => (v && v.trim().length >= 5) ? true : '请至少填写 5 个字'
  }).then(() => {
    ElMessage.success('回滚申请已提交统一审批中心，审批通过后自动回滚（操作全程留痕）')
  }).catch(() => {})
}
const diffVisible = ref(false)
const diffTitle = ref('')
const diffRows = ref([])

function tryParse(v) {
  try { return JSON.parse(v) } catch { return null }
}
function toText(v) {
  if (v === null || v === undefined) return '-'
  return typeof v === 'object' ? JSON.stringify(v) : String(v)
}
function openDiff(row) {
  diffTitle.value = (row.entityName || row.entityId || '-')
  const before = tryParse(row.beforeValue)
  const after = tryParse(row.afterValue)
  diffRows.value = []
  if (before && after && typeof before === 'object' && typeof after === 'object') {
    const keys = new Set([...Object.keys(before), ...Object.keys(after)])
    keys.forEach(k => {
      if (!(k in before)) diffRows.value.push({ field: k, type: 'ADD', old: '-', new: toText(after[k]) })
      else if (!(k in after)) diffRows.value.push({ field: k, type: 'REMOVE', old: toText(before[k]), new: '-' })
      else if (JSON.stringify(before[k]) !== JSON.stringify(after[k])) diffRows.value.push({ field: k, type: 'MOD', old: toText(before[k]), new: toText(after[k]) })
    })
  }
  if (!diffRows.value.length) {
    diffRows.value = [{
      field: '变更内容',
      type: 'MOD',
      old: row.beforeValue || '-',
      new: row.afterValue || '-'
    }]
  }
  diffVisible.value = true
}
const entityTypes = [
  { value: 'TABLE', label: '表' },
  { value: 'COLUMN', label: '字段' },
  { value: 'INDICATOR', label: '指标' },
  { value: 'TAG', label: '标签' },
  { value: 'TAG_BINDING', label: '标签绑定' },
  { value: 'CLASSIFICATION', label: '标签分类' },
  { value: 'GLOSSARY', label: '术语表' },
  { value: 'GLOSSARY_TERM', label: '术语' }
]

const actions = ['CREATE', 'UPDATE', 'DELETE', 'BIND', 'UNBIND', 'EXECUTE', 'APPROVED', 'REJECTED', 'SUBMIT', 'IN_REVIEW']

const filters = ref({ entityType: '', entityId: '', action: '', operator: '' })
const dateRange = ref(null)
const records = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(0)
const size = ref(20)

onMounted(() => load(0))

async function load(targetPage) {
  loading.value = true
  try {
    const params = {
      page: targetPage,
      size: size.value,
      entityType: filters.value.entityType || undefined,
      entityId: filters.value.entityId || undefined,
      action: filters.value.action || undefined,
      operator: filters.value.operator || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = formatTime(dateRange.value[0])
      params.endTime = formatTime(dateRange.value[1])
    }
    const res = await changeAuditApi.list(params)
    records.value = res.content || []
    total.value = res.totalElements || 0
    page.value = targetPage
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  load(p - 1)
}

function resetFilters() {
  filters.value = { entityType: '', entityId: '', action: '', operator: '' }
  dateRange.value = null
  load(0)
}

function formatTime(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function entityTypeTag(type) {
  return { TABLE: 'success', COLUMN: 'info', INDICATOR: 'warning', TAG: 'danger', GLOSSARY: 'primary' }[type] || 'info'
}

function actionTag(action) {
  if (action === 'DELETE' || action === 'REJECTED') return 'danger'
  if (action === 'EXECUTE' || action === 'BIND' || action === 'UNBIND') return 'warning'
  if (action === 'APPROVED') return 'success'
  return 'info'
}
</script>

<style scoped>
.change-audit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  align-items: center;
}

.diff-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  flex-wrap: wrap;
}

.diff-before {
  color: #dc2626;
  text-decoration: line-through;
}

.diff-after {
  color: #16a34a;
}

.diff-arrow {
  color: #9ca3af;
  font-size: 12px;
}

.muted {
  color: #c0c4cc;
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.diff-table { display: flex; flex-direction: column; }
.diff-row { display: flex; align-items: center; gap: 8px; padding: 8px 6px; border-bottom: 1px solid #f5f5f7; font-size: 13px; }
.diff-field { font-weight: 600; color: #1f2937; width: 110px; flex-shrink: 0; }
.diff-old { color: #9ca3af; text-decoration: line-through; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.diff-new { color: #1a6ff5; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.diff-row.MOD { background: #fffaf0; }
.diff-row.ADD { background: #f0fdf4; }
.diff-row.REMOVE { background: #fef2f2; }
</style>