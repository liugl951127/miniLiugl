<!--
  WebSocket 流式输出 ShowCase (V4.2)
  5 种流式类型: chat/vision/audio/agent/battle
  后端: minimax-ws 8095
-->
<template>
  <div class="stream">
    <header class="header">
      <h1>📡 WebSocket 流式输出</h1>
      <p class="subtitle">5 种实时流式类型: chat / vision / audio / agent / battle</p>
      <div class="badges">
        <span class="badge">WS:8095</span>
        <span class="badge">{{ connected ? '🟢 已连接' : '🔴 未连接' }}</span>
        <span class="badge">{{ chunksReceived }} chunks</span>
      </div>
    </header>

    <el-tabs v-model="activeType" type="border-card" @tab-change="reconnect">
      <!-- chat -->
      <el-tab-pane label="💬 Chat" name="chat">
        <div class="type-grid">
          <section class="ctrl">
            <h3>控制</h3>
            <el-input v-model="chatPrompt" type="textarea" :rows="3" placeholder="输入 prompt" />
            <el-select v-model="chatModel" style="width:100%; margin-top: 8px">
              <el-option label="mock (沙箱)" value="mock" />
              <el-option label="gpt-4o-mini" value="gpt-4o-mini" />
              <el-option label="MiniMax-Text-01" value="MiniMax-Text-01" />
            </el-select>
            <div class="actions">
              <el-button type="primary" @click="reconnect" :disabled="connected">
                {{ connected ? '已连接' : '🔌 连接' }}
              </el-button>
              <el-button type="warning" @click="cancel" :disabled="!connected || !streaming">
                ⏹ 取消流
              </el-button>
              <el-button @click="sendPing" :disabled="!connected">📶 Ping</el-button>
            </div>
          </section>
          <section class="output">
            <h3>流式输出 (实时)</h3>
            <div class="stream-output" :class="{ streaming }">
              <span v-if="!chatText && !streaming" class="placeholder">等待连接...</span>
              <span class="text">{{ chatText }}</span>
              <span v-if="streaming" class="cursor">▊</span>
            </div>
            <div v-if="progress" class="progress-bar">
              <div class="bar" :style="{width: progress + '%'}"></div>
              <span class="bar-text">{{ Math.round(progress * 100) }}%</span>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- battle -->
      <el-tab-pane label="⚔ Battle" name="battle">
        <div class="type-grid">
          <section class="ctrl">
            <h3>控制</h3>
            <el-input v-model="battlePrompt" type="textarea" :rows="3" placeholder="prompt" />
            <div class="model-grid">
              <div v-for="m in battleModels" :key="m"
                   :class="['m-chip', { active: battleSel.includes(m) }]"
                   @click="toggleBattle(m)">{{ m }}</div>
            </div>
            <div class="actions">
              <el-button type="primary" @click="reconnect" :disabled="connected">连接</el-button>
              <el-button type="warning" @click="cancel">取消</el-button>
            </div>
          </section>
          <section class="output">
            <h3>对决流 (每模型并行)</h3>
            <div v-for="m in Object.keys(battleResults)" :key="m" class="battle-row">
              <div class="br-head">
                <span class="br-name">{{ m }}</span>
                <span v-if="battleResults[m].done" class="br-done">
                  ✓ {{ battleResults[m].latency }}ms
                </span>
              </div>
              <div class="br-text">{{ battleResults[m].text }}<span v-if="!battleResults[m].done && battleResults[m].text" class="cursor">▊</span></div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- vision -->
      <el-tab-pane label="👁 Vision" name="vision">
        <div class="type-grid">
          <section class="ctrl">
            <h3>控制</h3>
            <p style="color:#64748b">无需图片, mock 直接流式输出"看到"的内容</p>
            <el-input v-model="visionPrompt" placeholder="问题 (可空)" />
            <div class="actions">
              <el-button type="primary" @click="reconnect">连接</el-button>
              <el-button type="warning" @click="cancel">取消</el-button>
            </div>
          </section>
          <section class="output">
            <h3>视觉流</h3>
            <div class="stream-output">{{ visionText }}<span v-if="streaming" class="cursor">▊</span></div>
          </section>
        </div>
      </el-tab-pane>

      <!-- audio -->
      <el-tab-pane label="🔊 TTS Stream" name="audio">
        <div class="type-grid">
          <section class="ctrl">
            <h3>控制</h3>
            <el-input v-model="ttsText" type="textarea" :rows="3" placeholder="文字" />
            <div class="actions">
              <el-button type="primary" @click="reconnect">连接</el-button>
              <el-button type="warning" @click="cancel">取消</el-button>
            </div>
          </section>
          <section class="output">
            <h3>音频流</h3>
            <div v-for="(c, i) in ttsChunks" :key="i" class="tts-chunk">
              <span class="tc-num">{{ i + 1 }}.</span> {{ c }}
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- agent -->
      <el-tab-pane label="🤖 Agent" name="agent">
        <div class="type-grid">
          <section class="ctrl">
            <h3>控制</h3>
            <el-input v-model="agentGoal" type="textarea" :rows="3" placeholder="Agent 目标" />
            <div class="actions">
              <el-button type="primary" @click="reconnect">连接</el-button>
              <el-button type="warning" @click="cancel">取消</el-button>
            </div>
          </section>
          <section class="output">
            <h3>Agent 步骤 (实时)</h3>
            <div v-for="(s, i) in agentSteps" :key="i" class="agent-step">
              <span class="step-num">{{ i + 1 }}</span>
              <span class="step-text">{{ s }}</span>
            </div>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 事件日志 -->
    <section class="event-log">
      <h3>📜 WebSocket 事件流 (最近 {{ events.length }} 条)</h3>
      <div class="log-list">
        <div v-for="(e, i) in events.slice(-30)" :key="i" :class="`log log-${e.type}`">
          <span class="log-time">{{ e.time }}</span>
          <span class="log-type">{{ e.type }}</span>
          <span class="log-data">{{ e.data }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeType = ref('chat')
const connected = ref(false)
const streaming = ref(false)
const chunksReceived = ref(0)
const progress = ref(0)

let ws = null

const chatPrompt = ref('一句话介绍 MiniMax')
const chatModel = ref('mock')
const chatText = ref('')

const battlePrompt = ref('写一个 Python 装饰器')
const battleModels = ['mock', 'gpt-4o-mini', 'qwen-max', 'MiniMax-Text-01']
const battleSel = ref(['mock', 'qwen-max'])
const battleResults = reactive({})

const visionPrompt = ref('这张图里有什么?')
const visionText = ref('')

const ttsText = ref('你好 MiniMax')
const ttsChunks = ref([])

const agentGoal = ref('查找 3 个竞品的价格')
const agentSteps = ref([])

const events = ref([])

function log(type, data) {
  events.value.push({
    time: new Date().toISOString().slice(11, 19),
    type,
    data: typeof data === 'object' ? JSON.stringify(data).slice(0, 100) : String(data).slice(0, 100)
  })
}

function reconnect() {
  if (ws) try { ws.close() } catch {}
  // 重置
  chatText.value = ''
  visionText.value = ''
  ttsChunks.value = []
  agentSteps.value = []
  progress.value = 0
  for (const k of Object.keys(battleResults)) delete battleResults[k]
  battleSel.value.forEach(m => battleResults[m] = { text: '', done: false, latency: 0 })

  const params = new URLSearchParams()
  params.set('type', activeType.value)
  if (activeType.value === 'chat') {
    params.set('prompt', chatPrompt.value)
    params.set('model', chatModel.value)
  } else if (activeType.value === 'battle') {
    params.set('prompt', battlePrompt.value)
    params.set('models', battleSel.value.join(','))
  } else if (activeType.value === 'vision') {
    params.set('prompt', visionPrompt.value)
  } else if (activeType.value === 'audio') {
    params.set('text', ttsText.value)
  } else if (activeType.value === 'agent') {
    params.set('goal', agentGoal.value)
  }

  // 同源 ws: 走 nginx 80 → gateway 8080 (V1 一体化部署, 不直连 8095)
  const wsProto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = `${wsProto}//${location.host}/api/v1/ws/stream?${params.toString()}`
  log('connect', url)
  ws = new WebSocket(url)

  ws.onopen = () => { connected.value = true; streaming.value = true }
  ws.onclose = () => { connected.value = false; streaming.value = false }
  ws.onerror = (e) => log('error', e)
  ws.onmessage = (e) => {
    let msg
    try { msg = JSON.parse(e.data) } catch { log('raw', e.data); return }
    chunksReceived.value++
    log(msg.type, msg)

    if (msg.type === 'chunk') {
      if (activeType.value === 'chat') chatText.value += msg.content
      else if (activeType.value === 'vision') visionText.value += msg.content
      else if (msg.model && battleResults[msg.model]) battleResults[msg.model].text += msg.content
      if (msg.progress) progress.value = msg.progress
    } else if (msg.type === 'audio_chunk') {
      ttsChunks.value.push(msg.text)
    } else if (msg.type === 'agent_step') {
      agentSteps.value.push(msg.step)
    } else if (msg.type === 'model_start') {
      battleResults[msg.model] = { text: '', done: false, latency: 0 }
    } else if (msg.type === 'model_done') {
      if (battleResults[msg.model]) {
        battleResults[msg.model].done = true
        battleResults[msg.model].latency = msg.latencyMs
      }
    } else if (msg.type === 'done') {
      streaming.value = false
      ElMessage.success('流式完成')
    } else if (msg.type === 'error') {
      streaming.value = false
      ElMessage.error(msg.message)
    }
  }
}

function cancel() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'cancel' }))
  }
}

function sendPing() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'ping' }))
  }
}

function toggleBattle(m) {
  const i = battleSel.value.indexOf(m)
  if (i >= 0) battleSel.value.splice(i, 1)
  else battleSel.value.push(m)
}

onUnmounted(() => {
  if (ws) try { ws.close() } catch {}
})

onMounted(() => {
  // 不自动连, 让用户点
})
</script>

<style scoped>
.stream { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #10b981, #059669); color: #fff; border-radius: 12px; font-size: 12px; }
.type-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 20px; margin-top: 16px; }
.ctrl, .output { background: #f8fafc; border-radius: 12px; padding: 20px; }
h3 { margin: 0 0 16px; }
.actions { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
.model-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; margin-top: 12px; }
.m-chip { padding: 8px; border: 2px solid #e2e8f0; border-radius: 6px; cursor: pointer; text-align: center; font-size: 12px; }
.m-chip.active { border-color: #10b981; background: linear-gradient(135deg, #d1fae5, #a7f3d0); }
.stream-output { background: #0f172a; color: #e2e8f0; padding: 16px; border-radius: 8px;
  min-height: 200px; max-height: 400px; overflow-y: auto; font-family: 'SF Mono', monospace;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.stream-output.streaming { border: 2px solid #10b981; }
.placeholder { color: #64748b; font-style: italic; }
.cursor { animation: blink 1s step-start infinite; color: #10b981; }
@keyframes blink { 50% { opacity: 0; } }
.progress-bar { margin-top: 8px; position: relative; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.bar { height: 100%; background: linear-gradient(90deg, #10b981, #34d399); transition: width 0.3s; }
.bar-text { position: absolute; top: -2px; right: 0; font-size: 11px; color: #334155; }

.battle-row { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 8px; border-left: 3px solid #10b981; }
.br-head { display: flex; justify-content: space-between; margin-bottom: 4px; }
.br-name { font-weight: 600; color: #10b981; }
.br-done { font-size: 12px; color: #64748b; }
.br-text { font-family: monospace; font-size: 13px; white-space: pre-wrap; min-height: 24px; }

.tts-chunk { padding: 4px 8px; margin: 2px 0; background: #fff; border-radius: 4px; }
.tc-num { color: #10b981; font-weight: 600; }

.agent-step { display: flex; gap: 12px; padding: 8px 12px; margin-bottom: 6px; background: #fff; border-radius: 6px; }
.step-num { background: #10b981; color: #fff; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.step-text { font-family: monospace; font-size: 13px; white-space: pre-wrap; }

.event-log { margin-top: 24px; background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.event-log h3 { margin: 0 0 12px; font-size: 16px; }
.log-list { max-height: 240px; overflow-y: auto; background: #0f172a; padding: 8px; border-radius: 8px; }
.log { display: flex; gap: 8px; font-family: monospace; font-size: 11px; padding: 2px 0; }
.log-time { color: #64748b; }
.log-type { color: #fbbf24; min-width: 70px; }
.log-data { color: #e2e8f0; }
</style>
