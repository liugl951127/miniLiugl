<!-- @file pipeline/Designer.vue - 画布设计器 V7.0 (UX 强化) -->
<template>
  <div class="designer-page">
    <div class="designer-header">
      <h2>画布设计器</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-if="workflows.length" v-model="currentWfId" size="small" placeholder="选择工作流" style="width:180px" @change="loadWf">
          <el-option v-for="wf in workflows" :key="wf.id" :label="wf.name || wf.id" :value="wf.id" />
        </el-select>
        <el-button size="small" :loading="listLoading" @click="loadList"><el-icon><Refresh /></el-icon>加载</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="save"><el-icon><FolderChecked /></el-icon>保存</el-button>
        <el-button type="success" size="small" :loading="running" :disabled="!nodes.length" @click="run"><el-icon><VideoPlay /></el-icon>执行</el-button>
      </div>
    </div>
    <div class="designer-body">
      <!-- 左侧节点面板 -->
      <div class="node-palette">
        <div class="palette-title">节点库</div>
        <div
          v-for="n in nodeTypes" :key="n.type"
          class="palette-node"
          :style="{ borderLeftColor: n.color }"
          draggable="true"
          :class="{ 'is-dragging': dragNode?.type === n.type }"
          @dragstart="onDragStart(n, $event)"
          @dragend="onDragEnd"
        >
          <span class="palette-node-icon">{{ n.icon }}</span>
          <span class="palette-node-label">{{ n.label }}</span>
        </div>
        <div class="palette-tip">拖拽节点到右侧画布</div>
      </div>

      <!-- 画布 -->
      <div
        class="canvas-drop"
        ref="canvasRef"
        :class="{ 'is-drag-over': isDragOver }"
        @dragover.prevent="onDragOver"
        @dragenter.prevent="onDragEnter"
        @dragleave="onDragLeave"
        @drop="onDrop"
        @click="selectedIdx = -1"
      >
        <svg class="edge-svg" :width="canvasW" :height="canvasH">
          <defs><marker id="ds-arr" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#409eff"/></marker></defs>
          <line v-for="(e, i) in edges" :key="i"
            :x1="nodes[e.from]?.x + 60" :y1="nodes[e.from]?.y + 25"
            :x2="nodes[e.to]?.x + 60" :y2="nodes[e.to]?.y + 25"
            stroke="#409eff" stroke-width="1.5" marker-end="url(#ds-arr)"
          />
        </svg>

        <div
          v-for="(n, i) in nodes" :key="n.id"
          class="canvas-node"
          :style="{ left: n.x+'px', top: n.y+'px', borderTopColor: n.color }"
          :class="{ selected: i === selectedIdx, 'is-source': addingEdgeFrom === i }"
          @click.stop="onNodeClick(i, $event)"
        >
          <div class="node-hdr">
            <span>{{ n.icon }} {{ n.label }}</span>
            <el-icon class="del-btn" @click.stop="confirmDelNode(i)"><Close /></el-icon>
          </div>
          <div class="node-body" v-if="n.desc">{{ n.desc }}</div>
        </div>

        <div v-if="!nodes.length" class="canvas-tip">
          <el-icon :size="32" color="#c0d0e0"><Plus /></el-icon>
          <p>从左侧拖拽节点到画布</p>
        </div>

        <div v-if="addingEdgeFrom >= 0" class="edge-hint">
          <el-icon><Aim /></el-icon>
          正在从「{{ nodes[addingEdgeFrom]?.label }}」画连线，请点击目标节点
          <el-button size="small" link @click="addingEdgeFrom = -1">取消</el-button>
        </div>
      </div>

      <!-- 右侧属性 -->
      <div class="prop-panel">
        <div class="palette-title">属性配置</div>
        <div v-if="selectedIdx < 0" class="prop-empty">
          <el-icon :size="32" color="#c0d0e0"><Pointer /></el-icon>
          <p>点击节点配置属性</p>
        </div>
        <div v-else class="prop-form">
          <el-form label-width="70px" size="small" :model="nodes[selectedIdx]">
            <el-form-item label="名称">
              <el-input v-model="nodes[selectedIdx].label" placeholder="节点名称" maxlength="50" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="nodes[selectedIdx].type" style="width:100%">
                <el-option v-for="nt in nodeTypes" :key="nt.type" :label="nt.label" :value="nt.type" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="nodes[selectedIdx].desc" type="textarea" :rows="2" placeholder="节点说明" maxlength="200" />
            </el-form-item>
            <el-form-item label="配置">
              <el-input
                v-model="nodes[selectedIdx].config"
                type="textarea"
                :rows="3"
                placeholder='JSON 配置，如 {"key": "value"}'
                :class="{ 'is-invalid': !isConfigValid(selectedIdx) && nodes[selectedIdx].config?.trim() }"
                @blur="validateConfig(selectedIdx)"
              />
              <div v-if="configError[selectedIdx]" class="config-error-msg">
                <el-icon><WarningFilled /></el-icon> {{ configError[selectedIdx] }}
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="startAddEdge(selectedIdx)">从此节点画连线</el-button>
              <el-button size="small" type="danger" @click="confirmDelNode(selectedIdx)">删除节点</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWorkflows,
  createWorkflow,
  getWorkflow,
  runWorkflow,
  updateWorkflow,
  validateWorkflow
} from '@/api/pipeline'
import { Refresh, FolderChecked, VideoPlay, Close, Plus, Aim, Pointer, WarningFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const canvasRef = ref(null)
const canvasW = ref(800), canvasH = ref(500)
const nodes = ref([])
const edges = ref([])
const selectedIdx = ref(-1)
const workflows = ref([])
const currentWfId = ref(route.params.id || '')
const addingEdgeFrom = ref(-1)
const listLoading = ref(false)
const saving = ref(false)
const running = ref(false)
const isDragOver = ref(false)
const configError = ref({})
let idCnt = 1

const nodeTypes = [
  { type: 'input', label: '输入', icon: '📥', color: '#67c23a', desc: '接收用户输入' },
  { type: 'llm', label: 'LLM', icon: '🤖', color: '#409eff', desc: '大模型节点' },
  { type: 'rag', label: 'RAG', icon: '📚', color: '#e6a23c', desc: '知识检索' },
  { type: 'condition', label: '条件', icon: '🔀', color: '#f56c6c', desc: '分支路由' },
  { type: 'tool', label: '工具', icon: '🔧', color: '#7c3aed', desc: '调用工具' },
  { type: 'output', label: '输出', icon: '📤', color: '#909399', desc: '返回结果' },
]

let dragNode = null
function onDragStart(n, e) {
  dragNode = n
  // 设置拖拽图像 (自定义)
  if (e?.dataTransfer) {
    e.dataTransfer.effectAllowed = 'copy'
    e.dataTransfer.setData('text/plain', n.type)
  }
}
function onDragEnd() {
  dragNode = null
  isDragOver.value = false
}
function onDragOver() {
  if (dragNode) isDragOver.value = true
}
function onDragEnter() {
  if (dragNode) isDragOver.value = true
}
function onDragLeave(e) {
  // 只有真正离开画布时清除状态
  if (!canvasRef.value?.contains(e.relatedTarget)) {
    isDragOver.value = false
  }
}
function onDrop(e) {
  isDragOver.value = false
  if (!dragNode) return
  const rect = canvasRef.value.getBoundingClientRect()
  const newNode = {
    id: 'n' + (idCnt++),
    type: dragNode.type,
    label: dragNode.label,
    icon: dragNode.icon,
    color: dragNode.color,
    desc: dragNode.desc,
    x: Math.max(0, e.clientX - rect.left - 30),
    y: Math.max(0, e.clientY - rect.top - 25),
    config: ''
  }
  nodes.value.push(newNode)
  selectedIdx.value = nodes.value.length - 1
  ElMessage.success(`已添加节点：${newNode.label}`)
  dragNode = null
}

function onNodeClick(i, e) {
  // 如果正在画连线, 完成连线
  if (addingEdgeFrom.value >= 0) {
    if (addingEdgeFrom.value === i) {
      ElMessage.warning('不能连接到自身')
      return
    }
    // 检查重复连线
    const exists = edges.value.some(ed =>
      (ed.from === addingEdgeFrom.value && ed.to === i) ||
      (ed.from === i && ed.to === addingEdgeFrom.value)
    )
    if (exists) {
      ElMessage.warning('该连线已存在')
      addingEdgeFrom.value = -1
      return
    }
    edges.value.push({ from: addingEdgeFrom.value, to: i })
    ElMessage.success('连线已添加')
    addingEdgeFrom.value = -1
    return
  }
  selectedIdx.value = i
}

function confirmDelNode(i) {
  ElMessageBox.confirm(
    `确认删除节点「${nodes.value[i]?.label}」？${edges.value.some(e => e.from === i || e.to === i) ? '关联的连线也会被删除。' : ''}`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).then(() => {
    delNode(i)
  }).catch(() => {})
}

function delNode(i) {
  const removed = nodes.value.splice(i, 1)[0]
  edges.value = edges.value
    .filter(e => e.from !== i && e.to !== i)
    .map(e => ({
      from: e.from > i ? e.from - 1 : e.from,
      to: e.to > i ? e.to - 1 : e.to
    }))
  if (selectedIdx.value === i) selectedIdx.value = -1
  else if (selectedIdx.value > i) selectedIdx.value -= 1
  ElMessage.success('已删除节点：' + (removed?.label || ''))
}

function startAddEdge(idx) {
  if (nodes.value.length < 2) {
    ElMessage.warning('至少需要 2 个节点才能画连线')
    return
  }
  addingEdgeFrom.value = idx
  ElMessage.info('请点击目标节点完成连线')
}

async function loadList() {
  listLoading.value = true
  try {
    const r = await listWorkflows({ limit: 20 })
    workflows.value = r.data?.list || r.data || []
  } catch (e) {
    workflows.value = []
    ElMessage.error('加载工作流列表失败')
  } finally {
    listLoading.value = false
  }
}

async function loadWf(id) {
  const target = id || currentWfId.value
  if (!target) {
    // 新建模式, 清空
    nodes.value = []
    edges.value = []
    return
  }
  try {
    const r = await getWorkflow(target)
    const wf = r.data || {}
    nodes.value = (wf.nodes || []).map(n => {
      const idStr = n.id || 'n' + idCnt++
      const nt = nodeTypes.find(t => t.type === n.type) || nodeTypes[0]
      return {
        id: idStr,
        type: n.type || nt.type,
        label: n.label || n.name || nt.label,
        icon: n.icon || nt.icon,
        color: n.color || nt.color,
        desc: n.desc || n.description || nt.desc,
        x: n.x ?? 40,
        y: n.y ?? 40,
        config: n.config || ''
      }
    })
    // 修正 edges 索引
    edges.value = (wf.edges || []).map(e => ({
      from: typeof e.from === 'string' ? nodes.value.findIndex(n => n.id === e.from) : e.from,
      to: typeof e.to === 'string' ? nodes.value.findIndex(n => n.id === e.to) : e.to
    })).filter(e => e.from >= 0 && e.to >= 0)
    ElMessage.success('已加载：' + (wf.name || target))
  } catch (e) {
    ElMessage.error('加载失败：' + (e?.message || '未知错误'))
  }
}

function isConfigValid(idx) {
  const cfg = nodes.value[idx]?.config?.trim()
  if (!cfg) return true
  try {
    JSON.parse(cfg)
    return true
  } catch {
    return false
  }
}

function validateConfig(idx) {
  const cfg = nodes.value[idx]?.config?.trim()
  if (!cfg) {
    configError.value[idx] = ''
    return true
  }
  try {
    JSON.parse(cfg)
    configError.value[idx] = ''
    return true
  } catch (e) {
    configError.value[idx] = 'JSON 格式错误：' + e.message
    return false
  }
}

function validateAllConfig() {
  let firstError = -1
  for (let i = 0; i < nodes.value.length; i++) {
    const cfg = nodes.value[i]?.config?.trim()
    if (cfg) {
      try { JSON.parse(cfg) }
      catch (e) {
        configError.value[i] = 'JSON 格式错误：' + e.message
        if (firstError < 0) firstError = i
      }
    }
  }
  return firstError
}

async function save() {
  if (!nodes.value.length) {
    ElMessage.warning('画布为空，请先添加节点')
    return
  }

  // 校验 JSON 配置
  const errIdx = validateAllConfig()
  if (errIdx >= 0) {
    ElMessage.error('存在 JSON 配置错误，请修正后再保存')
    selectedIdx.value = errIdx
    return
  }

  // 校验 DAG 是否有环路 (简易检测: 至少需要 1 个 output)
  const hasOutput = nodes.value.some(n => n.type === 'output')
  const hasInput = nodes.value.some(n => n.type === 'input')
  if (!hasInput) {
    try {
      await ElMessageBox.confirm('工作流缺少「输入」节点，是否继续保存？', '提示', {
        type: 'warning',
        confirmButtonText: '继续保存',
        cancelButtonText: '取消'
      })
    } catch { return }
  }
  if (!hasOutput) {
    try {
      await ElMessageBox.confirm('工作流缺少「输出」节点，是否继续保存？', '提示', {
        type: 'warning',
        confirmButtonText: '继续保存',
        cancelButtonText: '取消'
      })
    } catch { return }
  }

  const payload = {
    name: currentWfId.value ? undefined : '工作流_' + Date.now(),
    description: '',
    nodes: nodes.value.map(n => ({
      id: n.id, type: n.type, label: n.label, desc: n.desc,
      x: n.x, y: n.y, config: n.config || ''
    })),
    edges: edges.value
  }
  saving.value = true
  try {
    if (currentWfId.value) {
      await updateWorkflow(currentWfId.value, payload)
      ElMessage.success('保存成功')
    } else {
      const r = await createWorkflow(payload)
      currentWfId.value = r.data?.id || r.data
      ElMessage.success('创建成功，ID: ' + currentWfId.value)
      await loadList()
    }
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || '请检查后端服务'))
  } finally {
    saving.value = false
  }
}

async function run() {
  if (!nodes.value.length) {
    ElMessage.warning('画布为空')
    return
  }
  if (validateAllConfig() >= 0) {
    ElMessage.error('请先修正 JSON 配置错误')
    return
  }
  running.value = true
  try {
    let wfId = currentWfId.value
    if (!wfId) {
      // 先临时保存
      const r = await createWorkflow({
        name: 'temp_' + Date.now(),
        nodes: nodes.value.map(n => ({ ...n })),
        edges: edges.value
      })
      wfId = r.data?.id || r.data
      currentWfId.value = wfId
      ElMessage.info('已自动保存为新工作流')
    }
    const r = await runWorkflow(wfId, {})
    ElMessage.success('执行已启动：' + (r.data?.runId || r.data?.id || ''))
  } catch (e) {
    ElMessage.error('执行失败：' + (e?.message || '请检查工作流配置'))
  } finally {
    running.value = false
  }
}

onMounted(() => {
  nextTick(() => {
    if (canvasRef.value) {
      canvasW.value = canvasRef.value.offsetWidth
      canvasH.value = canvasRef.value.offsetHeight
    }
  })
  loadList()
  if (currentWfId.value) {
    loadWf()
  }
})
</script>

<style lang="scss" scoped>
.designer-page { display: flex; flex-direction: column; height: calc(100vh - 88px); background: #fff; border-radius: 8px; overflow: hidden; }
.designer-header { padding: 10px 16px; border-bottom: 1px solid #e4e7ed; display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; font-size: 15px; } }
.designer-body { flex: 1; display: flex; overflow: hidden; }
.node-palette { width: 160px; border-right: 1px solid #e4e7ed; padding: 8px; flex-shrink: 0; overflow-y: auto; }
.palette-title { font-size: 12px; font-weight: 600; color: var(--el-text-color-secondary); margin-bottom: 8px; }
.palette-node {
  padding: 8px 10px; border: 1px solid #e4e7ed; border-radius: 4px; margin-bottom: 6px;
  cursor: grab; font-size: 13px; border-left-width: 3px; background: var(--el-fill-color-light);
  transition: all 0.15s; user-select: none;
  display: flex; align-items: center; gap: 6px;
  &:hover { background: var(--el-color-primary-light-9); border-color: #b3d8ff; transform: translateX(2px); }
  &:active { cursor: grabbing; }
  &.is-dragging { opacity: 0.4; }
}
.palette-node-icon { font-size: 16px; }
.palette-node-label { flex: 1; }
.palette-tip { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 8px; padding: 4px; text-align: center; }

.canvas-drop {
  flex: 1; position: relative; background: #f8fafc;
  background-image: radial-gradient(circle, #dcdfe6 1px, transparent 1px); background-size: 20px 20px;
  overflow: hidden; transition: background-color 0.2s;
  &.is-drag-over {
    background-color: #e6f0ff;
    background-image: radial-gradient(circle, #409eff 1px, transparent 1px);
  }
}
.edge-svg { position: absolute; inset: 0; pointer-events: none; }
.canvas-tip {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
  color: #bbb; font-size: 14px; pointer-events: none; text-align: center;
  p { margin: 8px 0 0; }
}
.canvas-node {
  position: absolute; width: 120px; background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  cursor: move; z-index: 1; box-shadow: 0 2px 6px rgba(0,0,0,0.06); transition: box-shadow 0.15s;
  &.selected { box-shadow: 0 0 0 2px #409eff; }
  &.is-source { box-shadow: 0 0 0 2px #67c23a; animation: pulse 1.5s infinite; }
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 2px #67c23a; }
  50% { box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.4); }
}
.node-hdr { padding: 6px 8px; background: var(--el-fill-color-light); border-radius: 6px 6px 0 0; font-size: 12px; display: flex; justify-content: space-between; align-items: center; }
.del-btn { opacity: 0; cursor: pointer; font-size: 12px; color: var(--el-color-danger); transition: opacity 0.15s; }
.canvas-node:hover .del-btn { opacity: 1; }
.node-body { padding: 4px 8px 6px; font-size: 11px; color: var(--el-text-color-secondary); }

.edge-hint {
  position: absolute; top: 12px; left: 50%; transform: translateX(-50%);
  background: #67c23a; color: #fff; padding: 6px 14px; border-radius: 16px;
  font-size: 13px; display: flex; align-items: center; gap: 6px;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  z-index: 10;
}

.prop-panel { width: 240px; border-left: 1px solid #e4e7ed; padding: 8px; flex-shrink: 0; overflow-y: auto; }
.prop-empty { text-align: center; color: var(--el-text-color-secondary); padding: 30px 0; font-size: 13px; p { margin: 8px 0 0; } }
.prop-form { .el-form-item { margin-bottom: 8px; } }
.config-error-msg {
  font-size: 11px; color: var(--el-color-danger); margin-top: 4px;
  display: flex; align-items: center; gap: 4px;
}
:deep(.is-invalid textarea) { border-color: var(--el-color-danger) !important; }
</style>
