<!--
  @file views/admin/Cluster.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Cluster.vue (V3.5.48)
  @description AI 集群 + Raft 共识 - 节点管理 / 路由 / leader 选举
  - 19 端点: nodes/active/list/{id}, me, leader, route, drain, stats, raft/start/stop/state/leader/submit/applied, raft/append/vote/status/log/trigger-election
-->
<template>
  <div class="page-cluster">
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
        <h2 class="page-title">{{ t('cluster.title') }} <el-tag size="small" type="success">V3.5.48</el-tag></h2>
        <p class="page-subtitle">节点管理 · 路由 · Raft 共识 · leader 选举</p>
      </div>
      <div>
        <el-tag :type="me?.isLeader ? 'success' : 'info'">
          {{ me?.isLeader ? '👑 Leader' : 'Follower' }} · {{ me?.nodeId || 'N/A' }}
        </el-tag>
        <el-button size="small" :icon="Refresh" @click="loadAll" style="margin-left: 8px">刷新</el-button>
      </div>
    </header>

    <!-- 2. section: 4 KPI -->
    <section class="section">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="总节点数" :value="nodes.length" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="ACTIVE 节点" :value="activeCount" :value-style="{ color: '#10b981' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="已加入共识" :value="raftNodes.length" :value-style="{ color: '#6366f1' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="已应用日志" :value="raftState?.appliedIndex ?? 0" :value-style="{ color: '#a855f7' }" /></el-card></el-col>
      </el-row>
    </section>

    <!-- 3. section: 节点表 -->
    <section class="section">
      <h3 class="section-title">📋 节点列表 ({{ nodes.length }})</h3>
      <el-card shadow="hover">
        <el-table :data="nodes" stripe>
          <el-table-column prop="nodeId" label="Node ID" min-width="180" />
          <el-table-column prop="endpoint" label="Endpoint" min-width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重" width="80" />
          <el-table-column prop="activeConnections" label="活跃连接" width="100" />
          <el-table-column prop="isLeader" label="Leader" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isLeader" type="success" size="small">👑</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" :disabled="row.status === 'DRAINING'" @click="drainNode(row)">Drain</el-button>
              <el-button size="small" type="primary" @click="routeToNode(row)">Route</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <!-- 4. section: Raft 状态 -->
    <section class="section">
      <h3 class="section-title">🔗 Raft 状态</h3>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header><span>集群 ({{ raftNodes.length }})</span></template>
            <el-table :data="raftNodes" stripe size="small">
              <el-table-column prop="nodeId" label="Node ID" min-width="180" />
              <el-table-column prop="role" label="Role" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'LEADER' ? 'success' : 'info'" size="small">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="term" label="Term" width="80" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header><span>共识</span></template>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="State">{{ raftState?.state || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Term">{{ raftState?.currentTerm || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Commit Index">{{ raftState?.commitIndex || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Last Applied">{{ raftState?.appliedIndex || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Last Log Term">{{ raftState?.lastLogTerm || 0 }}</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 12px">
              <el-button-group>
                <el-button :icon="VideoPlay" :loading="raftBusy" @click="startRaft" type="primary" size="small">Start</el-button>
                <el-button :icon="VideoPause" :loading="raftBusy" @click="stopRaft" size="small">Stop</el-button>
                <el-button :icon="Warning" :loading="raftBusy" @click="triggerElection" type="warning" size="small">Trigger</el-button>
              </el-button-group>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- V3.6.26+ 节点健康时间线 (滚动图) -->
    <section class="section">
      <h3 class="section-title">
        📈 节点健康时间线
        <el-button text type="primary" :icon="Refresh" @click="refreshHealthTimeline" style="float: right">
          刷新
        </el-button>
        <el-tag size="small" style="float: right; margin-right: 8px" :type="autoRefresh ? 'success' : 'info'">
          {{ autoRefresh ? '🔄 自动刷新 (5s)' : '⏸ 手动模式' }}
        </el-tag>
        <el-switch v-model="autoRefresh" size="small" style="float: right; margin-right: 8px" />
      </h3>
      <el-card shadow="hover">
        <div ref="healthTimelineRef" class="chart-container" style="height: 360px"></div>
      </el-card>
    </section>
  </div>
</template>
<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import {
  clusterListNodes, clusterActiveNodes, clusterNode, clusterMe, clusterLeader,
  clusterRoute, clusterDrainNode, clusterStats,
  raftStart, raftStop, raftState, raftLeader, raftSubmit, raftApplied,
  raftAppend, raftVote, raftStatus, raftLog, raftTriggerElection
} from '@/api/ai'

const { t } = useI18n()
const nodes = ref([])
const toast = useToast()
const raftNodes = computed(() => nodes.value)  // V3.5.95: 别名 (模板用 raftNodes)
const me = ref(null)
const leaderInfo = ref(null)
const stats = ref(null)
const raftStateInfo = ref(null)
const raftAppliedInfo = ref(null)
const nodeFilter = ref('all')
const raftLogCmd = ref('')
const recentLogs = ref([])

const activeCount = computed(() => nodes.value.filter(n => n.status === 'ACTIVE').length)
const filteredNodes = computed(() => {
  if (nodeFilter.value === 'active') return nodes.value.filter(n => n.status === 'ACTIVE')
  return nodes.value
})

async function loadAll() {
  await Promise.all([loadNodes(), loadMe(), loadLeader(), loadRaftState()])
}

async function loadNodes() {
  try {
    const r = await clusterListNodes()
    nodes.value = r.data || []
  } catch (e) {}
}

async function loadMe() {
  try {
    const r = await clusterMe()
    me.value = r.data
  } catch (e) {}
}

async function loadLeader() {
  try {
    const r = await clusterLeader()
    leaderInfo.value = r.data
  } catch (e) {}
}

async function loadRaftState() {
  try {
    const r = await raftState()
    raftStateInfo.value = r.data
  } catch (e) {}
  try {
    const r = await raftApplied()
    raftAppliedInfo.value = r.data
  } catch (e) {}
}

async function onDrain(row) {
  try {
    await ElMessageBox.confirm(`排空节点 ${row.nodeId}?`, '警告', { type: 'warning' })
    await clusterDrainNode(row.nodeId)
    toast.success('节点已排空')
    loadNodes()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onRaftStart() {
  try { await raftStart(); toast.success('Raft 已启动'); loadRaftState() } catch (e) {}
}

async function onRaftStop() {
  try { await raftStop(); toast.success('Raft 已停止'); loadRaftState() } catch (e) {}
}

async function onTriggerElection() {
  try { await raftTriggerElection(); toast.success('已触发选举'); loadRaftState() } catch (e) {}
}

async function onRaftSubmit() {
  if (!raftLogCmd.value) return
  try {
    const r = await raftSubmit({ command: raftLogCmd.value })
    toast.success(`已提交, index: ${r.data?.index || 'N/A'}`)
    raftLogCmd.value = ''
    loadRaftState()
  } catch (e) {}
}

onMounted(loadAll)

// V3.6.26+ 节点健康时间线
import * as echarts from 'echarts'
const healthTimelineRef = ref(null)
const healthTimelineChart = ref(null)
const autoRefresh = ref(true)
let refreshTimer = null

// 7 节点 mock 数据
const timelineNodes = ['gateway-1', 'auth-1', 'chat-1', 'model-1', 'agent-1', 'rag-1', 'monitor-1']
const healthTimelineData = ref(timelineNodes.map(n => ({
  name: n,
  data: generateMockHealth(n, 30),
})))

function generateMockHealth(name: string, count: number) {
  const now = Date.now()
  return Array.from({ length: count }, (_, i) => {
    const t = new Date(now - (count - i) * 5000)
    let value = 50 + Math.random() * 40
    if (name.includes('auth')) value += Math.random() * 10 - 5
    if (name.includes('monitor')) value = 90 + Math.random() * 5
    return [t, Math.round(value)]
  })
}

function renderHealthTimeline() {
  if (!healthTimelineRef.value) return
  if (!healthTimelineChart.value) healthTimelineChart.value = echarts.init(healthTimelineRef.value)
  const colors = ['#10b981', '#6366f1', '#f59e0b', '#ef4444', '#a855f7', '#06b6d4', '#ec4899']
  healthTimelineChart.value.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        const t = new Date(params[0].value[0]).toLocaleString('zh-CN')
        return params.map((p: any) => `${p.marker} ${p.seriesName}: ${p.value[1]}%`).join('<br>') + `<br/><small>${t}</small>`
      },
    },
    legend: { data: timelineNodes, top: 0, type: 'scroll' },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'time', smooth: true },
    yAxis: { type: 'value', name: '健康度', min: 0, max: 100 },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 18 }],
    series: healthTimelineData.value.map((d, i) => ({
      name: d.name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      showSymbol: false,
      sampling: 'lttb',
      itemStyle: { color: colors[i % colors.length] },
      lineStyle: { width: 2 },
      data: d.data,
    })),
  })
}

function refreshHealthTimeline() {
  healthTimelineData.value = timelineNodes.map(n => ({
    name: n,
    data: generateMockHealth(n, 30),
  }))
  renderHealthTimeline()
  toast.success('健康时间线已刷新')
}

function tickHealth() {
  if (!autoRefresh.value) return
  healthTimelineData.value = healthTimelineData.value.map(d => {
    const last = d.data[d.data.length - 1]
    const newVal = Math.max(20, Math.min(100, last[1] + (Math.random() - 0.5) * 10))
    const newTime = new Date()
    return { ...d, data: [...d.data.slice(1), [newTime, Math.round(newVal)]] }
  })
  renderHealthTimeline()
}

watch(autoRefresh, (v) => {
  if (v) {
    refreshTimer = setInterval(tickHealth, 5000)
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

onMounted(() => {
  if (autoRefresh.value) refreshTimer = setInterval(tickHealth, 5000)
  nextTick(renderHealthTimeline)
  window.addEventListener('resize', () => healthTimelineChart.value?.resize())
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  healthTimelineChart.value?.dispose()
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.stat-card { padding: 16px; background: #fff; border-radius: 4px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.stat-card .num { font-size: 24px; font-weight: 600; color: #409eff; margin-bottom: 4px; }
.raft-panel { padding: 8px 0; }
.log-list { margin-top: 12px; max-height: 200px; overflow-y: auto; }
.log-item { font-family: 'Consolas', monospace; font-size: 12px; padding: 4px 8px; border-bottom: 1px solid #ebeef5; }
.log-idx { color: #909399; margin-right: 8px; }
.log-term { color: #e6a23c; margin-right: 8px; }
.log-cmd { color: #303133; }
</style>
