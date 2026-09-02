<!--
  @file monitor/Alerts.vue - 告警管理 (V7.9, Day 59)
  路由: /monitor/alerts
  合并: 活跃告警 + 历史告警 (原 2 个 tab)
  Day 55 新增: RCA 根因分析详情抽屉 (category / cause / suggestedActions / historicalKnowledge)
  Day 57 新增: 知识库 Tab「触发 RCA」按钮（点击条目自动触发同类告警 RCA 分析）
  Day 59 新增: 已保存 RCA Tab — 查看/删除已保存的 RCA 知识条目
-->
<template>
  <div class="alerts-page">
    <el-tabs v-model="activeTab" class="alerts-tabs">
      <!-- 活跃告警 -->
      <el-tab-pane label="活跃告警" name="active">
        <div class="toolbar">
          <el-input
            v-model="activeFilter.keyword"
            placeholder="搜索告警"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="loadActive"
          />
          <el-select v-model="activeFilter.level" placeholder="级别" size="default" clearable style="width: 120px" @change="loadActive">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
          <el-button type="primary" :icon="Refresh" @click="loadActive">刷新</el-button>
        </div>
        <el-table :data="activeAlerts" v-loading="loadingActive" stripe>
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)" size="small">{{ getLevelLabel(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="source" label="来源" width="120" />
          <el-table-column prop="firedAt" label="触发时间" width="180" />
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openRcaDrawer(row)">根因分析</el-button>
              <el-button size="small" link type="warning" @click="ackAlert(row)">确认</el-button>
              <el-button size="small" link type="success" @click="resolveAlert(row)">解决</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState
          v-if="!loadingActive && activeAlerts.length === 0"
          title="当前无活跃告警"
          description="系统运行正常, 持续关注"
          compact
        />
      </el-tab-pane>

      <!-- 知识库 (Day 56) -->
      <el-tab-pane label="知识库" name="knowledge">
        <div class="toolbar">
          <el-input
            v-model="kbFilter.metricName"
            placeholder="搜索指标名称"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="loadKbKnowledge"
          />
          <el-select v-model="kbFilter.severity" placeholder="级别" size="default" clearable style="width: 120px" @change="loadKbKnowledge">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
          <el-select v-model="kbFilter.days" placeholder="时间范围" size="default" style="width: 130px" @change="loadKbKnowledge">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 30 天" :value="30" />
            <el-option label="近 90 天" :value="90" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadKbKnowledge">查询</el-button>
          <el-button :icon="Refresh" @click="loadKbSummary">知识摘要</el-button>
        </div>

        <!-- 知识摘要卡片 (Day 56) -->
        <div v-if="kbSummary" class="kb-summary-cards">
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ kbSummary.totalCount || 0 }}</div>
            <div class="kb-summary-label">历史经验总数</div>
          </div>
          <div class="kb-summary-card">
            <div class="kb-summary-num" :style="{ color: getLevelColor(kbSummary.topSeverity) }">{{ kbSummary.topSeverity || '-' }}</div>
            <div class="kb-summary-label">高频级别</div>
          </div>
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ kbSummary.avgDuration ? formatDuration(kbSummary.avgDuration) : '-' }}</div>
            <div class="kb-summary-label">平均恢复时长</div>
          </div>
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ kbSummary.topMetric || '-' }}</div>
            <div class="kb-summary-label">高频指标</div>
          </div>
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ kbSummary.resolvedCount || 0 }}</div>
            <div class="kb-summary-label">已解决</div>
          </div>
        </div>

        <!-- 知识库列表 -->
        <el-table :data="kbEntries" v-loading="kbLoading" stripe style="margin-top:12px" empty-text="暂无知识库记录">
          <el-table-column prop="severity" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.severity)" size="small">{{ getLevelLabel(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="metricName" label="指标名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="ruleName" label="规则名" min-width="120" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'resolved' ? 'success' : row.status === 'acked' ? 'warning' : 'info'" size="small">
                {{ { resolved: '已解决', acked: '已确认', firing: '触发中' }[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="持续时长" width="100">
            <template #default="{ row }"><span>{{ row.duration ? formatDuration(row.duration) : '-' }}</span></template>
          </el-table-column>
          <el-table-column prop="firedAt" label="触发时间" width="170" />
          <el-table-column prop="resolvedAt" label="解决时间" width="170" />
          <el-table-column prop="resolvedBy" label="处理人" width="90" show-overflow-tooltip />
          <el-table-column prop="notes" label="备注" min-width="160" show-overflow-tooltip />
          <!-- Day 57: 操作列 - 触发 RCA -->
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" link type="primary" :loading="rcaFromKbLoading === row.metricName" @click="triggerRcaFromKb(row)">
                触发 RCA
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="kbTotal > 20"
          v-model:current-page="kbPage"
          :total="kbTotal"
          :page-size="20"
          layout="total, prev, pager, next"
          @current-change="loadKbKnowledge"
          style="margin-top: 12px; justify-content: flex-end; display: flex"
        />
      </el-tab-pane>

      <!-- Day 59: RCA 知识库管理 Tab（查看/删除已保存的 RCA 知识条目） -->
      <el-tab-pane label="已保存 RCA" name="savedRca">
        <div class="toolbar">
          <el-input
            v-model="savedRcaFilter.metricName"
            placeholder="搜索指标名称"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="loadSavedRca"
          />
          <el-select v-model="savedRcaFilter.severity" placeholder="级别" size="default" clearable style="width: 120px" @change="loadSavedRca">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadSavedRca">查询</el-button>
          <el-button :icon="Refresh" @click="loadSavedRca">刷新</el-button>
        </div>

        <!-- 统计卡片 -->
        <div class="kb-summary-cards" style="margin-bottom:14px">
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ savedRcaEntries.length }}</div>
            <div class="kb-summary-label">当前条目数</div>
          </div>
          <div class="kb-summary-card">
            <div class="kb-summary-num">{{ savedRcaEntries.filter(e => e.severity === 'critical').length || 0 }}</div>
            <div class="kb-summary-label">严重级别</div>
          </div>
        </div>

        <!-- RCA 知识条目列表 -->
        <el-table :data="savedRcaEntries" v-loading="savedRcaLoading" stripe empty-text="暂无已保存的 RCA 知识条目">
          <el-table-column prop="severity" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.severity)" size="small">{{ getLevelLabel(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="metricName" label="指标名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="ruleName" label="规则名" min-width="140" show-overflow-tooltip />
          <el-table-column prop="category" label="根因分类" min-width="120">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)" size="small">{{ getCategoryLabel(row.category) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cause" label="根因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="confidence" label="置信度" width="90" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round((row.confidence || 0) * 100)"
                :color="getConfidenceColor(row.confidence)"
                :stroke-width="6"
                :show-text="false"
              />
              <span style="font-size:11px;color:var(--el-text-color-secondary)">{{ ((row.confidence || 0) * 100).toFixed(0) }}%</span>
            </template>
          </el-table-column>
          <el-table-column prop="method" label="分析方法" width="90">
            <template #default="{ row }">
              <el-tag :type="getRcaMethodType(row.method)" size="small">{{ getRcaMethodLabel(row.method) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="保存时间" width="170" />
          <!-- Day 59: 操作列 - 删除 -->
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                type="danger"
                link
                :loading="deletingRcaId === row.id"
                @click="confirmDeleteRca(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 历史告警 -->
      <el-tab-pane label="历史告警" name="history">
        <div class="toolbar">
          <el-date-picker
            v-model="historyFilter.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="default"
            style="width: 380px"
          />
          <el-select v-model="historyFilter.level" placeholder="级别" size="default" clearable style="width: 120px">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadHistory">查询</el-button>
        </div>
        <el-table :data="historyAlerts" v-loading="loadingHistory" stripe>
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)" size="small">{{ getLevelLabel(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="source" label="来源" width="120" />
          <el-table-column prop="firedAt" label="触发时间" width="170" />
          <el-table-column prop="resolvedAt" label="解决时间" width="170" />
        </el-table>
        <el-pagination
          v-model:current-page="historyPage"
          :total="historyTotal"
          :page-size="20"
          layout="total, prev, pager, next"
          @current-change="loadHistory"
          style="margin-top: 12px; justify-content: flex-end; display: flex"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ========== RCA 详情抽屉 (Day 55) ========== -->
    <el-drawer v-model="rcaDrawerVisible" title="告警根因分析" size="600px" direction="rtl">
      <div v-if="rcaLoading" style="text-align:center;padding:60px 0">
        <el-icon class="is-loading" style="font-size:32px"><Loading /></el-icon>
        <p style="margin-top:12px;color:var(--el-text-color-secondary)">正在分析根因...</p>
      </div>

      <div v-else-if="rcaError" style="padding:20px">
        <el-alert type="error" :title="rcaError" show-icon :closable="false" />
      </div>

      <div v-else-if="rcaResult" class="rca-content">
        <!-- 分析元信息 -->
        <div class="rca-meta">
          <el-tag :type="getRcaMethodType(rcaResult.method)" size="small">
            {{ getRcaMethodLabel(rcaResult.method) }}
          </el-tag>
          <span class="rca-ms">耗时 {{ rcaResult.analysisMs }}ms</span>
          <el-tooltip v-if="rcaResult.confidence > 0" content="分析置信度">
            <span class="rca-confidence">
              置信度 {{ (rcaResult.confidence * 100).toFixed(0) }}%
            </span>
          </el-tooltip>
        </div>

        <!-- 根因分类 -->
        <div class="rca-section">
          <h4 class="rca-section-title">根因分类</h4>
          <el-tag :type="getCategoryType(rcaResult.category)" size="large" style="font-size:14px;padding:6px 16px">
            {{ getCategoryLabel(rcaResult.category) }}
          </el-tag>
        </div>

        <!-- 根因分析 -->
        <div v-if="rcaResult.cause" class="rca-section">
          <h4 class="rca-section-title">根因分析</h4>
          <p class="rca-cause">{{ rcaResult.cause }}</p>
        </div>

        <!-- 建议操作 -->
        <div v-if="rcaResult.suggestedActions?.length" class="rca-section">
          <h4 class="rca-section-title">建议操作</h4>
          <ul class="rca-actions">
            <li v-for="(action, i) in rcaResult.suggestedActions" :key="i">{{ action }}</li>
          </ul>
        </div>

        <!-- 历史知识 (Day 55) -->
        <div v-if="rcaResult.historicalKnowledge?.length" class="rca-section">
          <h4 class="rca-section-title">
            历史处理经验
            <el-tag type="info" size="small" style="margin-left:8px">{{ rcaResult.historicalKnowledge.length }} 条</el-tag>
          </h4>
          <div class="rca-knowledge-list">
            <div v-for="(entry, i) in rcaResult.historicalKnowledge" :key="i" class="rca-knowledge-item">
              <div class="rca-knowledge-header">
                <el-tag :type="getLevelType(entry.severity)" size="small">{{ entry.severity }}</el-tag>
                <span class="rca-knowledge-title">{{ entry.ruleName || entry.metricName }}</span>
                <span class="rca-knowledge-status">{{ entry.status }}</span>
              </div>
              <div v-if="entry.duration" class="rca-knowledge-meta">
                持续 {{ formatDuration(entry.duration) }} · 处理人: {{ entry.resolvedBy || '未知' }}
              </div>
              <div v-if="entry.notes" class="rca-knowledge-notes">
                <el-icon size="12"><InfoFilled /></el-icon>
                {{ entry.notes }}
              </div>
            </div>
          </div>
        </div>

        <!-- 原始回答 (LLM 时展开) -->
        <div v-if="rcaResult.rawAnswer && rcaResult.method === 'llm'" class="rca-section">
          <h4 class="rca-section-title">
            LLM 原始分析
            <el-button link type="primary" size="small" style="margin-left:8px" @click="rawAnswerExpanded = !rawAnswerExpanded">
              {{ rawAnswerExpanded ? '收起' : '展开' }}
            </el-button>
          </h4>
          <pre v-if="rawAnswerExpanded" class="rca-raw-answer">{{ rcaResult.rawAnswer }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="rcaDrawerVisible = false">关闭</el-button>
        <el-button type="primary" @click="refreshRca" :loading="rcaLoading">重新分析</el-button>
        <!-- Day 58: 保存 RCA 分析结果到知识库 -->
        <el-button type="success" @click="saveToKnowledgeBase" :loading="saveToKbLoading" :disabled="!rcaResult">
          <el-icon v-if="!saveToKbLoading"><Collection /></el-icon>
          保存到知识库
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Loading, InfoFilled, Collection } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { monitorApi } from '@/api/monitor'

const activeTab = ref('active')

// Day 59: Tab 切换时懒加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'savedRca') loadSavedRca()
})

const loadingActive = ref(false)
const loadingHistory = ref(false)

const activeAlerts = ref([])
const activeFilter = reactive({ keyword: '', level: '' })

const historyAlerts = ref([])
const historyFilter = reactive({ dateRange: null, level: '' })
const historyPage = ref(1)
const historyTotal = ref(0)

// ==================== RCA 抽屉 (Day 55) ====================
const rcaDrawerVisible = ref(false)
const rcaLoading = ref(false)
const rcaError = ref('')
const rcaResult = ref(null)
const rcaCurrentAlert = ref(null)
const rawAnswerExpanded = ref(false)

// ==================== 知识库 Tab (Day 56) ====================
const kbLoading = ref(false)
const kbEntries = ref([])
const kbSummary = ref(null)
const kbFilter = reactive({ metricName: '', severity: '', days: 30 })
const kbPage = ref(1)
const kbTotal = ref(0)

// Day 57: 知识库条目触发 RCA — 加载状态（key = metricName 防止多个按钮同时 loading）
const rcaFromKbLoading = ref('')

// Day 58: 保存到知识库加载状态
const saveToKbLoading = ref(false)

// ==================== Day 59: 已保存 RCA 知识库管理 ====================
const savedRcaLoading = ref(false)
const savedRcaEntries = ref([])
const savedRcaFilter = reactive({ metricName: '', severity: '' })
const deletingRcaId = ref(null)

function getConfidenceColor(score) {
  const s = Math.max(0, Math.min(1, score || 0))
  if (s >= 0.8) return '#67c23a'
  if (s >= 0.6) return '#e6a23c'
  if (s >= 0.4) return '#f56c6c'
  return '#909399'
}

async function loadSavedRca() {
  savedRcaLoading.value = true
  try {
    const params = { limit: 100 }
    if (savedRcaFilter.metricName) params.metricName = savedRcaFilter.metricName
    const res = await monitorApi.listRcaKnowledge(params)
    if (res.code === 0) {
      const list = Array.isArray(res.data) ? res.data : []
      // 按 severity 过滤（前端）
      savedRcaEntries.value = savedRcaFilter.severity
        ? list.filter(e => e.severity === savedRcaFilter.severity)
        : list
    } else {
      ElMessage.error('加载 RCA 知识库失败: ' + (res.message || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || '网络错误'))
  } finally {
    savedRcaLoading.value = false
  }
}

async function confirmDeleteRca(entry) {
  try {
    await ElMessageBox.confirm(
      `确认删除 RCA 知识条目「${entry.metricName}」（${entry.category || ''}）？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch (_) {
    return
  }
  deletingRcaId.value = entry.id
  try {
    await monitorApi.deleteRcaKnowledge(entry.id)
    ElMessage.success('已删除')
    loadSavedRca()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    deletingRcaId.value = null
  }
}

/** Day 57: 知识库条目触发 RCA 分析（联动 RCA 抽屉） */
async function triggerRcaFromKb(entry) {
  if (!entry?.metricName) {
    ElMessage.warning('指标名称为空，无法触发 RCA')
    return
  }
  rcaFromKbLoading.value = entry.metricName
  rcaError.value = ''
  rcaResult.value = null
  rcaCurrentAlert.value = { id: null, metricName: entry.metricName, severity: entry.severity, title: entry.metricName }
  rcaDrawerVisible.value = true
  rawAnswerExpanded.value = false
  try {
    const res = await monitorApi.rcaAnalysisByMetric(entry.metricName, entry.severity, kbFilter.days)
    if (res.code === 0) {
      rcaResult.value = res.data
    } else {
      rcaError.value = res.message || 'RCA 分析失败'
    }
  } catch (e) {
    rcaError.value = e.message || '网络错误，请重试'
  } finally {
    rcaFromKbLoading.value = ''
  }
}

async function loadKbKnowledge() {
  kbLoading.value = true
  try {
    const params = { historyDays: kbFilter.days, limit: 100 }
    if (kbFilter.metricName) params.metricName = kbFilter.metricName
    if (kbFilter.severity) params.severity = kbFilter.severity
    const res = await monitorApi.getAlertRcaKnowledge(params)
    if (res.code === 0) {
      const list = Array.isArray(res.data) ? res.data : res.data?.list || []
      kbEntries.value = list
      kbTotal.value = list.length
    }
  } catch (e) { ElMessage.error('加载知识库失败: ' + e.message) }
  finally { kbLoading.value = false }
}

async function loadKbSummary() {
  try {
    const res = await monitorApi.getAlertRcaSummary(kbFilter.metricName || null, kbFilter.days)
    if (res.code === 0) kbSummary.value = res.data
    else kbSummary.value = null
  } catch { kbSummary.value = null }
}

function getLevelColor(level) {
  return { critical: 'var(--el-color-danger)', warning: 'var(--el-color-warning)', info: 'var(--el-color-info)' }[level] || 'inherit'
}

function getLevelType(level) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[level] || 'info'
}
function getLevelLabel(level) {
  return { critical: '严重', warning: '警告', info: '信息' }[level] || level
}

// RCA 方法标签
function getRcaMethodType(method) {
  return { llm: 'primary', 'rule-based': 'success', 'rule-based fallback': 'warning', error: 'danger', skipped: 'info' }[method] || 'info'
}
function getRcaMethodLabel(method) {
  return { llm: 'LLM 分析', 'rule-based': '规则匹配', 'rule-based fallback': '规则降级', error: '分析失败', skipped: '已跳过' }[method] || method
}

// 根因分类标签
function getCategoryType(cat) {
  return {
    RESOURCE_BOTTLENECK: 'danger',
    CONFIG_ERROR: 'warning',
    EXTERNAL_DEPENDENCY: 'info',
    CODE_BUG: 'danger',
    TRAFFIC_SPIKE: 'warning',
    NETWORK: 'info',
    UNKNOWN: 'info'
  }[cat] || 'info'
}
function getCategoryLabel(cat) {
  return {
    RESOURCE_BOTTLENECK: '资源瓶颈',
    CONFIG_ERROR: '配置错误',
    EXTERNAL_DEPENDENCY: '外部依赖',
    CODE_BUG: '代码缺陷',
    TRAFFIC_SPIKE: '流量突增',
    NETWORK: '网络问题',
    UNKNOWN: '未知'
  }[cat] || cat
}

// 时长格式化
function formatDuration(seconds) {
  if (!seconds) return '未知'
  if (seconds < 60) return seconds + 's'
  if (seconds < 3600) return Math.floor(seconds / 60) + 'm ' + (seconds % 60) + 's'
  return (seconds / 3600).toFixed(1) + 'h'
}

async function openRcaDrawer(alert) {
  rcaCurrentAlert.value = alert
  rcaDrawerVisible.value = true
  rcaError.value = ''
  rcaResult.value = null
  rawAnswerExpanded.value = false
  await fetchRca()
}

async function fetchRca() {
  if (!rcaCurrentAlert.value) return
  rcaLoading.value = true
  rcaError.value = ''
  try {
    const res = await monitorApi.rcaAnalysis(rcaCurrentAlert.value.id)
    if (res.code === 0) {
      rcaResult.value = res.data
    } else {
      rcaError.value = res.message || 'RCA 分析失败'
    }
  } catch (e) {
    rcaError.value = e.message || '网络错误，请重试'
  } finally {
    rcaLoading.value = false
  }
}

async function refreshRca() {
  await fetchRca()
}

/** Day 58: 将当前 RCA 分析结果保存为知识库条目 */
async function saveToKnowledgeBase() {
  if (!rcaCurrentAlert.value?.id) {
    ElMessage.warning('当前告警 ID 不存在，无法保存')
    return
  }
  saveToKbLoading.value = true
  try {
    const res = await monitorApi.saveRcaToKnowledge(rcaCurrentAlert.value.id)
    if (res.code === 0) {
      ElMessage.success('已保存到知识库（条目 #' + res.data.id + '）')
    } else {
      ElMessage.error('保存失败: ' + (res.message || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '网络错误'))
  } finally {
    saveToKbLoading.value = false
  }
}

async function loadActive() {
  loadingActive.value = true
  try {
    const res = await monitorApi.listActiveAlerts({
      keyword: activeFilter.keyword,
      level: activeFilter.level
    })
    if (res.code === 0) activeAlerts.value = res.data?.list || res.data || []
  } catch (e) { ElMessage.error('加载告警失败: ' + e.message) }
  finally { loadingActive.value = false }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const params = { page: historyPage.value, size: 20, level: historyFilter.level }
    if (historyFilter.dateRange?.length === 2) {
      params.startTime = historyFilter.dateRange[0]
      params.endTime = historyFilter.dateRange[1]
    }
    const res = await monitorApi.listAlertHistory(params)
    if (res.code === 0) {
      historyAlerts.value = res.data?.list || res.data || []
      historyTotal.value = res.data?.total || 0
    }
  } catch (e) { ElMessage.error('加载历史失败: ' + e.message) }
  finally { loadingHistory.value = false }
}

async function ackAlert(row) {
  try {
    await monitorApi.ackAlert(row.id)
    ElMessage.success('已确认')
    loadActive()
  } catch (e) { ElMessage.error('操作失败') }
}

async function resolveAlert(row) {
  try {
    await monitorApi.resolveAlert(row.id)
    ElMessage.success('已解决')
    loadActive()
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(() => {
  loadActive()
  loadHistory()
  loadKbKnowledge()
  loadKbSummary()
})
</script>

<style scoped>
.alerts-page { background: white; border-radius: 12px; padding: 16px; }
.alerts-tabs { background: transparent; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }

/* RCA 抽屉样式 (Day 55) */
.rca-content { padding: 0 20px 20px; }
.rca-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding: 10px 14px; background: var(--el-fill-color-light); border-radius: 8px; }
.rca-ms, .rca-confidence { font-size: 12px; color: var(--el-text-color-secondary); }
.rca-section { margin-bottom: 20px; }
.rca-section-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); margin: 0 0 10px; display: flex; align-items: center; }
.rca-cause { margin: 0; font-size: 13px; color: var(--el-text-color-regular); line-height: 1.7; white-space: pre-wrap; }
.rca-actions { margin: 0; padding-left: 20px; font-size: 13px; color: var(--el-text-color-regular); line-height: 2; }
.rca-knowledge-list { display: flex; flex-direction: column; gap: 10px; }
.rca-knowledge-item { padding: 10px 12px; border: 1px solid var(--el-border-color-light); border-radius: 8px; background: var(--el-fill-color-blank); }
.rca-knowledge-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.rca-knowledge-title { font-size: 13px; font-weight: 500; color: var(--el-text-color-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rca-knowledge-status { font-size: 11px; color: var(--el-color-success); }
.rca-knowledge-meta { font-size: 11px; color: var(--el-text-color-secondary); }
.rca-knowledge-notes { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px; display: flex; align-items: flex-start; gap: 4px; }
.rca-raw-answer { font-size: 11px; background: var(--el-fill-color-dark); color: var(--el-text-color-regular); padding: 12px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow-y: auto; margin: 0; }

/* 知识库 Tab (Day 56) */
.kb-summary-cards { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.kb-summary-card { flex: 1; min-width: 100px; background: var(--el-fill-color-light); border-radius: 8px; padding: 12px 16px; text-align: center; border: 1px solid var(--el-border-color-lighter); }
.kb-summary-num { font-size: 22px; font-weight: 700; color: var(--el-text-color-primary); line-height: 1.2; }
.kb-summary-label { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px; }
</style>
