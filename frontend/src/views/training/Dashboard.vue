<!--
  @file training/Dashboard.vue - 训练总览 Dashboard (V7.1)
  V7.0 Flow① 训练总览: 对标 Analytics/Index.vue 的专业仪表盘
-->
<template>
  <div class="page-card" v-loading="loading && firstLoad">
    <div class="page-header">
      <h2>📊 训练总览</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="load" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="$router.push('/training')">
          <el-icon><Monitor /></el-icon>训练控制台
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px" v-loading="loading && firstLoad">
      <el-col :span="6">
        <el-tooltip content="累计创建的训练任务数" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:16px;text-align:center;cursor:help">
            <div style="font-size:32px;font-weight:700;color: var(--el-color-primary)">{{ data.totalTasks }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">总任务数</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="成功完成训练的任务" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:16px;text-align:center;cursor:help">
            <div style="font-size:32px;font-weight:700;color: var(--el-color-success)">{{ data.completed }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">已完成</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="当前正在训练的任务" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:16px;text-align:center;cursor:help">
            <div style="font-size:32px;font-weight:700;color: var(--el-color-warning)">{{ data.running }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">运行中</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip content="累计训练总时长" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:16px;text-align:center;cursor:help">
            <div style="font-size:32px;font-weight:700;color: var(--el-color-primary)">{{ data.totalTrainingHours || 0 }}h</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">累计训练时长</div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-bottom:16px" v-loading="loading && firstLoad">
      <el-col :span="4">
        <el-tooltip content="训练失败的任务数" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:12px;text-align:center;cursor:help">
            <div style="font-size:24px;font-weight:700;color: var(--el-color-danger)">{{ data.failed }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">失败</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" body-style="padding:12px;text-align:center">
          <div style="font-size:24px;font-weight:700;color: var(--el-text-color-secondary)">{{ data.pending }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">等待中</div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <!-- 任务状态分布 -->
        <el-card shadow="hover" body-style="padding:12px">
          <template #header><span style="font-size:13px">📈 任务状态分布</span></template>
          <div ref="statusPieRef" style="height:100px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" v-loading="loading && firstLoad">
      <!-- Tab1: 最近训练任务 -->
      <el-tab-pane name="recent">
        <template #label>
          <el-icon><Clock /></el-icon> 最近任务
        </template>
        <el-row :gutter="12">
          <el-col :span="14">
            <el-card shadow="hover" body-style="padding:0">
              <template #header>
                <span>🏃 最近训练任务</span>
                <el-button size="small" text type="primary" style="float:right" @click="$router.push('/training')">
                  全部任务 →
                </el-button>
              </template>
              <el-table :data="data.recentTasks || []" size="small" stripe
                v-loading="loading && firstLoad" empty-text="暂无训练任务">
                <el-table-column label="任务" min-width="140">
                  <template #default="{ row }">
                    <div style="font-size:13px;font-weight:600;color: var(--el-text-color-primary)">{{ row.name }}</div>
                    <div style="font-size:11px;color: var(--el-text-color-secondary)">{{ row.taskId }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="模型" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ row.model || '-' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="statusType(row.status)" :disable-transitions="true">
                      {{ statusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="最终 Loss" width="90" align="center">
                  <template #default="{ row }">
                    <span v-if="row.finalLoss != null" style="font-size:13px;font-weight:600;color: var(--el-color-success)">
                      {{ row.finalLoss.toFixed(4) }}
                    </span>
                    <span v-else style="color: var(--el-text-color-placeholder)">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="耗时" width="80" align="center">
                  <template #default="{ row }">
                    <span style="font-size:12px;color: var(--el-text-color-regular)">{{ row.durationStr || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row }">
                    <el-button size="small" text type="primary" @click="openTask(row)">
                      {{ row.status === 'COMPLETED' ? '启用模型' : '详情' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- Loss 曲线对比 -->
          <el-col :span="10">
            <el-card shadow="hover" body-style="padding:12px">
              <template #header>
                <span>📉 Loss 曲线对比</span>
                <el-select v-if="lossOptions.length" v-model="selectedTaskId" size="small" clearable
                  placeholder="选择任务查看曲线" style="float:right;width:140px" @change="onTaskSelect">
                  <el-option v-for="o in lossOptions" :key="o.taskId" :label="o.name" :value="o.taskId" />
                </el-select>
              </template>
              <div v-if="!selectedTaskId" style="text-align:center;color: var(--el-text-color-secondary);padding:40px 0">
                <el-empty description="请选择任务查看 Loss 曲线" :image-size="80" />
              </div>
              <div v-else ref="lossChartRef" v-loading="lossLoading" style="height:260px"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- Tab2: 模型分布 -->
      <el-tab-pane name="models">
        <template #label>
          <el-icon><Box /></el-icon> 模型分布
        </template>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-card shadow="hover" body-style="padding:16px">
              <template #header><span>🤖 按 Base Model 统计</span></template>
              <div v-if="!modelPieEmpty" ref="modelPieRef" style="height:300px"></div>
              <el-empty v-else description="暂无模型数据" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" body-style="padding:16px">
              <template #header><span>🏆 训练成效排行 (按 Final Loss)</span></template>
              <el-table :data="lossRanking" size="small" stripe
                v-loading="loading && firstLoad" empty-text="暂无已完成任务">
                <el-table-column type="index" label="排名" width="60" align="center" />
                <el-table-column label="任务" min-width="120">
                  <template #default="{ row }">
                    <div style="font-size:13px">{{ row.name }}</div>
                    <div style="font-size:11px;color: var(--el-text-color-secondary)">{{ row.taskId }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="模型" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ row.model }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="Final Loss" width="110" align="center">
                  <template #default="{ row }">
                    <span style="font-weight:700;color: var(--el-color-success)">{{ row.finalLoss?.toFixed(4) || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="训练时长" width="90" align="center">
                  <template #default="{ row }">
                    {{ row.durationStr || '-' }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- Tab3: 资源使用 -->
      <el-tab-pane name="resource">
        <template #label>
          <el-icon><Cpu /></el-icon> 资源使用
        </template>
        <el-alert type="info" show-icon :closable="false" style="margin-bottom:12px">
          <template #title>
            💡 沙箱模式说明：训练在内存中模拟执行，无真实 GPU 资源。
            生产环境请配置 NVIDIA GPU + nvidia-smi 获取真实资源数据。
          </template>
        </el-alert>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-card shadow="hover" body-style="padding:16px;text-align:center">
              <div style="font-size:36px;margin-bottom:8px">🟢</div>
              <div style="font-size:22px;font-weight:700;color: var(--el-color-success)">{{ data.running || 0 }}</div>
              <div style="font-size:13px;color: var(--el-text-color-secondary)">当前运行任务</div>
              <el-divider style="margin:12px 0" />
              <div style="font-size:13px;color: var(--el-text-color-regular)">
                沙箱模式下任务在 JVM 内存中运行<br>共享宿主 CPU 资源
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" body-style="padding:16px;text-align:center">
              <div style="font-size:36px;margin-bottom:8px">💾</div>
              <div style="font-size:22px;font-weight:700;color: var(--el-color-primary)">MiniTransformer</div>
              <div style="font-size:13px;color: var(--el-text-color-secondary)">当前模型架构</div>
              <el-divider style="margin:12px 0" />
              <div style="font-size:13px;color: var(--el-text-color-regular)">
                隐藏维度 128 / 4头 / 2层 / 128最大序列<br>
                轻量级，适合快速迭代验证
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" body-style="padding:16px;text-align:center">
              <div style="font-size:36px;margin-bottom:8px">⏱️</div>
              <div style="font-size:22px;font-weight:700;color: var(--el-color-primary)">{{ data.totalTrainingHours || 0 }}h</div>
              <div style="font-size:13px;color: var(--el-text-color-secondary)">累计训练时长</div>
              <el-divider style="margin:12px 0" />
              <div style="font-size:13px;color: var(--el-text-color-regular)">
                共 {{ data.totalTasks }} 个任务<br>
                {{ data.completed }} 个成功完成
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 启用模型确认弹窗 -->
    <el-dialog v-model="showEnableDialog" title="🚀 启用训练模型" width="420px">
      <div v-if="selectedTask">
        <p>任务: <strong>{{ selectedTask.name }}</strong></p>
        <p>模型: <el-tag type="info">{{ selectedTask.model }}</el-tag></p>
        <p>Final Loss: <strong style="color: var(--el-color-success)">{{ selectedTask.finalLoss?.toFixed(4) }}</strong></p>
        <p style="color: var(--el-text-color-secondary);font-size:13px">模型将注册到 Model 服务，Agent 可调用。</p>
      </div>
      <template #footer>
        <el-button @click="showEnableDialog = false">取消</el-button>
        <el-button type="primary" :loading="enabling" @click="confirmEnable">确认启用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { trainingApi } from '@/api/training'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'

const router = useRouter()

// ——— 数据 ———
const loading = ref(false)
const firstLoad = ref(true)
const activeTab = ref('recent')
const data = ref({
  totalTasks: 0, completed: 0, running: 0,
  failed: 0, pending: 0, totalTrainingHours: '0',
  totalTrainingMs: 0, recentTasks: [], byModel: {}, updatedAt: ''
})

// ——— 启用弹窗 ———
const showEnableDialog = ref(false)
const selectedTask = ref(null)
const enabling = ref(false)

// ——— Loss 曲线 ———
const lossChartRef = ref(null)
const lossChart = ref(null)
const selectedTaskId = ref(null)
const lossOptions = ref([])
const lossLoading = ref(false)

const lossRanking = computed(() => {
  return (data.value.recentTasks || [])
    .filter(t => t.finalLoss != null)
    .sort((a, b) => (a.finalLoss || 999) - (b.finalLoss || 999))
    .slice(0, 10)
})

const modelPieEmpty = computed(() => {
  const byModel = data.value.byModel || {}
  return Object.keys(byModel).length === 0
})

// ——— 状态分布饼图 ———
const statusPieRef = ref(null)
const statusPieChart = ref(null)

// ——— 模型分布饼图 ———
const modelPieRef = ref(null)
const modelPieChart = ref(null)

// ——— 加载数据 ———
async function load() {
  loading.value = true
  try {
    const r = await trainingApi.dashboardOverview()
    data.value = r.data || {}
    lossOptions.value = (r.data?.recentTasks || []).map(t => ({
      taskId: t.taskId, name: t.name || t.taskId
    }))
    await nextTick()
    renderStatusPie()
    renderModelPie()
    if (selectedTaskId.value) await loadLossCurve(selectedTaskId.value)
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || ''))
  } finally {
    loading.value = false
    firstLoad.value = false
  }
}

// ——— 状态标签 ———
function statusLabel(s) {
  return { PENDING: '等待', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败' }[s] || s
}
function statusType(s) {
  return { PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info'
}

// ——— Loss 曲线 ———
async function loadLossCurve(taskId) {
  if (!lossChartRef.value || !taskId) return
  lossLoading.value = true
  try {
    const r = await trainingApi.getHistory(taskId)
    const history = r.data || []
    if (history.length === 0) {
      if (lossChart.value) {
        lossChart.value.setOption({ title: { text: '暂无曲线数据', left: 'center', top: 'center', textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 'normal' } }, series: [] })
      }
      return
    }
    const steps = history.map(p => p.step || 0)
    const losses = history.map(p => p.loss ?? null)
    const accuracies = history.map(p => p.accuracy ?? null)

    if (!lossChart.value) {
      lossChart.value = echarts.init(lossChartRef.value)
    }
    lossChart.value.setOption({
      tooltip: { trigger: 'axis', formatter: p => {
        const [loss, acc] = p
        return `<b>Step ${loss.axisValue}</b><br/>Loss: <b>${loss.data?.toFixed(4) || '-'}</b><br/>Acc: <b>${acc?.data != null ? (acc.data * 100).toFixed(2) + '%' : '-'}</b>`
      }},
      legend: { data: ['Loss', 'Accuracy'], top: 0, textStyle: { fontSize: 11 } },
      grid: { top: 32, right: 16, bottom: 24, left: 48 },
      xAxis: { type: 'category', data: steps, name: 'Step', nameLocation: 'end', nameTextStyle: { fontSize: 10 } },
      yAxis: [
        { type: 'value', name: 'Loss', nameTextStyle: { fontSize: 10 }, splitLine: { lineStyle: { type: 'dashed', opacity: 0.3 } } },
        { type: 'value', name: 'Acc', nameTextStyle: { fontSize: 10 }, min: 0, max: 1, splitLine: { show: false } }
      ],
      series: [
        { name: 'Loss', type: 'line', data: losses, smooth: 0.3, color: '#409eff', yAxisIndex: 0 },
        { name: 'Accuracy', type: 'line', data: accuracies, smooth: 0.3, color: '#67c23a', yAxisIndex: 1 }
      ]
    })
  } catch (e) {
    ElMessage.warning('加载曲线失败: ' + (e.message || ''))
  } finally {
    lossLoading.value = false
  }
}

async function onTaskSelect(taskId) {
  selectedTaskId.value = taskId
  if (taskId) {
    await loadLossCurve(taskId)
  } else {
    lossChart.value?.clear()
  }
}

// ——— 状态分布饼图 ———
function renderStatusPie() {
  if (!statusPieRef.value) return
  if (!statusPieChart.value) statusPieChart.value = echarts.init(statusPieRef.value)
  const d = data.value
  const pieData = [
    { name: '已完成', value: d.completed || 0, itemStyle: { color: '#67c23a' } },
    { name: '运行中', value: d.running || 0, itemStyle: { color: '#e6a23c' } },
    { name: '失败', value: d.failed || 0, itemStyle: { color: '#f56c6c' } },
    { name: '等待', value: d.pending || 0, itemStyle: { color: '#909399' } }
  ].filter(x => x.value > 0)
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { show: false },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
      label: { show: pieData.length > 0, formatter: '{b}\n{c}', fontSize: 11 },
      data: pieData.length > 0 ? pieData : [{ name: '无数据', value: 1, itemStyle: { color: '#f0f0f0' } }]
    }]
  }
  statusPieChart.value.setOption(option)
}

// ——— 模型分布饼图 ———
function renderModelPie() {
  if (!modelPieRef.value) return
  const byModel = data.value.byModel || {}
  const entries = Object.entries(byModel)
  if (entries.length === 0) return

  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9254de', '#00b894', '#fd79a8', '#74b9ff']
  if (!modelPieChart.value) modelPieChart.value = echarts.init(modelPieRef.value)

  modelPieChart.value.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 个任务 ({d}%)' },
    legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['30%', '65%'], center: ['35%', '50%'],
      label: { show: true, formatter: '{c}', fontSize: 11 },
      data: entries.map(([name, value], i) => ({
        name, value,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}

// ——— 打开任务 ———
async function openTask(task) {
  selectedTask.value = task
  if (task.status === 'COMPLETED') {
    showEnableDialog.value = true
  } else {
    ElMessage.info(`任务状态: ${statusLabel(task.status)}`)
    router.push('/training')
  }
}

async function confirmEnable() {
  if (!selectedTask.value) return
  enabling.value = true
  try {
    const r = await trainingApi.enableModel(selectedTask.value.taskId)
    const modelCode = r.data?.modelCode
    // P2-3: 启用按钮反馈
    ElMessage.success('模型已启用: ' + (selectedTask.value.name || modelCode || '模型'))
    showEnableDialog.value = false
    router.push('/chat?model=' + encodeURIComponent(modelCode || ''))
  } catch (e) {
    ElMessage.error('启用失败: ' + (e.message || ''))
  } finally {
    enabling.value = false
  }
}

// ——— Resize 响应 ———
function handleResize() {
  if (statusPieChart.value) statusPieChart.value.resize()
  if (modelPieChart.value) modelPieChart.value.resize()
  if (lossChart.value) lossChart.value.resize()
}

onMounted(async () => {
  await load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (statusPieChart.value) statusPieChart.value.dispose()
  if (modelPieChart.value) modelPieChart.value.dispose()
  if (lossChart.value) lossChart.value.dispose()
})
</script>

<style scoped>
.page-card { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; }
</style>
