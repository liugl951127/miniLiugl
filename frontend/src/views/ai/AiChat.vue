<template>
  <PageContainer title="AI 智能助手" subtitle="自研 AI 引擎 · 0 外部依赖 · 13 种意图识别" icon="🤖">
    <el-row :gutter="16">
      <!-- 左侧: 历史会话 -->
      <el-col :span="6">
        <el-card shadow="never" class="chat-side">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>💬 会话</span>
              <el-button size="small" type="primary" @click="newSession">+ 新建</el-button>
            </div>
          </template>
          <StateBlock v-if="loadingSessions" type="loading" message="加载中..." />
          <div v-else class="session-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              :class="['session-item', { active: currentSessionId === s.id }]"
              @click="loadSession(s)"
            >
              <div class="session-title">{{ s.title || '新会话' }}</div>
              <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
            </div>
            <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 主区: 对话 -->
      <el-col :span="18">
        <el-card shadow="never">
          <template #header>
            <div class="chat-header">
              <span>🚀 智能路由 (Ctrl+Enter 发送)</span>
              <el-tag size="small" :type="lastResult ? 'success' : 'info'">
                {{ lastResult ? `意图: ${lastResult.intent}` : '等待输入' }}
              </el-tag>
            </div>
          </template>

          <div class="quick-actions">
            <span class="quick-label">快捷:</span>
            <el-tag v-for="ex in examples" :key="ex.text" class="quick-tag" effect="plain" @click="fillExample(ex.text)">
              {{ ex.icon }} {{ ex.label }}
            </el-tag>
          </div>

          <div class="messages" ref="messagesRef">
            <div v-for="(m, i) in messages" :key="i" :class="['message', `msg-${m.role}`]">
              <div class="message-avatar">
                {{ m.role === 'user' ? '👤' : '🤖' }}
              </div>
              <div class="message-content">
                <div class="message-bubble" v-html="formatMsg(m.content)"></div>
                <div v-if="m.intent" class="message-meta">
                  <el-tag size="small" :type="m.intent === 'UNKNOWN' ? 'warning' : 'success'">
                    {{ m.intent }}
                  </el-tag>
                  <span v-if="m.handler" class="meta-handler">{{ m.handler }}</span>
                </div>
                <div v-if="m.data" class="message-extra">
                  <pre>{{ JSON.stringify(m.data, null, 2) }}</pre>
                </div>
              </div>
            </div>
            <StateBlock v-if="loading" type="loading" message="AI 思考中..." />
            <el-empty v-if="!messages.length && !loading" description="开始你的第一次对话吧 🚀" :image-size="80" />
          </div>

          <el-input
            v-model="userInput"
            type="textarea"
            :rows="3"
            placeholder="试试: 画一个统计 user 表的柱状图 / 生成 8 小节 C 大调音乐 / 转人工 / 生成 Spring Boot 项目"
            @keydown.ctrl.enter="handleSend"
            :disabled="loading"
          />
          <div class="actions">
            <el-button :disabled="!messages.length" @click="clearAll">🗑 清空</el-button>
            <el-button type="primary" :loading="loading" @click="handleSend" size="large">
              🚀 发送
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </PageContainer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StateBlock from '@/components/StateBlock.vue'
import { dispatchPrompt, listAiSessions, createAiSession } from '@/api/ai'

const examples = [
  { icon: '📊', label: '统计图表', text: '统计 user 表前 10 条, 柱状图' },
  { icon: '🥧', label: '饼图', text: '画一个产品销量饼图, 苹果香蕉橙子, 占比 50/30/20' },
  { icon: '🎵', label: '音乐', text: '生成 C 大调 120bpm 8 小节音乐' },
  { icon: '💻', label: '代码', text: '生成一个 Spring Boot 项目, 叫 demo' },
  { icon: '🙋', label: '转人工', text: '转人工' },
  { icon: '🎨', label: 'AIGC', text: '生成一张蓝色渐变背景图' },
  { icon: '📄', label: '文档', text: '解析文档提取关键词' },
  { icon: '🎬', label: '视频', text: '生成一个 5 秒的视频' }
]

const userInput = ref('')
const loading = ref(false)
const messages = ref([])
const lastResult = ref(null)
const messagesRef = ref()

// 会话管理
const sessions = ref([])
const currentSessionId = ref(null)
const loadingSessions = ref(false)

async function refreshSessions() {
  loadingSessions.value = true
  try {
    const res = await listAiSessions()
    sessions.value = res.data || []
  } catch (e) {
    // 静默
  } finally {
    loadingSessions.value = false
  }
}

async function newSession() {
  try {
    const res = await createAiSession({ title: '新会话 ' + new Date().toLocaleString() })
    currentSessionId.value = res.data?.id
    messages.value = []
    lastResult.value = null
    await refreshSessions()
    ElMessage.success('新会话已创建')
  } catch (e) {
    // 失败也允许继续
    currentSessionId.value = null
    messages.value = []
  }
}

function loadSession(s) {
  currentSessionId.value = s.id
  messages.value = s.messages || []
  lastResult.value = null
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return d.toLocaleDateString()
}

function fillExample(text) {
  userInput.value = text
  ElMessage.info('已填入, 按 Ctrl+Enter 发送')
}

async function handleSend() {
  const text = userInput.value.trim()
  if (!text) {
    ElMessage.warning('请输入内容')
    return
  }
  loading.value = true
  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  await nextTick()
  scrollToBottom()

  try {
    const res = await dispatchPrompt(text, currentSessionId.value)
    const r = res.data
    lastResult.value = r
    messages.value.push({
      role: 'assistant',
      content: r.message || `已处理 (${r.intent})`,
      intent: r.intent,
      handler: r.handler,
      data: r.data
    })
    if (r.sessionId) currentSessionId.value = r.sessionId
    await refreshSessions()
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '❌ 错误: ' + (e.message || '未知') })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function clearAll() {
  messages.value = []
  lastResult.value = null
  ElMessage.success('已清空')
}

function formatMsg(c) {
  if (!c) return ''
  return c.replace(/\n/g, '<br>')
}

refreshSessions()
</script>

<style scoped>
.chat-side { height: 600px; }
.session-list { max-height: 500px; overflow-y: auto; }
.session-item {
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; border-left: 3px solid #409EFF; }
.session-title { font-size: 14px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-time { font-size: 11px; color: #909399; margin-top: 2px; }

.chat-header { display: flex; justify-content: space-between; align-items: center; }
.quick-actions { margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.quick-label { font-size: 13px; color: #909399; margin-right: 4px; }
.quick-tag { cursor: pointer; }
.quick-tag:hover { transform: scale(1.05); }

.messages {
  height: 400px;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 12px;
}
.message { display: flex; gap: 8px; margin-bottom: 16px; }
.message-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.message-content { flex: 1; min-width: 0; }
.msg-user .message-bubble { background: #409EFF; color: white; }
.msg-assistant .message-bubble { background: white; border: 1px solid #ebeef5; }
.message-bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 8px;
  max-width: 80%;
  word-break: break-word;
  line-height: 1.6;
}
.message-meta { margin-top: 4px; font-size: 12px; }
.meta-handler { color: #909399; margin-left: 8px; }
.message-extra {
  margin-top: 8px;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 11px;
  max-height: 200px;
  overflow: auto;
}
.message-extra pre { margin: 0; white-space: pre-wrap; }
.actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
</style>
