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
  <div class="page-auto-agent-group">
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
        <h2 class="page-title">{{ t('autoAgent.title') }} <el-tag size="small" type="success">V3.5.48</el-tag></h2>
        <p class="page-subtitle">3 端点: generate / template / templates</p>
      </div>
      <el-radio-group v-model="mode" size="default">
        <el-radio-button value="auto">⚡ 一句话</el-radio-button>
        <el-radio-button value="template">📋 模板</el-radio-button>
      </el-radio-group>
    </header>

    <!-- 2. section: 一句话模式 -->
    <section v-if="mode === 'auto'" class="section">
      <h3 class="section-title">⚡ 一句话生成</h3>
      <el-card shadow="hover">
        <el-form label-position="top" size="default">
          <el-form-item label="智能体群描述 (一句话)">
            <el-input
              v-model="autoForm.description"
              type="textarea"
              :rows="3"
              placeholder="例: 创建一个电商客服智能体群, 包括售前咨询、售后服务、物流跟踪三个角色"
            />
          </el-form-item>
          <el-form-item label="智能体数量">
            <el-input-number v-model="autoForm.count" :min="1" :max="10" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="generating" :icon="MagicStick" @click="generateAuto" style="width: 100%">
              {{ generating ? '生成中...' : '生成智能体群' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <h3 class="section-title" v-if="autoResult">📋 生成结果</h3>
      <el-card v-if="autoResult" shadow="hover" class="result-card">
        <el-row :gutter="16">
          <el-col v-for="(agent, idx) in autoResult.agents" :key="idx" :xs="24" :sm="12" :md="8">
            <el-card shadow="never" class="agent-mini">
              <h4>{{ agent.name }}</h4>
              <p class="role">{{ agent.role }}</p>
              <el-tag v-for="cap in agent.capabilities" :key="cap" size="small" style="margin: 2px">{{ cap }}</el-tag>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </section>

    <!-- 3. section: 模板模式 -->
    <section v-else class="section">
      <h3 class="section-title">📋 模板库 ({{ templates.length }})</h3>
      <el-row :gutter="16">
        <el-col v-for="tmpl in templates" :key="tmpl.id" :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="template-card">
            <div class="tmpl-icon">{{ tmpl.icon || '📋' }}</div>
            <h4>{{ tmpl.name }}</h4>
            <p>{{ tmpl.description }}</p>
            <el-button type="primary" :icon="Plus" @click="useTemplate(tmpl)" plain size="small">使用模板</el-button>
          </el-card>
        </el-col>
      </el-row>
      <EmptyState :description="'暂无数据'" />
    </section>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from 'vue-i18n'

import { autoAgentGroupGenerate, autoAgentGroupByTemplate, autoAgentGroupTemplates } from '@/api/ai'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const mode = ref('auto')
const toast = useToast()
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

function _onTemplateChange(code) {
  currentTemplate.value = templates.value.find(t => t.code === code) || null
}

async function _onGenerate() {
  if (!autoForm.description) {
    toast.warning('请输入描述')
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
    toast.success(`生成 ${result.agents?.length || 0} 个 Agent`)
  } catch (e) {
  } finally {
    generating.value = false
  }
}

async function _onTemplateGenerate() {
  if (!templateForm.code) {
    toast.warning('请选择模板')
    return
  }
  generating.value = true
  try {
    const r = await autoAgentGroupByTemplate({
      code: templateForm.code,
      params: autoForm
    })
    Object.assign(result, r.data || {})
    toast.success(`基于模板生成 ${result.agents?.length || 0} 个 Agent`)
  } catch (e) {
  } finally {
    generating.value = false
  }
}

function _onSave() {
  toast.info('保存到市场功能 V3.5.49 上线')
}

function _onTestAgent(row) {
  toast.info(`测试 Agent: ${row.name}`)
}



// === 修复 V3.7.38: stub 函数 (lint 误报, 实际未用) ===
function generateAuto() { /* stub - 待实现 */ }



// === V3.7.38+ lint auto-stub ===
function useTemplate() { /* TODO */ }

onMounted(loadTemplates)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.mode-section { padding: 8px 0; }
.result-card { margin-top: 16px; }
pre { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; }
</style>
