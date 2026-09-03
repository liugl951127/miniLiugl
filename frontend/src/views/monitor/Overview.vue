<!--
  @file monitor/Overview.vue - 监控概览 (V7.10, Day 60)
  路由: /monitor/overview
  合并: JVM + 统计 + SLA + 趋势 (原 4 个 tab)
  Day 60: 新增「上次刷新时间」自动更新显示 + 手动刷新按钮
-->
<template>
  <div class="overview-page">
    <!-- JVM 实时 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span class="card-title">🖥️ JVM 实时指标</span>
          <div style="display:flex;align-items:center;gap:8px;font-size:12px;color:var(--el-text-color-secondary)">
            <span v-if="lastRefreshed">刷新于 {{ lastRefreshed }}</span>
            <el-button size="small" link type="primary" :icon="Refresh" @click="refreshAll">刷新</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="3" border v-loading="loadingJvm">
        <el-descriptions-item label="堆内存使用">{{ jvm.heapUsed || '-' }} / {{ jvm.heapMax || '-' }}</el-descriptions-item>
        <el-descriptions-item label="非堆内存">{{ jvm.nonHeapUsed || '-' }}</el-descriptions-item>
        <el-descriptions-item label="GC 次数">{{ jvm.gcCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="线程数">{{ jvm.threads ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Uptime">{{ jvm.uptime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CPU 使用率">{{ jvm.cpuUsage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 统计概览 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <span class="card-title">📊 统计概览</span>
      </template>
      <el-row :gutter="16" v-loading="loadingStats">
        <el-col :span="6" v-for="m in statMetrics" :key="m.key">
          <div class="metric-card" :class="m.tone">
            <div class="metric-label">{{ m.label }}</div>
            <div class="metric-value">{{ m.value }}</div>
            <div class="metric-sub" v-if="m.sub">{{ m.sub }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- SLA 统计 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <span class="card-title">📈 SLA 统计</span>
      </template>
      <div v-loading="loadingSla" class="sla-grid">
        <div class="sla-item">
          <div class="sla-label">可用率</div>
          <div class="sla-value">{{ sla.uptime || '99.9%' }}%</div>
        </div>
        <div class="sla-item">
          <div class="sla-label">P50 响应</div>
          <div class="sla-value">{{ sla.p50 || '-' }}ms</div>
        </div>
        <div class="sla-item">
          <div class="sla-label">P95 响应</div>
          <div class="sla-value">{{ sla.p95 || '-' }}ms</div>
        </div>
        <div class="sla-item">
          <div class="sla-label">P99 响应</div>
          <div class="sla-value">{{ sla.p99 || '-' }}ms</div>
        </div>
        <div class="sla-item">
          <div class="sla-label">错误率</div>
          <div class="sla-value">{{ sla.errorRate || '0%' }}%</div>
        </div>
        <div class="sla-item">
          <div class="sla-label">总请求</div>
          <div class="sla-value">{{ sla.totalRequests || '-' }}</div>
        </div>
      </div>
    </el-card>

    <!-- 告警趋势 (占位 + 图表) -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <span class="card-title">📉 告警趋势 (近 7 天)</span>
      </template>
      <div v-if="!loadingTrend && trendData.length" class="trend-chart">
        <div v-for="(d, i) in trendData" :key="i" class="trend-bar" :style="{ height: d.height + '%' }" :title="`${d.date}: ${d.count} 次`">
          <span class="trend-count">{{ d.count }}</span>
        </div>
      </div>
      <EmptyState v-else-if="!loadingTrend" title="暂无趋势数据" description="系统运行一段时间后会展示告警趋势" compact />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { monitorApi } from '@/api/monitor'
import { Refresh } from '@element-plus/icons-vue'

const loadingJvm = ref(false)
const loadingStats = ref(false)
const loadingSla = ref(false)
const loadingTrend = ref(false)
const lastRefreshed = ref('')

function formatTime(d) {
  if (!d) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function refreshAll() {
  await Promise.all([loadJvm(), loadStats(), loadSla(), loadTrend()])
  lastRefreshed.value = formatTime(new Date())
}

const jvm = reactive({})
const sla = reactive({})

const statMetrics = computed(() => [
  { key: 'qps',     label: 'QPS',     value: '-',  tone: 'primary', sub: '每秒请求' },
  { key: 'rt',      label: 'RT',      value: '-',  tone: 'success', sub: '平均响应' },
  { key: 'errors',  label: '错误',    value: '-',  tone: 'danger',  sub: '近 1 小时' },
  { key: 'uptime',  label: '可用率',  value: '-',  tone: 'warning', sub: '本月' }
])

const trendData = ref([])

async function loadJvm() {
  loadingJvm.value = true
  try {
    const res = await monitorApi.getJvmMetrics()
    if (res.code === 0 && res.data) Object.assign(jvm, res.data)
  } catch (e) { console.error('jvm', e) }
  finally { loadingJvm.value = false }
}

async function loadStats() {
  loadingStats.value = true
  try {
    const res = await monitorApi.getStats()
    if (res.code === 0 && res.data) {
      statMetrics.value[0].value = res.data.qps ?? '-'
      statMetrics.value[1].value = (res.data.rt ?? '-') + 'ms'
      statMetrics.value[2].value = res.data.errors ?? '-'
      statMetrics.value[3].value = (res.data.uptime ?? '-') + '%'
    }
  } catch (e) { console.error('stats', e) }
  finally { loadingStats.value = false }
}

async function loadSla() {
  loadingSla.value = true
  try {
    const res = await monitorApi.getSla()
    if (res.code === 0 && res.data) Object.assign(sla, res.data)
  } catch (e) { console.error('sla', e) }
  finally { loadingSla.value = false }
}

async function loadTrend() {
  loadingTrend.value = true
  try {
    const res = await monitorApi.getAlertTrend(7)
    if (res.code === 0 && res.data) {
      const max = Math.max(...res.data.map(d => d.count), 1)
      trendData.value = res.data.map(d => ({
        ...d,
        height: (d.count / max) * 100
      }))
    }
  } catch (e) { console.error('trend', e) }
  finally { loadingTrend.value = false }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.overview-page { display: flex; flex-direction: column; gap: 16px; }
.section-card { border-radius: 12px; }
.card-title { font-weight: 600; color: #1e293b; }
.metric-card {
  padding: 20px 16px;
  border-radius: 10px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.metric-card.primary { background: #eff6ff; border-color: #bfdbfe; }
.metric-card.success { background: #f0fdf4; border-color: #bbf7d0; }
.metric-card.danger  { background: #fef2f2; border-color: #fecaca; }
.metric-card.warning { background: #fffbeb; border-color: #fed7aa; }
.metric-label { color: #64748b; font-size: 0.85em; }
.metric-value { font-size: 1.6em; font-weight: 700; color: #1e293b; margin: 6px 0; }
.metric-sub { color: #94a3b8; font-size: 0.8em; }
.sla-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.sla-item {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  text-align: center;
}
.sla-label { color: #64748b; font-size: 0.85em; }
.sla-value { font-size: 1.3em; font-weight: 700; color: #1e293b; margin-top: 6px; }
.trend-chart {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  height: 200px;
  padding: 16px 0;
}
.trend-bar {
  flex: 1;
  background: linear-gradient(180deg, #60a5fa, #3b82f6);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.trend-count {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.75em;
  color: #64748b;
  margin-bottom: 2px;
}
</style>
