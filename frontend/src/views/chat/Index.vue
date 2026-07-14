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
<template>
  <div class="chat-page">
    <!-- 侧边栏 -->
    <aside class="chat-side">
      <el-button type="primary" class="new-chat-btn" @click="newSession" :icon="EditPen">
        新建对话
      </el-button>
      <el-input v-model="searchKw" placeholder="搜索会话..." size="small" clearable class="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="session-list">
        <div
          v-for="s in filteredSessions"
          :key="s.id"
          :class="['session-item', { active: s.id === currentSessionId }]"
          @click="switchSession(s.id)"
        >
          <el-icon class="session-icon"><ChatDotRound /></el-icon>
          <div class="session-info">
            <div class="session-title">{{ s.title || '新对话' }}</div>
            <div class="session-meta">{{ s.lastMessageAt ? formatTime(s.lastMessageAt) : '刚刚' }}</div>
          </div>
          <el-dropdown trigger="click" @click.stop>
            <el-icon class="session-more"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="renameSession(s)">重命名</el-dropdown-item>
                <el-dropdown-item divided @click="deleteSession(s.id)">
                  <span style="color: #ef4444">删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-empty v-if="filteredSessions.length === 0" description="暂无会话" :image-size="60" />
      </div>
    </aside>

    <!-- 主区 -->
    <main class="chat-main">
      <!-- 空状态 -->
      <div v-if="messages.length === 0" class="chat-empty">
        <div class="empty-logo">🤖</div>
        <h2>Liugl-AI 智能助手</h2>
        <p>支持对话 · 记忆 · 知识库 · 工具调用 · 图片理解</p>
        <div class="quick-prompts">
          <div class="quick-prompt" @click="sendQuick('你好, 请介绍一下你自己')">
            <el-icon><Promotion /></el-icon> 你好, 请介绍一下你自己
          </div>
          <div class="quick-prompt" @click="sendQuick('帮我算一下 (123+456)*789 等于多少')">
            <el-icon><Cpu /></el-icon> 计算 (123+456)*789
          </div>
          <div class="quick-prompt" @click="sendQuick('上海现在几点?')">
            <el-icon><Clock /></el-icon> 上海现在几点?
          </div>
          <div class="quick-prompt" @click="sendQuick('选一个 1-100 的随机数')">
            <el-icon><MagicStick /></el-icon> 随机 1-100
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-else ref="messagesRef" class="chat-messages">
        <ChatMessage
          v-for="(m, i) in messages"
          :key="i"
          :role="m.role"
          :content="m.content"
          :images="m.images"
          :tool-calls="m.toolCalls"
          :sources="m.sources"
          :streaming="m.streaming"
          :status="m.status"
          :created-at="m.createdAt"
          @retry="retryMessage(i)"
        />
      </div>

      <!-- 输入区 -->
      <div class="chat-input-wrap">
        <!-- 拖拽提示 -->
        <transition name="fade">
          <div v-if="dragging" class="drop-overlay">
            <div class="drop-hint">
              <el-icon :size="48"><UploadFilled /></el-icon>
              <p>松开上传图片</p>
            </div>
          </div>
        </transition>

        <!-- 附件预览 -->
        <div v-if="pendingImages.length" class="image-preview-row">
          <div v-for="(img, i) in pendingImages" :key="i" class="image-preview">
            <img :src="img.url" alt="preview" />
            <el-icon class="image-remove" @click="removeImage(i)"><CircleCloseFilled /></el-icon>
          </div>
        </div>

        <div
          class="input-box"
          :class="{ 'is-drag': dragging }"
          @dragover.prevent="dragging = true"
          @dragleave.prevent="dragging = false"
          @drop.prevent="onDrop"
        >
          <textarea
            v-model="inputText"
            class="input-textarea"
            placeholder="输入消息, Enter 发送, Shift+Enter 换行 (可拖拽图片到此处)"
            rows="3"
            @keydown="onKey"
          />
          <div class="input-toolbar">
            <div class="toolbar-left">
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :on-change="onFileChange"
              >
                <el-button text size="small">
                  <el-icon><Picture /></el-icon>
                  图片
                </el-button>
              </el-upload>
              <el-select v-model="selectedModel" placeholder="选择模型" size="small" style="width: 180px; margin-left: 8px">
                <el-option v-for="m in models" :key="m.code" :label="m.displayName" :value="m.code" />
              </el-select>
            </div>
            <div class="toolbar-right">
              <span v-if="streaming" class="streaming-hint">
                <el-icon class="is-loading"><Loading /></el-icon>
                正在生成
              </span>
              <el-button v-if="streaming" @click="stopStream" type="warning" size="small">
                <el-icon><VideoPause /></el-icon>
                停止
              </el-button>
              <el-button
                v-else
                type="primary"
                size="small"
                :disabled="!canSend"
                @click="sendMessage"
              >
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </div>
        </div>
        <div class="input-hint">
          <span>内容由 AI 生成, 仅供参考</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
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

async function switchSession(id) {
  if (streaming.value) {
    ElMessage.warning('正在生成中, 请先停止')
    return
  }
  currentSessionId.value = id
  // 简化: 实际应调 GET /sessions/{id}/messages
  messages.value = []
}

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

function renameSession(s) {
  ElMessageBox.prompt('输入新标题', '重命名', { inputValue: s.title })
    .then(({ value }) => {
      s.title = value
      ElMessage.success('已修改')
    }).catch(() => {})
}

function sendQuick(text) {
  inputText.value = text
  sendMessage()
}

function onKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (canSend.value) sendMessage()
  }
}

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
