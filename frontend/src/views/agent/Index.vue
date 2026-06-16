<template>
  <div class="agent-container">
    <div class="agent-header">
      <h1>🤖 Agent 自主任务 <span class="badge">V2.0</span></h1>
      <p class="sub">给 Agent 一个目标, 它会自主规划 → 调工具 → 反思 → 给出最终答案</p>
    </div>

    <el-card class="input-card">
      <el-form>
        <el-form-item label="任务目标">
          <el-input
            v-model="goal"
            type="textarea"
            :rows="3"
            placeholder="例: 查一下北京今天的天气, 算出明天温差, 然后发邮件给张伟"
          />
        </el-form-item>
        <el-form-item label="允许使用的工具 (留空 = 全部)">
          <el-select v-model="tools" multiple filterable placeholder="选择工具" style="width:100%">
            <el-option v-for="t in availableTools" :key="t.name"
                        :label="t.displayName + ' (' + t.name + ')'" :value="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="running" @click="run">🚀 执行</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="steps.length" class="timeline-card">
      <template #header>
        <div class="card-header">
          <span>📋 思考过程 (ReAct 循环)</span>
          <div>
            <el-tag>轮数: {{ rounds }}</el-tag>
            <el-tag type="success" style="margin-left:8px">工具: {{ toolsUsed.join(', ') || '无' }}</el-tag>
            <el-tag type="info" style="margin-left:8px">耗时: {{ duration }}ms</el-tag>
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
        <span>✨ 最终答案</span>
      </template>
      <div class="final-answer">
        <MarkdownView :content="finalAnswer" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import MarkdownView from '@/components/MarkdownView.vue'

const goal = ref('查一下北京今天的天气, 然后给出一个旅游建议')
const tools = ref<string[]>([])
const running = ref(false)
const steps = ref<any[]>([])
const rounds = ref(0)
const toolsUsed = ref<string[]>([])
const duration = ref(0)
const finalAnswer = ref('')
const availableTools = ref<any[]>([])

const API = import.meta.env.VITE_API_BASE || 'http://localhost'

async function loadTools() {
  try {
    const { data } = await axios.get(`${API}/api/v1/function/tools`)
    availableTools.value = (data?.data || []).filter((t: any) => t.enabled)
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
    ElMessage.warning('请输入任务目标')
    return
  }
  running.value = true
  steps.value = []
  finalAnswer.value = ''
  try {
    const token = localStorage.getItem('access_token')
    const userId = localStorage.getItem('user_id') || '1'
    const { data } = await axios.post(`${API}/api/v1/agent/run`,
      { userId, goal: goal.value, tools: tools.value },
      { headers: { Authorization: `Bearer ${token}` } })
    const result = data?.data
    if (!result) {
      ElMessage.error('执行失败: ' + (data?.message || '无响应'))
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
    ElMessage.error('请求失败: ' + (e?.response?.data?.message || e?.message))
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
