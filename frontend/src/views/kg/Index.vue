<!-- @file kg/Index.vue - 知识图谱 V7.0 - D3力导向布局 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>知识图谱</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-input v-model="searchKw" placeholder="搜索实体" size="small" style="width:200px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="small" @click="showUpload = true">
          <el-icon><Upload /></el-icon>导入
        </el-button>
        <el-button size="small" @click="showExportMenu = !showExportMenu">
          <el-icon><Download /></el-icon>导出
        </el-button>
        <el-button size="small" @click="loadKg">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 导出下拉菜单 -->
    <div v-if="showExportMenu" class="export-dropdown" v-click-outside="() => showExportMenu = false">
      <div class="export-item" @click="exportPNG">导出 PNG</div>
      <div class="export-item" @click="exportJSON">导出 JSON</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color: var(--el-color-primary)">{{ stats.entities }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">实体数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color: var(--el-color-success)">{{ stats.edges }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">关系数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color: var(--el-color-warning)">{{ stats.types }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">类型数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color: var(--el-text-color-secondary)">-</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">查询次数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图谱可视化 -->
    <el-card style="margin-bottom:12px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>图谱可视化</span>
          <div style="display:flex;gap:8px;align-items:center;font-size:12px;color: var(--el-text-color-secondary)">
            <span>缩放: {{ Math.round(zoomScale * 100) }}%</span>
            <el-button size="small" link @click="resetView">重置</el-button>
          </div>
        </div>
      </template>
      <div class="kg-canvas" ref="canvasRef">
        <svg ref="svgRef" class="kg-svg">
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="25" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#909399" />
            </marker>
            <marker id="arrowhead-highlight" markerWidth="10" markerHeight="7" refX="25" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#409eff" />
            </marker>
            <filter id="glow">
              <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>
          <!-- 缩放平移容器 -->
          <g ref="zoomGroup">
            <!-- 连线层 -->
            <g class="links-layer" ref="linksLayer"></g>
            <!-- 节点层 -->
            <g class="nodes-layer" ref="nodesLayer"></g>
          </g>
        </svg>
        <!-- 搜索结果提示 -->
        <div v-if="searchResults.length > 0" class="search-results-panel">
          <div class="search-results-header">搜索结果 ({{ searchResults.length }})</div>
          <div v-for="item in searchResults" :key="item.id" class="search-result-item" @click="focusNode(item)">
            <span class="node-type-dot" :style="{ background: nodeColor(item.type) }"></span>
            <span class="node-name">{{ item.name }}</span>
            <span class="node-type-label">{{ item.type }}</span>
          </div>
        </div>
        <div v-if="searchNoMatch" class="kg-empty">未找到匹配的实体</div>
        <div v-if="loading" class="kg-loading"><el-icon class="is-loading"><Loading /></el-icon> 加载中…</div>
        <div v-if="!loading && nodes.length === 0" class="kg-empty">暂无图谱数据，请先导入实体</div>
        <div v-if="showLimitTip" class="kg-limit-tip">显示前 {{ displayLimit }} 个节点</div>
      </div>
    </el-card>

    <!-- 选中实体详情面板 -->
    <div v-if="selectedEntity" class="entity-detail-panel">
      <div class="panel-header">
        <span>实体详情</span>
        <el-button size="small" link type="primary" @click="closeDetail">关闭</el-button>
      </div>
      <div class="panel-body">
        <div class="entity-name-row">
          <span class="node-type-dot large" :style="{ background: nodeColor(selectedEntity.type) }"></span>
          <span class="entity-name">{{ selectedEntity.label }}</span>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ selectedEntity.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag size="small">{{ selectedEntity.type }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="relationLabels.length">
          <div class="section-title">关系详情</div>
          <div class="relation-list">
            <div v-for="(rel, idx) in relationLabels" :key="idx" class="relation-item">
              <span class="relation-label">{{ rel.label }}</span>
              <span class="relation-target">{{ rel.target }}</span>
            </div>
          </div>
        </div>
        <div v-if="neighbors.length">
          <div class="section-title">关联实体 ({{ neighbors.length }})</div>
          <div class="neighbors-grid">
            <el-tag v-for="nb in neighbors" :key="nb.id" size="small" class="neighbor-tag"
              @click="focusToNeighbor(nb)">
              {{ nb.name }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 实体列表 -->
    <el-card style="margin-top:12px">
      <template #header>
        <span>实体列表</span>
        <el-button size="small" type="primary" link @click="showAddForm = !showAddForm">
          <el-icon><Plus /></el-icon>添加实体
        </el-button>
      </template>
      <!-- 添加实体/关系表单 -->
      <el-form
        v-if="showAddForm"
        ref="addFormRef"
        :model="addForm"
        :rules="addFormRules"
        inline
        size="small"
        style="margin-bottom:12px;padding:12px;background: var(--el-fill-color-light);border-radius:4px"
        @submit.prevent
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="addForm.name" placeholder="实体名称" style="width:140px" maxlength="50" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="addForm.type" placeholder="选择类型" style="width:120px">
            <el-option label="PERSON" value="PERSON" />
            <el-option label="ORG" value="ORG" />
            <el-option label="PLACE" value="PLACE" />
            <el-option label="PRODUCT" value="PRODUCT" />
            <el-option label="EVENT" value="EVENT" />
            <el-option label="OTHER" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系目标">
          <el-input v-model="addForm.relation" placeholder="目标实体名称" style="width:140px" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="关系名">
          <el-input v-model="addForm.relationLabel" placeholder="关系名称" style="width:100px" maxlength="20" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="addEntityLoading" @click="handleAddEntity">添加</el-button>
          <el-button size="small" @click="resetAddForm">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="entities" stripe size="small" v-loading="loading" empty-text="暂无实体，先添加一个吧">
        <el-table-column prop="id" label="ID" width="220" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" link :loading="loadingNeighborsId === row.id" @click="viewNeighbors(row)">邻居</el-button>
            <el-button size="small" link type="primary" @click="selectEntity(row)">图谱</el-button>
            <el-button size="small" link type="danger" :loading="deletingEntityId === row.id" @click="confirmDeleteEntity(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!loading && entities.length === 0 && !searchKw"
        description="暂无实体，点击右上角「添加实体」开始构建图谱"
        :image-size="60"
        style="margin-top:16px"
      />
      <el-empty
        v-else-if="!loading && entities.length === 0 && searchKw"
        :description="`未找到匹配 \"${searchKw}\" 的实体`"
        :image-size="60"
        style="margin-top:16px"
      />
      <el-pagination
        v-model:current-page="page" :page-size="20" :total="total"
        layout="total, prev, pager, next" style="margin-top:10px;justify-content:center"
        @current-change="loadEntities"
      />
    </el-card>

    <!-- 导入弹窗 -->
    <el-dialog v-model="showUpload" title="导入图谱数据" width="500px" :close-on-click-modal="!importing">
      <div class="upload-area">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="true"
          accept=".json"
          :on-change="handleFileChange"
          :limit="1"
          :disabled="importing"
        >
          <template #trigger>
            <el-button type="primary" :disabled="importing">选择 JSON 文件</el-button>
          </template>
        </el-upload>
        <div class="upload-tip">
          <p>支持 JSON 格式批量导入：</p>
          <pre class="json-example">{
  "entities": [
    {"name": "张三", "type": "PERSON"},
    {"name": "阿里巴巴", "type": "ORG"}
  ],
  "relations": [
    {"fromName": "张三", "toName": "阿里巴巴", "label": "工作于"}
  ]
}</pre>
        </div>
        <el-progress
          v-if="importing"
          :percentage="importProgress"
          :status="importProgress >= 100 ? 'success' : undefined"
          :indeterminate="importProgress < 5"
          style="margin-top:12px"
        />
        <div v-if="importing" class="import-progress-text">
          正在导入 {{ importCurrentIndex }} / {{ importTotalCount }} ...
        </div>
      </div>
      <template #footer>
        <el-button @click="showUpload = false" :disabled="importing">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { kgSearchEntities, kgGetEntity, kgNeighbors } from '@/api/monitor'
import { kgUpsertEntity, kgCreateRelation, kgDeleteEntity, kgBatchImportEntities, kgBatchImportRelations } from '@/api/kg'
import { useUserStore } from '@/store/user'
import { Upload, Search, Refresh, Loading, Download, Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.profile?.id || userStore.userInfo?.id || null)

// SVG 引用
const canvasRef = ref(null)
const svgRef = ref(null)
const zoomGroup = ref(null)
const linksLayer = ref(null)
const nodesLayer = ref(null)

// 画布尺寸
const canvasW = ref(800)
const canvasH = ref(450)

// 状态
const nodes = ref([])
const links = ref([])
const entities = ref([])
const neighbors = ref([])
const selectedEntity = ref(null)
const relationLabels = ref([])
const showUpload = ref(false)
const showExportMenu = ref(false)
const showAddForm = ref(false)
const loading = ref(false)
const searchKw = ref('')
const searchResults = ref([])
const searchNoMatch = ref(false)
const showLimitTip = ref(false)
const displayLimit = ref(50)
const page = ref(1)
const total = ref(0)
const zoomScale = ref(1)
const uploadRef = ref(null)
const pendingFile = ref(null)

// 导入进度
const importing = ref(false)
const importProgress = ref(0)
const importCurrentIndex = ref(0)
const importTotalCount = ref(0)

// 添加实体 loading
const addEntityLoading = ref(false)
const addFormRef = ref(null)

// 实体操作 loading
const loadingNeighborsId = ref(null)
const deletingEntityId = ref(null)

// D3 相关
let simulation = null
let svg = null
let zoom = null
let currentTransform = null

// 统计数据
const stats = reactive({ entities: 0, edges: 0, types: 0 })

// 添加表单
const addForm = reactive({
  name: '',
  type: 'PERSON',
  relation: '',
  relationLabel: ''
})
const addFormRules = {
  name: [
    { required: true, message: '请输入实体名称', trigger: 'blur' },
    { min: 1, max: 50, message: '名称长度应在 1-50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择实体类型', trigger: 'change' }
  ]
}

// 节点颜色映射
const NODE_COLORS = {
  PERSON: '#409eff',
  ORG: '#67c23a',
  PLACE: '#e6a23c',
  PRODUCT: '#f56c6c',
  EVENT: '#909399',
  OTHER: '#7c3aed',
  DEFAULT: '#7c3aed'
}

function nodeColor(type) {
  if (!type) return NODE_COLORS.DEFAULT
  return NODE_COLORS[type.toUpperCase()] || NODE_COLORS.DEFAULT
}

// 截断名称
function truncateName(name, maxLen = 10) {
  if (!name) return ''
  return name.length > maxLen ? name.slice(0, maxLen) + '…' : name
}

// 初始化 D3
function initD3() {
  if (!svgRef.value) return

  svg = d3.select(svgRef.value)
    .attr('width', canvasW.value)
    .attr('height', canvasH.value)

  // 缩放行为
  zoom = d3.zoom()
    .scaleExtent([0.2, 3])
    .on('zoom', (event) => {
      currentTransform = event.transform
      zoomScale.value = event.transform.k
      d3.select(zoomGroup.value).attr('transform', event.transform)
    })

  svg.call(zoom)
    .on('dblclick.zoom', null) // 禁用双击缩放

  // 初始化力模拟
  simulation = d3.forceSimulation()
    .force('link', d3.forceLink().id(d => d.id).distance(120).strength(0.5))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(canvasW.value / 2, canvasH.value / 2))
    .force('collision', d3.forceCollide().radius(40))
    .on('tick', ticked)

  // 拖拽空白区域平移
  svg.on('click', (event) => {
    if (event.target === svgRef.value || event.target.tagName === 'svg') {
      closeDetail()
    }
  })
}

// 更新力模拟数据
function updateGraph() {
  if (!simulation || !linksLayer.value || !nodesLayer.value) return

  // 更新连线
  const link = d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .data(links.value, d => `${d.source.id || d.source}-${d.target.id || d.target}`)

  link.exit().remove()

  const linkEnter = link.enter()
    .append('g')
    .attr('class', 'link-group')

  linkEnter.append('line')
    .attr('class', 'link-line')
    .attr('stroke', '#dcdfe6')
    .attr('stroke-width', 1.5)
    .attr('marker-end', 'url(#arrowhead)')

  linkEnter.append('text')
    .attr('class', 'link-label')
    .attr('text-anchor', 'middle')
    .attr('dy', -8)
    .attr('font-size', 11)
    .attr('fill', '#909399')
    .style('opacity', 0)

  const linkMerge = linkEnter.merge(link)

  linkMerge.select('line')
    .attr('marker-end', selectedEntity.value ? 'url(#arrowhead)' : 'url(#arrowhead)')

  linkMerge.select('text')
    .text(d => d.label || '')

  // 更新节点
  const node = d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .data(nodes.value, d => d.id)

  node.exit().remove()

  const nodeEnter = node.enter()
    .append('g')
    .attr('class', 'node-group')
    .style('cursor', 'pointer')

  // 节点圆形
  nodeEnter.append('circle')
    .attr('class', 'node-circle')
    .attr('r', 28)
    .attr('fill', d => nodeColor(d.type))
    .attr('opacity', 0.85)
    .attr('stroke', '#fff')
    .attr('stroke-width', 2)

  // 节点名称
  nodeEnter.append('text')
    .attr('class', 'node-label')
    .attr('text-anchor', 'middle')
    .attr('dy', 4)
    .attr('font-size', 12)
    .attr('fill', '#fff')
    .attr('font-weight', 500)
    .text(d => truncateName(d.label))

  // 脉冲效果（用于搜索高亮）
  nodeEnter.append('circle')
    .attr('class', 'node-pulse')
    .attr('r', 28)
    .attr('fill', 'none')
    .attr('stroke', d => nodeColor(d.type))
    .attr('stroke-width', 2)
    .style('opacity', 0)

  const nodeMerge = nodeEnter.merge(node)

  // 节点交互
  nodeMerge
    .on('click', (event, d) => {
      event.stopPropagation()
      selectEntityById(d.id)
    })
    .on('mouseenter', (event, d) => {
      highlightNeighbors(d.id)
    })
    .on('mouseleave', () => {
      resetHighlight()
    })

  // 拖拽行为
  const drag = d3.drag()
    .on('start', (event, d) => {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    })
    .on('drag', (event, d) => {
      d.fx = event.x
      d.fy = event.y
    })
    .on('end', (event, d) => {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    })

  nodeMerge.call(drag)

  // 更新力模拟
  simulation.nodes(nodes.value)
  simulation.force('link').links(links.value)
  simulation.alpha(1).restart()
}

// Tick 函数
function ticked() {
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .each(function(d) {
      const g = d3.select(this)
      const line = g.select('line')
      const text = g.select('text')
      
      const sourceX = d.source.x
      const sourceY = d.source.y
      const targetX = d.target.x
      const targetY = d.target.y
      
      // 计算箭头位置
      const dx = targetX - sourceX
      const dy = targetY - sourceY
      const dr = Math.sqrt(dx * dx + dy * dy)
      const offset = 32 // 节点半径 + 间隙
      
      const endX = targetX - (dx / dr) * offset
      const endY = targetY - (dy / dr) * offset
      
      line
        .attr('x1', sourceX)
        .attr('y1', sourceY)
        .attr('x2', endX)
        .attr('y2', endY)
      
      text
        .attr('x', (sourceX + endX) / 2)
        .attr('y', (sourceY + endY) / 2 - 5)
    })

  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .attr('transform', d => `translate(${d.x},${d.y})`)
}

// 高亮邻居
function highlightNeighbors(nodeId) {
  const neighborIds = new Set([nodeId])
  const relatedLinks = []

  links.value.forEach(l => {
    const srcId = l.source.id || l.source
    const tgtId = l.target.id || l.target
    if (srcId === nodeId || tgtId === nodeId) {
      neighborIds.add(srcId)
      neighborIds.add(tgtId)
      relatedLinks.push(`${srcId}-${tgtId}`)
    }
  })

  // 淡化节点
  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .style('opacity', d => neighborIds.has(d.id) ? 1 : 0.2)

  // 淡化连线
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .style('opacity', function() {
      const line = d3.select(this).select('line')
      const x1 = line.attr('x1')
      const x2 = line.attr('x2')
      // 简化检查：如果连线两端节点在邻居中则显示
      return relatedLinks.some(id => {
        const l = links.value.find(ln => `${ln.source.id || ln.source}-${ln.target.id || ln.target}` === id)
        return l && (neighborIds.has(l.source.id || l.source) || neighborIds.has(l.target.id || l.target))
      }) ? 1 : 0.1
    })
}

// 重置高亮
function resetHighlight() {
  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .style('opacity', 1)
  
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .style('opacity', 1)
}

// 根据 ID 选择实体
async function selectEntityById(entityId) {
  const node = nodes.value.find(n => n.id === entityId)
  if (!node) return

  selectedEntity.value = {
    id: node.id,
    label: node.label,
    type: node.type,
    color: nodeColor(node.type)
  }

  // 加载邻居
  try {
    const r = await kgNeighbors(entityId)
    neighbors.value = r.data || []
    
    // 构建关系标签
    relationLabels.value = []
    links.value.forEach(l => {
      const srcId = l.source.id || l.source
      const tgtId = l.target.id || l.target
      if (srcId === entityId || tgtId === entityId) {
        const otherId = srcId === entityId ? tgtId : srcId
        const otherNode = nodes.value.find(n => n.id === otherId)
        if (otherNode && l.label) {
          relationLabels.value.push({
            label: l.label,
            target: otherNode.label
          })
        }
      }
    })
  } catch (e) {
    neighbors.value = []
  }

  // 显示连线标签
  d3.select(linksLayer.value)
    .selectAll('text.link-label')
    .style('opacity', d => {
      const srcId = d.source.id || d.source
      const tgtId = d.target.id || d.target
      return srcId === entityId || tgtId === entityId ? 1 : 0
    })
}

// 点击节点
async function selectEntity(row) {
  // 确保节点在图谱中
  if (!nodes.value.find(n => n.id === row.id)) {
    await loadKg()
  }
  await selectEntityById(row.id)
}

// 关闭详情
function closeDetail() {
  selectedEntity.value = null
  neighbors.value = []
  relationLabels.value = []
  
  d3.select(linksLayer.value)
    .selectAll('text.link-label')
    .style('opacity', 0)
}

// 聚焦邻居
function focusToNeighbor(nb) {
  selectEntityById(nb.id)
}

// 加载图谱
async function loadKg() {
  loading.value = true
  try {
    const r = await kgSearchEntities(null, '', 50)
    const list = r.data?.list || r.data || []
    stats.entities = r.data?.total || list.length

    // 限制显示数量
    const displayList = list.slice(0, displayLimit.value)
    showLimitTip.value = list.length > displayLimit.value

    // 构建节点和连线
    const newNodes = displayList.map(e => ({
      id: e.id,
      label: e.name,
      type: e.type || 'OTHER',
      x: canvasW.value / 2 + (Math.random() - 0.5) * 200,
      y: canvasH.value / 2 + (Math.random() - 0.5) * 200
    }))

    const newLinks = []
    const types = new Set()

    // 尝试从邻居构建连线
    for (let i = 0; i < Math.min(displayList.length, 20); i++) {
      const e = displayList[i]
      types.add(e.type)
      try {
        const r = await kgNeighbors(e.id)
        const nbs = r.data || []
        for (let j = 0; j < Math.min(nbs.length, 3); j++) {
          const nb = nbs[j]
          const targetExists = displayList.find(x => x.id === nb.id)
          if (targetExists && e.id !== nb.id) {
            newLinks.push({
              source: e.id,
              target: nb.id,
              label: nb.relation || '关联'
            })
          }
        }
      } catch {}
    }

    nodes.value = newNodes
    links.value = newLinks
    stats.edges = newLinks.length
    stats.types = types.size || 1

    await nextTick()
    updateGraph()
  } catch (e) {
    ElMessage.error('加载图谱失败')
  } finally {
    loading.value = false
  }
}

// 加载实体列表
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

// 搜索
async function handleSearch() {
  if (!searchKw.value.trim()) {
    searchResults.value = []
    searchNoMatch.value = false
    return
  }

  searchNoMatch.value = false
  const r = await kgSearchEntities(null, searchKw.value, 50)
  const list = r.data?.list || r.data || []
  
  if (list.length === 0) {
    searchNoMatch.value = true
    searchResults.value = []
  } else {
    searchResults.value = list
    // 高亮搜索结果节点
    highlightSearchResults(list)
  }
}

// 高亮搜索结果
function highlightSearchResults(results) {
  const matchIds = new Set(results.map(r => r.id))
  
  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .each(function(d) {
      const g = d3.select(this)
      const pulse = g.select('.node-pulse')
      
      if (matchIds.has(d.id)) {
        // 添加脉冲动画
        pulse
          .style('opacity', 1)
          .attr('stroke-width', 2)
        
        function pulseAnim() {
          pulse
            .attr('r', 28)
            .style('opacity', 1)
            .transition()
            .duration(1000)
            .attr('r', 50)
            .style('opacity', 0)
            .on('end', function() {
              if (matchIds.has(d.id)) {
                pulseAnim()
              }
            })
        }
        pulseAnim()
      } else {
        pulse
          .style('opacity', 0)
          .interrupt()
      }
    })
}

// 聚焦节点
function focusNode(item) {
  const node = nodes.value.find(n => n.id === item.id)
  if (node) {
    // 居中到节点
    const transform = d3.zoomIdentity
      .translate(canvasW.value / 2 - node.x, canvasH.value / 2 - node.y)
      .scale(1)
    
    d3.select(svgRef.value)
      .transition()
      .duration(500)
      .call(zoom.transform, transform)
    
    selectEntityById(item.id)
  } else {
    ElMessage.info('该节点不在当前图谱中，请刷新图谱')
  }
}

// 重置视图
function resetView() {
  d3.select(svgRef.value)
    .transition()
    .duration(500)
    .call(zoom.transform, d3.zoomIdentity)
}

// 查看邻居
async function viewNeighbors(row) {
  loadingNeighborsId.value = row.id
  try {
    const r = await kgNeighbors(row.id)
    neighbors.value = r.data || []
    ElMessage.success(`找到 ${neighbors.value.length} 个邻居`)
  } catch (e) {
    ElMessage.error('加载邻居失败：' + (e?.message || '未知错误'))
    neighbors.value = []
  } finally {
    loadingNeighborsId.value = null
  }
}

// 删除实体
async function confirmDeleteEntity(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除实体「${row.name}」？关联的关系也会被一并删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch (_) {
    return
  }
  deletingEntityId.value = row.id
  try {
    await kgDeleteEntity(row.id, currentUserId.value)
    ElMessage.success('已删除')
    await Promise.all([loadKg(), loadEntities()])
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || '未知错误'))
  } finally {
    deletingEntityId.value = null
  }
}

// 导出 PNG
function exportPNG() {
  showExportMenu.value = false
  
  const svgEl = svgRef.value
  const svgData = new XMLSerializer().serializeToString(svgEl)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  
  canvas.width = canvasW.value * 2
  canvas.height = canvasH.value * 2
  ctx.scale(2, 2)
  
  const img = new Image()
  const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  
  img.onload = () => {
    ctx.fillStyle = '#fafafa'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0)
    URL.revokeObjectURL(url)
    
    const link = document.createElement('a')
    link.download = `knowledge-graph-${Date.now()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('PNG 导出成功')
  }
  
  img.onerror = () => {
    ElMessage.error('PNG 导出失败')
  }
  
  img.src = url
}

// 导出 JSON
function exportJSON() {
  showExportMenu.value = false
  
  const data = {
    entities: nodes.value.map(n => ({
      id: n.id,
      name: n.label,
      type: n.type
    })),
    relations: links.value.map(l => ({
      from: l.source.id || l.source,
      to: l.target.id || l.target,
      label: l.label
    }))
  }
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const link = document.createElement('a')
  link.download = `knowledge-graph-${Date.now()}.json`
  link.href = URL.createObjectURL(blob)
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('JSON 导出成功')
}

// 处理文件选择
function handleFileChange(file) {
  pendingFile.value = file
}

// 重置添加表单
function resetAddForm() {
  addForm.name = ''
  addForm.type = 'PERSON'
  addForm.relation = ''
  addForm.relationLabel = ''
  addFormRef.value?.clearValidate()
}

// 导入数据
async function handleImport() {
  if (!pendingFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (importing.value) return // 防止重复点击

  importing.value = true
  importProgress.value = 0
  importCurrentIndex.value = 0
  importTotalCount.value = 0

  try {
    const text = await pendingFile.value.raw.text()
    const data = JSON.parse(text)

    if (!data.entities || !Array.isArray(data.entities)) {
      throw new Error('文件格式错误：缺少 entities 数组')
    }

    // 1) 解析所有实体, 调批量 upsert API
    const entitiesArr = data.entities.filter(e => e && e.name)
    importTotalCount.value = entitiesArr.length
    if (entitiesArr.length === 0) {
      throw new Error('文件格式错误：实体列表为空')
    }

    const entityResult = await kgBatchImportEntities(currentUserId.value, entitiesArr)
    importCurrentIndex.value = entitiesArr.length
    importProgress.value = 60
    if (entityResult.failed.length > 0) {
      ElMessage.warning(`实体导入: 成功 ${entityResult.succeeded} / 失败 ${entityResult.failed.length}`)
    }

    // 2) 处理关系 (可选)
    let relationResult = { succeeded: 0, failed: [] }
    if (Array.isArray(data.relations) && data.relations.length > 0) {
      // 通过名称查找实体 ID
      const entityNameToId = new Map()
      // 重新拉一次保证 ID 完整
      try {
        const r = await kgSearchEntities(currentUserId.value, '', 200)
        const list = r.data?.list || r.data || []
        for (const e of list) entityNameToId.set(e.name, e.id)
      } catch (_) {}

      // 把 from/to name 解析成 id
      const relationsWithIds = []
      for (const rel of data.relations) {
        const fromId = entityNameToId.get(rel.fromName || rel.from)
        const toId = entityNameToId.get(rel.toName || rel.to)
        if (fromId && toId) {
          relationsWithIds.push({
            fromId, toId,
            type: rel.label || rel.type || '关联',
            description: rel.description || '',
            weight: rel.weight || 1.0
          })
        }
      }
      if (relationsWithIds.length > 0) {
        relationResult = await kgBatchImportRelations(currentUserId.value, relationsWithIds)
      }
    }
    importProgress.value = 100

    // 3) 汇总
    const entitySucceeded = entityResult.succeeded
    const entityFailed = entityResult.failed.length
    const relationSucceeded = relationResult.succeeded
    const relationFailed = relationResult.failed.length

    if (entityFailed === 0 && relationFailed === 0) {
      ElMessage.success(`导入完成: ${entitySucceeded} 个实体, ${relationSucceeded} 个关系`)
    } else {
      ElMessageBox.alert(
        `实体: 成功 ${entitySucceeded} / 失败 ${entityFailed}\n关系: 成功 ${relationSucceeded} / 失败 ${relationFailed}`,
        '导入结果 (部分失败)',
        { type: 'warning', confirmButtonText: '我知道了' }
      )
    }

    // 4) 关闭弹窗 + 刷新
    showUpload.value = false
    pendingFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()

    await Promise.all([loadKg(), loadEntities()])
  } catch (e) {
    ElMessage.error('导入失败: ' + (e?.message || '文件格式错误'))
  } finally {
    importing.value = false
    importProgress.value = 0
    importCurrentIndex.value = 0
    importTotalCount.value = 0
  }
}

// 添加实体
async function handleAddEntity() {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
  } catch (_) {
    ElMessage.warning('请检查表单填写')
    return
  }
  addEntityLoading.value = true
  try {
    // 1) upsert 主实体
    const mainEntityId = await kgUpsertEntity({
      userId: currentUserId.value,
      name: addForm.name.trim(),
      type: addForm.type,
      description: ''
    })
    const newEntityId = mainEntityId?.data?.data || mainEntityId?.data || mainEntityId

    // 2) 如果填写了关系目标, 先 upsert 目标实体, 再建关系
    if (addForm.relation && addForm.relationLabel) {
      try {
        const targetIdResp = await kgUpsertEntity({
          userId: currentUserId.value,
          name: addForm.relation.trim(),
          type: 'OTHER'
        })
        const targetId = targetIdResp?.data?.data || targetIdResp?.data || targetIdResp
        if (newEntityId && targetId) {
          await kgCreateRelation({
            userId: currentUserId.value,
            fromId: newEntityId,
            toId: targetId,
            type: addForm.relationLabel.trim(),
            description: '',
            weight: 1.0
          })
        }
      } catch (e) {
        // 目标实体 upsert 失败不影响主实体创建
      }
    }

    ElMessage.success('实体添加成功')

    // 3) 重置表单
    resetAddForm()
    showAddForm.value = false

    // 4) 刷新
    await Promise.all([loadKg(), loadEntities()])
  } catch (e) {
    ElMessage.error('添加失败: ' + (e?.message || '请检查后端服务'))
  } finally {
    addEntityLoading.value = false
  }
}

// 窗口调整
function handleResize() {
  if (canvasRef.value) {
    canvasW.value = canvasRef.value.offsetWidth
    canvasH.value = canvasRef.value.offsetHeight || 450
    
    if (svgRef.value) {
      d3.select(svgRef.value)
        .attr('width', canvasW.value)
        .attr('height', canvasH.value)
      
      if (simulation) {
        simulation.force('center', d3.forceCenter(canvasW.value / 2, canvasH.value / 2))
        simulation.alpha(0.3).restart()
      }
    }
  }
}

// 指令：点击外部关闭
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (event) => {
      if (!el.contains(event.target)) {
        binding.value()
      }
    }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('click', el._clickOutside)
  }
}

onMounted(() => {
  nextTick(() => {
    handleResize()
    initD3()
    loadKg()
    loadEntities()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (simulation) {
    simulation.stop()
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 16px; 
  h2 { margin: 0; font-size: 16px; } 
  position: relative;
}

.export-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  z-index: 100;
}

.export-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  &:hover {
    background: var(--el-fill-color-light);
  }
  &:first-child {
    border-radius: 4px 4px 0 0;
  }
  &:last-child {
    border-radius: 0 0 4px 4px;
  }
}

.kg-canvas { 
  position: relative; 
  height: 450px; 
  border: 1px solid #f0f0f0; 
  border-radius: 4px; 
  background: #fafafa; 
  overflow: hidden; 
}

.kg-svg { 
  display: block; 
  width: 100%; 
  height: 100%; 
}

.kg-loading, .kg-empty {
  position: absolute; 
  inset: 0; 
  display: flex; 
  align-items: center; 
  justify-content: center;
  color: var(--el-text-color-secondary); 
  font-size: 14px; 
  gap: 8px;
}

.kg-limit-tip {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.search-results-panel {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  max-height: 200px;
  overflow-y: auto;
  min-width: 200px;
  z-index: 10;
}

.search-results-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  border-bottom: 1px solid #f0f0f0;
  background: var(--el-fill-color-light);
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  &:hover {
    background: var(--el-color-primary-light-9);
  }
}

.node-type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  &.large {
    width: 12px;
    height: 12px;
  }
}

.node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-type-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.entity-detail-panel {
  position: fixed;
  top: 100px;
  right: 20px;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 14px;
}

.panel-body {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.entity-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.entity-name {
  font-size: 16px;
  font-weight: 600;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: var(--el-text-color-primary);
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.relation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.relation-label {
  color: var(--el-color-primary);
  font-weight: 500;
}

.relation-target {
  color: var(--el-text-color-regular);
}

.neighbors-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.neighbor-tag {
  cursor: pointer;
  &:hover {
    opacity: 0.8;
  }
}

.upload-area {
  .upload-tip {
    margin-top: 16px;
    p {
      font-size: 13px;
      color: var(--el-text-color-regular);
      margin-bottom: 8px;
    }
  }
}

.json-example {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  overflow-x: auto;
  white-space: pre;
}

// D3 节点样式（全局）
:deep(.node-group) {
  transition: opacity 0.3s;
}

:deep(.node-circle) {
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
  transition: all 0.2s;
}

:deep(.node-group:hover .node-circle) {
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2));
  transform: scale(1.1);
}

:deep(.link-line) {
  transition: stroke 0.2s, stroke-width 0.2s;
}

:deep(.link-group:hover .link-line) {
  stroke: #409eff;
  stroke-width: 2;
}

:deep(.link-label) {
  pointer-events: none;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}
</style>

<style>
/* 全局样式用于 D3 动画 */
@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}
</style>
