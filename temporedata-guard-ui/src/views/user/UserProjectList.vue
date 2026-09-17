<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的专案</h2>
        <p class="page-desc">跨模块专案工作容器</p>
      </div>
    </div>
    <div class="page-actions">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建专案</el-button>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="name" label="专案名称" min-width="130" />
        <el-table-column prop="level" label="密级" min-width="130" />
        <el-table-column prop="status" label="状态" min-width="130" />
        <el-table-column prop="member" label="成员" min-width="130" />
        <el-table-column prop="clue" label="线索数" min-width="130" />
        <el-table-column prop="update" label="更新时间" min-width="130" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default>
            <el-button size="small" type="primary" link>查看</el-button>
            <el-button size="small" type="danger" link>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="新建专案" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="专案名称"><el-input v-model="createForm.name" placeholder="请输入专案名称" /></el-form-item>
        <el-form-item label="密级">
          <el-select v-model="createForm.level" style="width: 100%">
            <el-option v-for="l in ['L1','L2','L3','L4']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const rows = ref([])
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', level: 'L3', description: '' })

function openCreate() { createForm.value = { name: '', level: 'L3', description: '' }; createVisible.value = true }

async function doCreate() {
  if (!createForm.value.name.trim()) { ElMessage.warning('请输入专案名称'); return }
  creating.value = true
  try {
    await userApi.createProject(createForm.value)
    ElMessage.success('专案已创建')
    createVisible.value = false
    rows.value = await userApi.listProjects() || []
  } catch (e) { ElMessage.error(e.message || '创建失败') } finally { creating.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await userApi.listProjects() || []
  } catch (e) { ElMessage.error(e.message || '加载失败') } finally { loading.value = false }
})
</script>

<style scoped>
.user-page { max-width: 1200px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 12px; color: #8492a6; margin-top: 4px; }
.perm-alert { margin-bottom: 16px; }
.page-card { border-radius: 8px; margin-bottom: 16px; }
</style>
