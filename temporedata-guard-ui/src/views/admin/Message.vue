<template>
  <div class="message-page">
    <div class="page-header">
      <h2 class="page-title">消息中心</h2>
      <span class="page-sub">预警、审批、任务与系统消息集中处理</span>
      <div class="header-actions">
        <el-button size="small" type="primary" plain :disabled="!unreadTotal" @click="handleMarkAllRead">全部已读</el-button>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="msg-stats">
      <div class="msg-stat-card">
        <div class="ms-num">{{ unreadTotal }}</div><div class="ms-label">未读消息</div>
      </div>
      <div class="msg-stat-card">
        <div class="ms-num">{{ todayCount }}</div><div class="ms-label">今日消息</div>
      </div>
      <div class="msg-stat-card alert">
        <div class="ms-num">{{ urgentCount }}</div><div class="ms-label">紧急预警</div>
      </div>
    </div>

    <!-- 分类与级别筛选 -->
    <div class="msg-filter">
      <el-radio-group v-model="category" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="alert">预警</el-radio-button>
        <el-radio-button label="approval">审批</el-radio-button>
        <el-radio-button label="task">任务</el-radio-button>
        <el-radio-button label="system">系统</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="level" size="small" style="margin-left:16px">
        <el-radio-button label="all">全部级别</el-radio-button>
        <el-radio-button label="URGENT">紧急</el-radio-button>
        <el-radio-button label="IMPORTANT">重要</el-radio-button>
        <el-radio-button label="NORMAL">一般</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 消息列表 -->
    <el-card shadow="never" class="msg-card" v-loading="loading">
      <div v-if="!filtered.length" class="msg-empty">暂无消息</div>
      <div v-for="m in filtered" :key="m.id" :class="['msg-row', { unread: m.isRead !== 1 }]" @click="openDetail(m)">
        <div class="msg-icon" :style="{ background: catStyle(m).bg, color: catStyle(m).color }">
          <el-icon :size="16"><component :is="catStyle(m).icon" /></el-icon>
        </div>
        <div class="msg-body">
          <div class="msg-title-line">
            <span class="msg-dot" v-if="m.isRead !== 1"></span>
            <span class="msg-title">{{ m.title }}</span>
            <el-tag size="small" :type="levelType(m)" effect="light" class="msg-level-tag">{{ levelLabel(m) }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ categoryLabel(m) }}</el-tag>
          </div>
          <div class="msg-content">{{ summary(m.content) }}</div>
        </div>
        <div class="msg-time">{{ formatTime(m.createTime) }}</div>
        <div class="msg-actions" @click.stop>
          <el-button v-if="m.isRead !== 1" size="small" link type="primary" @click="handleMarkRead(m)">已读</el-button>
          <el-button size="small" link type="danger" @click="handleDelete(m)">删除</el-button>
        </div>
      </div>
    </el-card>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '消息详情'" width="480px" :close-on-click-modal="false">
      <div class="detail-meta">
        <el-tag size="small" :type="levelType(detail)" effect="light">{{ levelLabel(detail) }}</el-tag>
        <el-tag size="small" type="info" effect="plain">{{ categoryLabel(detail) }}</el-tag>
        <span class="detail-time">{{ detail ? formatTime(detail.createTime) : '' }}</span>
      </div>
      <div class="detail-content">{{ detail?.content }}</div>
      <template #footer>
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && detail.isRead !== 1" size="small" type="primary" @click="handleMarkRead(detail); detailVisible = false">标记已读</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { messageApi } from '@/api/modules/message'
import { ElMessage } from 'element-plus'
import { AlarmClock, ChatDotRound, Document, Setting, WarningFilled } from '@element-plus/icons-vue'

function getUserId() {
  try {
    const user = JSON.parse(localStorage.getItem('zqy_user') || '{}')
    return user.userId || '866103203da44dfaa53f9098826a334a'
  } catch {
    return '866103203da44dfaa53f9098826a334a'
  }
}

const userId = getUserId()
const loading = ref(false)
const messages = ref([])
const category = ref('all')
const level = ref('all')
const detailVisible = ref(false)
const detail = ref(null)

// 警务业务分类映射
function categoryOf(m) {
  const t = ((m.type || '') + '|' + (m.bizType || '')).toUpperCase()
  if (/ALERT|预警|异常|布控|线索/.test(t)) return 'alert'
  if (/APPROV|审批|申请|审核|批准/.test(t)) return 'approval'
  if (/TASK|REPORT|任务|报告|异步/.test(t)) return 'task'
  return 'system'
}
function levelOf(m) {
  const t = ((m.type || '') + '|' + (m.title || '')).toUpperCase()
  if (/URGENT|紧急/.test(t)) return 'URGENT'
  if (/IMPORTANT|重要|审批/.test(t)) return 'IMPORTANT'
  return 'NORMAL'
}
const categoryLabel = m => ({ alert: '预警', approval: '审批', task: '任务', system: '系统' }[categoryOf(m)])
const levelLabel = m => ({ URGENT: '紧急', IMPORTANT: '重要', NORMAL: '一般' }[levelOf(m)])
const levelType = m => ({ URGENT: 'danger', IMPORTANT: 'warning', NORMAL: 'info' }[levelOf(m)])
const catStyle = m => ({
  alert: { bg: '#fef2f2', color: '#dc2626', icon: WarningFilled },
  approval: { bg: '#fff7ed', color: '#d97706', icon: AlarmClock },
  task: { bg: '#eff6ff', color: '#1a6ff5', icon: Document },
  system: { bg: '#f3f4f6', color: '#6b7280', icon: Setting }
}[categoryOf(m)])

const filtered = computed(() =>
  messages.value.filter(m => (category.value === 'all' || categoryOf(m) === category.value) && (level.value === 'all' || levelOf(m) === level.value))
)
const unreadTotal = computed(() => messages.value.filter(m => m.isRead !== 1).length)
const todayCount = computed(() => {
  const t = new Date(); const start = new Date(t.getFullYear(), t.getMonth(), t.getDate()).getTime()
  return messages.value.filter(m => new Date(m.createTime || m.create_time || 0).getTime() >= start).length
})
const urgentCount = computed(() => messages.value.filter(m => levelOf(m) === 'URGENT' && m.isRead !== 1).length)

function summary(content) {
  const c = content || ''
  return c.length > 60 ? c.slice(0, 60) + '…' : c
}
function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function load() {
  loading.value = true
  try {
    const res = await messageApi.list(userId)
    const list = Array.isArray(res) ? res : (res.data || [])
    messages.value = [...list].sort((a, b) => new Date(b.createTime || b.create_time || 0) - new Date(a.createTime || a.create_time || 0))
  } catch { /* ignore */ } finally { loading.value = false }
}
function openDetail(m) { detail.value = m; detailVisible.value = true }
async function handleMarkRead(m) {
  try {
    await messageApi.markRead(m.id)
    m.isRead = 1
    ElMessage.success('已标记为已读')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}
async function handleMarkAllRead() {
  try {
    await messageApi.markAllRead(userId)
    messages.value.forEach(m => { m.isRead = 1 })
    ElMessage.success('全部已标记为已读')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}
async function handleDelete(m) {
  try {
    await messageApi.delete(m.id)
    messages.value = messages.value.filter(x => x.id !== m.id)
    ElMessage.success('已删除')
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}

onMounted(load)
</script>

<style scoped>
.message-page { padding: 0; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-sub { font-size: 12.5px; color: #9ca3af; flex: 1; }
.msg-stats { display: flex; gap: 12px; margin-bottom: 14px; }
.msg-stat-card {
  flex: 1; border: 1px solid #eef1f5; border-radius: 4px; padding: 12px 16px;
  display: flex; align-items: center; gap: 10px; background: #fbfcff;
}
.msg-stat-card.alert { background: #fefaf5; border-color: #fde8cd; }
.ms-num { font-size: 24px; font-weight: 700; color: #1a6ff5; }
.msg-stat-card.alert .ms-num { color: #d97706; }
.ms-label { font-size: 12.5px; color: #9ca3af; }
.msg-filter { margin-bottom: 12px; }
.msg-card { border-radius: 4px; }
.msg-row {
  display: flex; align-items: center; gap: 12px; padding: 12px 8px;
  border-bottom: 1px solid #f5f5f7; cursor: pointer; transition: background .15s;
}
.msg-row:hover { background: #f8faff; }
.msg-row.unread { background: #f6f9ff; }
.msg-icon {
  width: 34px; height: 34px; border-radius: 4px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.msg-body { flex: 1; min-width: 0; }
.msg-title-line { display: flex; align-items: center; gap: 8px; }
.msg-dot { width: 7px; height: 7px; border-radius: 50%; background: #dc2626; flex-shrink: 0; }
.msg-title { font-size: 13.5px; font-weight: 600; color: #1f2937; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.msg-content { font-size: 12.5px; color: #9ca3af; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.msg-time { font-size: 12px; color: #c3c8d1; width: 140px; flex-shrink: 0; text-align: right; }
.msg-actions { width: 110px; flex-shrink: 0; display: flex; justify-content: flex-end; visibility: hidden; }
.msg-row:hover .msg-actions { visibility: visible; }
.msg-empty { text-align: center; color: #9ca3af; padding: 48px 0; font-size: 13px; }
.detail-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.detail-time { font-size: 12px; color: #9ca3af; margin-left: auto; }
.detail-content { font-size: 13.5px; color: #374151; line-height: 1.9; white-space: pre-wrap; }
</style>
