<!--
  @file views/analytics/VoteStats.vue (Day 37)
  @description 投票对话一致率分析图表
-->
<template>
  <div class="page-vote-stats">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#5b8def"><ChatDotRound /></el-icon>
            <div>
              <div class="stat-num">{{ stats.totalVotes }}</div>
              <div class="stat-label">总投票次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#67c23a"><TrendCharts /></el-icon>
            <div>
              <div class="stat-num">{{ (stats.avgAgreement * 100).toFixed(1) }}%</div>
              <div class="stat-label">平均一致率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#e6a23c"><Cpu /></el-icon>
            <div>
              <div class="stat-num">{{ stats.avgModelCount }}</div>
              <div class="stat-label">平均模型数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#f56c6c"><Timer /></el-icon>
            <div>
              <div class="stat-num">{{ stats.avgLatencyMs }}ms</div>
              <div class="stat-label">平均延迟</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="charts-row">
      <!-- 一致率趋势折线图 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📈 一致率趋势（近14天）</span>
              <el-select v-model="selectedStrategy" size="small" style="width:160px" @change="loadChartData">
                <el-option value="all" label="全部策略" />
                <el-option value="majority" label="多数投票" />
                <el-option value="weighted" label="加权投票" />
                <el-option value="unanimous" label="全票通过" />
              </el-select>
            </div>
          </template>
          <div v-loading="loading.chart" class="chart-wrapper">
            <div ref="agreementChartRef" class="echarts-container" />
            <el-empty v-if="!loading.chart && chartData.length === 0" description="暂无投票数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 策略分布饼图 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>🥧 投票策略分布</span>
            </div>
          </template>
          <div v-loading="loading.chart" class="chart-wrapper">
            <div ref="strategyChartRef" class="echarts-container" />
            <el-empty v-if="!loading.chart && strategyData.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <!-- 模型参与次数柱状图 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>🤖 各模型参与次数 TOP 10</span>
            </div>
          </template>
          <div v-loading="loading.chart" class="chart-wrapper">
            <div ref="modelChartRef" class="echarts-container" />
            <el-empty v-if="!loading.chart && modelData.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 延迟分布柱状图 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>⏱ 响应延迟分布</span>
              <el-select v-model="latencyBucket" size="small" style="width:140px" @change="loadChartData">
                <el-option value="day" label="按天" />
                <el-option value="hour" label="按小时" />
              </el-select>
            </div>
          </template>
          <div v-loading="loading.chart" class="chart-wrapper">
            <div ref="latencyChartRef" class="echarts-container" />
            <el-empty v-if="!loading.chart && latencyData.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 投票记录表格 -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span>📋 最近投票记录</span>
          <el-button size="small" @click="loadRecords">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="records" v-loading="loading.table" stripe>
        <el-table-column label="时间" prop="createdAt" width="170" />
        <el-table-column label="会话" prop="sessionId" min-width="160" show-overflow-tooltip />
        <el-table-column label="问题摘要" prop="question" min-width="240" show-overflow-tooltip />
        <el-table-column label="策略" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.strategy }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="一致率" width="100">
          <template #default="{ row }">
            <span :style="{ color: agreementColor(row.agreementScore) }">
              {{ (row.agreementScore * 100).toFixed(0) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="模型数" prop="modelCount" width="90" align="center" />
        <el-table-column label="延迟" width="100" align="center">
          <template #default="{ row }">
            {{ row.latencyMs }}ms
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadRecords"
        />
      </div>
    </el-card>

    <!-- 投票详情对话框 -->
    <el-dialog v-model="detailVisible" title="投票详情" width="700px" top="5vh">
      <div v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="会话 ID" :span="2">{{ currentRecord.sessionId }}</el-descriptions-item>
          <el-descriptions-item label="问题">{{ currentRecord.question }}</el-descriptions-item>
          <el-descriptions-item label="策略">{{ currentRecord.strategy }}</el-descriptions-item>
          <el-descriptions-item label="一致率">
            <span :style="{ color: agreementColor(currentRecord.agreementScore) }">
              {{ (currentRecord.agreementScore * 100).toFixed(1) }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="参与模型数">{{ currentRecord.modelCount }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentRecord.latencyMs }}ms</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 16px; margin-bottom: 8px;">各模型答案：</h4>
        <div v-for="(ans, i) in currentRecord.modelAnswers" :key="i" class="model-answer-card">
          <div class="model-answer-header">
            <el-tag size="small" :type="i === 0 ? 'primary' : 'info'">
              {{ ans.model || 'Model ' + (i+1) }}
            </el-tag>
            <span class="model-provider">{{ ans.provider }}</span>
            <span class="model-latency">{{ ans.latencyMs }}ms</span>
            <span v-if="ans.error" class="model-error">❌ {{ ans.error }}</span>
          </div>
          <div v-if="ans.answer" class="model-answer-body">{{ ans.answer }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * @file views/analytics/VoteStats.vue (Day 37)
 * 投票一致率分析图表
 * 后端数据来源: /api/v1/ai/voting/stats (GET)
 * Mock 数据展示图表，实际数据由后端提供
 */
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ChatDotRound, TrendCharts, Cpu, Timer, Refresh } from '@element-plus/icons-vue'

// === 状态 ===
const loading = reactive({ chart: false, table: false })
const stats = ref({ totalVotes: 0, avgAgreement: 0, avgModelCount: 0, avgLatencyMs: 0 })
const chartData = ref([])
const strategyData = ref([])
const modelData = ref([])
const latencyData = ref([])
const records = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedStrategy = ref('all')
const latencyBucket = ref('day')
const detailVisible = ref(false)
const currentRecord = ref(null)

let agreementChart = null
let strategyChart = null
let modelChart = null
let latencyChart = null
const agreementChartRef = ref(null)
const strategyChartRef = ref(null)
const modelChartRef = ref(null)
const latencyChartRef = ref(null)

// === 辅助 ===
function agreementColor(score) {
  if (score >= 0.8) return '#67c23a'
  if (score >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

function viewDetail(row) {
  currentRecord.value = row
  detailVisible.value = true
}

// === Mock 数据（Day 37：后端统计 API 接入后替换这部分） ===
function generateMockData() {
  const now = Date.now()
  const days = []
  const strategies = ['majority', 'weighted', 'unanimous']
  const models = ['gpt-4o', 'gpt-4o-mini', 'claude-3.5', 'deepseek-chat', 'qwen-max', 'minimax-text-01', 'llama3.1-70b', 'mixtral-8x7b']

  for (let i = 13; i >= 0; i--) {
    const d = new Date(now - i * 86400000)
    const dateStr = `${d.getMonth()+1}/${d.getDate()}`
    days.push({
      date: dateStr,
      score: +(Math.random() * 0.4 + 0.5).toFixed(3),
      strategy: strategies[Math.floor(Math.random() * strategies.length)],
      modelCount: Math.floor(Math.random() * 3) + 2,
      latency: Math.floor(Math.random() * 2000) + 500
    })
  }

  chartData.value = days
  strategyData.value = [
    { name: '多数投票', value: Math.floor(Math.random() * 50) + 20 },
    { name: '加权投票', value: Math.floor(Math.random() * 40) + 15 },
    { name: '全票通过', value: Math.floor(Math.random() * 30) + 10 }
  ]
  modelData.value = models.slice(0, 8).map(m => ({
    model: m,
    count: Math.floor(Math.random() * 60) + 10
  }))
  const buckets = ['<500ms', '500-1000ms', '1-2s', '2-5s', '>5s']
  latencyData.value = buckets.map(b => ({
    bucket: b,
    count: Math.floor(Math.random() * 40) + 5
  }))

  stats.value = {
    totalVotes: Math.floor(Math.random() * 500) + 100,
    avgAgreement: +(Math.random() * 0.3 + 0.6).toFixed(3),
    avgModelCount: +(Math.random() + 2).toFixed(1),
    avgLatencyMs: Math.floor(Math.random() * 1000) + 800
  }

  // 生成表格记录
  total.value = 50
  records.value = Array.from({ length: 10 }, (_, idx) => ({
    createdAt: new Date(now - idx * 3600000).toLocaleString(),
    sessionId: `sess-${Math.random().toString(36).slice(2, 10)}`,
    question: ['请介绍一下大模型的技术原理', '什么是 RAG，它有什么优势', '如何优化向量检索的召回率', '多模型投票的原理是什么'][idx % 4],
    strategy: strategies[idx % 3],
    agreementScore: +(Math.random() * 0.4 + 0.5).toFixed(3),
    modelCount: Math.floor(Math.random() * 3) + 2,
    latencyMs: Math.floor(Math.random() * 2000) + 500,
    modelAnswers: Array.from({ length: 3 }, (_, mi) => ({
      model: models[mi],
      provider: ['OpenAI', 'Anthropic', 'DeepSeek'][mi % 3],
      latencyMs: Math.floor(Math.random() * 1500) + 200,
      answer: mi === 0 ? '（主模型答案）' : '（备选答案）'
    }))
  }))
}

// === ECharts 图表 ===
function initCharts() {
  nextTick(() => {
    if (agreementChartRef.value) {
      agreementChart = echarts.init(agreementChartRef.value)
    }
    if (strategyChartRef.value) {
      strategyChart = echarts.init(strategyChartRef.value)
    }
    if (modelChartRef.value) {
      modelChart = echarts.init(modelChartRef.value)
    }
    if (latencyChartRef.value) {
      latencyChart = echarts.init(latencyChartRef.value)
    }
  })
}

function renderAgreementChart() {
  if (!agreementChart || !chartData.value.length) return
  agreementChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 30, top: 20 },
    xAxis: {
      type: 'category',
      data: chartData.value.map(d => d.date),
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 1,
      axisLabel: { formatter: v => (v * 100).toFixed(0) + '%', fontSize: 11 }
    },
    series: [{
      name: '一致率',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.25, color: new echarts.graphic.LinearGradient(0,0,0,1,[
        { offset: 0, color: 'rgba(91,143,222,0.5)' },
        { offset: 1, color: 'rgba(91,143,222,0.05)' }
      ])},
      lineStyle: { width: 2, color: '#5b8def' },
      itemStyle: { color: '#5b8def' },
      data: chartData.value.map(d => d.score)
    }]
  })
}

function renderStrategyChart() {
  if (!strategyChart || !strategyData.value.length) return
  strategyChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { fontSize: 11 },
      data: strategyData.value.map((d, i) => ({
        ...d,
        itemStyle: { color: ['#5b8def','#67c23a','#e6a23c'][i] }
      }))
    }]
  })
}

function renderModelChart() {
  if (!modelChart || !modelData.value.length) return
  const sorted = [...modelData.value].sort((a,b) => b.count - a.count)
  modelChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 30, bottom: 30, top: 10 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: sorted.map(d => d.model),
      axisLabel: { fontSize: 11, width: 90, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: sorted.map(d => d.count),
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,1,0,[
        { offset: 0, color: '#5b8def' },
        { offset: 1, color: '#7db3ff' }
      ])}
    }]
  })
}

function renderLatencyChart() {
  if (!latencyChart || !latencyData.value.length) return
  latencyChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, bottom: 30, top: 20 },
    xAxis: { type: 'category', data: latencyData.value.map(d => d.bucket), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar',
      data: latencyData.value.map(d => d.count),
      itemStyle: { color: '#67c23a' },
      barWidth: '50%'
    }]
  })
}

function renderAllCharts() {
  renderAgreementChart()
  renderStrategyChart()
  renderModelChart()
  renderLatencyChart()
}

// === 数据加载 ===
async function loadChartData() {
  loading.chart = true
  try {
    await new Promise(r => setTimeout(r, 300)) // simulate API
    generateMockData()
    await nextTick()
    renderAllCharts()
  } finally {
    loading.chart = false
  }
}

async function loadRecords() {
  loading.table = true
  try {
    await new Promise(r => setTimeout(r, 200))
    generateMockData()
  } finally {
    loading.table = false
  }
}

// === 生命周期 ===
onMounted(async () => {
  initCharts()
  await loadChartData()
  await loadRecords()
})

onUnmounted(() => {
  agreementChart?.dispose()
  strategyChart?.dispose()
  modelChart?.dispose()
  latencyChart?.dispose()
})
</script>

<style scoped>
.page-vote-stats { padding: 0; }
.stats-row { margin-bottom: 16px; }
.charts-row { margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-num { font-size: 24px; font-weight: 600; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.chart-wrapper { min-height: 280px; display: flex; align-items: center; justify-content: center; }
.echarts-container { width: 100%; height: 280px; }
.table-card { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.model-answer-card { background: #f5f7fa; border-radius: 6px; padding: 10px; margin-bottom: 8px; }
.model-answer-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.model-provider { font-size: 12px; color: #909399; }
.model-latency { font-size: 12px; color: #909399; margin-left: auto; }
.model-error { font-size: 12px; color: #f56c6c; }
.model-answer-body { font-size: 13px; color: #303133; line-height: 1.6; white-space: pre-wrap; }
</style>
