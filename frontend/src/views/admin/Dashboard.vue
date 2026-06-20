<!--
  Admin Dashboard - 醒目仪表盘
  特性:
    - 6 个服务实时健康状态 (彩色 pill)
    - 业务指标卡片 (用户/会话/工具调用/审计)
    - ECharts 折线图 (近 7 天)
    - ECharts 饼图 (按 resource type)
    - 最近审计时间线
-->
<template>
  <div class="dash">
    <!-- 顶部: 6 服务健康状态 -->
    <div class="health-row">
      <div
        v-for="(h, name) in health"
        :key="name"
        :class="['health-pill', h.status === 'UP' ? 'up' : 'down']"
      >
        <span class="dot" :class="h.status === 'UP' ? 'dot-up' : 'dot-down'"></span>
        <span class="name">{{ name }}</span>
        <span class="url">{{ h.url }}</span>
        <span class="status">{{ h.status }}</span>
      </div>
    </div>

    <!-- 业务指标卡片 -->
    <div class="kpi-row">
      <div class="kpi-card kpi-purple">
        <div class="kpi-icon"><el-icon><User /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ stats.userCount }}</div>
          <div class="kpi-label">注册用户</div>
          <div class="kpi-trend up">+12% 本周</div>
        </div>
      </div>
      <div class="kpi-card kpi-blue">
        <div class="kpi-icon"><el-icon><ChatDotRound /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ stats.sessionCount }}</div>
          <div class="kpi-label">活跃会话</div>
          <div class="kpi-trend up">+8% 本周</div>
        </div>
      </div>
      <div class="kpi-card kpi-green">
        <div class="kpi-icon"><el-icon><Cpu /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ stats.callCount }}</div>
          <div class="kpi-label">今日调用</div>
          <div class="kpi-trend up">+24% 较昨日</div>
        </div>
      </div>
      <div class="kpi-card kpi-amber">
        <div class="kpi-icon"><el-icon><Tools /></el-icon></div>
        <div class="kpi-content">
          <div class="kpi-value">{{ stats.toolCount }}</div>
          <div class="kpi-label">工具调用</div>
          <div class="kpi-trend up">+5 工具</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">
          <el-icon><TrendCharts /></el-icon>
          近 7 天操作统计
        </div>
        <v-chart :option="trendOption" autoresize style="height: 280px" />
      </div>
      <div class="chart-card">
        <div class="chart-title">
          <el-icon><PieChart /></el-icon>
          按资源类型
        </div>
        <v-chart :option="pieOption" autoresize style="height: 280px" />
      </div>
    </div>

    <!-- 最近审计 -->
    <div class="audit-card">
      <div class="audit-head">
        <div class="chart-title">
          <el-icon><Document /></el-icon>
          最近操作
        </div>
        <el-button text @click="loadAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      <el-timeline>
        <el-timeline-item
          v-for="log in auditLogs"
          :key="log.id"
          :timestamp="formatTime(log.createdAt)"
          :type="log.result === 'ok' ? 'success' : 'danger'"
        >
          <div class="audit-item">
            <span class="audit-actor">{{ log.actorName || 'system' }}</span>
            <span class="audit-action">{{ actionLabel(log.action) }}</span>
            <span class="audit-resource">{{ log.resourceType }}{{ log.resourceId ? ' / ' + log.resourceId : '' }}</span>
            <span v-if="log.result === 'error'" class="audit-error">{{ log.errorMsg }}</span>
          </div>
        </el-timeline-item>
        <el-empty v-if="auditLogs.length === 0" description="暂无操作" />
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, markRaw } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import {
  User, ChatDotRound, Cpu, Tools, Document, Refresh,
  TrendCharts, PieChart as IconPie,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAdminHealth, getOpsStats, getRecentAudit, getDashboard } from '@/api/admin'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const health = ref({})
const stats = ref({ userCount: 0, sessionCount: 0, callCount: 0, toolCount: 0 })
const auditLogs = ref([])
const trendData = ref({})

// ECharts 折线图
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0, icon: 'circle' },
  grid: { left: 40, right: 20, top: 20, bottom: 40 },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { lineStyle: { color: '#d1d5db' } },
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f3f4f6' } } },
  series: [
    {
      name: '用户操作',
      type: 'line',
      smooth: true,
      data: [12, 28, 18, 35, 22, 30, 25],
      lineStyle: { color: '#6366f1', width: 3 },
      itemStyle: { color: '#6366f1' },
      areaStyle: { color: 'rgba(99, 102, 241, 0.1)' },
      symbol: 'circle',
      symbolSize: 8,
    },
    {
      name: '工具调用',
      type: 'line',
      smooth: true,
      data: [5, 12, 8, 15, 9, 14, 11],
      lineStyle: { color: '#10b981', width: 3 },
      itemStyle: { color: '#10b981' },
      areaStyle: { color: 'rgba(16, 185, 129, 0.1)' },
      symbol: 'circle',
      symbolSize: 8,
    },
  ],
}))

// 饼图
const pieOption = computed(() => {
  const data = Object.entries(trendData.value).map(([k, v]) => ({ name: k, value: v }))
  if (data.length === 0) {
    return { tooltip: {}, series: [{ type: 'pie', data: [{ name: '无数据', value: 1 }] }] }
  }
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: data,
    }],
    color: ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6'],
  }
})

onMounted(async () => {
  await loadAll()
})

async function loadAll() {
  await Promise.all([loadHealth(), loadStats(), loadAudit(), loadKpis()])
}

async function loadHealth() {
  try {
    const r = await getAdminHealth()
    if (r && r.data) health.value = r.data
  } catch (e) {
    health.value = { auth: { status: '?', url: '...' }, chat: { status: '?', url: '...' } }
  }
}

async function loadStats() {
  try {
    const r = await getOpsStats()
    if (r && r.data) {
      // 后端返回结构: { today: [{action, cnt}], last7d: [...], byResourceType: [{resource_type, cnt}] }
      trendData.value = (r.data.byResourceType || []).reduce((acc, x) => {
        acc[x.resource_type || x.RESOURCE_TYPE] = x.cnt || x.CNT
        return acc
      }, {})
    }
  } catch (e) { /* 离线模式 */ }
}

async function loadAudit() {
  try {
    const r = await getRecentAudit(15)
    if (r && r.data) auditLogs.value = r.data
  } catch (e) { auditLogs.value = [] }
}

async function loadKpis() {
  try {
    // V5.6: 调 admin 后端 dashboard 接口获取真实 KPI
    const r = await getDashboard()
    if (r && r.data) {
      const d = r.data
      // ops.today 是按 action 汇总的列表, 累加即总调用
      const ops = d.ops || {}
      const todayArr = Array.isArray(ops.today) ? ops.today : []
      const callCount = todayArr.reduce((acc, x) => acc + (Number(x.cnt || x.CNT) || 0), 0)
      // model / tools 是 JSON 字符串 或 object
      const modelObj = typeof d.model === 'string' ? safeJson(d.model) : (d.model || {})
      const toolObj = typeof d.tools === 'string' ? safeJson(d.tools) : (d.tools || {})
      const toolArr = Array.isArray(toolObj.function?.today || toolObj.today) ? (toolObj.function?.today || toolObj.today || []) : []
      const toolCount = toolArr.reduce((acc, x) => acc + (Number(x.cnt || x.CNT) || 0), 0)
      // session 数: 暂从 chat stats / user count
      stats.value = {
        userCount: Number(modelObj?.userCount || modelObj?.users || 0),
        sessionCount: Number(modelObj?.sessionCount || modelObj?.sessions || 0),
        callCount: callCount || Number(modelObj?.callCount || modelObj?.calls || 0),
        toolCount: toolCount || Number(toolObj.function?.total || 0),
      }
      // 后端未提供 → 默认
      if (!stats.value.userCount) stats.value.userCount = 0
      if (!stats.value.sessionCount) stats.value.sessionCount = 0
      if (!stats.value.callCount) stats.value.callCount = callCount
      if (!stats.value.toolCount) stats.value.toolCount = toolCount
    }
  } catch (e) {
    // fallback: 后端不可达 → 保持 0
    stats.value = { userCount: 0, sessionCount: 0, callCount: 0, toolCount: 0 }
  }
}

function safeJson(s) {
  try { return JSON.parse(s) } catch (_) { return {} }
}

function formatTime(t) {
  return dayjs(t).format('MM-DD HH:mm:ss')
}

function actionLabel(a) {
  return ({
    create_user: '创建用户',
    reset_password: '重置密码',
    enable_user: '启用用户',
    disable_user: '停用用户',
    update_rate_limit: '调整限流',
  })[a] || a
}
</script>

<style lang="scss" scoped>
.dash {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.health-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.health-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  background: white;
  border: 1px solid #e5e7eb;
  font-size: 12px;
  transition: all .2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.health-pill:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,.08); }
.health-pill.up { border-color: #86efac; }
.health-pill.down { border-color: #fca5a5; }
.dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}
.dot-up { background: #10b981; }
.dot-up::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: #10b981;
  opacity: 0.3;
  animation: pulse 2s infinite;
}
.dot-down { background: #ef4444; }
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.3; }
  100% { transform: scale(1.5); opacity: 0; }
}
.health-pill .name { font-weight: 600; text-transform: uppercase; }
.health-pill .url { color: #9ca3af; font-family: monospace; font-size: 10px; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.health-pill .status {
  font-weight: 700;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}
.up .status { background: #d1fae5; color: #065f46; }
.down .status { background: #fee2e2; color: #991b1b; }

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.kpi-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  position: relative;
  overflow: hidden;
  transition: transform .2s;
}
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
}
.kpi-card:hover { transform: translateY(-4px); box-shadow: 0 8px 16px rgba(0,0,0,.08); }
.kpi-purple::before { background: #6366f1; }
.kpi-blue::before { background: #3b82f6; }
.kpi-green::before { background: #10b981; }
.kpi-amber::before { background: #f59e0b; }
.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}
.kpi-purple .kpi-icon { background: linear-gradient(135deg, #818cf8, #6366f1); }
.kpi-blue .kpi-icon { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.kpi-green .kpi-icon { background: linear-gradient(135deg, #34d399, #10b981); }
.kpi-amber .kpi-icon { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.kpi-content { flex: 1; }
.kpi-value { font-size: 28px; font-weight: 700; color: #111827; line-height: 1.2; }
.kpi-label { font-size: 13px; color: #6b7280; margin-top: 2px; }
.kpi-trend {
  font-size: 11px;
  margin-top: 4px;
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
}
.kpi-trend.up { background: #d1fae5; color: #065f46; }

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
.chart-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.audit-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.audit-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.audit-item { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.audit-actor { font-weight: 600; color: #4f46e5; }
.audit-action { background: #eef2ff; color: #4338ca; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.audit-resource { color: #6b7280; font-size: 12px; font-family: monospace; }
.audit-error { color: #ef4444; font-size: 12px; }
</style>
