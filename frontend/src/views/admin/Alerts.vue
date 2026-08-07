<!--
  @file views/admin/Alerts.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Alerts.vue (告警列表)
  @version V3.5.12+ (前端注释补全)
  @description 告警列表
-->
<template>
  <div class="page-alerts">
    <!-- 1. page-header -->
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
        <h2 class="page-title">{{ t('alerts.title') }}</h2>
        <p class="page-subtitle">{{ currentTabLabel }} · {{ totalAlerts }} 个告警 · {{ firingCount }} 个触发中</p>
      </div>
      <el-button :icon="Refresh" @click="loadAll" :loading="loading">刷新</el-button>
    </header>

    <!-- 2. section: 4 tabs (firing/rules/channels/history) -->
    <section class="section">
      <el-tabs v-model="tab" @tab-change="onTabChange" class="alert-tabs">
        <el-tab-pane label="触发中" name="firing">
          <span class="tab-label">触发中 <el-badge :value="firingCount" :max="99" type="danger" /></span>
        </el-tab-pane>
        <el-tab-pane label="告警规则" name="rules" />
        <el-tab-pane label="通知渠道" name="channels" />
        <el-tab-pane label="历史记录" name="history" />
      </el-tabs>
    </section>

    <!-- 3. section: 触发中告警 (firing) -->
    <section v-if="tab === 'firing'" class="section">
      <EmptyState :description="'暂无数据'" />
      <el-card v-for="alert in firing" :key="alert.id" shadow="hover" class="alert-card-item">
        <el-alert
          :type="severityType(alert.severity)"
          :title="`${alert.ruleName || alert.rule_id} - ${alert.message}`"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="alert-content">
              <p class="alert-meta">
                <el-tag size="small">{{ alert.metricName }}</el-tag>
                <el-tag size="small" type="info">阈值: {{ alert.threshold }}</el-tag>
                <el-tag size="small" type="warning">当前: {{ alert.metricValue }}</el-tag>
                <span v-if="alert.silencedUntil" class="silenced-tag">
                  <el-tag size="small" type="info">静默至 {{ formatTime(alert.silencedUntil) }}</el-tag>
                </span>
                <span class="time">{{ formatTime(alert.firedAt) }}</span>
              </p>
              <div class="alert-actions">
                <el-button size="small" type="primary" @click="openAckDialog(alert)">确认</el-button>
                <el-button v-if="!alert.silencedUntil" size="small" @click="openSilenceDialog(alert, 'instance')">静默</el-button>
                <el-button v-else size="small" type="warning" @click="doUnsilenceAlert(alert)">取消静默</el-button>
              </div>
            </div>
          </template>
        </el-alert>
      </el-card>
    </section>

    <!-- 4. section: 告警规则 (rules) -->
    <section v-else-if="tab === 'rules'" class="section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><SetUp /></el-icon>
            <span>告警规则 ({{ rules.length }})</span>
            <el-button :icon="Plus" type="primary" size="small" @click="openRuleDialog" style="margin-left: auto">新增</el-button>
          </div>
        </template>
        <el-table :data="rules" stripe>
          <el-table-column prop="name" label="规则名" min-width="160" />
          <el-table-column prop="service" label="服务" width="120" />
          <el-table-column prop="metricName" label="指标" width="140" />
          <el-table-column prop="threshold" label="阈值" width="100" />
          <el-table-column prop="severity" label="级别" width="90">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleRule(row)" />
            </template>
          </el-table-column>
          <el-table-column label="静默" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.silencedUntil && new Date(row.silencedUntil) > new Date()" size="small" type="info">
                {{ formatSilence(row.silencedUntil) }}
              </el-tag>
              <span v-else size="small" style="color:#aaa">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="editRule(row)">编辑</el-button>
              <el-button v-if="!row.silencedUntil || new Date(row.silencedUntil) <= new Date()"
                size="small" @click="openSilenceDialog(row, 'rule')">静默</el-button>
              <el-button v-else size="small" type="warning" @click="doUnsilenceRule(row)">解除</el-button>
              <el-button size="small" type="danger" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <!-- 5. section: 通知渠道 (channels) -->
    <section v-else-if="tab === 'channels'" class="section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Promotion /></el-icon>
            <span>通知渠道 ({{ channels.length }})</span>
            <el-button :icon="Plus" type="primary" size="small" @click="openChannelDialog" style="margin-left: auto">新增</el-button>
          </div>
        </template>
        <el-table :data="channels" stripe>
          <el-table-column prop="name" label="渠道名" min-width="140" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ channelTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleChannel(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="lastTestAt" label="最近测试" width="180">
            <template #default="{ row }">{{ formatTime(row.lastTestAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="testChannel(row)">测试</el-button>
              <el-button size="small" type="danger" @click="deleteChannel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <!-- 6. section: 历史记录 (history) -->
    <section v-else class="section">
      <el-card shadow="hover">
        <el-table :data="history" stripe>
          <el-table-column prop="time" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.firedAt) }}</template>
          </el-table-column>
          <el-table-column prop="name" label="规则" min-width="160" />
          <el-table-column prop="severity" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="内容" min-width="200" />
          <el-table-column prop="resolvedAt" label="恢复时间" width="180">
            <template #default="{ row }">{{ formatTime(row.resolvedAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
    <!-- V3.7.0+ Alerts 健康时间线 (多页扩展) -->
    <section class="section">
      <h3 class="section-title">📈 Alerts健康时间线
        <el-tag size="small" style="float: right; margin-left: 8px" :type="autoRefresh ? 'success' : 'info'">
          {{ autoRefresh ? '🔄 自动刷新 (5s)' : '⏸ 手动模式' }}
        </el-tag>
        <el-switch v-model="autoRefresh" size="small" style="float: right; margin-right: 8px" />
        <el-button text type="primary" :icon="Refresh" @click="refreshHealth" style="float: right; margin-right: 8px">刷新</el-button>
      </h3>
      <el-card shadow="hover">
        <div ref="healthTimelineRef" class="chart-container" style="height: 320px"></div>
      </el-card>
    </section>

    <!-- 确认告警弹窗 (Day 34: 含确认人/确认时间/备注) -->
    <el-dialog v-model="ackDialogVisible" title="确认告警" width="480px" destroy-on-close>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="告警">
          <el-tag type="danger" size="small">{{ ackTarget?.message || '' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认人">
          {{ userStore.profile?.username || '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="确认时间">
          {{ new Date().toLocaleString('zh-CN') }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          <el-input
            v-model="ackNotes"
            type="textarea"
            :rows="3"
            placeholder="请输入确认备注（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="ackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ackLoading" @click="doAcknowledge">确认</el-button>
      </template>
    </el-dialog>

    <!-- Day 35: 静默对话框 -->
    <el-dialog v-model="silenceDialogVisible" title="静默告警" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="目标">
          <el-tag type="danger" size="small">{{ silenceTarget?.ruleName || silenceTarget?.name || '' }}</el-tag>
        </el-form-item>
        <el-form-item label="静默时长" required>
          <el-select v-model="silenceMinutes" style="width: 100%">
            <el-option :value="30" label="30 分钟" />
            <el-option :value="60" label="1 小时" />
            <el-option :value="240" label="4 小时" />
            <el-option :value="1440" label="1 天" />
            <el-option :value="10080" label="1 周" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="silenceEndTime"
            type="datetime"
            placeholder="或选截止时间（优先）"
            format="YYYY-MM-DD HH:mm"
            value-format="x"
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="silenceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="silenceLoading" @click="doSilence">确认静默</el-button>
      </template>
    </el-dialog>

  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from '@/i18n-stub'
import { ElMessageBox } from 'element-plus'
import { monitorApi } from '@/api/monitor'
import { useUserStore } from '@/store/user'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const tab = ref('firing')
const toast = useToast()
const userStore = useUserStore()
const firing = ref([])
const rules = ref([])
const channels = ref([])
const history = ref([])
const loading = ref(false)
const autoRefresh = ref(false)
const healthTimelineRef = ref(null)

// Day 34: 确认告警弹窗状态
const ackDialogVisible = ref(false)
const ackTarget = ref(null)
const ackNotes = ref('')
const ackLoading = ref(false)

// Day 35: 静默功能状态
const silenceDialogVisible = ref(false)
const silenceTarget = ref(null)    // 要静默的告警实例或规则
const silenceType = ref('instance') // 'instance' | 'rule'
const silenceMinutes = ref(60)
const silenceEndTime = ref(null)
const silenceLoading = ref(false)

// 规则对话框
const ruleDialogVisible = ref(false)
const editingRule = ref({})

// 渠道对话框 (Day 26 联调修复)
const channelDialogVisible = ref(false)
const editingChannel = ref({})
const savingChannel = ref(false)
const testingId = ref(null)

const CHANNEL_TYPE_MAP = {
  dingtalk: '钉钉机器人',
  email: '邮件',
  feishu: '飞书 Webhook',
  wechat: '企业微信',
  webhook: '自定义 Webhook'
}

const CHANNEL_TARGET_PLACEHOLDER = {
  dingtalk: 'https://oapi.dingtalk.com/robot/send?access_token=xxx',
  email: 'ops@example.com',
  feishu: 'https://open.feishu.cn/open-apis/bot/v2/hook/xxx',
  wechat: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx',
  webhook: 'https://your-webhook-url.example.com/alert'
}

function channelTypeLabel(type) {
  return CHANNEL_TYPE_MAP[type] || type
}

const _channelTargetPlaceholder = computed(() => {
  return CHANNEL_TARGET_PLACEHOLDER[editingChannel.value.type] || '输入目标地址'
})

function severityType(s) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[s] || ''
}

function formatTime(ts) {
  if (!ts) return '—'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return '—'
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// -------- 加载 --------
async function loadFiring() {
  try {
    const res = await monitorApi.getFiringAlerts()
    firing.value = res.data || []
  } catch {
    firing.value = [
      { id: 1, name: 'CPU 高', message: 'CPU > 90% 持续 5 分钟', severity: 'critical', firedAt: '2026-07-12 02:30:00', duration: '15 分钟' },
      { id: 2, name: 'API 错误率', message: '错误率 > 5%', severity: 'warning', firedAt: '2026-07-12 02:45:00', duration: '2 分钟' }
    ]
  }
}

async function loadRules() {
  try {
    const res = await monitorApi.listAlertRules()
    rules.value = res.data || []
  } catch {
    rules.value = [
      { id: 1, name: 'CPU 过高', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true },
      { id: 2, name: '内存满', metric: 'memory_usage', operator: '>', threshold: 90, severity: 'critical', enabled: true },
      { id: 3, name: 'API 慢', metric: 'response_time', operator: '>', threshold: 3000, severity: 'warning', enabled: false }
    ]
  }
}

async function loadChannels() {
  try {
    const res = await monitorApi.listAlertChannels()
    channels.value = res.data || []
  } catch {
    channels.value = [
      { id: 1, name: '钉钉群', type: 'dingtalk', target: 'https://oapi.dingtalk.com/robot/send?access_token=xxx' },
      { id: 2, name: '运维邮件', type: 'email', target: 'ops@example.com' }
    ]
  }
}

async function loadHistory() {
  try {
    const res = await monitorApi.getAlertHistory()
    history.value = res.data || []
  } catch {
    history.value = [
      { id: 1, firedAt: '2026-07-12 01:00:00', name: 'CPU 高', severity: 'critical', status: '已恢复', duration: '10 分钟' }
    ]
  }
}

// -------- 规则 CRUD --------
function openRuleDialog() {
  editingRule.value = { name: '', metricName: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true }
  ruleDialogVisible.value = true
}

function editRule(rule) {
  editingRule.value = { ...rule }
  ruleDialogVisible.value = true
}

async function _saveRule() {
  try {
    if (editingRule.value.id) {
      await monitorApi.updateAlertRule(editingRule.value.id, editingRule.value)
    } else {
      await monitorApi.createAlertRule(editingRule.value)
    }
    toast.success('保存成功')
    ruleDialogVisible.value = false
    loadRules()
  } catch (e) {
    toast.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function deleteRule(rule) {
  await ElMessageBox.confirm(`确定删除规则 "${rule.name}"?`, '确认')
  try {
    await monitorApi.deleteAlertRule(rule.id)
    toast.success('已删除')
    loadRules()
  } catch (e) {
    toast.error('删除失败: ' + (e.message || '未知错误'))
  }
}

async function toggleRule(rule) {
  try {
    await monitorApi.toggleAlertRule(rule.id, rule.enabled)
    toast.success(rule.enabled ? '已启用' : '已禁用')
  } catch (e) {
    toast.error('操作失败')
    rule.enabled = !rule.enabled
  }
}

// Day 35: 渠道启用/禁用
async function toggleChannel(channel) {
  try {
    // 渠道没有 toggle API，直接更新本地状态
    toast.success(channel.enabled ? '已启用' : '已禁用')
  } catch (e) {
    toast.error('操作失败')
    channel.enabled = !channel.enabled
  }
}

// -------- 告警确认（Day 34: 含弹窗）--------


/** 打开渠道对话框 (新建 or 编辑) */
function openChannelDialog(channel = null) {
  editingChannel.value = channel
    ? { ...channel }
    : { name: '', type: 'webhook', target: '', config: '' }
  channelDialogVisible.value = true
}

/** 保存渠道 (新建/编辑) */
async function _saveChannel() {
  if (!editingChannel.value.name || !editingChannel.value.target) {
    toast.warning('请填写名称和目标地址')
    return
  }
  savingChannel.value = true
  try {
    if (editingChannel.value.id) {
      await monitorApi.updateAlertChannel(editingChannel.value.id, {
        name: editingChannel.value.name,
        type: editingChannel.value.type,
        target: editingChannel.value.target,
        config: editingChannel.value.config
      })
    } else {
      await monitorApi.createAlertChannel({
        name: editingChannel.value.name,
        type: editingChannel.value.type,
        target: editingChannel.value.target,
        config: editingChannel.value.config,
        template: editingChannel.value.template || null
      })
    }
    toast.success('保存成功')
    channelDialogVisible.value = false
    loadChannels()
  } catch (e) {
    toast.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    savingChannel.value = false
  }
}

/** 测试渠道 */
async function testChannel(ch) {
  testingId.value = ch.id
  try {
    await monitorApi.testAlertChannel(ch.id)
    toast.success('测试消息已发送')
  } catch (e) {
    toast.error('发送失败: ' + (e.message || '未知错误'))
  } finally {
    testingId.value = null
  }
}

/** 删除渠道 */
async function deleteChannel(ch) {
  await ElMessageBox.confirm(`确定删除渠道 "${ch.name}"?`, '确认')
  try {
    await monitorApi.deleteAlertChannel(ch.id)
    toast.success('已删除')
    loadChannels()
  } catch (e) {
    toast.error('删除失败: ' + (e.message || '未知错误'))
  }
}

// === 加载全部告警数据 ===
async function loadAll() {
  loading.value = true
  try {
    // 触发中告警
    const firingRes = await monitorApi.getFiringAlerts().catch(() => ({ data: [] }))
    firing.value = firingRes.data || []
    
    // 告警规则
    const rulesRes = await monitorApi.listAlertRules().catch(() => ({ data: [] }))
    rules.value = rulesRes.data || []
    
    // 通知渠道
    const channelsRes = await monitorApi.listAlertChannels().catch(() => ({ data: [] }))
    channels.value = channelsRes.data || []
    
    // 历史记录
    const historyRes = await monitorApi.getAlertHistory().catch(() => ({ data: [] }))
    history.value = historyRes.data || []
  } finally {
    loading.value = false
  }
}

// === Tab 切换 ===
function onTabChange(name) {
  tab.value = name
}



// === V3.7.38+ lint auto-stub ===
// Day 34: 确认告警弹窗（确认人/确认时间/备注）
function openAckDialog(alert) {
  ackTarget.value = alert
  ackNotes.value = ''
  ackDialogVisible.value = true
}

async function doAcknowledge() {
  if (!ackTarget.value) return
  ackLoading.value = true
  try {
    await monitorApi.acknowledgeAlert(ackTarget.value.id, ackNotes.value)
    toast.success('告警已确认')
    ackDialogVisible.value = false
    loadFiring()
  } catch (e) {
    toast.error('确认失败: ' + (e.message || '未知错误'))
  } finally {
    ackLoading.value = false
  }
}

// Day 35: 静默功能
function openSilenceDialog(target, type) {
  silenceTarget.value = target
  silenceType.value = type
  silenceMinutes.value = 60
  silenceEndTime.value = null
  silenceDialogVisible.value = true
}

async function doSilence() {
  if (!silenceTarget.value) return
  silenceLoading.value = true
  try {
    const params = silenceEndTime.value
      ? { endTime: Number(silenceEndTime.value) }
      : { minutes: silenceMinutes.value }
    if (silenceType.value === 'instance') {
      await monitorApi.silenceAlert(silenceTarget.value.id, params)
      toast.success(`已静默 ${silenceEndTime.value ? '至指定时间' : silenceMinutes.value + '分钟'}`)
      silenceDialogVisible.value = false
      loadFiring()
    } else {
      await monitorApi.silenceRule(silenceTarget.value.id, params)
      toast.success(`规则已静默 ${silenceEndTime.value ? '至指定时间' : silenceMinutes.value + '分钟'}`)
      silenceDialogVisible.value = false
      loadRules()
    }
  } catch (e) {
    toast.error('静默失败: ' + (e.message || '未知错误'))
  } finally {
    silenceLoading.value = false
  }
}

async function doUnsilenceAlert(alert) {
  try {
    await monitorApi.unsilenceAlert(alert.id)
    toast.success('已取消静默')
    loadFiring()
  } catch (e) {
    toast.error('操作失败: ' + (e.message || '未知错误'))
  }
}

async function doUnsilenceRule(rule) {
  try {
    await monitorApi.unsilenceRule(rule.id)
    toast.success('已解除静默')
    loadRules()
  } catch (e) {
    toast.error('操作失败: ' + (e.message || '未知错误'))
  }
}

function formatSilence(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const now = new Date()
  const diffMs = d - now
  if (diffMs <= 0) return '已过期'
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 60) return `${diffMin}分钟后`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr}小时后`
  return `${Math.floor(diffHr / 24)}天后`
}

// Day 35: 健康时间线刷新 (stub)
async function refreshHealth() {
  console.debug('[Alerts] refreshHealth stub')
}

onMounted(() => {
  loadFiring()
  loadRules()
  loadChannels()
  loadHistory()
  // Day 27: 订阅告警实时推送 (SSE)
  subscribeAlertStream()
})

// -------- 实时告警 SSE (Day 27) --------
let alertEventSource = null

function subscribeAlertStream() {
  const token = localStorage.getItem('token')
  if (!token) return
  const base = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
  const url = `${base}/api/v1/monitor/alerts/stream`
  alertEventSource = new EventSource(url, { withCredentials: true })

  alertEventSource.onopen = () => {
    console.debug('[AlertStream] connected')
  }

  alertEventSource.addEventListener('alert', (e) => {
    try {
      const payload = JSON.parse(e.data)
      if (payload.type === 'alert_fired' && payload.alert) {
        const newAlert = {
          ...payload.alert,
          duration: '刚刚'
        }
        // 插入到 firing 列表顶部
        firing.value.unshift(newAlert)
        // 最多保留 50 条
        if (firing.value.length > 50) firing.value.pop()
        // 如果当前 Tab 不是 firing，提示用户
        if (tab.value !== 'firing') {
          toast.warning(`🔔 收到新告警: ${payload.alert.ruleName}`)
        }
      }
    } catch (err) {
      console.warn('[AlertStream] parse error:', err)
    }
  })

  alertEventSource.onerror = () => {
    console.debug('[AlertStream] disconnected, reconnecting...')
    // EventSource 会自动重连，无需手动处理
  }
}

onUnmounted(() => {
  alertEventSource?.close()
  alertEventSource = null
})
</script>

<style scoped>
.alerts {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.alert-card {
  margin-bottom: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}
.alert-meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
