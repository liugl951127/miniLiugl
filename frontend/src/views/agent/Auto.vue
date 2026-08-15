<template>
  <div class="auto-group-page">
    <div class="page-header">
      <div>
        <h1>🤖 智能体群生成 <span class="badge">V3.4.2</span></h1>
        <p class="sub">一句话描述任务 → AI 自动选模板 → 生成完整 Agent 协作群 → 可直接执行</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="input-card">
          <template #header><span>🎯 生成方式</span></template>

          <el-tabs v-model="mode">
            <el-tab-pane label="💬 一句话生成" name="oneliner">
              <div class="oneliner-form">
                <el-input
                  v-model="oneLiner"
                  type="textarea"
                  :rows="3"
                  placeholder="例如: 写一份季度销售报告、分析竞品对比、做一个代码评审、选最佳投资方案..."
                />
                <div class="form-tip">输入任务描述，AI 自动识别意图并匹配合适的 Agent 群模板</div>
                <el-button type="primary" :loading="generating" @click="doOneLiner" style="margin-top: 12px">
                  🚀 一句话生成
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="📋 使用模板生成" name="template">
              <div class="template-grid">
                <div
                  v-for="tmpl in templateList"
                  :key="tmpl.name"
                  :class="['tmpl-card', { selected: selectedTemplate === tmpl.name }]"
                  @click="toggleTemplate(tmpl.name)"
                >
                  <div class="tmpl-icon">{{ tmpl.icon }}</div>
                  <div class="tmpl-name">{{ tmpl.label }}</div>
                  <div class="tmpl-desc">{{ tmpl.desc }}</div>
                  <div style="text-align: right">
                    <el-tag size="small" :type="strategyColor(tmpl.strategy)">{{ tmpl.strategy }}</el-tag>
                  </div>
                </div>
              </div>

              <div v-if="selectedTemplate" style="margin-top: 16px">
                <el-input
                  v-model="templateDesc"
                  type="textarea"
                  :rows="2"
                  placeholder="（可选）描述你的具体任务，如：写一份 Q3 销售报告..."
                />
                <el-button type="primary" :loading="generating" @click="doTemplate" style="margin-top: 12px">
                  📋 使用模板生成
                </el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card v-if="generatedGroup" class="result-card" style="margin-top: 16px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>✅ 生成的智能体群</span>
              <div>
                <el-button size="small" type="success" @click="saveGroup" :loading="saving">
                  💾 保存到数据库
                </el-button>
                <el-button size="small" type="primary" @click="showRunDialog = true">
                  ▶ 执行任务
                </el-button>
              </div>
            </div>
          </template>

          <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
            <el-descriptions-item label="群组 ID">
              <code>{{ generatedGroup.groupId }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="协作策略">
              <el-tag size="small" :type="strategyColor(generatedGroup.strategy)">{{ generatedGroup.strategy }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="群组名称">{{ generatedGroup.name }}</el-descriptions-item>
            <el-descriptions-item label="成员数量">{{ generatedGroup.members?.length || 0 }} 个 Agent</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ generatedGroup.description }}</el-descriptions-item>
          </el-descriptions>

          <div class="members-section">
            <h4>🧑‍🤝‍🧑 Agent 成员</h4>
            <div class="member-list">
              <div
                v-for="m in generatedGroup.members"
                :key="m.memberId"
                :class="['member-card', 'role-' + (m.role || '').toLowerCase()]"
              >
                <div class="member-head">
                  <span class="member-icon">{{ roleIcon(m.role) }}</span>
                  <span class="member-name">{{ m.persona }}</span>
                  <el-tag size="small" :type="roleColor(m.role)">{{ m.role }}</el-tag>
                  <span class="member-weight">权重 {{ m.weight }}</span>
                </div>
                <div class="member-duty">
                  <span class="duty-label">职责:</span> {{ m.responsibility }}
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header><span>💡 工作原理</span></template>
          <el-steps direction="vertical" :space="60" :active="5">
            <el-step title="输入任务" description="用自然语言描述你想做什么" />
            <el-step title="AI 意图识别" description="自动识别关键词（写/分析/辩论/投票/编码/研究）" />
            <el-step title="选择模板" description="6 种内置模板（写作/分析/辩论/投票/编码/研究）" />
            <el-step title="组装成员" description="Manager + N 个 Worker + 可选 Critic" />
            <el-step title="协作执行" description="按策略 PIPELINE/DEBATE/VOTE/SWARM 协作" />
          </el-steps>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>📁 已保存的群组</span>
            <el-button size="small" @click="loadGroups" style="float: right">🔄 刷新</el-button>
          </template>

          <div v-if="groupsLoading" style="text-align: center; padding: 20px">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中...
          </div>
          <div v-else-if="savedGroups.length === 0" style="text-align: center; color: #999; padding: 20px">
            暂无已保存的群组
          </div>
          <div v-else class="saved-group-list">
            <div v-for="g in savedGroups" :key="g.groupId" class="saved-group-item">
              <div class="sg-head">
                <span class="sg-name">{{ g.name }}</span>
                <el-tag size="small" :type="strategyColor(g.strategy)">{{ g.strategy }}</el-tag>
              </div>
              <div class="sg-desc">{{ g.description }}</div>
              <div class="sg-meta">
                <span>{{ g.createdAt }}</span>
                <el-button size="small" type="primary" link @click="loadGroupDetail(g.groupId)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showRunDialog" title="▶ 执行群组任务" width="560px">
      <el-form :model="runForm" label-width="80px">
        <el-form-item label="任务主题" required>
          <el-input v-model="runForm.subject" placeholder="描述本次要完成的具体任务" />
        </el-form-item>
        <el-form-item label="附加输入">
          <el-input v-model="runForm.input" type="textarea" :rows="2" placeholder="（可选）补充材料或上下文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRunDialog = false">取消</el-button>
        <el-button type="primary" :loading="running" @click="executeGroup">▶ 执行</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showResultDialog" title="📊 执行结果" width="720px">
      <div v-if="runResult">
        <el-alert
          :type="runResult.success ? 'success' : 'error'"
          :title="runResult.success ? '执行成功' : '执行失败'"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <div v-if="runResult.result">
          <h4>最终结果</h4>
          <pre class="result-json">{{ JSON.stringify(runResult.result, null, 2) }}</pre>
        </div>
        <div v-if="runResult.error" style="color: #f56c6c; margin-top: 8px">{{ runResult.error }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  generateFromOneLiner,
  generateFromTemplate,
  listTemplates,
  createGroup,
  listGroups,
  runGroup,
} from '@/api/agentAuto'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const mode = ref('oneliner')
const oneLiner = ref('')
const selectedTemplate = ref('')
const templateDesc = ref('')
const generating = ref(false)
const generatedGroup = ref(null)

const templateList = ref([])

const saving = ref(false)
const savedGroups = ref([])
const groupsLoading = ref(false)

const showRunDialog = ref(false)
const showResultDialog = ref(false)
const running = ref(false)
const runForm = reactive({ subject: '', input: '' })
const runResult = ref(null)

const roleIcon = (role) => ({ MANAGER: '👔', WORKER: '🔧', CRITIC: '🔍', OBSERVER: '👀' }[role] || '🤖')
const roleColor = (role) => ({ MANAGER: 'primary', WORKER: 'success', CRITIC: 'warning', OBSERVER: 'info' }[role] || 'info')
const strategyColor = (s) => ({ PIPELINE: 'primary', DEBATE: 'warning', VOTE: 'success', SWARM: 'danger' }[s] || 'info')

const TMPL_META = {
  WRITING_TEAM:  { icon: '✍️', label: '写作团队',   desc: '大纲 → 撰写 → 润色 → 审校',       strategy: 'PIPELINE' },
  ANALYST_TEAM: { icon: '📊', label: '分析团队',   desc: '多维并行分析, Manager 汇总',       strategy: 'SWARM' },
  DEBATE_PANEL: { icon: '🎤', label: '辩论小组',   desc: '正反方辩论, Critic 评最优',         strategy: 'DEBATE' },
  VOTE_COUNCIL: { icon: '🗳️', label: '投票委员会', desc: '多 Agent 独立提议, 加权投票',       strategy: 'VOTE' },
  CODER_TEAM:   { icon: '💻', label: '编码团队',   desc: '设计 → 实现 → Code Review',         strategy: 'PIPELINE' },
  RESEARCH_TEAM:{ icon: '🔬', label: '研究团队',   desc: '多维探索, 整合研究报告',             strategy: 'SWARM' },
}

async function loadTemplates() {
  try {
    const r = await listTemplates()
    const data = r.data?.data || r.data || {}
    templateList.value = Object.entries(data).map(([name, tmpl]) => ({
      name,
      ...(TMPL_META[name] || { icon: '🤖', label: name, desc: tmpl.description, strategy: tmpl.strategy }),
    }))
  } catch {
    // fallback to local list
    templateList.value = Object.entries(TMPL_META).map(([name, m]) => ({ name, ...m }))
  }
}

function toggleTemplate(name) {
  selectedTemplate.value = selectedTemplate.value === name ? '' : name
}

async function doOneLiner() {
  if (!oneLiner.value.trim()) return ElMessage.warning('请输入任务描述')
  generating.value = true
  generatedGroup.value = null
  try {
    const r = await generateFromOneLiner(oneLiner.value)
    generatedGroup.value = r.data?.data || r.data
    ElMessage.success('生成成功！')
  } catch (e) {
    ElMessage.error('生成失败: ' + (e?.message || ''))
  } finally {
    generating.value = false
  }
}

async function doTemplate() {
  if (!selectedTemplate.value) return ElMessage.warning('请先选择一个模板')
  generating.value = true
  generatedGroup.value = null
  try {
    const r = await generateFromTemplate(selectedTemplate.value, templateDesc.value)
    generatedGroup.value = r.data?.data || r.data
    ElMessage.success('生成成功！')
  } catch (e) {
    ElMessage.error('生成失败: ' + (e?.message || ''))
  } finally {
    generating.value = false
  }
}

async function saveGroup() {
  if (!generatedGroup.value) return
  saving.value = true
  try {
    const members = (generatedGroup.value.members || []).map(m => ({
      memberId: m.memberId,
      groupId: generatedGroup.value.groupId,
      agentName: m.persona,
      role: m.role,
      weight: m.weight,
      capability: m.responsibility,
      order: 0,
    }))
    const r = await createGroup({
      name: generatedGroup.value.name,
      description: generatedGroup.value.description,
      strategy: generatedGroup.value.strategy,
      userId: userStore.profile?.id || 1,
      ownerId: userStore.profile?.id || 1,
      members,
    })
    ElMessage.success('保存成功！群组 ID: ' + (r.data?.data?.groupId || ''))
    loadGroups()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e?.message || ''))
  } finally {
    saving.value = false
  }
}

async function loadGroups() {
  groupsLoading.value = true
  try {
    const r = await listGroups()
    savedGroups.value = r.data?.data || r.data || []
  } catch {
    savedGroups.value = []
  } finally {
    groupsLoading.value = false
  }
}

function loadGroupDetail(groupId) {
  const g = savedGroups.value.find(x => x.groupId === groupId)
  if (!g) return
  try {
    const members = JSON.parse(g.membersJson || '[]')
    generatedGroup.value = {
      groupId: g.groupId,
      name: g.name,
      description: g.description,
      strategy: g.strategy,
      members,
    }
  } catch {
    generatedGroup.value = {
      groupId: g.groupId,
      name: g.name,
      description: g.description,
      strategy: g.strategy,
      members: [],
    }
  }
}

async function executeGroup() {
  if (!runForm.subject.trim()) return ElMessage.warning('请输入任务主题')
  running.value = true
  runResult.value = null
  try {
    const groupId = generatedGroup.value?.groupId
    const r = await runGroup(groupId, runForm.subject)
    runResult.value = r.data?.data || r.data
    showResultDialog.value = true
    ElMessage.success('执行完成！')
  } catch (e) {
    ElMessage.error('执行失败: ' + (e?.message || ''))
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadGroups()
})
</script>

<style scoped>
.auto-group-page { padding: 20px; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; display: flex; align-items: center; gap: 8px; }
.sub { color: #6b7280; margin: 6px 0 0; font-size: 13px; }
.badge {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px;
}
.form-tip { font-size: 12px; color: #909399; margin-top: 6px; }
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 8px;
}
.tmpl-card {
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.tmpl-card:hover { border-color: #6366f1; box-shadow: 0 2px 8px rgba(99,102,241,0.15); }
.tmpl-card.selected { border-color: #6366f1; background: #eef2ff; box-shadow: 0 2px 12px rgba(99,102,241,0.25); }
.tmpl-icon { font-size: 28px; margin-bottom: 6px; }
.tmpl-name { font-weight: 600; font-size: 14px; color: #1f2937; margin-bottom: 4px; }
.tmpl-desc { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.member-list { display: flex; flex-direction: column; gap: 10px; }
.member-card {
  border-radius: 8px;
  padding: 12px 14px;
  border-left: 4px solid #6366f1;
  background: #fafafa;
  transition: all 0.2s;
}
.member-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.member-card.role-manager { border-left-color: #3b82f6; background: #eff6ff; }
.member-card.role-worker  { border-left-color: #10b981; background: #f0fdf4; }
.member-card.role-critic { border-left-color: #f59e0b; background: #fffbeb; }
.member-card.role-observer { border-left-color: #6b7280; }
.member-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.member-icon { font-size: 18px; }
.member-name { font-weight: 600; color: #1f2937; flex: 1; }
.member-weight { font-size: 12px; color: #9ca3af; }
.member-duty { font-size: 13px; color: #4b5563; }
.duty-label { color: #9ca3af; margin-right: 4px; }
.saved-group-list { display: flex; flex-direction: column; gap: 10px; }
.saved-group-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fafafa;
}
.sg-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.sg-name { font-weight: 600; color: #1f2937; font-size: 14px; }
.sg-desc { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.sg-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #9ca3af; }
.result-json {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
  max-height: 400px;
  font-size: 13px;
}
</style>
