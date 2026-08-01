<!--
  @file views/auth/Login.vue (登录页)
  @version V3.5.80+ (前端重写 Element Plus 2.4 标准模板)
  @description 5 段标准结构 - 平台登录 + 演示账号 + 微信扫码
  @template V3.5.74 5 段样板 (header / brand / form / demos / footer)
-->
<template>
  <div class="page-login">
    <!-- 1. page-header: 顶部品牌 + 副标题 -->
    <header class="page-header">
      <div class="brand">
        <img src="/icons/icon-192.svg" alt="Liugl-AI" class="brand-logo" />
        <div>
          <h1 class="brand-title gradient-text">Liugl-AI Platform</h1>
          <p class="brand-tagline">{{ t("login.subtitle") }}</p>
        </div>
      </div>
      <el-button :icon="ChatLineRound" @click="router.push('/chat')" plain>{{ t("login.guest") }}</el-button>
    </header>

    <!-- 2. section: 4 标签页 (login/register/wechat/quick) -->
    <section class="section">
      <el-tabs v-model="mode" class="login-tabs" stretch>
        <el-tab-pane :label="t('login.tab.account')" name="login">
          <el-icon><Lock /></el-icon>
        </el-tab-pane>
        <el-tab-pane :label="t('login.tab.register')" name="register">
          <el-icon><User /></el-icon>
        </el-tab-pane>
        <el-tab-pane :label="t('login.tab.wechat')" name="wechat">
          <el-icon><ChatDotRound /></el-icon>
        </el-tab-pane>
      </el-tabs>
    </section>

    <!-- 3. section: 表单 (账号密码 / 注册) -->
    <section v-if="mode !== 'wechat'" class="section">
      <el-card shadow="hover" class="form-card">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="onSubmit"
        >
          <el-form-item v-if="mode === 'register'" label="昵称" prop="nickname">
            <el-input v-model="form.nickname" :placeholder="t('login.placeholder.nickname')" :prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              :placeholder="t('login.placeholder.username')"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :placeholder="t('login.placeholder.password')"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="邮箱" prop="email">
            <el-input v-model="form.email" :placeholder="t('login.placeholder.email')" :prefix-icon="Message" clearable />
          </el-form-item>

          <el-form-item v-if="mode === 'login'">
            <div class="form-options">
              <el-checkbox v-model="remember">记住用户名</el-checkbox>
              <el-link type="primary" :underline="false" @click="onForgot">{{ t('login.forgot') }}</el-link>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            @click="onSubmit"
            size="large"
          >
            {{ mode === 'login' ? '登录' : '注册' }}
          </el-button>
        </el-form>
      </el-card>
    </section>

    <!-- 3-alt. section: 微信扫码 -->
    <section v-else class="section">
      <el-card shadow="hover" class="wechat-card">
        <WechatScanLogin />
      </el-card>
    </section>

    <!-- 4. section: 演示账号 (dev 模式) -->
    <section v-if="mode === 'login'" class="section">
      <el-card shadow="never" class="demos-card">
        <template #header>
          <div class="card-header">
            <el-icon><Cpu /></el-icon>
            <span>{{ t('login.demos.title') }}</span>
          </div>
        </template>
        <el-row :gutter="12">
          <el-col v-for="acc in demoAccounts" :key="acc.username" :xs="12" :sm="8">
            <el-card
              shadow="hover"
              class="demo-card"
              @click="fillAccount(acc.username, acc.password)"
            >
              <div class="demo-role">{{ acc.role }}</div>
              <div class="demo-name">{{ acc.username }}</div>
              <div class="demo-desc">{{ acc.desc }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </section>

    <!-- 5. section: 平台特性 (footer 信息) -->
    <section class="section">
      <div class="features-grid">
        <div v-for="f in features" :key="f.title" class="feature-item">
          <el-icon :size="20" :color="f.color">{{ f.icon }}</el-icon>
          <span>{{ f.title }}</span>
        </div>
      </div>
      <p class="footer-text">© 2026 Liugl-AI Platform · V3.5.80 标准化模板</p>
    </section>
  </div>
</template>

<script setup>
/**
 * V3.5.80 登录页 5 段结构 + 6 原则
 * 1. page-header: 品牌 + 访客入口
 * 2. section (4 标签): login / register / wechat
 * 3. section: 表单 (账号密码 / 注册) 或 微信扫码
 * 4. section: {{ t('login.demos.title') }}
 * 5. section: 平台特性 + footer
 *
 * 设计 token: var(--liugl-primary/accent/success)
 * 响应式: el-col xs 12 / sm 8 / md 6
 * i18n: 预留 $t() (V3.5.81+ 接入)
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Message, ChatDotRound, ChatLineRound, Cpu, DataLine, Connection, TrendCharts
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useI18n } from 'vue-i18n'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// === 1. 状态 ===
const mode = ref('login')
const formRef = ref()
const loading = ref(false)
const remember = ref(true)
const form = ref({ username: '', password: '', nickname: '', email: '' })

// === 2. 表单规则 (i18n 预留) ===
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' }
  ]
}

// === 3. 演示账号 (V3.5.50+ 5 账号 BCrypt) ===
const demoAccounts = [
  { role: '👑 超级管理员', username: 'adminLiugl',  password: 'admin123456', desc: '平台所有者' },
  { role: '🛡️ 管理员',    username: 'admin',       password: 'admin123',    desc: '管理后台' },
  { role: '🧪 沙箱测试',  username: 'admin_user',  password: 'admin123',    desc: '管理员级' },
  { role: '👤 普通用户',  username: 'test_user',   password: 'admin123',    desc: '受限权限' },
  { role: '🏢 Demo 租户', username: 'demo_user',   password: 'admin123',    desc: '租户 ID=2' }
]

// === 4. 平台特性 (用于底部展示) ===
const features = [
  { icon: DataLine,     color: 'var(--liugl-primary)', title: '17 微服务 · 145+ 单元测试' },
  { icon: Cpu,          color: 'var(--liugl-accent)',  title: '自研 AI · 0 外部 LLM 依赖' },
  { icon: Connection,   color: 'var(--liugl-success)', title: '实时流式 · SSE / WebSocket' },
  { icon: TrendCharts,  color: 'var(--liugl-warning)', title: 'OpenTelemetry · 全链路追踪' }
]

// === 5. 行为 ===
function fillAccount(u, p) {
  form.value.username = u
  form.value.password = p
  ElMessage.info('已填入演示账号, 点击登录')
}

function onForgot() {
  ElMessage.warning('请联系管理员重置密码')
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      if (mode.value === 'login') {
        await userStore.login(form.value)
        if (remember.value) localStorage.setItem('minimax_remember_user', form.value.username)
        else localStorage.removeItem('minimax_remember_user')
      } else {
        await userStore.register(form.value)
      }
      ElMessage.success(`${mode.value === 'login' ? '登录' : '注册'}成功`)
      router.push(route.query.redirect || '/admin/dashboard')
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  const saved = localStorage.getItem('minimax_remember_user')
  if (saved) form.value.username = saved
})
</script>

<style lang="scss" scoped>
/* V3.5.80 标准化样式 - 全 var() 引用, 5 段结构对应 5 个 section class */
.page-login {
  max-width: 480px;
  margin: 0 auto;
  padding: 24px 20px 40px;
  min-height: 100vh;
  background: var(--liugl-bg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 32px;

  .brand {
    display: flex;
    align-items: center;
    gap: 12px;

    .brand-logo {
      width: 48px;
      height: 48px;
      border-radius: var(--liugl-radius);
    }
    .brand-title {
      margin: 0;
      font-size: 22px;
      font-weight: 700;
    }
    .brand-tagline {
      margin: 4px 0 0 0;
      font-size: 12px;
      color: var(--liugl-text-secondary);
    }
  }
}

.login-tabs {
  margin-bottom: 16px;
  :deep(.el-tabs__item) {
    font-size: 14px;
    font-weight: 500;
  }
}

.form-card,
.wechat-card,
.demos-card {
  margin-bottom: 16px;
  border-radius: var(--liugl-radius-lg);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--liugl-radius);
  margin-top: 8px;
}

.demo-card {
  cursor: pointer;
  margin-bottom: 8px;
  text-align: center;
  transition: transform var(--liugl-transition-fast);
  &:hover { transform: translateY(-2px); }

  .demo-role {
    font-size: 12px;
    font-weight: 600;
    color: var(--liugl-primary);
    margin-bottom: 4px;
  }
  .demo-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--liugl-text);
    font-family: var(--liugl-font-mono);
  }
  .demo-desc {
    font-size: 11px;
    color: var(--liugl-text-secondary);
    margin-top: 2px;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
  .feature-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: var(--liugl-bg-elevated);
    border: 1px solid var(--liugl-border);
    border-radius: var(--liugl-radius);
    font-size: 12px;
    color: var(--liugl-text);
  }
}

.footer-text {
  text-align: center;
  font-size: 11px;
  color: var(--liugl-text-secondary);
  margin: 16px 0 0 0;
}
</style>
