<template>
  <transition name="fade">
    <div v-if="isOffline || isInstallable" class="pwa-bar" :class="barClass">
      <span class="icon">{{ icon }}</span>
      <span class="msg">{{ message }}</span>
      <el-button v-if="isInstallable" type="primary" size="small" @click="install">
        📥 安装
      </el-button>
      <el-button v-if="isOffline" size="small" @click="$router.push('/')">
        🏠 回首页
      </el-button>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { usePwa } from '@/composables/usePwa'

const { isOffline, isInstallable, install, swVersion } = usePwa()

const barClass = computed(() => isOffline.value ? 'offline' : 'installable')
const icon = computed(() => isOffline.value ? '📡' : '📲')
const message = computed(() => {
  if (isOffline.value) return '当前离线 · 可访问已缓存的页面 (SW ' + swVersion.value + ')'
  if (isInstallable.value) return 'MiniMax 支持安装到桌面, 离线也能用'
  return ''
})
</script>

<style scoped>
.pwa-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  gap: 12px; padding: 8px 16px;
  font-size: 13px; font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.pwa-bar.offline {
  background: linear-gradient(90deg, #f59e0b, #f97316);
  color: #fff;
}
.pwa-bar.installable {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  color: #fff;
}
.icon { font-size: 16px; }
.msg { flex: 0 1 auto; }
.fade-enter-active, .fade-leave-active { transition: all 0.3s ease; }
.fade-enter-from { opacity: 0; transform: translateY(-100%); }
.fade-leave-to { opacity: 0; transform: translateY(-100%); }
</style>
