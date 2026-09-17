<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">专案任务</h2>
        <p class="page-desc">任务分解与核查跟踪</p>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="task" label="任务" min-width="130" />
        <el-table-column prop="assignee" label="负责人" min-width="130" />
        <el-table-column prop="status" label="状态" min-width="130" />
        <el-table-column prop="deadline" label="期限" min-width="130" />
        <el-table-column prop="result" label="结果" min-width="130" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default>
            <el-button size="small" type="primary" link>查看</el-button>
            <el-button size="small" type="danger" link>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'

const route = useRoute()
const rows = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await userApi.projectTasks(route.query.id) || []
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
