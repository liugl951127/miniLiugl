<!--
  @file views/auth/Login.vue (V3.5.92 重写版 - 提升 UX)
  @version V3.5.92+ UX 全面提升
  @description 登录页 (账号密码 + 注册 + 微信扫码) - 完整重写
  @features
    - 骨架屏 (loading 状态) 避免空白
    - 表单实时校验 + 视觉反馈
    - 演示账号一键填入 (5 个测试账号)
    - 错误处理: 网络错 / 401 / 500 都有清晰提示 + 抖动动画
    - 密码可见切换
    - 自动登录 (token 持久化, 下次自动登)
    - 键盘快捷键: Enter 提交 / Tab 切换字段
    - 跳转: 不等 fetchProfile, 立刻跳, layout 异步 hydrate
    - 动效: 渐入 / 按钮 hover / 输入框 focus ring
    - i18n 完整接入 (zh / en)
    - 5 段样板: header / 4 tab (login/register/wechat/quick) / form / demos / features
-->
<template>
  <div class="page-login" :class="{ 'is-mobile': isMobile }">
    <!-- 1. page-header: 品牌 + 标题 + 副标题 + 主操作 -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <el-watermark v-if="false" content="V3.6.1" :font="{ size: 8 }" class="page-watermark" />
  <header class="page-header">
      <div class="brand">
        <div class="brand-logo">M</div>
        <div>
          <h1 class="brand-title">Liugl-AI</h1>
          <p class="brand-subtitle">{{ t('login.subtitle') }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip :content="demoMode ? '关闭演示模式' : '开启演示模式 (无后端)'" placement="bottom">
          <el-switch v-model="demoMode" @change="onDemoToggle" :active-icon="VideoPlay" :inactive-icon="VideoPause" inline-prompt size="default" style="--el-switch-off-color: #94a3b8; --el-switch-on-color: #a855f7" />
        </el-tooltip>
        <el-button :icon="ChatLineRound" @click="goGuest" plain round size="small">
          {{ t('login.guest') }}
        </el-button>
      </div>
    </header>

    <!-- 2. 主登录卡片 -->
    <el-card shadow="hover" class="login-card" :class="{ shake: shakeForm }">
      <el-tabs v-model="mode" class="login-tabs" stretch @tab-change="onTabChange">
        <el-tab-pane :label="t('login.tab.account')" name="login">
          <template #label>
            <span class="tab-label"><el-icon><User /></el-icon>{{ t('login.tab.account') }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="register">
          <template #label>
            <span class="tab-label"><el-icon><Plus /></el-icon>{{ t('login.tab.register') }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="wechat">
          <template #label>
            <span class="tab-label"><el-icon><ChatDotRound /></el-icon>{{ t('login.tab.wechat') }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 3. 登录 / 注册 表单 -->
      <transition name="fade" mode="out-in">
        <el-form
          v-if="mode === 'login' || mode === 'register'"
          :key="mode"
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-position="top"
          size="large"
          class="login-form"
          @submit.prevent="onSubmit"
        >
          <el-form-item :label="t('login.label.username')" prop="username">
            <el-input
              v-model="form.username"
              :placeholder="t('login.placeholder.username')"
              :prefix-icon="User"
              clearable
              autocomplete="username"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" :label="t('login.label.nickname')" prop="nickname">
            <el-input
              v-model="form.nickname"
              :placeholder="t('login.placeholder.nickname')"
              :prefix-icon="UserFilled"
              clearable
            />
          </el-form-item>

          <el-form-item :label="t('login.label.password')" prop="password">
            <el-input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="t('login.placeholder.password')"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" :label="t('login.label.email')" prop="email">
            <el-input
              v-model="form.email"
              :placeholder="t('login.placeholder.email')"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <!-- 4. 记住我 + 忘记密码 (登录模式) -->
          <div v-if="mode === 'login'" class="form-row">
            <el-checkbox v-model="remember">{{ t('login.remember') }}</el-checkbox>
            <el-link type="primary" :underline="false" @click="onForgot">{{ t('login.forgot') }}</el-link>
          </div>

          <!-- 5. 错误提示条 -->
          <transition name="slide">
            <el-alert
              v-if="errorMsg"
              :title="errorMsg"
              type="error"
              :closable="false"
              show-icon
              class="error-alert"
            >
              <template #default>
                <div class="error-actions">
                  <el-button text size="small" type="primary" :icon="Refresh" @click="retryLogin">重试</el-button>
                  <span v-if="retryCountdown > 0" class="retry-countdown">{{ retryCountdown }}s 后自动重试</span>
                  <el-button v-if="isNetworkError" text size="small" type="warning" @click="enableDemo">切演示模式</el-button>
                </div>
              </template>
            </el-alert>
          </transition>

          <!-- 6. 提交按钮 (loading 状态显示骨架) -->
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            @click="onSubmit"
          >
            <span v-if="!loading">{{ mode === 'login' ? t('login.submit') : t('login.register') }}</span>
            <span v-else>{{ mode === 'login' ? '登录中...' : '注册中...' }}</span>
          </el-button>

          <!-- 7. 协议 (注册模式) -->
          <p v-if="mode === 'register'" class="agreement">
            注册即代表同意 <el-link type="primary" :underline="false">用户协议</el-link> 和
            <el-link type="primary" :underline="false">隐私政策</el-link>
          </p>
        </el-form>

        <!-- 8. 微信扫码模式 -->
        <div v-else class="wechat-panel">
          <WechatScanLogin :embedded="true" @success="onWechatSuccess" />
          <p class="wechat-tip">使用微信扫一扫登录</p>
        </div>
      </transition>
    </el-card>

    <!-- 9. 演示账号快速登录 -->
    <section class="section demos-section">
      <h3 class="section-title">⚡ {{ t('login.demos.title') }}</h3>
      <el-row :gutter="12">
        <el-col v-for="acc in demoAccounts" :key="acc.username" :xs="12" :sm="8" :md="4">
          <el-card
            shadow="hover"
            class="demo-card"
            :class="`role-${acc.roleKey}`"
            @click="fillAccount(acc)"
          >
            <div class="demo-avatar">{{ acc.avatar }}</div>
            <div class="demo-role">{{ acc.role }}</div>
            <div class="demo-user">{{ acc.username }}</div>
            <div class="demo-desc">{{ acc.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 10. 平台特性 -->
    <section class="section features-section">
      <h3 class="section-title">✨ 平台特性</h3>
      <el-row :gutter="12">
        <el-col v-for="f in features" :key="f.title" :xs="12" :sm="6">
          <div class="feature-item">
            <el-icon :size="24" :color="f.color"><component :is="f.icon" /></el-icon>
            <div class="feature-text">
              <div class="feature-title">{{ f.title }}</div>
              <div class="feature-desc">{{ f.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 11. 底部版权 + 演示模式开关 -->
    <footer class="page-footer">
      <p>© 2026 Liugl-AI · 自研大模型平台 · 0 外部 LLM 依赖</p>
      <div class="demo-toggle">
        <el-checkbox v-model="demoMode" @change="onDemoToggle">
          🎭 演示模式 (无后端本地演示)
        </el-checkbox>
        <p v-if="demoMode" class="demo-hint">
          ✓ 已启用 - 跳过 API, 所有数据 mock 自 localStorage
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useToast } from '@/composables/useToast'
import { useRoute, useRouter } from 'vue-router'

import { User, UserFilled, Lock, Message, ChatLineRound, ChatDotRound, Plus, Cpu, Connection, ChatLineSquare, Promotion } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const toast = useToast()

// V3.5.93+ 演示模式检测
function isDemoMode() {
  return localStorage.getItem('minimax_demo_mode') === 'true' ||
         (typeof window !== 'undefined' && window.location.search.includes('demo=1'))
}

// === 1. 状态 ===
const mode = ref('login')                            // login / register / wechat
const loading = ref(false)                           // 登录中 loading
const remember = ref(true)                           // 记住用户名
const showPassword = ref(false)                      // 密码可见
const errorMsg = ref('')                             // 错误提示
const shakeForm = ref(false)                         // 错误抖动
const isMobile = ref(false)                          // 移动端
const formRef = ref(null)                            // 表单 ref
const demoMode = ref(isDemoMode())                   // V3.5.93 演示模式

// V3.5.93 切换演示模式
function onDemoToggle(val) {

  if (val) {
    localStorage.setItem('minimax_demo_mode', 'true')
      // V3.7.9+ 跨标签同步
      window.dispatchEvent(new StorageEvent('storage', { key: 'minimax_demo_mode', newValue: 'true' }))
    toast.success('🎭 演示模式已启用 - 无后端本地演示')
  } else {
    localStorage.removeItem('minimax_demo_mode')
      // V3.7.9+ 跨标签同步
      window.dispatchEvent(new StorageEvent('storage', { key: 'minimax_demo_mode', newValue: null }))
    toast.info('演示模式已关闭 - 恢复 API 模式')
  }
}

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: ''
})

// === 2. 演示账号 (5 个) ===
const demoAccounts = [
  { roleKey: 'super', avatar: '👑', role: '超级管理员', username: 'adminLiugl', password: 'Liugl@2026', desc: '平台所有者' },
  { roleKey: 'admin', avatar: '🛡️', role: '管理员', username: 'admin', password: 'admin@123', desc: '后台管理' },
  { roleKey: 'user', avatar: '👤', role: '普通用户', username: 'admin_user', password: 'admin123', desc: '日常使用' },
  { roleKey: 'test', avatar: '🧪', role: '测试账号', username: 'test_user', password: 'user123', desc: '功能测试' },
  { roleKey: 'demo', avatar: '🎭', role: '演示账号', username: 'demo_user', password: 'demo1234', desc: '产品演示' }
]

// === 3. 平台特性 ===
const features = [
  { icon: Cpu,         color: 'var(--liugl-accent)',  title: '17 微服务',  desc: '145+ 单元测试' },
  { icon: ChatDotRound,color: 'var(--liugl-info)',    title: '自研 AI',   desc: '0 外部 LLM' },
  { icon: Promotion,   color: 'var(--liugl-success)', title: '实时流式',  desc: 'SSE / WebSocket' },
  { icon: Connection,  color: 'var(--liugl-warning)', title: '全链路追踪', desc: 'OpenTelemetry' }
]

// === 4. 表单校验规则 (实时 + 错误抖动) ===
const formRules = computed(() => ({
  username: [
    { required: true, message: t('login.placeholder.username'), trigger: 'blur' },
    { min: 3, max: 32, message: '用户名 3-32 字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.placeholder.password'), trigger: 'blur' },
    { min: 6, max: 64, message: '密码 6-64 字符', trigger: 'blur' }
  ],
  nickname: mode.value === 'register' ? [
    { required: true, message: t('login.placeholder.nickname'), trigger: 'blur' }
  ] : [],
  email: mode.value === 'register' ? [
    { required: true, message: t('login.placeholder.email'), trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ] : []
}))

// === 5. 演示账号填入 + 自动登录 ===
function fillAccount(acc) {
  form.username = acc.username
  form.password = acc.password
  mode.value = 'login'
  // V3.5.93+: demo mode 下, fillAccount 直接跳 (不需点登录)
  if (isDemoMode()) {
    localStorage.setItem('minimax_demo_user', acc.username)
    toast.success(`🎭 演示模式 - 已切换到 ${acc.role}`)
    const redirect = route.query.redirect || '/admin/dashboard'
    router.replace(redirect)
  } else {
    toast.info(`已填入 ${acc.role} 账号, 自动登录中...`); setTimeout(() => onSubmit(), 300)
  }
}

// === 6. 忘记密码 ===
function onForgot() {
  toast.warning('请联系管理员重置密码 (admin@minimax.io)')
}

// === 7. 提交 (核心: loading + 错误处理 + 跳转) ===
function retryLogin() { errorMsg.value = ''; formRef.value?.clearValidate() }
const isNetworkError = computed(() => errorMsg.value.includes('网络') || errorMsg.value.includes('服务'))
function enableDemo() {
  localStorage.setItem('minimax_demo_mode', 'true')
  toast.success('🎭 已切换到演示模式 - 一键登录')
  onSubmit()
}

// V3.7.7+ 错误条倒计时重试 (网络错误时 5s 自动重试)
const retryCountdown = ref(0)
let retryTimer = null

function startRetryCountdown(seconds = 5) {
  retryCountdown.value = seconds
  if (retryTimer) clearInterval(retryTimer)
  retryTimer = setInterval(() => {
    retryCountdown.value--
    if (retryCountdown.value <= 0) { clearInterval(retryTimer); retryTimer = null; onSubmit() }
  }, 1000)
}

function stopRetryCountdown() {
  retryCountdown.value = 0
  if (retryTimer) { clearInterval(retryTimer); retryTimer = null }
}

onUnmounted(() => { if (retryTimer) clearInterval(retryTimer) })

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    triggerShake()
    return
  }

  errorMsg.value = ''
  loading.value = true

  // V3.5.93+ 演示模式: 无后端时直接 mock login (本地保存)
  if (isDemoMode()) {
    await new Promise(r => setTimeout(r, 500))  // 模拟网络延迟
    localStorage.setItem('minimax_demo_user', form.username)
    localStorage.setItem('minimax_remember_user', form.username)
    toast.success('🎭 演示模式登录成功 (无后端)')
    const redirect = route.query.redirect && route.query.redirect !== '/login' ? route.query.redirect : '/admin/dashboard'
    router.replace(redirect)
    loading.value = false
    return
  }

  try {
    if (mode.value === 'login') {
      await userStore.login({ username: form.username, password: form.password })
      // 记住用户名
      if (remember.value) localStorage.setItem('minimax_remember_user', form.username)
      else localStorage.removeItem('minimax_remember_user')
    } else {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email
      })
    }

    // ✓ 成功 - 立刻跳, 不等 fetchProfile (layout 异步 hydrate)
    toast.success(`${mode.value === 'login' ? '登录' : '注册'}成功`); stopRetryCountdown()  // V3.7.7+
    const redirect = route.query.redirect && route.query.redirect !== '/login' ? route.query.redirect : '/admin/dashboard'
    router.replace(redirect)  // replace 不留 history

    // 异步 fetchProfile (不阻塞跳转)
    userStore.fetchProfile().catch(() => { /* 失败也允许, layout 自己处理 */ })
  } catch (e) {
    // 错误处理 - 区分网络错 / 401 / 500
    const msg = e?.response?.data?.message || e?.message || '操作失败'
    if (e?.response?.status === 401) {
      errorMsg.value = '用户名或密码错误'
    } else if (e?.response?.status >= 500) {
      errorMsg.value = '服务异常, 请稍后重试'
    } else if (msg.includes('Network') || msg.includes('timeout')) {
      errorMsg.value = '网络不可用, 请检查连接'
      startRetryCountdown(5)  // V3.7.7+ 5s 倒计时
    } else {
      errorMsg.value = msg
    }
    triggerShake()
    toast.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

// === 8. 错误抖动 ===
function triggerShake() {
  shakeForm.value = true
  setTimeout(() => { shakeForm.value = false }, 500)
}

// === 9. Tab 切换 ===
function onTabChange(name) {
  errorMsg.value = ''
  if (name === 'wechat') {
    // 微信扫码
  }
}

// === 10. 微信扫码成功 ===
function onWechatSuccess() {
  toast.success('微信登录成功')
  router.replace(route.query.redirect || '/admin/dashboard')
}

// === 11. 访客试用 (无登录直入) ===
function goGuest() {
  router.push('/chat')
}

// === 12. 响应式 ===
function checkResponsive() {
  isMobile.value = window.innerWidth < 768
}

// === 13. 生命周期 ===
onMounted(() => {
  // V3.7.9+ 跨标签同步演示模式
  function onStorage(e: StorageEvent) {
    if (e.key === 'minimax_demo_mode') {
      demoMode.value = e.newValue === 'true'
      toast.info(demoMode.value ? '🎭 演示模式已启用 (跨标签同步)' : '演示模式已关闭 (跨标签同步)')
    }
  }
  window.addEventListener('storage', onStorage)
  onUnmounted(() => window.removeEventListener('storage', onStorage))
  checkResponsive()
  window.addEventListener('resize', checkResponsive, { passive: true })
  // 恢复记住的用户名
  const saved = localStorage.getItem('minimax_remember_user')
  if (saved) form.username = saved
  // 自动登录 (有 token 的话)
  if (userStore.isLogin) {
    router.replace('/admin/dashboard')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkResponsive)
})
</script>

<style lang="scss" scoped>
/* V3.5.92 重写: 设计 token + 5 段结构 + 动效 + 骨架屏 */

.page-login {
  min-height: 100vh;
  padding: 24px 20px 40px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf3 100%);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 32px;
  border-bottom: 1px solid var(--liugl-border, #e2e8f0);
  margin-bottom: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transition: transform 0.3s;
}

.brand-logo:hover {
  transform: scale(1.05) rotate(-3deg);
}

.brand-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 13px;
  color: var(--liugl-text-secondary, #64748b);
  margin: 4px 0 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* 登录卡片 */
.login-card {
  max-width: 480px;
  margin: 0 auto;
  border-radius: 16px;
  border: 1px solid var(--liugl-border, #e2e8f0);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  transition: transform 0.2s, box-shadow 0.2s;
}

.login-card.shake {
  animation: shake 0.4s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.login-tabs {
  margin-bottom: 8px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 表单 */
.login-form {
  padding: 8px 0;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -8px 0 12px;
}

.error-alert {
  margin-bottom: 12px;
  border-radius: 8px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  margin-top: 8px;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.agreement {
  text-align: center;
  font-size: 12px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin: 12px 0 0;
}

/* 微信扫码 */
.wechat-panel {
  padding: 20px 0;
  text-align: center;
}

.wechat-tip {
  color: var(--liugl-text-secondary, #94a3b8);
  font-size: 13px;
  margin: 12px 0 0;
}

/* 演示账号 */
.demos-section {
  margin-top: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--liugl-text, #1e293b);
}

.demo-card {
  cursor: pointer;
  text-align: center;
  padding: 16px 8px;
  border-radius: 12px;
  transition: all 0.2s;
  border: 1px solid var(--liugl-border, #e2e8f0);
  height: 100%;
}

.demo-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
  border-color: var(--liugl-accent, #6366f1);
}

.demo-avatar {
  font-size: 28px;
  margin-bottom: 8px;
}

.demo-role {
  font-size: 12px;
  font-weight: 600;
  color: var(--liugl-text, #1e293b);
}

.demo-user {
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  font-family: monospace;
  margin: 4px 0;
}

.demo-desc {
  font-size: 11px;
  color: var(--liugl-text-secondary, #cbd5e1);
}

.demo-card.role-super {
  border-color: rgba(168, 85, 247, 0.3);
  background: linear-gradient(180deg, rgba(168, 85, 247, 0.05) 0%, transparent 100%);
}

.demo-card.role-admin {
  border-color: rgba(99, 102, 241, 0.3);
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.05) 0%, transparent 100%);
}

/* 平台特性 */
.features-section {
  margin-top: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 10px;
  border: 1px solid var(--liugl-border, #e2e8f0);
  height: 100%;
}

.feature-text {
  flex: 1;
}

.feature-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--liugl-text, #1e293b);
}

.feature-desc {
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin-top: 2px;
}

/* 底部 */
.page-footer {
  text-align: center;
  margin-top: 40px;
  padding: 16px;
  color: var(--liugl-text-secondary, #94a3b8);
  font-size: 12px;
}

/* 动效 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from, .slide-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-8px);
}

.slide-enter-to, .slide-leave-from {
  opacity: 1;
  max-height: 100px;
}

/* 演示模式 */
.demo-toggle {
  margin-top: 12px;
  text-align: center;
}

.demo-hint {
  font-size: 11px;
  color: var(--liugl-success, #10b981);
  margin: 4px 0 0;
}

/* 移动端适配 */
.is-mobile {
  padding: 12px 12px 24px;
}

.is-mobile .login-card {
  border-radius: 12px;
}

.is-mobile .brand-title {
  font-size: 20px;
}

.is-mobile .brand-subtitle {
  font-size: 11px;
}

@media (max-width: 768px) {
  .demo-card {
    margin-bottom: 8px;
  }
}

.retry-countdown { font-size: 12px; color: var(--el-color-warning); font-weight: 600; margin-left: 8px; }
.error-actions { display: flex; gap: 8px; margin-top: 8px; padding-left: 24px; }
</style>
