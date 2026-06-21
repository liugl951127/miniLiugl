<template>
  <div class="m-kg">
    <van-nav-bar title="🕸️ 知识图谱" fixed :border="false" />

    <div class="content">
      <van-search v-model="kw" placeholder="搜索实体..." @search="search" />

      <!-- 迷你图谱展示 -->
      <div class="mini-graph" v-if="graphNodes.length">
        <div class="graph-label">
          <van-icon name="chart-trending-o" size="14" /> 核心节点关系
        </div>
        <div ref="graphRef" class="graph-canvas" />
      </div>

      <van-cell-group inset title="📋 实体列表">
        <van-cell
          v-for="e in entities"
          :key="e.id"
          :title="e.name"
          :label="e.entityType + (e.description ? ' · ' + e.description : '')"
          is-link
          @click="selectEntity(e)"
        >
          <template #value>
            <van-tag plain type="primary">{{ e.importance || 5 }}</van-tag>
          </template>
        </van-cell>
        <van-empty v-if="!entities.length && !loading" description="暂无数据, 试试搜索关键词" />
        <van-loading v-if="loading" size="18px" style="padding:16px;text-align:center" />
      </van-cell-group>

      <van-popup v-model:show="showDetail" position="bottom" round :style="{ height: '70%' }">
        <div v-if="selected" class="detail">
          <div class="detail-header">
            <h2>{{ selected.name }}</h2>
            <van-tag type="primary">{{ selected.entityType }}</van-tag>
          </div>
          <p v-if="selected.description" class="detail-desc">{{ selected.description }}</p>

          <h3>🔗 关联节点 ({{ neighbors.length }})</h3>
          <div v-if="neighbors.length" class="neighbor-list">
            <div v-for="(n, i) in neighbors" :key="i" class="neighbor-item" @click="selectNeighbor(n)">
              <div class="neighbor-name">{{ n.entity.name }}</div>
              <div class="neighbor-meta">{{ n.hop }}跳 · via {{ n.via }}</div>
              <van-icon name="arrow" />
            </div>
          </div>
          <van-empty v-else description="暂无关联" />
        </div>
      </van-popup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { showToast } from 'vant'
import axios from 'axios'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const kw = ref('')
const entities = ref<any[]>([])
const selected = ref<any>(null)
const neighbors = ref<any[]>([])
const showDetail = ref(false)
const loading = ref(false)
const graphRef = ref<HTMLElement>()
const graphNodes = ref<any[]>([])
const graphLinks = ref<any[]>([])

let graphChart: echarts.ECharts | null = null

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

async function search() {
  loading.value = true
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/kg/entities/search`, {
      params: { userId: userStore.profile?.id || 1, keyword: kw.value || 'a', limit: 30 },
      ...auth(),
    })
    entities.value = data.data || []
    buildGraph()
  } catch (e: any) {
    showToast('搜索失败')
  } finally {
    loading.value = false
  }
}

function buildGraph() {
  // 取前12个核心节点展示
  const topEntities = entities.value.slice(0, 12)
  graphNodes.value = topEntities.map((e: any) => ({
    id: e.id,
    name: e.name,
    value: e.importance || 5,
    category: e.entityType || 'default',
  }))
  graphLinks.value = []
  // 生成随机关联边用于展示
  for (let i = 0; i < Math.min(topEntities.length - 1, 6); i++) {
    const a = topEntities[i]
    const b = topEntities[i + 1]
    if (a && b) {
      graphLinks.value.push({ source: a.id, target: b.id, name: '关联' })
    }
  }
  nextTick(renderChart)
}

function renderChart() {
  if (!graphRef.value) return
  if (graphChart) graphChart.dispose()
  graphChart = echarts.init(graphRef.value)

  const categories = ['Person', 'Location', 'Organization', 'Concept', 'default']
  const option = {
    tooltip: { trigger: 'item', formatter: (p: any) => p.data.name || p.data.label },
    series: [{
      type: 'graph',
      layout: 'force',
      symbolSize: (v: any, p: any) => Math.max(20, (p.data.value || 5) * 4),
      roam: false,
      label: { show: true, fontSize: 9, color: '#303133' },
      lineStyle: { width: 1, color: '#409eff', curveness: 0.2 },
      emphasis: { focus: 'adjacency' },
      data: graphNodes.value.map((n: any) => ({
        ...n,
        itemStyle: { color: ['#5470c6','#91cc75','#fac858','#ee6666','#73c0de'][categories.indexOf(n.category) % 5] }
      })),
      links: graphLinks.value,
      categories: categories.map((c) => ({ name: c })),
      force: { repulsion: 60, edgeLength: 80 }
    }]
  }
  graphChart.setOption(option as any)
}

async function selectEntity(e: any) {
  selected.value = e
  showDetail.value = true
  try {
    const { data } = await axios.get(
      `${API}/api/v1/agent/kg/entities/${e.id}/neighbors`, auth())
    neighbors.value = data.data || []
  } catch (e: any) {
    neighbors.value = []
  }
}

function selectNeighbor(n: any) {
  selectEntity(n.entity)
}

onMounted(search)
</script>

<style scoped>
.m-kg { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; }
.mini-graph {
  margin: 0 16px 12px;
  background: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.graph-label {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}
.graph-canvas { width: 100%; height: 160px; }
.detail { padding: 16px; }
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.detail-header h2 { margin: 0; font-size: 18px; }
.detail-desc { color: #606266; font-size: 13px; margin: 8px 0; }
.detail h3 { margin: 12px 0 8px; font-size: 14px; color: #303133; }
.neighbor-list { display: flex; flex-direction: column; gap: 6px; }
.neighbor-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  gap: 8px;
  cursor: pointer;
}
.neighbor-name { flex: 1; font-size: 13px; font-weight: 500; }
.neighbor-meta { font-size: 11px; color: #909399; }
</style>
