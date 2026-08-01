<!--
  @file views/auth/H5Login.vue (H5 跨平台登录)
  @version V3.5.80+ (前端重写 Element Plus 2.4 标准模板)
  @description 5 段标准结构 - 移动端 4 平台 OAuth 登录
  @template V3.5.74 5 段样板 (header / platform / scan / qr / footer)
-->
<template>
  <div class="page-h5login">
    <!-- 1. page-header: 顶部品牌 + 副标题 -->
    <header class="page-header">
      <div class="brand">
        <img src="/icons/icon-192.svg" alt="Liugl-AI" class="brand-logo" />
        <div>
          <h1 class="brand-title gradient-text">登录</h1>
          <p class="brand-tagline">Liugl-AI 大模型平台 · 跨端</p>
        </div>
      </div>
      <el-button :icon="Back" @click="$router.push('/login')" plain>PC 登录</el-button>
    </header>

    <!-- 2. section: 4 平台 OAuth 按钮 -->
    <section class="section">
      <h3 class="section-title">选择登录方式</h3>
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

    <!-- 3. section: 扫码登录面板 (动态切换) -->
    <transition name="slide">
      <section v-if="activePlatform" class="section">
        <el-card shadow="hover" class="scan-card">
          <template #header>
            <div class="card-header">
              <el-icon><component :is="activePlatform.icon" /></el-icon>
              <span>{{ activePlatform.name }} 扫码登录</span>
              <el-button text :icon="Close" @click="activePlatform = null" style="margin-left: auto">取消</el-button>
            </div>
          </template>
          <WechatScanLogin v-if="activePlatform.id === 'wechat'" @login-success="onLogin" />
          <div v-else class="scan-placeholder">
            <el-icon :size="48" :color="activePlatform.color"><component :is="activePlatform.icon" /></el-icon>
            <p>{{ activePlatform.name }} 登录开发中</p>
            <p class="hint">请联系管理员开通 {{ activePlatform.name }} OAuth</p>
          </div>
        </el-card>
      </section>
    </transition>

    <!-- 4. section: 二维码 / 平台信息 (移动端) -->
    <section v-if="!activePlatform" class="section">
      <el-card shadow="never" class="info-card">
        <el-row :gutter="16" align="middle">
          <el-col :span="10">
            <img src="/icons/icon-192.svg" alt="Liugl-AI" class="info-qr" />
            <p class="info-qr-hint">扫码下载移动 App</p>
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

    <!-- 5. section: 平台识别 + footer -->
    <section class="section">
      <el-alert :title="detectedPlatform" type="info" :closable="false" class="platform-alert">
        <template #default>
          自动识别: <b>{{ detectedPlatform }}</b>
          <span v-if="unionId"> · UnionID: <code>{{ unionId }}</code></span>
        </template>
      </el-alert>
      <p class="footer-text">© 2026 Liugl-AI Platform · H5 跨端登录 V3.5.80</p>
    </section>
  </div>
</template>

<script setup>
/**
 * V3.5.80 H5 跨平台登录 5 段结构 + 6 原则
 * 1. page-header: 品牌 + 切回 PC
 * 2. section: 4 平台 OAuth 按钮 (微信扫码 / 公众号 / QQ / 支付宝)
 * 3. section: 扫码登录面板 (动态切换)
 * 4. section: 移动端信息 (二维码 + 能力)
 * 5. section: 平台识别 + footer
 *
 * 设计 token + 响应式断点
 * 业务代码保留: 4 平台 OAuth 跳转 + unionid 跨平台打通
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Close, Check } from '@element-plus/icons-vue'
import { ChatDotRound, ChatLineRound, Money } from '@element-plus/icons-vue'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const router = useRouter()
const activePlatform = ref(null)
const unionId = ref(localStorage.getItem('minimax_unionid') || '')

// === 1. 4 平台配置 (数据驱动) ===
const platforms = [
  { id: 'wechat', name: '微信扫码', desc: '打开微信扫一扫', icon: ChatDotRound, color: '#07c160' },
  { id: 'mp',     name: '公众号',  desc: '关注后授权登录', icon: ChatLineRound, color: '#07c160' },
  { id: 'qq',     name: 'QQ',      desc: 'QQ 一键登录',   icon: ChatDotRound, color: "#1296db"},
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

// === 3. 平台检测 (V3.5.76+ useBrowserCompat 已含, 简化版) ===
const detectedPlatform = computed(() => {
  const ua = navigator.userAgent
  if (/MicroMessenger/i.test(ua)) return '微信内置浏览器'
  if (/AlipayClient/i.test(ua)) return '支付宝内置浏览器'
  if (/QQ\//i.test(ua)) return 'QQ 内置浏览器'
  if (/iPhone|iPad|iPod/i.test(ua)) return 'iOS Safari'
  if (/Android/i.test(ua)) return 'Android Chrome'
  return '通用浏览器'
})

// === 4. 行为 ===
function onPlatformLogin(p) {
  if (p.id === 'wechat') {
    activePlatform.value = p
  } else {
    // 其它平台 OAuth 跳转 (开发中)
    ElMessage.info(`${p.name} 登录开发中, 暂用微信扫码`)
    activePlatform.value = p
  }
}

function onLogin(user) {
  // 登录成功, 存 unionid 跨平台打通
  if (user?.unionId) {
    localStorage.setItem('minimax_unionid', user.unionId)
    unionId.value = user.unionId
  }
  ElMessage.success('登录成功')
  router.push('/chat')
}

onMounted(() => {
  // 自动识别 unionid (从 URL query)
  const params = new URLSearchParams(window.location.search)
  if (params.get('unionid')) {
    unionId.value = params.get('unionid')
    localStorage.setItem('minimax_unionid', unionId.value)
  }
})
</script>

<style lang="scss" scoped>
.page-h5login {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px 16px 32px;
  min-height: 100vh;
  background: var(--liugl-bg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0 24px;

  .brand {
    display: flex;
    align-items: center;
    gap: 10px;
    .brand-logo {
      width: 40px;
      height: 40px;
      border-radius: var(--liugl-radius);
    }
    .brand-title {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
    }
    .brand-tagline {
      margin: 2px 0 0 0;
      font-size: 11px;
      color: var(--liugl-text-secondary);
    }
  }
}

.section { margin-bottom: 20px; }

.section-title {
  margin: 0 0 12px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--liugl-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.platform-card {
  text-align: center;
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform var(--liugl-transition-fast);
  &:hover { transform: translateY(-2px); }

  .platform-name {
    margin-top: 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--liugl-text);
  }
  .platform-desc {
    font-size: 11px;
    color: var(--liugl-text-secondary);
    margin-top: 2px;
  }
}

.scan-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }
}

.scan-placeholder {
  text-align: center;
  padding: 32px 16px;
  color: var(--liugl-text-secondary);
  p { margin: 12px 0 0 0; font-size: 14px; }
  .hint { font-size: 12px; color: var(--liugl-text-secondary); margin-top: 4px; }
}

.info-card {
  .info-qr {
    width: 100%;
    max-width: 120px;
    height: auto;
    border-radius: var(--liugl-radius);
    background: white;
    padding: 8px;
    display: block;
    margin: 0 auto;
  }
  .info-qr-hint {
    text-align: center;
    font-size: 11px;
    color: var(--liugl-text-secondary);
    margin: 4px 0 0 0;
  }
  h4 { margin: 0 0 8px 0; font-size: 14px; color: var(--liugl-text); }
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
      color: var(--liugl-text);
      .el-icon { color: var(--liugl-success); }
    }
  }
}

.platform-alert {
  margin-bottom: 12px;
  code {
    background: var(--liugl-bg-elevated);
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 11px;
  }
}

.footer-text {
  text-align: center;
  font-size: 11px;
  color: var(--liugl-text-secondary);
  margin: 16px 0 0 0;
}

.slide-enter-active,
.slide-leave-active { transition: all var(--liugl-transition); }
.slide-enter-from,
.slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
