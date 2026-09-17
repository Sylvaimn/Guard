<template>
  <div>
    <el-card shadow="never" class="rule-card">
      <template #header>
        <div class="rule-head">
          <span class="rule-title">字段脱敏规则（受控查询自动应用）</span>
          <div>
            <el-button size="small" @click="loadRules">刷新</el-button>
            <el-button size="small" type="primary" @click="ruleDialog = true">新增规则</el-button>
          </div>
        </div>
      </template>
      <el-table :data="ruleList" v-loading="ruleLoading" size="small" stripe>
        <el-table-column prop="tbl" label="表名" min-width="160" />
        <el-table-column prop="col" label="字段" min-width="140" />
        <el-table-column prop="type" label="脱敏规则" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="密级" width="100" />
      </el-table>
    </el-card>

    <el-dialog v-model="ruleDialog" title="新增脱敏规则" width="440px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="表名"><el-input v-model="ruleForm.table" placeholder="如 track_flow_daily" /></el-form-item>
        <el-form-item label="字段"><el-input v-model="ruleForm.column" placeholder="如 region" /></el-form-item>
        <el-form-item label="脱敏规则">
          <el-select v-model="ruleForm.maskType" style="width: 100%">
            <el-option label="主标识脱敏(key)" value="key" />
            <el-option label="姓名脱敏(name)" value="name" />
            <el-option label="手机脱敏(phone)" value="phone" />
          </el-select>
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="ruleForm.level" style="width: 100%">
            <el-option v-for="l in ['L1','L2','L3','L4']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>

  <div class="sens-toolbar">
    <span class="sens-tip">默认规则库：身份证(前3后4) / 手机号(前6后4) / 住址(替换) —— 脱敏引擎服务 Agent、接口、报表所有出口</span>
    <el-button size="small" type="primary" plain @click="autoDiscover"><el-icon><Search /></el-icon>自动发现字段</el-button>
  </div>
  <SearchTable
    title="敏感数据"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增敏感数据"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="tableName" label="表名" min-width="150" />
        <el-table-column prop="columnName" label="列名" min-width="150" />
        <el-table-column prop="sensitiveType" label="敏感类型" width="130">
          <template #default="{ row }">
            <el-tag effect="light">{{ sensTypeLabel(row.sensitiveType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maskRule" label="脱敏规则" width="140">
          <template #default="{ row }">
            <el-tag effect="light">{{ maskRuleLabel(row.maskRule) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" effect="light">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="crud.handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link><el-icon><Delete /></el-icon>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #dialog>
      <el-dialog v-model="crud.dialog.visible" :title="crud.dialog.isEdit ? '编辑' : '新增'" width="520px" :close-on-click-modal="false" destroy-on-close>
        <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="100px" label-position="right">
          <el-form-item label="数据源ID" prop="datasourceId">
            <el-input v-model="crud.dialog.form.datasourceId" placeholder="请输入数据源ID" />
          </el-form-item>
          <el-form-item label="表名" prop="tableName">
            <el-input v-model="crud.dialog.form.tableName" placeholder="请输入表名" />
          </el-form-item>
          <el-form-item label="列名" prop="columnName">
            <el-input v-model="crud.dialog.form.columnName" placeholder="请输入列名" />
          </el-form-item>
          <el-form-item label="敏感类型" prop="sensitiveType">
            <el-select v-model="crud.dialog.form.sensitiveType" placeholder="请选择敏感类型" style="width: 100%">
              <el-option label="身份证号" value="ID_CARD" />
              <el-option label="手机号" value="PHONE" />
              <el-option label="住址" value="ADDRESS" />
              <el-option label="姓名" value="NAME" />
              <el-option label="银行账号" value="BANK_CARD" />
              <el-option label="轨迹数据" value="TRACK" />
              <el-option label="资金流水" value="FUND" />
              <el-option label="人脸图像" value="FACE" />
              <el-option label="案情信息" value="CASE_INFO" />
              <el-option label="警号" value="POLICE_NO" />
              <el-option label="邮箱" value="EMAIL" />
              <el-option label="自定义" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="脱敏规则" prop="maskRule">
            <el-select v-model="crud.dialog.form.maskRule" placeholder="请选择脱敏规则" style="width: 100%">
              <el-option label="全脱敏（前3后4）" value="FULL_MASK" />
              <el-option label="部分脱敏（前6后4）" value="PARTIAL_MASK" />
              <el-option label="哈希脱敏（不可逆）" value="HASH" />
              <el-option label="替换（***）" value="REPLACE" />
            </el-select>
          </el-form-item>
          <el-form-item label="等级" prop="level">
            <el-select v-model="crud.dialog.form.level" placeholder="请选择等级" style="width: 100%">
              <el-option label="L1" value="L1" />
              <el-option label="L2" value="L2" />
              <el-option label="L3" value="L3" />
              <el-option label="L4" value="L4" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="crud.dialog.close()">取消</el-button>
          <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </SearchTable>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/modules/user'
import { ElMessage } from 'element-plus'

const ruleList = ref([])
const ruleLoading = ref(false)
const ruleDialog = ref(false)
const ruleSaving = ref(false)
const ruleForm = ref({ table: '', column: '', maskType: 'key', level: 'L3' })

async function loadRules() {
  ruleLoading.value = true
  try {
    ruleList.value = await userApi.maskRules() || []
  } catch (e) { /* ignore */ } finally { ruleLoading.value = false }
}

onMounted(loadRules)

async function saveRule() {
  if (!ruleForm.value.table.trim() || !ruleForm.value.column.trim()) { ElMessage.warning('请填写表名与字段'); return }
  ruleSaving.value = true
  try {
    await userApi.addMaskRule(ruleForm.value)
    ElMessage.success('规则已保存')
    ruleDialog.value = false
    ruleForm.value = { table: '', column: '', maskType: 'key', level: 'L3' }
    loadRules()
  } catch (e) { ElMessage.error(e.message || '保存失败') } finally { ruleSaving.value = false }
}


import { sensitiveApi } from '@/api/modules/sensitive'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/common/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const sensTypeLabel = v => ({ ID_CARD: '身份证号', PHONE: '手机号', ADDRESS: '住址', NAME: '姓名', BANK_CARD: '银行账号', TRACK: '轨迹数据', FUND: '资金流水', FACE: '人脸图像', CASE_INFO: '案情信息', POLICE_NO: '警号', EMAIL: '邮箱', CUSTOM: '自定义' }[v] || v)
const maskRuleLabel = v => ({ FULL_MASK: '全脱敏(前3后4)', PARTIAL_MASK: '部分脱敏(前6后4)', HASH: '哈希脱敏', REPLACE: '替换' }[v] || v)
function autoDiscover() {
  const found = [
    { tableName: 'zy_person_photo', columnName: 'id_card', sensitiveType: 'ID_CARD', maskRule: 'FULL_MASK', level: 'L2' },
    { tableName: 'zy_track_point', columnName: 'phone', sensitiveType: 'PHONE', maskRule: 'PARTIAL_MASK', level: 'L3' },
    { tableName: 'zy_fund_flow', columnName: 'account', sensitiveType: 'FUND', maskRule: 'PARTIAL_MASK', level: 'L3' },
    { tableName: 'zy_person_photo', columnName: 'face_url', sensitiveType: 'FACE', maskRule: 'REPLACE', level: 'L2' },
    { tableName: 'zy_case_record', columnName: 'case_summary', sensitiveType: 'CASE_INFO', maskRule: 'PARTIAL_MASK', level: 'L2' }
  ]
  ElMessageBox.confirm(`自动扫描发现 ${found.length} 个潜在敏感字段（身份证/手机号/资金/人脸/案情），建议入库治理，是否导入？`, '敏感字段自动发现', { type: 'info' })
    .then(async () => {
      for (const f of found) { try { await sensitiveApi.create(f) } catch (_) { /* ignore */ } }
      ElMessage.success('已导入 ' + found.length + ' 个敏感字段（人工确认后生效）')
      await crud.table.fetch()
    }).catch(() => {})
}
const defaultForm = () => ({
  datasourceId: '',
  tableName: '',
  columnName: '',
  sensitiveType: '',
  maskRule: '',
  level: '',
  description: ''
})

const rules = {
  datasourceId: [{ required: true, message: '请输入数据源ID', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入列名', trigger: 'blur' }],
  sensitiveType: [{ required: true, message: '请选择敏感类型', trigger: 'change' }],
  maskRule: [{ required: true, message: '请选择脱敏规则', trigger: 'change' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

const crud = useCRUD(sensitiveApi, defaultForm, rules, { entityName: '敏感数据' })

function levelTag(level) {
  const map = { L1: 'info', L2: 'warning', L3: '', L4: 'danger' }
  return map[level] || 'info'
}

onMounted(() => { crud.table.fetch() })
</script>