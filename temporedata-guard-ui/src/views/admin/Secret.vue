<template>
  <div class="secret-page">
    <PageHeader title="密钥管理" desc="管理 API 密钥和数据库连接凭证，支持密钥生成、轮转和过期管理">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">生成密钥</el-button>
      </template>
    </PageHeader>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe>
        <el-table-column prop="name" label="密钥名称" min-width="160" />
        <el-table-column prop="keyMask" label="密钥" min-width="240">
          <template #default="{ row }">
            <code class="secret-key">{{ row.keyMask }}</code>
            <el-button type="primary" link size="small" @click="handleCopy(row)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag effect="dark">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该密钥吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next"
          @size-change="currentPage = 1"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑密钥' : '生成密钥'" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="密钥名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入密钥名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="API 密钥" value="API_KEY" />
            <el-option label="数据库密码" value="DB_PASSWORD" />
            <el-option label="Token" value="TOKEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker v-model="form.expireTime" type="datetime" placeholder="选择过期时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { secretApi } from '@/api/modules/secret'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', type: '', expireTime: '' })
const rules = {
  name: [{ required: true, message: '请输入密钥名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    tableData.value = await secretApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载密钥列表失败')
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', type: '', expireTime: '' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { name: row.name, type: row.type, expireTime: row.expireTime || '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await secretApi.update(editId.value, { ...form })
      ElMessage.success('密钥更新成功')
    } else {
      await secretApi.create({ ...form })
      ElMessage.success('密钥生成成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await secretApi.delete(id)
    ElMessage.success('密钥已删除')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

function handleCopy(row) {
  navigator.clipboard.writeText(row.keyMask).then(() => {
    ElMessage.success('密钥已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => fetchData())
</script>

<style scoped>
.secret-page { max-width: 1200px; }
.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.secret-key {
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  color: #6b7280;
  margin-right: 8px;
}
</style>