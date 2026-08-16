<!-- @file agent/Canvas.vue - Agent 画布 V6.8 -->
<template>
  <div class="page-card" style="padding:0;overflow:hidden">
    <!-- 顶部工具栏 -->
    <div class="canvas-toolbar">
      <span style="font-size:14px;font-weight:600;color:#303133">Agent 画布</span>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadWorkflows"><el-icon><Refresh /></el-icon>加载</el-button>
        <el-button size="small" @click="newCanvas"><el-icon><Plus /></el-icon>新建</el-button>
        <el-button type="primary" size="small" @click="saveCanvas"><el-icon><FolderChecked /></el-icon>保存</el-button>
        <el-button type="success" size="small" @click="runCanvas" :loading="running"><el-icon><VideoPlay /></el-icon>执行</el-button>
        <el-button size="small" :type="multiRunMode?'warning':''" @click="multiRunMode = !multiRunMode" :disabled="running">
          {{ multiRunMode ? '⚡ 多Agent ON' : '🤖 多Agent' }}
        </el-button>
        <el-button size="small" @click="autoLayout"><el-icon><Grid /></el-icon>自动布局</el-button>
        <el-button size="small" @click="exportFlow"><el-icon><Download /></el-icon>导出</el-button>
        <el-button size="small" @click="importFlow"><el-icon><Upload /></el-icon>导入</el-button>
        <el-button size="small" @click="clearCanvas"><el-icon><Delete /></el-icon>清空</el-button>
      </div>
    </div>

    <div class="canvas-body">
      <!-- 左侧节点面板 -->
      <div class="node-palette">
        <div class="palette-title">节点库</div>
        <div
          v-for="nt in nodeTypes" :key="nt.type"
          class="palette-node"
          :style="{ borderLeftColor: nt.color }"
          draggable="true"
          @dragstart="onDragStart($event, nt)"
        >
          <el-icon><component :is="nt.icon" /></el-icon>
          <span>{{ nt.label }}</span>
        </div>
      </div>

      <!-- 画布区域 -->
      <div class="canvas-area" ref="canvasRef"
        @dragover.prevent
        @drop="onDrop"
        @click="deselectAll"
        @wheel.prevent="onWheel"
      >
        <!-- SVG 连线层 -->
        <svg class="edge-svg" :width="canvasW" :height="canvasH">
          <defs>
            <marker id="arr" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#409eff" />
            </marker>
          </defs>
          <path
            v-for="(e, i) in edges" :key="i"
            :d="edgePath(e)"
            :stroke="executedEdges.has(i) ? '#67c23a' : '#409eff'"
            :stroke-width="executedEdges.has(i) ? 2.5 : 1.5"
            :class="{ 'edge-executed': executedEdges.has(i) }"
            fill="none"
            marker-end="url(#arr)"
            style="cursor:pointer"
            @click.stop="removeEdge(i)"
          />
          <!-- 拖拽中的连线预览 -->
          <path
            v-if="dragLine"
            :d="`M${dragLine.x1},${dragLine.y1} L${dragLine.x2},${dragLine.y2}`"
            stroke="#409eff" stroke-width="1.5" stroke-dasharray="5,3" fill="none"
          />
        </svg>

        <!-- 节点 -->
        <div
          v-for="n in nodes" :key="n.id"
          class="canvas-node"
          :style="{ left: n.x+'px', top: n.y+'px', borderTopColor: n.color }"
          :class="{ selected: n.id === selectedId, running: n.id === runningNode }"
          @click.stop="selectNode(n)"
          @mousedown="startMove($event, n)"
        >
          <div class="node-header">
            <el-icon><component :is="n.icon" /></el-icon>
            <span>{{ n.label }}</span>
            <el-icon class="node-del" @click.stop="removeNode(n.id)"><Close /></el-icon>
          </div>
          <div class="node-body">
            <div class="node-type-tag" :style="{ background: n.color + '22', color: n.color }">{{ n.type }}</div>
            <div v-if="n.config?.prompt" class="node-desc">{{ n.config.prompt.slice(0,40) }}…</div>
          </div>
          <!-- V6.9: 多Agent执行状态浮层 -->
          <div v-if="nodeExecStatus[n.id]" class="node-exec-badge" :class="`exec-${nodeExecStatus[n.id]}`">
            <span>{{ {planner:'🧠',executor:'⚡',critic:'🔍',done:'✅',error:'❌'}[nodeExecStatus[n.id]] || nodeExecStatus[n.id] }}</span>
          </div>
          <div v-if="nodeExecResult[n.id]" class="node-exec-popup">
            {{ nodeExecResult[n.id]?.slice(0,80) }}{{ (nodeExecResult[n.id]?.length||0) > 80 ? '…' : '' }}
          </div>
          <!-- Step badge when execution order is set -->
          <div
            v-if="executionOrder.has(n.id)"
            class="step-badge"
            :class="{ 'step-error': runLog[executionOrder.get(n.id)-1]?.result?.includes('错误') || runLog[executionOrder.get(n.id)-1]?.result?.includes('失败') }"
          >{{ executionOrder.get(n.id) }}</div>
          <!-- 拖拽连接点 -->
          <div class="port port-out" @mousedown.stop="startEdge($event, n, 'out')" />
          <div class="port port-in" />
        </div>

        <div v-if="nodes.length === 0 && !loading" class="canvas-empty">
          <div style="font-size:40px">🕸️</div>
          <div style="margin-top:8px">从左侧拖拽节点到画布</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">或点击"加载"导入已有工作流</div>
        </div>

        <!-- Zoom controls (bottom-left) -->
        <div class="zoom-controls">
          <el-button size="small" @click="zoom = Math.max(0.3, +(zoom - 0.1).toFixed(2))">−</el-button>
          <span class="zoom-pct">{{ Math.round(zoom * 100) }}%</span>
          <el-button size="small" @click="zoom = Math.min(2.0, +(zoom + 0.1).toFixed(2))">+</el-button>
        </div>

        <!-- Mini-map (bottom-right) -->
        <div class="minimap">
          <svg :width="minimapW" :height="minimapH" style="display:block">
            <!-- Nodes as dots -->
            <circle
              v-for="n in nodes" :key="n.id"
              :cx="(n.x / canvasW) * minimapW + 3"
              :cy="(n.y / canvasH) * minimapH + 2"
              r="3"
              :fill="n.color || '#409eff'"
            />
            <!-- Viewport indicator -->
            <rect
              x="2" y="2"
              :width="(minimapW / canvasW) * minimapW - 4"
              :height="(minimapH / canvasH) * minimapH - 4"
              fill="none" stroke="#409eff" stroke-width="1"
            />
          </svg>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="prop-panel">
        <div class="palette-title">属性配置</div>
        <div v-if="!selectedNode" class="prop-empty">点击节点查看属性</div>
        <div v-else class="prop-form">
          <el-form label-width="80px" size="small">
            <el-form-item label="名称">
              <el-input v-model="selectedNode.label" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="selectedNode.type" style="width:100%">
                <el-option v-for="nt in nodeTypes" :key="nt.type" :label="nt.label" :value="nt.type" />
              </el-select>
            </el-form-item>
            <el-form-item label="Prompt">
              <el-input v-model="selectedNode.config.prompt" type="textarea" :rows="4"
                placeholder="输入节点指令…" />
            </el-form-item>
            <el-form-item label="工具">
              <el-select v-model="selectedNode.config.tools" multiple style="width:100%" placeholder="选择工具">
                <el-option v-for="t in toolOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="模型">
              <el-select v-model="selectedNode.config.model" style="width:100%" placeholder="默认模型" filterable>
                <el-option label="默认" value="" />
                <el-option-group v-if="trainedModels.length" label="🏷️ 自研模型">
                  <el-option v-for="m in trainedModels" :key="m.code" :label="m.name" :value="m.code">
                    {{ m.name }}
                    <span v-if="m.accuracy" style="float:right;font-size:11px;color:#67c23a">{{ m.accuracy }}%</span>
                  </el-option>
                </el-option-group>
                <el-option-group label="🤖 云端模型">
                  <el-option v-for="m in allModels.filter(a => !trainedModels.find(t => t.code === a.code))" :key="a.code" :label="a.name" :value="a.code">
                    {{ a.name }}
                  </el-option>
                </el-option-group>
              </el-select>
            </el-form-item>
            <el-form-item label="Max 步数">
              <el-input-number v-model="selectedNode.config.maxSteps" :min="1" :max="20" />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 执行日志 -->
    <el-drawer v-model="logVisible" title="执行日志" size="50%">
      <div class="log-view">
        <div v-for="(step, i) in runLog" :key="i" class="log-step">
          <span class="log-i">{{ i+1 }}</span>
          <span class="log-tool">{{ step.tool }}</span>
          <span class="log-result">{{ step.result }}</span>
        </div>
        <div v-if="!runLog.length" style="color:#909399;text-align:center;padding:40px">暂无执行记录</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentApi, multiAgentApi } from '@/api/agent'
import { listEnabledModels } from '@/api/model'
import { listTrainedModels } from '@/api/ai'
import {
  EditPen, FolderChecked, Delete, Plus, Refresh, VideoPlay, Close,
  ChatDotRound, Connection, Operation, Files, Tools, DataLine, Folder, Grid, Upload, Download
} from '@element-plus/icons-vue'

const canvasRef = ref(null)
const canvasW = ref(900)
const canvasH = ref(600)
const canvasName = ref('')

// 模型列表
const allModels = ref([])
const trainedModels = ref([])

async function loadModels() {
  try {
    const r = await listEnabledModels()
    allModels.value = (r.data || []).map(m => ({
      code: m.code || m.model_code || m.name,
      name: m.displayName || m.name || m.code,
      local: !!(m.protocol === 'local' || (m.providerCode || '').startsWith('local-')),
      vision: !!(m.supportsVision || m.vision),
    }))
  } catch { allModels.value = [] }
  try {
    const r = await listTrainedModels()
    trainedModels.value = (r.data || []).map(m => ({
      code: m.code,
      name: m.name,
      accuracy: m.accuracy || 0,
    }))
  } catch { trainedModels.value = [] }
}

const agentModels = computed(() => {
  const trained = trainedModels.value.map(m => ({ ...m, _trained: true }))
  const cloud = allModels.value.map(m => ({ ...m, _trained: false }))
  return [...trained, ...cloud]
})

const nodes = ref([])
const edges = ref([])
const selectedId = ref(null)
const runningNode = ref(null)
const running = ref(false)
const logVisible = ref(false)
const runLog = ref([])
const loading = ref(false)
const multiRunMode = ref(false)        // V6.9: 多Agent模式开关
const nodeExecStatus = ref({})         // V6.9: nodeId → 'planner'|'executor'|'critic'|'done'|'error'
const nodeExecResult = ref({})          // V6.9: nodeId → result text
const multiAbortCtrl = ref(null)

// New: zoom & execution tracking
const zoom = ref(1.0)
const executionOrder = ref(new Map())
const executedEdges = ref(new Set())
const minimapW = 120
const minimapH = 80

const transformStyle = computed(() => ({
  transform: `scale(${zoom.value})`,
  transformOrigin: 'top left',
}))

const selectedNode = computed(() => nodes.value.find(n => n.id === selectedId.value) || null)

const dragLine = ref(null)
let draggingNode = null, dragOffset = { x: 0, y: 0 }
let idCounter = 1

const nodeTypes = [
  { type: 'START', label: '开始', icon: 'VideoPlay', color: '#67c23a', desc: '触发入口' },
  { type: 'LLM', label: 'LLM 节点', icon: 'ChatDotRound', color: '#409eff', desc: '大模型调用' },
  { type: 'TOOL', label: '工具节点', icon: 'Tools', color: '#e6a23c', desc: '调用外部工具' },
  { type: 'CONDITION', label: '条件分支', icon: 'Operation', color: '#f56c6c', desc: '条件路由' },
  { type: 'MEMORY', label: '记忆节点', icon: 'Files', color: '#7c3aed', desc: '读写上下文' },
  { type: 'KNOWLEDGE', label: '知识检索', icon: 'DataLine', color: '#00b4d8', desc: 'RAG 检索' },
  { type: 'END', label: '结束', icon: 'Folder', color: '#909399', desc: '输出结果' },
]

const toolOptions = ['web-search', 'code-exec', 'file-read', 'file-write', 'api-call', 'calculator']

function newId() { return 'node_' + (idCounter++) }

function newCanvas() {
  ElMessageBox.confirm('新建画布将清空当前内容，确定？').then(() => {
    nodes.value = []
    edges.value = []
    selectedId.value = null
    idCounter = 1
  }).catch(() => {})
}

function clearCanvas() {
  nodes.value = []
  edges.value = []
  selectedId.value = null
}

function selectNode(n) { selectedId.value = n.id }
function deselectAll() { selectedId.value = null }
function removeNode(id) { nodes.value = nodes.value.filter(n => n.id !== id); edges.value = edges.value.filter(e => e.from !== id && e.to !== id); if (selectedId.value === id) selectedId.value = null }

function onDragStart(ev, nt) {
  ev.dataTransfer.setData('nodeType', nt.type)
  ev.dataTransfer.effectAllowed = 'copy'
}

async function onDrop(ev) {
  const type = ev.dataTransfer.getData('nodeType')
  const fromEdge = ev.dataTransfer.getData('fromNode')
  if (!type && !fromEdge) return
  const rect = canvasRef.value.getBoundingClientRect()
  const x = ev.clientX - rect.left - 60
  const y = ev.clientY - rect.top - 30
  if (type) {
    const nt = nodeTypes.find(n => n.type === type) || nodeTypes[1]
    nodes.value.push({
      id: newId(), type: nt.type, label: nt.label,
      icon: nt.icon, color: nt.color,
      x, y, config: { prompt: '', tools: [], model: '', maxSteps: 5 }
    })
  } else if (fromEdge) {
    const fromNode = nodes.value.find(n => n.id === fromEdge)
    const toNode = nodes.value.find(n => Math.abs(n.x - x) < 120 && Math.abs(n.y - y) < 60)
    if (fromNode && toNode && fromNode.id !== toNode.id) {
      edges.value.push({ from: fromNode.id, to: toNode.id })
      ElMessage.success('连线已添加')
    }
  }
}

function startMove(ev, n) {
  draggingNode = n
  dragOffset = { x: ev.clientX - n.x, y: ev.clientY - n.y }
  const onMove = (e) => {
    if (!draggingNode) return
    draggingNode.x = Math.max(0, Math.min(canvasW.value - 120, e.clientX - dragOffset.x))
    draggingNode.y = Math.max(0, Math.min(canvasH.value - 60, e.clientY - dragOffset.y))
  }
  const onUp = () => {
    draggingNode = null
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function startEdge(ev, n, dir) {
  const rect = canvasRef.value.getBoundingClientRect()
  dragLine.value = { x1: n.x + 60, y1: n.y + 20, x2: ev.clientX - rect.left, y2: ev.clientY - rect.top, from: n.id }
  const onMove = (e) => {
    if (!dragLine.value) return
    dragLine.value.x2 = Math.max(0, Math.min(canvasW.value, e.clientX - rect.left))
    dragLine.value.y2 = Math.max(0, Math.min(canvasH.value, e.clientY - rect.top))
  }
  const onUp = (e) => {
    if (dragLine.value) {
      const target = nodes.value.find(nd =>
        nd.id !== dragLine.value.from &&
        Math.abs(nd.x + 60 - dragLine.value.x2) < 40 &&
        Math.abs(nd.y + 20 - dragLine.value.y2) < 40
      )
      if (target) {
        edges.value.push({ from: dragLine.value.from, to: target.id })
        ElMessage.success('连线已创建')
      }
      dragLine.value = null
    }
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function removeEdge(i) {
  ElMessageBox.confirm('删除此连线？').then(() => { edges.value.splice(i, 1) }).catch(() => {})
}

// ── New feature functions ──────────────────────────────────────────

function onWheel(e) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  zoom.value = Math.max(0.3, Math.min(2.0, +(zoom.value + delta).toFixed(2)))
}

function autoLayout() {
  if (!nodes.value.length) return
  const starts = nodes.value.filter(n => n.type === 'START')
  const ends   = nodes.value.filter(n => n.type === 'END')
  const middles = nodes.value.filter(n => n.type !== 'START' && n.type !== 'END')
  const rowH = Math.max(80, Math.floor((canvasH.value - 40) / (Math.max(middles.length, 1) + 2)))
  const colW = 180
  const cols = Math.ceil(Math.sqrt(nodes.value.length))
  // place START nodes at top
  starts.forEach((n, i) => { n.x = (i % cols) * colW + 20; n.y = 20 })
  // place middles in rows
  middles.forEach((n, i) => {
    n.x = (i % cols) * colW + 20
    n.y = 80 + Math.floor(i / cols) * rowH
  })
  // place END nodes at bottom
  ends.forEach((n, i) => { n.x = (i % cols) * colW + 20; n.y = canvasH.value - 80 })
  ElMessage.success('布局完成')
}

function exportFlow() {
  if (!nodes.value.length) { ElMessage.warning('画布为空'); return }
  const data = JSON.stringify({ nodes: nodes.value, edges: edges.value }, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'flow.json'; a.click()
  URL.revokeObjectURL(url)
}

function importFlow() {
  const input = document.createElement('input')
  input.type = 'file'; input.accept = '.json'
  input.onchange = () => {
    const file = input.files[0]; if (!file) return
    const reader = new FileReader()
    reader.onload = e => {
      try {
        const d = JSON.parse(e.target.result)
        nodes.value = (d.nodes || []).map(n => ({ ...n, id: n.id || newId() }))
        edges.value = d.edges || []
        idCounter = nodes.value.length + 1
        ElMessage.success('导入成功')
      } catch { ElMessage.error('JSON 格式错误') }
    }
    reader.readAsText(file)
  }
  input.click()
}

function highlightExecution() {
  // Parse runLog to build execution order (step per node label/type)
  const order = new Map()
  const edgeSet = new Set()
  runLog.value.forEach((step, i) => {
    // Find node by label match
    const node = nodes.value.find(n =>
      step.tool?.includes(n.label) || step.tool?.includes(n.type)
    )
    if (node) order.set(node.id, i + 1)
  })
  // Mark edges that connect ordered nodes in sequence
  const sorted = [...order.entries()].sort((a, b) => a[1] - b[1])
  for (let i = 0; i < sorted.length - 1; i++) {
    const idx = edges.value.findIndex(e => e.from === sorted[i][0] && e.to === sorted[i + 1][0])
    if (idx !== -1) edgeSet.add(idx)
  }
  executionOrder.value = order
  executedEdges.value = edgeSet
}

function edgePath(e) {
  const from = nodes.value.find(n => n.id === e.from)
  const to = nodes.value.find(n => n.id === e.to)
  if (!from || !to) return ''
  const sx = from.x + 60, sy = from.y + 20
  const ex = to.x + 60, ey = to.y + 20
  const cx = (sx + ex) / 2
  return `M${sx},${sy} C${cx},${sy} ${cx},${ey} ${ex},${ey}`
}

async function loadWorkflows() {
  try {
    const r = await agentApi.list({ limit: 5 })
    const list = r.data?.list || r.data || []
    if (!list.length) { ElMessage.warning('暂无已保存的工作流'); return }
    // 取最新一个加载（作为示例）
    const wf = list[0]
    canvasName.value = wf.name || ''
    nodes.value = (wf.nodes || []).map(n => ({ ...n, id: n.id || newId() }))
    edges.value = wf.edges || []
    idCounter = nodes.value.length + 1
    ElMessage.success(`已加载工作流: ${wf.name || '未命名'}`)
  } catch { ElMessage.error('加载失败') }
}

async function saveCanvas() {
  if (!nodes.value.length) { ElMessage.warning('画布为空'); return }
  try {
    // V6.8.1 fix: 添加 name 字段，否则后端默认为"未命名工作流"
    const name = canvasName.value || '工作流-' + new Date().toLocaleString('zh-CN')
    const payload = { name, nodes: nodes.value, edges: edges.value }
    await agentApi.save(payload)
    ElMessage.success('画布已保存')
  } catch { ElMessage.error('保存失败') }
}

async function runCanvas() {
  if (!nodes.value.length) { ElMessage.warning('画布为空'); return }
  running.value = true
  runLog.value = []
  nodeExecStatus.value = {}
  nodeExecResult.value = {}
  logVisible.value = true

  const startNode = nodes.value.find(n => n.type === 'START')
  const goal = startNode?.config?.prompt || '请执行工作流'

  if (multiRunMode.value) {
    // ── V6.9: 多Agent流式执行 ──
    const tools = nodes.value.flatMap(n => n.config?.tools || []).filter(Boolean)
    try {
      multiAbortCtrl.value = new AbortController()
      // 收集所有LLM节点的prompt作为子任务描述
      const llmNodes = nodes.value.filter(n => n.type === 'LLM' && n.config?.prompt)
      const canvasGoal = llmNodes.length
        ? `请按以下要求协作完成：\n${llmNodes.map((n,i) => `${i+1}. ${n.label}: ${n.config.prompt}`).join('\n')}`
        : goal

      await multiAgentApi.xhrStream(
        { goal: canvasGoal, tools, maxRounds: 3 },
        handleCanvasMultiEvent
      )
    } catch (e) {
      if (e.name !== 'AbortError') {
        runLog.value.push({ tool: '错误', result: e.message || '多Agent执行失败' })
        ElMessage.error('多Agent执行失败')
      }
    } finally {
      running.value = false
      multiAbortCtrl.value = null
    }
  } else {
    // ── 单Agent执行（原有逻辑） ──
    try {
      const r = await agentApi.execute({ goal, nodes: nodes.value, edges: edges.value })
      const log = r.data?.steps || []
      runLog.value = log.map((s, i) => ({
        tool: s.action || `步骤${i+1}`,
        result: s.observation || s.thinking || s.answer || '',
      }))
      highlightExecution()
      ElMessage.success('执行完成')
    } catch (e) {
      runLog.value = [{ tool: '错误', result: e.message || '执行失败' }]
      ElMessage.error('执行失败')
      highlightExecution()
    } finally {
      running.value = false
    }
  }
}

// V6.9: 处理画布上的多Agent SSE事件
function handleCanvasMultiEvent(eventName, data) {
  const ts = new Date().toLocaleTimeString('zh-CN', { hour12: false })

  // 映射事件到节点状态
  if (eventName === 'planner-start') {
    const agentNode = nodes.value.find(n => n.type === 'LLM')
    if (agentNode) nodeExecStatus.value[agentNode.id] = 'planner'
    runLog.value.push({ tool: '🧠 Planner', result: `第 ${data.round} 轮规划开始` })
  } else if (eventName === 'planner-plan') {
    const plan = (data.steps || []).join('\n')
    runLog.value.push({ tool: '📋 计划', result: plan })
  } else if (eventName === 'executor-step') {
    // 找到对应的 LLM 节点高亮
    const llmNodes = nodes.value.filter(n => n.type === 'LLM')
    const target = llmNodes[data.step - 1] || llmNodes[0]
    if (target) {
      nodeExecStatus.value[target.id] = 'executor'
      nodeExecResult.value[target.id] = `执行中: ${data.goal}`
    }
    runLog.value.push({ tool: `⚡ 步骤${data.step}`, result: data.goal })
  } else if (eventName === 'executor-result') {
    const llmNodes = nodes.value.filter(n => n.type === 'LLM')
    const target = llmNodes[data.step - 1] || llmNodes[0]
    if (target) {
      nodeExecStatus.value[target.id] = 'done'
      nodeExecResult.value[target.id] = data.observation
    }
    runLog.value.push({ tool: `📥 步骤${data.step}结果`, result: data.observation })
  } else if (eventName === 'critic-result') {
    runLog.value.push({ tool: '🔍 Critic评估', result: `评分 ${data.score}/10 · ${data.passed ? '通过' : '未通过'} · ${data.feedback}` })
    if (!data.passed) {
      // 标记所有 LLM 节点为 critic 失败状态
      nodes.value.filter(n => n.type === 'LLM').forEach(n => {
        nodeExecStatus.value[n.id] = 'critic'
      })
    } else {
      nodes.value.filter(n => n.type === 'LLM').forEach(n => {
        if (nodeExecStatus.value[n.id] !== 'done') nodeExecStatus.value[n.id] = 'done'
      })
    }
  } else if (eventName === 'final') {
    runLog.value.push({ tool: '🎉 最终答案', result: data.answer })
    nodes.value.filter(n => n.type === 'LLM').forEach(n => {
      if (nodeExecStatus.value[n.id] !== 'done') nodeExecStatus.value[n.id] = 'done'
    })
    ElMessage.success('多Agent协作完成')
  } else if (eventName === 'done') {
    running.value = false
    highlightExecution()
  } else if (eventName === 'error') {
    runLog.value.push({ tool: '⚠️ 错误', result: data.message })
    nodes.value.filter(n => n.type === 'LLM').forEach(n => { nodeExecStatus.value[n.id] = 'error' })
    ElMessage.error(data.message)
    running.value = false
  }
}

// ── Keyboard shortcuts ─────────────────────────────────────────────
function onKeyDown(e) {
  const tag = document.activeElement?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedId.value) {
      removeNode(selectedId.value)
      e.preventDefault()
    }
  } else if (e.key === 's' && (e.ctrlKey || e.metaKey)) {
    saveCanvas()
    e.preventDefault()
  } else if (e.key === 'Escape') {
    deselectAll()
  }
}

onMounted(() => {
  loadModels()
  nextTick(() => {
    if (canvasRef.value) {
      canvasW.value = canvasRef.value.offsetWidth
      canvasH.value = canvasRef.value.offsetHeight
    }
  })
  const ro = new ResizeObserver(() => {
    if (canvasRef.value) { canvasW.value = canvasRef.value.offsetWidth; canvasH.value = canvasRef.value.offsetHeight }
  })
  if (canvasRef.value) ro.observe(canvasRef.value)
  window.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style lang="scss" scoped>
.canvas-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #fff; border-bottom: 1px solid #e4e7ed;
}
.canvas-body { display: flex; height: calc(100vh - 105px); }
.node-palette {
  width: 140px; background: #fff; border-right: 1px solid #e4e7ed; padding: 8px;
  overflow-y: auto;
}
.palette-title { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 8px; padding: 0 4px; }
.palette-node {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; margin-bottom: 4px; border-radius: 4px;
  background: #f5f7fa; border-left: 3px solid; cursor: grab; font-size: 13px;
  transition: background 0.15s;
  &:hover { background: #ecf5ff; }
}
.canvas-area {
  flex: 1; position: relative; overflow: hidden; background: #f8fafc;
  background-image: radial-gradient(circle, #dcdfe6 1px, transparent 1px);
  background-size: 20px 20px;
}
.edge-svg { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.edge-svg path { pointer-events: stroke; }
.canvas-node {
  position: absolute; width: 120px; background: #fff; border: 1px solid #e4e7ed;
  border-radius: 6px; cursor: move; user-select: none; z-index: 1;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: box-shadow 0.15s;
  &.selected { box-shadow: 0 0 0 2px #409eff; border-color: #409eff; }
  &.running { animation: pulse 1s infinite; }
}
@keyframes pulse { 0%,100%{box-shadow:0 0 0 2px #67c23a} 50%{box-shadow:0 0 0 6px #67c23a55} }
.node-header {
  display: flex; align-items: center; gap: 4px; padding: 6px 8px;
  background: #f5f7fa; border-radius: 6px 6px 0 0; font-size: 12px; font-weight: 500;
  span { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.node-del { opacity: 0; cursor: pointer; color: #f56c6c; transition: opacity 0.15s; }
.canvas-node:hover .node-del { opacity: 1; }
.node-body { padding: 6px 8px; }
.node-type-tag { font-size: 10px; padding: 1px 5px; border-radius: 3px; display: inline-block; margin-bottom: 2px; }
.node-desc { font-size: 11px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.port { position: absolute; width: 10px; height: 10px; border-radius: 50%; background: #409eff; border: 2px solid #fff; cursor: crosshair; z-index: 2; }
.port-out { right: -5px; top: 50%; transform: translateY(-50%); }
.port-in { left: -5px; top: 50%; transform: translateY(-50%); background: #67c23a; }
.canvas-empty { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; pointer-events: none; }
.prop-panel {
  width: 260px; background: #fff; border-left: 1px solid #e4e7ed; padding: 8px; overflow-y: auto;
}
.prop-empty { text-align: center; color: #909399; padding: 40px 0; font-size: 13px; }
.prop-form { .el-form-item { margin-bottom: 10px; } }
.log-view { font-family: 'Fira Code', monospace; font-size: 13px; }
.log-step { display: flex; gap: 8px; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.log-i { color: #909399; min-width: 20px; }
.log-tool { font-weight: 600; color: #409eff; min-width: 80px; }
.log-result { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; }

// ── New feature styles ───────────────────────────────────────────
.zoom-controls {
  position: absolute; bottom: 12px; left: 12px;
  display: flex; align-items: center; gap: 4px;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 4px 6px; z-index: 10;
}
.zoom-pct {
  font-size: 12px; color: #606266; min-width: 38px; text-align: center;
}
.minimap {
  position: absolute; bottom: 12px; right: 12px;
  border: 1px solid #e4e7ed; border-radius: 6px;
  background: #fff; pointer-events: none; z-index: 10;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.step-badge {
  position: absolute; top: -8px; right: -8px;
  width: 18px; height: 18px; border-radius: 50%;
  background: #67c23a; color: #fff;
  font-size: 10px; font-weight: 700; line-height: 18px; text-align: center;
  border: 2px solid #fff; z-index: 5;
  animation: badge-pulse 1s infinite;
}
.step-badge.step-error { background: #f56c6c; animation: none; }
@keyframes badge-pulse { 0%,100%{transform:scale(1)} 50%{transform:scale(1.15)} }
.edge-executed { filter: drop-shadow(0 0 3px #67c23a); }

/* V6.9: 多Agent执行状态 */
.node-exec-badge {
  position: absolute; top: -10px; right: -10px;
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; z-index: 10; border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,.15);
  animation: badge-pulse 1.5s infinite;
}
.exec-planner { background: #3b82f6; color: #fff; }
.exec-executor { background: #f59e0b; color: #fff; }
.exec-critic { background: #ec4899; color: #fff; }
.exec-done { background: #10b981; color: #fff; animation: none; }
.exec-error { background: #ef4444; color: #fff; animation: none; }
.node-exec-popup {
  position: absolute; bottom: calc(100% + 4px); left: 50%; transform: translateX(-50%);
  background: #1f2937; color: #f9fafb; font-size: 11px;
  padding: 6px 8px; border-radius: 6px; white-space: nowrap;
  max-width: 200px; overflow: hidden; text-overflow: ellipsis;
  z-index: 20; pointer-events: none;
  box-shadow: 0 4px 12px rgba(0,0,0,.2);
  &::after { content:''; position:absolute; top:100%; left:50%; transform:translateX(-50%);
    border:5px solid transparent; border-top-color:#1f2937; }
}
</style>
