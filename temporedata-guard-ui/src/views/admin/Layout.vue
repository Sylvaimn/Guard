<template>
  <div class="layout">
    <!-- White Sidebar -->
    <aside
      class="sidebar"
      :class="{ expanded: sidebarExpanded, pinned: sidebarPinned }"
      @mouseenter="onSidebarEnter"
      @mouseleave="onSidebarLeave"
    >
      <!-- Logo -->
      <div class="sidebar-logo-area">
        <router-link :to="menuPrefix + '/dashboard'" class="sidebar-logo-link">
          <svg class="sidebar-logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          <span class="sidebar-logo-text">金盾智云大数据</span>
        </router-link>
        <el-button class="sidebar-pin-btn" text size="small" @click="sidebarPinned = !sidebarPinned">
          <el-icon><component :is="sidebarPinned ? Lock : Unlock" /></el-icon>
        </el-button>
      </div>

      <!-- Menu Search -->
      <div class="sidebar-search" v-show="sidebarExpanded">
        <el-input
          v-model="menuSearch"
          placeholder="搜索菜单..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="menu-search-input"
        />
      </div>

      <!-- Menu Groups -->
      <nav class="sidebar-nav">
        <!-- Dashboard -->
        <router-link :to="menuPrefix + '/dashboard'" class="nav-item" :class="{ active: activeMenu === '/dashboard' }">
          <span class="nav-icon"><DataBoard /></span>
          <span class="nav-title">工作台</span>
        </router-link>

        <template v-for="group in filteredMenu" :key="group.group">
          <div class="nav-group-title" v-show="sidebarExpanded">{{ group.group }}</div>
          <router-link
            v-for="item in group.children"
            :key="item.path"
            :to="menuPrefix + item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
          >
            <span class="nav-icon"><component :is="item.icon" /></span>
            <span class="nav-title">{{ item.title }}</span>
          </router-link>
        </template>

        <!-- Agent -->
        <router-link :to="menuPrefix + '/agent'" class="nav-item" :class="{ active: activeMenu === '/agent' }">
          <span class="nav-icon"><ChatDotRound /></span>
          <span class="nav-title">Agent助手</span>
        </router-link>
      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer" v-show="sidebarExpanded">
        <span class="sidebar-version">v1.0.0</span>
      </div>
    </aside>

    <!-- Main Area -->
    <div class="main-area">
      <!-- Header -->
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: menuPrefix + '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <!-- Global Search -->
          <div class="global-search" @click="showGlobalSearch = true">
            <el-icon><Search /></el-icon>
            <span class="search-hint">搜索菜单、页面...</span>
            <kbd>Ctrl+K</kbd>
          </div>
          <!-- User -->
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" style="background: #1a6ff5">{{ userInitial }}</el-avatar>
              <span class="user-name">{{ authStore.user?.username || '管理员' }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span style="font-size:12px;color:#9ca3af">{{ authStore.user?.username || 'admin' }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Tab Bar -->
      <div class="tab-bar" v-if="tabPages.length > 0">
        <div
          v-for="tab in tabPages"
          :key="tab.path"
          class="tab-item"
          :class="{ active: tab.path === activeMenu }"
          @click="router.push(tab.path)"
        >
          <span class="tab-title">{{ tab.title }}</span>
          <el-icon class="tab-close" @click.stop="closeTab(tab.path)"><Close /></el-icon>
        </div>
      </div>

      <!-- Page Content -->
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="cachedPages">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>

    <!-- Global Search Dialog -->
    <el-dialog
      v-model="showGlobalSearch"
      title="全局搜索"
      width="560px"
      :show-close="true"
      :close-on-click-modal="true"
      destroy-on-close
      class="global-search-dialog"
    >
      <el-input
        v-model="globalSearchText"
        placeholder="输入关键词搜索菜单或页面..."
        :prefix-icon="Search"
        size="large"
        clearable
        autofocus
        @keydown.enter="navigateToFirstResult"
      />
      <div class="search-results" v-if="globalSearchText">
        <div class="search-result-group" v-for="group in globalSearchResults" :key="group.group">
          <div class="search-result-group-title">{{ group.group }}</div>
          <div
            v-for="item in group.children"
            :key="item.path"
            class="search-result-item"
            :class="{ active: globalActiveIndex === item._index }"
            @click="navigateTo(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
            <span class="search-result-path">{{ item.path }}</span>
          </div>
        </div>
        <el-empty v-if="globalSearchFlat.length === 0" description="未找到匹配的菜单" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { menuConfig, flatMenus } from '@/config/menu.config'

// Icons
import {
  DataBoard, Search, ArrowDown, SwitchButton, Close, ChatDotRound,
  Lock, Unlock
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Sidebar state
const sidebarExpanded = ref(false)
const sidebarPinned = ref(false)
const menuSearch = ref('')
let expandTimer = null

function onSidebarEnter() {
  clearTimeout(expandTimer)
  sidebarExpanded.value = true
}
function onSidebarLeave() {
  if (sidebarPinned.value) return
  expandTimer = setTimeout(() => {
    sidebarExpanded.value = false
  }, 200)
}

// Admin workspace prefix (双工作台: /admin/* 使用管理员菜单)
const menuPrefix = computed(() => route.path.startsWith('/admin') ? '/admin' : '')

// Active menu (去掉 /admin 前缀匹配菜单)
const activeMenu = computed(() => route.path.startsWith('/admin')
  ? route.path.replace('/admin', '')
  : route.path)
const currentTitle = computed(() => route.meta?.title || '')

// Filtered menu
const filteredMenu = computed(() => {
  if (!menuSearch.value) return menuConfig
  const kw = menuSearch.value.toLowerCase()
  return menuConfig
    .map(g => ({
      ...g,
      children: g.children.filter(c => c.title.toLowerCase().includes(kw))
    }))
    .filter(g => g.children.length > 0)
})

// User
const userInitial = computed(() => {
  const name = authStore.user?.username || 'A'
  return name.charAt(0).toUpperCase()
})

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// Tab bar
const tabPages = ref([])
const cachedPages = ref([])

watch(() => route.path, (path) => {
  if (path === '/dashboard') return // don't tab dashboard
  const title = route.meta?.title || path
  const exists = tabPages.value.find(t => t.path === path)
  if (!exists) {
    tabPages.value.push({ path, title })
    if (!cachedPages.value.includes(title)) {
      cachedPages.value.push(title)
    }
  }
}, { immediate: true })

function closeTab(path) {
  const idx = tabPages.value.findIndex(t => t.path === path)
  if (idx === -1) return
  tabPages.value.splice(idx, 1)
  if (path === activeMenu.value) {
    const next = tabPages.value[idx] || tabPages.value[idx - 1]
    if (next) router.push(next.path)
    else router.push('/dashboard')
  }
}

// Global search
const showGlobalSearch = ref(false)
const globalSearchText = ref('')
const globalActiveIndex = ref(0)

const globalSearchFlat = computed(() => {
  if (!globalSearchText.value) return []
  const kw = globalSearchText.value.toLowerCase()
  let idx = 0
  return menuConfig
    .map(g => ({
      ...g,
      children: g.children
        .filter(c => c.title.toLowerCase().includes(kw))
        .map(c => ({ ...c, _index: idx++ }))
    }))
    .filter(g => g.children.length > 0)
})

const globalSearchResults = computed(() => globalSearchFlat.value)

function navigateTo(path) {
  showGlobalSearch.value = false
  globalSearchText.value = ''
  router.push(path)
}

function navigateToFirstResult() {
  const first = globalSearchFlat.value[0]?.children[0]
  if (first) navigateTo(first.path)
}

// Keyboard shortcut
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    showGlobalSearch.value = true
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ========== Layout ========== */
.layout {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ========== Sidebar ========== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  width: 80px;
  background: #fff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s;
  overflow: hidden;
  box-shadow: 0 0 0 0 rgba(0,0,0,0);
}
.sidebar.expanded {
  width: 220px;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.06);
}
.sidebar:hover {
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.06);
}

/* Logo */
.sidebar-logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sidebar-logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #1f2937;
  overflow: hidden;
}
.sidebar-logo-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  color: #1a6ff5;
}
.sidebar-logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.5px;
}
.sidebar-pin-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.sidebar.expanded .sidebar-pin-btn,
.sidebar:hover .sidebar-pin-btn {
  opacity: 0.5;
}
.sidebar-pin-btn:hover {
  opacity: 1 !important;
}

/* Search */
.sidebar-search {
  padding: 8px 12px;
  flex-shrink: 0;
}
.menu-search-input :deep(.el-input__wrapper) {
  background: #f5f6f8;
  border-radius: 6px;
  box-shadow: none;
}

/* Nav */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 8px;
}
.sidebar-nav::-webkit-scrollbar {
  width: 3px;
}
.sidebar-nav::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 3px;
}

.nav-group-title {
  font-size: 11px;
  color: #9ca3af;
  padding: 16px 12px 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 12px;
  margin: 2px 0;
  border-radius: 8px;
  text-decoration: none;
  color: #4b5563;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
  gap: 12px;
  overflow: hidden;
  white-space: nowrap;
}
.nav-item:hover {
  background: #f3f4f6;
  color: #1f2937;
}
.nav-item.active {
  background: #e8f1fe;
  color: #1a6ff5;
  font-weight: 500;
}
.nav-item.active .nav-icon {
  color: #1a6ff5;
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
  color: #6b7280;
  transition: color 0.15s;
}
.nav-item:hover .nav-icon {
  color: #4b5563;
}
.nav-item.active .nav-icon {
  color: #1a6ff5;
}

.nav-title {
  opacity: 0;
  transition: opacity 0.15s;
}
.sidebar.expanded .nav-title {
  opacity: 1;
}

/* Footer */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sidebar-version {
  font-size: 11px;
  color: #c0c4cc;
}

/* ========== Main Area ========== */
.main-area {
  flex: 1;
  margin-left: 80px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== Topbar ========== */
.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Global Search */
.global-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #f5f6f8;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #9ca3af;
  transition: all 0.2s;
  min-width: 220px;
  border: 1px solid transparent;
}
.global-search:hover {
  background: #eceff3;
  border-color: #d0d5dd;
  color: #6b7280;
}
.global-search kbd {
  margin-left: auto;
  padding: 1px 6px;
  font-size: 11px;
  background: #e5e7eb;
  border-radius: 4px;
  color: #6b7280;
  font-family: inherit;
}
.search-hint {
  flex: 1;
}

/* User */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}
.user-info:hover {
  background: #f5f6f8;
}
.user-name {
  color: #1f2937;
  font-weight: 500;
}
.user-arrow {
  font-size: 12px;
  color: #9ca3af;
}

/* ========== Tab Bar ========== */
.tab-bar {
  display: flex;
  align-items: center;
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  padding: 0 12px;
  gap: 4px;
  overflow-x: auto;
  flex-shrink: 0;
  position: sticky;
  top: 56px;
  z-index: 49;
}
.tab-bar::-webkit-scrollbar {
  height: 2px;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  background: transparent;
  border: 1px solid transparent;
}
.tab-item:hover {
  background: #f3f4f6;
  color: #1f2937;
}
.tab-item.active {
  background: #e8f1fe;
  color: #1a6ff5;
  border-color: #d0e2fd;
}
.tab-close {
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.15s;
  border-radius: 3px;
  padding: 1px;
}
.tab-item:hover .tab-close {
  opacity: 0.5;
}
.tab-close:hover {
  opacity: 1 !important;
  background: #d0d5dd;
}

/* ========== Content ========== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f0f2f5;
}

/* ========== Global Search Dialog ========== */
.global-search-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}
.search-results {
  margin-top: 16px;
  max-height: 420px;
  overflow-y: auto;
}
.search-result-group {
  margin-bottom: 8px;
}
.search-result-group-title {
  font-size: 11px;
  color: #9ca3af;
  padding: 8px 12px 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #4b5563;
  transition: background 0.15s;
}
.search-result-item:hover,
.search-result-item.active {
  background: #f3f4f6;
  color: #1a6ff5;
}
.search-result-path {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
}
</style>