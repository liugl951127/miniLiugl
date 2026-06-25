<!--
  API Key 配额统计页面 (Day 20)
  管理员视角: 全局 Key 数量 / 活跃数 / 总调用量
-->
<template>
  <div class="apikey-stats">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1>🔑 {{ t('apikey.adminStats') }}</h1>
        <p class="sub">{{ t('apikey.adminStatsDesc') }}</p>
      </div>
      <el-button :icon="Refresh" @click="fetchStats" :loading="loading">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-row" v-loading="loading">
      <div class="kpi-card kpi-blue">
        <div class="kpi-icon"><el-icon><Key /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ fmt(stats.totalKeys) }}</div>
          <div class="kpi-label">{{ t('apikey.totalKeys') }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-green">
        <div class="kpi-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ fmt(stats.activeKeys) }}</div>
          <div class="kpi-label">{{ t('apikey.activeKeys') }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-red">
        <div class="kpi-icon"><el-icon><CircleClose /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ fmt(stats.inactiveKeys) }}</div>
          <div class="kpi-label">{{ t('apikey.inactiveKeys') }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-purple">
        <div class="kpi-icon"><el-icon><DataLine /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ fmt(stats.totalCalls) }}</div>
          <div class="kpi-label">{{ t('apikey.totalCalls') }}</div>
        </div>
      </div>
    </div>

    <!-- 服务状态提示 -->
    <el-alert
      v-if="!stats.available"
      :title="t('apikey.authServiceDown')"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 状态分布 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">
          <el-icon><PieChart /></el-icon>
          {{ t('apikey.statusDist') }}
        </div>
        <v-chart :option="pieOption" autoresize style="height: 280px" />
      </div>
      <div class="chart-card">
        <div class="chart-title">
          <el-icon><Histogram /></el-icon>
          {{ t('apikey.usageOverview') }}
        </div>
        <div class="usage-table">
          <el-table :data="usageData" stripe>
            <el-table-column :label="t('apikey.metric')" prop="label" />
            <el-table-column :label="t('apikey.value')" prop="value" align="center">
              <template #default="{ row }">
                <span class="mono">{{ row.value }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getApiKeyStats } from '@/api/admin'

const { t } = useI18n()
const loading = ref(false)
const rawStats = ref(null)

const stats = computed(() => rawStats.value ?? {
  totalKeys: 0, totalCalls: 0, activeKeys: 0, inactiveKeys: 0, available: false
})

const fmt = (n) => n == null ? '0' : Number(n).toLocaleString()

const usageData = computed(() => [
  { label: t('apikey.totalKeys'), value: fmt(stats.value.totalKeys) },
  { label: t('apikey.activeKeys'), value: fmt(stats.value.activeKeys) },
  { label: t('apikey.inactiveKeys'), value: fmt(stats.value.inactiveKeys) },
  { label: t('apikey.totalCalls'), value: fmt(stats.value.totalCalls) },
  { label: t('apikey.avgPerKey'), value: avgPerKey.value },
])

const avgPerKey = computed(() => {
  const total = stats.value?.totalKeys || 0
  const calls = stats.value?.totalCalls || 0
  if (!total) return '0'
  return (calls / total).toFixed(1)
})

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  color: ['#67C23A', '#F56C6C'],
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    label: { show: true },
    data: [
      { value: stats.value?.activeKeys || 0, name: t('apikey.active') },
      { value: stats.value?.inactiveKeys || 0, name: t('apikey.inactive') },
    ]
  }]
}))

async function fetchStats() {
  loading.value = true
  try {
    const res = await getApiKeyStats()
    if (res.code === 0 || res.ok !== false) {
      rawStats.value = res.data ?? res
    } else {
      ElMessage.error(res.message || t('apikey.fetchFailed'))
    }
  } catch (e) {
    ElMessage.error(t('apikey.fetchFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.apikey-stats { padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
}
.page-header h1 { margin: 0; font-size: 22px; }
.sub { margin: 4px 0 0; color: #666; font-size: 14px; }
.kpi-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  margin-bottom: 20px;
}
.kpi-card {
  background: #fff; border-radius: 10px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.kpi-icon { font-size: 32px; }
.kpi-blue .kpi-icon { color: #409EFF; }
.kpi-green .kpi-icon { color: #67C23A; }
.kpi-red .kpi-icon { color: #F56C6C; }
.kpi-purple .kpi-icon { color: #9B59B6; }
.kpi-value { font-size: 26px; font-weight: 700; color: #222; line-height: 1.2; }
.kpi-label { font-size: 13px; color: #888; margin-top: 4px; }
.chart-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;
}
.chart-card {
  background: #fff; border-radius: 10px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.chart-title {
  display: flex; align-items: center; gap: 6px;
  font-weight: 600; font-size: 15px; margin-bottom: 12px; color: #333;
}
.mono { font-family: 'Courier New', monospace; font-weight: 600; }
</style>
