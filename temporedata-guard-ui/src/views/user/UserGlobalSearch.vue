<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">跨对象统一检索</h2>
        <p class="page-desc">搜索 人员/车辆/案件/线索/数据/API/专案，结果聚合 + 权限/密级过滤</p>
      </div>
            <div class="page-actions"><el-button type="primary" :icon="Search" @click="onAction('search')">搜索</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-input v-model="keyword" size="large" placeholder="输入关键词，如：张某某 / 138****8888 / 陕A*****" clearable :prefix-icon="Search" @keyup.enter="doSearch" />
      <div class="scope-tags">
        <el-tag v-for="s in scopes" :key="s" effect="plain" size="small" style="margin-right:6px">{{ s }}</el-tag>
        <el-button size="small" type="primary" :loading="loading" @click="doSearch" style="margin-left: 8px">搜索</el-button>
      </div>
      <template v-if="searched">
        <div v-for="(group, name) in results" :key="name" class="result-group">
          <div class="group-title">{{ groupLabel(name) }}</div>
          <div v-for="(item, i) in group" :key="i" class="result-item">
            <span class="ri-name">{{ item.name }}</span>
            <el-tag v-if="item.level" size="small" effect="light">{{ item.level }}</el-tag>
            <span v-if="item.type" class="ri-type">{{ item.type }}</span>
            <span v-if="item.source" class="ri-source">{{ item.source }}</span>
          </div>
          <el-empty v-if="!group.length" description="无结果" :image-size="40" />
        </div>
      </template>
      <el-empty v-else description="输入关键词后展示聚合结果（人员/车辆/案件/线索/数据，含密级与脱敏）" :image-size="80" />
      <div v-if="keyword.trim()" class="agent-call">
        <el-button type="primary" plain :icon="MagicStick" @click="askAgent">让金盾智云 Agent 分析「{{ keyword }}」</el-button>
      </div>
    </el-card>  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, MagicStick } from '@element-plus/icons-vue'
import { userApi } from '@/api/modules/user'

const keyword = ref('')
const router = useRouter()

function askAgent() {
  router.push({ path: '/workbench/agent', query: { q: keyword.value.trim() } })
}
const loading = ref(false)
const searched = ref(false)
const results = ref({})
const scopes = ['人员', '车辆', '案件', '线索', '数据', 'API', '专案']

const groupLabels = { entities: '人员/对象', data: '数据资产', apis: 'API 服务', projects: '专案' }
function groupLabel(name) { return groupLabels[name] || name }

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  searched.value = true
  try {
    results.value = await userApi.search(keyword.value.trim()) || {}
  } catch (e) {
    ElMessage.error(e.message || '搜索失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.user-page { max-width: 1200px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 12px; color: #8492a6; margin-top: 4px; }
.page-actions { display: flex; gap: 8px; }
.perm-alert { margin-bottom: 16px; }
.page-card { border-radius: 8px; margin-bottom: 16px; }
</style>
