<!--
  @file views/admin/Index.vue (V3.5.96 重写: 侧边栏分组 + 折叠 + 面包屑 + 快捷链接)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  Admin 后台容器 (V5.24)
  - 提供侧边栏子导航 + 主内容 router-view
  - V3.5.96 新增: 侧边栏分组 (V5 阶段) + 折叠按钮 + 顶部面包屑
  - V3.5.96 增强: aside-footer 改成快捷链接卡片 (8 入口)
  - V3.5.96 i18n: admin.menu.* 完整 12 键
  - 子页: Dashboard / Metrics / Traces / Monitor / Provider / Leaderboard / ApiKeyStats
-->
<template>
  <div class="page-admin admin-container" :class="{ 'is-collapsed': collapsed }">
    <!-- 1. page-header: 面包屑 + 折叠按钮 + 用户信息 -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <el-watermark v-if="false" content="V3.6.1" :font="{ size: 8 }" class="page-watermark" />
  <header class="page-header">
      <div class="header-left">
        <el-button :icon="Fold" @click="toggleCollapsed" plain circle :title="t('admin.collapse')" />
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">{{ t('admin.home') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ t('admin.title') }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentPage">{{ currentPage }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <el-tag :type="healthTagType" size="small">
          <el-icon :size="12"><CircleCheck v-if="health === 'up'" /><CircleClose v-else /></el-icon>
          {{ t(`admin.health.${health}`) }}
        </el-tag>
        <el-badge :value="alertCount" :max="99" :hidden="!alertCount" type="danger">
          <el-button :icon="Bell" @click="goAlerts" plain>{{ t('admin.alerts') }}</el-button>
        </el-badge>
        <span class="header-user" v-if="userStore.profile">
          <el-avatar :size="28" :src="userStore.profile.avatar">{{ userStore.profile.username?.[0] || 'U' }}</el-avatar>
          <span class="user-name">{{ userStore.profile.nickname || userStore.profile.username }}</span>
        </span>
      </div>
    </header>

    <!-- 2. el-aside: 侧边栏分组 + 折叠状态 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="admin-aside">
      <div class="aside-header" @click="toggleCollapsed">
        <el-icon :size="22" color="#5b8def"><Setting /></el-icon>
        <span v-show="!collapsed">{{ t('admin.title') }}</span>
      </div>

      <!-- 2.1 核心管理 -->
      <el-menu :default-active="activeMenu" router class="admin-menu" :collapse="collapsed">
        <div class="menu-group-title" v-show="!collapsed">{{ t('admin.group.core') }}</div>
        <el-menu-item index="/admin">
          <el-icon><DataLine /></el-icon>
          <template #title>{{ t('admin.menu.dashboard') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/metrics">
          <el-icon><TrendCharts /></el-icon>
          <template #title>
            {{ t('admin.menu.metrics') }}
            <el-tag size="small" type="info">V5.10</el-tag>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/audit">
          <el-icon><Document /></el-icon>
          <template #title>{{ t('admin.menu.audit') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/alerts">
          <el-icon><Warning /></el-icon>
          <template #title>
            {{ t('admin.menu.alerts') }}
            <el-badge v-if="alertCount" :value="alertCount" :max="99" type="danger" />
          </template>
        </el-menu-item>

        <!-- 2.2 可观测性 -->
        <div class="menu-group-title" v-show="!collapsed">{{ t('admin.group.observability') }}</div>
        <el-menu-item index="/admin/traces">
          <el-icon><Connection /></el-icon>
          <template #title>
            {{ t('admin.menu.traces') }}
            <el-tag size="small" type="info">V5.14</el-tag>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/cluster">
          <el-icon><Share /></el-icon>
          <template #title>{{ t('admin.menu.cluster') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/monitor">
          <el-icon><Monitor /></el-icon>
          <template #title>{{ t('admin.menu.monitor') }}</template>
        </el-menu-item>

        <!-- 2.3 AI 模型 -->
        <div class="menu-group-title" v-show="!collapsed">{{ t('admin.group.ai') }}</div>
        <el-menu-item index="/admin/provider">
          <el-icon><Cpu /></el-icon>
          <template #title>
            {{ t('admin.menu.provider') }}
            <el-tag size="small" type="success">V5.24</el-tag>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/leaderboard">
          <el-icon><Trophy /></el-icon>
          <template #title>
            {{ t('admin.menu.leaderboard') }}
            <el-tag size="small" type="success">V5.24</el-tag>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/apikey-stats">
          <el-icon><Key /></el-icon>
          <template #title>
            {{ t('admin.menu.apikey') }}
            <el-tag size="small" type="success">Day 20</el-tag>
          </template>
        </el-menu-item>

        <!-- 2.4 系统 -->
        <div class="menu-group-title" v-show="!collapsed">{{ t('admin.group.system') }}</div>
        <el-menu-item index="/admin/framework">
          <el-icon><Files /></el-icon>
          <template #title>{{ t('admin.menu.framework') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/governance">
          <el-icon><Lock /></el-icon>
          <template #title>{{ t('admin.menu.governance') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/document">
          <el-icon><Notebook /></el-icon>
          <template #title>{{ t('admin.menu.document') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/push">
          <el-icon><Promotion /></el-icon>
          <template #title>{{ t('admin.menu.push') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/wechat">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>{{ t('admin.menu.wechat') }}</template>
        </el-menu-item>
        <el-menu-item index="/admin/wechat-unionid">
          <el-icon><Link /></el-icon>
          <template #title>{{ t('admin.menu.wechatUnionid') }}</template>
        </el-menu-item>
      </el-menu>

      <!-- 2.5 aside-footer: 快捷入口卡片 -->
      <div class="aside-footer" v-show="!collapsed">
        <div class="quick-card">
          <div class="quick-title">⚡ {{ t('admin.quick.title') }}</div>
          <div class="quick-grid">
            <router-link v-for="q in quickLinks" :key="q.to" :to="q.to" class="quick-link">
              <el-icon :size="14" :color="q.color"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </el-aside>

    <!-- 3. el-main: router-view + 页面切换动画 -->
    <el-main class="admin-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'
import {
  Fold, Setting, DataLine, TrendCharts, Connection, Monitor, Cpu, Trophy, Key,
  ChatDotRound, Bell, CircleCheck, CircleClose, Document, Warning, Share, Files,
  Lock, Notebook, Promotion, Link, ChatLineRound, User, MagicStick, DataAnalysis, ChatDotSquare
} from '@element-plus/icons-vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ───── 响应式状态 ─────
const collapsed = ref(false)  // 侧边栏折叠
const health = ref('up')       // 系统健康状态: up / down / unknown
const alertCount = ref(0)      // 告警数 (V3.5.96+ 演示默认 0, 真后端可接 /admin/alerts API)

const activeMenu = computed(() => route.path)

// 面包屑当前页
const PAGE_NAMES = {
  '/admin': 'admin.menu.dashboard',
  '/admin/metrics': 'admin.menu.metrics',
  '/admin/audit': 'admin.menu.audit',
  '/admin/alerts': 'admin.menu.alerts',
  '/admin/traces': 'admin.menu.traces',
  '/admin/cluster': 'admin.menu.cluster',
  '/admin/monitor': 'admin.menu.monitor',
  '/admin/provider': 'admin.menu.provider',
  '/admin/leaderboard': 'admin.menu.leaderboard',
  '/admin/apikey-stats': 'admin.menu.apikey',
  '/admin/framework': 'admin.menu.framework',
  '/admin/governance': 'admin.menu.governance',
  '/admin/document': 'admin.menu.document',
  '/admin/push': 'admin.menu.push',
  '/admin/wechat': 'admin.menu.wechat',
  '/admin/wechat-unionid': 'admin.menu.wechatUnionid',
}
const currentPage = computed(() => {
  const key = PAGE_NAMES[route.path]
  return key ? t(key) : ''
})

const healthTagType = computed(() => ({
  up: 'success',
  down: 'danger',
  unknown: 'info',
}[health.value]))

// 快捷链接 (V3.5.96 8 入口)
const quickLinks = computed(() => [
  { to: '/chat', label: t('admin.quick.chat'), icon: ChatDotRound, color: '#5b8def' },
  { to: '/kg', label: t('admin.quick.kg'), icon: DataAnalysis, color: '#10b981' },
  { to: '/agent', label: t('admin.quick.agent'), icon: MagicStick, color: '#a855f7' },
  { to: '/ai/chat', label: t('admin.quick.ai'), icon: ChatDotSquare, color: '#f59e0b' },
  { to: '/ai/marketplace', label: t('admin.quick.marketplace'), icon: ChatLineRound, color: '#ec4899' },
  { to: '/monitor', label: t('admin.quick.monitor'), icon: Monitor, color: '#06b6d4' },
  { to: '/user', label: t('admin.quick.user'), icon: User, color: '#6366f1' },
  { to: '/profile', label: t('admin.quick.profile'), icon: User, color: '#84cc16' },
])

// ───── 方法 ─────
function toggleCollapsed() {
  collapsed.value = !collapsed.value
  localStorage.setItem('minimax_admin_collapsed', collapsed.value ? '1' : '0')
}

function goAlerts() {
  router.push('/admin/alerts')
}

// ───── 生命周期 ─────
onMounted(async () => {
  // 恢复折叠状态
  collapsed.value = localStorage.getItem('minimax_admin_collapsed') === '1'

  // V3.5.96+ 演示模式: alertCount = 0, health = 'up'
  // 真后端:
  // try {
  //   const r = await adminApi.health()
  //   health.value = r.data.status
  //   alertCount.value = r.data.alertCount
  // } catch (e) {
  //   health.value = 'down'
  // }
})
</script>

<style lang="scss" scoped>
.admin-container {
  display: flex;
  min-height: calc(100vh - 60px);
  background: #f1f5f9;
  transition: all 0.3s;
}

.page-header {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 16px;
  background: #f1f5f9;
}

.user-name {
  font-size: 13px;
  color: #475569;
}

.admin-aside {
  position: fixed;
  top: 116px;
  bottom: 0;
  left: 0;
  background: #fff;
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
  z-index: 50;
  display: flex;
  flex-direction: column;
}

.is-collapsed .admin-aside {
  width: 64px !important;
}

.aside-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  border-bottom: 1px solid #e2e8f0;
}

.admin-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
}

.menu-group-title {
  padding: 12px 20px 6px;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.aside-footer {
  padding: 12px;
  border-top: 1px solid #e2e8f0;
}

.quick-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  padding: 12px;
}

.quick-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  font-size: 11px;
  color: #475569;
  text-decoration: none;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.2s;
}

.quick-link:hover {
  background: #fff;
  transform: translateX(2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.admin-main {
  margin-left: 220px;
  margin-top: 56px;
  padding: 24px;
  flex: 1;
  min-height: calc(100vh - 60px - 56px);
  transition: margin-left 0.3s;
}

.is-collapsed .admin-main {
  margin-left: 64px;
}

@media (max-width: 768px) {
  .admin-aside {
    width: 64px !important;
  }
  .admin-main {
    margin-left: 64px !important;
    padding: 12px;
  }
  .aside-header span,
  .menu-group-title,
  .aside-footer {
    display: none;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
