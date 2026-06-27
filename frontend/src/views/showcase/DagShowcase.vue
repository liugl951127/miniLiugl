<!--
  Agent DAG 工作流设计器 (V4.2)
  - 6 种节点: input/llm/tool/condition/loop/output
  - 拖拽连线
  - 实时执行进度 (mock)
-->
<template>
  <div class="dag">
    <header class="header">
      <h1>🔀 Agent DAG 工作流</h1>
      <p class="subtitle">拖拽节点 + 连线, 设计复杂 AI 工作流</p>
      <div class="badges">
        <span class="badge">6 节点类型</span>
        <span class="badge">拖拽设计</span>
        <span class="badge">实时执行</span>
      </div>
    </header>

    <div class="dag-layout">
      <!-- 节点面板 -->
      <aside class="palette">
        <h3>节点</h3>
        <div
          v-for="t in nodeTypes"
          :key="t.type"
          :class="['palette-node', `type-${t.type}`]"
          draggable="true"
          @dragstart="onDragStart($event, t.type)"
        >
          <span class="pn-icon">{{ t.icon }}</span>
          <span class="pn-label">{{ t.label }}</span>
          <span class="pn-desc">{{ t.desc }}</span>
        </div>

        <h3 style="margin-top: 24px">模板</h3>
        <el-button v-for="tpl in templates" :key="tpl.name" size="small" plain block
                   style="margin-bottom: 6px; text-align: left"
                   @click="loadTemplate(tpl)">
          {{ tpl.icon }} {{ tpl.name }}
        </el-button>

        <h3 style="margin-top: 24px">操作</h3>
        <el-button type="success" size="small" @click="executeDag" :disabled="executing || nodes.length === 0" block>
          ▶ 执行
        </el-button>
        <el-button type="warning" size="small" @click="cancelExec" :disabled="!executing" block>
          ⏹ 停止
        </el-button>
        <el-button size="small" @click="clearCanvas" block>🗑 清空</el-button>
        <el-button size="small" @click="exportJson" block>📤 导出 JSON</el-button>
      </aside>

      <!-- 画布 -->
      <main class="canvas-wrap" @drop="onDrop" @dragover.prevent>
        <div ref="canvas" class="canvas" :style="{ backgroundSize: '20px 20px' }">
          <svg class="lines" :width="canvasWidth" :height="canvasHeight">
            <defs>
              <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth">
                <path d="M0,0 L0,6 L9,3 z" fill="#6366f1" />
              </marker>
            </defs>
            <path v-for="(e, i) in edges" :key="i"
                  :d="edgePath(e)" stroke="#6366f1" stroke-width="2" fill="none"
                  marker-end="url(#arrow)" class="edge-path" />
          </svg>
          <div v-for="(n, i) in nodes" :key="n.id"
               :class="['node', `type-${n.type}`, { selected: selectedNode === n, executing: n.status === 'running', done: n.status === 'done' }]"
               :style="{ left: n.x + 'px', top: n.y + 'px' }"
               @mousedown="startDrag($event, i)"
               @click="selectedNode = n">
            <div class="node-icon">{{ nodeIcon(n.type) }}</div>
            <div class="node-body">
              <div class="node-title">{{ n.label }}</div>
              <div class="node-cfg">{{ formatCfg(n) }}</div>
            </div>
            <div class="node-port in" @mousedown.stop="startConnect($event, n, 'in')"></div>
            <div class="node-port out" @mousedown.stop="startConnect($event, n, 'out')"></div>
            <el-icon v-if="n.status === 'done'" class="node-status done-status"><CircleCheckFilled /></el-icon>
            <el-icon v-else-if="n.status === 'running'" class="node-status running-status is-loading"><Loading /></el-icon>
            <el-icon v-else-if="n.status === 'error'" class="node-status error-status"><WarningFilled /></el-icon>
          </div>
          <div v-if="nodes.length === 0" class="canvas-placeholder">
            <p>👈 从左侧拖拽节点到此处开始</p>
            <p>或选个模板快速开始</p>
          </div>
        </div>
      </main>

      <!-- 配置面板 -->
      <aside class="inspector">
        <h3>{{ selectedNode ? '节点配置' : '执行结果' }}</h3>
        <div v-if="selectedNode" class="inspector-form">
          <el-form label-position="top" size="small">
            <el-form-item label="标签">
              <el-input v-model="selectedNode.label" />
            </el-form-item>
            <el-form-item v-if="['llm'].includes(selectedNode.type)" label="模型">
              <el-select v-model="selectedNode.cfg.model" style="width: 100%">
                <el-option v-for="m in availableModels" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="['llm'].includes(selectedNode.type)" label="Prompt">
              <el-input v-model="selectedNode.cfg.prompt" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item v-if="['tool'].includes(selectedNode.type)" label="工具">
              <el-select v-model="selectedNode.cfg.tool" style="width: 100%">
                <el-option label="calculator" value="calculator" />
                <el-option label="time" value="time" />
                <el-option label="random" value="random" />
                <el-option label="http_get" value="http_get" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="['condition'].includes(selectedNode.type)" label="条件表达式">
              <el-input v-model="selectedNode.cfg.expr" placeholder="result.includes('success')" />
            </el-form-item>
            <el-form-item v-if="['loop'].includes(selectedNode.type)" label="循环次数">
              <el-input-number v-model="selectedNode.cfg.times" :min="1" :max="10" />
            </el-form-item>
            <el-form-item v-if="['input'].includes(selectedNode.type)" label="输入值">
              <el-input v-model="selectedNode.cfg.value" type="textarea" :rows="2" />
            </el-form-item>
            <el-button size="small" type="danger" @click="deleteNode(selectedNode)" block>
              删除节点
            </el-button>
          </el-form>
        </div>
        <div v-else class="exec-log">
          <div v-if="execSteps.length === 0" style="color: #94a3b8; text-align: center; padding: 40px 0">
            执行后这里显示步骤结果
          </div>
          <div v-for="(s, i) in execSteps" :key="i" :class="['exec-step', `s-${s.status}`]">
            <div class="es-head">
              <span class="es-num">{{ i + 1 }}</span>
              <span class="es-name">{{ s.label }}</span>
              <el-tag size="small" :type="stepTag(s.status)">{{ s.status }}</el-tag>
            </div>
            <div class="es-body">{{ s.output }}</div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Loading, WarningFilled } from '@element-plus/icons-vue'
import { listProviders } from '@/api/model'

const availableModels = ref(['mock', 'gpt-4o-mini', 'qwen-max', 'deepseek-chat'])

async function loadModelOptions() {
  try {
    const r = await listProviders(1, 50)
    const providers = r?.data?.records || r?.data || []
    if (providers.length) {
      const models = new Set(availableModels.value)
      providers.forEach(p => {
        if (p.models) {
          JSON.parse(p.models).forEach(m => models.add(m))
        }
      })
      availableModels.value = Array.from(models)
    }
  } catch {
    // 拉取失败用默认列表
  }
}

const nodeTypes = [
  { type: 'input', icon: '📥', label: '输入', desc: '起始输入' },
  { type: 'llm', icon: '🧠', label: 'LLM', desc: '大模型调用' },
  { type: 'tool', icon: '🛠', label: '工具', desc: '4 内置工具' },
  { type: 'condition', icon: '🔀', label: '条件', desc: '分支判断' },
  { type: 'loop', icon: '🔁', label: '循环', desc: '重复执行' },
  { type: 'output', icon: '📤', label: '输出', desc: '最终结果' },
]

const nodeIcon = (type) => nodeTypes.find(n => n.type === type)?.icon || '●'

const templates = [
  { name: '简单问答', icon: '💬', nodes: [
    { id: 'n1', type: 'input', label: '用户问题', x: 80, y: 100, cfg: { value: '你好' } },
    { id: 'n2', type: 'llm', label: 'GPT 回答', x: 320, y: 100, cfg: { model: 'mock', prompt: '回答: {{input}}' } },
    { id: 'n3', type: 'output', label: '返回', x: 560, y: 100, cfg: {} },
  ], edges: [{ from: 'n1', to: 'n2' }, { from: 'n2', to: 'n3' }] },
  { name: '搜索 + 总结', icon: '🔍', nodes: [
    { id: 'n1', type: 'input', label: '查询', x: 80, y: 100, cfg: { value: 'AI 大模型最新进展' } },
    { id: 'n2', type: 'tool', label: '搜索', x: 320, y: 100, cfg: { tool: 'http_get' } },
    { id: 'n3', type: 'llm', label: '总结', x: 560, y: 100, cfg: { model: 'mock', prompt: '总结搜索结果' } },
    { id: 'n4', type: 'output', label: '输出', x: 800, y: 100, cfg: {} },
  ], edges: [{ from: 'n1', to: 'n2' }, { from: 'n2', to: 'n3' }, { from: 'n3', to: 'n4' }] },
  { name: '多模型投票', icon: '🗳', nodes: [
    { id: 'n1', type: 'input', label: '问题', x: 80, y: 100, cfg: { value: '哪个城市人口最多?' } },
    { id: 'n2', type: 'llm', label: 'GPT 答', x: 320, y: 50, cfg: { model: 'gpt-4o-mini', prompt: '简短回答: {{input}}' } },
    { id: 'n3', type: 'llm', label: 'Qwen 答', x: 320, y: 200, cfg: { model: 'qwen-max', prompt: '简短回答: {{input}}' } },
    { id: 'n4', type: 'llm', label: '裁决', x: 560, y: 125, cfg: { model: 'mock', prompt: '从 2 个回答选最佳: A=... B=...' } },
    { id: 'n5', type: 'output', label: '结果', x: 800, y: 125, cfg: {} },
  ], edges: [{ from: 'n1', to: 'n2' }, { from: 'n1', to: 'n3' }, { from: 'n2', to: 'n4' }, { from: 'n3', to: 'n4' }, { from: 'n4', to: 'n5' }] },
]

const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const canvasWidth = 1200
const canvasHeight = 600
const executing = ref(false)
const execSteps = ref([])

// ===== 拖拽节点到画布 =====
function onDragStart(e, type) {
  e.dataTransfer.setData('nodeType', type)
}
function onDrop(e) {
  const type = e.dataTransfer.getData('nodeType')
  if (!type) return
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX - rect.left - 60
  const y = e.clientY - rect.top - 30
  const id = 'n' + (Date.now() % 100000)
  const defaultCfg = {
    input: { value: '' },
    llm: { model: 'mock', prompt: '请回答: {{input}}' },
    tool: { tool: 'calculator' },
    condition: { expr: '' },
    loop: { times: 3 },
    output: {},
  }[type]
  nodes.value.push({
    id, type, x: Math.max(0, x), y: Math.max(0, y),
    label: nodeTypes.find(n => n.type === type)?.label || type,
    cfg: defaultCfg,
    status: 'idle',
  })
}

// ===== 拖动节点 =====
let dragInfo = null
function startDrag(e, idx) {
  dragInfo = { idx, dx: e.clientX - nodes.value[idx].x, dy: e.clientY - nodes.value[idx].y }
  const onMove = (ev) => {
    if (!dragInfo) return
    nodes.value[dragInfo.idx].x = ev.clientX - dragInfo.dx
    nodes.value[dragInfo.idx].y = ev.clientY - dragInfo.dy
  }
  const onUp = () => {
    dragInfo = null
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// ===== 连线 =====
let connectInfo = null
function startConnect(e, node, port) {
  connectInfo = { node, port, x: e.clientX, y: e.clientY }
  const onMove = (ev) => { connectInfo.x = ev.clientX; connectInfo.y = ev.clientY }
  const onUp = (ev) => {
    // 检查是否落在另一个节点的 port
    const target = document.elementFromPoint(ev.clientX, ev.clientY)
    if (target && target.classList.contains('node-port')) {
      const fromNode = connectInfo.port === 'out' ? connectInfo.node : findNodeFromPort(target)
      const toNode = connectInfo.port === 'in' ? connectInfo.node : findNodeFromPort(target)
      if (fromNode && toNode && fromNode !== toNode) {
        addEdge(fromNode, toNode)
      }
    }
    connectInfo = null
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}
function findNodeFromPort(el) {
  const nodeEl = el.closest('.node')
  const id = nodes.value.find(n => nodeEl && nodeEl.style.left === n.x + 'px')
  return id
}
function addEdge(from, to) {
  if (edges.value.some(e => e.from === from.id && e.to === to.id)) return
  edges.value.push({ from: from.id, to: to.id })
}
function edgePath(e) {
  const from = nodes.value.find(n => n.id === e.from)
  const to = nodes.value.find(n => n.id === e.to)
  if (!from || !to) return ''
  const x1 = from.x + 140, y1 = from.y + 30
  const x2 = to.x, y2 = to.y + 30
  const cx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${cx} ${y1}, ${cx} ${y2}, ${x2} ${y2}`
}

function formatCfg(n) {
  if (n.type === 'llm') return n.cfg.model
  if (n.type === 'tool') return n.cfg.tool
  if (n.type === 'input') return (n.cfg.value || '').slice(0, 20)
  return n.type
}

function deleteNode(n) {
  nodes.value = nodes.value.filter(x => x !== n)
  edges.value = edges.value.filter(e => e.from !== n.id && e.to !== n.id)
  selectedNode.value = null
}
function clearCanvas() {
  nodes.value = []
  edges.value = []
  selectedNode.value = null
  execSteps.value = []
}
function loadTemplate(tpl) {
  try {
    if (!tpl || !tpl.nodes || !tpl.edges) {
      ElMessage.error('模板数据不完整, 缺少 nodes 或 edges 字段')
      return
    }
    // 兼容后端返回字符串 或 前端直接返回对象
    const parsedNodes = typeof tpl.nodes === 'string' ? JSON.parse(tpl.nodes) : tpl.nodes
    const parsedEdges = typeof tpl.edges === 'string' ? JSON.parse(tpl.edges) : tpl.edges
    nodes.value = JSON.parse(JSON.stringify(parsedNodes))
    edges.value = JSON.parse(JSON.stringify(parsedEdges))
    ElMessage.success(`已加载模板: ${tpl.name}`)
  } catch (e) {
    ElMessage.error('模板格式错误, 加载失败: ' + e.message)
  }
}
function exportJson() {
  const data = { nodes: nodes.value, edges: edges.value }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `dag-${Date.now()}.json`
  a.click()
}

async function executeDag() {
  if (nodes.value.length === 0) return
  executing.value = true
  execSteps.value = []
  for (const n of nodes.value) n.status = 'idle'

  // 拓扑排序
  const order = topoSort(nodes.value, edges.value)
  if (!order) {
    ElMessage.error('DAG 有循环依赖!')
    executing.value = false
    return
  }

  // 模拟执行
  for (const id of order) {
    const n = nodes.value.find(x => x.id === id)
    if (!n) continue
    n.status = 'running'
    await new Promise(r => setTimeout(r, 600 + Math.random() * 400))
    const output = mockExec(n)
    execSteps.value.push({ id, label: n.label, status: 'done', output })
    n.status = 'done'
  }
  executing.value = false
  ElMessage.success('DAG 执行完成')
}
function cancelExec() {
  executing.value = false
  for (const n of nodes.value) if (n.status === 'running') n.status = 'idle'
}
function mockExec(n) {
  const inputs = execSteps.value.filter(s => s.status === 'done').map(s => s.output).join(' | ')
  switch (n.type) {
    case 'input': return n.cfg.value || '(空输入)'
    case 'llm': return `LLM(${n.cfg.model}) 响应: 已生成回答 (基于 prompt "${(n.cfg.prompt || '').slice(0, 30)}...")`
    case 'tool': return `Tool(${n.cfg.tool}) 执行: 返回结果 (基于 ${inputs.slice(0, 30)})`
    case 'condition': return `条件 "${n.cfg.expr || 'true'}" 判定: true → 走 then 分支`
    case 'loop': return `循环 ${n.cfg.times || 1} 次: 已迭代 ${n.cfg.times || 1} 次`
    case 'output': return `最终输出: ${inputs || '(无输入)'}`
    default: return '(未知节点)'
  }
}
function topoSort(nodes, edges) {
  const adj = {}
  const inDeg = {}
  nodes.forEach(n => { adj[n.id] = []; inDeg[n.id] = 0 })
  edges.forEach(e => { adj[e.from].push(e.to); inDeg[e.to]++ })
  const q = []
  for (const id in inDeg) if (inDeg[id] === 0) q.push(id)
  const order = []
  while (q.length) {
    const u = q.shift()
    order.push(u)
    for (const v of adj[u]) {
      if (--inDeg[v] === 0) q.push(v)
    }
  }
  return order.length === nodes.length ? order : null
}
function stepTag(status) {
  return { done: 'success', running: 'warning', error: 'danger', idle: 'info' }[status]
}

onMounted(async () => {
  await loadModelOptions()
  try { loadTemplate(templates[0]) } catch {}
})
</script>

<style scoped>
.dag { max-width: 1600px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; border-radius: 12px; font-size: 12px; }

.dag-layout { display: grid; grid-template-columns: 200px 1fr 280px; gap: 12px; margin-top: 16px; min-height: 600px; }
.palette, .canvas-wrap, .inspector { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.palette h3, .inspector h3 { font-size: 14px; margin: 0 0 12px; }

.palette-node { display: grid; grid-template-columns: 28px 1fr; gap: 4px 8px; padding: 8px 10px;
  border: 2px solid #e2e8f0; border-radius: 8px; margin-bottom: 6px; cursor: grab; }
.palette-node:hover { border-color: #6366f1; background: #f8fafc; }
.palette-node:active { cursor: grabbing; }
.pn-icon { font-size: 22px; }
.pn-label { font-weight: 600; font-size: 13px; }
.pn-desc { grid-column: 2; font-size: 11px; color: #94a3b8; }
.type-input { border-left: 3px solid #10b981; }
.type-llm { border-left: 3px solid #6366f1; }
.type-tool { border-left: 3px solid #f59e0b; }
.type-condition { border-left: 3px solid #ef4444; }
.type-loop { border-left: 3px solid #8b5cf6; }
.type-output { border-left: 3px solid #06b6d4; }

.canvas { width: 100%; min-height: 600px; background-color: #fafafa;
  background-image: radial-gradient(circle, #cbd5e1 1px, transparent 1px); position: relative; }
.canvas-placeholder { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  color: #94a3b8; text-align: center; }

.lines { position: absolute; inset: 0; pointer-events: none; }
.edge-path { pointer-events: stroke; }

.node { position: absolute; width: 140px; min-height: 60px; background: #fff;
  border: 2px solid #6366f1; border-radius: 8px; padding: 8px; cursor: move;
  box-shadow: 0 2px 6px rgba(0,0,0,.08); user-select: none; display: grid;
  grid-template-columns: 28px 1fr; gap: 4px 8px; }
.node.selected { box-shadow: 0 0 0 3px rgba(99,102,241,.3); }
.node.executing { animation: pulse 1.5s infinite; border-color: #f59e0b; }
.node.done { border-color: #10b981; background: linear-gradient(135deg, #f0fdf4, #fff); }
@keyframes pulse { 0%, 100% { box-shadow: 0 0 0 0 rgba(245,158,11,.4); } 50% { box-shadow: 0 0 0 8px rgba(245,158,11,0); } }
.node-icon { font-size: 20px; }
.node-title { font-weight: 600; font-size: 13px; }
.node-cfg { grid-column: 2; font-size: 11px; color: #64748b; }
.node-port { position: absolute; width: 12px; height: 12px; border-radius: 50%; background: #6366f1;
  top: 50%; transform: translateY(-50%); cursor: crosshair; border: 2px solid #fff; box-shadow: 0 0 0 1px #6366f1; }
.node-port.in { left: -6px; }
.node-port.out { right: -6px; }
.node-status { position: absolute; top: -8px; right: -8px; background: #fff; border-radius: 50%; padding: 2px; }

.inspector-form { margin-top: 12px; }
.exec-log { margin-top: 12px; max-height: 500px; overflow-y: auto; }
.exec-step { background: #f8fafc; border-radius: 6px; padding: 8px; margin-bottom: 6px;
  border-left: 3px solid #94a3b8; }
.exec-step.s-done { border-left-color: #10b981; }
.exec-step.s-running { border-left-color: #f59e0b; }
.exec-step.s-error { border-left-color: #ef4444; }
.es-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.es-num { background: #6366f1; color: #fff; border-radius: 50%; width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center; font-size: 11px; }
.es-name { font-weight: 600; font-size: 13px; flex: 1; }
.es-body { font-family: monospace; font-size: 11px; color: #334155; word-break: break-word; }
</style>
