<!--
  @file views/admin/Traces.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Traces.vue (调用链追踪)
  @version V3.5.12+ (前端注释补全)
  @description 调用链追踪
-->
<template>
  <div class="page-traces">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('traces.title') }} <el-tag size="small" type="info">V5.14 OpenTelemetry</el-tag></h2>
        <p class="page-subtitle">Trace ID / 服务 / 慢调用 / Span / 自动刷新</p>
      </div>
      <div class="header-actions">
        <el-input v-model="service" placeholder="服务名 (e.g. minimax-auth)" style="width:200px" clearable />
        <el-input v-model="traceId" placeholder="Trace ID" style="width:280px" clearable />
        <el-button :icon="Search" @click="search" :loading="loading">查询</el-button>
        <el-switch v-model="autoRefresh" active-text="10s 自动刷新" @change="toggleAuto" />
      </div>
    </header>

    <!-- 2. section: 4 KPI -->
    <section class="section">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="Traces" :value="traces.length" :value-style="{ color: '#6366f1' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="Spans" :value="totalSpans" :value-style="{ color: '#10b981' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="Errors" :value="errorCount" :value-style="{ color: '#ef4444' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="Avg P95" :value="p95" suffix="ms" :value-style="{ color: '#a855f7' }" /></el-card></el-col>
      </el-row>
    </section>

    <!-- 3. section: 慢调用 Top 10 -->
    <section class="section">
      <h3 class="section-title">🐢 慢调用 Top 10</h3>
      <el-card shadow="hover">
        <el-table :data="slowCalls" stripe size="small">
          <el-table-column prop="service" label="服务" width="160" />
          <el-table-column prop="operation" label="Operation" min-width="200" />
          <el-table-column prop="duration" label="耗时 (ms)" width="120" sortable>
            <template #default="{ row }">
              <el-tag :type="row.duration > 1000 ? 'danger' : row.duration > 500 ? 'warning' : 'success'" size="small">
                {{ row.duration.toFixed(1) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="timestamp" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" @click="viewTrace(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <!-- 4. section: Trace 列表 -->
    <section class="section">
      <h3 class="section-title">📋 最近 Trace ({{ traces.length }})</h3>
      <el-card shadow="hover">
        <el-table :data="traces" stripe>
          <el-table-column prop="traceId" label="Trace ID" min-width="200">
            <template #default="{ row }">
              <code class="trace-id">{{ row.traceId.substring(0, 16) }}...</code>
            </template>
          </el-table-column>
          <el-table-column prop="service" label="服务" width="140" />
          <el-table-column prop="operation" label="Operation" min-width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'OK' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时" width="100">
            <template #default="{ row }">{{ row.duration.toFixed(1) }} ms</template>
          </el-table-column>
          <el-table-column prop="spans" label="Spans" width="80" />
          <el-table-column prop="timestamp" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
          </el-table-column>
        </el-table>
        <EmptyState :description="'暂无数据'" />
      </el-card>
    </section>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'
import http from '@/api/http'

import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const service = ref('')
const toast = useToast()
const traceId = ref('')
const loading = ref(false)
const autoRefresh = ref(false)
const traces = ref([])
let timer = null

const jaegerUrl = ref(import.meta.env.VITE_JAEGER_URL || 'http://localhost:16686')

const totalSpans = computed(() => traces.value.reduce((a, t) => a + (t.spans?.length || 0), 0))
const _avgDuration = computed(() => {
  if (traces.value.length === 0) return 0
  const sum = traces.value.reduce((a, t) => a + (t.durationUs || 0), 0)
  return sum / traces.value.length / 1000  // ms
})
const _errorRate = computed(() => {
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

function _formatRel(trace) {
  if (!trace.durationUs) return '-'
  return formatMs(trace.durationUs)
}

function formatTime(us) {
  if (!us) return '-'
  // Jaeger 时间戳是微秒
  return new Date(us / 1000).toLocaleString('zh-CN')
}

function _openJaeger(tid) {
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
    toast.warning('Jaeger 未启动或不可达: ' + (e?.message || '未知错误'))
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
