<template>
  <div v-if="expiringCount > 0" class="expiring-banner">
    <el-icon :size="14" style="margin-right:6px"><WarningFilled /></el-icon>
    有 {{ expiringCount }} 项临时权限/专案抽调将在 7 天内到期，到期后自动回收，请提前办理续期或移交。
  </div>
  <SearchTable
    title="我的数据"
    :loading="table.loading"
    :total="table.total"
    v-model:current-page="table.currentPage"
    v-model:page-size="table.pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="table.pagedData" v-loading="table.loading" stripe style="width: 100%">
        <el-table-column prop="resourceName" label="资源名称" min-width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.resourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限来源" width="130">
          <template #default="{ row }">
            <el-tag :type="sourceTag(sourceOf(row))" effect="light" size="small">{{ sourceLabel(sourceOf(row)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessType" label="访问类型" width="100">
          <template #default="{ row }">
            <el-tag :type="accessTypeTag(row.accessType)" effect="light">{{ row.accessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="到期时间" width="160">
          <template #default="{ row }">
            <span v-if="row.expireTime" :style="{ color: expiryState(row).color, fontWeight: expiryState(row).weight }">
              {{ row.expireTime }}<el-icon v-if="expiryState(row).warn" :size="12" style="margin-left:2px"><WarningFilled /></el-icon>
            </span>
            <el-tag v-else size="small" type="info" effect="plain">长期有效</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="获取时间" width="160" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="showSource(row)">溯源</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { mydataApi } from '@/api/modules/mydata'
import { useTable } from '@/composables/useTable'
import SearchTable from '@/components/common/SearchTable.vue'
import { ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

const table = useTable(() => mydataApi.list())
const expiringCount = computed(() => (table.pagedData.value || []).filter(r => expiryState(r).warn).length)

function sourceOf(row) {
  const t = ((row.resourceType || '') + '|' + (row.accessType || '') + '|' + (row.remark || '')).toUpperCase()
  if (/TEMP|临时|EXPIR/.test(t)) return 'TEMP'
  if (/CASE|专案/.test(t)) return 'CASE_GROUP'
  if (/ROLE|角色/.test(t)) return 'ROLE'
  return 'ORG'
}
const sourceLabel = v => ({ ORG: '组织架构', ROLE: '角色分配', CASE_GROUP: '专案抽调', TEMP: '临时申请' }[v] || '组织架构')
const sourceTag = v => ({ ORG: 'info', ROLE: 'success', CASE_GROUP: 'warning', TEMP: 'danger' }[v] || 'info')
function expiryState(row) {
  if (!row.expireTime) return { color: '#9ca3af', warn: false }
  const days = Math.ceil((new Date(row.expireTime).getTime() - Date.now()) / 864e5)
  if (days < 0) return { color: '#dc2626', warn: true }
  if (days <= 7) return { color: '#d97706', warn: true }
  return { color: '#6b7280', warn: false }
}
function showSource(row) {
  ElMessageBox.alert(
    `权限来源链路：${sourceLabel(sourceOf(row))} → 成员挂靠 → 数据范围(本单位/专案) → 授予「${row.resourceName}」\n获取时间：${row.createTime || '-'}\n${row.expireTime ? '到期时间：' + row.expireTime + '（到期自动回收）' : '长期有效'}`,
    '权限来源溯源', { type: 'info' }
  )
}
function accessTypeTag(type) {
  const map = { OWNER: '', GRANTED: 'success', FAVORITE: 'warning' }
  return map[type] || 'info'
}

onMounted(() => { table.fetch() })
</script>