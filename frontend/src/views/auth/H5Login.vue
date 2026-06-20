<!--
  H5 跨平台登录页 (V5.2)
  - 微信扫码 / 公众号 OAuth / QQ / 支付宝 4 种登录方式
  - 移动端适配 (H5)
  - 自动识别平台 (navigator.userAgent)
  - 支持 unionid 跨平台打通
-->
<template>
  <div class="h5-login">
    <header class="header">
      <h1>🔑 登录</h1>
      <p class="subtitle">MiniMax 大模型平台</p>
    </header>

    <!-- 平台按钮 -->
    <section class="oauth-buttons">
      <button class="oauth-btn wechat" @click="loginWechat">
        <div class="icon">📱</div>
        <div class="label">微信扫码登录</div>
      </button>
      <button class="oauth-btn mp" @click="loginMp">
        <div class="icon">📢</div>
        <div class="label">微信公众号登录</div>
      </button>
      <button class="oauth-btn qq" @click="loginQq">
        <div class="icon">🐧</div>
        <div class="label">QQ 登录</div>
      </button>
      <button class="oauth-btn alipay" @click="loginAlipay">
        <div class="icon">💰</div>
        <div class="label">支付宝登录</div>
      </button>
    </section>

    <!-- 扫码登录面板 -->
    <transition name="slide">
      <section v-if="showScan" class="scan-panel">
        <WechatScanLogin @login-success="onLogin" />
        <el-button text @click="showScan = false">← 取消</el-button>
      </section>
    </transition>

    <!-- 账号密码登录 -->
    <el-divider>或</el-divider>
    <el-form :model="form" @submit.prevent="onPasswordLogin" class="pwd-form">
      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" placeholder="密码" type="password" prefix-icon="Lock" size="large" show-password />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" @click="onPasswordLogin" style="width: 100%">
        登录
      </el-button>
    </el-form>

    <p class="footer">
      登录即代表同意 <a href="#">用户协议</a> 和 <a href="#">隐私政策</a>
    </p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import WechatScanLogin from '@/components/WechatScanLogin.vue'
import http from '@/api/http'
import { useUserStore } from '@/store/user'
import { isWechatBrowser } from '@/utils/platform'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const showScan = ref(false)
const loading = ref(false)
const form = ref({ username: '', password: '' })

const isMobile = computed(() => /Mobile|iPhone|Android/i.test(navigator.userAgent))
const inWechat = computed(() => isWechatBrowser())

async function loginWechat() {
  showScan.value = true
}

function loginMp() {
  // 公众号 OAuth 跳转
  if (!inWechat.value) {
    ElMessage.warning('请在微信内打开此页面')
    return
  }
  triggerAuthorize('wechat', 'mp')
}

function loginQq() {
  triggerAuthorize('qq', 'web')
}

function loginAlipay() {
  triggerAuthorize('alipay', 'web')
}

async function triggerAuthorize(platform, appType) {
  try {
    const r = await http.get(`/auth/oauth/${platform}/authorize-url`, {
      params: {
        appType,
        redirectUri: window.location.origin + '/auth/oauth/' + platform + '/callback',
        state: Math.random().toString(36).substring(7),
        scope: 'user_info'
      }
    })
    const url = r.data?.authorizeUrl
    if (!url) {
      ElMessage.error('获取授权 URL 失败')
      return
    }
    // mock 模式: alert + 后端 mock 接口演示
    if (r.data?.mock) {
      ElMessage.info(`沙箱模式: 模拟 ${platform} 授权, 直接调 login`)
      mockLogin(platform, appType)
      return
    }
    window.location.href = url
  } catch (e) {
    ElMessage.error('跳转失败: ' + e.message)
  }
}

async function mockLogin(platform, appType) {
  try {
    loading.value = true
    const r = await http.post(`/auth/oauth/${platform}/login`, {
      code: 'mock_' + platform + '_code_' + Date.now(),
      appType
    })
    onLogin(r.data)
  } catch (e) {
    ElMessage.error('沙箱登录失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function onLogin(loginResp) {
  if (!loginResp || !loginResp.accessToken) {
    ElMessage.error('登录失败')
    return
  }
  userStore.setToken(loginResp.accessToken)
  userStore.setProfile(loginResp.user || {})
  ElMessage.success('登录成功')
  const redirect = route.query.redirect || '/chat'
  router.push(redirect)
}

async function onPasswordLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const r = await http.post('/auth/login', form.value)
    onLogin(r.data)
  } catch (e) {
    ElMessage.error('登录失败: ' + e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-login {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 24px;
  max-width: 480px;
  margin: 0 auto;
}
.header { text-align: center; color: white; margin-bottom: 32px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { opacity: 0.9; margin: 0; }

.oauth-buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.oauth-btn {
  background: white; border: none; border-radius: 12px; padding: 20px 16px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.oauth-btn:active { transform: scale(0.98); }
.oauth-btn .icon { font-size: 36px; }
.oauth-btn .label { font-size: 13px; font-weight: 500; }
.oauth-btn.wechat { background: #07c160; color: white; }
.oauth-btn.mp { background: #f0f9ff; color: #0369a1; border: 1px solid #bae6fd; }
.oauth-btn.qq { background: #1296db; color: white; }
.oauth-btn.alipay { background: #1677ff; color: white; }

.scan-panel {
  background: white; border-radius: 16px; padding: 24px; margin-bottom: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

:deep(.el-divider__text) { background: transparent; color: white; }

.pwd-form {
  background: white; border-radius: 12px; padding: 20px; margin-bottom: 16px;
}
.footer { text-align: center; color: rgba(255,255,255,0.7); font-size: 12px; }
.footer a { color: white; text-decoration: underline; }

.slide-enter-active, .slide-leave-active { transition: all 0.3s; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-20px); }
</style>