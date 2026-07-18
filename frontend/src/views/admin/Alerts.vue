<!--
  @file views/admin/Alerts.vue (告警列表)
  @version V3.5.12+ (前端注释补全)
  @description 告警列表
-->
<template>
  <div class="alerts">
    <el-card>
      <template #header>
        <div class="header">
          <span>🔔 告警中心</span>
          <el-button-group>
            <el-button :type="tab === 'firing' ? 'primary' : ''" @click="tab = 'firing'">触发中</el-button>
            <el-button :type="tab === 'rules' ? 'primary' : ''" @click="tab = 'rules'">告警规则</el-button>
            <el-button :type="tab === 'channels' ? 'primary' : ''" @click="tab = 'channels'">通知渠道</el-button>
            <el-button :type="tab === 'history' ? 'primary' : ''" @click="tab = 'history'">历史记录</el-button>
          </el-button-group>
        </div>
      </template>

      <!-- 触发中告警 -->
      <div v-if="tab === 'firing'">
        <div v-for="alert in firing" :key="alert.id" class="alert-card">
          <el-alert
            :type="severityType(alert.severity)"
            :title="alert.name + ' - ' + alert.message"
            show-icon
            :closable="false"
          />
          <div class="alert-meta">
            <span style="color: #999; font-size: 12px">
              触发时间: {{ alert.firedAt }} | 持续: {{ alert.duration }}
            </span>
            <el-button size="small" @click="acknowledge(alert)">确认</el-button>
          </div>
        </div>
        <el-empty v-if="!firing.length" description="没有触发中的告警" />
      </div>

      <!-- 告警规则 -->
      <div v-else-if="tab === 'rules'">
        <el-button type="primary" @click="newRule" style="margin-bottom: 12px">+ 新建规则</el-button>
        <el-table :data="rules" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="metric" label="指标" width="160" />
          <el-table-column prop="operator" label="条件" width="100" />
          <el-table-column prop="threshold" label="阈值" width="100" />
          <el-table-column prop="severity" label="严重度" width="100">
            <template #default="scope">
              <el-tag :type="severityType(scope.row.severity)">{{ scope.row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="scope">
              <el-switch v-model="scope.row.enabled" @change="toggleRule(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="editRule(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteRule(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 通知渠道 -->
      <div v-else-if="tab === 'channels'">
        <el-button type="primary" @click="openChannelDialog()" style="margin-bottom: 12px">+ 新建渠道</el-button>
        <el-table :data="channels" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              <el-tag>{{ channelTypeLabel(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="target" label="目标" show-overflow-tooltip />
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button size="small" @click="testChannel(scope.row)" :loading="testingId === scope.row.id">测试</el-button>
              <el-button size="small" @click="openChannelDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteChannel(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 历史 -->
      <div v-else-if="tab === 'history'">
        <el-table :data="history" stripe>
          <el-table-column prop="firedAt" label="时间" width="180" />
          <el-table-column prop="name" label="告警" />
          <el-table-column prop="severity" label="严重度" width="100">
            <template #default="scope">
              <el-tag :type="severityType(scope.row.severity)">{{ scope.row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="duration" label="持续" width="100" />
        </el-table>
      </div>
    </el-card>

    <!-- 新建/编辑规则对话框 -->
    <el-dialog v-model="ruleDialogVisible" :title="editingRule.id ? '编辑规则' : '新建规则'" width="600px">
      <el-form :model="editingRule" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="editingRule.name" />
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="editingRule.metric">
            <el-option label="CPU 使用率" value="cpu_usage" />
            <el-option label="内存使用率" value="memory_usage" />
            <el-option label="磁盘使用率" value="disk_usage" />
            <el-option label="API 错误率" value="api_error_rate" />
            <el-option label="响应时间" value="response_time" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-select v-model="editingRule.operator">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="==" value="==" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="editingRule.threshold" />
        </el-form-item>
        <el-form-item label="严重度">
          <el-select v-model="editingRule.severity">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑渠道对话框 (Day 26) -->
    <el-dialog v-model="channelDialogVisible" :title="editingChannel.id ? '编辑渠道' : '新建渠道'" width="500px">
      <el-form :model="editingChannel" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="editingChannel.name" placeholder="如: 运维钉钉群" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="editingChannel.type" style="width: 100%">
            <el-option label="钉钉群机器人" value="dingtalk" />
            <el-option label="邮件" value="email" />
            <el-option label="飞书 Webhook" value="feishu" />
            <el-option label="企业微信" value="wechat" />
            <el-option label="自定义 Webhook" value="webhook" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址" required>
          <el-input
            v-model="editingChannel.target"
            :placeholder="channelTargetPlaceholder"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item v-if="editingChannel.type === 'email'" label="收件人">
          <el-input v-model="editingChannel.config" placeholder='{"email":"ops@example.com"}' />
        </el-form-item>
        <el-form-item label="通知模板">
          <el-input
            v-model="editingChannel.template"
            type="textarea"
            :rows="3"
            placeholder="支持变量替换，不填则用默认模板。&#10;可用变量: ${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt}&#10;示例: 【${severity}】告警: ${ruleName} 当前值 ${metricValue} 超过阈值 ${threshold}"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            变量: ${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChannel" :loading="savingChannel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { monitorApi } from '@/api/monitor'

const tab = ref('firing')
const firing = ref([])
const rules = ref([])
const channels = ref([])
const history = ref([])

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

const channelTargetPlaceholder = computed(() => {
  return CHANNEL_TARGET_PLACEHOLDER[editingChannel.value.type] || '输入目标地址'
})

function severityType(s) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[s] || ''
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
function newRule() {
  editingRule.value = { name: '', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true }
  ruleDialogVisible.value = true
}

function editRule(rule) {
  editingRule.value = { ...rule }
  ruleDialogVisible.value = true
}

async function saveRule() {
  try {
    if (editingRule.value.id) {
      await monitorApi.updateAlertRule(editingRule.value.id, editingRule.value)
    } else {
      await monitorApi.createAlertRule(editingRule.value)
    }
    ElMessage.success('保存成功')
    ruleDialogVisible.value = false
    loadRules()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function deleteRule(rule) {
  await ElMessageBox.confirm(`确定删除规则 "${rule.name}"?`, '确认')
  try {
    await monitorApi.deleteAlertRule(rule.id)
    ElMessage.success('已删除')
    loadRules()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.message || '未知错误'))
  }
}

async function toggleRule(rule) {
  try {
    await monitorApi.toggleAlertRule(rule.id, rule.enabled)
    ElMessage.success(rule.enabled ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error('操作失败')
    rule.enabled = !rule.enabled
  }
}

// -------- 告警确认 --------
async function acknowledge(alert) {
  try {
    await monitorApi.acknowledgeAlert(alert.id)
    ElMessage.success('已确认')
    loadFiring()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

// -------- 渠道 CRUD (Day 26 — 端到端联调修复) --------

/** 打开渠道对话框 (新建 or 编辑) */
function openChannelDialog(channel = null) {
  editingChannel.value = channel
    ? { ...channel }
    : { name: '', type: 'webhook', target: '', config: '' }
  channelDialogVisible.value = true
}

/** 保存渠道 (新建/编辑) */
async function saveChannel() {
  if (!editingChannel.value.name || !editingChannel.value.target) {
    ElMessage.warning('请填写名称和目标地址')
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
    ElMessage.success('保存成功')
    channelDialogVisible.value = false
    loadChannels()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    savingChannel.value = false
  }
}

/** 测试渠道 */
async function testChannel(ch) {
  testingId.value = ch.id
  try {
    await monitorApi.testAlertChannel(ch.id)
    ElMessage.success('测试消息已发送')
  } catch (e) {
    ElMessage.error('发送失败: ' + (e.message || '未知错误'))
  } finally {
    testingId.value = null
  }
}

/** 删除渠道 */
async function deleteChannel(ch) {
  await ElMessageBox.confirm(`确定删除渠道 "${ch.name}"?`, '确认')
  try {
    await monitorApi.deleteAlertChannel(ch.id)
    ElMessage.success('已删除')
    loadChannels()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.message || '未知错误'))
  }
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
          ElMessage.warning(`🔔 收到新告警: ${payload.alert.ruleName}`)
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
