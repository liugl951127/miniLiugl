<!-- @file agent/Multi.vue - 多智能体协作 V7.0 (Planner + Executor + Critic) -->
<template>
  <div class="multi-page">

    <!-- ====== 顶部工具栏 ====== -->
    <div class="topbar">
      <div class="topbar-left">
        <span class="topbar-title">🤖 多智能体协作</span>
        <el-tag v-if="phase" :type="phaseTagType" size="small">{{ phaseLabel }}</el-tag>
        <el-tag v-if="running" type="warning" size="small" effect="plain" style="animation:pulse 1.5s infinite">
          ● 运行中 · R{{ round }}/{{ form.maxRounds }}
        </el-tag>
      </div>
      <div class="topbar-right">
        <el-button size="small" :type="tab==='collab'?'primary':''" :disabled="running" @click="switchTab('collab')">
          🔀 协作
        </el-button>
        <el-button size="small" :type="tab==='tree'?'primary':''" :disabled="running" @click="switchTab('tree')">
          🌲 协作树
        </el-button>
        <el-button size="small" :type="tab==='radar'?'primary':''" :disabled="running" @click="switchTab('radar')">
          📡 雷达图
        </el-button>
        <el-button size="small" :type="tab==='canvas'?'primary':''" :disabled="running" @click="switchTab('canvas')">
          🎨 画布
        </el-button>
        <el-button size="small" @click="clearLog" :disabled="running">🗑️ 清空</el-button>
        <el-button size="small" type="info" :disabled="!history.length" @click="switchTab('history')">
          📋 历史 {{ history.length }}
        </el-button>
      </div>
    </div>

    <!-- ====== 执行进度条 ====== -->
    <div v-if="running || execStats" class="progress-bar-wrap">
      <div class="progress-meta">
        <span>执行进度</span>
        <span v-if="running">{{ progressPct }}% · {{ completedSteps }}/{{ totalSteps }} 步 · {{ (elapsedMs/1000).toFixed(1) }}s</span>
        <span v-else-if="execStats">{{ execStats.totalSteps }} 步 · {{ (execStats.totalMs/1000).toFixed(1) }}s</span>
      </div>
      <el-progress :percentage="running ? progressPct : 100"
        :status="execStats?.criticPassed ? 'success' : execStats ? 'warning' : undefined"
        :stroke-width="6" />
      <!-- Token 消耗条 -->
      <div class="token-stats" v-if="totalTokens > 0 || running">
        <span>💎 Token</span>
        <span>{{ totalTokens.toLocaleString() }}</span>
        <span class="token-sep">|</span>
        <span>输入 {{ inputTokens.toLocaleString() }}</span>
        <span class="token-sep">|</span>
        <span>输出 {{ outputTokens.toLocaleString() }}</span>
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
          <span class="meta-info">{{ (h.totalDurationMs/1000).toFixed(1) }}s</span>
          <span class="meta-info">{{ h.time }}</span>
          <span class="meta-info" v-if="h.totalTokens">💎 {{ h.totalTokens.toLocaleString() }}</span>
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
              placeholder="描述你想达成的目标…" />
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
          <!-- 三角色简介 -->
          <el-form-item label="角色说明">
            <div class="role-hints">
              <div class="role-hint planner">
                <span class="role-icon">🧠</span>
                <div><div class="role-name">Planner 规划师</div><div class="role-desc">将目标拆解为 3-7 步</div></div>
              </div>
              <div class="role-hint executor">
                <span class="role-icon">⚡</span>
                <div><div class="role-name">Executor 执行者</div><div class="role-desc">逐个执行子任务</div></div>
              </div>
              <div class="role-hint critic">
                <span class="role-icon">🔍</span>
                <div><div class="role-name">Critic 评估者</div><div class="role-desc">评估质量，不通过则重规划</div></div>
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
              <template v-else-if="entry.type === 'executor-step'">
                <div class="log-msg">⚡ 执行第 {{ entry.step }} 步：{{ entry.goal }}</div>
              </template>
              <template v-else-if="entry.type === 'executor-result'">
                <div class="log-obs">{{ entry.observation?.slice(0, 300) }}{{ (entry.observation?.length||0) > 300 ? '…' : '' }}</div>
              </template>
              <template v-else-if="entry.type === 'critic-eval'">
                <div class="log-msg">🔍 Critic 正在评估…</div>
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
                <div class="log-msg warn">🔄 未通过，触发第 {{ entry.round + 1 }} 轮重规划</div>
              </template>
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
                  <span v-if="entry.totalTokens" class="log-tokens">💎 {{ entry.totalTokens.toLocaleString() }} tokens</span>
                </div>
              </template>
              <template v-else-if="entry.type === 'error'">
                <div class="log-msg error">⚠️ {{ entry.message }}</div>
              </template>
              <template v-else-if="entry.type === 'multi-agent-start'">
                <div class="log-msg info">🚀 协作启动 | 目标：{{ entry.goal }}</div>
              </template>
              <template v-else>
                <div class="log-msg">{{ JSON.stringify(entry.raw || entry).slice(0, 120) }}</div>
              </template>
            </div>
          </div>
          <div v-if="running && !logEntries.length" class="log-empty">
            <span>⏳ 等待连接…</span>
          </div>
        </div>
      </div>

      <!-- 右：评估 + 树 + 雷达切换 -->
      <div class="panel panel-right">
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="📊 评估" name="eval">
            <div class="eval-summary" v-if="currentEval">
              <div class="eval-big-score" :class="currentEval.passed ? 'score-pass' : 'score-fail'">
                {{ currentEval.score }}
                <span class="score-max">/ 10</span>
              </div>
              <div class="eval-badges">
                <el-tag :type="currentEval.passed ? 'success' : 'danger'" size="small">
                  {{ currentEval.passed ? '✅ 通过' : '❌ 需改进' }}
                </el-tag>
                <el-tag type="info" size="small">{{ completedRounds }}/{{ form.maxRounds }} 轮</el-tag>
              </div>
              <div v-if="currentEval.feedback" class="eval-feedback">
                <div class="eval-feedback-label">💬 Critic 反馈</div>
                <div class="eval-feedback-text">{{ currentEval.feedback }}</div>
              </div>
            </div>
            <!-- 评分历史柱状图 -->
            <div class="eval-history" v-if="evalHistory.length">
              <div class="section-label">📈 各轮评分</div>
              <div v-for="(ev, i) in evalHistory" :key="i" class="eval-bar-row">
                <span class="ev-round">R{{ ev.round }}</span>
                <div class="ev-bar-wrap">
                  <div class="ev-bar" :style="{ width: ev.score * 10 + '%' }"
                    :class="ev.passed ? 'bar-pass' : 'bar-fail'"></div>
                </div>
                <span class="ev-score">{{ ev.score }}</span>
                <span>{{ ev.passed ? '✅' : '❌' }}</span>
              </div>
            </div>
            <!-- 执行统计 -->
            <div class="exec-stats" v-if="execStats">
              <div class="section-label">📊 统计</div>
              <div class="stat-row"><span>总耗时</span><span>{{ (execStats.totalMs/1000).toFixed(1) }}s</span></div>
              <div class="stat-row"><span>执行步骤</span><span>{{ execStats.totalSteps }}</span></div>
              <div class="stat-row"><span>Critic 通过</span><span>{{ execStats.criticPassed ? '是' : '否' }}</span></div>
              <div class="stat-row" v-if="execStats.totalTokens"><span>Token 消耗</span><span>{{ execStats.totalTokens.toLocaleString() }}</span></div>
            </div>
            <!-- 步骤时间线 -->
            <div class="step-timeline" v-if="stepHistory.length">
              <div class="section-label">⏱️ 执行时间线</div>
              <div v-for="(s, i) in stepHistory" :key="i" class="timeline-item">
                <div class="timeline-dot" :class="i === stepHistory.length-1 ? 'dot-active' : ''"></div>
                <div class="timeline-content">
                  <div class="timeline-step">R{{ s.criticRound }}.{{ s.stepIndex }} {{ s.goal?.slice(0,25) }}…</div>
                  <div class="timeline-dur">{{ s.durationMs }}ms</div>
                </div>
              </div>
            </div>
            <div v-if="!currentEval && !evalHistory.length" class="empty-hint">
              开始执行后评估报告将显示在这里
            </div>
          </el-tab-pane>

          <el-tab-pane label="🌲 协作树" name="tree">
            <div class="tree-container" ref="treeContainer">
              <svg :width="treeW" :height="treeH" class="collab-tree">
                <defs>
                  <marker id="arr" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
                    <path d="M0,0 L0,6 L8,3 z" fill="#409eff" />
                  </marker>
                  <marker id="arr-pass" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
                    <path d="M0,0 L0,6 L8,3 z" fill="#67c23a" />
                  </marker>
                  <marker id="arr-fail" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
                    <path d="M0,0 L0,6 L8,3 z" fill="#f56c6c" />
                  </marker>
                </defs>
                <!-- Planner 节点 -->
                <g class="tree-node" @click="selectedTreeNode='planner'">
                  <rect :x="treeW/2 - 50" :y="20" width="100" height="44" rx="8"
                    :fill="activePhase==='planner' ? '#dbeafe' : '#eff6ff'"
                    :stroke="activePhase==='planner' ? '#3b82f6' : '#bfdbfe'"
                    stroke-width="2" class="node-rect" />
                  <text :x="treeW/2" y="48" text-anchor="middle" font-size="22">🧠</text>
                  <text :x="treeW/2" y="60" text-anchor="middle" font-size="9" fill="#1d4ed8" font-weight="600">Planner</text>
                </g>
                <!-- Planner → Executor 箭头 -->
                <line :x1="treeW/2" y1="64" :x2="treeW/2" :y2="100"
                  :stroke="stepHistory.length ? '#3b82f6' : '#d1d5db'" stroke-width="2" />
                <!-- Executor 节点 -->
                <g class="tree-node">
                  <rect :x="treeW/2 - 50" :y="100" width="100" height="44" rx="8"
                    :fill="activePhase==='executor' ? '#fef3c7' : '#fffbeb'"
                    :stroke="activePhase==='executor' ? '#f59e0b' : '#fde68a'"
                    stroke-width="2" class="node-rect" />
                  <text :x="treeW/2" y="128" text-anchor="middle" font-size="22">⚡</text>
                  <text :x="treeW/2" y="140" text-anchor="middle" font-size="9" fill="#b45309" font-weight="600">Executor</text>
                </g>
                <!-- Executor 步骤子节点 -->
                <g v-for="(step, i) in stepHistory" :key="'step-'+i">
                  <!-- 垂直连接线 -->
                  <line :x1="treeW/2 + 60" :y1="122 + i*52" :x2="treeW/2 + 60" :y2="136 + i*52"
                    :stroke="step.durationMs > 5000 ? '#f56c6c' : '#3b82f6'" stroke-width="1.5" />
                  <!-- 步骤节点 -->
                  <rect :x="treeW/2 + 60" :y="136 + i*52" width="120" height="36" rx="6"
                    fill="#f0f9ff" stroke="#93c5fd" stroke-width="1.5" class="step-node" />
                  <text :x="treeW/2 + 68" :y="153 + i*52" font-size="9" fill="#1e40af" font-weight="600">
                    Step {{ step.stepIndex }}
                  </text>
                  <text :x="treeW/2 + 68" :y="165 + i*52" font-size="8" fill="#6b7280">
                    {{ step.durationMs }}ms
                  </text>
                  <!-- 步骤内连线 -->
                  <line :x1="treeW/2 + 50" :y1="122 + i*52" :x2="treeW/2 + 60" :y2="154 + i*52"
                    stroke="#93c5fd" stroke-width="1" />
                </g>
                <!-- Planner → Executor → Critic 箭头 -->
                <template v-if="stepHistory.length">
                  <line :x1="treeW/2" :y1="148" :x2="treeW/2" :y2="184"
                    :stroke="activePhase==='critic' ? '#ec4899' : '#d1d5db'" stroke-width="2" />
                  <!-- Critic 节点 -->
                  <g class="tree-node">
                    <rect :x="treeW/2 - 50" :y="184" width="100" height="44" rx="8"
                      :fill="activePhase==='critic' ? '#fce7f3' : '#fdf2f8'"
                      :stroke="activePhase==='critic' ? '#ec4899' : '#f9a8d4'"
                      stroke-width="2" class="node-rect" />
                    <text :x="treeW/2" y="212" text-anchor="middle" font-size="22">🔍</text>
                    <text :x="treeW/2" y="224" text-anchor="middle" font-size="9" fill="#be185d" font-weight="600">Critic</text>
                  </g>
                  <!-- Critic 评分显示 -->
                  <g v-if="currentEval" :transform="`translate(${treeW/2 + 60}, 196)`">
                    <rect width="120" height="36" rx="6"
                      :fill="currentEval.passed ? '#dcfce7' : '#fee2e2'"
                      :stroke="currentEval.passed ? '#22c55e' : '#ef4444'"
                      stroke-width="1.5" />
                    <text x="60" y="16" text-anchor="middle" font-size="18" font-weight="700"
                      :fill="currentEval.passed ? '#166534' : '#991b1b'">
                      {{ currentEval.score }}/10
                    </text>
                    <text x="60" y="28" text-anchor="middle" font-size="8"
                      :fill="currentEval.passed ? '#15803d' : '#dc2626'">
                      {{ currentEval.passed ? '✅ 通过' : '❌ 未通过' }}
                    </text>
                  </g>
                  <!-- 轮次箭头：Critic → Planner（重规划） -->
                  <template v-if="!currentEval?.passed && round < form.maxRounds">
                    <path d="M {{treeW/2 - 50}} 206 Q {{treeW/2 - 80}} 250 {{treeW/2 - 50}} 280"
                      :stroke="'#f59e0b'" stroke-width="2" fill="none" stroke-dasharray="4,2"
                      marker-end="url(#arr)" />
                    <text :x="treeW/2 - 90" y="270" font-size="8" fill="#b45309">重规划</text>
                  </template>
                  <!-- 完成路径：Critic → 右下角 -->
                  <template v-if="currentEval?.passed">
                    <path :d="`M ${treeW/2 + 50} 206 Q ${treeW/2 + 100} 260 ${treeW/2 + 50} 290`"
                      stroke="#67c23a" stroke-width="2" fill="none" marker-end="url(#arr-pass)" />
                    <text :x="treeW/2 + 60" y="300" font-size="9" fill="#166534" font-weight="600">🎉 完成</text>
                  </template>
                </template>
                <!-- 空状态 -->
                <template v-if="!stepHistory.length && !running">
                  <text :x="treeW/2" :y="150" text-anchor="middle" font-size="12" fill="#9ca3af">
                    开始执行后协作树将展开
                  </text>
                </template>
              </svg>
            </div>
          </el-tab-pane>

          <el-tab-pane label="📡 雷达图" name="radar">
            <div class="radar-wrap" v-if="radarData.length">
              <v-chart class="radar-chart" :option="radarOption" autoresize />
              <div class="radar-legend">
                <div v-for="(r, i) in radarData" :key="i" class="legend-item">
                  <span class="legend-dot" :style="{ background: radarColors[i % radarColors.length] }"></span>
                  <span>第 {{ r.round }} 轮 ({{ r.score }}/10)</span>
                  <el-tag size="small" :type="r.passed ? 'success' : 'danger'" style="margin-left:4px">
                    {{ r.passed ? '✅' : '❌' }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div v-else-if="!evalHistory.length" class="empty-hint">
              执行后雷达图将显示多维度评分
            </div>
            <div v-else class="empty-hint">无评分数据</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- ====== 画布模式 ====== -->
    <div v-if="tab==='canvas'" class="canvas-mode">
      <div class="canvas-mode-hint">
        👇 从左侧拖拽 Agent 节点到画布，连接后点「执行」即可触发多智能体协作。
        <br>开启「🤖 多Agent」模式后，执行结果会实时同步到协作树和雷达图。
        <br><br>
        <el-button type="primary" @click="openCanvasDialog = true">🎨 在此页面打开画布</el-button>
        <el-button @click="switchTab('collab')">← 切换到协作模式</el-button>
      </div>
      <div class="canvas-placeholder" @click="openCanvasDialog = true">
        <span>🎨 点击打开 Agent 画布编辑器</span>
      </div>
    </div>

    <!-- ====== Canvas 弹窗 ====== -->
    <el-dialog v-model="openCanvasDialog" title="Agent 画布编辑器" width="90%" top="2vh"
      :close-on-click-modal="false" destroy-on-close>
      <iframe v-if="openCanvasDialog" src="/canvas" class="canvas-iframe" />
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onUnmounted, watch, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { multiAgentApi } from '@/api/agent'
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([RadarChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// ========== 状态 ==========
const tab = ref('collab')
const rightTab = ref('eval')
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
const openCanvasDialog = ref(false)

// 树
const treeContainer = ref(null)
const treeW = ref(320)
const treeH = ref(400)
const selectedTreeNode = ref('')

// Token
const totalTokens = ref(0)
const inputTokens = ref(0)
const outputTokens = ref(0)
const elapsedMs = ref(0)
let timerInterval = null

// 雷达图维度定义
const RADAR_INDICATORS = [
  { name: '准确性', max: 10 },
  { name: '完整性', max: 10 },
  { name: '创造性', max: 10 },
  { name: '效率', max: 10 },
  { name: '安全性', max: 10 },
]
const radarColors = ['#3b82f6', '#f59e0b', '#10b981', '#ec4899', '#8b5cf6', '#ef4444']

// 表单
const form = reactive({
  goal: '',
  model: 'MiniMax-Text-01',
  maxRounds: 3,
  tools: ['web-search', 'calculator']
})

// ========== 计算属性 ==========
const phaseTagType = computed(() => ({
  planner: 'primary', executor: 'warning', critic: 'danger', done: 'success'
}[phase.value] || 'info'))
const phaseLabel = computed(() => ({
  planner: '🧠 规划中', executor: '⚡ 执行中', critic: '🔍 评估中', done: '✅ 完成'
}[phase.value] || phase.value))
const completedRounds = computed(() => evalHistory.value.length)
const totalSteps = computed(() => form.maxRounds * (stepHistory.value.length / Math.max(round.value, 1)))
const completedSteps = computed(() => stepHistory.value.length)
const progressPct = computed(() => {
  if (!running.value) return 100
  const total = form.maxRounds
  const done = completedRounds.value
  return Math.min(100, Math.round((done / total) * 100))
})
const activePhase = computed(() => phase.value)

// ========== 雷达图数据 ==========
const radarData = computed(() => evalHistory.value.map((ev, i) => {
  // 从 feedback 文本推断各维度分数（简单规则）
  const dims = parseDimensions(ev.score, ev.feedback || '')
  return { round: ev.round, score: ev.score, passed: ev.passed, dims }
}))

function parseDimensions(score, feedback) {
  // 基于总分和 critic 反馈推断各维度
  const s = score / 10  // 归一化
  const negatives = (feedback.match(/不|未|缺|差|错|误|漏/g) || []).length
  const positives = (feedback.match(/好|优|准|全|完|通过/g) || []).length
  const delta = Math.max(-2, Math.min(2, positives - negatives))
  const base = Math.max(1, Math.min(10, score + delta))
  return [
    Math.max(1, Math.min(10, score + (feedback.includes('准确') ? 1 : 0))),
    Math.max(1, Math.min(10, score + (feedback.includes('完整') ? 1 : 0))),
    Math.max(1, Math.min(10, base)),
    Math.max(1, Math.min(10, score - (feedback.includes('效率') ? 0 : -1))),
    Math.max(1, Math.min(10, score + (feedback.includes('安全') ? 1 : 0))),
  ]
}

const radarOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item' },
  legend: { show: false },
  radar: {
    indicator: RADAR_INDICATORS,
    radius: '65%',
    splitNumber: 5,
    axisName: { color: '#6b7280', fontSize: 10 },
    splitLine: { lineStyle: { color: '#f0f0f0' } },
    splitArea: { areaStyle: { color: ['#fafafa', '#f5f5f5'] } },
    axisLine: { lineStyle: { color: '#e5e7eb' } },
  },
  series: [{
    type: 'radar',
    data: radarData.value.map((r, i) => ({
      value: r.dims,
      name: `第 ${r.round} 轮`,
      lineStyle: { color: radarColors[i % radarColors.length], width: 2 },
      areaStyle: { color: radarColors[i % radarColors.length] + '44' },
      itemStyle: { color: radarColors[i % radarColors.length] },
      symbol: 'circle', symbolSize: 5,
    })),
  }],
}))

// ========== 事件图标/标签 ==========
function eventIcon(type) {
  return { 'multi-agent-start': '🚀', 'planner-start': '🧠', 'planner-plan': '📋',
    'executor-step': '⚡', 'executor-result': '📥', 'critic-eval': '🔍',
    'critic-result': '✅', 'critic-retry': '🔄', 'final': '🎉', 'done': '🏁',
    'error': '⚠️', 'token-update': '💎' }[type] || '📌'
}
function eventLabel(type) {
  return { 'multi-agent-start': '启动', 'planner-start': 'Planner开始', 'planner-plan': 'Planner计划',
    'executor-step': 'Executor执行', 'executor-result': 'Executor结果', 'critic-eval': 'Critic评估',
    'critic-result': 'Critic结论', 'critic-retry': 'Critic重试', 'final': '最终答案',
    'done': '完成', 'error': '错误', 'token-update': 'Token消耗' }[type] || type
}

// ========== 启动 ==========
async function startMulti() {
  if (!form.goal.trim()) { ElMessage.warning('请输入目标任务'); return }
  running.value = true
  phase.value = ''; round.value = 0
  logEntries.value = []; evalHistory.value = []; stepHistory.value = []
  currentEval.value = null; execStats.value = null
  totalTokens.value = 0; inputTokens.value = 0; outputTokens.value = 0
  elapsedMs.value = 0

  // 启动计时器
  const startTime = Date.now()
  timerInterval = setInterval(() => {
    elapsedMs.value = Date.now() - startTime
  }, 500)

  abortController.value = new AbortController()
  try {
    await multiAgentApi.xhrStream(
      { goal: form.goal, tools: form.tools, maxRounds: form.maxRounds, model: form.model },
      (eventName, data, raw) => {
        try {
          handleSSEEvent(eventName, data, raw)
        } catch (e) {
          console.error('[Multi] SSE 事件处理异常:', eventName, e)
          pushLog('error', { message: `事件 ${eventName} 处理异常: ${e.message}` })
        }
      }
    )
  } catch (e) {
    if (e.name === 'AbortError') return
    const msg = e.message || 'SSE 连接失败'
    pushLog('error', { message: msg })
    if (msg.includes('401') || msg.includes('Unauthorized') || msg.includes('需要登录')) {
      ElMessage.error('请先登录后再使用多智能体协作')
    } else if (msg.includes('fetch') || msg.includes('Failed') || msg.includes('Network')) {
      ElMessage.error('无法连接后端服务，请确认 MiniMax 平台已启动')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    clearInterval(timerInterval)
    running.value = false
  }
}

function stopMulti() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  clearInterval(timerInterval)
  running.value = false
  pushLog('error', { message: '用户主动停止' })
}

// ========== SSE 事件处理 ==========
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
      if (data.round && data.step) {
        stepHistory.value.push({
          criticRound: data.round, stepIndex: data.step,
          goal: data.goal, observation: data.observation,
          durationMs: data.durationMs || 0
        })
        // 动态扩展树高度
        treeH.value = Math.max(400, 300 + stepHistory.value.length * 56)
      }
      break

    case 'token-update':
      inputTokens.value += data.inputTokens || 0
      outputTokens.value += data.outputTokens || 0
      totalTokens.value = (data.totalTokens || totalTokens.value)
      pushLog('token-update', { ...data, ts, type: 'token-update' })
      break

    case 'critic-eval':
      phase.value = 'critic'
      pushLog('critic-eval', { ...data, ts, type: 'critic-eval' })
      break

    case 'critic-result':
      pushLog('critic-result', { ...data, ts, type: 'critic-result' })
      currentEval.value = { score: data.score, passed: data.passed, feedback: data.feedback }
      evalHistory.value.push({ round: data.round, score: data.score, passed: data.passed, feedback: data.feedback })
      // 自动切雷达图
      rightTab.value = 'eval'
      break

    case 'critic-retry':
      pushLog('critic-retry', { ...data, ts, type: 'critic-retry' })
      break

    case 'final':
      phase.value = 'done'
      pushLog('final', { ...data, ts, type: 'final' })
      rightTab.value = 'tree'
      break

    case 'done':
      execStats.value = {
        totalMs: data.totalDurationMs,
        totalSteps: stepHistory.value.length,
        maxRounds: data.rounds,
        criticPassed: data.criticPassed,
        totalTokens: totalTokens.value
      }
      pushLog('done', { ...data, ts, type: 'done', totalTokens: totalTokens.value })
      history.value.unshift({
        goal: form.goal, rounds: data.rounds, criticPassed: data.criticPassed,
        totalDurationMs: data.totalDurationMs, time: ts,
        steps: [...stepHistory.value], evals: [...evalHistory.value],
        finalAnswer: data.answer, totalTokens: totalTokens.value
      })
      rightTab.value = 'radar'
      ElMessage.success('多智能体协作完成')
      break

    case 'error':
      pushLog('error', { ...data, ts, type: 'error' })
      ElMessage.error(data.message || '执行异常')
      break

    default:
      pushLog('message', { ...data, ts, type: eventName, raw })
  }

  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
}

function pushLog(type, data) {
  logEntries.value.push({ type, ...data,
    ts: data.ts || new Date().toLocaleTimeString('zh-CN', { hour12: false }) })
}

// ========== 历史 ==========
function loadHistory(h) {
  form.goal = h.goal
  logEntries.value = []
  evalHistory.value = h.evals || []
  stepHistory.value = h.steps || []
  currentEval.value = h.evals?.length ? h.evals[h.evals.length - 1] : null
  execStats.value = {
    totalMs: h.totalDurationMs, totalSteps: h.steps?.length || 0,
    maxRounds: h.rounds, criticPassed: h.criticPassed,
    totalTokens: h.totalTokens || 0
  }
  totalTokens.value = h.totalTokens || 0
  tab.value = 'collab'
  rightTab.value = h.evals?.length ? 'radar' : 'eval'
}

// ========== 工具 ==========
function clearLog() {
  logEntries.value = []; evalHistory.value = []; stepHistory.value = []
  currentEval.value = null; execStats.value = null
  totalTokens.value = 0; inputTokens.value = 0; outputTokens.value = 0
  elapsedMs.value = 0; phase.value = ''; round.value = 0
}
function switchTab(t) { tab.value = t }

// ========== 树尺寸响应 ==========
watch(tab, t => {
  if (t === 'tree') {
    nextTick(() => {
      if (treeContainer.value) {
        treeW.value = treeContainer.value.offsetWidth || 320
      }
    })
  }
})

onUnmounted(() => {
  abortController.value?.abort()
  clearInterval(timerInterval)
})
</script>

<style lang="scss" scoped>
.multi-page {
  display: flex; flex-direction: column;
  height: calc(100vh - 60px);
  background: #f5f7fa;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ====== 顶部工具栏 ====== */
.topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8ecf0; flex-shrink: 0;
}
.topbar-left { display: flex; align-items: center; gap: 10px; }
.topbar-right { display: flex; gap: 6px; }
.topbar-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }

/* ====== 进度条 ====== */
.progress-bar-wrap {
  background: #fff; padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0; flex-shrink: 0;
  :deep(.el-progress-bar__outer) { border-radius: 4px; }
}
.progress-meta {
  display: flex; justify-content: space-between; font-size: 11px; color: #6b7280; margin-bottom: 4px;
}
.token-stats {
  display: flex; gap: 8px; align-items: center; font-size: 11px; color: #6b7280; margin-top: 4px;
}
.token-sep { color: #d1d5db; }

/* ====== 历史面板 ====== */
.history-panel { flex: 1; overflow-y: auto; padding: 12px; }
.history-card {
  background: #fff; border-radius: 8px; padding: 14px; margin-bottom: 10px;
  cursor: pointer; transition: box-shadow .2s;
  &:hover { box-shadow: 0 2px 12px rgba(0,0,0,.1); }
}
.history-goal { font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.history-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.meta-info { font-size: 12px; color: #888; }

/* ====== 协作模式 ====== */
.collab-body { display: flex; flex: 1; overflow: hidden; gap: 10px; padding: 10px; }
.panel {
  background: #fff; border-radius: 10px; display: flex; flex-direction: column; overflow: hidden;
}
.panel-title {
  padding: 10px 14px; font-size: 13px; font-weight: 600; color: #374151;
  border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;
}
.panel-left { width: 300px; flex-shrink: 0; }
.panel-center { flex: 1; }
.panel-right { width: 300px; flex-shrink: 0; overflow: hidden; }

/* 角色说明 */
.role-hints { display: flex; flex-direction: column; gap: 6px; }
.role-hint {
  display: flex; align-items: flex-start; gap: 8px; padding: 6px 8px;
  border-radius: 6px; font-size: 12px;
}
.role-icon { font-size: 16px; flex-shrink: 0; }
.role-name { font-weight: 600; color: #374151; }
.role-desc { color: #6b7280; font-size: 11px; }

/* ====== 执行日志 ====== */
.log-container { flex: 1; overflow-y: auto; padding: 10px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #d0d7e0; border-radius: 2px; }
}
.log-count { font-size: 11px; font-weight: 400; color: #9ca3af; }
.log-entry {
  margin-bottom: 10px; border-radius: 8px; overflow: hidden;
  border: 1px solid #f0f0f0; background: #fafafa;
}
.log-header {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  font-size: 11px; color: #6b7280; background: #f5f5f5;
}
.log-badge { padding: 1px 7px; border-radius: 10px; font-weight: 600; font-size: 11px; }
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
.badge-token-update { background: #fef3c7; color: #92400e; }
.log-round { color: #60a5fa; font-weight: 600; }
.log-dur { color: #f59e0b; }
.log-ts { margin-left: auto; color: #9ca3af; }
.log-body { padding: 8px 10px; font-size: 12px; line-height: 1.6; }
.log-msg { color: #374151; }
.log-msg.success { color: #059669; font-weight: 600; }
.log-msg.error { color: #dc2626; font-weight: 600; }
.log-msg.warn { color: #d97706; }
.log-msg.info { color: #2563eb; }
.log-tokens { margin-left: 8px; color: #d97706; font-weight: 600; }
.log-feedback { color: #9ca3af; font-size: 11px; }
.log-plan-list { display: flex; flex-direction: column; gap: 4px; }
.log-plan-step { display: flex; align-items: baseline; gap: 6px; }
.step-num {
  flex-shrink: 0; width: 18px; height: 18px; line-height: 18px; text-align: center;
  background: #3b82f6; color: #fff; border-radius: 50%; font-size: 10px; font-weight: 700;
}
.log-obs { color: #6b7280; word-break: break-all; }
.log-final { background: #f0fdf4; border-radius: 6px; padding: 8px; }
.final-label { font-weight: 700; color: #166534; margin-bottom: 6px; font-size: 12px; }
.final-answer { color: #1f2937; white-space: pre-wrap; max-height: 200px; overflow-y: auto; font-size: 12px; }
.critic-result { display: flex; flex-direction: column; gap: 4px; }
.critic-score { font-size: 12px; color: #6b7280; }
.critic-feedback { font-size: 11px; color: #6b7280; background: #f9fafb; border-radius: 4px; padding: 4px 6px; margin-top: 4px; }
.log-empty { text-align: center; color: #9ca3af; padding: 40px; font-size: 13px; }

/* ====== 右侧评估面板 ====== */
.right-tabs { height: 100%; display: flex; flex-direction: column;
  :deep(.el-tabs__content) { padding: 10px; overflow-y: auto; flex: 1; }
  :deep(.el-tabs__nav-wrap::after) { display: none; }
  :deep(.el-tabs__item) { font-size: 12px; padding: 0 8px; }
}

.eval-summary {
  padding: 12px; background: #f9fafb; border-radius: 10px; border: 1px solid #f0f0f0; margin-bottom: 10px;
}
.eval-big-score { font-size: 48px; font-weight: 800; line-height: 1; text-align: center;
  &.score-pass { color: #059669; }
  &.score-fail { color: #dc2626; }
}
.score-max { font-size: 18px; color: #9ca3af; }
.eval-badges { display: flex; gap: 6px; flex-wrap: wrap; justify-content: center; margin: 8px 0; }
.eval-feedback { margin-top: 6px; }
.eval-feedback-label { font-size: 11px; color: #9ca3af; margin-bottom: 4px; }
.eval-feedback-text { font-size: 12px; color: #374151; line-height: 1.5; max-height: 80px; overflow-y: auto; }

.section-label { font-size: 11px; color: #6b7280; margin-bottom: 6px; font-weight: 600; }
.eval-history { margin-bottom: 10px; }
.eval-bar-row { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ev-round { font-size: 11px; color: #6b7280; width: 24px; flex-shrink: 0; }
.ev-bar-wrap { flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden; }
.ev-bar { height: 100%; border-radius: 3px; transition: width .5s; }
.bar-pass { background: linear-gradient(90deg, #34d399, #10b981); }
.bar-fail { background: linear-gradient(90deg, #f87171, #ef4444); }
.ev-score { font-size: 11px; font-weight: 700; color: #374151; width: 18px; text-align: right; }

.exec-stats { padding: 10px; background: #f9fafb; border-radius: 8px; margin-bottom: 10px; }
.stat-row { display: flex; justify-content: space-between; font-size: 12px; color: #374151; padding: 2px 0; }
.stat-row span:first-child { color: #6b7280; }

.step-timeline { }
.timeline-item { display: flex; gap: 8px; margin-bottom: 8px; }
.timeline-dot { width: 8px; height: 8px; border-radius: 50%; background: #3b82f6; flex-shrink: 0; margin-top: 4px;
  &.dot-active { background: #67c23a; box-shadow: 0 0 6px #67c23a; }
}
.timeline-content { flex: 1; }
.timeline-step { font-size: 11px; color: #374151; }
.timeline-dur { font-size: 10px; color: #9ca3af; }

/* ====== 协作树 ====== */
.tree-container { padding: 8px; overflow: auto; }
.collab-tree { display: block; margin: 0 auto; }
.tree-node { cursor: pointer; }
.node-rect { transition: all .3s; }
.step-node { transition: all .2s; cursor: default; }
.step-node:hover { filter: brightness(0.95); }

/* ====== 雷达图 ====== */
.radar-wrap { display: flex; flex-direction: column; gap: 10px; }
.radar-chart { width: 100%; height: 240px; }
.radar-legend { display: flex; flex-direction: column; gap: 4px; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #374151; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }

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
.canvas-iframe { width: 100%; height: 70vh; border: none; border-radius: 8px; }

.empty-hint { text-align: center; color: #9ca3af; font-size: 12px; padding: 30px 10px; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
</style>
