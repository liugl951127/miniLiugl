<!--
  @file components/PwaStatusBar.vue (PWA 状态栏)
  @version V3.7.18+ (V3.7.12 升级: 总显示版本 + 检查更新按钮)
  @description PWA 状态栏
  V3.7.18 修复: 之前只在 needRefresh/isOffline/isInstallable 时显示, 用户不知道 SW 当前版本
  现在总显示, 加"检查更新"按钮
-->
<template>
  <!-- V3.7.18+ 总显示, 让用户能看到当前 SW 版本 -->
  <div v-if="showBar" class="pwa-bar" :class="barClass">
    <span class="icon">{{ icon }}</span>
    <span class="msg">{{ message }}</span>
    <el-button v-if="isInstallable" type="primary" size="small" @click="install">
      📥 安装
    </el-button>
    <el-button v-if="needRefresh" type="success" size="small" @click="update">
      🔄 立即更新
    </el-button>
    <el-button v-if="!needRefresh && swRegistered" size="small" @click="checkForUpdate">
      🔍 检查更新
    </el-button>
    <el-button v-if="isOffline" size="small" @click="$router.push('/')">
      🏠 回首页
    </el-button>
    <el-button class="close-btn" size="small" @click="hideBar" :title="'隐藏 (1h 后再显示)'">
      ✕
    </el-button>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { usePwa } from '@/composables/usePwa'

const { 
  isOffline, isInstallable, needRefresh, swVersion, swRegistered,
  install, update, checkForUpdate 
} = usePwa()

// V3.7.18+ 控制条显示: 1.5s 后延迟显示, 避免闪烁
const showBar = ref(false)
let showBarTimer = null
const hideBar = () => {
  showBar.value = false
  // 1h 后重新显示
  localStorage.setItem('pwa-bar-hide-until', String(Date.now() + 3600 * 1000))
}

const checkShow = () => {
  const hideUntil = Number(localStorage.getItem('pwa-bar-hide-until') || 0)
  if (Date.now() < hideUntil) {
    return
  }
  // 总是显示 (V3.7.18+), 用户能看版本
  showBar.value = true
}

onMounted(() => {
  // 1.5s 后检查显示
  showBarTimer = setTimeout(checkShow, 1500)
})

onBeforeUnmount(() => {
  if (showBarTimer) {
    clearTimeout(showBarTimer)
    showBarTimer = null
  }
})

const barClass = computed(() => {
  if (isOffline.value) return 'offline'
  if (needRefresh.value) return 'update'
  if (isInstallable.value) return 'installable'
  return 'normal'
})
const icon = computed(() => {
  if (isOffline.value) return '📡'
  if (needRefresh.value) return '🔄'
  if (isInstallable.value) return '📲'
  return '✅'  // V3.7.18+ 正常状态
})
const message = computed(() => {
  if (isOffline.value) return '当前离线 · 可访问已缓存的页面 (SW ' + swVersion.value + ')'
  if (isInstallable.value) return 'Liugl-AI 支持安装到桌面, 离线也能用'
  if (needRefresh.value) return '新版本可用, 点击立即更新加载 (SW ' + swVersion.value + ')'
  return '当前版本 SW ' + swVersion.value + ' · V3.7.18+ 已激活'
})
</script>

<style scoped>
.pwa-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  gap: 12px; padding: 6px 16px;
  font-size: 13px; font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.pwa-bar.normal {
  background: rgba(99, 102, 241, 0.9);
  color: #fff;
}
.pwa-bar.offline {
  background: linear-gradient(90deg, #f59e0b, #f97316);
  color: #fff;
}
.pwa-bar.installable {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  color: #fff;
}
.pwa-bar.update {
  background: linear-gradient(90deg, #10b981, #06b6d4);
  color: #fff;
}
.icon { font-size: 16px; }
.msg { flex: 0 1 auto; }
.close-btn {
  opacity: 0.6;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  padding: 4px 8px;
}
.close-btn:hover { opacity: 1; }
</style>
