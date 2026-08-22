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
            <span class="alert-meta-text">
              触发时间: {{ alert.firedAt }} | 持续: {{ alert.duration }}
            </span>
            <el-button size="small" :loading="ackingId === alert.id" @click="acknowledge(alert)">确认</el-button>
          </div>
        </div>
        <div v-if="!loading.firing && firing.length === 0" class="tab-empty">
          <EmptyState
            icon="BellFilled"
            title="没有触发中的告警"
            description="系统运行良好，所有指标在阈值范围内"
            compact
          />
        </div>
        <div v-else-if="loading.firing" v-loading="true" class="firing-skeleton"></div>
      </div>

      <!-- 告警规则 -->
      <div v-else-if="tab === 'rules'">
        <el-button type="primary" :icon="Plus" @click="newRule" style="margin-bottom: 12px">+ 新建规则</el-button>
        <el-table
          :data="rules"
          v-loading="loading.rules"
          empty-text="暂无告警规则，点击右上角新建"
          stripe
        >
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
              <el-switch v-model="scope.row.enabled" :loading="togglingRuleId === scope.row.id" @change="toggleRule(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="editRule(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" :loading="deletingRuleId === scope.row.id" @click="deleteRule(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 通知渠道 -->
      <div v-else-if="tab === 'channels'">
        <el-button type="primary" :icon="Plus" @click="openChannelDialog()" style="margin-bottom: 12px">+ 新建渠道</el-button>
        <el-table
          :data="channels"
          v-loading="loading.channels"
          empty-text="暂无通知渠道，点击右上角新建"
          stripe
        >
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
              <el-button size="small" type="danger" :loading="deletingChannelId === scope.row.id" @click="deleteChannel(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 历史 -->
      <div v-else-if="tab === 'history'">
        <el-table
          :data="history"
          v-loading="loading.history"
          empty-text="暂无历史告警记录"
          stripe
        >
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
      <el-form
        ref="ruleFormRef"
        :model="editingRule"
        :rules="ruleFormRules"
        label-width="100px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="editingRule.name" placeholder="如: CPU 过高告警" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="指标" prop="metric">
          <el-select v-model="editingRule.metric" style="width: 100%">
            <el-option label="CPU 使用率" value="cpu_usage" />
            <el-option label="内存使用率" value="memory_usage" />
            <el-option label="磁盘使用率" value="disk_usage" />
            <el-option label="API 错误率" value="api_error_rate" />
            <el-option label="响应时间" value="response_time" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件" prop="operator">
          <el-select v-model="editingRule.operator" style="width: 100%">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="==" value="==" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold">
          <el-input-number v-model="editingRule.threshold" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="严重度" prop="severity">
          <el-select v-model="editingRule.severity" style="width: 100%">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRule" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑渠道对话框 (Day 26) -->
    <el-dialog v-model="channelDialogVisible" :title="editingChannel.id ? '编辑渠道' : '新建渠道'" width="500px">
      <el-form
        ref="channelFormRef"
        :model="editingChannel"
        :rules="channelFormRules"
        label-width="90px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="editingChannel.name" placeholder="如: 运维钉钉群" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="editingChannel.type" style="width: 100%">
            <el-option label="钉钉群机器人" value="dingtalk" />
            <el-option label="邮件" value="email" />
            <el-option label="飞书 Webhook" value="feishu" />
            <el-option label="企业微信" value="wechat" />
            <el-option label="自定义 Webhook" value="webhook" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址" prop="target">
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
          <div class="alert-meta-text" style="margin-top: 4px">
            变量: ${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt}
          </div>
        </el-form-item>

        <!-- 渠道测试面板 (对话框内实时测试) -->
        <el-divider content-position="left" style="margin: 16px 0 12px">
          <span style="font-size: 13px; color: var(--el-text-color-regular)">🔧 渠道连通性测试</span>
        </el-divider>
        <div style="background: var(--el-fill-color-lightest); border-radius: 6px; padding: 12px; border: 1px solid var(--el-border-color-lighter)">
          <div style="margin-bottom: 10px; font-size: 13px; color: var(--el-text-color-regular)">
            填写完配置后，点击"发送测试"验证渠道是否连通（无需保存即可测试）。
          </div>
          <el-button
            type="info"
            plain
            size="small"
            @click="testChannelInDialog"
            :loading="testingChannel"
            :disabled="!editingChannel.target"
          >
            📡 发送测试消息
          </el-button>
          <!-- 测试结果 -->
          <div v-if="testResult" style="margin-top: 12px">
            <el-tag
              :type="testResult.ok ? 'success' : 'danger'"
              size="large"
              style="margin-bottom: 8px; font-size: 13px"
            >
              {{ testResult.ok ? '✅ 发送成功' : '❌ 发送失败' }}
              <span v-if="!testResult.ok" style="margin-left: 6px">{{ testResult.error }}</span>
            </el-tag>
            <div style="margin-top: 8px">
              <div style="font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px">📨 实际发送内容：</div>
              <pre style="
                background: var(--el-fill-color);
                border: 1px solid var(--el-border-color);
                border-radius: 4px;
                padding: 8px 10px;
                font-size: 12px;
                color: var(--el-text-color-primary);
                white-space: pre-wrap;
                word-break: break-all;
                margin: 0;
                max-height: 120px;
                overflow-y: auto;
              ">{{ testResult.preview.replace('📨 发送内容预览:\n', '') }}</pre>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChannel" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * V6.8.10+ 升级: 加 v-loading / empty / 表单 :rules / 移除 console
 */
import { ref, computed, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useToast } from '@/composables/useToast'
import { monitorApi } from '@/api/monitor'
import EmptyState from '@/components/EmptyState.vue'

const toast = useToast()
const tab = ref('firing')
const firing = ref([])
const rules = ref([])
const channels = ref([])
const history = ref([])

// 集中的 loading 状态 (4 个 tab 各一份)
const loading = reactive({ firing: false, rules: false, channels: false, history: false })

// 行级 loading
const ackingId = ref(null)
const togglingRuleId = ref(null)
const deletingRuleId = ref(null)
const deletingChannelId = ref(null)
const testingId = ref(null)
const testingChannel = ref(false)
const testResult = ref(null)

// 规则对话框
const ruleDialogVisible = ref(false)
const editingRule = ref({})
const ruleFormRef = ref(null)
const savingRule = ref(false)

// 渠道对话框 (Day 26 联调修复)
const channelDialogVisible = ref(false)
const editingChannel = ref({})
const channelFormRef = ref(null)
const savingChannel = ref(false)

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

// 表单 :rules
const ruleFormRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { min: 2, max: 64, message: '名称长度 2-64 字符', trigger: 'blur' }
  ],
  metric: [{ required: true, message: '请选择指标', trigger: 'change' }],
  operator: [{ required: true, message: '请选择条件', trigger: 'change' }],
  threshold: [
    { required: true, message: '请输入阈值', trigger: 'blur' },
    { type: 'number', message: '阈值必须为数字', trigger: 'blur' }
  ],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }]
}

const channelFormRules = {
  name: [
    { required: true, message: '请输入渠道名称', trigger: 'blur' },
    { min: 2, max: 64, message: '名称长度 2-64 字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  target: [
    { required: true, message: '请输入目标地址', trigger: 'blur' },
    { min: 4, max: 500, message: '目标地址长度 4-500 字符', trigger: 'blur' }
  ]
}

// -------- 加载 --------
async function loadFiring() {
  loading.firing = true
  try {
    const res = await monitorApi.getFiringAlerts()
    firing.value = res.data || []
  } catch (e) {
    firing.value = [
      { id: 1, name: 'CPU 高', message: 'CPU > 90% 持续 5 分钟', severity: 'critical', firedAt: '2026-07-12 02:30:00', duration: '15 分钟' },
      { id: 2, name: 'API 错误率', message: '错误率 > 5%', severity: 'warning', firedAt: '2026-07-12 02:45:00', duration: '2 分钟' }
    ]
    toast.warning('无法连接告警服务，已加载演示数据')
  } finally {
    loading.firing = false
  }
}

async function loadRules() {
  loading.rules = true
  try {
    const res = await monitorApi.listAlertRules()
    rules.value = res.data || []
  } catch (e) {
    rules.value = [
      { id: 1, name: 'CPU 过高', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true },
      { id: 2, name: '内存满', metric: 'memory_usage', operator: '>', threshold: 90, severity: 'critical', enabled: true },
      { id: 3, name: 'API 慢', metric: 'response_time', operator: '>', threshold: 3000, severity: 'warning', enabled: false }
    ]
    toast.warning('无法加载告警规则，已加载演示数据')
  } finally {
    loading.rules = false
  }
}

async function loadChannels() {
  loading.channels = true
  try {
    const res = await monitorApi.listAlertChannels()
    channels.value = res.data || []
  } catch (e) {
    channels.value = [
      { id: 1, name: '钉钉群', type: 'dingtalk', target: 'https://oapi.dingtalk.com/robot/send?access_token=xxx' },
      { id: 2, name: '运维邮件', type: 'email', target: 'ops@example.com' }
    ]
    toast.warning('无法加载通知渠道，已加载演示数据')
  } finally {
    loading.channels = false
  }
}

async function loadHistory() {
  loading.history = true
  try {
    const res = await monitorApi.getAlertHistory()
    history.value = res.data || []
  } catch (e) {
    history.value = [
      { id: 1, firedAt: '2026-07-12 01:00:00', name: 'CPU 高', severity: 'critical', status: '已恢复', duration: '10 分钟' }
    ]
  } finally {
    loading.history = false
  }
}

// -------- 规则 CRUD --------
function newRule() {
  editingRule.value = { name: '', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true }
  ruleDialogVisible.value = true
  nextTick(() => ruleFormRef.value?.clearValidate())
}

function editRule(rule) {
  editingRule.value = { ...rule }
  ruleDialogVisible.value = true
  nextTick(() => ruleFormRef.value?.clearValidate())
}

async function saveRule() {
  if (!ruleFormRef.value) return
  try {
    await ruleFormRef.value.validate()
  } catch {
    toast.warning('请检查表单填写是否正确')
    return
  }
  savingRule.value = true
  try {
    if (editingRule.value.id) {
      await monitorApi.updateAlertRule(editingRule.value.id, editingRule.value)
    } else {
      await monitorApi.createAlertRule(editingRule.value)
    }
    toast.success('保存成功')
    ruleDialogVisible.value = false
    await loadRules()
  } catch (e) {
    toast.error('保存失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    savingRule.value = false
  }
}

async function deleteRule(rule) {
  try {
    await ElMessageBox.confirm(`确定删除规则 "${rule.name}"?`, '确认删除', { type: 'warning' })
  } catch { return }
  deletingRuleId.value = rule.id
  try {
    await monitorApi.deleteAlertRule(rule.id)
    toast.success('已删除')
    await loadRules()
  } catch (e) {
    toast.error('删除失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    deletingRuleId.value = null
  }
}

async function toggleRule(rule) {
  togglingRuleId.value = rule.id
  const target = rule.enabled
  try {
    await monitorApi.toggleAlertRule(rule.id, target)
    toast.success(target ? '已启用' : '已禁用')
  } catch (e) {
    rule.enabled = !target
    toast.error('操作失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    togglingRuleId.value = null
  }
}

// -------- 告警确认 --------
async function acknowledge(alert) {
  ackingId.value = alert.id
  try {
    await monitorApi.acknowledgeAlert(alert.id)
    toast.success('已确认')
    await loadFiring()
  } catch (e) {
    toast.error('确认失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    ackingId.value = null
  }
}

// -------- 渠道 CRUD (Day 26 — 端到端联调修复) --------

/** 打开渠道对话框 (新建 or 编辑) */
function openChannelDialog(channel = null) {
  editingChannel.value = channel
    ? { ...channel }
    : { name: '', type: 'webhook', target: '', config: '' }
  channelDialogVisible.value = true
  testResult.value = null
  nextTick(() => channelFormRef.value?.clearValidate())
}

/** 保存渠道 (新建/编辑) */
async function saveChannel() {
  if (!channelFormRef.value) return
  try {
    await channelFormRef.value.validate()
  } catch {
    toast.warning('请检查表单填写是否正确')
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
    await loadChannels()
  } catch (e) {
    toast.error('保存失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
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
    toast.error('发送失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    testingId.value = null
  }
}

/** 在对话框内测试当前编辑中的渠道配置 (无需保存即可测) */
async function testChannelInDialog() {
  if (!editingChannel.value.target) {
    toast.warning('请先填写目标地址')
    return
  }
  testingChannel.value = true
  testResult.value = null
  try {
    let channelId = editingChannel.value.id
    if (!channelId) {
      const created = await monitorApi.createAlertChannel({
        name: editingChannel.value.name || '临时测试渠道',
        type: editingChannel.value.type,
        target: editingChannel.value.target,
        config: editingChannel.value.config,
        template: editingChannel.value.template || null,
        enabled: 0
      })
      channelId = created.data?.id
    }
    const res = await monitorApi.testAlertChannel(channelId)
    if (res.code === 0 || res.code === 200) {
      testResult.value = {
        ok: true,
        message: '✅ 发送成功！请检查对方是否收到测试消息',
        preview: buildTestPreview()
      }
    } else {
      testResult.value = {
        ok: false,
        error: res.message || '返回结果异常',
        preview: buildTestPreview()
      }
    }
  } catch (e) {
    const errMsg = e?.response?.data?.message
      || e?.response?.data?.msg
      || e?.response?.data
      || e?.message
      || '网络错误，请检查网络或后端是否运行'
    testResult.value = {
      ok: false,
      error: errMsg,
      preview: buildTestPreview()
    }
  } finally {
    testingChannel.value = false
  }
}

/** 构造测试消息预览 (当前配置的模板解析结果) */
function buildTestPreview() {
  const tpl = editingChannel.value.template || ''
  const now = new Date().toLocaleString('zh-CN')
  const vars = {
    ruleName: '[测试] 告警渠道连通性检测',
    severity: 'info',
    metricName: 'test_metric',
    metricValue: '0',
    threshold: '0',
    message: '[测试消息] 您好，这是来自 Liugl-AI 平台的告警渠道测试消息。如果收到此消息，说明渠道配置正确。',
    firedAt: now
  }
  let preview = tpl || '【info】告警: [测试] 告警渠道连通性检测 当前值 0 超过阈值 0'
  Object.entries(vars).forEach(([k, v]) => {
    preview = preview.replace(new RegExp('\\$\\{' + k + '\\}', 'g'), v)
  })
  return '📨 发送内容预览:\n' + preview
}

/** 删除渠道 */
async function deleteChannel(ch) {
  try {
    await ElMessageBox.confirm(`确定删除渠道 "${ch.name}"?`, '确认删除', { type: 'warning' })
  } catch { return }
  deletingChannelId.value = ch.id
  try {
    await monitorApi.deleteAlertChannel(ch.id)
    toast.success('已删除')
    await loadChannels()
  } catch (e) {
    toast.error('删除失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    deletingChannelId.value = null
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
  try {
    alertEventSource = new EventSource(url, { withCredentials: true })

    alertEventSource.onopen = () => {
      // SSE 连接成功，无需日志
    }

    alertEventSource.addEventListener('alert', (e) => {
      try {
        const payload = JSON.parse(e.data)
        if (payload.type === 'alert_fired' && payload.alert) {
          const newAlert = {
            ...payload.alert,
            duration: '刚刚'
          }
          firing.value.unshift(newAlert)
          if (firing.value.length > 50) firing.value.pop()
          if (tab.value !== 'firing') {
            toast.warning(`🔔 收到新告警: ${payload.alert.ruleName}`)
          }
        }
      } catch (err) {
        // SSE 数据解析失败，静默忽略
      }
    })

    alertEventSource.onerror = () => {
      // EventSource 会自动重连
    }
  } catch (err) {
    // SSE 不可用（如浏览器不支持），静默忽略
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
  background: var(--el-fill-color-lightest);
  border-radius: 4px;
}
.alert-meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.alert-meta-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.tab-empty {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}
.firing-skeleton {
  min-height: 120px;
  width: 100%;
}
</style>
