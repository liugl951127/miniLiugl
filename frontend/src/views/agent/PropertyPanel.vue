<!--
  @file views/agent/PropertyPanel.vue (V6.8.13+ 4 Tab 属性面板 - 企业级)
  @version V6.8.13 (基本 / 输入 / 输出 / 高级)
-->
<template>
  <div class="property-panel" v-loading="loading">
    <div v-if="!selectedNode && !selectedEdge" class="empty-panel">
      <div class="empty-icon">🎨</div>
      <h3>未选中节点或连线</h3>
      <p>从画布选中节点/连线以编辑属性</p>
    </div>

    <div v-else>
      <div class="panel-header">
        <h3>
          <span class="header-icon">
            {{ selectedNode ? getIcon(selectedNode.type) : (selectedEdge?.type === 'control' ? '🔀' : '🔗') }}
          </span>
          {{ selectedNode ? selectedNode.name : '连线属性' }}
        </h3>
        <div class="panel-header-actions">
          <el-tag size="small" effect="plain" type="info">
            {{ selectedNode ? getName(selectedNode.type) : (selectedEdge?.type === 'control' ? '控制流' : '数据流') }}
          </el-tag>
          <el-tooltip content="删除当前选中" placement="top">
            <el-button type="danger" size="small" text @click="handleDelete">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="panel-tabs">
        <!-- Tab 1: 基本 -->
        <el-tab-pane label="基本" name="basic">
          <el-form v-if="selectedNode" label-position="top" size="small">
            <el-form-item label="名称">
              <el-input
                v-model="selectedNode.name"
                placeholder="节点名称"
                maxlength="50"
                show-word-limit
                @input="emitUpdate"
              />
            </el-form-item>
            <el-form-item label="类型">
              <el-tag :color="getColor(selectedNode.type)" effect="dark">
                {{ getName(selectedNode.type) }}
              </el-tag>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="selectedNode.status" placeholder="选择状态" @change="emitUpdate">
                <el-option label="待运行" value="pending" />
                <el-option label="运行中" value="running" />
                <el-option label="成功" value="success" />
                <el-option label="失败" value="failed" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input
                v-model="(selectedNode.config || {}).desc"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="简要描述此节点的作用"
                @input="emitUpdate"
              />
            </el-form-item>
          </el-form>

          <el-form v-else-if="selectedEdge" label-position="top" size="small">
            <el-form-item label="连线类型">
              <el-radio-group v-model="selectedEdge.type" @change="emitUpdate">
                <el-radio-button label="data">数据流</el-radio-button>
                <el-radio-button label="control">控制流</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="连线标签">
              <el-input v-model="selectedEdge.label" placeholder="选填" maxlength="50" @input="emitUpdate" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: 输入 -->
        <el-tab-pane label="输入" name="input">
          <el-form v-if="selectedNode" label-position="top" size="small">
            <template v-if="selectedNode.type === 'llm'">
              <el-form-item label="模型">
                <el-select v-model="selectedNode.config.model" placeholder="选择模型" @change="emitUpdate">
                  <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
                  <el-option label="GPT-4" value="gpt-4" />
                  <el-option label="Claude 3 Haiku" value="claude-3-haiku" />
                  <el-option label="DeepSeek Chat" value="deepseek-chat" />
                </el-select>
              </el-form-item>
              <el-form-item label="Prompt 模板">
                <el-input
                  v-model="selectedNode.config.prompt"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：根据以下输入回答问题：\n$input"
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="Temperature">
                <el-slider
                  v-model="selectedNode.config.temperature"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  show-input
                  @change="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="Max Tokens">
                <el-input-number
                  v-model="selectedNode.config.maxTokens"
                  :min="100"
                  :max="32000"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'rag'">
              <el-form-item label="知识库">
                <el-select v-model="selectedNode.config.kbId" placeholder="选择知识库" @change="emitUpdate">
                  <el-option :label="`KB ${i}`" :value="i" v-for="i in 5" :key="i" />
                </el-select>
              </el-form-item>
              <el-form-item label="Top K">
                <el-input-number
                  v-model="selectedNode.config.topK"
                  :min="1"
                  :max="20"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="相似度阈值">
                <el-slider
                  v-model="selectedNode.config.threshold"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  show-input
                  @change="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'tool'">
              <el-form-item label="工具">
                <el-select v-model="selectedNode.config.toolId" placeholder="选择工具" @change="emitUpdate">
                  <el-option label="时间查询" value="time" />
                  <el-option label="天气查询" value="weather" />
                  <el-option label="计算器" value="calculator" />
                  <el-option label="搜索引擎" value="search" />
                </el-select>
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'code'">
              <el-form-item label="语言">
                <el-radio-group v-model="selectedNode.config.language" @change="emitUpdate">
                  <el-radio-button label="python">Python</el-radio-button>
                  <el-radio-button label="javascript">JS</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="代码">
                <el-input
                  v-model="selectedNode.config.code"
                  type="textarea"
                  :rows="6"
                  placeholder="# 输入: $input\n# 输出: $output"
                  @input="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'http'">
              <el-form-item label="URL">
                <el-input
                  v-model="selectedNode.config.url"
                  placeholder="https://api.example.com"
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="方法">
                <el-radio-group v-model="selectedNode.config.method" @change="emitUpdate">
                  <el-radio-button label="GET">GET</el-radio-button>
                  <el-radio-button label="POST">POST</el-radio-button>
                  <el-radio-button label="PUT">PUT</el-radio-button>
                  <el-radio-button label="DELETE">DELETE</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="请求体">
                <el-input
                  v-model="selectedNode.config.body"
                  type="textarea"
                  :rows="3"
                  @input="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'condition'">
              <el-form-item label="条件表达式">
                <el-input
                  v-model="selectedNode.config.expression"
                  type="textarea"
                  :rows="3"
                  placeholder="$input.length > 100"
                  @input="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'memory'">
              <el-form-item label="记忆类型">
                <el-radio-group v-model="selectedNode.config.type" @change="emitUpdate">
                  <el-radio-button label="short">短期</el-radio-button>
                  <el-radio-button label="long">长期</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="Key">
                <el-input v-model="selectedNode.config.key" placeholder="记忆键名" @input="emitUpdate" />
              </el-form-item>
              <el-form-item label="TTL (秒)">
                <el-input-number
                  v-model="selectedNode.config.ttl"
                  :min="0"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'loop'">
              <el-form-item label="循环类型">
                <el-radio-group v-model="selectedNode.config.loopType" @change="emitUpdate">
                  <el-radio-button label="for">for</el-radio-button>
                  <el-radio-button label="while">while</el-radio-button>
                  <el-radio-button label="doWhile">do-while</el-radio-button>
                  <el-radio-button label="forEach">forEach</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="迭代器">
                <el-input
                  v-model="selectedNode.config.iterator"
                  placeholder="$input.items"
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item
                v-if="selectedNode.config.loopType === 'while' || selectedNode.config.loopType === 'doWhile'"
                label="条件"
              >
                <el-input
                  v-model="selectedNode.config.condition"
                  type="textarea"
                  :rows="2"
                  placeholder="$counter < 10"
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="最大迭代次数">
                <el-input-number
                  v-model="selectedNode.config.maxIterations"
                  :min="1"
                  :max="10000"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'parallel'">
              <el-form-item label="并行模式">
                <el-radio-group v-model="selectedNode.config.parallelMode" @change="emitUpdate">
                  <el-radio-button label="all">all</el-radio-button>
                  <el-radio-button label="any">any</el-radio-button>
                  <el-radio-button label="race">race</el-radio-button>
                  <el-radio-button label="batch">batch</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="并发度">
                <el-input-number
                  v-model="selectedNode.config.concurrency"
                  :min="1"
                  :max="100"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="超时 (秒)">
                <el-input-number
                  v-model="selectedNode.config.timeout"
                  :min="1"
                  :max="3600"
                  style="width:100%"
                  @change="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="失败策略">
                <el-radio-group v-model="selectedNode.config.failStrategy" @change="emitUpdate">
                  <el-radio-button label="fail">失败停止</el-radio-button>
                  <el-radio-button label="ignore">忽略</el-radio-button>
                  <el-radio-button label="retry">重试</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'subflow'">
              <el-form-item label="子流 ID">
                <el-input
                  v-model="selectedNode.config.subflowId"
                  placeholder="subflow_xxx"
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="输入映射">
                <el-input
                  v-model="selectedNode.config.inputMap"
                  type="textarea"
                  :rows="2"
                  placeholder='{"key": "$value"}'
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="输出映射">
                <el-input
                  v-model="selectedNode.config.outputMap"
                  type="textarea"
                  :rows="2"
                  placeholder='{"result": "$value"}'
                  @input="emitUpdate"
                />
              </el-form-item>
              <el-form-item label="同步/异步">
                <el-radio-group v-model="selectedNode.config.async" @change="emitUpdate">
                  <el-radio-button :label="false">同步</el-radio-button>
                  <el-radio-button :label="true">异步</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </template>
            <el-empty
              v-else
              description="该节点类型暂无可配置输入参数"
              :image-size="60"
            />
          </el-form>
        </el-tab-pane>

        <!-- Tab 3: 输出 -->
        <el-tab-pane label="输出" name="output">
          <el-form v-if="selectedNode" label-position="top" size="small">
            <el-form-item label="输出变量名">
              <el-input :model-value="`\${${selectedNode.name || 'node'}.output}`" readonly />
            </el-form-item>
            <el-form-item label="下游节点">
              <div class="downstream-list">
                <el-tag
                  v-for="eid in getDownstream(selectedNode.id)"
                  :key="eid"
                  class="downstream-tag clickable"
                  @click="onDownstreamClick(eid)"
                >{{ getNodeName(eid) }}</el-tag>
                <span v-if="!getDownstream(selectedNode.id).length" class="no-downstream">无下游</span>
              </div>
            </el-form-item>
            <el-form-item label="输出 Schema (JSON)">
              <el-input
                :model-value="getOutputSchema(selectedNode)"
                type="textarea"
                :rows="4"
                readonly
              />
            </el-form-item>
          </el-form>
          <el-empty
            v-else
            description="连线无输出配置"
            :image-size="60"
          />
        </el-tab-pane>

        <!-- Tab 4: 高级 -->
        <el-tab-pane label="高级" name="advanced">
          <el-form v-if="selectedNode" label-position="top" size="small">
            <el-form-item label="超时 (秒)">
              <el-input-number
                :model-value="(selectedNode.config || {}).timeout || 60"
                :min="1"
                :max="3600"
                style="width:100%"
                @change="v => { selectedNode.config.timeout = v; emitUpdate(); }"
              />
            </el-form-item>
            <el-form-item label="重试次数">
              <el-input-number
                :model-value="(selectedNode.config || {}).retry || 3"
                :min="0"
                :max="10"
                style="width:100%"
                @change="v => { selectedNode.config.retry = v; emitUpdate(); }"
              />
            </el-form-item>
            <el-form-item label="错误处理">
              <el-radio-group
                :model-value="(selectedNode.config || {}).onError || 'fail'"
                @change="v => { selectedNode.config.onError = v; emitUpdate(); }"
              >
                <el-radio-button label="fail">失败停止</el-radio-button>
                <el-radio-button label="skip">跳过继续</el-radio-button>
                <el-radio-button label="retry">重试</el-radio-button>
                <el-radio-button label="default">使用默认</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="日志级别">
              <el-radio-group
                :model-value="(selectedNode.config || {}).logLevel || 'info'"
                @change="v => { selectedNode.config.logLevel = v; emitUpdate(); }"
              >
                <el-radio-button label="debug">DEBUG</el-radio-button>
                <el-radio-button label="info">INFO</el-radio-button>
                <el-radio-button label="warn">WARN</el-radio-button>
                <el-radio-button label="error">ERROR</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="缓存结果">
              <el-switch
                :model-value="(selectedNode.config || {}).cache || false"
                @change="v => { selectedNode.config.cache = v; emitUpdate(); }"
              />
            </el-form-item>
          </el-form>
          <el-empty
            v-else
            description="连线无高级配置"
            :image-size="60"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
    <BackToTop />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import BackToTop from '@/components/BackToTop.vue'

const props = defineProps({
  selectedNode: { type: Object, default: null },
  selectedEdge: { type: Object, default: null },
  edges: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['update', 'delete-node', 'delete-edge', 'select-node'])

const activeTab = ref('basic')

const NODE_TYPES = [
  { type: 'llm', name: 'LLM 大模型', icon: '🤖', color: '#3b82f6' },
  { type: 'rag', name: 'RAG 检索', icon: '📚', color: '#10b981' },
  { type: 'tool', name: '工具调用', icon: '🔧', color: '#f59e0b' },
  { type: 'code', name: '代码执行', icon: '💻', color: '#8b5cf6' },
  { type: 'http', name: 'HTTP 请求', icon: '🌐', color: '#06b6d4' },
  { type: 'condition', name: '条件分支', icon: '🔀', color: '#eab308' },
  { type: 'memory', name: '记忆读写', icon: '🧠', color: '#ec4899' },
  { type: 'loop', name: '循环 Loop', icon: '🔁', color: '#14b8a6' },
  { type: 'parallel', name: '并行 Parallel', icon: '⚡', color: '#f97316' },
  { type: 'subflow', name: '子流 SubFlow', icon: '📦', color: '#0ea5e9' },
]

function getIcon(type) {
  return NODE_TYPES.find(n => n.type === type)?.icon || '❓'
}
function getColor(type) {
  return NODE_TYPES.find(n => n.type === type)?.color || '#6b7280'
}
function getName(type) {
  return NODE_TYPES.find(n => n.type === type)?.name || type
}

function getDownstream(nodeId) {
  return (props.edges || [])
    .filter(e => e.from === nodeId)
    .map(e => e.to)
}
function getNodeName(id) {
  // 优先查找节点名（如果父组件传了 nodes 列表）；否则用 edge label 兜底
  // 这里用 edge.label 兜底，保持向后兼容
  const edge = (props.edges || []).find(e => e.to === id)
  return edge?.label || edge?.name || id
}

function onDownstreamClick(nodeId) {
  emit('select-node', nodeId)
}
function getOutputSchema() {
  // 简化 schema 描述
  return JSON.stringify({ output: { type: 'string' } }, null, 2)
}

function emitUpdate() {
  emit('update', props.selectedNode || props.selectedEdge)
}
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      props.selectedNode
        ? `确认删除节点「${props.selectedNode.name || props.selectedNode.id}」？`
        : '确认删除当前连线？',
      '确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  if (props.selectedNode) {
    emit('delete-node', props.selectedNode)
    ElMessage.success('已删除节点')
  } else if (props.selectedEdge) {
    emit('delete-edge', props.selectedEdge)
    ElMessage.success('已删除连线')
  }
}
</script>

<style scoped>
.property-panel {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 16px;
  overflow-y: auto;
  color: white;
  min-height: 200px;
}
.empty-panel {
  text-align: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-panel h3 {
  margin: 0 0 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}
.empty-panel p {
  margin: 0;
  font-size: 12px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  gap: 8px;
}
.panel-header h3 {
  margin: 0;
  font-size: 14px;
  color: white;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.header-icon {
  margin-right: 6px;
}
.panel-tabs :deep(.el-tabs__nav-wrap::after) {
  background: rgba(255, 255, 255, 0.1);
}
.panel-tabs :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.6);
}
.panel-tabs :deep(.el-tabs__item.is-active) {
  color: white;
}
.panel-tabs :deep(.el-tabs__active-bar) {
  background: #a855f7;
}
.panel-tabs :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.7);
}
.panel-tabs :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.panel-tabs :deep(.el-input__inner) {
  color: white;
}
.panel-tabs :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.05);
  color: white;
  border-color: rgba(255, 255, 255, 0.1);
}
.downstream-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.downstream-tag {
  background: rgba(168, 85, 247, 0.2);
  border-color: #a855f7;
  color: white;
}
.downstream-tag.clickable {
  cursor: pointer;
}
.downstream-tag.clickable:hover {
  background: rgba(168, 85, 247, 0.4);
  text-decoration: underline;
}
.no-downstream {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}
</style>
