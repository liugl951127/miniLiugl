<!--
  我的微信 (V5)
  - 显示当前用户是否绑定微信
  - 已绑定: 显示微信信息 + 解绑按钮
  - 未绑定: 显示"去扫码登录"按钮
-->
<template>
  <div class="my-wechat">
    <div v-if="loading" class="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      加载中...
    </div>
    <div v-else-if="!bound" class="unbound">
      <el-icon :size="48" color="#10b981"><ChatDotRound /></el-icon>
      <h3>未绑定微信</h3>
      <p>绑定微信后可用微信扫码登录, 更便捷</p>
      <el-button type="primary" @click="goBind">去扫码绑定</el-button>
    </div>
    <div v-else class="bound">
      <el-avatar :size="64" :src="binding.avatar || ''">
        <el-icon><User /></el-icon>
      </el-avatar>
      <div class="info">
        <h3>{{ binding.nickname }}</h3>
        <p class="openid">OpenID: <code>{{ binding.openid }}</code></p>
        <p class="time">绑定于 {{ formatTime(binding.boundAt) }}</p>
        <p class="bindings">共 {{ (binding.bindings || []).length }} 个应用绑定</p>
      </div>
      <div class="actions">
        <el-button type="primary" plain @click="goCrossApp">跨应用管理</el-button>
        <el-button type="danger" plain @click="doUnbind">解绑</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, User, ChatDotRound } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const bound = ref(false)
const binding = ref({})

async function load() {
  loading.value = true
  try {
    const r = await http.get('/auth/wechat/binding/me')
    if (r && r.data) {
      bound.value = !!r.data.bound
      binding.value = r.data
    }
  } catch (e) {
    console.warn('加载绑定信息失败:', e.message)
  } finally {
    loading.value = false
  }
}

async function doUnbind() {
  try {
    await ElMessageBox.confirm(
      '解绑后无法用微信扫码登录, 确定吗?',
      '确认解绑',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await http.delete('/auth/wechat/binding/me')
    ElMessage.success('已解绑')
    await load()
  } catch (e) {
    ElMessage.error('解绑失败: ' + e.message)
  }
}

function goBind() {
  ElMessage.info('请用本账号先登录, 然后通过"微信扫码登录"页扫码, 会自动绑定当前账号')
  router.push('/login')
}

function goCrossApp() {
  router.push('/profile/wechat/cross')
}

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

onMounted(load)
</script>

<style scoped>
.my-wechat { padding: 16px 0; }
.loading { text-align: center; color: #94a3b8; padding: 40px 0; }
.unbound { text-align: center; padding: 40px 0; color: #64748b; }
.unbound h3 { margin: 12px 0 8px; }
.unbound p { margin-bottom: 16px; }

.bound { display: flex; align-items: center; gap: 20px; padding: 20px;
  background: linear-gradient(135deg, #f0fdf4, #fff); border-radius: 12px;
  border: 1px solid #d1fae5; }
.actions { display: flex; flex-direction: column; gap: 8px; }
.info { flex: 1; }
.info h3 { margin: 0 0 8px; font-size: 18px; }
.openid code { font-size: 11px; background: #fff; padding: 2px 6px; border-radius: 3px; }
.time, .bindings { color: #64748b; font-size: 12px; margin: 4px 0; }
</style>
