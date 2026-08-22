<!--
  @file views/auth/H5Login.vue (H5 跨平台登录)
  @version V6.8.10+ 企业级升级 (加账号密码表单 + 错误区分 + 反馈)
  @description H5 跨端登录 - 4 平台 OAuth + 账号密码 + 表单规则
-->
<template>
  <div class="page-h5login">
    <el-watermark v-if="false" content="V3.6.1" :font="{ size: 8 }" class="page-watermark" />
    <header class="page-header">
      <div class="brand">
        <img src="/icons/icon-192.svg" alt="Liugl-AI" class="brand-logo" />
        <div>
          <h1 class="brand-title gradient-text">登录</h1>
          <p class="brand-tagline">Liugl-AI 大模型平台 · 跨端</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Back" @click="$router.push('/login')" plain size="small">
          {{ t('h5login.pc') }}
        </el-button>
      </div>
    </header>

    <!-- 1. 账号密码登录 (V6.8.10+ 新增，与 PC 端统一) -->
    <section class="section" v-if="mode === 'account'">
      <el-card shadow="hover" class="login-card" :class="{ shake: shakeForm }">
        <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-position="top"
          size="default"
          @submit.prevent="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              autocomplete="username"
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <!-- 错误提示 (明确区分错误类型) -->
          <transition name="slide">
            <el-alert
              v-if="errorMsg"
              :title="errorMsg"
              :type="errorType"
              :closable="false"
              show-icon
              class="error-alert"
            />
          </transition>

          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            @click="onSubmit"
          >
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
          <el-button text size="small" class="switch-btn" @click="mode = 'platform'">
            ← 返回第三方登录
          </el-button>
        </el-form>
      </el-card>
    </section>

    <!-- 2. 4 平台 OAuth (V6.8.10+ 强化: 加 loading 反馈) -->
    <section v-else class="section">
      <h3 class="section-title">{{ t('h5login.platforms') }}</h3>
      <el-row :gutter="12">
        <el-col v-for="p in platforms" :key="p.id" :xs="12" :sm="6">
          <el-card
            shadow="hover"
            class="platform-card"
            :loading="loggingPlatform === p.id"
            @click="onPlatformLogin(p)"
          >
            <el-icon :size="32" :color="p.color"><component :is="p.icon" /></el-icon>
            <div class="platform-name">{{ p.name }}</div>
            <div class="platform-desc">{{ p.desc }}</div>
          </el-card>
        </el-col>
      </el-row>

      <div class="account-fallback">
        <el-divider><span class="divider-text">或</span></el-divider>
        <el-button :icon="User" plain size="default" @click="mode = 'account'" class="account-btn">
          账号密码登录
        </el-button>
      </div>
    </section>

    <!-- 3. 扫码登录面板 (动态切换) -->
    <transition name="slide">
      <section v-if="activePlatform" class="section">
        <el-card shadow="hover" class="scan-card">
          <template #header>
            <div class="card-header">
              <el-icon><component :is="activePlatform.icon" /></el-icon>
              <span>{{ activePlatform.name }} 扫码登录</span>
              <el-button text :icon="Close" @click="activePlatform = null" style="margin-left: auto">
                取消
              </el-button>
            </div>
          </template>
          <WechatScanLogin v-if="activePlatform.id === 'wechat'" @login-success="onLogin" />
          <div v-else class="scan-placeholder">
            <el-icon :size="48" :color="activePlatform.color">
              <component :is="activePlatform.icon" />
            </el-icon>
            <p>{{ activePlatform.name }} 登录开发中</p>
            <p class="hint">请联系管理员开通 {{ activePlatform.name }} OAuth</p>
            <el-alert type="info" :closable="false" style="margin-top: 12px">
              <template #title>💡 提示: 可使用账号密码登录</template>
            </el-alert>
          </div>
        </el-card>
      </section>
    </transition>

    <!-- 4. 平台能力 + footer -->
    <section v-if="!activePlatform" class="section">
      <el-card shadow="never" class="info-card">
        <el-row :gutter="16" align="middle">
          <el-col :span="10">
            <img src="/icons/icon-192.svg" alt="Liugl-AI" class="info-qr" />
            <p class="info-qr-hint">{{ t('h5login.app') }}</p>
          </el-col>
          <el-col :span="14">
            <h4>平台能力</h4>
            <ul class="info-list">
              <li v-for="(item, i) in capabilities" :key="i">
                <el-icon><Check /></el-icon> {{ item }}
              </li>
            </ul>
          </el-col>
        </el-row>
      </el-card>
    </section>

    <section class="section">
      <el-alert :title="detectedPlatform" type="info" :closable="false" class="platform-alert">
        <template #default>
          自动识别: <b>{{ detectedPlatform }}</b>
          <span v-if="unionId"> · UnionID: <code>{{ unionId }}</code></span>
        </template>
      </el-alert>
      <p class="footer-text">© {{ currentYear }} Liugl-AI Platform · H5 跨端登录 v{{ appVersion }}</p>
    </section>
  </div>
</template>

<script setup>
/**
 * V6.8.10 升级: 加账号密码表单 + 错误区分 + 反馈
 */
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from '@/i18n'
import { useRouter } from 'vue-router'
import {
  Back, Close, Check, Promotion, User, Lock,
  ChatDotRound, ChatLineRound, Money,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const { t } = useI18n()
const router = useRouter()
const toast = useToast()
const userStore = useUserStore()

// 静态元信息
const currentYear = new Date().getFullYear()
const appVersion = '6.8.10'

// === 1. 模式: platform (默认) / account (账号密码登录) ===
const mode = ref('platform')

// === 2. 平台扫码相关状态 ===
const activePlatform = ref(null)
const loggingPlatform = ref(null)  // 平台 OAuth loading 状态
const unionId = ref(localStorage.getItem('minimax_unionid') || '')

// === 3. 账号密码登录 ===
const formRef = ref(null)
const loading = ref(false)
const shakeForm = ref(false)
const errorMsg = ref('')
const errorType = ref<'error' | 'warning' | 'info'>('error')

const form = reactive({
  username: '',
  password: ''
})

// === 4. 表单 :rules ===
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32 字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 字符', trigger: 'blur' }
  ]
}

// === 5. 4 平台配置 ===
const platforms = [
  { id: 'wechat', name: '微信扫码', desc: '打开微信扫一扫', icon: ChatDotRound, color: '#07c160' },
  { id: 'mp',     name: '公众号',  desc: '关注后授权登录', icon: ChatLineRound, color: '#07c160' },
  { id: 'qq',     name: 'QQ',      desc: 'QQ 一键登录',   icon: ChatDotRound, color: '#1296db' },
  { id: 'alipay', name: '支付宝',   desc: '支付宝扫一扫',  icon: Money,        color: '#1677ff' }
]

// === 6. 平台能力 ===
const capabilities = [
  '多模型对话 (GPT/Claude/自研)',
  'AI 工具调用 + 插件市场',
  '知识库 + RAG 检索',
  '流式响应 (SSE / WebSocket)',
  'OpenTelemetry 链路追踪'
]

// === 7. 平台检测 ===
const detectedPlatform = computed(() => {
  const ua = navigator.userAgent
  if (/MicroMessenger/i.test(ua)) return '微信内置浏览器'
  if (/AlipayClient/i.test(ua)) return '支付宝内置浏览器'
  if (/QQ\//i.test(ua)) return 'QQ 内置浏览器'
  if (/iPhone|iPad|iPod/i.test(ua)) return 'iOS Safari'
  if (/Android/i.test(ua)) return 'Android Chrome'
  return '通用浏览器'
})

// === 8. 平台 OAuth 登录 ===
function onPlatformLogin(p) {
  if (loggingPlatform.value) return // 防抖
  if (p.id === 'wechat') {
    activePlatform.value = p
  } else {
    // 非微信平台：模拟 OAuth 流程 + 反馈
    loggingPlatform.value = p.id
    setTimeout(() => {
      loggingPlatform.value = null
      activePlatform.value = p
      toast.info(`${p.name} 登录开发中，请使用微信扫码或账号密码登录`)
    }, 300)
  }
}

// === 9. 账号密码登录 (V6.8.10+ 加 :rules + 错误区分) ===
async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    errorType.value = 'warning'
    errorMsg.value = '请检查表单填写是否正确'
    triggerShake()
    return
  }
  errorMsg.value = ''
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    toast.success('登录成功')
    router.replace('/chat')
    userStore.fetchProfile().catch(() => { /* layout 自己 hydrate */ })
  } catch (e) {
    const msg = e?.response?.data?.message || e?.response?.data?.msg || e?.message || '操作失败'
    const status = e?.response?.status
    if (status === 403 || /锁定|封禁|禁用|banned|locked/i.test(msg)) {
      errorType.value = 'error'
      errorMsg.value = '🚫 账号已被锁定或禁用，请联系管理员'
    } else if (status === 404 || /不存在|未找到|not\s*found/i.test(msg)) {
      errorType.value = 'error'
      errorMsg.value = '👤 账号不存在，请检查用户名'
    } else if (status === 401 || /密码|password|invalid/i.test(msg)) {
      errorType.value = 'error'
      errorMsg.value = '🔑 密码错误，请重新输入'
    } else if (status >= 500) {
      errorType.value = 'error'
      errorMsg.value = '💥 服务异常，请稍后重试'
    } else if (/network|timeout|网络/i.test(msg)) {
      errorType.value = 'warning'
      errorMsg.value = '📡 网络不可用，请检查连接'
    } else {
      errorType.value = 'error'
      errorMsg.value = msg
    }
    triggerShake()
  } finally {
    loading.value = false
  }
}

function triggerShake() {
  shakeForm.value = true
  setTimeout(() => { shakeForm.value = false }, 500)
}

// === 10. 微信扫码成功 ===
function onLogin(user) {
  if (user?.unionId) {
    localStorage.setItem('minimax_unionid', user.unionId)
    unionId.value = user.unionId
  }
  toast.success('登录成功')
  router.push('/chat')
}

onMounted(() => {
  // 恢复记住的用户名 (可选增强)
  const saved = localStorage.getItem('minimax_remember_user')
  if (saved) form.username = saved
})
</script>

<style lang="scss" scoped>
.page-h5login {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 16px;
  padding-bottom: 60px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 40px;
  height: 40px;
}

.brand-title {
  font-size: 20px;
  margin: 0;
  font-weight: 700;
}

.brand-tagline {
  font-size: 12px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--liugl-text, #1e293b);
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 账号密码登录 */
.login-card {
  border-radius: 12px;
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
.error-alert {
  margin-bottom: 12px;
  border-radius: 8px;
}
.submit-btn {
  width: 100%;
  height: 40px;
  font-weight: 600;
  border-radius: 8px;
}
.switch-btn {
  width: 100%;
  margin-top: 8px;
}

.account-fallback {
  margin-top: 16px;
}
.divider-text {
  font-size: 12px;
  color: var(--liugl-text-secondary, #94a3b8);
}
.account-btn {
  width: 100%;
  height: 40px;
}

.platform-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--liugl-border, #e2e8f0);
  border-radius: 12px;
}

.platform-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.platform-name {
  font-size: 14px;
  font-weight: 600;
  margin: 6px 0 2px;
}

.platform-desc {
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
}

.demo-card {
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--liugl-border, #e2e8f0);
  border-radius: 10px;
  padding: 8px;
}

.demo-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--liugl-primary, #5b8def);
}

.demo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.demo-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--liugl-text, #1e293b);
}

.demo-username {
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin-bottom: 6px;
}

.scan-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.scan-placeholder {
  text-align: center;
  padding: 24px 0;
  color: var(--liugl-text-secondary, #94a3b8);
}

.scan-placeholder p {
  margin: 8px 0;
}

.scan-placeholder .hint {
  font-size: 12px;
}

.info-card {
  border-radius: 12px;
}

.info-qr {
  width: 100%;
  max-width: 100px;
  background: white;
  padding: 8px;
  display: block;
  margin: 0 auto;
}

.info-qr-hint {
  text-align: center;
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin: 4px 0 0 0;
}

h4 { margin: 0 0 8px 0; font-size: 14px; color: var(--liugl-text, #1e293b); }

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  li {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 0;
    font-size: 12px;
    color: var(--liugl-text, #1e293b);
    .el-icon { color: var(--liugl-success, #10b981); }
  }
}

.platform-alert {
  margin-bottom: 12px;
  code {
    background: var(--liugl-bg-elevated, #f1f5f9);
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 11px;
  }
}

.demo-badge {
  color: #f59e0b;
  font-weight: 600;
}

.footer-text {
  text-align: center;
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin: 16px 0 0 0;
}

.gradient-text {
  background: linear-gradient(135deg, #5b8def 0%, #a855f7 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.slide-enter-active,
.slide-leave-active { transition: all 0.3s; }
.slide-enter-from,
.slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
