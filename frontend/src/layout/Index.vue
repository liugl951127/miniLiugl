<template>
  <el-container class="layout-container">
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="layout-logo" :class="{ collapsed }">
        <span class="logo-text gradient-text">{{ collapsed ? 'M' : 'MiniMax' }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        background-color="#0b1220"
        text-color="#aab4cf"
        active-text-color="#fff"
        router
        class="layout-menu"
      >
        <el-menu-item v-for="r in menuRoutes" :key="r.path" :index="r.path">
          <el-icon><component :is="r.icon" /></el-icon>
          <template #title>{{ r.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <span class="header-title">{{ activeTitle }}</span>
          <span v-if="healthSummary" :class="['health-pill', healthSummary.allUp ? 'up' : 'partial']">
            <span class="dot" :class="healthSummary.allUp ? 'dot-up' : 'dot-amber'"></span>
            {{ healthSummary.upCount }}/{{ healthSummary.total }} 服务 UP
          </span>
        </div>
        <div class="header-right">
          <el-tooltip content="刷新">
            <el-button text @click="reload"><el-icon><Refresh /></el-icon></el-button>
          </el-tooltip>
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="28" :src="userStore.profile?.avatar">
                {{ userStore.profile?.nickname?.[0] || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.profile?.nickname || '未登录' }}</span>
              <el-tag v-if="userStore.isSuperAdmin" type="danger" size="small" effect="dark" style="margin-left:6px">
                👑 SUPER
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isSuperAdmin" command="super" divided>
                  👑 超级管理控制台
                </el-dropdown-item>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { systemApi } from '@/api/system'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const platformInfo = ref({})

const menuRoutes = computed(() => {
  const base = [
    { path: '/chat', title: '智能对话', icon: 'ChatDotRound' },
    { path: '/knowledge', title: '知识库', icon: 'Files' },
    { path: '/memory', title: '记忆中心', icon: 'Memory' },
    { path: '/agent', title: 'Agent 自主任务', icon: 'MagicStick' },
    { path: '/kg', title: '知识图谱', icon: 'Share' },
    { path: '/collab', title: '实时协作', icon: 'UserFilled' },
    { path: '/plugins', title: '插件市场', icon: 'Grid' },
    { path: '/admin', title: '管理后台', icon: 'Setting' },
    { path: '/about', title: '关于', icon: 'InfoFilled' }
  ]
  // 超级管理员 (adminLiugl) 专属菜单
  if (userStore.isSuperAdmin) {
    base.push({ path: '/super', title: '👑 超级管理', icon: 'Key' })
  }
  return base
})

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/chat')) return '/chat'
  if (p.startsWith('/knowledge')) return '/knowledge'
  if (p.startsWith('/memory')) return '/memory'
  if (p.startsWith('/admin')) return '/admin'
  if (p.startsWith('/super')) return '/super'
  if (p.startsWith('/agent')) return '/agent'
  if (p.startsWith('/kg')) return '/kg'
  if (p.startsWith('/collab')) return '/collab'
  if (p.startsWith('/plugins')) return '/plugins'
  return p
})

const activeTitle = computed(() => {
  const r = menuRoutes.find((m) => m.path === activeMenu.value)
  return r?.title || 'MiniMax'
})

async function reload() {
  try {
    const res = await systemApi.health()
    ElMessage.success(`服务正常 · ${res.data.app} v${res.data.version} · ${res.data.day}`)
  } catch (e) { /* ignore */ }
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'profile') {
    ElMessage.info('个人中心 - Day 3 上线')
  } else if (cmd === 'super') {
    router.push('/super')
  }
}

onMounted(async () => {
  if (!userStore.profile && userStore.isLogin) {
    try { await userStore.fetchProfile() } catch (e) { /* ignore */ }
  }
  try {
    const res = await systemApi.intro()
    platformInfo.value = res.data
  } catch (e) { /* ignore */ }
  // 后台拉取健康状态
  refreshHealth()
  setInterval(refreshHealth, 30000)
})

const healthSummary = ref(null)
async function refreshHealth() {
  try {
    const r = await fetch('/api/v1/admin/health').then(r => r.json()).catch(() => null)
    if (r && r.data) {
      const all = r.data
      const upCount = Object.values(all).filter(v => v && v.status === 'UP').length
      healthSummary.value = { upCount, total: 6, allUp: upCount === 6 }
    }
  } catch (e) { /* ignore */ }
}
</script>

<style lang="scss" scoped>
.layout-container { height: 100vh; }
.layout-aside {
  background: #0b1220;
  transition: width 0.2s;
  overflow: hidden;
}
.layout-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  &.collapsed { font-size: 24px; }
}
.layout-menu {
  border-right: none;
  height: calc(100vh - 56px);
  :deep(.el-menu-item) {
    &.is-active {
      background: linear-gradient(90deg, rgba(91,141,239,0.25), transparent);
      border-left: 3px solid #5b8def;
    }
    &:hover { background-color: rgba(255,255,255,0.04) !important; }
  }
}
.layout-header {
  background: #fff;
  border-bottom: 1px solid var(--minimax-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 16px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 8px; border-radius: 4px;
  &:hover { background: var(--minimax-bg); }
}
.user-name { font-size: 14px; color: var(--minimax-text); }
.layout-main {
  background: var(--minimax-bg);
  padding: 16px;
  overflow: auto;
}
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.2s; }
.fade-slide-enter-from { opacity: 0; transform: translateX(8px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-8px); }

.health-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid;
  margin-left: 8px;
}
.health-pill.up { background: #d1fae5; color: #065f46; border-color: #6ee7b7; }
.health-pill.partial { background: #fef3c7; color: #92400e; border-color: #fcd34d; }
.health-pill .dot {
  width: 8px; height: 8px;
  border-radius: 50%;
}
.dot-up { background: #10b981; }
.dot-amber { background: #f59e0b; }
</style>
