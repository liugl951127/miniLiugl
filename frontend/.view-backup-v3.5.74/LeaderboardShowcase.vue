<!--
  模型对决排行榜 (V4.1)
  - 4 个 tab: 总体 / 速度 / 最近 / 分类
  - 数据源: model_battle_log 表
  - ECharts 可视化
-->
<!--
  @file views/showcase/LeaderboardShowcase.vue (排行榜 (LeaderboardShowcase))
  @version V3.5.12+ (前端注释补全)
  @description 排行榜 (LeaderboardShowcase)
-->
<template>
  <div class="leaderboard">
    <header class="header">
      <h1>🏆 模型排行榜</h1>
      <p class="subtitle">基于真实对决日志, 谁是最强大模型一目了然</p>
      <div class="badges">
        <span class="badge">{{ totalBattles }} 次对决</span>
        <span class="badge">{{ uniqueModels }} 个模型</span>
        <span class="badge">实时统计</span>
      </div>
    </header>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="loadData">
      <el-tab-pane label="🥇 综合评分" name="overall">
        <div v-if="loading" class="loading"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
        <el-table v-else :data="overallData" stripe>
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="model_code" label="模型" min-width="220">
            <template #default="{ row }">
              <div class="model-cell">
                <span class="model-name">{{ row.displayName || row.model_code }}</span>
                <span class="model-code">{{ row.model_code }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="平均评分" width="200">
            <template #default="{ row }">
              <el-rate v-model="row.avg_score" disabled show-score :max="5" />
            </template>
          </el-table-column>
          <el-table-column prop="avg_latency" label="平均延迟" width="150">
            <template #default="{ row }">
              <span :class="latencyClass(row.avg_latency)">{{ Math.round(row.avg_latency || 0) }}ms</span>
            </template>
          </el-table-column>
          <el-table-column prop="cnt" label="对决次数" width="100" sortable />
          <el-table-column label="成功率" width="120">
            <template #default="{ row }">
              <span>{{ Math.round((row.ok_cnt / row.cnt) * 100) }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="🥇 奖牌" width="120">
            <template #default="{ $index }">
              <span v-if="$index === 0" class="medal gold">🥇</span>
              <span v-else-if="$index === 1" class="medal silver">🥈</span>
              <span v-else-if="$index === 2" class="medal bronze">🥉</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="⚡ 速度榜" name="latency">
        <div ref="latencyChart" class="chart-area"></div>
        <el-table :data="latencyData" stripe style="margin-top: 16px">
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="model_code" label="模型" />
          <el-table-column prop="avg_latency" label="P50 延迟" width="150">
            <template #default="{ row }">
              <span :class="latencyClass(row.avg_latency)">{{ Math.round(row.avg_latency || 0) }}ms</span>
            </template>
          </el-table-column>
          <el-table-column prop="cnt" label="样本数" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="🕐 最近对决" name="recent">
        <el-table :data="recentData" stripe>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column prop="model_code" label="模型" min-width="200" />
          <el-table-column label="延迟" width="120">
            <template #default="{ row }">
              <span :class="latencyClass(row.latency_ms)">{{ row.latency_ms }}ms</span>
            </template>
          </el-table-column>
          <el-table-column prop="total_tokens" label="Tokens" width="100" />
          <el-table-column label="评分" width="150">
            <template #default="{ row }">
              <el-rate v-model="row.score" disabled :max="5" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ok' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="📂 模型分类" name="categories">
        <div v-for="(group, pname) in categories" :key="pname" class="category-group">
          <h3>{{ pname }} ({{ group.length }})</h3>
          <div class="category-grid">
            <div v-for="m in group" :key="m.modelCode" class="category-card">
              <div class="cc-name">{{ m.displayName }}</div>
              <div class="cc-code">{{ m.modelCode }}</div>
              <div class="cc-tags">
                <el-tag v-if="m.supportsVision" size="small" type="warning">👁 Vision</el-tag>
                <el-tag v-if="m.enabled" size="small" type="success">启用</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '@/api/http'
import dayjs from 'dayjs'

const activeTab = ref('overall')
const loading = ref(false)
const overallData = ref([])
const latencyData = ref([])
const recentData = ref([])
const categories = ref({})
const latencyChart = ref(null)
let chart = null

const totalBattles = computed(() => recentData.value.length)
const uniqueModels = computed(() => new Set(recentData.value.map(r => r.model_code)).size)

function latencyClass(ms) {
  if (!ms) return ''
  if (ms < 1000) return 'fast'
  if (ms < 3000) return 'medium'
  return 'slow'
}
function formatTime(t) {
  return dayjs(t).format('MM-DD HH:mm:ss')
}

async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'overall') {
      const r = await http.get('/api/v1/leaderboard/overall')
      overallData.value = r.data || []
    } else if (activeTab.value === 'latency') {
      const r = await http.get('/api/v1/leaderboard/latency')
      latencyData.value = r.data || []
      await nextTick()
      renderLatencyChart()
    } else if (activeTab.value === 'recent') {
      const r = await http.get('/api/v1/leaderboard/recent?limit=50')
      recentData.value = r.data || []
    } else if (activeTab.value === 'categories') {
      const r = await http.get('/api/v1/leaderboard/categories')
      categories.value = r.data || {}
    }
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function renderLatencyChart() {
  if (!latencyChart.value || latencyData.value.length === 0) return
  if (!chart) chart = echarts.init(latencyChart.value)
  const data = latencyData.value.slice(0, 10)
  chart.setOption({
    title: { text: 'Top 10 模型延迟对比', left: 'center' },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', name: 'ms', axisLabel: { formatter: '{value} ms' } },
    yAxis: { type: 'category', data: data.map(d => d.displayName || d.model_code).reverse() },
    series: [{
      name: '平均延迟', type: 'bar', data: data.map(d => Math.round(d.avg_latency || 0)).reverse(),
      itemStyle: {
        color: (params) => {
          const v = params.value
          if (v < 1000) return '#22c55e'
          if (v < 3000) return '#f59e0b'
          return '#ef4444'
        }
      },
      label: { show: true, position: 'right', formatter: '{c} ms' }
    }]
  })
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.leaderboard { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; border-radius: 12px; font-size: 12px; }
.loading { text-align: center; padding: 40px; color: #64748b; }
.chart-area { width: 100%; height: 400px; }
.model-cell { display: flex; flex-direction: column; }
.model-name { font-weight: 600; }
.model-code { font-size: 12px; color: #64748b; }
.fast { color: #16a34a; font-weight: 600; }
.medium { color: #f59e0b; font-weight: 600; }
.slow { color: #dc2626; font-weight: 600; }
.medal { font-size: 24px; }
.category-group { margin-bottom: 24px; }
.category-group h3 { font-size: 18px; margin: 0 0 12px; padding-bottom: 8px;
  border-bottom: 2px solid #e2e8f0; }
.category-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.category-card { background: #f8fafc; border-radius: 8px; padding: 12px; }
.cc-name { font-weight: 600; }
.cc-code { font-size: 12px; color: #64748b; margin: 4px 0; }
.cc-tags { display: flex; gap: 4px; margin-top: 8px; }
</style>
