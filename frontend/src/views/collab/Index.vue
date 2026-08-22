<!-- @file collab/Index.vue - 协作空间 V6.8.13 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>👥 协作空间</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadRooms">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="showCreate = true">
          <el-icon><Plus /></el-icon>创建房间
        </el-button>
      </div>
    </div>

    <!-- 统计 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#409eff">{{ rooms.length }}</div>
        <div style="font-size:12px;color:#909399">总房间数</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#67c23a">{{ activeRooms }}</div>
        <div style="font-size:12px;color:#909399">活跃房间</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ totalMembers }}</div>
        <div style="font-size:12px;color:#909399">在线成员</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#909399">{{ todayMsgs }}</div>
        <div style="font-size:12px;color:#909399">今日消息</div>
      </el-card></el-col>
    </el-row>

    <el-row :gutter="12">
      <!-- 房间列表 -->
      <el-col :span="14">
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="keyword" size="small" placeholder="搜索房间…" style="width:180px" clearable @change="loadRooms">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="statusFilter" size="small" style="width:100px" @change="loadRooms">
            <el-option label="全部" value="" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="空闲" value="IDLE" />
          </el-select>
        </div>

        <el-table :data="rooms" v-loading="loading" stripe :empty-text="roomsEmptyText">
          <template #empty>
            <el-empty :description="roomsEmptyText" :image-size="80">
              <el-button type="primary" size="small" @click="showCreate = true">
                <el-icon><Plus /></el-icon>创建第一个房间
              </el-button>
            </el-empty>
          </template>
          <el-table-column prop="name" label="房间名称">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:8px">
                <span style="font-weight:600">{{ row.name }}</span>
                <el-tag v-if="row.type === 'PRIVATE'" size="small">私密</el-tag>
              </div>
              <div style="font-size:11px;color:#909399;margin-top:2px">{{ row.topic || '暂无主题' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="成员" width="80" align="center">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:4px;justify-content:center">
                <el-avatar v-for="u in (row.members || []).slice(0,3)" :key="u.id" :size="20">{{ u.name?.charAt(0) || 'U' }}</el-avatar>
                <span v-if="(row.members?.length || 0) > 3" style="font-size:11px;color:#909399">+{{ row.members.length - 3 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '活跃' : '空闲' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="messages" label="消息" width="70" align="center">
            <template #default="{ row }">{{ row.messages || 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="joinRoom(row)">加入</el-button>
              <el-button size="small" link @click="inviteMember(row)">邀请</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>

      <!-- 右侧: 热门房间 + 成员在线 -->
      <el-col :span="10">
        <el-card title="🔥 热门房间" body-style="padding:0" style="margin-bottom:12px">
          <template #header><span>🔥 热门房间</span></template>
          <div v-for="r in hotRooms" :key="r.id" class="hot-room" @click="joinRoom(r)">
            <div style="flex:1">
              <div style="font-weight:600;font-size:13px">{{ r.name }}</div>
              <div style="font-size:11px;color:#909399">{{ r.members?.length || 0 }} 成员 · {{ r.messages || 0 }} 条消息</div>
            </div>
            <el-icon color="#f56c6c"><HotWater /></el-icon>
          </div>
          <div v-if="!hotRooms.length" style="padding:20px;text-align:center;color:#909399">暂无热门房间</div>
        </el-card>

        <el-card title="🟢 在线成员" body-style="padding:0">
          <template #header><span>🟢 在线成员</span></template>
          <div v-for="m in onlineMembers" :key="m.id" class="online-member">
            <el-avatar :size="28" style="flex-shrink:0">{{ m.name?.charAt(0) || 'U' }}</el-avatar>
            <div style="flex:1;margin-left:8px">
              <div style="font-size:13px;font-weight:600">{{ m.name }}</div>
              <div style="font-size:11px;color:#909399">{{ m.room || '未在房间' }}</div>
            </div>
            <div class="online-dot"></div>
          </div>
          <div v-if="!onlineMembers.length" style="padding:20px;text-align:center;color:#909399">暂无在线成员</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建房间弹窗 -->
    <el-dialog v-model="showCreate" title="创建协作房间" width="480px" @closed="resetCreateForm">
      <el-form :model="newRoom" :rules="createRoomRules" ref="createFormRef" label-width="80px">
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="newRoom.name" placeholder="给房间起个名字" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="主题" prop="topic">
          <el-input v-model="newRoom.topic" placeholder="描述房间主题（可选）" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="newRoom.type">
            <el-radio value="PUBLIC">公开</el-radio>
            <el-radio value="PRIVATE">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="newRoom.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createRoom">创建</el-button>
      </template>
    </el-dialog>

    <!-- 房间聊天抽屉 (V6.8.13: 修复加入跳转404) -->
    <el-drawer v-model="roomDrawer" :title="currentRoom?.name || '房间'" direction="rtl" size="420px"
      :before-close="leaveRoom">
      <template #header>
        <div style="display:flex;align-items:center;gap:8px">
          <span>{{ currentRoom?.name }}</span>
          <el-tag v-if="currentRoom?.type === 'PRIVATE'" size="small">私密</el-tag>
          <el-tag size="small" :type="roomJoined ? 'success' : 'info'">
            {{ roomJoined ? '🟢 已加入' : '⚪ 未加入' }}
          </el-tag>
        </div>
      </template>

      <!-- 房间信息 -->
      <div v-if="currentRoom" class="room-info">
        <div class="room-topic">{{ currentRoom.topic || currentRoom.description || '暂无主题' }}</div>
        <div class="room-members">
          <el-avatar v-for="u in participants.slice(0,6)" :key="u.id || u.userId" :size="28" style="margin-right:4px" :title="u.nickname || u.username">
            {{ (u.nickname || u.username || 'U')?.charAt(0) }}
          </el-avatar>
          <span v-if="participants.length > 6" style="font-size:12px;color:#909399;margin-left:4px">+{{ participants.length - 6 }}</span>
        </div>
      </div>

      <!-- 消息列表 -->
      <el-scrollbar ref="msgScrollRef" class="room-messages" v-loading="historyLoading">
        <div v-if="!roomMessages.length && !wsConnected && !historyLoading" style="text-align:center;padding:40px 0;color:#c0c4cc;font-size:13px">
          正在连接协作房间…
        </div>
        <div v-else-if="!roomMessages.length && !historyLoading" style="text-align:center;padding:40px 0;color:#c0c4cc;font-size:13px">
          暂无消息<br/>发送消息或问 AI 开始协作
        </div>
        <template v-for="(msg, i) in roomMessages" :key="msg.id">
          <!-- P1-6: 不同用户消息之间的分隔线 -->
          <div v-if="i > 0 && !isSameGroup(msg, roomMessages[i-1])" class="msg-divider"></div>
          <div class="room-msg" :class="msg.role">
            <el-avatar :size="24" class="msg-avatar">
              {{ msg.role === 'ai' ? '🤖' : (msg.user || 'U').charAt(0) }}
            </el-avatar>
            <div class="msg-body">
              <div class="msg-meta">
                {{ msg.role === 'ai' ? '🤖 AI 助手' : (msg.user || '未知') }}
                <!-- P1-6: 仅显示第一条消息的时间戳 -->
                <template v-if="!i || isSameGroup(roomMessages[i-1], msg)">
                  · {{ formatTime(msg.createdAt) }}
                </template>
                <el-tag v-if="msg.role === 'ai'" size="small" type="success" style="margin-left:4px">AI</el-tag>
              </div>
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </div>
        </template>
      </el-scrollbar>

      <!-- 消息输入 -->
      <div class="room-input">
        <el-input v-model="roomInput" placeholder="输入消息，按 Enter 发送" size="default"
          :disabled="!roomJoined"
          @keydown.enter.prevent="sendRoomMessage" />
        <el-button type="primary" :disabled="!roomJoined || !roomInput.trim()" :loading="sendingMsg"
          @click="sendRoomMessage" style="margin-left:8px">发送</el-button>
      </div>

      <!-- AI 触发区 (V6.8.13) -->
      <div class="room-ai-area">
        <el-input v-model="aiInput" placeholder="🤖 问 AI，按 Enter 触发" size="default"
          :disabled="!roomJoined || aiLoading"
          @keydown.enter.prevent="sendRoomAi" />
        <el-button type="success" :disabled="!roomJoined || !aiInput.trim() || aiLoading"
          :loading="aiLoading" @click="sendRoomAi" style="margin-left:8px">
          {{ aiLoading ? '思考中…' : '问 AI' }}
        </el-button>
      </div>

      <template #footer>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;gap:8px;align-items:center">
            <el-tag v-if="wsConnected" size="small" type="success">🟢 在线</el-tag>
            <el-tag v-else-if="roomJoined" size="small" type="warning">⚡ 降级轮询</el-tag>
            <el-tag v-else-if="wsReconnectAttempt > 0" size="small" type="danger">⚠️ 连接断开 ({{ wsReconnectAttempt }}/{{ MAX_RECONNECT_ATTEMPTS }})</el-tag>
            <span v-if="participants.length" style="font-size:12px;color:#909399">
              {{ participants.length }} 人在线
            </span>
          </div>
          <div style="display:flex;gap:8px">
            <el-button v-if="!roomJoined" type="primary" @click="doJoinRoom">加入房间</el-button>
            <el-button v-else type="danger" plain @click="leaveRoom">离开</el-button>
            <el-button v-if="wsReconnectAttempt > 0 && !wsConnected" size="small" type="primary" @click="manualReconnect">
              <el-icon><Refresh /></el-icon>重新连接
            </el-button>
            <el-button @click="leaveRoom">关闭</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPublicRooms, createRoom as createRoomApi, getMessages, buildCollabWsUrl } from '@/api/collab'
import { getRoom } from '@/api/collab'
import http from '@/api/http'
import { useUserStore } from '@/store/user'
import { Plus, Refresh, Search, HotWater } from '@element-plus/icons-vue'

const rooms = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const showCreate = ref(false)
const creating = ref(false)
const newRoom = ref({ name: '', topic: '', type: 'PUBLIC', description: '' })
const createFormRef = ref(null)

const createRoomRules = {
  name: [
    { required: true, message: '请填写房间名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度需在 2-50 个字符之间', trigger: 'blur' },
  ],
  topic: [
    { max: 100, message: '主题不能超过 100 个字符', trigger: 'blur' },
  ],
  description: [
    { max: 200, message: '描述不能超过 200 个字符', trigger: 'blur' },
  ],
}

function resetCreateForm() {
  newRoom.value = { name: '', topic: '', type: 'PUBLIC', description: '' }
  createFormRef.value?.clearValidate()
}

// V6.8.13: 房间聊天 WebSocket 状态
const userStore = useUserStore()
const roomDrawer = ref(false)
const currentRoom = ref(null)
const roomJoined = ref(false)       // WebSocket 连接成功
const roomMessages = ref([])
const roomInput = ref('')
const aiInput = ref('')
const sendingMsg = ref(false)
const aiLoading = ref(false)
const aiPartialContent = ref('')    // AI_CHUNK 拼接中
const msgScrollRef = ref(null)
const participants = ref([])
const historyLoading = ref(false)

// WebSocket
let ws = null
let wsConnected = ref(false)
let wsReconnectTimer = null
let wsHeartbeatTimer = null
const wsReconnectAttempt = ref(0)
const MAX_RECONNECT_ATTEMPTS = 5

const activeRooms = computed(() => rooms.value.filter(r => r.status === 'ACTIVE').length)
const totalMembers = computed(() => rooms.value.reduce((s, r) => s + (r.members?.length || 0), 0))
const todayMsgs = computed(() => rooms.value.reduce((s, r) => s + (r.todayMessages || 0), 0))
const hotRooms = computed(() => [...rooms.value].sort((a, b) => (b.messages || 0) - (a.messages || 0)).slice(0, 5))
const onlineMembers = computed(() => {
  const members = []
  rooms.value.forEach(r => {
    if (r.status === 'ACTIVE') {
      r.members?.forEach?.(m => members.push({ ...m, room: r.name }))
    }
  })
  return members.slice(0, 10)
})

// ============ WebSocket 核心 ============
function connectWebSocket(room) {
  const user = userStore.profile
  if (!user?.id) { ElMessage.warning('请先登录'); return }
  const wsUrl = buildCollabWsUrl(room.roomId || room.id, user)
  // 清理旧连接
  if (ws) { try { ws.close() } catch {} ws = null }
  ws = new WebSocket(wsUrl)
  ws.onopen = () => {
    const isReconnect = wsReconnectAttempt.value > 0
    wsConnected.value = true
    roomJoined.value = true
    wsReconnectAttempt.value = 0
    clearTimeout(wsReconnectTimer)
    startHeartbeat()
    if (isReconnect) {
      ElMessage.success('协作连接已恢复')
    }
  }
  ws.onmessage = (evt) => {
    try {
      const msg = JSON.parse(evt.data)
      handleWsMessage(msg)
    } catch {}
  }
  ws.onerror = () => {
    wsConnected.value = false
  }
  ws.onclose = () => {
    wsConnected.value = false
    roomJoined.value = false
    stopHeartbeat()
    // 非主动关闭时尝试重连
    if (roomDrawer.value && currentRoom.value) {
      wsReconnectAttempt.value++
      if (wsReconnectAttempt.value > MAX_RECONNECT_ATTEMPTS) {
        ElMessage.error({
          message: '协作连接已断开，请检查网络后手动重试',
          duration: 5000,
          showClose: true
        })
        return
      }
      // 首次断开提示
      if (wsReconnectAttempt.value === 1) {
        ElMessage.warning({
          message: '连接已断开，正在尝试重连…',
          duration: 3000
        })
      }
      wsReconnectTimer = setTimeout(() => {
        if (roomDrawer.value && currentRoom.value) connectWebSocket(currentRoom.value)
      }, 3000)
    }
  }
}

function manualReconnect() {
  if (!currentRoom.value) return
  wsReconnectAttempt.value = 0
  ElMessage.info('正在重新连接协作房间…')
  connectWebSocket(currentRoom.value)
}

function wsSend(action, data = {}) {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action, ...data }))
  }
}

function startHeartbeat() {
  stopHeartbeat()
  wsHeartbeatTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ action: 'ping' }))
    }
  }, 30000)
}
function stopHeartbeat() {
  if (wsHeartbeatTimer) { clearInterval(wsHeartbeatTimer); wsHeartbeatTimer = null }
}

function handleWsMessage(msg) {
  switch (msg.type) {
    case 'ROOM_STATE': {
      // 加入后全量状态
      currentRoom.value = msg.room
      participants.value = msg.participants || []
      roomJoined.value = true
      // 拉历史消息
      loadHistory()
      break
    }
    case 'MESSAGE': {
      // 过滤掉自己发的（已乐观显示）
      if (msg.userId !== userStore.profile?.id) {
        roomMessages.value.push({
          id: msg.id,
          user: msg.nickname || msg.username,
          content: msg.content,
          createdAt: msg.createdAt,
          role: 'theirs',
          msgType: msg.messageType,
        })
        scrollToBottom()
      }
      break
    }
    case 'AI_CHUNK': {
      // AI 流式输出
      if (!aiPartialContent.value) {
        // 第一块：创建 AI 消息占位
        roomMessages.value.push({
          id: msg.msgId || 'ai-' + Date.now(),
          user: '🤖 AI',
          content: '',
          createdAt: Date.now(),
          role: 'ai',
          msgType: 'AI',
        })
      }
      aiPartialContent.value += msg.content
      const last = roomMessages.value[roomMessages.value.length - 1]
      if (last?.role === 'ai') last.content = aiPartialContent.value
      if (msg.finished) {
        aiPartialContent.value = ''
        aiLoading.value = false
      }
      scrollToBottom()
      break
    }
    case 'PARTICIPANT_UPDATE': {
      participants.value = msg.participants || []
      break
    }
    case 'ERROR': {
      ElMessage.error('协作错误: ' + msg.message)
      break
    }
    case 'PONG': case 'HEARTBEAT_ACK': {
      // 心跳响应
      break
    }
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const r = await getMessages(currentRoom.value?.roomId || currentRoom.value?.id, 50)
    const msgs = (r.data?.list || r.data || []).map(m => ({
      id: m.id,
      user: m.nickname || m.username || (m.userId === null ? '🤖 AI' : m.username || '未知'),
      content: m.content,
      createdAt: m.createdAt,
      role: String(m.userId) === String(userStore.profile?.id) ? 'mine' : (m.type === 'AI' ? 'ai' : 'theirs'),
      msgType: m.type,
    }))
    roomMessages.value = msgs
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.warning('加载历史消息失败：' + (e.message || '请稍后重试'))
  } finally {
    historyLoading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgScrollRef.value?.wrapRef
    if (el) el.scrollTop = el.scrollHeight
  })
}

// ============ 房间操作 ============
const roomsEmptyText = computed(() => {
  if (loading.value) return '加载中...'
  if (keyword.value || statusFilter.value) return '没有匹配的房间，换个关键词试试'
  return '暂无房间，点击右上角「创建房间」开始协作'
})

async function loadRooms(silent = false) {
  if (!silent) loading.value = true
  try {
    const r = await listPublicRooms(50)
    rooms.value = r.data?.data || r.data?.list || r.data || []
  } catch { rooms.value = [] }
  finally { if (!silent) loading.value = false }
}

async function createRoom() {
  // 表单校验
  try {
    await createFormRef.value?.validate()
  } catch {
    ElMessage.warning('请检查表单填写')
    return
  }
  if (!newRoom.value.name.trim()) { ElMessage.warning('请填写房间名称'); return }
  creating.value = true
  try {
    const r = await createRoomApi(newRoom.value)
    const data = r.data?.data || r.data
    ElMessage.success(`房间「${newRoom.value.name}」已创建` + (data?.roomId ? `，ID: ${data.roomId}` : ''))
    showCreate.value = false
    await loadRooms()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    creating.value = false
  }
}

function joinRoom(r) {
  currentRoom.value = r
  roomJoined.value = false
  roomMessages.value = []
  aiPartialContent.value = ''
  roomDrawer.value = true
  connectWebSocket(r)
}

/** 确认加入（备用：WebSocket 已自动 join） */
async function doJoinRoom() {
  if (!currentRoom.value) return
  connectWebSocket(currentRoom.value)
}

/** 离开房间 */
function leaveRoom() {
  stopHeartbeat()
  clearTimeout(wsReconnectTimer)
  if (ws) { ws.close(); ws = null }
  wsConnected.value = false
  roomJoined.value = false
  roomDrawer.value = false
  currentRoom.value = null
  participants.value = []
  roomMessages.value = []
  aiPartialContent.value = ''
}

/** 发送聊天消息 */
function sendRoomMessage() {
  if (!roomInput.value.trim() || !roomJoined.value) return
  wsSend('chat', { content: roomInput.value.trim(), clientMsgId: 'c' + Date.now() })
  // 乐观显示
  roomMessages.value.push({
    id: 'local-' + Date.now(),
    user: userStore.profile?.nickname || userStore.profile?.username || '我',
    content: roomInput.value.trim(),
    createdAt: Date.now(),
    role: 'mine',
    msgType: 'CHAT',
  })
  roomInput.value = ''
  scrollToBottom()
}

/** 发送 AI 消息 → 后端 LLM 推理 + 流式广播 */
function sendRoomAi() {
  if (!aiInput.value.trim() || !roomJoined.value || aiLoading.value) return
  const prompt = aiInput.value.trim()
  aiLoading.value = true
  aiPartialContent.value = ''
  wsSend('ai', { prompt })
  aiInput.value = ''
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(typeof ts === 'string' ? ts.replace(' ', 'T') : ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// P1-6: 判断两条消息是否属于同一组（同用户、同分钟内）
function isSameGroup(prev, curr) {
  if (!prev || !curr) return false
  const sameUser = prev.user === curr.user && prev.role === curr.role
  if (!sameUser) return false
  const timeDiff = Math.abs(new Date(prev.createdAt) - new Date(curr.createdAt))
  return timeDiff < 60000 // 同一分钟内
}

async function inviteMember(r) {
  try {
    const { value: email } = await ElMessageBox.prompt('请输入要邀请的用户邮箱', '邀请成员', {
      confirmButtonText: '发送邀请',
      cancelButtonText: '取消',
      inputPattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/,
      inputErrorMessage: '邮箱格式不正确',
    })
    if (!email) return
    // 模拟发送邀请反馈
    ElMessage.success(`邀请已发送到 ${email}（房间号: ${r.roomId || r.id}）`)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('邀请失败：' + (e.message || ''))
    }
  }
}

function copyRoomId(r) {
  const id = String(r.roomId || r.id || '')
  if (!id) { ElMessage.warning('房间号为空'); return }
  navigator.clipboard.writeText(id).then(
    () => ElMessage.success(`房间号已复制：${id}`),
    () => ElMessage.error('复制失败，请手动选中复制')
  )
}

onMounted(() => loadRooms())
onUnmounted(() => {
  wsReconnectAttempt.value = 0
  clearTimeout(wsReconnectTimer)
  stopHeartbeat()
  if (ws) { try { ws.close() } catch {} ws = null }
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.hot-room { display: flex; align-items: center; gap: 12px; padding: 12px 16px; cursor: pointer; border-bottom: 1px solid #f0f0f0; &:hover { background: #f5f7fa; } }
.online-member { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-bottom: 1px solid #f0f0f0; }
.online-dot { width: 8px; height: 8px; border-radius: 50%; background: #67c23a; flex-shrink: 0; }

// V6.8.13: 房间聊天抽屉样式
.room-info {
  padding: 8px 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
  .room-topic { font-size: 13px; color: #909399; margin-bottom: 8px; }
  .room-members { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
}
.room-messages {
  height: calc(100vh - 380px);
  min-height: 200px;
  padding: 4px 0;
}
.msg-divider {
  height: 1px;
  background: #e8e8e8;
  margin: 8px 0;
}
.room-msg {
    display: flex;
    gap: 8px;
    margin-bottom: 10px;
    align-items: flex-start;
    .msg-body { flex: 1; }
    .msg-meta { font-size: 11px; color: #909399; margin-bottom: 2px; }
    .msg-content { font-size: 13px; color: #303133; background: #f5f7fa; border-radius: 8px; padding: 6px 10px; display: inline-block; max-width: 90%; word-break: break-all; }
    &.mine {
      flex-direction: row-reverse;
      .msg-meta { text-align: right; }
      .msg-content { background: #ecf5ff; color: #409eff; }
    }
    &.ai {
      .msg-content { background: linear-gradient(135deg, #f0fdf4, #dcfce7); color: #15803d; border: 1px solid #bbf7d0; }
    }
  }

.room-input {
  display: flex;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  position: sticky;
  bottom: 0;
  background: #fff;
}
.room-ai-area {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-top: 1px dashed #e8f5e9;
  margin-top: 4px;
  background: #f0fdf4;
}
</style>
