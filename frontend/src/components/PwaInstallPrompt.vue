<!--
  PWA 安装提示 (V4.2)
  - 监听 beforeinstallprompt 事件
  - 提供 "安装到桌面" 按钮
  - iOS 提供 "添加到主屏幕" 说明
-->
<template>
  <transition name="fade">
    <div v-if="showPrompt" class="install-prompt">
      <div class="ip-content">
        <div class="ip-icon">📱</div>
        <div class="ip-info">
          <div class="ip-title">{{ t('pwa.installTitle') }}</div>
          <div class="ip-sub">{{ t('pwa.installSub') }}</div>
        </div>
        <div class="ip-actions">
          <el-button type="primary" size="small" @click="install">
            {{ t('pwa.install') }}
          </el-button>
          <el-button text size="small" @click="dismiss">
            {{ t('pwa.later') }}
          </el-button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { t } from '@/i18n'

const showPrompt = ref(false)
let deferredPrompt = null
const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
const isStandalone = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone

onMounted(() => {
  // 已安装不提示
  if (isStandalone) return

  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault()
    deferredPrompt = e
    showPrompt.value = true
  })

  // iOS 单独处理
  if (isIOS && !isStandalone) {
    setTimeout(() => {
      if (!localStorage.getItem('minimax_ios_prompt_dismissed')) {
        showPrompt.value = true
      }
    }, 3000)
  }
})

async function install() {
  if (!deferredPrompt) {
    if (isIOS) {
      alert(t('pwa.iosHint'))
    }
    return
  }
  deferredPrompt.prompt()
  const { outcome } = await deferredPrompt.userChoice
  if (outcome === 'accepted') {
    showPrompt.value = false
  }
  deferredPrompt = null
}

function dismiss() {
  showPrompt.value = false
  localStorage.setItem('minimax_ios_prompt_dismissed', '1')
  setTimeout(() => {
    localStorage.removeItem('minimax_ios_prompt_dismissed')
  }, 7 * 24 * 3600 * 1000) // 7 天后再提示
}
</script>

<style scoped>
.install-prompt {
  position: fixed;
  bottom: 20px;
  left: 20px;
  right: 20px;
  max-width: 480px;
  margin: 0 auto;
  background: linear-gradient(135deg, #1e293b, #0f172a);
  color: #fff;
  border-radius: 14px;
  padding: 14px 18px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  z-index: 9999;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.ip-content { display: flex; align-items: center; gap: 12px; }
.ip-icon { font-size: 32px; }
.ip-info { flex: 1; }
.ip-title { font-weight: 600; font-size: 15px; }
.ip-sub { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.ip-actions { display: flex; gap: 6px; }

.fade-enter-active, .fade-leave-active { transition: all 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(20px); }
</style>
