<!--
  @file views/pipeline/Designer.vue (Designer 页面)
  @version V3.5.12+ (前端注释补全)
  @description Designer 页面
-->
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <div>
            <el-button @click="$router.push('/pipeline')">← 返回</el-button>
            <el-input v-model="wf.name" placeholder="工作流名称" style="width:240px;margin-left:12px" />
          </div>
          <div>
            <el-button @click="validate">校验</el-button>
            <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            <el-button type="success" :loading="running" @click="run">运行</el-button>
          </div>
        </div>
      </template>

      <div class="designer">
        <!-- 左: 节点面板 -->
        <div class="palette">
          <h4>节点</h4>
          <div class="group">
            <div class="group-title">📥 输入</div>
            <div class="node-item" v-for="t in inputTypes" :key="t.code"
                 draggable="true" @dragstart="onDragStart($event, t)">
              <span class="node-icon" :style="{background: t.color}">{{ t.icon }}</span>
              {{ t.name }}
            </div>
          </div>
          <div class="group">
            <div class="group-title">🔧 转换</div>
            <div class="node-item" v-for="t in transformTypes" :key="t.code"
                 draggable="true" @dragstart="onDragStart($event, t)">
              <span class="node-icon" :style="{background: t.color}">{{ t.icon }}</span>
              {{ t.name }}
            </div>
          </div>
          <div class="group">
            <div class="group-title">📤 输出</div>
            <div class="node-item" v-for="t in outputTypes" :key="t.code"
                 draggable="true" @dragstart="onDragStart($event, t)">
              <span class="node-icon" :style="{background: t.color}">{{ t.icon }}</span>
              {{ t.name }}
            </div>
          </div>
        </div>

        <!-- 中: 画布 -->
        <div class="canvas" ref="canvasRef"
             @dragover.prevent @drop="onDrop">
          <svg class="lines" :width="canvasWidth" :height="canvasHeight">
            <defs>
              <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5"
                      markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="#409eff" />
              </marker>
            </defs>
            <line v-for="(edge, i) in wf.edges" :key="i"
                  :x1="nodeById(edge.from)?.x + 140"
                  :y1="nodeById(edge.from)?.y + 30"
                  :x2="nodeById(edge.to)?.x"
                  :y2="nodeById(edge.to)?.y + 30"
                  stroke="#409eff" stroke-width="2"
                  marker-end="url(#arrow)" />
          </svg>
          <div v-for="node in wf.nodes" :key="node.id"
               class="node-card"
               :style="{ left: node.x + 'px', top: node.y + 'px', borderColor: nodeTypeMeta(node.type).color }"
               @mousedown="startDrag($event, node)">
            <div class="node-header" :style="{ background: nodeTypeMeta(node.type).color }">
              <span>{{ nodeTypeMeta(node.type).icon }} {{ nodeTypeMeta(node.type).name }}</span>
              <span class="del" @click.stop="removeNode(node)">×</span>
            </div>
            <div class="node-body">{{ node.label || node.id }}</div>
            <div class="port out" :style="{left:'140px'}" @click.stop="startConnect($event, node)"></div>
            <div class="port in" @click.stop="endConnect(node)"></div>
          </div>
        </div>

        <!-- 右: 属性面板 -->
        <div class="props">
          <h4>属性</h4>
          <template v-if="selected">
            <el-form label-width="80" size="small">
              <el-form-item label="ID"><el-input v-model="selected.id" disabled /></el-form-item>
              <el-form-item label="标签"><el-input v-model="selected.label" /></el-form-item>
              <el-form-item label="类型">
                <el-tag>{{ nodeTypeMeta(selected.type).name }}</el-tag>
              </el-form-item>
              <el-form-item label="参数">
                <el-input v-model="paramsText" type="textarea" :rows="6"
                          placeholder='JSON, 例: {"url":"jdbc:mysql://..."}' />
              </el-form-item>
            </el-form>
          </template>
          <el-empty v-else description="选择节点查看属性" :image-size="60" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWorkflow, createWorkflow, updateWorkflow, runWorkflow, validateWorkflow } from '@/api/pipeline'

const route = useRoute()
const router = useRouter()
const canvasRef = ref(null)
const canvasWidth = ref(2000)
const canvasHeight = ref(1200)
const saving = ref(false)
const running = ref(false)
const selected = ref(null)
const paramsText = ref('{}')
const connecting = ref(null) // { fromNode }

// 13 种节点类型
const inputTypes = [
  { code: 'mysql_input', name: 'MySQL 输入', icon: '🗄', color: '#5470c6', category: 'INPUT' },
  { code: 'file_input', name: '文件输入', icon: '📁', color: '#5470c6', category: 'INPUT' },
  { code: 'api_input', name: 'API 输入', icon: '🌐', color: '#5470c6', category: 'INPUT' }
]
const transformTypes = [
  { code: 'filter', name: '过滤', icon: '🔍', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'select', name: '字段选择', icon: '☐', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'join', name: '连接', icon: '🔗', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'aggregate', name: '聚合', icon: '∑', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'sort', name: '排序', icon: '⇅', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'limit', name: '限制', icon: '#', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'union', name: '合并', icon: '⋃', color: '#91cc75', category: 'TRANSFORM' },
  { code: 'distinct', name: '去重', icon: '∄', color: '#91cc75', category: 'TRANSFORM' }
]
const outputTypes = [
  { code: 'db_output', name: 'DB 输出', icon: '💾', color: '#ee6666', category: 'OUTPUT' },
  { code: 'file_output', name: '文件输出', icon: '📝', color: '#ee6666', category: 'OUTPUT' },
  { code: 'report_output', name: '报告输出', icon: '📊', color: '#ee6666', category: 'OUTPUT' }
]
const allTypes = [...inputTypes, ...transformTypes, ...outputTypes]
const typeMap = Object.fromEntries(allTypes.map(t => [t.code, t]))
const nodeTypeMeta = (code) => typeMap[code] || { name: code, icon: '?', color: '#999' }

const wf = reactive({
  id: null,
  name: '新工作流',
  description: '',
  nodes: [],
  edges: [],
  status: 'DRAFT'
})

function nodeById(id) { return wf.nodes.find(n => n.id === id) }

let dragNode = null
let dragOffset = { x: 0, y: 0 }

function startDrag(e, node) {
  dragNode = node
  dragOffset.x = e.offsetX
  dragOffset.y = e.offsetY
  selected.value = node
  paramsText.value = JSON.stringify(node.params || {}, null, 2)
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e) {
  if (!dragNode) return
  const rect = canvasRef.value.getBoundingClientRect()
  dragNode.x = Math.max(0, e.clientX - rect.left - dragOffset.x)
  dragNode.y = Math.max(0, e.clientY - rect.top - dragOffset.y)
}

function onDragEnd() {
  dragNode = null
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

function onDragStart(e, type) {
  e.dataTransfer.setData('nodeType', type.code)
}

function onDrop(e) {
  const code = e.dataTransfer.getData('nodeType')
  if (!code) return
  const rect = canvasRef.value.getBoundingClientRect()
  const newNode = {
    id: 'n' + (wf.nodes.length + 1),
    type: code,
    label: nodeTypeMeta(code).name,
    x: e.clientX - rect.left - 70,
    y: e.clientY - rect.top - 30,
    params: {}
  }
  wf.nodes.push(newNode)
  selected.value = newNode
  paramsText.value = '{}'
}

function startConnect(e, node) {
  connecting.value = { fromNode: node.id }
  window.addEventListener('click', cancelConnect, { once: true })
}

function endConnect(node) {
  if (connecting.value && connecting.value.fromNode !== node.id) {
    wf.edges.push({ from: connecting.value.fromNode, to: node.id })
  }
  connecting.value = null
}

function cancelConnect() {
  connecting.value = null
}

function removeNode(node) {
  wf.nodes = wf.nodes.filter(n => n.id !== node.id)
  wf.edges = wf.edges.filter(e => e.from !== node.id && e.to !== node.id)
  if (selected.value === node) selected.value = null
}

watch(paramsText, (v) => {
  if (selected.value) {
    try { selected.value.params = JSON.parse(v) } catch {}
  }
})

async function save() {
  saving.value = true
  try {
    const body = { ...wf }
    delete body.id
    if (wf.id) {
      await updateWorkflow(wf.id, body)
      ElMessage.success('已更新')
    } else {
      const res = await createWorkflow(body)
      wf.id = res.data?.id || res.data
      ElMessage.success('已创建, ID: ' + wf.id)
    }
  } catch (e) {} finally { saving.value = false }
}

async function validate() {
  try {
    const res = await validateWorkflow(wf)
    if (res.data?.valid) ElMessage.success('校验通过')
    else ElMessage.warning(res.data?.message || '校验失败')
  } catch (e) {}
}

async function run() {
  if (!wf.id) {
    ElMessage.warning('请先保存')
    return
  }
  running.value = true
  try {
    const res = await runWorkflow(wf.id, {})
    ElMessage.success('运行已启动, Run ID: ' + (res.data?.runId || 'N/A'))
  } catch (e) {} finally { running.value = false }
}

onMounted(async () => {
  const id = route.query.id
  if (id) {
    const res = await getWorkflow(id)
    Object.assign(wf, res.data)
  } else {
    // 初始示例节点
    wf.nodes = [
      { id: 'n1', type: 'mysql_input', label: '用户表', x: 50, y: 100, params: { table: 'sys_user' } },
      { id: 'n2', type: 'filter', label: '过滤活跃用户', x: 280, y: 100, params: { expr: 'status == 1' } },
      { id: 'n3', type: 'report_output', label: '生成报告', x: 510, y: 100, params: {} }
    ]
    wf.edges = [{ from: 'n1', to: 'n2' }, { from: 'n2', to: 'n3' }]
  }
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.designer { display: flex; height: calc(100vh - 200px); gap: 8px; }
.palette { width: 220px; background: #f5f7fa; padding: 12px; overflow: auto; border-radius: 4px; }
.canvas { flex: 1; position: relative; background: #fafbfc; background-image: radial-gradient(circle, #ddd 1px, transparent 1px); background-size: 20px 20px; overflow: auto; }
.props { width: 280px; background: #f5f7fa; padding: 12px; overflow: auto; border-radius: 4px; }
.group { margin-bottom: 12px; }
.group-title { font-size: 12px; color: #909399; margin-bottom: 4px; }
.node-item { padding: 6px 8px; margin: 4px 0; background: white; border-radius: 4px; cursor: grab; display: flex; align-items: center; gap: 6px; font-size: 13px; border: 1px solid #ebeef5; }
.node-item:hover { background: #ecf5ff; border-color: #409eff; }
.node-icon { width: 24px; height: 24px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; color: white; font-size: 14px; }
.node-card { position: absolute; width: 140px; background: white; border: 2px solid; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); cursor: move; user-select: none; }
.node-header { color: white; padding: 4px 8px; font-size: 12px; display: flex; justify-content: space-between; }
.del { cursor: pointer; padding: 0 4px; }
.node-body { padding: 6px 8px; font-size: 12px; }
.port { position: absolute; width: 12px; height: 12px; background: #409eff; border-radius: 50%; top: 24px; cursor: crosshair; }
.port.in { left: -6px; }
.port.out { left: 134px; }
.lines { position: absolute; top: 0; left: 0; pointer-events: none; }
h4 { margin: 0 0 8px 0; }
</style>
