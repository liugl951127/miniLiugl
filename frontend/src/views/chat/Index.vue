<!-- @file chat/Index.vue - AI 对话页面 V6.8.12 -->
<template>
  <div class="chat-page">
    <!-- 会话列表 -->
    <div class="session-list">
      <div class="session-header">
        <span>会话列表</span>
        <el-button type="primary" size="small" @click="createSession">
          <el-icon><Plus /></el-icon>新建
        </el-button>
      </div>

      <!-- V7.0: 知识库选择器 -->
      <div class="kb-selector-wrap">
        <el-tooltip content="选择知识库后，AI 将基于知识库内容回答（RAG检索）" placement="right">
          <el-select
            v-model="selectedKbId"
            size="small"
            clearable
            placeholder="📚 选知识库（RAG）"
            style="width:100%"
            @change="onKbChange"
          >
            <el-option-group label="我的知识库">
              <el-option
                v-for="kb in myKbs"
                :key="kb.id"
                :label="kb.name"
                :value="kb.id"
              >
                <span>{{ kb.name }}</span>
                <span style="float:right;font-size:11px;color:#909399">{{ kb.docCount || 0 }} 文档</span>
              </el-option>
            </el-option-group>
            <el-option-group label="公共知识库">
              <el-option
                v-for="kb in publicKbs"
                :key="kb.id"
                :label="kb.name"
                :value="kb.id"
              >
                <span>{{ kb.name }}</span>
                <span style="float:right;font-size:11px;color:#67c23a">公开</span>
              </el-option>
            </el-option-group>
          </el-select>
        </el-tooltip>

        <!-- RAG 模式专用模型选择器 -->
        <div v-if="selectedKbId" style="margin-top:8px">
          <el-tooltip content="RAG 问答使用的模型，默认使用自研模型效果更优" placement="right">
            <el-select
              v-model="ragModel"
              size="small"
              clearable
              placeholder="🤖 RAG 模型"
              style="width:100%"
            >
              <el-option-group label="🧬 自研模型（推荐）" v-if="selfTextModels.length">
                <el-option v-for="m in selfTextModels" :key="m.code" :label="m.name" :value="m.code">
                  {{ m.name }}
                  <span v-if="m.accuracy" style="float:right;font-size:11px;color:#67c23a">{{ m.accuracy }}%</span>
                </el-option>
              </el-option-group>
              <el-option-group label="🤖 商业模型">
                <el-option v-for="m in cloudTextModels" :key="m.code" :label="m.displayName || m.name" :value="m.code">
                  {{ m.displayName || m.name }}
                </el-option>
              </el-option-group>
            </el-select>
          </el-tooltip>
        </div>
      </div>

      <el-scrollbar>
        <div v-if="sessionsLoading" v-loading="true" class="session-loading">
          <span style="font-size:12px;color:#909399">加载会话列表…</span>
        </div>
        <el-empty
          v-else-if="!sessions.length"
          description="还没有会话"
          :image-size="80"
          style="padding: 20px 8px"
        >
          <el-button type="primary" size="small" @click="createSession">创建第一个会话</el-button>
        </el-empty>
        <el-menu v-else :default-active="activeSessionId" class="session-menu">
          <el-menu-item
            v-for="s in sessions" :key="s.id"
            @click="switchSession(s)"
          >
            <span class="session-title">{{ s.title || '新会话' }}</span>
            <el-tag v-if="s.kbId" size="small" type="success" style="margin-left:4px" title="知识库模式">📚</el-tag>
            <el-tag v-if="s.agentId" size="small" type="warning" style="margin-left:2px" title="Agent模式">🤖</el-tag>
            <el-tag size="small" type="info">{{ s.model || 'chat' }}</el-tag>
            <!-- 会话管理按钮 -->
            <el-button :icon="EditPen" size="small" link style="margin-left:4px;padding:2px"
              title="重命名" @click.stop="renameSession(s)" />
            <el-button :icon="Delete" size="small" link style="padding:2px;color:#f56c6c"
              title="删除会话" @click.stop="removeSession(s)" />
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </div>

    <!-- 对话区 -->
    <div class="chat-main">
      <!-- 模型选择栏 -->
      <div class="model-bar">
        <el-tooltip content="切换对话使用的 AI 模型，不同模型擅长不同任务" placement="bottom">
          <el-select v-model="currentModel" size="small" filterable placeholder="选择模型" style="width:240px" @change="onModelChange">
          <!-- ====== V7.1: 自研模型 ====== -->
          <el-option-group label="🧬 自研模型（管理员）" v-if="isSuperAdmin">
            <el-option-group label="  📝 文本" v-if="selfTextModels.length">
              <el-option v-for="m in selfTextModels" :key="m.code" :label="m.name" :value="m.code">
                <span>{{ m.name }}</span>
                <span style="float:right;display:flex;align-items:center;gap:4px">
                  <span style="font-size:10px;background:#f0f9ff;color:#0284c7;padding:1px 5px;border-radius:3px;font-weight:600">自研</span>
                  <span v-if="m.accuracy" style="font-size:10px;color:#67c23a">{{ m.accuracy }}%</span>
                  <span style="font-size:10px;color:#9ca3af">{{ m.provider || '' }}</span>
                </span>
              </el-option>
            </el-option-group>
            <!-- V7.1: ONNX 本地推理模型 -->
            <el-option-group label="  ⚡ ONNX 本地推理" v-if="onnxTextModels.length">
              <el-option v-for="m in onnxTextModels" :key="m.code" :label="m.name" :value="m.code">
                <span>{{ m.name }}</span>
                <span style="float:right;display:flex;align-items:center;gap:4px">
                  <span style="font-size:10px;background:#fff7ed;color:#c2410c;padding:1px 5px;border-radius:3px;font-weight:600">⚡ ONNX</span>
                  <span v-if="m.accuracy" style="font-size:10px;color:#67c23a">{{ m.accuracy }}%</span>
                  <span style="font-size:10px;color:#9ca3af">{{ m.providerCode || '' }}</span>
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="  🖼️ 视觉" v-if="selfVisionModels.length">
              <el-option v-for="m in selfVisionModels" :key="m.code" :label="m.name" :value="m.code">
                <span>{{ m.name }}</span>
                <span style="float:right;display:flex;align-items:center;gap:4px">
                  <span style="font-size:10px;background:#f0f9ff;color:#0284c7;padding:1px 5px;border-radius:3px;font-weight:600">自研</span>
                  <span v-if="m.accuracy" style="font-size:10px;color:#67c23a">{{ m.accuracy }}%</span>
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="  💻 本地部署" v-if="localTextModels.length">
              <el-option v-for="m in localTextModels" :key="m.code" :label="m.displayName || m.name" :value="m.code">
                <span>{{ m.displayName || m.name }}</span>
                <span style="float:right;font-size:10px;background:#fff7ed;color:#c2410c;padding:1px 5px;border-radius:3px">本地</span>
              </el-option>
            </el-option-group>
          </el-option-group>
          <!-- ====== V7.1: 商业模型 ====== -->
          <el-option-group label="🤖 商业模型">
            <el-option-group label="  📝 文本">
              <el-option v-for="m in commercialTextModels" :key="m.code" :label="m.displayName || m.name" :value="m.code">
                <span>{{ m.displayName || m.name }}</span>
                <span style="float:right;font-size:10px;color:#6b7280">{{ m.providerName || m.provider || '' }}</span>
              </el-option>
            </el-option-group>
            <el-option-group label="  🖼️ 视觉" v-if="commercialVisionModels.length">
              <el-option v-for="m in commercialVisionModels" :key="m.code" :label="m.displayName || m.name" :value="m.code">
                <span>{{ m.displayName || m.name }}</span>
                <span style="float:right;font-size:10px;color:#6b7280">{{ m.providerName || m.provider || '' }}</span>
              </el-option>
            </el-option-group>
            <el-option-group label="  🎵 音频" v-if="audioModels.length">
              <el-option v-for="m in audioModels" :key="m.code" :label="m.displayName || m.name" :value="m.code">
                <span>{{ m.displayName || m.name }}</span>
                <span style="float:right;font-size:10px;color:#92400e">{{ m.providerName || m.provider || '' }}</span>
              </el-option>
            </el-option-group>
          </el-option-group>
        </el-select>
        </el-tooltip>

        <!-- V7.0: Agent 委托选择器 -->
        <el-tooltip content="委托 Agent 辅助执行（可调用知识库搜索等工具）" placement="bottom">
          <el-select
            v-model="selectedAgentId"
            size="small"
            clearable
            placeholder="🤖 委托Agent"
            style="width:160px"
          >
            <el-option
              v-for="agent in availableAgents"
              :key="agent.agentId || agent.id || agent.name"
              :label="agent.displayName || agent.name"
              :value="agent.agentId || agent.id || agent.name"
            >
              <span>{{ agent.displayName || agent.name }}</span>
              <span style="float:right;font-size:11px;color:#909399">{{ agent.category || 'agent' }}</span>
            </el-option>
            <el-option v-if="!availableAgents.length" label="无可用Agent" disabled />
          </el-select>
        </el-tooltip>

        <!-- 当前路由目标 -->
        <div class="route-info">
          <el-tag v-if="isSuperAdmin" size="small" type="warning" style="margin-right:6px">🧬 管理员</el-tag>
          <el-tag size="small" :type="routeTag.type">{{ routeTag.icon }} {{ routeTag.label }}</el-tag>
        </div>
        <!-- 清空对话 -->
        <el-tooltip content="清空当前对话历史（服务器端记录不受影响）" placement="bottom">
          <el-button size="small" plain @click="clearChat" :disabled="!messages.length">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>

      <!-- 消息列表 -->
      <el-scrollbar ref="scrollRef" class="message-list">
        <div class="messages">
          <div v-if="!messages.length" class="welcome-msg">
            <div style="font-size:40px;margin-bottom:12px">🤖</div>
            <div style="font-size:16px;font-weight:600;color:#303133">你好，我是 MiniMax AI</div>
            <div style="font-size:13px;color:#909399;margin-top:6px">当前模型: <b>{{ currentModel }}</b>{{ currentModelAccuracy ? ' 🧬自研(' + currentModelAccuracy + '%)' : '' }} · {{ routeTag.label }}</div>
            <div style="font-size:12px;color:#c0c4cc;margin-top:4px">支持上传图片/视频/文件，我会根据内容智能回复</div>
          </div>

          <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
            <el-avatar :size="32" class="msg-avatar">
              {{ msg.role === 'user' ? 'U' : (msg.type === 'analysis' ? '🔍' : 'AI') }}
            </el-avatar>
            <div class="msg-content">
              <div class="msg-meta">
                {{ msg.role === 'user' ? '你' : (msg.type === 'analysis' ? '视觉分析' : (msg.model || 'MiniMax')) }}
                <el-tag v-if="msg.type === 'analysis'" size="small" type="success" style="margin-left:4px">🔍 分析</el-tag>
                <el-tag v-if="msg.role === 'assistant' && msg.agentGroup" size="small" type="info" style="margin-left:4px">
                  {{ msg.agentGroup }}
                </el-tag>
                <!-- V7.3 情绪标签 -->
                <el-tag v-if="msg.emotion" size="small" type="warning" style="margin-left:4px">
                  {{ msg.emotion.emoji }} {{ msg.emotion.label }}
                </el-tag>
              </div>

              <!-- 附件预览 (用户消息) -->
              <div v-if="msg.attachments?.length" class="attachment-row">
                <div v-for="att in msg.attachments" :key="att.url" class="attachment-chip">
                  <el-icon v-if="att.type === 'image'"><Picture /></el-icon>
                  <el-icon v-else-if="att.type === 'video'"><VideoCamera /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                  <span>{{ att.name }}</span>
                </div>
              </div>

              <!-- 文本气泡 -->
              <div class="msg-bubble" :class="{ 'analysis-bubble': msg.type === 'analysis' }">
                <!-- 图片附件 -->
                <div v-if="msg.imageUrls?.length" class="inline-images">
                  <el-image
                    v-for="url in msg.imageUrls" :key="url"
                    :src="url" fit="cover" :preview-src-list="msg.imageUrls"
                    style="width:120px;height:120px;border-radius:8px;margin-right:6px;cursor:pointer"
                  />
                </div>
                <!-- 文本内容 -->
                <div v-if="msg.content" v-html="formatContent(msg.content)"></div>
                <!-- 视频附件 -->
                <div v-if="msg.videoUrls?.length">
                  <video v-for="url in msg.videoUrls" :key="url" :src="url" controls style="width:240px;border-radius:8px;margin-top:6px" />
                </div>
                <!-- V7.3 语音播报按钮 -->
                <div v-if="msg.role === 'assistant' && msg.content && !loading" class="msg-actions">
                  <el-tooltip content="朗读" placement="bottom">
                    <el-button size="small" text
                      @click="speakText(msg.content, i)"
                      :disabled="playingAudio !== null && playingAudio !== 'playing-' + i"
                    >
                      <el-icon><component :is="playingAudio === 'playing-' + i ? 'VideoPause' : 'VideoPlay'" /></el-icon>
                      {{ playingAudio === 'playing-' + i ? '停止' : '朗读' }}
                    </el-button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>

          <!-- 分析中指示器 (V7.3) -->
          <div v-if="analyzingAttachment" class="message assistant">
            <el-avatar :size="32">🔍</el-avatar>
            <div class="msg-content">
              <div class="msg-meta">视觉分析</div>
              <div class="msg-bubble" style="color:#909399;font-size:13px">
                <span v-if="currentAnalyzeType === 'image'">🖼️ 正在分析图片…</span>
                <span v-else>🎬 正在分析视频…</span>
                <el-icon class="is-loading" style="margin-left:6px"><Loading /></el-icon>
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading && !analyzingAttachment" class="message assistant">
            <el-avatar :size="32">AI</el-avatar>
            <div class="msg-content">
              <div class="msg-bubble loading-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>

          <!-- V7.3+: 流式响应中断 - 重连按钮 -->
          <div v-if="streamError" class="message assistant">
            <el-avatar :size="32">⚠️</el-avatar>
            <div class="msg-content">
              <div class="msg-bubble" style="background:#fef0f0;border:1px solid #fde2e2">
                <div style="color:#f56c6c;font-size:13px;margin-bottom:6px">
                  ⚠️ 流式响应中断：{{ streamError }}
                </div>
                <el-button size="small" type="primary" @click="reconnectStream">
                  <el-icon><Refresh /></el-icon>重连
                </el-button>
                <el-button size="small" @click="streamError = ''" style="margin-left:6px">忽略</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <!-- 输入区 -->
      <div class="chat-input">
        <!-- 附件预览 -->
        <div v-if="pendingAttachments.length" class="pending-attachments">
          <div v-for="(att, i) in pendingAttachments" :key="i" class="pending-chip">
            <el-icon v-if="att.type === 'image'"><Picture /></el-icon>
            <el-icon v-else-if="att.type === 'video'"><VideoCamera /></el-icon>
            <el-icon v-else><Document /></el-icon>
            <span>{{ att.name }}</span>
            <el-icon class="remove-btn" @click="removeAttachment(i)"><Close /></el-icon>
          </div>
        </div>

        <!-- 文本输入 -->
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入消息，支持粘贴图片/拖拽文件。Shift+Enter 换行，Enter 发送"
          :disabled="loading"
          @keydown.enter.exact.prevent="sendMessage"
          @paste="handlePaste"
          @dragover.prevent
          @drop.prevent="handleDrop"
        />

        <!-- 工具栏 -->
        <div class="input-footer">
          <div style="display:flex;gap:4px">
            <!-- 图片上传 -->
            <el-tooltip content="上传图片" placement="top">
              <el-button size="small" :disabled="!canVision" @click="triggerUpload('image')" plain>
                <el-icon><Picture /></el-icon>
              </el-button>
            </el-tooltip>
            <!-- 视频上传 -->
            <el-tooltip content="上传视频" placement="top">
              <el-button size="small" :disabled="!canVideo" @click="triggerUpload('video')" plain>
                <el-icon><VideoCamera /></el-icon>
              </el-button>
            </el-tooltip>
            <!-- 文件上传 -->
            <el-tooltip content="上传文件" placement="top">
              <el-button size="small" @click="triggerUpload('file')" plain>
                <el-icon><Document /></el-icon>
              </el-button>
            </el-tooltip>
            <input
              ref="fileInputRef"
              type="file"
              :accept="acceptMap[uploadType]"
              style="display:none"
              @change="onFileSelected"
            />
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <span v-if="isStreaming" class="streaming-indicator">
              <span class="dot"></span>{{ isTrainedStreaming ? '训练模型推理中…' : 'AI 思考中…' }}
            </span>
            <el-button v-if="isStreaming" type="danger" size="small" @click="cancelStream">
              <el-icon><Close /></el-icon>停止
            </el-button>
            <el-tooltip content="发送消息，Shift+Enter 换行" placement="top">
              <el-button type="primary" :loading="loading && !isStreaming" :disabled="isStreaming" @click="sendMessage">
                {{ loading && !isStreaming ? '发送中…' : '发送' }}
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProviders, listEnabledModels } from '@/api/model'
import { uploadImage, uploadAudio, uploadVideo, videoUnderstand } from '@/api/ai'
import { visionAnalyze, audioTts } from '@/api/multimodal'
import { listSessions, createSession as createSess, listMessages, sendMessageStream, deleteSession, updateSession, onnxGenerate } from '@/api/session'
import { trainingChat, listTrainedModels } from '@/api/ai'
import { listMyKbs, listPublicKbs } from '@/api/rag'
import { listAgents } from '@/api/agent'
import { useUserStore } from '@/store/user'
import { Plus, Picture, VideoCamera, Document, Close, Delete, EditPen, VideoPlay, VideoPause, Loading } from '@element-plus/icons-vue'

// ============ 状态 ============
const sessions = ref([])
const sessionsLoading = ref(false)  // 会话列表加载状态
// V7.3: 流式响应中断错误
const streamError = ref('')
const lastFailedMessage = ref('')  // 上次失败的消息（用于重连）
// V7.0: 知识库状态
const selectedKbId = ref(null)
const ragModel = ref('') // RAG 模式专用模型
const myKbs = ref([])
const publicKbs = ref([])
// V7.0: Agent 委托状态
const selectedAgentId = ref(null)
const availableAgents = ref([])
const activeSessionId = ref('')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const isStreaming = ref(false)
const isTrainedStreaming = ref(false)  // 训练模型推理中（同步但慢）
const scrollRef = ref(null)
const fileInputRef = ref(null)
let currentStream = null  // 流取消函数

// V7.3: 附件预分析状态
const analyzingAttachment = ref(false)  // 是否正在分析附件
const currentAnalyzeType = ref('')       // 'image' | 'video'
// 播放中的 TTS (message index → audio url)
const playingAudio = ref(null)  // 当前播放的 audio URL
let currentAudioEl = null       // HTMLAudioElement 实例

// ============ 情绪分析 (前端关键词算法, V7.3) ============
const EMOTION_DICT = [
  { keywords: ['开心','高兴','快乐','哈哈','笑死','太好了','棒','赞','爱你','喜欢','么么哒','好开心','happy','happiness','joy','lol'], emoji: '😊', label: '开心' },
  { keywords: ['难过','伤心','痛苦','哭','泪','心碎','抑郁','郁闷','不爽','烦','sad','cry','unhappy','depressed','anxiety','焦虑'], emoji: '😢', label: '难过/焦虑' },
  { keywords: ['生气','愤怒','气死','滚','滚蛋','可恶','讨厌','恨','火大','angry','rage','furious','fuck','shit'], emoji: '😠', label: '愤怒' },
  { keywords: ['害怕','恐怖','吓人','惊悚','好怕','紧张','怕','fear','scary','terrified','nervous'], emoji: '😰', label: '害怕/紧张' },
  { keywords: ['惊讶','震惊','卧槽','牛','厉害','太强','震惊','wow','amazing','shocked','incredible'], emoji: '😮', label: '惊讶' },
  { keywords: ['无聊','困','累','疲惫','没劲','无语','boring','tired','sleepy','whatever'], emoji: '😴', label: '无聊/疲惫' },
]
function detectEmotion(text) {
  if (!text) return null
  const lower = text.toLowerCase()
  for (const item of EMOTION_DICT) {
    for (const kw of item.keywords) {
      if (lower.includes(kw.toLowerCase())) return { emoji: item.emoji, label: item.label }
    }
  }
  return null
}

// ============ 附件预分析 (V7.3) ============
/** File 对象 → data URL (base64) */
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/** 分析图片，返回描述文本。
 * 兼容 imageUrl (string URL) 和 base64 data URL。 */
async function analyzeImage(imageUrl) {
  try {
    const r = await visionAnalyze(imageUrl, '请详细描述这张图片的内容，包括场景、人物、物品、动作等细节', currentModel.value)
    return r.data?.content || r.data?.description || null
  } catch {
    return null
  }
}

/** 分析视频，返回描述文本 */
async function analyzeVideo(file) {
  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('prompt', '请详细描述这段视频的内容，包括场景、人物、动作、对话等')
    fd.append('model', currentModel.value)
    const r = await videoUnderstand(fd)
    const frames = r.data?.frames || []
    if (!frames.length) return null
    // 拼接各帧描述
    return frames.map((f, i) => `【第${i+1}帧】${f.content || f.description || ''}`).join('\n')
  } catch {
    return null
  }
}

// ============ 语音播报 (V7.3) ============
/** 朗读指定文本 (V7.3) */
async function speakText(text, msgIdx) {
  if (!text?.trim()) return
  // 正在播放则停止
  if (playingAudio.value === 'playing-' + msgIdx) {
    stopSpeaking()
    return
  }
  try {
    stopSpeaking()
    const r = await audioTts({ text: text.slice(0, 500) })  // TTS 限制500字
    const audioUrl = r.data?.audio || r.data?.audioUrl
    if (!audioUrl) { ElMessage.warning('语音合成失败'); return }
    currentAudioEl = new Audio(audioUrl)
    playingAudio.value = 'playing-' + msgIdx
    currentAudioEl.onended = () => { playingAudio.value = null; currentAudioEl = null }
    currentAudioEl.onerror = () => { playingAudio.value = null; currentAudioEl = null }
    await currentAudioEl.play()
  } catch (e) {
    ElMessage.error('播放失败: ' + (e?.message || ''))
    playingAudio.value = null
  }
}

function stopSpeaking() {
  if (currentAudioEl) {
    currentAudioEl.pause()
    currentAudioEl.currentTime = 0
    currentAudioEl = null
  }
  playingAudio.value = null
}

// V7.0: 加载知识库列表
async function loadKnowledgeBases() {
  try {
    const [myRes, pubRes] = await Promise.all([
      listMyKbs().catch(() => ({ data: [] })),
      listPublicKbs().catch(() => ({ data: [] })),
    ])
    myKbs.value = myRes.data || []
    publicKbs.value = pubRes.data || []
  } catch (e) {
    // V7.4: 静默失败（不影响主功能）
    myKbs.value = []
    publicKbs.value = []
  }
}

function onKbChange(kbId) {
  if (kbId) {
    const kb = [...myKbs.value, ...publicKbs.value].find(k => k.id === kbId)
    ElMessage.success(`📚 已启用知识库: ${kb?.name || '未知'}`)
  } else {
    ElMessage.info('已关闭知识库 RAG，将使用通用模式回答')
  }
}

// V7.0: 加载可用 Agent 列表
async function loadAgents() {
  try {
    const r = await listAgents()
    // http interceptor 自动剥 Result 包装，所以 r 已经是数组
    availableAgents.value = Array.isArray(r) ? r : (r.data || [])
  } catch (e) {
    console.warn('[Chat] 加载 Agent 列表失败:', e)
    availableAgents.value = []
  }
}

/** 取消当前流式请求 */
function cancelStream() {
  if (currentStream?.cancel) {
    currentStream.cancel()
    currentStream = null
  }
  isStreaming.value = false
  isTrainedStreaming.value = false
}

/** 清空当前对话 */
function clearChat() {
  messages.value = []
}
const uploadType = ref('image')
const uploadProgress = ref(0)
const pendingAttachments = ref([]) // [{type, name, url, localUrl}]

// ============ 模型加载 ============
const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

const allModels = ref([])
const currentModel = ref('gpt-4o-mini')

// 🧬 训练模型（平台自主训练，仅超级管理员可见）
// V6.8.2: 从后端 /ai/training/models API 加载，不再硬编码
const trainedModels = ref([])

async function loadTrainedModels() {
  try {
    const r = await listTrainedModels()
    trainedModels.value = (r.data || []).map(m => ({
      code: m.code,
      name: m.name,
      provider: m.provider || '训练模型',
      providerCode: m.providerCode || m.provider || '',   // V7.1: ONNX 模型检测用
      // 从代码或 backend 标记自动判断视觉
      vision: !!(m.vision || (m.code || '').toLowerCase().includes('vision') || (m.code || '').toLowerCase().includes('vl')),
      audio: !!(m.audio || (m.code || '').toLowerCase().includes('audio')),
      trained: true,
      accuracy: m.accuracy || 0,
      category: 'self',  // V7.1: 明确标记为自研
    }))
  } catch (e) {
    console.warn('[Chat] 加载训练模型失败:', e)
  }
}
loadTrainedModels()

const textModels = computed(() =>
  allModels.value.filter(m => !m.vision && !m.audio && !m.trained)
)
// V6.8.2: 本地 vs 云端模型分离
const localTextModels = computed(() => textModels.value.filter(m => m.local))
const cloudTextModels = computed(() => textModels.value.filter(m => !m.local))
const trainedTextModels = computed(() => trainedModels.value.filter(m => !m.vision))
const visionModels = computed(() => allModels.value.filter(m => m.vision))
const trainedVisionModels = computed(() => trainedModels.value.filter(m => m.vision))
const audioModels = computed(() => allModels.value.filter(m => m.audio))

// V7.1: 自研模型（来自 trainedModels API + 本地部署）
const selfTextModels = computed(() => trainedModels.value.filter(m => !m.vision && !m.audio))
const selfVisionModels = computed(() => trainedModels.value.filter(m => m.vision && !m.audio))

// V7.1: ONNX 模型（providerCode 包含 onnx 的文本模型）
const onnxTextModels = computed(() =>
  trainedModels.value.filter(m =>
    !m.vision && !m.audio &&
    ((m.providerCode || '').toLowerCase().includes('onnx') ||
     (m.code || '').toLowerCase().includes('onnx'))
  )
)

// V7.1: 当前是否为 ONNX 模型
const isOnnxModel = computed(() =>
  onnxTextModels.value.some(m => m.code === currentModel.value)
)

// V7.1: 商业模型（来自 /models 接口的 commercial category）
const commercialTextModels = computed(() =>
  allModels.value.filter(m => !m.vision && !m.audio && m.category === 'commercial')
)
const commercialVisionModels = computed(() =>
  allModels.value.filter(m => m.vision && !m.audio && m.category === 'commercial')
)
const canVision = computed(() =>
  visionModels.value.length > 0 || trainedVisionModels.value.some(m => m.code === currentModel.value) ||
  currentModel.value.includes('4o') || currentModel.value.includes('vision')
)
const canVideo = computed(() =>
  currentModel.value.includes('4o') || currentModel.value.includes('video')
)
// 当前选中的自研模型准确率
const currentModelAccuracy = computed(() => {
  const t = trainedModels.value.find(m => m.code === currentModel.value)
  return t?.accuracy || null
})

// ============ 模型 → 智能体群路由 ============
// V7.2: 自研模型已全部动态加载（trainedModels from API），不再硬编码
// - 自研视觉模型 → VisionAgent（已在 routeTag computed 顶部动态检查）
// - 自研文本模型 → TrainedAgent（fallback: trainedModels.some 检查）
const ROUTE_MAP = {
  // 🧬 训练模型（通过 trainedModels 动态判断，此处只保留外部已知模型名匹配）
  'minimax': { group: 'TrainedAgent', label: '训练模型', icon: '🧬', type: 'primary' },
  // 🖼️ 视觉模型 → vision agent group
  'minimax-vision': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'gpt-4o': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'gpt-4-turbo': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'claude-3': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'claude-3-5': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'gemini': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'qwen-vl': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  'yi-vl': { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' },
  // 🎵 音频模型 → audio agent group
  'whisper': { group: 'AudioAgent', label: '音频智能体', icon: '🎵', type: 'warning' },
  'speech': { group: 'AudioAgent', label: '音频智能体', icon: '🎵', type: 'warning' },
  'tts': { group: 'AudioAgent', label: '音频智能体', icon: '🎵', type: 'warning' },
  // 💻 代码模型 → code agent group
  'claude-3-7': { group: 'CodeAgent', label: '代码智能体', icon: '💻', type: 'primary' },
  'gpt-4o-code': { group: 'CodeAgent', label: '代码智能体', icon: '💻', type: 'primary' },
  // 默认 → 通用对话智能体
  'default': { group: 'ChatAgent', label: '通用对话', icon: '🤖', type: 'info' },
}

const routeTag = computed(() => {
  const model = currentModel.value.toLowerCase()
  // V7.2: 自研模型全部动态判断（trainedModels from API）
  // 自研视觉模型
  if (trainedVisionModels.value.some(m => m.code === currentModel.value)) {
    return { group: 'VisionAgent', label: '视觉智能体', icon: '🖼️', type: 'success' }
  }
  // 自研文本模型
  if (trainedTextModels.value.some(m => m.code === currentModel.value)) {
    return { group: 'TrainedAgent', label: '训练模型', icon: '🧬', type: 'primary' }
  }
  // 本地模型优先匹配（更精确的 key）
  for (const [key, val] of Object.entries(ROUTE_MAP)) {
    if (key === 'default') continue
    if (key.endsWith('-') ? model.startsWith(key) : model.includes(key)) {
      return val
    }
  }
  // 含 vision/vl/4o 关键字
  if (model.includes('vision') || model.includes('vl') || model.includes('4o')) {
    return ROUTE_MAP['gpt-4o']
  }
  return ROUTE_MAP['default']
})

// ============ 上传 ============
const acceptMap = {
  image: 'image/*',
  video: 'video/*',
  file: '*/*',
}

function triggerUpload(type) {
  uploadType.value = type
  fileInputRef.value?.click()
}

async function onFileSelected(ev) {
  const file = ev.target.files?.[0]
  if (!file) return
  ev.target.value = ''
  try {
    await uploadFile(file)
  } catch (e) {
    ElMessage.error('文件上传失败: ' + (e?.message || '未知错误'))
  }
}

async function handlePaste(ev) {
  const items = ev.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file) await uploadFile(file)
    }
  }
}

async function handleDrop(ev) {
  for (const file of ev.dataTransfer?.files || []) {
    await uploadFile(file)
  }
}

async function uploadFile(file) {
  const type = file.type.startsWith('image/') ? 'image'
    : file.type.startsWith('video/') ? 'video'
    : 'file'

  if (type === 'image' && !canVision.value) {
    ElMessage.warning('当前模型不支持图片，请切换到视觉模型')
    return
  }

  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('filename', file.name)

    let url = ''
    if (type === 'image') {
      const r = await uploadImage(fd)
      url = r.data?.url || r.data
    } else if (type === 'video') {
      const r = await uploadVideo(fd)
      url = r.data?.url || r.data
    } else {
      // 文件：走通用上传接口
      const r = await uploadImage(fd) // 复用 image 接口传文件
      url = r.data?.url || r.data
    }

    if (!url) { ElMessage.error('上传失败，未返回 URL'); return }

    pendingAttachments.value.push({ type, name: file.name, url, localUrl: URL.createObjectURL(file), _file: file })
    ElMessage.success(`已上传: ${file.name}`)
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || ''))
  }
}

function removeAttachment(i) {
  const att = pendingAttachments.value[i]
  if (att.localUrl) URL.revokeObjectURL(att.localUrl)
  pendingAttachments.value.splice(i, 1)
}

// ============ 消息发送 ============
async function sendMessage() {
  if (!inputText.value.trim() && !pendingAttachments.value.length) {
    ElMessage.warning('请输入内容或上传附件')
    return
  }
  if (loading.value) return

  const text = inputText.value.trim()
  inputText.value = ''

  // 收集附件
  const attachments = pendingAttachments.value.map(a => ({ type: a.type, name: a.name, url: a.url, _file: a._file }))
  const imageUrls = attachments.filter(a => a.type === 'image').map(a => a.url)
  const videoAttachments = attachments.filter(a => a.type === 'video')
  const videoFiles = videoAttachments.map(a => a._file).filter(Boolean)
  pendingAttachments.value = []
  uploadProgress.value = 0

  // 构造内容：文本 + OpenAI style image URLs
  // uploadImage 没返回 url (只有 fileId)，需要转 base64
  const imageAttachments = attachments.filter(a => a.type === 'image')
  let content = text
  if (imageAttachments.length) {
    const imageContent = []
    for (const att of imageAttachments) {
      let imgUrl = att.url
      if (!imgUrl || typeof imgUrl !== 'string' || imgUrl.startsWith('{')) {
        imgUrl = att._file ? await fileToBase64(att._file) : null
      }
      if (imgUrl) {
        imageContent.push({ type: 'image_url', image_url: { url: imgUrl } })
      }
    }
    if (imageContent.length > 0) {
      content = [{ type: 'text', text }, ...imageContent]
    }
  }

  // ── 情绪分析 (V7.3): 用户消息打情绪标签 ──
  const emotion = detectEmotion(text)

  // ── 附件预分析 (V7.3): 图片/视频发之前先分析 ──
  const hasMedia = imageUrls.length > 0 || videoAttachments.length > 0

  // 视频URL处理：uploadVideo 无公开URL，用 localUrl (blob URL) 供预览
  const videoUrls = videoAttachments.map(a => a.localUrl || a.url).filter(Boolean)

  // 用户消息
  const userMsg = {
    role: 'user',
    content: typeof content === 'string' ? content : text,
    attachments,
    // 显示用 localUrl（上传前blob预览）或 url（外部URL）
    imageUrls: imageAttachments.map(a => a.localUrl || a.url).filter(Boolean),
    videoUrls,
    model: currentModel.value,
    emotion,  // V7.3 情绪标签
  }
  messages.value.push(userMsg)

  // ── 附件预分析: 有图片/视频时，先分析再送 AI (V7.3) ──
  if (hasMedia) {
    analyzingAttachment.value = true
    currentAnalyzeType.value = imageUrls.length > 0 ? 'image' : 'video'
    await scrollBottom()
    try {
      let analysisTexts = []
      // 图片分析：支持 URL字符串 和 upload响应对象
      const imageAttachments = attachments.filter(a => a.type === 'image')
      for (const att of imageAttachments) {
        let target = att.url
        // uploadImage 没返回 url 时，用 base64
        if (!att.url || typeof att.url !== 'string' || att.url.startsWith('{')) {
          if (att._file) {
            target = await fileToBase64(att._file)
          } else {
            continue  // 既不是URL也没有文件，跳过
          }
        }
        const desc = await analyzeImage(target)
        if (desc) analysisTexts.push(`🖼️ 图片分析：${desc}`)
      }
      // 视频分析
      for (const vFile of videoFiles) {
        const desc = await analyzeVideo(vFile)
        if (desc) analysisTexts.push(`🎬 视频分析：${desc}`)
      }
      if (analysisTexts.length > 0) {
        messages.value.push({
          role: 'assistant',
          content: analysisTexts.join('\n\n'),
          type: 'analysis',   // 标记为分析结果，不触发 AI 对话
          model: currentModel.value,
          emotion: null,
        })
        await scrollBottom()
      }
    } catch (e) {
      console.warn('[Chat] 附件分析失败:', e)
    } finally {
      analyzingAttachment.value = false
    }
  }

  loading.value = true
  isStreaming.value = true
  await scrollBottom()

  try {
    const model = currentModel.value
    const isTrained = trainedModels.value.some(m => m.code === model)

    // AI 回复气泡（训练模型同步/普通模型流式）
    const assistantMsg = reactive({ role: 'assistant', content: '', model, agentGroup: routeTag.value.group })
    messages.value.push(assistantMsg)
    await scrollBottom()

    if (isTrained) {
      // 🧬 训练模型：同步请求
      isTrainedStreaming.value = true
      const r = await trainingChat({ model, message: text })
      assistantMsg.content = r.data?.content || r.data?.answer || '...'
      assistantMsg.model = r.data?.model || model
      isTrainedStreaming.value = false
    } else if (isOnnxModel.value) {
      // ⚡ ONNX 本地推理：同步请求（等生成完毕再显示）
      isTrainedStreaming.value = true
      try {
        const r = await onnxGenerate({ prompt: text, model })
        assistantMsg.content = r.data?.text || '（ONNX 推理完成，无输出）'
        assistantMsg.model = model
      } catch (e) {
        assistantMsg.content = `⚠️ ONNX 推理失败：${e?.message || e || '未知错误'}`
        ElMessage.error('ONNX 推理失败，请检查模型是否加载')
      }
      isTrainedStreaming.value = false
    } else {
      // 🤖 普通模型：SSE 流式
      // V7.0: 透传 kbId(知识库) + agentId(Agent委托) 触发 RAG/Agent
      const streamPayload = {
        message: text,   // ← 后端 AiChatRealController 读取 body.message
        model,
        agentGroup: routeTag.value.group,
      }
      if (selectedKbId.value) streamPayload.kbId = selectedKbId.value
      if (selectedAgentId.value) streamPayload.agentId = selectedAgentId.value
      // RAG 模式：优先使用用户指定的模型
      if (ragModel.value) streamPayload.model = ragModel.value

      // V7.0: 查找选中的知识库名称
      if (selectedKbId.value) {
        const kb = [...myKbs.value, ...publicKbs.value].find(k => k.id === selectedKbId.value)
        if (kb) streamPayload.kbName = kb.name
      }

      currentStream = await sendMessageStream(activeSessionId.value, streamPayload, {
        onContent: (chunk) => {
          assistantMsg.content += chunk
          streamError.value = ''  // 收到内容时清空错误状态
          scrollBottom()
        },
        onAgentStatus: (status) => {
          // V7.0: Agent 状态更新
          assistantMsg.content += '\n\n' + status + '\n'
          scrollBottom()
        },
        onAgentResult: (result) => {
          // V7.0: Agent 执行结果
          assistantMsg.content += '\n\n🤖 【Agent 执行结果】\n' + result + '\n'
          scrollBottom()
        },
        onDone: () => {
          assistantMsg.model = model
          streamError.value = ''
        },
        onError: (err) => {
          const errMsg = err?.message || '未知错误'
          assistantMsg.content = '⚠️ 流式响应错误：' + errMsg
          streamError.value = errMsg
          lastFailedMessage.value = text  // 记录失败的消息用于重连
        },
      })
    }
  } catch (e) {
    const errMsg = e?.message || '未知错误'
    messages.value.push({
      role: 'assistant',
      content: '⚠️ 请求失败：' + errMsg,
    })
    streamError.value = errMsg
    lastFailedMessage.value = text
    ElMessage.error('发送失败：' + errMsg)
  } finally {
    loading.value = false
    isStreaming.value = false
    await scrollBottom()
  }
}

/** V7.3: 重连流式响应 - 重新发送上次失败的消息 */
async function reconnectStream() {
  if (!lastFailedMessage.value) {
    ElMessage.warning('没有可重连的消息')
    return
  }
  streamError.value = ''
  inputText.value = lastFailedMessage.value
  await sendMessage()
}

// ============ 会话管理 ============
async function loadSessions() {
  sessionsLoading.value = true
  try {
    const r = await listSessions()
    sessions.value = r.data?.list || r.data || []
  } catch (e) {
    sessions.value = []
    ElMessage.error('加载会话列表失败：' + (e?.message || '网络错误'))
  } finally {
    sessionsLoading.value = false
  }
}

async function loadMessages(sessionId) {
  try {
    const r = await listMessages(sessionId)
    messages.value = (r.data || []).map(m => ({
      ...m,
      imageUrls: extractImageUrls(m.content),
      videoUrls: extractVideoUrls(m.content),
    }))
  } catch (e) {
    messages.value = []
    ElMessage.error('加载消息失败：' + (e?.message || '网络错误'))
  }
}

async function createSession() {
  try {
    // V7.0: 创建会话时关联知识库 + Agent
    const payload = { title: '新会话', model: currentModel.value }
    if (selectedKbId.value) {
      payload.kbId = selectedKbId.value
      payload.kbName = [...myKbs.value, ...publicKbs.value].find(k => k.id === selectedKbId.value)?.name || ''
      if (ragModel.value) payload.model = ragModel.value
    }
    if (selectedAgentId.value) {
      payload.agentId = selectedAgentId.value
      const agent = availableAgents.value.find(a => (a.agentId || a.id || a.name) === selectedAgentId.value)
      if (agent) payload.agentName = agent.displayName || agent.name || selectedAgentId.value
    }
    const r = await createSess(payload)
    const id = r.data?.id || r.id
    sessions.value.unshift({ id, title: '新会话', model: currentModel.value,
      kbId: selectedKbId.value, agentId: selectedAgentId.value })
    activeSessionId.value = id
    messages.value = []
    ElMessage.success('会话已创建')
  } catch (e) {
    ElMessage.error('创建会话失败：' + (e?.message || '网络错误'))
  }
}

async function switchSession(s) {
  activeSessionId.value = s.id
  // V7.0: 切换会话时恢复知识库 + Agent 选择
  if (s.kbId) {
    selectedKbId.value = s.kbId
  } else {
    selectedKbId.value = null
  }
  selectedAgentId.value = s.agentId || null
  await loadMessages(s.id)
}

// ============ 会话管理 ============
async function removeSession(s) {
  try {
    await ElMessageBox.confirm(`删除会话「${s.title || '新会话'}」？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    await deleteSession(s.id)
    sessions.value = sessions.value.filter(x => x.id !== s.id)
    if (activeSessionId.value === s.id) {
      // 切换到第一个
      if (sessions.value.length) await switchSession(sessions.value[0])
      else { activeSessionId.value = null; messages.value = [] }
    }
    ElMessage.success('会话已删除')
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    ElMessage.error('删除失败：' + (e?.message || '网络错误'))
  }
}

async function renameSession(s) {
  try {
    const { value: newTitle } = await ElMessageBox.prompt(
      '输入新名称', '重命名会话', { confirmButtonText: '确定', cancelButtonText: '取消',
        inputValue: s.title || '', })
    if (!newTitle?.trim()) return
    await updateSession(s.id, { title: newTitle.trim() })
    s.title = newTitle.trim()
    ElMessage.success('已重命名')
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    ElMessage.error('重命名失败：' + (e?.message || '网络错误'))
  }
}

// ============ 加载模型列表 (V6.8.2: 用 /models 含本地模型) ============
async function loadModels() {
  try {
    // V6.8.2: 直接调 /models 接口，返回所有可用模型 (含本地)
    const r = await listEnabledModels()
    const rows = r.data || []
    allModels.value = rows.map(m => ({
      code: m.code || m.model_code || m.name,
      name: m.displayName || m.name || m.code,
      displayName: m.displayName || m.name || m.code,
      provider: m.providerName || m.provider || '',
      providerName: m.providerName || m.provider || '',
      providerCode: m.providerCode || m.provider_code || '',
      // 本地模型标记
      local: (m.protocol === 'local' || (m.providerCode || '').startsWith('local-')),
      vision: !!(m.supportsVision || m.vision),
      audio: !!(m.supportsAudio || m.audio),
      // V7.1: 分类（self=自研, commercial=商业）
      category: m.category || 'commercial',
    }))
    // 默认选第一个
    if (allModels.value.length && !allModels.value.find(m => m.code === currentModel.value)) {
      currentModel.value = allModels.value[0].code
    }
  } catch (e) {
    console.warn('[Chat] 加载模型列表失败，使用默认模型:', e)
  }
}

function onModelChange() {
  const tag = routeTag.value
  if (tag.group === 'TrainedAgent') {
    // 找到对应的训练模型
    const tm = trainedModels.value.find(m => m.code === currentModel.value)
    ElMessage.success(`🧬 已切换到训练模型: ${tm?.name || currentModel.value}（准确率 ${tm?.accuracy || '-'}%）`)
  } else if (tag.group !== 'ChatAgent') {
    ElMessage.info(`已切换到 ${tag.label}，支持 ${currentModel.value} 特殊能力`)
  }
}

// ============ 工具函数 ============
function extractImageUrls(content) {
  if (!content) return []
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed.filter(i => i.type === 'image_url').map(i => i.image_url?.url)
    }
  } catch {}
  return []
}

function extractVideoUrls(content) {
  return [] // 视频 URL 暂从 content 里正则匹配
}

function formatContent(c) {
  if (!c) return ''
  const s = String(c)
  // 去掉 OpenAI image_url JSON 块
  const cleaned = s.replace(/\[\s*\{?"type"?:\s*"image_url".*?\}\s*\]/gs, '[图片附件]')
  return cleaned
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>')
}

async function scrollBottom() {
  await nextTick()
  if (scrollRef.value?.setScrollTop) {
    scrollRef.value.setScrollTop(999999)
  }
}

onMounted(async () => {
  await Promise.all([loadModels(), loadSessions(), loadKnowledgeBases(), loadAgents()])
  // V7.0 Flow⑤: 支持 ?sessionId=xxx 跳转到指定会话
  const urlParams = new URLSearchParams(window.location.search)
  const targetSessionId = urlParams.get('sessionId')
  if (targetSessionId) {
    const s = sessions.value.find(x => String(x.sessionId) === targetSessionId || String(x.id) === targetSessionId)
    if (s) {
      activeSessionId.value = s.id
      await loadMessages(s.id)
    }
  } else if (sessions.value.length) {
    activeSessionId.value = sessions.value[0].id
    await loadMessages(activeSessionId.value)
  }
})
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 88px);
  gap: 12px;
}
.session-list {
  width: 220px; flex-shrink: 0; background: #fff; border-radius: 8px;
  display: flex; flex-direction: column; overflow: hidden;
}
.session-header {
  padding: 12px; border-bottom: 1px solid #f0f0f0;
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; font-weight: 600;
}
.session-menu { border-right: none; }
.session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.session-loading {
  display: flex; align-items: center; justify-content: center;
  min-height: 100px; padding: 16px;
}
.kb-selector-wrap {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.chat-main {
  flex: 1; background: #fff; border-radius: 8px; display: flex; flex-direction: column; overflow: hidden;
}

.model-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px; border-bottom: 1px solid #f0f0f0; flex-shrink: 0;
}
.route-info { font-size: 12px; }

.message-list { flex: 1; padding: 16px; }
.messages { display: flex; flex-direction: column; gap: 16px; }
.message {
  display: flex; gap: 10px;
  &.user { flex-direction: row-reverse; .msg-bubble { background: #3b82f6; color: #fff; } }
  &.assistant { .msg-bubble { background: #f4f4f5; color: #333; } }
}
.msg-avatar { flex-shrink: 0; background: #dbeafe; color: #1d4ed8; font-weight: 700; }
.msg-content { max-width: 72%; }
.msg-meta { font-size: 11px; color: #999; margin-bottom: 4px; display: flex; align-items: center; }

.msg-bubble {
  padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
  :deep(code) { background: rgba(0,0,0,0.08); padding: 1px 5px; border-radius: 3px; font-family: monospace; }
}
.attachment-row { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
.attachment-chip {
  display: flex; align-items: center; gap: 4px; padding: 3px 8px;
  background: #f0f9eb; border: 1px solid #e1f3d8; border-radius: 4px;
  font-size: 12px; color: #67c23a;
}
.pending-attachments {
  display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px;
}
.pending-chip {
  display: flex; align-items: center; gap: 4px; padding: 4px 10px;
  background: #ecf5ff; border: 1px solid #d9ecff; border-radius: 20px;
  font-size: 12px; color: #409eff; cursor: default;
  .remove-btn { cursor: pointer; margin-left: 4px; opacity: 0.6; &:hover { opacity: 1; } }
}
.inline-images { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }

.welcome-msg { text-align: center; padding: 60px 0; color: #909399; }

.loading-dots { display: flex; gap: 4px; align-items: center;
  span { width: 6px; height: 6px; background: #999; border-radius: 50%; animation: bounce 1.4s infinite; }
  span:nth-child(2) { animation-delay: 0.2s; }
  span:nth-child(3) { animation-delay: 0.4s; }
}
@keyframes bounce { 0%,80%,100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }

.streaming-indicator { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #67c23a; }
.dot { width: 6px; height: 6px; background: #67c23a; border-radius: 50%; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }

.chat-input { padding: 12px 16px; border-top: 1px solid #f0f0f0; flex-shrink: 0; }
.input-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }

// V7.3 消息气泡动作按钮
.msg-bubble { position: relative; }
.msg-actions {
  display: flex; gap: 4px; margin-top: 6px;
  :deep(.el-button) { font-size: 12px; padding: 2px 8px; color: #909399; }
  :deep(.el-button:hover) { color: #409eff; }
}

// V7.3 附件预览行
.attachment-row { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
.attachment-chip {
  display: flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: 12px;
  background: #f0f7ff; border: 1px solid #d9ecff;
  font-size: 11px; color: #409eff;
}

// V7.3 视觉分析气泡样式
.analysis-bubble {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: #333;
}
</style>
