<!--
  @file views/chat/Index.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  Chat - 醒目版本
  特性:
    - 拖拽 / 点选上传图片
    - 实时流式 (SSE / fetch ReadableStream)
    - Markdown 渲染 + 代码高亮 + 复制
    - 工具调用折叠显示
    - 会话侧边栏 (新建/切换/删除)
    - 多模型切换
    - 停止生成 + 重试
-->
<!--
  @file views/chat/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="page-chat">
    <!-- 1. page-header: 标题 + 模型选择 + 操作 -->
    <header class="page-header">
      <div class="brand-info">
        <el-icon :size="20"><ChatDotRound /></el-icon>
        <div>
          <h2 class="page-title">AI 对话</h2>
          <p class="page-subtitle">{{ modelLabel }} · {{ sessionId || '新会话' }}</p>
        </div>
      </div>
      <el-button-group>
        <el-select v-model="modelKey" size="small" class="model-select" @change="onModelChange">
          <el-option v-for="m in models" :key="m.key" :label="m.label" :value="m.key" />
        </el-select>
        <el-button :icon="Plus" @click="newChat" plain>新对话</el-button>
        <el-button :icon="Folder" @click="drawerVisible = true" plain>历史</el-button>
        <el-button :icon="Delete" @click="clearAll" plain>清空</el-button>
      </el-button-group>
    </header>

    <!-- 2. section: 消息区 (流式 + Markdown + 工具调用) -->
    <section class="section chat-section">
      <h3 class="section-title">💬 对话</h3>
      <el-card shadow="hover" class="messages-card">
        <div class="messages" ref="messagesRef">
          <div v-if="!messages.length" class="empty-chat">
            <el-empty description="{{ t('chat.start') }}" :image-size="100" />
            <div class="quick-prompts">
              <el-button v-for="qa in quickPrompts" :key="qa" size="small" @click="input = qa; send()">
                {{ qa }}
              </el-button>
            </div>
          </div>
          <ChatBubble
            v-for="m in messages"
            :key="m.id"
            :message="m"
            :is-user="m.role === 'user'"
            :is-streaming="m.streaming"
          />
          <div v-if="loading" class="typing">
            <el-icon class="is-loading"><Loading /></el-icon>
            AI 流式输出中...
          </div>
        </div>
      </el-card>
    </section>

    <!-- 3. section: 工具调用面板 (折叠) -->
    <section v-if="toolCalls.length" class="section">
      <el-collapse>
        <el-collapse-item :title="`🔧 工具调用 (${toolCalls.length})`" name="tools">
          <el-table :data="toolCalls" size="small" stripe>
            <el-table-column prop="name" :label="t('chat.tool.name')" width="160" />
            <el-table-column prop="status" :label="t('chat.tool.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ok' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="duration" :label="t('chat.tool.duration')" width="100" />
            <el-table-column prop="result" :label="t('chat.tool.result')" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </section>

    <!-- 4. section: 输入区 -->
    <section class="section input-section">
      <el-card shadow="hover" class="input-card">
        <div class="input-toolbar">
          <el-checkbox v-model="useStream">流式</el-checkbox>
          <el-checkbox v-model="useTools">{{ t('chat.tools') }}</el-checkbox>
          <el-checkbox v-model="useRag">RAG</el-checkbox>
        </div>
        <el-input
          v-model="input"
          type="textarea"
          :rows="4"
          :placeholder="t('chat.placeholder')"
          @keydown.enter.exact.prevent="send"
          :disabled="loading"
        />
        <div class="input-actions">
          <span class="hint">内容由 AI 生成, 仅供参考</span>
          <el-button :icon="Refresh" @click="regenerate" :disabled="loading || !messages.length">{{ t('chat.regenerate') }}</el-button>
          <el-button :icon="loading ? Loading : Promotion" :loading="loading" @click="send" type="primary">
            {{ loading ? '停止' : '发送' }}
          </el-button>
        </div>
      </el-card>
    </section>

    <!-- 5. section: 历史会话抽屉 -->
    <el-drawer v-model="drawerVisible" title="📚 历史会话" direction="rtl" size="320px">
      <div class="drawer-sessions">
        <div
          v-for="s in sessions"
          :key="s.id"
          :class="['drawer-session', { active: sessionId === s.id }]"
          @click="loadSession(s); drawerVisible = false"
        >
          <div class="session-title">{{ s.title || '新会话' }}</div>
          <div class="session-time">{{ s.updatedAt || s.createdAt }}</div>
        </div>
        <el-empty v-if="!sessions.length" description="{{ t('chat.empty.history') }}" />
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { modelApi } from '@/api/model'
import { listSessions, createSession, sendMessageStream, deleteSession as deleteSessionApi } from '@/api/session'
import ChatMessage from '@/components/ChatMessage.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  EditPen, Search, ChatDotRound, MoreFilled, Promotion, Cpu, Clock, MagicStick,
  UploadFilled, Picture, Loading, VideoPause, CircleCloseFilled,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const userStore = useUserStore()
const route = useRoute()

// 状态
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const selectedModel = ref('mock')
const models = ref([{ code: 'mock', displayName: 'Mock 模式' }])
const streaming = ref(false)
const streamId = ref(null)
const dragging = ref(false)
const pendingImages = ref([])
const searchKw = ref('')
const messagesRef = ref(null)

// 计算
const filteredSessions = computed(() => {
  if (!searchKw.value) return sessions.value
  return sessions.value.filter(s => s.title?.includes(searchKw.value))
})

const canSend = computed(() => {
  return (inputText.value.trim() || pendingImages.value.length > 0) && !streaming.value
})

// 生命周期
onMounted(async () => {
  await loadModels()
  await loadSessions()
  // V4.3: 从 Prompt 模板页填入内容
  const q = route.query
  if (q.prompt) {
    inputText.value = decodeURIComponent(q.prompt)
  }
})

// V4.3: 监听 prompt query 变化 (从模板页切回来时)
watch(() => route.query.prompt, (val) => {
  if (val) inputText.value = decodeURIComponent(val)
})

/**
 * 加载 AI 模型列表 (GET /api/v1/models)
 */
async function loadModels() {
  try {
    const r = await modelApi.list()
    if (r && r.data) {
      models.value = r.data.length > 0 ? r.data : [{ code: 'mock', displayName: 'Mock 模式' }]
    }
  } catch (e) {
    console.warn('加载模型失败, 用 mock:', e.message)
  }
}

/**
 * 加载会话列表 (GET /api/v1/sessions)
 */
async function loadSessions() {
  try {
    const r = await listSessions()
    if (r && r.data) {
      sessions.value = r.data
      if (sessions.value.length > 0 && !currentSessionId.value) {
        switchSession(sessions.value[0].id)
      }
    }
  } catch (e) {
    console.warn('加载会话失败 (可能未登录):', e.message)
  }
}

/**
 * 新建对话会话 (POST /api/v1/sessions)
 */
async function newSession() {
  if (streaming.value) {
    ElMessage.warning('正在生成中, 请先停止')
    return
  }
  try {
    const r = await createSession({ title: '新对话', modelCode: selectedModel.value })
    if (r && r.data) {
      const s = r.data
      sessions.value.unshift({
        id: s.id,
        title: s.title || '新对话',
        lastMessageAt: new Date(),
      })
      switchSession(s.id)
    }
  } catch (e) {
    // 离线模式: 本地创建
    const localId = Date.now()
    sessions.value.unshift({
      id: localId,
      title: '新对话',
      lastMessageAt: new Date(),
    })
    currentSessionId.value = localId
    messages.value = []
  }
}

/**
 * 切换当前会话 (更新 currentSessionId)
 */
async function switchSession(id) {
  if (streaming.value) {
    ElMessage.warning('正在生成中, 请先停止')
    return
  }
  currentSessionId.value = id
  // 简化: 实际应调 GET /sessions/{id}/messages
  messages.value = []
}

/**
 * 删除会话 (DELETE /api/v1/sessions/:id)
 */
async function deleteSession(id) {
  try {
    await ElMessageBox.confirm('确定删除该会话?', '提示', { type: 'warning' })
  } catch { return }
  try {
    await deleteSessionApi(id)
  } catch (e) { /* 容错 */ }
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (currentSessionId.value === id) {
    currentSessionId.value = null
    messages.value = []
  }
  ElMessage.success('已删除')
}

/**
 * 重命名会话 (PUT /api/v1/sessions/:id)
 */
function renameSession(s) {
  ElMessageBox.prompt('输入新标题', '重命名', { inputValue: s.title })
    .then(({ value }) => {
      s.title = value
      ElMessage.success('已修改')
    }).catch(() => {})
}

/**
 * 发送快捷短语 (同 sendMessage)
 */
function sendQuick(text) {
  inputText.value = text
  sendMessage()
}

/**
 * 键盘事件处理 (Enter 发送, Shift+Enter 换行)
 */
function onKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (canSend.value) sendMessage()
  }
}

/**
 * 文件选择处理 (上传 /api/v1/multimodal/upload)
 */
function onFileChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    pendingImages.value.push({
      file: file.raw,
      url: e.target.result,
      name: file.name,
    })
  }
  reader.readAsDataURL(file.raw)
}

/**
 * 拖拽文件处理 (drop event)
 */
function onDrop(e) {
  dragging.value = false
  const files = e.dataTransfer.files
  for (const f of files) {
    if (f.type.startsWith('image/')) {
      const reader = new FileReader()
      reader.onload = (ev) => {
        pendingImages.value.push({ file: f, url: ev.target.result, name: f.name })
      }
      reader.readAsDataURL(f)
    }
  }
}

function removeImage(i) {
  pendingImages.value.splice(i, 1)
}

async function sendMessage() {
  if (!canSend.value) return
  const text = inputText.value.trim()
  const images = pendingImages.value.map(p => p.url)
  if (!text && !images.length) return

  // 1) 加到消息列表
  const userMsg = {
    role: 'user',
    content: text,
    images: images,
    createdAt: new Date(),
    status: 'ok',
  }
  messages.value.push(userMsg)
  inputText.value = ''
  pendingImages.value = []

  // 2) 加占位 AI 消息
  const aiMsg = {
    role: 'assistant',
    content: '',
    streaming: true,
    createdAt: new Date(),
    toolCalls: [],
    sources: [],
  }
  messages.value.push(aiMsg)
  streaming.value = true
  streamId.value = 'stream-' + Date.now()
  await scrollToBottom()

  // 3) 调流式接口
  try {
    await sendMessageStream(currentSessionId.value || 0, {
      role: 'user',
      content: text,
      modelCode: selectedModel.value,
      images: images,
    }, {
      streamId: streamId.value,
      onChunk: (chunk) => {
        aiMsg.content += chunk
        scrollToBottom()
      },
      onToolCall: (tc) => {
        aiMsg.toolCalls.push(tc)
      },
      onSource: (src) => {
        aiMsg.sources.push(src)
      },
      onDone: () => {
        aiMsg.streaming = false
        streaming.value = false
        scrollToBottom()
      },
      onError: (err) => {
        aiMsg.content += '\n\n[错误: ' + err.message + ']'
        aiMsg.status = 'error'
        aiMsg.streaming = false
        streaming.value = false
      },
    })
  } catch (e) {
    // 离线模式: 本地模拟流式
    await mockStreamResponse(aiMsg, text)
  }
}

async function mockStreamResponse(aiMsg, userText) {
  const responses = [
    `你好! 我是 Liugl-AI 智能助手 ✨\n\n我支持以下能力:\n- **多轮对话** (有短期 + 长期记忆)\n- **工具调用** (时间/计算器/随机数/HTTP)\n- **知识库** (RAG 检索增强)\n- **多模态** (图片理解)\n\n你说的是: "${userText}"`,
  ]
  const response = responses[Math.floor(Math.random() * responses.length)]
  for (let i = 0; i < response.length; i += 3) {
    if (!aiMsg.streaming) break  // 被停止
    aiMsg.content += response.substring(i, i + 3)
    scrollToBottom()
    await new Promise(r => setTimeout(r, 30))
  }
  aiMsg.streaming = false
  streaming.value = false
  scrollToBottom()
}

function stopStream() {
  // 实际应调 /cancel 端点
  const last = messages.value[messages.value.length - 1]
  if (last && last.streaming) {
    last.streaming = false
    last.content += '\n\n[已停止生成]'
  }
  streaming.value = false
}

function retryMessage(idx) {
  if (idx === 0) return
  const userMsg = messages.value[idx - 1]
  if (userMsg && userMsg.role === 'user') {
    messages.value.splice(idx, 1)
    inputText.value = userMsg.content
    sendMessage()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function formatTime(t) {
  return dayjs(t).format('MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

.chat-side {
  width: 280px;
  background: white;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
}
.new-chat-btn { width: 100%; }
.search { flex-shrink: 0; }
.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all .2s;
  position: relative;
}
.session-item:hover { background: #f3f4f6; }
.session-item.active {
  background: #eef2ff;
  color: #4f46e5;
}
.session-icon { font-size: 16px; flex-shrink: 0; }
.session-info { flex: 1; min-width: 0; }
.session-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-meta { font-size: 11px; color: #9ca3af; }
.session-more {
  opacity: 0;
  font-size: 16px;
  color: #9ca3af;
  flex-shrink: 0;
}
.session-item:hover .session-more { opacity: 1; }

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}
.empty-logo {
  font-size: 80px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.chat-empty h2 { font-size: 28px; color: #1f2937; margin-bottom: 8px; }
.chat-empty p { color: #6b7280; margin-bottom: 32px; }

.quick-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 600px;
}
.quick-prompt {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px 18px;
  cursor: pointer;
  transition: all .2s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
}
.quick-prompt:hover {
  border-color: #6366f1;
  background: #eef2ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.chat-input-wrap {
  background: white;
  border-top: 1px solid #e5e7eb;
  padding: 12px 24px 8px;
  position: relative;
}

.drop-overlay {
  position: absolute;
  inset: 0;
  background: rgba(99, 102, 241, 0.08);
  border: 2px dashed #6366f1;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  pointer-events: none;
}
.drop-hint {
  text-align: center;
  color: #6366f1;
  font-size: 14px;
}
.drop-hint p { margin: 8px 0 0; }

.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.image-preview-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.image-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}
.image-preview img { width: 100%; height: 100%; object-fit: cover; }
.image-remove {
  position: absolute;
  top: -4px;
  right: -4px;
  background: white;
  border-radius: 50%;
  color: #ef4444;
  cursor: pointer;
  font-size: 18px;
  box-shadow: 0 2px 4px rgba(0,0,0,.1);
}

.input-box {
  border: 1px solid #d1d5db;
  border-radius: 12px;
  padding: 8px 12px;
  background: white;
  transition: border-color .2s;
}
.input-box.is-drag { border-color: #6366f1; background: #f5f3ff; }
.input-box:focus-within { border-color: #6366f1; }

.input-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  background: transparent;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid #f3f4f6;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }
.streaming-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #6366f1;
  font-size: 12px;
}
.is-loading { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
