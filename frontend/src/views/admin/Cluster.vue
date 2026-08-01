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
  <div class="page-cluster page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🖥️ AI 集群管理 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <div>
            <el-tag :type="me?.isLeader ? 'success' : 'info'">
              {{ me?.isLeader ? '👑 Leader' : 'Follower' }} · {{ me?.nodeId || 'N/A' }}
            </el-tag>
            <el-button size="small" @click="loadAll" :icon="Refresh" style="margin-left: 8px">刷新</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-card"><div class="num">{{ nodes.length }}</div><div>总节点数</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="num">{{ activeCount }}</div><div>ACTIVE 节点</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="num">{{ leaderInfo?.nodeId || 'N/A' }}</div><div>Leader</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="num">{{ stats?.loadAvg?.toFixed(2) || '0' }}</div><div>集群负载</div></div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="header">
              <span>📋 节点列表</span>
              <el-radio-group v-model="nodeFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="active">ACTIVE</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="filteredNodes" border stripe>
            <el-table-column prop="nodeId" label="节点 ID" width="180" />
            <el-table-column prop="address" label="地址" width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.role === 'LEADER'" type="warning" size="small">👑 LEADER</el-tag>
                <el-tag v-else size="small">FOLLOWER</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="load" label="负载" width="120">
              <template #default="{ row }">
                <el-progress :percentage="(row.load || 0) * 100" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="onDrain(row)" type="danger" :disabled="row.status !== 'ACTIVE'">
                  排空
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="header">
              <span>🗳 Raft 共识</span>
            </div>
          </template>
          <div class="raft-panel">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="State">{{ raftStateInfo?.state || 'UNKNOWN' }}</el-descriptions-item>
              <el-descriptions-item label="Term">{{ raftStateInfo?.term || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Leader">{{ raftStateInfo?.leader || 'N/A' }}</el-descriptions-item>
              <el-descriptions-item label="Last Log Index">{{ raftStateInfo?.lastLogIndex || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Commit Index">{{ raftStateInfo?.commitIndex || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Applied Index">{{ raftAppliedInfo?.applied || 0 }}</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 12px">
              <el-button-group>
                <el-button type="success" @click="onRaftStart" :disabled="raftStateInfo?.state === 'RUNNING'">▶ Start</el-button>
                <el-button type="danger" @click="onRaftStop" :disabled="raftStateInfo?.state !== 'RUNNING'">⏹ Stop</el-button>
                <el-button type="warning" @click="onTriggerElection">🗳 Trigger Election</el-button>
              </el-button-group>
            </div>
            <el-divider>提交日志</el-divider>
            <el-input v-model="raftLogCmd" placeholder="提交命令 (e.g. SET key value)" size="small">
              <template #append>
                <el-button @click="onRaftSubmit" :icon="Upload">Submit</el-button>
              </template>
            </el-input>
            <div v-if="recentLogs.length" class="log-list">
              <h5>最近日志</h5>
              <div v-for="(l, i) in recentLogs" :key="i" class="log-item">
                <span class="log-idx">#{{ l.index }}</span>
                <span class="log-term">T{{ l.term }}</span>
                <span class="log-cmd">{{ l.command }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import {
  clusterListNodes, clusterActiveNodes, clusterNode, clusterMe, clusterLeader,
  clusterRoute, clusterDrainNode, clusterStats,
  raftStart, raftStop, raftState, raftLeader, raftSubmit, raftApplied,
  raftAppend, raftVote, raftStatus, raftLog, raftTriggerElection
} from '@/api/ai'

const nodes = ref([])
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
    ElMessage.success('节点已排空')
    loadNodes()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onRaftStart() {
  try { await raftStart(); ElMessage.success('Raft 已启动'); loadRaftState() } catch (e) {}
}

async function onRaftStop() {
  try { await raftStop(); ElMessage.success('Raft 已停止'); loadRaftState() } catch (e) {}
}

async function onTriggerElection() {
  try { await raftTriggerElection(); ElMessage.success('已触发选举'); loadRaftState() } catch (e) {}
}

async function onRaftSubmit() {
  if (!raftLogCmd.value) return
  try {
    const r = await raftSubmit({ command: raftLogCmd.value })
    ElMessage.success(`已提交, index: ${r.data?.index || 'N/A'}`)
    raftLogCmd.value = ''
    loadRaftState()
  } catch (e) {}
}

onMounted(loadAll)
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
