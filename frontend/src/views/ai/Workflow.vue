<!--
  @file views/ai/Workflow.vue (工作流)
  @version V3.5.12+ (前端注释补全)
  @description 工作流
-->
<template>
  <div class="workflow">
    <el-card>
      <template #header>
        <div class="header">
          <span>🔗 AI 工作流编排 (V2.7.3)</span>
          <el-button-group>
            <el-button @click="addStep">+ 添加节点</el-button>
            <el-button type="success" :loading="running" @click="runWorkflow">▶️ 执行</el-button>
            <el-button @click="validateWorkflow">✓ 验证</el-button>
            <el-button type="primary" @click="loadExample">📋 示例</el-button>
          </el-button-group>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 工作流编辑区 -->
        <el-col :span="14">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span>📐 工作流编辑 (DAG)</span>
                <el-text size="small" type="info">节点表示 AI 工具, 边表示数据流</el-text>
              </div>
            </template>

            <div class="nodes">
              <div v-for="(node, idx) in nodes" :key="node.id" class="node-card">
                <div class="node-header">
                  <el-tag :type="nodeStatusType(node.status)">{{ node.id }}</el-tag>
                  <el-input v-model="node.toolCode" placeholder="工具编码 (如 sql.query)" size="small" style="width: 280px" />
                  <el-button size="small" type="danger" @click="removeNode(idx)">删除</el-button>
                </div>
                <div class="node-body">
                  <el-input v-model="node.inputJson" type="textarea" :rows="3" placeholder='{"dataSourceId": 1}' size="small" />
                </div>
                <div v-if="idx < nodes.length - 1" class="edge-arrow">↓</div>
              </div>
            </div>

            <el-empty v-if="!nodes.length" description="点击 + 添加节点 开始构建" />
          </el-card>
        </el-col>

        <!-- 执行结果 -->
        <el-col :span="10">
          <el-card shadow="never">
            <template #header>📊 执行结果</template>

            <el-alert
              v-if="lastResult"
              :type="lastResult.success ? 'success' : 'error'"
              :title="lastResult.success ? '✓ 工作流执行成功' : '✗ 执行失败: ' + (lastResult.error || '')"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />

            <el-timeline v-if="lastResult">
              <el-timeline-item
                v-for="node in lastResult.nodes"
                :key="node.id"
                :type="nodeStatusType(node.status)"
                :timestamp="node.durationMs + 'ms'"
              >
                <b>{{ node.id }}</b> ({{ node.toolCode }})
                <el-tag size="small" :type="nodeStatusType(node.status)">{{ node.status }}</el-tag>
                <div v-if="node.error" style="color: red; font-size: 12px">{{ node.error }}</div>
                <div v-if="node.output" style="font-size: 12px; margin-top: 4px">
                  <pre style="background: #f5f5f5; padding: 4px; max-height: 100px; overflow: auto">{{ formatOutput(node.output) }}</pre>
                </div>
              </el-timeline-item>
            </el-timeline>

            <el-empty v-if="!lastResult" description="执行工作流后查看结果" />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeWorkflow, validateWorkflow as apiValidate, dispatchPrompt } from '@/api/ai'

const nodes = ref([
  { id: 'step1', toolCode: 'sql.query', inputJson: '{"dataSourceId":1,"question":"查询 user 表前 5"}', status: 'PENDING' },
  { id: 'step2', toolCode: 'data.analyze.stats', inputJson: '{"dataSourceId":1,"table":"user","column":"age"}', status: 'PENDING' }
])
const running = ref(false)
const lastResult = ref(null)

function nodeStatusType(s) {
  return { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'danger' }[s] || ''
}

function addStep() {
  const id = 'step' + (nodes.value.length + 1)
  nodes.value.push({ id, toolCode: '', inputJson: '{}', status: 'PENDING' })
}

function removeNode(idx) {
  nodes.value.splice(idx, 1)
}

function buildWorkflow() {
  return {
    name: 'ad-hoc',
    description: '',
    nodes: nodes.value.map(n => ({
      id: n.id,
      toolCode: n.toolCode,
      input: safeParse(n.inputJson)
    })),
    edges: nodes.value.length > 1
      ? nodes.value.slice(0, -1).map((n, i) => ({ from: n.id, to: nodes.value[i + 1].id }))
      : []
  }
}

function safeParse(s) {
  try { return JSON.parse(s || '{}') } catch { return {} }
}

async function runWorkflow() {
  if (!nodes.value.length) {
    ElMessage.warning('请先添加节点')
    return
  }
  if (nodes.value.some(n => !n.toolCode)) {
    ElMessage.warning('请填写所有节点的工具编码')
    return
  }
  running.value = true
  try {
    const wf = buildWorkflow()
    const res = await executeWorkflow(wf)
    lastResult.value = res.data
    // 更新节点状态
    if (res.data && res.data.nodes) {
      res.data.nodes.forEach(n => {
        const local = nodes.value.find(x => x.id === n.id)
        if (local) local.status = n.status
      })
    }
    if (res.data.success) {
      ElMessage.success('工作流执行成功')
    } else {
      ElMessage.error('执行失败: ' + (res.data.error || '未知'))
    }
  } catch (e) {
    ElMessage.error('执行异常: ' + e.message)
  } finally {
    running.value = false
  }
}

async function validateWorkflow() {
  if (!nodes.value.length) {
    ElMessage.warning('工作流为空')
    return
  }
  try {
    const wf = buildWorkflow()
    const res = await apiValidate(wf)
    if (res.data.valid) {
      ElMessage.success(`验证通过 (${res.data.nodeCount} 节点, ${res.data.edgeCount} 边)`)
    } else {
      ElMessage.error('验证失败: ' + (res.data.error || '结构错误'))
    }
  } catch (e) {
    ElMessage.error('验证失败')
  }
}

function loadExample() {
  nodes.value = [
    { id: 'query', toolCode: 'sql.query', inputJson: '{"dataSourceId":1,"question":"查询 user 表的城市分布"}', status: 'PENDING' },
    { id: 'analyze', toolCode: 'data.analyze.stats', inputJson: '{"dataSourceId":1,"table":"user"}', status: 'PENDING' },
    { id: 'chart', toolCode: 'data.analyze.distribution', inputJson: '{"dataSourceId":1,"table":"user","column":"age","buckets":10}', status: 'PENDING' }
  ]
  ElMessage.success('已加载示例')
}

function formatOutput(o) {
  if (typeof o === 'string') return o
  try { return JSON.stringify(o, null, 2) } catch { return String(o) }
}
</script>

<style scoped>
.workflow {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.node-card {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px;
  background: #fafafa;
}
.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.node-body {
  margin-top: 8px;
}
.edge-arrow {
  text-align: center;
  font-size: 20px;
  color: #999;
  margin: 4px 0;
}
</style>
