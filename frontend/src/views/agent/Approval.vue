<!--
  @file agent/Approval.vue - Skill 审批页 (V7.6)
  路由: /agent/approval
-->
<template>
  <div class="approval-page">
    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="状态" size="default" clearable style="width: 140px" @change="loadList">
        <el-option label="待审批" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="username" label="申请人" width="120" />
      <el-table-column prop="toolName" label="工具" width="180" show-overflow-tooltip />
      <el-table-column label="风险" width="90">
        <template #default="{ row }">
          <el-tag :type="riskType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="goal" label="执行目的" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="申请时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" link type="success" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" link type="danger" @click="reject(row)">拒绝</el-button>
          <span v-else class="muted">{{ row.status === 'APPROVED' ? '已处理' : '已拒绝' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState
      v-if="!loading && list.length === 0"
      title="暂无审批请求"
      description="Skill 执行时高风险操作会进入这里审批"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { agentApi } from '@/api/agent'

const list = ref([])
const loading = ref(false)
const statusFilter = ref('PENDING')

function riskType(r) {
  return { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'info', LOW: 'success' }[r] || 'info'
}
function statusType(s) {
  return { APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning' }[s] || 'info'
}
function statusLabel(s) {
  return { APPROVED: '已通过', REJECTED: '已拒绝', PENDING: '待审批' }[s] || s
}

async function loadList() {
  loading.value = true
  try {
    const res = await agentApi.listApprovals({ status: statusFilter.value })
    if (res.code === 0) list.value = res.data?.list || res.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function approve(row) {
  try {
    await agentApi.approveRequest(row.id)
    ElMessage.success('已通过')
    loadList()
  } catch (e) { ElMessage.error('操作失败') }
}

async function reject(row) {
  try {
    await agentApi.rejectRequest(row.id)
    ElMessage.success('已拒绝')
    loadList()
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(loadList)
</script>

<style scoped>
.approval-page { background: white; border-radius: 12px; padding: 16px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.muted { color: #94a3b8; font-size: 0.85em; }
</style>
