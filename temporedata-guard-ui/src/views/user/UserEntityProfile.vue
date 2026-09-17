<template>
  <div class="user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">人员/车辆/组织对象画像</h2>
        <p class="page-desc">概览/关系/轨迹/案件/设备/车辆/数据来源 多维度研判</p>
      </div>
            <div class="page-actions"><el-button type="primary" :icon="Share" @click="onAction('graph')">关系分析</el-button><el-button type="" :icon="View" @click="onAction('track')">轨迹</el-button><el-button type="" :icon="Plus" @click="onAction('project')">加入专案</el-button><el-button type="" :icon="MagicStick" @click="onAction('summary')">研判摘要</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="perm-alert" title="权限继承：数据范围 + 密级 + 专案范围 + 字段脱敏统一控制（后端二次校验）" />
    <el-card shadow="never" class="page-card">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="对象">{{ profile.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ profile.type }}</el-descriptions-item>
        <el-descriptions-item label="密级"><el-tag size="small" type="danger" effect="light">{{ profile.level }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
        <el-descriptions-item label="风险标签"><el-tag v-for="t in profile.riskTags" :key="t" size="small" effect="light" style="margin-right:4px">{{ t }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="关联案件">{{ profile.cases }}</el-descriptions-item>
        <el-descriptions-item label="研判摘要" :span="3">{{ profile.summary }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="projectDialog" title="加入专案" width="440px" destroy-on-close>
      <el-select v-model="selectedProject" placeholder="选择专案" style="width: 100%">
        <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <template #footer>
        <el-button @click="projectDialog = false">取消</el-button>
        <el-button type="primary" @click="doJoinProject">加入</el-button>
      </template>
    </el-dialog>
    <el-card shadow="never" class="page-card">
      <el-tabs v-model="activeTab">
      <el-tab-pane label="概览" name="overview">
        <el-empty description="概览内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="关系" name="relation">
        <el-empty description="关系内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="轨迹" name="track">
        <el-empty description="轨迹内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="案件" name="case">
        <el-empty description="案件内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="设备" name="device">
        <el-empty description="设备内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="车辆" name="vehicle">
        <el-empty description="车辆内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      <el-tab-pane label="数据来源" name="source">
        <el-empty description="数据来源内容占位（接入业务数据）" :image-size="60" />
      </el-tab-pane>
      </el-tabs>
    </el-card>  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { Plus, Search, Download, View, Edit, Delete, MagicStick, Share } from '@element-plus/icons-vue'

const activeTab = ref('overview')
const route = useRoute()
const profile = ref({ name: '-', type: '人员', level: 'L3', phone: '***', riskTags: [], cases: 0, summary: '' })

const projectDialog = ref(false)
const projectOptions = ref([])
const selectedProject = ref('')

async function onAction(a) {
  if (a === 'project') {
    try {
      projectOptions.value = await userApi.listProjects() || []
      projectDialog.value = true
    } catch (e) { ElMessage.error(e.message || '加载专案失败') }
  } else {
    ElMessage.info('操作: ' + a)
  }
}

async function doJoinProject() {
  if (!selectedProject.value) { ElMessage.warning('请选择专案'); return }
  try {
    await userApi.addProjectObject(selectedProject.value, {
      name: profile.value.name, type: profile.value.type, role: '关联对象', evidence: '研判关联'
    })
    ElMessage.success('已加入专案')
    projectDialog.value = false
  } catch (e) { ElMessage.error(e.message || '加入失败') }
}

onMounted(async () => {
  const id = route.query.name || '张某某'
  try {
    const p = await userApi.entityProfile(id)
    profile.value = p
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
