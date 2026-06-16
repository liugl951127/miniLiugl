<template>
  <div class="chat-page">
    <!-- 左侧：会话侧边栏 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="onNewChat">
          新建会话
        </el-button>
      </div>

      <el-input
        v-model="search"
        placeholder="搜索会话..."
        :prefix-icon="Search"
        clearable
        class="search-input"
      />

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
        </div>

        <div class="chat-messages" ref="msgBox">
          <div
            v-for="m in sessionStore.messages"
            :key="m.id"
            class="message"
            :class="`role-${m.role}`"
          >
            <div class="bubble">{{ m.content }}</div>
            <div class="meta">{{ formatTime(m.createdAt) }} · {{ m.role }}</div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行（Day 5 接入流式）"
            @keydown.enter.exact.prevent="onSend"
          />
          <el-button type="primary" :loading="sending" @click="onSend" class="send-btn">
            发送
          </el-button>
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
import { modelApi } from '@/api/model'

const sessionStore = useSessionStore()
const modelStore = useModelStore()
const search = ref('')
const input = ref('')
const sending = ref(false)
const msgBox = ref(null)
const modelCode = ref(modelStore.currentModel)

const filteredSessions = computed(() => {
  const kw = search.value.trim().toLowerCase()
  if (!kw) return sessionStore.sessions
  return sessionStore.sessions.filter(s =>
    (s.title || '').toLowerCase().includes(kw)
  )
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
      inputValue: '新会话',
      confirmButtonText: '创建',
      cancelButtonText: '取消'
    })
    await sessionStore.createSession({ title: title || '新会话' })
    ElMessage.success('会话已创建')
  } catch (e) {
    // 用户取消
  }
}

async function onSessionCmd(cmd, s) {
  if (cmd === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('修改会话名', '重命名', {
        inputValue: s.title,
        confirmButtonText: '保存',
        cancelButtonText: '取消'
      })
      await sessionApi.update(s.id, { title: value })
      s.title = value
      ElMessage.success('已重命名')
    } catch (e) { /* cancel */ }
  } else if (cmd === 'archive') {
    try {
      await ElMessageBox.confirm(`归档会话 "${s.title}"?`, '确认', {
        type: 'warning', confirmButtonText: '归档', cancelButtonText: '取消'
      })
      await sessionStore.removeSession(s.id)
      ElMessage.success('已归档')
    } catch (e) { /* cancel */ }
  }
}

async function onSend() {
  if (!input.value.trim() || !sessionStore.currentSession) return
  const sid = sessionStore.currentSessionId
  const text = input.value.trim()
  input.value = ''
  sending.value = true
  try {
    // 1) 发送用户消息到 chat 模块入库
    await sessionStore.appendMessage(sid, { role: 'user', content: text })
    await scrollBottom()
    // 2) 调真实模型（带历史上下文）
    const historyMsgs = sessionStore.messages
      .filter(m => m.role === 'user' || m.role === 'assistant')
      .slice(-10)
      .map(m => ({ role: m.role, content: m.content }))
    const res = await modelApi.chat({
      model: modelCode.value,
      messages: historyMsgs
    })
    const reply = res.data?.content || '（模型无响应）'
    await sessionStore.appendMessage(sid, { role: 'assistant', content: reply })
    await scrollBottom()
  } catch (e) {
    console.error(e)
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
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
  height: calc(100vh - 88px); // 减去 layout header/main padding
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

.chat-main {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
}
.chat-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px; color: #999;
  h2 { margin: 0; font-size: 20px; }
  p { margin: 0; font-size: 14px; }
}
.chat-header {
  padding: 16px 24px; border-bottom: 1px solid var(--minimax-border);
  display: flex; align-items: center; gap: 12px;
  h3 { margin: 0; font-size: 16px; font-weight: 600; }
}
.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  display: flex; flex-direction: column; gap: 16px;
  .message { max-width: 70%; display: flex; flex-direction: column; gap: 4px; }
  .role-user { align-self: flex-end; align-items: flex-end; .bubble { background: #5b8def; color: #fff; } }
  .role-assistant { align-self: flex-start; .bubble { background: #f0f2f5; color: var(--minimax-text); } }
  .role-system { align-self: center; .bubble { background: #fdf6ec; color: #b88230; font-size: 13px; } }
  .role-tool { align-self: flex-end; .bubble { background: #ecf5ff; color: #5b8def; font-family: monospace; font-size: 13px; } }
  .bubble { padding: 10px 14px; border-radius: 12px; line-height: 1.6; white-space: pre-wrap; word-wrap: break-word; }
  .meta { font-size: 11px; color: #999; padding: 0 4px; }
}
.chat-input {
  padding: 12px 24px 20px; border-top: 1px solid var(--minimax-border);
  display: flex; gap: 12px; align-items: flex-end;
  .send-btn { height: 60px; }
}
</style>
