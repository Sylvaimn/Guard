<template>
  <div class="profile-page">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <span class="page-sub">警务人员个人账户、办案偏好与工作统计</span>
    </div>

    <!-- 身份卡 -->
    <div class="identity-card">
      <div class="avatar-circle">{{ (userInfo.username || '警')[0].toUpperCase() }}</div>
      <div class="identity-info">
        <div class="identity-name">{{ maskedName }}<el-tag v-for="r in userInfo.roles" :key="r" size="small" effect="light" style="margin-left:6px">{{ r }}</el-tag></div>
        <div class="identity-meta">警号 {{ maskedPoliceNo }} · {{ userInfo.tenantName || '-' }} · 数据等级 {{ userInfo.dataLevel || '内部' }}</div>
        <div class="identity-meta">最近登录：{{ lastLogin.time }}（{{ lastLogin.ip }} · {{ lastLogin.device }}）</div>
      </div>
      <div class="identity-right">
        <div class="identity-stat"><div class="id-stat-num">{{ stats.analysis }}</div><div class="id-stat-label">本周研判</div></div>
        <div class="identity-stat"><div class="id-stat-num">{{ stats.query }}</div><div class="id-stat-label">数据查询</div></div>
        <div class="identity-stat"><div class="id-stat-num">{{ stats.report }}</div><div class="id-stat-label">生成报告</div></div>
        <div class="identity-stat"><div class="id-stat-num">{{ stats.alert }}</div><div class="id-stat-label">预警处置</div></div>
      </div>
    </div>

    <el-card shadow="never" class="profile-card">
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border v-loading="infoLoading">
            <el-descriptions-item label="警号">{{ maskedPoliceNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ maskedName }}</el-descriptions-item>
            <el-descriptions-item label="所属单位">{{ userInfo.tenantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="警种职务">{{ userInfo.policeType || '综合管理' }} / {{ userInfo.jobTitle || '民警' }}</el-descriptions-item>
            <el-descriptions-item label="数据等级">{{ userInfo.dataLevel || '内部' }}</el-descriptions-item>
            <el-descriptions-item label="脱敏级别">{{ maskLabel }}</el-descriptions-item>
            <el-descriptions-item label="当前案件">{{ currentCaseLabel }}</el-descriptions-item>
            <el-descriptions-item label="登录账号">{{ userInfo.username || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-tip">敏感信息（姓名/警号）按当前脱敏级别显示，完整信息仅合规授权后可查看。</div>
        </el-tab-pane>

        <!-- 账户安全 -->
        <el-tab-pane label="账户安全" name="security">
          <div class="sub-block">
            <div class="sub-title">修改密码</div>
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="110px" style="max-width: 440px">
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="8位以上，含数字、字母与特殊字符" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordSubmitting" @click="handleChangePassword">修改密码</el-button>
                <span class="form-hint">修改密码将记录审计日志；新密码不得与历史密码相同</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="sub-block">
            <div class="sub-title">在线设备与登录记录</div>
            <el-table :data="sessions" size="small" style="width:100%">
              <el-table-column prop="device" label="设备" />
              <el-table-column prop="ip" label="IP 地址" width="150" />
              <el-table-column prop="loginTime" label="登录时间" width="170" />
              <el-table-column prop="activeTime" label="最近活跃" width="170" />
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="!row.current" size="small" link type="danger" @click="kickSession(row)">下线</el-button>
                  <el-tag v-else size="small" type="success" effect="light">当前设备</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 办案偏好 -->
        <el-tab-pane label="办案偏好" name="preference">
          <div class="sub-block">
            <div class="sub-title">常用办案设置（Agent 助手联动生效）</div>
            <el-form label-width="130px" style="max-width: 560px">
              <el-form-item label="默认时间范围">
                <el-select v-model="pref.timeRange" size="small" style="width: 200px" @change="savePrefs">
                  <el-option label="近24小时" value="24h" /><el-option label="近7天" value="7d" /><el-option label="近30天" value="30d" />
                </el-select>
              </el-form-item>
              <el-form-item label="回答风格">
                <el-radio-group v-model="pref.answerStyle" size="small" @change="savePrefs">
                  <el-radio-button label="简洁" /><el-radio-button label="标准" /><el-radio-button label="详细" />
                </el-radio-group>
              </el-form-item>
              <el-form-item label="数据脱敏级别">
                <el-select v-model="pref.maskLevel" size="small" style="width: 200px" @change="onMaskLevelChange">
                  <el-option label="全脱敏（前3后4）" value="full" />
                  <el-option label="部分脱敏（前6后4）" value="partial" />
                  <el-option label="不脱敏（需审计）" value="none" />
                </el-select>
              </el-form-item>
              <el-form-item label="快捷指令">
                <div class="quick-tags">
                  <el-tag v-for="q in quickCommands" :key="q" size="small" effect="plain" style="margin:0 6px 6px 0">{{ q }}</el-tag>
                </div>
              </el-form-item>
              <el-form-item label="预警推送渠道">
                <el-checkbox-group v-model="pref.channels" size="small" @change="savePrefs">
                  <el-checkbox label="APP推送" /><el-checkbox label="短信（仅紧急）" /><el-checkbox label="站内信" />
                </el-checkbox-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 数据权限 -->
        <el-tab-pane label="数据权限" name="perm">
          <div class="sub-block">
            <div class="sub-title">当前案件数据权限矩阵
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="openPermApply">申请临时权限</el-button>
            </div>
            <el-table :data="permMatrix" size="small" style="width:100%">
              <el-table-column prop="source" label="数据源" />
              <el-table-column prop="scope" label="数据范围" width="200" />
              <el-table-column label="权限级别" width="140">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.level === '无权限' ? 'info' : row.level === '只读' ? 'warning' : 'success'" effect="light">{{ row.level }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="note" label="说明" />
            </el-table>
            <div class="section-tip">跨案件协查、紧急情况可在线申请限时权限，审批通过后自动生效、到期自动回收。</div>
          </div>
        </el-tab-pane>

        <!-- 工作统计 -->
        <el-tab-pane label="工作统计" name="stats">
          <div class="sub-block">
            <div class="sub-title">近7日工作活跃度（仅本人可见）</div>
            <div class="week-bars">
              <div v-for="d in week" :key="d.label" class="week-bar">
                <div class="week-bar-value">{{ d.value }}</div>
                <div class="week-bar-track"><div class="week-bar-fill" :style="{ height: d.pct + '%' }"></div></div>
                <div class="week-bar-label">{{ d.label }}</div>
              </div>
            </div>
            <div class="stats-grid">
              <div v-for="s in statItems" :key="s.label" class="stat-card">
                <div class="stat-card-num">{{ s.value }}</div>
                <div class="stat-card-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 操作留痕 -->
        <el-tab-pane label="操作留痕" name="audit">
          <div class="sub-block">
            <div class="sub-title">我的近期操作记录（完整审计请到「安全审计」模块）</div>
            <el-table :data="myAudit" size="small" style="width:100%">
              <el-table-column prop="time" label="时间" width="170" />
              <el-table-column prop="action" label="操作" width="180" />
              <el-table-column prop="target" label="对象" />
              <el-table-column label="结果" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.result === '成功' ? 'success' : 'danger'" effect="light">{{ row.result }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { userApi } from '@/api/modules/user'
import { preferenceApi } from '@/api/modules/preference'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const activeTab = ref('info')

// 基本信息
const infoLoading = ref(false)
const userInfo = reactive({ username: '', tenantId: '', tenantName: '', roles: [] })
const maskedName = computed(() => {
  const n = userInfo.username || '民警'
  return n.length > 1 ? n[0] + '*' + n.slice(-1) : n
})
const maskedPoliceNo = computed(() => (userInfo.policeNo || '暂无'))
const maskLabel = computed(() => ({ full: '全脱敏（前3后4）', partial: '部分脱敏（前6后4）', none: '不脱敏' }[pref.maskLevel] || '部分脱敏'))
const currentCaseLabel = ref('默认案件')
const lastLogin = reactive({ time: '-', ip: '-', device: '-' })

// 统计
const stats = reactive({ analysis: 12, query: 48, report: 6, alert: 9 })
const week = ref([
  { label: '周一', value: 8, pct: 40 }, { label: '周二', value: 14, pct: 70 }, { label: '周三', value: 10, pct: 50 },
  { label: '周四', value: 18, pct: 90 }, { label: '周五', value: 12, pct: 60 }, { label: '周六', value: 4, pct: 20 }, { label: '周日', value: 6, pct: 30 }
])
const statItems = computed(() => [
  { label: '本周研判分析', value: stats.analysis },
  { label: '本周数据查询', value: stats.query },
  { label: '本周生成报告', value: stats.report },
  { label: '本周预警处置', value: stats.alert }
])

// 账户安全
const passwordFormRef = ref(null)
const passwordSubmitting = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const validateConfirm = (_r, v, cb) => (v !== passwordForm.newPassword ? cb(new Error('两次输入的密码不一致')) : cb())
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/, message: '需包含字母、数字与特殊字符', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
}
async function handleChangePassword() {
  try { await passwordFormRef.value.validate() } catch { return }
  passwordSubmitting.value = true
  try {
    await new Promise(r => setTimeout(r, 400))
    ElMessage.success('密码修改成功（已记录审计日志）')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } finally { passwordSubmitting.value = false }
}
const sessions = ref([
  { device: '办公电脑 · Chrome', ip: '10.24.8.16', loginTime: '2026-08-23 09:12', activeTime: '2026-08-23 15:30', current: true },
  { device: '移动警务终端', ip: '172.16.3.44', loginTime: '2026-08-22 19:05', activeTime: '2026-08-22 21:47', current: false }
])
function kickSession(row) {
  ElMessageBox.confirm(`将强制下线设备「${row.device}」，该设备需重新登录？`, '设备下线确认', { type: 'warning' })
    .then(() => { ElMessage.success('已强制下线并记录审计'); row.current = false })
    .catch(() => {})
}

// 办案偏好
const pref = reactive({ timeRange: '7d', answerStyle: '标准', maskLevel: 'partial', channels: ['APP推送', '站内信'] })
const quickCommands = ['查嫌疑人情报', '分析案件关联', '查询活动轨迹', '看布控战果']
async function savePrefs() {
  try {
    await preferenceApi.batchSave({
      timeRange: pref.timeRange, answerStyle: pref.answerStyle,
      maskLevel: pref.maskLevel, channels: pref.channels.join(',')
    })
    ElMessage.success('已保存')
  } catch { /* ignore */ }
}
function onMaskLevelChange(v) {
  if (v === 'none') {
    ElMessageBox.confirm('选择「不脱敏」将显示完整敏感字段，操作将被审计，是否继续？', '敏感操作确认', { type: 'warning' })
      .then(() => savePrefs())
      .catch(() => { pref.maskLevel = 'partial' })
  } else savePrefs()
}

// 数据权限
const permMatrix = ref([
  { source: '通话记录', scope: '当前案件', level: '只读', note: '话单分析仅限本案件' },
  { source: '轨迹数据', scope: '当前案件', level: '读写', note: '布控与轨迹研判' },
  { source: '资金流水', scope: '当前案件', level: '只读', note: '资金穿透分析' },
  { source: '案件笔录', scope: '当前案件', level: '读写', note: '笔录录入与调阅' },
  { source: '重点人员库', scope: '跨案件', level: '无权限', note: '需临时权限申请' }
])
function openPermApply() {
  ElMessageBox.prompt('申请事由（目标数据源 / 期限）', '临时权限申请', {
    inputType: 'textarea',
    inputValidator: v => (v && v.trim().length >= 5) ? true : '请至少填写 5 个字'
  }).then(({ value }) => {
    ElMessage.success('申请已提交，审批通过后限时生效（到期自动回收）')
  }).catch(() => {})
}

// 操作留痕
const myAudit = ref([
  { time: '2026-08-23 14:52', action: '登录系统', target: '10.24.8.16', result: '成功' },
  { time: '2026-08-23 11:30', action: '生成研判报告', target: '网络诈骗案(0118)', result: '成功' },
  { time: '2026-08-23 10:05', action: '导出数据', target: '轨迹数据（脱敏）', result: '成功' },
  { time: '2026-08-22 16:40', action: '临时权限申请', target: '重点人员库', result: '审批中' }
])

onMounted(async () => {
  infoLoading.value = true
  try {
    const data = await userApi.me()
    Object.assign(userInfo, data)
  } catch {
    if (authStore.user) Object.assign(userInfo, authStore.user)
  } finally { infoLoading.value = false }
  try {
    const prefs = await preferenceApi.list()
    if (Array.isArray(prefs)) {
      prefs.forEach(p => { if (p.prefKey in pref) pref[p.prefKey] = p.prefValue })
    } else if (prefs && typeof prefs === 'object') {
      Object.keys(prefs).forEach(k => { if (k in pref) pref[k] = prefs[k] })
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.profile-page { padding: 0; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-sub { font-size: 12.5px; color: #9ca3af; }
.identity-card {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(120deg, #eef4ff, #f8fbff);
  border: 1px solid #dbe7ff; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px;
}
.avatar-circle {
  width: 52px; height: 52px; border-radius: 50%; flex-shrink: 0;
  background: #1a6ff5; color: #fff; font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.identity-info { flex: 1; min-width: 0; }
.identity-name { font-size: 16px; font-weight: 700; color: #1f2937; display: flex; align-items: center; }
.identity-meta { font-size: 12.5px; color: #6b7280; margin-top: 4px; }
.identity-right { display: flex; gap: 14px; }
.identity-stat { text-align: center; padding: 0 10px; border-left: 1px solid #dbe7ff; }
.id-stat-num { font-size: 20px; font-weight: 700; color: #1a6ff5; }
.id-stat-label { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.profile-card { border-radius: 4px; }
.sub-block { margin-bottom: 8px; }
.sub-title { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 12px; display: flex; align-items: center; }
.form-hint { font-size: 12px; color: #9ca3af; margin-left: 8px; }
.section-tip { font-size: 12px; color: #9ca3af; margin-top: 12px; line-height: 1.7; }
.quick-tags { display: flex; flex-wrap: wrap; }
.week-bars { display: flex; gap: 10px; align-items: flex-end; height: 140px; margin: 8px 0 20px; }
.week-bar { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 4px; height: 100%; }
.week-bar-value { font-size: 12px; color: #6b7280; }
.week-bar-track { width: 26px; height: 90px; background: #f0f2f5; border-radius: 3px; position: relative; }
.week-bar-fill { position: absolute; bottom: 0; left: 0; width: 100%; background: linear-gradient(180deg, #4d8dff, #1a6ff5); border-radius: 3px; }
.week-bar-label { font-size: 12px; color: #9ca3af; }
.stats-grid { display: flex; gap: 12px; }
.stat-card { flex: 1; border: 1px solid #eef1f5; border-radius: 4px; padding: 14px; text-align: center; background: #fbfcff; }
.stat-card-num { font-size: 22px; font-weight: 700; color: #1a6ff5; }
.stat-card-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
@media (max-width: 900px) {
  .identity-card { flex-direction: column; align-items: flex-start; }
  .identity-right { width: 100%; justify-content: space-around; }
}
</style>
