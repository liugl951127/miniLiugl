<!-- @file agent/Multi.vue - 多智能体协作 V6.9 (Planner + Executor + Critic) -->
<template>
  <div class="multi-page">

    <!-- ====== 顶部工具栏 ====== -->
    <div class="topbar">
      <div class="topbar-left">
        <span class="topbar-title">🤖 多智能体协作</span>
        <el-tag v-if="phase" :type="phaseTagType" size="small">{{ phaseLabel }}</el-tag>
        <el-tag v-if="running" type="warning" size="small" effect="plain" style="animation:pulse 1.5s infinite">
          ● 运行中 · {{ round }} / {{ maxRounds }} 轮
        </el-tag>
      </div>
      <div class="topbar-right">
        <el-button size="small" :disabled="running" @click="switchTab('collab')" :type="tab==='collab'?'primary':''">
          🔀 协作模式
        </el-button>
        <el-button size="small" :disabled="running" @click="switchTab('canvas')" :type="tab==='canvas'?'primary':''">
          🎨 画布模式
        </el-button>
        <el-button size="small" @click="clearLog" :disabled="running">🗑️ 清空</el-button>
        <el-button size="small" type="info" :disabled="!history.length" @click="switchTab('history')">
          📋 历史 {{ history.length }}
        </el-button>
      </div>
    </div>

    <!-- ====== 历史记录视图 ====== -->
    <div v-if="tab==='history'" class="history-panel">
      <div v-if="!history.length" class="empty-hint">暂无执行历史</div>
      <div v-for="(h, i) in history" :key="i" class="history-card" @click="loadHistory(h)">
        <div class="history-goal">{{ h.goal }}</div>
        <div class="history-meta">
          <el-tag size="small" :type="h.criticPassed?'success':'danger'">
            {{ h.criticPassed ? '✅ 通过' : '⚠️ 未通过' }}
          </el-tag>
          <span class="meta-info">{{ h.rounds }} 轮</span>
          <span class="meta-info">{{ (h.durationMs/1000).toFixed(1) }}s</span>
          <span class="meta-info">{{ h.time }}</span>
        </div>
      </div>
    </div>

    <!-- ====== 协作模式：三面板 ====== -->
    <div v-if="tab==='collab'" class="collab-body">

      <!-- 左：角色配置 -->
      <div class="panel panel-left">
        <div class="panel-title">⚙️ 协作配置</div>

        <el-form label-width="80px" size="small">

          <el-form-item label="目标任务">
            <el-input v-model="form.goal" type="textarea" :rows="4"
              placeholder="描述你想达成的目标，越具体越好…" />
          </el-form-item>

          <el-form-item label="协作模型">
            <el-select v-model="form.model" style="width:100%">
              <el-option label="MiniMax-Text-01" value="MiniMax-Text-01" />
              <el-option label="GPT-4o" value="gpt-4o" />
              <el-option label="DeepSeek-V3" value="deepseek-chat" />
            </el-select>
          </el-form-item>

          <el-form-item label="最大轮次">
            <el-slider v-model="form.maxRounds" :min="1" :max="5" :step="1"
              show-stops show-input style="width:100%" />
          </el-form-item>

          <el-form-item label="可用工具">
            <el-checkbox-group v-model="form.tools">
              <el-checkbox label="web-search">🌐 网页搜索</el-checkbox>
              <el-checkbox label="calculator">🔢 计算器</el-checkbox>
              <el-checkbox label="code-interpreter">💻 代码执行</el-checkbox>
              <el-checkbox label="file-read">📄 文件读取</el-checkbox>
              <el-checkbox label="file-write">✏️ 文件写入</el-checkbox>
              <el-checkbox label="web-fetch">🌍 网页抓取</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <!-- 角色能力预览 -->
          <el-form-item label="角色说明">
            <div class="role-hints">
              <div class="role-hint planner">
                <span class="role-icon">🧠</span>
                <div>
                  <div class="role-name">Planner 规划师</div>
                  <div class="role-desc">将目标拆解为 3-7 步执行计划</div>
                </div>
              </div>
              <div class="role-hint executor">
                <span class="role-icon">⚡</span>
                <div>
                  <div class="role-name">Executor 执行者</div>
                  <div class="role-desc">逐个执行子任务，整合执行结果</div>
                </div>
              </div>
              <div class="role-hint critic">
                <span class="role-icon">🔍</span>
                <div>
                  <div class="role-name">Critic 评估者</div>
                  <div class="role-desc">评估结果质量，不通过则触发重规划</div>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button v-if="!running" type="primary" style="width:100%" @click="startMulti">
              ▶️ 启动多智能体协作
            </el-button>
            <el-button v-else type="danger" style="width:100%" @click="stopMulti">
              ⏹ 停止执行
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 中：实时执行日志 -->
      <div class="panel panel-center">
        <div class="panel-title">
          📊 执行日志
          <span class="log-count">{{ logEntries.length }} 条</span>
        </div>
        <div class="log-container" ref="logContainer">
          <div v-for="(entry, i) in logEntries" :key="i"
            class="log-entry" :class="`log-${entry.type}`">
            <div class="log-header">
              <span class="log-badge" :class="`badge-${entry.type}`">
                {{ eventIcon(entry.type) }} {{ eventLabel(entry.type) }}
              </span>
              <span v-if="entry.round" class="log-round">轮次 {{ entry.round }}</span>
              <span v-if="entry.durationMs" class="log-dur">{{ entry.durationMs }}ms</span>
              <span class="log-ts">{{ entry.ts }}</span>
            </div>
            <div class="log-body">
              <!-- planner-start / planner-plan -->
              <template v-if="entry.type === 'planner-start'">
                <div class="log-msg">🎯 Planner 开始规划
                  <span v-if="entry.feedback" class="log-feedback">（采纳反馈：{{ entry.feedback?.slice(0,60) }}…）</span>
                </div>
              </template>
              <template v-else-if="entry.type === 'planner-plan'">
                <div class="log-plan-list">
                  <div v-for="(step, si) in entry.steps" :key="si" class="log-plan-step">
                    <span class="step-num">{{ si + 1 }}</span>
                    <span>{{ step }}</span>
                  </div>
                </div>
              </template>
              <!-- executor -->
              <template v-else-if="entry.type === 'executor-step'">
                <div class="log-msg">⚡ 执行第 {{ entry.step }} 步：{{ entry.goal }}</div>
              </template>
              <template v-else-if="entry.type === 'executor-result'">
                <div class="log-obs" :title="entry.observation">
                  {{ entry.observation?.slice(0, 200) }}{{ entry.observation?.length > 200 ? '…' : '' }}
                </div>
              </template>
              <!-- critic -->
              <template v-else-if="entry.type === 'critic-eval'">
                <div class="log-msg">🔍 Critic 正在评估 {{ entry.plan?.length }} 个步骤的结果…</div>
              </template>
              <template v-else-if="entry.type === 'critic-result'">
                <div class="critic-result">
                  <el-tag :type="entry.passed ? 'success' : 'danger'" size="small">
                    {{ entry.passed ? '✅ 通过' : '❌ 未通过' }}
                  </el-tag>
                  <span class="critic-score">评分 {{ entry.score }}/10</span>
                  <div v-if="entry.feedback" class="critic-feedback">{{ entry.feedback }}</div>
                </div>
              </template>
              <template v-else-if="entry.type === 'critic-retry'">
                <div class="log-msg warn">🔄 Critic 未通过，触发第 {{ entry.round + 1 }} 轮重规划</div>
              </template>
              <!-- final / done -->
              <template v-else-if="entry.type === 'final'">
                <div class="log-final">
                  <div class="final-label">🎉 最终答案（第 {{ entry.rounds }} 轮）</div>
                  <div class="final-answer">{{ entry.answer }}</div>
                </div>
              </template>
              <template v-else-if="entry.type === 'done'">
                <div class="log-msg" :class="entry.success ? 'success' : 'error'">
                  {{ entry.success ? '✅ 协作完成' : '❌ 执行异常' }}
                  · {{ (entry.totalDurationMs/1000).toFixed(1) }}s
                </div>
              </template>
              <template v-else-if="entry.type === 'error'">
                <div class="log-msg error">⚠️ {{ entry.message }}</div>
              </template>
              <template v-else-if="entry.type === 'multi-agent-start'">
                <div class="log-msg info">🚀 多智能体协作启动 | 目标：{{ entry.goal }}</div>
              </template>
              <!-- fallback -->
              <template v-else>
                <div class="log-msg">{{ JSON.stringify(entry.raw).slice(0, 120) }}</div>
              </template>
            </div>
          </div>
          <div v-if="running && !logEntries.length" class="log-empty">
            <span>⏳ 等待连接…</span>
          </div>
        </div>
      </div>

      <!-- 右：Critic 评分 + 统计 -->
      <div class="panel panel-right">
        <div class="panel-title">🔍 评估报告</div>

        <!-- 总体评分卡片 -->
        <div class="eval-summary" v-if="currentEval">
          <div class="eval-status" :class="currentEval.passed ? 'passed' : 'failed'">
            <div class="eval-big-score">{{ currentEval.score }}</div>
            <div class="eval-max">/ 10</div>
          </div>
          <div class="eval-badges">
            <el-tag :type="currentEval.passed ? 'success' : 'danger'" size="small">
              {{ currentEval.passed ? '✅ 质量通过' : '❌ 需改进' }}
            </el-tag>
            <el-tag type="info" size="small">{{ completedRounds }} / {{ form.maxRounds }} 轮</el-tag>
          </div>
          <div v-if="currentEval.feedback" class="eval-feedback">
            <div class="eval-feedback-label">💬 Critic 反馈</div>
            <div class="eval-feedback-text">{{ currentEval.feedback }}</div>
          </div>
        </div>

        <!-- 评分历史 -->
        <div class="eval-history" v-if="evalHistory.length">
          <div class="eval-history-label">📈 各轮评分</div>
          <div v-for="(ev, i) in evalHistory" :key="i" class="eval-history-item">
            <span class="ev-round">R{{ ev.round }}</span>
            <div class="ev-bar-wrap">
              <div class="ev-bar" :style="{ width: ev.score * 10 + '%' }"
                :class="ev.passed ? 'bar-pass' : 'bar-fail'"></div>
            </div>
            <span class="ev-score">{{ ev.score }}</span>
            <span class="ev-icon">{{ ev.passed ? '✅' : '❌' }}</span>
          </div>
        </div>

        <!-- 执行统计 -->
        <div class="exec-stats" v-if="execStats">
          <div class="exec-stats-label">📊 执行统计</div>
          <div class="stat-row">
            <span>总耗时</span>
            <span>{{ (execStats.totalMs/1000).toFixed(1) }}s</span>
          </div>
          <div class="stat-row">
            <span>执行步骤</span>
            <span>{{ execStats.totalSteps }}</span>
          </div>
          <div class="stat-row">
            <span>最大轮次</span>
            <span>{{ execStats.maxRounds }}</span>
          </div>
          <div class="stat-row">
            <span>Critic 通过</span>
            <span>{{ execStats.criticPassed ? '是' : '否' }}</span>
          </div>
        </div>

        <!-- 步骤时间线 -->
        <div class="step-timeline" v-if="stepHistory.length">
          <div class="timeline-label">⏱️ 执行时间线</div>
          <div v-for="(s, i) in stepHistory" :key="i" class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-step">R{{ s.criticRound }}.{{ s.stepIndex }} {{ s.goal?.slice(0,30) }}…</div>
              <div class="timeline-dur">{{ s.durationMs }}ms</div>
            </div>
          </div>
        </div>

        <div v-if="!currentEval && !evalHistory.length" class="empty-hint">
          开始执行后评估报告将显示在这里
        </div>
      </div>
    </div>

    <!-- ====== 画布模式 ====== -->
    <div v-if="tab==='canvas'" class="canvas-mode">
      <div class="canvas-mode-hint">
        👇 从左侧拖拽 Agent 节点到画布，连接后点「执行」即可触发多智能体协作。
        <br>画布上的 Agent 节点将实时显示执行状态。
        <br><br>
        <el-button type="primary" @click="switchTab('collab')">← 切换到协作模式</el-button>
      </div>
      <!-- Canvas 占位：实际内容由 Canvas.vue 提供，此处引用 -->
      <div class="canvas-placeholder" @click="$emit('open-canvas')">
        <span>🎨 点击打开 Agent 画布编辑器</span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { multiAgentApi } from '@/api/agent'

const tab = ref('collab')  // 'collab' | 'canvas' | 'history'
const running = ref(false)
const phase = ref('')
const round = ref(0)
const logEntries = ref([])
const logContainer = ref(null)
const evalHistory = ref([])
const stepHistory = ref([])
const currentEval = ref(null)
const execStats = ref(null)
const history = ref([])
const abortController = ref(null)

// 表单
const form = reactive({
  goal: '',
  model: 'MiniMax-Text-01',
  maxRounds: 3,
  tools: ['web-search', 'calculator']
})

// ---------- 计算属性 ----------
const phaseTagType = computed(() => ({
  planner: 'primary', executor: 'warning', critic: 'danger', done: 'success'
}[phase.value] || 'info'))
const phaseLabel = computed(() => ({
  planner: '🧠 规划中', executor: '⚡ 执行中', critic: '🔍 评估中', done: '✅ 完成'
}[phase.value] || phase.value))
const completedRounds = computed(() => evalHistory.value.length)
const maxRounds = computed(() => form.maxRounds)

// ---------- 事件图标/标签 ----------
function eventIcon(type) {
  return { 'multi-agent-start': '🚀', 'planner-start': '🧠', 'planner-plan': '📋',
    'executor-step': '⚡', 'executor-result': '📥', 'critic-eval': '🔍',
    'critic-result': '✅', 'critic-retry': '🔄', 'final': '🎉', 'done': '🏁',
    'error': '⚠️' }[type] || '📌'
}
function eventLabel(type) {
  return { 'multi-agent-start': '启动', 'planner-start': 'Planner开始', 'planner-plan': 'Planner计划',
    'executor-step': 'Executor执行', 'executor-result': 'Executor结果', 'critic-eval': 'Critic评估',
    'critic-result': 'Critic结论', 'critic-retry': 'Critic重试', 'final': '最终答案',
    'done': '完成', 'error': '错误' }[type] || type
}

// ---------- 启动多智能体协作 ----------
let streamCleanup = null

async function startMulti() {
  if (!form.goal.trim()) { ElMessage.warning('请输入目标任务'); return }
  running.value = true
  phase.value = ''
  round.value = 0
  logEntries.value = []
  evalHistory.value = []
  stepHistory.value = []
  currentEval.value = null
  execStats.value = null

  abortController.value = new AbortController()

  try {
    await multiAgentApi.xhrStream(
      { goal: form.goal, tools: form.tools, maxRounds: form.maxRounds, model: form.model },
      handleSSEEvent
    )
  } catch (e) {
    if (e.name !== 'AbortError') {
      pushLog('error', { message: e.message || 'SSE 连接失败' })
    }
  } finally {
    running.value = false
  }
}

function stopMulti() {
  abortController.value?.abort()
  running.value = false
  pushLog('error', { message: '用户主动停止' })
}

// ---------- SSE 事件处理 ----------
function handleSSEEvent(eventName, data, raw) {
  const ts = new Date().toLocaleTimeString('zh-CN', { hour12: false })

  switch (eventName) {
    case 'multi-agent-start':
      pushLog('multi-agent-start', { ...data, ts })
      break

    case 'planner-start':
      phase.value = 'planner'
      round.value = data.round || 1
      pushLog('planner-start', { ...data, ts, type: 'planner-start' })
      break

    case 'planner-plan':
      pushLog('planner-plan', { ...data, ts, type: 'planner-plan' })
      break

    case 'executor-step':
      phase.value = 'executor'
      pushLog('executor-step', { ...data, ts, type: 'executor-step' })
      break

    case 'executor-result':
      pushLog('executor-result', { ...data, ts, type: 'executor-result' })
      // 记录到步骤历史
      if (data.round && data.step) {
        stepHistory.value.push({
          criticRound: data.round,
          stepIndex: data.step,
          goal: data.goal,
          observation: data.observation,
          durationMs: data.durationMs || 0
        })
      }
      break

    case 'critic-eval':
      phase.value = 'critic'
      pushLog('critic-eval', { ...data, ts, type: 'critic-eval' })
      break

    case 'critic-result':
      pushLog('critic-result', { ...data, ts, type: 'critic-result' })
      currentEval.value = { score: data.score, passed: data.passed, feedback: data.feedback }
      evalHistory.value.push({ round: data.round, score: data.score, passed: data.passed, feedback: data.feedback })
      break

    case 'critic-retry':
      pushLog('critic-retry', { ...data, ts, type: 'critic-retry' })
      break

    case 'final':
      phase.value = 'done'
      pushLog('final', { ...data, ts, type: 'final' })
      break

    case 'done':
      running.value = false
      execStats.value = { totalMs: data.totalDurationMs, totalSteps: stepHistory.value.length,
        maxRounds: data.rounds, criticPassed: data.criticPassed }
      pushLog('done', { ...data, ts, type: 'done' })
      // 写入历史
      history.value.unshift({
        goal: form.goal,
        rounds: data.rounds,
        criticPassed: data.criticPassed,
        totalDurationMs: data.totalDurationMs,
        time: ts,
        steps: [...stepHistory.value],
        evals: [...evalHistory.value],
        finalAnswer: data.answer
      })
      break

    case 'error':
      running.value = false
      pushLog('error', { ...data, ts, type: 'error' })
      ElMessage.error(data.message || '执行异常')
      break

    default:
      pushLog('message', { ...data, ts, type: eventName, raw })
  }

  // 自动滚动
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
}

function pushLog(type, data) {
  logEntries.value.push({ type, ...data, ts: data.ts || new Date().toLocaleTimeString('zh-CN', { hour12: false }) })
}

// ---------- 历史 ----------
function loadHistory(h) {
  form.goal = h.goal
  logEntries.value = []
  evalHistory.value = h.evals || []
  stepHistory.value = h.steps || []
  currentEval.value = h.evals?.length ? h.evals[h.evals.length - 1] : null
  execStats.value = { totalMs: h.totalDurationMs, totalSteps: h.steps?.length || 0,
    maxRounds: h.rounds, criticPassed: h.criticPassed }
  tab.value = 'collab'
}

// ---------- 工具 ----------
function clearLog() { logEntries.value = []; evalHistory.value = []; stepHistory.value = []; currentEval.value = null; execStats.value = null }
function switchTab(t) { tab.value = t }

onUnmounted(() => { abortController.value?.abort() })
</script>

<style lang="scss" scoped>
.multi-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  background: #f5f7fa;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ====== 顶部工具栏 ====== */
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.topbar-left { display: flex; align-items: center; gap: 10px; }
.topbar-right { display: flex; gap: 8px; }
.topbar-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }

/* ====== 历史面板 ====== */
.history-panel { flex: 1; overflow-y: auto; padding: 12px; }
.history-card {
  background: #fff; border-radius: 8px; padding: 14px; margin-bottom: 10px;
  cursor: pointer; transition: box-shadow .2s;
  &:hover { box-shadow: 0 2px 12px rgba(0,0,0,.1); }
}
.history-goal { font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.history-meta { display: flex; gap: 10px; align-items: center; }
.meta-info { font-size: 12px; color: #888; }

/* ====== 协作模式三面板 ====== */
.collab-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 10px;
  padding: 10px;
}

.panel {
  background: #fff;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-title {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}
.panel-left { width: 300px; flex-shrink: 0; }
.panel-center { flex: 1; }
.panel-right { width: 280px; flex-shrink: 0; }

/* 角色说明 */
.role-hints { display: flex; flex-direction: column; gap: 6px; }
.role-hint {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 6px 8px; border-radius: 6px; font-size: 12px;
}
.role-icon { font-size: 16px; flex-shrink: 0; margin-top: 1px; }
.role-name { font-weight: 600; color: #374151; }
.role-desc { color: #6b7280; font-size: 11px; }

/* ====== 执行日志 ====== */
.log-container {
  flex: 1; overflow-y: auto; padding: 10px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #d0d7e0; border-radius: 2px; }
}
.log-count { font-size: 11px; font-weight: 400; color: #9ca3af; }
.log-entry {
  margin-bottom: 10px; border-radius: 8px; overflow: hidden;
  border: 1px solid #f0f0f0;
  background: #fafafa;
}
.log-header {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; font-size: 11px; color: #6b7280;
  background: #f5f5f5;
}
.log-badge {
  padding: 1px 7px; border-radius: 10px; font-weight: 600; font-size: 11px;
}
.badge-multi-agent-start { background: #ede9fe; color: #7c3aed; }
.badge-planner-start { background: #dbeafe; color: #1d4ed8; }
.badge-planner-plan { background: #dcfce7; color: #15803d; }
.badge-executor-step { background: #fef3c7; color: #b45309; }
.badge-executor-result { background: #fff7ed; color: #c2410c; }
.badge-critic-eval { background: #fce7f3; color: #be185d; }
.badge-critic-result { background: #f0fdf4; color: #166534; }
.badge-critic-retry { background: #fef9c3; color: #854d0e; }
.badge-final { background: #d1fae5; color: #065f46; }
.badge-done { background: #e0f2fe; color: #0369a1; }
.badge-error { background: #fee2e2; color: #991b1b; }
.log-round { color: #60a5fa; font-weight: 600; }
.log-dur { color: #f59e0b; }
.log-ts { margin-left: auto; color: #9ca3af; }
.log-body { padding: 8px 10px; font-size: 12px; line-height: 1.6; }
.log-msg { color: #374151; }
.log-msg.success { color: #059669; font-weight: 600; }
.log-msg.error { color: #dc2626; font-weight: 600; }
.log-msg.warn { color: #d97706; }
.log-msg.info { color: #2563eb; }
.log-feedback { color: #9ca3af; font-size: 11px; }
.log-plan-list { display: flex; flex-direction: column; gap: 4px; }
.log-plan-step {
  display: flex; align-items: baseline; gap: 6px;
  color: #374151;
}
.step-num {
  flex-shrink: 0; width: 18px; height: 18px; line-height: 18px; text-align: center;
  background: #3b82f6; color: #fff; border-radius: 50%; font-size: 10px; font-weight: 700;
}
.log-obs { color: #6b7280; word-break: break-all; max-height: 80px; overflow: hidden; }
.log-final { background: #f0fdf4; border-radius: 6px; padding: 8px; }
.final-label { font-weight: 700; color: #166534; margin-bottom: 6px; font-size: 12px; }
.final-answer { color: #1f2937; white-space: pre-wrap; max-height: 200px; overflow-y: auto;
  font-size: 12px; }
.critic-result { display: flex; flex-direction: column; gap: 4px; }
.critic-score { font-size: 12px; color: #6b7280; }
.critic-feedback { font-size: 11px; color: #6b7280; background: #f9fafb;
  border-radius: 4px; padding: 4px 6px; margin-top: 4px; }
.log-empty { text-align: center; color: #9ca3af; padding: 40px; font-size: 13px; }

/* ====== 右侧评估面板 ====== */
.eval-summary {
  margin: 10px; padding: 14px; background: #f9fafb; border-radius: 10px;
  border: 1px solid #f0f0f0;
}
.eval-status { display: flex; align-items: baseline; gap: 4px; margin-bottom: 8px; }
.eval-big-score { font-size: 48px; font-weight: 800; line-height: 1;
  color: #1f2937; }
.eval-max { font-size: 18px; color: #9ca3af; }
.eval-badges { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.eval-feedback { margin-top: 6px; }
.eval-feedback-label { font-size: 11px; color: #9ca3af; margin-bottom: 4px; }
.eval-feedback-text { font-size: 12px; color: #374151; line-height: 1.5; }

.eval-history { margin: 10px; }
.eval-history-label { font-size: 11px; color: #6b7280; margin-bottom: 6px; font-weight: 600; }
.eval-history-item { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ev-round { font-size: 11px; color: #6b7280; width: 24px; flex-shrink: 0; }
.ev-bar-wrap { flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden; }
.ev-bar { height: 100%; border-radius: 3px; transition: width .5s; }
.bar-pass { background: linear-gradient(90deg, #34d399, #10b981); }
.bar-fail { background: linear-gradient(90deg, #f87171, #ef4444); }
.ev-score { font-size: 11px; font-weight: 700; color: #374151; width: 18px; text-align: right; }
.ev-icon { font-size: 12px; }

.exec-stats { margin: 10px; padding: 10px; background: #f9fafb; border-radius: 8px; }
.exec-stats-label { font-size: 11px; color: #6b7280; margin-bottom: 6px; font-weight: 600; }
.stat-row { display: flex; justify-content: space-between; font-size: 12px; color: #374151;
  padding: 2px 0; }
.stat-row span:first-child { color: #6b7280; }

.step-timeline { margin: 10px; }
.timeline-label { font-size: 11px; color: #6b7280; margin-bottom: 6px; font-weight: 600; }
.timeline-item { display: flex; gap: 8px; margin-bottom: 8px; }
.timeline-dot { width: 8px; height: 8px; border-radius: 50%; background: #3b82f6;
  flex-shrink: 0; margin-top: 4px; }
.timeline-content { flex: 1; }
.timeline-step { font-size: 11px; color: #374151; }
.timeline-dur { font-size: 10px; color: #9ca3af; }

.empty-hint { text-align: center; color: #9ca3af; font-size: 12px; padding: 30px 10px; }

/* ====== 画布模式 ====== */
.canvas-mode { flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 16px; padding: 30px; }
.canvas-mode-hint { text-align: center; color: #6b7280; font-size: 14px; line-height: 1.8; }
.canvas-placeholder {
  width: 300px; height: 200px; border: 2px dashed #d1d5db; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #9ca3af; font-size: 16px; transition: all .2s;
  &:hover { border-color: #3b82f6; color: #3b82f6; background: #eff6ff; }
}

/* ====== 动画 ====== */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
