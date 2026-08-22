<!-- @file rule/Index.vue - NL 规则助手 V7.4 (自研模型 + 规则引擎) -->
<template>
  <div class="rule-page">
    <!-- 顶部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>⚙️ NL 规则助手 <el-tag size="small" type="success">V7.4</el-tag></h2>
        <p class="subtitle">自研模型生成结构化规则 → 规则引擎执行 → 模型解读决策</p>
      </div>
      <div class="header-right">
        <el-tag v-if="selfModels.length" size="small" type="info">
          🧠 自研模型 {{ selfModels.length }} 个
        </el-tag>
        <el-button size="small" @click="saveRule" :disabled="!ruleValid" :loading="saving">
          <el-icon><Download /></el-icon>保存
        </el-button>
        <el-button size="small" @click="loadRuleList">
          <el-icon><FolderOpened /></el-icon>规则库
        </el-button>
      </div>
    </div>

    <!-- 模型选择条 -->
    <div class="model-bar">
      <span class="bar-label">规则生成引擎：</span>
      <el-select v-model="selectedRuleModel" size="small" style="width:200px" @change="onModelChange">
        <el-option label="🤖 Auto (自动选择)" value="auto" />
        <el-option
          v-for="m in selfModels" :key="m.modelCode"
          :label="`🧠 ${m.modelName || m.modelCode}`"
          :value="m.modelCode"
        >
          <span>{{ m.modelName || m.modelCode }}</span>
          <el-tag size="small" type="success" style="margin-left:6px">{{ m.modality || 'text' }}</el-tag>
        </el-option>
      </el-select>
      <el-tag v-if="suggestedModelName" size="small" type="warning">💡 推荐: {{ suggestedModelName }}</el-tag>
    </div>

    <div class="main-layout">
      <!-- ===== 左侧: NL输入 + 规则编辑 ===== -->
      <div class="left-panel">
        <!-- NL → 规则生成 -->
        <el-card body-style="padding:16px" shadow="never">
          <template #header>
            <div class="card-header">
              <span>📝 描述业务规则</span>
              <el-tag v-if="selectedRuleModel !== 'auto'" size="small" type="success">
                🧠 {{ selectedRuleModel }}
              </el-tag>
            </div>
          </template>
          <el-input
            v-model="nlInput" type="textarea" :rows="5"
            placeholder="例如：
• 用户年龄大于60岁，且城市为北京，发送敬老优惠
• 订单金额超过5000元且用户等级为VIP，打9折
• 用户连续登录超过7天未消费，发送流失预警通知
• 购买金额满300减50，不与其他优惠同用"
            @input="onNlInput"
          />
          <!-- 快捷示例 -->
          <div class="examples-row">
            <span class="examples-label">示例：</span>
            <el-tag
              v-for="ex in examples" :key="ex.text"
              size="small" effect="plain" class="example-tag"
              @click="nlInput = ex.text; detectDomain(); onNlInput()">
              📌 {{ ex.label }}
            </el-tag>
          </div>
          <!-- 生成按钮行 -->
          <div class="gen-row">
            <el-button type="primary" :loading="generating" @click="generateRule">
              <el-icon><MagicStick /></el-icon>
              {{ selectedRuleModel === 'auto' ? '🤖 Auto 生成规则' : '🧠 生成规则' }}
            </el-button>
            <el-button :disabled="generating" @click="resetRule">重置</el-button>
            <el-tag v-if="genMs" size="small" type="info">⏱ {{ genMs }}ms</el-tag>
          </div>
          <!-- 流式生成内容 -->
          <div v-if="generating && streamingContent" class="streaming-box">
            <div class="streaming-label">生成中…</div>
            <pre class="streaming-pre">{{ streamingContent }}█</pre>
          </div>
        </el-card>

        <!-- 规则编辑器 -->
        <el-card body-style="padding:16px" shadow="never">
          <template #header>
            <div class="card-header">
              <span>📋 结构化规则 (JSON)</span>
              <div style="display:flex;gap:6px;align-items:center">
                <el-tag v-if="ruleValid" size="small" type="success">✅ 语法正确</el-tag>
                <el-tag v-else size="small" type="danger">⚠️ JSON 错误</el-tag>
                <el-tag size="small" type="info">条件 {{ conditionCount }} | 操作 {{ actionCount }}</el-tag>
                <el-button size="small" link type="primary" @click="formatJson">格式化</el-button>
                <el-button size="small" link type="primary" @click="copyRule">复制</el-button>
              </div>
            </div>
          </template>
          <div class="rule-editor-wrap">
            <el-input
              v-model="ruleJson" type="textarea"
              :rows="14"
              placeholder="规则 JSON..."
              style="font-family:monospace;font-size:12px"
              @input="onRuleEdit"
            />
            <!-- JSON 错误提示 -->
            <div v-if="jsonError" class="json-error">
              <el-icon><WarningFilled /></el-icon> {{ jsonError }}
            </div>
          </div>
          <!-- 规则树视图 -->
          <div v-if="ruleValid && parsedRule" class="rule-tree-view">
            <div class="tree-label">📊 规则结构预览</div>
            <div class="tree-node root-node">
              <span class="tree-icon">📋</span>
              <span class="tree-name">{{ parsedRule.name || '未命名规则' }}</span>
              <el-tag size="small" type="info" style="margin-left:6px">{{ parsedRule.type || 'IF_THEN' }}</el-tag>
            </div>
            <div v-for="(c, i) in (parsedRule.conditions || [])" :key="i" class="tree-node cond-node">
              <span class="tree-connector">├─</span>
              <span class="tree-op">{{ c.operator || '==' }}</span>
              <span class="tree-field">{{ c.field }}</span>
              <span class="tree-val">{{ c.value }}</span>
              <span v-if="c.label" class="tree-label-text">({{ c.label }})</span>
            </div>
            <div v-if="parsedRule.logic" class="tree-node logic-node">
              <span class="tree-connector">└─</span>
              <el-tag size="small" :type="parsedRule.logic === 'AND' ? 'success' : 'warning'">
                {{ parsedRule.logic }}
              </el-tag>
            </div>
            <div v-for="(a, i) in (parsedRule.actions || [])" :key="'a'+i" class="tree-node action-node">
              <span class="tree-connector">└─</span>
              <span class="tree-action-icon">⚡</span>
              <span>{{ a.type }}</span>
              <span v-if="a.params" class="tree-action-params">{{ JSON.stringify(a.params) }}</span>
            </div>
          </div>
        </el-card>

        <!-- 规则模板 -->
        <el-card body-style="padding:12px" shadow="never">
          <template #header><span style="font-weight:600;font-size:13px">📚 规则模板</span></template>
          <div class="tpl-grid">
            <div v-for="tpl in templates" :key="tpl.type" class="tpl-card" @click="applyTemplate(tpl)">
              <div style="font-size:22px;text-align:center">{{ tpl.icon }}</div>
              <div style="font-size:12px;font-weight:600;text-align:center;margin-top:2px">{{ tpl.name }}</div>
              <div style="font-size:10px;color:#909399;text-align:center">{{ tpl.desc }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- ===== 右侧: 测试数据 + 执行结果 ===== -->
      <div class="right-panel">
        <!-- 测试数据 -->
        <el-card body-style="padding:16px" shadow="never">
          <template #header>
            <div class="card-header">
              <span>🧪 测试数据</span>
              <el-button size="small" link type="primary" @click="generateTestData">
                🎲 自动生成
              </el-button>
            </div>
          </template>
          <el-input
            v-model="testData" type="textarea" :rows="8"
            placeholder='{"age": 65, "city": "北京", "userLevel": "VIP", "orderAmount": 8000}'
            style="font-family:monospace;font-size:12px"
            @input="onTestDataChange"
          />
          <!-- 字段预览 (自动高亮匹配条件) -->
          <div v-if="fieldHighlights.length" class="field-highlights">
            <div class="field-highlights-label">🔔 字段匹配：</div>
            <el-tag
              v-for="h in fieldHighlights" :key="h.field"
              size="small" :type="h.matched ? 'success' : 'info'"
              style="margin:2px"
            >
              {{ h.field }} = {{ h.actual }} {{ h.matched ? '✓' : '' }}
            </el-tag>
          </div>
        </el-card>

        <!-- 规则引擎执行 -->
        <el-card body-style="padding:16px" shadow="never">
          <template #header>
            <div class="card-header">
              <span>⚡ 规则引擎</span>
              <div style="display:flex;gap:8px;align-items:center">
                <el-select v-model="engineMode" size="small" style="width:110px">
                  <el-option label="快速匹配" value="fast" />
                  <el-option label="完整评估" value="full" />
                  <el-option label="评分模式" value="score" />
                </el-select>
                <el-button type="danger" size="small" :loading="executing" @click="executeRule">
                  <el-icon><CaretRight /></el-icon>执行规则
                </el-button>
              </div>
            </div>
          </template>

          <!-- 执行结果 -->
          <div v-if="execResult">
            <!-- 最终决策 -->
            <el-result
              :icon="execResult.passed ? 'success' : 'error'"
              :title="execResult.passed ? '✅ 规则命中' : '❌ 规则未命中'"
              :sub-title="execResult.reason"
              style="padding:12px 0"
            >
              <template #extra>
                <el-tag v-if="execResult.action" type="warning">{{ execResult.action }}</el-tag>
                <el-tag v-if="execResult.score !== undefined" type="info">评分: {{ execResult.score }}</el-tag>
              </template>
            </el-result>

            <!-- 条件命中详情 -->
            <div v-if="execResult.details?.length" class="result-section">
              <div class="result-section-title">🔍 条件命中详情</div>
              <el-table :data="execResult.details" size="small" border>
                <el-table-column prop="field" label="字段" width="120" />
                <el-table-column prop="operator" label="操作符" width="80" />
                <el-table-column prop="value" label="阈值" width="100">
                  <template #default="{ row }">{{ row.value }}</template>
                </el-table-column>
                <el-table-column prop="actual" label="实际值" width="100" />
                <el-table-column label="结果" width="70">
                  <template #default="{ row }">
                    <el-tag :type="row.matched ? 'success' : 'danger'" size="small">
                      {{ row.matched ? '✓ 命中' : '✗' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 评分明细 -->
            <div v-if="execResult.scoreDetails?.length" class="result-section">
              <div class="result-section-title">📊 评分明细</div>
              <div v-for="sd in execResult.scoreDetails" :key="sd.name" class="score-row">
                <span class="score-name">{{ sd.name }}</span>
                <el-progress :percentage="sd.percent" :color="sd.color" style="flex:1" :stroke-width="6" />
                <span class="score-val">+{{ sd.score }}</span>
              </div>
              <div class="score-total">总分: <strong style="color:#409eff;font-size:18px">{{ execResult.score }}</strong> / 100</div>
            </div>

            <!-- AI 解读 -->
            <div class="result-section">
              <div class="ai-interpret-header">
                <span class="result-section-title" style="margin:0">🧠 AI 解读</span>
                <el-select v-if="selfModels.length" v-model="selectedExplainModel" size="small" style="width:150px">
                  <el-option label="🤖 Auto" value="auto" />
                  <el-option v-for="m in selfModels" :key="m.modelCode" :label="m.modelName || m.modelCode" :value="m.modelCode" />
                </el-select>
                <el-button size="small" type="primary" :loading="explaining" @click="explainResult">
                  <el-icon><MagicStick /></el-icon>{{ explaining ? '解读中…' : '解读决策' }}
                </el-button>
              </div>
              <div v-if="explainText" class="explain-box">{{ explainText }}</div>
              <el-empty
                v-else-if="!explaining"
                description="点击「解读决策」，模型将分析规则逻辑、命中原因及业务建议"
                :image-size="60"
                class="explain-empty"
              >
                <el-button type="primary" size="small" :loading="explaining" @click="explainResult">
                  <el-icon><MagicStick /></el-icon>开始解读
                </el-button>
              </el-empty>
            </div>
          </div>

          <el-empty v-else description="输入规则和测试数据，点击「执行规则」" :image-size="60" />
        </el-card>

        <!-- 执行历史 -->
        <el-card body-style="padding:12px" shadow="never">
          <template #header>
            <div class="card-header">
              <span style="font-weight:600;font-size:13px">📜 执行历史</span>
              <el-tag size="small" type="info">{{ historyList.length }} 条</el-tag>
            </div>
          </template>
          <div v-if="historyList.length" class="history-list">
            <div v-for="(h, idx) in historyList.slice(0, 5)" :key="idx" class="history-item" @click="loadFromHistory(h)">
              <div class="history-top">
                <el-tag :type="h.passed ? 'success' : 'danger'" size="small">{{ h.passed ? '命中' : '未命中' }}</el-tag>
                <span class="history-time">{{ h.executedAt }}</span>
              </div>
              <div class="history-desc">{{ h.ruleName || h.nl || '规则执行' }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无执行历史" :image-size="40" />
        </el-card>
      </div>
    </div>

    <!-- 规则库抽屉 -->
    <el-drawer v-model="ruleLibVisible" title="📚 规则库" size="55%" direction="rtl">
      <div class="rule-lib">
        <div class="lib-toolbar">
          <el-input v-model="libSearch" size="small" placeholder="搜索规则名称…" style="width:200px" clearable />
          <el-select v-model="libFilterType" size="small" style="width:120px" clearable placeholder="规则类型">
            <el-option label="IF_THEN" value="IF_THEN" />
            <el-option label="SCORE" value="SCORE" />
            <el-option label="DECISION_TABLE" value="DECISION_TABLE" />
          </el-select>
          <el-button size="small" type="primary" @click="loadRuleList">刷新</el-button>
        </div>
        <el-table :data="filteredRuleLib" size="small" v-loading="libLoading" stripe
          :empty-text="libEmptyText">
          <template #empty>
            <el-empty v-if="!libLoading" :description="libEmptyText" :image-size="80">
              <el-button v-if="libSearch || libFilterType" type="primary" size="small" @click="clearLibFilter">清除筛选</el-button>
            </el-empty>
          </template>
          <el-table-column prop="name" label="规则名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.type || 'IF_THEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="conditionCount" label="条件数" width="70" align="center" />
          <el-table-column prop="updatedAt" label="更新时间" width="140" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="loadRule(row)">加载</el-button>
              <el-button size="small" link type="success" @click="loadAndExecute(row)">执行</el-button>
              <el-button size="small" link type="danger" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, CaretRight, Download, FolderOpened, WarningFilled } from '@element-plus/icons-vue'
import { listEnabledModels, modelApi } from '@/api/model'
import { nl2sqlHistory, nl2sqlAsk } from '@/api/analytics'
import { ruleApi } from '@/api/rule'

// ===== 示例提示词 =====
const examples = [
  { label: '敬老优惠', text: '如果用户年龄大于60岁，且城市为北京，发送敬老优惠' },
  { label: 'VIP折扣', text: '订单金额超过5000元且用户等级为VIP，打9折' },
  { label: '流失预警', text: '用户连续登录超过7天未消费，发送流失预警通知' },
  { label: '满减活动', text: '购买金额满300减50，不与其他优惠同用' },
]

// ===== 规则模板 =====
const templates = [
  { name: '年龄分层', icon: '👤', desc: '按年龄段打折', type: 'discount' },
  { name: 'VIP优惠', icon: '⭐', desc: 'VIP等级折扣', type: 'vip' },
  { name: '满减活动', icon: '💰', desc: '满N减M优惠', type: 'threshold' },
  { name: '流失预警', icon: '⚠️', desc: '用户流失检测', type: 'churn' },
  { name: '风险评估', icon: '🔒', desc: '信贷风险评分', type: 'risk' },
  { name: '推荐商品', icon: '🛍️', desc: '个性化推荐', type: 'recommend' },
]

function defaultRuleTemplate() {
  return JSON.stringify({
    name: '折扣规则', type: 'IF_THEN',
    conditions: [
      { field: 'age', operator: '>', value: 60, label: '年龄大于60岁' },
      { field: 'city', operator: '==', value: '北京', label: '城市为北京' },
    ],
    logic: 'AND',
    actions: [{ type: 'send_coupon', params: { code: 'ELDER_DISCOUNT', amount: 50 } }],
    priority: 1,
  }, null, 2)
}

// ===== 状态 =====
const nlInput = ref('')
const ruleJson = ref(defaultRuleTemplate())
const testData = ref('{\n  "age": 65,\n  "city": "北京",\n  "userLevel": "VIP",\n  "orderAmount": 8000\n}')
const engineMode = ref('fast')
const generating = ref(false)
const executing = ref(false)
const saving = ref(false)
const streamingContent = ref('')
const execResult = ref(null)
const genMs = ref(0)
const jsonError = ref('')

// 自研模型
const allModels = ref([])
const selfModels = computed(() =>
  allModels.value.filter(m =>
    m.enabled !== false &&
    (m.providerCode === 'self-trained' || m.providerCode === 'trained' ||
     m.providerCode === 'self' || m.providerCode === 'self-developed' ||
     m.providerCode === 'local' || m.providerCode === 'minimax')
  )
)
const selectedRuleModel = ref('auto')
const selectedExplainModel = ref('auto')
const suggestedModelName = ref('')
const explaining = ref(false)
const explainText = ref('')

// 字段高亮
const fieldHighlights = ref([])

// 执行历史
const historyList = ref([])

// 规则库
const ruleLibVisible = ref(false)
const ruleLib = ref([])
const libLoading = ref(false)
const libSearch = ref('')
const libFilterType = ref('')
const filteredRuleLib = computed(() => {
  let list = ruleLib.value
  if (libSearch.value) list = list.filter(r => (r.name || '').includes(libSearch.value))
  if (libFilterType.value) list = list.filter(r => (r.type || '') === libFilterType.value)
  return list
})

const libEmptyText = computed(() => {
  if (libLoading.value) return '加载中...'
  if (libSearch.value || libFilterType.value) return '没有匹配的规则'
  return '规则库为空，先生成或保存规则后再来查看'
})

function clearLibFilter() {
  libSearch.value = ''
  libFilterType.value = ''
}

// ===== 规则语法 =====
const parsedRule = computed(() => {
  try { return JSON.parse(ruleJson.value) } catch { return null }
})
const ruleValid = computed(() => {
  try {
    const r = JSON.parse(ruleJson.value)
    return r && (r.conditions || r.rules || r.type)
  } catch { return false }
})
const conditionCount = computed(() => {
  try {
    const r = JSON.parse(ruleJson.value)
    return r.conditions?.length || r.rules?.length || 0
  } catch { return 0 }
})
const actionCount = computed(() => {
  try {
    const r = JSON.parse(ruleJson.value)
    return r.actions?.length || 0
  } catch { return 0 }
})

// ===== 加载模型列表 =====
async function loadModels() {
  try {
    const r = await listEnabledModels()
    const list = r?.data || r || []
    allModels.value = Array.isArray(list) ? list : []
    if (selfModels.value.length && selectedRuleModel.value === 'auto') {
      selectedRuleModel.value = selfModels.value[0].modelCode
    }
  } catch {
    allModels.value = []
  }
}

// ===== 领域检测 =====
const DOMAIN_MAP = [
  { keywords: /金融|银行|理财|信贷|风控|信用|贷款/, modelCode: 'fin-gpt', name: '金融大模型' },
  { keywords: /法律|法规|合规|诉讼|合同|律师/, modelCode: 'law-gpt', name: '法律大模型' },
  { keywords: /医疗|诊断|药品|医生|医院|疾病/, modelCode: 'med-gpt', name: '医疗大模型' },
  { keywords: /代码|编程|函数|算法|接口|bug|debug/, modelCode: 'code-gpt', name: '代码大模型' },
  { keywords: /客服|对话|问答|聊天/, modelCode: 'qa-gpt', name: '问答大模型' },
  { keywords: /营销|推广|广告|获客|转化/, modelCode: 'chat-gpt', name: '营销大模型' },
]
function detectDomain() {
  const nl = nlInput.value.toLowerCase()
  for (const d of DOMAIN_MAP) {
    if (d.keywords.test(nl)) {
      const found = selfModels.value.find(m => m.modelCode === d.modelCode)
      suggestedModelName.value = found ? d.name : ''
      if (found && selfModels.value.length > 1) selectedRuleModel.value = d.modelCode
      return
    }
  }
  suggestedModelName.value = ''
}
function onModelChange() { explainText.value = '' }
function onNlInput() { detectDomain(); fieldHighlights.value = [] }
function onTestDataChange() { updateFieldHighlights() }
function onRuleEdit() {
  try { JSON.parse(ruleJson.value); jsonError.value = '' } catch (e) { jsonError.value = e.message }
  updateFieldHighlights()
}

// ===== 字段高亮 =====
function updateFieldHighlights() {
  if (!parsedRule.value) { fieldHighlights.value = []; return }
  const data = tryParseJson(testData.value)
  if (!data) { fieldHighlights.value = []; return }
  const conds = parsedRule.value.conditions || []
  fieldHighlights.value = conds.map(c => {
    const actual = data[c.field]
    const matched = evalCondition(actual, c.operator, c.value)
    return { field: c.field, actual: actual ?? '(undefined)', matched: matched === true }
  })
}
function tryParseJson(str) {
  try { return JSON.parse(str) } catch { return null }
}
function evalCondition(actual, operator, threshold) {
  if (actual === undefined || actual === null) return false
  switch (operator) {
    case '>':  return actual > threshold
    case '>=': return actual >= threshold
    case '<':  return actual < threshold
    case '<=': return actual <= threshold
    case '==': return String(actual) === String(threshold)
    case '!=': return String(actual) !== String(threshold)
    case 'contains': return String(actual).includes(String(threshold))
    case 'in': return Array.isArray(threshold) && threshold.includes(actual)
    default: return false
  }
}

// ===== LLM 生成规则 (直接调 modelApi) =====
async function generateRule() {
  if (!nlInput.value.trim()) { ElMessage.warning('请先描述业务规则'); return }
  generating.value = true
  genMs.value = 0
  streamingContent.value = ''
  const start = Date.now()
  try {
    let model = selectedRuleModel.value
    if (model === 'auto') {
      model = selfModels.value[0]?.modelCode || 'auto'
    }

    const prompt = `你是一个业务规则工程师。请将以下自然语言描述的业务规则，转换为严格合法的 JSON 结构化规则。

要求：
1. 返回纯 JSON，不要任何解释文字，不要 markdown 代码块标记
2. JSON 必须包含: name(规则名称), type(固定"IF_THEN"), conditions(数组,每项含field/operator/value/label), logic(AND/OR), actions(数组,每项含type/params), priority
3. operator 支持: >, >=, <, <=, ==, !=, contains, in
4. action type 支持: send_coupon, discount, notify, recommend, pass, reject
5. 如果是优惠/折扣类规则，actions 用 discount 或 send_coupon
6. 如果是预警/通知类规则，actions 用 notify
7. 如果是风控/评分类，type 可用 "SCORE"
8. JSON 的 name 字段用中文描述规则用途

自然语言规则: ${nlInput.value}

直接返回 JSON：`

    let ruleText = ''
    try {
      const r = await modelApi.chat({ model, messages: [{ role: 'user', content: prompt }] })
      ruleText = r?.data?.content || r?.content || r?.text || ''
    } catch {
      // 降级到本地解析
      ruleText = ''
    }

    genMs.value = Date.now() - start

    // 从返回文本中提取 JSON
    const jsonMatch = ruleText.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      try {
        const parsed = JSON.parse(jsonMatch[0])
        ruleJson.value = JSON.stringify(parsed, null, 2)
        jsonError.value = ''
        ElMessage.success(`🧠 规则生成完成 (${genMs.value}ms)`)
        updateFieldHighlights()
      } catch {
        ruleJson.value = buildSimpleRule(nlInput.value)
        ElMessage.warning('JSON 解析失败，使用简化解析')
      }
    } else if (ruleText.trim()) {
      ruleJson.value = buildSimpleRule(nlInput.value)
      ElMessage.warning('未提取到 JSON，使用简化解析')
    } else {
      ruleJson.value = buildSimpleRule(nlInput.value)
    }
  } catch (e) {
    genMs.value = Date.now() - start
    ruleJson.value = buildSimpleRule(nlInput.value)
    ElMessage.error('生成失败: ' + (e.message || '') + '，已使用简化解析')
  } finally {
    generating.value = false
    streamingContent.value = ''
  }
}

/** 本地 NL → Rule 解析（不依赖 LLM API） */
function buildSimpleRule(nl) {
  const conditions = [], actions = []
  const numPatterns = [
    [/(?:年龄|age)[^\d]*>?\s*(\d+)/, 'age'],
    [/(?:订单金额|消费|金额|amount)[^\d]*>?\s*(\d+)/, 'orderAmount'],
    [/(?:天数|连续|days?)[^\d]*>?\s*(\d+)/, 'days'],
    [/(?:满|threshold)[^\d]*?(\d+)/, 'orderAmount'],
  ]
  for (const [re, field] of numPatterns) {
    const m = nl.match(re)
    if (m) conditions.push({ field, operator: nl.includes('满') ? '>=' : '>', value: parseInt(m[1]), label: `${field} ${nl.includes('满') ? '>=' : '>'} ${m[1]}` })
  }
  if (/北京|上海|深圳/.test(nl)) conditions.push({ field: 'city', operator: '==', value: '北京', label: '城市=北京' })
  if (/VIP|高级|钻石/.test(nl)) conditions.push({ field: 'userLevel', operator: '==', value: 'VIP', label: '等级=VIP' })
  if (/老年|敬老|60/.test(nl)) conditions.push({ field: 'age', operator: '>', value: 60, label: '年龄>60' })
  if (/优惠|折扣|打折/.test(nl)) {
    const disc = nl.match(/(\d+)[%折]/) || nl.match(/减(\d+)/)
    actions.push({ type: 'discount', params: { type: disc?.[1]?.includes('%') ? 'percent' : 'amount', value: disc ? parseInt(disc[1]) : 10 } })
  }
  if (/预警|通知|提醒/.test(nl)) actions.push({ type: 'notify', params: { channel: 'sms', template: '流失预警' } })
  if (/推荐/.test(nl)) actions.push({ type: 'recommend', params: {} })
  if (!actions.length) actions.push({ type: 'pass', params: {} })
  return JSON.stringify({ name: 'NL生成规则', type: 'IF_THEN', conditions, logic: /且|并且/.test(nl) ? 'AND' : 'OR', actions, priority: 1 }, null, 2)
}

// ===== 规则执行引擎 =====
async function executeRule() {
  if (!ruleValid.value) { ElMessage.error('规则格式错误，请检查 JSON'); return }
  let data
  try { data = JSON.parse(testData.value) } catch { ElMessage.error('测试数据 JSON 格式错误'); return }
  executing.value = true
  execResult.value = null
  try {
    const rule = JSON.parse(ruleJson.value)
    const result = runRuleEngine(rule, data)
    execResult.value = result
    // 保存历史
    historyList.value.unshift({
      nl: nlInput.value,
      ruleName: rule.name || '未命名规则',
      rule: ruleJson.value,
      testData: testData.value,
      passed: result.passed,
      reason: result.reason,
      action: result.action,
      score: result.score,
      executedAt: new Date().toLocaleString(),
    })
    if (historyList.value.length > 50) historyList.value.pop()
    ElMessage.success(`规则执行完成：${result.passed ? '✅ 命中' : '❌ 未命中'}`)
  } catch (e) {
    ElMessage.error('执行失败: ' + (e.message || '未知错误'))
  } finally {
    executing.value = false
  }
}

function runRuleEngine(rule, data) {
  const conditions = rule.conditions || []
  const details = []
  let allMatched = true
  for (const cond of conditions) {
    const actual = data[cond.field]
    const threshold = cond.value
    let matched = evalCondition(actual, cond.operator, threshold)
    details.push({ field: cond.field, operator: cond.operator, value: threshold, actual: actual ?? '(undefined)', matched })
    if (!matched) allMatched = false
  }
  if (rule.logic === 'OR') allMatched = details.some(d => d.matched)
  else if (rule.logic === 'AND') allMatched = details.every(d => d.matched)

  // 评分模式
  let score = 0; const scoreDetails = []
  if (engineMode.value === 'score') {
    for (const cond of conditions) {
      const actual = data[cond.field]
      const base = 25; let condScore = 0, percent = 0, color = '#67c23a'
      if (actual !== undefined && actual !== null) {
        if (typeof actual === 'number' && typeof cond.value === 'number') {
          const ratio = Math.min(actual / cond.value, 2)
          condScore = Math.round(base * Math.min(ratio, 1))
          percent = Math.round(Math.min(ratio, 1) * 100)
          color = percent >= 100 ? '#67c23a' : percent >= 70 ? '#e6a23c' : '#f56c6c'
        } else if (String(actual) === String(cond.value)) { condScore = base; percent = 100 }
      }
      score += condScore; scoreDetails.push({ name: cond.field, score: condScore, percent, color })
    }
    score = Math.min(score, 100); allMatched = score >= 60
  }

  // 执行操作
  let actionLabel = ''
  if (allMatched && rule.actions?.length) {
    const act = rule.actions[0]
    switch (act.type) {
      case 'send_coupon': actionLabel = `🎫 发放优惠券: ${act.params?.code} (面值 ${act.params?.amount}元)`; break
      case 'discount': actionLabel = act.params?.type === 'percent' ? `🎁 折扣: ${act.params?.value}%` : `🎁 优惠: 减${act.params?.value}元`; break
      case 'notify': actionLabel = `📱 通知: ${act.params?.channel} - ${act.params?.template}`; break
      case 'recommend': actionLabel = '🛍️ 个性化推荐'; break
      case 'reject': actionLabel = '🚫 拒绝申请'; break
      case 'pass': actionLabel = '✅ 审核通过'; break
      default: actionLabel = `⚙️ 执行: ${act.type}`
    }
  }
  return {
    passed: allMatched,
    reason: allMatched ? `命中所有条件 (${rule.logic})` : `未满足条件，${details.filter(d => !d.matched).map(d => d.field).join(', ')} 不匹配`,
    action: actionLabel, details,
    score: engineMode.value === 'score' ? score : undefined,
    scoreDetails: engineMode.value === 'score' ? scoreDetails : undefined,
  }
}

// ===== 工具 =====
function formatJson() {
  try {
    const r = JSON.parse(ruleJson.value)
    ruleJson.value = JSON.stringify(r, null, 2)
    jsonError.value = ''
    ElMessage.success('JSON 已格式化')
  } catch (e) { ElMessage.error('不是有效 JSON: ' + e.message) }
}
async function copyRule() {
  try {
    await navigator.clipboard.writeText(ruleJson.value)
    ElMessage.success('规则已复制到剪贴板')
  } catch {
    // 降级：使用 document.execCommand
    try {
      const ta = document.createElement('textarea')
      ta.value = ruleJson.value
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('规则已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败，请手动选中复制')
    }
  }
}
function resetRule() {
  nlInput.value = ''
  ruleJson.value = defaultRuleTemplate()
  jsonError.value = ''
  execResult.value = null
  explainText.value = ''
  fieldHighlights.value = []
  ElMessage.success('已重置为默认模板')
}

function applyTemplate(tpl) {
  const templates = {
    discount: { name: '年龄折扣规则', type: 'IF_THEN', priority: 1, conditions: [{ field: 'age', operator: '>=', value: 60, label: '年龄>=60岁' }, { field: 'age', operator: '<', value: 80, label: '年龄<80岁' }], logic: 'AND', actions: [{ type: 'send_coupon', params: { code: 'ELDER_DISCOUNT', amount: 30 } }] },
    vip: { name: 'VIP折扣规则', type: 'IF_THEN', priority: 2, conditions: [{ field: 'userLevel', operator: '==', value: 'VIP', label: '等级=VIP' }, { field: 'orderAmount', operator: '>', value: 1000, label: '订单>1000元' }], logic: 'AND', actions: [{ type: 'discount', params: { type: 'percent', value: 15 } }] },
    threshold: { name: '满减活动规则', type: 'IF_THEN', priority: 3, conditions: [{ field: 'orderAmount', operator: '>=', value: 300, label: '满300元' }], logic: 'AND', actions: [{ type: 'discount', params: { type: 'amount', value: 50 } }] },
    churn: { name: '流失预警规则', type: 'IF_THEN', priority: 4, conditions: [{ field: 'lastLoginDays', operator: '>', value: 7, label: '7天未登录' }, { field: 'hasPurchase', operator: '==', value: false, label: '未消费' }], logic: 'AND', actions: [{ type: 'notify', params: { channel: 'sms', template: '流失预警' } }] },
    risk: { name: '信贷风险评分', type: 'SCORE', priority: 5, conditions: [{ field: 'creditScore', operator: '>=', value: 700, label: '信用分>=700' }, { field: 'incomeMonthly', operator: '>=', value: 5000, label: '月收入>=5000' }, { field: 'debtRatio', operator: '<', value: 0.5, label: '负债率<50%' }, { field: 'employmentYears', operator: '>=', value: 1, label: '工作>=1年' }], logic: 'AND', actions: [{ type: 'pass', params: {} }] },
    recommend: { name: '个性化推荐规则', type: 'IF_THEN', priority: 6, conditions: [{ field: 'browseCount', operator: '>', value: 10, label: '浏览>10次' }], logic: 'AND', actions: [{ type: 'recommend', params: {} }] },
  }
  const t = templates[tpl.type]
  if (t) { ruleJson.value = JSON.stringify(t, null, 2); nlInput.value = tpl.desc; jsonError.value = ''; updateFieldHighlights(); ElMessage.success(`已加载「${tpl.name}」模板`) }
}

function generateTestData() {
  const data = {
    age: Math.floor(Math.random() * 50) + 18,
    city: ['北京', '上海', '深圳', '广州', '杭州'][Math.floor(Math.random() * 5)],
    userLevel: ['NORMAL', 'SILVER', 'GOLD', 'VIP', 'PLATINUM'][Math.floor(Math.random() * 5)],
    orderAmount: Math.floor(Math.random() * 10000) + 500,
    lastLoginDays: Math.floor(Math.random() * 30),
    hasPurchase: Math.random() > 0.3,
    creditScore: Math.floor(Math.random() * 300) + 500,
    incomeMonthly: Math.floor(Math.random() * 20000) + 3000,
    debtRatio: Math.round(Math.random() * 100) / 100,
    employmentYears: Math.floor(Math.random() * 10),
    browseCount: Math.floor(Math.random() * 30),
    favoriteCategory: ['电子产品', '服装', '食品', ''][Math.floor(Math.random() * 4)],
  }
  testData.value = JSON.stringify(data, null, 2)
  updateFieldHighlights()
  ElMessage.success('已生成随机测试数据')
}

function loadFromHistory(h) {
  nlInput.value = h.nl || ''
  ruleJson.value = h.rule || defaultRuleTemplate()
  testData.value = h.testData || '{}'
  execResult.value = null
  explainText.value = ''
  updateFieldHighlights()
}

// ===== 规则库 =====
async function loadRuleList() {
  ruleLibVisible.value = true
  libLoading.value = true
  try {
    // 复用 NL2SQL 历史作为规则历史存储
    const r = await nl2sqlHistory({ page: 1, size: 50 })
    const list = (r?.data || []).map(h => ({
      id: h.id, name: h.question?.substring(0, 40) || '规则', type: 'IF_THEN',
      rule: h.generatedSql || h.correctedSql || '{}',
      conditionCount: (() => { try { return JSON.parse(h.generatedSql || '{}').conditions?.length || 0 } catch { return 0 } })(),
      updatedAt: h.createdAt ? new Date(h.createdAt).toLocaleString() : '-',
    }))
    ruleLib.value = list
  } catch (e) {
    ruleLib.value = []
    ElMessage.error('加载规则库失败：' + (e.response?.data?.message || e.message || '请稍后重试'))
  } finally {
    libLoading.value = false
  }
}
function loadRule(r) {
  try { ruleJson.value = JSON.stringify(JSON.parse(r.rule), null, 2) } catch { ruleJson.value = r.rule }
  ruleLibVisible.value = false
  updateFieldHighlights()
  ElMessage.success(`规则「${r.name}」已加载`)
}
function loadAndExecute(r) {
  loadRule(r)
  setTimeout(() => executeRule(), 200)
}
async function deleteRule(r) {
  // 备份以便失败回滚
  const original = ruleLib.value
  try {
    await ElMessageBox.confirm(`确定删除规则「${r.name}」?该操作不可恢复。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await ruleApi.remove(r.id)
    ruleLib.value = ruleLib.value.filter(x => x.id !== r.id)
    ElMessage.success(`规则「${r.name}」已删除`)
  } catch (e) {
    // 失败还原本地数组
    ruleLib.value = original
    ElMessage.error('删除失败：' + (e.__result?.message || e.response?.data?.message || e.message || '请稍后重试'))
  }
}

// ===== 保存规则 =====
async function saveRule() {
  if (!ruleValid.value) {
    ElMessage.error('规则 JSON 格式错误，请先修正')
    return
  }
  saving.value = true
  try {
    const rule = JSON.parse(ruleJson.value)
    if (!rule.name?.trim()) {
      ElMessage.warning('请填写规则名称')
      saving.value = false
      return
    }
    // T1-mock-fix: 改为真实后端调用
    const res = await ruleApi.create({
      name: rule.name,
      json: ruleJson.value,
      scope: 'global',
      enabled: true,
    })
    const newId = res?.data ?? res
    ElMessage.success(`规则「${rule.name}」已保存 (id: ${newId})`)
    // 刷新规则库列表 (在后台异步执行, 不阻塞)
    loadRuleList().catch(() => {})
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.__result?.message || e.response?.data?.message || e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// ===== AI 解读 =====
async function explainResult() {
  if (!execResult.value) {
    ElMessage.warning('请先执行规则，再进行 AI 解读')
    return
  }
  explaining.value = true
  explainText.value = ''
  try {
    let model = selectedExplainModel.value
    if (model === 'auto') model = selectedRuleModel.value !== 'auto' ? selectedRuleModel.value : (selfModels.value[0]?.modelCode || '')
    const prompt = `你是一个专业的业务规则分析专家。请解读以下规则引擎的执行结果，给出业务建议。
【规则】${ruleJson.value}
【测试数据】${testData.value}
【执行结果】${JSON.stringify(execResult.value, null, 2)}
请用中文简洁分析：1. 为什么该规则被命中（或未命中）？2. 每个条件的实际值是否合理？3. 对业务运营有什么建议？`
    const r = await modelApi.chat({ model, messages: [{ role: 'user', content: prompt }] })
    explainText.value = (r?.data?.content || r?.content || r?.text || '').trim() || '模型未返回有效解读，请稍后重试'
  } catch (e) {
    explainText.value = `解读失败: ${e.message || '未知错误'}\n\n请检查：\n1. 模型服务是否可用\n2. 网络连接是否正常\n3. 是否已配置 API Key`
  } finally {
    explaining.value = false
  }
}

// ===== 启动 =====
onMounted(() => { loadModels(); updateFieldHighlights() })
</script>

<style lang="scss" scoped>
.rule-page { display: flex; flex-direction: column; height: 100%; padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 12px 16px; background: #fff; border-bottom: 1px solid #eee; gap: 12px;
  h2 { margin: 0; font-size: 16px; }
}
.header-left { display: flex; flex-direction: column; gap: 2px; }
.header-right { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
.subtitle { margin: 2px 0 0; font-size: 12px; color: #909399; }
.model-bar {
  display: flex; align-items: center; gap: 10px; padding: 8px 16px;
  background: #f5f7fa; border-bottom: 1px solid #eee;
}
.bar-label { font-size: 13px; color: #606266; white-space: nowrap; }
.main-layout { display: flex; flex: 1; overflow: hidden; gap: 0; }
.left-panel { width: 48%; border-right: 1px solid #eee; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.right-panel { flex: 1; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.card-header { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.examples-row { display: flex; align-items: center; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.examples-label { font-size: 12px; color: #909399; white-space: nowrap; }
.example-tag { cursor: pointer; }
.gen-row { display: flex; gap: 8px; margin-top: 10px; align-items: center; }
.streaming-box { margin-top: 8px; background: #f0f9eb; border-radius: 6px; padding: 8px 12px; }
.streaming-label { font-size: 11px; color: #67c23a; margin-bottom: 4px; }
.streaming-pre { font-family: monospace; font-size: 12px; color: #67c23a; white-space: pre-wrap; margin: 0; }
.rule-editor-wrap { position: relative; }
.json-error { color: #f56c6c; font-size: 12px; padding: 4px 0; display: flex; align-items: center; gap: 4px; }
.rule-tree-view { margin-top: 10px; background: #f9f9f9; border-radius: 6px; padding: 10px 12px; }
.tree-label { font-size: 12px; font-weight: 600; color: #606266; margin-bottom: 6px; }
.tree-node { font-size: 12px; line-height: 1.8; font-family: monospace; }
.tree-icon { margin-right: 4px }
.tree-name { font-weight: 600; }
.tree-connector { color: #909399; margin-right: 4px; }
.tree-op { color: #409eff; margin-right: 4px; }
.tree-field { color: #e6a23c; margin-right: 4px; }
.tree-val { color: #67c23a; margin-right: 4px; }
.tree-label-text { color: #909399; font-size: 11px; }
.tree-action-params { color: #909399; font-size: 11px; margin-left: 4px; }
.tpl-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.tpl-card {
  padding: 8px 6px; border: 1px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all .15s;
  &:hover { border-color: #409eff; background: #ecf5ff; }
}
.field-highlights { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; align-items: center; }
.field-highlights-label { font-size: 12px; color: #606266; white-space: nowrap; }
.result-section { margin-top: 12px; }
.result-section-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.score-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.score-name { font-size: 12px; width: 90px; flex-shrink: 0; }
.score-val { font-size: 12px; width: 40px; text-align: right; color: #67c23a; }
.score-total { font-size: 13px; text-align: right; margin-top: 6px; }
.ai-interpret-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.explain-box {
  background: linear-gradient(135deg, #f0f9eb, #ecf5ff);
  border: 1px solid #d4edda; border-radius: 8px; padding: 10px 12px;
  font-size: 13px; line-height: 1.8; white-space: pre-wrap;
}
.explain-placeholder { font-size: 12px; color: #c0c4cc; text-align: center; padding: 8px; }
.history-list { display: flex; flex-direction: column; gap: 6px; }
.history-item {
  padding: 6px 10px; border-radius: 4px; cursor: pointer;
  border: 1px solid transparent; transition: all .15s;
  &:hover { border-color: #409eff; background: #f0f7ff; }
}
.history-top { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
.history-time { font-size: 11px; color: #909399; }
.history-desc { font-size: 12px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rule-lib { padding: 12px; }
.lib-toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; }

// ================================================================
// H5 移动端
// ================================================================
@media (max-width: 768px) {
  .page-header { padding: 8px 12px; h2 { font-size: 14px; } }
  .model-bar { flex-wrap: wrap; padding: 8px 12px; }
  .main-layout { flex-direction: column; overflow: visible; }
  .left-panel, .right-panel { width: 100%; border-right: none; overflow: visible; }
  .left-panel { gap: 8px; }
  .gen-row :deep(.el-button) { font-size: 13px; padding: 8px 12px; }
  .tpl-grid { grid-template-columns: repeat(2, 1fr); }
  .rule-lib { padding: 8px; }
  .lib-toolbar { flex-wrap: wrap; }
}
@media (max-width: 400px) {
  .tpl-grid { grid-template-columns: repeat(2, 1fr); }
  .header-right :deep(.el-tag) { display: none; }
}
</style>
