<!-- @file agent/Training.vue - Agent 训练可视化 V6.8.1 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>Agent 训练可视化</h2>
      <el-button size="small" @click="loadMetrics">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <!-- 训练任务列表 -->
    <el-row :gutter="12" style="margin-bottom:20px">
      <el-col v-for="m in metrics" :key="m.label" :span="6">
        <el-tooltip :content="m.tip || ''" placement="top" effect="light">
          <el-card shadow="hover" style="cursor:default">
            <div style="font-size:12px;color:#999">{{ m.label }}</div>
            <div style="font-size:24px;font-weight:700;color:#7c3aed;margin-top:4px">{{ m.value }}</div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <!-- 任务历史列表 -->
    <el-card title="训练任务列表" body-style="padding:0" style="margin-bottom:16px">
      <el-table :data="taskList" v-loading="loading" stripe size="small">
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'completed' ? 'success' : row.status === 'failed' ? 'danger' : 'primary'">
              {{ {running:'进行中',completed:'已完成',failed:'失败'}[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Epoch" width="80" align="center">
          <template #default="{ row }">{{ row.epochs ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="学习率" width="100">
          <template #default="{ row }">{{ row.learningRate ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="样本数" width="100" align="right">
          <template #default="{ row }">{{ row.totalSamples ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="意图数" width="100" align="right">
          <template #default="{ row }">{{ row.totalIntents ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ row.startedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="消息" min-width="120">
          <template #default="{ row }">
            <span style="color:#909399;font-size:12px">{{ row.message || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !taskList.length" style="padding:32px;text-align:center;color:#909399">
        暂无训练任务
      </div>
    </el-card>

    <!-- 训练曲线 -->
    <el-card title="意图识别准确率曲线" body-style="padding:16px">
      <div ref="chartRef" style="height:280px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { trainingApi } from '@/api/agent'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const chartRef = ref(null)
let chart = null
const loading = ref(false)
const taskList = ref([])

// V6.8.1 fix: 后端返回 List<TrainingStatus>，正确映射到 metric 卡片
const metrics = ref([
  { label: '总任务数', value: '-', tip: '历史累计训练任务数' },
  { label: '进行中', value: '-', tip: '当前正在训练的任务数' },
  { label: '已完成', value: '-', tip: '已成功完成的任务数' },
  { label: '总样本数', value: '-', tip: '累计训练样本总数' },
])

async function loadMetrics() {
  loading.value = true
  try {
    const r = await trainingApi.llmList()
    const list = r.data || []
    taskList.value = list

    const running = list.filter(t => t.status === 'running').length
    const completed = list.filter(t => t.status === 'completed').length
    const totalSamples = list.reduce((s, t) => s + (t.totalSamples || 0), 0)

    metrics.value = [
      { label: '总任务数', value: list.length, tip: '历史累计训练任务数' },
      { label: '进行中', value: running, tip: '当前正在训练的任务数' },
      { label: '已完成', value: completed, tip: '已成功完成的任务数' },
      { label: '总样本数', value: totalSamples > 999 ? (totalSamples / 1000).toFixed(1) + 'k' : totalSamples, tip: '累计训练样本总数' },
    ]

    // 渲染准确率曲线（从已完成任务的历史中提取）
    renderChart(list.filter(t => t.status === 'completed').slice(-10))
  } catch { taskList.value = [] }
  finally { loading.value = false }
}

function renderChart(completedTasks) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  if (!completedTasks.length) {
    chart.setOption({
      title: { text: '暂无训练历史', left: 'center', top: 'middle',
        textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' } },
      xAxis: { show: false }, yAxis: { show: false }, series: []
    })
    return
  }

  // V6.8.1: 数据来自 LlmTrainingService 总准确率趋势
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['准确率趋势'] },
    xAxis: { type: 'category', data: completedTasks.map((_, i) => '任务' + (i + 1)), name: '训练任务' },
    yAxis: { type: 'value', name: '样本量', min: 0 },
    series: [{
      name: '训练样本数',
      type: 'bar',
      data: completedTasks.map(t => t.totalSamples || 0),
      itemStyle: { color: '#7c3aed' },
    }]
  })
}

onMounted(loadMetrics)
onUnmounted(() => { chart?.dispose() })
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
</style>
