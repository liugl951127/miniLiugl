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
        <el-button size="small" @click="connect" :icon="Refresh" circle :loading="connecting" />
      </div>
    </div>

    <el-row :gutter="16">
      <!-- ===== 左侧: 控制面板 ===== -->
      <el-col :span="10">
        <el-card class="ctrl-card">
          <template #header>
            <span>训练控制台</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" size="default">
            <!-- 模型选择 -->
            <el-form-item label="模型" prop="modelName">
              <el-select v-model="form.modelName" placeholder="选择基座模型" style="width:100%" filterable>
                <el-option v-for="m in modelOptions" :key="m.code"
                  :label="`${m.name} (${m.params})`" :value="m.code" />
              </el-select>
            </el-form-item>

            <!-- 语料路径 -->
            <el-form-item label="语料路径" prop="corpusPath">
              <el-input v-model="form.corpusPath" placeholder="/opt/ai-platform/corpus/sample.txt" />
            </el-form-item>

            <!-- 超参数 -->
            <el-divider content-position="left">超参数</el-divider>

            <el-row :gutter="8">
              <el-col :span="12">
                <el-form-item :label="`层数 (n_layer)`" prop="nLayer">
                  <el-input-number v-model="form.nLayer" :min="1" :max="48" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="`头数 (n_head)`" prop="nHead">
                  <el-input-number v-model="form.nHead" :min="1" :max="16" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="嵌入维度 (n_embd)" prop="nEmbd">
                  <el-input-number v-model="form.nEmbd" :min="64" :max="4096" :step="64" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上下文 (block_size)" prop="blockSize">
                  <el-input-number v-model="form.blockSize" :min="16" :max="4096" :step="16" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="迭代数 (max_iters)" prop="maxIters">
                  <el-input-number v-model="form.maxIters" :min="10" :max="10000" :step="10" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="批大小 (batch_size)" prop="batchSize">
                  <el-input-number v-model="form.batchSize" :min="1" :max="256" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="学习率 (lr)" prop="learningRate">
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
        <el-card class="task-list-card" v-loading="tasksLoading">
          <template #header>
            <span>训练历史</span>
            <el-button size="small" text type="primary" style="float:right" @click="loadTasks">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </template>
          <el-empty v-if="!tasksLoading && tasks.length === 0"
            description="暂无训练任务，开始新训练后将在此显示" />
          <div v-else>
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
              <span v-if="currentTask.currentAccuracy != null">
                · Acc {{ (currentTask.currentAccuracy * 100).toFixed(2) }}%
              </span>
            </span>
          </template>

          <div v-if="!currentTask" class="chart-empty">
            <el-empty description="暂无训练任务，请在左侧开始新训练或选择历史任务" />
          </div>
          <div v-else ref="chartEl" class="loss-chart"></div>
        </el-card>

        <!-- 实时事件 -->
        <el-card class="events-card">
          <template #header>
            <span>实时事件</span>
            <div style="display:flex;align-items:center;gap:8px">
              <el-tag v-if="currentTask" size="small" type="info">
                {{ currentTask.status }}
              </el-tag>
              <!-- P1-7: 自动滚动开关 + 滚到最新按钮 -->
              <span style="font-size:11px;color:#9ca3af">{{ scrollPaused ? '已暂停滚动' : '自动滚动' }}</span>
              <el-switch v-model="scrollPaused" size="small" @change="onAutoScrollChange" />
              <el-button size="small" link @click="scrollToBottom" title="滚到最新">
                <el-icon><Bottom /></el-icon>
              </el-button>
            </div>
          </template>

          <div class="events-log" ref="eventsEl" @scroll="onEventsScroll">
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
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bottom, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { trainingApi } from '@/api/training'

// ---- 数据 ----
const connected = ref(false)
const starting = ref(false)
const connecting = ref(false)
const modelOptions = ref([])
const tasks = ref([])
const tasksLoading = ref(false)
const currentTask = ref(null)
const chartEl = ref(null)
const eventsEl = ref(null)
const formRef = ref(null)
// P1-7: 日志自动滚动开关，true=暂停
const scrollPaused = ref(false)
let chart = null
let pollTimer = null
let lossHistory = []  // 必须在 selectTask 之前声明
let accuracyHistory = []  // accuracy 曲线

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

const formRules = {
  modelName: [{ required: true, message: '请选择基座模型', trigger: 'change' }],
  corpusPath: [
    { required: true, message: '请填写语料路径', trigger: 'blur' },
    { min: 3, message: '路径长度至少 3 个字符', trigger: 'blur' }
  ]
}

const events = ref([])

// ---- 连接 ----
async function connect() {
  connecting.value = true
  try {
    const r = await trainingApi.listModels()
    modelOptions.value = r?.data || []
    connected.value = true
    await loadTasks()
    ElMessage.success(`已连接 · ${modelOptions.value.length} 个模型可用`)
  } catch (e) {
    connected.value = false
    ElMessage.error('连接失败: ' + (e?.message || '后端未启动'))
  } finally {
    connecting.value = false
  }
}

async function loadTasks() {
  tasksLoading.value = true
  try {
    const r = await trainingApi.listTasks()
    tasks.value = r?.data || []
    // 自动追踪最新的 TRAINING 任务
    const running = tasks.value.find(t => t.status === 'TRAINING')
    if (running && !currentTask.value) selectTask(running)
  } catch (e) {
    ElMessage.error('加载任务列表失败: ' + (e?.message || ''))
  } finally {
    tasksLoading.value = false
  }
}

// ---- 开始训练 ----
async function startTraining() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    starting.value = true
    try {
      const r = await trainingApi.createTask({ ...form })
      const task = r?.data
      if (!task) throw new Error('后端未返回任务')
      tasks.value.unshift(task)
      selectTask(task)
      ElMessage.success(`训练任务 #${task.id} 已创建`)
      pushEvent('info', `训练任务 #${task.id} 已创建: ${task.modelName}`)
    } catch (e) {
      ElMessage.error('创建失败: ' + (e?.message || '后端未启动'))
    } finally {
      starting.value = false
    }
  })
}

async function cancelTraining() {
  if (!currentTask.value) return
  try {
    await ElMessageBox.confirm(
      `确定要停止训练任务 #${currentTask.value.id} 吗？停止后无法恢复。`,
      '停止训练确认',
      { type: 'warning', confirmButtonText: '停止', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await trainingApi.cancelTask(currentTask.value.id)
    pushEvent('warn', `任务 #${currentTask.value.id} 已停止`)
    stopPoll()
    currentTask.value.status = 'FAILED'
    ElMessage.success('训练已停止')
  } catch (e) {
    ElMessage.error('取消失败: ' + (e?.message || ''))
  }
}

function selectTask(t) {
  currentTask.value = t
  lossHistory = []
  accuracyHistory = []
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
    if (t.currentAccuracy != null) {
      pushEvent('info', `最终 Accuracy: ${(t.currentAccuracy * 100).toFixed(2)}%`)
    }
    if (t.status === 'COMPLETED') pushEvent('success', '训练完成 ✓')
    if (t.status === 'FAILED') pushEvent('error', '训练失败 ✗')
  }
  nextTick(initChart)
}

function resetForm() {
  formRef.value?.clearValidate()
  form.modelName = 'MiniGPT-S'
  form.corpusPath = '/opt/ai-platform/corpus/sample.txt'
  form.nLayer = 12; form.nHead = 12; form.nEmbd = 768
  form.blockSize = 128; form.maxIters = 100; form.batchSize = 32; form.learningRate = 0.0003
  ElMessage.success('表单已重置')
}

// ---- 轮询实时指标 ----
function startPoll() {
  stopPoll()
  pollTimer = setInterval(async () => {
    if (!currentTask.value) { stopPoll(); return }
    try {
      const r = await trainingApi.getTask(currentTask.value.id)
      const t = r?.data
      if (!t) { stopPoll(); return }
      currentTask.value = t

      // 记录 loss & accuracy
      if (t.currentLoss != null) {
        lossHistory.push({ iter: t.currentIter, loss: t.currentLoss })
      }
      if (t.currentAccuracy != null) {
        accuracyHistory.push({ iter: t.currentIter, accuracy: t.currentAccuracy })
      }
      if (t.currentLoss != null || t.currentAccuracy != null) {
        updateChart()
      }

      // 推送事件
      if (t.status === 'COMPLETED') {
        stopPoll()
        pushEvent('success', `训练完成! 最终 Loss=${t.currentLoss?.toFixed(4)} 耗时=${fmtMs(Date.now() - new Date(t.createdAt).getTime())}`)
        ElMessage.success('训练已完成')
      } else if (t.status === 'FAILED') {
        stopPoll()
        pushEvent('error', `训练失败: ${t.errorMessage || '未知错误'}`)
        ElMessage.error('训练失败')
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
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        if (!params || !params.length) return ''
        const iter = params[0].axisValue
        let html = `<b>Iter ${iter}</b>`
        params.forEach(p => {
          const val = p.seriesName === 'Accuracy' && p.data != null
            ? (p.data * 100).toFixed(2) + '%'
            : p.data != null ? Number(p.data).toFixed(4) : '-'
          html += `<br/>${p.marker} ${p.seriesName}: <b>${val}</b>`
        })
        return html
      }
    },
    legend: {
      data: ['Loss', 'Accuracy'],
      top: 4,
      textStyle: { fontSize: 11 }
    },
    grid: { top: 36, right: 56, bottom: 30, left: 60 },
    xAxis: { type: 'value', name: 'Iter', nameLocation: 'middle', nameGap: 22,
      axisLine: { lineStyle: { color: '#ddd' } }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    yAxis: [
      {
        type: 'value', name: 'Loss', nameLocation: 'middle', nameGap: 42,
        axisLine: { lineStyle: { color: '#ddd' } },
        splitLine: { lineStyle: { color: '#f0f0f0' } }
      },
      {
        type: 'value', name: 'Accuracy', nameLocation: 'middle', nameGap: 42,
        min: 0, max: 1,
        axisLine: { lineStyle: { color: '#ddd' } },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Loss', type: 'line',
        smooth: true, symbol: 'none',
        yAxisIndex: 0,
        lineStyle: { color: '#6366f1', width: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.25)' }, { offset: 1, color: 'rgba(99,102,241,0.02)' }] } },
        data: [],
      },
      {
        name: 'Accuracy', type: 'line',
        smooth: true, symbol: 'none',
        yAxisIndex: 1,
        lineStyle: { color: '#67c23a', width: 2 },
        data: []
      }
    ],
    animation: true,
  }
  chart.setOption(option)
  updateChart()
}

function updateChart() {
  if (!chart) return
  const lossData = lossHistory.map(p => [p.iter, p.loss])
  const accData = accuracyHistory.map(p => [p.iter, p.accuracy])
  // 如果没有任何数据，显示提示
  if (lossData.length === 0 && accData.length === 0) {
    chart.setOption({
      title: {
        text: '等待数据...',
        left: 'center', top: 'center',
        textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 'normal' }
      }
    })
    return
  }
  chart.setOption({
    title: { text: '' },
    series: [
      { data: lossData },
      { data: accData }
    ]
  })
}

// ---- 事件日志 ----
// P1-7: 滚到最新
function scrollToBottom() {
  nextTick(() => {
    if (eventsEl.value) eventsEl.value.scrollTop = eventsEl.value.scrollHeight
  })
}

// P1-7: 监听手动滚动
function onEventsScroll() {
  if (!eventsEl.value) return
  const { scrollTop, scrollHeight, clientHeight } = eventsEl.value
  const isAtBottom = scrollHeight - scrollTop - clientHeight < 50
  // 在底部 → 开启自动滚动；远离底部 → 暂停
  if (isAtBottom && scrollPaused.value) {
    scrollPaused.value = false
  } else if (!isAtBottom && !scrollPaused.value) {
    scrollPaused.value = true
  }
}

// P1-7: 切换自动滚动
function onAutoScrollChange(val) {
  if (!val) scrollToBottom()
}

function pushEvent(type, msg) {
  const now = new Date()
  events.value.push({ type, msg, time: `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}` })
  // 限制最多 500 条，防止内存溢出
  if (events.value.length > 500) {
    events.value.splice(0, events.value.length - 500)
  }
  // 仅在未暂停滚动时自动跳到底部
  if (!scrollPaused.value) {
    nextTick(() => {
      if (eventsEl.value) eventsEl.value.scrollTop = eventsEl.value.scrollHeight
    })
  }
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

.loss-chart { width:100%; height:300px; }
.chart-empty { height:300px; display:flex; align-items:center; justify-content:center; }

.events-log { height:260px; overflow-y:auto; font-family:'JetBrains Mono','Consolas',monospace; font-size:12px; }
.no-events { color:#9ca3af; padding:16px; text-align:center; }
.ev { display:flex; gap:10px; padding:3px 8px; border-radius:3px; margin-bottom:2px; }
.ev-info { color:#374151; background:#f9fafb; }
.ev-success { color:#065f46; background:#ecfdf5; }
.ev-error { color:#991b1b; background:#fef2f2; }
.ev-warn { color:#92400e; background:#fffbeb; }
.ev-time { color:#9ca3af; flex-shrink:0; }
.ev-msg { flex:1; word-break: break-word; }

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
