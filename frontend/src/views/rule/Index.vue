<!-- @file rule/Index.vue - NL 规则助手 V7.2 (自研模型 + 规则引擎) -->
<template>
  <div class="page-card">
    <!-- 顶部 -->
    <div class="page-header">
      <div>
        <h2>⚙️ NL 规则助手 <el-tag size="small" type="success">自研模型 + 规则引擎</el-tag></h2>
        <p style="margin:4px 0 0;font-size:12px;color:#909399">
          选择自研模型 → NL 描述业务规则 → 模型生成结构化 JSON → 规则引擎执行 → 模型解读决策结果
        </p>
      </div>
      <div style="display:flex;gap:8px;align-items:center">
        <el-tag v-if="selfModels.length" size="small" type="info">
          ☁️ 自研模型 {{ selfModels.length }} 个可用
        </el-tag>
        <el-button size="small" @click="loadHistory">
          <el-icon><Refresh /></el-icon>历史
        </el-button>
      </div>
    </div>

    <!-- 自研模型状态条 -->
    <div v-if="selfModels.length" class="model-status-bar">
      <span style="font-size:12px;color:#606266">规则生成引擎：</span>
      <el-select v-model="selectedRuleModel" size="small" style="width:200px" placeholder="选择模型" @change="onModelChange">
        <el-option-group label="☁️ 云端模型">
          <el-option label="🤖 Auto (智能选择)" value="auto" />
        </el-option-group>
        <el-option-group label="🧠 自研模型">
          <el-option
            v-for="m in selfModels"
            :key="m.modelCode"
            :label="`${m.modelName || m.modelCode} ${m.providerCode ? '(' + m.providerCode + ')' : ''}`"
            :value="m.modelCode"
          >
            <span>{{ m.modelName || m.modelCode }}</span>
            <el-tag size="small" type="success" style="margin-left:6px">{{ m.modality || 'text' }}</el-tag>
          </el-option>
        </el-option-group>
      </el-select>
      <el-tag v-if="selectedRuleModel && selectedRuleModel !== 'auto'" size="small" type="success">
        ✓ {{ selectedRuleModel }}
      </el-tag>
      <el-tag v-if="suggestedModelName" size="small" type="warning">
        💡 推荐: {{ suggestedModelName }}
      </el-tag>
    </div>

    <el-row :gutter="16">
      <!-- ===== 左侧: NL输入 + 规则编辑 ===== -->
      <el-col :span="12">
        <!-- NL → 规则生成区 -->
        <el-card body-style="padding:16px" style="margin-bottom:12px">
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <span style="font-weight:600">📝 描述业务规则</span>
              <el-tag v-if="selectedRuleModel && selectedRuleModel !== 'auto'" size="small" type="success">
                🧠 {{ selectedRuleModel }}
              </el-tag>
            </div>
          </template>
          <el-input
            v-model="nlInput"
            type="textarea"
            :rows="4"
            placeholder="例如：
• 如果用户年龄大于60岁，且城市为北京，发送敬老优惠
• 订单金额超过5000元且用户等级为VIP，打9折
• 用户连续登录超过7天未消费，发送流失预警通知
• 购买金额满300减50，不与其他优惠同用"
            style="font-size:13px"
            @input="detectDomain"
          />
          <div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">
            <el-tag v-for="ex in examples" :key="ex" size="small" effect="plain"
              style="cursor:pointer" @click="nlInput = ex; detectDomain()">📌 {{ ex.slice(0,20) }}…</el-tag>
          </div>
          <div style="margin-top:10px;display:flex;gap:8px">
            <el-button type="primary" :loading="generating" @click="generateRule">
              <el-icon><MagicStick /></el-icon>
              {{ selectedRuleModel === 'auto' ? '🤖 Auto 生成' : '🧠 ' + selectedRuleModel }}
            </el-button>
            <el-button :disabled="generating" @click="nlInput = ''; ruleJson = defaultRuleTemplate()">
              重置
            </el-button>
            <!-- 规则生成耗时 -->
            <el-tag v-if="genMs" size="small" type="info">⏱ {{ genMs }}ms</el-tag>
          </div>
          <!-- 生成状态 -->
          <div v-if="generating" style="margin-top:8px;font-size:12px;color:#409eff">
            <span v-if="selectedRuleModel === 'auto'">🤖 Auto 模式：Agent 智能生成中…</span>
            <span v-else>🧠 {{ selectedRuleModel }} 生成中，请稍候…</span>
          </div>
        </el-card>

        <!-- 规则编辑器 -->
        <el-card body-style="padding:16px">
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <span style="font-weight:600">📋 结构化规则 (JSON)</span>
              <el-tag size="small" type="info">支持 IF-THEN / 评分卡 / 决策表</el-tag>
              <el-button size="small" link type="primary" style="margin-left:auto" @click="formatJson">
                格式化
              </el-button>
            </div>
          </template>
          <el-input v-model="ruleJson" type="textarea" :rows="12" placeholder="规则 JSON..." style="font-family:monospace;font-size:12px" />
          <div style="margin-top:8px;font-size:11px;color:#909399">
            <span v-if="ruleValid" style="color:#67c23a">✅ 规则语法正确</span>
            <span v-else style="color:#f56c6c">⚠️ JSON 格式错误，请检查</span>
            <span style="margin-left:12px">条件 {{ conditionCount }} 个 | 操作 {{ actionCount }} 个</span>
          </div>
        </el-card>
      </el-col>

      <!-- ===== 右侧: 测试数据 + 执行结果 ===== -->
      <el-col :span="12">
        <!-- 测试数据输入 -->
        <el-card body-style="padding:16px" style="margin-bottom:12px">
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <span style="font-weight:600">🧪 测试数据</span>
              <el-tag size="small">JSON 格式</el-tag>
              <el-button size="small" link type="primary" style="margin-left:auto" @click="generateTestData">
                🎲 自动生成
              </el-button>
            </div>
          </template>
          <el-input v-model="testData" type="textarea" :rows="6" placeholder='{"age": 65, "city": "北京", "userLevel": "VIP", "orderAmount": 8000}' style="font-family:monospace;font-size:12px" />
        </el-card>

        <!-- 执行按钮 + 结果 -->
        <el-card body-style="padding:16px" style="margin-bottom:12px">
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <span style="font-weight:600">⚡ 规则引擎执行</span>
              <el-select v-model="engineMode" size="small" style="width:120px">
                <el-option label="快速匹配" value="fast" />
                <el-option label="完整评估" value="full" />
                <el-option label="评分模式" value="score" />
              </el-select>
              <el-button type="danger" size="small" :loading="executing" style="margin-left:auto" @click="executeRule">
                <el-icon><CaretRight /></el-icon>执行规则
              </el-button>
            </div>
          </template>

          <!-- 执行结果 -->
          <div v-if="execResult">
            <!-- 最终决策 -->
            <el-result
              :icon="execResult.passed ? 'success' : 'error'"
              :title="execResult.passed ? '✅ 规则命中' : '❌ 规则未命中'"
              :sub-title="execResult.reason"
              style="padding:16px 0"
            >
              <template #extra>
                <el-tag v-if="execResult.action" type="warning">{{ execResult.action }}</el-tag>
                <el-tag v-if="execResult.score !== undefined" type="info">评分: {{ execResult.score }}</el-tag>
              </template>
            </el-result>

            <!-- 命中详情 -->
            <div v-if="execResult.details?.length" style="margin-top:12px">
              <div style="font-weight:600;font-size:13px;margin-bottom:8px">🔍 条件命中详情</div>
              <el-table :data="execResult.details" size="small" border>
                <el-table-column prop="field" label="字段" width="120" />
                <el-table-column prop="operator" label="操作符" width="100" />
                <el-table-column prop="value" label="阈值" width="100" />
                <el-table-column prop="actual" label="实际值" width="100" />
                <el-table-column label="结果" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.matched ? 'success' : 'danger'" size="small">
                      {{ row.matched ? '✓' : '✗' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 评分明细 -->
            <div v-if="execResult.scoreDetails?.length" style="margin-top:12px">
              <div style="font-weight:600;font-size:13px;margin-bottom:8px">📊 评分明细</div>
              <div v-for="sd in execResult.scoreDetails" :key="sd.name" style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
                <span style="font-size:12px;width:100px">{{ sd.name }}</span>
                <el-progress :percentage="sd.percent" :color="sd.color" style="flex:1" :stroke-width="8" />
                <span style="font-size:12px;width:60px;text-align:right">+{{ sd.score }}</span>
              </div>
              <div style="font-weight:600;margin-top:8px;text-align:right">
                总分: <span style="color:#409eff;font-size:16px">{{ execResult.score }}</span>
              </div>
            </div>

            <!-- 🧠 AI 解读决策结果 -->
            <div style="margin-top:12px">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
                <span style="font-weight:600;font-size:13px">🧠 AI 解读</span>
                <el-select v-if="selfModels.length" v-model="selectedExplainModel" size="small" style="width:160px">
                  <el-option label="🤖 Auto" value="auto" />
                  <el-option v-for="m in selfModels" :key="m.modelCode" :label="m.modelName || m.modelCode" :value="m.modelCode" />
                </el-select>
                <el-button size="small" type="primary" :loading="explaining" @click="explainResult"
                  :disabled="!execResult || explaining">
                  <el-icon><MagicStick /></el-icon>{{ explaining ? '解读中…' : '解读决策' }}
                </el-button>
              </div>
              <!-- 解读结果 -->
              <div v-if="explainText" class="explain-box">
                <div style="font-size:12px;white-space:pre-wrap;line-height:1.8">{{ explainText }}</div>
              </div>
              <div v-else-if="!explaining" style="font-size:12px;color:#c0c4cc;text-align:center;padding:8px">
                点击「解读决策」，模型将分析规则逻辑、命中原因及业务建议
              </div>
            </div>
          </div>

          <el-empty v-else description="输入规则和测试数据，点击「执行规则」查看结果" :image-size="60" />
        </el-card>

        <!-- 规则模板 -->
        <el-card body-style="padding:16px">
          <template #header>
            <span style="font-weight:600">📚 常用规则模板</span>
          </template>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
            <div v-for="tpl in templates" :key="tpl.name" class="tpl-card" @click="applyTemplate(tpl)">
              <div style="font-size:24px;text-align:center">{{ tpl.icon }}</div>
              <div style="font-size:12px;font-weight:600;text-align:center;margin-top:4px">{{ tpl.name }}</div>
              <div style="font-size:10px;color:#909399;text-align:center">{{ tpl.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录抽屉 -->
    <el-drawer v-model="historyVisible" title="📜 规则执行历史" size="50%">
      <el-table :data="historyList" size="small" v-loading="historyLoading">
        <el-table-column label="规则描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.nl || row.ruleName || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
              {{ row.passed ? '命中' : '未命中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行时间" width="160">
          <template #default="{ row }">{{ row.executedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="loadFromHistory(row)">加载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, CaretRight } from '@element-plus/icons-vue'
import { autoAgentGroupGenerate } from '@/api/ai'
import { listEnabledModels, modelApi } from '@/api/model'

// ===== 示例提示词 =====
const examples = [
  '如果年龄大于60岁且城市为北京，发送敬老优惠',
  '订单金额超过5000元且用户等级为VIP打9折',
  '用户连续登录超过7天未消费发送流失预警',
  '购买金额满300减50不与其他优惠同用',
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
    name: '折扣规则',
    type: 'IF_THEN',
    conditions: [
      { field: 'age', operator: '>', value: 60, label: '年龄大于60岁' },
      { field: 'city', operator: '==', value: '北京', label: '城市为北京' },
    ],
    logic: 'AND',
    actions: [
      { type: 'send_coupon', params: { code: 'ELDER_DISCOUNT', amount: 50 } },
    ],
    priority: 1,
  }, null, 2)
}

// ===== 状态 =====
const nlInput = ref('')
const ruleJson = ref(defaultRuleTemplate())
const testData = ref('{"age": 65, "city": "北京", "userLevel": "VIP", "orderAmount": 8000}')
const engineMode = ref('fast')
const generating = ref(false)
const executing = ref(false)
const execResult = ref(null)
const historyVisible = ref(false)
const historyList = ref([])
const historyLoading = ref(false)
const genMs = ref(0)

// 自研模型相关
const allModels = ref([])
const selfModels = computed(() =>
  allModels.value.filter(m =>
    m.enabled !== false &&
    (m.providerCode === 'self-trained' || m.providerCode === 'trained' ||
     m.providerCode === 'self' || m.providerCode === 'self-developed' ||
     m.providerCode === 'local')
  )
)
const selectedRuleModel = ref('auto')
const selectedExplainModel = ref('auto')
const suggestedModelName = ref('')
const explaining = ref(false)
const explainText = ref('')

// ===== 规则语法校验 =====
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

// ===== 加载自研模型列表 =====
async function loadModels() {
  try {
    const r = await listEnabledModels()
    const list = r?.data || r || []
    allModels.value = Array.isArray(list) ? list : []
    // 默认选中推荐模型
    if (selfModels.value.length && selectedRuleModel.value === 'auto') {
      selectedRuleModel.value = selfModels.value[0].modelCode
    }
  } catch (e) {
    console.warn('[Rule] 加载模型列表失败:', e)
    allModels.value = []
  }
}

// ===== 领域检测 → 推荐模型 =====
const DOMAIN_MAP = [
  { keywords: /金融|银行|理财|信贷|风控|信用|贷款|利率|投资|保险|基金|股票/, modelCode: 'fin-gpt', name: '金融大模型' },
  { keywords: /法律|法规|合规|诉讼|合同|律师|判例|司法|条款/, modelCode: 'law-gpt', name: '法律大模型' },
  { keywords: /医疗|诊断|药品|医生|医院|疾病|健康|处方|病历/, modelCode: 'med-gpt', name: '医疗大模型' },
  { keywords: /代码|编程|函数|算法|接口|bug|开发|debug|程序员/, modelCode: 'code-gpt', name: '代码大模型' },
  { keywords: /客服|对话|问答|聊天|解答|回复|话术/, modelCode: 'qa-gpt', name: '问答大模型' },
  { keywords: /营销|推广|广告|获客|转化|用户增长|留存|活跃/, modelCode: 'chat-gpt', name: '营销大模型' },
]

function detectDomain() {
  const nl = nlInput.value.toLowerCase()
  for (const d of DOMAIN_MAP) {
    if (d.keywords.test(nl)) {
      // 检查该模型是否在可用列表中
      const found = selfModels.value.find(m => m.modelCode === d.modelCode)
      suggestedModelName.value = found ? d.name : ''
      if (found && selfModels.value.length > 1) {
        selectedRuleModel.value = d.modelCode
      }
      return
    }
  }
  suggestedModelName.value = ''
}

function onModelChange() {
  explainText.value = '' // 换模型时清空解读
}

// ===== LLM 生成规则 =====
async function generateRule() {
  if (!nlInput.value.trim()) { ElMessage.warning('请先描述业务规则'); return }
  generating.value = true
  genMs.value = 0
  const start = Date.now()

  try {
    let ruleText = ''

    if (selectedRuleModel.value === 'auto') {
      // Auto 模式：调用 Agent API
      const r = await autoAgentGroupGenerate({
        oneLiner: `将以下业务规则转换为JSON结构化规则（IF-THEN格式，含conditions/conditionsLogic/actions字段）：${nlInput.value}`,
      })
      ruleText = r?.data?.description || r?.description || ''
    } else {
      // 自研模型直调
      const model = selectedRuleModel.value
      const prompt = `你是一个业务规则工程师。请将以下自然语言描述的业务规则，转换为严格合法的 JSON 结构化规则。

要求：
1. 返回纯 JSON，不要任何解释文字
2. JSON 必须包含: name(规则名称), type(固定"IF_THEN"), conditions(数组,每项含field/operator/value/label), logic(AND/OR), actions(数组,每项含type/params), priority
3. operator 支持: >, >=, <, <=, ==, !=, contains, in
4. action type 支持: send_coupon, discount, notify, recommend, pass, reject
5. 如果是优惠/折扣类规则，actions 用 discount 或 send_coupon
6. 如果是预警/通知类规则，actions 用 notify

自然语言规则: ${nlInput.value}

直接返回 JSON，不要前缀后缀标记`

      try {
        const r = await modelApi.chat({ model, messages: [{ role: 'user', content: prompt }] })
        ruleText = r?.data?.content || r?.content || r?.text || ''
      } catch (apiErr) {
        console.warn('[Rule] 模型直调失败，降级到 Auto 模式:', apiErr)
        const fallback = await autoAgentGroupGenerate({
          oneLiner: `将以下业务规则转换为JSON：${nlInput.value}`,
        })
        ruleText = fallback?.data?.description || fallback?.description || ''
      }
    }

    genMs.value = Date.now() - start

    // 从返回文本中提取 JSON
    const jsonMatch = ruleText.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      try {
        const parsed = JSON.parse(jsonMatch[0])
        ruleJson.value = JSON.stringify(parsed, null, 2)
        const modelName = selectedRuleModel.value === 'auto' ? 'Auto' : selectedRuleModel.value
        ElMessage.success(`🧠 ${modelName} 规则生成完成 (${genMs.value}ms)`)
      } catch {
        ruleJson.value = buildSimpleRule(nlInput.value)
        ElMessage.warning('JSON 解析失败，使用简化解析')
      }
    } else if (ruleText.trim()) {
      ruleJson.value = buildSimpleRule(nlInput.value)
      ElMessage.warning('未提取到 JSON，使用简化解析')
    } else {
      ruleJson.value = buildSimpleRule(nlInput.value)
      ElMessage.error('生成失败，使用简化解析')
    }
  } catch (e) {
    genMs.value = Date.now() - start
    console.error('[Rule] generateRule error:', e)
    ruleJson.value = buildSimpleRule(nlInput.value)
    ElMessage.error('生成失败: ' + (e.message || '') + '，已使用简化解析')
  } finally {
    generating.value = false
  }
}

/** 简单 NL → Rule 解析（不依赖 LLM API） */
function buildSimpleRule(nl) {
  const d = nl.toLowerCase()
  const conditions = []
  const actions = []

  // 提取数字条件
  const numPatterns = [
    [/(?:年龄|age)[^\d]*?>?\s*(\d+)/, 'age', 'age > '],
    [/(?:订单金额|消费|金额|amount)[^\d]*?>?\s*(\d+)/, 'orderAmount', 'orderAmount > '],
    [/(?:天数|连续|days?)[^\d]*?>?\s*(\d+)/, 'days', 'days > '],
    [/(?:满|threshold)[^\d]*?(\d+)/, 'orderAmount', 'orderAmount >= '],
    [/(?:减|discount)[^\d]*?(\d+)/, 'discountAmount', 'discount = '],
  ]
  for (const [re, field, prefix] of numPatterns) {
    const m = nl.match(re)
    if (m) {
      conditions.push({
        field,
        operator: d.includes('满') ? '>=' : '>',
        value: parseInt(m[1]),
        label: prefix + m[1],
      })
    }
  }

  // 提取字符串条件
  if (/北京|上海|深圳/.test(nl)) conditions.push({ field: 'city', operator: '==', value: '北京', label: '城市=北京' })
  if (/VIP|高级|钻石/.test(nl)) conditions.push({ field: 'userLevel', operator: '==', value: 'VIP', label: '用户等级=VIP' })
  if (/老年|敬老|60/.test(nl)) conditions.push({ field: 'age', operator: '>', value: 60, label: '年龄>60岁' })

  // 提取操作
  if (/优惠|折扣|打折/.test(nl)) {
    const disc = nl.match(/(\d+)[%折]/) || nl.match(/减(\d+)/)
    actions.push({
      type: 'discount',
      params: { type: disc?.[1]?.includes('%') ? 'percent' : 'amount', value: disc ? parseInt(disc[1]) : 10 }
    })
  }
  if (/预警|通知|提醒/.test(nl)) actions.push({ type: 'notify', params: { channel: 'sms', template: '流失预警' } })
  if (/推荐/.test(nl)) actions.push({ type: 'recommend', params: {} })

  if (!actions.length) actions.push({ type: 'pass', params: {} })

  const rule = {
    name: 'NL生成规则-' + Date.now(),
    type: 'IF_THEN',
    conditions,
    logic: /且|并且|and/.test(nl) ? 'AND' : 'OR',
    actions,
    priority: 1,
  }
  return JSON.stringify(rule, null, 2)
}

// ===== 规则引擎执行 =====
async function executeRule() {
  if (!ruleValid.value) { ElMessage.error('规则格式错误，请检查 JSON'); return }
  let data
  try {
    data = JSON.parse(testData.value)
  } catch { ElMessage.error('测试数据 JSON 格式错误'); return }

  executing.value = true
  execResult.value = null

  try {
    // 模拟规则引擎执行（真实场景对接到后端 RuleEngine）
    const rule = JSON.parse(ruleJson.value)
    const result = runRuleEngine(rule, data)
    execResult.value = result

    // 保存历史
    historyList.value.unshift({
      nl: nlInput.value,
      rule: ruleJson.value,
      testData: testData.value,
      passed: result.passed,
      reason: result.reason,
      action: result.action,
      score: result.score,
      executedAt: new Date().toLocaleString(),
    })
    if (historyList.value.length > 50) historyList.value.pop()
    ElMessage.success('执行完成')
  } catch (e) {
    ElMessage.error('执行失败: ' + e.message)
  } finally {
    executing.value = false
  }
}

/**
 * 前端规则引擎（完整评估模式）
 * 真实场景: 后端 Drools / ODM / 自研规则引擎
 */
function runRuleEngine(rule, data) {
  const conditions = rule.conditions || []
  const details = []
  let allMatched = true

  for (const cond of conditions) {
    const actual = data[cond.field]
    const threshold = cond.value
    let matched = false

    switch (cond.operator) {
      case '>':  matched = actual > threshold; break
      case '>=': matched = actual >= threshold; break
      case '<':  matched = actual < threshold; break
      case '<=': matched = actual <= threshold; break
      case '==': matched = String(actual) === String(threshold); break
      case '!=': matched = String(actual) !== String(threshold); break
      case 'contains': matched = String(actual).includes(String(threshold)); break
      case 'in': matched = Array.isArray(threshold) && threshold.includes(actual); break
      default: matched = false
    }

    details.push({
      field: cond.field,
      operator: cond.operator,
      value: threshold,
      actual: actual ?? '(undefined)',
      matched,
    })
    if (!matched) allMatched = false
  }

  // 逻辑运算
  if (rule.logic === 'OR') {
    allMatched = details.some(d => d.matched)
  } else if (rule.logic === 'AND') {
    allMatched = details.every(d => d.matched)
  }

  // 评分模式
  let score = 0
  const scoreDetails = []
  if (engineMode.value === 'score') {
    for (const cond of conditions) {
      const actual = data[cond.field]
      const base = 25 // 每条件最高25分
      let condScore = 0
      let percent = 0
      let color = '#67c23a'
      if (actual !== undefined && actual !== null) {
        if (typeof actual === 'number' && typeof cond.value === 'number') {
          const ratio = Math.min(actual / cond.value, 2)
          condScore = Math.round(base * Math.min(ratio, 1))
          percent = Math.round(Math.min(ratio, 1) * 100)
          color = percent >= 100 ? '#67c23a' : percent >= 70 ? '#e6a23c' : '#f56c6c'
        } else if (String(actual) === String(cond.value)) {
          condScore = base
          percent = 100
        }
      }
      score += condScore
      scoreDetails.push({ name: cond.field, score: condScore, percent, color })
    }
    score = Math.min(score, 100)
    allMatched = score >= 60
  }

  // 执行操作
  let actionLabel = ''
  let actionResult = ''
  if (allMatched && rule.actions?.length) {
    for (const act of rule.actions) {
      switch (act.type) {
        case 'send_coupon':
          actionLabel = `🎫 发放优惠券: ${act.params?.code} (面值 ${act.params?.amount}元)`
          break
        case 'discount':
          if (act.params?.type === 'percent') actionResult = `🎁 折扣: ${act.params?.value}%`
          else actionResult = `🎁 优惠: 减${act.params?.value}元`
          actionLabel = actionResult
          break
        case 'notify':
          actionLabel = `📱 发送通知: ${act.params?.channel} - ${act.params?.template}`
          break
        case 'recommend':
          actionLabel = '🛍️ 生成个性化推荐'
          break
        case 'reject':
          actionLabel = '🚫 拒绝申请'
          break
        case 'pass':
          actionLabel = '✅ 审核通过'
          break
        default:
          actionLabel = `⚙️ 执行: ${act.type}`
      }
    }
  }

  return {
    passed: allMatched,
    reason: allMatched
      ? `命中所有条件 (${rule.logic})`
      : `未满足条件，${details.filter(d => !d.matched).map(d => d.field).join(', ')} 不匹配`,
    action: actionLabel,
    details,
    score: engineMode.value === 'score' ? score : undefined,
    scoreDetails: engineMode.value === 'score' ? scoreDetails : undefined,
  }
}

// ===== 工具函数 =====
function formatJson() {
  try {
    const r = JSON.parse(ruleJson.value)
    ruleJson.value = JSON.stringify(r, null, 2)
    ElMessage.success('已格式化')
  } catch { ElMessage.error('不是有效 JSON') }
}

function applyTemplate(tpl) {
  const templates = {
    discount: {
      name: '年龄折扣规则', type: 'IF_THEN', priority: 1,
      conditions: [
        { field: 'age', operator: '>=', value: 60, label: '年龄>=60岁' },
        { field: 'age', operator: '<', value: 80, label: '年龄<80岁' },
      ],
      logic: 'AND',
      actions: [{ type: 'send_coupon', params: { code: 'ELDER_DISCOUNT', amount: 30 } }],
    },
    vip: {
      name: 'VIP折扣规则', type: 'IF_THEN', priority: 2,
      conditions: [
        { field: 'userLevel', operator: '==', value: 'VIP', label: '等级=VIP' },
        { field: 'orderAmount', operator: '>', value: 1000, label: '订单>1000元' },
      ],
      logic: 'AND',
      actions: [{ type: 'discount', params: { type: 'percent', value: 15 } }],
    },
    threshold: {
      name: '满减活动规则', type: 'IF_THEN', priority: 3,
      conditions: [
        { field: 'orderAmount', operator: '>=', value: 300, label: '满300元' },
      ],
      logic: 'AND',
      actions: [{ type: 'discount', params: { type: 'amount', value: 50 } }],
    },
    churn: {
      name: '流失预警规则', type: 'IF_THEN', priority: 4,
      conditions: [
        { field: 'lastLoginDays', operator: '>', value: 7, label: '7天未登录' },
        { field: 'hasPurchase', operator: '==', value: false, label: '未消费' },
      ],
      logic: 'AND',
      actions: [{ type: 'notify', params: { channel: 'sms', template: '流失预警' } }],
    },
    risk: {
      name: '信贷风险评分', type: 'SCORE', priority: 5,
      conditions: [
        { field: 'creditScore', operator: '>=', value: 700, label: '信用分>=700' },
        { field: 'incomeMonthly', operator: '>=', value: 5000, label: '月收入>=5000' },
        { field: 'debtRatio', operator: '<', value: 0.5, label: '负债率<50%' },
        { field: 'employmentYears', operator: '>=', value: 1, label: '工作>=1年' },
      ],
      logic: 'AND',
      actions: [{ type: 'pass', params: {} }],
    },
    recommend: {
      name: '个性化推荐规则', type: 'IF_THEN', priority: 6,
      conditions: [
        { field: 'browseCount', operator: '>', value: 10, label: '浏览>10次' },
        { field: 'favoriteCategory', operator: '!=', value: '', label: '有偏好分类' },
      ],
      logic: 'AND',
      actions: [{ type: 'recommend', params: {} }],
    },
  }
  const t = templates[tpl.type]
  if (t) {
    ruleJson.value = JSON.stringify(t, null, 2)
    nlInput.value = tpl.desc
    ElMessage.success(`已加载「${tpl.name}」模板`)
  }
}

function generateTestData() {
  const age = Math.floor(Math.random() * 50) + 18
  const amount = Math.floor(Math.random() * 10000) + 500
  const levels = ['NORMAL', 'SILVER', 'GOLD', 'VIP', 'PLATINUM']
  const cities = ['北京', '上海', '深圳', '广州', '杭州']
  const data = {
    age,
    city: cities[Math.floor(Math.random() * cities.length)],
    userLevel: levels[Math.floor(Math.random() * levels.length)],
    orderAmount: amount,
    lastLoginDays: Math.floor(Math.random() * 30),
    hasPurchase: Math.random() > 0.3,
    creditScore: Math.floor(Math.random() * 300) + 500,
    incomeMonthly: Math.floor(Math.random() * 20000) + 3000,
    debtRatio: Math.random(),
    employmentYears: Math.floor(Math.random() * 10),
    browseCount: Math.floor(Math.random() * 30),
    favoriteCategory: ['电子产品', '服装', '食品', ''][Math.floor(Math.random() * 4)],
  }
  testData.value = JSON.stringify(data, null, 2)
}

async function loadHistory() {
  historyVisible.value = true
  historyLoading.value = true
  await new Promise(r => setTimeout(r, 300))
  historyLoading.value = false
}

function loadFromHistory(row) {
  nlInput.value = row.nl || ''
  ruleJson.value = row.rule || defaultRuleTemplate()
  testData.value = row.testData || '{}'
  historyVisible.value = false
}

// ===== AI 解读决策结果 =====
async function explainResult() {
  if (!execResult.value) return
  explaining.value = true
  explainText.value = ''
  try {
    let model = selectedExplainModel.value
    if (model === 'auto') model = selectedRuleModel.value !== 'auto' ? selectedRuleModel.value : (selfModels.value[0]?.modelCode || '')

    const ruleStr = ruleJson.value
    const testStr = testData.value
    const resultStr = JSON.stringify(execResult.value, null, 2)

    const prompt = `你是一个专业的业务规则分析专家。请解读以下规则引擎的执行结果，并给出业务建议。

【规则定义】
${ruleStr}

【输入数据】
${testStr}

【执行结果】
${resultStr}

请用中文分析：
1. 为什么该规则被命中（或未命中）？
2. 每个条件的实际值是否合理？
3. 对业务运营有什么建议？

请简洁、专业，直接回答，不要复述规则。`

    const r = await modelApi.chat({
      model,
      messages: [{ role: 'user', content: prompt }],
    })
    const text = r?.data?.content || r?.content || r?.text || ''
    explainText.value = text.trim() || '模型未返回有效解读'
  } catch (e) {
    explainText.value = `解读失败: ${e.message || '未知错误'}`
    console.error('[Rule] explainResult error:', e)
  } finally {
    explaining.value = false
  }
}

// ===== 启动 =====
onMounted(() => {
  loadModels()
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px }
.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 16px; h2 { margin: 0; font-size: 16px; }
}
.model-status-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; background: #f5f7fa; border-radius: 8px;
  margin-bottom: 12px; border: 1px solid #e8ecf0;
}
.explain-box {
  background: linear-gradient(135deg, #f0f9eb, #ecf5ff);
  border: 1px solid #d4edda; border-radius: 8px;
  padding: 12px 14px; min-height: 60px;
}
.tpl-card {
  padding: 10px; border: 1px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all 0.15s;
  &:hover { border-color: #409eff; background: #ecf5ff; }
}

// ============================================================
// H5 移动端适配 (max-width: 768px)
// ============================================================
@media (max-width: 768px) {
  .page-card { padding: 12px; }
  .page-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start !important;
    h2 { font-size: 14px; }
  }

  // 模型状态栏: 换行
  .model-status-bar {
    flex-wrap: wrap;
    padding: 8px 12px;
    :deep(.el-select) { width: 100% !important; }
    :deep(.el-tag) { font-size: 11px; }
  }

  // 模板网格: 2列
  .tpl-grid {
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 8px;
  }

  // 所有 el-card body
  .el-card :deep(.el-card__body) { padding: 12px; }

  // textarea 全宽
  .el-card :deep(.el-textarea) {
    width: 100% !important;
    font-size: 12px !important;
  }

  // 按钮组换行
  .el-card :deep(.el-button) {
    font-size: 13px;
    padding: 8px 12px;
  }

  // 执行结果表格: 横向滚动
  .el-card :deep(.el-table) {
    font-size: 12px;
    overflow-x: auto;
  }

  // AI 解读区: 全宽
  .explain-box { padding: 10px; font-size: 13px; }
}

@media (max-width: 400px) {
  .tpl-grid { grid-template-columns: 1fr !important; }
  .page-header { font-size: 12px; }
}
</style>
