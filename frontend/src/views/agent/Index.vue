<!--
  @file views/agent/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="agent-container">
    <div class="agent-header">
      <h1>🤖 {{ t('agent.title') }} <span class="badge">V2.0</span></h1>
      <p class="sub">{{ t('agent.subtitle') }}</p>
    </div>

    <el-card class="input-card">
      <el-form>
        <el-form-item :label="t('agent.taskGoal')">
          <el-input
            v-model="goal"
            type="textarea"
            :rows="3"
            :placeholder="t('agent.taskGoalPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('agent.allowedTools')">
          <el-select v-model="tools" multiple filterable :placeholder="t('agent.selectTools')" style="width:100%">
            <el-option v-for="tool in availableTools" :key="tool.name"
                        :label="tool.displayName + ' (' + tool.name + ')'" :value="tool.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="running" @click="run">🚀 {{ t('agent.execute') }}</el-button>
          <el-button @click="reset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="steps.length" class="timeline-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('agent.thinkingProcess') }}</span>
          <div>
            <el-tag>{{ t('agent.rounds') }}: {{ rounds }}</el-tag>
            <el-tag type="success" style="margin-left:8px">{{ t('agent.tools') }}: {{ toolsUsed.join(', ') || t('common.none') }}</el-tag>
            <el-tag type="info" style="margin-left:8px">{{ t('agent.duration') }}: {{ duration }}ms</el-tag>
          </div>
        </div>
      </template>

      <el-timeline>
        <el-timeline-item v-for="(step, i) in steps" :key="i"
                          :type="stepIconType(step)" :timestamp="`Round ${step.round}`"
                          placement="top">
          <el-card shadow="hover" class="step-card">
            <div class="step-header">
              <span class="step-title">{{ step.action }}</span>
              <span v-if="step.durationMs" class="step-time">{{ step.durationMs }}ms</span>
            </div>
            <div v-if="step.thinking" class="thinking">
              <strong>💭 Thought:</strong> {{ step.thinking }}
            </div>
            <div v-if="step.arguments" class="args">
              <strong>📥 Args:</strong>
              <pre>{{ step.arguments }}</pre>
            </div>
            <div v-if="step.observation" class="obs">
              <strong>👁️ Observation:</strong>
              <pre>{{ step.observation }}</pre>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card v-if="finalAnswer" class="answer-card">
      <template #header>
        <span>✨ {{ t('agent.finalAnswer') }}</span>
      </template>
      <div class="final-answer">
        <MarkdownView :content="finalAnswer" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, onMounted } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'
import MarkdownView from '@/components/MarkdownView.vue'
import { t } from '@/i18n'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const goal = ref('查一下北京今天的天气, 然后给出一个旅游建议')
const tools = ref<string[]>([])
const running = ref(false)
const steps = ref<any[]>([])
const rounds = ref(0)
const toolsUsed = ref<string[]>([])
const duration = ref(0)
const finalAnswer = ref('')
const availableTools = ref<any[]>([])

async function loadTools() {
  try {
    const r = await http.get('/function/tools')
    availableTools.value = ((r?.data || []) as any[]).filter((t: any) => t.enabled)
  } catch (e) {
    // ignore - 可用工具列表不影响运行
    availableTools.value = [
      { name: 'get_time', displayName: '获取时间' },
      { name: 'calculator', displayName: '计算器' },
      { name: 'http_get', displayName: 'HTTP 请求' },
      { name: 'random_number', displayName: '随机数' },
    ]
  }
}

async function run() {
  if (!goal.value.trim()) {
    ElMessage.warning(t('agent.enterGoal'))
    return
  }
  running.value = true
  steps.value = []
  finalAnswer.value = ''
  try {
    const userId = userStore.profile?.id || 1
    const r = await http.post('/agent/run', {
      userId,
      goal: goal.value,
      tools: tools.value,
    })
    const result = r?.data
    if (!result) {
      ElMessage.error(t('agent.execFailed') + (r?.message || t('agent.noResponse')))
      return
    }
    steps.value = result.steps || []
    rounds.value = result.rounds
    toolsUsed.value = Array.from(result.toolsUsed || [])
    duration.value = result.durationMs
    finalAnswer.value = result.success
      ? result.answer
      : '❌ ' + (result.answer || '执行失败')
  } catch (e: any) {
    ElMessage.error(t('agent.requestFailed') + (e?.message || ''))
  } finally {
    running.value = false
  }
}

function reset() {
  steps.value = []
  finalAnswer.value = ''
  rounds.value = 0
  toolsUsed.value = []
  duration.value = 0
}

function stepIconType(step: any) {
  if (step.action?.startsWith('call:')) return 'primary'
  if (step.action === 'FinalAnswer') return 'success'
  if (step.action === 'stalled') return 'warning'
  return 'info'
}

onMounted(loadTools)
</script>

<style scoped>
.agent-container { padding: 20px; max-width: 1100px; margin: 0 auto; }
.agent-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.input-card, .timeline-card, .answer-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.step-card { background: #fafbfc; }
.step-header { display:flex; justify-content:space-between; align-items:center; margin-bottom: 8px; }
.step-title { font-weight: 600; color: #409eff; }
.step-time { color: #909399; font-size: 12px; }
.thinking, .args, .obs { padding: 8px 12px; margin-top: 8px; border-radius: 4px; }
.thinking { background: #f0f9ff; border-left: 3px solid #409eff; }
.args { background: #fdf6ec; border-left: 3px solid #e6a23c; }
.obs { background: #f0f9eb; border-left: 3px solid #67c23a; }
pre { margin: 4px 0 0; white-space: pre-wrap; word-break: break-all; font-size: 13px; }
.final-answer { font-size: 15px; line-height: 1.8; }
</style>
