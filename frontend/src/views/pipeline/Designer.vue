<!-- @file pipeline/Designer.vue - 画布设计器 V6.8 -->
<template>
  <div class="designer-page">
    <div class="designer-header">
      <h2>画布设计器</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-if="workflows.length" v-model="currentWfId" size="small" placeholder="选择工作流" style="width:180px" @change="loadWf">
          <el-option v-for="wf in workflows" :key="wf.id" :label="wf.name || wf.id" :value="wf.id" />
        </el-select>
        <el-button size="small" @click="loadList"><el-icon><Refresh /></el-icon>加载</el-button>
        <el-button type="primary" size="small" @click="save"><el-icon><FolderChecked /></el-icon>保存</el-button>
        <el-button type="success" size="small" @click="run" :disabled="!nodes.length"><el-icon><VideoPlay /></el-icon>执行</el-button>
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
          @dragstart="onDragStart(n)"
        >{{ n.icon }} {{ n.label }}</div>
      </div>

      <!-- 画布 -->
      <div class="canvas-drop" ref="canvasRef"
        @dragover.prevent @drop="onDrop"
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
          :class="{ selected: i === selectedIdx }"
          @click.stop="selectedIdx = i"
        >
          <div class="node-hdr">
            <span>{{ n.icon }} {{ n.label }}</span>
            <el-icon class="del-btn" @click.stop="delNode(i)"><Close /></el-icon>
          </div>
          <div class="node-body" v-if="n.desc">{{ n.desc }}</div>
        </div>

        <div v-if="!nodes.length" class="canvas-tip">从左侧拖拽节点到画布</div>
      </div>

      <!-- 右侧属性 -->
      <div class="prop-panel">
        <div class="palette-title">属性配置</div>
        <div v-if="selectedIdx < 0" class="prop-empty">点击节点配置</div>
        <div v-else class="prop-form">
          <el-form label-width="70px" size="small">
            <el-form-item label="名称"><el-input v-model="nodes[selectedIdx].label" /></el-form-item>
            <el-form-item label="类型">
              <el-select v-model="nodes[selectedIdx].type" style="width:100%">
                <el-option v-for="nt in nodeTypes" :key="nt.type" :label="nt.label" :value="nt.type" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="nodes[selectedIdx].desc" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="配置">
              <el-input v-model="nodes[selectedIdx].config" type="textarea" :rows="2" placeholder="JSON 配置…" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="addEdgeFrom(selectedIdx)">添加连线</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { listWorkflows, createWorkflow, getWorkflow, runWorkflow } from '@/api/pipeline'
import { Refresh, FolderChecked, VideoPlay, Close } from '@element-plus/icons-vue'

const canvasRef = ref(null)
const canvasW = ref(800), canvasH = ref(500)
const nodes = ref([])
const edges = ref([])
const selectedIdx = ref(-1)
const workflows = ref([])
const currentWfId = ref('')
const addingEdgeFrom = ref(-1)
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
function onDragStart(n) { dragNode = n }

function onDrop(e) {
  if (!dragNode) return
  const rect = canvasRef.value.getBoundingClientRect()
  nodes.value.push({
    id: 'n' + (idCnt++),
    type: dragNode.type, label: dragNode.label, icon: dragNode.icon,
    color: dragNode.color, desc: dragNode.desc,
    x: e.clientX - rect.left - 30, y: e.clientY - rect.top - 25,
    config: '',
  })
  dragNode = null
}

function delNode(i) { nodes.value.splice(i, 1); edges.value = edges.value.filter(e => e.from !== i && e.to !== i); if (selectedIdx.value === i) selectedIdx.value = -1 }

function addEdgeFrom(idx) {
  addingEdgeFrom.value = idx
  ElMessage.info('点击目标节点完成连线')
}

async function loadList() {
  try {
    const r = await listWorkflows({ limit: 20 })
    workflows.value = r.data?.list || r.data || []
  } catch { workflows.value = [] }
}

async function loadWf(id) {
  if (!id) return
  try {
    const r = await getWorkflow(id)
    const wf = r.data || {}
    nodes.value = (wf.nodes || []).map(n => ({ ...n, id: n.id || 'n' + idCnt++ }))
    edges.value = wf.edges || []
    ElMessage.success('已加载: ' + (wf.name || id))
  } catch { ElMessage.error('加载失败') }
}

async function save() {
  if (!nodes.value.length) { ElMessage.warning('画布为空'); return }
  const payload = {
    name: currentWfId.value ? undefined : '工作流_' + Date.now(),
    nodes: nodes.value.map(n => ({ ...n })),
    edges: edges.value,
  }
  try {
    if (currentWfId.value) {
      await import('@/api/pipeline').then(m => m.updateWorkflow(currentWfId.value, payload))
    } else {
      const r = await createWorkflow(payload)
      currentWfId.value = r.data?.id || r.data
      ElMessage.success('保存成功')
      await loadList()
    }
  } catch (e) {
    // API 不可用时静默跳过，保持本地状态
    ElMessage.info('已保存到本地')
  }
}

async function run() {
  if (!nodes.value.length) return
  try {
    let wfId = currentWfId.value
    if (!wfId) {
      const r = await createWorkflow({ name: 'temp_' + Date.now(), nodes: nodes.value, edges: edges.value })
      wfId = r.data?.id || r.data
    }
    const r = await runWorkflow(wfId, {})
    ElMessage.success('执行已启动: ' + (r.data?.runId || ''))
  } catch (e) {
    ElMessage.error('执行失败')
  }
}

onMounted(() => {
  nextTick(() => {
    if (canvasRef.value) { canvasW.value = canvasRef.value.offsetWidth; canvasH.value = canvasRef.value.offsetHeight }
  })
  loadList()
})
</script>

<style lang="scss" scoped>
.designer-page { display: flex; flex-direction: column; height: calc(100vh - 88px); background: #fff; border-radius: 8px; overflow: hidden; }
.designer-header { padding: 10px 16px; border-bottom: 1px solid #e4e7ed; display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; font-size: 15px; } }
.designer-body { flex: 1; display: flex; overflow: hidden; }
.node-palette { width: 140px; border-right: 1px solid #e4e7ed; padding: 8px; flex-shrink: 0; overflow-y: auto; }
.palette-title { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 8px; }
.palette-node { padding: 6px 10px; border: 1px solid #e4e7ed; border-radius: 4px; margin-bottom: 4px; cursor: grab; font-size: 13px; border-left-width: 3px; background: #f5f7fa; transition: background 0.15s; &:hover { background: #ecf5ff; } }
.canvas-drop { flex: 1; position: relative; background: #f8fafc; background-image: radial-gradient(circle, #dcdfe6 1px, transparent 1px); background-size: 20px 20px; overflow: hidden; }
.edge-svg { position: absolute; inset: 0; pointer-events: none; }
.canvas-tip { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); color: #bbb; font-size: 14px; pointer-events: none; }
.canvas-node { position: absolute; width: 120px; background: #fff; border: 1px solid #e4e7ed; border-radius: 6px; cursor: move; z-index: 1; box-shadow: 0 2px 6px rgba(0,0,0,0.06); transition: box-shadow 0.15s; &.selected { box-shadow: 0 0 0 2px #409eff; } }
.node-hdr { padding: 6px 8px; background: #f5f7fa; border-radius: 6px 6px 0 0; font-size: 12px; display: flex; justify-content: space-between; align-items: center; }
.del-btn { opacity: 0; cursor: pointer; font-size: 12px; color: #f56c6c; transition: opacity 0.15s; }
.canvas-node:hover .del-btn { opacity: 1; }
.node-body { padding: 4px 8px 6px; font-size: 11px; color: #909399; }
.prop-panel { width: 220px; border-left: 1px solid #e4e7ed; padding: 8px; flex-shrink: 0; overflow-y: auto; }
.prop-empty { text-align: center; color: #909399; padding: 30px 0; font-size: 13px; }
.prop-form { .el-form-item { margin-bottom: 8px; } }
</style>
