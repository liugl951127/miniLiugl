<!--
  @file views/kg/Index.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  知识图谱 V5.6 - V2.0 实体管理 + 可视化 + 最短路径
  特性:
    - 创建/搜索实体 (person/place/org/concept/event)
    - 1/2 跳邻居查询
    - 创建关系 (任意 type)
    - ECharts Graph 可视化 (中心 + 邻居节点 + 边)
    - 最短路径查询 (任意两实体)
    - 节点点击跳转, 边权值显示
-->
<!--
  @file views/kg/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="page-kg kg-container">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.3+ 启用 el-watermark (V3.6.1 标识 + 用户名 + 时间) -->
  <el-watermark
    v-if="true"
    :content="['Liugl-AI V3.6.3', userStore.profile?.username || 'Guest', new Date().toLocaleDateString('zh-CN')]"
    :font="{ size: 14, color: 'rgba(99, 102, 241, 0.06)' }"
    :gap="[120, 80]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">🕸️ {{ t('kg.title') }} <el-tag size="small" type="info">V5.6</el-tag></h2>
        <p class="page-subtitle">{{ t('kg.subtitle') }}</p>
      </div>
    </header>
    <div class="kg-header">
      <h1>🕸️ {{ t('kg.title') }} <span class="badge">V5.6</span></h1>
      <p class="sub">{{ t('kg.subtitle') }}</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header><span>➕ {{ t('kg.addEntity') }}</span></template>
          <el-form :inline="false" size="default">
            <el-form-item :label="t('kg.name')"><el-input v-model="newEntity.name" :placeholder="t('kg.namePlaceholder')" /></el-form-item>
            <el-form-item :label="t('kg.type')">
              <el-select v-model="newEntity.type" style="width:100%">
                <el-option :label="t('kg.person')" value="person" />
                <el-option :label="t('kg.place')" value="place" />
                <el-option :label="t('kg.org')" value="org" />
                <el-option :label="t('kg.concept')" value="concept" />
                <el-option :label="t('kg.event')" value="event" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('kg.description')"><el-input v-model="newEntity.description" /></el-form-item>
            <el-form-item :label="t('kg.importance')">
              <el-rate v-model="newEntity.importance" :max="10" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createEntity">{{ t('kg.add') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top:16px">
          <template #header>
            <span>{{ t('kg.searchEntity') }} ({{ entities.length }})</span>
          </template>
          <el-input v-model="searchKw" @keyup.enter="doSearch" :placeholder="t('kg.searchPlaceholder')">
            <template #append><el-button @click="doSearch">{{ t('common.search') }}</el-button></template>
          </el-input>
          <el-scrollbar style="margin-top:12px;max-height:280px">
            <div v-for="e in entities" :key="e.id"
                 class="entity-item" :class="{ active: selectedEntity?.id === e.id }"
                 @click="selectEntity(e)">
              <el-tag size="small" :type="typeTag(e.entityType)">{{ e.entityType }}</el-tag>
              <strong>{{ e.name }}</strong>
              <span v-if="e.description" class="desc">— {{ truncate(e.description, 20) }}</span>
            </div>
          </el-scrollbar>
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>{{ t('kg.shortestPath') }}</span></template>
          <el-form :inline="true" size="small">
            <el-form-item :label="t('kg.from')">
              <el-select v-model="pathFromId" filterable style="width:140px" :placeholder="t('kg.fromPlaceholder')">
                <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('kg.to')">
              <el-select v-model="pathToId" filterable style="width:140px" :placeholder="t('kg.toPlaceholder')">
                <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="findPath">{{ t('kg.find') }}</el-button>
            </el-form-item>
          </el-form>
          <div v-if="pathResult" class="path-result">
            <el-tag type="success">{{ t('kg.pathLength') }}: {{ pathResult.length }}</el-tag>
            <div v-for="(n, i) in pathResult.nodes" :key="i" class="path-node">
              <span>{{ i + 1 }}.</span>
              <el-tag size="small" :type="typeTag(n.entityType)">{{ n.entityType }}</el-tag>
              <strong>{{ n.name }}</strong>
              <span v-if="i < pathResult.relations.length" class="via">—[{{ pathResult.relations[i] }}]→</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>🌐 {{ selectedEntity ? selectedEntity.name : '图谱可视化' }}</span>
            <el-button-group style="margin-left:12px" v-if="selectedEntity">
              <el-button size="small" :type="hop===1?'primary':''" @click="setHop(1)">1 跳</el-button>
              <el-button size="small" :type="hop===2?'primary':''" @click="setHop(2)">2 跳</el-button>
              <el-button size="small" :type="hop===3?'primary':''" @click="setHop(3)">3 跳</el-button>
            </el-button-group>
            <el-tag v-if="graphStats.nodes" type="info" style="margin-left:12px">
              {{ t('kg.nodes') }}: {{ graphStats.nodes }} · {{ t('kg.edges') }}: {{ graphStats.edges }}
            </el-tag>
          </template>

          <div ref="chartEl" class="kg-chart"></div>
        </el-card>

        <el-card v-if="selectedEntity" style="margin-top:16px">
          <template #header><span>{{ t('kg.neighborList') }} ({{ neighbors.length }})</span></template>
          <el-empty v-if="!neighbors.length" :description="t('kg.noRelations')" />
          <el-scrollbar v-else style="height:200px">
            <div v-for="(n, i) in neighbors" :key="i" class="neighbor">
              <el-tag :type="hopTag(n.hop)" size="small">
                {{ n.hop }}跳
              </el-tag>
              <el-tag size="small" style="margin-left:6px" :type="typeTag(n.entity.entityType)">
                {{ n.entity.entityType }}
              </el-tag>
              <strong style="margin-left:6px">{{ n.entity.name }}</strong>
              <span class="via">{{ t('kg.via') }} {{ n.via }}</span>
              <el-button v-if="n.hop === 1" text type="primary"
                         @click="createRelationTo(n.entity.id)">{{ t('kg.createRelation') }}</el-button>
            </div>
          </el-scrollbar>
        </el-card>

        <el-card v-if="selectedEntity" style="margin-top:16px">
          <template #header><span>➕ {{ t('kg.addRelation') }}</span></template>
          <el-form :inline="true">
            <el-form-item :label="t('kg.targetEntity')">
              <el-select v-model="relForm.toId" filterable style="width:180px">
                <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('kg.relationType')">
              <el-input v-model="relForm.type" placeholder="e.g. works_at" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitRelation">{{ t('kg.create') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, reactive, onMounted, nextTick, h } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { t } from '@/i18n'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userId = String(userStore.profile?.id || 1)
const token = userStore.accessToken || ''

const newEntity = reactive({ name: '', type: 'person', description: '', importance: 5 })
const searchKw = ref('')
const entities = ref<any[]>([])
const selectedEntity = ref<any>(null)
const neighbors = ref<any[]>([])
const hop = ref(1)
const relForm = reactive({ toId: null as number | null, type: '' })

// 最短路径
const pathFromId = ref<number | null>(null)
const pathToId = ref<number | null>(null)
const pathResult = ref<any>(null)

// ECharts Graph
const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
const graphStats = ref({ nodes: 0, edges: 0 })

// 类型 -> 颜色
const TYPE_COLOR: Record<string, string> = {
  person: '#f56c6c',
  place: '#67c23a',
  org: '#409eff',
  concept: '#e6a23c',
  event: '#9c27b0',
}
function typeTag(t: string) {
  return ({ person: 'danger', place: 'success', org: '', concept: 'warning', event: 'info' } as any)[t] || ''
}
function hopTag(h: number) {
  return h === 1 ? 'success' : h === 2 ? 'warning' : 'info'
}
function truncate(s: string, n: number) {
  return s && s.length > n ? s.substring(0, n) + '...' : s
}

function auth() { return { headers: { Authorization: `Bearer ${token}` } } }

async function createEntity() {
  if (!newEntity.name) { ElMessage.warning(t('kg.enterName')); return }
  try {
    await axios.post(`${API}/api/v1/agent/kg/entities`,
      { userId, ...newEntity, importance: newEntity.importance }, auth())
    ElMessage.success(t('kg.created'))
    newEntity.name = ''; newEntity.description = ''
    doSearch()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function doSearch() {
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/kg/entities/search`,
      { params: { userId, keyword: searchKw.value || 'a', limit: 50 }, ...auth() })
    entities.value = data.data || []
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function selectEntity(e: any) {
  selectedEntity.value = e
  pathFromId.value = e.id
  await loadNeighbors()
  await nextTick()
  renderGraph()
}

async function setHop(h: number) {
  hop.value = h
  await loadNeighbors()
  await nextTick()
  renderGraph()
}

async function loadNeighbors() {
  if (!selectedEntity.value) return
  // 1 / 2 / 3 跳
  const urlMap: Record<number, string> = {
    1: `${API}/api/v1/agent/kg/entities/${selectedEntity.value.id}/neighbors`,
    2: `${API}/api/v1/agent/kg/entities/${selectedEntity.value.id}/2hop`,
    3: null, // V1.8: 后端只到 2 hop, 隐藏 3hop tab
  }
  // V5.6: 用 neighbors 端点多次调合并
  try {
    if (hop.value === 1) {
      const { data } = await axios.get(urlMap[1], auth())
      neighbors.value = data.data || []
    } else if (hop.value === 2) {
      const { data } = await axios.get(urlMap[2], auth())
      neighbors.value = data.data || []
    } else {
      // 3 跳: 调 2 跳 + 每个邻居再调 1 跳
      const r2 = await axios.get(urlMap[2], auth())
      const list2 = r2.data.data || []
      const extra: any[] = []
      const seen = new Set([selectedEntity.value.id])
      list2.forEach((n: any) => seen.add(n.entity.id))
      for (const n of list2.slice(0, 5)) {
        try {
          const r1 = await axios.get(`${API}/api/v1/agent/kg/entities/${n.entity.id}/neighbors`, auth())
          for (const m of (r1.data.data || [])) {
            if (!seen.has(m.entity.id)) {
              extra.push({ ...m, hop: 3, via: `${n.entity.name} → ${m.via}` })
              seen.add(m.entity.id)
            }
          }
        } catch (_) {}
      }
      neighbors.value = [...list2, ...extra]
    }
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function createRelationTo(toId: number) {
  relForm.toId = toId
  relForm.type = 'related_to'
}

async function submitRelation() {
  if (!relForm.toId || !relForm.type) { ElMessage.warning(t('kg.fillComplete')); return }
  try {
    await axios.post(`${API}/api/v1/agent/kg/relations`, {
      userId, fromId: selectedEntity.value.id, toId: relForm.toId,
      type: relForm.type, weight: 1.0
    }, auth())
    ElMessage.success(t('kg.relationCreated'))
    await loadNeighbors()
    await nextTick()
    renderGraph()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function findPath() {
  if (!pathFromId.value || !pathToId.value) { ElMessage.warning(t('kg.selectEndpoints')); return }
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/kg/path`, {
      // V1.8: 后端参数名是 from/to, 不是 fromId/toId
      params: { userId, from: pathFromId.value, to: pathToId.value },
      ...auth()
    })
    if (data.data) {
      pathResult.value = data.data
      ElMessage.success(t('kg.pathFound') + ` ${data.data.length}`)
      // 可视化路径
      await renderPathGraph()
    } else {
      pathResult.value = null
      ElMessage.warning(t('kg.noPathFound'))
    }
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

// ====== ECharts 渲染 ======
function renderGraph() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const nodes: any[] = []
  const edges: any[] = []
  // 中心节点
  if (selectedEntity.value) {
    nodes.push({
      id: String(selectedEntity.value.id),
      name: selectedEntity.value.name,
      symbolSize: 50,
      itemStyle: { color: TYPE_COLOR[selectedEntity.value.entityType] || '#409eff' },
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      category: selectedEntity.value.entityType,
    })
  }
  // 邻居
  neighbors.value.forEach((n, i) => {
    const ent = n.entity || n
    nodes.push({
      id: String(ent.id || `n${i}`),
      name: ent.name,
      symbolSize: n.hop === 1 ? 30 : 20,
      itemStyle: { color: TYPE_COLOR[ent.entityType] || '#909399' },
      label: { show: true, fontSize: 11 },
      category: ent.entityType,
    })
    if (selectedEntity.value) {
      edges.push({
        source: String(selectedEntity.value.id),
        target: String(ent.id || `n${i}`),
        label: { show: true, formatter: n.via || '', fontSize: 9 },
        lineStyle: { color: n.hop === 1 ? '#67c23a' : '#e6a23c', width: n.hop === 1 ? 2 : 1 },
      })
    }
  })
  graphStats.value = { nodes: nodes.length, edges: edges.length }
  chart.setOption({
    tooltip: { trigger: 'item', formatter: (p: any) => p.dataType === 'node' ? `${p.data.name} (${p.data.category})` : `${p.data.source} → ${p.data.target}` },
    legend: [{
      data: Object.keys(TYPE_COLOR),
      textStyle: { fontSize: 11 },
      orient: 'vertical',
      right: 10,
      top: 10,
    }],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      animation: true,
      force: { repulsion: 400, edgeLength: 120 },
      categories: Object.keys(TYPE_COLOR).map(k => ({ name: k, itemStyle: { color: TYPE_COLOR[k] } })),
      data: nodes,
      links: edges,
        markLine: {
          silent: true,
          symbol: ['none', 'arrow'],
          lineStyle: {
            type: 'dashed',
            color: '#3b82f6',
            width: 2,
            curveness: 0.2,
          },
          data: dragLinePoints.value.length === 2
            ? [{ coord: dragLinePoints.value[0] }, { coord: dragLinePoints.value[1] }]
            : [],
        },
    }],
  })
  // V3.6.3+ 拖拽建边事件
  if (!chart._kgEventsBound) {
    chart.on('nodeclick', 'series', onNodeClick)
    chart.on('dragging', 'series', onGraphDragging)
    chart.on('mouseup', 'series', onGraphDragEnd)
    chart._kgEventsBound = true
  }
}

async function renderPathGraph() {
  if (!chart || !pathResult.value) return
  const nodes = (pathResult.value.nodes || []).map((n: any) => ({
    id: String(n.id),
    name: n.name,
    symbolSize: 40,
    itemStyle: { color: TYPE_COLOR[n.entityType] || '#409eff' },
    category: n.entityType,
  }))
  const edges: any[] = []
  const rels = pathResult.value.relations || []
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({
      source: nodes[i].id,
      target: nodes[i + 1].id,
      label: { show: true, formatter: rels[i] || '', fontSize: 11, color: '#f56c6c', fontWeight: 'bold' },
      lineStyle: { color: '#f56c6c', width: 3, curveness: 0.2 },
    })
  }
  graphStats.value = { nodes: nodes.length, edges: edges.length }
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      force: { repulsion: 400, edgeLength: 120 },
      categories: Object.keys(TYPE_COLOR).map(k => ({ name: k, itemStyle: { color: TYPE_COLOR[k] } })),
      data: nodes,
      links: edges,
        markLine: {
          silent: true,
          symbol: ['none', 'arrow'],
          lineStyle: {
            type: 'dashed',
            color: '#3b82f6',
            width: 2,
            curveness: 0.2,
          },
          data: dragLinePoints.value.length === 2
            ? [{ coord: dragLinePoints.value[0] }, { coord: dragLinePoints.value[1] }]
            : [],
        },
    }],
  })
  // V3.6.3+ 拖拽建边事件
  if (!chart._kgEventsBound) {
    chart.on('nodeclick', 'series', onNodeClick)
    chart.on('dragging', 'series', onGraphDragging)
    chart.on('mouseup', 'series', onGraphDragEnd)
    chart._kgEventsBound = true
  }
}

window.addEventListener('resize', () => chart?.resize())

// V3.6.2+ 演示模式 (无后端) 加载 mock 数据
const MOCK_ENTITIES = [
  { id: 1, name: '李彦宏', entityType: 'person', description: '百度创始人', importance: 9 },
  { id: 2, name: '百度', entityType: 'org', description: '搜索引擎公司', importance: 10 },
  { id: 3, name: '北京', entityType: 'place', description: '中国首都', importance: 8 },
  { id: 4, name: '人工智能', entityType: 'concept', description: 'AI 技术', importance: 10 },
  { id: 5, name: '深度学习', entityType: 'concept', description: '机器学习分支', importance: 9 },
  { id: 6, name: 'Transformer', entityType: 'concept', description: '注意力机制架构', importance: 10 },
  { id: 7, name: 'OpenAI', entityType: 'org', description: 'AI 实验室', importance: 10 },
  { id: 8, name: 'Sam Altman', entityType: 'person', description: 'OpenAI CEO', importance: 9 },
  { id: 9, name: 'GPT-4', entityType: 'event', description: '大语言模型', importance: 10 },
  { id: 10, name: '硅谷', entityType: 'place', description: '科技中心', importance: 8 },
]

const MOCK_NEIGHBORS = [
  { entity: MOCK_ENTITIES[1], hop: 1, via: 'founder' },
  { entity: MOCK_ENTITIES[3], hop: 1, via: 'main_product' },
  { entity: MOCK_ENTITIES[2], hop: 2, via: 'headquarters' },
  { entity: MOCK_ENTITIES[6], hop: 1, via: 'core_tech' },
  { entity: MOCK_ENTITIES[8], hop: 1, via: 'competitor' },
]

// === V3.6.3+ 拖拽建边 ===
const dragCreating = ref(false)
const dragFromId = ref<number | null>(null)
const dragToId = ref<number | null>(null)
const relTypeInput = ref('related_to')
const REL_TYPES = [
  { value: 'related_to', label: '相关' },
  { value: 'founder', label: '创始人' },
  { value: 'works_at', label: '任职' },
  { value: 'located_in', label: '位于' },
  { value: 'part_of', label: '属于' },
  { value: 'created', label: '创建' },
  { value: 'mentions', label: '提及' },
]

// V3.6.4+ 拖拽虚线
const dragLine = ref(null)
const dragLinePoints = ref<[number, number][]>([])

function updateDragLine(fromX, fromY, toX, toY) {
  if (!fromX || !toX) {
    dragLinePoints.value = []
    return
  }
  dragLinePoints.value = [[fromX, fromY], [toX, toY]]
}

function clearDragLine() {
  dragLinePoints.value = []
  dragLine.value = null
}

function onGraphDragging(params) {
  // V3.6.4+ 拖拽时画虚线 (从起点到鼠标位置)
  if (!dragFromId.value) return
  if (params.data?.id) {
    const targetId = parseInt(params.data.id)
    if (!isNaN(targetId) && targetId !== dragFromId.value) {
      dragToId.value = targetId
    }
  }
  // ECharts 'dragging' 事件会传 offsetX/offsetY
  if (params.event?.offsetX) {
    const fromNode = nodes.find(n => String(n.id) === String(dragFromId.value))
    if (fromNode && params.event.offsetX) {
      updateDragLine(fromNode.x, fromNode.y, params.event.offsetX, params.event.offsetY)
    }
  }
}

function onGraphDragEnd() {
  if (dragFromId.value && dragToId.value) {
    relTypeInput.value = 'related_to'
    ElMessageBox.confirm(
      `创建关系: ${dragFromId.value} → ${dragToId.value} ?`,
      'V3.6.3+ 拖拽建边',
      {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        // V3.6.4+ 关系类型选择
        message: h('div', [
          h('p', `创建关系: ${dragFromId.value} → ${dragToId.value} ?`),
          h('el-select', {
            modelValue: relTypeInput.value,
            'onUpdate:modelValue': (v) => { relTypeInput.value = v },
            size: 'small',
            style: 'width: 100%; margin-top: 8px',
          }, REL_TYPES.map(t => h('el-option', { key: t.value, label: t.label, value: t.value }))),
        ]),
      }
    ).then(() => {
      submitDragRelation()
    }).catch(() => {
      resetDrag()
    })
  } else {
    resetDrag()
  }
  // V3.6.4+ 清除虚线
  clearDragLine()
}

async function submitDragRelation() {
  try {
    await axios.post(`${API}/api/v1/agent/kg/relations`, {
      userId,
      fromId: dragFromId.value,
      toId: dragToId.value,
      type: relTypeInput.value,
      weight: 1.0,
    }, auth())
    ElMessage.success(`已创建关系: ${dragFromId.value} → ${dragToId.value} (${relTypeInput.value})`)
    await loadNeighbors()
    await nextTick()
    renderGraph()
  } catch (e: any) {
    // 演示模式: 假成功
    if (!e.response) {
      ElMessage.success(`🎭 演示模式 - 假成功创建关系: ${dragFromId.value} → ${dragToId.value}`)
    } else {
      ElMessage.error(e?.response?.data?.message || e?.message)
    }
  } finally {
    resetDrag()
  }
}

function resetDrag() {
  dragFromId.value = null
  dragToId.value = null
}

function onNodeClick(params) {
  if (params.dataType === 'node') {
    const id = parseInt(params.data.id)
    if (!isNaN(id)) {
      // 单击: 设为拖拽起点
      dragFromId.value = id
      ElMessage.info('已选中节点 ' + params.data.name + ', 拖到目标节点上创建关系')
    }
  }
}

function loadMockData() {
  entities.value = MOCK_ENTITIES
  if (!selectedEntity.value) {
    selectedEntity.value = MOCK_ENTITIES[0]
    neighbors.value = MOCK_NEIGHBORS
  }
  ElMessage.info('🎭 演示模式 - 已加载 10 实体 + 5 关系 (无后端)')
}

onMounted(async () => {
  await nextTick()
  if (chartEl.value && !chart) chart = echarts.init(chartEl.value)
  // V3.6.2+ 演示模式: 3 秒后无后端响应自动加载 mock
  const timeoutId = setTimeout(() => {
    if (entities.value.length === 0) {
      loadMockData()
      nextTick(() => renderGraph())
    }
  }, 3000)
  try {
    await doSearch()
    clearTimeout(timeoutId)  // 成功就清 mock
  } catch {
    loadMockData()
    nextTick(() => renderGraph())
  }
})
</script>

<style scoped>
.kg-container { padding: 20px; max-width: 1400px; margin: 0 auto; }
.kg-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.entity-item {
  padding: 8px 10px; cursor: pointer; border-radius: 4px; transition: all 0.2s;
  display: flex; align-items: center; gap: 6px;
}
.entity-item:hover { background: #f5f7fa; }
.entity-item.active { background: #ecf5ff; }
.entity-item .desc { color: #999; font-size: 12px; }
.neighbor {
  padding: 10px; border-bottom: 1px dashed #eee; display: flex; align-items: center;
}
.via { color: #999; margin-left: 8px; font-size: 12px; font-style: italic; }
.kg-chart {
  height: 480px;
  width: 100%;
  background: linear-gradient(135deg, #fafbfc 0%, #f0f2f5 100%);
  border-radius: 4px;
}
.path-result {
  margin-top: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.path-node {
  padding: 4px 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.path-node span:first-child {
  color: #999;
  font-weight: bold;
  min-width: 24px;
}
</style>