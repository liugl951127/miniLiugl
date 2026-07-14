<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-feature-panel">
      <div class="feature-content">
        <h1 class="gradient-text">🚀 Liugl-AI Platform</h1>
        <p class="feature-tagline">企业级 AI · 智能协作 · 自研大模型</p>
        <ul class="feature-list">
          <li>✅ <b>17</b> 微服务 · <b>145+</b> 单元测试</li>
          <li>🤖 自研 AI (无外部 LLM 依赖)</li>
          <li>🎬 视频/音乐 <b>实时流式</b> 生成</li>
          <li>📊 7 种图表 / 看板 / 多模态</li>
          <li>🔐 RBAC + 审计 + 脱敏</li>
          <li>🌐 i18n 中英双语</li>
        </ul>
        <div class="feature-stats">
          <div><b>12</b><span>微服务</span></div>
          <div><b>211+</b><span>API</span></div>
          <div><b>62</b><span>页面</span></div>
          <div><b>532</b><span>i18n</span></div>
        </div>
      </div>
    </div>

    <div class="login-box">
      <div class="login-header">
        <h1 class="gradient-text" style="margin: 0 0 8px; font-size: 28px;">{{ t('app.name') || 'Liugl-AI' }}</h1>
        <p style="color: #909399; margin: 0; font-size: 13px;">{{ t('app.tagline') }}</p>
      </div>

      <el-tabs v-model="mode" stretch class="login-tabs">
        <el-tab-pane :label="'🔑 ' + (t('user.login') || '登录')" name="login" />
        <el-tab-pane :label="'📝 ' + (t('user.register') || '注册')" name="register" />
        <el-tab-pane :label="'📱 微信扫码'" name="wechat" />
      </el-tabs>

      <el-form v-if="mode !== 'wechat'" ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password" type="password" show-password
            placeholder="密码" :prefix-icon="Lock" autocomplete="current-password"
            @keyup.enter="onSubmit" clearable
          />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Avatar" clearable />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" clearable />
        </el-form-item>
        <div v-if="mode === 'login'" class="login-extras">
          <el-checkbox v-model="remember">记住我</el-checkbox>
          <a class="forgot-link" @click="onForgot">忘记密码？</a>
        </div>
        <el-button type="primary" size="large" :loading="loading" style="width: 100%;" @click="onSubmit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
        <el-divider v-if="mode === 'login'"><span class="divider-text">快速登录（演示）</span></el-divider>
        <div v-if="mode === 'login'" class="quick-accounts">
          <el-tag class="quick-tag" type="danger" effect="plain" @click="fillAccount('adminLiugl', 'Liugl@2026')">
            👑 adminLiugl (超管)
          </el-tag>
          <el-tag class="quick-tag" type="success" effect="plain" @click="fillAccount('admin', 'admin@123')">
            🔑 admin
          </el-tag>
          <el-tag class="quick-tag" type="info" effect="plain" @click="fillAccount('user', 'user@123')">
            👤 user
          </el-tag>
        </div>
      </el-form>

      <div v-else class="wechat-tab">
        <WechatScanLogin @login-success="onWechatLogin" />
        <p class="wechat-tip">
          没有微信？<a href="javascript:;" @click="mode = 'login'">账号密码登录</a>
        </p>
      </div>

      <div v-if="mode !== 'wechat'" class="login-tips">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <span style="font-size: 12px;">
              💡 演示账号: <code>admin</code> / <code>admin@123</code> (或点击上方快速登录)
            </span>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar, Message } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login')
const formRef = ref()
const loading = ref(false)
const remember = ref(true)
const form = ref({
  username: '',
  password: '',
  nickname: '',
  email: ''
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少 6 位', trigger: 'blur' }]
}

onMounted(() => {
  // 恢复记住的用户名
  const saved = localStorage.getItem('minimax_remember_user')
  if (saved) {
    form.value.username = saved
  }
})

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

function onWechatLogin(profile) {
  ElMessage.success('微信登录成功')
  router.push('/admin/dashboard')
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
  position: relative;
}
.login-bg {
  position: fixed; inset: 0; z-index: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  opacity: 0.95;
}
.login-feature-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
  color: white;
}
.feature-content { max-width: 480px; }
.gradient-text {
  background: linear-gradient(135deg, #fff 0%, #fbbf24 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 48px;
  font-weight: 800;
  margin: 0 0 8px;
}
.feature-tagline { font-size: 18px; opacity: 0.9; margin: 0 0 32px; }
.feature-list { list-style: none; padding: 0; margin: 0 0 32px; line-height: 2.2; font-size: 15px; }
.feature-list b { color: #fbbf24; }
.feature-stats {
  display: flex;
  gap: 32px;
  padding: 24px 0;
  border-top: 1px solid rgba(255,255,255,0.2);
  border-bottom: 1px solid rgba(255,255,255,0.2);
}
.feature-stats > div {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.feature-stats b { font-size: 32px; font-weight: 700; color: #fbbf24; }
.feature-stats span { font-size: 12px; opacity: 0.85; margin-top: 4px; }

.login-box {
  width: 460px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  padding: 40px;
  margin: auto;
  position: relative;
  z-index: 1;
}
.login-header { text-align: center; margin-bottom: 24px; }
.login-tabs :deep(.el-tabs__item) { font-size: 14px; }
.login-extras {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 13px;
}
.forgot-link { color: #409EFF; cursor: pointer; }
.forgot-link:hover { text-decoration: underline; }
.divider-text { font-size: 12px; color: #909399; }
.quick-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.quick-tag { cursor: pointer; }
.quick-tag:hover { transform: scale(1.05); }
.wechat-tab { text-align: center; padding: 20px 0; }
.wechat-tip { color: #909399; font-size: 13px; }
.login-tips { margin-top: 16px; }
.login-tips code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #d63384;
}

@media (max-width: 768px) {
  .login-feature-panel { display: none; }
  .login-box { width: 100%; max-width: 460px; margin: 0 auto; }
}
</style>
