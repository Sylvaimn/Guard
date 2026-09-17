<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据服务接口详情</h2>
        <p class="page-desc">概览/接口定义/权限/调用日志/血缘</p>
      </div>
            <div class="page-actions"><el-button type="primary" :icon="VideoPlay" @click="onAction('test')">测试调用</el-button><el-button type="" :icon="Star" @click="onAction('fav')">收藏</el-button><el-button type="" :icon="Document" @click="onAction('doc')">查看文档</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="API">{{ api.name }}</el-descriptions-item>
        <el-descriptions-item label="方法"><el-tag size="small" type="success" effect="light">{{ api.method }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="密级"><el-tag size="small" effect="light">{{ api.level }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="今日调用">{{ api.todayCalls }}</el-descriptions-item>
        <el-descriptions-item label="成功率">{{ api.successRate }}</el-descriptions-item>
        <el-descriptions-item label="平均响应">{{ api.avgResponse }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card shadow="never" class="page-card">
      <el-tabs v-model="activeTab">
      <el-tab-pane label="概览" name="overview">
        <el-empty description="概览内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="接口定义" name="def">
        <el-empty description="接口定义内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="权限" name="perm">
        <el-empty description="权限内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="调用日志" name="logs">
        <el-empty description="调用日志内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="血缘" name="lineage">
        <el-empty description="血缘内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      </el-tabs>
    </el-card>  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { Plus, Search, Download, View, Edit, Delete, MagicStick, Star, Document, VideoPlay } from '@element-plus/icons-vue'

const route = useRoute()
const api = ref({ name: '-', method: 'GET', level: 'L3', todayCalls: 0, successRate: '-', avgResponse: '-' })

function onAction(a) { ElMessage.info('操作: ' + a) }

onMounted(async () => {
  const id = route.query.id
  if (!id) return
  try {
    api.value = await userApi.apiDetail(id)
  } catch (e) { /* ignore */ }
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
