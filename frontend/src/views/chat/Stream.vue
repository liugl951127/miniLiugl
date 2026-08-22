<!-- @file chat/Stream.vue - 流式对话 V6.8.13 (企业级) -->
<template>
  <div class="stream-page">
    <div class="stream-header">
      <h2>🌊 双向流式对话</h2>
      <el-tag :type="streaming ? 'warning' : 'success'" effect="dark">
        {{ streaming ? '⏳ 生成中…' : 'SSE Streaming' }}
      </el-tag>
      <div style="flex:1"></div>
      <el-select v-model="sessionId" placeholder="选择会话" clearable style="width:200px" size="small">
        <el-option label="default" value="default" />
      </el-select>
    </div>

    <el-card class="stream-card" shadow="never" v-loading="streaming" element-loading-text="AI 正在生成回复…">
      <div ref="outputRef" class="stream-output">
        <div v-if="!outputLines.length && !streaming" class="stream-empty">
          <div class="stream-empty-icon">💬</div>
          <p>输入消息开始对话，支持 SSE 流式响应</p>
        </div>
        <div
          v-for="(line, i) in outputLines"
          :key="i"
          :class="['line', line.type]"
        >{{ line.text }}</div>
        <span v-if="streaming" class="cursor">▊</span>
      </div>
      <el-divider />
      <div class="input-row">
        <el-input
          v-model="input"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          type="textarea"
          :rows="2"
          :disabled="streaming"
          @keydown.enter.exact.prevent="send"
          maxlength="2000"
          show-word-limit
        />
        <div class="input-actions">
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="streaming"
            @click="send"
          >发送</el-button>
          <el-button
            v-if="streaming"
            type="danger"
            :icon="CircleClose"
            @click="stop"
          >停止</el-button>
          <el-button :disabled="streaming" @click="clearOutput">清空</el-button>
        </div>
      </div>
    </el-card>

    <!-- 状态栏 -->
    <div class="stream-status">
      <span v-if="lastChunkAt" style="color:#67c23a">
        ✓ 最近收到数据块: {{ formatTime(lastChunkAt) }}
      </span>
      <span v-else-if="streaming" style="color:#e6a23c">⏳ 等待数据…</span>
      <span v-else style="color:#909399">就绪</span>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, CircleClose } from '@element-plus/icons-vue'
import { sendMessageStream } from '@/api/session'

const input = ref('')
const outputLines = ref([])
const streaming = ref(false)
const outputRef = ref(null)
const sessionId = ref('default')
const lastChunkAt = ref(null)
let abortController = null

function formatTime(d) {
  if (!d) return ''
  const t = new Date(d)
  return t.toLocaleTimeString()
}

function scrollBottom() {
  nextTick(() => {
    if (outputRef.value) {
      outputRef.value.scrollTop = outputRef.value.scrollHeight
    }
  })
}

function clearOutput() {
  outputLines.value = []
  lastChunkAt.value = null
  scrollBottom()
}

function stop() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
  outputLines.value.push({ type: 'error', text: '⏹ 已手动停止' })
  ElMessage.warning('已停止生成')
}

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return

  input.value = ''
  outputLines.value.push({ type: 'user', text: '> ' + text })
  streaming.value = true
  lastChunkAt.value = null
  await nextTick()
  scrollBottom()

  // 创建 AbortController 用于停止
  abortController = new AbortController()

  try {
    await sendMessageStream(
      sessionId.value || 'default',
      { content: text },
      {
        signal: abortController.signal,
        onContent: (chunk) => {
          lastChunkAt.value = new Date()
          if (chunk == null) return
          const last = outputLines.value[outputLines.value.length - 1]
          if (last?.type === 'ai') {
            last.text += chunk
          } else {
            outputLines.value.push({ type: 'ai', text: String(chunk) })
          }
          scrollBottom()
        },
        onDone: () => {
          outputLines.value.push({ type: 'ai', text: '\n✓ [DONE]' })
        },
        onError: (e) => {
          outputLines.value.push({ type: 'error', text: '\n❌ 错误: ' + (e?.message || '流式请求失败') })
          ElMessage.error('流式请求失败')
        },
      }
    )
  } catch (e) {
    if (e?.name === 'AbortError') {
      // 用户主动停止
    } else {
      outputLines.value.push({ type: 'error', text: '\n❌ 错误: ' + (e?.message || '未知错误') })
      ElMessage.error('发送失败')
    }
  } finally {
    streaming.value = false
    abortController = null
    scrollBottom()
  }
}
</script>

<style lang="scss" scoped>
.stream-page { max-width: 800px; margin: 0 auto; }
.stream-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  h2 { margin: 0; font-size: 16px; }
}
.stream-card { padding: 0; }
.stream-output {
  min-height: 400px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  position: relative;
}
.stream-empty {
  text-align: center;
  color: rgba(226, 232, 240, 0.4);
  padding: 80px 20px;
  .stream-empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }
  p { margin: 0; font-size: 13px; }
}
.line {
  margin-bottom: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  &.user { color: #60a5fa; }
  &.ai { color: #4ade80; }
  &.error { color: #f87171; }
}
.cursor { animation: blink 1s infinite; }
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.input-row {
  display: flex;
  padding: 12px;
  gap: 8px;
  align-items: flex-start;
}
.input-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stream-status {
  margin-top: 8px;
  font-size: 12px;
  text-align: right;
  font-family: 'Courier New', monospace;
}
</style>
