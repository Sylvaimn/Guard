<template>
  <SearchTable
    title="驱动管理"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增驱动"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="驱动名称" min-width="150" />
        <el-table-column prop="dbType" label="数据库类型" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.dbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="driverPath" label="驱动路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
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
          <el-form-item label="名称" prop="name">
            <el-input v-model="crud.dialog.form.name" placeholder="请输入名称" />
          </el-form-item>
          <el-form-item label="数据库类型" prop="dbType">
            <el-input v-model="crud.dialog.form.dbType" placeholder="请输入数据库类型" />
          </el-form-item>
          <el-form-item label="版本" prop="version">
            <el-input v-model="crud.dialog.form.version" placeholder="请输入版本" />
          </el-form-item>
          <el-form-item label="驱动路径" prop="driverPath">
            <el-input v-model="crud.dialog.form.driverPath" placeholder="请输入驱动路径" />
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
import { driverApi } from '@/api/modules/driver'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/common/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const defaultForm = () => ({
  name: '',
  dbType: '',
  version: '',
  driverPath: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请输入数据库类型', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本', trigger: 'blur' }],
  driverPath: [{ required: true, message: '请输入驱动路径', trigger: 'blur' }]
}

// Driver API has no update endpoint, use create-only dialog
const crud = useCRUD({ ...driverApi, update: () => Promise.resolve() }, defaultForm, rules, { entityName: '驱动' })
onMounted(() => { crud.table.fetch() })
</script>