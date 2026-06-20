<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-box">
      <div class="login-header">
        <h1 class="gradient-text" style="margin: 0 0 8px; font-size: 32px;">MiniMax</h1>
        <p style="color: var(--minimax-text-secondary); margin: 0;">大模型应用平台 · v5</p>
      </div>

      <!-- V5: 3 种登录方式 (密码 / 注册 / 微信扫码) -->
      <el-tabs v-model="mode" stretch class="login-tabs">
        <el-tab-pane label="🔑 账号密码" name="login" />
        <el-tab-pane label="📝 注册" name="register" />
        <el-tab-pane label="📱 微信扫码" name="wechat" />
      </el-tabs>

      <!-- 密码登录 / 注册 -->
      <el-form v-if="mode !== 'wechat'" ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password" type="password" show-password
            placeholder="密码" :prefix-icon="Lock" autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width: 100%;" @click="onSubmit">
          {{ mode === 'login' ? '登 录' : '注 册 并 登 录' }}
        </el-button>
      </el-form>

      <!-- V5: 微信扫码登录 -->
      <div v-else class="wechat-tab">
        <WechatScanLogin @login-success="onWechatLogin" />
        <el-divider><span style="font-size: 12px; color: #94a3b8;">其他方式</span></el-divider>
        <p class="wechat-tip">
          没有微信? 用 <a href="javascript:;" @click="mode = 'login'">账号密码</a> 登录
        </p>
      </div>

      <div v-if="mode !== 'wechat'" class="login-tips">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <span style="font-size: 12px;">默认账号 admin / admin@123（仅初始化数据中）</span>
          </template>
        </el-alert>
        <el-alert type="warning" :closable="false" show-icon style="margin-top:8px">
          <template #title>
            <span style="font-size: 12px;">🔑 超级管理员 <code>adminLiugl / Liugl@2026</code> （平台所有者）</span>
          </template>
        </el-alert>
        <p style="margin: 12px 0 4px; color: var(--minimax-text-secondary); font-size: 12px;">
          平台能力路线：
        </p>
        <ul style="margin: 0; padding-left: 20px; color: var(--minimax-text-secondary); font-size: 12px; line-height: 1.8;">
          <li>✅ Day 2 - 真实账号体系 (JWT + RBAC)</li>
          <li>✅ Day 5 - 流式对话 + 实时打字机</li>
          <li>✅ Day 6-7 - 短/长期记忆</li>
          <li>✅ Day 8 - 知识库 RAG</li>
          <li>✅ V2 - Agent / 知识图谱 / 协作 / 插件</li>
          <li>✅ V3 - 多租户 + 移动端 H5 + OpenAI 兼容</li>
          <li>✅ V4 - 16 大模型对决 + 文生图/音/视频</li>
          <li>✅ V5 - <strong>微信扫码登录</strong></li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Avatar } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'
import WechatScanLogin from '@/components/WechatScanLogin.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const mode = ref('login')

const form = reactive({
  username: 'admin',
  password: 'admin@123',
  nickname: '',
  email: ''
})

const rules = {
  username: [{ required: true, min: 3, max: 64, message: '用户名长度 3-64', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 64, message: '密码长度 6-64', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

watch(mode, (v) => {
  if (v === 'register') {
    form.username = ''
    form.password = ''
  } else if (v === 'login') {
    form.username = 'admin'
    form.password = 'admin@123'
  }
})

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    } else {
      await authApi.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username,
        email: form.email || null
      })
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('注册成功，已自动登录')
    }
    const redirect = route.query.redirect || (isMobile() ? '/m/chat' : '/')
    router.push(redirect)
  } catch (e) {
    // http.js 已经弹过错误提示
  } finally {
    loading.value = false
  }
}

/**
 * V5: 微信扫码登录成功回调
 */
async function onWechatLogin({ accessToken, refreshToken, userId }) {
  try {
    // 把 token 存进 user store, 模拟正常登录
    userStore.accessToken = accessToken
    userStore.refreshToken = refreshToken
    // 拉一次 /auth/me 拿完整 user info
    await userStore.fetchProfile()
    ElMessage.success(`微信扫码登录成功! 欢迎 ${userStore.nickname || userStore.username}`)
    const redirect = route.query.redirect || (isMobile() ? '/m/chat' : '/')
    router.push(redirect)
  } catch (e) {
    ElMessage.error('登录后初始化失败: ' + e.message)
  }
}

function isMobile() {
  return /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; }
.login-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #1e293b 0%, #0f172a 50%, #1e1b4b 100%); z-index: 0; }
.login-box {
  position: relative; z-index: 1;
  width: 100%; max-width: 440px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(20px);
  border-radius: 18px; padding: 36px 32px;
  box-shadow: 0 30px 80px rgba(0,0,0,0.4);
}
.login-header { text-align: center; margin-bottom: 24px; }
.gradient-text { background: linear-gradient(90deg, #6366f1, #a855f7, #ec4899); -webkit-background-clip: text; background-clip: text; color: transparent; }
.login-tabs { margin-bottom: 18px; }

/* V5: 微信扫码 tab */
.wechat-tab { padding: 12px 0; }
.wechat-tip { text-align: center; color: #64748b; font-size: 13px; margin: 8px 0 0; }
.wechat-tip a { color: #6366f1; text-decoration: none; }
.wechat-tip a:hover { text-decoration: underline; }

.login-tips { margin-top: 18px; }
:deep(.el-tabs__item) { font-size: 14px; }
:deep(.el-form-item) { margin-bottom: 16px; }
</style>
