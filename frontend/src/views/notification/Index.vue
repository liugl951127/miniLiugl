<!-- @file notification/Index.vue - 通知中心 V6.8.12 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🔔 通知中心</h2>
      <div style="display:flex;gap:8px">
        <el-badge :value="unreadCount" :hidden="!unreadCount">
          <el-button size="small" :loading="markingAll" :disabled="!unreadCount" @click="doMarkAllRead">全部已读</el-button>
        </el-badge>
        <el-button size="small" :loading="loading" @click="loadNotifications">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" link type="primary" @click="showSettings = true">
          <el-icon><Setting /></el-icon>设置
        </el-button>
      </div>
    </div>

    <!-- 通知统计 -->
    <el-row :gutter="12" style="margin-bottom:16px" v-loading="loading">
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div class="stat-num" style="color:var(--el-color-primary)">{{ notifications.length }}</div>
        <div class="stat-label">总通知</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div class="stat-num" style="color: var(--el-color-danger)">{{ unreadCount }}</div>
        <div class="stat-label">未读</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div class="stat-num" style="color: var(--el-color-success)">{{ systemCount }}</div>
        <div class="stat-label">系统通知</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div class="stat-num" style="color: var(--el-color-warning)">{{ taskCount }}</div>
        <div class="stat-label">任务通知</div>
      </el-card></el-col>
    </el-row>

    <!-- 筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <el-radio-group v-model="typeFilter" size="small" @change="loadNotifications">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="SYSTEM">系统</el-radio-button>
        <el-radio-button value="TASK">任务</el-radio-button>
        <el-radio-button value="MESSAGE">消息</el-radio-button>
        <el-radio-button value="WARNING">警告</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="readFilter" size="small" @change="loadNotifications">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="false">未读</el-radio-button>
        <el-radio-button value="true">已读</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 通知列表 -->
    <div class="notif-list" v-loading="loading">
      <template v-if="!loading">
      <div
        v-for="n in notifications" :key="n.id"
        class="notif-item"
        :class="{ unread: !n.read }"
        @click="openNotification(n)"
      >
        <div class="notif-icon">
          <el-icon :size="20" :color="iconColor(n.type)">{{ iconName(n.type) }}</el-icon>
        </div>
        <div class="notif-body">
          <div class="notif-title">
            <span>{{ n.title }}</span>
            <span v-if="!n.read" class="unread-badge"></span>
          </div>
          <div class="notif-msg">{{ n.message }}</div>
          <div class="notif-meta">
            <el-tag size="small" :type="typeTag(n.type)">{{ typeName(n.type) }}</el-tag>
            <span style="font-size:11px;color:var(--el-text-color-placeholder);margin-left:8px">{{ n.createdAt }}</span>
          </div>
        </div>
        <div class="notif-actions" @click.stop>
          <el-button v-if="!n.read" size="small" link :loading="markingReadId === n.id" @click="markRead(n)">已读</el-button>
          <el-button size="small" link type="danger" :loading="deletingId === n.id" @click="deleteNotification(n)">删除</el-button>
        </div>
      </div>
      <el-empty
        v-if="!notifications.length"
        :description="notifEmptyText"
        :image-size="80"
      >
        <el-button v-if="notifEmptyTip" type="primary" size="small" @click="loadNotifications">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </el-empty>
      </template>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > pageSize"
      layout="prev, pager, next, total"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="page"
      style="margin-top:16px"
      @current-change="loadNotifications"
    />

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail?.title" width="520px">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="类型"><el-tag size="small" :type="typeTag(detail.type)">{{ typeName(detail.type) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div style="white-space:pre-wrap;font-size:14px;line-height:1.8">{{ detail.message }}</div>
        <div v-if="detail.data" style="margin-top:12px">
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-bottom:4px">附加数据</div>
          <pre style="background: var(--el-fill-color-light);padding:8px;border-radius:4px;font-size:12px">{{ JSON.stringify(detail.data, null, 2) }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail?.actionUrl" type="primary" @click="navigateToAction(detail.actionUrl)">查看详情</el-button>
      </template>
    </el-dialog>

    <!-- 通知设置弹窗 -->
    <el-dialog v-model="showSettings" title="通知设置" width="480px">
      <el-form label-width="120px">
        <el-form-item label="系统通知">
          <el-switch v-model="settings.system" />
        </el-form-item>
        <el-form-item label="任务通知">
          <el-switch v-model="settings.task" />
        </el-form-item>
        <el-form-item label="消息通知">
          <el-switch v-model="settings.message" />
        </el-form-item>
        <el-form-item label="警告通知">
          <el-switch v-model="settings.warning" />
        </el-form-item>
        <el-form-item label="邮件通知">
          <el-switch v-model="settings.email" />
        </el-form-item>
        <el-form-item label="声音提醒">
          <el-switch v-model="settings.sound" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listNotifications, markRead as markR, markAllRead, deleteNotification as deleteNotifApi } from '@/api/notification'
import {
  Refresh, Setting, Bell,
  SuccessFilled, WarningFilled, Message, InfoFilled, WarnTriangleFilled,
} from '@element-plus/icons-vue'

const notifications = ref([])
const loading = ref(false)
const markingAll = ref(false)
const markingReadId = ref(null)
const deletingId = ref(null)
const typeFilter = ref('')
const readFilter = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)
const showSettings = ref(false)

const settings = reactive({
  system: true, task: true, message: true, warning: true, email: false, sound: true,
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)
const systemCount = computed(() => notifications.value.filter(n => n.type === 'SYSTEM').length)
const taskCount = computed(() => notifications.value.filter(n => n.type === 'TASK').length)

const notifEmptyText = computed(() => {
  if (loading.value) return '加载中...'
  if (typeFilter.value || readFilter.value) return '没有符合筛选条件的通知'
  return '暂无通知，系统会在新事件发生时通知您'
})
const notifEmptyTip = computed(() => Boolean(typeFilter.value || readFilter.value))

function iconName(type) {
  return { SYSTEM: 'InfoFilled', TASK: 'SuccessFilled', MESSAGE: 'Message', WARNING: 'WarningFilled' }[type] || 'Bell'
}
function iconColor(type) {
  return { SYSTEM: '#409eff', TASK: '#67c23a', MESSAGE: '#909399', WARNING: '#f56c6c' }[type] || '#909399'
}
function typeTag(type) {
  return { SYSTEM: '', TASK: 'success', MESSAGE: 'info', WARNING: 'danger' }[type] || 'info'
}
function typeName(type) {
  return { SYSTEM: '系统', TASK: '任务', MESSAGE: '消息', WARNING: '警告' }[type] || type
}

async function loadNotifications() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (typeFilter.value) params.type = typeFilter.value
    if (readFilter.value) params.read = readFilter.value
    const r = await listNotifications(params)
    notifications.value = r.data?.list || r.data || []
    total.value = r.data?.total || notifications.value.length
  } catch (e) {
    notifications.value = []
    ElMessage.error('加载通知失败：' + (e.response?.data?.message || e.message || '请稍后重试'))
  } finally { loading.value = false }
}

async function markRead(n) {
  if (markingReadId.value !== null) return
  markingReadId.value = n.id
  try {
    await markR(n.id)
    n.read = true
  } catch (e) {
    ElMessage.error('标记已读失败：' + (e.response?.data?.message || e.message || ''))
  } finally {
    markingReadId.value = null
  }
}

async function doMarkAllRead() {
  if (markingAll.value) return
  markingAll.value = true
  try {
    await markAllRead()
    notifications.value.forEach(n => n.read = true)
    ElMessage.success(`已将 ${notifications.value.length} 条通知标记为已读`)
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message || ''))
  } finally {
    markingAll.value = false
  }
}

async function deleteNotification(n) {
  if (deletingId.value !== null) return
  deletingId.value = n.id
  try {
    await deleteNotifApi(n.id)
    notifications.value = notifications.value.filter(x => x.id !== n.id)
    ElMessage.success('通知已删除')
  } catch (e) {
    ElMessage.error('删除失败：' + (e.response?.data?.message || e.message || ''))
  } finally {
    deletingId.value = null
  }
}

function openNotification(n) {
  detail.value = n
  detailVisible.value = true
  if (!n.read) markRead(n)
}

function navigateToAction(url) {
  if (url) window.location.href = url
  detailVisible.value = false
}

function saveSettings() {
  ElMessage.success('通知设置已保存（仅本地生效）')
  showSettings.value = false
}

// 定时刷新
let refreshTimer = null
onMounted(() => {
  loadNotifications()
  refreshTimer = setInterval(loadNotifications, 30000)
})
onUnmounted(() => clearInterval(refreshTimer))
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.notif-list { display: flex; flex-direction: column; gap: 2px; }
.notif-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 16px; border-radius: 8px; cursor: pointer;
  transition: background 0.15s;
  background: transparent;
  &:hover { background: var(--el-fill-color-light); }
  &.unread { background: var(--el-color-primary-light-9); border-left: 3px solid var(--el-color-primary); }
  // 深色模式适配
  @at-root .dark & {
    border-left-color: var(--el-color-primary);
    &:hover { background: var(--el-fill-color-dark); }
    &.unread { background: var(--el-fill-color-dark); }
  }
}
.notif-icon {
  width: 36px; height: 36px; border-radius: 50%;
  background: var(--el-fill-color);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 8px; }
.unread-badge { width: 8px; height: 8px; border-radius: 50%; background: var(--el-color-primary); flex-shrink: 0; }
.notif-msg { font-size: 13px; color: var(--el-text-color-regular); margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notif-meta { display: flex; align-items: center; margin-top: 6px; }
.notif-actions { display: flex; flex-direction: column; gap: 4px; flex-shrink: 0; }

// Day 49: 深色模式适配 — 统计卡片数字颜色
.stat-num { font-size: 22px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
.stat-label { font-size: 12px; color: var(--el-text-color-placeholder); }
</style>
