<!-- @file agent/Canvas.vue - Agent 画布 V8.0 -->
<template>
  <div class="page-card" style="padding:0;overflow:hidden" @contextmenu.prevent>
    <!-- 顶部工具栏 -->
    <CanvasToolbar
      :can-undo="undoStack.length > 0"
      :can-redo="redoStack.length > 0"
      :has-selection="selectedIds.size > 0"
      :has-clipboard="clipboard.length > 0"
      :has-versions="localVersions.length > 0"
      :running="running"
      :multi-run-mode="multiRunMode"
      @undo="undo" @redo="redo" @copy="copySelected" @paste="pasteNodes(null)"
      @search="searchVisible = true" @history="showVersionHistory = true"
      @shortcuts="showShortcuts = true"
      @load="loadWorkflows" @new="newCanvas" @save="saveCanvas"
      @run="runCanvas" @stop="stopCanvasMulti" @toggle-multi="multiRunMode = !multiRunMode"
      @auto-layout="autoLayout" @export="exportFlow" @import="importFlow"
      @clear="clearCanvas"
    />

    <div class="canvas-body">
      <!-- 左侧节点面板 -->
      <NodePalette
        :node-types="nodeTypes"
        @drag-start="onDragStart"
        @context-menu="onNodeTypeContextMenu"
      />
      <!-- 画布区域 -->
      <div class="canvas-area" ref="canvasRef"
        @dragover.prevent
        @drop="onDrop"
        @wheel.prevent="onWheel"
        @mousedown="onCanvasMouseDown"
        @mousemove="onCanvasMouseMove"
        @mouseup="onCanvasMouseUp"
        @contextmenu.prevent="onCanvasContextMenu"
      >
        <!-- 右键菜单 -->
        <div v-if="contextMenu.visible" class="ctx-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
          <template v-if="contextMenu.type === 'canvas'">
            <div class="ctx-item" @click="ctxPaste"><el-icon><DocumentCopy /></el-icon> 粘贴 <span class="ctx-hint">Ctrl+V</span></div>
            <div class="ctx-divider" />
            <div class="ctx-item" @click="ctxSelectAll"><el-icon><Select /></el-icon> 全选 <span class="ctx-hint">Ctrl+A</span></div>
            <div class="ctx-item" @click="ctxAutoLayout"><el-icon><Grid /></el-icon> 自动布局</div>
          </template>
          <template v-else-if="contextMenu.type === 'node'">
            <div class="ctx-item" @click="ctxCopy"><el-icon><CopyDocument /></el-icon> 复制 <span class="ctx-hint">Ctrl+C</span></div>
            <div class="ctx-item" @click="ctxDuplicate"><el-icon><DocumentCopy /></el-icon> 复制节点</div>
            <div class="ctx-divider" />
            <div class="ctx-item" @click="ctxDelete"><el-icon><Delete /></el-icon> 删除 <span class="ctx-hint">Delete</span></div>
          </template>
        </div>

        <!-- SVG 连线层 -->
        <svg class="edge-svg" :width="canvasW" :height="canvasH">
          <defs>
            <marker id="arr" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#409eff" />
            </marker>
            <marker id="arr-executed" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#67c23a" />
            </marker>
          </defs>
          <path
            v-for="(e, i) in edges" :key="i"
            :d="edgePath(e)"
            :stroke="executedEdges.has(i) ? '#67c23a' : '#409eff'"
            :stroke-width="executedEdges.has(i) ? 2.5 : 1.5"
            :class="{ 'edge-executed': executedEdges.has(i) }"
            fill="none"
            :marker-end="executedEdges.has(i) ? 'url(#arr-executed)' : 'url(#arr)'"
            style="cursor:pointer"
            @click.stop="removeEdge(i)"
            @dblclick.stop="editEdgeLabel(i)"
          />
          <!-- Edge labels -->
          <g v-for="(e, i) in edges" :key="'label-'+i">
            <foreignObject
              v-if="e.label"
              :x="edgeMidX(e) - 40"
              :y="edgeMidY(e) - 10"
              width="80"
              height="20"
              style="pointer-events:all;cursor:pointer"
              @click.stop="editEdgeLabel(i)"
            >
              <div xmlns="http://www.w3.org/1999/xhtml" class="edge-label">{{ e.label }}</div>
            </foreignObject>
          </g>
          <!-- 拖拽中的连线预览 -->
          <path
            v-if="dragLine"
            :d="`M${dragLine.x1},${dragLine.y1} L${dragLine.x2},${dragLine.y2}`"
            stroke="#409eff" stroke-width="1.5" stroke-dasharray="5,3" fill="none"
          />
          <!-- 对齐辅助线 -->
          <g v-if="snapGuides.horizontal">
            <line
              :x1="0" :y1="snapGuides.horizontal"
              :x2="canvasW" :y2="snapGuides.horizontal"
              stroke="#409eff" stroke-width="1" stroke-dasharray="4,3"
            />
          </g>
          <g v-if="snapGuides.vertical">
            <line
              :x1="snapGuides.vertical" :y1="0"
              :x2="snapGuides.vertical" :y2="canvasH"
              stroke="#409eff" stroke-width="1" stroke-dasharray="4,3"
            />
          </g>
        </svg>

        <!-- 框选矩形 -->
        <div
          v-if="selectionBox"
          class="selection-box"
          :style="{
            left: Math.min(selectionBox.x1, selectionBox.x2) + 'px',
            top: Math.min(selectionBox.y1, selectionBox.y2) + 'px',
            width: Math.abs(selectionBox.x2 - selectionBox.x1) + 'px',
            height: Math.abs(selectionBox.y2 - selectionBox.y1) + 'px'
          }"
        />

        <!-- 节点 -->
        <div
          v-for="n in nodes" :key="n.id"
          class="canvas-node"
          :style="{ left: n.x+'px', top: n.y+'px', borderTopColor: n.color }"
          :class="{
            selected: selectedIds.has(n.id),
            'multi-selected': selectedIds.has(n.id) && selectedIds.size > 1,
            running: n.id === runningNode,
            'search-highlight': searchHighlightId === n.id,
          }"
          @click.stop="handleNodeClick($event, n)"
          @mousedown="startMove($event, n)"
          @dblclick.stop="onNodeDoubleClick(n)"
          @contextmenu.stop.prevent="onNodeContextMenu($event, n)"
        >
          <div class="node-header">
            <el-icon><component :is="n.icon" /></el-icon>
            <span>{{ n.label }}</span>
            <el-icon class="node-del" @click.stop="removeNode(n.id)"><Close /></el-icon>
          </div>
          <div class="node-body">
            <div class="node-type-tag" :style="{ background: n.color + '22', color: n.color }">{{ n.type }}</div>
            <div v-if="n.config?.prompt" class="node-desc">{{ n.config.prompt.slice(0,40) }}…</div>
            <!-- HTTP/API 节点额外显示 -->
            <div v-if="(n.type === 'HTTP' || n.type === 'API') && n.config?.url" class="node-url">{{ n.config.url.slice(0,30) }}…</div>
          </div>
          <!-- 多Agent执行状态浮层 -->
          <div v-if="nodeExecStatus[n.id]" class="node-exec-badge" :class="`exec-${nodeExecStatus[n.id]}`">
            <span>{{ {planner:'🧠',executor:'⚡',critic:'🔍',done:'✅',error:'❌'}[nodeExecStatus[n.id]] || nodeExecStatus[n.id] }}</span>
          </div>
          <div v-if="nodeExecResult[n.id]" class="node-exec-popup">
            {{ nodeExecResult[n.id]?.slice(0,80) }}{{ (nodeExecResult[n.id]?.length||0) > 80 ? '…' : '' }}
          </div>
          <!-- Step badge -->
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
          <div style="font-size:11px;color:#c0c4cc;margin-top:8px">右键画布查看快捷操作</div>
        </div>

        <!-- Zoom controls -->
        <div class="zoom-controls">
          <el-button size="small" @click="zoom = Math.max(0.3, +(zoom - 0.1).toFixed(2))">−</el-button>
          <span class="zoom-pct">{{ Math.round(zoom * 100) }}%</span>
          <el-button size="small" @click="zoom = Math.min(2.0, +(zoom + 0.1).toFixed(2))">+</el-button>
          <el-button size="small" @click="zoom = 1" style="margin-left:4px" title="重置100%">↺</el-button>
        </div>

        <!-- Mini-map -->
        <div class="minimap">
          <svg :width="minimapW" :height="minimapH" style="display:block">
            <circle
              v-for="n in nodes" :key="n.id"
              :cx="(n.x / canvasW) * minimapW + 3"
              :cy="(n.y / canvasH) * minimapH + 2"
              r="3"
              :fill="n.color || '#409eff'"
            />
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
              <el-input v-model="selectedNode.label" @change="recordHistory('prop')" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="selectedNode.type" style="width:100%" @change="onNodeTypeChange">
                <el-option v-for="nt in nodeTypes" :key="nt.type" :label="nt.label" :value="nt.type" />
              </el-select>
            </el-form-item>
            <!-- HTTP / API 节点特有字段 -->
            <template v-if="selectedNode.type === 'HTTP' || selectedNode.type === 'API'">
              <el-form-item label="请求URL">
                <el-input v-model="selectedNode.config.url" placeholder="https://api.example.com/endpoint" @change="recordHistory('prop')" />
              </el-form-item>
              <el-form-item label="请求方法">
                <el-select v-model="selectedNode.config.method" style="width:100%" @change="recordHistory('prop')">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                  <el-option label="PATCH" value="PATCH" />
                </el-select>
              </el-form-item>
              <el-form-item label="请求头">
                <el-input v-model="selectedNode.config.headers" type="textarea" :rows="2" placeholder='{"Content-Type":"application/json"}' @change="recordHistory('prop')" />
              </el-form-item>
              <el-form-item label="请求体">
                <el-input v-model="selectedNode.config.body" type="textarea" :rows="3" placeholder='{"key":"value"}' @change="recordHistory('prop')" />
              </el-form-item>
              <el-form-item label="超时(ms)">
                <el-input-number v-model="selectedNode.config.timeout" :min="1000" :max="60000" :step="1000" style="width:100%" @change="recordHistory('prop')" />
              </el-form-item>
            </template>
            <!-- 通用字段 -->
            <el-form-item label="Prompt">
              <el-input v-model="selectedNode.config.prompt" type="textarea" :rows="4"
                placeholder="输入节点指令…" @change="recordHistory('prop')" />
            </el-form-item>
            <template v-if="selectedNode.type !== 'HTTP' && selectedNode.type !== 'API'">
              <el-form-item label="工具">
                <el-select v-model="selectedNode.config.tools" multiple style="width:100%" placeholder="选择工具" @change="recordHistory('prop')">
                  <el-option v-for="t in toolOptions" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
              <el-form-item label="模型">
                <el-select v-model="selectedNode.config.model" style="width:100%" placeholder="默认模型" filterable @change="recordHistory('prop')">
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
                <el-input-number v-model="selectedNode.config.maxSteps" :min="1" :max="20" @change="recordHistory('prop')" />
              </el-form-item>
            </template>
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

    <!-- 搜索弹窗 -->
    <el-dialog v-model="searchVisible" title="搜索节点" width="380px" :append-to-body="true">
      <el-input
        ref="searchInputRef"
        v-model="searchQuery"
        placeholder="输入节点名称…"
        clearable
        @input="onSearchInput"
        @keydown.enter="focusFirstResult"
      />
      <div v-if="searchResults.length > 0" style="margin-top:8px;max-height:300px;overflow-y:auto">
        <div
          v-for="n in searchResults" :key="n.id"
          class="search-result-item"
          @click="focusNode(n)"
        >
          <span class="search-result-dot" :style="{ background: n.color }" />
          <span>{{ n.label }}</span>
          <span style="color:#909399;font-size:12px;margin-left:6px">{{ n.type }}</span>
        </div>
      </div>
      <div v-else-if="searchQuery && searchResults.length === 0" style="color:#909399;text-align:center;padding:20px">
        未找到匹配节点
      </div>
      <template #footer>
        <span style="font-size:12px;color:#c0c4cc">Enter 定位 · Esc 关闭</span>
      </template>
    </el-dialog>

    <!-- 连线标签编辑 -->
    <el-dialog v-model="edgeLabelVisible" title="编辑连线标签" width="320px" :append-to-body="true">
      <el-input v-model="edgeLabelText" placeholder="输入关系描述…" clearable @keydown.enter="saveEdgeLabel" />
      <template #footer>
        <el-button @click="edgeLabelVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdgeLabel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快捷键手册 -->
    <el-dialog v-model="showShortcuts" title="⌨️ 画布快捷键" width="480px" :append-to-body="true">
      <div class="shortcut-table">
        <div v-for="s in shortcuts" :key="s.key" class="shortcut-row">
          <div class="shortcut-keys">
            <kbd v-for="k in s.keys" :key="k">{{ k }}</kbd>
          </div>
          <span class="shortcut-desc">{{ s.desc }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 版本历史 -->
    <el-dialog v-model="showVersionHistory" title="📜 本地版本历史" width="520px" :append-to-body="true">
      <div v-if="localVersions.length === 0" style="color:#909399;text-align:center;padding:30px">
        暂无本地版本记录<br/>
        <span style="font-size:12px">点击工具栏「保存」时会自动创建快照</span>
      </div>
      <div v-else style="max-height:400px;overflow-y:auto">
        <div v-for="(v, i) in localVersions" :key="i" class="version-item">
          <div class="version-info">
            <span class="version-name">{{ v.name }}</span>
            <span class="version-time">{{ v.time }}</span>
            <span class="version-count">{{ v.nodeCount }} 节点</span>
          </div>
          <div style="display:flex;gap:6px">
            <el-button size="small" type="primary" @click="restoreVersion(i)">恢复</el-button>
            <el-button size="small" type="danger" plain @click="deleteVersion(i)">删除</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <span style="font-size:12px;color:#c0c4cc">最多保存 10 个本地快照 · 刷新页面后清除</span>
      </template>
    </el-dialog>
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
  ChatDotRound, Connection, Operation, Files, Tools, DataLine, Folder, Grid, Upload, Download,
  RefreshLeft, RefreshRight, CopyDocument, DocumentCopy, Search, Clock, QuestionFilled, Select
} from '@element-plus/icons-vue'
import CanvasToolbar from './CanvasToolbar.vue'
import NodePalette from './NodePalette.vue'

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

const nodes = ref([])
const edges = ref([])
const selectedId = ref(null)
const selectedIds = ref(new Set())
const runningNode = ref(null)
const running = ref(false)
const logVisible = ref(false)
const runLog = ref([])
const loading = ref(false)
const multiRunMode = ref(false)
const nodeExecStatus = ref({})
const nodeExecResult = ref({})
const multiAbortCtrl = ref(null)

// zoom & execution
const zoom = ref(1.0)
const executionOrder = ref(new Map())
const executedEdges = ref(new Set())
const minimapW = 120
const minimapH = 80

// ── History (Undo/Redo) ─────────────────────────────────────────────
const undoStack = ref([])
const redoStack = ref([])

function snapshot() {
  return JSON.stringify({ nodes: nodes.value, edges: edges.value })
}

function restoreSnapshot(data) {
  const d = JSON.parse(data)
  nodes.value = d.nodes
  edges.value = d.edges
}

function pushHistory() {
  undoStack.value.push(snapshot())
  if (undoStack.value.length > 50) undoStack.value.shift()
  redoStack.value = []
}

function recordHistory() { pushHistory() }

function undo() {
  if (!undoStack.value.length) return
  redoStack.value.push(snapshot())
  restoreSnapshot(undoStack.value.pop())
  selectedIds.value = new Set()
}

function redo() {
  if (!redoStack.value.length) return
  undoStack.value.push(snapshot())
  restoreSnapshot(redoStack.value.pop())
  selectedIds.value = new Set()
}

// ── Node Selection ───────────────────────────────────────────────────
const selectedNode = computed(() => {
  if (selectedId.value) return nodes.value.find(n => n.id === selectedId.value) || null
  return null
})

function handleNodeClick(e, n) {
  if (e.shiftKey) {
    if (selectedIds.value.has(n.id)) selectedIds.value.delete(n.id)
    else selectedIds.value.add(n.id)
    selectedIds.value = new Set(selectedIds.value)
  } else {
    selectedIds.value = new Set([n.id])
    selectedId.value = n.id
  }
}

function deselectAll() {
  selectedIds.value = new Set()
  selectedId.value = null
  if (searchVisible.value) { searchVisible.value = false; searchHighlightId.value = null }
  hideContextMenu()
}

function deleteSelected() {
  if (!selectedIds.value.size) return
  pushHistory()
  selectedIds.value.forEach(id => {
    edges.value = edges.value.filter(e => e.from !== id && e.to !== id)
  })
  nodes.value = nodes.value.filter(n => !selectedIds.value.has(n.id))
  selectedIds.value = new Set()
  selectedId.value = null
}

function removeNode(id) {
  pushHistory()
  nodes.value = nodes.value.filter(n => n.id !== id)
  edges.value = edges.value.filter(e => e.from !== id && e.to !== id)
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
    selectedIds.value = new Set(selectedIds.value)
  }
  if (selectedId.value === id) selectedId.value = null
}

function onNodeTypeChange(newType) {
  // Sync icon/color when type changes
  const nt = nodeTypes.find(t => t.type === newType)
  if (nt && selectedNode.value) {
    selectedNode.value.icon = nt.icon
    selectedNode.value.color = nt.color
    // Init type-specific config
    if (newType === 'HTTP' || newType === 'API') {
      selectedNode.value.config = {
        ...selectedNode.value.config,
        url: selectedNode.value.config.url || '',
        method: selectedNode.value.config.method || 'GET',
        headers: selectedNode.value.config.headers || '{}',
        body: selectedNode.value.config.body || '{}',
        timeout: selectedNode.value.config.timeout || 5000,
        prompt: selectedNode.value.config.prompt || '',
        tools: [],
        model: '',
        maxSteps: 5,
      }
    }
  }
  recordHistory()
}

function onNodeDoubleClick(n) {
  // Double-click node: select it and scroll prop panel into view
  selectedIds.value = new Set([n.id])
  selectedId.value = n.id
}

// ── Box Selection ───────────────────────────────────────────────────
const selectionBox = ref(null)
let boxStartX = 0, boxStartY = 0, isBoxSelecting = false

function onCanvasMouseDown(e) {
  if (e.button !== 0) return
  const target = e.target
  if (target !== canvasRef.value && !target.classList.contains('selection-box')) {
    if (target.closest('.canvas-node')) return
  }
  hideContextMenu()
  boxStartX = e.offsetX
  boxStartY = e.offsetY
  isBoxSelecting = true
  selectionBox.value = { x1: boxStartX, y1: boxStartY, x2: boxStartX, y2: boxStartY }
}

function onCanvasMouseMove(e) {
  if (!isBoxSelecting) return
  selectionBox.value = { x1: boxStartX, y1: boxStartY, x2: e.offsetX, y2: e.offsetY }
}

function onCanvasMouseUp(e) {
  if (!isBoxSelecting) return
  isBoxSelecting = false
  const box = selectionBox.value
  if (box) {
    const minX = Math.min(box.x1, box.x2), maxX = Math.max(box.x1, box.x2)
    const minY = Math.min(box.y1, box.y2), maxY = Math.max(box.y1, box.y2)
    const boxW = maxX - minX, boxH = maxY - minY
    if (boxW > 5 || boxH > 5) {
      const newSelected = e.shiftKey ? new Set(selectedIds.value) : new Set()
      nodes.value.forEach(n => {
        const cx = n.x + 60, cy = n.y + 30
        if (cx >= minX && cx <= maxX && cy >= minY && cy <= maxY) newSelected.add(n.id)
      })
      selectedIds.value = newSelected
      selectedId.value = newSelected.size === 1 ? [...newSelected][0] : null
    } else if (!e.shiftKey && !boxStartX) {
      deselectAll()
    }
  }
  selectionBox.value = null
}

// ── Snap Guides ─────────────────────────────────────────────────────
const snapGuides = reactive({ horizontal: null, vertical: null })
const SNAP_THRESHOLD = 10

function clearSnapGuides() { snapGuides.horizontal = null; snapGuides.vertical = null }

function checkSnapGuides(movingNode) {
  const mRight = movingNode.x + 120, mBottom = movingNode.y + 60
  const mCX = movingNode.x + 60, mCY = movingNode.y + 30
  let snapX = null, snapY = null, bestX = SNAP_THRESHOLD, bestY = SNAP_THRESHOLD

  nodes.value.forEach(n => {
    if (n.id === movingNode.id) return
    const nRight = n.x + 120, nBottom = n.y + 60
    const nCX = n.x + 60, nCY = n.y + 30

    const testsX = [
      { d: Math.abs(movingNode.x - n.x), v: n.x },
      { d: Math.abs(mRight - nRight), v: nRight - 120 },
      { d: Math.abs(mCX - nCX), v: nCX - 60 },
      { d: Math.abs(movingNode.x - nRight), v: nRight },
      { d: Math.abs(mRight - n.x), v: n.x - 120 },
    ]
    testsX.forEach(t => { if (t.d < bestX) { bestX = t.d; snapX = t.v } })

    const testsY = [
      { d: Math.abs(movingNode.y - n.y), v: n.y },
      { d: Math.abs(mBottom - nBottom), v: nBottom - 60 },
      { d: Math.abs(mCY - nCY), v: nCY - 30 },
      { d: Math.abs(movingNode.y - nBottom), v: nBottom },
      { d: Math.abs(mBottom - n.y), v: n.y - 60 },
    ]
    testsY.forEach(t => { if (t.d < bestY) { bestY = t.d; snapY = t.v } })
  })

  snapGuides.vertical = bestX < SNAP_THRESHOLD ? snapX + 60 : null
  snapGuides.horizontal = bestY < SNAP_THRESHOLD ? snapY + 30 : null
}

// ── Copy / Paste ────────────────────────────────────────────────────
const clipboard = ref([])

function copySelected() {
  if (!selectedIds.value.size) return
  clipboard.value = []
  selectedIds.value.forEach(id => {
    const n = nodes.value.find(nd => nd.id === id)
    if (n) {
      clipboard.value.push({
        label: n.label, type: n.type, icon: n.icon, color: n.color,
        config: JSON.parse(JSON.stringify(n.config || {})),
      })
    }
  })
  ElMessage.success(`已复制 ${clipboard.value.length} 个节点`)
}

function pasteNodes(e) {
  if (!clipboard.value.length) return
  pushHistory()
  const rect = canvasRef.value.getBoundingClientRect()
  const mouseX = e ? e.clientX - rect.left : canvasW.value / 2
  const mouseY = e ? e.clientY - rect.top : canvasH.value / 2

  // Smart offset: spread nodes in a grid to avoid overlap
  const cols = Math.ceil(Math.sqrt(clipboard.value.length))
  clipboard.value.forEach((item, i) => {
    const nt = nodeTypes.find(n => n.type === item.type) || nodeTypes[1]
    const col = i % cols, row = Math.floor(i / cols)
    nodes.value.push({
      id: newId(), type: item.type, label: item.label,
      icon: nt.icon, color: nt.color,
      x: mouseX - 60 + col * 140,
      y: mouseY - 30 + row * 90,
      config: JSON.parse(JSON.stringify(item.config)),
    })
  })
  ElMessage.success(`已粘贴 ${clipboard.value.length} 个节点`)
}

// ── Search ──────────────────────────────────────────────────────────
const searchVisible = ref(false)
const searchQuery = ref('')
const searchInputRef = ref(null)
const searchResults = computed(() => {
  if (!searchQuery.value) return []
  const q = searchQuery.value.toLowerCase()
  return nodes.value.filter(n => n.label.toLowerCase().includes(q) || n.type.toLowerCase().includes(q))
})
const searchHighlightId = ref(null)

function openSearch() {
  searchVisible.value = true
  searchQuery.value = ''
  nextTick(() => searchInputRef.value?.focus())
}

function onSearchInput() {
  if (searchResults.value.length > 0) focusNode(searchResults.value[0])
}

function focusFirstResult() {
  if (searchResults.value.length > 0) {
    focusNode(searchResults.value[0])
    searchVisible.value = false
  }
}

function focusNode(n) {
  searchHighlightId.value = n.id
  selectedIds.value = new Set([n.id])
  selectedId.value = n.id
  const el = canvasRef.value
  if (el) {
    el.scrollLeft = Math.max(0, n.x - canvasW.value / 2 + 60)
    el.scrollTop = Math.max(0, n.y - canvasH.value / 2 + 30)
  }
  setTimeout(() => { searchHighlightId.value = null }, 2000)
}

// ── Edge Labels ─────────────────────────────────────────────────────
const edgeLabelVisible = ref(false)
const edgeLabelText = ref('')
const edgeLabelIndex = ref(-1)

function editEdgeLabel(i) {
  edgeLabelIndex.value = i
  edgeLabelText.value = edges.value[i]?.label || ''
  edgeLabelVisible.value = true
}

function saveEdgeLabel() {
  pushHistory()
  edges.value[edgeLabelIndex.value].label = edgeLabelText.value.trim()
  edgeLabelVisible.value = false
}

function edgeMidX(e) {
  const from = nodes.value.find(n => n.id === e.from), to = nodes.value.find(n => n.id === e.to)
  if (!from || !to) return 0
  return (from.x + 60 + to.x + 60) / 2
}

function edgeMidY(e) {
  const from = nodes.value.find(n => n.id === e.from), to = nodes.value.find(n => n.id === e.to)
  if (!from || !to) return 0
  return (from.y + 20 + to.y + 20) / 2
}

// ── Context Menu ─────────────────────────────────────────────────────
const contextMenu = reactive({ visible: false, x: 0, y: 0, type: 'canvas' })
const contextNode = ref(null)

function hideContextMenu() { contextMenu.visible = false }

function onCanvasContextMenu(e) {
  // Don't show if clicking on node (node handler will catch it)
  if (e.target.closest('.canvas-node')) return
  contextMenu.type = 'canvas'
  contextMenu.x = e.offsetX
  contextMenu.y = e.offsetY
  contextMenu.visible = true
}

function onNodeContextMenu(e, n) {
  // Select this node if not already selected
  if (!selectedIds.value.has(n.id)) {
    selectedIds.value = new Set([n.id])
    selectedId.value = n.id
  }
  contextMenu.type = 'node'
  contextMenu.x = e.offsetX
  contextMenu.y = e.offsetY
  contextMenu.visible = true
  contextNode.value = n
}

function onNodeTypeContextMenu(e, nt) {
  // Double-click on palette item to add quickly
}

function ctxPaste() { pasteNodes({ clientX: contextMenu.x, clientY: contextMenu.y, offsetX: contextMenu.x, offsetY: contextMenu.y }); hideContextMenu() }
function ctxSelectAll() { nodes.value.forEach(n => selectedIds.value.add(n.id)); selectedIds.value = new Set(selectedIds.value); hideContextMenu() }
function ctxAutoLayout() { autoLayout(); hideContextMenu() }
function ctxCopy() { copySelected(); hideContextMenu() }
function ctxDuplicate() {
  if (!selectedIds.value.size) { hideContextMenu(); return }
  pushHistory()
  const newSet = new Set()
  selectedIds.value.forEach(id => {
    const n = nodes.value.find(nd => nd.id === id)
    if (n) {
      const newNode = {
        id: newId(), type: n.type, label: n.label + '_副本',
        icon: n.icon, color: n.color,
        x: n.x + 30, y: n.y + 30,
        config: JSON.parse(JSON.stringify(n.config || {})),
      }
      nodes.value.push(newNode)
      newSet.add(newNode.id)
    }
  })
  selectedIds.value = newSet
  hideContextMenu()
}
function ctxDelete() { deleteSelected(); hideContextMenu() }

// ── Shortcut Table ───────────────────────────────────────────────────
const showShortcuts = ref(false)
const shortcuts = [
  { keys: ['Ctrl', 'Z'], desc: '撤销' },
  { keys: ['Ctrl', 'Y'], desc: '重做' },
  { keys: ['Ctrl', 'C'], desc: '复制选中节点' },
  { keys: ['Ctrl', 'V'], desc: '粘贴' },
  { keys: ['Ctrl', 'A'], desc: '全选' },
  { keys: ['Ctrl', 'F'], desc: '搜索节点' },
  { keys: ['Ctrl', 'D'], desc: '复制节点（副本）' },
  { keys: ['Delete'], desc: '删除选中节点' },
  { keys: ['Backspace'], desc: '删除选中节点' },
  { keys: ['Shift', '点击'], desc: '多选节点' },
  { keys: ['Ctrl', '点击'], desc: '追加/取消选中' },
  { keys: ['F1'], desc: '查看快捷键' },
  { keys: ['Esc'], desc: '取消选中 / 关闭弹窗' },
  { keys: ['Ctrl', 'S'], desc: '保存画布' },
  { keys: ['鼠标拖拽'], desc: '框选多个节点' },
]

// ── Version History ──────────────────────────────────────────────────
const showVersionHistory = ref(false)
const localVersions = ref([])

function saveVersion(name) {
  const version = {
    name: name || `快照 ${localVersions.value.length + 1}`,
    time: new Date().toLocaleString('zh-CN'),
    nodeCount: nodes.value.length,
    data: snapshot(),
  }
  localVersions.value.unshift(version)
  if (localVersions.value.length > 10) localVersions.value.pop()
}

function restoreVersion(index) {
  const v = localVersions.value[index]
  if (!v) return
  ElMessageBox.confirm('恢复此版本将覆盖当前画布内容，确定？', '恢复版本').then(() => {
    pushHistory()
    restoreSnapshot(v.data)
    selectedIds.value = new Set()
    selectedId.value = null
    showVersionHistory.value = false
    ElMessage.success('已恢复到: ' + v.name)
  }).catch(() => {})
}

function deleteVersion(index) {
  localVersions.value.splice(index, 1)
}

// ── Main State ───────────────────────────────────────────────────────
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
  { type: 'HTTP', label: 'HTTP 请求', icon: 'Connection', color: '#f97316', desc: '发送 HTTP 请求' },
  { type: 'API', label: 'API 节点', icon: 'Operation', color: '#06b6d4', desc: '调用内部 API' },
  { type: 'END', label: '结束', icon: 'Folder', color: '#909399', desc: '输出结果' },
]

const toolOptions = ['web-search', 'code-exec', 'file-read', 'file-write', 'api-call', 'calculator']

function newId() { return 'node_' + (idCounter++) }

function newCanvas() {
  ElMessageBox.confirm('新建画布将清空当前内容，确定？').then(() => {
    pushHistory()
    nodes.value = []; edges.value = []
    selectedIds.value = new Set(); selectedId.value = null
    undoStack.value = []; redoStack.value = []
    idCounter = 1
  }).catch(() => {})
}

function clearCanvas() {
  pushHistory()
  nodes.value = []; edges.value = []
  selectedIds.value = new Set(); selectedId.value = null
}

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
    pushHistory()
    const nt = nodeTypes.find(n => n.type === type) || nodeTypes[1]
    const defaultConfig = { prompt: '', tools: [], model: '', maxSteps: 5 }
    if (type === 'HTTP' || type === 'API') {
      Object.assign(defaultConfig, { url: '', method: 'GET', headers: '{}', body: '{}', timeout: 5000 })
    }
    nodes.value.push({
      id: newId(), type: nt.type, label: nt.label,
      icon: nt.icon, color: nt.color, x, y, config: defaultConfig
    })
  } else if (fromEdge) {
    const fromNode = nodes.value.find(n => n.id === fromEdge)
    const toNode = nodes.value.find(n => Math.abs(n.x - x) < 120 && Math.abs(n.y - y) < 60)
    if (fromNode && toNode && fromNode.id !== toNode.id) {
      pushHistory()
      edges.value.push({ from: fromNode.id, to: toNode.id })
      ElMessage.success('连线已添加')
    }
  }
}

function startMove(ev, n) {
  draggingNode = n
  dragOffset = { x: ev.clientX - n.x, y: ev.clientY - n.y }
  let moved = false
  const onMove = (e) => {
    if (!draggingNode) return
    moved = true
    const dx = e.clientX - dragOffset.x - draggingNode.x
    const dy = e.clientY - dragOffset.y - draggingNode.y

    if (selectedIds.value.has(draggingNode.id) && selectedIds.value.size > 1) {
      selectedIds.value.forEach(id => {
        const node = nodes.value.find(nd => nd.id === id)
        if (node) {
          node.x = Math.max(0, Math.min(canvasW.value - 120, node.x + dx))
          node.y = Math.max(0, Math.min(canvasH.value - 60, node.y + dy))
        }
      })
      dragOffset.x = e.clientX - draggingNode.x
      dragOffset.y = e.clientY - draggingNode.y
    } else {
      draggingNode.x = Math.max(0, Math.min(canvasW.value - 120, e.clientX - dragOffset.x))
      draggingNode.y = Math.max(0, Math.min(canvasH.value - 60, e.clientY - dragOffset.y))
    }
    checkSnapGuides(draggingNode)
  }
  const onUp = () => {
    if (moved) {
      pushHistory()
      if (snapGuides.vertical !== null) {
        if (selectedIds.value.has(draggingNode.id) && selectedIds.value.size > 1) {
          const baseX = draggingNode.x
          selectedIds.value.forEach(id => {
            const node = nodes.value.find(nd => nd.id === id)
            if (node) node.x = snapGuides.vertical - 60 + (node.x - baseX)
          })
        } else {
          draggingNode.x = snapGuides.vertical - 60
        }
      }
      if (snapGuides.horizontal !== null) {
        if (selectedIds.value.has(draggingNode.id) && selectedIds.value.size > 1) {
          const baseY = draggingNode.y
          selectedIds.value.forEach(id => {
            const node = nodes.value.find(nd => nd.id === id)
            if (node) node.y = snapGuides.horizontal - 30 + (node.y - baseY)
          })
        } else {
          draggingNode.y = snapGuides.horizontal - 30
        }
      }
    }
    clearSnapGuides()
    draggingNode = null
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function startEdge(ev, n) {
  const rect = canvasRef.value.getBoundingClientRect()
  dragLine.value = { x1: n.x + 60, y1: n.y + 20, x2: ev.clientX - rect.left, y2: ev.clientY - rect.top, from: n.id }
  const onMove = (e) => {
    if (!dragLine.value) return
    dragLine.value.x2 = Math.max(0, Math.min(canvasW.value, e.clientX - rect.left))
    dragLine.value.y2 = Math.max(0, Math.min(canvasH.value, e.clientY - rect.top))
  }
  const onUp = () => {
    if (dragLine.value) {
      const target = nodes.value.find(nd =>
        nd.id !== dragLine.value.from &&
        Math.abs(nd.x + 60 - dragLine.value.x2) < 40 &&
        Math.abs(nd.y + 20 - dragLine.value.y2) < 40
      )
      if (target) {
        pushHistory()
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
  ElMessageBox.confirm('删除此连线？').then(() => { pushHistory(); edges.value.splice(i, 1) }).catch(() => {})
}

// ── Feature Functions ───────────────────────────────────────────────
function onWheel(e) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  zoom.value = Math.max(0.3, Math.min(2.0, +(zoom.value + delta).toFixed(2)))
}

function autoLayout() {
  if (!nodes.value.length) return
  pushHistory()
  const starts = nodes.value.filter(n => n.type === 'START')
  const ends   = nodes.value.filter(n => n.type === 'END')
  const middles = nodes.value.filter(n => n.type !== 'START' && n.type !== 'END')
  const rowH = Math.max(80, Math.floor((canvasH.value - 40) / (Math.max(middles.length, 1) + 2)))
  const colW = 180
  const cols = Math.ceil(Math.sqrt(nodes.value.length))
  starts.forEach((n, i) => { n.x = (i % cols) * colW + 20; n.y = 20 })
  middles.forEach((n, i) => { n.x = (i % cols) * colW + 20; n.y = 80 + Math.floor(i / cols) * rowH })
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
        pushHistory()
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
  const order = new Map()
  const edgeSet = new Set()
  runLog.value.forEach((step, i) => {
    const node = nodes.value.find(n => step.tool?.includes(n.label) || step.tool?.includes(n.type))
    if (node) order.set(node.id, i + 1)
  })
  const sorted = [...order.entries()].sort((a, b) => a[1] - b[1])
  for (let i = 0; i < sorted.length - 1; i++) {
    const idx = edges.value.findIndex(e => e.from === sorted[i][0] && e.to === sorted[i + 1][0])
    if (idx !== -1) edgeSet.add(idx)
  }
  executionOrder.value = order
  executedEdges.value = edgeSet
}

function edgePath(e) {
  const from = nodes.value.find(n => n.id === e.from), to = nodes.value.find(n => n.id === e.to)
  if (!from || !to) return ''
  const sx = from.x + 60, sy = from.y + 20, ex = to.x + 60, ey = to.y + 20
  const cx = (sx + ex) / 2
  return `M${sx},${sy} C${cx},${sy} ${cx},${ey} ${ex},${ey}`
}

async function loadWorkflows() {
  try {
    const r = await agentApi.list({ limit: 5 })
    const list = r.data?.list || r.data || []
    if (!list.length) { ElMessage.warning('暂无已保存的工作流'); return }
    const wf = list[0]
    pushHistory()
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
    const name = canvasName.value || '工作流-' + new Date().toLocaleString('zh-CN')
    const payload = { name, nodes: nodes.value, edges: edges.value }
    await agentApi.save(payload)
    // Auto-save local version snapshot
    saveVersion(name)
    ElMessage.success('画布已保存（含版本快照）')
  } catch { ElMessage.error('保存失败') }
}

async function runCanvas() {
  if (!nodes.value.length) { ElMessage.warning('画布为空'); return }
  running.value = true
  runLog.value = []; nodeExecStatus.value = {}; nodeExecResult.value = {}; logVisible.value = true
  const startNode = nodes.value.find(n => n.type === 'START')
  const goal = startNode?.config?.prompt || '请执行工作流'

  if (multiRunMode.value) {
    const tools = nodes.value.flatMap(n => n.config?.tools || []).filter(Boolean)
    try {
      multiAbortCtrl.value = new AbortController()
      const llmNodes = nodes.value.filter(n => n.type === 'LLM' && n.config?.prompt)
      const canvasGoal = llmNodes.length
        ? `请按以下要求协作完成：\n${llmNodes.map((n,i) => `${i+1}. ${n.label}: ${n.config.prompt}`).join('\n')}`
        : goal
      await multiAgentApi.xhrStream(
        { goal: canvasGoal, tools, maxRounds: 3 },
        (eventName, data) => { try { handleCanvasMultiEvent(eventName, data) } catch (e) { console.error('[Canvas] SSE 事件处理异常:', eventName, e); runLog.value.push({ tool: '错误', result: `事件 ${eventName} 处理异常: ${e.message}` }) } }
      )
    } catch (e) {
      if (e.name !== 'AbortError') { runLog.value.push({ tool: '错误', result: e.message || '多Agent执行失败' }); ElMessage.error('多Agent执行失败') }
    } finally { running.value = false; multiAbortCtrl.value = null }
  } else {
    try {
      const r = await agentApi.execute({ goal, nodes: nodes.value, edges: edges.value })
      runLog.value = (r.data?.steps || []).map((s, i) => ({ tool: s.action || `步骤${i+1}`, result: s.observation || s.thinking || s.answer || '' }))
      highlightExecution()
      ElMessage.success('执行完成')
    } catch (e) {
      runLog.value = [{ tool: '错误', result: e.message || '执行失败' }]
      ElMessage.error('执行失败')
      highlightExecution()
    } finally { running.value = false }
  }
}

function stopCanvasMulti() {
  if (multiAbortCtrl.value) { multiAbortCtrl.value.abort(); multiAbortCtrl.value = null }
  running.value = false; nodeExecStatus.value = {}; nodeExecResult.value = {}
  ElMessage.info('已停止多Agent执行')
}

function handleCanvasMultiEvent(eventName, data) {
  if (eventName === 'planner-start') {
    const agentNode = nodes.value.find(n => n.type === 'LLM')
    if (agentNode) nodeExecStatus.value[agentNode.id] = 'planner'
    runLog.value.push({ tool: '🧠 Planner', result: `第 ${data.round} 轮规划开始` })
  } else if (eventName === 'planner-plan') { runLog.value.push({ tool: '📋 计划', result: (data.steps || []).join('\n') }) }
  else if (eventName === 'executor-step') {
    const llmNodes = nodes.value.filter(n => n.type === 'LLM')
    const target = llmNodes[data.step - 1] || llmNodes[0]
    if (target) { nodeExecStatus.value[target.id] = 'executor'; nodeExecResult.value[target.id] = `执行中: ${data.goal}` }
    runLog.value.push({ tool: `⚡ 步骤${data.step}`, result: data.goal })
  } else if (eventName === 'executor-result') {
    const llmNodes = nodes.value.filter(n => n.type === 'LLM')
    const target = llmNodes[data.step - 1] || llmNodes[0]
    if (target) { nodeExecStatus.value[target.id] = 'done'; nodeExecResult.value[target.id] = data.observation }
    runLog.value.push({ tool: `📥 步骤${data.step}结果`, result: data.observation })
  } else if (eventName === 'critic-result') {
    runLog.value.push({ tool: '🔍 Critic评估', result: `评分 ${data.score}/10 · ${data.passed ? '通过' : '未通过'} · ${data.feedback}` })
    nodes.value.filter(n => n.type === 'LLM').forEach(n => { nodeExecStatus.value[n.id] = data.passed ? 'done' : 'critic' })
  } else if (eventName === 'final') {
    runLog.value.push({ tool: '🎉 最终答案', result: data.answer })
    nodes.value.filter(n => n.type === 'LLM').forEach(n => { if (nodeExecStatus.value[n.id] !== 'done') nodeExecStatus.value[n.id] = 'done' })
    ElMessage.success('多Agent协作完成')
  } else if (eventName === 'done') { highlightExecution() }
  else if (eventName === 'error') { runLog.value.push({ tool: '⚠️ 错误', result: data.message }); nodes.value.filter(n => n.type === 'LLM').forEach(n => { nodeExecStatus.value[n.id] = 'error' }); ElMessage.error(data.message) }
}

// ── Keyboard shortcuts ─────────────────────────────────────────────
function onKeyDown(e) {
  const tag = document.activeElement?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') {
    if (e.key !== 'f' && e.key !== 'Escape') return
  }
  hideContextMenu()
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedIds.value.size) { deleteSelected(); e.preventDefault() }
  } else if (e.key === 's' && (e.ctrlKey || e.metaKey)) { saveCanvas(); e.preventDefault() }
  else if (e.key === 'z' && (e.ctrlKey || e.metaKey) && !e.shiftKey) { undo(); e.preventDefault() }
  else if ((e.key === 'y' && (e.ctrlKey || e.metaKey)) || (e.key === 'z' && (e.ctrlKey || e.metaKey) && e.shiftKey)) { redo(); e.preventDefault() }
  else if (e.key === 'c' && (e.ctrlKey || e.metaKey)) { copySelected(); e.preventDefault() }
  else if (e.key === 'v' && (e.ctrlKey || e.metaKey)) { pasteNodes(e); e.preventDefault() }
  else if (e.key === 'a' && (e.ctrlKey || e.metaKey)) { nodes.value.forEach(n => selectedIds.value.add(n.id)); selectedIds.value = new Set(selectedIds.value); e.preventDefault() }
  else if (e.key === 'd' && (e.ctrlKey || e.metaKey)) { if (selectedIds.value.size) { ctxDuplicate() }; e.preventDefault() }
  else if (e.key === 'f' && (e.ctrlKey || e.metaKey)) { openSearch(); e.preventDefault() }
  else if (e.key === 'F1') { showShortcuts.value = true; e.preventDefault() }
  else if (e.key === 'Escape') { deselectAll() }
}

onMounted(() => {
  loadModels()
  nextTick(() => {
    if (canvasRef.value) { canvasW.value = canvasRef.value.offsetWidth; canvasH.value = canvasRef.value.offsetHeight }
  })
  const ro = new ResizeObserver(() => {
    if (canvasRef.value) { canvasW.value = canvasRef.value.offsetWidth; canvasH.value = canvasRef.value.offsetHeight }
  })
  if (canvasRef.value) ro.observe(canvasRef.value)
  window.addEventListener('keydown', onKeyDown)
  document.addEventListener('click', hideContextMenu)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style lang="scss" scoped>
.canvas-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #fff; border-bottom: 1px solid #e4e7ed;
}
.canvas-body { display: flex; height: calc(100vh - 105px); }
.node-palette {
  width: 140px; background: #fff; border-right: 1px solid #e4e7ed; padding: 8px; overflow-y: auto;
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
  flex: 1; position: relative; overflow: auto; background: #f8fafc;
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
  &.multi-selected { box-shadow: 0 0 0 2px #409eff, 0 2px 8px rgba(0,0,0,0.12); }
  &.running { animation: pulse 1s infinite; }
  &.search-highlight { box-shadow: 0 0 0 3px #e6a23c, 0 0 0 6px rgba(230,162,60,0.2) !important; animation: search-pulse 0.8s ease-out; }
}
@keyframes search-pulse {
  0% { box-shadow: 0 0 0 6px #e6a23c, 0 0 0 12px rgba(230,162,60,0.3) !important; }
  100% { box-shadow: 0 0 0 3px #e6a23c, 0 0 0 6px rgba(230,162,60,0.2) !important; }
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
.node-url { font-size: 10px; color: #f97316; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }
.port { position: absolute; width: 10px; height: 10px; border-radius: 50%; background: #409eff; border: 2px solid #fff; cursor: crosshair; z-index: 2; }
.port-out { right: -5px; top: 50%; transform: translateY(-50%); }
.port-in { left: -5px; top: 50%; transform: translateY(-50%); background: #67c23a; }
.canvas-empty { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; pointer-events: none; }
.prop-panel { width: 260px; background: #fff; border-left: 1px solid #e4e7ed; padding: 8px; overflow-y: auto; }
.prop-empty { text-align: center; color: #909399; padding: 40px 0; font-size: 13px; }
.prop-form { .el-form-item { margin-bottom: 10px; } }
.log-view { font-family: 'Fira Code', monospace; font-size: 13px; }
.log-step { display: flex; gap: 8px; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.log-i { color: #909399; min-width: 20px; }
.log-tool { font-weight: 600; color: #409eff; min-width: 80px; }
.log-result { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; }

// Zoom / minimap
.zoom-controls {
  position: absolute; bottom: 12px; left: 12px;
  display: flex; align-items: center; gap: 4px;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 4px 6px; z-index: 10;
}
.zoom-pct { font-size: 12px; color: #606266; min-width: 38px; text-align: center; }
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
  border: 2px solid #fff; z-index: 5; animation: badge-pulse 1s infinite;
}
.step-badge.step-error { background: #f56c6c; animation: none; }
@keyframes badge-pulse { 0%,100%{transform:scale(1)} 50%{transform:scale(1.15)} }
.edge-executed { filter: drop-shadow(0 0 3px #67c23a); }
.node-exec-badge {
  position: absolute; top: -10px; right: -10px;
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; z-index: 10; border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,.15); animation: badge-pulse 1.5s infinite;
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
  z-index: 20; pointer-events: none; box-shadow: 0 4px 12px rgba(0,0,0,.2);
  &::after { content:''; position:absolute; top:100%; left:50%; transform:translateX(-50%); border:5px solid transparent; border-top-color:#1f2937; }
}

// V8.0: Selection box
.selection-box {
  position: absolute; border: 1px dashed #409eff; background: rgba(64,158,255,0.08);
  z-index: 100; pointer-events: none;
}

// V8.0: Edge label
.edge-label {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; padding: 2px 6px;
  font-size: 11px; color: #606266; text-align: center; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis; cursor: pointer;
  &:hover { border-color: #409eff; color: #409eff; }
}

// V8.0: Selection badge
.sel-badge {
  display: flex; align-items: center; background: #ecf5ff; border: 1px solid #409eff;
  border-radius: 12px; padding: 2px 8px; font-size: 12px; color: #409eff; font-weight: 500;
}

// V8.0: Search results
.search-result-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer;
  border-radius: 4px; font-size: 13px; color: #303133;
  &:hover { background: #f5f7fa; }
}
.search-result-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

// V8.0: Context menu
.ctx-menu {
  position: absolute; z-index: 1000;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12); padding: 4px; min-width: 160px;
}
.ctx-item {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 12px; border-radius: 4px; font-size: 13px; color: #303133;
  cursor: pointer; transition: background 0.1s;
  &:hover { background: #f5f7fa; }
}
.ctx-hint { margin-left: auto; font-size: 11px; color: #c0c4cc; }
.ctx-divider { height: 1px; background: #e4e7ed; margin: 4px 0; }

// V8.0: Shortcut table
.shortcut-table { display: flex; flex-direction: column; gap: 6px; }
.shortcut-row { display: flex; align-items: center; gap: 12px; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.shortcut-keys { display: flex; gap: 4px; min-width: 120px; }
kbd {
  display: inline-block; padding: 2px 8px; font-size: 12px; font-family: monospace;
  background: #f5f7fa; border: 1px solid #dcdfe6; border-radius: 4px;
  color: #409eff; box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.shortcut-desc { color: #606266; font-size: 13px; }

// V8.0: Version history
.version-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 8px; border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
}
.version-info { display: flex; flex-direction: column; gap: 2px; }
.version-name { font-size: 13px; font-weight: 500; color: #303133; }
.version-time { font-size: 11px; color: #909399; }
.version-count { font-size: 11px; color: #c0c4cc; }
</style>
