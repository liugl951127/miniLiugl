<template>
  <div class="training-viz">
    <el-card>
      <template #header>
        <div class="header">
          <span>📈 训练可视化 (V2.7.5 - TensorBoard 风格)</span>
          <el-button-group>
            <el-button type="primary" :loading="starting" @click="startDemo">▶️ 启动演示训练</el-button>
            <el-button @click="refreshTasks">🔄 刷新</el-button>
          </el-button-group>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 任务列表 -->
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>📋 训练任务</template>
            <el-empty v-if="!tasks.length" description="暂无任务, 点击右上角启动演示" />
            <div v-else class="task-list">
              <div v-for="t in tasks" :key="t.taskId"
                   :class="['task-item', { active: selected === t.taskId }]"
                   @click="selectTask(t)">
                <div class="task-name">
                  <el-tag size="small" :type="statusType(t.status)">{{ t.status }}</el-tag>
                  <b>{{ t.name }}</b>
                </div>
                <div class="task-meta">
                  <el-text size="small">{{ t.model }} · {{ t.currentEpoch }}/{{ t.totalEpochs }} epoch</el-text>
                </div>
                <div class="task-progress">
                  <el-progress :percentage="t.totalEpochs > 0 ? Math.round(t.currentEpoch * 100 / t.totalEpochs) : 0" :stroke-width="6" />
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 曲线 + 详情 -->
        <el-col :span="16">
          <el-card v-if="selected" shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span>📊 训练曲线 - {{ currentTask.name }}</span>
                <el-button size="small" type="danger" @click="del">删除</el-button>
              </div>
            </template>

            <el-row :gutter="8" style="margin-bottom: 12px">
              <el-col :span="6">
                <el-statistic title="总 Epoch" :value="currentTask.totalEpochs" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="已完成" :value="currentTask.currentEpoch" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="当前 Loss" :value="latestPoint?.loss ?? 0" :precision="4" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="耗时" :value="currentTask.durationMs" suffix="ms" />
              </el-col>
            </el-row>

            <div class="chart-area">
              <canvas ref="lossChart" width="700" height="280"></canvas>
            </div>

            <el-table :data="points" stripe size="small" style="margin-top: 12px">
              <el-table-column prop="epoch" label="Epoch" width="80" />
              <el-table-column prop="loss" label="Loss" width="100">
                <template #default="{ row }">{{ row.loss.toFixed(4) }}</template>
              </el-table-column>
              <el-table-column prop="valLoss" label="Val Loss" width="100">
                <template #default="{ row }">{{ row.valLoss.toFixed(4) }}</template>
              </el-table-column>
              <el-table-column prop="accuracy" label="Acc" width="80">
                <template #default="{ row }">{{ (row.accuracy * 100).toFixed(1) }}%</template>
              </el-table-column>
              <el-table-column prop="perplexity" label="PPL" width="100">
                <template #default="{ row }">{{ row.perplexity.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="learningRate" label="LR" width="80" />
              <el-table-column prop="elapsedMs" label="ms" width="100" />
            </el-table>
          </el-card>
          <el-empty v-else description="选择左侧任务查看训练曲线" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  demoTraining, listTrainingTasks, getTrainingHistory, deleteTrainingTask
} from '@/api/ai'

const tasks = ref([])
const selected = ref(null)
const currentTask = ref({})
const points = ref([])
const latestPoint = computed(() => points.value[points.value.length - 1])
const starting = ref(false)
const lossChart = ref(null)
let pollTimer = null

function statusType(s) {
  return { PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || ''
}

async function refreshTasks() {
  try {
    const res = await listTrainingTasks()
    tasks.value = res.data || []
    if (selected.value) {
      const cur = tasks.value.find(t => t.taskId === selected.value)
      if (cur) currentTask.value = cur
    }
  } catch (e) {
    console.error(e)
  }
}

async function startDemo() {
  starting.value = true
  try {
    const res = await demoTraining()
    ElMessage.success('演示训练已启动: ' + res.data.taskId)
    await refreshTasks()
    selectTask(res.data)
    startPolling()
  } catch (e) {
    ElMessage.error('启动失败: ' + e.message)
  } finally {
    starting.value = false
  }
}

async function selectTask(t) {
  selected.value = t.taskId
  currentTask.value = t
  await loadHistory()
  startPolling()
}

async function loadHistory() {
  try {
    const res = await getTrainingHistory(selected.value)
    points.value = res.data.points || []
    currentTask.value = res.data.task || currentTask.value
    await nextTick()
    drawChart()
  } catch (e) {
    console.error(e)
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!selected.value) return
    if (currentTask.value.status === 'COMPLETED' || currentTask.value.status === 'FAILED') {
      stopPolling()
      return
    }
    await loadHistory()
  }, 2000)
}

function stopPolling() {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = null
}

async function del() {
  try {
    await ElMessageBox.confirm('确定删除?', '确认', { type: 'warning' })
    await deleteTrainingTask(selected.value)
    ElMessage.success('已删除')
    selected.value = null
    await refreshTasks()
  } catch (e) { /* 用户取消 */ }
}

function drawChart() {
  const canvas = lossChart.value
  if (!canvas || !points.value.length) return
  const ctx = canvas.getContext('2d')
  const W = canvas.width, H = canvas.height
  ctx.clearRect(0, 0, W, H)

  // 坐标轴
  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(40, 10)
  ctx.lineTo(40, H - 30)
  ctx.lineTo(W - 10, H - 30)
  ctx.stroke()

  // 计算范围
  const maxLoss = Math.max(...points.value.map(p => p.loss), 1)
  const minLoss = Math.min(...points.value.map(p => p.loss), 0)
  const n = points.value.length

  // 训练 loss
  ctx.strokeStyle = '#409EFF'
  ctx.lineWidth = 2
  ctx.beginPath()
  points.value.forEach((p, i) => {
    const x = 40 + (W - 60) * i / Math.max(n - 1, 1)
    const y = H - 30 - (H - 40) * (p.loss - minLoss) / Math.max(maxLoss - minLoss, 1)
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
  })
  ctx.stroke()

  // 验证 loss
  ctx.strokeStyle = '#67C23A'
  ctx.setLineDash([5, 5])
  ctx.beginPath()
  points.value.forEach((p, i) => {
    const x = 40 + (W - 60) * i / Math.max(n - 1, 1)
    const y = H - 30 - (H - 40) * (p.valLoss - minLoss) / Math.max(maxLoss - minLoss, 1)
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
  })
  ctx.stroke()
  ctx.setLineDash([])

  // Y 标签
  ctx.fillStyle = '#666'
  ctx.font = '10px sans-serif'
  ctx.textAlign = 'right'
  ctx.fillText(maxLoss.toFixed(2), 35, 15)
  ctx.fillText(minLoss.toFixed(2), 35, H - 30)

  // 图例
  ctx.fillStyle = '#409EFF'
  ctx.fillRect(W - 130, 10, 12, 2)
  ctx.fillStyle = '#666'
  ctx.textAlign = 'left'
  ctx.fillText('train', W - 115, 14)
  ctx.fillStyle = '#67C23A'
  ctx.fillRect(W - 75, 10, 12, 2)
  ctx.fillStyle = '#666'
  ctx.fillText('val', W - 60, 14)
}

onMounted(() => refreshTasks())
onUnmounted(() => stopPolling())
</script>

<style scoped>
.training-viz { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.task-list { max-height: 500px; overflow-y: auto; }
.task-item {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.task-item:hover { border-color: #409EFF; }
.task-item.active { border-color: #409EFF; background: #ecf5ff; }
.task-name { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.task-meta { color: #999; font-size: 12px; margin-bottom: 4px; }
.chart-area { background: #fafafa; border: 1px solid #eee; border-radius: 4px; }
</style>
