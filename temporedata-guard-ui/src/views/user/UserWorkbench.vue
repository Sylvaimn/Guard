<template>
  <div class="user-workbench">
    <div class="wb-hero">
      <div class="wb-greeting">{{ greeting }}，{{ authStore.user?.username || '用户' }}</div>
      <div class="wb-sub">{{ today }} · 今天有 <b>6</b> 项待处理</div>
      <div class="wb-actions">
        <el-button type="primary" :icon="Search" @click="$router.push('/workbench/intelligence')">情报检索</el-button>
        <el-button :icon="MagicStick" @click="$router.push('/workbench/agent')">金盾智助</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="wb-stats">
      <el-col :span="6"><div class="stat-card danger"><div class="v">8</div><div class="l">待办事项</div></div></el-col>
      <el-col :span="6"><div class="stat-card primary"><div class="v">3</div><div class="l">我的专案</div></div></el-col>
      <el-col :span="6"><div class="stat-card success"><div class="v">12</div><div class="l">已授权数据</div></div></el-col>
      <el-col :span="6"><div class="stat-card warning"><div class="v">2</div><div class="l">待审申请</div></div></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="wb-card">
          <template #header><span class="card-title">最新预警</span></template>
          <div v-for="(w, i) in alerts" :key="i" class="row-item">
            <el-tag size="small" :type="w.level === 'L1' ? 'danger' : w.level === 'L2' ? 'warning' : 'info'" effect="light">{{ w.level }}</el-tag>
            <span class="row-text">{{ w.text }}</span>
            <span class="row-time">{{ w.time }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="wb-card">
          <template #header><span class="card-title">我的专案</span></template>
          <div v-for="(p, i) in projects" :key="i" class="row-item" @click="$router.push('/workbench/intelligence')">
            <span class="row-text strong">{{ p.name }}</span>
            <el-tag size="small" effect="plain">{{ p.status }}</el-tag>
            <span class="row-time">{{ p.update }}</span>
          </div>
          <el-empty v-if="!projects.length" description="暂无专案" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never" class="wb-card">
          <template #header><span class="card-title">最近检索</span></template>
          <div v-for="(r, i) in recent" :key="i" class="row-item">
            <span class="row-text">{{ r }}</span>
            <span class="row-time">今天</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="wb-card">
          <template #header><span class="card-title">数据申请状态</span></template>
          <div v-for="(a, i) in applies" :key="i" class="row-item">
            <span class="row-text">{{ a.name }}</span>
            <el-tag size="small" :type="applyStatusType(a.status)" effect="light">{{ a.status }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Search, MagicStick } from '@element-plus/icons-vue'
import { userApi } from '@/api/modules/user'

const authStore = useAuthStore()
const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 12 ? '上午好' : h < 18 ? '下午好' : '晚上好'
})
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'long' })

const alerts = ref([])
const projects = ref([])
const recent = ref([])
const applies = ref([])

function applyStatusType(s) { return s === '已通过' ? 'success' : 'warning' }

onMounted(async () => {
  try {
    const wb = await userApi.workbench()
    if (wb) {
      policeStats.value = { alerts: wb.stats?.todo ?? 8, todos: wb.stats?.todo ?? 8, leads: wb.stats?.data ?? 12, platform: '正常' }
      alerts.value = wb.alerts || []
      projects.value = wb.projects || []
      recent.value = wb.recentSearches || []
      applies.value = wb.applyStatus || []
    }
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.user-workbench { max-width: 1200px; }
.wb-hero { background: linear-gradient(135deg, #1a6ff5, #0d47a1); border-radius: 12px; padding: 28px 32px; color: #fff; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; }
.wb-greeting { font-size: 22px; font-weight: 700; }
.wb-sub { font-size: 13px; opacity: .85; margin-top: 6px; }
.wb-stats { margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 8px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,.06); border-left: 4px solid #1a6ff5; }
.stat-card.danger { border-left-color: #ef4444; } .stat-card.success { border-left-color: #10b981; } .stat-card.warning { border-left-color: #f59e0b; }
.stat-card .v { font-size: 24px; font-weight: 700; color: #1f2937; }
.stat-card .l { font-size: 12px; color: #8492a6; margin-top: 4px; }
.wb-card { border-radius: 8px; }
.card-title { font-weight: 600; color: #1f2937; }
.row-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f3f4f6; font-size: 13px; cursor: pointer; }
.row-text { flex: 1; color: #374151; }
.row-text.strong { font-weight: 500; color: #1f2937; }
.row-time { font-size: 12px; color: #9ca3af; }
</style>
