<template>
  <div class="role-page">
    <PageHeader title="角色管理" desc="定义平台角色，分配成员权限。内置角色不可删除或修改编码" />

    <div class="page-body">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="crud.dialog.openAdd()">新增角色</el-button>
      </div>

      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe>
        <el-table-column prop="name" label="角色名称" min-width="140">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.protectedRole" size="small" type="warning" effect="light" style="margin-left: 8px">内置</el-tag>
              <!-- 权限配置 -->
    <el-dialog v-model="permVisible" :title="'权限配置 - ' + permRole.name" width="560px" destroy-on-close>
      <el-checkbox-group v-model="permSelected" class="perm-group">
        <el-checkbox v-for="p in PERM_CODES" :key="p" :value="p" :label="p" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePerm">保存</el-button>
      </template>
    </el-dialog>
</template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" width="130">
          <template #default="{ row }">
            <el-tag effect="dark" size="small">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="dataScope" label="数据范围" width="130">
          <template #default="{ row }">
            <el-tag :type="scopeTag(row.dataScope)" effect="light" size="small">{{ scopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成员" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMembers(row)">查看成员</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :disabled="row.protectedRole" @click="crud.dialog.openEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-popconfirm title="确定删除该角色？" :disabled="row.protectedRole" @confirm="crud.handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link :disabled="row.protectedRole"><el-icon><Delete /></el-icon>删除</el-button>
              <el-button size="small" type="warning" link @click="openPermConfig(row)">权限</el-button>
          </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="crud.table.currentPage"
          v-model:page-size="crud.table.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="crud.table.total"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="crud.dialog.visible" :title="crud.dialog.isEdit ? '编辑角色' : '新增角色'" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="90px" label-position="right">
        <el-form-item label="名称" prop="name">
          <el-input v-model="crud.dialog.form.name" placeholder="请输入角色名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="crud.dialog.form.code" placeholder="如：DATA_ANALYST" maxlength="32" :disabled="crud.dialog.isEdit && formProtected" />
        </el-form-item>
        <el-form-item label="说明" prop="remark">
          <el-input v-model="crud.dialog.form.remark" type="textarea" :rows="2" maxlength="255" placeholder="角色用途说明（可选）" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="crud.dialog.form.dataScope" style="width: 100%">
            <el-option label="本单位" value="UNIT" />
            <el-option label="本部门" value="DEPT" />
            <el-option label="专案内" value="CASE" />
            <el-option label="全局（谨慎）" value="GLOBAL" />
          </el-select>
          <div class="scope-hint">决定该角色成员可访问的数据边界（联动组织架构）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="crud.dialog.close()">取消</el-button>
        <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色成员 -->
    <el-dialog v-model="memberVisible" :title="`角色成员 · ${currentRole?.name || ''}`" width="620px" :close-on-click-modal="false">
      <el-table :data="members" v-loading="membersLoading" stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="tenantName" label="所属租户" min-width="150">
          <template #default="{ row }">{{ row.tenantName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="加入时间" min-width="160" />
      </el-table>
      <div v-if="!membersLoading && members.length === 0" class="member-empty">该角色下暂无成员</div>
    </el-dialog>
  </div>
</template>

<script setup>

const PERM_CODES = [
  'workbench:view', 'intelligence:view', 'intelligence:detail', 'intelligence:graph', 'intelligence:export',
  'project:view', 'project:create', 'project:update',
  'dataasset:view', 'dataasset:query', 'dataasset:apply', 'dataasset:export', 'dataasset:authorize',
  'dataapi:view', 'report:view', 'report:create', 'report:export', 'form:view', 'agent:view'
]
const permVisible = ref(false)
const permSaving = ref(false)
const permRole = ref({})
const permSelected = ref([])

async function openPermConfig(row) {
  permRole.value = row
  permSelected.value = []
  try {
    const list = await userApi.rolePermissions() || []
    const hit = list.find(r => r.roleId === row.id)
    if (hit && hit.permissions) permSelected.value = String(hit.permissions).split(',')
  } catch (e) { /* ignore */ }
  permVisible.value = true
}

async function savePerm() {
  permSaving.value = true
  try {
    await userApi.saveRolePermissions({ roleId: permRole.value.id, permissions: permSelected.value })
    ElMessage.success('权限已保存')
    permVisible.value = false
  } catch (e) { ElMessage.error(e.message || '保存失败') } finally { permSaving.value = false }
}

const defaultForm = () => ({
  name: '',
  code: '',
  remark: '',
  dataScope: 'UNIT'
})

const scopeLabel = v => ({ GLOBAL: '全局', UNIT: '本单位', DEPT: '本部门', CASE: '专案内' }[v] || '本单位')
const scopeTag = v => ({ GLOBAL: 'danger', UNIT: 'success', DEPT: 'primary', CASE: 'warning' }[v] || 'success')

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

const crud = useCRUD(roleApi, defaultForm, rules, { entityName: '角色' })

const formProtected = ref(false)
const memberVisible = ref(false)
const membersLoading = ref(false)
const members = ref([])
const currentRole = ref(null)

const openEdit = crud.dialog.openEdit
crud.dialog.openEdit = (row) => {
  formProtected.value = !!row.protectedRole
  openEdit(row)
}

async function openMembers(row) {
  currentRole.value = row
  memberVisible.value = true
  membersLoading.value = true
  try {
    const res = await roleApi.members(row.id)
    members.value = res || []
  } catch (e) {
    ElMessage.error(e.message || '加载成员失败')
  } finally {
    membersLoading.value = false
  }
}

onMounted(() => { crud.table.fetch() })
</script>

<style scoped>
.role-page { max-width: 1200px; }
.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.member-empty {
  text-align: center;
  color: #9ca3af;
  padding: 20px 0;
  font-size: 13px;
}
.scope-hint { font-size: 11.5px; color: #9ca3af; margin-top: 4px; }
</style>
