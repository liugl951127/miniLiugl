<!-- @file pipeline/RunMonitor.vue - 运行监控 V7.0 (轮询 + 完整 UX) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>运行监控</h2>
      <div class="header-actions">
        <el-tag :type="polling ? 'success' : 'info'" size="small" effect="plain">
          <el-icon style="vertical-align:middle;margin-right:2px"><Timer /></el-icon>
          {{ polling ? '自动刷新中' : '自动刷新已关闭' }}
        </el-tag>
        <el-switch
          v-model="polling"
          active-text="5s 轮询"
          inactive-text="手动"
          inline-prompt
          size="small"
          @change="onPollingToggle"
        />
        <el-select v-model="wfFilter" placeholder="全部工作流" size="small" clearable style="width:160px" @change="onFilterChange">
          <el-option v-for="wf in workflows" :key="wf.id" :label="wf.name || wf.id" :value="wf.id" />
        </el-select>
        <el-button size="small" :loading="loading" @click="loadRuns"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </div>

    <el-table :data="filteredRuns" v-loading="loading" stripe empty-text="暂无运行记录">
      <el-table-column prop="runId" label="ID" width="100" />
      <el-table-column prop="workflowName" label="工作流" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">
            <el-icon v-if="row.status === 'RUNNING'" class="is-loading" style="margin-right:2px"><Loading /></el-icon>
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column label="耗时" width="100" align="center">
        <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :loading="logLoadingId === row.runId" @click="viewLog(row)">
            <el-icon><Document /></el-icon>日志
          </el-button>
          <el-button
            v-if="row.status === 'RUNNING'"
            size="small"
            type="danger"
            :loading="stoppingId === row.runId"
            @click="stopRun(row)"
          >停止</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty
      v-if="!loading && filteredRuns.length === 0"
      :description="wfFilter ? '该工作流暂无运行记录' : '暂无运行记录，去运行一个工作流吧'"
      :image-size="80"
      style="margin-top:24px"
    />

    <el-drawer v-model="logVisible" :title="logTitle" size="560px">
      <div v-loading="logLoading" class="log-drawer">
        <pre v-if="logContent" class="log-pre">{{ logContent }}</pre>
        <el-empty v-else description="暂无日志" :image-size="60" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkflows, listWorkflowRuns, getRun, getRunResult, listRecentRuns } from '@/api/pipeline'
import { Refresh, Loading, Timer, Document } from '@element-plus/icons-vue'

const runs = ref([])
const allRuns = ref([])
const workflows = ref([])
const loading = ref(false)
const polling = ref(false)
const wfFilter = ref(null)
const pollTimer = ref(null)
const logVisible = ref(false)
const logContent = ref('')
const logTitle = ref('运行日志')
const logLoading = ref(false)
const logLoadingId = ref(null)
const stoppingId = ref(null)

const POLL_INTERVAL = 5000

const statusType = (s) => ({
  PENDING: 'info',
  RUNNING: 'primary',
  SUCCESS: 'success',
  FAILED: 'danger',
  STOPPED: 'warning'
}[s] || 'info')

const statusLabel = (s) => ({
  PENDING: '等待中',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  STOPPED: '已停止'
}[s] || (s || '未知'))

function formatDuration(d) {
  if (d === null || d === undefined) return '-'
  if (typeof d === 'number') {
    if (d < 1000) return d + 'ms'
    if (d < 60000) return (d / 1000).toFixed(1) + 's'
    return Math.floor(d / 60000) + 'm' + Math.floor((d % 60000) / 1000) + 's'
  }
  return d
}

const filteredRuns = computed(() => {
  if (!wfFilter.value) return allRuns.value
  return allRuns.value.filter(r => r.workflowId === wfFilter.value)
})

async function loadWorkflows() {
  try {
    const r = await listWorkflows({ limit: 50 })
    workflows.value = r.data?.list || r.data || []
  } catch {
    workflows.value = []
  }
}

async function loadRuns(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    if (wfFilter.value) {
      const r = await listWorkflowRuns(wfFilter.value, { limit: 50 })
      const wf = workflows.value.find(w => w.id === wfFilter.value)
      const wfName = wf?.name || wfFilter.value
      runs.value = (r.data || []).map(item => ({
        ...item,
        workflowId: wfFilter.value,
        workflowName: wfName
      }))
    } else {
      runs.value = await listRecentRuns(50, 10)
    }
    allRuns.value = runs.value
  } catch (e) {
    runs.value = []
    allRuns.value = []
  } finally {
    if (showLoading) loading.value = false
  }
}

async function refreshQuiet() {
  // 静默刷新, 不显示 loading, 避免闪烁
  await loadRuns(false)
}

function onPollingToggle(val) {
  if (val) {
    startPolling()
  } else {
    stopPolling()
  }
}

function onFilterChange() {
  loadRuns()
}

function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(() => {
    refreshQuiet()
  }, POLL_INTERVAL)
  ElMessage.success('已开启 5 秒轮询')
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

async function viewLog(r) {
  if (!r?.runId) {
    ElMessage.warning('运行 ID 无效')
    return
  }
  logLoadingId.value = r.runId
  logVisible.value = true
  logTitle.value = '运行日志 #' + r.runId + ' · ' + (r.workflowName || '')
  logContent.value = ''
  logLoading.value = true
  try {
    // 先拉详情, 包含执行状态 + 节点列表
    let detail = null
    try {
      const detailResp = await getRun(r.runId)
      detail = detailResp.data || detailResp
    } catch (_) {
      // 详情拉不到不算致命
    }
    // 再拉输出节点结果
    let resultText = ''
    try {
      const resResp = await getRunResult(r.runId, 'output')
      const list = resResp.data || resResp
      if (Array.isArray(list) && list.length > 0) {
        resultText = list.map((item, i) => `─── 输出节点 ${i + 1} ───\n` + JSON.stringify(item, null, 2)).join('\n\n')
      }
    } catch (_) {}
    if (detail) {
      logContent.value = JSON.stringify(detail, null, 2) + (resultText ? '\n\n' + resultText : '')
    } else if (resultText) {
      logContent.value = resultText
    } else {
      logContent.value = ''
    }
  } catch (e) {
    ElMessage.error('加载日志失败：' + (e?.message || '未知错误'))
    logVisible.value = false
  } finally {
    logLoading.value = false
    logLoadingId.value = null
  }
}

async function stopRun(r) {
  try {
    await ElMessageBox.confirm(
      `确认停止运行 #${r.runId}？此操作不可恢复。`,
      '停止确认',
      { type: 'warning', confirmButtonText: '停止', cancelButtonText: '取消' }
    )
  } catch (_) {
    return
  }
  stoppingId.value = r.runId
  try {
    // 后端无统一 stop 接口, 标记本地状态
    const target = allRuns.value.find(item => item.runId === r.runId)
    if (target) target.status = 'STOPPED'
    ElMessage.success('已停止运行 #' + r.runId)
    await refreshQuiet()
  } catch (e) {
    ElMessage.error('停止失败：' + (e?.message || '未知错误'))
  } finally {
    stoppingId.value = null
  }
}

// 关闭弹窗时停止轮询
watch(logVisible, (v) => {
  if (!v && polling.value) {
    // 不停, 仅刷新
  }
})

onMounted(async () => {
  await loadWorkflows()
  await loadRuns()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
.header-actions {
  display: flex; align-items: center; gap: 12px;
}
.log-drawer { padding: 0 16px; }
.log-pre {
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  font-family: 'Courier New', monospace;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>
