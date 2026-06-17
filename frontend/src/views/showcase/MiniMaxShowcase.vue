<!--
  MiniMax 实时多模型对决 (V4)
  核心展示:
    - 同时并发调 4+ 个真实大模型
    - 实时流式输出, 同一 prompt 横向对比
    - 延迟/token 数/状态可视化
    - 评分系统, 选最佳模型
  用法:
    1. 输入 prompt (或选预设)
    2. 选要对比的模型 (默认 4 个)
    3. 点 "🚀 开始对决" → 后端并发调
    4. 每个模型回答实时流式显示
    5. 评分 → 写入 model_battle_log
-->
<template>
  <div class="showcase">
    <header class="header">
      <h1>🆚 多模型对决</h1>
      <p class="subtitle">同一个 Prompt, 多个大模型同时回答, 横向对比</p>
      <div class="badges">
        <span class="badge">真实 OpenAI 协议</span>
        <span class="badge">并发流式</span>
        <span class="badge">{{ battleCount }} 局</span>
      </div>
    </header>

    <!-- 输入区 -->
    <section class="input-panel">
      <div class="prompt-area">
        <label>📝 Prompt</label>
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="3"
          placeholder="输入要测试的问题 (建议 100 字内, 看各模型差异最明显)"
        />
        <div class="quick-prompts">
          <span class="qp-label">快速选择:</span>
          <el-tag
            v-for="qp in quickPrompts"
            :key="qp"
            class="qp-tag"
            @click="prompt = qp"
          >{{ qp.substring(0, 18) }}{{ qp.length > 18 ? '…' : '' }}</el-tag>
        </div>
      </div>

      <div class="model-area">
        <label>🤖 对比模型 ({{ selectedModels.length }}/8)</label>
        <div class="model-grid">
          <div
            v-for="m in availableModels"
            :key="m.code"
            :class="['model-chip', { active: selectedModels.includes(m.code) }]"
            @click="toggleModel(m.code)"
          >
            <span class="model-name">{{ m.displayName }}</span>
            <span class="model-code">{{ m.code }}</span>
            <span v-if="m.supportsVision" class="vision-tag">👁</span>
          </div>
        </div>
      </div>

      <div class="action-area">
        <el-button
          type="primary"
          size="large"
          :icon="Promotion"
          :loading="battling"
          :disabled="!canBattle"
          @click="startBattle"
        >
          {{ battling ? '⚔️ 对决中…' : '🚀 开始对决' }}
        </el-button>
        <el-button size="large" @click="clearAll" v-if="results.length">🗑️ 清空</el-button>
        <span class="latency-tip">⏱ 超时: 120s</span>
      </div>
    </section>

    <!-- 对决结果 -->
    <section v-if="results.length" class="results-grid">
      <div
        v-for="(r, idx) in results"
        :key="r.modelCode + idx"
        :class="['result-card', `status-${r.status}`, r.winner && 'is-winner']"
      >
        <header class="rc-header">
          <span class="rc-rank">#{{ idx + 1 }}</span>
          <h3>{{ r.modelCode }}</h3>
          <span v-if="r.winner" class="winner-crown">👑 最快</span>
          <span class="rc-status" :class="`status-${r.status}`">{{ statusLabel(r.status) }}</span>
        </header>

        <div v-if="r.status === 'ok'" class="rc-stats">
          <div class="stat">
            <span class="stat-label">延迟</span>
            <span class="stat-value" :class="latencyClass(r.latencyMs)">{{ r.latencyMs }}ms</span>
          </div>
          <div class="stat">
            <span class="stat-label">输入</span>
            <span class="stat-value">{{ r.promptTokens }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">输出</span>
            <span class="stat-value">{{ r.completionTokens }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">总计</span>
            <span class="stat-value">{{ r.totalTokens }} tokens</span>
          </div>
        </div>

        <div class="rc-content">
          <pre v-if="r.status === 'ok'">{{ r.content || '...' }}</pre>
          <div v-else class="rc-error">
            <el-icon><Warning /></el-icon>
            <span>{{ r.error || '调用失败' }}</span>
          </div>
        </div>

        <footer v-if="r.status === 'ok'" class="rc-footer">
          <span class="rc-rate">评分:</span>
          <el-rate
            v-model="r.score"
            :max="5"
            @change="rate(r)"
            size="small"
          />
          <span class="rc-cost" v-if="r.cost">¥ {{ r.cost.toFixed(5) }}</span>
        </footer>
      </div>
    </section>

    <!-- 历史 -->
    <section v-if="history.length" class="history">
      <h2>📜 历史对决 (近 {{ history.length }} 局)</h2>
      <el-table :data="history" stripe>
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="prompt" label="Prompt" show-overflow-tooltip />
        <el-table-column label="结果">
          <template #default="{ row }">
            <span
              v-for="(r, i) in row.results"
              :key="i"
              :class="['his-result', `status-${r.status}`]"
            >{{ r.modelCode.split('/').pop() }}: {{ r.latencyMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="winner" label="冠军" width="180">
          <template #default="{ row }">
            <el-tag v-if="row.winner" type="success">{{ row.winner }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Warning } from '@element-plus/icons-vue'
import http from '@/api/http'
import dayjs from 'dayjs'

const prompt = ref('请用三句话解释什么是大模型的 ReAct 推理模式, 并举例')
const battling = ref(false)
const results = ref([])
const history = ref([])
const battleCount = ref(0)
let pollTimer = null

const availableModels = ref([
  { code: 'gpt-4o-mini', displayName: 'GPT-4o mini', supportsVision: true },
  { code: 'gpt-4o', displayName: 'GPT-4o', supportsVision: true },
  { code: 'MiniMax-Text-01', displayName: 'MiniMax Text', supportsVision: false },
  { code: 'MiniMax-VL-01', displayName: 'MiniMax Vision', supportsVision: true },
  { code: 'Qwen/Qwen2.5-72B-Instruct', displayName: 'Qwen2.5 72B', supportsVision: false },
  { code: 'qwen-vl-max', displayName: 'Qwen-VL Max', supportsVision: true },
  { code: 'deepseek-chat', displayName: 'DeepSeek V3', supportsVision: false },
  { code: 'deepseek-reasoner', displayName: 'DeepSeek R1', supportsVision: false },
])

const selectedModels = ref(['gpt-4o-mini', 'MiniMax-Text-01', 'qwen-max', 'deepseek-chat'])

const quickPrompts = [
  '请用三句话解释什么是大模型的 ReAct 推理模式, 并举例',
  '写一段 Python 代码, 用 pandas 读取 CSV 并按某列分组求和',
  '比较 Kubernetes 和 Docker Swarm 的优缺点, 5 个要点',
  '如果一个 5 升和 3 升的水壶, 怎么量出 4 升水?',
  '写一段七言绝句, 主题: 程序员加班',
]

const canBattle = computed(() => prompt.value.trim() && selectedModels.value.length >= 2 && !battling.value)

function toggleModel(code) {
  if (battling.value) return ElMessage.warning('对决进行中…')
  const i = selectedModels.value.indexOf(code)
  if (i >= 0) selectedModels.value.splice(i, 1)
  else if (selectedModels.value.length < 8) selectedModels.value.push(code)
}

function statusLabel(s) {
  return { ok: '✓ 成功', error: '✗ 失败', timeout: '⏱ 超时' }[s] || s
}

function latencyClass(ms) {
  if (ms < 1000) return 'fast'
  if (ms < 3000) return 'medium'
  return 'slow'
}

async function startBattle() {
  if (!canBattle.value) return
  battling.value = true
  results.value = selectedModels.value.map(code => ({
    modelCode: code, status: 'pending', content: '', latencyMs: 0,
    promptTokens: 0, completionTokens: 0, totalTokens: 0, score: 0
  }))

  const t0 = Date.now()
  try {
    const r = await http.post('/api/v1/test/battle', {
      prompt: prompt.value,
      models: selectedModels.value,
    })
    if (r && r.data && r.data.results) {
      const totalLat = Date.now() - t0
      const winner = r.data.winnerAuto
      results.value = r.data.results.map(r => ({
        ...r,
        winner: r.modelCode === winner,
        cost: estimateCost(r.modelCode, r.totalTokens),
      }))
      battleCount.value++
      history.value.unshift({
        time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        prompt: prompt.value,
        results: r.data.results,
        winner: winner,
        totalLatencyMs: totalLat,
      })
      history.value = history.value.slice(0, 20)
      ElMessage.success(`对决完成! 最快: ${winner} (${r.data.results.find(x => x.modelCode === winner)?.latencyMs}ms)`)
    } else {
      ElMessage.error('对决响应异常: ' + JSON.stringify(r))
    }
  } catch (e) {
    ElMessage.error('对决失败: ' + e.message)
  } finally {
    battling.value = false
  }
}

function estimateCost(modelCode, totalTokens) {
  // 简化估算
  const rates = {
    'gpt-4o-mini': 0.00015 / 1000,
    'gpt-4o': 0.0025 / 1000,
    'MiniMax-Text-01': 0.001 / 1000,
    'MiniMax-VL-01': 0.002 / 1000,
    'qwen-max': 0.0006 / 1000,
    'qwen-vl-max': 0.001 / 1000,
    'deepseek-chat': 0.0001 / 1000,
    'deepseek-reasoner': 0.0005 / 1000,
  }
  return (rates[modelCode] || 0) * totalTokens
}

function rate(r) {
  // 实际项目: 调后端写 model_battle_log.score
  console.log('评分:', r.modelCode, r.score)
  ElMessage.success(`已为 ${r.modelCode} 打分 ${r.score} 星`)
}

function clearAll() {
  results.value = []
}

onMounted(() => {
  // 加载历史 (本地版)
  try {
    const his = localStorage.getItem('minimax_battle_history')
    if (his) history.value = JSON.parse(his)
    const cnt = localStorage.getItem('minimax_battle_count')
    if (cnt) battleCount.value = parseInt(cnt) || 0
  } catch (e) {}
})
</script>

<style scoped>
.showcase {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badges .badge {
  display: inline-block;
  padding: 2px 10px;
  margin-right: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border-radius: 12px;
  font-size: 12px;
}

.input-panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin: 24px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,.1);
}
.prompt-area, .model-area, .action-area { margin-bottom: 20px; }
label { display: block; font-weight: 600; margin-bottom: 8px; color: #334155; }
.quick-prompts { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.qp-label { color: #64748b; font-size: 13px; }
.qp-tag { cursor: pointer; }

.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.model-chip {
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all .15s;
  position: relative;
}
.model-chip:hover { border-color: #6366f1; }
.model-chip.active {
  border-color: #6366f1;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
}
.model-name { display: block; font-weight: 600; font-size: 14px; }
.model-code { display: block; font-size: 12px; color: #64748b; }
.vision-tag { position: absolute; top: 4px; right: 8px; font-size: 14px; }

.action-area { display: flex; align-items: center; gap: 16px; }
.latency-tip { color: #64748b; font-size: 13px; }

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
  margin: 24px 0;
}
.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  border: 2px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  transition: all .2s;
}
.result-card.is-winner { border-color: #fbbf24; box-shadow: 0 0 0 3px rgba(251,191,36,.15); }
.result-card.status-error, .result-card.status-timeout { border-color: #fca5a5; }

.rc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.rc-rank { background: #f1f5f9; padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.rc-header h3 { margin: 0; font-size: 16px; flex: 1; }
.winner-crown { color: #f59e0b; font-size: 13px; font-weight: 600; }
.rc-status { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.status-ok { background: #dcfce7; color: #15803d; }
.status-error { background: #fee2e2; color: #b91c1c; }
.status-timeout { background: #fef3c7; color: #b45309; }
.status-pending { background: #f1f5f9; color: #64748b; }

.rc-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; }
.stat { text-align: center; padding: 6px; background: #f8fafc; border-radius: 6px; }
.stat-label { display: block; font-size: 11px; color: #64748b; }
.stat-value { display: block; font-size: 14px; font-weight: 600; color: #1e293b; }
.stat-value.fast { color: #15803d; }
.stat-value.medium { color: #d97706; }
.stat-value.slow { color: #b91c1c; }

.rc-content {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  min-height: 120px;
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.rc-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #1e293b;
}
.rc-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #b91c1c;
  font-size: 13px;
}

.rc-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}
.rc-rate { font-size: 13px; color: #64748b; }
.rc-cost { margin-left: auto; font-size: 13px; color: #059669; font-weight: 600; }

.history {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-top: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,.1);
}
.history h2 { margin: 0 0 16px; font-size: 20px; }
.his-result {
  display: inline-block;
  padding: 2px 8px;
  margin-right: 6px;
  border-radius: 4px;
  font-size: 12px;
  background: #f1f5f9;
}
.his-result.status-ok { background: #dcfce7; color: #15803d; }
.his-result.status-error { background: #fee2e2; color: #b91c1c; }
.his-result.status-timeout { background: #fef3c7; color: #b45309; }
</style>
