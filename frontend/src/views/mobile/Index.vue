<template>
  <div class="mobile-app">
    <!-- 顶部安全区 + 平台信息 -->
    <div class="mobile-header">
      <div class="header-left">
        <van-image
          round
          :src="userStore.profile?.avatar || 'https://img.yzcdn.cn/vant/cat.jpeg'"
          width="36"
          height="36"
        />
        <div class="header-info">
          <div class="user-name">
            {{ userStore.profile?.nickname || '未登录' }}
            <van-tag v-if="userStore.isSuperAdmin" type="danger" size="small" effect="dark">👑</van-tag>
          </div>
          <div class="tenant-info">{{ tenantLabel }}</div>
        </div>
      </div>
      <van-button text size="small" @click="logout" icon="setting-o" />
    </div>

    <!-- 平台品牌 Banner -->
    <div class="platform-banner">
      <div class="banner-content">
        <div class="banner-logo">🚀</div>
        <div class="banner-text">
          <h1 class="banner-title">MiniMax Platform</h1>
          <p class="banner-slogan">大模型 · 企业级 · 智能协作</p>
        </div>
      </div>
      <div class="banner-stats">
        <div class="stat-item"><span class="stat-num">12</span><span class="stat-label">微服务</span></div>
        <div class="stat-divider"></div>
        <div class="stat-item"><span class="stat-num">116+</span><span class="stat-label">API</span></div>
        <div class="stat-divider"></div>
        <div class="stat-item"><span class="stat-num">135</span><span class="stat-label">测试</span></div>
      </div>
    </div>

    <!-- 页面内容 -->
    <div class="mobile-content">
      <router-view v-slot="{ Component }">
        <transition name="slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <!-- 底部 Tab 栏 -->
    <van-tabbar v-model="activeTab" route safe-area-inset-bottom>
      <van-tabbar-item to="/mobile/chat" icon="comment-o">对话</van-tabbar-item>
      <van-tabbar-item to="/mobile/agent" icon="aiming">Agent</van-tabbar-item>
      <van-tabbar-item to="/mobile/kg" icon="cluster-o">图谱</van-tabbar-item>
      <van-tabbar-item to="/mobile/discover" icon="fire-o">发现</van-tabbar-item>
      <van-tabbar-item to="/mobile/market" icon="gem-o">市场</van-tabbar-item>
      <van-tabbar-item to="/mobile/me" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const activeTab = ref(0)

const tenantLabel = computed(() => {
  if (userStore.isSuperAdmin) return '👑 平台所有者'
  return '租户: default'
})

async function logout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认退出登录?' })
    await userStore.logout()
    showToast({ message: '已退出', position: 'bottom' })
    router.push('/login')
  } catch (e) {
    // cancel
  }
}
</script>

<style scoped>
.mobile-app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
}
.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #ebeef5;
  padding-top: calc(env(safe-area-inset-top) + 4px);
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-info { display: flex; flex-direction: column; }
.user-name { font-size: 14px; font-weight: 600; color: #303133; display: flex; align-items: center; gap: 4px; }
.tenant-info { font-size: 11px; color: #909399; }
.platform-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 16px 16px;
}
.banner-content { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.banner-logo { font-size: 40px; }
.banner-title { font-size: 20px; font-weight: 700; margin: 0 0 2px; }
.banner-slogan { font-size: 12px; opacity: 0.85; margin: 0; }
.banner-stats { display: flex; align-items: center; justify-content: space-around; background: rgba(255,255,255,0.15); border-radius: 8px; padding: 8px 0; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 16px; font-weight: 700; }
.stat-label { font-size: 10px; opacity: 0.8; }
.stat-divider { width: 1px; height: 24px; background: rgba(255,255,255,0.3); }
.mobile-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 60px;
  -webkit-overflow-scrolling: touch;
}
.slide-enter-active, .slide-leave-active {
  transition: transform 0.2s, opacity 0.2s;
}
.slide-enter-from { transform: translateX(100%); opacity: 0; }
.slide-leave-to { transform: translateX(-100%); opacity: 0; }

@media (min-width: 768px) {
  .mobile-app {
    border-left: 1px solid #ebeef5;
    border-right: 1px solid #ebeef5;
    box-shadow: 0 0 20px rgba(0,0,0,0.05);
  }
}
</style>
