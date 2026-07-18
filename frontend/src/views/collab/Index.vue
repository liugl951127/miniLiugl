<!--
  @file views/collab/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="collab-container">
    <div class="collab-header">
      <h1>👥 实时协作 <span class="badge">V2.8.7</span></h1>
      <p class="sub">多人协同 AI 对话 / 文档编辑 / 训练监控 · WebSocket 实时同步</p>
    </div>

    <!-- 未加入: 选择房间 -->
    <el-row v-if="!joined" :gutter="20">
      <el-col :span="14">
        <el-card>
          <template #header><span>🚀 创建新房间</span></template>
          <el-form :model="createForm" label-width="100px">
            <el-form-item label="房间名称">
              <el-input v-model="createForm.name" placeholder="例: 产品需求评审" />
            </el-form-item>
            <el-form-item label="房间类型">
              <el-select v-model="createForm.type" style="width:100%">
                <el-option label="AI 对话协作" value="AI_CHAT" />
                <el-option label="文档协作" value="DOC" />
                <el-option label="训练监控" value="TRAINING" />
                <el-option label="仪表盘协作" value="DASHBOARD" />
                <el-option label="代码协作" value="CODE" />
              </el-select>
            </el-form-item>
            <el-form-item label="最大人数">
              <el-input-number v-model="createForm.maxParticipants" :min="2" :max="100" />
            </el-form-item>
            <el-form-item label="是否公开">
              <el-switch v-model="createForm.isPublic" />
              <span class="hint">公开房间任何登录用户可加入</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onCreate" :loading="creating">创建并加入</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header>
            <span>🌐 公开房间</span>
            <el-button size="small" text @click="loadPublicRooms" style="float:right">刷新</el-button>
          </template>
          <div v-if="publicRooms.length === 0" class="empty-tip">暂无公开房间</div>
          <div v-else class="public-room-list">
            <div v-for="r in publicRooms" :key="r.id" class="public-room-item" @click="quickJoin(r)">
              <div class="info">
                <div class="name">
                  <span class="type-tag" :class="'type-' + r.type">{{ r.type }}</span>
                  {{ r.name }}
                </div>
                <div class="meta">
                  👤 {{ r.ownerName }} · 👥 {{ r.currentParticipants }}/{{ r.maxParticipants }}
                  · {{ formatTime(r.lastActivityAt) }}
                </div>
              </div>
              <el-button size="small" type="primary" plain>加入</el-button>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header><span>🔗 通过房间号加入</span></template>
          <el-input v-model="joinSessionId" placeholder="输入 8 位房间号" style="width: 100%">
            <template #append>
              <el-button @click="onJoinById" :disabled="!joinSessionId">加入</el-button>
            </template>
          </el-input>
        </el-card>
      </el-col>
    </el-row>

    <!-- 已加入: 实时协作界面 -->
    <div v-else>
      <el-card class="room-card">
        <template #header>
          <div class="room-header">
            <div>
              <span class="type-tag" :class="'type-' + (roomInfo?.type || 'AI_CHAT')">{{ roomInfo?.type }}</span>
              <span class="room-name">{{ roomInfo?.name }}</span>
              <span class="room-id">#{{ roomId }}</span>
              <el-tag :type="wsStatus === 'open' ? 'success' : 'danger'" size="small" style="margin-left: 8px">
                {{ wsStatus === 'open' ? '● 已连接' : '○ ' + wsStatus }}
              </el-tag>
            </div>
            <div>
              <el-button size="small" @click="copyInviteLink">
                <el-icon><Link /></el-icon> 复制邀请
              </el-button>
              <el-button v-if="isOwner" size="small" type="danger" @click="onCloseRoom">关闭房间</el-button>
              <el-button size="small" @click="onLeave">离开</el-button>
            </div>
          </div>
        </template>

        <el-row :gutter="20">
          <!-- 左侧: 参与者 + 光标地图 -->
          <el-col :span="6">
            <div class="participants-panel">
              <h4>👥 在线 ({{ participants.length }})</h4>
              <div class="participant-list">
                <div
                  v-for="p in participants"
                  :key="p.userId"
                  class="participant"
                  :class="{ 'is-you': p.userId === currentUserId }"
                >
                  <el-avatar :size="32" :src="p.avatar">{{ (p.nickname || p.username).charAt(0) }}</el-avatar>
                  <div class="info">
                    <div class="name">
                      {{ p.nickname || p.username }}
                      <span v-if="p.userId === currentUserId" class="me-tag">我</span>
                      <el-tag v-if="p.role === 'OWNER'" size="small" type="warning" effect="plain">创建者</el-tag>
                    </div>
                    <div class="status">
                      <span :class="['status-dot', p.status?.toLowerCase()]"></span>
                      {{ p.status }}
                      <span v-if="p.cursorX != null" class="cursor-info">📍 {{ p.cursorX }},{{ p.cursorY }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 实时光标地图 -->
              <h4 style="margin-top: 20px">🖱️ 实时光标</h4>
              <div
                ref="cursorCanvas"
                class="cursor-canvas"
                @mousemove="onCursorMove"
                @mouseleave="onCursorLeave"
              >
                <div
                  v-for="p in cursorUsers"
                  :key="p.userId"
                  class="remote-cursor"
                  :style="{
                    left: (p.cursorX || 0) + 'px',
                    top: (p.cursorY || 0) + 'px',
                    color: p.color
                  }"
                >
                  <svg width="20" height="20" viewBox="0 0 20 20">
                    <path d="M3,3 L3,17 L8,12 L11,17 L14,16 L11,11 L17,11 Z" :fill="p.color" stroke="#fff" stroke-width="1" />
                  </svg>
                  <span class="cursor-label" :style="{ background: p.color }">
                    {{ p.nickname || p.username }}
                  </span>
                </div>
                <div class="canvas-hint">移动鼠标 → 其他参与者会看到你的光标</div>
              </div>
            </div>
          </el-col>

          <!-- 中间: 聊天 / AI 协作 -->
          <el-col :span="18">
            <div class="chat-area">
              <el-tabs v-model="activeTab" class="tb">
                <el-tab-pane label="💬 聊天" name="chat">
              <div class="messages" ref="msgBox">
                <div
                  v-for="m in messages"
                  :key="m.id"
                  :class="['message', m.messageType === 'AI' ? 'ai' : '', m.userId === currentUserId ? 'mine' : '']"
                >
                  <div class="msg-header">
                    <el-avatar :size="24">{{ (m.nickname || m.username || 'S').charAt(0) }}</el-avatar>
                    <span class="msg-author">{{ m.nickname || m.username || 'System' }}</span>
                    <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
                  </div>
                  <div class="msg-body" v-if="m.messageType === 'CHAT' || m.messageType === 'AI'">
                    {{ m.content }}
                  </div>
                  <div class="msg-body edit" v-else-if="m.messageType === 'EDIT'">
                    <el-tag size="small">📝 编辑操作</el-tag> {{ m.content }}
                  </div>
                </div>
                <div v-if="aiTyping" class="ai-typing">
                  <span></span><span></span><span></span> AI 正在思考...
                </div>
              </div>

              <!-- 输入区 -->
              <div class="composer">
                <el-input
                  v-model="inputText"
                  type="textarea"
                  :rows="2"
                  placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
                  @keydown.enter.exact.prevent="sendChat"
                />
                <div class="composer-actions">
                  <el-button @click="triggerAi" :loading="aiTyping" type="primary" plain>
                    🤖 AI 协作
                  </el-button>
                  <el-button type="primary" @click="sendChat" :disabled="!inputText.trim()">发送</el-button>
                </div>
              </div>
                </el-tab-pane>

                <el-tab-pane :label="`📝 协作文档 (${docText.length} 字)`" name="doc">
                  <div class="doc-toolbar">
                    <span class="doc-info">CRDT 版本: v{{ docVersion }}</span>
                    <span class="doc-info">客户端 ID: {{ clientId }}</span>
                    <el-tag v-if="docSource === 'pipeline'" size="small" type="success">AI 接入真实 Pipeline</el-tag>
                    <el-tag v-else size="small" type="info">AI Fallback (Mock)</el-tag>
                  </div>
                  <textarea
                    v-model="docText"
                    class="crdt-editor"
                    placeholder="多人同时编辑这个文本框, 实时同步给所有参与者 (V2.8.8 CRDT 真实多人编辑)..."
                    @input="onDocInput"
                  ></textarea>
                  <div class="doc-helper">
                    <small>输入即同步, 换行符也支持. 所有参与者的编辑按 CRDT 顺序合并, 不会互相覆盖.</small>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import {
  createRoom, getRoom, listPublicRooms, closeRoom,
  buildCollabWsUrl
} from '@/api/collab'
import { CrdtDoc, CrdtIdFactory } from '@/utils/crdt'
import { useUserStore } from '@/store/user'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const joined = ref(false)
const roomId = ref('')
const roomInfo = ref(null)
const currentUserId = computed(() => userStore.profile?.id || 0)
const isOwner = computed(() => roomInfo.value?.ownerId === currentUserId.value)

const joinSessionId = ref('')
const creating = ref(false)
const publicRooms = ref([])

// WebSocket
let ws = null
const wsStatus = ref('closed')
const participants = ref([])
const messages = ref([])
const inputText = ref('')
const aiTyping = ref(false)
const msgBox = ref(null)
const cursorCanvas = ref(null)
const cursorThrottle = { last: 0 }

// V2.8.8: CRDT 协作文档
const activeTab = ref('chat')
const docText = ref('')
const docVersion = ref(0)
const docSource = ref('mock')  // 'pipeline' / 'mock'
const clientId = ref(Math.floor(Math.random() * 1e9))
let crdtDoc = null
let crdtIdFactory = null
let isApplyingRemote = false  // 防止远程更新触发本地 echo

// 光标 (其他用户)
const remoteCursors = ref(new Map()) // userId -> {x, y, nickname, color, ts}
const cursorColors = ['#f56c6c', '#67c23a', '#409eff', '#e6a23c', '#909399', '#9b59b6', '#1abc9c', '#e74c3c']

const cursorUsers = computed(() => {
  const now = Date.now()
  const list = []
  for (const [uid, c] of remoteCursors.value.entries()) {
    if (now - c.ts < 30000) { // 30s 内有效
      list.push({
        userId: uid,
        x: c.x, y: c.y,
        nickname: c.nickname,
        username: c.username,
        color: c.color
      })
    }
  }
  return list
})

// 创建房间
const createForm = reactive({
  name: '',
  type: 'AI_CHAT',
  maxParticipants: 20,
  isPublic: true
})

const onCreate = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  creating.value = true
  try {
    const res = await createRoom({
      name: createForm.name || '新房间',
      type: createForm.type,
      ownerId: currentUserId.value,
      ownerName: userStore.profile.username,
      isPublic: createForm.isPublic,
      maxParticipants: createForm.maxParticipants
    })
    const room = res.data.data
    ElMessage.success(`房间已创建: ${room.roomId}`)
    doJoin(room.roomId)
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.message || e.message))
  } finally {
    creating.value = false
  }
}

const onJoinById = () => {
  if (!joinSessionId.value.trim()) return
  doJoin(joinSessionId.value.trim().toUpperCase())
}

const quickJoin = (r) => doJoin(r.roomId)

const doJoin = (rid) => {
  roomId.value = rid
  joined.value = true
  // V2.8.8: 初始化 CRDT
  crdtIdFactory = new CrdtIdFactory(clientId.value)
  crdtDoc = new CrdtDoc(crdtIdFactory)
  crdtDoc.observe((op) => {
    if (!isApplyingRemote) {
      // 本地操作 -> 发送到服务端
      sendDocOps([op])
    }
  })
  // 拉初始快照
  loadDocSnapshot(rid)
  connectWs(rid)
}

const loadDocSnapshot = async (rid) => {
  try {
    const res = await fetch(`/api/v1/collab/rooms/${rid}/doc`).then(r => r.json())
    if (res.code === 0) {
      const snapshot = res.data.snapshot
      // 应用快照到本地 CRDT (重建)
      isApplyingRemote = true
      try {
        crdtDoc.items.clear()
        crdtDoc.tombstones.clear()
        for (const it of snapshot.items) {
          crdtDoc.applyOp({
            type: 'insert',
            id: it.id,
            parentId: it.parent,
            content: it.content
          })
        }
        for (const tk of snapshot.tombstones) {
          const [cid, clk] = tk.split(':')
          crdtDoc.applyOp({
            type: 'delete',
            id: { clientId: parseInt(cid), clock: parseInt(clk) }
          })
        }
        docText.value = crdtDoc.toText()
        docVersion.value = snapshot.version || 0
      } finally {
        isApplyingRemote = false
      }
    }
  } catch (e) {
    console.warn('loadDocSnapshot failed:', e)
  }
}

const onDocInput = (e) => {
  // 简化: 检测增删字符, 生成对应 op
  if (isApplyingRemote || !crdtDoc || !ws) return
  const oldText = crdtDoc.toText()
  const newText = docText.value

  // 找到第一个差异位置
  let commonPrefix = 0
  while (commonPrefix < oldText.length && commonPrefix < newText.length
         && oldText[commonPrefix] === newText[commonPrefix]) {
    commonPrefix++
  }
  // 找到公共后缀
  let commonSuffix = 0
  while (commonSuffix < oldText.length - commonPrefix
         && commonSuffix < newText.length - commonPrefix
         && oldText[oldText.length - 1 - commonSuffix] === newText[newText.length - 1 - commonSuffix]) {
    commonSuffix++
  }

  const ops = []
  // 删除中间部分
  for (let i = 0; i < oldText.length - commonPrefix - commonSuffix; i++) {
    const op = crdtDoc.deleteAt(commonPrefix)
    if (op) ops.push(op)
  }
  // 插入新字符
  const inserted = newText.substring(commonPrefix, newText.length - commonSuffix)
  if (inserted) {
    // 逐字符插入 (简化)
    for (const ch of inserted) {
      const op = crdtDoc.insertAt(commonPrefix, ch)
      ops.push(op)
      commonPrefix++
    }
  }

  if (ops.length > 0) {
    sendDocOps(ops)
  }
}

const sendDocOps = (ops) => {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  ws.send(JSON.stringify({
    action: 'edit',
    ops: ops,
    clientMsgId: `doc-${Date.now()}`
  }))
}

const onLeave = () => {
  if (ws) {
    ws.send(JSON.stringify({ action: 'leave' }))
    ws.close()
  }
  joined.value = false
  roomId.value = ''
  roomInfo.value = null
  participants.value = []
  messages.value = []
  remoteCursors.value.clear()
}

const onCloseRoom = async () => {
  try {
    await ElMessageBox.confirm('确定关闭房间? 所有参与者将被踢出', '确认', { type: 'warning' })
    await closeRoom(roomId.value, currentUserId.value)
    ElMessage.success('房间已关闭')
    onLeave()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('关闭失败: ' + (e.response?.data?.message || e.message))
  }
}

const copyInviteLink = () => {
  const url = `${window.location.origin}/collab?roomId=${roomId.value}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('邀请链接已复制')
  }).catch(() => {
    ElMessage.info('房间号: ' + roomId.value)
  })
}

// ============= WebSocket =============

const connectWs = async (rid) => {
  // 先拉房间信息
  try {
    const res = await getRoom(rid)
    if (res.data?.code === 0 && res.data?.data) {
      roomInfo.value = res.data.data
    } else {
      ElMessage.error('房间不存在')
      joined.value = false
      return
    }
  } catch (e) {
    ElMessage.error('房间查询失败')
    joined.value = false
    return
  }

  if (!userStore.profile) {
    ElMessage.warning('请先登录')
    joined.value = false
    return router.push('/login')
  }

  const user = {
    id: currentUserId.value,
    username: userStore.profile.username,
    nickname: userStore.profile.nickname,
    avatar: userStore.profile.avatar
  }

  const url = buildCollabWsUrl(rid, user)
  wsStatus.value = 'connecting'
  ws = new WebSocket(url)

  ws.onopen = () => {
    wsStatus.value = 'open'
    ElMessage.success('已连接到协作房间')
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      handleWsMessage(msg)
    } catch (e) {
      console.warn('[collab] parse error:', e)
    }
  }

  ws.onerror = () => {
    wsStatus.value = 'error'
  }

  ws.onclose = () => {
    wsStatus.value = 'closed'
  }
}

const handleWsMessage = (msg) => {
  switch (msg.type) {
    case 'ROOM_STATE': {
      roomInfo.value = msg.room
      participants.value = (msg.participants || []).map((p, i) => ({
        ...p,
        color: cursorColors[i % cursorColors.length]
      }))
      break
    }
    case 'MESSAGE': {
      messages.value.push({
        id: msg.id,
        userId: msg.userId,
        username: msg.username,
        nickname: msg.nickname,
        messageType: msg.messageType,
        content: msg.content,
        op: msg.op,
        payload: msg.payload,
        createdAt: msg.createdAt
      })
      scrollToBottom()
      break
    }
    case 'PARTICIPANT_UPDATE': {
      participants.value = (msg.participants || []).map((p, i) => ({
        ...p,
        color: cursorColors[i % cursorColors.length]
      }))
      break
    }
    case 'CURSOR': {
      const color = participants.value.find(p => p.userId === msg.userId)?.color || '#409eff'
      remoteCursors.value.set(msg.userId, {
        x: msg.x, y: msg.y,
        nickname: msg.nickname, username: msg.username,
        selectionId: msg.selectionId,
        color,
        ts: Date.now()
      })
      // 触发响应式更新
      remoteCursors.value = new Map(remoteCursors.value)
      break
    }
    case 'AI_CHUNK': {
      aiTyping.value = !msg.finished
      // V2.8.8: 记录 AI 来源 (pipeline 真实 / mock fallback)
      if (msg.source) docSource.value = msg.source
      // 把 AI 流式输出合并到消息列表
      const last = messages.value[messages.value.length - 1]
      if (last && last.messageType === 'AI' && last._streaming) {
        last.content = (last.content || '') + msg.content
      } else {
        messages.value.push({
          id: msg.msgId || Date.now(),
          userId: null,
          nickname: 'AI Assistant',
          messageType: 'AI',
          content: msg.content,
          _streaming: !msg.finished,
          createdAt: new Date().toISOString()
        })
      }
      scrollToBottom()
      break
    }
    case 'DOC_UPDATE': {
      // V2.8.8: 应用远程 CRDT op
      isApplyingRemote = true
      try {
        if (msg.ops) crdtDoc.applyBatch(msg.ops)
        docText.value = crdtDoc.toText()
        docVersion.value = msg.version || docVersion.value
      } finally {
        isApplyingRemote = false
      }
      break
    }
    case 'DOC_UPDATE_ACK': {
      // 收到服务端 ack, 更新版本
      if (msg.version) docVersion.value = msg.version
      break
    }
    case 'ERROR': {
      ElMessage.error(msg.message)
      break
    }
    case 'PONG':
    case 'HEARTBEAT_ACK':
      break
    default:
      console.debug('[collab] unknown message type:', msg.type)
  }
}

const sendChat = () => {
  if (!inputText.value.trim() || !ws) return
  ws.send(JSON.stringify({
    action: 'chat',
    content: inputText.value.trim(),
    clientMsgId: `c-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }))
  inputText.value = ''
}

const triggerAi = () => {
  if (aiTyping.value) return
  const prompt = inputText.value.trim() || '介绍一下自己'
  if (inputText.value.trim()) inputText.value = ''
  aiTyping.value = true
  ws?.send(JSON.stringify({ action: 'ai', prompt }))
}

const onCursorMove = (e) => {
  const now = Date.now()
  if (now - cursorThrottle.last < 50) return // 50ms 节流
  cursorThrottle.last = now
  const rect = cursorCanvas.value.getBoundingClientRect()
  const x = Math.round(e.clientX - rect.left)
  const y = Math.round(e.clientY - rect.top)
  ws?.send(JSON.stringify({ action: 'cursor', x, y, selectionId: '' }))
}

const onCursorLeave = () => {
  // 可选: 发送 cursor 离开
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgBox.value) {
      msgBox.value.scrollTop = msgBox.value.scrollHeight
    }
  })
}

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return d.toLocaleString('zh-CN', { hour12: false })
}

const loadPublicRooms = async () => {
  try {
    const res = await listPublicRooms(50)
    publicRooms.value = res.data?.data || []
  } catch (e) {
    console.warn('[collab] load public rooms failed:', e)
  }
}

let heartbeatTimer = null
onMounted(() => {
  loadPublicRooms()
  // 心跳 (30s)
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ action: 'heartbeat' }))
    }
  }, 30000)
})

onUnmounted(() => {
  clearInterval(heartbeatTimer)
  if (ws) ws.close()
})
</script>

<style scoped>
.collab-container { padding: 20px; }
.collab-header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #409eff; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }

.empty-tip { color: #909399; text-align: center; padding: 40px 0; }

.public-room-list { max-height: 400px; overflow-y: auto; }
.public-room-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px; border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 8px;
  cursor: pointer; transition: all 0.2s;
}
.public-room-item:hover { border-color: #409eff; background: #f5f7fa; }
.public-room-item .name { font-weight: 500; }
.public-room-item .meta { font-size: 12px; color: #909399; margin-top: 4px; }

.type-tag { display: inline-block; font-size: 11px; padding: 1px 6px; border-radius: 3px; margin-right: 6px; color: #fff; }
.type-AI_CHAT { background: #409eff; }
.type-DOC { background: #67c23a; }
.type-TRAINING { background: #e6a23c; }
.type-DASHBOARD { background: #9b59b6; }
.type-CODE { background: #1abc9c; }

.room-card { min-height: 600px; }
.room-header { display: flex; justify-content: space-between; align-items: center; }
.room-name { font-weight: 500; margin-left: 6px; }
.room-id { color: #909399; font-size: 12px; margin-left: 8px; }

.participants-panel { padding: 8px; }
.participants-panel h4 { margin: 0 0 8px 0; font-size: 14px; }
.participant-list { max-height: 300px; overflow-y: auto; }
.participant {
  display: flex; align-items: center; padding: 6px 4px; border-radius: 4px;
}
.participant.is-you { background: #ecf5ff; }
.participant .info { margin-left: 8px; flex: 1; }
.participant .name { font-size: 13px; }
.participant .name .me-tag { font-size: 11px; color: #fff; background: #409eff; padding: 1px 4px; border-radius: 2px; margin-left: 4px; }
.participant .status { font-size: 11px; color: #909399; margin-top: 2px; }
.cursor-info { margin-left: 8px; }

.status-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 4px; }
.status-dot.online { background: #67c23a; box-shadow: 0 0 4px #67c23a; }
.status-dot.away { background: #e6a23c; }
.status-dot.offline { background: #c0c4cc; }

.cursor-canvas {
  position: relative; height: 200px; background: #fafafa; border: 1px dashed #dcdfe6;
  border-radius: 6px; overflow: hidden;
}
.canvas-hint { position: absolute; top: 8px; left: 8px; font-size: 11px; color: #c0c4cc; pointer-events: none; }
.remote-cursor { position: absolute; pointer-events: none; transition: all 0.1s ease; z-index: 10; }
.cursor-label {
  display: inline-block; margin-left: 4px; padding: 1px 6px; border-radius: 3px;
  color: #fff; font-size: 11px; white-space: nowrap;
}

.chat-area { display: flex; flex-direction: column; height: 600px; }
.messages { flex: 1; overflow-y: auto; padding: 12px; background: #fafafa; border-radius: 6px; }
.message { margin-bottom: 12px; padding: 8px 12px; background: #fff; border-radius: 6px; max-width: 80%; }
.message.mine { margin-left: auto; background: #ecf5ff; }
.message.ai { background: #f0f9ff; border-left: 3px solid #409eff; }
.msg-header { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #909399; }
.msg-author { font-weight: 500; color: #303133; }
.msg-time { margin-left: auto; }
.msg-body { margin-top: 4px; white-space: pre-wrap; word-break: break-word; }
.msg-body.edit { color: #e6a23c; font-family: monospace; font-size: 12px; }

.ai-typing { padding: 8px 12px; color: #909399; font-size: 12px; }
.ai-typing span { display: inline-block; width: 6px; height: 6px; background: #409eff; border-radius: 50%; margin-right: 3px; animation: typing 1.4s infinite; }
.ai-typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }

.composer { margin-top: 12px; }
.composer-actions { margin-top: 8px; display: flex; justify-content: flex-end; gap: 8px; }
.hint { color: #909399; font-size: 12px; margin-left: 12px; }

/* V2.8.8: 协作文档 */
.tb :deep(.el-tabs__header) { margin-bottom: 8px; }
.doc-toolbar {
  display: flex; align-items: center; gap: 12px; margin-bottom: 8px;
  padding: 6px 12px; background: #fafafa; border-radius: 4px; font-size: 12px;
}
.doc-info { color: #909399; font-family: monospace; }
.crdt-editor {
  width: 100%; min-height: 400px; padding: 12px;
  border: 1px solid #dcdfe6; border-radius: 4px;
  font-family: 'Monaco', 'Consolas', monospace; font-size: 13px; line-height: 1.6;
  resize: vertical; outline: none; transition: border-color 0.2s;
  background: #fff;
}
.crdt-editor:focus { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,0.1); }
.doc-helper { color: #909399; font-size: 11px; margin-top: 8px; padding: 0 4px; }
</style>
