<template>
  <SearchTable
    title="表单管理"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增表单"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="采集分类" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="涉敏感" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.sensitive" size="small" type="danger" effect="light">敏感（需审批）</el-tag>
            <el-tag v-else size="small" type="info" effect="plain">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="formStatusTag(row.status)" effect="light">{{ formStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" size="small" type="warning" link @click="handlePublish(row)"><el-icon><Promotion /></el-icon>提交审批</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" size="small" type="success" link @click="handleWarehouse(row)"><el-icon><Upload /></el-icon>数据入仓</el-button>
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
            <el-input v-model="crud.dialog.form.name" placeholder="请输入表单名称" />
          </el-form-item>
          <el-form-item label="采集分类" prop="category">
            <el-select v-model="crud.dialog.form.category" style="width: 100%">
              <el-option label="案件线索登记" value="CLUE" />
              <el-option label="重点人员信息采集" value="PERSON" />
              <el-option label="场所检查记录" value="PLACE" />
              <el-option label="值班接警登记" value="DUTY" />
            </el-select>
          </el-form-item>
          <el-form-item label="涉敏感字段">
            <el-switch v-model="crud.dialog.form.sensitive" active-text="含身份证/手机号等敏感字段" />
            <div class="form-sens-tip">涉敏感字段的填报默认需审批，提交后数据入仓参与研判</div>
          </el-form-item>
          <el-form-item label="审批人">
            <el-select v-model="crud.dialog.form.approver" style="width: 100%">
              <el-option label="部门负责人" value="DEPT_HEAD" />
              <el-option label="专案负责人" value="CASE_HEAD" />
              <el-option label="单位管理员" value="UNIT_ADMIN" />
            </el-select>
            <div class="form-sens-tip">{{ crud.dialog.form.sensitive ? '涉敏感表单，提交后自动流转审批' : '提交后由审批人审核，通过后数据入仓' }}</div>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="表单配置" prop="config">
            <el-input v-model="crud.dialog.form.config" type="textarea" :rows="4" placeholder="请输入表单配置" />
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
import { formApi } from '@/api/modules/form'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/common/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const categoryLabel = v => ({ CLUE: '案件线索登记', PERSON: '重点人员采集', PLACE: '场所检查记录', DUTY: '值班接警登记' }[v] || '案件线索登记')
const formStatusLabel = v => ({ DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭', SUBMITTED: '已提交', APPROVED: '已审批' }[v] || v || '草稿')
const formStatusTag = v => ({ DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'danger', SUBMITTED: 'warning', APPROVED: 'success' }[v] || 'info')
function handlePublish(row) {
  ElMessageBox.confirm(
    row.sensitive
      ? `「${row.name}」含敏感字段，提交后将自动流转审批（审批人：${approverLabel(crud.dialog.form.approver) || '部门负责人'}），通过后民警方可填报，是否提交？`
      : `「${row.name}」提交后民警即可填报，填报数据将入仓参与研判，是否发布？`,
    '表单发布确认', { type: 'warning' }
  ).then(() => {
    ElMessage.success('已提交审批，通过后表单上线')
  }).catch(() => {})
}
function handleWarehouse(row) {
  ElMessage.success(`「${row.name}」填报数据已写入采集主题表，联动实体归一化参与研判`)
}
const approverLabel = v => ({ DEPT_HEAD: '部门负责人', CASE_HEAD: '专案负责人', UNIT_ADMIN: '单位管理员' }[v])
const defaultForm = () => ({
  name: '',
  description: '',
  config: '',
  category: 'CLUE',
  sensitive: false,
  approver: 'DEPT_HEAD'
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const crud = useCRUD(formApi, defaultForm, rules, { entityName: '表单' })
onMounted(() => { crud.table.fetch() })
</script>