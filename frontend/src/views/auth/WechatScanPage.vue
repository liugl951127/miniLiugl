<!--
  微信扫码演示页 (V5.24 完善)
  - 已登录用户: 扫码绑定当前账号
  - 未登录用户: 扫码登录 → 自动跳转
  - 增加: 扫码步骤说明 + 失败重试 + 已绑定状态展示
-->
<template>
  <div class="wechat-scan-page">
    <div class="container">
      <h1>📱 {{ t('user.wechatScan') }}</h1>
      <p class="hint">
        {{ t('user.scanHint') }}
        <el-tag v-if="userStore.isLogin" type="success" size="small" style="margin-left: 8px">已登录: {{ userStore.userInfo?.username }}</el-tag>
        <el-tag v-else type="warning" size="small" style="margin-left: 8px">未登录</el-tag>
      </p>

      <!-- 扫码登录组件 -->
      <WechatScanLogin @login-success="onLogin" @bind-success="onBind" @scan-error="onScanError" />

      <!-- 操作步骤 -->
      <el-divider><span style="color: #909399; font-size: 13px">扫码步骤</span></el-divider>
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="打开微信" description="扫一扫" />
        <el-step title="扫描二维码" description="等待识别" />
        <el-step title="确认授权" description="点击登录/绑定" />
        <el-step title="完成" :description="userStore.isLogin ? '绑定成功' : '登录成功'" />
      </el-steps>

      <!-- 帮助提示 -->
      <el-collapse style="margin-top: 24px">
        <el-collapse-item title="遇到问题?" name="help">
          <ul style="padding-left: 20px; color: #606266; line-height: 1.8; font-size: 13px">
            <li>二维码过期? 点击下方"刷新二维码"重新生成</li>
            <li>扫码后没反应? 检查微信版本 (需 8.0+)</li>
            <li>无法绑定? 当前账号可能已绑定其他微信</li>
            <li>仍有问题? 联系管理员 adminLiugl@minimax.com</li>
          </ul>
          <div style="margin-top: 12px; text-align: center">
            <el-button size="small" @click="goLogin"><el-icon><ArrowLeft /></el-icon> 返回密码登录</el-button>
            <el-button size="small" type="primary" @click="goHome"><el-icon><HomeFilled /></el-icon> 回首页</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 已绑定信息 -->
      <el-card v-if="binding" shadow="hover" class="binding-card">
        <template #header>
          <span>📋 当前绑定状态</span>
        </template>
        <div class="binding-row">
          <img v-if="binding.headimgurl" :src="binding.headimgurl" class="avatar" />
          <el-icon v-else :size="48" color="#67c23a"><User /></el-icon>
          <div class="binding-meta">
            <div class="nickname">{{ binding.nickname || '微信用户' }}</div>
            <div class="unionid"><code>{{ binding.unionid }}</code></div>
            <el-tag size="small" type="success">{{ binding.boundAt }} 绑定</el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import WechatScanLogin from '@/components/WechatScanLogin.vue'
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { ArrowLeft, HomeFilled, User } from '@element-plus/icons-vue'
import { t } from '@/i18n'
import { getMyBinding } from '@/api/wechat'

const router = useRouter()
const userStore = useUserStore()
const currentStep = ref(1)
const binding = ref(null)

async function loadBinding() {
  if (!userStore.isLogin) return
  try {
    const res = await getMyBinding()
    const data = res.data?.data || res.data
    if (data?.unionid) {
      binding.value = data
      currentStep.value = 4
    }
  } catch (e) {
    // 静默
  }
}

function onLogin(payload) {
  // V1.8: user store 是 setup-style Pinia, 字段是 ref, 必须用 .value
  // 但 store 本身代理了 ref 的读写, 正确写法: userStore.$patch({ ... })
  userStore.$patch({
    accessToken: payload.accessToken || '',
    refreshToken: payload.refreshToken || '',
    profile: payload.user || null
  })
  currentStep.value = 4
  ElMessage.success('微信登录成功, 跳转中...')
  setTimeout(() => router.push('/chat'), 1000)
}

function onBind(payload) {
  currentStep.value = 4
  ElMessage.success('绑定成功')
  loadBinding()
  setTimeout(() => router.push('/profile/wechat'), 1500)
}

function onScanError(err) {
  currentStep.value = 2
  ElMessage.warning('扫码失败: ' + err + ', 请重试')
}

function goLogin() {
  router.push('/login')
}

function goHome() {
  router.push('/')
}

// 首次加载时如果已登录, 加载绑定信息
loadBinding()
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
  max-width: 540px; width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}
h1 { margin: 0 0 12px; font-size: 28px; text-align: center; }
.hint { color: #64748b; margin-bottom: 24px; text-align: center; }
.binding-card { margin-top: 24px; }
.binding-row { display: flex; align-items: center; gap: 16px; }
.avatar { width: 48px; height: 48px; border-radius: 50%; }
.binding-meta { flex: 1; }
.nickname { font-weight: 600; color: #303133; font-size: 15px; }
.unionid { font-size: 12px; color: #909399; margin: 4px 0; }
code { font-family: 'JetBrains Mono', monospace; color: #d63384; padding: 2px 6px; background: #f5f7fa; border-radius: 3px; word-break: break-all; }
</style>