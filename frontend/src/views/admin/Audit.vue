<template>
  <div class="audit">
    <el-card>
      <template #header>
        <div class="header">
          <span>📋 审计日志 (合规要求保留 6 个月+)</span>
          <el-button @click="exportData" type="primary" size="small">📥 导出</el-button>
        </div>
      </template>

      <!-- 过滤 -->
      <el-form :inline="true" :model="filters" style="margin-bottom: 16px">
        <el-form-item label="用户">
          <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="filters.action" placeholder="全部" clearable style="width: 160px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="数据导出" value="EXPORT_DATA" />
            <el-option label="AI 调用" value="AI_GENERATE" />
            <el-option label="文件上传" value="FILE_UPLOAD" />
            <el-option label="配置变更" value="CONFIG_CHANGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filters.result" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILURE" />
            <el-option label="拒绝" value="DENIED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">🔍 查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="logs" stripe v-loading="loading">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="userIp" label="IP" width="140" />
        <el-table-column prop="action" label="操作" width="120">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceType" label="资源" width="100" />
        <el-table-column prop="path" label="路径" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="scope">
            <el-tag :type="resultType(scope.row.result)" size="small">{{ scope.row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="80">
          <template #default="scope">{{ scope.row.durationMs }}ms</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button size="small" link @click="showDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLogs"
        @current-change="loadLogs"
        style="margin-top: 16px; text-align: right"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="审计日志详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.username }} (ID: {{ detail.userId }})</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.userIp }}</el-descriptions-item>
        <el-descriptions-item label="UA">{{ detail.userAgent }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="资源">{{ detail.resourceType }} / {{ detail.resourceId }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.method }} {{ detail.path }}</el-descriptions-item>
        <el-descriptions-item label="请求体">
          <pre style="background: #f5f5f5; padding: 8px; max-height: 200px; overflow: auto">{{ detail.requestBody }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="resultType(detail.result)">{{ detail.result }}</el-tag>
          <span v-if="detail.errorMsg" style="color: red; margin-left: 8px">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.durationMs }}ms</el-descriptions-item>
        <el-descriptions-item label="链路 ID">
          <code>{{ detail.traceId }}</code>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { monitorApi } from '@/api/monitor'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = ref({ username: '', action: '', result: '', dateRange: null })
const detailVisible = ref(false)
const detail = ref({})

function resultType(r) {
  return { SUCCESS: 'success', FAILURE: 'danger', DENIED: 'warning' }[r] || ''
}

async function loadLogs() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
      ...filters.value
    }
    const res = await adminApi.getAuditLogs(params)
    logs.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    // mock 数据
    logs.value = Array.from({ length: 20 }, (_, i) => ({
      id: i + 1,
      createdAt: '2026-07-12 0' + (i % 9 + 1) + ':00:00',
      username: ['admin', 'user1', 'agent1'][i % 3],
      userIp: '192.168.1.' + (i + 1),
      action: ['LOGIN', 'AI_GENERATE', 'FILE_UPLOAD', 'EXPORT_DATA'][i % 4],
      resourceType: ['user', 'file', 'ai'][i % 3],
      resourceId: String(i + 1),
      method: 'POST',
      path: '/api/' + ['auth/login', 'ai/generate', 'multimodal/upload'][i % 3],
      requestBody: '{"key":"value"}',
      result: ['SUCCESS', 'SUCCESS', 'FAILURE', 'DENIED'][i % 4],
      durationMs: 50 + i * 10,
      traceId: 'trace-' + (i + 1)
    }))
    total.value = 200
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = { username: '', action: '', result: '', dateRange: null }
  loadLogs()
}

function showDetail(log) {
  detail.value = log
  detailVisible.value = true
}

async function exportData() {
  try {
    const res = await adminApi.exportAuditLogs(filters.value)
    const blob = new Blob([res.data], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit-${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => loadLogs())
</script>

<style scoped>
.audit {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
