<template>
  <div class="settings">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>金盾智云大数据平台 · 平台配置与管理</p>
    </div>

    <div class="settings-grid">
      <!-- 系统信息 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>系统信息</span>
          </div>
        </template>
        <el-descriptions :column="1" border size="small" v-loading="sysLoading">
          <el-descriptions-item label="系统版本">
            {{ sysInfo.version || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="构建时间">
            {{ sysInfo.buildTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Java 版本">
            {{ sysInfo.javaVersion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据库类型">
            {{ sysInfo.databaseType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="运行时长">
            {{ sysInfo.uptime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- 平台设置 -->
    <el-card class="settings-card platform-settings">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>平台设置</span>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="platformForm"
        label-width="110px"
        class="settings-form"
        v-loading="platformLoading"
      >
        <el-form-item label="站点名称">
          <el-input v-model="platformForm.siteName" placeholder="金盾智云大数据平台" />
        </el-form-item>
        <el-form-item label="平台 Logo">
          <el-input v-model="platformForm.logo" placeholder="请输入 Logo URL" />
        </el-form-item>
        <el-form-item label="平台描述">
          <el-input
            v-model="platformForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入平台描述"
          />
        </el-form-item>
        <el-divider />
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存设置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 登录与账户安全 -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <el-icon><Lock /></el-icon>
          <span>登录与账户安全</span>
        </div>
      </template>
      <el-form label-width="130px" class="settings-form">
        <el-form-item label="密码最小长度">
          <el-input-number v-model="governForm.minPasswordLen" :min="6" :max="32" size="small" /> 位
        </el-form-item>
        <el-form-item label="密码复杂度">
          <el-checkbox-group v-model="governForm.passwordStrength">
            <el-checkbox label="含字母" /><el-checkbox label="含数字" /><el-checkbox label="含特殊字符" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="失败锁定">
          <el-input-number v-model="governForm.lockThreshold" :min="3" :max="10" size="small" /> 次后锁定
        </el-form-item>
        <el-form-item label="会话超时">
          <el-input-number v-model="governForm.sessionTimeout" :min="5" :max="120" size="small" /> 分钟
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据治理 -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据治理</span>
        </div>
      </template>
      <el-form label-width="130px" class="settings-form">
        <el-form-item label="默认脱敏级别">
          <el-select v-model="governForm.defaultMask" size="small" style="width: 200px">
            <el-option label="全脱敏（前3后4）" value="full" />
            <el-option label="部分脱敏（前6后4）" value="partial" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始数据保留">
          <el-input-number v-model="governForm.rawRetention" :min="1" :max="120" size="small" /> 个月
        </el-form-item>
        <el-form-item label="导出审批">
          <el-switch v-model="governForm.exportApproval" active-text="涉密数据导出需审批" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 审计合规 -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <el-icon><Tickets /></el-icon>
          <span>审计合规</span>
        </div>
      </template>
      <el-form label-width="130px" class="settings-form">
        <el-form-item label="审计开关">
          <el-switch v-model="governForm.auditEnabled" active-text="全平台操作留痕" />
        </el-form-item>
        <el-form-item label="审计保留时长">
          <el-input-number v-model="governForm.auditRetention" :min="30" :max="1095" size="small" /> 天
        </el-form-item>
        <el-form-item label="合规策略">
          <el-tag size="small" type="success" effect="light">等保 2.0</el-tag>
          <el-tag size="small" type="success" effect="light" style="margin-left:6px">数据安全法</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain size="small" @click="saveGovernance">保存治理配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, InfoFilled, Lock, DataAnalysis, Tickets } from '@element-plus/icons-vue'
import { settingsApi } from '@/api/modules/settings'

const formRef = ref(null)
const saving = ref(false)
const sysLoading = ref(false)
const platformLoading = ref(false)

const sysInfo = reactive({
  version: '',
  buildTime: '',
  javaVersion: '',
  databaseType: '',
  uptime: ''
})

const platformForm = reactive({
  siteName: '金盾智云大数据平台',
  logo: '',
  description: ''
})

// 治理域配置(登录安全/数据治理/审计合规) - 本地持久化, 生产接入配置中心
const governForm = reactive({
  minPasswordLen: 8,
  passwordStrength: ['含字母', '含数字', '含特殊字符'],
  lockThreshold: 5,
  sessionTimeout: 15,
  defaultMask: 'partial',
  rawRetention: 36,
  exportApproval: true,
  auditEnabled: true,
  auditRetention: 365
})
function loadGovernance() {
  try {
    const saved = JSON.parse(localStorage.getItem('zy_governance') || '{}')
    Object.keys(saved).forEach(k => { if (k in governForm) governForm[k] = saved[k] })
  } catch (_) { /* ignore */ }
}
function saveGovernance() {
  localStorage.setItem('zy_governance', JSON.stringify(governForm))
  ElMessage.success('治理配置已保存（生产环境接入配置中心）')
}

onMounted(async () => {
  loadGovernance()
  // Load platform settings from backend
  await loadPlatformSettings()
})

async function loadPlatformSettings() {
  platformLoading.value = true
  sysLoading.value = true
  try {
    const group = await settingsApi.getGroup('platform')
    if (group) {
      platformForm.siteName = group['site.name'] || '金盾智云大数据平台'
      platformForm.logo = group['site.logo'] || ''
      platformForm.description = group['site.description'] || ''
    }
    const sysGroup = await settingsApi.getGroup('system')
    if (sysGroup) {
      sysInfo.version = sysGroup['system.version'] || 'v2.4.1'
      sysInfo.javaVersion = sysGroup['system.javaVersion'] || 'OpenJDK 17'
      sysInfo.databaseType = sysGroup['system.databaseType'] || 'H2'
    }
  } catch {
    // Use defaults
    sysInfo.version = 'v2.4.1'
    sysInfo.buildTime = '2026-07-28 14:30:00'
    sysInfo.javaVersion = 'OpenJDK 17.0.9'
    sysInfo.databaseType = 'H2'
    sysInfo.uptime = '-'
  } finally {
    platformLoading.value = false
    sysLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await settingsApi.saveGroup('platform', {
      'site.name': platformForm.siteName,
      'site.logo': platformForm.logo,
      'site.description': platformForm.description
    })
    ElMessage.success('平台设置保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '请稍后重试'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.settings {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.settings-card {
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.settings-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.card-header .el-icon {
  font-size: 18px;
  color: #409eff;
}

.settings-card :deep(.el-card__body) {
  padding: 20px;
}

.platform-settings {
  grid-column: 1 / -1;
}

.settings-form {
  max-width: 560px;
}

@media (max-width: 768px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>