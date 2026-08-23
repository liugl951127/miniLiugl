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
        <!-- V8.0: 空状态 hero -->
        <div v-if="!messages.length && !streaming" class="chat-hero">
          <div class="hero-icon">💬</div>
          <h2 class="hero-title">开始与 AI 对话</h2>
          <p class="hero-subtitle">支持本地 ONNX 模型 / 云端商业模型 / Agent 委托</p>
          <div class="hero-prompts">
            <div v-for="p in heroPrompts" :key="p.text" class="hero-prompt-card" @click="usePrompt(p.text)">
              <div class="hp-icon">{{ p.icon }}</div>
              <div class="hp-text">{{ p.text }}</div>
            </div>
          </div>
          <div class="hero-tips">
            <el-tag v-for="t in heroTips" :key="t" size="small" effect="plain" round>{{ t }}</el-tag>
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

// V8.0: hero 提示
const heroPrompts = [
  { icon: '💡', text: '用一句话介绍下 Vue 3 的 Composition API' },
  { icon: '🔍', text: '查找本月销售额最高的产品 TOP 10' },
  { icon: '🛠️', text: '帮我写一个 Python 爬虫抓取豆瓣电影' },
  { icon: '✍️', text: '润色一下我的会议纪要, 改成正式邮件' }
]
const heroTips = ['Ctrl+Enter 发送', '支持图片/视频/音频', '本地 ONNX 推理', 'Agent 委托']
function usePrompt(text) { inputText.value = text }

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
    // V8.0.3 fix: sendMessageStream 签名是 (sessionId, body, opts),
    // 之前调错了 (整对象当 sessionId 传), 现在拆开
    const res = await sendMessageStream(
      activeSessionId.value,
      {
        content: userMsg.content,
        model: currentModel.value,
        agentId: currentAgentId.value,
        attachments: userMsg.attachments
      }
    )
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
  background:
    radial-gradient(at 0% 0%, rgba(99, 102, 241, 0.03) 0px, transparent 40%),
    radial-gradient(at 100% 100%, rgba(236, 72, 153, 0.02) 0px, transparent 40%),
    #fafbfc;
}
.msg-content { margin-bottom: 14px; }
.msg-bubble {
  background: white;
  padding: 12px 16px;
  border-radius: 12px;
  display: inline-block;
  max-width: 70%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f1f5f9;
}

/* V8.0: 空状态 hero */
.chat-hero {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 24px; text-align: center;
  max-width: 640px; margin: 0 auto;
}
.hero-icon {
  font-size: 56px; margin-bottom: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.hero-title {
  font-size: 24px; font-weight: 700; margin: 0 0 8px;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.hero-subtitle {
  font-size: 14px; color: #64748b; margin: 0 0 24px;
}
.hero-prompts {
  display: grid; grid-template-columns: repeat(2, 1fr);
  gap: 10px; width: 100%; margin-bottom: 20px;
}
.hero-prompt-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; background: white;
  border: 1px solid #e2e8f0; border-radius: 10px;
  cursor: pointer; text-align: left;
  transition: all 0.2s;
}
.hero-prompt-card:hover {
  border-color: #6366f1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}
.hp-icon { font-size: 20px; flex-shrink: 0; }
.hp-text { font-size: 13px; color: #475569; line-height: 1.4; }
.hero-tips {
  display: flex; gap: 6px; flex-wrap: wrap; justify-content: center;
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
