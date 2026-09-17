<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">研判报告</h2>
        <p class="page-desc">结论 + 证据 + 建议操作</p>
      </div>
            <div class="page-actions"><el-button type="primary" :icon="Download" @click="onAction('export')">导出报告</el-button><el-button type="" :icon="Plus" @click="onAction('project')">加入专案</el-button><el-button type="" :icon="Refresh" @click="onAction('regenerate')">重新生成</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-alert :title="'结论：' + report.conclusion" type="success" :closable="false" show-icon />
      <div class="evidence">
        <p v-for="(e, i) in report.evidence" :key="i">• {{ e }}</p>
        <el-empty v-if="!report.evidence.length" description="暂无证据" :image-size="50" />
      </div>
    </el-card>  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { Plus, Search, Download, View, Edit, Delete, MagicStick, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const report = ref({ title: '-', conclusion: '-', evidence: [] })

function onAction(a) { ElMessage.info('操作: ' + a) }

onMounted(async () => {
  const id = route.query.id
  if (!id) return
  try {
    report.value = await userApi.reportDetail(id)
  } catch (e) { ElMessage.error(e.message || '加载失败') }
})
</script>

<style scoped>
.user-page { max-width: 1200px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 12px; color: #8492a6; margin-top: 4px; }
.page-actions { display: flex; gap: 8px; }
.perm-alert { margin-bottom: 16px; }
.page-card { border-radius: 8px; margin-bottom: 16px; }
</style>
