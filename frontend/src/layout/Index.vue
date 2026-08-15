<!--
  @file layout/Index.vue (V6.8 重构版)
  @description 布局容器：模块化二级菜单 + 动态路由
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
                <el-dropdown-item v-if="userStore.isSuperAdmin" command="super" divided>超级管理</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isSuperAdmin" command="tenant">租户管理</el-dropdown-item>
                <el-dropdown-item command="apikey">API Key</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  ChatDotRound, Files, MagicStick, Cpu, DataAnalysis, Grid, Setting,
  Document, Fold, Expand, Refresh
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)
const sidebarWidth = '220px'
const healthSummary = ref(null)

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

// ========== 菜单数据模型 ==========
// menuGroups: { label, icon, path?, children?: { label, path, icon? }[] }[]
const menuGroups = computed(() => {
  const groups = [
    {
      label: 'AI 对话', icon: 'ChatDotRound',
      children: [
        { label: '智能对话', path: '/chat', icon: 'ChatDotRound' },
        { label: '流式对话', path: '/chat/stream', icon: 'Connection' },
        { label: '协作空间', path: '/collab', icon: 'UserFilled' },
      ]
    },
    {
      label: '知识中心', icon: 'Files',
      children: [
        { label: '知识库管理', path: '/knowledge', icon: 'Files' },
        { label: '知识图谱', path: '/kg', icon: 'Share' },
        { label: '记忆中心', path: '/memory', icon: 'Memory' },
      ]
    },
    {
      label: 'Agent 智能体', icon: 'MagicStick',
      children: [
        { label: 'Agent 编排', path: '/agent', icon: 'MagicStick' },
        { label: 'Agent 流式', path: '/agent/stream', icon: 'VideoPlay' },
        { label: 'Agent 画布', path: '/agent/canvas', icon: 'Brush' },
        { label: '多智能体', path: '/agent/multi', icon: 'Connection' },
        { label: '训练可视化', path: '/agent/training', icon: 'TrendCharts' },
      ]
    },
    {
      label: '模型与服务', icon: 'Cpu',
      children: [
        { label: '模型管理', path: '/model', icon: 'Cpu' },
        { label: 'Function 工具', path: '/function', icon: 'Tools' },
        { label: '多模态', path: '/multimodal', icon: 'PictureFilled' },
        { label: '训练总览', path: '/training/dashboard', icon: 'DataAnalysis' },
        { label: '训练控制台', path: '/training', icon: 'Cpu' },
      ]
    },
    {
      label: '数据与工作流', icon: 'DataAnalysis',
      children: [
        { label: '数据分析', path: '/analytics', icon: 'DataAnalysis' },
        { label: 'NL2SQL', path: '/analytics/nlsql', icon: 'ChatLineRound' },
        { label: 'NL 规则助手', path: '/rule', icon: 'MagicStick' },
        { label: '工作流', path: '/pipeline', icon: 'Connection' },
        { label: '画布设计器', path: '/pipeline/designer', icon: 'EditPen' },
        { label: '运行监控', path: '/pipeline/runs', icon: 'Monitor' },
      ]
    },
    {
      label: '应用中心', icon: 'Grid',
      children: [
        { label: 'Prompt 模板', path: '/prompts', icon: 'DocumentCopy' },
        { label: '插件市场', path: '/plugins', icon: 'Grid' },
        { label: '通知中心', path: '/notification', icon: 'Bell' },
      ]
    },
    {
      label: '系统管理', icon: 'Setting',
      children: [
        { label: 'API Key', path: '/apikey', icon: 'Key' },
        { label: '管理后台', path: '/admin', icon: 'Setting' },
        ...(userStore.isSuperAdmin ? [
          { label: '超级管理', path: '/super', icon: 'Key' },
          { label: '租户管理', path: '/tenant', icon: 'Office' },
        ] : []),
      ]
    },
    { label: '关于', icon: 'InfoFilled', path: '/about' },
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

onMounted(async () => {
  if (!userStore.profile && userStore.isLogin) {
    try { await userStore.fetchProfile() } catch {}
  }
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

@media (max-width: 768px) {
  .layout-main { padding: 8px; }
  .header-right .user-name { display: none; }
}
</style>
