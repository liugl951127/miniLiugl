<template>
  <div class="traces-page">
    <div class="header">
      <h2>🔍 分布式追踪 (V5.14 OpenTelemetry)</h2>
      <div class="header-right">
        <el-input v-model="service" placeholder="服务名 (e.g. minimax-auth)" style="width:200px" clearable />
        <el-input v-model="traceId" placeholder="Trace ID (可选, 查具体请求)" style="width:280px" clearable />
        <el-button @click="search" :loading="loading">查询</el-button>
        <el-switch v-model="autoRefresh" active-text="10s 自动刷新" @change="toggleAuto" />
      </div>
    </div>

    <!-- 概览 -->
    <el-row :gutter="16" class="cards">
      <el-col :span="6">
        <el-card><div class="num">{{ traces.length }}</div><div class="lbl">Traces</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="num">{{ totalSpans }}</div><div class="lbl">Spans</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="num">{{ formatMs(avgDuration) }}</div><div class="lbl">平均耗时</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="num" :class="errorRate > 5 ? 'err' : ''">{{ errorRate.toFixed(1) }}%</div>
          <div class="lbl">错误率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Trace 列表 -->
    <el-card>
      <template #header>
        <span>📋 Trace 列表 (按时间倒序)</span>
        <span class="subtitle">数据来源: Jaeger Query API (OTel 后端) · <code>{{ jaegerUrl }}</code></span>
      </template>
      <el-table :data="traces" stripe size="small" empty-text="未找到 trace 数据">
        <el-table-column type="expand">
          <template #default="s">
            <!-- Span 树 -->
            <div class="span-tree">
              <div v-for="span in s.row.spans" :key="span.spanID" class="span-row" :style="{ paddingLeft: (span.depth || 0) * 24 + 'px' }">
                <span class="span-name">{{ span.operationName || span.spanName }}</span>
                <span class="span-svc">{{ span.process?.serviceName || span.service }}</span>
                <span class="span-dur">{{ formatRel(span) }}</span>
                <span v-if="span.tags?.error" class="err-tag">ERROR</span>
                <span v-if="span.spanID" class="span-id">id={{ span.spanID.substring(0, 8) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Trace ID" min-width="200">
          <template #default="s">
            <code class="trace-id" @click="openJaeger(s.row.traceID)">{{ s.row.traceID?.substring(0, 16) }}...</code>
          </template>
        </el-table-column>
        <el-table-column label="根 Span" min-width="180">
          <template #default="s">
            {{ s.rootName || s.row.spans?.[0]?.operationName }}
          </template>
        </el-table-column>
        <el-table-column label="服务" min-width="140">
          <template #default="s">
            <el-tag size="small" v-for="svc in s.row.services" :key="svc">{{ svc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Spans" width="80">
          <template #default="s">{{ s.row.spans?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="s">
            <span :class="s.row.durationUs > 1000000 ? 'err' : ''">
              {{ formatRel(s.row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="160">
          <template #default="s">{{ formatTime(s.row.startTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 提示 -->
    <el-alert
      title="需要 Jaeger 后端"
      type="info"
      :closable="false"
      style="margin-top:16px"
      show-icon>
      <p>本页面从 <code>{{ jaegerUrl }}</code> 拉取数据, 需要先部署 Jaeger / Tempo / SigNoz 等 OTel 后端.</p>
      <p>如未部署, 启动命令: <code>docker run -d -p 16686:16686 -p 4318:4318 jaegertracing/all-in-one:latest</code></p>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const service = ref('')
const traceId = ref('')
const loading = ref(false)
const autoRefresh = ref(false)
const traces = ref([])
let timer = null

const jaegerUrl = ref(import.meta.env.VITE_JAEGER_URL || 'http://localhost:16686')

const totalSpans = computed(() => traces.value.reduce((a, t) => a + (t.spans?.length || 0), 0))
const avgDuration = computed(() => {
  if (traces.value.length === 0) return 0
  const sum = traces.value.reduce((a, t) => a + (t.durationUs || 0), 0)
  return sum / traces.value.length / 1000  // ms
})
const errorRate = computed(() => {
  if (totalSpans.value === 0) return 0
  const errors = traces.value.reduce((a, t) => {
    return a + (t.spans || []).filter(s => s.tags?.error || s.tags?.['error.type']).length
  }, 0)
  return (errors / totalSpans.value) * 100
})

function formatMs(us) {
  if (!us) return '-'
  if (us < 1000) return us.toFixed(0) + ' μs'
  if (us < 1000000) return (us / 1000).toFixed(0) + ' ms'
  return (us / 1000000).toFixed(2) + ' s'
}

function formatRel(trace) {
  if (!trace.durationUs) return '-'
  return formatMs(trace.durationUs)
}

function formatTime(us) {
  if (!us) return '-'
  // Jaeger 时间戳是微秒
  return new Date(us / 1000).toLocaleString('zh-CN')
}

function openJaeger(tid) {
  window.open(`${jaegerUrl.value}/trace/${tid}`, '_blank')
}

async function search() {
  loading.value = true
  try {
    // Jaeger Query API: /api/traces?service=X&lookback=1h&limit=20
    const params = new URLSearchParams()
    if (service.value) params.set('service', service.value)
    if (traceId.value) params.set('traceID', traceId.value)
    params.set('lookback', '1h')
    params.set('limit', '50')

    // 通过 CORS proxy (开发) 或直连 (Jaeger 需配 CORS)
    const url = `${jaegerUrl.value}/api/traces?${params}`
    const r = await http.get(url, { skipAuth: true })
    const data = typeof r === 'string' ? JSON.parse(r) : (r?.data || r)
    traces.value = data?.data || []
  } catch (e) {
    ElMessage.warning('Jaeger 未启动或不可达: ' + (e?.message || '未知错误'))
    traces.value = []
  } finally {
    loading.value = false
  }
}

function toggleAuto(v) {
  if (v) timer = setInterval(search, 10000)
  else if (timer) { clearInterval(timer); timer = null }
}

onMounted(search)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.traces-page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header h2 { margin: 0; }
.header-right { display: flex; gap: 8px; align-items: center; }
.cards { margin-bottom: 16px; }
.num { font-size: 28px; font-weight: bold; color: #6366f1; text-align: center; }
.num.err { color: #ef4444; }
.lbl { text-align: center; color: #6b7280; font-size: 13px; margin-top: 4px; }
.subtitle { font-size: 12px; color: #9ca3af; margin-left: 12px; }
.subtitle code { background: #f3f4f6; padding: 1px 6px; border-radius: 3px; }
.trace-id { background: #f3f4f6; padding: 2px 6px; border-radius: 3px; cursor: pointer; font-size: 12px; }
.trace-id:hover { background: #e0e7ff; }
.span-tree { background: #f9fafb; padding: 12px; border-radius: 4px; }
.span-row { padding: 4px 0; font-size: 12px; font-family: monospace; }
.span-name { font-weight: 500; }
.span-svc { color: #6366f1; margin-left: 12px; }
.span-dur { color: #10b981; margin-left: 12px; }
.span-id { color: #9ca3af; margin-left: 12px; }
.err-tag { background: #ef4444; color: #fff; padding: 1px 6px; border-radius: 3px; font-size: 10px; margin-left: 12px; }
.err { color: #ef4444; font-weight: bold; }
</style>
