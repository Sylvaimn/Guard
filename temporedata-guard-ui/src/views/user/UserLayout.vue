<template>
  <div class="user-layout">
    <aside class="user-sidebar">
      <router-link to="/workbench" class="user-logo">
        <svg class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
        </svg>
        <span class="logo-text">金盾智云</span>
      </router-link>

      <div class="sidebar-search">
        <el-input v-model="menuSearch" placeholder="搜索菜单..." :prefix-icon="Search" size="small" clearable />
      </div>

      <nav class="user-nav">
        <template v-for="group in filteredMenus" :key="group.group">
          <div class="nav-group-title">{{ group.group }}</div>
          <router-link
            v-for="m in group.children"
            :key="m.path"
            :to="m.path"
            class="user-nav-item"
            :class="{ active: activeMenu === m.path }"
          >
            <el-icon class="nav-ic"><component :is="m.icon" /></el-icon>
            <span class="nav-tx">{{ m.title }}</span>
          </router-link>
        </template>
      </nav>

      <div class="user-sidebar-footer">
        <router-link to="/workbench/agent" class="agent-entry">
          <el-icon><MagicStick /></el-icon>
          <span>金盾智云 Agent</span>
          <el-tag size="small" type="success" effect="light">AI</el-tag>
        </router-link>
      </div>
    </aside>

    <div class="user-main">
      <header class="user-topbar">
        <div class="topbar-title">{{ currentTitle }}</div>
        <div class="topbar-right">
          <div class="global-search" @click="showSearch = true">
            <el-icon><Search /></el-icon>
            <span class="search-hint">全局搜索 人员/案件/数据/API...</span>
            <kbd>Ctrl+K</kbd>
          </div>
          <el-badge :value="3" :max="99" class="bell">
            <el-icon :size="18" @click="$router.push('/workbench/mine/message')"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="30" style="background: #1a6ff5">{{ userInitial }}</el-avatar>
              <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/workbench/mine/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/workbench/mine/apply')">我的申请</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/workbench/data/mydata')">我的数据</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="user-content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>

    <!-- 全局搜索 -->
    <el-dialog v-model="showSearch" title="全局搜索" width="560px" destroy-on-close>
      <el-input
        v-model="searchText"
        placeholder="搜索 人员 / 车辆 / 案件 / 线索 / 数据 / API / 专案..."
        :prefix-icon="Search"
        size="large"
        clearable
        autofocus
        @keyup.enter="goGlobalSearch"
      />
      <div class="search-scope">
        <el-tag size="small" effect="plain">人员</el-tag>
        <el-tag size="small" effect="plain">车辆</el-tag>
        <el-tag size="small" effect="plain">案件</el-tag>
        <el-tag size="small" effect="plain">线索</el-tag>
        <el-tag size="small" effect="plain">数据</el-tag>
        <el-tag size="small" effect="plain">API</el-tag>
        <el-tag size="small" effect="plain">专案</el-tag>
        <el-button size="small" type="primary" link @click="$router.push('/workbench/agent')">让金盾智云 Agent 分析</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  HomeFilled, Search, Share, Bell, Coin, User, List, ArrowDown, MagicStick,
  Star, Warning, Files, Tickets, Document, DataAnalysis, Link, DataBoard, Key
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuSearch = ref('')
const showSearch = ref(false)
const searchText = ref('')

// v2.1 用户端 8 大入口导航
const menuGroups = [
  {
    group: '工作台',
    children: [
      { path: '/workbench', title: '工作台', icon: HomeFilled },
      { path: '/workbench/search', title: '全局搜索', icon: Search }
    ]
  },
  {
    group: '警务实战',
    children: [
      { path: '/workbench/intel/search', title: '情报检索', icon: Search },
      { path: '/workbench/intel/entity', title: '对象研判', icon: User },
      { path: '/workbench/intel/graph', title: '关系图谱', icon: Share },
      { path: '/workbench/intel/clue', title: '线索中心', icon: Warning },
      { path: '/workbench/intel/alert', title: '预警中心', icon: Bell }
    ]
  },
  {
    group: '专案中心',
    children: [
      { path: '/workbench/project', title: '我的专案', icon: Files },
      { path: '/workbench/project/detail', title: '专案详情', icon: DataBoard },
      { path: '/workbench/project/tasks', title: '专案任务', icon: List },
      { path: '/workbench/project/clues', title: '专案线索', icon: Warning },
      { path: '/workbench/project/objects', title: '专案对象', icon: User },
      { path: '/workbench/project/graph', title: '专案图谱', icon: Share },
      { path: '/workbench/project/reports', title: '专案报告', icon: Document },
      { path: '/workbench/project/activity', title: '专案动态', icon: DataAnalysis }
    ]
  },
  {
    group: '数据中心',
    children: [
      { path: '/workbench/data/catalog', title: '数据目录', icon: Coin },
      { path: '/workbench/data/detail', title: '数据详情', icon: DataBoard },
      { path: '/workbench/data/query', title: '数据查询', icon: Search },
      { path: '/workbench/data/history', title: '查询历史', icon: Tickets },
      { path: '/workbench/data/metric', title: '指标中心', icon: DataAnalysis },
      { path: '/workbench/data/mydata', title: '我的数据', icon: User }
    ]
  },
  {
    group: '数据服务',
    children: [
      { path: '/workbench/service/api', title: 'API 服务', icon: Link },
      { path: '/workbench/service/api-detail', title: 'API 详情', icon: DataBoard },
      { path: '/workbench/service/report', title: '报表服务', icon: Document },
      { path: '/workbench/service/form', title: '表单服务', icon: Tickets }
    ]
  },
  {
    group: '成果中心',
    children: [
      { path: '/workbench/result', title: '我的报告', icon: Document },
      { path: '/workbench/result/create', title: '报告创建', icon: Edit },
      { path: '/workbench/result/detail', title: '报告详情', icon: DataBoard },
      { path: '/workbench/result/tasks', title: '生成任务', icon: List }
    ]
  },
  {
    group: '我的',
    children: [
      { path: '/workbench/mine/apply', title: '我的申请', icon: Key },
      { path: '/workbench/mine/favorite', title: '我的收藏', icon: Star },
      { path: '/workbench/mine/ops', title: '我的操作', icon: List },
      { path: '/workbench/mine/message', title: '消息中心', icon: Bell },
      { path: '/workbench/mine/profile', title: '个人中心', icon: User }
    ]
  }
]

const filteredMenus = computed(() => {
  if (!menuSearch.value) return menuGroups
  const kw = menuSearch.value.toLowerCase()
  return menuGroups
    .map(g => ({ ...g, children: g.children.filter(c => c.title.toLowerCase().includes(kw)) }))
    .filter(g => g.children.length > 0)
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const userInitial = computed(() => (authStore.user?.username || 'U').charAt(0).toUpperCase())

function logout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function goGlobalSearch() {
  if (!searchText.value.trim()) return
  router.push({ path: '/workbench/intel/search', query: { keyword: searchText.value.trim() } })
  showSearch.value = false
  searchText.value = ''
}
</script>

<style scoped>
.user-layout { display: flex; min-height: 100vh; background: #f5f7fa; }
.user-sidebar { width: 210px; background: #fff; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; }
.user-logo { display: flex; align-items: center; gap: 8px; padding: 16px 20px; color: #1a6ff5; text-decoration: none; }
.logo-icon { width: 26px; height: 26px; }
.logo-text { font-size: 17px; font-weight: 700; color: #1f2937; }
.sidebar-search { padding: 0 12px 8px; }
.user-nav { flex: 1; padding: 4px 10px 12px; overflow-y: auto; }
.nav-group-title { font-size: 11px; color: #9ca3af; padding: 10px 12px 4px; }
.user-nav-item { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-radius: 6px; color: #4b5563; text-decoration: none; font-size: 13px; margin-bottom: 1px; }
.user-nav-item:hover { background: #f3f4f6; }
.user-nav-item.active { background: #eff6ff; color: #1a6ff5; font-weight: 600; }
.nav-ic { font-size: 16px; }
.user-sidebar-footer { padding: 12px; border-top: 1px solid #f3f4f6; }
.agent-entry { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 6px; background: #eff6ff; color: #1a6ff5; text-decoration: none; font-size: 13px; }
.user-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.user-topbar { height: 52px; background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.topbar-title { font-size: 15px; font-weight: 600; color: #1f2937; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.global-search { display: flex; align-items: center; gap: 6px; background: #f5f7fa; border: 1px solid #e5e7eb; border-radius: 6px; padding: 5px 12px; cursor: pointer; color: #6b7280; font-size: 12px; }
.global-search kbd { background: #fff; border: 1px solid #e5e7eb; border-radius: 3px; padding: 0 5px; font-size: 11px; }
.bell { cursor: pointer; color: #6b7280; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-name { font-size: 13px; color: #374151; }
.arrow { font-size: 12px; color: #9ca3af; }
.user-content { flex: 1; padding: 20px; overflow-y: auto; }
.search-scope { display: flex; align-items: center; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
</style>
