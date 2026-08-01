<!--
  @file views/admin/Metrics.vue (指标)
  @version V3.5.12+ (前端注释补全)
  @description 指标
-->
<template>
  <div class="metrics-page">
    <div class="header">
      <h2>📊 实时 Metrics 监控 (V5.10)</h2>
      <div class="header-right">
        <el-select v-model="service" placeholder="选择服务" style="width:240px" @change="loadAll">
          <el-option label="minimax-gateway" value="gateway" />
          <el-option v-for="s in services" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button @click="loadAll" :loading="loading">刷新</el-button>
        <el-switch v-model="autoRefresh" active-text="10s 自动刷新" @change="toggleAuto" />
      </div>
    </div>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="cards">
      <el-col :span="6">
        <el-card><div class="num">{{ summary.totalReq || 0 }}</div><div class="lbl">总请求数</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="num err">{{ summary.error4xx || 0 }}</div><div class="lbl">4xx 错误</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="num err">{{ summary.error5xx || 0 }}</div><div class="lbl">5xx 错误</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="num">{{ summary.avgMs || 0 }} ms</div><div class="lbl">平均延迟</div></el-card>
      </el-col>
    </el-row>

    <!-- Top 10 URI -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header><span>🔥 Top 10 高频 URI</span></template>
          <el-table :data="topUris" stripe size="small" empty-text="暂无数据">
            <el-table-column type="index" width="50" />
            <el-table-column prop="uri" label="URI" min-width="180" />
            <el-table-column prop="count" label="请求数" width="100" />
            <el-table-column label="占比" width="100">
              <template #default="s">
                <el-progress :percentage="pct(s.row.count)" :stroke-width="8" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>🐢 Top 10 慢 URI (平均 ms)</span></template>
          <el-table :data="slowUris" stripe size="small" empty-text="暂无数据">
            <el-table-column type="index" width="50" />
            <el-table-column prop="uri" label="URI" min-width="180" />
            <el-table-column prop="avgMs" label="平均 (ms)" width="100" />
            <el-table-column prop="maxMs" label="最大 (ms)" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 状态码分布 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header><span>📈 状态码分布</span></template>
          <v-chart :option="statusPieOption" autoresize style="height:280px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>⏱️ URI 耗时 Top 5 (折线)</span></template>
          <v-chart :option="durationOption" autoresize style="height:280px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 原始 Prometheus 文本 (折叠) -->
    <el-collapse style="margin-top:16px">
      <el-collapse-item title="📋 原始 Prometheus 输出 (调试用)" name="raw">
        <pre class="raw">{{ rawText }}</pre>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, onUnmounted, markRaw } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const services = [
  'minimax-auth', 'minimax-chat', 'minimax-model', 'minimax-memory',
  'minimax-rag', 'minimax-function', 'minimax-agent', 'minimax-admin',
  'minimax-prompt', 'minimax-multimodal', 'minimax-monitor', 'minimax-ws',
]
const service = ref('minimax-auth')
const loading = ref(false)
const autoRefresh = ref(false)
const rawText = ref('')
const metrics = ref({})  // 解析后的指标 map
let timer = null

const summary = computed(() => {
  const totalReq = sumByTag('minimax_http_requests_total')
  const error4xx = sumByTag('minimax_http_4xx_errors_total')
  const error5xx = sumByTag('minimax_http_5xx_errors_total')
  const avgMs = avgDuration()
  return { totalReq, error4xx, error5xx, avgMs: avgMs.toFixed(0) }
})

const topUris = computed(() => aggregateByUri('minimax_http_requests_total').slice(0, 10))
const slowUris = computed(() => aggregateDuration().sort((a, b) => b.avgMs - a.avgMs).slice(0, 10))

const statusPieOption = computed(() => {
  const statusMap = {}
  Object.entries(metrics.value).forEach(([k, v]) => {
    if (k.startsWith('minimax_http_requests_total{') && k.includes('status=')) {
      const m = k.match(/status="(\d+)"/)
      if (m) {
        const code = m[1]
        statusMap[code] = (statusMap[code] || 0) + v
      }
    }
  })
  const data = Object.entries(statusMap).map(([name, value]) => ({ name, value }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data,
      label: { formatter: '{b}: {d}%' },
      color: ['#10b981', '#f59e0b', '#ef4444', '#6366f1', '#8b5cf6'],
    }],
  }
})

const durationOption = computed(() => {
  const top = aggregateDuration().slice(0, 5)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 20, top: 20, bottom: 20 },
    xAxis: { type: 'value', name: 'ms' },
    yAxis: { type: 'category', data: top.map(x => x.uri), inverse: true },
    series: [{
      type: 'bar',
      data: top.map(x => x.avgMs),
      itemStyle: { color: '#6366f1' },
      label: { show: true, position: 'right', formatter: '{c} ms' },
    }],
  }
})

function pct(n) {
  const max = Math.max(...topUris.value.map(x => x.count), 1)
  return Math.round((n / max) * 100)
}

function sumByTag(name) {
  return Object.entries(metrics.value)
    .filter(([k]) => k.startsWith(name + '{'))
    .reduce((acc, [, v]) => acc + v, 0)
}

function avgDuration() {
  const entries = Object.entries(metrics.value)
    .filter(([k]) => k.startsWith('minimax_http_requests_duration_seconds_count{'))
  if (entries.length === 0) return 0
  let totalCount = 0, totalSum = 0
  entries.forEach(([k, count]) => {
    const sumKey = k.replace('_count', '_sum')
    const sum = metrics.value[sumKey] || 0
    totalCount += count
    totalSum += sum
  })
  return totalCount > 0 ? (totalSum / totalCount) * 1000 : 0  // 转为 ms
}

function aggregateByUri(name) {
  const map = {}
  Object.entries(metrics.value).forEach(([k, v]) => {
    if (k.startsWith(name + '{')) {
      const m = k.match(/uri="([^"]+)"/)
      if (m) {
        const uri = m[1]
        map[uri] = (map[uri] || 0) + v
      }
    }
  })
  return Object.entries(map).map(([uri, count]) => ({ uri, count }))
    .sort((a, b) => b.count - a.count)
}

function aggregateDuration() {
  const counts = aggregateByUri('minimax_http_requests_duration_seconds_count')
  const sums = aggregateByUri('minimax_http_requests_duration_seconds_sum')
  const sumMap = Object.fromEntries(sums.map(x => [x.uri, x.count]))
  return counts.map(({ uri, count }) => {
    const sumSec = sumMap[uri] || 0
    const avgMs = count > 0 ? (sumSec / count) * 1000 : 0
    return { uri, count, avgMs: Math.round(avgMs), maxMs: 0 }
  })
}

async function loadAll() {
  loading.value = true
  try {
    // 通过 gateway 路由 (带 JWT) 调到目标服务的 /actuator/prometheus
    const url = service.value === 'gateway'
      ? '/actuator/prometheus'
      : `/api/v1/monitor/forward-prometheus?service=${service.value}`
    const r = await http.get(url, { responseType: 'text' })
    rawText.value = typeof r === 'string' ? r : (r?.data || '')
    parsePrometheus(rawText.value)
  } catch (e) {
    ElMessage.warning('该服务未暴露 /actuator/prometheus 或未启动')
    rawText.value = ''
    metrics.value = {}
  } finally {
    loading.value = false
  }
}

/**
 * 解析 Prometheus 文本格式.
 * 例:
 *   minimax_http_requests_total{method="GET",uri="/api/v1/auth/login",status="200"} 12.0
 *   minimax_http_requests_total{method="POST",...} 5.0
 */
function parsePrometheus(text) {
  const map = {}
  if (!text) { metrics.value = {}; return }
  text.split('\n').forEach(line => {
    line = line.trim()
    if (!line || line.startsWith('#')) return
    const idx = line.lastIndexOf(' ')
    if (idx < 0) return
    const key = line.substring(0, idx)
    const val = parseFloat(line.substring(idx + 1))
    if (isNaN(val)) return
    map[key] = val
  })
  metrics.value = map
}

function toggleAuto(v) {
  if (v) timer = setInterval(loadAll, 10000)
  else if (timer) { clearInterval(timer); timer = null }
}

onMounted(loadAll)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.metrics-page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header h2 { margin: 0; }
.header-right { display: flex; gap: 12px; align-items: center; }
.cards { margin-bottom: 16px; }
.num { font-size: 28px; font-weight: bold; color: #6366f1; text-align: center; }
.num.err { color: #ef4444; }
.lbl { text-align: center; color: #6b7280; font-size: 13px; margin-top: 4px; }
.raw {
  max-height: 400px;
  overflow: auto;
  background: #f9fafb;
  padding: 12px;
  border-radius: 4px;
  font-size: 11px;
  font-family: monospace;
}
</style>
