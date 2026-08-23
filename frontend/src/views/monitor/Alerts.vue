<!--
  @file monitor/Alerts.vue - 告警管理 (V7.6)
  路由: /monitor/alerts
  合并: 活跃告警 + 历史告警 (原 2 个 tab)
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
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="ackAlert(row)">确认</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { monitorApi } from '@/api/monitor'

const activeTab = ref('active')

const loadingActive = ref(false)
const loadingHistory = ref(false)

const activeAlerts = ref([])
const activeFilter = reactive({ keyword: '', level: '' })

const historyAlerts = ref([])
const historyFilter = reactive({ dateRange: null, level: '' })
const historyPage = ref(1)
const historyTotal = ref(0)

function getLevelType(level) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[level] || 'info'
}
function getLevelLabel(level) {
  return { critical: '严重', warning: '警告', info: '信息' }[level] || level
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
})
</script>

<style scoped>
.alerts-page { background: white; border-radius: 12px; padding: 16px; }
.alerts-tabs { background: transparent; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
</style>
