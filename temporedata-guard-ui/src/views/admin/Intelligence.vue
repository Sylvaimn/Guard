<template>
  <div class="intel-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Search /></el-icon>情报检索</h2>
      <p>实体联合检索（姓名 / 主标识 / 标签），行级密级过滤 · 主标识自动脱敏</p>
    </div>

    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入姓名 / 身份证号 / 车牌号 / 标签编码..."
        size="large"
        clearable
        style="width: 420px"
        @keyup.enter="doSearch"
      >
        <template #append>
          <el-button :icon="Search" :loading="loading" @click="doSearch">检索</el-button>
        </template>
      </el-input>
      <el-select v-model="labelFilter" placeholder="按标签筛选" clearable size="large" style="width: 200px">
        <el-option v-for="l in labelOptions" :key="l.value" :label="l.label" :value="l.value" />
      </el-select>
      <div v-if="results.length" class="result-count">共 {{ results.length }} 条结果</div>
    </div>

    <el-card shadow="never">
      <el-table :data="results" v-loading="loading" stripe empty-text="输入关键词检索实体情报">
        <el-table-column prop="displayName" label="实体名称" min-width="130">
          <template #default="{ row }">
            <span class="entity-name">{{ row.displayName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.entityType)" size="small">{{ typeLabel(row.entityType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="primaryKey" label="主标识（脱敏）" min-width="160">
          <template #default="{ row }">
            <code class="masked-key">{{ row.primaryKey || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="l in row.labels" :key="l.labelCode" size="small" effect="light" style="margin-right: 4px">
              {{ l.labelName }}
            </el-tag>
            <span v-if="!row.labels || !row.labels.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="来源系统" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="s in row.sourceSystems" :key="s" size="small" type="info" effect="plain" style="margin-right: 4px">{{ s }}</el-tag>
            <span v-if="!row.sourceSystems || !row.sourceSystems.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="densityTag(row.density)">{{ densityLabel(row.density) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="gotoGraph(row)">图谱分析</el-button>
            <el-button size="small" link type="warning" @click="gotoWatch(row)">布控</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { intelligenceApi } from '@/api/modules/intelligence'
import { ElMessage } from 'element-plus'

const router = useRouter()
const keyword = ref('')
const labelFilter = ref('')
const loading = ref(false)
const results = ref([])

const labelOptions = [
  { label: '吸毒前科', value: 'DRUG_HISTORY' },
  { label: '涉赌', value: 'GAMBLING' },
  { label: '涉恐', value: 'TERRORISM' },
  { label: '跨省频繁', value: 'FREQ_TRAVEL' },
  { label: '夜间活动', value: 'NIGHT_ACTIVITY' }
]

function typeTag(t) {
  return { PERSON: 'primary', VEHICLE: 'warning', CASE: 'danger', ITEM: 'info', ORG: 'success' }[t] || 'info'
}
function typeLabel(t) {
  return { PERSON: '人员', VEHICLE: '车辆', CASE: '案件', ITEM: '物品', ORG: '组织' }[t] || t
}
function densityLabel(d) {
  return { PUBLIC: '公开', INTERNAL: '内部', CONFIDENTIAL: '秘密', SECRET: '机密' }[d] || d
}
function densityTag(d) {
  return { PUBLIC: 'info', INTERNAL: '', CONFIDENTIAL: 'warning', SECRET: 'danger' }[d] || ''
}

async function doSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  loading.value = true
  try {
    const params = { keyword: keyword.value.trim() }
    if (labelFilter.value) params.labelCode = labelFilter.value
    results.value = await intelligenceApi.search(params)
  } catch (e) {
    ElMessage.error(e.message || '检索失败')
  } finally {
    loading.value = false
  }
}

function gotoGraph(row) {
  router.push({ path: '/entitygraph', query: { entityId: row.entityId, name: row.displayName } })
}

function gotoWatch(row) {
  router.push({ path: '/watchcenter', query: { entityId: row.entityId, name: row.displayName } })
}

onMounted(() => {})
</script>

<style scoped>
.intel-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.page-header h2 {
  font-size: 19px;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.page-header p {
  margin: 0;
  color: #9ca3af;
  font-size: 13px;
}
.header-icon {
  color: #0ea5e9;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.result-count {
  font-size: 13px;
  color: #6b7280;
}
.entity-name {
  font-weight: 600;
  color: #1f2937;
}
.masked-key {
  font-family: Menlo, Consolas, monospace;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #374151;
}
</style>
