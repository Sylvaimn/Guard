<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">线索中心</h2>
        <p class="page-desc">线索采集/核查/流转</p>
      </div>
    </div>
    <div class="page-actions">
      <el-button type="primary" :icon="Plus" @click="openCreate">登记线索</el-button>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="clue_no" label="线索编号" min-width="130" />
        <el-table-column prop="title" label="线索标题" min-width="130" />
        <el-table-column prop="level" label="重要程度" min-width="130" />
        <el-table-column prop="status" label="状态" min-width="130" />
        <el-table-column prop="create_time" label="发现时间" min-width="130" />
        <el-table-column prop="source" label="来源" min-width="130" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default>
            <el-button size="small" type="primary" link>查看</el-button>
            <el-button size="small" type="danger" link>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="登记线索" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="线索标题"><el-input v-model="createForm.title" placeholder="线索描述" /></el-form-item>
        <el-form-item label="重要程度">
          <el-select v-model="createForm.level" style="width: 100%">
            <el-option v-for="l in ['L1','L2','L3']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源"><el-input v-model="createForm.source" placeholder="如 轨迹分析 / 群众举报" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import { Plus } from '@element-plus/icons-vue'

const rows = ref([])
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ title: '', level: 'L3', source: '' })

function openCreate() { createForm.value = { title: '', level: 'L3', source: '' }; createVisible.value = true }

async function doCreate() {
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入线索标题'); return }
  creating.value = true
  try {
    await userApi.createClue(createForm.value)
    ElMessage.success('线索已登记')
    createVisible.value = false
    rows.value = await userApi.clues() || []
  } catch (e) { ElMessage.error(e.message || '登记失败') } finally { creating.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await userApi.clues() || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
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
