<template>
  <div class="notif-page">
    <div class="notif-header">
      <div class="header-title">
        <el-icon><Bell /></el-icon>
        通知中心
        <el-badge :value="notifStore.unreadCount" :hidden="!notifStore.unreadCount" type="danger" />
      </div>
      <div class="header-actions">
        <el-button size="small" @click="onMarkAllRead" :disabled="!notifStore.unreadCount">
          全部已读
        </el-button>
        <el-button size="small" type="danger" plain @click="onClear" :disabled="!notifStore.notifications.length">
          清空
        </el-button>
      </div>
    </div>

    <!-- WS 连接状态 -->
    <div class="ws-status" :class="notifStore.wsConnected ? 'online' : 'offline'">
      <span class="dot"></span>
      {{ notifStore.wsConnected ? '实时推送已连接' : '实时推送未连接' }}
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && !notifStore.notifications.length" description="暂无通知" />

    <!-- 通知列表 -->
    <div v-else class="notif-list">
      <div
        v-for="n in notifStore.notifications"
        :key="n.id"
        class="notif-item"
        :class="{ unread: !n.isRead }"
        @click="onItemClick(n)"
      >
        <div class="notif-icon">
          <el-icon size="20">
            <component :is="typeIcon(n.type)" />
          </el-icon>
        </div>
        <div class="notif-body">
          <div class="notif-title">{{ n.title }}</div>
          <div v-if="n.content" class="notif-content">{{ n.content }}</div>
          <div class="notif-time">{{ formatTime(n.createdAt) }}</div>
        </div>
        <div class="notif-action" v-if="!n.isRead" @click.stop="onMarkRead(n.id)">
          <el-tag type="success" size="small" effect="light" class="mark-read-btn">标为已读</el-tag>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNotificationStore } from '@/store/notification'
import {
  Bell, ChatDotRound, DocumentChecked, MagicStick,
  FolderChecked, Loading
} from '@element-plus/icons-vue'

const notifStore = useNotificationStore()
const loading = ref(false)

// ── 图标映射 ────────────────────────────────────────────────────────────
function typeIcon(type) {
  const map = {
    'SESSION_CREATED': 'ChatDotRound',
    'AGENT_COMPLETE': 'MagicStick',
    'DOC_APPROVED': 'DocumentChecked',
    'DOC_REJECTED': 'FolderChecked',
    'MEMORY_SAVED': 'Bell',
  }
  const icon = map[type] || 'Bell'
  return icon
}

// ── 时间格式化 ──────────────────────────────────────────────────────────
function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleString()
}

// ── 操作 ────────────────────────────────────────────────────────────────
async function onItemClick(n) {
  if (!n.isRead) {
    await notifStore.markRead(n.id)
  }
}

async function onMarkRead(id) {
  await notifStore.markRead(id)
  ElMessage.success('已标记已读')
}

async function onMarkAllRead() {
  await notifStore.markAllRead()
  ElMessage.success('已全部已读')
}

async function onClear() {
  try {
    await ElMessageBox.confirm('确定清空所有通知？此操作不可恢复。', '清空通知', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await notifStore.clear()
    ElMessage.success('已清空')
  } catch (_) {}
}

// ── 初始化 ──────────────────────────────────────────────────────────────
onMounted(async () => {
  loading.value = true
  try {
    await notifStore.fetchList(1, 50)
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.notif-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 16px;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--minimax-text, #303133);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  width: fit-content;

  &.online { color: #10b981; background: #d1fae5; }
  &.offline { color: #f59e0b; background: #fef3c7; }

  .dot {
    width: 6px; height: 6px;
    border-radius: 50%;
    background: currentColor;
  }
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s;
  border: 1px solid transparent;

  &:hover {
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    transform: translateY(-1px);
  }

  &.unread {
    background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
    border-color: rgba(91, 141, 239, 0.2);

    .notif-title { font-weight: 600; color: #1a1a2e; }
    .notif-icon { color: #5b8def; }
  }
}

.notif-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--minimax-bg, #f5f7fa);
  border-radius: 8px;
  flex-shrink: 0;
  color: #909399;
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 4px;
}

.notif-content {
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notif-time {
  font-size: 12px;
  color: #c0c4cc;
}

.notif-action {
  flex-shrink: 0;
}

.mark-read-btn {
  cursor: pointer;
  &:hover { opacity: 0.8; }
}

.loading-more {
  text-align: center;
  padding: 16px;
  color: #909399;
}
</style>