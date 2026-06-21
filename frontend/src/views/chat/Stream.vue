<template>
  <div class="chat-stream">
    <div class="header">
      <h1>💬 双向流式聊天 <span class="badge">V5.19</span></h1>
      <p class="sub">实时双向: 你可以暂停 / 引导方向 / 评分反馈 / 注入上下文 / 切换模型</p>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="chat-card">
          <template #header>
            <div class="chat-head">
              <el-tag :type="connected ? 'success' : 'info'">
                {{ connected ? '🟢 已连接' : '⚫ 未连接' }}
              </el-tag>
              <el-tag style="margin-left:8px">stream: {{ streamId || '-' }}</el-tag>
              <el-tag style="margin-left:8px">model: {{ currentModel }}</el-tag>
              <el-tag v-if="paused" type="warning" style="margin-left:8px">⏸ 暂停中</el-tag>
            </div>
          </template>

          <div class="messages" ref="msgBox">
            <div v-for="(m, i) in messages" :key="i" :class="['msg', m.type]">
              <div class="msg-head">
                <span class="msg-type">{{ typeLabel(m.type) }}</span>
                <span v-if="m.progress !== null && m.progress !== undefined" class="msg-progress">
                  {{ Math.round(m.progress * 100) }}%
                </span>
              </div>
              <div class="msg-content">{{ m.content }}</div>
            </div>
            <div v-if="streaming" class="msg streaming">
              <span class="cursor">▊</span>
            </div>
          </div>

          <div class="input-bar">
            <el-input v-model="prompt" type="textarea" :rows="2"
              :disabled="!connected" placeholder="输入问题, Enter 发送 (Shift+Enter 换行)"
              @keydown.enter.exact.prevent="send" />
            <div class="input-actions">
              <el-button type="primary" :disabled="!connected" @click="send">🚀 发送</el-button>
              <el-button :disabled="!connected" @click="disconnect">🔌 断开</el-button>
              <el-button type="success" :disabled="!connected" @click="connect">🔗 连接</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 双向交互面板 -->
        <el-card>
          <template #header>🎮 双向交互</template>

          <el-button-group style="width:100%; margin-bottom:12px">
            <el-button :disabled="!connected || paused" @click="pause" style="flex:1">
              ⏸ 暂停
            </el-button>
            <el-button :disabled="!connected || !paused" @click="resume" style="flex:1">
              ▶ 恢复
            </el-button>
            <el-button :disabled="!connected" @click="cancel" style="flex:1">
              ⏹ 取消
            </el-button>
          </el-button-group>

          <el-divider>引导方向 (steer)</el-divider>
          <el-input v-model="steerText" placeholder="例如: 用更简洁的语言回答" size="small" />
          <el-button :disabled="!connected" size="small" style="margin-top:6px; width:100%"
        @click="steer">🎯 发送引导</el-button>

          <el-divider>反馈评分 (feedback)</el-divider>
          <el-rate v-model="feedbackScore" :max="5" />
          <el-input v-model="feedbackText" placeholder="反馈内容 (可选)" size="small" />
          <el-button :disabled="!connected" size="small" style="margin-top:6px; width:100%"
        @click="sendFeedback">⭐ 提交反馈</el-button>

          <el-divider>注入上下文 (inject)</el-divider>
          <el-input v-model="injectText" type="textarea" :rows="3" placeholder="注入的额外上下文 (RAG 召回结果等)"
            size="small" />
          <el-button :disabled="!connected" size="small" style="margin-top:6px; width:100%"
        @click="inject">📥 注入</el-button>

          <el-divider>切换模型 (set_model)</el-divider>
          <el-select v-model="selectedModel" placeholder="选择模型" size="small" style="width:100%">
            <el-option label="mock (演示)" value="mock" />
            <el-option label="gpt-4o-mini" value="gpt-4o-mini" />
            <el-option label="gpt-4o" value="gpt-4o" />
            <el-option label="claude-3-haiku-20240307" value="claude-3-haiku-20240307" />
            <el-option label="gemini-1.5-flash" value="gemini-1.5-flash" />
          </el-select>
          <el-button :disabled="!connected" size="small" style="margin-top:6px; width:100%"
        @click="setModel">🔄 切换</el-button>
        </el-card>

        <!-- 事件日志 -->
        <el-card style="margin-top:16px">
          <template #header>📋 事件日志</template>
          <div class="event-log">
            <div v-for="(log, i) in logs.slice(-10)" :key="i" class="log-item">
              <span class="log-type">{{ log.type }}</span>
              <span class="log-msg">{{ log.msg }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const prompt = ref('你好, 请介绍一下 MiniMax 平台')
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
  const url = `ws://localhost:3000/api/v1/ws/bidi?type=chat&model=${selectedModel.value}`
  // 通过 nginx :3000 走 gateway lb://minimax-ws
  ws = new WebSocket(url)

  ws.onopen = () => {
    connected.value = true
    streaming.value = true
    log('open', 'WebSocket 连接成功')
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
    log('error', '连接错误')
    ElMessage.error('WebSocket 连接失败')
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
