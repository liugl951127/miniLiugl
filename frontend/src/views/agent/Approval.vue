<!--
  @file Approval.vue - Skill 审批管理 (V6.8.2+)
  @description HIGH/CRITICAL 工具执行审批列表
-->
<template>
  <PageStandard title="⚖️ Skill 审批" subtitle="HIGH / CRITICAL 工具执行审批">
    <template #actions>
      <el-button :icon="Refresh" @click="loadPending">刷新</el-button>
    </template>

    <!-- KPI -->
    <StatCardGroup :stats="kpiStats" />

    <el-tabs v-model="activeTab" class="mt-4">
      <!-- 待审批 -->
      <el-tab-pane label="⏳ 待审批" name="pending">
        <div v-loading="pendingTable.loading.value">
          <CrudTable
            :table="pendingTable"
            :columns="columns"
            @action="onPendingAction"
          />
          <el-empty
            v-if="!pendingTable.loading.value && !(pendingTable.data.value || []).length"
            description="暂无待审批请求"
            :image-size="100"
            style="padding: 32px 0"
          />
        </div>
      </el-tab-pane>

      <!-- 审批历史 -->
      <el-tab-pane label="📋 审批历史" name="history">
        <div v-loading="historyTable.loading.value">
          <CrudTable
            :table="historyTable"
            :columns="historyColumns"
            @action="onHistoryAction"
          />
          <el-empty
            v-if="!historyTable.loading.value && !(historyTable.data.value || []).length"
            description="暂无审批历史"
            :image-size="100"
            style="padding: 32px 0"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 详情 Dialog -->
    <el-dialog v-model="detailVisible" title="审批详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="申请人">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">
          <el-tag :type="riskType(detail.riskLevel)" size="small">{{ detail.riskLevel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工具名称" :span="2">{{ detail.toolName }}</el-descriptions-item>
        <el-descriptions-item label="任务ID" :span="2">{{ detail.taskId }}</el-descriptions-item>
        <el-descriptions-item label="执行目的" :span="2">{{ detail.goal }}</el-descriptions-item>
        <el-descriptions-item label="工具参数" :span="2">
          <pre class="code-block">{{ detail.toolParams }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ detail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        <template v-if="detail.approverName">
          <el-descriptions-item label="审批人">{{ detail.approverName }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ fmtTime(detail.approvedAt) }}</el-descriptions-item>
          <el-descriptions-item label="审批理由" :span="2">{{ detail.approvalReason }}</el-descriptions-item>
        </template>
      </el-descriptions>
    </el-dialog>
  </PageStandard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageSetup } from '@/composables/usePageSetup'
import http from '@/api/http'

usePageSetup({ title: 'Skill 审批' })

const activeTab = ref('pending')
const detailVisible = ref(false)
const detail = ref(null)

// ==================== 列表 ====================

const pendingTable = useTable2({
  fetcher: async () => {
    const r = await http.get('/skill-approval/pending/all')
    return { data: r.data || [], total: (r.data || []).length }
  },
})

const historyTable = useTable2({
  fetcher: async (params) => {
    const r = await http.get('/skill-approval/history', { params: { page: 1, size: 50 } })
    return { data: r.data || [], total: (r.data || []).length }
  },
})

const columns = [
  { prop: 'id', label: 'ID', width: 70, type: 'number' },
  { prop: 'username', label: '申请人', width: 140 },
  { prop: 'toolName', label: '工具', width: 180 },
  { prop: 'riskLevel', label: '风险', width: 100, type: 'tag' },
  { prop: 'goal', label: '执行目的', minWidth: 200, showOverflowTooltip: true },
  { prop: 'createdAt', label: '提交时间', width: 170, type: 'time' },
  { prop: 'actions', label: '操作', width: 200, type: 'actions', fixed: 'right',
    actions: [
      { label: '查看', icon: 'View', event: 'view' },
      { label: '通过', icon: 'Check', event: 'approve', type: 'success' },
      { label: '拒绝', icon: 'Close', event: 'reject', type: 'danger' },
    ]
  }
]

const historyColumns = [
  { prop: 'id', label: 'ID', width: 70, type: 'number' },
  { prop: 'username', label: '申请人', width: 140 },
  { prop: 'toolName', label: '工具', width: 180 },
  { prop: 'riskLevel', label: '风险', width: 100, type: 'tag' },
  { prop: 'status', label: '结果', width: 90, type: 'tag' },
  { prop: 'approverName', label: '审批人', width: 120 },
  { prop: 'createdAt', label: '提交时间', width: 170, type: 'time' },
  { prop: 'approvedAt', label: '审批时间', width: 170, type: 'time' },
  { prop: 'actions', label: '操作', width: 80, type: 'actions', fixed: 'right',
    actions: [
      { label: '查看', icon: 'View', event: 'view' },
    ]
  }
]

// ==================== KPI ====================

const kpiStats = computed(() => {
  const pending = (pendingTable.data.value || []).length
  return [
    { key: 'pending', label: '待审批', value: pending, icon: 'Clock', color: '#e6a23c' },
    { key: 'history', label: '历史记录', value: (historyTable.data.value || []).length, icon: 'Document', color: '#409eff' },
  ]
})

// ==================== 动作 ====================

async function onPendingAction({ row, event }) {
  if (event === 'view') { showDetail(row); return }
  if (event === 'approve') { await handleApprove(row); return }
  if (event === 'reject') { await handleReject(row); return }
}

async function onHistoryAction({ row, event }) {
  if (event === 'view') { showDetail(row); return }
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function handleApprove(row) {
  try {
    // 先弹确认框
    await ElMessageBox.confirm(
      `确认批准「${row.toolName}」(${row.riskLevel}) 的执行请求？`,
      '审批通过',
      { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    return
  }
  try {
    await http.post(`/skill-approval/${row.id}/approve`, { reason: '' })
    ElMessage.success('已审批通过')
    await loadPending()
    activeTab.value = 'history'
  } catch (e) {
    ElMessage.error('审批失败：' + (e?.message || '网络错误'))
  }
}

async function handleReject(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝理由', '审批拒绝', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
    })
    if (!value?.trim()) {
      ElMessage.warning('请输入拒绝理由')
      return
    }
    reason = value.trim()
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    return
  }
  try {
    await http.post(`/skill-approval/${row.id}/reject`, { reason: reason || '拒绝执行' })
    ElMessage.success('已拒绝')
    await loadPending()
  } catch (e) {
    ElMessage.error('拒绝失败：' + (e?.message || '网络错误'))
  }
}

async function loadPending() {
  await pendingTable.refresh()
  await historyTable.refresh()
}

// ==================== Utils ====================

function statusType(s) {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  if (s === 'PENDING') return 'warning'
  return 'info'
}

function riskType(r) {
  if (r === 'CRITICAL') return 'danger'
  if (r === 'HIGH') return 'warning'
  return 'info'
}

function fmtTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

// ==================== useTable2 (简化版) ====================

function useTable2({ fetcher }) {
  const data = ref([])
  const total = ref(0)
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    try {
      const r = await fetcher()
      data.value = r.data || []
      total.value = r.total || 0
    } finally {
      loading.value = false
    }
  }

  onMounted(() => refresh())
  return { data, total, loading, refresh }
}
</script>

<style scoped>
.mt-4 { margin-top: 16px; }
.code-block {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
