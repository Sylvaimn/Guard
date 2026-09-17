<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">专案总览</h2>
        <p class="page-desc">对象/线索/任务/图谱/报告/动态 统一视图</p>
      </div>
          </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="专案">XX 电信诈骗专案</el-descriptions-item>
        <el-descriptions-item label="密级"><el-tag size="small" type="danger" effect="light">L1</el-tag></el-descriptions-item>
        <el-descriptions-item label="状态">进行中</el-descriptions-item>
        <el-descriptions-item label="对象">12</el-descriptions-item>
        <el-descriptions-item label="线索">26</el-descriptions-item>
        <el-descriptions-item label="报告">4</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card shadow="never" class="page-card">
      <el-tabs v-model="activeTab">
      <el-tab-pane label="概览" name="overview">
        <el-empty description="概览内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="对象" name="objects">
        <el-empty description="对象内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="线索" name="clues">
        <el-empty description="线索内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="关系" name="graph">
        <el-empty description="关系内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="轨迹" name="track">
        <el-empty description="轨迹内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="数据" name="data">
        <el-empty description="数据内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="研判记录" name="notes">
        <el-empty description="研判记录内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="文书" name="docs">
        <el-empty description="文书内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      </el-tabs>
    </el-card>  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { Plus, Search, Download, View, Edit, Delete, MagicStick } from '@element-plus/icons-vue'

const activeTab = ref('overview')
const route = useRoute()
const project = ref({ name: '-', level: 'L3', status: '-', objects: 0, tasks: 0, reports: 0 })
const tabsData = ref({ objects: [], tasks: [], reports: [] })

function onAction(a) { ElMessage.info('操作: ' + a) }

onMounted(async () => {
  const id = route.query.id
  if (!id) return
  try {
    const d = await userApi.projectDetail(id)
    project.value = d
    tabsData.value = { objects: d.objects || [], tasks: d.tasks || [], reports: d.reports || [] }
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
