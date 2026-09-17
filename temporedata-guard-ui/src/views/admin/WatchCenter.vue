<template>
  <div class="watch-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Bell /></el-icon>布控预警中心</h2>
      <p>布控名单管理 · 实时预警 · 签收处置 · 战果闭环</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in statItems" :key="s.label">
        <span class="stat-value" :style="{ color: s.color }">{{ stats[s.key] ?? 0 }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 布控名单 -->
      <el-tab-pane label="布控名单" name="lists">
        <div class="toolbar">
          <el-radio-group v-model="listFilter" size="small" @change="fetchLists">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="ACTIVE">生效中</el-radio-button>
            <el-radio-button value="REVOKED">已撤销</el-radio-button>
          </el-radio-group>
          <el-button type="primary" :icon="Plus" @click="openListDialog">新增布控</el-button>
        </div>
        <el-card shadow="never">
          <el-table :data="lists" v-loading="listsLoading" stripe>
            <el-table-column prop="targetName" label="布控对象" min-width="130" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.entityType === 'PERSON' ? 'primary' : 'warning'" size="small">{{ row.entityType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="listGroup" label="分组" width="80" />
            <el-table-column label="级别" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.level === 1 ? 'danger' : row.level === 2 ? 'warning' : 'info'" size="small">L{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="布控原因" min-width="160" show-overflow-tooltip />
            <el-table-column label="有效期" min-width="190">
              <template #default="{ row }">{{ row.effectiveFrom ? row.effectiveFrom.slice(0, 10) : '即刻' }} ~ {{ row.effectiveTo ? row.effectiveTo.slice(0, 10) : '长期' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="light">
                  {{ { ACTIVE: '生效中', REVOKED: '已撤销', EXPIRED: '已过期' }[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 'ACTIVE'" size="small" link type="danger" @click="revokeList(row)">撤销</el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 预警处置 -->
      <el-tab-pane label="预警处置" name="alerts">
        <div class="toolbar">
          <el-radio-group v-model="alertFilter" size="small" @change="fetchAlerts">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待签收</el-radio-button>
            <el-radio-button value="PROCESSING">处置中</el-radio-button>
            <el-radio-button value="CLOSED">已办结</el-radio-button>
          </el-radio-group>
          <el-button :icon="RefreshRight" size="small" @click="fetchAlerts">刷新</el-button>
        </div>
        <el-card shadow="never">
          <el-table :data="alerts" v-loading="alertsLoading" stripe>
            <el-table-column prop="targetName" label="对象" min-width="110" />
            <el-table-column prop="hitScene" label="命中场景" min-width="140" show-overflow-tooltip />
            <el-table-column prop="hitSource" label="来源" min-width="130" show-overflow-tooltip />
            <el-table-column label="级别" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.level === 1 ? 'danger' : 'warning'" size="small">L{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="{ PENDING: 'danger', PROCESSING: 'warning', CLOSED: 'success' }[row.status]" size="small" effect="light">
                  {{ { PENDING: '待签收', PROCESSING: '处置中', CLOSED: '已办结' }[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assignee" label="签收人" width="100">
              <template #default="{ row }">{{ row.assignee || '-' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.result" :type="row.result === 'CAPTURED' ? 'success' : 'info'" size="small">
                  {{ { CAPTURED: '抓获', EXCLUDED: '排除', PENDING: '待查' }[row.result] || row.result }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="预警时间" width="170">
              <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 16) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'PENDING'" size="small" link type="primary" @click="handleAssign(row)">签收</el-button>
                <el-button v-if="row.status !== 'CLOSED'" size="small" link type="success" @click="openProcess(row)">处置</el-button>
                <el-button v-if="row.status === 'CLOSED'" size="small" link @click="openProcess(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增布控 -->
    <el-dialog v-model="listDialogVisible" title="新增布控" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="listForm" label-width="100px">
        <el-form-item label="实体ID" required>
          <el-input v-model="listForm.entityId" placeholder="归一化实体ID（entity-master）" />
        </el-form-item>
        <el-form-item label="实体类型">
          <el-select v-model="listForm.entityType" style="width: 100%">
            <el-option label="人员" value="PERSON" />
            <el-option label="车辆" value="VEHICLE" />
            <el-option label="案件" value="CASE" />
            <el-option label="物品" value="ITEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象名称">
          <el-input v-model="listForm.targetName" placeholder="姓名/车牌号" />
        </el-form-item>
        <el-form-item label="名单分组">
          <el-select v-model="listForm.listGroup" style="width: 100%">
            <el-option label="部级" value="部级" />
            <el-option label="省级" value="省级" />
            <el-option label="市级" value="市级" />
            <el-option label="临时" value="临时" />
          </el-select>
        </el-form-item>
        <el-form-item label="布控级别">
          <el-select v-model="listForm.level" style="width: 100%">
            <el-option label="L1 最高" :value="1" />
            <el-option label="L2 高" :value="2" />
            <el-option label="L3 普通" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="布控原因">
          <el-input v-model="listForm.reason" type="textarea" :rows="2" placeholder="涉毒在逃 / 涉案车辆..." />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="listForm.effectiveTo" type="datetime" placeholder="不选则长期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="listForm.density" style="width: 100%">
            <el-option label="秘密" value="CONFIDENTIAL" />
            <el-option label="机密" value="SECRET" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="listDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateList">发布布控</el-button>
      </template>
    </el-dialog>

    <!-- 处置 -->
    <el-dialog v-model="processVisible" :title="`处置预警 · ${currentAlert?.targetName || ''}`" width="520px">
      <el-form :model="processForm" label-width="90px">
        <el-form-item label="处置结果" required>
          <el-select v-model="processForm.result" style="width: 100%">
            <el-option label="抓获" value="CAPTURED" />
            <el-option label="排除" value="EXCLUDED" />
            <el-option label="待查" value="PENDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置反馈">
          <el-input v-model="processForm.feedback" type="textarea" :rows="3" placeholder="现场情况、查证过程..." />
        </el-form-item>
        <el-form-item label="战果">
          <el-input v-model="processForm.merit" type="textarea" :rows="2" placeholder="抓获归因/破案贡献，如：破获涉毒案件1起" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProcess">提交处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Bell, RefreshRight } from '@element-plus/icons-vue'
import { watchApi } from '@/api/modules/watch'
import { ElMessage } from 'element-plus'

const route = useRoute()
const activeTab = ref('lists')
const lists = ref([])
const listsLoading = ref(false)
const listFilter = ref('')
const alerts = ref([])
const alertsLoading = ref(false)
const alertFilter = ref('')
const stats = ref({})

const statItems = [
  { key: 'pending', label: '待签收', color: '#ef4444' },
  { key: 'processing', label: '处置中', color: '#f59e0b' },
  { key: 'closed', label: '已办结', color: '#10b981' },
  { key: 'captured', label: '抓获', color: '#3b82f6' }
]

const listDialogVisible = ref(false)
const submitting = ref(false)
const listForm = reactive({ entityId: '', entityType: 'PERSON', targetName: '', listGroup: '市级', level: 2, reason: '', effectiveTo: null, density: 'SECRET' })

const processVisible = ref(false)
const currentAlert = ref(null)
const processForm = reactive({ result: 'CAPTURED', feedback: '', merit: '' })

async function fetchStats() {
  try { stats.value = await watchApi.stats() } catch { /* ignore */ }
}

async function fetchLists() {
  listsLoading.value = true
  try {
    const params = {}
    if (listFilter.value) params.status = listFilter.value
    lists.value = await watchApi.lists(params)
  } catch (e) {
    ElMessage.error(e.message || '加载名单失败')
  } finally {
    listsLoading.value = false
  }
}

async function fetchAlerts() {
  alertsLoading.value = true
  try {
    const params = {}
    if (alertFilter.value) params.status = alertFilter.value
    alerts.value = await watchApi.alerts(params)
  } catch (e) {
    ElMessage.error(e.message || '加载预警失败')
  } finally {
    alertsLoading.value = false
  }
}

function openListDialog() {
  Object.assign(listForm, { entityId: '', entityType: 'PERSON', targetName: '', listGroup: '市级', level: 2, reason: '', effectiveTo: null, density: 'SECRET' })
  listDialogVisible.value = true
}

async function handleCreateList() {
  if (!listForm.entityId.trim()) {
    ElMessage.warning('请输入实体ID')
    return
  }
  submitting.value = true
  try {
    const payload = { ...listForm }
    if (listForm.effectiveTo) payload.effectiveTo = new Date(listForm.effectiveTo).toISOString().replace('T', ' ').slice(0, 19)
    delete payload.effectiveTo_0
    await watchApi.createList(payload)
    ElMessage.success('布控已发布')
    listDialogVisible.value = false
    await fetchLists()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

async function revokeList(row) {
  try {
    await watchApi.updateListStatus(row.id, 'REVOKED')
    ElMessage.success('已撤销布控')
    await fetchLists()
  } catch (e) {
    ElMessage.error(e.message || '撤销失败')
  }
}

async function handleAssign(row) {
  try {
    await watchApi.assign(row.id)
    ElMessage.success('已签收')
    await fetchAlerts()
    await fetchStats()
  } catch (e) {
    ElMessage.error(e.message || '签收失败')
  }
}

function openProcess(row) {
  currentAlert.value = row
  Object.assign(processForm, { result: row.result || 'CAPTURED', feedback: row.feedback || '', merit: row.merit || '' })
  processVisible.value = true
}

async function handleProcess() {
  try {
    await watchApi.process(currentAlert.value.id, { ...processForm })
    ElMessage.success('处置已提交')
    processVisible.value = false
    await fetchAlerts()
    await fetchStats()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  }
}

let pollTimer = null

onMounted(async () => {
  await Promise.all([fetchLists(), fetchStats()])
  // 支持从情报检索跳转直达：预填布控表单并打开
  if (route.query.entityId) {
    Object.assign(listForm, {
      entityId: String(route.query.entityId),
      targetName: String(route.query.name || ''),
      entityType: route.query.type || 'PERSON'
    })
    listDialogVisible.value = true
  }
  // 预警自动轮询（30s）
  pollTimer = setInterval(() => {
    fetchStats()
    fetchAlerts()
  }, 30000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
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
  color: #dc2626;
}
.stats-row {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 140px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
}
.stat-label {
  font-size: 12px;
  color: #9ca3af;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.toolbar .el-button:last-child {
  margin-left: auto;
}
</style>
