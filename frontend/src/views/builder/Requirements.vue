<!--
  @file builder/Requirements.vue - 需求接收 (V1.0)
  路由: /builder/requirements
  3 种方式: 文档上传 / AI 对话 / 模板库
-->
<template>
  <div class="req-page">
    <!-- 3 列入口 -->
    <div class="entry-grid">
      <div
        v-for="m in methods" :key="m.key"
        class="entry-card" :class="{ active: activeMethod === m.key }"
        @click="activeMethod = m.key"
      >
        <div class="entry-icon" :style="{ background: m.bg, color: m.fg }">{{ m.icon }}</div>
        <div class="entry-name">{{ m.name }}</div>
        <div class="entry-desc">{{ m.desc }}</div>
      </div>
    </div>

    <!-- 方式 1: 文档上传 -->
    <div v-if="activeMethod === 'upload'" class="method-panel">
      <div
        class="drop-zone"
        :class="{ dragover: dragover, hasfile: uploadedFile }"
        @dragover.prevent="dragover = true"
        @dragleave="dragover = false"
        @drop.prevent="onDrop"
      >
        <div v-if="!uploadedFile" class="drop-content">
          <div class="drop-icon">📄</div>
          <h3>拖拽文档到这里, 或点击选择</h3>
          <p>支持 PDF / Word / Markdown / TXT, 最大 20MB</p>
          <el-button type="primary" size="large" round :icon="Upload" @click="$refs.fileInput.click()">选择文件</el-button>
          <input ref="fileInput" type="file" accept=".pdf,.doc,.docx,.md,.txt" hidden @change="onFileSelect" />
        </div>
        <div v-else class="drop-loaded">
          <div class="loaded-icon">✓</div>
          <div class="loaded-info">
            <div class="loaded-name">{{ uploadedFile.name }}</div>
            <div class="loaded-meta">{{ formatSize(uploadedFile.size) }} · 解析中...</div>
          </div>
          <el-button :icon="Refresh" @click="resetFile">重新上传</el-button>
        </div>
      </div>

      <!-- 上传后展示解析预览 -->
      <div v-if="uploadedFile" class="preview-card">
        <h4>📋 解析预览</h4>
        <el-skeleton v-if="parsing" :rows="6" animated />
        <div v-else class="preview-content">
          <p class="preview-text">{{ parsedContent }}</p>
          <el-divider />
          <div class="extract-tags">
            <el-tag v-for="t in extractedTags" :key="t" effect="plain" round>{{ t }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 方式 2: AI 对话 -->
    <div v-if="activeMethod === 'chat'" class="method-panel">
      <div class="chat-panel">
        <div class="chat-msgs" ref="chatScroll">
          <div v-for="(m, i) in chatMsgs" :key="i" :class="['msg', m.role]">
            <div class="msg-avatar">{{ m.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="msg-content">{{ m.text }}</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="chatInput" type="textarea" :rows="2"
            placeholder="描述你的需求, 例如: 我要做一个在线教育的智能客服系统, 能处理学员咨询、退费、课程推荐..."
            @keydown.ctrl.enter="sendChat"
          />
          <el-button type="primary" :loading="chatLoading" :icon="Promotion" @click="sendChat">发送 (Ctrl+Enter)</el-button>
        </div>
        <div class="quick-questions">
          <span style="font-size:12px;color:#64748b">快速问:</span>
          <el-tag v-for="q in quickQuestions" :key="q" effect="plain" round
            class="qq" @click="chatInput = q">{{ q }}</el-tag>
        </div>
      </div>
    </div>

    <!-- 方式 3: 模板库 -->
    <div v-if="activeMethod === 'template'" class="method-panel">
      <div class="template-grid">
        <div
          v-for="t in templates" :key="t.key"
          class="template-card" @click="selectTemplate(t)"
        >
          <div class="tpl-cover" :style="{ background: t.cover }">
            <span class="tpl-emoji">{{ t.emoji }}</span>
          </div>
          <div class="tpl-body">
            <div class="tpl-name">{{ t.name }}</div>
            <div class="tpl-desc">{{ t.desc }}</div>
            <div class="tpl-meta">
              <el-tag v-for="a in t.agents" :key="a" size="small" effect="plain" round>{{ a }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下一步按钮 -->
    <div class="next-bar">
      <el-button size="large" round :icon="ArrowRight" type="primary"
        :disabled="!hasInput" @click="goNext">
        下一步: AI 解析 →
      </el-button>
      <span v-if="!hasInput" class="hint">请先输入或选择需求</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, Refresh, Promotion, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const activeMethod = ref('upload')

const methods = [
  { key: 'upload',   name: '上传文档', icon: '📄', desc: 'PDF / Word / Markdown', bg: 'linear-gradient(135deg, #dbeafe, #ede9fe)', fg: '#6366f1' },
  { key: 'chat',     name: 'AI 对话',  icon: '💬', desc: '与 AI 助手多轮澄清',   bg: 'linear-gradient(135deg, #d1fae5, #d1fae5)', fg: '#10b981' },
  { key: 'template', name: '模板库',   icon: '📋', desc: '一键应用行业方案',     bg: 'linear-gradient(135deg, #fef3c7, #fce7f3)', fg: '#f59e0b' }
]

const dragover = ref(false)
const uploadedFile = ref(null)
const parsing = ref(false)
const parsedContent = ref('')
const extractedTags = ref([])

function onDrop(e) {
  dragover.value = false
  const f = e.dataTransfer.files[0]
  if (f) handleFile(f)
}
function onFileSelect(e) {
  const f = e.target.files[0]
  if (f) handleFile(f)
}
function handleFile(f) {
  uploadedFile.value = f
  parsing.value = true
  // 模拟 AI 解析
  setTimeout(() => {
    parsedContent.value = `${f.name} 是一个面向教育行业的智能客服系统需求文档...`
    extractedTags.value = ['🎓 教育行业', '💬 客服场景', '👥 多角色', '📊 数据分析', '🔒 数据合规', '🌐 多语言']
    parsing.value = false
  }, 1500)
}
function resetFile() {
  uploadedFile.value = null
  parsedContent.value = ''
  extractedTags.value = []
}
function formatSize(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

const chatMsgs = ref([
  { role: 'ai', text: '你好! 我是 Agent Forge 的需求澄清助手。请描述你的项目, 我会帮你梳理成结构化需求。' }
])
const chatInput = ref('')
const chatLoading = ref(false)
const chatScroll = ref(null)
const quickQuestions = ref([
  '电商客服系统', '代码评审助手', '金融风控平台', '医疗问诊机器人'
])

async function sendChat() {
  if (!chatInput.value.trim()) return
  chatMsgs.value.push({ role: 'user', text: chatInput.value })
  const q = chatInput.value
  chatInput.value = ''
  chatLoading.value = true
  await nextTick(); scrollChat()
  setTimeout(() => {
    chatMsgs.value.push({
      role: 'ai',
      text: `好的, "${q.slice(0, 30)}..." 这类项目我已记录。我会基于此生成智能体群方案, 包括: 项目角色 (PM/客服/质检), 工具集 (订单查询/工单), 部署目标 (容器化)。请确认或补充其他需求。`
    })
    chatLoading.value = false
    nextTick(scrollChat)
  }, 1200)
}
function scrollChat() {
  if (chatScroll.value) chatScroll.value.scrollTop = chatScroll.value.scrollHeight
}

const templates = [
  { key: 'edu',     name: '在线教育客服', emoji: '🎓', cover: 'linear-gradient(135deg, #6366f1, #8b5cf6)', desc: '课程咨询 / 退费处理 / 学习指导', agents: ['客服', '质检', '推荐'] },
  { key: 'ecom',    name: '电商客服系统', emoji: '🛒', cover: 'linear-gradient(135deg, #f59e0b, #ef4444)', desc: '订单查询 / 退换货 / 评价',     agents: ['客服', '订单', '物流'] },
  { key: 'code',    name: '代码评审助手', emoji: '💻', cover: 'linear-gradient(135deg, #10b981, #06b6d4)', desc: 'PR 审查 / 规范检查 / 建议',     agents: ['审查', '规范', '测试'] },
  { key: 'finance', name: '金融风控平台', emoji: '🏦', cover: 'linear-gradient(135deg, #1e293b, #475569)', desc: '欺诈检测 / 信用评估 / 告警',   agents: ['风控', '审核', '告警'] },
  { key: 'medical', name: '医疗问诊机器人', emoji: '⚕️', cover: 'linear-gradient(135deg, #ec4899, #f43f5e)', desc: '症状问诊 / 导诊 / 知识库',     agents: ['问诊', '导诊', '知识'] },
  { key: 'custom',  name: '自定义项目',   emoji: '✨', cover: 'linear-gradient(135deg, #8b5cf6, #ec4899)', desc: '自由组合任意智能体',           agents: ['灵活'] }
]
function selectTemplate(t) {
  ElMessage.success(`已选择模板: ${t.name}, 进入解析...`)
  setTimeout(() => goNext(), 800)
}

const hasInput = computed(() => uploadedFile.value || chatMsgs.value.length > 1 || activeMethod.value === 'template')

function goNext() {
  router.push('/builder/analysis')
}
</script>

<style scoped>
.req-page { max-width: 1200px; margin: 0 auto; }

.entry-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 16px; margin-bottom: 20px;
}
.entry-card {
  padding: 24px; background: white; border-radius: 16px;
  border: 2px solid transparent; cursor: pointer;
  text-align: center; transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.entry-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.08); }
.entry-card.active {
  border-color: #6366f1;
  background: linear-gradient(180deg, #f5f7ff 0%, #fafbff 100%);
}
.entry-icon {
  width: 56px; height: 56px; border-radius: 14px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 28px; margin-bottom: 12px;
}
.entry-name { font-size: 16px; font-weight: 600; color: #1e293b; }
.entry-desc { font-size: 12px; color: #64748b; margin-top: 4px; }

.method-panel {
  background: white; border-radius: 16px; padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* Drop zone */
.drop-zone {
  border: 2px dashed #cbd5e1; border-radius: 14px;
  padding: 60px 20px; text-align: center;
  transition: all 0.2s; background: #fafbfc;
}
.drop-zone.dragover { border-color: #6366f1; background: #f0f7ff; }
.drop-zone.hasfile { padding: 30px 20px; }
.drop-content h3 { margin: 12px 0 6px; color: #1e293b; }
.drop-content p { color: #64748b; margin: 0 0 16px; font-size: 13px; }
.drop-icon { font-size: 48px; }
.drop-loaded { display: flex; align-items: center; gap: 16px; }
.loaded-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700;
}
.loaded-info { flex: 1; text-align: left; }
.loaded-name { font-weight: 600; color: #1e293b; }
.loaded-meta { font-size: 12px; color: #64748b; margin-top: 2px; }

.preview-card { margin-top: 20px; padding: 20px; background: #f8fafc; border-radius: 12px; }
.preview-card h4 { margin: 0 0 12px; color: #1e293b; }
.preview-text { color: #334155; line-height: 1.7; margin: 0; }
.extract-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }

/* Chat */
.chat-panel { display: flex; flex-direction: column; height: 480px; }
.chat-msgs {
  flex: 1; overflow-y: auto; padding: 16px;
  background: #fafbfc; border-radius: 12px; margin-bottom: 12px;
}
.msg { display: flex; gap: 10px; margin-bottom: 14px; }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: #f1f5f9; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-size: 16px;
}
.msg-content {
  padding: 10px 14px; background: white; border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  max-width: 75%; line-height: 1.6; color: #1e293b; font-size: 14px;
}
.msg.user { flex-direction: row-reverse; }
.msg.user .msg-content { background: #6366f1; color: white; }
.chat-input { display: flex; gap: 8px; align-items: flex-end; margin-bottom: 8px; }
.chat-input :deep(.el-textarea) { flex: 1; }
.quick-questions { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.qq { cursor: pointer; transition: all 0.2s; }
.qq:hover { transform: translateY(-1px); }

/* Templates */
.template-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.template-card {
  border-radius: 14px; overflow: hidden; cursor: pointer;
  background: white; border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.template-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.tpl-cover {
  height: 100px; display: flex; align-items: center; justify-content: center;
}
.tpl-emoji { font-size: 48px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); }
.tpl-body { padding: 14px; }
.tpl-name { font-weight: 600; color: #1e293b; font-size: 14px; }
.tpl-desc { font-size: 12px; color: #64748b; margin: 4px 0 8px; }
.tpl-meta { display: flex; gap: 4px; flex-wrap: wrap; }

/* Next */
.next-bar { display: flex; align-items: center; gap: 12px; margin-top: 20px; justify-content: flex-end; }
.next-bar .hint { font-size: 12px; color: #94a3b8; }
</style>
