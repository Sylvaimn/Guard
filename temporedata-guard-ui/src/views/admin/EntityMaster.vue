<template>
  <div class="entity-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><UserFilled /></el-icon>实体归一化</h2>
      <p>多源人员/物品记录融合为统一实体，支撑图分析与布控</p>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="filterType" size="small" @change="fetchList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PERSON">人员</el-radio-button>
        <el-radio-button value="VEHICLE">车辆</el-radio-button>
        <el-radio-button value="CASE">案件</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="filterStatus" size="small" @change="fetchList">
        <el-radio-button value="">全部状态</el-radio-button>
        <el-radio-button value="CANDIDATE">候选待确认</el-radio-button>
        <el-radio-button value="MASTER">已确认</el-radio-button>
      </el-radio-group>
      <el-button type="primary" :icon="Plus" @click="openRegister">登记来源记录</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="displayName" label="实体名称" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.entityType)" size="small">{{ typeLabel(row.entityType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="primaryKey" label="主标识" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'MASTER' ? 'success' : 'warning'" effect="light" size="small">
              {{ row.status === 'MASTER' ? '已确认' : '候选待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源映射" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.mappings?.length || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="200">
          <template #default="{ row }">
            <el-tag v-for="l in (row.labels || []).slice(0, 3)" :key="l.id" size="small" effect="light" style="margin-right: 4px">
              {{ l.labelName }}
            </el-tag>
            <span v-if="!row.labels || !row.labels.length">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="density" label="密级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="densityTag(row.density)">{{ densityLabel(row.density) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'CANDIDATE'" size="small" link type="success" @click="handleConfirm(row)">确认</el-button>
            <el-button size="small" link type="warning" @click="openLabel(row)">标签</el-button>
            <el-popconfirm title="确定删除该实体？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 登记来源记录 -->
    <el-dialog v-model="registerVisible" title="登记来源记录（归一化）" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="实体类型" required>
          <el-select v-model="registerForm.entityType" style="width: 100%">
            <el-option label="人员" value="PERSON" />
            <el-option label="车辆" value="VEHICLE" />
            <el-option label="案件" value="CASE" />
            <el-option label="物品" value="ITEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="主标识" required>
          <el-input v-model="registerForm.primaryKey" placeholder="身份证号/车牌号/案件编号" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="registerForm.displayName" placeholder="姓名/车牌等" />
        </el-form-item>
        <el-form-item label="来源系统">
          <el-input v-model="registerForm.sourceSystem" placeholder="警综/卡口/话单..." />
        </el-form-item>
        <el-form-item label="来源记录ID">
          <el-input v-model="registerForm.sourceRecordId" placeholder="来源系统原始记录ID" />
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="registerForm.density" style="width: 100%">
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="CONFIDENTIAL" />
            <el-option label="机密" value="SECRET" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRegister">登记</el-button>
      </template>
    </el-dialog>

    <!-- 详情（映射+标签+合并） -->
    <el-dialog v-model="detailVisible" :title="`实体详情 · ${current?.displayName || ''}`" width="720px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">{{ typeLabel(current.entityType) }}</el-descriptions-item>
          <el-descriptions-item label="主标识">{{ current.primaryKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ current.status === 'MASTER' ? '已确认' : '候选待确认' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ densityLabel(current.density) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="detail-section-title">来源映射（{{ current.mappings?.length || 0 }}）</div>
          <el-table :data="current.mappings || []" size="small" border>
            <el-table-column prop="sourceSystem" label="来源系统" width="110" />
            <el-table-column prop="sourceRecordId" label="来源记录ID" min-width="150" show-overflow-tooltip />
            <el-table-column prop="matchMethod" label="匹配方式" width="100">
              <template #default="{ row }">{{ { RULE: '规则', SIMILAR: '相似', MANUAL: '人工' }[row.matchMethod] || row.matchMethod }}</template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="80">
              <template #default="{ row }">{{ row.confidence != null ? row.confidence : '-' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">标签（{{ current.labels?.length || 0 }}）</div>
          <el-table :data="current.labels || []" size="small" border>
            <el-table-column prop="labelName" label="标签名" min-width="120" />
            <el-table-column prop="labelCode" label="编码" width="140" />
            <el-table-column prop="source" label="来源" width="110" />
            <el-table-column prop="expireTime" label="有效期至" width="160">
              <template #default="{ row }">{{ row.expireTime || '长期' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="current.status === 'CANDIDATE'" class="detail-section">
          <div class="detail-section-title">人工确认 / 合并</div>
          <div class="merge-row">
            <el-select v-model="mergeTarget" filterable placeholder="选择要合并到的主实体" style="width: 320px" clearable>
              <el-option
                v-for="e in mergeCandidates"
                :key="e.id"
                :label="`${e.displayName} (${e.primaryKey})`"
                :value="e.id"
              />
            </el-select>
            <el-button type="primary" size="small" :disabled="!mergeTarget" @click="handleMerge">合并到此实体</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 打标签 -->
    <el-dialog v-model="labelVisible" :title="`打标签 · ${current?.displayName || ''}`" width="460px">
      <el-form :model="labelForm" label-width="90px">
        <el-form-item label="标签类型" required>
          <el-select v-model="labelForm.labelType" style="width: 100%">
            <el-option label="身份标签" value="IDENTITY" />
            <el-option label="行为标签" value="BEHAVIOR" />
            <el-option label="时空标签" value="SPATIO_TEMPORAL" />
            <el-option label="案件标签" value="CASE" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签编码" required>
          <el-input v-model="labelForm.labelCode" placeholder="如 DRUG_HISTORY / FREQ_TRAVEL" />
        </el-form-item>
        <el-form-item label="标签名" required>
          <el-input v-model="labelForm.labelName" placeholder="如 吸毒前科 / 跨省频繁" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="labelForm.source" placeholder="案事件库 / 人工研判..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="labelVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddLabel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, UserFilled } from '@element-plus/icons-vue'
import { entityMasterApi } from '@/api/modules/entitymaster'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const filterType = ref('')
const filterStatus = ref('')

const registerVisible = ref(false)
const submitting = ref(false)
const registerForm = reactive({ entityType: 'PERSON', primaryKey: '', displayName: '', sourceSystem: '', sourceRecordId: '', density: 'SECRET' })

const detailVisible = ref(false)
const labelVisible = ref(false)
const current = ref(null)
const mergeCandidates = ref([])
const mergeTarget = ref('')
const labelForm = reactive({ labelType: 'IDENTITY', labelCode: '', labelName: '', source: '' })

function typeTag(t) {
  return { PERSON: 'primary', VEHICLE: 'warning', CASE: 'danger', ITEM: 'info' }[t] || 'info'
}
function typeLabel(t) {
  return { PERSON: '人员', VEHICLE: '车辆', CASE: '案件', ITEM: '物品' }[t] || t
}
function densityLabel(d) {
  return { PUBLIC: '公开', INTERNAL: '内部', CONFIDENTIAL: '秘密', SECRET: '机密' }[d] || d
}
function densityTag(d) {
  return { PUBLIC: 'info', INTERNAL: '', CONFIDENTIAL: 'warning', SECRET: 'danger' }[d] || ''
}

async function fetchList() {
  loading.value = true
  try {
    const params = {}
    if (filterType.value) params.entityType = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    tableData.value = await entityMasterApi.list(params)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openRegister() {
  Object.assign(registerForm, { entityType: 'PERSON', primaryKey: '', displayName: '', sourceSystem: '', sourceRecordId: '', density: 'SECRET' })
  registerVisible.value = true
}

async function handleRegister() {
  if (!registerForm.primaryKey.trim()) {
    ElMessage.warning('请输入主标识')
    return
  }
  submitting.value = true
  try {
    const res = await entityMasterApi.register({ ...registerForm })
    registerVisible.value = false
    ElMessage.success(res.status === 'MASTER' ? '已归一到已确认实体' : '已登记为候选实体，待人工确认')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '登记失败')
  } finally {
    submitting.value = false
  }
}

async function openDetail(row) {
  current.value = await entityMasterApi.detail(row.id)
  mergeTarget.value = ''
  mergeCandidates.value = (await entityMasterApi.list({ entityType: row.entityType, status: 'MASTER' }))
    .filter(e => e.id !== row.id)
  detailVisible.value = true
}

async function handleConfirm(row) {
  try {
    await entityMasterApi.confirm(row.id)
    ElMessage.success('已确认为正式实体')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '确认失败')
  }
}

async function handleMerge() {
  try {
    await entityMasterApi.merge(mergeTarget.value, current.value.id)
    ElMessage.success('合并成功')
    detailVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '合并失败')
  }
}

function openLabel(row) {
  current.value = row
  Object.assign(labelForm, { labelType: 'IDENTITY', labelCode: '', labelName: '', source: '' })
  labelVisible.value = true
}

async function handleAddLabel() {
  if (!labelForm.labelCode.trim() || !labelForm.labelName.trim()) {
    ElMessage.warning('请填写标签编码和名称')
    return
  }
  try {
    await entityMasterApi.addLabel(current.value.id, { ...labelForm })
    ElMessage.success('标签已添加')
    labelVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function handleDelete(id) {
  try {
    await entityMasterApi.remove(id)
    ElMessage.success('已删除')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(fetchList)
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
  color: #6366f1;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.toolbar .el-button {
  margin-left: auto;
}
.table-card {
  border-radius: 8px;
}
.detail-section {
  margin-top: 16px;
}
.detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}
.merge-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
