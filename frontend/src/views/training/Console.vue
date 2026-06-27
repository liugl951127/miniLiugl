<!--
  训练控制台 (Day 23)
  联动后端: /training/models | /training/tasks
  实时指标: 轮询 /training/tasks/{id} 驱动 ECharts
-->
<template>
  <div class="training-console">
    <div class="tc-header">
      <h1>🧠 模型训练控制台 <span class="badge">V1.0</span></h1>
      <div class="conn-status">
        <span :class="['dot', connected ? 'dot-green' : 'dot-gray']"></span>
        <span>{{ connected ? '已连接' : '未连接' }}</span>
        <el-button size="small" @click="connect" :icon="Refresh" circle />
      </div>
    </div>

    <el-row :gutter="16">
      <!-- ===== 左侧: 控制面板 ===== -->
      <el-col :span="10">
        <el-card class="ctrl-card">
          <template #header>
            <span>训练控制台</span>
          </template>

          <el-form label-position="top" size="default">
            <!-- 模型选择 -->
            <el-form-item label="模型">
              <el-select v-model="form.modelName" placeholder="选择基座模型" style="width:100%" filterable>
                <el-option v-for="m in modelOptions" :key="m.code"
                  :label="`${m.name} (${m.params})`" :value="m.code" />
              </el-select>
            </el-form-item>

            <!-- 语料路径 -->
            <el-form-item label="语料路径">
              <el-input v-model="form.corpusPath" placeholder="/opt/ai-platform/corpus/sample.txt" />
            </el-form-item>

            <!-- 超参数 -->
            <el-divider content-position="left">超参数</el-divider>

            <el-row :gutter="8">
              <el-col :span="12">
                <el-form-item :label="`层数 (n_layer)`">
                  <el-input-number v-model="form.nLayer" :min="1" :max="48" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="`头数 (n_head)`">
                  <el-input-number v-model="form.nHead" :min="1" :max="16" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="嵌入维度 (n_embd)">
                  <el-input-number v-model="form.nEmbd" :min="64" :max="4096" :step="64" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上下文 (block_size)">
                  <el-input-number v-model="form.blockSize" :min="16" :max="4096" :step="16" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="迭代数 (max_iters)">
                  <el-input-number v-model="form.maxIters" :min="10" :max="10000" :step="10" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="批大小 (batch_size)">
                  <el-input-number v-model="form.batchSize" :min="1" :max="256" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="学习率 (lr)">
                  <el-input-number v-model="form.learningRate" :min="0.00001" :max="0.1"
                    :precision="5" :step="0.0001" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 操作按钮 -->
            <div class="ctrl-actions">
              <el-button type="primary" :loading="starting" @click="startTraining">
                ▶ 开始训练
              </el-button>
              <el-button v-if="currentTask" type="danger" @click="cancelTraining">
                ⏹ 停止训练
              </el-button>
              <el-button @click="resetForm">🔄 重置</el-button>
            </div>
          </el-form>
        </el-card>

        <!-- 任务列表 -->
        <el-card class="task-list-card" v-if="tasks.length > 0">
          <template #header>
            <span>训练历史</span>
          </template>
          <div v-for="t in tasks" :key="t.id" :class="['task-item', `status-${t.status.toLowerCase()}`]"
               @click="selectTask(t)">
            <div class="task-name">{{ t.modelName }}</div>
            <div class="task-meta">
              <el-tag size="small" :type="statusTagType(t.status)">{{ t.status }}</el-tag>
              <span class="task-date">{{ fmtDate(t.createdAt) }}</span>
            </div>
            <el-progress v-if="t.status === 'TRAINING'" :percentage="t.progress || 0"
              :stroke-width="4" style="margin-top:4px" />
          </div>
        </el-card>
      </el-col>

      <!-- ===== 右侧: 指标 + 事件 ===== -->
      <el-col :span="14">
        <!-- 训练指标 -->
        <el-card class="metrics-card">
          <template #header>
            <span>训练指标</span>
            <span v-if="currentTask" class="task-badge">
              {{ currentTask.modelName }} · Iter {{ currentTask.currentIter || 0 }}/{{ currentTask.maxIters || 0 }}
              · Loss {{ currentTask.currentLoss != null ? currentTask.currentLoss.toFixed(4) : '--' }}
            </span>
          </template>

          <div v-if="!currentTask" class="chart-placeholder">
            暂无训练任务<br>
            <small>选择左侧历史任务或开始新训练</small>
          </div>
          <div v-else ref="chartEl" class="loss-chart"></div>
        </el-card>

        <!-- 实时事件 -->
        <el-card class="events-card">
          <template #header>
            <span>实时事件</span>
            <el-tag v-if="currentTask" size="small" type="info">
              {{ currentTask.status }}
            </el-tag>
          </template>

          <div class="events-log" ref="eventsEl">
            <div v-if="events.length === 0" class="no-events">
              等待训练开始...
            </div>
            <div v-for="(ev, i) in events" :key="i" :class="['ev', `ev-${ev.type}`]">
              <span class="ev-time">{{ ev.time }}</span>
              <span class="ev-msg">{{ ev.msg }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { trainingApi } from '@/api/training'

// ---- 数据 ----
const connected = ref(false)
const starting = ref(false)
const modelOptions = ref([])
const tasks = ref([])
const currentTask = ref(null)
const chartEl = ref(null)
const eventsEl = ref(null)
let chart = null
let pollTimer = null

const form = reactive({
  modelName: 'MiniGPT-S',
  corpusPath: '/opt/ai-platform/corpus/sample.txt',
  nLayer: 12,
  nHead: 12,
  nEmbd: 768,
  blockSize: 128,
  maxIters: 100,
  batchSize: 32,
  learningRate: 0.0003,
})

const events = ref([])

// ---- 连接 ----
async function connect() {
  try {
    const r = await trainingApi.listModels()
    modelOptions.value = r?.data || []
    connected.value = true
    await loadTasks()
    ElMessage.success(`已连接 · ${modelOptions.value.length} 个模型可用`)
  } catch (e) {
    connected.value = false
    ElMessage.error('连接失败: ' + (e?.message || '后端未启动'))
  }
}

async function loadTasks() {
  try {
    const r = await trainingApi.listTasks()
    tasks.value = r?.data || []
    // 自动追踪最新的 TRAINING 任务
    const running = tasks.value.find(t => t.status === 'TRAINING')
    if (running) selectTask(running)
  } catch {}
}

// ---- 开始训练 ----
async function startTraining() {
  if (!form.modelName) return ElMessage.warning('请选择模型')
  if (!form.corpusPath.trim()) return ElMessage.warning('请填写语料路径')
  starting.value = true
  try {
    const r = await trainingApi.createTask({ ...form })
    const task = r?.data
    if (!task) throw new Error('后端未返回任务')
    tasks.value.unshift(task)
    selectTask(task)
    pushEvent('info', `训练任务 #${task.id} 已创建: ${task.modelName}`)
  } catch (e) {
    ElMessage.error('创建失败: ' + (e?.message || '后端未启动'))
  } finally {
    starting.value = false
  }
}

async function cancelTraining() {
  if (!currentTask.value) return
  try {
    await trainingApi.cancelTask(currentTask.value.id)
    pushEvent('warn', `任务 #${currentTask.value.id} 已停止`)
    stopPoll()
    currentTask.value.status = 'FAILED'
  } catch (e) {
    ElMessage.error('取消失败')
  }
}

function selectTask(t) {
  currentTask.value = t
  lossHistory = []
  chart?.clear()
  events.value = []
  pushEvent('info', `已加载任务 #${t.id}: ${t.modelName}`)
  if (t.status === 'TRAINING') {
    startPoll()
  } else {
    stopPoll()
    if (t.currentLoss != null) {
      pushEvent('info', `最终 Loss: ${t.currentLoss.toFixed(4)}`)
    }
    if (t.status === 'COMPLETED') pushEvent('success', '训练完成 ✓')
    if (t.status === 'FAILED') pushEvent('error', '训练失败 ✗')
  }
  nextTick(initChart)
}

function resetForm() {
  form.modelName = 'MiniGPT-S'
  form.corpusPath = '/opt/ai-platform/corpus/sample.txt'
  form.nLayer = 12; form.nHead = 12; form.nEmbd = 768
  form.blockSize = 128; form.maxIters = 100; form.batchSize = 32; form.learningRate = 0.0003
}

// ---- 轮询实时指标 ----
let lossHistory = []
function startPoll() {
  stopPoll()
  pollTimer = setInterval(async () => {
    if (!currentTask.value) { stopPoll(); return }
    try {
      const r = await trainingApi.getTask(currentTask.value.id)
      const t = r?.data
      if (!t) { stopPoll(); return }
      currentTask.value = t

      // 记录 loss
      if (t.currentLoss != null) {
        lossHistory.push({ iter: t.currentIter, loss: t.currentLoss })
        updateChart()
      }

      // 推送事件
      if (t.status === 'COMPLETED') {
        stopPoll()
        pushEvent('success', `训练完成! 最终 Loss=${t.currentLoss?.toFixed(4)} 耗时=${fmtMs(Date.now() - new Date(t.createdAt).getTime())}`)
      } else if (t.status === 'FAILED') {
        stopPoll()
        pushEvent('error', `训练失败: ${t.errorMessage || '未知错误'}`)
      }

      // 同步到 tasks 列表
      const idx = tasks.value.findIndex(x => x.id === t.id)
      if (idx >= 0) tasks.value[idx] = t
    } catch {
      // ignore
    }
  }, 1000)
}

function stopPoll() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

// ---- 图表 ----
function initChart() {
  if (!chartEl.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartEl.value)
  const option = {
    backgroundColor: 'transparent',
    grid: { top: 30, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'value', name: 'Iter', nameLocation: 'middle', nameGap: 22,
      axisLine: { lineStyle: { color: '#ddd' } }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    yAxis: { type: 'value', name: 'Loss', nameLocation: 'middle', nameGap: 38,
      axisLine: { lineStyle: { color: '#ddd' } }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    series: [{
      name: 'Loss', type: 'line',
      smooth: true, symbol: 'none',
      lineStyle: { color: '#6366f1', width: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.25)' }, { offset: 1, color: 'rgba(99,102,241,0.02)' }] } },
      data: [],
    }],
    animation: true,
  }
  chart.setOption(option)
}

function updateChart() {
  if (!chart || lossHistory.length === 0) return
  chart.setOption({
    series: [{ data: lossHistory.map(p => [p.iter, p.loss]) }]
  })
}

// ---- 事件日志 ----
function pushEvent(type, msg) {
  const now = new Date()
  events.value.push({ type, msg, time: `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}` })
  nextTick(() => {
    if (eventsEl.value) eventsEl.value.scrollTop = eventsEl.value.scrollHeight
  })
}

// ---- 工具 ----
function statusTagType(s) {
  return { PENDING: 'info', TRAINING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info'
}
function fmtDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2,'0')}`
}
function fmtMs(ms) {
  if (ms < 60000) return Math.round(ms/1000) + 's'
  return Math.round(ms/60000) + 'm'
}

onMounted(() => {
  connect()
})

onUnmounted(() => {
  stopPoll()
  chart?.dispose()
})
</script>

<style scoped>
.training-console { padding: 16px; }
.tc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tc-header h1 { margin: 0; font-size: 22px; }
.badge { background: linear-gradient(135deg,#6366f1,#8b5cf6); color:#fff;
  padding:2px 10px; border-radius:12px; font-size:12px; margin-left:8px; }
.conn-status { display:flex; align-items:center; gap:8px; color:#6b7280; font-size:13px; }
.dot { width:8px; height:8px; border-radius:50%; display:inline-block; }
.dot-green { background:#22c55e; box-shadow:0 0 4px #22c55e; }
.dot-gray { background:#9ca3af; }

.ctrl-card, .task-list-card { margin-bottom: 16px; }
.metrics-card, .events-card { height: calc(50% - 8px); display: flex; flex-direction: column; }

.task-badge { font-size:12px; color:#6b7280; margin-left:12px; }

.loss-chart { width:100%; height:220px; }
.chart-placeholder { height:220px; display:flex; flex-direction:column;
  align-items:center; justify-content:center; color:#9ca3af; font-size:14px; line-height:2; }
.chart-placeholder small { font-size:12px; color:#c0c4cc; }

.events-log { height:220px; overflow-y:auto; font-family:'JetBrains Mono','Consolas',monospace; font-size:12px; }
.no-events { color:#9ca3af; padding:16px; text-align:center; }
.ev { display:flex; gap:10px; padding:3px 8px; border-radius:3px; margin-bottom:2px; }
.ev-info { color:#374151; background:#f9fafb; }
.ev-success { color:#065f46; background:#ecfdf5; }
.ev-error { color:#991b1b; background:#fef2f2; }
.ev-warn { color:#92400e; background:#fffbeb; }
.ev-time { color:#9ca3af; flex-shrink:0; }
.ev-msg { flex:1; }

.ctrl-actions { display:flex; gap:8px; margin-top:8px; }

.task-item { padding:8px 10px; border-radius:6px; cursor:pointer;
  border:1px solid #e5e7eb; margin-bottom:6px; transition:background .2s; }
.task-item:hover { background:#f9fafb; }
.task-item.status-training { border-color:#f59e0b; background:#fffbeb; }
.task-item.status-completed { border-color:#10b981; }
.task-item.status-failed { border-color:#ef4444; }
.task-name { font-weight:600; font-size:13px; color:#1f2937; }
.task-meta { display:flex; align-items:center; gap:8px; margin-top:4px; }
.task-date { font-size:11px; color:#9ca3af; margin-left:auto; }
</style>
