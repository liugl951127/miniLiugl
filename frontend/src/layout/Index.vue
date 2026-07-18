<!--
  @file layout/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <el-container class="layout-container">
    <!-- V3.5.8: Desktop 侧边栏 (固定) -->
    <el-aside
      v-if="!isMobile"
      :width="collapsed ? '64px' : '220px'"
      class="layout-aside"
    >
      <div class="layout-logo" :class="{ collapsed }">
        <span class="logo-text gradient-text">{{ collapsed ? 'L' : 'Liugl-AI' }}</span>
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

    <!-- V3.5.8: Mobile 侧边栏 (抽屉式, 点击遮罩关闭) -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerVisible"
      direction="ltr"
      :with-header="false"
      size="260px"
      class="mobile-drawer"
      :modal-class="'mobile-mask'"
    >
      <div class="layout-aside" style="width: 100%; height: 100%; display: block;">
        <div class="layout-logo">
          <span class="logo-text gradient-text">Liugl-AI</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          background-color="#0b1220"
          text-color="#aab4cf"
          active-text-color="#fff"
          router
          class="layout-menu"
          @select="drawerVisible = false"
        >
          <el-menu-item v-for="r in menuRoutes" :key="r.path" :index="r.path">
            <el-icon><component :is="r.icon" /></el-icon>
            <template #title>{{ r.title }}</template>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="toggleSidebar">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <span class="header-title">{{ activeTitle }}</span>
          <span v-if="healthSummary" :class="['health-pill', healthSummary.allUp ? 'up' : 'partial']">
            <span class="dot" :class="healthSummary.allUp ? 'dot-up' : 'dot-amber'"></span>
            {{ healthSummary.upCount }}/{{ healthSummary.total }} 服务 UP
          </span>
        </div>
        <div class="header-right">
          <el-tooltip content="通知中心">
            <el-badge :value="notifStore.unreadCount" :hidden="!notifStore.unreadCount" type="danger" :max="99">
              <el-button text @click="$router.push('/notification')">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>
          <el-tooltip content="刷新">
            <el-button text @click="reload"><el-icon><Refresh /></el-icon></el-button>
          </el-tooltip>
          <!-- V2.7.8: 语言切换 -->
          <LangSwitcher />
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
                <el-dropdown-item v-if="userStore.isSuperAdmin" command="tenant">
                  🏢 租户管理
                </el-dropdown-item>
                <el-dropdown-item command="apikey">
                  🔑 API Key
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
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useNotificationStore } from '@/store/notification'
import { systemApi } from '@/api/system'
import LangSwitcher from '@/components/LangSwitcher.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notifStore = useNotificationStore()
const collapsed = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)
const platformInfo = ref({})

// V3.5.8: 响应式检测 (兼容所有浏览器)
function checkResponsive() {
  if (typeof window === 'undefined') return
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) collapsed.value = true
}
onMounted(() => {
  checkResponsive()
  window.addEventListener('resize', checkResponsive, { passive: true })
})
onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', checkResponsive)
  }
})

// 切换侧边栏 (mobile 用抽屉)
function toggleSidebar() {
  if (isMobile.value) {
    drawerVisible.value = !drawerVisible.value
  } else {
    collapsed.value = !collapsed.value
  }
}

const menuRoutes = computed(() => {
  const base = [
    { path: '/chat', title: '智能对话', icon: 'ChatDotRound' },
    { path: '/knowledge', title: '知识库', icon: 'Files' },
    { path: '/memory', title: '记忆中心', icon: 'Memory' },
    { path: '/prompts', title: 'Prompt 模板', icon: 'DocumentCopy' },
    { path: '/agent', title: 'Agent 自主任务', icon: 'MagicStick' },
    // V3.5.5+ 新增 3 个后端模块菜单 (model/function/multimodal)
    { path: '/model', title: '模型管理', icon: 'Cpu' },
    { path: '/function', title: 'Function 工具', icon: 'Tools' },
    { path: '/multimodal', title: '多模态', icon: 'PictureFilled' },
    { path: '/kg', title: '知识图谱', icon: 'Share' },
    { path: '/collab', title: '实时协作', icon: 'UserFilled' },
    { path: '/plugins', title: '插件市场', icon: 'Grid' },
    // Day 23: 模型训练控制台
    { path: '/training', title: '模型训练', icon: 'Cpu' },
    // V1.8: V5.31 数据分析入口
    { path: '/analytics/datasource', title: '数据源', icon: 'Coin' },
    { path: '/analytics/nlsql', title: 'NL2SQL 实验室', icon: 'ChatLineRound' },
    { path: '/analytics/ingest', title: '文件导入', icon: 'UploadFilled' },
    { path: '/analytics/reports', title: '报告中心', icon: 'DataAnalysis' },
    // V1.8: V5.32 工作流入口
    { path: '/pipeline', title: '工作流', icon: 'Connection' },
    { path: '/pipeline/designer', title: '画布设计器', icon: 'EditPen' },
    { path: '/pipeline/runs', title: '运行监控', icon: 'Monitor' },
    { path: '/apikey', title: 'API Key', icon: 'Key' },
    { path: '/admin', title: '管理后台', icon: 'Setting' },
    { path: '/about', title: '关于', icon: 'InfoFilled' }
  ]
  // 超级管理员 (adminLiugl) 专属菜单
  if (userStore.isSuperAdmin) {
    base.push({ path: '/super', title: '👑 超级管理', icon: 'Key' })
    base.push({ path: '/tenant', title: '🏢 租户管理', icon: 'Office' })
    // V5.9 Day 20: API Key 用量统计
    base.push({ path: '/apikey-stats', title: '📊 Key 统计', icon: 'DataLine' })
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
  if (p.startsWith('/tenant')) return '/tenant'
  if (p.startsWith('/agent')) return '/agent'
  if (p.startsWith('/kg')) return '/kg'
  if (p.startsWith('/collab')) return '/collab'
  if (p.startsWith('/plugins')) return '/plugins'
  if (p.startsWith('/notification')) return '/notification'
  if (p.startsWith('/analytics/datasource')) return '/analytics/datasource'
  if (p.startsWith('/analytics/nlsql')) return '/analytics/nlsql'
  if (p.startsWith('/analytics/ingest')) return '/analytics/ingest'
  if (p.startsWith('/analytics/reports')) return '/analytics/reports'
  if (p.startsWith('/pipeline/designer')) return '/pipeline/designer'
  if (p.startsWith('/pipeline/runs')) return '/pipeline/runs'
  if (p.startsWith('/pipeline')) return '/pipeline'
  if (p.startsWith('/apikey')) return '/apikey'
  if (p.startsWith('/training')) return '/training'
  return p
})

const activeTitle = computed(() => {
  const r = menuRoutes.find((m) => m.path === activeMenu.value)
  return r?.title || 'Liugl-AI'
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
  } else if (cmd === 'tenant') {
    router.push('/tenant')
  } else if (cmd === 'apikey') {
    router.push('/apikey')
  }
}

onMounted(async () => {
  if (!userStore.profile && userStore.isLogin) {
    try { await userStore.fetchProfile() } catch (e) { /* ignore */ }
  }
  // V3.5.8: 首轮 fetchProfile 后在 mobile 下隐藏欢迎语
  if (isMobile.value) collapsed.value = true
  // 初始化通知中心（WS 连接 + 未读数）
  if (userStore.isLogin) {
    notifStore.init()
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
.layout-container { height: 100vh; height: 100dvh; height: -webkit-fill-available; }
.mobile-drawer { width: 260px !important; }
.mobile-mask { background: rgba(0, 0, 0, 0.5); }
.layout-aside {
  background: #0b1220;
  transition: width 0.2s, transform 0.3s;
  overflow: hidden;
}
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

/* V3.5.8 响应式布局: mobile 抽窗式, desktop 固定侧边栏 */
@media (max-width: 768px) {
  .layout-aside { display: none; }
  .layout-main { padding: 8px; }
  .layout-header { padding: 0 8px; height: 48px; }
  .header-title { font-size: 14px; }
  .user-name { display: none; } /* mobile 不显示名字 */
  .header-right { gap: 4px; }
}
</style>
