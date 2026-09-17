<template>
  <div class="license-page">
    <PageHeader title="平台授权" desc="采购合规授权：授权单位、模块、数据量与期限管理，超期自动降级保护">
      <template #actions>
        <el-button type="primary" :icon="Upload" @click="showInstallDialog">安装许可证</el-button>
      </template>
    </PageHeader>

    <!-- 超期降级横幅 -->
    <el-alert
      v-if="licenseStatus === 'EXPIRED'"
      type="error"
      :closable="false"
      show-icon
      class="license-banner"
      title="授权已过期，平台进入降级保护模式：禁止新增数据写入与新建用户，保留数据查询与审计导出（合规保留）"
    />
    <el-alert
      v-else-if="licenseStatus === 'EXPIRING'"
      type="warning"
      :closable="false"
      show-icon
      class="license-banner"
      :title="`授权将于 ${licenseInfo.expireTime} 到期（剩余 ${remainDays} 天），请及时联系厂商续期，到期后将进入降级保护模式`"
    />

    <!-- 状态总览 -->
    <div class="page-body">
      <el-row :gutter="20" class="license-cards">
        <el-col :span="6">
          <div class="license-stat-card">
            <div class="stat-label">授权状态</div>
            <StatusTag
              :type="statusTagType"
              :label="statusLabel"
            />
            <div v-if="licenseStatus === 'EXPIRING'" class="stat-warn">剩余 {{ remainDays }} 天</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="license-stat-card">
            <div class="stat-label">授权单位</div>
            <div class="stat-value small">{{ licenseInfo.organization || '未安装' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="license-stat-card">
            <div class="stat-label">授权用户数</div>
            <div class="stat-value">{{ licenseInfo.maxUsers || '-' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="license-stat-card">
            <div class="stat-label">到期时间</div>
            <div class="stat-value">{{ licenseInfo.expireTime || '-' }}</div>
          </div>
        </el-col>
      </el-row>

      <el-descriptions :column="2" border class="license-detail">
        <el-descriptions-item label="许可证类型">{{ licenseInfo.type || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ licenseInfo.project || '-' }}</el-descriptions-item>
        <el-descriptions-item label="签发时间">{{ licenseInfo.issueTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权数据量">{{ licenseInfo.dataQuota ? (licenseInfo.dataQuota / 100000000).toFixed(0) + ' 亿条' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="最大数据源数量">{{ licenseInfo.maxDatasources || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最大作业流数量">{{ licenseInfo.maxWorkflows || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 模块授权清单 -->
      <div class="module-section">
        <div class="module-title">模块授权清单</div>
        <el-row :gutter="12">
          <el-col v-for="m in moduleList" :key="m.name" :span="6" class="module-col">
            <div class="module-card">
              <div class="module-name">{{ m.name }}</div>
              <el-tag :type="m.granted ? 'success' : 'info'" size="small" effect="light">
                {{ m.granted ? '已授权' : '未授权' }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
        <div v-if="!hasLicense" class="module-empty">安装许可证后自动解析授权模块清单</div>
      </div>
    </div>

    <el-dialog v-model="installDialogVisible" title="安装许可证" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="许可证密钥" prop="licenseKey">
          <el-input v-model="form.licenseKey" type="textarea" :rows="6" placeholder="请粘贴许可证密钥内容（含授权单位、模块、配额与有效期）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleInstall">安装</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { licenseApi } from '@/api/modules/license'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const licenseInfo = ref({})
const installDialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ licenseKey: '' })
const rules = {
  licenseKey: [{ required: true, message: '请输入许可证密钥', trigger: 'blur' }]
}

const PLATFORM_MODULES = ['情报检索', '轨迹分析', '资金分析', '以图找人', '涉诈研判', '布控预警', '实体图谱', 'Agent助手']

const hasLicense = computed(() => !!(licenseInfo.value && licenseInfo.value.valid))
const remainDays = computed(() => {
  const t = licenseInfo.value.expireTime
  if (!t) return 0
  return Math.max(0, Math.ceil((new Date(t).getTime() - Date.now()) / 864e5))
})
const licenseStatus = computed(() => {
  if (!hasLicense.value) return 'NONE'
  if (remainDays.value <= 0) return 'EXPIRED'
  if (remainDays.value <= 30) return 'EXPIRING'
  return 'VALID'
})
const statusLabel = computed(() => ({ VALID: '有效', EXPIRING: '即将到期', EXPIRED: '已过期', NONE: '未授权' }[licenseStatus.value]))
const statusTagType = computed(() => ({ VALID: 'success', EXPIRING: 'warning', EXPIRED: 'danger', NONE: 'info' }[licenseStatus.value]))
const moduleList = computed(() => {
  const granted = (licenseInfo.value.modules || [])
  if (!granted.length) return PLATFORM_MODULES.map(name => ({ name, granted: true }))
  return PLATFORM_MODULES.map(name => ({ name, granted: granted.includes(name) || granted.includes(moduleCodeOf(name)) }))
})
function moduleCodeOf(name) {
  return { '情报检索': 'INTELLIGENCE', '轨迹分析': 'TRACK', '资金分析': 'FUND', '以图找人': 'PHOTO', '涉诈研判': 'FRAUD', '布控预警': 'WATCH', '实体图谱': 'GRAPH', 'Agent助手': 'AGENT' }[name] || name
}

async function fetchData() {
  try {
    const data = await licenseApi.status()
    if (data) licenseInfo.value = data
  } catch (err) {
    // license may not be installed yet
  }
}

function showInstallDialog() {
  form.licenseKey = ''
  installDialogVisible.value = true
}

async function handleInstall() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await licenseApi.install(form.licenseKey)
    ElMessage.success('许可证安装成功')
    installDialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '许可证安装失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.license-page { max-width: 1200px; }
.license-banner { margin-bottom: 16px; border-radius: 4px; }
.page-body {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.license-cards { margin-bottom: 20px; }
.license-stat-card {
  padding: 18px;
  background: #f9fafb;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}
.stat-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.stat-value { font-size: 22px; font-weight: 600; color: #1f2937; margin-top: 4px; }
.stat-value.small { font-size: 16px; }
.stat-warn { font-size: 12px; color: #d97706; margin-top: 6px; }
.license-detail { margin-top: 0; }
.module-section { margin-top: 20px; }
.module-title { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 12px; }
.module-col { margin-bottom: 10px; }
.module-card {
  display: flex; align-items: center; justify-content: space-between;
  border: 1px solid #eef1f5; border-radius: 4px; padding: 10px 12px; background: #fbfcff;
}
.module-name { font-size: 13px; color: #374151; }
.module-empty { font-size: 12px; color: #9ca3af; margin-top: 8px; }
</style>
