<!--
  @file analytics/Vote.vue - 多模型投票 (V8.0)
  路由: /analytics/vote
-->
<template>
  <div v-loading="voteLoading">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6">
        <el-card body-style="padding:14px" shadow="hover">
          <div style="font-size:12px;color:var(--el-text-color-secondary)">总投票数</div>
          <div style="font-size:24px;font-weight:700;color:var(--liugl-primary)">{{ summary.totalVotes || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:14px" shadow="hover">
          <div style="font-size:12px;color:var(--el-text-color-secondary)">总问题数</div>
          <div style="font-size:24px;font-weight:700;color:var(--liugl-success)">{{ summary.totalQuestions || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:14px" shadow="hover">
          <div style="font-size:12px;color:var(--el-text-color-secondary)">平均一致率</div>
          <div style="font-size:24px;font-weight:700;color:var(--liugl-warning)">{{ summary.avgAgreement || 0 }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:14px" shadow="hover">
          <div style="font-size:12px;color:var(--el-text-color-secondary)">参与模型数</div>
          <div style="font-size:24px;font-weight:700;color:var(--liugl-danger)">{{ summary.modelCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="16">
        <el-card>
          <template #header><span>📈 一致率趋势 (近 30 天)</span></template>
          <div ref="voteTrendRef" style="width:100%;height:280px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>🎯 策略分布</span></template>
          <div ref="strategyRef" style="width:100%;height:280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>🗳️ 投票记录</span>
          <el-button size="small" @click="loadAll">刷新</el-button>
        </div>
      </template>
      <el-table :data="votes" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="text" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="strategy" label="策略" width="100" />
        <el-table-column prop="modelCount" label="模型数" width="100" />
        <el-table-column label="一致率" width="160">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.agreementRate || 0)" :color="agreementColor(row.agreementRate)" style="width:120px" :show-text="false" />
            <span style="margin-left:6px">{{ (row.agreementRate || 0).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openVoteDetail(row)">详情</el-button>
            <el-button size="small" link @click="duplicateVote(row)">复用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 投票详情弹窗 -->
    <el-dialog v-model="voteDetailVisible" title="投票详情" width="680px" destroy-on-close>
      <div v-if="voteDetail">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="问题">{{ voteDetail.text }}</el-descriptions-item>
          <el-descriptions-item label="策略">{{ voteDetail.strategy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投票总数">{{ voteDetail.votes }}</el-descriptions-item>
          <el-descriptions-item label="一致率">
            <el-progress :percentage="Math.round(voteDetail.agreementRate || 0)" :color="agreementColor(voteDetail.agreementRate)" style="width:120px;display:inline-block;vertical-align:middle" :show-text="false" />
            <span style="margin-left:8px">{{ (voteDetail.agreementRate || 0).toFixed(1) }}%</span>
          </el-descriptions-item>
          <el-descriptions-item label="投票时间" :span="2">{{ voteDetail.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="font-size:14px;font-weight:600;margin-bottom:10px">各模型答案</div>
        <el-table :data="voteDetail.modelVotes || []" stripe size="small">
          <el-table-column prop="model" label="模型" width="160" />
          <el-table-column prop="answer" label="答案" min-width="200" show-overflow-tooltip />
          <el-table-column label="置信度" width="120">
            <template #default="{ row }">
              <el-progress :percentage="Math.round((row.confidence || 0) * 100)" :color="confidenceColor(row.confidence)" style="width:90px" :show-text="false" />
              <span style="margin-left:6px;font-size:12px">{{ ((row.confidence || 0) * 100).toFixed(0) }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="voteDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getVoteStatsSummary, getVoteTrend, getVoteRecords, duplicateVote as duplicateVoteApi } from '@/api/analytics'
import * as echarts from 'echarts'

const voteLoading = ref(false)
const summary = ref({})
const votes = ref([])
const voteDetail = ref(null)
const voteDetailVisible = ref(false)

const voteTrendRef = ref(null)
const strategyRef = ref(null)
let voteChart, strategyChart

function agreementColor(rate) {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.5) return '#e6a23c'
  return '#f56c6c'
}
function confidenceColor(conf) {
  if (conf >= 0.8) return '#67c23a'
  if (conf >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

async function loadAll() {
  voteLoading.value = true
  try {
    const [s, t, v] = await Promise.all([
      getVoteStatsSummary().catch(() => ({})),
      getVoteTrend().catch(() => []),
      getVoteRecords().catch(() => [])
    ])
    summary.value = s.data?.data ?? s.data ?? s ?? {}
    votes.value = v.data?.data ?? v.data ?? v ?? []
    await nextTick()
    renderCharts(t.data?.data ?? t.data ?? t ?? [])
  } finally { voteLoading.value = false }
}

function renderCharts(trend) {
  if (voteTrendRef.value) {
    voteChart = echarts.init(voteTrendRef.value)
    voteChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trend.map(d => d.date) },
      yAxis: { type: 'value', max: 100 },
      series: [{ data: trend.map(d => d.agreement), type: 'line', smooth: true, areaStyle: {} }]
    })
  }
  if (strategyRef.value) {
    strategyChart = echarts.init(strategyRef.value)
    const strategyData = summary.value.strategies || []
    strategyChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: strategyData }]
    })
  }
}

function openVoteDetail(v) {
  voteDetail.value = v
  voteDetailVisible.value = true
}

async function duplicateVote(v) {
  try {
    await duplicateVoteApi(v.id)
    ElMessage.success('已复用')
  } catch (e) { ElMessage.error('复用失败') }
}

function resize() {
  voteChart?.resize()
  strategyChart?.resize()
}

onMounted(() => { loadAll(); window.addEventListener('resize', resize) })
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  voteChart?.dispose()
  strategyChart?.dispose()
})
</script>
