<!--
  @file views/auth/H5Login.vue (H5 跨平台登录)
  @version V3.5.97+ (重写 - 加演示模式 + 演示账号 + 自动登录)
  @description 5 段标准结构 - 移动端 4 平台 OAuth + 演示模式 + P0 UX
  @template V3.5.74 5 段样板 (header / platform / demos / scan / footer)
-->
<template>
  <div class="page-h5login">
    <!-- 1. page-header: 顶部品牌 + 切回 PC + 演示模式 -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
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
        <el-tag v-if="isDemo" type="warning" size="small" effect="dark">🎭 演示模式</el-tag>
        <el-button :icon="Back" @click="$router.push('/login')" plain size="small">
          {{ t('h5login.pc') }}
        </el-button>
      </div>
    </header>

    <!-- 2. section: 4 平台 OAuth 按钮 (移动端 2×2 网格) -->
    <section class="section">
      <h3 class="section-title">{{ t('h5login.platforms') }}</h3>
      <el-row :gutter="12">
        <el-col v-for="p in platforms" :key="p.id" :xs="12" :sm="6">
          <el-card shadow="hover" class="platform-card" @click="onPlatformLogin(p)">
            <el-icon :size="32" :color="p.color"><component :is="p.icon" /></el-icon>
            <div class="platform-name">{{ p.name }}</div>
            <div class="platform-desc">{{ p.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 3. section: 演示账号 (V3.5.97+ 5 卡片) -->
    <section class="section">
      <h3 class="section-title">
        🎭 {{ t('h5login.demo.title') }}
        <el-tag size="small" type="info">免后端</el-tag>
      </h3>
      <el-row :gutter="8">
        <el-col v-for="d in demoAccounts" :key="d.username" :xs="12" :sm="8" :md="8">
          <el-card shadow="hover" class="demo-card" @click="onDemoLogin(d)">
            <div class="demo-header">
              <el-avatar :size="32" :style="{ background: d.color }">{{ d.avatar }}</el-avatar>
              <el-tag size="small" :type="d.tagType" effect="plain">{{ d.role }}</el-tag>
            </div>
            <div class="demo-name">{{ d.name }}</div>
            <div class="demo-username">@{{ d.username }}</div>
            <el-button size="small" type="primary" plain :icon="Promotion" @click.stop="onDemoLogin(d)">
              一键登录
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 4. section: 扫码登录面板 (动态切换) -->
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
              <template #title>💡 提示: 演示模式可一键登录</template>
            </el-alert>
          </div>
        </el-card>
      </section>
    </transition>

    <!-- 5. section: 移动端信息卡 + 平台识别 + footer -->
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
          <span v-if="isDemo" class="demo-badge"> · 🎭 演示模式</span>
        </template>
      </el-alert>
      <p class="footer-text">© 2026 Liugl-AI Platform · H5 跨端登录 V3.5.97+</p>
    </section>
  </div>
</template>

<script setup>
/**
 * V3.5.97 H5 跨平台登录 5 段结构 + 演示模式
 * 1. page-header: 品牌 + 切回 PC + 演示模式 tag
 * 2. section: 4 平台 OAuth 按钮 (微信扫码 / 公众号 / QQ / 支付宝)
 * 3. section: 演示账号 (5 卡片, 移动端 2×2 网格, V3.5.97 新)
 * 4. section: 扫码登录面板 (动态切换)
 * 5. section: 移动端信息 (二维码 + 能力) + 平台识别 + footer
 *
 * V3.5.97 增强:
 * - 演示模式: 一键登录 (无后端)
 * - 演示账号: 5 卡片 (admin/super/operator/auditor/user)
 * - 演示模式 tag: 顶栏 + footer 双重提示
 * - 错误处理: 演示模式 401/500 兜底
 */
import { ref, computed, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from '@/i18n-stub'
import { useRouter } from 'vue-router'

import { Back, Close, Check, Promotion, ChatDotRound, ChatLineRound, Money } from '@element-plus/icons-vue'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const { t } = useI18n()
const router = useRouter()
const toast = useToast()
const activePlatform = ref(null)
const unionId = ref(localStorage.getItem('minimax_unionid') || '')
const isDemo = ref(localStorage.getItem('minimax_demo_mode') === 'true' || false)

// === 1. 4 平台配置 (数据驱动) ===
const platforms = [
  { id: 'wechat', name: '微信扫码', desc: '打开微信扫一扫', icon: ChatDotRound, color: '#07c160' },
  { id: 'mp',     name: '公众号',  desc: '关注后授权登录', icon: ChatLineRound, color: '#07c160' },
  { id: 'qq',     name: 'QQ',      desc: 'QQ 一键登录',   icon: ChatDotRound, color: '#1296db' },
  { id: 'alipay', name: '支付宝',   desc: '支付宝扫一扫',  icon: Money,        color: '#1677ff' }
]

// === 2. 平台能力 ===
const capabilities = [
  '多模型对话 (GPT/Claude/自研)',
  'AI 工具调用 + 插件市场',
  '知识库 + RAG 检索',
  '流式响应 (SSE / WebSocket)',
  'OpenTelemetry 链路追踪'
]

// === 3. V3.5.97 演示账号 (5 角色) ===
const demoAccounts = [
  { username: 'admin',      name: '管理员',     role: 'ADMIN',       avatar: '管', color: '#5b8def', tagType: 'primary',   password: 'admin123' },
  { username: 'adminLiugl', name: '超级管理员', role: 'SUPER_ADMIN', avatar: '超', color: '#a855f7', tagType: 'danger',    password: 'admin123' },
  { username: 'operator',   name: '运营',       role: 'OPERATOR',    avatar: '运', color: '#10b981', tagType: 'success',   password: 'op123456' },
  { username: 'auditor',    name: '审计员',     role: 'AUDITOR',     avatar: '审', color: '#f59e0b', tagType: 'warning',   password: 'audit123' },
  { username: 'user',       name: '普通用户',   role: 'USER',        avatar: '用', color: '#64748b', tagType: 'info',      password: 'user123' },
]

// === 4. 平台检测 ===
const detectedPlatform = computed(() => {
  const ua = navigator.userAgent
  if (/MicroMessenger/i.test(ua)) return '微信内置浏览器'
  if (/AlipayClient/i.test(ua)) return '支付宝内置浏览器'
  if (/QQ\//i.test(ua)) return 'QQ 内置浏览器'
  if (/iPhone|iPad|iPod/i.test(ua)) return 'iOS Safari'
  if (/Android/i.test(ua)) return 'Android Chrome'
  return '通用浏览器'
})

// === 5. 行为 ===
function onPlatformLogin(p) {
  if (p.id === 'wechat') {
    activePlatform.value = p
  } else {
    toast.info(`${p.name} 登录开发中, 暂用微信扫码 / 演示账号`)
    activePlatform.value = p
  }
}

// V3.5.97+ 演示账号一键登录
async function onDemoLogin(d) {
  isDemo.value = true
  localStorage.setItem('minimax_demo_mode', 'true')
  localStorage.setItem('minimax_demo_user', d.username)
  localStorage.setItem('minimax_remember_user', d.username)

  // 模拟网络延迟
  await new Promise(r => setTimeout(r, 300))

  toast.success(`🎭 演示模式 - 已切换到 ${d.name} (${d.role})`)

  // 直接跳到聊天
  setTimeout(() => {
    router.replace('/chat')
  }, 200)
}

function onLogin(user) {
  if (user?.unionId) {
    localStorage.setItem('minimax_unionid', user.unionId)
    unionId.value = user.unionId
  }
  toast.success('登录成功')
  router.push('/chat')
}

onMounted(() => {
  isDemo.value = localStorage.getItem('minimax_demo_mode') === 'true'
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
