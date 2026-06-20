<!--
  微信扫码演示页 (V5)
  - 已登录用户可扫码绑定当前账号
  - 未登录用户可扫码登录 (跳 Login)
-->
<template>
  <div class="wechat-scan-page">
    <div class="container">
      <h1>📱 微信扫码</h1>
      <p class="hint">用微信扫一扫下方二维码, {{ userStore.isLogin ? '自动绑定当前账号' : '快速登录' }}</p>
      <WechatScanLogin @login-success="onLogin" @bind-success="onBind" />
    </div>
  </div>
</template>

<script setup>
import WechatScanLogin from '@/components/WechatScanLogin.vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

function onLogin(payload) {
  userStore.setToken(payload.accessToken)
  userStore.setProfile(payload.user || {})
  ElMessage.success('登录成功')
  router.push('/chat')
}

function onBind(payload) {
  ElMessage.success('绑定成功')
  router.push('/profile/wechat')
}
</script>

<style scoped>
.wechat-scan-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.container {
  background: white; padding: 40px 60px; border-radius: 16px;
  text-align: center; max-width: 480px; width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}
h1 { margin: 0 0 12px; font-size: 28px; }
.hint { color: #64748b; margin-bottom: 24px; }
</style>