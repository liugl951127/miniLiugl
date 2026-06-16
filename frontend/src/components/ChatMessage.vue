<!--
  ChatMessage.vue
  单条消息气泡 - 支持:
    - 文本 (Markdown 渲染)
    - 图片附件 (缩略图 + 点击放大)
    - 工具调用记录 (折叠显示)
    - 来源引用 (RAG sources)
    - 流式打字机
    - 时间戳 + 复制 + 重试
-->
<template>
  <div :class="['msg', { 'msg-user': role === 'user', 'msg-ai': role !== 'user' }]">
    <!-- 头像 -->
    <div class="msg-avatar">
      <el-avatar :size="36" :style="{ background: avatarBg }">
        <el-icon><component :is="avatarIcon" /></el-icon>
      </el-avatar>
    </div>

    <div class="msg-body">
      <!-- 头部: 角色 + 时间 + 状态 -->
      <div class="msg-meta">
        <span class="msg-role">{{ roleLabel }}</span>
        <span class="msg-time">{{ timeStr }}</span>
        <span v-if="streaming" class="msg-status">
          <el-icon class="is-loading"><Loading /></el-icon>
          生成中...
        </span>
        <span v-else-if="status === 'error'" class="msg-status msg-status-error">
          <el-icon><CircleClose /></el-icon>
          失败
        </span>
      </div>

      <!-- 图片附件 -->
      <div v-if="images && images.length" class="msg-images">
        <div
          v-for="(img, i) in images"
          :key="i"
          class="msg-image"
          @click="previewImage(img)"
        >
          <img :src="img" alt="attachment" />
        </div>
      </div>

      <!-- 正文 (Markdown 渲染) -->
      <div v-if="content" class="msg-content">
        <MarkdownView :source="content" />
        <span v-if="streaming && !content" class="typing-dot"></span>
        <span v-if="streaming && content" class="typing-cursor">▍</span>
      </div>

      <!-- 工具调用 -->
      <details v-if="toolCalls && toolCalls.length" class="msg-tools">
        <summary>
          <el-icon><Tools /></el-icon>
          调用了 {{ toolCalls.length }} 个工具
        </summary>
        <div v-for="(tc, i) in toolCalls" :key="i" class="msg-tool">
          <div class="msg-tool-head">
            <span class="msg-tool-name">{{ tc.name }}</span>
            <el-tag :type="tc.status === 'ok' ? 'success' : 'danger'" size="small">
              {{ tc.status }} · {{ tc.durationMs }}ms
            </el-tag>
          </div>
          <div class="msg-tool-args">{{ truncate(tc.arguments, 120) }}</div>
          <div class="msg-tool-result">{{ truncate(tc.result, 200) }}</div>
        </div>
      </details>

      <!-- 来源引用 (RAG) -->
      <div v-if="sources && sources.length" class="msg-sources">
        <div class="msg-sources-title">
          <el-icon><Document /></el-icon>
          引用 ({{ sources.length }})
        </div>
        <div
          v-for="(src, i) in sources"
          :key="i"
          class="msg-source"
          @click="$emit('openSource', src)"
        >
          <span class="msg-source-idx">[{{ i + 1 }}]</span>
          <span class="msg-source-title">{{ src.docTitle || '未知文档' }}</span>
          <span class="msg-source-score">相似度 {{ (src.score * 100).toFixed(0) }}%</span>
        </div>
      </div>

      <!-- 操作 -->
      <div v-if="!streaming && role !== 'user'" class="msg-actions">
        <el-button text size="small" @click="copyContent">
          <el-icon><CopyDocument /></el-icon>
          复制
        </el-button>
        <el-button v-if="onRetry" text size="small" @click="$emit('retry')">
          <el-icon><RefreshRight /></el-icon>
          重试
        </el-button>
        <el-button v-if="onLike" text size="small" @click="$emit('like')">
          <el-icon><Star /></el-icon>
          收藏
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  User, ChatDotRound, Loading, CircleClose, Tools,
  CopyDocument, RefreshRight, Star, Document,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownView from './MarkdownView.vue'

const props = defineProps({
  role: { type: String, default: 'assistant' },
  content: { type: String, default: '' },
  images: { type: Array, default: () => [] },
  toolCalls: { type: Array, default: () => [] },
  sources: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false },
  status: { type: String, default: 'ok' },
  createdAt: { type: [Date, String], default: () => new Date() },
  onRetry: Function,
  onLike: Function,
})
defineEmits(['retry', 'like', 'openSource'])

const roleLabel = computed(() => {
  return { user: '我', assistant: 'AI 助手', system: '系统', tool: '工具' }[props.role] || 'AI'
})

const avatarIcon = computed(() => {
  return { user: User, assistant: ChatDotRound, system: Tools, tool: Tools }[props.role] || ChatDotRound
})

const avatarBg = computed(() => {
  return { user: '#6366f1', assistant: '#10b981', system: '#6b7280', tool: '#f59e0b' }[props.role] || '#10b981'
})

const timeStr = computed(() => {
  const d = props.createdAt instanceof Date ? props.createdAt : new Date(props.createdAt)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.substring(0, n) + '...' : s
}

function copyContent() {
  navigator.clipboard.writeText(props.content || '').then(() => {
    ElMessage.success('已复制')
  })
}

function previewImage(url) {
  // 全屏预览
  const w = window.open('')
  w.document.write(`
    <html><head><title>图片预览</title>
    <style>body{margin:0;background:#111;display:flex;align-items:center;justify-content:center;height:100vh}
    img{max-width:95vw;max-height:95vh;border-radius:8px}</style></head>
    <body><img src="${url}" /></body></html>
  `)
}
</script>

<style lang="scss" scoped>
.msg {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  margin: 8px 0;
  animation: slideIn 0.3s ease;
}
@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-user {
  flex-direction: row-reverse;
  background: linear-gradient(90deg, #eef2ff 0%, #f5f3ff 100%);
  border-radius: 12px;
  margin-left: 60px;
}
.msg-ai {
  background: #f9fafb;
  border-radius: 12px;
  margin-right: 60px;
}

.msg-avatar { flex-shrink: 0; }

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}
.msg-role { font-weight: 600; color: #374151; }
.msg-time { color: #9ca3af; }
.msg-status { display: inline-flex; align-items: center; gap: 2px; color: #6366f1; }
.msg-status-error { color: #ef4444; }
.is-loading { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.msg-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.msg-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  transition: transform .2s;
}
.msg-image:hover { transform: scale(1.05); box-shadow: 0 4px 12px rgba(0,0,0,.1); }
.msg-image img { width: 100%; height: 100%; object-fit: cover; }

.msg-content {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  background: white;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.typing-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #6366f1;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
.typing-cursor {
  display: inline-block;
  color: #6366f1;
  animation: blink 1s infinite;
  font-weight: bold;
}
@keyframes blink { 50% { opacity: 0; } }

.msg-tools {
  margin-top: 8px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  summary {
    cursor: pointer;
    color: #b45309;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}
.msg-tool {
  margin-top: 8px;
  padding: 6px 10px;
  background: white;
  border-radius: 4px;
  border-left: 3px solid #f59e0b;
}
.msg-tool-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.msg-tool-name { font-weight: 600; color: #92400e; }
.msg-tool-args, .msg-tool-result {
  font-family: monospace;
  font-size: 12px;
  color: #6b7280;
  word-break: break-all;
}

.msg-sources {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  font-size: 12px;
}
.msg-sources-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #15803d;
  font-weight: 600;
  margin-bottom: 6px;
}
.msg-source {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  transition: background .2s;
  margin: 2px 0;
}
.msg-source:hover { background: #dcfce7; }
.msg-source-idx { color: #15803d; font-weight: 600; min-width: 24px; }
.msg-source-title { flex: 1; color: #166534; }
.msg-source-score { color: #65a30d; font-family: monospace; }

.msg-actions {
  margin-top: 6px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity .2s;
}
.msg:hover .msg-actions { opacity: 1; }
</style>
