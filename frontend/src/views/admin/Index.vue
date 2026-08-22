<!-- @file admin/Index.vue - 管理后台 V6.8 (V6.8.10+ 企业级升级) -->
<template>
  <div class="page-card" v-loading="anyLoading">
    <div class="page-header">
      <h2>管理后台</h2>
      <el-button :icon="Refresh" :loading="loading || auditLoading" size="small" @click="refreshAll">刷新</el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:20px">
      <el-col v-for="s in stats" :key="s.label" :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-change" :class="s.up ? 'up' : 'down'">{{ s.change }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab 页 -->
    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <el-table
          :data="users"
          v-loading="loading"
          empty-text="暂无用户数据"
          stripe
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="role" label="角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'success'">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="160" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" :loading="togglingId === row.id" @click="toggleUser(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 运维统计 -->
      <el-tab-pane label="运维统计" name="stats">
        <div v-if="opsStats.length === 0" class="tab-empty">
          <EmptyState
            icon="DataAnalysis"
            title="暂无运维统计数据"
            description="后端暂未提供运维指标，请稍后再试"
            compact
          />
        </div>
        <div v-else class="stats-grid">
          <el-card v-for="item in opsStats" :key="item.label">
            <template #header>{{ item.label }}</template>
            <div class="big-num">{{ item.value }}</div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 审计日志 -->
      <el-tab-pane label="审计日志" name="audit">
        <el-table
          :data="auditLogs"
          v-loading="auditLoading"
          empty-text="暂无审计日志"
          stripe
        >
          <el-table-column prop="user" label="用户" width="120" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="timestamp" label="时间" width="160" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useToast } from '@/composables/useToast'
import { listAdminUsers, toggleAdminUser, getDashboard, getOpsStats, getRecentAudit } from '@/api/admin'
import EmptyState from '@/components/EmptyState.vue'

const activeTab = ref('users')
const users = ref([])
const stats = ref([])
const opsStats = ref([])
const auditLogs = ref([])
const loading = ref(false)
const auditLoading = ref(false)
const togglingId = ref(null)
const toast = useToast()

const anyLoading = computed(() => loading.value || auditLoading.value)

async function loadUsers() {
  loading.value = true
  try {
    const r = await listAdminUsers()
    users.value = r.data?.list || r.data || []
  } catch (e) {
    users.value = []
    toast.error('加载用户列表失败: ' + (e?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

async function loadDashboard() {
  try {
    const r = await getDashboard()
    const d = r.data || {}
    stats.value = [
      { label: '总用户', value: d.totalUsers || 0, change: '+12%', up: true },
      { label: '今日登录', value: d.todayLogins || 0, change: '实时', up: true },
      { label: '活跃会话', value: d.activeSessions || 0, change: '+8%', up: true },
      { label: 'API 调用', value: d.apiCalls || 0, change: '+23%', up: true },
      { label: '错误率', value: d.errorRate || '0%', change: '-5%', up: false },
    ]
    opsStats.value = d.opsStats || []
  } catch {
    stats.value = [
      { label: '总用户', value: 0, change: '-', up: true },
      { label: '今日登录', value: 0, change: '-', up: true },
      { label: '活跃会话', value: 0, change: '-', up: true },
      { label: 'API 调用', value: 0, change: '-', up: true },
      { label: '错误率', value: '0%', change: '-', up: false },
    ]
    opsStats.value = []
  }
}

async function loadAudit() {
  auditLoading.value = true
  try {
    const r = await getRecentAudit()
    auditLogs.value = r.data || []
  } catch (e) {
    auditLogs.value = []
    toast.error('加载审计日志失败: ' + (e?.message || '网络错误'))
  } finally {
    auditLoading.value = false
  }
}

async function toggleUser(u) {
  togglingId.value = u.id
  try {
    await toggleAdminUser(u.id, u.enabled ? false : true)
    toast.success(u.enabled ? '已禁用' : '已启用')
    await loadUsers()
  } catch (e) {
    toast.error('操作失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    togglingId.value = null
  }
}

function refreshAll() {
  loadUsers()
  loadDashboard()
  loadAudit()
}

onMounted(() => { loadUsers(); loadDashboard() })
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.stat-card { text-align: center; }
.stat-label { font-size: 12px; color: #999; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1e40af; }
.stat-change { font-size: 11px; margin-top: 4px; &.up { color: #10b981; } &.down { color: #ef4444; } }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.big-num { font-size: 32px; font-weight: 700; color: #1e40af; text-align: center; }
.tab-empty { padding: 40px 0; display: flex; justify-content: center; }
</style>
