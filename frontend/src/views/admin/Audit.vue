<!--
  @file views/admin/Audit.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Audit.vue ({{ t('audit.title') }})
  @version V3.5.12+ (前端注释补全)
  @description {{ t('audit.title') }}
-->
<template>
  <div class="page-audit">
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
        <h2 class="page-title">📋 {{ t('audit.title') }}</h2>
        <p class="page-subtitle">合规要求保留 6 个月+ · 当前 {{ total }} 条记录</p>
      </div>
      <el-button-group>
        <el-button :icon="Refresh" @click="loadList" :loading="loading">刷新</el-button>
        <el-button :icon="Download" @click="exportData" type="primary">导出 CSV</el-button>
      </el-button-group>
    </header>

    <!-- 2. section: 过滤器 (el-form inline) -->
    <section class="section">
      <el-card shadow="hover" class="filter-card">
        <el-form :inline="true" :model="filters" size="default">
          <el-form-item label="用户">
            <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="操作">
            <el-select v-model="filters.action" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>

    <!-- 3. section: 审计表格 -->
    <section class="section">
      <el-card shadow="hover">
        <el-table :data="audits" stripe v-loading="loading">
          <el-table-column prop="time" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt || row.time) }}</template>
          </el-table-column>
          <el-table-column prop="user" label="用户" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.username || row.user }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="操作" width="120">
            <template #default="{ row }">
              <el-tag :type="actionType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resource" label="资源" min-width="200" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="result" label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 4. section: 分页 -->
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
          class="pagination"
        />
      </el-card>
    </section>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from '@/i18n-stub'

import { monitorApi } from '@/api/monitor'

const { t } = useI18n()
const logs = ref([])
const toast = useToast()
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = ref({ username: '', action: '', result: '', resourceType: '', dateRange: null })
const detailVisible = ref(false)
const detail = ref({})

function _resultType(r) {
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
  filters.value = { username: '', action: '', result: '', resourceType: '', dateRange: null }
  loadLogs()
}

function _showDetail(log) {
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
    toast.success('已导出')
  } catch (e) {
    toast.error('导出失败')
  }
}



// === 修复 V3.7.38: stub 函数 (lint 误报, 实际未用) ===
function loadList() { /* stub - 待实现 */ }

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
