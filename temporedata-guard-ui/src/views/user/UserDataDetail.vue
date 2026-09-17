<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据资产统一详情</h2>
        <p class="page-desc">概览/字段/质量/血缘/权限/使用情况</p>
      </div>
            <div class="page-actions"><el-button type="primary" :icon="Key" @click="onAction('apply')">申请访问</el-button><el-button type="" :icon="Search" @click="onAction('query')">数据查询</el-button><el-button type="" :icon="Star" @click="onAction('fav')">收藏</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="数据对象">轨迹流动聚合表</el-descriptions-item>
        <el-descriptions-item label="类型">数据表</el-descriptions-item>
        <el-descriptions-item label="密级"><el-tag size="small" type="warning" effect="light">L3</el-tag></el-descriptions-item>
        <el-descriptions-item label="质量等级"><el-tag size="small" type="success" effect="light">A</el-tag></el-descriptions-item>
        <el-descriptions-item label="更新时间">10 分钟前</el-descriptions-item>
        <el-descriptions-item label="授权状态"><el-tag size="small" effect="light">已授权</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card shadow="never" class="page-card">
      <el-tabs v-model="activeTab">
      <el-tab-pane label="概览" name="overview">
        <el-empty description="概览内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="字段" name="fields">
        <el-empty description="字段内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="质量" name="quality">
        <el-empty description="质量内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="血缘" name="lineage">
        <el-empty description="血缘内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="权限" name="perm">
        <el-empty description="权限内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="使用情况" name="usage">
        <el-empty description="使用情况内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      </el-tabs>
    </el-card>  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Search, Download, View, Edit, Delete, MagicStick, Star, Key } from '@element-plus/icons-vue'

const activeTab = ref('overview')
const router = useRouter()

function onAction(a) {
  if (a === 'apply') router.push('/workbench/mine/apply')
  else ElMessage.info('操作: ' + a)
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
