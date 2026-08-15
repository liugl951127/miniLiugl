<!-- @file kg/Index.vue - 知识图谱 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>知识图谱</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="searchKw" placeholder="搜索实体" size="small" style="width:200px" clearable @change="searchEntities">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="small" @click="showUpload = true">
          <el-icon><Upload /></el-icon>导入
        </el-button>
        <el-button size="small" @click="loadKg">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:#409eff">{{ stats.entities }}</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">实体数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:#67c23a">{{ stats.edges }}</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">关系数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:#e6a23c">{{ stats.types }}</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">类型数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:#909399">-</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">查询次数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图谱可视化 -->
    <el-card style="margin-bottom:12px">
      <template #header><span>图谱可视化</span></template>
      <div class="kg-canvas" ref="canvasRef">
        <svg ref="svgRef" :width="canvasW" :height="canvasH" @click="onSvgClick">
          <!-- 边 -->
          <g v-for="(e, i) in edges" :key="'e'+i">
            <line
              :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"
              stroke="#dcdfe6" stroke-width="1.5"
              :marker-end="selectedEntity ? 'url(#arrow)' : ''"
            />
            <text
              v-if="selectedEntity"
              :x="(e.x1+e.x2)/2" :y="(e.y1+e.y2)/2-6"
              text-anchor="middle" font-size="11" fill="#909399"
            >{{ e.label }}</text>
          </g>
          <!-- 节点 -->
          <g v-for="n in nodes" :key="n.id"
             :transform="`translate(${n.x},${n.y})`"
             style="cursor:pointer"
             @click.stop="selectEntity(n)">
            <circle r="28" :fill="n.color" opacity="0.85" />
            <text text-anchor="middle" dy="4" font-size="12" fill="#fff" font-weight="500">
              {{ n.label.length > 6 ? n.label.slice(0,6)+'…' : n.label }}
            </text>
          </g>
          <defs>
            <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#909399" />
            </marker>
          </defs>
        </svg>
        <div v-if="loading" class="kg-loading"><el-icon class="is-loading"><Loading /></el-icon> 加载中…</div>
        <div v-if="!loading && nodes.length === 0" class="kg-empty">暂无图谱数据，请先导入实体</div>
      </div>
    </el-card>

    <!-- 选中实体详情 -->
    <el-card v-if="selectedEntity">
      <template #header>
        <span>实体详情: {{ selectedEntity.label }}</span>
        <el-button size="small" link type="primary" style="float:right" @click="selectedEntity = null">关闭</el-button>
      </template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="ID">{{ selectedEntity.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag size="small">{{ selectedEntity.type }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="neighbors.length">
        <div style="font-size:13px;font-weight:600;margin:10px 0 6px">关联实体 ({{ neighbors.length }})</div>
        <el-tag v-for="nb in neighbors" :key="nb.id" size="small"
          style="margin:0 4px 4px 0;cursor:pointer"
          @click="selectEntity({ id: nb.id, label: nb.name, type: nb.type, color: '#67c23a' })">
          {{ nb.name }}
        </el-tag>
      </div>
    </el-card>

    <!-- 实体列表 -->
    <el-card style="margin-top:12px">
      <template #header><span>实体列表</span></template>
      <el-table :data="entities" stripe size="small" v-loading="loading">
        <el-table-column prop="id" label="ID" width="220" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" link @click="viewNeighbors(row)">邻居</el-button>
            <el-button size="small" link @click="selectEntity({ id: row.id, label: row.name, type: row.type, color: '#409eff' })">图谱</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" :page-size="20" :total="total"
        layout="total, prev, pager, next" style="margin-top:10px;justify-content:center"
        @current-change="loadEntities"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { kgSearchEntities, kgGetEntity, kgNeighbors } from '@/api/monitor'
import { Upload, Search, Refresh, Loading } from '@element-plus/icons-vue'

const canvasRef = ref(null)
const svgRef = ref(null)
const canvasW = ref(800)
const canvasH = ref(320)

const nodes = ref([])
const edges = ref([])
const entities = ref([])
const neighbors = ref([])
const selectedEntity = ref(null)
const showUpload = ref(false)
const loading = ref(false)
const searchKw = ref('')
const page = ref(1)
const total = ref(0)

const stats = reactive({ entities: 0, edges: 0, types: 0 })

const NODE_COLORS = {
  PERSON: '#409eff', ORG: '#67c23a', PLACE: '#e6a23c',
  PRODUCT: '#f56c6c', EVENT: '#909399', DEFAULT: '#7c3aed'
}
function nodeColor(type) {
  return type ? (NODE_COLORS[type.toUpperCase()] || NODE_COLORS.DEFAULT) : NODE_COLORS.DEFAULT
}

function layoutCircle(items, cx, cy, r) {
  const n = items.length
  return items.map((item, i) => ({
    ...item,
    x: cx + r * Math.cos(2 * Math.PI * i / n - Math.PI / 2),
    y: cy + r * Math.sin(2 * Math.PI * i / n - Math.PI / 2),
  }))
}

async function loadKg() {
  loading.value = true
  nodes.value = []
  edges.value = []
  try {
    // 加载实体列表用于统计
    const r = await kgSearchEntities(null, '', 50)
    const list = r.data?.list || r.data || []
    stats.entities = r.data?.total || list.length
    entities.value = list

    // 取前 12 个做可视化（中心发散布局）
    const top = list.slice(0, 12)
    if (top.length > 0) {
      const cx = canvasW.value / 2
      const cy = canvasH.value / 2
      // 中心节点
      const center = { id: 'center', label: '知识图谱', type: 'ROOT', x: cx, y: cy, color: '#7c3aed' }
      nodes.value = [center, ...layoutCircle(top.map(e => ({ id: e.id, label: e.name, type: e.type, color: nodeColor(e.type) })), cx, cy, Math.min(canvasW.value, canvasH.value) * 0.38)]
      edges.value = top.map(e => ({
        x1: cx, y1: cy,
        x2: nodes.value.find(n => n.id === e.id)?.x || cx,
        y2: nodes.value.find(n => n.id === e.id)?.y || cy,
        label: '关联'
      }))
      stats.edges = top.length
      const types = new Set(top.map(e => e.type).filter(Boolean))
      stats.types = types.size || 1
    }
  } catch (e) {
    ElMessage.error('加载图谱失败')
  } finally {
    loading.value = false
  }
}

async function loadEntities() {
  loading.value = true
  try {
    const r = await kgSearchEntities(null, searchKw.value, 20)
    const list = r.data?.list || r.data || []
    entities.value = list
    total.value = r.data?.total || list.length
  } catch {
    entities.value = []
  } finally {
    loading.value = false
  }
}

async function searchEntities() {
  page.value = 1
  await loadEntities()
}

async function viewNeighbors(row) {
  try {
    const r = await kgNeighbors(row.id)
    neighbors.value = r.data || []
    ElMessage.success(`找到 ${neighbors.value.length} 个邻居`)
  } catch {
    ElMessage.error('加载邻居失败')
    neighbors.value = []
  }
}

async function selectEntity(n) {
  selectedEntity.value = n
  nodes.value = nodes.value.map(x => x.id === n.id ? { ...x, color: '#f56c6c' } : x)
  try {
    const r = await kgNeighbors(n.id)
    neighbors.value = r.data || []
    const nbList = (r.data || []).slice(0, 8)
    if (nbList.length > 0) {
      const cx = canvasW.value * 0.7
      const cy = canvasH.value / 2
      const r2 = Math.min(canvasW.value, canvasH.value) * 0.3
      const nbNodes = layoutCircle(nbList.map(e => ({ id: e.id, label: e.name, type: e.type, color: nodeColor(e.type) })), cx, cy, r2)
      const centerNode = { id: n.id, label: n.label, type: n.type, x: canvasW.value * 0.25, y: cy, color: '#f56c6c' }
      nodes.value = [centerNode, ...nbNodes]
      edges.value = nbList.map((e, i) => ({
        x1: centerNode.x, y1: centerNode.y,
        x2: nbNodes[i].x, y2: nbNodes[i].y,
        label: e.relation || '关联'
      }))
    }
  } catch {}
}

function onSvgClick() {
  // 点击空白处重置
}

onMounted(() => {
  nextTick(() => {
    if (canvasRef.value) {
      canvasW.value = canvasRef.value.offsetWidth
      canvasH.value = canvasRef.value.offsetHeight || 320
    }
    loadKg()
  })
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.kg-canvas { position: relative; height: 320px; border: 1px solid #f0f0f0; border-radius: 4px; background: #fafafa; overflow: hidden; }
.kg-canvas svg { display: block; width: 100%; height: 100%; }
.kg-loading, .kg-empty {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  color: #909399; font-size: 14px; gap: 8px;
}
</style>
