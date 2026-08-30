<!--
  @file rule/Index.vue - NL 规则助手 V7.5

  V7.4 → V7.5 改造 (告别 regex 土味解析):
  1. 真实 LLM 驱动 (Qwen2.5 + BGE fallback)
  2. 模板市场 (6 个业务模板, 一键应用)
  3. 可视化规则构造 (chip-based conditions/actions, 告别手写 JSON)
  4. 测试数据预设 (3 套用户/订单/会员样例)
  5. NL → 规则 → 测试一条龙
-->
<template>
  <div class="rule-page">
    <header class="page-header">
      <div class="title-row">
        <h2>⚙️ NL 规则助手</h2>
        <div class="header-actions">
          <el-tag :type="llmReady ? 'success' : 'info'" size="small">
            {{ llmReady ? 'Qwen2.5 已就绪' : '本地简化解析' }}
          </el-tag>
          <el-button size="small" @click="resetAll">重置</el-button>
        </div>
      </div>
      <p class="subtitle">自然语言 → 业务规则 · 可视化构造 · 即时测试</p>
    </header>

    <el-row :gutter="20">
      <!-- 左侧: NL 输入 + 模板 -->
      <el-col :span="14">
        <el-card shadow="never" class="left-card">
          <!-- 1. 模板市场 -->
          <div class="section">
            <h3>📋 模板市场 <span class="hint">点击直接应用</span></h3>
            <div class="template-grid">
              <div
                v-for="t in templates"
                :key="t.id"
                class="template-card"
                :class="{ active: t.id === appliedTemplate }"
                @click="applyTemplate(t)"
              >
                <div class="t-icon">{{ t.icon }}</div>
                <div class="t-name">{{ t.name }}</div>
                <div class="t-desc">{{ t.desc }}</div>
              </div>
            </div>
          </div>

          <!-- 2. NL 输入 -->
          <div class="section">
            <h3>✍️ 业务规则描述 (自然语言)</h3>
            <el-input
              v-model="nlInput"
              type="textarea"
              :rows="4"
              placeholder="例: VIP 用户, 单笔订单金额满 1000 元, 减 100 元; 否则推荐相关商品"
            />
            <div class="nl-actions">
              <el-button
                type="primary"
                :loading="generating"
                :icon="Promotion"
                @click="generateRule"
              >
                {{ llmReady ? '智能生成 (Qwen2.5)' : '解析为规则' }}
              </el-button>
              <el-button :icon="Refresh" @click="nlInput = ''">清空</el-button>
              <span class="char-count">{{ nlInput.length }} 字</span>
            </div>
          </div>

          <!-- 3. 可视化规则 -->
          <div class="section" v-if="rule.name || parsedRule.conditions?.length">
            <h3>
              🧱 规则结构
              <el-button size="small" link type="primary" @click="showJson = !showJson">
                {{ showJson ? '隐藏' : '查看' }} JSON
              </el-button>
            </h3>

            <el-form label-position="top" :model="rule">
              <el-form-item label="规则名称">
                <el-input v-model="rule.name" placeholder="例如: VIP大额优惠" />
              </el-form-item>

              <el-form-item label="条件 (Conditions)">
                <div class="chip-list">
                  <div v-for="(c, i) in rule.conditions" :key="i" class="chip cond-chip">
                    <span class="chip-label">{{ c.label || c.field + ' ' + c.operator + ' ' + c.value }}</span>
                    <el-button size="small" link :icon="Close" @click="removeCondition(i)" />
                  </div>
                  <el-button size="small" :icon="Plus" @click="addCondition">添加条件</el-button>
                </div>
              </el-form-item>

              <el-form-item label="组合逻辑" v-if="rule.conditions.length > 1">
                <el-radio-group v-model="rule.logic">
                  <el-radio-button value="AND">全部满足 (AND)</el-radio-button>
                  <el-radio-button value="OR">任一满足 (OR)</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="动作 (Actions)">
                <div class="chip-list">
                  <div v-for="(a, i) in rule.actions" :key="i" class="chip action-chip">
                    <span class="chip-label">{{ formatAction(a) }}</span>
                    <el-button size="small" link :icon="Close" @click="removeAction(i)" />
                  </div>
                  <el-button size="small" :icon="Plus" @click="addAction">添加动作</el-button>
                </div>
              </el-form-item>

              <el-form-item label="优先级">
                <el-input-number v-model="rule.priority" :min="1" :max="100" />
              </el-form-item>
            </el-form>

            <!-- JSON 预览 -->
            <el-collapse v-model="showJson" v-show="showJson">
              <el-collapse-item title="JSON 预览" name="json">
                <pre class="json-preview">{{ JSON.stringify(rule, null, 2) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧: 测试 + 历史 -->
      <el-col :span="10">
        <el-card shadow="never" class="right-card">
          <!-- 4. 测试 -->
          <div class="section">
            <h3>🧪 即时测试</h3>
            <el-radio-group v-model="testPreset" size="small" @change="applyTestPreset">
              <el-radio-button value="user">用户</el-radio-button>
              <el-radio-button value="order">订单</el-radio-button>
              <el-radio-button value="vip">会员</el-radio-button>
              <el-radio-button label="自定义" value="custom" />
            </el-radio-group>
            <el-input
              v-model="testDataJson"
              type="textarea"
              :rows="6"
              class="test-data"
              placeholder='{"age": 65, "city": "北京", "userLevel": "VIP", "orderAmount": 1200}'
            />
            <el-button
              type="success"
              :loading="executing"
              :icon="VideoPlay"
              @click="runTest"
              :disabled="!rule.conditions.length"
            >
              执行规则
            </el-button>
            <div v-if="execResult" class="exec-result" :class="execResult.passed ? 'pass' : 'fail'">
              <div class="result-header">
                <el-icon :size="20">
                  <component :is="execResult.passed ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
                </el-icon>
                <strong>{{ execResult.passed ? '规则命中' : '未命中' }}</strong>
                <el-tag size="small" :type="execResult.passed ? 'success' : 'danger'">
                  分数 {{ execResult.score }}
                </el-tag>
              </div>
              <div class="result-reason">{{ execResult.reason }}</div>
              <div v-if="execResult.action" class="result-action">
                <strong>触发动作:</strong> {{ formatAction(execResult.action) }}
              </div>
            </div>
          </div>

          <!-- 5. 历史 -->
          <div class="section" v-if="historyList.length">
            <h3>📜 最近执行 ({{ historyList.length }})</h3>
            <el-scrollbar height="240px">
              <div
                v-for="(h, i) in historyList"
                :key="i"
                class="history-item"
                @click="restoreHistory(h)"
              >
                <div class="h-line1">
                  <span class="h-name">{{ h.ruleName }}</span>
                  <el-tag size="small" :type="h.passed ? 'success' : 'info'">
                    {{ h.passed ? '✓' : '✗' }}
                  </el-tag>
                </div>
                <div class="h-nl">{{ h.nl }}</div>
                <div class="h-time">{{ h.executedAt }}</div>
              </div>
            </el-scrollbar>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部: 规则列表 -->
    <el-card shadow="never" class="bottom-card" style="margin-top: 20px">
      <div class="section">
        <h3>
          💾 已保存规则 ({{ savedRules.length }})
          <el-button size="small" :icon="Refresh" @click="loadSavedList">刷新</el-button>
          <el-button
            size="small"
            type="primary"
            :icon="Check"
            :disabled="!rule.name || !rule.conditions.length"
            :loading="saving"
            @click="saveCurrent"
          >
            保存当前
          </el-button>
        </h3>
        <el-empty v-if="!savedRules.length" description="暂无规则, 试试上方模板 + 保存" />
        <el-table v-else :data="savedRules" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type || 'IF_THEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="条件" width="80">
            <template #default="{ row }">
              {{ countConditions(row) }}
            </template>
          </el-table-column>
          <el-table-column label="动作" width="80">
            <template #default="{ row }">
              {{ countActions(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="loadRule(row)">加载</el-button>
              <el-button size="small" link type="danger" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 添加条件/动作对话框 -->
    <el-dialog v-model="condDialog.visible" title="添加条件" width="500px">
      <el-form :model="condDialog.form" label-width="80px">
        <el-form-item label="字段">
          <el-select v-model="condDialog.form.field" filterable allow-create>
            <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作符">
          <el-select v-model="condDialog.form.operator">
            <el-option label="等于 ==" value="==" />
            <el-option label="不等于 !=" value="!=" />
            <el-option label="大于 >" value=">" />
            <el-option label="大于等于 >=" value=">=" />
            <el-option label="小于 <" value="<" />
            <el-option label="小于等于 <=" value="<=" />
            <el-option label="包含 contains" value="contains" />
          </el-select>
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="condDialog.form.value" placeholder="数字 / 字符串 / 数组 JSON" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="condDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmCondition">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actDialog.visible" title="添加动作" width="500px">
      <el-form :model="actDialog.form" label-width="100px">
        <el-form-item label="动作类型">
          <el-select v-model="actDialog.form.type" @change="resetActionParams">
            <el-option v-for="a in actionTypes" :key="a.type" :label="a.label" :value="a.type" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-for="p in currentActionParams"
          :key="p.key"
          :label="p.label"
        >
          <el-input
            v-if="p.type === 'string'"
            v-model="actDialog.form.params[p.key]"
            :placeholder="p.placeholder || ''"
          />
          <el-input-number
            v-else-if="p.type === 'number'"
            v-model="actDialog.form.params[p.key]"
          />
          <el-select
            v-else-if="p.type === 'select'"
            v-model="actDialog.form.params[p.key]"
          >
            <el-option
              v-for="opt in p.options"
              :key="opt"
              :label="opt"
              :value="opt"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAction">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Promotion, Refresh, Close, Plus, VideoPlay, Check,
  CircleCheckFilled, CircleCloseFilled
} from '@element-plus/icons-vue'
import { ruleApi } from '@/api/rule'
import { multimodalApi } from '@/api/multimodal'

// ===== State =====
const nlInput = ref('')
const generating = ref(false)
const llmReady = ref(false)
const appliedTemplate = ref(null)
const showJson = ref(false)

const rule = reactive({
  name: '',
  type: 'IF_THEN',
  conditions: [],
  logic: 'AND',
  actions: [],
  priority: 1
})

const testPreset = ref('user')
const testDataJson = ref('')
const executing = ref(false)
const execResult = ref(null)

const historyList = ref([])
const savedRules = ref([])
const saving = ref(false)

const condDialog = reactive({ visible: false, form: { field: '', operator: '==', value: '' } })
const actDialog = reactive({ visible: false, form: { type: 'discount', params: {} } })

// ===== 模板 =====
const templates = [
  {
    id: 'vip-discount',
    icon: '💎', name: 'VIP 大额优惠',
    desc: 'VIP 用户满 1000 减 100',
    nl: 'VIP 用户, 单笔订单金额满 1000 元, 减 100 元',
    rule: {
      name: 'VIP 大额优惠',
      conditions: [
        { field: 'userLevel', operator: '==', value: 'VIP', label: '用户等级=VIP' },
        { field: 'orderAmount', operator: '>=', value: 1000, label: '订单金额>=1000' }
      ],
      logic: 'AND',
      actions: [{ type: 'discount', params: { type: 'amount', value: 100 } }],
      priority: 5
    }
  },
  {
    id: 'elder-care',
    icon: '👴', name: '老年关爱',
    desc: '60 岁以上北京用户短信关怀',
    nl: '60 岁以上北京用户, 发送短信关怀',
    rule: {
      name: '老年关爱',
      conditions: [
        { field: 'age', operator: '>=', value: 60, label: '年龄>=60' },
        { field: 'city', operator: '==', value: '北京', label: '城市=北京' }
      ],
      logic: 'AND',
      actions: [{ type: 'notify', params: { channel: 'sms', template: 'elderly_care' } }],
      priority: 3
    }
  },
  {
    id: 'churn-alert',
    icon: '⚠️', name: '流失预警',
    desc: '30 天未活跃用户告警',
    nl: '用户连续 30 天未登录, 触发流失预警, 邮件通知运营',
    rule: {
      name: '流失预警',
      conditions: [
        { field: 'daysSinceLogin', operator: '>=', value: 30, label: '未登录天数>=30' }
      ],
      logic: 'AND',
      actions: [{ type: 'notify', params: { channel: 'email', template: 'churn_alert' } }],
      priority: 8
    }
  },
  {
    id: 'newbie-coupon',
    icon: '🎁', name: '新人首单券',
    desc: '注册 7 天内首单 9 折',
    nl: '注册 7 天内的用户, 首单 9 折优惠',
    rule: {
      name: '新人首单',
      conditions: [
        { field: 'daysSinceSignup', operator: '<=', value: 7, label: '注册天数<=7' },
        { field: 'isFirstOrder', operator: '==', value: true, label: '是否首单=true' }
      ],
      logic: 'AND',
      actions: [{ type: 'discount', params: { type: 'percent', value: 10 } }],
      priority: 4
    }
  },
  {
    id: 'high-value',
    icon: '⭐', name: '高价值推荐',
    desc: '消费满 5 万专属客服',
    nl: '累计消费超过 50000 的高价值用户, 推荐专属客服',
    rule: {
      name: '高价值用户',
      conditions: [
        { field: 'totalSpent', operator: '>=', value: 50000, label: '累计消费>=50000' }
      ],
      logic: 'AND',
      actions: [{ type: 'recommend', params: { target: 'vip_service' } }],
      priority: 6
    }
  },
  {
    id: 'fraud',
    icon: '🚨', name: '反欺诈',
    desc: '异地大额订单告警',
    nl: '用户异地登录且订单金额超 10000, 立即冻结账户',
    rule: {
      name: '反欺诈',
      conditions: [
        { field: 'isRemoteLogin', operator: '==', value: true, label: '异地登录=true' },
        { field: 'orderAmount', operator: '>', value: 10000, label: '订单金额>10000' }
      ],
      logic: 'AND',
      actions: [{ type: 'freeze', params: { reason: 'suspected_fraud' } }],
      priority: 10
    }
  }
]

// ===== 字段 & 动作 =====
const fieldOptions = computed(() => {
  const set = new Set([
    'age', 'city', 'userLevel', 'orderAmount', 'orderCount',
    'totalSpent', 'daysSinceLogin', 'daysSinceSignup', 'isFirstOrder',
    'isRemoteLogin', 'gender', 'isVip', 'category', 'productId'
  ])
  rule.conditions.forEach(c => c.field && set.add(c.field))
  return [...set]
})

const actionTypes = [
  {
    type: 'discount', label: '折扣',
    params: [
      { key: 'type', label: '类型', type: 'select', options: ['percent', 'amount'] },
      { key: 'value', label: '数值', type: 'number' }
    ]
  },
  {
    type: 'send_coupon', label: '发券',
    params: [
      { key: 'code', label: '券码', type: 'string' },
      { key: 'amount', label: '金额', type: 'number' }
    ]
  },
  {
    type: 'notify', label: '通知',
    params: [
      { key: 'channel', label: '渠道', type: 'select', options: ['sms', 'email', 'push', 'webhook'] },
      { key: 'template', label: '模板', type: 'string' }
    ]
  },
  {
    type: 'recommend', label: '推荐',
    params: [
      { key: 'target', label: '目标', type: 'string' }
    ]
  },
  {
    type: 'freeze', label: '冻结',
    params: [
      { key: 'reason', label: '原因', type: 'string' }
    ]
  },
  {
    type: 'pass', label: '放行',
    params: []
  }
]

const currentActionParams = computed(() => {
  return actionTypes.find(a => a.type === actDialog.form.type)?.params || []
})

// ===== 测试数据预设 =====
const testPresets = {
  user: '{"age": 25, "city": "上海", "userLevel": "NORMAL", "orderAmount": 200, "isFirstOrder": false, "daysSinceSignup": 100, "daysSinceLogin": 5}',
  order: '{"orderAmount": 1500, "orderCount": 3, "category": "电子产品", "isFirstOrder": false}',
  vip: '{"age": 35, "city": "北京", "userLevel": "VIP", "orderAmount": 1200, "totalSpent": 8000, "isFirstOrder": false, "daysSinceLogin": 1}'
}

function applyTestPreset(key) {
  if (key === 'custom') return
  testDataJson.value = testPresets[key] || ''
}

// ===== 模板应用 =====
function applyTemplate(t) {
  appliedTemplate.value = t.id
  nlInput.value = t.nl
  Object.assign(rule, JSON.parse(JSON.stringify(t.rule)))
  ElMessage.success(`已应用模板: ${t.name}`)
}

// ===== 条件/动作 =====
function addCondition() {
  condDialog.form = { field: '', operator: '==', value: '' }
  condDialog.visible = true
}
function confirmCondition() {
  const f = condDialog.form
  if (!f.field) return ElMessage.warning('请输入字段')
  const label = `${f.field} ${f.operator} ${f.value}`
  rule.conditions.push({ field: f.field, operator: f.operator, value: parseValue(f.value), label })
  condDialog.visible = false
}
function removeCondition(i) { rule.conditions.splice(i, 1) }

function addAction() {
  actDialog.form = { type: 'discount', params: { type: 'amount', value: 10 } }
  actDialog.visible = true
}
function resetActionParams() {
  const t = actionTypes.find(a => a.type === actDialog.form.type)
  const obj = {}
  t?.params?.forEach(p => { obj[p.key] = p.type === 'number' ? 0 : '' })
  actDialog.form.params = obj
}
function confirmAction() {
  rule.actions.push({ type: actDialog.form.type, params: { ...actDialog.form.params } })
  actDialog.visible = false
}
function removeAction(i) { rule.actions.splice(i, 1) }

function formatAction(a) {
  const params = Object.entries(a.params || {})
    .map(([k, v]) => `${k}=${v}`)
    .join(', ')
  return `${a.type}(${params})`
}

function parseValue(v) {
  if (typeof v !== 'string') return v
  if (v === 'true') return true
  if (v === 'false') return false
  if (/^-?\d+(\.\d+)?$/.test(v)) return Number(v)
  return v
}

// ===== LLM 生成 (V9.1: 走 LlmClient, 自动兜底) =====
async function generateRule() {
  if (!nlInput.value.trim()) return ElMessage.warning('请输入业务规则描述')
  generating.value = true
  try {
    // V9.1: 调后端 AI 端点 (LlmClient 走 LLM Gateway, cloud→local 兜底)
    const res = await ruleApi.aiGenerate(nlInput.value)
    if (res.code === 0 && res.data && res.data.jsonContent) {
      Object.assign(rule, normalizeRule(res.data.jsonContent))
      const sourceLabel = res.data.llmSource === 'CLOUD' ? '☁️ 云端'
        : res.data.llmSource === 'LOCAL' ? '💻 本地'
        : res.data.llmSource === 'LOCAL_FALLBACK' ? '🔄 本地兜底'
        : '❌ ' + res.data.llmSource
      ElMessage.success(`AI 生成成功 (${sourceLabel} · ${res.data.llmModel} · ${res.data.durationMs}ms)`)
    } else {
      throw new Error(res.data?.reason || 'AI 返回空')
    }
  } catch (e) {
    // 降级到本地简化解析
    const fallback = buildSimpleRule(nlInput.value)
    if (fallback) {
      Object.assign(rule, fallback)
      ElMessage.warning(`LLM 失败, 已用简化解析: ${e.message}`)
    } else {
      ElMessage.error('解析失败: ' + e.message)
    }
  } finally {
    generating.value = false
  }
}

function buildQwenPrompt(nl) {
  return `请将以下自然语言业务规则转换为严格 JSON 格式 (只输出 JSON, 不要解释):

格式:
{
  "name": "规则名",
  "type": "IF_THEN",
  "conditions": [{"field": "...", "operator": "==|!=|>|>=|<|<=|contains", "value": ..., "label": "..."}],
  "logic": "AND|OR",
  "actions": [{"type": "discount|send_coupon|notify|recommend|freeze|pass", "params": {...}}],
  "priority": 1-10
}

业务规则: ${nl}`
}

function extractJson(text) {
  const m = text.match(/\{[\s\S]*\}/)
  if (!m) return null
  try { return JSON.parse(m[0]) } catch { return null }
}

function normalizeRule(r) {
  return {
    name: r.name || '未命名规则',
    type: r.type || 'IF_THEN',
    conditions: (r.conditions || []).map(c => ({
      field: c.field, operator: c.operator || '==',
      value: c.value, label: c.label || `${c.field} ${c.operator} ${c.value}`
    })),
    logic: r.logic || 'AND',
    actions: r.actions || [],
    priority: r.priority || 5
  }
}

/**
 * 简化本地解析 (fallback)
 * 提取: 数字+单位, 城市, 等级, 动作关键词
 */
function buildSimpleRule(nl) {
  const conditions = [], actions = []
  const numMatch = (re, field, op = '>') => {
    const m = nl.match(re)
    if (m) conditions.push({ field, operator: nl.includes('满') ? '>=' : op, value: parseInt(m[1]), label: `${field} ${nl.includes('满') ? '>=' : op} ${m[1]}` })
  }
  numMatch(/(?:年龄|age)[^\d]*?(\d+)/, 'age', '>')
  numMatch(/(?:订单|金额|消费|amount)[^\d]*?(\d+)/, 'orderAmount', '>')
  numMatch(/(?:累计|总额|total)[^\d]*?(\d+)/, 'totalSpent', '>')
  numMatch(/(?:天数|连续|days?)[^\d]*?(\d+)/, 'daysSinceLogin', '>')
  if (/北京|上海|深圳|广州/.test(nl)) {
    const city = nl.match(/北京|上海|深圳|广州/)[0]
    conditions.push({ field: 'city', operator: '==', value: city, label: `城市=${city}` })
  }
  if (/VIP|高级|钻石/.test(nl)) conditions.push({ field: 'userLevel', operator: '==', value: 'VIP', label: '等级=VIP' })
  if (/老年|敬老|60\s*岁/.test(nl)) conditions.push({ field: 'age', operator: '>=', value: 60, label: '年龄>=60' })
  if (/首单|新用户/.test(nl)) conditions.push({ field: 'isFirstOrder', operator: '==', value: true, label: '首单=true' })
  if (/异地/.test(nl)) conditions.push({ field: 'isRemoteLogin', operator: '==', value: true, label: '异地=true' })

  if (/折扣|减|折|off/i.test(nl)) {
    const disc = nl.match(/(\d+)\s*[%折]/) || nl.match(/减\s*(\d+)/)
    if (disc) {
      const isPercent = /%/.test(nl) || /折/.test(nl)
      actions.push({ type: isPercent ? 'discount' : 'discount',
        params: { type: isPercent ? 'percent' : 'amount', value: parseInt(disc[1]) } })
    } else {
      actions.push({ type: 'discount', params: { type: 'amount', value: 10 } })
    }
  }
  if (/券/.test(nl)) actions.push({ type: 'send_coupon', params: { code: 'DEFAULT', amount: 50 } })
  if (/通知|提醒|短信|邮件|推送/.test(nl)) {
    const ch = /邮件/.test(nl) ? 'email' : /短信/.test(nl) ? 'sms' : /推送/.test(nl) ? 'push' : 'sms'
    actions.push({ type: 'notify', params: { channel: ch, template: 'default' } })
  }
  if (/推荐/.test(nl)) actions.push({ type: 'recommend', params: { target: 'default' } })
  if (/冻结|封禁/.test(nl)) actions.push({ type: 'freeze', params: { reason: 'auto' } })
  if (!actions.length) actions.push({ type: 'pass', params: {} })

  return {
    name: nl.slice(0, 20),
    type: 'IF_THEN',
    conditions, logic: /且|并且/.test(nl) ? 'AND' : 'OR',
    actions, priority: 5
  }
}

// ===== 规则执行 =====
function runTest() {
  let data
  try { data = JSON.parse(testDataJson.value) }
  catch { return ElMessage.error('测试数据 JSON 格式错误') }
  if (!rule.conditions.length) return ElMessage.warning('规则条件为空')
  executing.value = true
  try {
    const r = execute(rule, data)
    execResult.value = r
    historyList.value.unshift({
      nl: nlInput.value,
      ruleName: rule.name || '未命名',
      rule: JSON.stringify(rule),
      testData: testDataJson.value,
      passed: r.passed,
      reason: r.reason,
      action: r.action,
      score: r.score,
      executedAt: new Date().toLocaleTimeString()
    })
    if (historyList.value.length > 20) historyList.value.pop()
  } catch (e) {
    ElMessage.error('执行失败: ' + e.message)
  } finally {
    executing.value = false
  }
}

function execute(rule, data) {
  const condResults = rule.conditions.map(c => {
    const val = getByPath(data, c.field)
    let pass = false
    switch (c.operator) {
      case '==': pass = val == c.value; break
      case '!=': pass = val != c.value; break
      case '>': pass = Number(val) > Number(c.value); break
      case '>=': pass = Number(val) >= Number(c.value); break
      case '<': pass = Number(val) < Number(c.value); break
      case '<=': pass = Number(val) <= Number(c.value); break
      case 'contains': pass = String(val).includes(String(c.value)); break
    }
    return { cond: c, pass, val }
  })
  const pass = rule.logic === 'OR'
    ? condResults.some(c => c.pass)
    : condResults.every(c => c.pass)
  const score = Math.round(condResults.filter(c => c.pass).length / condResults.length * 100)
  return {
    passed: pass,
    score,
    reason: condResults.map(c => `${c.pass ? '✓' : '✗'} ${c.cond.label} (实际: ${c.val})`).join(' / '),
    action: pass && rule.actions.length ? rule.actions[0] : null
  }
}

function getByPath(obj, path) {
  return path.split('.').reduce((o, k) => o?.[k], obj)
}

function restoreHistory(h) {
  nlInput.value = h.nl
  testDataJson.value = h.testData
  try { Object.assign(rule, JSON.parse(h.rule)) } catch {}
  ElMessage.info(`已恢复历史: ${h.ruleName}`)
}

// ===== 保存/加载 =====
async function loadSavedList() {
  try {
    const res = await ruleApi.list({ page: 1, size: 50 })
    if (res.code === 0) savedRules.value = res.data?.list || res.data || []
  } catch (e) { console.error('loadSaved', e) }
}
function loadRule(row) {
  const r = row.rule ? (typeof row.rule === 'string' ? JSON.parse(row.rule) : row.rule) : row
  Object.assign(rule, normalizeRule(r))
  nlInput.value = `${r.conditions?.map(c => c.label).join(' 且 ')} → ${r.actions?.map(formatAction).join(', ')}`
  ElMessage.success(`已加载: ${row.name}`)
}
async function saveCurrent() {
  saving.value = true
  try {
    const payload = { ...rule, rule: JSON.stringify(rule) }
    if (rule.id) await ruleApi.update(rule.id, payload)
    else await ruleApi.create(payload)
    ElMessage.success('保存成功')
    await loadSavedList()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  } finally { saving.value = false }
}
async function deleteRule(row) {
  await ElMessageBox.confirm(`确认删除 "${row.name}"?`, '提示', { type: 'warning' })
  await ruleApi.remove(row.id)
  ElMessage.success('已删除')
  await loadSavedList()
}
function countConditions(r) { return r.conditions?.length || r.rule ? (typeof r.rule === 'string' ? JSON.parse(r.rule).conditions?.length : r.rule.conditions?.length) || 0 : 0 }
function countActions(r) { return r.actions?.length || r.rule ? (typeof r.rule === 'string' ? JSON.parse(r.rule).actions?.length : r.rule.actions?.length) || 0 : 0 }

// ===== 初始化 =====
async function checkLlmStatus() {
  try {
    const res = await multimodalApi.status()
    if (res.code === 0) llmReady.value = !!res.data?.qwen?.ready
  } catch {}
}
function resetAll() {
  nlInput.value = ''
  Object.assign(rule, { name: '', conditions: [], actions: [], logic: 'AND', priority: 1 })
  execResult.value = null
  testPreset.value = 'user'
  applyTestPreset('user')
  appliedTemplate.value = null
}
function parsedRule() { return rule }

onMounted(() => {
  applyTestPreset('user')
  checkLlmStatus()
  loadSavedList()
})
</script>

<style scoped>
.rule-page { padding: 24px; max-width: 1600px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.title-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.title-row h2 { margin: 0; }
.header-actions { margin-left: auto; display: flex; gap: 8px; }
.subtitle { color: #64748b; font-size: 0.9em; margin-top: 4px; }
.left-card, .right-card, .bottom-card { border-radius: 12px; }
.section { margin-bottom: 20px; }
.section h3 { font-size: 1.05em; margin: 0 0 12px; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.section h3 .hint { font-size: 0.8em; color: #94a3b8; font-weight: normal; margin-left: 4px; }

.template-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.template-card { padding: 12px; border: 2px solid #e2e8f0; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.template-card:hover { border-color: #409eff; background: #f0f9ff; }
.template-card.active { border-color: #67c23a; background: #f0f9eb; }
.t-icon { font-size: 24px; margin-bottom: 4px; }
.t-name { font-weight: 600; color: #1e293b; font-size: 0.95em; }
.t-desc { color: #64748b; font-size: 0.8em; margin-top: 2px; }

.nl-actions { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.char-count { color: #94a3b8; font-size: 0.8em; margin-left: auto; }

.chip-list { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.chip { display: inline-flex; align-items: center; padding: 4px 8px; border-radius: 16px; font-size: 0.85em; }
.cond-chip { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
.action-chip { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
.chip-label { margin-right: 4px; }

.json-preview { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 6px; font-size: 0.85em; overflow-x: auto; }

.test-data { margin: 8px 0; }
.exec-result { margin-top: 12px; padding: 12px; border-radius: 8px; border: 1px solid; }
.exec-result.pass { background: #f0f9eb; border-color: #67c23a; }
.exec-result.fail { background: #fef2f2; border-color: #f56c6c; }
.result-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.result-reason { font-size: 0.85em; color: #475569; }
.result-action { font-size: 0.85em; margin-top: 6px; padding: 6px; background: rgba(255,255,255,0.5); border-radius: 4px; }

.history-item { padding: 8px 12px; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
.history-item:hover { background: #f8fafc; }
.h-line1 { display: flex; align-items: center; gap: 8px; }
.h-name { font-weight: 600; font-size: 0.9em; }
.h-nl { color: #64748b; font-size: 0.8em; margin: 2px 0; }
.h-time { color: #94a3b8; font-size: 0.75em; }
</style>
