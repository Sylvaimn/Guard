<template>
  <SearchTable
    title="数据报表"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增报表"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="type" label="模板" width="140">
          <template #default="{ row }">
            <el-tag effect="light">{{ reportTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="100">
          <template #default="{ row }">
            <el-tag :type="secretTag(row.secretLevel)" effect="light">{{ secretLabel(row.secretLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="周期" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ periodLabel(row.schedulePeriod) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="light">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleGenerate(row)"><el-icon><VideoPlay /></el-icon>生成</el-button>
            <el-button size="small" type="success" link @click="handleExport(row)"><el-icon><Download /></el-icon>导出</el-button>
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
          <el-form-item label="名称" prop="name">
            <el-input v-model="crud.dialog.form.name" placeholder="请输入名称" />
          </el-form-item>
          <el-form-item label="报表模板" prop="type">
            <el-select v-model="crud.dialog.form.type" placeholder="请选择模板" style="width: 100%">
              <el-option label="涉诈研判周报" value="FRAUD_WEEKLY" />
              <el-option label="治安态势月报" value="SECURITY_MONTHLY" />
              <el-option label="重点人员动态简报" value="PERSON_BRIEF" />
              <el-option label="专案阶段报告" value="CASE_STAGE" />
              <el-option label="自定义报表" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="生成周期" prop="schedulePeriod">
            <el-select v-model="crud.dialog.form.schedulePeriod" style="width: 100%">
              <el-option label="日报（每日 8:00）" value="DAILY" />
              <el-option label="周报（周一 8:00）" value="WEEKLY" />
              <el-option label="月报（每月 1 日 8:00）" value="MONTHLY" />
              <el-option label="手动生成" value="MANUAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="密级" prop="secretLevel">
            <el-select v-model="crud.dialog.form.secretLevel" style="width: 100%">
              <el-option label="内部" value="INTERNAL" />
              <el-option label="秘密（导出需审批）" value="SECRET" />
              <el-option label="机密（导出需审批）" value="TOP_SECRET" />
            </el-select>
          </el-form-item>
          <el-form-item label="口径说明" prop="caliberDesc">
            <el-input v-model="crud.dialog.form.caliberDesc" type="textarea" :rows="2" placeholder="指标计算口径，如：立案数=当日立案登记数（跨单位口径统一）" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="配置" prop="config">
            <el-input v-model="crud.dialog.form.config" type="textarea" :rows="4" placeholder="请输入配置" />
          </el-form-item>
          <el-form-item label="数据源ID" prop="datasourceId">
            <el-input v-model="crud.dialog.form.datasourceId" placeholder="请输入数据源ID" />
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
import { onMounted } from 'vue'
import { reportApi } from '@/api/modules/report'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/common/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const periodLabel = v => ({ DAILY: '日报', WEEKLY: '周报', MONTHLY: '月报', MANUAL: '手动' }[v] || '手动')
function handleGenerate(row) {
  ElMessage.success(`「${row.name}」已加入生成任务队列（${periodLabel(row.schedulePeriod)}），完成后消息通知`)
}
function handleExport(row) {
  const level = row.secretLevel || 'INTERNAL'
  if (level === 'INTERNAL') {
    ElMessage.success('导出任务已提交（内部报表，直接导出）')
    return
  }
  ElMessageBox.prompt('报表密级为' + (level === 'SECRET' ? '秘密' : '机密') + '，请填写导出事由与用途（审批通过后脱敏导出）', '导出审批申请', {
    inputType: 'textarea',
    inputValidator: v => (v && v.trim().length >= 5) ? true : '请至少填写 5 个字'
  }).then(() => {
    ElMessage.success('导出审批申请已提交，审批通过后自动导出（按脱敏策略处理）')
  }).catch(() => {})
}
const reportTypeLabel = v => ({ FRAUD_WEEKLY: '涉诈研判周报', SECURITY_MONTHLY: '治安态势月报', PERSON_BRIEF: '重点人员动态简报', CASE_STAGE: '专案阶段报告', CUSTOM: '自定义报表' }[v] || v || '自定义报表')
const secretLabel = v => ({ INTERNAL: '内部', SECRET: '秘密', TOP_SECRET: '机密' }[v] || '内部')
const secretTag = v => ({ INTERNAL: 'info', SECRET: 'warning', TOP_SECRET: 'danger' }[v] || 'info')
const defaultForm = () => ({
  name: '',
  type: '',
  description: '',
  config: '',
  datasourceId: '',
  secretLevel: 'INTERNAL',
  caliberDesc: '',
  schedulePeriod: 'MANUAL'
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const crud = useCRUD(reportApi, defaultForm, rules, { entityName: '报表' })
onMounted(() => { crud.table.fetch() })
</script>