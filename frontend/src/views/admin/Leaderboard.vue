<template>
  <div class="leaderboard-page">
    <div class="page-header">
      <h2>🏆 模型对决排行榜</h2>
      <el-button @click="loadAll"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>📊 说明</template>
      数据来源于 model_battle_log (Day 11 多模型对决), 可按综合评分 / 速度 / 最近对决查看
    </el-alert>

    <el-tabs v-model="activeTab" class="lb-tabs">
      <el-tab-pane label="🥇 综合排行 (评分)" name="overall">
        <el-table :data="overall" v-loading="loading.overall" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span :class="['rank', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型" prop="modelCode" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.modelCode }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="平均分" prop="avgScore" width="120">
            <template #default="{ row }">
              <el-progress :percentage="(row.avgScore || 0) * 100 / 5" :stroke-width="14" :format="() => (row.avgScore || 0).toFixed(2)" />
            </template>
          </el-table-column>
          <el-table-column label="对决次数" prop="battleCount" width="120" sortable />
          <el-table-column label="胜率" width="120">
            <template #default="{ row }">
              <el-tag :type="row.winRate > 0.5 ? 'success' : row.winRate > 0.3 ? 'warning' : 'info'">
                {{ ((row.winRate || 0) * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="⚡ 速度排行 (延迟升序)" name="latency">
        <el-table :data="latency" v-loading="loading.latency" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span :class="['rank', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型" prop="modelCode" min-width="180" />
          <el-table-column label="平均延迟" prop="avgLatencyMs" width="160">
            <template #default="{ row }">
              <span :class="latencyClass(row.avgLatencyMs)">{{ row.avgLatencyMs }}ms</span>
            </template>
          </el-table-column>
          <el-table-column label="P50" prop="p50Ms" width="100" />
          <el-table-column label="P95" prop="p95Ms" width="100" />
          <el-table-column label="P99" prop="p99Ms" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="🕐 最近对决" name="recent">
        <el-table :data="recent" v-loading="loading.recent" stripe>
          <el-table-column label="对决 ID" prop="id" width="80" />
          <el-table-column label="问题" prop="question" show-overflow-tooltip min-width="300" />
          <el-table-column label="胜出模型" prop="winnerCode" width="140">
            <template #default="{ row }">
              <el-tag type="success">{{ row.winnerCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对比模型" prop="competitorCode" width="140" />
          <el-table-column label="评分" prop="score" width="80" />
          <el-table-column label="延迟" width="100">
            <template #default="{ row }">{{ row.latencyMs }}ms</template>
          </el-table-column>
          <el-table-column label="时间" prop="createdAt" width="170" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="📂 分类排行" name="categories">
        <el-collapse v-model="openCategories">
          <el-collapse-item v-for="(items, cat) in categories" :key="cat" :name="cat" :title="`${cat} (${items.length})`">
            <el-table :data="items" size="small">
              <el-table-column label="排名" width="70">
                <template #default="{ $index }">
                  <span :class="['rank', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="模型" prop="modelCode" min-width="160" />
              <el-table-column label="平均分" prop="avgScore" width="120" />
              <el-table-column label="次数" prop="battleCount" width="80" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  leaderboardOverall, leaderboardLatency,
  leaderboardRecent, leaderboardCategories
} from '@/api/model'

const activeTab = ref('overall')
const overall = ref([])
const latency = ref([])
const recent = ref([])
const categories = ref({})
const openCategories = ref([])

const loading = reactive({ overall: false, latency: false, recent: false, categories: false })

function latencyClass(ms) {
  if (!ms) return ''
  if (ms < 1000) return 'fast'
  if (ms < 3000) return 'medium'
  return 'slow'
}

async function loadOverall() {
  loading.overall = true
  try {
    const res = await leaderboardOverall(50)
    overall.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.overall = false }
}

async function loadLatency() {
  loading.latency = true
  try {
    const res = await leaderboardLatency(50)
    latency.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.latency = false }
}

async function loadRecent() {
  loading.recent = true
  try {
    const res = await leaderboardRecent(100)
    recent.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.recent = false }
}

async function loadCategories() {
  loading.categories = true
  try {
    const res = await leaderboardCategories()
    categories.value = res.data?.data || res.data || {}
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.categories = false }
}

async function loadAll() {
  await Promise.all([loadOverall(), loadLatency(), loadRecent(), loadCategories()])
}

onMounted(loadAll)
</script>

<style scoped>
.leaderboard-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.lb-tabs { background: #fff; border-radius: 8px; padding: 16px; }
.rank { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 50%; font-weight: 600; font-size: 13px; background: #909399; color: #fff; }
.rank-1 { background: linear-gradient(135deg, #ffd700, #ffa500); }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a8a8a8); }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #a05a2c); }
.fast { color: #67c23a; font-weight: 600; }
.medium { color: #e6a23c; font-weight: 600; }
.slow { color: #f56c6c; font-weight: 600; }
</style>