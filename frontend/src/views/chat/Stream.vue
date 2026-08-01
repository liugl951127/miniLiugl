<!--
  @file views/chat/Stream.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/chat/Stream.vue (AI 流式对话)
  @version V3.5.12+ (前端注释补全)
  @description AI 流式对话
-->
<template>
  <div class="page-stream">
    <!-- 1. page-header -->
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('stream.title') }} <el-tag size="small" type="info">V5.19</el-tag></h2>
        <p class="page-subtitle">实时双向: 暂停 / 引导 / 评分反馈 / 注入上下文 / 切换模型</p>
      </div>
      <el-button-group>
        <el-button :icon="Connection" :type="connected ? 'success' : 'info'" plain>
          {{ connected ? '🟢 已连接' : '⚫ 未连接' }}
        </el-button>
        <el-button :icon="Refresh" @click="reconnect">重连</el-button>
      </el-button-group>
    </header>

    <el-row :gutter="16">
      <!-- 2. section: 主聊天区 -->
      <el-col :xs="24" :md="16">
        <section class="section chat-section">
          <h3 class="section-title">💬 实时对话</h3>
          <el-card shadow="hover" class="chat-card">
            <div class="chat-meta">
              <el-tag :type="connected ? 'success' : 'info'">{{ connected ? '🟢 已连接' : '⚫ 未连接' }}</el-tag>
              <el-tag>stream: {{ streamId || '-' }}</el-tag>
              <el-tag>model: {{ currentModel }}</el-tag>
              <el-tag v-if="paused" type="warning">⏸ 暂停中</el-tag>
            </div>

            <div class="messages" ref="messagesRef">
              <ChatBubble
                v-for="m in messages"
                :key="m.id"
                :message="m"
                :is-user="m.role === 'user'"
                :is-streaming="m.streaming"
              />
              <div v-if="!messages.length" class="empty-chat">
                <el-empty description="开始双向流式对话" :image-size="80" />
              </div>
            </div>

            <!-- 3. section: 输入 + 控制 -->
            <div class="input-bar">
              <el-input
                v-model="input"
                type="textarea"
                :rows="2"
                placeholder="输入消息... (Enter 发送 / Shift+Enter 换行)"
                @keydown.enter.exact.prevent="send"
                :disabled="!connected"
              />
              <div class="input-actions">
                <el-button v-if="!paused" :icon="VideoPause" @click="pauseStream" :disabled="!streaming">暂停</el-button>
                <el-button v-else :icon="VideoPlay" @click="resumeStream" type="primary">继续</el-button>
                <el-button :icon="Promotion" :loading="streaming" @click="send" type="primary">
                  {{ streaming ? '停止' : '发送' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </section>
      </el-col>

      <!-- 4. section: 侧栏 (模型 + 评分反馈) -->
      <el-col :xs="24" :md="8">
        <section class="section side-section">
          <h3 class="section-title">⚙️ 控制台</h3>
          <el-card shadow="hover" class="side-card">
            <el-form label-position="top" size="default">
              <el-form-item label="模型">
                <el-select v-model="currentModel" style="width: 100%">
                  <el-option v-for="m in models" :key="m.key" :label="m.label" :value="m.key" />
                </el-select>
              </el-form-item>
              <el-form-item label="评分 (上一条回复)">
                <el-rate v-model="lastRating" :max="5" show-text />
              </el-form-item>
              <el-form-item label="注入上下文">
                <el-input v-model="contextHint" type="textarea" :rows="3" placeholder="附加上下文..." />
                <el-button size="small" @click="injectContext" style="margin-top: 8px">注入</el-button>
              </el-form-item>
              <el-form-item>
                <el-button :icon="RefreshRight" @click="resetStream" plain style="width: 100%">重置会话</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </section>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const { t } = useI18n()
const userStore = useUserStore()
const connected = ref(false)
const streaming = ref(false)
const paused = ref(false)
const streamId = ref('')
const currentModel = ref('mock')
const selectedModel = ref('mock')
const steerText = ref('')
const feedbackScore = ref(0)
const feedbackText = ref('')
const injectText = ref('')

const messages = ref([])
const logs = ref([])

let ws = null

function log(type, msg) {
  const ts = new Date().toLocaleTimeString('zh-CN')
  logs.value.push({ ts, type, msg: msg.substring(0, 80) })
}

function connect() {
  if (connected.value) return
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再使用 WebSocket 聊天')
    return
  }
  const token = userStore.accessToken || ''
  const wsProto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const base = `${wsProto}//${location.host}`
  // 走 /api/v1/ws/bidi (nginx → gateway → minimax-ws via lb:ws://)
  const url = `${base}/api/v1/ws/bidi?type=chat&model=${selectedModel.value}&token=${encodeURIComponent(token)}`
  ws = new WebSocket(url)

  ws.onopen = () => {
    connected.value = true
    streaming.value = true
    log('open', 'WebSocket 已连接 (token=' + (token ? '✓' : '✗') + ')')
  }

  ws.onmessage = (ev) => {
    try {
      const m = JSON.parse(ev.data)
      handleEvent(m)
    } catch (e) { log('parse_err', ev.data) }
  }

  ws.onclose = () => {
    connected.value = false
    streaming.value = false
    paused.value = false
    log('close', '连接关闭')
  }

  ws.onerror = (e) => {
    log('error', 'WS 连接错误, 请检查登录状态')
    ElMessage.error('WebSocket 连接失败, 请先登录')
  }
}

function disconnect() {
  if (ws) {
    ws.close()
    ws = null
  }
}

function send() {
  if (!prompt.value.trim() || !connected.value) return
  // V5.19 WS bidi 是 URL 参数 (连接时定), 提示词放 stream state
  // 这里简化: 重连带新 prompt
  disconnect()
  // 实际生产: prompt 通过 first message 发送
  // 这里 mock: 直接连, prompt 写死 (mock 用)
  connect()
}

function handleEvent(m) {
  log(m.type || m.action, JSON.stringify(m).substring(0, 80))
  switch (m.type) {
    case 'ready':
      streamId.value = m.streamId
      currentModel.value = m.model
      messages.value.push({ type: 'system', content: `🚀 已就绪 (${m.model}, ${m.streamId})`, progress: null })
      break
    case 'chunk':
      // 累加到最后一条 chunk (流式效果)
      const last = messages.value[messages.value.length - 1]
      if (last && last.type === 'chunk') {
        last.content += m.content
        last.progress = m.progress
      } else {
        messages.value.push({ type: 'chunk', content: m.content, progress: m.progress })
      }
      scrollToBottom()
      break
    case 'thinking':
      messages.value.push({ type: 'thinking', content: m.content, progress: null })
      scrollToBottom()
      break
    case 'tool_call':
      messages.value.push({ type: 'tool_call', content: m.content, progress: null })
      scrollToBottom()
      break
    case 'observation':
      messages.value.push({ type: 'observation', content: m.content, progress: null })
      scrollToBottom()
      break
    case 'status':
      messages.value.push({ type: 'status', content: `[${m.state}] ${m.message}`, progress: null })
      paused.value = (m.state === 'paused')
      scrollToBottom()
      break
    case 'done':
      streaming.value = false
      messages.value.push({ type: 'done', content: `✅ 完成 (${m.finishReason})`, progress: null })
      scrollToBottom()
      break
    case 'error':
      streaming.value = false
      messages.value.push({ type: 'error', content: `❌ ${m.message}`, progress: null })
      scrollToBottom()
      break
  }
}

function pause() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'pause' }))
  }
}

function resume() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'resume' }))
  }
}

function cancel() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'cancel' }))
  }
}

function steer() {
  if (!steerText.value.trim()) return ElMessage.warning('请输入引导方向')
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'steer', direction: steerText.value }))
    steerText.value = ''
  }
}

function sendFeedback() {
  if (!feedbackScore.value) return ElMessage.warning('请评分')
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      action: 'feedback',
      text: feedbackText.value,
      score: feedbackScore.value,
    }))
    feedbackScore.value = 0
    feedbackText.value = ''
  }
}

function inject() {
  if (!injectText.value.trim()) return ElMessage.warning('请输入注入内容')
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      action: 'inject',
      messages: [{ role: 'system', content: injectText.value }]
    }))
    injectText.value = ''
  }
}

function setModel() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'set_model', model: selectedModel.value }))
    currentModel.value = selectedModel.value
  }
}

function typeLabel(t) {
  return {
    chunk: 'AI', thinking: '思考', tool_call: '工具', observation: '结果',
    status: '状态', done: '完成', error: '错误', system: '系统'
  }[t] || t
}

const msgBox = ref(null)
function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}
</script>

<style scoped>
.chat-stream { padding: 16px; }
.header h1 { margin: 0; }
.header .sub { color: #6b7280; margin: 4px 0 16px 0; font-size: 13px; }
.badge { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-left: 8px; }

.chat-head { display: flex; align-items: center; }

.messages { height: 480px; overflow-y: auto; padding: 8px;
  background: #f9fafb; border-radius: 6px; margin-bottom: 12px; }
.msg { margin-bottom: 12px; padding: 10px; border-radius: 8px;
  background: #fff; border-left: 3px solid #6366f1; }
.msg-head { display: flex; justify-content: space-between; margin-bottom: 4px;
  font-size: 11px; color: #6b7280; }
.msg-type { background: #6366f1; color: #fff; padding: 1px 6px; border-radius: 3px;
  font-size: 10px; font-weight: 500; }
.msg-progress { color: #9ca3af; }

.msg.chunk { border-left-color: #10b981; background: #f0fdf4; }
.msg.chunk .msg-type { background: #10b981; }
.msg.thinking { border-left-color: #8b5cf6; background: #faf5ff; }
.msg.thinking .msg-type { background: #8b5cf6; }
.msg.tool_call { border-left-color: #f59e0b; background: #fffbeb; }
.msg.tool_call .msg-type { background: #f59e0b; }
.msg.observation { border-left-color: #06b6d4; background: #ecfeff; }
.msg.observation .msg-type { background: #06b6d4; }
.msg.status { border-left-color: #6b7280; background: #f3f4f6; font-style: italic; }
.msg.done { border-left-color: #10b981; background: #d1fae5; }
.msg.error { border-left-color: #ef4444; background: #fee2e2; }
.msg.system { border-left-color: #9ca3af; background: #f3f4f6; text-align: center; font-size: 12px; }

.msg.streaming .cursor { animation: blink 1s infinite; font-size: 20px; color: #6366f1; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

.msg-content { white-space: pre-wrap; line-height: 1.6; color: #1f2937; font-size: 14px; }

.input-bar { display: flex; gap: 8px; }
.input-bar .el-input { flex: 1; }
.input-actions { display: flex; flex-direction: column; gap: 6px; }

.event-log { max-height: 200px; overflow-y: auto; font-family: monospace; font-size: 11px; }
.log-item { display: flex; gap: 8px; padding: 2px 0; border-bottom: 1px dotted #e5e7eb; }
.log-type { color: #6366f1; min-width: 80px; }
.log-msg { color: #4b5563; }
</style>
