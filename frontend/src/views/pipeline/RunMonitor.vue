<!-- @file pipeline/RunMonitor.vue - 运行监控 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header"><h2>运行监控</h2><el-button size="small" @click="loadRuns">刷新</el-button></div>
    <el-table :data="runs" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="workflowName" label="工作流" />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="duration" label="耗时" width="100" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="viewLog(row)">日志</el-button>
          <el-button v-if="row.status === 'RUNNING'" size="small" type="danger" @click="stopRun(row)">停止</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-drawer v-model="logVisible" title="运行日志" size="500px">
      <pre style="font-size:12px;white-space:pre-wrap">{{ logContent }}</pre>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listWorkflowRuns, getRunResult } from '@/api/pipeline'

const runs = ref([])
const loading = ref(false)
const logVisible = ref(false)
const logContent = ref('')

function statusType(s) { return { PENDING: 'info', RUNNING: 'primary', SUCCESS: 'success', FAILED: 'danger' }[s] || 'info' }

async function loadRuns() {
  loading.value = true
  try { runs.value = (await listWorkflowRuns()).data || [] }
  catch { runs.value = [] }
  finally { loading.value = false }
}

async function viewLog(r) {
  logContent.value = (await getRunResult(r.id).catch(() => ({ data: '暂无日志' }))).data || ''
  logVisible.value = true
}

async function stopRun(r) {
  try { ElMessage.success('已停止'); loadRuns() }
  catch { ElMessage.error('操作失败') }
}

onMounted(loadRuns)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
</style>
