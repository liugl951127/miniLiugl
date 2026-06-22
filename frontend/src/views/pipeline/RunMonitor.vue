<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📊 运行监控 (V5.32)</span>
          <el-button @click="$router.push('/pipeline')">← 返回列表</el-button>
        </div>
      </template>

      <el-table :data="runs" v-loading="loading" stripe>
        <el-table-column prop="runId" label="Run ID" width="200" />
        <el-table-column prop="workflowId" label="工作流" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress || 0"
              :status="row.status === 'FAILED' ? 'exception' : (row.status === 'SUCCESS' ? 'success' : '')" />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始" width="180" />
        <el-table-column prop="endTime" label="结束" width="180" />
        <el-table-column prop="duration" label="耗时" width="100">
          <template #default="{ row }">
            {{ row.duration ? row.duration + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewRun(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" :title="`运行 ${current?.runId}`" width="900">
      <template v-if="current">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)">{{ current.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始">{{ current.startTime }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ current.duration }}ms</el-descriptions-item>
        </el-descriptions>

        <h4>节点执行</h4>
        <el-table :data="current.nodeStatuses" size="small" border>
          <el-table-column prop="nodeId" label="节点" width="100" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时(ms)" width="100" />
          <el-table-column prop="error" label="错误" />
        </el-table>

        <h4>输出</h4>
        <pre class="result">{{ JSON.stringify(current.result, null, 2) }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getRun, getRunResult, listWorkflowRuns } from '@/api/pipeline'

const route = useRoute()
const runs = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const current = ref(null)
let pollTimer = null

function statusType(s) {
  return {
    SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning',
    PENDING: 'info', SKIPPED: 'info', CANCELLED: 'info'
  }[s] || ''
}

async function load() {
  loading.value = true
  try {
    const wfId = route.params.id
    if (wfId) {
      const res = await listWorkflowRuns(wfId, { page: 1, size: 50 })
      runs.value = res.data?.records || res.data || []
    } else {
      runs.value = []
    }
  } catch (e) {} finally { loading.value = false }
}

async function viewRun(row) {
  const res = await getRun(row.runId)
  current.value = res.data
  // 加载结果
  try {
    const r2 = await getRunResult(row.runId)
    current.value.result = r2.data
  } catch (e) {}
  detailVisible.value = true
}

onMounted(() => {
  load()
  // 轮询刷新
  pollTimer = setInterval(load, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.result { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 12px; max-height: 300px; overflow: auto; }
h4 { margin: 16px 0 8px; }
</style>
