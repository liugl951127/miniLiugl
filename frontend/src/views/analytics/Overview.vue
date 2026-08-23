<!--
  @file analytics/Overview.vue - 总览 (V8.0)
  路由: /analytics/overview
-->
<template>
  <div v-loading="metricsLoading">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col v-for="m in metrics" :key="m.label" :span="6">
        <el-card shadow="hover" body-style="padding:14px">
          <el-tooltip :content="m.tip" placement="top">
            <div style="font-size:12px;color:var(--el-text-color-secondary);margin-bottom:4px">
              {{ m.label }} <el-icon><InfoFilled /></el-icon>
            </div>
          </el-tooltip>
          <div :style="{ fontSize: '24px', fontWeight: 700, color: m.color }">{{ m.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="12">
        <el-card>
          <template #header><span>📈 调用趋势 (近 30 天)</span></template>
          <div ref="trendChartRef" style="width:100%;height:280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>🥧 模型调用分布</span></template>
          <div ref="pieChartRef" style="width:100%;height:280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="12">
        <el-card>
          <template #header><span>✅ 成功率趋势</span></template>
          <div ref="successRateRef" style="width:100%;height:240px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>👥 活跃用户 TOP</span></template>
          <el-table :data="topUsers" stripe v-loading="overviewLoading" :empty-text="topUsersEmptyText">
            <el-table-column prop="username" label="用户" min-width="120" />
            <el-table-column prop="calls" label="调用次数" width="120" sortable />
            <el-table-column prop="lastCallAt" label="最后调用" min-width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import http from '@/api/http'
import * as echarts from 'echarts'

const metricsLoading = ref(false)
const overviewLoading = ref(false)

const metrics = ref([
  { label: '总调用量', value: '-', color: '#409eff', tip: '平台累计所有 API 调用总次数' },
  { label: '今日调用', value: '-', color: '#67c23a', tip: '今日 0 点至今的 API 调用量' },
  { label: '独立用户', value: '-', color: '#e6a23c', tip: '调用过 API 的去重用户数' },
  { label: '平均延迟', value: '-', color: '#f56c6c', tip: 'API 响应的平均延迟时间' }
])
const topUsers = ref([])
const topUsersEmptyText = computed(() => overviewLoading.value ? '加载中...' : '暂无用户调用数据')

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const successRateRef = ref(null)
let trendChart, pieChart, successChart

async function loadOverview() {
  metricsLoading.value = true
  overviewLoading.value = true
  try {
    const [overview, trend, dist, users, successRate] = await Promise.all([
      http.get('/api/v1/analytics/stats/overview').catch(() => ({})),
      http.get('/api/v1/analytics/stats/trend').catch(() => []),
      http.get('/api/v1/analytics/stats/distribution').catch(() => []),
      http.get('/api/v1/analytics/stats/top-users').catch(() => []),
      http.get('/api/v1/analytics/stats/success-rate').catch(() => [])
    ])
    const o = overview.data?.data ?? overview.data ?? overview ?? {}
    metrics.value[0].value = (o.totalCalls || 0).toLocaleString()
    metrics.value[1].value = (o.todayCalls || 0).toLocaleString()
    metrics.value[2].value = (o.totalUsers || 0).toLocaleString()
    metrics.value[3].value = o.avgLatency ? `${o.avgLatency}ms` : '—'
    topUsers.value = users.data?.data || users.data || []
    await nextTick()
    renderCharts(trend.data?.data || trend.data || [], dist.data?.data || dist.data || [], successRate.data?.data || successRate.data || [])
  } finally {
    metricsLoading.value = false
    overviewLoading.value = false
  }
}

function renderCharts(trendData, distData, successData) {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map(d => d.date) },
      yAxis: { type: 'value' },
      series: [{ data: trendData.map(d => d.calls), type: 'line', smooth: true, areaStyle: {} }]
    })
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: distData.map(d => ({ name: d.model, value: d.calls })) }]
    })
  }
  if (successRateRef.value) {
    successChart = echarts.init(successRateRef.value)
    successChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: successData.map(d => d.date) },
      yAxis: { type: 'value', max: 100 },
      series: [{ data: successData.map(d => d.rate), type: 'bar' }]
    })
  }
}

function resize() {
  trendChart?.resize()
  pieChart?.resize()
  successChart?.resize()
}

onMounted(() => {
  loadOverview()
  window.addEventListener('resize', resize)
})
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  pieChart?.dispose()
  successChart?.dispose()
})
</script>
