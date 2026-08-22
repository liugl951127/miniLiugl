<!--
  @file WechatScanPageV2.vue - V6.8.10+ 扫码状态反馈强化版
  @description 微信扫码管理 - 加扫码状态反馈 / v-loading / empty
-->
<template>
  <PageStandard title="📱 微信扫码" subtitle="扫码登录 / 公众号绑定 / UnionID 识别">
    <template #actions>
      <el-button :icon="Refresh" :loading="table.loading.value" @click="table.refresh">刷新</el-button>
    </template>

    <!-- 状态统计 (V6.8.10+ 加扫码状态分类) -->
    <StatCardGroup :stats="kpiStats" :loading="table.loading.value" />

    <!-- 扫码状态实时反馈 (V6.8.10+ 新增) -->
    <section class="section">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="6">
          <el-card shadow="never" class="status-card status-pending">
            <div class="status-label">
              <el-icon><Clock /></el-icon> 待扫码
            </div>
            <div class="status-value">{{ scanStatus.pending }}</div>
            <div class="status-hint">用户已请求，尚未扫描二维码</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card shadow="never" class="status-card status-scanned">
            <div class="status-label">
              <el-icon><View /></el-icon> 已扫码
            </div>
            <div class="status-value">{{ scanStatus.scanned }}</div>
            <div class="status-hint">等待用户确认登录</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card shadow="never" class="status-card status-success">
            <div class="status-label">
              <el-icon><CircleCheckFilled /></el-icon> 已登录
            </div>
            <div class="status-value">{{ scanStatus.success }}</div>
            <div class="status-hint">扫码登录成功</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card shadow="never" class="status-card status-expired">
            <div class="status-label">
              <el-icon><CircleCloseFilled /></el-icon> 已过期
            </div>
            <div class="status-value">{{ scanStatus.expired }}</div>
            <div class="status-hint">二维码已失效</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 扫码记录列表 -->
    <section class="section">
      <h3 class="section-title">📋 扫码记录</h3>
      <CrudTable
        :table="table"
        :columns="columns"
        @action="onAction"
      />
    </section>
  </PageStandard>
</template>

<script setup>
/**
 * V6.8.10+ 升级: 加扫码状态分类卡片 / 视觉化反馈
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Refresh, Clock, View, CircleCheckFilled, CircleCloseFilled, ChatDotRound } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { usePageSetup } from '@/composables/usePageSetup'
import { useToast } from '@/composables/useToast'
import http from '@/api/http'

usePageSetup({ title: '微信扫码' })
const toast = useToast()

const table = useTable({
  fetcher: async (params) => await http.get('/auth/wechat-scan', { params }),
  defaultPageSize: 20,
})

// 扫码状态 (V6.8.10+)
const scanStatus = ref({ pending: 0, scanned: 0, success: 0, expired: 0 })
let pollTimer = null

async function loadScanStatus() {
  try {
    const res = await http.get('/auth/wechat-scan/status')
    const data = res.data?.data ?? res.data ?? {}
    scanStatus.value = {
      pending: data.pending ?? 0,
      scanned: data.scanned ?? 0,
      success: data.success ?? 0,
      expired: data.expired ?? 0
    }
  } catch (e) {
    // 静默失败，保留默认 0
  }
}

const kpiStats = computed(() => [
  { key: 'total', label: '扫码总数', value: table.total.value, icon: ChatDotRound, color: '#409eff',
    tip: '历史扫码记录总数' },
])

const columns = [
  { prop: 'id', label: 'ID', width: 80, type: 'number' },
  { prop: 'name', label: '名称', minWidth: 200, sortable: true },
  { prop: 'status', label: '状态', width: 120,
    render: (row) => statusTag(row.status)
  },
  { prop: 'createdAt', label: '创建时间', type: 'time', width: 160 },
  { prop: 'actions', label: '操作', type: 'actions', width: 160, fixed: 'right',
    actions: [
      { label: '查看', icon: 'View', event: 'view' },
      { label: '复制', icon: 'Copy', event: 'copy' }
    ]
  },
]

function statusTag(status) {
  const map = {
    pending:  { text: '待扫码', type: 'info' },
    scanned:  { text: '已扫码', type: 'warning' },
    success:  { text: '已登录', type: 'success' },
    expired:  { text: '已过期', type: 'danger' }
  }
  const item = map[status] || { text: status || '未知', type: '' }
  return { type: 'tag', tagType: item.type, text: item.text }
}

function onAction({ event, row }) {
  if (event === 'view') {
    toast.info(`查看扫码记录: ${row.name || row.id}`)
  } else if (event === 'copy') {
    const text = row.name || row.id || ''
    if (!text) {
      toast.warning('无可复制内容')
      return
    }
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(String(text))
        .then(() => toast.success('已复制到剪贴板'))
        .catch(() => toast.error('复制失败，请手动复制'))
    } else {
      toast.error('当前浏览器不支持剪贴板 API')
    }
  } else {
    toast.info(`${event}: ${row.name || row.id}`)
  }
}

onMounted(() => {
  loadScanStatus()
  // 30s 轮询扫码状态
  pollTimer = setInterval(loadScanStatus, 30_000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.section { margin-top: 16px; }
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
}

/* 状态卡 */
.status-card {
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.2s;
  text-align: center;
  padding: 4px 0;
}
.status-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.status-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.status-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.status-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}
.status-pending .status-value { color: var(--el-text-color-secondary); }
.status-scanned .status-value { color: var(--el-color-warning); }
.status-success .status-value { color: var(--el-color-success); }
.status-expired .status-value { color: var(--el-color-danger); }
</style>
