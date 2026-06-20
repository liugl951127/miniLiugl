<!--
  微信扫码登录组件 (V5)
  - 调后端生成二维码 + 拿到 ticket
  - 显示二维码 (用 qrcode 库渲染 qrcodeUrl)
  - 2 秒轮询状态
  - status=confirmed 自动回调 onLoginSuccess(accessToken, refreshToken)
  - mock 模式: 显示"模拟扫码"按钮直接跳确认
-->
<template>
  <div class="wechat-scan">
    <div class="qrcode-box" v-if="qrcode">
      <img v-if="qrcodeImage" :src="qrcodeImage" class="qr-img" alt="QR Code" />
      <div v-else class="qr-fallback">
        <el-icon :size="32" class="is-loading"><Loading /></el-icon>
        <p>生成二维码...</p>
      </div>
      <!-- mock 模式模拟按钮 -->
      <div v-if="qrcode.mock" class="mock-actions">
        <el-button type="warning" size="small" @click="doMockScan">
          🧪 模拟扫码 (沙箱演示)
        </el-button>
        <p class="mock-tip">真实部署时, 这里会显示微信二维码</p>
      </div>
      <!-- 过期覆盖 -->
      <div v-if="status === 'expired'" class="expired-overlay">
        <p>二维码已过期</p>
        <el-button type="primary" size="small" @click="refresh">刷新</el-button>
      </div>
    </div>
    <p class="tip">用 <strong>微信</strong> 扫一扫上方二维码</p>
    <p class="status-text">
      <span v-if="status === 'pending'" class="dot pending"></span>
      <span v-else-if="status === 'scanned'" class="dot scanned"></span>
      <span v-else-if="status === 'confirmed'" class="dot confirmed"></span>
      <span v-else-if="status === 'expired'" class="dot expired"></span>
      <span class="text">{{ statusText }}</span>
    </p>
    <p v-if="qrcode && qrcode.expiresAt" class="expire-tip">
      过期: {{ formatExpire(qrcode.expiresAt) }}
    </p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { wechatApi } from '@/api/wechat'
import dayjs from 'dayjs'

const props = defineProps({
  autoStart: { type: Boolean, default: true },
})
const emit = defineEmits(['login-success', 'cancel'])

const qrcode = ref(null)
const qrcodeImage = ref('')
const status = ref('idle')  // idle / pending / scanned / confirmed / expired / error
let pollTimer = null

const statusText = computed(() => ({
  idle: '准备中...',
  pending: '等待扫码...',
  scanned: '已扫码, 请在手机上点击确认',
  confirmed: '✓ 登录成功, 跳转中...',
  expired: '已过期, 请刷新',
  error: '出错了',
})[status.value] || status.value)

async function start() {
  status.value = 'pending'
  try {
    const r = await wechatApi.createQrCode()
    if (!r || !r.data) {
      ElMessage.error('生成二维码失败')
      status.value = 'error'
      return
    }
    qrcode.value = r.data
    // 用 qrcode 库生成 data URL
    try {
      qrcodeImage.value = await QRCode.toDataURL(r.data.qrcodeUrl, {
        width: 220,
        margin: 1,
        color: { dark: '#1e293b', light: '#ffffff' }
      })
    } catch (e) {
      console.warn('QRCode render failed:', e)
      qrcodeImage.value = ''
    }
    startPoll()
  } catch (e) {
    ElMessage.error('生成二维码失败: ' + e.message)
    status.value = 'error'
  }
}

function startPoll() {
  stopPoll()
  if (!qrcode.value) return
  pollTimer = setInterval(() => poll(), 2000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function poll() {
  if (!qrcode.value) return
  try {
    const r = await wechatApi.getStatus(qrcode.value.ticket)
    if (!r || !r.data) return
    const s = r.data.status
    if (s === 'confirmed' && status.value !== 'confirmed') {
      status.value = 'confirmed'
      stopPoll()
      ElMessage.success('登录成功!')
      emit('login-success', {
        accessToken: r.data.accessToken,
        refreshToken: r.data.refreshToken,
        userId: r.data.userId,
      })
    } else if (s === 'scanned') {
      status.value = 'scanned'
    } else if (s === 'expired') {
      status.value = 'expired'
      stopPoll()
    } else if (s !== status.value) {
      status.value = s
    }
  } catch (e) {
    console.warn('poll failed:', e)
  }
}

async function doMockScan() {
  if (!qrcode.value) return
  try {
    await wechatApi.mockScan(qrcode.value.ticket)
    ElMessage.info('已触发 mock 扫码, 等前端轮询...')
  } catch (e) {
    ElMessage.error('mock 失败: ' + e.message)
  }
}

function refresh() {
  start()
}

function formatExpire(iso) {
  return dayjs(iso).format('HH:mm:ss')
}

onMounted(() => {
  if (props.autoStart) start()
})

onUnmounted(() => {
  stopPoll()
})

watch(() => props.autoStart, (v) => { if (v) start() })
</script>

<style scoped>
.wechat-scan { display: flex; flex-direction: column; align-items: center; padding: 8px; }
.qrcode-box {
  position: relative;
  width: 220px; height: 220px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}
.qr-img { width: 100%; height: 100%; }
.qr-fallback { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #94a3b8; }
.mock-actions { position: absolute; inset: 0; background: rgba(0,0,0,0.7); display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px; }
.mock-tip { color: #fbbf24; font-size: 11px; margin-top: 8px; }
.expired-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.75); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px; }
.expired-overlay p { margin-bottom: 8px; }

.tip { margin-top: 12px; color: #64748b; font-size: 13px; }
.status-text { display: flex; align-items: center; gap: 6px; margin-top: 8px; font-size: 13px; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot.pending { background: #fbbf24; animation: pulse 1.5s infinite; }
.dot.scanned { background: #10b981; animation: pulse 1s infinite; }
.dot.confirmed { background: #10b981; }
.dot.expired { background: #ef4444; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.expire-tip { margin-top: 4px; color: #94a3b8; font-size: 11px; }
</style>
