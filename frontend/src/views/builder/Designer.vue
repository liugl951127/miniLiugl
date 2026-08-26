<!--
  @file builder/Designer.vue - 智能体群设计器 (V1.0)
  路由: /builder/designer
  拖拽编辑智能体角色 + 工具/提示词配置 + 拓扑连接
-->
<template>
  <div class="designer-page">
    <el-row :gutter="16">
      <!-- 左: 智能体池 -->
      <el-col :span="6">
        <el-card shadow="never" class="palette-card">
          <template #header>
            <div class="palette-head">
              <span>🧩 智能体库</span>
              <el-button size="small" type="primary" :icon="Plus" @click="showAddAgent = true">添加</el-button>
            </div>
          </template>
          <div class="palette-search">
            <el-input v-model="paletteSearch" size="small" placeholder="搜索..." :prefix-icon="Search" />
          </div>
          <div class="palette-list">
            <div v-for="a in filteredAgents" :key="a.name"
              class="palette-item" draggable="true"
              @dragstart="onDragStart($event, a)">
              <div class="pi-avatar" :style="{ background: a.color }">{{ a.emoji }}</div>
              <div class="pi-body">
                <div class="pi-name">{{ a.name }}</div>
                <div class="pi-role">{{ a.role }}</div>
              </div>
              <el-icon class="pi-drag"><Rank /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中: 画布 -->
      <el-col :span="12">
        <el-card shadow="never" class="canvas-card">
          <template #header>
            <div class="canvas-head">
              <span>🎨 团队拓扑</span>
              <div class="canvas-tools">
                <el-button-group>
                  <el-button size="small" :icon="ZoomIn" @click="zoom = Math.min(1.5, zoom + 0.1)">放大</el-button>
                  <el-button size="small" :icon="ZoomOut" @click="zoom = Math.max(0.7, zoom - 0.1)">缩小</el-button>
                </el-button-group>
                <el-button size="small" :icon="RefreshLeft" @click="autoLayout">自动布局</el-button>
              </div>
            </div>
          </template>

          <div class="canvas-area" ref="canvasRef"
            @dragover.prevent @drop="onDrop"
            @mousemove="onMouseMove" @mouseup="onMouseUp">
            <!-- SVG 连线层 -->
            <svg class="connection-svg" :width="canvasW" :height="canvasH">
              <defs>
                <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
                  <path d="M0,0 L0,6 L8,3 z" fill="#6366f1" />
                </marker>
              </defs>
              <path v-for="(e, i) in connections" :key="i"
                :d="edgePath(e)" stroke="#6366f1" stroke-width="2" fill="none"
                stroke-dasharray="0" marker-end="url(#arrow)"
                style="cursor:pointer;opacity:0.6"
              />
            </svg>
            <!-- 节点层 -->
            <div v-for="(node, i) in nodes" :key="node.name"
              class="canvas-node"
              :class="{ selected: selectedNode === node.name, dragging: dragging === i }"
              :style="{ left: node.x + 'px', top: node.y + 'px', transform: `scale(${zoom})` }"
              @mousedown.stop="onNodeMouseDown($event, i)"
              @click="selectedNode = node.name"
            >
              <div class="cn-avatar" :style="{ background: node.color }">{{ node.emoji }}</div>
              <div class="cn-body">
                <div class="cn-name">{{ node.name }}</div>
                <div class="cn-role">{{ node.role }}</div>
                <div class="cn-model">{{ node.model }}</div>
              </div>
              <button class="cn-remove" @click.stop="removeNode(i)">×</button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右: 属性面板 -->
      <el-col :span="6">
        <el-card shadow="never" class="props-card">
          <template #header>
            <span>⚙️ 属性配置</span>
          </template>
          <div v-if="!selectedNode" class="props-empty">
            <div class="empty-icon">👆</div>
            <p>点击节点编辑属性</p>
          </div>
          <div v-else class="props-form">
            <el-form label-width="60px" size="default" label-position="top">
              <el-form-item label="名称">
                <el-input v-model="currentNode.name" />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="currentNode.role" style="width:100%">
                  <el-option v-for="r in agentRoles" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="模型">
                <el-select v-model="currentNode.model" style="width:100%">
                  <el-option label="Qwen2.5-7B" value="Qwen2.5-7B" />
                  <el-option label="Qwen2.5-0.5B" value="Qwen2.5-0.5B" />
                  <el-option label="本地 ONNX" value="local-onnx" />
                </el-select>
              </el-form-item>
              <el-form-item label="工具">
                <el-checkbox-group v-model="currentNode.tools">
                  <el-checkbox value="search">课程搜索</el-checkbox>
                  <el-checkbox value="order">订单查询</el-checkbox>
                  <el-checkbox value="ticket">工单系统</el-checkbox>
                  <el-checkbox value="payment">支付接口</el-checkbox>
                  <el-checkbox value="image">图像理解</el-checkbox>
                  <el-checkbox value="kb">知识库</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="系统提示词">
                <el-input v-model="currentNode.prompt" type="textarea" :rows="6" />
              </el-form-item>
              <el-form-item label="连接">
                <el-select v-model="currentNode.connections" multiple style="width:100%">
                  <el-option v-for="n in otherNodes" :key="n.name" :label="n.name" :value="n.name" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部操作栏 -->
    <div class="footer-bar">
      <el-button size="large" round :icon="RefreshLeft" @click="$router.push('/builder/analysis')">返回</el-button>
      <div class="footer-info">
        <span>👥 {{ nodes.length }} 智能体</span>
        <span>🔗 {{ connections.length }} 连接</span>
        <span>🧬 {{ totalTools }} 工具</span>
      </div>
      <el-button size="large" round type="primary" :icon="ArrowRight" @click="$router.push('/builder/deploy')">
        下一步: 部署 →
      </el-button>
    </div>

    <!-- 添加智能体弹窗 -->
    <el-dialog v-model="showAddAgent" title="添加智能体" width="500px">
      <el-form :model="newAgent" label-width="80px">
        <el-form-item label="名称"><el-input v-model="newAgent.name" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="newAgent.role" style="width:100%">
            <el-option v-for="r in agentRoles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Emoji"><el-input v-model="newAgent.emoji" placeholder="🤖" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAgent = false">取消</el-button>
        <el-button type="primary" @click="addNewAgent">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Rank, ZoomIn, ZoomOut, RefreshLeft, ArrowRight } from '@element-plus/icons-vue'
import { useDict } from '@/api/dict'

const router = useRouter()
const canvasRef = ref(null)
const canvasW = ref(800)
const canvasH = ref(500)
const zoom = ref(1)
const selectedNode = ref(null)

// V8.0.3.2: 字典下拉 (useDict composable, API 失败用 fallback)
const { data: agentRoles } = useDict('agentRoles')
const dragging = ref(null)
const dragOffset = reactive({ x: 0, y: 0 })
const showAddAgent = ref(false)

const paletteSearch = ref('')
const paletteAgents = ref([
  { name: '小课', role: '课程顾问', emoji: '📚', color: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { name: '小助', role: '退费专员', emoji: '💰', color: 'linear-gradient(135deg, #f59e0b, #ef4444)' },
  { name: '小导', role: '学习规划师', emoji: '🎯', color: 'linear-gradient(135deg, #10b981, #06b6d4)' },
  { name: '小审', role: '质检员', emoji: '🔍', color: 'linear-gradient(135deg, #ec4899, #f43f5e)' },
  { name: '小经', role: '经纪人', emoji: '👔', color: 'linear-gradient(135deg, #8b5cf6, #ec4899)' },
  { name: '小医', role: '医疗顾问', emoji: '⚕️', color: 'linear-gradient(135deg, #06b6d4, #10b981)' }
])
const filteredAgents = computed(() => {
  const k = paletteSearch.value
  if (!k) return paletteAgents.value
  return paletteAgents.value.filter(a => a.name.includes(k) || a.role.includes(k))
})

const nodes = ref([
  { name: '小课', role: '课程顾问', emoji: '📚', color: 'linear-gradient(135deg, #6366f1, #8b5cf6)', x: 80, y: 80, model: 'Qwen2.5-7B', tools: ['search', 'kb'], prompt: '你是专业课程顾问, 帮助用户选择合适的课程...', connections: ['小审'] },
  { name: '小助', role: '退费专员', emoji: '💰', color: 'linear-gradient(135deg, #f59e0b, #ef4444)', x: 80, y: 240, model: 'Qwen2.5-7B', tools: ['order', 'ticket', 'payment'], prompt: '你是退费专员, 帮助用户处理退费流程...', connections: ['小审'] },
  { name: '小审', role: '质检员', emoji: '🔍', color: 'linear-gradient(135deg, #ec4899, #f43f5e)', x: 380, y: 160, model: 'Qwen2.5-0.5B', tools: [], prompt: '你是质检员, 监控对话质量...', connections: [] }
])

const connections = computed(() => {
  const result = []
  nodes.value.forEach(n => {
    (n.connections || []).forEach(target => {
      const targetNode = nodes.value.find(x => x.name === target)
      if (targetNode) result.push({ from: n, to: targetNode })
    })
  })
  return result
})

const currentNode = computed({
  get: () => nodes.value.find(n => n.name === selectedNode.value) || { name: '', role: '', tools: [], prompt: '', connections: [], model: '' },
  set: () => {}
})
const otherNodes = computed(() => nodes.value.filter(n => n.name !== selectedNode.value))
const totalTools = computed(() => {
  const all = new Set()
  nodes.value.forEach(n => (n.tools || []).forEach(t => all.add(t)))
  return all.size
})

function edgePath(e) {
  const x1 = e.from.x + 200
  const y1 = e.from.y + 60
  const x2 = e.to.x
  const y2 = e.to.y + 60
  const cx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${cx} ${y1}, ${cx} ${y2}, ${x2} ${y2}`
}

function onDragStart(e, agent) {
  e.dataTransfer.setData('text/plain', agent.name)
  e.dataTransfer.effectAllowed = 'copy'
}
function onDrop(e) {
  const name = e.dataTransfer.getData('text/plain')
  const agent = paletteAgents.value.find(a => a.name === name)
  if (!agent) return
  const rect = canvasRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left - 100
  const y = e.clientY - rect.top - 40
  nodes.value.push({
    ...agent, x: Math.max(0, x), y: Math.max(0, y),
    model: 'Qwen2.5-7B', tools: [], prompt: `你是${agent.role}, ...`, connections: []
  })
  ElMessage.success(`已添加: ${name}`)
}

function onNodeMouseDown(e, i) {
  dragging.value = i
  const rect = e.target.getBoundingClientRect()
  dragOffset.x = e.clientX - rect.left
  dragOffset.y = e.clientY - rect.top
}
function onMouseMove(e) {
  if (dragging.value === null || !canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  nodes.value[dragging.value].x = e.clientX - rect.left - dragOffset.x
  nodes.value[dragging.value].y = e.clientY - rect.top - dragOffset.y
}
function onMouseUp() { dragging.value = null }

function removeNode(i) {
  ElMessage.success(`已移除: ${nodes.value[i].name}`)
  nodes.value.splice(i, 1)
  selectedNode.value = null
}

function autoLayout() {
  // 自动布局 - 简单横向排列
  nodes.value.forEach((n, i) => {
    n.x = 80 + (i % 3) * 200
    n.y = 80 + Math.floor(i / 3) * 200
  })
  ElMessage.success('已自动布局')
}

const newAgent = reactive({ name: '', role: '客服', emoji: '🤖' })
function addNewAgent() {
  if (!newAgent.name) return ElMessage.warning('请输入名称')
  nodes.value.push({
    name: newAgent.name, role: newAgent.role, emoji: newAgent.emoji,
    color: 'linear-gradient(135deg, #6366f1, #ec4899)',
    x: 200, y: 200, model: 'Qwen2.5-7B', tools: [], prompt: `你是${newAgent.role}, ...`, connections: []
  })
  paletteAgents.value.push({ name: newAgent.name, role: newAgent.role, emoji: newAgent.emoji, color: 'linear-gradient(135deg, #6366f1, #ec4899)' })
  ElMessage.success('已添加')
  Object.assign(newAgent, { name: '', role: '客服', emoji: '🤖' })
  showAddAgent.value = false
}
</script>

<style scoped>
.designer-page { max-width: 1400px; margin: 0 auto; }

.palette-card, .canvas-card, .props-card { border-radius: 14px; height: 600px; }
.palette-head { display: flex; justify-content: space-between; align-items: center; }
.palette-search { margin-bottom: 12px; }
.palette-list { display: flex; flex-direction: column; gap: 6px; max-height: 480px; overflow-y: auto; }
.palette-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  background: #fafbfc; border-radius: 10px; cursor: grab;
  border: 1px solid #f1f5f9; transition: all 0.15s;
}
.palette-item:hover { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.06); transform: translateX(2px); }
.palette-item:active { cursor: grabbing; }
.pi-avatar {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0;
}
.pi-body { flex: 1; }
.pi-name { font-weight: 600; font-size: 13px; color: #1e293b; }
.pi-role { font-size: 11px; color: #64748b; }
.pi-drag { color: #cbd5e1; }

.canvas-head { display: flex; justify-content: space-between; align-items: center; }
.canvas-tools { display: flex; gap: 8px; }
.canvas-area {
  position: relative; width: 100%; height: 500px;
  background: #fafbfc; border-radius: 10px; overflow: auto;
  background-image: radial-gradient(circle, #cbd5e1 1px, transparent 1px);
  background-size: 16px 16px;
}
.connection-svg { position: absolute; inset: 0; pointer-events: none; }
.canvas-node {
  position: absolute; display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; background: white; border-radius: 12px;
  border: 2px solid #e2e8f0; cursor: move;
  width: 180px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: border-color 0.2s, transform 0.1s;
  transform-origin: top left;
}
.canvas-node:hover { border-color: #6366f1; }
.canvas-node.selected { border-color: #6366f1; box-shadow: 0 4px 16px rgba(99, 102, 241, 0.2); }
.canvas-node.dragging { box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
.cn-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.cn-body { flex: 1; min-width: 0; }
.cn-name { font-weight: 600; font-size: 13px; color: #1e293b; }
.cn-role { font-size: 11px; color: #6366f1; }
.cn-model { font-size: 10px; color: #94a3b8; margin-top: 2px; }
.cn-remove {
  position: absolute; top: -8px; right: -8px;
  width: 20px; height: 20px; border-radius: 50%;
  background: #ef4444; color: white; border: none;
  cursor: pointer; font-size: 14px; line-height: 1;
  display: none;
}
.canvas-node:hover .cn-remove { display: block; }

.props-empty { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 48px; opacity: 0.3; }
.props-empty p { color: #94a3b8; font-size: 13px; }
.props-form { max-height: 520px; overflow-y: auto; padding-right: 4px; }

.footer-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 16px; padding: 12px 20px; background: white;
  border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.footer-info { display: flex; gap: 16px; font-size: 13px; color: #475569; }
</style>
