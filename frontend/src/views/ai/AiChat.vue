<!--
  @file views/ai/AiChat.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/AiChat.vue (AI 聊天对话 (AiChat))
  @version V3.5.12+ (前端注释补全)
  @description AI 聊天对话 (AiChat)
-->
<template>
  <div class="page-ai-chat">
    <!-- 1. page-header: 标题 + 副标题 -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">🤖 {{ t('aichat.title') }}</h2>
        <p class="page-subtitle">自研 AI 引擎 · 0 外部依赖 · 13 种意图识别</p>
      </div>
      <el-button-group>
        <el-tag size="large" :type="lastResult ? 'success' : 'info'">
          {{ lastResult ? `意图: ${lastResult.intent}` : '等待输入' }}
        </el-tag>
        <el-button :icon="Plus" @click="newSession" type="primary">{{ t('aichat.new') }}</el-button>
      </el-button-group>
    </header>

    <el-row :gutter="16">
      <!-- 2. section: 左侧会话列表 -->
      <el-col :xs="24" :md="6">
        <section class="section side-section">
          <h3 class="section-title">💬 会话</h3>
          <el-card shadow="hover" class="side-card">
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
              <el-empty v-if="!sessions.length" description="{{ t('aichat.empty') }}" :image-size="60" />
            </div>
          </el-card>
        </section>
      </el-col>

      <!-- 3. section: 主对话区 -->
      <el-col :xs="24" :md="18">
        <section class="section chat-section">
          <h3 class="section-title">🚀 智能路由 (Ctrl+Enter 发送)</h3>
          <el-card shadow="hover" class="chat-card">
            <div class="quick-actions">
              <el-button v-for="qa in quickActions" :key="qa.text" size="small" @click="sendQuick(qa.text)">
                {{ qa.label }}
              </el-button>
            </div>

            <div class="messages" ref="messagesRef">
              <div v-if="!messages.length" class="empty-chat">
                <el-empty description="{{ t('aichat.start') }}" :image-size="80" />
              </div>
              <ChatBubble
                v-for="m in messages"
                :key="m.id"
                :message="m"
                :is-user="m.role === 'user'"
              />
              <div v-if="loading" class="typing">
                <el-icon class="is-loading"><Loading /></el-icon>
                {{ t('aichat.typing') }}
              </div>
            </div>

            <div class="input-bar">
              <el-input
                v-model="input"
                type="textarea"
                :rows="3"
                :placeholder="t('aichat.placeholder')"
                @keydown.ctrl.enter="send"
                :disabled="loading"
              />
              <div class="input-actions">
                <el-button @click="clear" :disabled="loading">清空</el-button>
                <el-button type="primary" :loading="loading" @click="send">发送</el-button>
              </div>
            </div>
          </el-card>
        </section>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
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
