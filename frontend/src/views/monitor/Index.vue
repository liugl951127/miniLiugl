<!--
  @file views/monitor/Index.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  系统监控 V5.6
  特性:
    - 5 个服务健康卡片 (UP/DOWN + 详情)
    - 实时业务指标 (chat/tool/rag/tokens/http 4xx/5xx)
    - JVM 内存 + CPU + 线程
    - 数据库连接池 (HikariCP)
    - 磁盘使用率
    - 告警 firing 列表
    - 自动刷新 (10s)
-->
<!--
  @file views/monitor/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="page-monitor">
    <!-- 1. page-header: 标题 + 副标题 + 操作 -->
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
        <h2 class="page-title">📊 系统监控</h2>
        <p class="page-subtitle">实时指标 + JVM + DB + 磁盘 + 告警 · 自动刷新 {{ refreshSec }}s</p>
      </div>
      <el-button-group>
        <el-switch v-model="autoRefresh" active-text="自动" inactive-text="手动" @change="toggleAuto" />
        <el-button type="primary" :icon="Refresh" @click="loadAll">{{ t('monitor.refresh') }}</el-button>
      </el-button-group>
    </header>

    <!-- 2. section: 4 健康卡片 -->
    <section class="section">
      <h3 class="section-title">{{ t('monitor.health') }}</h3>
      <el-row :gutter="16">
        <el-col v-for="(h, key) in healths" :key="key" :xs="12" :sm="6">
          <el-card :class="['health-card', h.status === 'UP' ? 'up' : 'down']" shadow="hover">
            <div class="hc-top">
              <el-icon :size="20" :color="h.status === 'UP' ? 'var(--liugl-success)' : 'var(--liugl-danger)'">
                <component :is="h.status === 'UP' ? CircleCheck : CircleClose" />
              </el-icon>
              <strong>{{ key }}</strong>
            </div>
            <div class="hc-status">{{ h.status || '...' }}</div>
            <div class="hc-detail" v-if="h.detail">
              <div v-for="(v, k) in flatten(h.detail).slice(0, 4)" :key="k" class="detail-row">
                <span class="k">{{ k }}:</span>
                <span class="v">{{ v }}</span>
              </div>
            </div>
            <div class="hc-detail muted" v-else>点击刷新加载</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 3. section: 实时业务指标 (KPI 网格) -->
    <section class="section">
      <h3 class="section-title">{{ t('monitor.metrics') }}</h3>
      <el-card shadow="hover">
        <div class="metric-grid">
          <div class="metric-cell" v-for="(v, k) in metrics" :key="k">
            <div class="metric-label">{{ metricLabel(k) }}</div>
            <div class="metric-value" :class="metricClass(k)">{{ formatNum(v) }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <!-- 4. section: JVM / 磁盘 详情 -->
    <section class="section">
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Cpu /></el-icon>
                <span>☕ JVM 内存</span>
              </div>
            </template>
            <div v-if="jvmInfo">
              <el-progress :percentage="jvmInfo.usedPercent || 0" :stroke-width="14" :color="jvmBarColor" />
              <div class="jvm-stats">
                <div><span class="k">Heap Used</span><span class="v">{{ jvmInfo.heapUsed || '-' }}</span></div>
                <div><span class="k">Heap Max</span><span class="v">{{ jvmInfo.heapMax || '-' }}</span></div>
                <div><span class="k">Threads</span><span class="v">{{ jvmInfo.threads || '-' }}</span></div>
                <div><span class="k">GC</span><span class="v">{{ jvmInfo.gc || '-' }}</span></div>
              </div>
            </div>
            <el-skeleton v-else :rows="4" animated />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Coin /></el-icon>
                <span>💾 磁盘 + DB</span>
              </div>
            </template>
            <div v-if="diskInfo">
              <el-progress :percentage="diskInfo.usedPercent || 0" :stroke-width="14" :color="diskBarColor" />
              <div class="jvm-stats">
                <div><span class="k">Used</span><span class="v">{{ diskInfo.used || '-' }}</span></div>
                <div><span class="k">Total</span><span class="v">{{ diskInfo.total || '-' }}</span></div>
                <div v-if="dbInfo"><span class="k">DB Pool</span><span class="v">{{ dbInfo.active || 0 }}/{{ dbInfo.max || 0 }}</span></div>
              </div>
            </div>
            <el-skeleton v-else :rows="4" animated />
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 5. section: 告警 firing 列表 -->
    <section v-if="alerts.length" class="section">
      <h3 class="section-title">🚨 当前告警</h3>
      <el-card shadow="hover">
        <el-table :data="alerts" stripe>
          <el-table-column prop="time" :label="t('monitor.alert.time')" width="180">
            <template #default="{ row }">{{ formatTime(row.firedAt || row.time) }}</template>
          </el-table-column>
          <el-table-column prop="name" :label="t('monitor.alert.name')" />
          <el-table-column prop="severity" :label="t('monitor.alert.severity')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.severity === 'critical' ? 'danger' : 'warning'" size="small">{{ row.severity || 'warning' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" :label="t('monitor.alert.content')" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="ackAlert(row)">确认</el-button>
              <el-button size="small" type="warning" @click="openRcaDialog(row)">RCA</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
    <!-- V3.7.0+ Monitor 健康时间线 (多页扩展) -->
    <section class="section">
      <h3 class="section-title">📈 Monitor健康时间线
        <el-tag size="small" style="float: right; margin-left: 8px" :type="autoRefresh ? 'success' : 'info'">
          { autoRefresh ? '🔄 自动刷新 (5s)' : '⏸ 手动模式' }
        </el-tag>
        <el-switch v-model="autoRefresh" size="small" style="float: right; margin-right: 8px" />
        <el-button text type="primary" :icon="Refresh" @click="refreshHealth" style="float: right; margin-right: 8px">刷新</el-button>
      </h3>
      <el-card shadow="hover">
        <div ref="healthTimelineRef" class="chart-container" style="height: 320px"></div>
      </el-card>
    </section>

    <!-- Day 32: 异常检测趋势图 -->
    <section class="section">
      <h3 class="section-title">📉 异常检测
        <el-select v-model="anomalySelectedMetric" placeholder="选择指标" size="small" style="float:right;width:200px;margin-right:8px" @change="loadAnomalyTrend">
          <el-option v-for="m in anomalyMetrics" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button text type="primary" :icon="Refresh" @click="loadAnomalyMetrics" size="small" style="float:right;margin-right:8px">刷新</el-button>
      </h3>
      <el-card shadow="hover">
        <div v-if="anomalyMetrics.length === 0" style="text-align:center;padding:40px;color:#999">
          暂无活跃检测指标，请先配置告警规则
        </div>
        <div v-else ref="anomalyChartRef" class="chart-container" style="height: 320px"></div>
      </el-card>
    </section>

    <!-- Day 32: RCA 分析结果弹窗 -->
    <el-dialog v-model="rcaDialog" title="🔍 告警 RCA 根因分析" width="680px" :close-on-click-modal="false">
      <el-skeleton v-if="rcaLoading" :rows="5" animated />
      <div v-else-if="rcaResult">
        <!-- 告警基本信息 -->
        <el-descriptions title="告警信息" :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="规则">{{ rcaResult.alert?.ruleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag size="small" :type="rcaResult.alert?.severity === 'critical' ? 'danger' : 'warning'">
              {{ rcaResult.alert?.severity || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指标">{{ rcaResult.alert?.metricName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="指标值">{{ rcaResult.alert?.metricValue || '-' }}</el-descriptions-item>
          <el-descriptions-item label="消息" :span="2">{{ rcaResult.alert?.message || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- RCA 结果 -->
        <el-divider content-position="left">根因分析结果</el-divider>
        <div v-if="rcaResult.error" class="rca-error">
          <el-alert type="danger" :title="rcaResult.error" :closable="false" show-icon />
        </div>
        <div v-else>
          <el-row :gutter="12" style="margin-bottom:12px">
            <el-col :span="8">
              <el-statistic title="根因类别" :value="rcaResult.rca?.category || '-'">
                <template #prefix><el-tag size="small" :type="rcaCategoryColor(rcaResult.rca?.category)">{{ rcaResult.rca?.category || 'N/A' }}</el-tag></template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="分析方法" :value="rcaResult.rca?.method || '-'">
                <template #suffix><span style="font-size:12px;color:#999">ms</span></template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="置信度" :value="((rcaResult.rca?.confidence || 0) * 100).toFixed(0) + '%'" />
            </el-col>
          </el-row>

          <el-card shadow="never" style="margin-bottom:12px">
            <template #header><strong>🔎 根因描述</strong></template>
            <div style="font-size:14px;line-height:1.8">{{ rcaResult.rca?.cause || '未检测到根因' }}</div>
          </el-card>

          <el-card shadow="never" v-if="rcaResult.rca?.suggestedActions?.length">
            <template #header><strong>🛠 建议操作</strong></template>
            <el-timeline>
              <el-timeline-item v-for="(action, idx) in rcaResult.rca.suggestedActions" :key="idx" :icon="idx === 0 ? 'Operation' : undefined" :color="idx === 0 ? '#409EFF' : undefined">
                {{ action }}
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>
      </div>
    </el-dialog>

  </div>
</template>
<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, reactive, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import { ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'
import { getMonitorAlertRules, createMonitorAlertRule, updateMonitorAlertRule, deleteMonitorAlertRule, getAlertChannels, createAlertChannel, updateAlertChannel, deleteAlertChannel, rcaAnalysis, anomalySummary, activeAnomalyMetrics } from '@/api/monitor'

const userStore = useUserStore()
const toast = useToast()
const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const token = userStore.accessToken || ''
function auth() { return { headers: { Authorization: `Bearer ${token}` } } }

const autoRefresh = ref(true)
const refreshSec = 10
let timer: number | null = null

const healths = reactive({
  '总健康': { status: '...', detail: null as any },
  '数据库': { status: '...', detail: null as any },
  'JVM': { status: '...', detail: null as any },
  '磁盘': { status: '...', detail: null as any },
})
const metrics = ref<Record<string, number>>({})
const jvmInfo = ref<any>(null)
const dbInfo = ref<any>(null)
const diskInfo = ref<any>(null)
const alerts = ref<any[]>([])

// V5.9: 告警规则管理
const rules = ref<any[]>([])
const canEditRules = ref(true)  // 是否能编辑 (根据角色控制)
const ruleDialog = ref(false)
const ruleSaving = ref(false)
const ruleForm = reactive<any>({
  id: null, name: '', service: '', metricName: '',
  operator: '>', threshold: 0, severity: 'warning',
  cooldownMinutes: 15, notifyChannel: 'websocket', enabled: 1,
})
const serviceOptions = [
  'minimax-gateway', 'minimax-auth', 'minimax-chat', 'minimax-model',
  'minimax-memory', 'minimax-rag', 'minimax-function', 'minimax-agent',
  'minimax-admin', 'minimax-prompt', 'minimax-multimodal', 'minimax-monitor', 'minimax-ws',
]

function metricLabel(k: string) {
  return ({
    chat_messages_total: '聊天消息',
    tool_calls_total: '工具调用',
    rag_queries_total: 'RAG 查询',
    llm_tokens_total: 'LLM Tokens',
    http_5xx_total: 'HTTP 5xx',
    http_4xx_total: 'HTTP 4xx',
    online_users: '在线用户',
    sessions_active: '活跃会话',
  } as any)[k] || k
}
function metricClass(k: string) {
  if (k.includes('5xx')) return 'metric-bad'
  if (k.includes('4xx')) return 'metric-warn'
  return 'metric-good'
}
function formatNum(n: number) {
  if (typeof n !== 'number') return '-'
  if (n > 1e6) return (n / 1e6).toFixed(2) + 'M'
  if (n > 1e3) return (n / 1e3).toFixed(2) + 'k'
  return n.toString()
}
function flatten(o: any, prefix = ''): Record<string, any> {
  if (!o || typeof o !== 'object') return {}
  const r: any = {}
  for (const k in o) {
    const v = o[k]
    if (v && typeof v === 'object' && !Array.isArray(v)) {
      Object.assign(r, flatten(v, prefix + k + '.'))
    } else {
      r[prefix + k] = Array.isArray(v) ? `[${v.length}]` : v
    }
  }
  return r
}
function formatTime(t: any) {
  return t ? dayjs(t).format('MM-DD HH:mm:ss') : '-'
}

async function loadHealth() {
  for (const key of Object.keys(healths)) {
    const urlMap: Record<string, string> = {
      '总健康': '/monitor/health',
      '数据库': '/monitor/health/database',
      'JVM': '/monitor/health/jvm',
      '磁盘': '/monitor/health/disk',
    }
    try {
      const { data } = await axios.get(`${API}/api/v1${urlMap[key]}`, auth())
      const d = data.data || {}
      healths[key as keyof typeof healths].detail = d
      healths[key as keyof typeof healths].status = d.status || (d.healthy === false ? 'DOWN' : 'UP')
    } catch (e: any) {
      healths[key as keyof typeof healths].status = 'DOWN'
    }
  }
}

async function loadMetrics() {
  try {
    const { data } = await axios.get(`${API}/api/v1/monitor/metrics`, auth())
    metrics.value = data.data || {}
  } catch (_) { metrics.value = {} }
}

async function loadJvm() {
  try {
    const { data } = await axios.get(`${API}/api/v1/monitor/health/jvm`, auth())
    jvmInfo.value = data.data || null
  } catch (_) { jvmInfo.value = null }
}

async function loadDb() {
  try {
    const { data } = await axios.get(`${API}/api/v1/monitor/health/database`, auth())
    dbInfo.value = data.data || null
  } catch (_) { dbInfo.value = null }
}

async function loadDisk() {
  try {
    const { data } = await axios.get(`${API}/api/v1/monitor/health/disk`, auth())
    diskInfo.value = data.data || null
  } catch (_) { diskInfo.value = null }
}

async function loadAlerts() {
  try {
    const { data } = await axios.get(`${API}/api/v1/monitor/alerts/firing`, auth())
    alerts.value = data.data || []
  } catch (_) { alerts.value = [] }
}

// Day 32: RCA 分析状态
const rcaDialog = ref(false)
const rcaLoading = ref(false)
const rcaResult = ref(null)
const rcaAlertId = ref(null)

// Day 32: 异常检测状态
const anomalyChartRef = ref(null)
let anomalyChart = null
const anomalyMetrics = ref([])
const anomalySelectedMetric = ref('')
const anomalyTrendData = ref([])

async function loadAnomalyMetrics() {
  try {
    const { data } = await activeAnomalyMetrics()
    anomalyMetrics.value = data.data || []
    if (anomalyMetrics.value.length > 0 && !anomalySelectedMetric.value) {
      anomalySelectedMetric.value = anomalyMetrics.value[0]
    }
  } catch (_) { anomalyMetrics.value = [] }
}

async function loadAnomalyTrend(metric) {
  if (!metric) return
  anomalySelectedMetric.value = metric
  try {
    const { data } = await anomalySummary({ metric })
    anomalyTrendData.value = data.data || {}
    await nextTick()
    renderAnomalyChart()
  } catch (_) { anomalyTrendData.value = {} }
}

function renderAnomalyChart() {
  if (!anomalyChartRef.value) return
  if (!anomalyChart) {
    anomalyChart = echarts.init(anomalyChartRef.value)
  }
  const summary = anomalyTrendData.value
  const zScore = summary.currentZScore || 0
  const isAnomaly = summary.currentlyAnomalous || false

  // Build simple indicator chart
  const option = {
    title: {
      text: `📈 ${anomalySelectedMetric.value} 异常检测`,
      subtext: `Z-Score: ${zScore.toFixed(3)} | ${isAnomaly ? '🚨 异常' : '✅ 正常'} | 样本: ${summary.sampleCount || 0}`,
      left: 'center'
    },
    tooltip: { trigger: 'axis' },
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: -5,
      max: 5,
      splitNumber: 10,
      axisLine: {
        lineStyle: {
          width: 6,
          color: [
            [0.2, '#67c23a'], [0.4, '#95d475'],
            [0.6, '#e6a23c'], [0.8, '#f56c6c'],
            [1, '#c0392b']
          ]
        }
      },
      pointer: { icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z', length: '12%', width: 20, offsetCenter: [0, '-60%'], itemStyle: { color: '#409EFF' } },
      axisTick: { length: 12, lineStyle: { color: 'auto', width: 2 } },
      splitLine: { length: 20, lineStyle: { color: 'auto', width: 5 } },
      axisLabel: { distance: -60, color: '#999', fontSize: 12, formatter: function(v) { return v.toFixed(1) } },
      title: { offsetCenter: [0, '-10%'], fontSize: 14 },
      detail: {
        fontSize: 28, offsetCenter: [0, '0%'],
        valueAnimation: true,
        formatter: function(v) { return v.toFixed(3) },
        color: isAnomaly ? '#f56c6c' : '#67c23a'
      },
      data: [{ value: zScore, name: 'Z-Score' }]
    }]
  }
  anomalyChart.setOption(option)
}

async function openRcaDialog(alertRow) {
  rcaAlertId.value = alertRow.id
  rcaResult.value = null
  rcaDialog.value = true
  rcaLoading.value = true
  try {
    const { data } = await rcaAnalysis(alertRow.id, {})
    rcaResult.value = data.data || data
  } catch (e) {
    rcaResult.value = { error: 'RCA 分析失败: ' + (e?.message || '未知') }
  } finally {
    rcaLoading.value = false
  }
}

function rcaCategoryColor(cat) {
  const map = { RESOURCE: 'warning', CONFIG: 'info', EXTERNAL: 'primary', CODE: 'danger', TRAFFIC: 'success', NETWORK: 'warning', UNKNOWN: 'info' }
  return map[cat] || 'info'
}

async function loadAll() {
  await Promise.all([loadHealth(), loadMetrics(), loadJvm(), loadDb(), loadDisk(), loadAlerts(), loadRules(), loadChannels()])
}

// V5.9: 加载告警规则
async function loadRules() {
  try {
    const { data } = await getMonitorAlertRules()
    rules.value = data.data || []
  } catch (_) { rules.value = [] }
}

// V5.9: 严重程度 → el-tag 类型
function severityType(s: string) {
  if (s === 'critical') return 'danger'
  if (s === 'warning') return 'warning'
  return 'info'
}

// V5.9: 打开编辑弹窗 (row 可为 null → 新增)
function openRuleDialog(row?: any) {
  if (row) {
    Object.assign(ruleForm, row)
  } else {
    Object.assign(ruleForm, {
      id: null, name: '', service: 'minimax-gateway', metricName: '',
      operator: '>', threshold: 0, severity: 'warning',
      cooldownMinutes: 15, notifyChannel: 'websocket', enabled: 1,
    })
  }
  ruleDialog.value = true
}

// V5.9: 保存规则
async function saveRule() {
  if (!ruleForm.name?.trim()) return toast.warning('请输入名称')
  if (!ruleForm.metricName?.trim()) return toast.warning('请输入指标名')
  ruleSaving.value = true
  try {
    if (ruleForm.id) {
      await updateMonitorAlertRule(ruleForm.id, ruleForm)
      toast.success('规则已更新')
    } else {
      await createMonitorAlertRule(ruleForm)
      toast.success('规则已创建')
    }
    ruleDialog.value = false
    await loadRules()
  } catch (e: any) {
    toast.error('保存失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    ruleSaving.value = false
  }
}

// V5.9: 删除规则 (二次确认)
async function removeRule(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除规则 [${row.name}]?`, '提示', { type: 'warning' })
    await deleteMonitorAlertRule(row.id)
    toast.success('已删除')
    await loadRules()
  } catch (_) { /* cancel */ }
}

// V5.33 Day 24: 告警渠道管理
const channels = ref<any[]>([])
const channelDialog = ref(false)
const channelSaving = ref(false)
const channelForm = reactive<any>({
  id: null, name: '', channelType: 'EMAIL',
  enabled: 1, priority: 10,
  // 临时字段
  configEmail: '', configWebhook: '', configSecret: '', configMethod: 'POST',
})

async function loadChannels() {
  try {
    const { data } = await getAlertChannels()
    channels.value = data.data || []
  } catch (_) { channels.value = [] }
}

function channelConfigPreview(cfg: any) {
  if (!cfg) return '-'
  if (typeof cfg === 'string') {
    try { cfg = JSON.parse(cfg) } catch (_) { return cfg }
  }
  if (cfg.email) return `收件人: ${cfg.email}`
  if (cfg.webhook) return cfg.webhook.length > 60 ? cfg.webhook.slice(0, 60) + '...' : cfg.webhook
  return JSON.stringify(cfg)
}

function channelTypeTag(type: string) {
  if (type === 'EMAIL') return 'primary'
  if (type === 'DINGTALK') return 'success'
  return 'warning'
}

function openChannelDialog(row?: any) {
  if (row) {
    channelForm.id = row.id
    channelForm.name = row.name
    channelForm.channelType = row.channelType
    channelForm.enabled = row.enabled
    channelForm.priority = row.priority || 10
    // 解析 config JSON
    let cfg: any = row.config
    if (typeof cfg === 'string') { try { cfg = JSON.parse(cfg) } catch (_) { cfg = {} } }
    channelForm.configEmail = cfg?.email || ''
    channelForm.configWebhook = cfg?.webhook || ''
    channelForm.configSecret = cfg?.secret || ''
    channelForm.configMethod = cfg?.method || 'POST'
  } else {
    channelForm.id = null; channelForm.name = ''; channelForm.channelType = 'EMAIL'
    channelForm.enabled = 1; channelForm.priority = 10
    channelForm.configEmail = ''; channelForm.configWebhook = ''
    channelForm.configSecret = ''; channelForm.configMethod = 'POST'
  }
  channelDialog.value = true
}

async function saveChannel() {
  if (!channelForm.name?.trim()) return toast.warning('请输入名称')
  channelSaving.value = true
  try {
    let config: any = {}
    if (channelForm.channelType === 'EMAIL') {
      if (!channelForm.configEmail?.trim()) return toast.warning('请输入收件人邮箱')
      config = { email: channelForm.configEmail.trim() }
    } else if (channelForm.channelType === 'DINGTALK') {
      if (!channelForm.configWebhook?.trim()) return toast.warning('请输入 WebHook URL')
      config = { webhook: channelForm.configWebhook.trim(), secret: channelForm.configSecret?.trim() || undefined }
    } else {
      if (!channelForm.configWebhook?.trim()) return toast.warning('请输入 WebHook URL')
      config = { webhook: channelForm.configWebhook.trim(), method: channelForm.configMethod }
    }
    const body = {
      name: channelForm.name.trim(),
      channelType: channelForm.channelType,
      config: JSON.stringify(config),
      enabled: channelForm.enabled,
      priority: channelForm.priority,
    }
    if (channelForm.id) {
      await updateAlertChannel(channelForm.id, body)
      toast.success('渠道已更新')
    } else {
      await createAlertChannel(body)
      toast.success('渠道已创建')
    }
    channelDialog.value = false
    await loadChannels()
  } catch (e: any) {
    toast.error('保存失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    channelSaving.value = false
  }
}

async function removeChannel(row: any) {
  try {
    await deleteAlertChannel(row.id)
    toast.success('已删除')
    await loadChannels()
  } catch (_) {}
}

function toggleAuto(v: boolean) {
  if (v) {
    timer = window.setInterval(loadAll, refreshSec * 1000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  await loadAll()
  if (autoRefresh.value) timer = window.setInterval(loadAll, refreshSec * 1000)
  await loadAnomalyMetrics()
  if (anomalySelectedMetric.value) await loadAnomalyTrend(anomalySelectedMetric.value)
  window.addEventListener('resize', () => { anomalyChart?.resize() })
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (anomalyChart) { anomalyChart.dispose(); anomalyChart = null }
})
</script>

<style scoped>
.monitor-container { padding: 20px; max-width: 1400px; margin: 0 auto; }
.mon-header { margin-bottom: 16px; display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.mon-header h1 { margin: 0; display: flex; align-items: center; gap: 10px; }
.badge {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin: 0; flex: 1; }
.row { margin-bottom: 16px; }

.health-card { transition: all 0.2s; }
.health-card.up { border-left: 4px solid #67c23a; }
.health-card.down { border-left: 4px solid #f56c6c; }
.hc-top { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.hc-status { font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.hc-detail { font-size: 11px; color: #666; line-height: 1.5; max-height: 100px; overflow: auto; }
.hc-detail .k { display: inline-block; min-width: 60px; color: #999; }
.hc-detail .v { color: #333; font-weight: 500; }
.muted { color: #ccc; font-style: italic; }

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.metric-cell {
  padding: 12px;
  border-radius: 6px;
  background: linear-gradient(135deg, #fafbfc 0%, #f0f2f5 100%);
  text-align: center;
}
.metric-label { font-size: 12px; color: #666; margin-bottom: 6px; }
.metric-value { font-size: 22px; font-weight: 700; }
.metric-good { color: #67c23a; }
.metric-warn { color: #e6a23c; }
.metric-bad { color: #f56c6c; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
  margin-top: 12px;
  font-size: 12px;
}
.info-grid .k { color: #999; margin-right: 4px; }
.info-grid .v { color: #333; font-weight: 500; }
.info-grid .url { font-family: monospace; font-size: 11px; color: #909399; word-break: break-all; }

.alert-item {
  padding: 8px 10px;
  border-bottom: 1px dashed #eee;
}
.alert-msg { font-size: 12px; color: #666; margin: 4px 0; }
.alert-time { font-size: 11px; color: #999; }
.rca-error { margin: 8px 0; }
</style>