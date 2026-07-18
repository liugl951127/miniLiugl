<!--
  @file views/agent/Stream.vue (AI 流式对话)
  @version V3.5.12+ (前端注释补全)
  @description AI 流式对话
-->
<template>
  <div class="agent-stream">
    <div class="header">
      <h1>🤖 Agent 流式执行 <span class="badge">V5.16</span></h1>
      <p class="sub">实时看到 Agent 思考过程 / 工具调用 / 步骤演进</p>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card class="input-card">
          <el-form>
            <el-form-item label="目标">
              <el-input v-model="goal" type="textarea" :rows="3"
                placeholder="例如: 查明天上海天气, 算 (123+456)*789, 发邮件给我" />
            </el-form-item>
            <el-form-item label="允许工具">
              <el-select v-model="tools" multiple filterable placeholder="选择工具 (留空 = 全部)" style="width:100%">
                <el-option v-for="tool in availableTools" :key="tool.name"
                  :label="tool.displayName + ' (' + tool.name + ')'" :value="tool.name" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-radio-group v-model="mode" style="margin-right:12px">
                <el-radio-button label="stream">🚀 流式执行</el-radio-button>
                <el-radio-button label="plan">📋 Plan 模式</el-radio-button>
                <el-radio-button label="memory">🧠 带记忆</el-radio-button>
              </el-radio-group>
              <el-button type="primary" :loading="running" @click="execute">▶ 执行</el-button>
              <el-button v-if="running" @click="cancel">⏹ 停止</el-button>
              <el-button @click="reset">🔄 重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="timeline-card" v-if="events.length || planSteps.length">
          <template #header>
            <span>📊 实时事件流</span>
            <el-tag style="margin-left:12px">事件: {{ events.length }}</el-tag>
            <el-tag v-if="rounds" type="success" style="margin-left:8px">轮次: {{ rounds }}</el-tag>
            <el-tag v-if="duration" type="info" style="margin-left:8px">耗时: {{ duration }}ms</el-tag>
          </template>

          <el-timeline>
            <el-timeline-item v-for="(ev, i) in events" :key="i"
              :type="eventTypeColor(ev.event)" :timestamp="ev.event" placement="top">
              <div class="event-row">
                <pre v-if="ev.data" class="event-data">{{ formatData(ev) }}</pre>
              </div>
            </el-timeline-item>
          </el-timeline>

          <div v-if="finalAnswer" class="final-box">
            <h3>✅ 最终答案</h3>
            <div class="final-content">{{ finalAnswer }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <!-- Plan 模式: 步骤编辑 -->
        <el-card v-if="mode === 'plan'">
          <template #header>
            <span>📋 Plan 步骤</span>
            <el-button v-if="planSteps.length" type="primary" size="small" style="float:right" @click="runPlan">▶ 执行 Plan</el-button>
          </template>
          <div v-if="!planSteps.length" class="empty">
            点击"执行"生成 Plan, 用户确认后再执行
          </div>
          <el-input v-for="(s, i) in planSteps" :key="i" v-model="planSteps[i]"
            style="margin-bottom:8px" type="textarea" :rows="2" :placeholder="`步骤 ${i+1}`" />
        </el-card>

        <!-- 工具列表 -->
        <el-card v-if="availableTools.length">
          <template #header>🛠 可用工具 ({{ availableTools.length }})</template>
          <div v-for="t in availableTools" :key="t.name" class="tool-row">
            <strong>{{ t.name }}</strong>
            <div class="tool-desc">{{ t.description || t.displayName }}</div>
          </div>
        </el-card>

        <!-- 调试: 原始事件流 -->
        <el-card v-if="events.length">
          <template #header>📜 原始事件 (raw JSON)</template>
          <pre class="raw">{{ JSON.stringify(events.slice(-10), null, 2) }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const goal = ref('查明天上海天气, 然后用计算器算 (123+456)*789, 最后把结果发邮件给 admin@minimax.com')
const tools = ref([])
const availableTools = ref([])
const running = ref(false)
const mode = ref('stream')
const events = ref([])
const planSteps = ref([])
const finalAnswer = ref('')
const rounds = ref(0)
const duration = ref(0)
const userStore = useUserStore()
let esController = new AbortController()

async function loadTools() {
  try {
    const r = await http.get('/agent/plugins')
    if (r && r.data) availableTools.value = r.data
  } catch (e) {
    availableTools.value = [
      { name: 'get_current_time', displayName: '获取时间', description: '获取当前时间' },
      { name: 'calculator', displayName: '计算器', description: '数学计算' },
      { name: 'http_get', displayName: 'HTTP', description: 'HTTP 请求' },
      { name: 'send_email', displayName: '邮件', description: '发邮件' },
    ]
  }
}

function formatData(ev) {
  const d = ev.data || {}
  if (ev.event === 'thought') return `💭 ${d.thought || d.message}`
  if (ev.event === 'tool-call') return `🔧 ${d.tool}(${truncate(d.arguments, 60)})`
  if (ev.event === 'observation') return `📥 ${truncate(d.observation, 200)}`
  if (ev.event === 'final') return `✅ ${d.answer}`
  if (ev.event === 'done') return `${d.success ? '🎉' : '⚠️'} 轮次=${d.rounds} 工具=${(d.toolsUsed || []).join(',')} 耗时=${d.durationMs}ms`
  if (ev.event === 'start') return `🎯 目标: ${d.goal}  (maxRounds=${d.maxRounds})`
  if (ev.event === 'tools') return `🛠 ${(d.tools || []).map(t => t.name).join(', ')}`
  return JSON.stringify(d, null, 2).substring(0, 200)
}

function eventTypeColor(t) {
  if (t === 'thought') return 'primary'
  if (t === 'tool-call') return 'warning'
  if (t === 'observation') return 'success'
  if (t === 'final' || t === 'done') return 'success'
  if (t === 'error') return 'danger'
  return 'info'
}

function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.substring(0, n) + '...' : s
}

async function execute() {
  if (!goal.value.trim()) return ElMessage.warning('请输入目标')
  reset()

  if (mode.value === 'plan') return runPlanMode()
  if (mode.value === 'memory') return runWithMemory()

  // 流式模式
  await runStreamMode()
}

async function runStreamMode() {
  running.value = true
  esController = new AbortController()
  try {
    // V5.22: 用 userStore.accessToken 代替 localStorage
    const token = userStore.accessToken || ''
    const resp = await fetch('/api/v1/agent/run-stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify({
        userId: userStore.profile?.id || 1,
        goal: goal.value,
        tools: tools.value.length ? tools.value : null,
      }),
    })

    if (!resp.ok) {
      ElMessage.error('启动失败: ' + resp.status)
      running.value = false
      return
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 解析 SSE: event: ... \n data: ... \n\n
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let curEvent = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          curEvent = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (curEvent && data) {
            try {
              const parsed = JSON.parse(data)
              events.value.push({ event: curEvent, data: parsed })
              if (curEvent === 'final') finalAnswer.value = parsed.answer
              if (curEvent === 'done') {
                rounds.value = parsed.rounds
                duration.value = parsed.durationMs
              }
            } catch (e) {
              events.value.push({ event: curEvent, data: { raw: data } })
            }
            curEvent = ''
          }
        }
      }
    }
  } catch (e) {
    ElMessage.error('流式执行失败: ' + e.message)
  } finally {
    running.value = false
  }
}

async function runPlanMode() {
  running.value = true
  try {
    const r = await http.post('/agent/plan', {
      userId: userStore.profile?.id || 1,
      goal: goal.value,
    })
    if (r && r.data) {
      planSteps.value = r.data
      ElMessage.success(`已生成 ${planSteps.value.length} 步 Plan`)
    }
  } catch (e) {
    ElMessage.error('生成 Plan 失败: ' + e.message)
  } finally {
    running.value = false
  }
}

async function runPlan() {
  if (!planSteps.value.length) return ElMessage.warning('Plan 为空')
  running.value = true
  events.value = []
  try {
    const r = await http.post('/agent/run-plan', {
      userId: userStore.profile?.id || 1,
      goal: goal.value,
      planSteps: planSteps.value,
    })
    if (r && r.data) {
      finalAnswer.value = r.data.answer
      rounds.value = r.data.rounds
      duration.value = r.data.durationMs
      ElMessage.success('Plan 执行完成')
    }
  } catch (e) {
    ElMessage.error('Plan 执行失败: ' + e.message)
  } finally {
    running.value = false
  }
}

async function runWithMemory() {
  running.value = true
  events.value = []
  try {
    const r = await http.post('/agent/run-with-memory', {
      userId: userStore.profile?.id || 1,
      goal: goal.value,
      tools: tools.value.length ? tools.value : null,
    })
    if (r && r.data) {
      finalAnswer.value = r.data.answer
      rounds.value = r.data.rounds
      duration.value = r.data.durationMs
      ElMessage.success('带记忆执行完成')
    }
  } catch (e) {
    ElMessage.error('带记忆执行失败: ' + e.message)
  } finally {
    running.value = false
  }
}

function cancel() {
  if (esController) {
    esController.abort()
    esController = new AbortController()
  }
  running.value = false
  ElMessage.info('已停止')
}

function reset() {
  events.value = []
  planSteps.value = []
  finalAnswer.value = ''
  rounds.value = 0
  duration.value = 0
}

onMounted(loadTools)
</script>

<style scoped>
.agent-stream { padding: 16px; }
.header h1 { margin: 0; }
.header .sub { color: #6b7280; margin: 4px 0 16px 0; font-size: 13px; }
.badge { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-left: 8px; }
.event-row { font-family: monospace; }
.event-data { background: #f9fafb; padding: 8px; border-radius: 4px; font-size: 12px; margin: 0; white-space: pre-wrap; }
.tool-row { padding: 6px 0; border-bottom: 1px solid #f3f4f6; }
.tool-desc { color: #6b7280; font-size: 12px; margin-top: 2px; }
.empty { color: #9ca3af; padding: 20px; text-align: center; }
.final-box { margin-top: 16px; padding: 16px; background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 100%);
  border-radius: 8px; border-left: 4px solid #10b981; }
.final-content { color: #064e3b; white-space: pre-wrap; line-height: 1.6; }
.raw { max-height: 200px; overflow: auto; background: #f9fafb; padding: 8px;
  font-size: 11px; border-radius: 4px; margin: 0; }
</style>
