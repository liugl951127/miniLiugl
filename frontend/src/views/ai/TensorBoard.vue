<template>
  <div class="tensorboard-container">
    <div class="tb-header">
      <h1>📊 TensorBoard <span class="badge">V2.8.8 自托管</span></h1>
      <p class="sub">训练指标可视化 · 多 run 对比 · 实时刷新 (3s 轮询)</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧: runs 列表 -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <span>🗂️ Runs ({{ runs.length }})</span>
            <el-button size="small" text @click="loadRuns" style="float:right">🔄</el-button>
          </template>
          <div v-if="runs.length === 0" class="empty">
            暂无 run<br/>
            <small>训练任务会生成 events.tfevents</small>
          </div>
          <div v-else class="run-list">
            <div
              v-for="r in runs"
              :key="r"
              class="run-item"
              :class="{ active: selectedRuns.includes(r) }"
              @click="toggleRun(r)"
            >
              <el-icon><Folder /></el-icon>
              <span class="run-name">{{ r }}</span>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header><span>⚙️ 控制</span></template>
          <el-form label-width="80px" size="small">
            <el-form-item label="刷新">
              <el-switch v-model="autoRefresh" />
              <span class="hint">每 3s</span>
            </el-form-item>
            <el-form-item label="平滑">
              <el-slider v-model="smoothing" :min="0" :max="0.99" :step="0.01" />
            </el-form-item>
            <el-form-item label="Y 轴">
              <el-radio-group v-model="yScale">
                <el-radio value="linear">线性</el-radio>
                <el-radio value="log">对数</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧: 图表区 -->
      <el-col :span="18">
        <!-- Tags 多选 -->
        <el-card>
          <template #header>
            <span>📈 指标 ({{ selectedTags.length }} 个)</span>
          </template>
          <el-checkbox-group v-model="selectedTags">
            <el-checkbox
              v-for="t in availableTags"
              :key="`${t.runId}-${t.tag}`"
              :value="`${t.runId}|${t.tag}`"
            >
              <span :style="{ color: t.color }">●</span>
              {{ t.runId }} / {{ t.tag }}
            </el-checkbox>
          </el-checkbox-group>
        </el-card>

        <!-- 折线图 -->
        <el-card style="margin-top: 16px">
          <template #header>
            <span>📉 训练曲线</span>
            <span class="legend-hint" style="float: right; font-size: 12px; color: #909399">
              共 {{ chartData.series.length }} 条线
            </span>
          </template>
          <v-chart
            v-if="chartData.series.length"
            :option="chartOption"
            :loading="loading"
            style="height: 400px"
            autoresize
          />
          <el-empty v-else description="选择 run 和 tag 后显示图表" />
        </el-card>

        <!-- 数据表 -->
        <el-card v-if="lastUpdate" style="margin-top: 16px">
          <template #header>
            <span>🔢 最新数据点</span>
            <span style="float: right; font-size: 12px; color: #909399">
              最后更新: {{ formatTime(lastUpdate) }}
            </span>
          </template>
          <el-table :data="latestPoints" size="small" stripe>
            <el-table-column prop="runId" label="Run" />
            <el-table-column prop="tag" label="Tag" />
            <el-table-column prop="step" label="Step" />
            <el-table-column prop="value" label="Value" />
            <el-table-column prop="wallTime" label="Wall Time">
              <template #default="{ row }">
                {{ formatWallTime(row.wallTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent, DataZoomComponent
} from 'echarts/components'
import { tensorboardApi } from '@/api/tensorboard'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent])

const runs = ref([])
const selectedRuns = ref([])
const availableTags = ref([])     // { runId, tag, color }
const selectedTags = ref([])      // ["runId|tag", ...]
const autoRefresh = ref(true)
const smoothing = ref(0.6)
const yScale = ref('linear')
const loading = ref(false)
const lastUpdate = ref(null)

const colorPalette = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const chartData = reactive({ series: [] })

// 选中的 tag 完整数据缓存
const allScalars = ref(new Map())  // key: "runId|tag" -> [{ step, value, wall_time }]

const latestPoints = computed(() => {
  const list = []
  for (const key of selectedTags) {
    const pts = allScalars.value.get(key)
    if (pts && pts.length) {
      const last = pts[pts.length - 1]
      const [runId, tag] = key.split('|')
      list.push({ runId, tag, step: last.step, value: last.value, wallTime: last.wall_time })
    }
  }
  return list
})

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'cross' }
  },
  legend: { type: 'scroll', top: 0 },
  grid: { left: 60, right: 30, top: 50, bottom: 60 },
  xAxis: {
    type: 'value',
    name: 'Step',
    nameLocation: 'middle',
    nameGap: 30
  },
  yAxis: {
    type: yScale.value,
    name: 'Value',
    nameLocation: 'middle',
    nameGap: 50,
    scale: true
  },
  dataZoom: [
    { type: 'inside', start: 0, end: 100 },
    { type: 'slider', start: 0, end: 100, height: 20, bottom: 10 }
  ],
  series: chartData.series
}))

const toggleRun = (r) => {
  const idx = selectedRuns.value.indexOf(r)
  if (idx > -1) selectedRuns.value.splice(idx, 1)
  else selectedRuns.value.push(r)
}

const loadRuns = async () => {
  try {
    const res = await tensorboardApi.listRuns()
    runs.value = res.data || []
  } catch (e) {
    console.warn('loadRuns failed:', e)
  }
}

const loadTagsForRuns = async () => {
  availableTags.value = []
  let colorIdx = 0
  for (const runId of selectedRuns.value) {
    try {
      const res = await tensorboardApi.listTags(runId)
      const tags = res.data || []
      for (const tag of tags) {
        availableTags.value.push({
          runId, tag,
          color: colorPalette[colorIdx % colorPalette.length]
        })
        colorIdx++
      }
    } catch (e) {
      console.warn(`loadTags ${runId} failed:`, e)
    }
  }
  // 默认选第一个 run 的 loss
  if (selectedTags.value.length === 0 && availableTags.value.length > 0) {
    const first = availableTags.value[0]
    selectedTags.value = [`${first.runId}|${first.tag}`]
  }
}

const loadScalars = async () => {
  loading.value = true
  try {
    // 加载所有选中的 tag
    for (const key of selectedTags) {
      const [runId, tag] = key.split('|')
      const res = await tensorboardApi.readScalar(runId, tag)
      const data = res.data?.data
      if (data && data.points) {
        allScalars.value.set(key, data.points)
      }
    }
    rebuildChart()
    lastUpdate.value = Date.now()
  } catch (e) {
    console.warn('loadScalars failed:', e)
  } finally {
    loading.value = false
  }
}

const rebuildChart = () => {
  const series = []
  for (const key of selectedTags) {
    const pts = allScalars.value.get(key)
    if (!pts || pts.length === 0) continue
    const [runId, tag] = key.split('|')
    const tagInfo = availableTags.value.find(t => `${t.runId}|${t.tag}` === key)
    const color = tagInfo?.color || colorPalette[series.length % colorPalette.length]
    // 应用平滑 (EMA)
    const smoothed = applySmoothing(pts.map(p => [p.step, p.value]), smoothing.value)
    series.push({
      name: `${runId}/${tag}`,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2 },
      data: smoothed,
      itemStyle: { color },
      emphasis: { focus: 'series' }
    })
  }
  chartData.series = series
}

const applySmoothing = (data, alpha) => {
  if (alpha <= 0 || data.length === 0) return data
  const out = []
  let ema = data[0][1]
  out.push([data[0][0], ema])
  for (let i = 1; i < data.length; i++) {
    ema = alpha * data[i][1] + (1 - alpha) * ema
    out.push([data[i][0], ema])
  }
  return out
}

const formatTime = (ts) => {
  return new Date(ts).toLocaleString('zh-CN', { hour12: false })
}

const formatWallTime = (t) => {
  if (!t) return '-'
  return new Date(t * 1000).toLocaleString('zh-CN', { hour12: false })
}

let pollTimer = null

watch(selectedRuns, () => {
  loadTagsForRuns()
}, { deep: true })

watch(selectedTags, () => {
  loadScalars()
}, { deep: true })

watch(smoothing, () => rebuildChart())
watch(yScale, () => rebuildChart())

onMounted(async () => {
  await loadRuns()
  // 默认选第一个 run
  if (runs.value.length > 0) {
    selectedRuns.value = [runs.value[0]]
    await loadTagsForRuns()
    await loadScalars()
  }
  if (autoRefresh.value) {
    pollTimer = setInterval(() => {
      loadScalars()
    }, 3000)
  }
})

watch(autoRefresh, (v) => {
  if (v) {
    pollTimer = setInterval(loadScalars, 3000)
  } else {
    clearInterval(pollTimer)
    pollTimer = null
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.tensorboard-container { padding: 20px; }
.tb-header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #e6a23c; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }
.empty { color: #909399; text-align: center; padding: 30px 0; }
.run-list { max-height: 400px; overflow-y: auto; }
.run-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 4px; cursor: pointer; margin-bottom: 4px;
  transition: all 0.2s;
}
.run-item:hover { background: #f5f7fa; }
.run-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.run-name { font-family: monospace; font-size: 13px; }
.hint { color: #909399; font-size: 11px; margin-left: 8px; }
.legend-hint { color: #909399; }
</style>
