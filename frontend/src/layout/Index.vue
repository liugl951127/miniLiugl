<!--
  @file layout/Index.vue (V6.9 重构版)
  @description 布局容器：精简菜单 (7 分组) + 统一系统管理 + 动态路由
-->
<template>
  <el-container class="layout-container">

    <!-- 侧边栏 (Desktop) -->
    <el-aside
      v-if="!isMobile"
      :width="collapsed ? '64px' : sidebarWidth"
      class="layout-aside"
    >
      <!-- Logo -->
      <div class="layout-logo" :class="{ collapsed }">
        <span class="logo-text">{{ collapsed ? 'L' : 'Liugl-AI' }}</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="#0b1220"
        text-color="#aab4cf"
        active-text-color="#fff"
        router
        class="layout-menu"
      >
        <template v-for="group in menuGroups" :key="group.label">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="group.children && group.children.length" :index="group.label">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.label }}</span>
            </template>
            <el-menu-item
              v-for="item in group.children"
              :key="item.path"
              :index="item.path"
            >
              <el-icon><component :is="item.icon || 'Document'" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 一级菜单 -->
          <el-menu-item v-else :index="group.path">
            <el-icon><component :is="group.icon" /></el-icon>
            <template #title>{{ group.label }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 侧边栏 (Mobile) -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerVisible"
      direction="ltr"
      :with-header="false"
      size="260px"
    >
      <div class="layout-logo" style="width:100%">
        <span class="logo-text">Liugl-AI</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#0b1220"
        text-color="#aab4cf"
        active-text-color="#fff"
        router
        @select="drawerVisible = false"
      >
        <template v-for="group in menuGroups" :key="group.label">
          <el-sub-menu v-if="group.children && group.children.length" :index="group.label">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.label }}</span>
            </template>
            <el-menu-item
              v-for="item in group.children"
              :key="item.path"
              :index="item.path"
            >{{ item.label }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="group.path">
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-drawer>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="toggleSidebar">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <span class="header-title">{{ activeTitle }}</span>
          <span v-if="healthSummary" :class="['health-pill', healthSummary.allUp ? 'up' : 'partial']">
            <span class="dot" :class="healthSummary.allUp ? 'up' : 'amber'"></span>
            {{ healthSummary.upCount }}/{{ healthSummary.total }} 服务
          </span>
        </div>
        <div class="header-right">
          <!-- 租户隔离状态指示器 (SUPER_ADMIN) -->
          <div v-if="userStore.isSuperAdmin && currentTenant" class="tenant-indicator">
            <el-tooltip :content="'数据隔离: ' + (currentTenant.dataIsolation !== false ? '已开启' : '未开启')">
              <span class="tenant-name">
                <el-icon><OfficeBuilding /></el-icon>
                {{ currentTenant.name }}
                <el-icon class="isolation-icon" :class="currentTenant.dataIsolation !== false ? 'locked' : 'unlocked'">
                  <Lock v-if="currentTenant.dataIsolation !== false" />
                  <Unlock v-else />
                </el-icon>
              </span>
            </el-tooltip>
          </div>
          <el-tooltip :content="prefsStore.theme === 'dark' ? '切换亮色模式' : '切换深色模式'">
            <el-button text @click="prefsStore.toggleTheme()">
              <el-icon><Sunny v-if="prefsStore.theme === 'dark'" /><Moon v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="刷新"><el-button text @click="reload"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="28">{{ userStore.profile?.nickname?.[0] || 'U' }}</el-avatar>
              <span class="user-name">{{ userStore.profile?.nickname || '未登录' }}</span>
              <el-tag v-if="userStore.isSuperAdmin" type="danger" size="small" effect="dark">SUPER</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="about">关于平台</el-dropdown-item>
                <el-dropdown-item command="settings" divided>⚙️ 系统管理</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <!-- Day 52: 路由切换骨架屏 -->
        <div v-if="routeChanging" class="route-skeleton">
          <el-skeleton :rows="8" animated style="padding:16px" />
        </div>
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePreferencesStore } from '@/store/preferences'
import {
  ChatDotRound, Files, MagicStick, Cpu, DataAnalysis, Grid, Setting,
  Document, Fold, Expand, Refresh, Sunny, Moon,
  OfficeBuilding, Lock, Unlock
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const prefsStore = usePreferencesStore()

const collapsed = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)
const sidebarWidth = '220px'
const healthSummary = ref(null)
const currentTenant = ref(null)

// Day 52: 路由切换骨架屏
const routeChanging = ref(false)
let routeTimer = null
watch(() => route.path, () => {
  routeChanging.value = true
  if (routeTimer) clearTimeout(routeTimer)
  routeTimer = setTimeout(() => { routeChanging.value = false }, 400)
})

// ========== 响应式检测 ==========
function checkResponsive() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) collapsed.value = true
}
onMounted(() => {
  checkResponsive()
  window.addEventListener('resize', checkResponsive, { passive: true })
  refreshHealth()
})
onUnmounted(() => window.removeEventListener('resize', checkResponsive))

function toggleSidebar() {
  if (isMobile.value) drawerVisible.value = !drawerVisible.value
  else collapsed.value = !collapsed.value
}

// ========== 菜单数据模型 (V6.9 重构) ==========
// V6.9: 合并系统管理为 /settings，知识中心/Agent/数据中心/工作流 各自单页 tab 化
const menuGroups = computed(() => {
  const groups = [
    // ── 对话 (5 入口合并) ──
    {
      label: '对话', icon: 'ChatDotRound',
      children: [
        { label: 'AI 对话', path: '/chat', icon: 'ChatDotRound' },
        { label: '协作空间', path: '/collab', icon: 'UserFilled' },
      ]
    },
    // ── 知识 (3 入口) ──
    {
      label: '知识', icon: 'Files',
      children: [
        { label: '知识库', path: '/knowledge', icon: 'Files' },
        { label: '知识图谱', path: '/kg', icon: 'Share' },
      ]
    },
    // ── Agent (4 入口合并) ──
    {
      label: 'Agent', icon: 'MagicStick',
      children: [
        { label: '画布编排', path: '/agent', icon: 'MagicStick' },
        { label: '智能体群', path: '/agent-auto', icon: 'Grid' },
        { label: '模型管理', path: '/model', icon: 'Cpu' },
      ]
    },
    // ── 数据 (4 入口) ──
    {
      label: '数据', icon: 'DataAnalysis',
      children: [
        { label: '数据分析', path: '/analytics', icon: 'DataAnalysis' },
        { label: '规则助手', path: '/rule', icon: 'MagicStick' },
        { label: '工作流', path: '/pipeline', icon: 'Connection' },
      ]
    },
    // ── 应用 (4 入口合并) ──
    {
      label: '应用', icon: 'Grid',
      children: [
        { label: '多模态', path: '/multimodal', icon: 'PictureFilled' },
        { label: '训练', path: '/training', icon: 'TrendCharts' },
        { label: '提示词', path: '/prompts', icon: 'DocumentCopy' },
        { label: '插件', path: '/plugins', icon: 'Grid' },
      ]
    },
    // ── 系统 (3 入口) ──
    {
      label: '系统', icon: 'Setting',
      children: [
        { label: '运维', path: '/settings', icon: 'Setting' },
        { label: '监控', path: '/monitor', icon: 'Monitor' },
        { label: '通知', path: '/notification', icon: 'Bell' },
      ]
    },
  ]
  return groups
})

// ========== 当前激活菜单 ==========
const activeMenu = computed(() => {
  const p = route.path
  // 精确匹配优先
  const flat = menuGroups.value.flatMap(g =>
    g.children ? g.children.map(c => c.path) : [g.path].filter(Boolean)
  )
  if (flat.includes(p)) return p
  // 前缀匹配
  const prefix = flat.find(f => f !== '/' && p.startsWith(f))
  return prefix || p
})

const activeTitle = computed(() => {
  for (const g of menuGroups.value) {
    if (g.children) {
      const child = g.children.find(c => c.path === activeMenu.value)
      if (child) return child.label
    }
    if (g.path === activeMenu.value) return g.label
  }
  return 'Liugl-AI'
})

// ========== 操作 ==========
async function onCommand(cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    ElMessage.info('个人中心开发中')
  } else if (cmd === 'about') {
    router.push('/about')
  } else if (cmd === 'settings') {
    router.push('/settings')
  } else {
    router.push('/' + cmd)
  }
}

async function refreshHealth() {
  try {
    const r = await fetch('/api/v1/admin/health').then(r => r.json()).catch(() => null)
    if (r?.data) {
      const entries = Object.entries(r.data)
      const upCount = entries.filter(([, v]) => v?.status === 'UP').length
      healthSummary.value = { upCount, total: entries.length, allUp: upCount === entries.length }
    }
  } catch {}
}

async function reload() {
  try {
    const r = await fetch('/api/v1/system/health').then(r => r.json())
    ElMessage.success('系统正常')
  } catch {
    ElMessage.warning('部分服务异常，请检查')
  }
}

async function loadTenantInfo() {
  if (!userStore.isSuperAdmin) return
  try {
    const { myTenant } = await import('@/api/tenant')
    const r = await myTenant().catch(() => null)
    if (r?.data) currentTenant.value = r.data
  } catch {}
}

onMounted(async () => {
  if (!userStore.profile && userStore.isLogin) {
    try { await userStore.fetchProfile() } catch {}
  }
  loadTenantInfo()
})
</script>

<style lang="scss" scoped>
.layout-container { height: 100vh; display: flex; }
.layout-aside {
  background: #0b1220;
  transition: width 0.25s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.layout-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  font-size: 18px;
  font-weight: 700;
  color: #5b8def;
  flex-shrink: 0;
  &.collapsed { font-size: 22px; }
}
.layout-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    font-size: 13px;
    &.is-active {
      background: linear-gradient(90deg, rgba(91,141,239,0.22), transparent) !important;
      border-left: 3px solid #5b8def;
    }
    &:hover { background: rgba(255,255,255,0.05) !important; }
  }
  :deep(.el-sub-menu .el-menu-item) { min-width: 0; padding-left: 52px !important; }
}
.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 15px; font-weight: 600; color: #303133; }
.header-right { display: flex; align-items: center; gap: 8px; }
.user-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 8px; border-radius: 6px;
  &:hover { background: #f5f7fa; }
}
.user-name { font-size: 13px; color: #303133; }
.layout-main {
  background: #f5f7fa;
  padding: 16px;
  overflow: auto;
  flex: 1;
}
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.2s; }
.fade-slide-enter-from { opacity: 0; transform: translateX(8px); }
.fade-slide-leave-to { opacity: 0; }

.health-pill {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 2px 10px; border-radius: 12px;
  font-size: 11px; font-weight: 600;
}
.health-pill.up { background: #d1fae5; color: #065f46; }
.health-pill.partial { background: #fef3c7; color: #92400e; }
.dot { width: 7px; height: 7px; border-radius: 50%; }
.dot.up { background: #10b981; }
.dot.amber { background: #f59e0b; }

.tenant-indicator {
  margin-right: 12px;
  padding-right: 12px;
  border-right: 1px solid var(--el-border-color);
}
.tenant-name {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 600;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  padding: 3px 10px; border-radius: 14px;
  cursor: default;
}
.isolation-icon { font-size: 11px; }
.isolation-icon.locked { color: #10b981; }
.isolation-icon.unlocked { color: #f59e0b; }

@media (max-width: 768px) {
  .layout-main { padding: 8px; }
  .header-right .user-name { display: none; }
  .tenant-indicator { display: none; }
}

/* V6.8.9+ 深色模式布局覆盖 */
.el-theme-dark .layout-header {
  background: #16213e !important;
  border-bottom-color: #3a3f5c !important;
}
.el-theme-dark .header-title { color: #e4e7ed !important; }
.el-theme-dark .user-name { color: #e4e7ed !important; }
.el-theme-dark .user-info:hover { background: rgba(255,255,255,0.08) !important; }
.el-theme-dark .layout-main { background: #1a1a2e !important; }
.el-theme-dark .app-fallback { background: #1a1a2e !important; }

/* Day 52: 路由切换骨架屏 */
.route-skeleton {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: var(--el-bg-color, #fff);
  z-index: 10;
}
</style>
