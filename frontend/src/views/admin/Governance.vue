<template>
  <div class="governance-container">
    <div class="gov-header">
      <h1>🛡️ 治理后台 <span class="badge">V2.9.0</span></h1>
      <p class="sub">操作审计可视化 / 异常检测 / 合规检查 / 数据保留</p>
    </div>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" v-if="overview">
      <el-col :span="4">
        <el-card class="kpi">
          <div class="kpi-label">总操作数</div>
          <div class="kpi-value">{{ overview.totalOps.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="kpi success">
          <div class="kpi-label">成功</div>
          <div class="kpi-value">{{ overview.successOps.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="kpi danger">
          <div class="kpi-label">失败</div>
          <div class="kpi-value">{{ overview.failOps.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="kpi warn">
          <div class="kpi-label">失败率</div>
          <div class="kpi-value">{{ (overview.failRate * 100).toFixed(2) }}%</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="kpi">
          <div class="kpi-label">独立用户</div>
          <div class="kpi-value">{{ overview.uniqueUsers }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="kpi" :class="complianceClass">
          <div class="kpi-label">合规评分</div>
          <div class="kpi-value">{{ compliance ? compliance.score.toFixed(1) : '-' }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 时间线 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <span>📈 操作时间线 (近 24h)</span>
        <el-button size="small" text @click="loadTimeline" style="float:right">🔄</el-button>
      </template>
      <v-chart
        v-if="timeline.length"
        :option="timelineOption"
        style="height: 300px"
        autoresize
      />
    </el-card>

    <!-- Top 操作 + 资源分布 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header><span>🔝 Top 10 操作类型</span></template>
          <el-table :data="overview?.topActions || []" size="small" stripe>
            <el-table-column prop="action" label="操作" />
            <el-table-column prop="count" label="次数" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Math.min(100, row.count / maxActionCount * 100)" :show-text="false" :stroke-width="8" />
                <span style="margin-left: 8px">{{ row.count }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>📦 资源类型分布</span></template>
          <v-chart
            v-if="resourceChartData.length"
            :option="resourceOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 异常检测 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <span>⚠️ 异常检测 (近 7 天)</span>
        <el-tag v-if="anomalies" type="warning" style="margin-left: 12px">
          越权删除: {{ anomalies.unauthorizedDeleteAttempts }}
        </el-tag>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <h4>高频失败用户 (>10 次)</h4>
          <el-table :data="anomalies?.highFailUsers || []" size="small" max-height="200">
            <el-table-column prop="userId" label="用户" width="100" />
            <el-table-column prop="failCount" label="失败次数" />
          </el-table>
        </el-col>
        <el-col :span="8">
          <h4>异常 IP (>1000 次)</h4>
          <el-table :data="anomalies?.suspiciousIps || []" size="small" max-height="200">
            <el-table-column prop="ip" label="IP" />
            <el-table-column prop="count" label="次数" width="80" />
          </el-table>
        </el-col>
        <el-col :span="8">
          <h4>突发操作 (1分钟 >50)</h4>
          <el-table :data="anomalies?.burstUsers || []" size="small" max-height="200">
            <el-table-column prop="userId" label="用户" width="100" />
            <el-table-column prop="burstCount" label="次数" />
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <!-- 合规检查 -->
    <el-card style="margin-top: 16px">
      <template #header><span>✅ 合规检查</span></template>
      <el-row :gutter="16">
        <el-col :span="4" v-for="check in compliance?.checks || []" :key="check.name">
          <el-card class="compliance-card" :class="'status-' + check.status.toLowerCase()">
            <div class="check-status">{{ checkIcon(check.status) }}</div>
            <div class="check-name">{{ check.name }}</div>
            <div class="check-detail">{{ check.detail }}</div>
            <el-tag :type="checkStatusType(check.status)" size="small">{{ check.status }}</el-tag>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 数据保留策略 -->
    <el-card style="margin-top: 16px">
      <template #header><span>💾 数据保留策略</span></template>
      <el-table :data="retention" size="small" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="table" label="表名" />
        <el-table-column prop="retentionDays" label="保留天数" width="120" />
        <el-table-column prop="archiveEnabled" label="归档" width="100">
          <template #default="{ row }">
            <el-tag :type="row.archiveEnabled ? 'success' : 'info'" size="small">
              {{ row.archiveEnabled ? '已开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCleanup" label="上次清理">
          <template #default="{ row }">
            {{ formatTime(row.lastCleanup) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent
} from 'echarts/components'
import { adminApi } from '@/api/admin'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const overview = ref(null)
const timeline = ref([])
const anomalies = ref(null)
const compliance = ref(null)
const retention = ref([])

const maxActionCount = computed(() => {
  const list = overview.value?.topActions || []
  return list.length ? list[0].count : 1
})

const resourceChartData = computed(() => {
  const dist = overview.value?.resourceDistribution || {}
  return Object.entries(dist).map(([name, value]) => ({ name, value }))
})

const complianceClass = computed(() => {
  if (!compliance.value) return ''
  if (compliance.value.score >= 90) return 'success'
  if (compliance.value.score >= 60) return 'warn'
  return 'danger'
})

const timelineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 60, right: 30, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: timeline.value.map(t => t.time.substring(11)) },
  yAxis: { type: 'value' },
  series: [
    {
      name: '总数',
      type: 'line',
      data: timeline.value.map(t => t.total),
      smooth: true,
      showSymbol: false,
      itemStyle: { color: '#6366f1' },
      areaStyle: { opacity: 0.2 }
    },
    {
      name: '失败',
      type: 'line',
      data: timeline.value.map(t => t.failed),
      smooth: true,
      showSymbol: false,
      itemStyle: { color: '#ef4444' }
    }
  ]
}))

const resourceOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: resourceChartData.value,
    label: { formatter: '{b}: {c}' }
  }]
}))

const checkIcon = (status) => {
  return { PASS: '✅', WARN: '⚠️', FAIL: '❌' }[status] || '?'
}

const checkStatusType = (status) => {
  return { PASS: 'success', WARN: 'warning', FAIL: 'danger' }[status] || 'info'
}

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

const loadAll = async () => {
  try {
    const [ov, tl, an, co, rt] = await Promise.all([
      adminApi.governance.overview(),
      adminApi.governance.timeline(),
      adminApi.governance.anomalies(),
      adminApi.governance.compliance(),
      adminApi.governance.retention()
    ])
    overview.value = ov.data
    timeline.value = tl.data || []
    anomalies.value = an.data
    compliance.value = co.data
    retention.value = rt.data || []
  } catch (e) {
    console.error('load governance failed:', e)
  }
}

const loadTimeline = async () => {
  try {
    const res = await adminApi.governance.timeline()
    timeline.value = res.data || []
  } catch (e) { console.warn(e) }
}

onMounted(loadAll)
</script>

<style scoped>
.governance-container { padding: 20px; }
.gov-header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #ef4444; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }
.kpi { text-align: center; }
.kpi-label { color: #909399; font-size: 12px; }
.kpi-value { font-size: 24px; font-weight: 600; margin-top: 4px; }
.kpi.success { border-left: 3px solid #67c23a; }
.kpi.danger { border-left: 3px solid #f56c6c; }
.kpi.warn { border-left: 3px solid #e6a23c; }

.compliance-card { text-align: center; padding: 12px 8px; }
.check-status { font-size: 32px; }
.check-name { font-size: 14px; font-weight: 500; margin: 4px 0; }
.check-detail { color: #909399; font-size: 12px; min-height: 32px; }
.compliance-card.status-pass { border-left: 3px solid #67c23a; }
.compliance-card.status-warn { border-left: 3px solid #e6a23c; }
.compliance-card.status-fail { border-left: 3px solid #f56c6c; }
h4 { margin: 0 0 8px 0; font-size: 13px; color: #606266; }
</style>
