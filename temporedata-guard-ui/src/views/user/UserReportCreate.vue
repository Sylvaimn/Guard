<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">研判报告生成</h2>
        <p class="page-desc">选择专案/对象/数据范围 → 生成报告</p>
      </div>
          </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-form label-width="100px" style="max-width: 620px">
        <el-form-item label="报告标题"><el-input v-model="form.title" placeholder="如：张某某关联关系研判报告" /></el-form-item>
        <el-form-item label="关联专案"><el-input v-model="form.project" placeholder="选择或输入专案（可选）" /></el-form-item>
        <el-form-item label="数据范围"><el-input v-model="form.range" placeholder="如：2026-01-01 ~ 2026-08-29" /></el-form-item>
        <el-form-item label="分析对象"><el-input v-model="form.objects" placeholder="张某某 / 陕A*****（多个用逗号分隔）" /></el-form-item>
        <el-form-item><el-button type="primary" :icon="MagicStick" @click="onAction('create')">AI 辅助生成</el-button></el-form-item>
      </el-form>
    </el-card>  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import { Plus, Search, Download, View, Edit, Delete, MagicStick } from '@element-plus/icons-vue'

const activeTab = ref('a')

const form = ref({ title: '', project: '', range: '', objects: '' })

async function onAction(a) {
  if (a === 'create') {
    if (!form.value.title.trim()) { ElMessage.warning('请输入报告标题'); return }
    try {
      const r = await userApi.createReport({ title: form.value.title, project: form.value.project, type: '研判报告' })
      await userApi.generateReport(r.id)
      ElMessage.success('报告已创建，AI 生成任务已启动')
    } catch (e) { ElMessage.error(e.message || '创建失败') }
  } else {
    ElMessage.info('操作: ' + a)
  }
}
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
