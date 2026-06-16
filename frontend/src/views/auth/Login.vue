<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-box">
      <div class="login-header">
        <h1 class="gradient-text" style="margin: 0 0 8px; font-size: 32px;">MiniMax</h1>
        <p style="color: var(--minimax-text-secondary); margin: 0;">大模型应用平台</p>
      </div>

      <el-tabs v-model="mode" stretch class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onSubmit">
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

      <div class="login-tips">
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
          <li>Day 3 - 多会话历史持久化</li>
          <li>Day 5 - 流式对话 + 实时打字机</li>
          <li>Day 6-7 - 短/长期记忆</li>
          <li>Day 8 - 知识库 RAG</li>
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
  } else {
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
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // http.js 已经弹过错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative; height: 100vh; width: 100vw;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.login-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #1f2a44 0%, #0b1220 50%, #2d1b4e 100%);
  &::before, &::after {
    content: ''; position: absolute; border-radius: 50%;
    background: radial-gradient(circle, rgba(91,141,239,0.3), transparent 70%);
  }
  &::before { width: 500px; height: 500px; top: -100px; right: -100px; }
  &::after { width: 400px; height: 400px; bottom: -100px; left: -100px;
    background: radial-gradient(circle, rgba(182,109,255,0.3), transparent 70%); }
}
.login-box {
  position: relative; z-index: 1;
  width: 440px; padding: 32px 40px 24px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  backdrop-filter: blur(10px);
}
.login-header { text-align: center; margin-bottom: 16px; }
.login-tabs { margin-bottom: 16px; }
code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
.login-tips { margin-top: 12px; }
</style>
