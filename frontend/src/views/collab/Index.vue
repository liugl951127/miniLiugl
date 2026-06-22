<template>
  <div class="collab-container">
    <div class="collab-header">
      <h1>👥 {{ t('collab.title') }} <span class="badge">V2.0</span></h1>
      <p class="sub">{{ t('collab.subtitle') }}</p>
    </div>

    <el-card v-if="!joined">
      <el-button type="primary" @click="createRoom">🚀 {{ t('collab.createSession') }}</el-button>
      <el-divider>{{ t('collab.orJoin') }}</el-divider>
      <el-input v-model="joinSessionId" :placeholder="t('collab.enterSessionId')" style="width:300px">
        <template #append><el-button @click="joinRoom">{{ t('collab.join') }}</el-button></template>
      </el-input>
    </el-card>

    <el-row v-else :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>💬 {{ t('collab.room') }} {{ sessionId }}</span>
              <div>
                <el-tag type="success">{{ status }}</el-tag>
                <el-tag type="info" style="margin-left:8px">👥 {{ users.length }} {{ t('collab.users') }}</el-tag>
                <el-button size="small" type="danger" text @click="leave" style="margin-left:8px">{{ t('collab.leave') }}</el-button>
              </div>
            </div>
          </template>

          <div class="messages" ref="msgBox">
            <div v-for="(m, i) in messages" :key="i" class="message">
              <strong>U{{ m.userId }}:</strong> {{ m.content }}
              <span class="ts">{{ formatTime(m.ts) }}</span>
            </div>
          </div>

          <div class="typing" v-if="typingUsers.length">
            <em>{{ typingUsers.join(', ') }} {{ t('collab.typing') }}...</em>
          </div>

          <el-input v-model="input" @keydown="onTyping" @keyup.enter="send"
                    :placeholder="t('collab.enterToSend')" style="margin-top:12px">
            <template #append><el-button type="primary" @click="send">{{ t('collab.send') }}</el-button></template>
          </el-input>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header><span>{{ t('collab.online') }}</span></template>
          <div v-for="u in users" :key="u" class="user">
            <el-avatar :size="32" style="background:#409eff">U{{ u }}</el-avatar>
            <span style="margin-left:8px">{{ t('collab.user') }} {{ u }}</span>
          </div>
          <el-empty v-if="!users.length" :description="t('collab.noOneOnline')" :image-size="60" />
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>{{ t('collab.connectionInfo') }}</span></template>
          <p>{{ t('collab.session') }}: <code>{{ sessionId }}</code></p>
          <p>{{ t('collab.status') }}: <el-tag size="small">{{ status }}</el-tag></p>
          <p>{{ t('collab.messageCount') }}: {{ messages.length }}</p>
          <el-button size="small" @click="ping">{{ t('collab.ping') }}</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { t } from '@/i18n'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
// 同源 ws: 走 nginx 80 → gateway 8080 (V1 一体化部署)
const WS_BASE = (import.meta.env.VITE_WS_BASE || `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}`)
const token = localStorage.getItem('access_token') || ''
const userId = localStorage.getItem('user_id') || '1'

const joined = ref(false)
const sessionId = ref('')
const joinSessionId = ref('')
const status = ref('disconnected')
const users = ref<number[]>([])
const messages = ref<any[]>([])
const input = ref('')
const typingUsers = ref<number[]>([])
let ws: WebSocket | null = null
let typingTimeout: any = null

function auth() { return { headers: { Authorization: `Bearer ${token}` } } }

async function createRoom() {
  try {
    const { data } = await axios.post(`${API}/api/v1/agent/collab/sessions`,
      { ownerId: userId, title: '新协作', maxUsers: 10 }, auth())
    const collabId = data.data
    // 拿到 collabSession 的 sessionId (字符串) 需另查, 这里用 ID 替代
    sessionId.value = String(collabId)
    connectWs(sessionId.value)
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function joinRoom() {
  if (!joinSessionId.value) { ElMessage.warning('请输入 sessionId'); return }
  try {
    await axios.post(`${API}/api/v1/agent/collab/${joinSessionId.value}/join?userId=${userId}`, {}, auth())
    sessionId.value = joinSessionId.value
    connectWs(sessionId.value)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

function connectWs(sid: string) {
  // V1.8: gateway ws 路由是 /api/v1/ws/** (原 /ws/collab 会被 gateway StripPrefix=2 错误拑截)
  const url = `${WS_BASE}/api/v1/ws/collab/${sid}?userId=${userId}`
  ws = new WebSocket(url)
  status.value = 'connecting'
  ws.onopen = () => {
    status.value = 'connected'
    joined.value = true
    ElMessage.success(t('collab.joinedRoom'))
  }
  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data)
    handleMsg(msg)
  }
  ws.onclose = () => {
    status.value = 'disconnected'
  }
  ws.onerror = () => { status.value = 'error' }
}

function handleMsg(msg: any) {
  switch (msg.type) {
    case 'join': case 'leave':
      users.value = msg.users || []
      break
    case 'msg':
      messages.value.push(msg)
      nextTick(() => scrollToBottom())
      break
    case 'typing':
      if (!typingUsers.value.includes(msg.userId)) typingUsers.value.push(msg.userId)
      setTimeout(() => {
        typingUsers.value = typingUsers.value.filter(u => u !== msg.userId)
      }, 3000)
      break
  }
}

function send() {
  if (!input.value.trim() || !ws) return
  ws.send(JSON.stringify({ type: 'msg', content: input.value }))
  input.value = ''
}

function onTyping() {
  if (!ws) return
  ws.send(JSON.stringify({ type: 'typing' }))
  if (typingTimeout) clearTimeout(typingTimeout)
}

function ping() {
  if (ws) ws.send(JSON.stringify({ type: 'ping' }))
}

function leave() {
  if (ws) ws.close()
  joined.value = false
  status.value = 'disconnected'
  users.value = []
  messages.value = []
}

function scrollToBottom() {
  const box: any = document.querySelector('.messages')
  if (box) box.scrollTop = box.scrollHeight
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString()
}
</script>

<style scoped>
.collab-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.collab-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.messages {
  height: 400px; overflow-y: auto; padding: 10px; background: #fafbfc;
  border-radius: 4px; border: 1px solid #ebeef5;
}
.message { padding: 6px 0; border-bottom: 1px dashed #ebeef5; }
.message .ts { color: #999; font-size: 11px; margin-left: 8px; }
.typing { color: #999; font-size: 12px; margin-top: 8px; font-style: italic; }
.user { display: flex; align-items: center; padding: 8px 0; }
code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
</style>
