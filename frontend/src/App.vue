<!--
  V3.6.21+ 增强 App.vue
  - ErrorBoundary 默认显示 Skeleton 而非空白
  - 全局 loading state
  - 演示模式自动降级
-->
<template>
  <PwaStatusBar />
  <ErrorBoundary>
    <template #fallback>
      <!-- V3.6.21+ 错误时显示 Skeleton, 不再空白 -->
      <div class="app-fallback">
        <div class="app-fallback-content">
          <el-skeleton :rows="6" animated />
        </div>
      </div>
    </template>
    <router-view v-slot="{ Component, route }">
      <!-- V3.6.21+ route 切换时显示加载态 -->
      <transition name="app-fade" mode="out-in">
        <div v-if="isLoading" key="loading" class="app-loading">
          <el-skeleton :rows="3" animated />
        </div>
        <component v-else :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </ErrorBoundary>
</template>

<script setup>
import { ref, onMounted, provide } from 'vue'
import PwaStatusBar from '@/components/PwaStatusBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
// V2.8.9: 加载 usePwa 以触发 SW 注册
import { usePwa } from '@/composables/usePwa'
import { useDemoMode } from '@/composables/useDemoMode'
import { usePreferencesStore } from '@/store/preferences'
import { useUserStore } from '@/store/user'
usePwa()  // 触发 SW 注册 (swVersion 在 PwaStatusBar 组件用)

const isLoading = ref(false)
provide('appLoading', isLoading)

// 路由切换时显示 loading - 配合 router/index.js
onMounted(async () => {
  useDemoMode().initFromStorage()
  // V6.8.9+: 初始化主题偏好
  const prefsStore = usePreferencesStore()
  prefsStore.init()
  if (useUserStore().isLogin) {
    await prefsStore.fetchFromBackend().catch(() => {})
  }
  console.log('[App] 已挂载 V3.6.21+ / 主题:', prefsStore.theme)
})
</script>

<style>
/* V6.8.9+ 深色模式变量覆盖 */
[data-theme="dark"] {
  --el-bg-color: #1a1a2e;
  --el-bg-color-overlay: #16213e;
  --el-text-color-primary: #e4e7ed;
  --el-border-color: #3a3f5c;
  --el-fill-color-light: #2a2d4a;
}
.el-theme-dark { background: #1a1a2e; }

.app-loading {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.app-fallback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.app-fallback-content {
  width: 100%;
  max-width: 800px;
  padding: 20px;
}
.app-fade-enter-active, .app-fade-leave-active {
  transition: opacity 0.15s ease;
}
.app-fade-enter-from, .app-fade-leave-to {
  opacity: 0;
}
</style>
