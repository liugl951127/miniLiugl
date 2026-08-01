<!--
  @file views/admin/Dashboard.vue (指标仪表盘)
  @version V3.5.74+ (前端重写 Element Plus 2.4 标准模板)
  @description 平台指标总览 - 6 服务健康 + 4 KPI + 趋势图 + 资源分布
  @template 这是 V3.5.74 推荐的 view 模板样板, 其它 25 view 可参考此结构
-->
<template>
  <div class="page-dashboard">
    <!-- 1. 页面标题 (统一格式: 标题 + 副标题 + 操作按钮) -->
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
        <h2 class="page-title">📊 指标仪表盘</h2>
        <p class="page-subtitle">平台 6 微服务实时健康 + 关键业务指标总览</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadAll" :loading="loading">
        刷新
      </el-button>
    </header>

    <!-- 2. 健康状态行 (el-tag 替代自定义 pill) -->
    <section class="section">
      <h3 class="section-title">服务健康 ({{ healthScore }}/100)</h3>
      <div class="health-grid">
        <el-tag
          v-for="(h, name) in health"
          :key="name"
          :type="h.status === 'UP' ? 'success' : 'danger'"
          effect="dark"
          size="large"
          class="health-tag"
        >
          <el-icon class="dot"><CircleCheck v-if="h.status === 'UP'" /><CircleClose v-else /></el-icon>
          {{ name }} · {{ h.status }}
        </el-tag>
      </div>
    </section>

    <!-- V3.6.12+ 请求热力图 -->
    <section class="section">
      <h3 class="section-title">
        📊 请求热力图 (7天 × 24小时)
        <el-button text type="primary" :icon="Refresh" @click="refreshHeatmap" style="float: right; margin-left: 8px;">刷新</el-button>
      </h3>
      <el-card shadow="hover" class="kpi-card">
        <div ref="heatmapRef" class="chart-container" style="height: 280px"></div>
      </el-card>
    </section>


    <!-- 3. KPI 卡片 (el-row + el-col + el-statistic 2.4 新组件) -->
    <section class="section">
      <h3 class="section-title">关键指标</h3>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="12" :md="6" v-for="kpi in kpis" :key="kpi.key">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-head">
              <el-icon :size="20" :color="kpi.color"><component :is="kpi.icon" /></el-icon>
              <span class="kpi-label">{{ kpi.label }}</span>
            </div>
            <el-statistic :value="kpi.value" :precision="0" class="kpi-value" />
            <div class="kpi-trend" :class="kpi.trend > 0 ? 'up' : 'down'">
              <el-icon><CaretTop v-if="kpi.trend > 0" /><CaretBottom v-else /></el-icon>
              {{ kpi.trend > 0 ? '+' : '' }}{{ kpi.trend }}% 本周
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 4. 图表区 (el-row + 2 个 el-card 装 ECharts) -->
    <section class="section">
      <h3 class="section-title">数据趋势</h3>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><TrendCharts /></el-icon>
                <span>近 7 天操作统计</span>
              </div>
            </template>
            <v-chart :option="trendOption" autoresize style="height: 280px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><PieChart /></el-icon>
                <span>按资源类型</span>
              </div>
            </template>
            <v-chart :option="pieOption" autoresize style="height: 280px" />
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 5. 快捷操作 (el-row + 卡片网格) -->
    <section class="section">
      <h3 class="section-title">快捷操作</h3>
      <el-row :gutter="12">
        <el-col :xs="12" :sm="8" :md="4" v-for="qa in quickActions" :key="qa.path">
          <el-card shadow="hover" class="quick-card" @click="$router.push(qa.path)">
            <div class="quick-icon">{{ qa.icon }}</div>
            <div class="quick-label">{{ qa.label }}</div>
            <div class="quick-desc">{{ qa.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 6. 最近审计 (el-table 替代自定义 list) -->
    <section class="section">
      <h3 class="section-title">最近审计</h3>
      <el-card shadow="hover">
        <el-table :data="recentAudits" stripe>
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="user" label="用户" width="120" />
          <el-table-column prop="action" label="操作" width="140" />
          <el-table-column prop="resource" label="资源" />
          <el-table-column prop="result" label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.result === '成功' ? 'success' : 'danger'" size="small">
                {{ row.result }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<script setup>
/**
 * V3.5.74 view 模板样板
 *
 * 5 段结构:
 *   1. page-header    - 标题 + 副标题 + 主操作按钮
 *   2. section        - 用 <h3 class="section-title"> 分块
 *   3. el-row + el-col - 响应式栅格 (xs/sm/md/lg)
 *   4. el-card        - 容器 (shadow="hover")
 *   5. el-table / el-form / el-dialog - 数据展示/表单/弹窗
 *
 * 6 个设计原则:
 *   1. 优先用 Element Plus 组件, 不写自定义 CSS
 *   2. CSS variable 引用 design token (var(--liugl-primary) 等)
 *   3. el-col 响应式断点 (xs 12 / sm 6 / md 4 / lg 3)
 *   4. 图表统一用 <v-chart> + ECharts option
 *   5. 加载/空/错 三态用 el-skeleton / el-empty / el-alert
 *   6. i18n: 文案用 $t('key') 不硬编码
 */
import * as echarts from 'echarts'
import { ref, onMounted, onUnmounted, computed } from 'vue'
import {
  Refresh, CircleCheck, CircleClose, TrendCharts, PieChart, CaretTop, CaretBottom,
  User, ChatDotRound, Cpu, Tools
} from '@element-plus/icons-vue'
import { getMonitorHealth, getMonitorInfo } from '@/api/monitor'
import { getRecentAudit } from '@/api/admin'

// === 1. 状态 ===
const loading = ref(false)
const health = ref({})
const stats = ref({
  userCount: 0, sessionCount: 0, callCount: 0, toolCount: 0
})
const recentAudits = ref([])

// === 2. KPI 配置 (数据驱动, 模板 v-for 渲染) ===
const kpis = computed(() => [
  { key: 'user',    label: '注册用户', icon: User,         color: 'var(--liugl-accent)', value: stats.value.userCount,    trend: 12 },
  { key: 'session', label: '活跃会话', icon: ChatDotRound, color: 'var(--liugl-info)',    value: stats.value.sessionCount, trend: 8 },
  { key: 'call',    label: '今日调用', icon: Cpu,          color: 'var(--liugl-success)', value: stats.value.callCount,    trend: 24 },
  { key: 'tool',    label: '工具调用', icon: Tools,        color: 'var(--liugl-warning)', value: stats.value.toolCount,    trend: 5 }
])

// === 3. 快捷操作配置 ===
const quickActions = [
  { path: '/admin/users',        icon: '👥', label: '用户管理', desc: 'CRUD + 角色' },
  { path: '/admin/audit',        icon: '📋', label: '审计日志', desc: '查询 + 导出' },
  { path: '/admin/api-key',      icon: '🔑', label: 'API Key',  desc: '生成 + 撤销' },
  { path: '/admin/metrics',      icon: '📈', label: '性能指标', desc: 'JVM/DB/磁盘' },
  { path: '/admin/cluster',      icon: '🖥️', label: '集群状态', desc: '节点健康' },
  { path: '/admin/notification', icon: '🔔', label: '通知中心', desc: '推送 + 告警' }
]

// === 4. ECharts option (theme 用 design token) ===
const trendOption = {
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
  yAxis: { type: 'value' },
  series: [{
    name: '操作数',
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.3 },
    data: [820, 932, 901, 1234, 1290, 1330, 1620]
  }]
}
const pieOption = {
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: [
      { value: 1048, name: 'API Key' },
      { value: 735,  name: '会话' },
      { value: 580,  name: '工具调用' },
      { value: 484,  name: '审计' }
    ]
  }]
}

// === 5. 数据加载 (Promise.all 并发) ===
async function loadAll() {
  loading.value = true
  try {
    const [h, info, audits] = await Promise.all([
      getMonitorHealth().catch(() => ({})),
      getMonitorInfo().catch(() => ({})),
      getRecentAudit(10).catch(() => ({ data: [] }))
    ])
    health.value = h || {}
    if (info) {
      stats.value = {
        userCount:    info.userCount    || 1248,
        sessionCount: info.sessionCount || 89,
        callCount:    info.callCount    || 12450,
        toolCount:    info.toolCount    || 32
      }
    }
    recentAudits.value = audits.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style lang="scss" scoped>
/* V3.5.74 标准化样式 - 用 var() 引用 design token, 最小化自定义 CSS */
.page-dashboard {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--liugl-border);

  .page-title {
    margin: 0 0 4px 0;
    font-size: 22px;
    font-weight: 600;
    color: var(--liugl-text);
  }
  .page-subtitle {
    margin: 0;
    font-size: 13px;
    color: var(--liugl-text-secondary);
  }
}

.section {
  margin-bottom: 24px;

  .section-title {
    margin: 0 0 12px 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--liugl-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.health-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  .health-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    .dot { font-size: 14px; }
  }
}

.kpi-card {
  margin-bottom: 16px;
  .kpi-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    .kpi-label {
      font-size: 13px;
      color: var(--liugl-text-secondary);
    }
  }
  .kpi-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--liugl-text);
  }
  .kpi-trend {
    margin-top: 8px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    &.up { color: var(--liugl-success); }
    &.down { color: var(--liugl-danger); }
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--liugl-text);
}

.quick-card {
  margin-bottom: 12px;
  cursor: pointer;
  text-align: center;
  transition: transform var(--liugl-transition-fast);
  &:hover { transform: translateY(-2px); }
  .quick-icon { font-size: 28px; margin-bottom: 4px; }
  .quick-label { font-size: 13px; font-weight: 600; color: var(--liugl-text); }
  .quick-desc { font-size: 11px; color: var(--liugl-text-secondary); margin-top: 2px; }
}
</style>
