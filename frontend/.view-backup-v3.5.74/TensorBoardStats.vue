<!--
  @file views/ai/TensorBoardStats.vue (TensorBoardStats 页面)
  @version V3.5.12+ (前端注释补全)
  @description TensorBoardStats 页面
-->
<template>
  <div class="tb-stats-container">
    <div class="header">
      <h1>📊 统计分布 <span class="badge">V2.8.9</span></h1>
      <p class="sub">单 run 详细统计 / 多 run 对比 / 直方图</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧: 选择 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>🔍 选择 Run + Tag</span></template>
          <el-form size="small" label-position="top">
            <el-form-item label="Run">
              <el-select v-model="selectedRun" filterable placeholder="选择 run" style="width:100%">
                <el-option v-for="r in runs" :key="r" :value="r" :label="r" />
              </el-select>
            </el-form-item>
            <el-form-item label="Tag">
              <el-select v-model="selectedTag" placeholder="选择 tag" style="width:100%">
                <el-option v-for="t in tags" :key="t" :value="t" :label="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="直方图 Bins">
              <el-input-number v-model="bins" :min="5" :max="50" :step="1" />
            </el-form-item>
            <el-button type="primary" @click="loadAll" :loading="loading" style="width:100%">
              加载
            </el-button>
          </el-form>
        </el-card>

        <el-card v-if="stats" style="margin-top: 16px">
          <template #header><span>📐 统计指标</span></template>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="样本数">{{ stats.count }}</el-descriptions-item>
            <el-descriptions-item label="最小">{{ format(stats.min) }}</el-descriptions-item>
            <el-descriptions-item label="最大">{{ format(stats.max) }}</el-descriptions-item>
            <el-descriptions-item label="均值">{{ format(stats.mean) }}</el-descriptions-item>
            <el-descriptions-item label="标准差">{{ format(stats.std) }}</el-descriptions-item>
            <el-descriptions-item label="中位数">{{ format(stats.median) }}</el-descriptions-item>
            <el-descriptions-item label="P25">{{ format(stats.p25) }}</el-descriptions-item>
            <el-descriptions-item label="P75">{{ format(stats.p75) }}</el-descriptions-item>
            <el-descriptions-item label="P95">{{ format(stats.p95) }}</el-descriptions-item>
            <el-descriptions-item label="P99">{{ format(stats.p99) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧: 图表 -->
      <el-col :span="16">
        <!-- 直方图 -->
        <el-card>
          <template #header>
            <span>📊 分布直方图</span>
            <span v-if="histogram" style="float:right;font-size:12px;color:#909399">
              {{ histogram.count }} 个样本 / {{ bins }} 个 bins
            </span>
          </template>
          <v-chart
            v-if="histogram"
            :option="histOption"
            :loading="loading"
            style="height: 350px"
            autoresize
          />
          <el-empty v-else description="选择 run + tag 后加载" />
        </el-card>

        <!-- 折线图 (带 ±std 阴影) -->
        <el-card style="margin-top: 16px">
          <template #header>
            <span>📈 趋势 + ±1σ</span>
          </template>
          <v-chart
            v-if="trendData.steps.length"
            :option="trendOption"
            style="height: 300px"
            autoresize
          />
          <el-empty v-else description="无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 多 run 对比 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>🔀 多 Run 对比 (Tag: {{ selectedTag || '请选择' }})</span>
      </template>
      <el-form :inline="true" size="small">
        <el-form-item label="Run">
          <el-select v-model="compareRunsList" multiple filterable placeholder="选择多个 run" style="width: 400px">
            <el-option v-for="r in runs" :key="r" :value="r" :label="r" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadCompare" :loading="loadingCompare">对比</el-button>
      </el-form>

      <el-table v-if="compareTable.length" :data="compareTable" stripe size="small" style="margin-top: 12px">
        <el-table-column prop="runId" label="Run" width="160" />
        <el-table-column prop="count" label="样本" width="80" />
        <el-table-column prop="min" label="Min" :formatter="fmt" />
        <el-table-column prop="max" label="Max" :formatter="fmt" />
        <el-table-column prop="mean" label="Mean" :formatter="fmt" />
        <el-table-column prop="std" label="Std" :formatter="fmt" />
        <el-table-column prop="median" label="Median" :formatter="fmt" />
        <el-table-column prop="p95" label="P95" :formatter="fmt" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent
} from 'echarts/components'
import { tensorboardApi } from '@/api/tensorboard'

use([CanvasRenderer, BarChart, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const runs = ref([])
const tags = ref([])
const selectedRun = ref('')
const selectedTag = ref('loss')
const bins = ref(20)
const loading = ref(false)
const loadingCompare = ref(false)
const stats = ref(null)
const histogram = ref(null)
const trendData = reactive({ steps: [], values: [] })
const compareRunsList = ref([])
const compareTable = ref([])

const histOption = computed(() => {
  if (!histogram.value) return {}
  const counts = histogram.value.counts
  const edges = histogram.value.bins
  const data = counts.map((c, i) => ({
    value: [(edges[i] + edges[i + 1]) / 2, c],
    label: `${edges[i].toFixed(3)} ~ ${edges[i + 1].toFixed(3)}`
  }))
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => `<b>${p.data.label}</b><br/>数量: ${p.data.value[1]}`
    },
    grid: { left: 60, right: 30, top: 30, bottom: 50 },
    xAxis: { type: 'value', name: '值', nameLocation: 'middle', nameGap: 30 },
    yAxis: { type: 'value', name: '频次', nameLocation: 'middle', nameGap: 40 },
    series: [{
      type: 'bar',
      data: data,
      itemStyle: { color: '#6366f1' },
      barWidth: '95%'
    }]
  }
})

const trendOption = computed(() => {
  if (!trendData.steps.length) return {}
  const mean = stats.value?.mean || 0
  const std = stats.value?.std || 0
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 30, bottom: 50 },
    xAxis: { type: 'value', name: 'Step', nameLocation: 'middle', nameGap: 30 },
    yAxis: { type: 'value', name: 'Value', nameLocation: 'middle', nameGap: 50, scale: true },
    series: [
      {
        name: '实际值',
        type: 'line',
        data: trendData.steps.map((s, i) => [s, trendData.values[i]]),
        showSymbol: false,
        smooth: true,
        lineStyle: { color: '#6366f1', width: 2 }
      },
      {
        name: '+1σ',
        type: 'line',
        data: trendData.steps.map(s => [s, mean + std]),
        showSymbol: false,
        lineStyle: { type: 'dashed', color: '#10b981', width: 1 }
      },
      {
        name: '均值',
        type: 'line',
        data: trendData.steps.map(s => [s, mean]),
        showSymbol: false,
        lineStyle: { type: 'dotted', color: '#f59e0b', width: 1 }
      },
      {
        name: '-1σ',
        type: 'line',
        data: trendData.steps.map(s => [s, mean - std]),
        showSymbol: false,
        lineStyle: { type: 'dashed', color: '#ef4444', width: 1 }
      }
    ]
  }
})

const format = (v) => {
  if (v == null) return '-'
  if (Math.abs(v) < 0.001 || Math.abs(v) > 10000) return v.toExponential(3)
  return v.toFixed(4)
}

const fmt = ({ value }) => format(value)

const loadRuns = async () => {
  try {
    const res = await tensorboardApi.listRuns()
    runs.value = res.data || []
    if (runs.value.length) selectedRun.value = runs.value[0]
  } catch (e) { console.warn(e) }
}

const loadTags = async () => {
  if (!selectedRun.value) { tags.value = []; return }
  try {
    const res = await tensorboardApi.listTags(selectedRun.value)
    tags.value = res.data || []
    if (tags.value.length && !tags.value.includes(selectedTag.value)) {
      selectedTag.value = tags.value[0]
    }
  } catch (e) { console.warn(e) }
}

const loadAll = async () => {
  if (!selectedRun.value || !selectedTag.value) return
  loading.value = true
  try {
    // 1. stats
    const statsRes = await tensorboardApi.readStats(selectedRun.value, selectedTag.value)
    stats.value = statsRes.data
    // 2. histogram
    const histRes = await tensorboardApi.readHistogram(selectedRun.value, selectedTag.value, bins.value)
    histogram.value = histRes.data
    // 3. trend data
    const scalarRes = await tensorboardApi.readScalar(selectedRun.value, selectedTag.value)
    const pts = scalarRes.data?.data?.points || []
    trendData.steps = pts.map(p => p.step)
    trendData.values = pts.map(p => p.value)
  } catch (e) {
    console.warn('loadAll failed:', e)
  } finally {
    loading.value = false
  }
}

const loadCompare = async () => {
  if (!compareRunsList.value.length || !selectedTag.value) return
  loadingCompare.value = true
  try {
    const res = await tensorboardApi.compareRuns(compareRunsList.value, selectedTag.value)
    compareTable.value = res.data || []
  } catch (e) {
    console.warn('compare failed:', e)
  } finally {
    loadingCompare.value = false
  }
}

watch(selectedRun, loadTags)
onMounted(async () => {
  await loadRuns()
  await loadTags()
  if (selectedRun.value && selectedTag.value) await loadAll()
})
</script>

<style scoped>
.tb-stats-container { padding: 20px; }
.header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #67c23a; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }
</style>
