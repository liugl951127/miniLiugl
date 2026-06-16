<template>
  <div class="chat-page">
    <!-- 左侧：会话侧边栏 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="onNewChat">
          新建会话
        </el-button>
      </div>

      <el-input v-model="search" placeholder="搜索会话..." :prefix-icon="Search" clearable class="search-input" />

      <div class="session-list" v-loading="sessionStore.loading">
        <div
          v-for="s in filteredSessions"
          :key="s.id"
          class="session-item"
          :class="{ active: sessionStore.currentSessionId === s.id }"
          @click="sessionStore.selectSession(s.id)"
        >
          <el-icon class="session-icon"><ChatDotRound /></el-icon>
          <div class="session-meta">
            <div class="session-title">{{ s.title || '新会话' }}</div>
            <div class="session-sub">
              {{ s.messageCount || 0 }} 条 · {{ formatTime(s.lastMessageAt || s.updatedAt) }}
            </div>
          </div>
          <el-dropdown trigger="click" @command="onSessionCmd($event, s)">
            <el-icon class="session-more" @click.stop><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="archive" divided>归档</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-empty v-if="!sessionStore.loading && filteredSessions.length === 0" description="还没有会话" :image-size="80" />
      </div>
    </aside>

    <!-- 右侧：对话区 -->
    <main class="chat-main">
      <div v-if="!sessionStore.currentSession" class="chat-empty">
        <el-icon :size="100" color="#dcdfe6"><ChatDotRound /></el-icon>
        <h2>开始新对话</h2>
        <p>选择左侧的会话，或点击"新建会话"开始</p>
      </div>

      <template v-else>
        <div class="chat-header">
          <h3>{{ sessionStore.currentSession.title }}</h3>
          <el-select
            v-model="modelCode"
            placeholder="选择模型"
            size="small"
            style="width: 220px"
            @change="onModelChange"
          >
            <el-option
              v-for="m in modelStore.models"
              :key="m.code"
              :label="`${m.displayName} (${m.providerCode})`"
              :value="m.code"
            />
          </el-select>
          <el-tag v-if="streamInfo" type="success" size="small" class="streaming-tag">
            实时生成中... {{ streamInfo.chars }} 字符
          </el-tag>
        </div>

        <div class="chat-messages" ref="msgBox">
          <div
            v-for="m in sessionStore.messages"
            :key="m.id"
            class="message"
            :class="`role-${m.role}`"
          >
            <div class="bubble" v-if="m.role === 'assistant' && streamingId === m.id">
              {{ m.content }}<span class="cursor-blink">▊</span>
            </div>
            <div class="bubble" v-else>{{ m.content }}</div>
            <div class="meta">
              {{ formatTime(m.createdAt) }} · {{ m.role }}
              <span v-if="m.tokens" class="tokens"> · {{ m.tokens }} tokens</span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            :disabled="streaming"
            @keydown.enter.exact.prevent="onSend"
          />
          <div class="action-btns">
            <el-button v-if="!streaming" type="primary" @click="onSend" class="send-btn" :loading="sending">
              发送
            </el-button>
            <el-button v-else type="danger" @click="onStop" class="send-btn">
              停止
            </el-button>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { Plus, Search, ChatDotRound, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSessionStore } from '@/store/session'
import { useModelStore } from '@/store/model'
import { sessionApi } from '@/api/session'
import { streamChat as apiStreamChat } from '@/api/model'

const sessionStore = useSessionStore()
const modelStore = useModelStore()
const search = ref('')
const input = ref('')
const sending = ref(false)
const streaming = ref(false)
const streamingId = ref(null)
const streamInfo = ref(null)
const msgBox = ref(null)
const modelCode = ref(modelStore.currentModel)
let streamController = null

const filteredSessions = computed(() => {
  const kw = search.value.trim().toLowerCase()
  if (!kw) return sessionStore.sessions
  return sessionStore.sessions.filter(s => (s.title || '').toLowerCase().includes(kw))
})

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return d.toLocaleDateString()
}

async function onNewChat() {
  try {
    const { value: title } = await ElMessageBox.prompt('给新会话起个名字', '新建会话', {
      inputValue: '新会话', confirmButtonText: '创建', cancelButtonText: '取消'
    })
    await sessionStore.createSession({ title: title || '新会话' })
    ElMessage.success('会话已创建')
  } catch (e) {}
}

async function onSessionCmd(cmd, s) {
  if (cmd === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('修改会话名', '重命名', {
        inputValue: s.title, confirmButtonText: '保存', cancelButtonText: '取消'
      })
      await sessionApi.update(s.id, { title: value })
      s.title = value
      ElMessage.success('已重命名')
    } catch (e) {}
  } else if (cmd === 'archive') {
    try {
      await ElMessageBox.confirm(`归档会话 "${s.title}"?`, '确认', { type: 'warning' })
      await sessionStore.removeSession(s.id)
      ElMessage.success('已归档')
    } catch (e) {}
  }
}

async function onSend() {
  if (!input.value.trim() || !sessionStore.currentSession || streaming.value) return
  const sid = sessionStore.currentSessionId
  const text = input.value.trim()
  input.value = ''
  sending.value = true
  try {
    // 1) 用户消息入库
    await sessionStore.appendMessage(sid, { role: 'user', content: text })
    await scrollBottom()

    // 2) 准备 assistant 占位（id 为负数，本地标识，UI 显示）
    const placeholderId = -Date.now()
    sessionStore.messages.push({
      id: placeholderId, sessionId: sid, role: 'assistant', content: '',
      createdAt: new Date().toISOString()
    })
    streaming.value = true
    streamingId.value = placeholderId
    let accText = ''
    let charCount = 0

    // 3) 调流式 API
    const historyMsgs = sessionStore.messages
      .filter(m => (m.role === 'user' || m.role === 'assistant') && m.id > 0)
      .slice(-10)
      .map(m => ({ role: m.role, content: m.content }))

    streamController = apiStreamChat(historyMsgs, modelCode.value, (chunk) => {
      if (chunk.error) {
        ElMessage.error('流式失败: ' + chunk.error)
        stopStreaming(placeholderId, sid)
        return
      }
      if (chunk.done) {
        // 流完成，落库
        saveAssistantMessage(placeholderId, sid, accText, Math.max(1, Math.floor(accText.length / 2)))
        return
      }
      if (chunk.content) {
        accText += chunk.content
        charCount += chunk.content.length
        // 更新 UI 上对应占位消息的 content
        const idx = sessionStore.messages.findIndex(m => m.id === placeholderId)
        if (idx >= 0) sessionStore.messages[idx].content = accText
        streamInfo.value = { chars: charCount }
        scrollBottom()
      }
    })
  } catch (e) {
    console.error(e)
    ElMessage.error('发送失败')
    sending.value = false
  }
}

function onStop() {
  if (streamController) {
    streamController.abort()
    // 流会自己停止；占位消息也要落库
    const idx = sessionStore.messages.findIndex(m => m.id === streamingId.value)
    if (idx >= 0) {
      const m = sessionStore.messages[idx]
      saveAssistantMessage(m.id, m.sessionId, m.content + ' [已停止]', Math.max(1, Math.floor(m.content.length / 2)))
    }
  }
}

async function stopStreaming(placeholderId, sid) {
  streaming.value = false
  streamingId.value = null
  streamInfo.value = null
  sending.value = false
  streamController = null
  // 移除占位
  const idx = sessionStore.messages.findIndex(m => m.id === placeholderId)
  if (idx >= 0) sessionStore.messages.splice(idx, 1)
}

async function saveAssistantMessage(placeholderId, sid, content, tokens) {
  try {
    const res = await sessionApi.append ? null : null
    const sessionStore_ = sessionStore
    // 直接调 messageApi
    const { messageApi } = await import('@/api/session')
    const resp = await messageApi.append(sid, { role: 'assistant', content, tokens, finishReason: streaming.value ? 'cancelled' : 'stop' })
    const idx = sessionStore_.messages.findIndex(m => m.id === placeholderId)
    if (idx >= 0) sessionStore_.messages[idx] = resp.data
  } catch (e) {
    console.error('save assistant failed', e)
  } finally {
    streaming.value = false
    streamingId.value = null
    streamInfo.value = null
    sending.value = false
    streamController = null
  }
}

function onModelChange(code) {
  modelStore.setCurrentModel(code)
}

async function scrollBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

onMounted(async () => {
  await Promise.all([
    sessionStore.loadSessions(1),
    modelStore.loadModels()
  ])
  if (sessionStore.sessions.length > 0) {
    await sessionStore.selectSession(sessionStore.sessions[0].id)
  }
  if (modelStore.models.length > 0) {
    modelCode.value = modelStore.currentModel || modelStore.models[0].code
  }
})
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 88px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.chat-sidebar {
  width: 280px;
  border-right: 1px solid var(--minimax-border);
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  .sidebar-header { padding: 16px; }
  .new-chat-btn { width: 100%; }
  .search-input { margin: 0 16px 8px; width: calc(100% - 32px); }
  .session-list { flex: 1; overflow-y: auto; padding: 8px; }
  .session-item {
    display: flex; align-items: center; gap: 8px;
    padding: 10px 12px; border-radius: 6px; cursor: pointer;
    margin-bottom: 4px;
    transition: background 0.15s;
    &:hover { background: rgba(91,141,239,0.08); }
    &.active { background: rgba(91,141,239,0.15); }
    .session-icon { color: #5b8def; flex-shrink: 0; }
    .session-meta { flex: 1; min-width: 0; }
    .session-title {
      font-size: 14px; font-weight: 500; color: var(--minimax-text);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .session-sub { font-size: 11px; color: #999; margin-top: 2px; }
    .session-more { color: #aaa; padding: 4px; }
  }
}

.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px; color: #999;
  h2 { margin: 0; font-size: 20px; }
  p { margin: 0; font-size: 14px; }
}
.chat-header {
  padding: 16px 24px; border-bottom: 1px solid var(--minimax-border);
  display: flex; align-items: center; gap: 12px;
  h3 { margin: 0; font-size: 16px; font-weight: 600; flex: 1; }
  .streaming-tag { animation: pulse 1.5s infinite; }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  display: flex; flex-direction: column; gap: 16px;
  .message { max-width: 70%; display: flex; flex-direction: column; gap: 4px; }
  .role-user { align-self: flex-end; align-items: flex-end; .bubble { background: #5b8def; color: #fff; } }
  .role-assistant {
    align-self: flex-start; .bubble { background: #f0f2f5; color: var(--minimax-text); }
    .cursor-blink { animation: blink 1s steps(2) infinite; margin-left: 2px; }
  }
  .role-system { align-self: center; .bubble { background: #fdf6ec; color: #b88230; font-size: 13px; } }
  .bubble { padding: 10px 14px; border-radius: 12px; line-height: 1.6; white-space: pre-wrap; word-wrap: break-word; }
  .meta { font-size: 11px; color: #999; padding: 0 4px; .tokens { color: #5b8def; } }
}
@keyframes blink { 50% { opacity: 0; } }

.chat-input {
  padding: 12px 24px 20px; border-top: 1px solid var(--minimax-border);
  display: flex; gap: 12px; align-items: flex-end;
  .action-btns { display: flex; flex-direction: column; }
  .send-btn { height: 60px; min-width: 80px; }
}
</style>
