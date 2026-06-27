<template>
  <div class="multi-agent">
    <div class="header">
      <h1>🧠 多智能体协作 <span class="badge">V5.17</span></h1>
      <p class="sub">Planner 规划 → Executor 执行 → Critic 评估 (3 角色协作, 失败自动重规划)</p>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card class="input-card">
          <el-form>
            <el-form-item label="目标">
              <el-input v-model="goal" type="textarea" :rows="4"
                placeholder="例如: 给我公司生成一份竞品分析报告 (含 3 个核心竞品, 各自优劣势)" />
            </el-form-item>
            <el-form-item label="允许工具">
              <el-select v-model="tools" multiple filterable placeholder="选择工具 (留空全部)" style="width:100%">
                <el-option v-for="t in availableTools" :key="t.name"
                  :label="t.name" :value="t.name" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-radio-group v-model="mode" style="margin-right:12px">
                <el-radio-button label="stream">🚀 流式</el-radio-button>
                <el-radio-button label="sync">📦 同步</el-radio-button>
              </el-radio-group>
              <el-button type="primary" :loading="running" @click="execute">▶ 执行</el-button>
              <el-button v-if="running" @click="cancel">⏹ 停止</el-button>
              <el-button @click="reset">🔄 重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-if="finalAnswer" class="final-card">
          <template #header>
            <span>✅ 最终答案</span>
            <el-tag v-if="criticPassed" type="success" style="margin-left:8px">Critic 通过</el-tag>
            <el-tag v-else type="warning" style="margin-left:8px">Critic 未通过 (max 3 轮)</el-tag>
            <el-tag style="margin-left:8px">轮次: {{ rounds }}</el-tag>
            <el-tag type="info" style="margin-left:8px">耗时: {{ totalDurationMs }}ms</el-tag>
          </template>
          <div class="final-content">{{ finalAnswer }}</div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <!-- 3 角色实时展示 -->
        <el-card>
          <template #header>
            <span>🎭 3 角色协作过程</span>
          </template>

          <div v-for="(round, ri) in roundsData" :key="ri" class="round-block">
            <h3>第 {{ ri + 1 }} 轮 (Critic Round)</h3>

            <!-- Planner -->
            <div class="role-card planner">
              <div class="role-head">
                <span class="role-icon">📋</span>
                <strong>Planner 规划师</strong>
                <span class="role-tag" v-if="round.plan">已生成 {{ round.plan.length }} 步</span>
              </div>
              <div v-if="round.plan && round.plan.length" class="plan-list">
                <div v-for="(s, i) in round.plan" :key="i" class="plan-item">
                  <span class="step-num">{{ i + 1 }}</span>
                  <span>{{ s }}</span>
                </div>
              </div>
            </div>

            <!-- Executor -->
            <div v-if="round.results && round.results.length" class="role-card executor">
              <div class="role-head">
                <span class="role-icon">⚡</span>
                <strong>Executor 执行者</strong>
                <span class="role-tag">{{ round.results.length }} 步执行</span>
              </div>
              <div v-for="(r, i) in round.results" :key="i" class="exec-item">
                <div class="exec-goal">【{{ i + 1 }}】{{ r.goal }}</div>
                <div class="exec-obs">→ {{ r.observation }}</div>
                <div class="exec-time" v-if="r.durationMs">{{ r.durationMs }}ms</div>
              </div>
            </div>

            <!-- Critic -->
            <div v-if="round.critic" class="role-card critic" :class="{ passed: round.critic.passed }">
              <div class="role-head">
                <span class="role-icon">🔍</span>
                <strong>Critic 评估者</strong>
                <el-tag :type="round.critic.passed ? 'success' : 'danger'" size="small">
                  {{ round.critic.passed ? '✓ 通过' : '✗ 不通过' }} ({{ round.critic.score }}/10)
                </el-tag>
              </div>
              <div v-if="round.critic.feedback" class="critic-feedback">
                <strong>反馈:</strong> {{ round.critic.feedback }}
              </div>
            </div>
          </div>
        </el-card>

        <!-- 原始事件 -->
        <el-card v-if="rawEvents.length" style="margin-top:16px">
          <template #header>📜 原始事件流 (raw, last 10)</template>
          <pre class="raw">{{ JSON.stringify(rawEvents.slice(-10), null, 2) }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const goal = ref('给我公司生成一份 MiniMax 平台的竞品分析报告, 包含 3 个核心竞品的功能对比、优劣势分析、市场定位')
const tools = ref([])
const availableTools = ref([])
const running = ref(false)
const mode = ref('stream')
const finalAnswer = ref('')
const rounds = ref(0)
const criticPassed = ref(false)
const totalDurationMs = ref(0)
const roundsData = ref([])  // [{plan, results, critic}]
const rawEvents = ref([])

async function loadTools() {
  try {
    const r = await http.get('/agent/plugins')
    if (r && r.data) availableTools.value = r.data
  } catch (e) {
    availableTools.value = [
      { name: 'get_current_time' },
      { name: 'calculator' },
      { name: 'http_get' },
    ]
  }
}

function reset() {
  finalAnswer.value = ''
  rounds.value = 0
  criticPassed.value = false
  totalDurationMs.value = 0
  roundsData.value = []
  rawEvents.value = []
}

async function execute() {
  if (!goal.value.trim()) return ElMessage.warning('请输入目标')
  reset()

  if (mode.value === 'sync') {
    return runSync()
  }
  await runStream()
}

async function runSync() {
  running.value = true
  try {
    const r = await http.post('/agent/multi/run', {
      userId: userStore.profile?.id || 1,
      goal: goal.value,
      tools: tools.value,
    })
    if (r && r.data) {
      const d = r.data
      finalAnswer.value = d.finalAnswer
      rounds.value = d.rounds
      criticPassed.value = d.criticPassed
      totalDurationMs.value = d.totalDurationMs
      // 把 steps/criticEvals 转成 roundsData
      rebuildRoundsData(d.steps || [], d.criticEvals || [])
    }
  } catch (e) {
    ElMessage.error('执行失败: ' + e.message)
  } finally {
    running.value = false
  }
}

function rebuildRoundsData(steps, criticEvals) {
  const byRound = {}
  for (const s of steps) {
    if (!byRound[s.criticRound]) byRound[s.criticRound] = { results: [] }
    byRound[s.criticRound].results.push(s)
  }
  for (const c of criticEvals) {
    if (!byRound[c.round]) byRound[c.round] = {}
    byRound[c.round].critic = c
  }
  // plan 需要从 events 里恢复, 这里没存, 显示空
  roundsData.value = Object.keys(byRound).sort((a, b) => a - b).map(k => byRound[k])
}

async function runStream() {
  running.value = true
  roundsData.value = []  // 实时追加
  let currentRound = null
  try {
    const resp = await fetch('/api/v1/agent/multi/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + (userStore.accessToken || ''),
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify({
        userId: userStore.profile?.id || 1,
        goal: goal.value,
        tools: tools.value,
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
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let curEvent = ''
      for (const line of lines) {
        if (line.startsWith('event:')) curEvent = line.substring(6).trim()
        else if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (curEvent && data) {
            try {
              const parsed = JSON.parse(data)
              rawEvents.value.push({ event: curEvent, data: parsed })
              applyEvent(curEvent, parsed)
            } catch (e) {}
            curEvent = ''
          }
        }
      }
    }
  } catch (e) {
    ElMessage.error('流式失败: ' + e.message)
  } finally {
    running.value = false
  }
}

function applyEvent(event, data) {
  if (event === 'planner-start') {
    // 新一轮开始
    currentRound = { results: [], critic: null, plan: [] }
    roundsData.value.push(currentRound)
  } else if (event === 'planner-plan') {
    if (currentRound) currentRound.plan = data.steps || []
  } else if (event === 'executor-step') {
    // 不立即插入, 等 executor-result
  } else if (event === 'executor-result') {
    if (currentRound) {
      currentRound.results.push({
        goal: '(从事件恢复)',
        observation: data.observation,
        durationMs: data.durationMs,
      })
    }
  } else if (event === 'critic-result') {
    if (currentRound) {
      currentRound.critic = { passed: data.passed, score: data.score, feedback: data.feedback }
    }
  } else if (event === 'final') {
    finalAnswer.value = data.answer
  } else if (event === 'done') {
    criticPassed.value = data.criticPassed
    rounds.value = data.rounds
    totalDurationMs.value = data.totalDurationMs
  } else if (event === 'error') {
    ElMessage.error('错误: ' + (data.message || ''))
  }
}

function cancel() { running.value = false; ElMessage.info('已停止') }

onMounted(loadTools)
</script>

<style scoped>
.multi-agent { padding: 16px; }
.header h1 { margin: 0; }
.header .sub { color: #6b7280; margin: 4px 0 16px 0; font-size: 13px; }
.badge { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-left: 8px; }

.round-block { margin-bottom: 20px; padding: 12px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 8px; border: 1px solid #e5e7eb; }
.round-block h3 { margin: 0 0 12px 0; color: #4f46e5; font-size: 14px; }

.role-card { background: #fff; border-radius: 6px; padding: 10px; margin-bottom: 8px;
  border-left: 3px solid #6366f1; }
.role-card.planner { border-left-color: #3b82f6; }
.role-card.executor { border-left-color: #10b981; }
.role-card.critic { border-left-color: #f59e0b; }
.role-card.critic.passed { border-left-color: #10b981; background: #f0fdf4; }
.role-card.critic:not(.passed) { border-left-color: #ef4444; background: #fef2f2; }

.role-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.role-icon { font-size: 18px; }
.role-tag { font-size: 12px; color: #6b7280; margin-left: auto; }

.plan-item { display: flex; gap: 8px; padding: 4px 0; font-size: 13px; }
.step-num { background: #6366f1; color: #fff; border-radius: 50%;
  width: 22px; height: 22px; display: flex; align-items: center; justify-content: center;
  font-size: 12px; flex-shrink: 0; }

.exec-item { padding: 6px 8px; background: #f9fafb; border-radius: 4px; margin: 4px 0; font-size: 13px; }
.exec-goal { font-weight: 500; color: #4b5563; }
.exec-obs { color: #059669; margin-top: 2px; }
.exec-time { color: #9ca3af; font-size: 11px; margin-top: 2px; }

.critic-feedback { padding: 8px; background: rgba(255,255,255,0.6); border-radius: 4px;
  font-size: 13px; color: #4b5563; }

.final-card { background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 100%); }
.final-content { white-space: pre-wrap; line-height: 1.6; color: #064e3b; }
.raw { max-height: 200px; overflow: auto; background: #f9fafb; padding: 8px;
  font-size: 11px; border-radius: 4px; margin: 0; }
</style>
