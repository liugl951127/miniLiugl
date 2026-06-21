<template>
  <div class="chat-page">
    <!-- 会话列表 -->
    <div class="session-bar">
      <van-dropdown-menu>
        <van-dropdown-item v-model="currentSession" :options="sessionOptions" @change="switchSession" />
      </van-dropdown-menu>
      <van-button size="mini" type="primary" @click="newSession" icon="plus" />
      <van-button size="mini" @click="clearChat" icon="delete-o" />
    </div>

    <!-- 消息流 -->
    <div class="messages" ref="msgBox">
      <div v-if="!messages.length && !streaming" class="empty">
        <van-empty description="开始第一句对话吧 👋">
          <van-button round type="primary" class="primary-btn" @click="quickAsk('你好, 请介绍一下你自己')">
            打个招呼
          </van-button>
        </van-empty>
      </div>

      <div
        v-for="(m, i) in messages"
        :key="i"
        :class="['msg-row', m.role]"
      >
        <div v-if="m.role === 'assistant'" class="avatar ai-avatar">🤖</div>
        <div v-else class="avatar user-avatar">
          {{ userStore.profile?.nickname?.[0] || 'U' }}
        </div>
        <div class="bubble-wrap">
          <div :class="['bubble', m.role]">
            <MarkdownView v-if="m.role === 'assistant'" :content="m.content" />
            <div v-else class="user-text">{{ m.content }}</div>
          </div>
          <div class="msg-time">{{ formatTime(m.time) }}</div>
        </div>
      </div>

      <!-- 流式中的消息 -->
      <div v-if="streaming" class="msg-row assistant">
        <div class="avatar ai-avatar">🤖</div>
        <div class="bubble-wrap">
          <div class="bubble assistant streaming">
            <span>{{ streamContent }}</span>
            <span class="cursor">▊</span>
          </div>
          <div class="msg-time">正在输入...</div>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-bar">
      <van-field
        v-model="input"
        type="textarea"
        rows="1"
        autosize
        placeholder="说点什么..."
        :border="false"
        class="input-field"
        @keyup.enter.exact.prevent="send"
        @focus="inputFocused = true"
        @blur="inputFocused = false"
      />
      <van-button
        type="primary"
        size="small"
        :loading="sending"
        :disabled="!input.trim()"
        @click="send"
        class="send-btn"
      >
        <template #icon>
          <van-icon name="arrow-up" v-if="!sending" />
          <van-loading v-else size="14px" color="white" />
        </template>
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import axios from 'axios'
import MarkdownView from '@/components/MarkdownView.vue'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const input = ref('')
const messages = ref<any[]>([])
const streaming = ref(false)
const streamContent = ref('')
const sending = ref(false)
const sessions = ref<any[]>([])
const currentSession = ref<number | string>(0)
const sessionOptions = ref<any[]>([{ text: '+ 新会话', value: 0 }])
const msgBox = ref<HTMLElement>()
const inputFocused = ref(false)

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

function formatTime(ts: number) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

async function loadSessions() {
  try {
    const { data } = await axios.get(`${API}/api/v1/sessions`, auth())
    sessions.value = data.data || []
    sessionOptions.value = [
      { text: '+ 新会话', value: 0 },
      ...sessions.value.map((s: any) => ({ text: s.title || `会话 ${s.id}`, value: s.id }))
    ]
  } catch (e) {}
}

async function newSession() {
  currentSession.value = 0
  messages.value = []
  try {
    const { data } = await axios.post(`${API}/api/v1/sessions`,
      { title: '移动端会话', model: 'MiniMax-Text-01' }, auth())
    currentSession.value = data.data.id
    await loadSessions()
  } catch (e) {
    showToast('创建失败')
  }
}

async function clearChat() {
  try {
    await showConfirmDialog({ title: '提示', message: '清空当前对话?' })
    messages.value = []
  } catch (e) {}
}

async function switchSession(id: number | string) {
  if (id === 0) return newSession()
  try {
    const { data } = await axios.get(`${API}/api/v1/sessions/${id}/messages`, auth())
    messages.value = (data.data || []).map((m: any) => ({
      role: m.role,
      content: m.content,
      time: Date.now(),
    }))
    scrollToBottom()
  } catch (e) {
    showToast('加载失败')
  }
}

async function quickAsk(text: string) {
  input.value = text
  await send()
}

async function send() {
  if (!input.value.trim() || sending.value) return
  const userMsg = { role: 'user', content: input.value, time: Date.now() }
  messages.value.push(userMsg)
  const text = input.value
  input.value = ''
  sending.value = true
  streaming.value = true
  streamContent.value = ''
  scrollToBottom()

  try {
    const url = `${API}/api/v1/models/chat/stream`
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userStore.accessToken}`,
      },
      body: JSON.stringify({
        messages: [...messages.value.filter(m => m.role !== 'assistant'), userMsg],
        model: 'MiniMax-Text-01',
      }),
    })
    if (!resp.body) throw new Error('No stream')

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const payload = line.substring(6).trim()
          if (payload === '[DONE]') continue
          try {
            const json = JSON.parse(payload)
            const delta = json.choices?.[0]?.delta?.content
            if (delta) {
              streamContent.value += delta
              scrollToBottom()
            }
          } catch {}
        }
      }
    }
    messages.value.push({ role: 'assistant', content: streamContent.value, time: Date.now() })
  } catch (e: any) {
    showToast('发送失败: ' + e.message)
  } finally {
    streaming.value = false
    streamContent.value = ''
    sending.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const box: any = msgBox.value
    if (box) box.scrollTop = box.scrollHeight
  })
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.session-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-bottom: 1px solid #ebeef5;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
  background: #f0f2f5;
}
.empty { padding: 40px 0; }
.msg-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.user-avatar {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: white;
}
.ai-avatar {
  background: #f0f9ff;
  border: 1px solid #e0efff;
}
.bubble-wrap { display: flex; flex-direction: column; max-width: 75%; }
.msg-row.user .bubble-wrap { align-items: flex-end; }
.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.bubble.user {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: white;
  border-bottom-right-radius: 4px;
}
.bubble.assistant {
  background: white;
  color: #303133;
  border-bottom-left-radius: 4px;
}
.bubble.streaming {
  background: white;
}
.user-text { white-space: pre-wrap; }
.msg-time {
  font-size: 10px;
  color: #c0c4cc;
  margin-top: 4px;
  padding: 0 4px;
}
.cursor { animation: blink 1s infinite; }
@keyframes blink { 0%, 50% { opacity: 1; } 50%, 100% { opacity: 0; } }
.input-bar {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-top: 1px solid #ebeef5;
  align-items: flex-end;
}
.input-field {
  flex: 1;
  background: #f5f7fa;
  border-radius: 20px;
  padding: 8px 14px;
}
.input-field :deep(.van-field__control) {
  font-size: 14px;
}
.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.send-btn :deep(.van-button__content) {
  padding: 0;
}
</style>
