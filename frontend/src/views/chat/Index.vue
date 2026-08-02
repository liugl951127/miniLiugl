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
  <div class="page-chat" :class="[`font-${fontSize}`]">
    <!-- 1. page-header: 标题 + 模型选择 + 操作 -->
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
      <div class="brand-info">
        <el-icon :size="20"><ChatDotRound /></el-icon>
        <div>
          <h2 class="page-title">AI 对话</h2>
          <el-text type="info" size="small" class="page-subtitle" truncated>
  {{ modelLabel }} · {{ sessionId || '新会话' }}
</el-text>
        </div>
      </div>
      <el-button-group>
        <!-- V3.5.98+ RAG 知识库选择器 -->
        <el-select v-if="showRag" v-model="ragId" size="small" class="rag-select" placeholder="知识库" clearable @change="onRagChange">
          <el-option v-for="k in knowledgeBases" :key="k.id" :label="k.name" :value="k.id">
            <span style="float: left">{{ k.name }}</span>
            <el-tag size="small" type="info" style="float: right">{{ k.docCount }} 文档</el-tag>
          </el-option>
        </el-select>

        <!-- V3.6.6+ Agent 模式多选 (Chat + Agent + RAG + Flow 组合) -->
        <el-checkbox-group v-model="agentMode" size="small" class="agent-modes" @change="onAgentModeChange">
          <el-checkbox-button
            v-for="m in agentModes"
            :key="m.key"
            :value="m.key"
            :label="m.key"
          >
            <el-icon style="margin-right: 4px"><component :is="m.icon" /></el-icon>
            {{ m.label }}
          </el-checkbox-button>
        </el-checkbox-group>

        <!-- V3.6.1+ el-segmented 模型选择器 (移动端 P0 替代 el-select) -->
        <el-segmented
          v-model="modelKey"
          :options="modelOptions"
          size="small"
          class="model-segmented"
          @change="onModelChange"
        />
        <el-button :icon="Plus" @click="newChat" plain>新对话</el-button>
        <el-button :icon="Folder" @click="drawerVisible = true" plain>历史</el-button>
        <el-button :icon="Delete" @click="clearAll" plain>清空</el-button>
        <!-- V3.6.2+ 导出按钮 -->
        <el-dropdown @command="onExport" size="small">
          <el-button :icon="Download" plain>
            导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="markdown">📝 Markdown</el-dropdown-item>
              <el-dropdown-item command="json">📋 JSON</el-dropdown-item>
              <el-dropdown-item command="txt">📄 纯文本</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-button-group>
    </header>

  <!-- V3.6.9+ 语音通话面板 -->
  <transition name="call-fade">
    <div v-if="speechCall.isCallActive.value" class="call-panel">
      <div class="call-header">
        <span class="call-status">
          <span class="call-dot" :class="`state-${speechCall.state.value}`"></span>
          {{ speechCall.stateLabel.value }} · {{ speechCall.callDurationFormatted.value }}
        </span>
        <el-space :size="6">
          <el-button size="small" :type="speechCall.isMuted.value ? 'warning' : 'default'" @click="speechCall.toggleMute()">
            {{ speechCall.isMuted.value ? '取消静音' : '静音' }}
          </el-button>
          <el-button size="small" type="danger" @click="speechCall.stop()">挂断</el-button>
        </el-space>
      </div>
      <div class="call-body">
        <div class="volume-bar">
          <div
            v-for="i in 20"
            :key="i"
            class="volume-bar-cell"
            :class="{ active: i <= Math.ceil(speechCall.volume.value / 5) }"
          ></div>
        </div>
        <div class="call-text">
          <div v-if="speechCall.interimText.value" class="call-interim">
            🎤 {{ speechCall.interimText.value }}
          </div>
          <div v-else-if="speechCall.finalText.value" class="call-final">
            ✓ {{ speechCall.finalText.value }}
          </div>
          <div v-else class="call-hint">请说话...</div>
        </div>
      </div>
    </div>
  </transition>


    <!-- 2. section: 消息区 (流式 + Markdown + 工具调用) -->
    <section class="section chat-section">
      <h3 class="section-title">💬 对话</h3>
      <el-card shadow="hover" class="messages-card">
        <div class="messages" ref="messagesRef">
          <div v-if="!messages.length" class="empty-chat">
            <el-empty :description="t('chat.start')" :image-size="100" />
            <div class="quick-prompts">
              <el-button v-for="qa in quickPrompts" :key="qa" size="small" @click="input = qa; send()">
                {{ qa }}
              </el-button>
            </div>
          </div>
          <ChatMessage
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
      <el-card>
    <!-- V3.7.1+ 打字机进度条 + 暂停/继续 (独立控制条) -->
    <section v-if="typewriterEnabled && typewriterTyping" class="section typewriter-controls-global">
      <el-card shadow="hover" class="typewriter-card">
        <div class="typewriter-row">
          <span class="typewriter-label">⌨️ 打字机</span>
          <el-progress
            :percentage="typewriterProgress"
            :stroke-width="8"
            :show-text="true"
            :format="() => `${typewriterProgress}% (${typewriterIndex}/${typewriterContent.length})`"
            style="flex: 1; margin: 0 16px"
            :status="typewriterPaused ? 'warning' : 'success'"
          />
          <el-button-group>
            <el-button
              :icon="typewriterPaused ? VideoPlay : VideoPause"
              :type="typewriterPaused ? 'primary' : 'default'"
              @click="typewriterPaused ? resumeTypewriter() : pauseTypewriter()"
              size="small"
            >
              {{ typewriterPaused ? '继续' : '暂停' }}
            </el-button>
            <el-button :icon="Close" @click="stopTypewriter" size="small">停止</el-button>
          </el-button-group>
        </div>
      </el-card>
    </section>
 shadow="hover" class="input-card">
        <el-space :size="8" wrap class="input-toolbar">
          <el-checkbox v-model="useStream">流式</el-checkbox>
          <el-checkbox v-model="useTools">{{ t('chat.tools') }}</el-checkbox>
          <el-checkbox v-model="useRag">RAG</el-checkbox>
          <!-- V3.6.7+ 响应深度 (el-segmented 单选) -->
          <el-segmented
            v-model="responseDepth"
            :options="depthOptions"
            size="small"
            class="depth-segmented"
            @change="onDepthChange"
          />
          <!-- V3.6.8+ 字体大小 -->
          <el-segmented
            v-model="fontSize"
            :options="fontSizeOptions"
            size="small"
            class="font-size-segmented"
          />
          <!-- V3.6.9+ 语音通话 -->
          <el-tooltip content="语音通话 (V3.6.9+)" placement="top">
            <el-button
              :icon="Phone"
              circle
              :type="speechCall.isCallActive.value ? 'danger' : 'primary'"
              size="small"
              @click="toggleCall"
            />
          </el-tooltip>
          <!-- V3.6.0+ 语音播报开关 -->
          <el-checkbox v-model="autoSpeak">
            🔊 TTS
            <el-tag v-if="ttsSpeaking" size="small" type="success" effect="dark" class="tts-tag">
              播报中...
            </el-tag>
          </el-checkbox>
          <el-button
            v-if="ttsSupported && !autoSpeak"
            :icon="ttsSpeaking ? VideoPause : VideoPlay"
            size="small"
            plain
            @click="toggleTTSTest"
            title="试听 TTS 播报"
          />
          <!-- V3.6.1+ OCR 识别按钮 -->
          <el-button
            :icon="Document"
            size="small"
            plain
            :loading="ocrProcessing"
            @click="triggerOCR"
            title="OCR 识别图片文字"
          >
            OCR
          </el-button>
          <!-- V3.6.5+ 浏览器通知开关 -->
          <el-button
            v-if="notificationSupported"
            :icon="notificationEnabled ? BellFilled : Bell"
            :type="notificationEnabled ? 'success' : 'default'"
            size="small"
            plain
            @click="requestNotificationPermission"
            :title="notificationEnabled ? '通知已开启' : '点击开启浏览器通知'"
          >
            {{ notificationEnabled ? '🔔' : '🔕' }}
          </el-button>
        </el-space>
        <!-- V3.6.1+ OCR 状态面板 -->
        <transition name="slide-up">
          <div v-if="ocrProcessing || ocrText" class="ocr-panel">
            <el-icon class="ocr-icon" :class="{ processing: ocrProcessing }">
              <Document />
            </el-icon>
            <div class="ocr-content">
              <div v-if="ocrProcessing" class="ocr-status">
                <el-progress :percentage="ocrProgress" :stroke-width="6" />
                <span class="ocr-hint">正在识别图片文字... {{ ocrProgress }}%</span>
              </div>
              <div v-else-if="ocrText" class="ocr-result">
                <el-text type="success" size="small" truncated>✓ OCR 完成, 已追加到输入框</el-text>
                <div class="ocr-text-preview">{{ ocrText.slice(0, 80) }}{{ ocrText.length > 80 ? '...' : '' }}</div>
              </div>
            </div>
            <el-button text :icon="CircleClose" @click="ocrText = ''; ocrProgress = 0" size="small">关闭</el-button>
          </div>
        </transition>

        <!-- V3.5.99+ 语音输入按钮 -->
        <div class="input-row">
          <el-input
            v-model="input"
            type="textarea"
            :rows="4"
            :placeholder="t('chat.placeholder')"
            @keydown.enter.exact.prevent="send"
            :disabled="loading"
            class="input-textarea"
          />
          <el-button
            :icon="voiceRecording ? VideoPause : Microphone"
            :type="voiceRecording ? 'danger' : 'default'"
            :loading="voiceProcessing"
            @click="toggleVoice"
            :title="voiceRecording ? '点击停止' : '点击开始语音输入'"
            size="large"
            circle
            class="voice-btn"
          />
        </div>

        <!-- V3.5.99+ 语音识别状态面板 -->
        <transition name="slide-up">
          <div v-if="voiceRecording || voiceResult" class="voice-panel">
            <el-icon class="voice-icon" :class="{ recording: voiceRecording }">
              <Microphone />
            </el-icon>
            <div class="voice-content">
              <div v-if="voiceRecording" class="voice-status">
                <span class="voice-dot"></span>
                正在聆听... {{ voiceInterim }}
              </div>
              <div v-else-if="voiceResult" class="voice-result">
                ✓ 识别完成: {{ voiceResult }}
              </div>
            </div>
            <el-button text :icon="CircleClose" @click="resetVoice" size="small">关闭</el-button>
          </div>
        </transition>

        <div class="input-actions">
          <el-text type="info" size="small" class="hint">内容由 AI 生成, 仅供参考</el-text>
          <el-button :icon="Refresh" @click="regenerate" :disabled="loading || !messages.length">{{ t('chat.regenerate') }}</el-button>
          <el-button :icon="loading ? Loading : Promotion" :loading="loading" @click="send" type="primary">
            {{ loading ? '停止' : '发送' }}
          </el-button>
        </div>
      </el-card>
    </section>

    <!-- 5. section: 历史会话抽屉 (V3.6.3+ 搜索增强) -->
    <el-drawer v-model="drawerVisible" title="历史会话" direction="rtl" size="380px">
      <div class="drawer-sessions">
        <!-- V3.6.3+ 搜索框 -->
        <el-input
          v-model="searchKw"
          placeholder="搜索标题 / 消息 / 日期"
          :prefix-icon="Search"
          clearable
          size="small"
          class="session-search"
        />
        <el-checkbox v-model="searchInContent" size="small" class="session-search-content">
          搜索消息内容
        </el-checkbox>

        <!-- V3.6.3+ 搜索结果统计 -->
        <div v-if="searchKw" class="search-stats">
          <el-tag size="small" type="info">
            {{ filteredSessions.length }} / {{ sessions.length }} 个会话
          </el-tag>
          <el-button text size="small" @click="searchKw = ''">清除</el-button>
        </div>

        <div
          v-for="s in filteredSessions"
          :key="s.id"
          :class="['drawer-session', { active: sessionId === s.id }]"
          @click="loadSession(s); drawerVisible = false"
        >
          <div class="session-title">{{ s.title || '新会话' }}</div>
          <div class="session-time">{{ formatSessionTime(s.updatedAt || s.createdAt) }}</div>
          <div v-if="s.preview" class="session-preview">{{ truncate(s.preview, 60) }}</div>
        </div>
        <el-empty v-if="!filteredSessions.length && sessions.length" :description="`未找到 '${searchKw}' 匹配的会话`" />
        <el-empty v-if="!sessions.length" :description="t('chat.empty.history')" />
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { modelApi } from '@/api/model'
import { listSessions, createSession, deleteSession as deleteSessionApi } from '@/api/session'
import { useBusinessStream } from '@/composables/useBusinessStream'
import ChatMessage from '@/components/ChatMessage.vue'
import { useSpeechCall } from '@/composables/useSpeechCall'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  EditPen, Search, ChatDotRound, MoreFilled, Promotion, Cpu, Clock, MagicStick,
  UploadFilled, Picture, Loading, VideoPause, VideoPlay, CircleCloseFilled, Document, Share,
  Microphone, CircleClose, Headset, Download, ArrowDown, Bell, BellFilled, Phone,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const userStore = useUserStore()
const toast = useToast()
const route = useRoute()

// V3.6.9+ 语音通话
const speechCall = useSpeechCall()

// V3.6.16+ 语音交互链路
// STT 完成 → 调 sendMessage → 流式字符时打字机 + TTS 同步
speechCall.setCallbacks({
  onRecognized: (text) => {
    // STT 识别完成, 把识别到的文字填入输入框
    inputMessage.value = text
    toast.info(`识别: ${text.slice(0, 20)}...`)
  },
})

// 状态
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const selectedModel = ref('mock')

// === V3.6.1+ 模型选择器 (el-segmented) ===
const modelOptions = computed(() => models.value.map(m => ({
  label: m.label,
  value: m.key,
  // el-segmented 支持自定义渲染, 但只用 label 也行
})))

// === V3.5.98+ RAG 知识库 ===
const showRag = ref(true)
const ragId = ref(null)
const knowledgeBases = ref([
  { id: 1, name: '产品手册',     docCount: 128 },
  { id: 2, name: '技术文档',     docCount: 256 },
  { id: 3, name: '用户 FAQ',     docCount: 64 },
  { id: 4, name: '行业知识',     docCount: 512 },
  { id: 5, name: '代码片段库',   docCount: 1024 },
])
function onRagChange(id) {
  if (id) toast.success(`📚 已启用 RAG 检索: ${knowledgeBases.value.find(k => k.id === id)?.name}`)
}

// === V3.5.99+ 语音输入 (Web Speech API) ===
const voiceSupported = ref(false)
const voiceRecording = ref(false)
const voiceProcessing = ref(false)
const voiceResult = ref('')
const voiceInterim = ref('')
let voiceRecognition = null

function checkVoiceSupport() {
  if (typeof window === 'undefined') return
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  voiceSupported.value = !!SR
}

function initVoice() {
  if (!voiceSupported.value) return null
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  const recognition = new SR()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = false
  recognition.maxAlternatives = 1

  recognition.onstart = () => {
    voiceRecording.value = true
    voiceInterim.value = ''
    voiceResult.value = ''
  }
  recognition.onresult = (e) => {
    let interim = ''
    let final = ''
    for (let i = e.resultIndex; i < e.results.length; i++) {
      const transcript = e.results[i][0].transcript
      if (e.results[i].isFinal) {
        final += transcript
      } else {
        interim += transcript
      }
    }
    if (interim) voiceInterim.value = interim
    if (final) {
      voiceResult.value = final.trim()
      input.value = input.value + (input.value ? ' ' : '') + final.trim()
    }
  }
  recognition.onerror = (e) => {
    console.error('语音识别错误:', e.error)
    toast.error(`语音识别失败: ${e.error}`)
    voiceRecording.value = false
    voiceProcessing.value = false
  }
  recognition.onend = () => {
    voiceRecording.value = false
    voiceProcessing.value = false
    setTimeout(() => {
      voiceResult.value = ''
      voiceInterim.value = ''
    }, 2000)
  }
  return recognition
}

async function toggleVoice() {
  if (!voiceSupported.value) {
    toast.warning('当前浏览器不支持语音输入 (Chrome/Edge/Safari 支持)')
    return
  }
  if (voiceRecording.value) {
    voiceProcessing.value = true
    voiceRecognition?.stop()
  } else {
    voiceRecognition = initVoice()
    if (voiceRecognition) {
      try {
        voiceRecognition.start()
      } catch (e) {
        toast.error('启动语音失败: ' + e.message)
        voiceProcessing.value = false
      }
    }
  }
}

function resetVoice() {
  if (voiceRecording.value) voiceRecognition?.stop()
  voiceRecording.value = false
  voiceProcessing.value = false
  voiceResult.value = ''
  voiceInterim.value = ''
}

// === V3.6.0+ 语音播报 (TTS, Web Speech API speechSynthesis) ===
const ttsSupported = ref(false)
const ttsSpeaking = ref(false)
const autoSpeak = ref(false)
let ttsUtterance = null

function checkTTSSupport() {
  if (typeof window === 'undefined') return
  ttsSupported.value = 'speechSynthesis' in window
}

function speak(text) {
  if (!ttsSupported.value || !text) return
  // 停止之前的播报
  window.speechSynthesis.cancel()

  // 清理 markdown / HTML 标签
  const cleanText = text
    .replace(/<[^>]+>/g, '')      // 去除 HTML
    .replace(/[*#_`>~\-]+/g, '') // 去除 markdown
    .replace(/\[(.+?)\]\(.+?\)/g, '$1')  // 去除链接
    .replace(/!\[(.+?)\]\(.+?\)/g, '$1') // 去除图片
    .trim()

  if (!cleanText) return

  ttsUtterance = new SpeechSynthesisUtterance(cleanText)
  ttsUtterance.lang = 'zh-CN'
  ttsUtterance.rate = 1.0
  ttsUtterance.pitch = 1.0
  ttsUtterance.volume = 1.0

  ttsUtterance.onstart = () => { ttsSpeaking.value = true }
  ttsUtterance.onend = () => { ttsSpeaking.value = false }
  ttsUtterance.onerror = (e) => {
    console.error('TTS 错误:', e.error)
    ttsSpeaking.value = false
  }

  window.speechSynthesis.speak(ttsUtterance)
}

function stopSpeak() {
  if (ttsSupported.value) {
    window.speechSynthesis.cancel()
    ttsSpeaking.value = false
  }
}

function toggleTTSTest() {
  if (ttsSpeaking.value) {
    stopSpeak()
  } else {
    speak('你好, 我是 Liugl-AI 智能助手, 有什么可以帮你的吗?')
  }
}

// === V3.6.3+ 工具函数 ===
function formatSessionTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  // 1 天内: HH:MM
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  // 7 天内: N 天前
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  // 否则: YYYY-MM-DD
  return d.toLocaleDateString('zh-CN')
}
function truncate(s, n) {
  return s && s.length > n ? s.substring(0, n) + '...' : s
}

// === V3.6.5+ 浏览器通知 (Notification API + PWA) ===
const notificationSupported = ref(false)
const notificationEnabled = ref(false)
let notificationPermission = 'default'

function checkNotificationSupport() {
  if (typeof window === 'undefined') return
  notificationSupported.value = 'Notification' in window
  if (notificationSupported.value) {
    notificationPermission = Notification.permission
    notificationEnabled.value = notificationPermission === 'granted'
  }
}

async function requestNotificationPermission() {
  if (!notificationSupported.value) {
    toast.warning('当前浏览器不支持通知')
    return
  }
  if (notificationPermission === 'granted') {
    notificationEnabled.value = true
    toast.success('通知已开启')
    return
  }
  try {
    const permission = await Notification.requestPermission()
    notificationPermission = permission
    notificationEnabled.value = permission === 'granted'
    if (permission === 'granted') {
      toast.success('通知已授权 (AI 答完会发通知)')
    } else if (permission === 'denied') {
      toast.warning('通知被拒绝, 请在浏览器设置中开启')
    } else {
      toast.info('通知未授权')
    }
  } catch (e) {
    toast.error('请求通知权限失败: ' + e.message)
  }
}

function showNotification(title, body) {
  if (!notificationEnabled.value || !notificationSupported.value) return
  if (document.visibilityState === 'visible') return  // 页面可见不通知
  try {
    new Notification(title, {
      body,
      icon: '/icons/icon-192.svg',
      badge: '/icons/icon-192.svg',
      tag: 'minimax-chat',
      requireInteraction: false,
    })
  } catch (e) {
    console.warn('通知失败:', e)
  }
}

// === V3.6.2+ 导出 (Markdown / JSON / 纯文本) ===
function onExport(format) {
  if (!messages.value.length) {
    toast.warning('暂无消息可导出')
    return
  }

  let content = ''
  let filename = `chat-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}`
  let mime = 'text/plain;charset=utf-8'

  if (format === 'markdown') {
    content = messages.value.map(m => {
      const role = m.role === 'user' ? '用户' : 'AI'
      const time = m.createdAt ? new Date(m.createdAt).toLocaleString('zh-CN') : ''
      let body = m.content || ''
      if (m.toolCalls?.length) {
        body += '\n\n<details><summary>工具调用 (' + m.toolCalls.length + ')</summary>\n\n'
        m.toolCalls.forEach(tc => {
          body += `- **${tc.name}** (${tc.status}) - ${tc.duration || 0}ms\n`
        })
        body += '\n</details>'
      }
      if (m.sources?.length) {
        body += '\n\n> 来源:\n'
        m.sources.forEach((s, i) => { body += `> ${i+1}. ${s.title || s.name || '文档'}\n` })
      }
      return `## ${role}\n\n*${time}*\n\n${body}\n`
    }).join('---\n\n')
    filename += '.md'
    mime = 'text/markdown;charset=utf-8'
  } else if (format === 'json') {
    content = JSON.stringify({
      exportedAt: new Date().toISOString(),
      sessionId: currentSessionId.value,
      model: selectedModel.value,
      ragId: ragId.value,
      agentMode: agentMode.value,
      messageCount: messages.value.length,
      messages: messages.value.map(m => ({
        role: m.role, content: m.content, images: m.images,
        toolCalls: m.toolCalls, sources: m.sources,
        createdAt: m.createdAt, status: m.status,
      })),
    }, null, 2)
    filename += '.json'
    mime = 'application/json;charset=utf-8'
  } else {
    content = messages.value.map(m => {
      const role = m.role === 'user' ? '用户' : 'AI'
      const time = m.createdAt ? new Date(m.createdAt).toLocaleString('zh-CN') : ''
      return `[${time}] ${role}:\n${m.content || ''}\n`
    }).join('\n---\n\n')
    filename += '.txt'
  }

  // 触发下载
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  toast.success(`已导出 ${filename} (${format})`)
}

// === V3.6.1+ OCR 图片识别 (Tesseract.js 客户端) ===
const ocrProcessing = ref(false)
const ocrProgress = ref(0)
const ocrText = ref('')
let tesseractWorker = null

async function initOCR() {
  if (tesseractWorker) return tesseractWorker
  try {
    const Tesseract = await import('tesseract.js')
    tesseractWorker = await Tesseract.createWorker('chi_sim+eng', 1, {
      logger: (m) => {
        if (m.status === 'recognizing text') {
          ocrProgress.value = Math.round(m.progress * 100)
        }
      },
    })
    return tesseractWorker
  } catch (e) {
    console.error('OCR 初始化失败:', e)
    toast.error('OCR 初始化失败: ' + e.message)
    return null
  }
}

async function triggerOCR() {
  if (ocrProcessing.value) return

  // 创建隐藏 input 选择图片
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    await performOCR(file)
  }
  input.click()
}

async function performOCR(file) {
  if (!file) return
  ocrProcessing.value = true
  ocrProgress.value = 0
  ocrText.value = ''

  try {
    const worker = await initOCR()
    if (!worker) {
      ocrProcessing.value = false
      return
    }

    const { data } = await worker.recognize(file)
    ocrText.value = data.text.trim()

    if (ocrText.value) {
      // 自动追加到 input
      input.value = input.value + (input.value ? ' ' : '') + ocrText.value
      toast.success(`✓ OCR 识别完成 (${ocrProgress.value}%, ${ocrText.value.length} 字符)`)
    } else {
      toast.warning('OCR 未识别到文字')
    }
  } catch (e) {
    console.error('OCR 错误:', e)
    toast.error('OCR 识别失败: ' + e.message)
  } finally {
    ocrProcessing.value = false
    ocrProgress.value = 0
  }
}

// === V3.5.98+ Agent 模式 ===
// V3.6.6+ 多选模式 (Agent + RAG + Flow 可同时启用)
const agentMode = ref(['chat'])
const enabledModes = computed({
  get: () => agentMode.value,
  set: (v) => { agentMode.value = v }
})
const agentModes = ref([
  { key: 'chat',   label: '💬 普通对话', icon: 'ChatDotRound' },
  { key: 'agent',  label: '🤖 Agent 编排', icon: 'MagicStick' },
  { key: 'rag',    label: '📚 RAG 检索', icon: 'Document' },
  { key: 'flow',   label: '🔀 Flow 流程', icon: 'Share' },
])
// V3.6.9+ 语音通话切换
async function toggleCall() {
  if (speechCall.isCallActive.value) {
    speechCall.stop()
  } else {
    await speechCall.start()
  }
}

function onDepthChange(depth) {
  const label = depthOptions.value.find(d => d.value === depth)?.label || depth
  toast.info(`响应深度: ${label}`)
}

function onAgentModeChange(modes) {
  if (!Array.isArray(modes) || !modes.length) {
    // 至少保留 chat
    agentMode.value = ['chat']
    toast.warning('至少需要保留一个模式')
    return
  }
  const labels = modes.map(k => agentModes.value.find(x => x.key === k)?.label || k).join(' + ')
  toast.success(`已启用: ${labels}`)
}
const models = ref([{ code: 'mock', displayName: 'Mock 模式' }])
const streaming = ref(false)

// V3.7.1+ 打字机进度条 + 暂停/继续
const typewriterEnabled = ref(localStorage.getItem('minimax_typewriter') !== 'false')
const typewriterSpeed = ref(20)
const typewriterPaused = ref(false)
const typewriterProgress = ref(0)
const typewriterTyping = ref(false)
const typewriterIndex = ref(0)
const typewriterContent = ref('')
// V3.7.2+ 打字机真实流式队列
const typewriterQueue = ref<{ msgId: string, target: HTMLElement, fullText: string, onDone?: () => void }[]>([])
const typewriterCurrent = ref<any>(null)

function typewriterEnqueue(msgId: string, target: HTMLElement, fullText: string, onDone?: () => void) {
  typewriterQueue.value.push({ msgId, target, fullText, onDone })
  if (!typewriterTyping.value) typewriterProcessNext()
}

function typewriterProcessNext() {
  if (typewriterPaused.value) return
  const next = typewriterQueue.value.shift()
  if (!next) { typewriterTyping.value = false; typewriterProgress.value = 0; return }
  typewriterCurrent.value = next
  typewriterType(next.target, next.fullText, next.onDone)
}

watch(typewriterPaused, (v) => { if (!v) typewriterProcessNext() })

let typewriterTimer = null

function typewriterAppendChunk(target, chunk) {
  // V3.7.10+ 流式 chunk 集成 (SSE onChunk 段调用)
  if (!target) return
  if (typewriterPaused.value) {
    // 暂停时累积, 不渲染
    typewriterContent.value = (typewriterContent.value || '') + chunk
    return
  }
  // 逐字符渲染
  for (let i = 0; i < chunk.length; i++) {
    if (typewriterPaused.value) {
      // 暂停时停止, 把剩余 chunk 累积
      typewriterContent.value = (typewriterContent.value || '') + chunk.slice(i)
      return
    }
    const ch = chunk[i]
    target.innerHTML += ch === '\n' ? '<br>' : ch
  }
  typewriterContent.value = (typewriterContent.value || '') + chunk
  typewriterProgress.value = Math.min(100, Math.round((target.innerHTML.length / Math.max(1, (typewriterContent.value.length || 1))) * 100))
}

function typewriterType(target, fullText, onDone) {
  if (typewriterTimer) { clearTimeout(typewriterTimer); typewriterTimer = null }
  typewriterContent.value = fullText
  typewriterTyping.value = true
  typewriterPaused.value = false
  typewriterIndex.value = 0
  typewriterProgress.value = 0
  target.innerHTML = ''

  function tick() {
    // V3.7.9+ 暂停保留 typewriterIndex (恢复点)
    if (typewriterPaused.value) {
      typewriterTimer = setTimeout(tick, 100)
      return
    }
    if (typewriterIndex.value >= fullText.length) {
      typewriterTyping.value = false
      typewriterProgress.value = 100
      typewriterTimer = null
      if (onDone) onDone()
      return
    }
    const ch = fullText[typewriterIndex.value]
    target.innerHTML += ch === '\n' ? '<br>' : ch
    typewriterIndex.value++
    typewriterProgress.value = Math.round(typewriterIndex.value / fullText.length * 100)
    typewriterTimer = setTimeout(tick, typewriterSpeed.value)
  }
  tick()
}

watch(typewriterEnabled, (v) => localStorage.setItem('minimax_typewriter', String(v)))

function pauseTypewriter() { typewriterPaused.value = true }

function resumeTypewriter() { typewriterPaused.value = false }

function stopTypewriter() {
  if (typewriterTimer) { clearTimeout(typewriterTimer); typewriterTimer = null }
  streamAbortController?.abort()  // V3.7.7+ 中断 SSE
  streamAbortController = null
  typewriterTyping.value = false
  typewriterProgress.value = 0
  typewriterIndex.value = 0
}

onUnmounted(() => { if (typewriterTimer) clearTimeout(typewriterTimer) })
const streamId = ref(null)
const dragging = ref(false)
const pendingImages = ref([])
const searchKw = ref('')
const messagesRef = ref(null)
const toolCalls = ref([])  // V3.5.95: 工具调用列表 (顶层)

// V3.6.26+ ToolCalls 统计 + JSON 格式化
const successCount = computed(() => toolCalls.value.filter((t: any) => t.status === 'ok' || !t.status).length)

function formatJson(obj: any) {
  if (!obj) return ''
  try { return JSON.stringify(obj, null, 2) } catch (e) { return String(obj) }
}
const sources = ref([])    // V3.5.95: 来源列表 (RAG 引用)

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
  checkVoiceSupport()  // V3.5.99+
  checkTTSSupport()    // V3.6.0+
  checkNotificationSupport()  // V3.6.5+
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
    toast.warning('正在生成中, 请先停止')
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
    toast.warning('正在生成中, 请先停止')
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
  toast.success('已删除')
}

/**
 * 重命名会话 (PUT /api/v1/sessions/:id)
 */
function renameSession(s) {
  ElMessageBox.prompt('输入新标题', '重命名', { inputValue: s.title })
    .then(({ value }) => {
      s.title = value
      toast.success('已修改')
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
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    if (canSend.value) sendMessage()
    return
  }
  // Ctrl/Cmd+Enter 也发送
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    if (canSend.value) sendMessage()
    return
  }
  // Ctrl/Cmd+K 新对话
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    newChat()
    return
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

// V3.7.7+ 流式 abort 控制 (module-scope, 让 stopTypewriter 可访问)
let streamAbortController: AbortController | null = null

// V3.7.8+ 当前 AI 消息 (让 stopTypewriter 可访问)
let currentAiMsg: any = null

async function sendMessage() {
  if (speechCall.state.value !== 'idle') speechCall.setProcessing()
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
  toolCalls.value = []  // V3.5.94 清空上轮工具调用
  sources.value = []    // V3.5.94 清空上轮来源

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
  currentAiMsg = aiMsg  // V3.7.8+ 让 stopTypewriter 可访问
  streaming.value = true
  streamId.value = 'stream-' + Date.now()
  streamAbortController = new AbortController()  // V3.7.7+ 流式中断
  await scrollToBottom()

  // 3) 调流式接口
  try {
    // V3.7.27+ 直接用 useBusinessStream.send (统一 5 type + Result)
    await stream.send(`/sessions/${currentSessionId.value || 0}/messages/stream`, {
      role: 'user',
      content: text,
      modelCode: selectedModel.value,
      images: images,
      streamId: streamId.value,
    }, {
      signal: streamAbortController?.signal,  // V3.7.7+
      onContent: (chunk) => {
        aiMsg.content += chunk
        // V3.7.10+ 打字机流式 chunk 集成
        if (typewriterEnabled.value) {
          typewriterAppendChunk(messagesRef.value?.lastChild?.querySelector?.('.message-content') || null, chunk)
        }
        scrollToBottom()
      },
      onToolCall: (tc) => {
        aiMsg.toolCalls.push(tc)
        toolCalls.value.push(tc)  // V3.5.94 同步到顶层
      },
      onSource: (src) => {
        aiMsg.sources.push(src)
        sources.value.push(src)  // V3.5.94 同步到顶层
      },
      onDone: () => {
        aiMsg.streaming = false
        streaming.value = false
        scrollToBottom()
        if (autoSpeak.value && aiMsg.content) {
          speak(aiMsg.content)
        }
        if (aiMsg.content) {
          showNotification('Liugl-AI', aiMsg.content.slice(0, 100))
        }
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
// === V3.5.99+ 语音输入 ===
.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.input-textarea {
  flex: 1;
}
.voice-btn {
  flex-shrink: 0;
  height: 80px !important;
  width: 80px !important;
  font-size: 24px !important;
}
.voice-panel {
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #f59e0b;
}
.voice-icon {
  font-size: 20px;
  color: #92400e;
  transition: all 0.2s;
}
.voice-icon.recording {
  color: #dc2626;
  animation: pulse 1.5s infinite;
}
.voice-content {
  flex: 1;
  font-size: 14px;
  color: #78350f;
}
.voice-status {
  display: flex;
  align-items: center;
  gap: 6px;
}
.voice-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #dc2626;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}
.voice-result {
  color: #166534;
  font-weight: 600;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}
.slide-up-enter-active,
.slide-up-leave-active { transition: all 0.3s ease; }

// === V3.6.1+ OCR 面板 ===
.ocr-panel {
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #3b82f6;
}

.ocr-icon {
  font-size: 20px;
  color: #1e40af;
  transition: all 0.2s;
}
.ocr-icon.processing {
  color: #1d4ed8;
  animation: pulse 1.5s infinite;
}

.ocr-content {
  flex: 1;
  font-size: 14px;
  color: #1e3a8a;
}

.ocr-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ocr-hint {
  font-size: 12px;
  color: #1e40af;
}

.ocr-result {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ocr-text-preview {
  font-size: 12px;
  color: #1e3a8a;
  background: rgba(255, 255, 255, 0.6);
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
}
.slide-up-enter-from,
.slide-up-leave-to { opacity: 0; transform: translateY(10px); }

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

.session-search {
  margin-bottom: 8px;
}
.session-search-content {
  margin-bottom: 12px;
}
.search-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 4px 0;
  border-bottom: 1px solid var(--liugl-border, #e2e8f0);
}
.session-preview {
  font-size: 11px;
  color: var(--liugl-text-secondary, #94a3b8);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
