<!--
  @file views/ai/AutoAgentGroup.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/AutoAgentGroup.vue (V3.5.48)
  @description AI 工具配置 - 一句话生成智能体群 - 用户目标能力之一
  - 3 端点: generate / template / templates
-->
<template>
  <div class="page-auto-agent-group page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🤖 一句话生成智能体群 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <el-radio-group v-model="mode" size="small">
            <el-radio-button value="auto">⚡ 一句话</el-radio-button>
            <el-radio-button value="template">📋 模板</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 模式 1: 一句话生成 -->
      <div v-if="mode === 'auto'" class="mode-section">
        <el-form label-position="top">
          <el-form-item label="智能体群描述 (一句话)">
            <el-input
              v-model="autoForm.description"
              type="textarea"
              :rows="3"
              placeholder="例: 给我公司生成一个竞品分析智能体群, 包含 3 个核心竞品对比, 自动出报告"
            />
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="autoForm.count" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="协作模式">
            <el-select v-model="autoForm.collaboration" style="width: 200px">
              <el-option label="顺序执行" value="sequential" />
              <el-option label="并行执行" value="parallel" />
              <el-option label="专家投票" value="voting" />
            </el-select>
          </el-form-item>
          <el-button type="primary" @click="onGenerate" :loading="generating">⚡ 生成智能体群</el-button>
        </el-form>
      </div>

      <!-- 模式 2: 模板 -->
      <div v-if="mode === 'template'" class="mode-section">
        <el-form label-position="top">
          <el-form-item label="选择模板">
            <el-select v-model="templateForm.code" placeholder="选择模板" style="width: 100%" @change="onTemplateChange">
              <el-option v-for="t in templates" :key="t.code" :label="t.name" :value="t.code">
                <div>
                  <strong>{{ t.name }}</strong>
                  <div style="font-size: 12px; color: #909399">{{ t.description }}</div>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="currentTemplate" label="模板说明">
            <el-alert type="info" :closable="false">
              <pre style="white-space: pre-wrap; margin: 0">{{ currentTemplate.content }}</pre>
            </el-alert>
          </el-form-item>
          <el-button type="primary" @click="onTemplateGenerate" :loading="generating">📋 基于模板生成</el-button>
        </el-form>
      </div>
    </el-card>

    <!-- 生成结果 -->
    <el-card v-if="result.agents" class="result-card">
      <template #header>
        <div class="header">
          <span>✅ 智能体群已生成 ({{ result.agents.length }} 个 Agent)</span>
          <el-button type="primary" @click="onSave">💾 保存到市场</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="群组 ID">{{ result.groupId }}</el-descriptions-item>
        <el-descriptions-item label="协作模式">{{ result.collaboration }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ result.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success">已就绪</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <h4 style="margin-top: 16px">📋 Agent 列表</h4>
      <el-table :data="result.agents" border>
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="name" label="名称" width="200" />
        <el-table-column prop="role" label="角色" width="150" />
        <el-table-column prop="tool" label="使用工具" width="180" />
        <el-table-column prop="description" label="职责说明" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="onTestAgent(row)">测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { autoAgentGroupGenerate, autoAgentGroupByTemplate, autoAgentGroupTemplates } from '@/api/ai'

const mode = ref('auto')
const generating = ref(false)
const templates = ref([])
const currentTemplate = ref(null)

const autoForm = reactive({
  description: '给我公司生成一个竞品分析智能体群, 包含 3 个核心竞品对比, 自动出报告',
  count: 3,
  collaboration: 'sequential'
})

const templateForm = reactive({
  code: ''
})

const result = reactive({
  groupId: '',
  collaboration: '',
  createdAt: '',
  agents: null
})

async function loadTemplates() {
  try {
    const r = await autoAgentGroupTemplates()
    templates.value = r.data || []
  } catch (e) {
    // 静默
  }
}

function onTemplateChange(code) {
  currentTemplate.value = templates.value.find(t => t.code === code) || null
}

async function onGenerate() {
  if (!autoForm.description) {
    ElMessage.warning('请输入描述')
    return
  }
  generating.value = true
  try {
    const r = await autoAgentGroupGenerate({
      description: autoForm.description,
      count: autoForm.count,
      collaboration: autoForm.collaboration
    })
    Object.assign(result, r.data || {})
    ElMessage.success(`生成 ${result.agents?.length || 0} 个 Agent`)
  } catch (e) {
  } finally {
    generating.value = false
  }
}

async function onTemplateGenerate() {
  if (!templateForm.code) {
    ElMessage.warning('请选择模板')
    return
  }
  generating.value = true
  try {
    const r = await autoAgentGroupByTemplate({
      code: templateForm.code,
      params: autoForm
    })
    Object.assign(result, r.data || {})
    ElMessage.success(`基于模板生成 ${result.agents?.length || 0} 个 Agent`)
  } catch (e) {
  } finally {
    generating.value = false
  }
}

function onSave() {
  ElMessage.info('保存到市场功能 V3.5.49 上线')
}

function onTestAgent(row) {
  ElMessage.info(`测试 Agent: ${row.name}`)
}

onMounted(loadTemplates)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.mode-section { padding: 8px 0; }
.result-card { margin-top: 16px; }
pre { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; }
</style>
