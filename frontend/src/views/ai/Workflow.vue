<!--
  @file views/ai/Workflow.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/Workflow.vue (工作流)
  @version V3.5.12+ (前端注释补全)
  @description 工作流
-->
<template>
  <div class="page-workflow">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('workflow.title') }} <el-tag size="small" type="info">V2.7.3</el-tag></h2>
        <p class="page-subtitle">节点编排 · 串行/并行 · 验证 · 执行 · 示例</p>
      </div>
      <el-button-group>
        <el-button :icon="Plus" @click="addStep">添加节点</el-button>
        <el-button :icon="CircleCheck" @click="validateWorkflow">验证</el-button>
        <el-button :icon="Document" @click="loadExample">示例</el-button>
        <el-button type="success" :icon="VideoPlay" :loading="running" @click="runWorkflow">执行</el-button>
      </el-button-group>
    </header>

    <!-- 2. section: 编辑区 (16:8 分栏) -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <section class="section">
          <h3 class="section-title">📝 工作流定义</h3>
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>{{ steps.length }} 节点</span>
                <el-tag v-if="workflowValid === true" type="success" size="small">✓ 有效</el-tag>
                <el-tag v-else-if="workflowValid === false" type="danger" size="small">✗ 错误</el-tag>
                <el-tag v-else size="small">未验证</el-tag>
              </div>
            </template>
            <div v-for="(step, idx) in steps" :key="idx" class="step-row">
              <el-tag size="small" :type="stepTypeColor(step.type)">{{ idx + 1 }}. {{ step.type }}</el-tag>
              <el-input v-model="step.name" placeholder="节点名" style="width: 200px" />
              <el-input v-model="step.config" placeholder="config (JSON)" style="flex: 1" />
              <el-button size="small" :icon="Delete" @click="steps.splice(idx, 1)" type="danger" plain />
            </div>
            <EmptyState :description="'暂无数据'" />
          </el-card>
        </section>
      </el-col>

      <el-col :xs="24" :md="10">
        <section class="section">
          <h3 class="section-title">▶️ 执行结果</h3>
          <el-card shadow="hover">
            <EmptyState v-if="!lastResult" :description="'暂无数据'" />
            <pre v-else class="result-pre">{{ JSON.stringify(lastResult, null, 2) }}</pre>
          </el-card>
        </section>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'

import { executeWorkflow, validateWorkflow as apiValidate, dispatchPrompt } from '@/api/ai'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const nodes = ref([
  { id: 'step1', toolCode: 'sql.query', inputJson: '{"dataSourceId":1,"question":"查询 user 表前 5"}', status: 'PENDING' },
  { id: 'step2', toolCode: 'data.analyze.stats', inputJson: '{"dataSourceId":1,"table":"user","column":"age"}', status: 'PENDING' }
])
const running = ref(false)
const lastResult = ref(null)

function _nodeStatusType(s) {
  return { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'danger' }[s] || ''
}

function addStep() {
  const id = 'step' + (nodes.value.length + 1)
  nodes.value.push({ id, toolCode: '', inputJson: '{}', status: 'PENDING' })
}

function _removeNode(idx) {
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
    toast.warning('请先添加节点')
    return
  }
  if (nodes.value.some(n => !n.toolCode)) {
    toast.warning('请填写所有节点的工具编码')
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
      toast.success('工作流执行成功')
    } else {
      toast.error('执行失败: ' + (res.data.error || '未知'))
    }
  } catch (e) {
    toast.error('执行异常: ' + e.message)
  } finally {
    running.value = false
  }
}

async function validateWorkflow() {
  if (!nodes.value.length) {
    toast.warning('工作流为空')
    return
  }
  try {
    const wf = buildWorkflow()
    const res = await apiValidate(wf)
    if (res.data.valid) {
      toast.success(`验证通过 (${res.data.nodeCount} 节点, ${res.data.edgeCount} 边)`)
    } else {
      toast.error('验证失败: ' + (res.data.error || '结构错误'))
    }
  } catch (e) {
    toast.error('验证失败')
  }
}

function loadExample() {
  nodes.value = [
    { id: 'query', toolCode: 'sql.query', inputJson: '{"dataSourceId":1,"question":"查询 user 表的城市分布"}', status: 'PENDING' },
    { id: 'analyze', toolCode: 'data.analyze.stats', inputJson: '{"dataSourceId":1,"table":"user"}', status: 'PENDING' },
    { id: 'chart', toolCode: 'data.analyze.distribution', inputJson: '{"dataSourceId":1,"table":"user","column":"age","buckets":10}', status: 'PENDING' }
  ]
  toast.success('已加载示例')
}

function _formatOutput(o) {
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
