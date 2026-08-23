<!--
  @file builder/Monitor.vue - 实时部署监控 (V1.0)
  路由: /builder/monitor
  功能: 部署进度 / 阶段日志 / 容器状态 / 实时指标 / 告警
-->
<template>
  <div class="monitor-page">
    <!-- 顶部状态卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-deploying">
          <div class="sc-icon">🚀</div>
          <div class="sc-body">
            <div class="sc-value">{{ deployStatus }}</div>
            <div class="sc-label">部署状态</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="sc-icon">📦</div>
          <div class="sc-body">
            <div class="sc-value">{{ completedCount }}/{{ stages.length }}</div>
            <div class="sc-label">完成阶段</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="sc-icon">⏱️</div>
          <div class="sc-body">
            <div class="sc-value">{{ elapsedTime }}</div>
            <div class="sc-label">已耗时</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="sc-icon">📊</div>
          <div class="sc-body">
            <div class="sc-value">{{ runningAgents }}/{{ totalAgents }}</div>
            <div class="sc-label">运行智能体</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 部署阶段时间线 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>📋 部署阶段</span>
              <el-button-group size="small">
                <el-button :icon="VideoPlay" @click="start" :disabled="deploying">启动</el-button>
                <el-button :icon="VideoPause" @click="pause" :disabled="!deploying">暂停</el-button>
                <el-button :icon="RefreshRight" @click="restart">重试</el-button>
              </el-button-group>
            </div>
          </template>

          <div class="stages">
            <div v-for="(s, i) in stages" :key="i" class="stage-row">
              <div class="stage-icon" :class="s.status">
                <span v-if="s.status === 'done'">✓</span>
                <span v-else-if="s.status === 'running'">⏳</span>
                <span v-else-if="s.status === 'failed'">✗</span>
                <span v-else>{{ i + 1 }}</span>
              </div>
              <div class="stage-info">
                <div class="stage-name">{{ s.name }}</div>
                <div class="stage-desc">{{ s.desc }}</div>
              </div>
              <div class="stage-time">
                <el-tag v-if="s.status === 'done'" type="success" effect="plain" size="small">{{ s.duration }}s</el-tag>
                <el-tag v-else-if="s.status === 'running'" type="warning" effect="dark" size="small">进行中</el-tag>
                <el-tag v-else-if="s.status === 'failed'" type="danger" effect="dark" size="small">失败</el-tag>
                <el-tag v-else type="info" effect="plain" size="small">等待</el-tag>
              </div>
            </div>
          </div>

          <!-- 进度条 -->
          <el-progress :percentage="progressPct" :status="progressStatus"
            :stroke-width="10" style="margin-top:16px" />
        </el-card>

        <!-- 实时日志 -->
        <el-card shadow="never" style="margin-top:12px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>📜 实时日志</span>
              <div>
                <el-tag v-for="lvl in ['INFO', 'WARN', 'ERROR']" :key="lvl" size="small" :type="logLevelColor(lvl)"
                  :effect="logLevel === lvl ? 'dark' : 'plain'" style="margin-left:4px;cursor:pointer"
                  @click="logLevel = lvl">{{ lvl }}</el-tag>
                <el-button size="small" link @click="logs = []" style="margin-left:8px">清空</el-button>
              </div>
            </div>
          </template>
          <div class="log-stream" ref="logScroll">
            <div v-for="(l, i) in filteredLogs" :key="i" class="log-line" :class="l.level.toLowerCase()">
              <span class="log-time">[{{ l.time }}]</span>
              <span class="log-level">{{ l.level }}</span>
              <span class="log-text">{{ l.text }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 容器状态 + 指标 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>🐳 智能体容器</span></template>
          <div class="container-list">
            <div v-for="c in containers" :key="c.name" class="container-item">
              <div class="ci-status" :class="c.status">
                <div class="ci-dot"></div>
                <span>{{ c.status }}</span>
              </div>
              <div class="ci-info">
                <div class="ci-name">{{ c.emoji }} {{ c.name }}</div>
                <div class="ci-image">{{ c.image }}</div>
              </div>
              <div class="ci-metrics">
                <div class="ci-metric">CPU {{ c.cpu }}%</div>
                <div class="ci-metric">MEM {{ c.mem }}%</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 实时指标 (简易折线) -->
        <el-card shadow="never" style="margin-top:12px">
          <template #header><span>📈 实时指标 (QPS)</span></template>
          <div class="metric-chart">
            <svg viewBox="0 0 400 100" preserveAspectRatio="none" style="width:100%;height:100px">
              <defs>
                <linearGradient id="grad" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#6366f1" stop-opacity="0.4" />
                  <stop offset="100%" stop-color="#6366f1" stop-opacity="0" />
                </linearGradient>
              </defs>
              <path :d="chartPath" fill="url(#grad)" />
              <path :d="chartLine" stroke="#6366f1" stroke-width="2" fill="none" />
            </svg>
            <div class="chart-stats">
              <div><span>当前</span><strong>{{ currentQps }} req/s</strong></div>
              <div><span>峰值</span><strong>{{ peakQps }} req/s</strong></div>
              <div><span>平均</span><strong>{{ avgQps }} req/s</strong></div>
            </div>
          </div>
        </el-card>

        <!-- 告警 -->
        <el-card shadow="never" style="margin-top:12px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>🔔 告警</span>
              <el-badge :value="alerts.length" :max="99" />
            </div>
          </template>
          <div v-if="alerts.length === 0" class="alerts-empty">无告警, 系统健康 ✓</div>
          <div v-else>
            <div v-for="(a, i) in alerts" :key="i" class="alert-item" :class="a.level">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ a.text }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部操作 -->
    <div class="action-bar">
      <el-button size="large" round :icon="RefreshLeft" @click="$router.push('/builder/deploy')">返回</el-button>
      <el-button size="large" round :icon="View" @click="showDetails = true">查看详情</el-button>
      <el-button size="large" round type="primary" :icon="ArrowRight" @click="$router.push('/builder/releases')"
        :disabled="deployStatus !== '运行中'">
        下一步: 发布管理 →
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, RefreshRight, RefreshLeft, ArrowRight, View, WarningFilled } from '@element-plus/icons-vue'

const deploying = ref(true)
const deployStatus = ref('部署中')
const elapsedTime = ref('00:00')
const startTime = ref(Date.now())
const logLevel = ref('INFO')
const logScroll = ref(null)

const stages = ref([
  { name: '代码校验', desc: '检查智能体配置合法性', status: 'done', duration: 1 },
  { name: '构建镜像', desc: 'Docker build, 4 个镜像并行', status: 'done', duration: 35 },
  { name: '镜像推送', desc: '推送到 registry.minimax.io', status: 'done', duration: 22 },
  { name: '创建命名空间', desc: 'agent-forge namespace', status: 'done', duration: 2 },
  { name: '应用配置', desc: 'ConfigMap / Secret / ServiceAccount', status: 'running', duration: 0 },
  { name: '部署 Pod', desc: '3 个 Deployment, 6 个 Pod', status: 'pending', duration: 0 },
  { name: '健康检查', desc: 'Liveness / Readiness probe', status: 'pending', duration: 0 },
  { name: '流量接入', desc: 'Service / Ingress 接入', status: 'pending', duration: 0 }
])
const completedCount = computed(() => stages.value.filter(s => s.status === 'done').length)
const progressPct = computed(() => Math.round(completedCount.value / stages.value.length * 100))
const progressStatus = computed(() => {
  if (stages.value.some(s => s.status === 'failed')) return 'exception'
  if (completedCount.value === stages.value.length) return 'success'
  return ''
})

const totalAgents = ref(3)
const runningAgents = ref(0)

const containers = ref([
  { name: '小课', emoji: '📚', image: 'agent-forge/xiaoke:v1.0.0', status: 'running', cpu: 23, mem: 45 },
  { name: '小助', emoji: '💰', image: 'agent-forge/xiaozhu:v1.0.0', status: 'running', cpu: 18, mem: 38 },
  { name: '小审', emoji: '🔍', image: 'agent-forge/xiaoshen:v1.0.0', status: 'starting', cpu: 5, mem: 22 }
])

const logs = ref([
  { time: '13:30:01', level: 'INFO', text: '开始部署 Agent Forge v1.0.0' },
  { time: '13:30:02', level: 'INFO', text: '代码校验通过, 4 个智能体配置合法' },
  { time: '13:30:03', level: 'INFO', text: '开始构建镜像: agent-forge/xiaoke' },
  { time: '13:30:08', level: 'INFO', text: 'Step 1/8 : FROM minimax/base-agent:v6.8' },
  { time: '13:30:15', level: 'INFO', text: 'Step 4/8 : COPY prompts/ /app/prompts/' },
  { time: '13:30:30', level: 'INFO', text: '镜像构建完成: xiaoke (340 MB)' },
  { time: '13:30:32', level: 'INFO', text: '开始构建镜像: agent-forge/xiaozhu' },
  { time: '13:30:55', level: 'INFO', text: '镜像构建完成: xiaozhu (340 MB)' },
  { time: '13:31:00', level: 'INFO', text: '开始构建镜像: agent-forge/xiaoshen' },
  { time: '13:31:22', level: 'INFO', text: '镜像构建完成: xiaoshen (340 MB)' },
  { time: '13:31:25', level: 'INFO', text: '推送到 registry.minimax.io/agent-forge' },
  { time: '13:31:47', level: 'INFO', text: '镜像推送完成 (3/3)' },
  { time: '13:31:48', level: 'INFO', text: '创建命名空间: agent-forge' },
  { time: '13:31:50', level: 'INFO', text: '应用 ConfigMap: agent-forge-config' },
  { time: '13:31:51', level: 'INFO', text: '应用 Secret: registry-credentials' },
  { time: '13:32:00', level: 'INFO', text: '创建 Deployment: xiaoke (2 副本)' },
  { time: '13:32:01', level: 'INFO', text: '创建 Deployment: xiaozhu (2 副本)' },
  { time: '13:32:02', level: 'INFO', text: '创建 Deployment: xiaoshen (2 副本)' },
  { time: '13:32:30', level: 'INFO', text: 'Pod xiaoke-7d8f-abc12 启动中' },
  { time: '13:32:45', level: 'INFO', text: 'Pod xiaoke-7d8f-abc12 Ready' },
  { time: '13:32:50', level: 'WARN', text: 'Pod xiaoshen-9a3f-xyz78 内存使用率 22% (低)' },
  { time: '13:33:00', level: 'INFO', text: '所有 Pod 健康检查通过' },
  { time: '13:33:10', level: 'INFO', text: 'Service agent-forge-svc 创建' }
])

const filteredLogs = computed(() => {
  if (logLevel.value === 'INFO') return logs.value
  return logs.value.filter(l => l.level === logLevel.value)
})

const alerts = ref([
  { level: 'warn', text: 'Pod xiaoshen-9a3f 内存使用率持续低于 25%, 可降低副本' }
])

const chartData = ref([])
const currentQps = ref(120)
const peakQps = ref(180)
const avgQps = ref(125)

let timer = null

function start() {
  deploying.value = true
  startTime.value = Date.now()
  timer = setInterval(() => {
    const elapsed = Math.floor((Date.now() - startTime.value) / 1000)
    const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
    const s = String(elapsed % 60).padStart(2, '0')
    elapsedTime.value = `${m}:${s}`

    // 模拟 QPS 抖动
    const newQps = 100 + Math.random() * 80
    chartData.value.push(newQps)
    if (chartData.value.length > 30) chartData.value.shift()
    currentQps.value = Math.round(newQps)
    peakQps.value = Math.max(peakQps.value, currentQps.value)
    avgQps.value = Math.round(chartData.value.reduce((a, b) => a + b, 0) / chartData.value.length)

    // 推进 stage
    const runningIdx = stages.value.findIndex(s => s.status === 'running')
    if (runningIdx >= 0 && Math.random() < 0.15) {
      stages.value[runningIdx].status = 'done'
      stages.value[runningIdx].duration = Math.floor(Math.random() * 10) + 5
      if (runningIdx + 1 < stages.value.length) {
        stages.value[runningIdx + 1].status = 'running'
      }
      addLog('INFO', `阶段完成: ${stages.value[runningIdx].name}`)
    }
    if (completedCount.value === stages.value.length) {
      deployStatus.value = '运行中'
      runningAgents.value = totalAgents.value
      ElMessage.success('部署完成! 所有智能体已上线')
    }
  }, 1000)
}
function pause() { deploying.value = false; clearInterval(timer) }
function restart() { stages.value.forEach(s => s.status = 'pending'); start() }

function addLog(level, text) {
  const t = new Date()
  logs.value.push({
    time: `${String(t.getHours()).padStart(2,'0')}:${String(t.getMinutes()).padStart(2,'0')}:${String(t.getSeconds()).padStart(2,'0')}`,
    level, text
  })
  nextTick(() => {
    if (logScroll.value) logScroll.value.scrollTop = logScroll.value.scrollHeight
  })
}

const chartPath = computed(() => {
  if (!chartData.value.length) return ''
  const w = 400, h = 100
  const max = Math.max(...chartData.value, 200)
  const points = chartData.value.map((v, i) => {
    const x = (i / (chartData.value.length - 1)) * w
    const y = h - (v / max) * h
    return `${x},${y}`
  })
  return `M 0,${h} L ${points.join(' L ')} L ${w},${h} Z`
})
const chartLine = computed(() => {
  if (!chartData.value.length) return ''
  const w = 400, h = 100
  const max = Math.max(...chartData.value, 200)
  const points = chartData.value.map((v, i) => {
    const x = (i / (chartData.value.length - 1)) * w
    const y = h - (v / max) * h
    return `${x},${y}`
  })
  return `M ${points.join(' L ')}`
})

function logLevelColor(lvl) {
  return lvl === 'INFO' ? 'info' : lvl === 'WARN' ? 'warning' : 'danger'
}

onMounted(() => {
  // 初始 30 个数据点
  for (let i = 0; i < 30; i++) chartData.value.push(100 + Math.random() * 80)
  start()
})
onUnmounted(() => clearInterval(timer))

const showDetails = ref(false)
</script>

<style scoped>
.monitor-page { max-width: 1400px; margin: 0 auto; }

.stat-card { display: flex; align-items: center; gap: 12px; border-radius: 12px; }
.sc-icon { font-size: 32px; }
.sc-body { flex: 1; }
.sc-value { font-size: 18px; font-weight: 700; color: #1e293b; }
.sc-label { font-size: 12px; color: #64748b; margin-top: 2px; }
.stat-card.stat-deploying {
  background: linear-gradient(135deg, #dbeafe 0%, #ede9fe 100%);
  border: 1px solid #c7d2fe;
}

/* 阶段时间线 */
.stages { display: flex; flex-direction: column; gap: 8px; }
.stage-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; background: #fafbfc;
  border-radius: 8px; border: 1px solid #f1f5f9;
}
.stage-icon {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
  background: #e2e8f0; color: #64748b;
}
.stage-icon.done { background: #10b981; color: white; }
.stage-icon.running {
  background: #f59e0b; color: white;
  animation: spin 2s linear infinite;
}
.stage-icon.failed { background: #ef4444; color: white; }
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }

.stage-info { flex: 1; }
.stage-name { font-weight: 600; color: #1e293b; font-size: 14px; }
.stage-desc { font-size: 12px; color: #64748b; margin-top: 2px; }

/* 日志流 */
.log-stream {
  height: 280px; overflow-y: auto; padding: 12px;
  background: #0f172a; color: #e2e8f0; border-radius: 8px;
  font-family: 'JetBrains Mono', 'Courier New', monospace; font-size: 12px;
  line-height: 1.6;
}
.log-line { display: flex; gap: 8px; }
.log-time { color: #64748b; }
.log-level { font-weight: 700; min-width: 50px; }
.log-line.info .log-level { color: #60a5fa; }
.log-line.warn .log-level { color: #fbbf24; }
.log-line.error .log-level { color: #f87171; }
.log-text { color: #cbd5e1; }

/* 容器 */
.container-list { display: flex; flex-direction: column; gap: 8px; }
.container-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; background: #fafbfc;
  border-radius: 8px; border: 1px solid #f1f5f9;
}
.ci-status { display: flex; align-items: center; gap: 6px; font-size: 11px; }
.ci-dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; }
.ci-status.running .ci-dot { background: #10b981; }
.ci-status.running { color: #10b981; }
.ci-status.starting .ci-dot { background: #f59e0b; animation: blink 1s infinite; }
.ci-status.starting { color: #f59e0b; }
.ci-status.failed .ci-dot { background: #ef4444; }
.ci-status.failed { color: #ef4444; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.ci-info { flex: 1; min-width: 0; }
.ci-name { font-weight: 600; font-size: 13px; color: #1e293b; }
.ci-image { font-size: 10px; color: #94a3b8; font-family: monospace; }
.ci-metrics { display: flex; gap: 8px; font-size: 10px; color: #475569; }
.ci-metric { padding: 2px 6px; background: white; border-radius: 4px; }

/* 指标图 */
.metric-chart { }
.chart-stats { display: flex; justify-content: space-around; margin-top: 8px; font-size: 12px; }
.chart-stats div { display: flex; flex-direction: column; align-items: center; }
.chart-stats span { color: #64748b; }
.chart-stats strong { color: #1e293b; font-size: 14px; margin-top: 2px; }

/* 告警 */
.alerts-empty { padding: 16px; text-align: center; color: #10b981; font-size: 13px; }
.alert-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 6px;
  font-size: 12px; margin-bottom: 4px;
}
.alert-item.warn { background: #fef3c7; color: #92400e; }
.alert-item.error { background: #fee2e2; color: #991b1b; }

.action-bar { display: flex; justify-content: space-between; margin-top: 16px; }
</style>
