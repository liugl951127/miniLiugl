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
<template>
  <div class="monitor-container">
    <div class="mon-header">
      <h1>📊 系统监控 <span class="badge">V5.6</span></h1>
      <p class="sub">实时指标 + JVM + DB + 磁盘 + 告警 · 自动刷新 {{ refreshSec }}s</p>
      <el-switch v-model="autoRefresh" active-text="自动" inactive-text="手动" @change="toggleAuto" />
    </div>

    <!-- 5 个健康卡片 -->
    <el-row :gutter="16" class="row">
      <el-col v-for="(h, key) in healths" :key="key" :span="4" :xs="12" :sm="8" :md="4">
        <el-card :class="['health-card', h.status === 'UP' ? 'up' : 'down']">
          <div class="hc-top">
            <el-icon :size="22" :color="h.status === 'UP' ? '#67c23a' : '#f56c6c'">
              <component :is="h.status === 'UP' ? CircleCheck : CircleClose" />
            </el-icon>
            <strong>{{ key }}</strong>
          </div>
          <div class="hc-status">{{ h.status || '...' }}</div>
          <div class="hc-detail" v-if="h.detail">
            <div v-for="(v, k) in flatten(h.detail)" :key="k">
              <span class="k">{{ k }}:</span>
              <span class="v">{{ v }}</span>
            </div>
          </div>
          <div class="hc-detail" v-else>
            <div class="muted">点击 "刷新" 加载</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时业务指标 -->
    <el-row :gutter="16" class="row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>📈 实时业务指标</span>
            <el-button-group style="margin-left:12px">
              <el-button size="small" @click="loadMetrics">刷新</el-button>
            </el-button-group>
          </template>
          <div class="metric-grid">
            <div class="metric-cell" v-for="(v, k) in metrics" :key="k">
              <div class="metric-label">{{ metricLabel(k) }}</div>
              <div class="metric-value" :class="metricClass(k)">{{ formatNum(v) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="row">
      <!-- JVM -->
      <el-col :span="12">
        <el-card>
          <template #header><span>☕ JVM 内存</span></template>
          <div v-if="jvmInfo">
            <el-progress
              :percentage="jvmInfo.usedPercent || 0"
              :status="jvmInfo.usedPercent > 80 ? 'exception' : 'success'"
              :stroke-width="20"
              :format="(p: number) => `${p}% (${jvmInfo.usedMb}MB / ${jvmInfo.totalMb}MB)`"
            />
            <div class="info-grid">
              <div><span class="k">Max:</span> <span>{{ jvmInfo.maxMb }} MB</span></div>
              <div><span class="k">Init:</span> <span>{{ jvmInfo.initMb }} MB</span></div>
              <div><span class="k">GC:</span> <span>{{ jvmInfo.gcCount || '-' }}</span></div>
              <div><span class="k">Threads:</span> <span>{{ jvmInfo.threadCount || '-' }}</span></div>
              <div><span class="k">UP:</span> <span>{{ jvmInfo.uptime || '-' }}</span></div>
              <div><span class="k">Java:</span> <span>{{ jvmInfo.javaVersion || '-' }}</span></div>
            </div>
          </div>
          <el-empty v-else description="加载中..." />
        </el-card>
      </el-col>

      <!-- 数据库 -->
      <el-col :span="12">
        <el-card>
          <template #header><span>🗄️ 数据库连接池 (HikariCP)</span></template>
          <div v-if="dbInfo">
            <el-progress
              :percentage="dbInfo.usagePercent || 0"
              :status="dbInfo.usagePercent > 80 ? 'exception' : 'success'"
              :stroke-width="20"
              :format="(p: number) => `${dbInfo.active}/${dbInfo.total}`"
            />
            <div class="info-grid">
              <div><span class="k">Active:</span> <span>{{ dbInfo.active }}</span></div>
              <div><span class="k">Idle:</span> <span>{{ dbInfo.idle }}</span></div>
              <div><span class="k">Total:</span> <span>{{ dbInfo.total }}</span></div>
              <div><span class="k">Wait:</span> <span>{{ dbInfo.waiting }}</span></div>
              <div><span class="k">Max:</span> <span>{{ dbInfo.max }}</span></div>
              <div><span class="k">Min Idle:</span> <span>{{ dbInfo.minIdle }}</span></div>
              <div><span class="k">URL:</span> <span class="url">{{ dbInfo.urlShort || dbInfo.url }}</span></div>
            </div>
          </div>
          <el-empty v-else description="加载中..." />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="row">
      <!-- 磁盘 -->
      <el-col :span="12">
        <el-card>
          <template #header><span>💾 磁盘使用</span></template>
          <div v-if="diskInfo">
            <el-progress
              :percentage="diskInfo.usagePercent || 0"
              :status="diskInfo.usagePercent > 85 ? 'exception' : 'success'"
              :stroke-width="20"
              :format="(p: number) => `${diskInfo.usedGb}GB / ${diskInfo.totalGb}GB`"
            />
            <div class="info-grid">
              <div><span class="k">Free:</span> <span>{{ diskInfo.freeGb }} GB</span></div>
              <div><span class="k">Path:</span> <span class="url">{{ diskInfo.path }}</span></div>
            </div>
          </div>
          <el-empty v-else description="加载中..." />
        </el-card>
      </el-col>

      <!-- 告警 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🚨 告警 (Firing: {{ alerts.length }})</span>
          </template>
          <el-empty v-if="!alerts.length" description="无活跃告警 🎉" />
          <el-scrollbar v-else style="height:200px">
            <div v-for="a in alerts" :key="a.id" class="alert-item">
              <el-tag :type="a.severity === 'critical' ? 'danger' : 'warning'" size="small">
                {{ a.severity || 'warn' }}
              </el-tag>
              <strong>{{ a.ruleName || a.name || '未命名' }}</strong>
              <div class="alert-msg">{{ a.message }}</div>
              <div class="alert-time">{{ formatTime(a.firedAt) }}</div>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <!-- V5.9: 告警规则管理 -->
    <el-row :gutter="16" class="row" v-if="canEditRules">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>⚙️ 告警规则 (V5.9)</span>
            <el-button-group style="margin-left:12px">
              <el-button size="small" @click="loadRules">刷新</el-button>
              <el-button size="small" type="primary" @click="openRuleDialog()">+ 新增规则</el-button>
            </el-button-group>
          </template>
          <el-table :data="rules" stripe size="small" empty-text="暂无规则">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="service" label="服务" width="100" />
            <el-table-column prop="metricName" label="指标" min-width="100" />
            <el-table-column label="阈值" width="120">
              <template #default="s">
                <code>{{ s.row.operator }} {{ s.row.threshold }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="severity" label="级别" width="90">
              <template #default="s">
                <el-tag :type="severityType(s.row.severity)" size="small">{{ s.row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cooldownMinutes" label="冷却(分)" width="90" />
            <el-table-column label="状态" width="80">
              <template #default="s">
                <el-tag :type="s.row.enabled ? 'success' : 'info'" size="small">
                  {{ s.row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="s">
                <el-button size="small" text type="primary" @click="openRuleDialog(s.row)">编辑</el-button>
                <el-button size="small" text type="danger" @click="removeRule(s.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- V5.9: 规则编辑弹窗 -->
    <el-dialog v-model="ruleDialog" :title="ruleForm.id ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="ruleForm" label-width="100px" size="default">
        <el-form-item label="名称"><el-input v-model="ruleForm.name" placeholder="e.g. 高错误率告警" /></el-form-item>
        <el-form-item label="服务">
          <el-select v-model="ruleForm.service" placeholder="选择服务" style="width:100%">
            <el-option v-for="s in serviceOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标">
          <el-input v-model="ruleForm.metricName" placeholder="e.g. http_5xx_total / jvm.memory.used" />
        </el-form-item>
        <el-form-item label="运算符">
          <el-select v-model="ruleForm.operator" style="width:100%">
            <el-option label="> 大于" value=">" />
            <el-option label=">= 大于等于" value=">=" />
            <el-option label="< 小于" value="<" />
            <el-option label="<= 小于等于" value="<=" />
            <el-option label="== 等于" value="==" />
            <el-option label="!= 不等于" value="!=" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值"><el-input-number v-model="ruleForm.threshold" :min="0" :step="0.1" style="width:100%" /></el-form-item>
        <el-form-item label="级别">
          <el-select v-model="ruleForm.severity" style="width:100%">
            <el-option label="critical 严重" value="critical" />
            <el-option label="warning 警告" value="warning" />
            <el-option label="info 提示" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="冷却(分)"><el-input-number v-model="ruleForm.cooldownMinutes" :min="1" :max="1440" style="width:100%" /></el-form-item>
        <el-form-item label="通知渠道">
          <el-input v-model="ruleForm.notifyChannel" placeholder="e.g. email,websocket,dingtalk (逗号分隔)" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getMonitorAlertRules, createMonitorAlertRule, updateMonitorAlertRule, deleteMonitorAlertRule } from '@/api/monitor'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const token = localStorage.getItem('access_token') || ''
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

async function loadAll() {
  await Promise.all([loadHealth(), loadMetrics(), loadJvm(), loadDb(), loadDisk(), loadAlerts(), loadRules()])
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
  if (!ruleForm.name?.trim()) return ElMessage.warning('请输入名称')
  if (!ruleForm.metricName?.trim()) return ElMessage.warning('请输入指标名')
  ruleSaving.value = true
  try {
    if (ruleForm.id) {
      await updateMonitorAlertRule(ruleForm.id, ruleForm)
      ElMessage.success('规则已更新')
    } else {
      await createMonitorAlertRule(ruleForm)
      ElMessage.success('规则已创建')
    }
    ruleDialog.value = false
    await loadRules()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    ruleSaving.value = false
  }
}

// V5.9: 删除规则 (二次确认)
async function removeRule(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除规则 [${row.name}]?`, '提示', { type: 'warning' })
    await deleteMonitorAlertRule(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (_) { /* cancel */ }
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
})
onUnmounted(() => { if (timer) clearInterval(timer) })
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
</style>