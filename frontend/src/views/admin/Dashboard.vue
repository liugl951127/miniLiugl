<!--
  @file views/admin/DashboardV2.vue - V6.8.2+ 重构版
  @description 综合管理仪表盘
    - 原 603 行 -> V2 180 行 (-70%)
-->
<template>
  <PageStandard
    :title="`📊 ${greeting}, ${userName}`"
    :subtitle="`${todayText} · ${weekDay}`"
  >
    <template #actions>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </template>

    <AITip :context="tipContext" dismiss-key="admin-dashboard-tip" />

    <!-- 4 核心指标 -->
    <StatCardGroup :stats="kpiStats" :loading="loading" />

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover">
          <template #header>
            <el-tooltip content="过去 24 小时每小时 API 调用总量，反映系统负载峰值时段" placement="top">
              <span>📈 流量趋势 (24h) <el-icon style="cursor:pointer;vertical-align:middle"><InfoFilled /></el-icon></span>
            </el-tooltip>
          </template>
          <v-chart :option="trendOption" autoresize style="height: 240px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover">
          <template #header>
            <el-tooltip content="各微服务健康状态，接入 Nacos/K8s 后可实时感知服务可用性" placement="top">
              <span>🟢 服务状态 <el-icon style="cursor:pointer;vertical-align:middle"><InfoFilled /></el-icon></span>
            </el-tooltip>
          </template>
          <div class="service-grid">
            <el-tooltip v-for="s in services" :key="s.name" :content="`服务: ${s.name} | 状态: ${s.status}`" placement="top">
              <div class="service-item" :class="s.status">
                <span class="dot"></span>
                <span class="name">{{ s.name }}</span>
                <el-tag :type="s.status === 'UP' ? 'success' : s.status === 'DOWN' ? 'danger' : 'info'" size="small">{{ s.status }}</el-tag>
              </div>
            </el-tooltip>
            <div v-if="services.length === 0" class="service-empty">暂无服务数据</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <section class="quick-section">
      <h3 class="section-title">🚀 快捷入口</h3>
      <div class="quick-grid">
        <div v-for="q in quickEntries" :key="q.path" class="quick-card" @click="$router.push(q.path)">
          <el-tooltip :content="q.tip" placement="top">
            <div style="display:contents">
              <el-icon :size="28" :color="q.color"><component :is="q.icon" /></el-icon>
              <div class="quick-info">
                <div class="quick-label">{{ q.label }}</div>
                <div class="quick-desc">{{ q.desc }}</div>
              </div>
            </div>
          </el-tooltip>
        </div>
      </div>
    </section>

    <!-- 最近活动 -->
    <section class="activity-section">
      <h3 class="section-title">📜 最近活动</h3>
      <el-table :data="activityTable.data.value" v-loading="activityTable.loading.value" stripe size="small">
        <el-table-column prop="createdAt" label="时间" width="140">
          <template #default="{ row }">
            <el-tooltip :content="row.createdAt + '（北京时间）'" placement="top">
              <span>{{ row.createdAt }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="100">
          <template #default="{ row }">
            <el-tooltip :content="row.username + '（执行该操作的用户账号）'" placement="top">
              <span>{{ row.username }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tooltip :content="row.action + '（操作类型）'" placement="top">
              <el-tag size="small" type="primary">{{ row.action }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="resource" label="资源" min-width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.resource + '（被操作的目标对象）'" placement="top">
              <span class="resource-cell">{{ row.resource }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="120">
          <template #default="{ row }">
            <el-tooltip :content="row.ip + '（请求来源 IP）'" placement="top">
              <code style="font-size:11px">{{ row.ip }}</code>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </PageStandard>
</template>

<script setup>
/**
 * V6.8.2+ 重构 - Dashboard
 * 原 603 行 -> V2 180 行 (-70%)
 */

import { ref, computed, onMounted } from 'vue'
import { Refresh, Cpu, User, ChatDotRound, Promotion, Monitor, DataAnalysis, Setting, Tools, Document, Connection, InfoFilled } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { usePageSetup } from '@/composables/usePageSetup'
import http from '@/api/http'
import { useUserStore } from '@/store/user'

usePageSetup({ title: '管理仪表盘' })
const userStore = useUserStore()

// 1. 问候
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const userName = computed(() => userStore.profile?.nickname || userStore.profile?.username || '管理员')
const todayText = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
const weekDay = ['日', '一', '二', '三', '四', '五', '六'][new Date().getDay()]

// 2. 数据
const loading = ref(false)
const stats = ref({ users: 0, sessions: 0, calls: 0, models: 0 })
const services = ref([])

// 3. 加载
async function loadAll() {
  loading.value = true
  try {
    const [dashRes, hrRes, svcRes] = await Promise.all([
      http.get('/admin/stats/dashboard').catch(() => ({ data: {} })),
      http.get('/admin/stats/hourly').catch(() => ({ data: [] })),
      http.get('/admin/stats/services').catch(() => ({ data: [] })),
    ])
    const d = dashRes.data?.data ?? dashRes.data ?? {}
    Object.assign(stats.value, d)
    // 趋势图真实化
    const hrs = hrRes.data?.data ?? hrRes.data ?? []
    if (hrs.length > 0) {
      trendOption.value.xAxis.data = hrs.map(h => h.hour)
      trendOption.value.series[0].data = hrs.map(h => h.calls)
    }
    // 服务状态真实化
    const svcs = svcRes.data?.data ?? svcRes.data ?? []
    if (svcs.length > 0) {
      services.value = svcs
    }
  } catch (e) {
    console.warn('[Dashboard] 部分数据加载失败:', e.message)
  } finally {
    loading.value = false
  }
}
onMounted(loadAll)

// 4. KPI
const kpiStats = computed(() => [
  { key: 'users', label: '注册用户', value: stats.value.users, icon: User, color: '#409eff', bgColor: 'rgba(64, 158, 255, 0.1)', tip: '平台累计注册用户数，包含已激活和未激活账号' },
  { key: 'sessions', label: '今日会话', value: stats.value.sessions, icon: ChatDotRound, color: '#67c23a', bgColor: 'rgba(103, 194, 58, 0.1)', tip: '当日创建的所有对话会话数，含活跃和已结束的会话' },
  { key: 'calls', label: 'API 调用', value: stats.value.calls, icon: Promotion, color: '#e6a23c', bgColor: 'rgba(230, 162, 60, 0.1)', tip: '当日平台 API 总调用次数，包含成功和失败的请求' },
  { key: 'models', label: '在线模型', value: stats.value.models, icon: Cpu, color: '#909399', bgColor: 'rgba(156, 154, 181, 0.1)', tip: '当前在线且可调用的模型数量，灰色表示部分模型不可用' },
])

// 5. 趋势图
const trendOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: [], smooth: true, areaStyle: { opacity: 0.3 } }],
})

// 6. 快捷入口
const quickEntries = [
  { path: '/admin/users', label: '用户管理', desc: '管理用户与角色', icon: User, color: '#409eff', tip: '管理平台所有用户账号、角色权限与访问策略' },
  { path: '/admin/provider', label: '模型管理', desc: 'OpenAI / Gemini', icon: Cpu, color: '#67c23a', tip: '配置第三方模型 API Key、端点及用量限制' },
  { path: '/admin/monitor', label: '系统监控', desc: '健康/告警/指标', icon: Monitor, color: '#e6a23c', tip: '查看服务器状态、告警日志与实时性能指标' },
  { path: '/admin/audit', label: '审计日志', desc: '操作记录', icon: Document, color: '#f56c6c', tip: '追踪所有用户的操作行为与系统变更记录' },
  { path: '/admin/leaderboard', label: '排行榜', desc: '模型评分', icon: Tools, color: '#9c27b0', tip: '对比各模型准确率、响应速度与用户满意度评分' },
  { path: '/admin/governance', label: '治理', desc: '合规/审计', icon: DataAnalysis, color: '#607d8b', tip: '数据合规检查、权限审计与策略配置' },
]

// 7. 最近活动
const activityTable = useTable({
  fetcher: async (params) => await http.get('/admin/audit/recent', { params: { ...params, limit: 10 } }),
  showPagination: false,
})

// 8. 提示
const tipContext = computed(() => `当前 ${stats.value.users} 用户 / ${stats.value.calls} 调用`)
</script>

<style scoped>
.quick-section, .activity-section {
  margin-top: 16px;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.section-title { font-size: 16px; margin: 0 0 12px; color: #1e293b; }
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.quick-info { flex: 1; }
.quick-label { font-size: 14px; font-weight: 500; color: #1e293b; }
.quick-desc { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.service-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.service-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.service-item .dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #67c23a;
}
.service-item.DOWN .dot { background: #f56c6b; }
.service-item .name { flex: 1; font-size: 13px; }
.resource-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.service-empty { text-align: center; color: #94a3b8; font-size: 13px; padding: 16px; }
</style>
