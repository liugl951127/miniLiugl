<!--
  @file settings/Audit.vue - 审计日志 + 运维统计 (V8.0)
  路由: /settings/audit
-->
<template>
  <div>
    <div class="page-header">
      <h3>📋 审计与运维</h3>
    </div>

    <el-tabs v-model="activeSection" class="section-tabs">
      <el-tab-pane label="审计日志" name="audit">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;gap:8px">
              <el-input v-model="auditSearch" size="small" placeholder="搜索用户/操作" clearable style="max-width:240px" />
              <el-select v-model="auditLevel" size="small" placeholder="级别" clearable style="width:120px">
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
              </el-select>
              <el-button size="small" :icon="Refresh" @click="loadAudit" :loading="auditLoading">刷新</el-button>
            </div>
          </template>
          <el-table :data="filteredAudit" stripe v-loading="auditLoading" max-height="500">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户" min-width="120" />
            <el-table-column prop="action" label="操作" min-width="160" />
            <el-table-column prop="resource" label="资源" min-width="140" />
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="row.level === 'ERROR' ? 'danger' : row.level === 'WARN' ? 'warning' : 'info'" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" min-width="140" />
            <el-table-column prop="createdAt" label="时间" min-width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="运维统计" name="stats">
        <el-row :gutter="12" style="margin-bottom:12px">
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">总调用</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-primary)">{{ stats.totalCalls || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">成功率</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-success)">{{ stats.successRate || 0 }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">平均延迟</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-warning)">{{ stats.avgLatency || 0 }}ms</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">活跃用户</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-info)">{{ stats.activeUsers || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>
        <el-card>
          <template #header><span>运维指标</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="服务数">{{ stats.serviceCount || 0 }} 个微服务</el-descriptions-item>
            <el-descriptions-item label="数据库">{{ stats.dbConnections || 0 }} 连接</el-descriptions-item>
            <el-descriptions-item label="Redis">{{ stats.redisUsed || 0 }} MB / {{ stats.redisTotal || 0 }} MB</el-descriptions-item>
            <el-descriptions-item label="磁盘">{{ stats.diskUsed || 0 }}% 已用</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getRecentAudit, getOpsStats } from '@/api/admin'

const activeSection = ref('audit')
const audit = ref([])
const auditLoading = ref(false)
const auditSearch = ref('')
const auditLevel = ref('')

const filteredAudit = computed(() => {
  let list = audit.value
  if (auditLevel.value) list = list.filter(a => a.level === auditLevel.value)
  const k = auditSearch.value
  if (k) list = list.filter(a =>
    (a.username && a.username.includes(k)) || (a.action && a.action.includes(k))
  )
  return list
})

const stats = ref({})

async function loadAudit() {
  auditLoading.value = true
  try {
    const res = await getRecentAudit()
    audit.value = res.data?.data ?? res.data ?? res ?? []
  } catch (e) { audit.value = [] }
  finally { auditLoading.value = false }
}

async function loadStats() {
  try {
    const res = await getOpsStats()
    stats.value = res.data?.data ?? res.data ?? res ?? {}
  } catch (e) { stats.value = {} }
}

onMounted(() => { loadAudit(); loadStats() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
.section-tabs { background: white; padding: 8px; border-radius: 8px; }
</style>
