<!--
  @file chat/Index.vue - AI 对话 V7.8 (组件化拆分)
  V7.8 改造 (告别 1366 行单文件):
  - SessionList.vue  - 左侧会话列表
  - ModelBar.vue    - 顶部模型选择
  - MessageList     - 消息列表 (使用现有 ChatMessage 组件)
  - ChatInput.vue   - 底部输入区
  - 本 Index.vue    - 编排 (~200 行)
-->
<template>
  <div class="chat-page">
    <SessionList
      :sessions="sessions"
      :active-id="activeSessionId"
      @create="createNewSession"
      @switch="switchSession"
      @rename="renameSession"
      @remove="removeSession"
    />

    <div class="chat-main">
      <ModelBar
        v-model:modelValue="currentModel"
        :agent-id="currentAgentId"
        :self-models="models.self"
        :onnx-models="models.onnx"
        :cloud-models="models.cloud"
        :agents="agents"
        @update:agent-id="currentAgentId = $event"
        @clear="clearMessages"
      />

      <div ref="messagesEl" class="messages">
        <ChatMessage
          v-for="(msg, i) in messages" :key="i"
          :message="msg"
        />
        <div v-if="streaming" class="msg-content">
          <div class="msg-bubble loading-dots">正在输入</div>
        </div>
        <div v-if="streamError" class="msg-content">
          <div class="msg-bubble error">
            流式连接中断
            <el-button size="small" type="primary" link @click="retryLast">重连</el-button>
          </div>
        </div>
      </div>

      <ChatInput
        v-model:text="inputText"
        v-model:attachments="attachments"
        :sending="sending"
        @send="sendMessage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import SessionList from './SessionList.vue'
import ModelBar from './ModelBar.vue'
import ChatInput from './ChatInput.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import { listSessions, listMessages, createSession, sendMessageStream, deleteSession } from '@/api/session'
import { listEnabledModels } from '@/api/model'
import { listAgents } from '@/api/agent'

const sessions = ref([])
const activeSessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const attachments = ref([])
const sending = ref(false)
const streaming = ref(false)
const streamError = ref(false)
const messagesEl = ref(null)

const currentModel = ref('chat')
const currentAgentId = ref(null)

const models = reactive({ self: [], onnx: [], cloud: [] })
const agents = ref([])

async function loadSessions() {
  try {
    const res = await listSessions()
    if (res.code === 0) {
      sessions.value = res.data || []
      if (sessions.value.length && !activeSessionId.value) {
        await switchSession(sessions.value[0])
      }
    }
  } catch (e) { console.error(e) }
}

async function switchSession(s) {
  activeSessionId.value = s.id
  currentModel.value = s.model || 'chat'
  currentAgentId.value = s.agentId || null
  await loadMessages(s.id)
}

async function loadMessages(sid) {
  try {
    const res = await listMessages(sid)
    if (res.code === 0) messages.value = res.data || []
  } catch (e) { messages.value = [] }
  scrollToBottom()
}

async function createNewSession() {
  try {
    const res = await apiCreateSession({ title: '新会话', model: currentModel.value })
    if (res.code === 0) {
      sessions.value.unshift(res.data)
      await switchSession(res.data)
    }
  } catch (e) { ElMessage.error('创建失败') }
}

function renameSession(s) {
  ElMessage.info('重命名: ' + s.title)
}

async function removeSession(s) {
  try {
    await deleteSession(s.id)
    sessions.value = sessions.value.filter(x => x.id !== s.id)
    if (activeSessionId.value === s.id) {
      activeSessionId.value = null
      messages.value = []
      if (sessions.value.length) await switchSession(sessions.value[0])
    }
  } catch (e) { ElMessage.error('删除失败') }
}

function clearMessages() {
  messages.value = []
}

async function sendMessage() {
  if (sending.value) return
  if (!inputText.value.trim() && !attachments.value.length) return
  sending.value = true
  streamError.value = false
  const userMsg = { role: 'user', content: inputText.value, attachments: [...attachments.value] }
  messages.value.push(userMsg)
  inputText.value = ''
  attachments.value = []
  scrollToBottom()
  streaming.value = true
  try {
    const res = await sendMessageStream({
      sessionId: activeSessionId.value,
      content: userMsg.content,
      model: currentModel.value,
      agentId: currentAgentId.value,
      attachments: userMsg.attachments
    })
    // 简化处理: 流式响应最终合并
    messages.value.push({ role: 'assistant', content: res.data?.text || res.data || '...', model: currentModel.value })
  } catch (e) {
    streamError.value = true
  } finally {
    streaming.value = false
    sending.value = false
    scrollToBottom()
  }
}

function retryLast() {
  streamError.value = false
  sendMessage()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  })
}

async function loadModels() {
  try {
    const res = await listEnabledModels()
    const list = Array.isArray(res) ? res : (res?.data || [])
    models.self = list.filter(m => m.source === 'self' || m.source === 'local')
    models.onnx = list.filter(m => m.source === 'onnx')
    models.cloud = list.filter(m => m.source === 'cloud' || m.source === 'api')
  } catch (e) { console.error('loadModels', e) }
}

async function loadAgents() {
  try {
    const res = await listAgents()
    if (res.code === 0) agents.value = res.data || []
  } catch (e) { console.error('loadAgents', e) }
}

onMounted(() => {
  loadSessions()
  loadModels()
  loadAgents()
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 240px 1fr;
  height: calc(100vh - 100px);
  background: white;
  border-radius: 12px;
  overflow: hidden;
}
.chat-main { display: flex; flex-direction: column; height: 100%; }
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}
.msg-content { margin-bottom: 12px; }
.msg-bubble {
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  display: inline-block;
  max-width: 70%;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.msg-bubble.error {
  background: var(--el-color-danger-light-9);
  border: 1px solid var(--el-color-danger-light-7);
  color: var(--el-color-danger);
}
.loading-dots::after {
  content: '...';
  animation: dots 1.5s infinite;
}
@keyframes dots {
  0%, 20% { content: '.'; }
  40% { content: '..'; }
  60%, 100% { content: '...'; }
}
</style>
