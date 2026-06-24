<!--
  V5.9 Day 20: API Key 用量统计页面
  功能: 全局 Key 统计 / 启用状态分布 / 调用次数 Top / 趋势图
-->
<template>
  <div class="apikey-stats-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1>📊 {{ t('apikey.statsTitle') || 'API Key 用量统计' }}</h1>
        <p class="sub">{{ t('apikey.statsSub') || '全局 API Key 使用概况' }}</p>
      </div>
      <el-button :icon="Refresh" @click="loadStats">{{ t('common.refresh') || '刷新' }}</el-button>
    </div>

    <!-- 不可用提示 -->
    <el-alert v-if="stats && stats.status !== 'ok'" type="warning" show-icon style="margin-bottom:16px">
      <template #title>
        <b>{{ t('apikey.statsUnavailable') || '统计暂不可用' }}</b>
        <span style="margin-left:8px;color:#909399;font-size:12px">{{ stats.message }}</span>
      </template>
    </el-alert>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" v-if="stats && stats.status === 'ok'" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-icon blue">🔑</div>
          <div class="kpi-body">
            <div class="kpi-value">{{ stats.totalKeys ?? 0 }}</div>
            <div class="kpi-label">{{ t('apikey.totalKeys') || '总 Key 数' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-icon green">✅</div>
          <div class="kpi-body">
            <div class="kpi-value">{{ stats.enabledKeys ?? 0 }}</div>
            <div class="kpi-label">{{ t('apikey.enabledKeys') || '启用中' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-icon orange">⛔</div>
          <div class="kpi-body">
            <div class="kpi-value">{{ stats.disabledKeys ?? 0 }}</div>
            <div class="kpi-label">{{ t('apikey.disabledKeys') || '已禁用' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-icon purple">📈</div>
          <div class="kpi-body">
            <div class="kpi-value">{{ (stats.totalCalls ?? 0).toLocaleString() }}</div>
            <div class="kpi-label">{{ t('apikey.totalCalls') || '总调用次数' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" v-if="stats && stats.status === 'ok'" style="margin-top:16px">
      <!-- 启用 vs 禁用饼图 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>{{ t('apikey.statusDist') || 'Key 状态分布' }}</span></template>
          <div ref="pieChartEl" style="height:220px"></div>
        </el-card>
      </el-col>

      <!-- 调用 Top 用户 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>{{ t('apikey.topUsers') || 'Top 用户 (调用量)' }}</span></template>
          <div class="top-users">
            <div v-if="!stats.topUsersByCalls?.length" class="empty-tip">
              {{ t('apikey.noData') || '暂无数据' }}
            </div>
            <div
              v-for="(u, i) in stats.topUsersByCalls"
              :key="u.userId"
              class="top-user-item"
            >
              <span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
              <span class="uid">UID {{ u.userId }}</span>
              <span class="calls">{{ (u.totalCalls ?? 0).toLocaleString() }} 次</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 平均调用 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>{{ t('apikey.quotaOverview') || '配额概览' }}</span></template>
          <div class="quota-list">
            <div class="quota-item">
              <span class="ql-label">{{ t('apikey.uniqueUsers') || '独立用户' }}</span>
              <span class="ql-value">{{ stats.uniqueUsers ?? 0 }}</span>
            </div>
            <div class="quota-item">
              <span class="ql-label">{{ t('apikey.avgCalls') || '平均调用/Key' }}</span>
              <span class="ql-value">{{ ((stats.avgCallsPerKey ?? 0)).toFixed(1) }}</span>
            </div>
            <div class="quota-item">
              <span class="ql-label">{{ t('apikey.enabledRate') || '启用率' }}</span>
              <span class="ql-value">
                {{ stats.totalKeys > 0 ? ((stats.enabledKeys / stats.totalKeys) * 100).toFixed(1) : 0 }}%
              </span>
            </div>
            <div class="quota-item">
              <span class="ql-label">{{ t('apikey.generatedAt') || '统计时间' }}</span>
              <span class="ql-value ts">{{ stats.generatedAt ?? '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { apiKeyApi } from '@/api/apikey'

const t = (k, def) => k  // i18n placeholder

const stats = ref(null)
const pieChartEl = ref(null)
let pieChart = null

const loadStats = async () => {
  try {
    const res = await apiKeyApi.adminSummary()
    stats.value = res.data?.data ?? res.data ?? res
    await nextTick()
    renderPie()
  } catch (e) {
    ElMessage.error((t('apikey.loadError') || '加载统计失败') + ': ' + (e.message || ''))
  }
}

const renderPie = () => {
  if (!pieChartEl.value || !stats.value) return
  if (typeof window.echarts === 'undefined') return

  if (!pieChart) {
    pieChart = window.echarts.init(pieChartEl.value)
  }
  const s = stats.value
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: s.enabledKeys ?? 0, name: '启用', itemStyle: { color: '#67C23A' } },
        { value: s.disabledKeys ?? 0, name: '禁用', itemStyle: { color: '#F56C6C' } }
      ],
      label: { formatter: '{b}: {d}%' }
    }]
  })
}

onMounted(async () => {
  await loadStats()
  window.addEventListener('resize', () => pieChart?.resize())
})
</script>

<style scoped>
.apikey-stats-page { padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.page-header h1 { margin: 0; font-size: 22px; }
.sub { margin: 4px 0 0; color: #909399; font-size: 13px; }
.kpi-row { margin-bottom: 4px; }
.kpi-card { display: flex; align-items: center; gap: 14px; padding: 4px 0; }
.kpi-icon { font-size: 32px; line-height: 1; }
.kpi-icon.blue { color: #409EFF; }
.kpi-icon.green { color: #67C23A; }
.kpi-icon.orange { color: #E6A23C; }
.kpi-icon.purple { color: #9C27B0; }
.kpi-body { flex: 1; }
.kpi-value { font-size: 26px; font-weight: 700; color: #303133; line-height: 1.2; }
.kpi-label { font-size: 12px; color: #909399; margin-top: 4px; }
.top-users { display: flex; flex-direction: column; gap: 10px; }
.top-user-item { display: flex; align-items: center; gap: 10px; }
.rank-badge {
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: #fff; flex-shrink: 0;
}
.rank-1 { background: #FFD700; color: #333; }
.rank-2 { background: #C0C0C0; color: #333; }
.rank-3 { background: #CD7F32; color: #fff; }
.rank-4, .rank-5 { background: #909399; }
.uid { flex: 1; font-size: 13px; color: #606266; font-family: monospace; }
.calls { font-size: 13px; font-weight: 600; color: #409EFF; }
.quota-list { display: flex; flex-direction: column; gap: 14px; }
.quota-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.ql-label { font-size: 13px; color: #909399; }
.ql-value { font-size: 15px; font-weight: 600; color: #303133; }
.ql-value.ts { font-size: 11px; color: #C0C4CC; }
.empty-tip { text-align: center; color: #C0C4CC; padding: 40px 0; }
</style>
