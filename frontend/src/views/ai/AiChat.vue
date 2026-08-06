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
          {{ lastResult ? lastResult.intent : '等待输入' }}
        </el-tag>
        <!-- Day 32: 投票模式切换 -->
        <el-tooltip content="开启后强制多模型投票" placement="bottom">
          <el-button :type="votingMode ? 'warning' : 'default'" size="large" @click="votingMode = !votingMode">
            🗳 投票 {{ votingMode ? 'ON' : 'OFF' }}
          </el-button>
        </el-tooltip>
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
              <EmptyState :description="'暂无数据'" />
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
                <EmptyState :description="'暂无数据'" />
              </div>
              <ChatBubble
                v-for="m in messages"
                :key="m.id"
                :message="m"
                :streaming="m.streaming"
                :is-user="m.role === 'user'"
              />
              <div v-if="loading" class="typing">
                <el-icon class="is-loading"><Loading /></el-icon>
                {{ t('aichat.typing') }}
              </div>
            </div>

            <!-- Day 32: 投票结果面板 -->
            <div v-if="votingResults?.meta?.votingTriggered" class="voting-panel">
              <div class="voting-header">
                <span class="voting-tag">🗳 投票触发</span>
                <el-tag size="small" type="warning">{{ votingResults.meta.votingStrategy }}</el-tag>
                <span class="voting-stat">⏱ {{ votingResults.meta.votingElapsedMs }}ms</span>
                <span class="voting-stat">📊 一致率: <strong>{{ ((votingResults.meta.agreementScore || 0) * 100).toFixed(0) }}%</strong></span>
                <span class="voting-stat">🤖 {{ votingResults.meta.modelCount }} 模型</span>
              </div>
              <!-- 各模型答案 -->
              <div class="model-answers">
                <div v-for="(ans, i) in votingResults.meta.modelAnswers" :key="i" class="model-answer-item">
                  <div class="model-answer-header">
                    <el-tag size="small" :type="i === 0 ? 'primary' : 'info'">
                      {{ ans.model || 'Model ' + (i+1) }}
                    </el-tag>
                    <span class="model-provider">{{ ans.provider }}</span>
                    <span class="model-latency">{{ ans.latencyMs }}ms</span>
                    <span v-if="ans.error" class="model-error">❌ {{ ans.error }}</span>
                  </div>
                  <div v-if="ans.answer" class="model-answer-content">{{ ans.answer }}</div>
                </div>
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
                <el-button @click="clearAll" :disabled="loading">清空</el-button>
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
import { ref, nextTick, _reactive } from 'vue'
import { _useToast } from '@/composables/useToast'
import { _useI18n } from 'vue-i18n'

import StateBlock from '@/components/StateBlock.vue'
import { _votingChat, forceVotingChat, votingInfo, chatStream, listAiSessions, createAiSession } from '@/api/ai'
import EmptyState from '@/components/EmptyState.vue'

const _examples = [
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

// Day 32: 投票对话状态
const votingMode = ref(false)   // true = 强制多模型投票
const votingInfo_ = ref(null)   // 投票配置信息
const votingResults = ref(null) // 最近投票结果
const quickActions = ref([
  { label: '📊 统计', text: '统计 user 表前 10 条, 柱状图' },
  { label: '🥧 饼图', text: '画一个产品销量饼图' },
  { label: '💻 代码', text: '生成一个 Spring Boot 项目, 叫 demo' },
  { label: '🎨 AIGC', text: '生成一张蓝色渐变背景图' },
])

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
    loadVotingInfo()
    toast.success('新会话已创建')
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

function _fillExample(text) {
  userInput.value = text
  toast.info('已填入, 按 Ctrl+Enter 发送')
}

async function loadVotingInfo() {
  try {
    const { data } = await votingInfo()
    votingInfo_.value = data.data || data
  } catch (_) { votingInfo_.value = null }
}

async function handleSend() {
  const text = userInput.value.trim()
  if (!text) { toast.warning('请输入内容'); return }

  loading.value = true
  votingResults.value = null
  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  await nextTick()
  scrollToBottom()

  try {
    if (votingMode.value) {
      // 强制投票模式 — 走 REST，等完整响应
      const res = await forceVotingChat({ text, sessionId: currentSessionId.value })
      const body = res.data
      const resp = body.response || body
      const meta = body.meta || {}
      votingResults.value = body
      messages.value.push({
        role: 'assistant',
        content: resp.content || '(无内容)',
        model: resp.model,
        votingMeta: meta
      })
      if (meta.votingTriggered) {
        lastResult.value = { intent: `投票(${meta.votingStrategy || 'AUTO'}) 一致率 ${((meta.agreementScore || 0) * 100).toFixed(0)}%` }
      } else {
        lastResult.value = { intent: meta.confidence > 0.8 ? '高置信' : '中置信' }
      }
      if (resp.sessionId) currentSessionId.value = resp.sessionId
    } else {
      // Day 34: 正常模式 — SSE 流式响应
      const assistantMsg = { role: 'assistant', content: '', streaming: true, model: '' }
      messages.value.push(assistantMsg)

      await new Promise((resolve, reject) => {
        chatStream(
          { text, sessionId: currentSessionId.value },
          (chunk) => {
            assistantMsg.content += chunk
            scrollToBottom()
          },
          (err) => {
            assistantMsg.content += '\n❌ 流式错误: ' + (err?.message || err)
            assistantMsg.streaming = false
            reject(err)
          },
          () => {
            assistantMsg.streaming = false
            lastResult.value = { intent: '高置信' }
            resolve()
          }
        )
      })

      if (assistantMsg.content) {
        await refreshSessions()
        loadVotingInfo()
      }
    }
  } catch (e) {
    if (e?.message !== 'HTTP 401') {
      messages.value.push({ role: 'assistant', content: '❌ 错误: ' + (e?.message || '未知') })
    }
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
  toast.success('已清空')
}

function _formatMsg(c) {
  if (!c) return ''
  return c.replace(/\n/g, '<br>')
}

refreshSessions()
loadVotingInfo()


// 发送消息（快捷键 Ctrl+Enter）
function send() { handleSend() }



// 快捷操作
function _sendQuickAction(label) {
  const qa = quickActions.value.find(a => a.label === label)
  if (qa) userInput.value = qa.text
}

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
.voting-panel {
  margin-bottom: 12px;
  border: 1px solid #e6a23c;
  border-radius: 8px;
  background: #fef9f0;
  padding: 12px;
}
.voting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.voting-tag {
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
}
.voting-stat {
  font-size: 12px;
  color: #666;
  margin-left: 8px;
}
.model-answers {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.model-answer-item {
  background: white;
  border-radius: 6px;
  padding: 8px 10px;
  border: 1px solid #eee;
}
.model-answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.model-provider {
  font-size: 11px;
  color: #999;
}
.model-latency {
  font-size: 11px;
  color: #409eff;
  margin-left: auto;
}
.model-error {
  font-size: 11px;
  color: #f56c6c;
}
.model-answer-content {
  font-size: 12px;
  color: #333;
  line-height: 1.6;
  max-height: 80px;
  overflow: auto;
}
/* Day 35: 优化流式加载态 */
.typing {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  color: #6366f1;
  font-size: 13px;
  animation: typing-fade 1.5s ease-in-out infinite;
}
@keyframes typing-fade {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
