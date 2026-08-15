<template>
  <el-popover
    :width="380"
    trigger="click"
    placement="bottom-end"
    popper-class="notif-popover"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button text circle :title="t('notification.title')">
          <el-icon :size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    
    <div class="notif-panel">
      <div class="notif-header">
        <h4>{{ t('notification.title') }}</h4>
        <div class="actions">
          <el-button link size="small" @click="markAllRead" v-if="unreadCount > 0">
            {{ t('notification.markAllRead') }}
          </el-button>
          <el-button link size="small" @click="clearAll" v-if="notifications.length">
            {{ t('notification.clearAll') }}
          </el-button>
        </div>
      </div>
      
      <el-tabs v-model="activeTab" class="notif-tabs">
        <el-tab-pane :label="t('notification.all')" name="all" />
        <el-tab-pane :label="t('notification.unread')" name="unread" :badge="unreadCount || ''" />
        <el-tab-pane :label="t('notification.mentions')" name="mention" />
      </el-tabs>
      
      <div class="notif-list">
        <transition-group name="notif">
          <div
            v-for="n in filteredNotifications"
            :key="n.id"
            class="notif-item"
            :class="[`type-${n.type}`, { unread: !n.read }]"
            @click="handleClick(n)"
          >
            <el-icon class="notif-icon" :size="20">
              <component :is="iconFor(n.type)" />
            </el-icon>
            <div class="notif-body">
              <div class="notif-title">{{ n.title }}</div>
              <div class="notif-desc">{{ n.description }}</div>
              <div class="notif-time">{{ formatTime(n.timestamp) }}</div>
            </div>
            <div v-if="!n.read" class="unread-dot" />
          </div>
        </transition-group>
        
        <div v-if="filteredNotifications.length === 0" class="notif-empty">
          <el-icon :size="40"><BellFilled /></el-icon>
          <p>{{ t('notification.empty') }}</p>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bell, BellFilled, InfoFilled, CircleCheck, Warning, CircleClose, ChatDotRound, Star } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'
import { listNotifications, markAllRead as apiMarkAllRead, markRead as apiMarkRead, deleteNotification as apiDeleteNotification } from '@/api/notification'

const { t } = useI18n()

const notifications = ref([])
const activeTab = ref('all')
const loading = ref(false)

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const filteredNotifications = computed(() => {
  if (activeTab.value === 'unread') return notifications.value.filter(n => !n.read)
  if (activeTab.value === 'mention') return notifications.value.filter(n => n.mention)
  return notifications.value
})

function iconFor(type) {
  const map = {
    info: InfoFilled,
    success: CircleCheck,
    warning: Warning,
    error: CircleClose,
    message: ChatDotRound,
    mention: Star
  }
  return map[type] || InfoFilled
}

async function loadNotifications() {
  loading.value = true
  try {
    const r = await listNotifications({ page: 1, size: 50 })
    // 兼容后端返回格式: { data: { records: [...] } } 或 { records: [...] } 或直接数组
    const list = r?.data?.records || r?.data || r || []
    notifications.value = list.map(n => ({
      id: n.id,
      type: n.type || 'info',
      title: n.title,
      description: n.content || n.description || '',
      timestamp: n.createdAt ? new Date(n.createdAt).getTime() : Date.now(),
      read: n.read === true || n.read === 1,
      link: n.link || n.url || '',
      mention: n.type === 'mention',
    }))
  } catch (e) {
    console.error('[Notification] 加载失败:', e)
    // 降级: 不显示任何通知
    notifications.value = []
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  try {
    await apiMarkAllRead()
    notifications.value.forEach(n => n.read = true)
  } catch (e) {
    console.error('[Notification] 全部已读失败:', e)
  }
}

async function clearAll() {
  notifications.value = []
}

async function handleClick(n) {
  if (!n.read) {
    try {
      await apiMarkRead(n.id)
      n.read = true
    } catch (e) {
      console.error('[Notification] 标记已读失败:', e)
    }
  }
  if (n.link) {
    window.location.href = n.link
  }
}

function formatTime(timestamp) {
  const diff = Date.now() - new Date(timestamp).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return t('notification.justNow')
  if (min < 60) return t('notification.minutesAgo', { n: min })
  const hr = Math.floor(min / 60)
  if (hr < 24) return t('notification.hoursAgo', { n: hr })
  const day = Math.floor(hr / 24)
  if (day < 7) return t('notification.daysAgo', { n: day })
  return new Date(timestamp).toLocaleDateString()
}

// V7.1: 从后端真实加载通知列表
onMounted(() => {
  loadNotifications()
})
</script>

<style lang="scss" scoped>
.notif-panel {
  width: 100%;
  max-height: 500px;
  display: flex;
  flex-direction: column;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  
  h4 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
  }
}

.actions {
  display: flex;
  gap: 4px;
}

.notif-tabs {
  margin-bottom: 8px;
}

.notif-list {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
}

.notif-item {
  display: flex;
  gap: 12px;
  padding: 12px 8px;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  
  &:hover {
    background: var(--el-fill-color-light);
  }
  
  &.unread {
    background: var(--el-color-primary-light-9);
    
    &:hover {
      background: var(--el-color-primary-light-8);
    }
  }
}

.notif-icon {
  flex-shrink: 0;
  margin-top: 2px;
  
  .type-success & { color: var(--el-color-success); }
  .type-warning & { color: var(--el-color-warning); }
  .type-error & { color: var(--el-color-danger); }
  .type-info & { color: var(--el-color-info); }
  .type-message & { color: var(--el-color-primary); }
  .type-mention & { color: var(--el-color-warning); }
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
}

.notif-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: var(--el-color-danger);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.notif-empty {
  padding: 60px 0;
  text-align: center;
  color: var(--el-text-color-secondary);
  
  p {
    margin: 12px 0 0 0;
    font-size: 13px;
  }
}

.notif-enter-active,
.notif-leave-active {
  transition: all 0.3s;
}

.notif-enter-from,
.notif-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
