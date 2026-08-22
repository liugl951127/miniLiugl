<!-- @file apikey/Index.vue - API Key 管理 V6.8.13 (企业级) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🔑 API Key 管理</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" :icon="Refresh" :loading="loading" @click="loadKeys">刷新</el-button>
        <el-button size="small" type="primary" :icon="Plus" @click="openCreate">生成 Key</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center" shadow="never">
        <div style="font-size:22px;font-weight:700;color:#409eff">{{ keys.length }}</div>
        <div style="font-size:12px;color:#909399">总 Key 数</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center" shadow="never">
        <div style="font-size:22px;font-weight:700;color:#67c23a">{{ activeCount }}</div>
        <div style="font-size:12px;color:#909399">启用中</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center" shadow="never">
        <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ totalUsed.toLocaleString() }}</div>
        <div style="font-size:12px;color:#909399">总调用量</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center" shadow="never">
        <div style="font-size:22px;font-weight:700;color:#909399">{{ totalQuotaLabel }}</div>
        <div style="font-size:12px;color:#909399">总限额</div>
      </el-card></el-col>
    </el-row>

    <!-- 过滤/搜索 -->
    <div class="toolbar">
      <el-input
        v-model="filterName"
        placeholder="按名称过滤"
        clearable
        :prefix-icon="Search"
        style="width:220px"
      />
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width:140px">
        <el-option label="启用" value="enabled" />
        <el-option label="禁用" value="disabled" />
      </el-select>
    </div>

    <el-table
      :data="filteredKeys"
      v-loading="loading"
      stripe
      :empty-text="loading ? '加载中…' : '暂无 API Key，点击右上角“生成 Key”创建'"
    >
      <el-table-column label="名称" min-width="180">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.name }}</div>
          <div style="font-size:11px;color:#909399">{{ row.description || '—' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="Key" min-width="320">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:4px">
            <code class="key-code">
              {{ row.show ? row.key : maskKey(row.key) }}
            </code>
            <el-button size="small" link :title="row.show ? '隐藏' : '显示'" @click="row.show = !row.show">
              <el-icon><View v-if="!row.show" /><Hide v-else /></el-icon>
            </el-button>
            <el-button size="small" link type="primary" title="复制完整 Key" @click="copyKey(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用量" width="160">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:6px">
            <el-progress
              :percentage="quotaPercent(row)"
              :stroke-width="6"
              :status="quotaStatus(row)"
              style="width:90px"
            />
            <span style="font-size:12px;color:#909399">{{ row.used || 0 }}/{{ row.quota || '∞' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="120">
        <template #default="{ row }">
          <span style="font-size:12px;color:#909399">{{ row.expireAt || '永久' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link :loading="togglingId === row.id" @click="toggleKey(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" link type="primary" @click="refreshKey(row)">刷新</el-button>
          <el-button size="small" link type="danger" @click="revokeKey(row)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- ========== 🤖 外部系统 Agent 编排 API ========== -->
    <el-tabs style="margin-top:24px" v-model="extTab">
      <el-tab-pane label="📡 Agent 外部调用文档" name="agent-doc">
        <div class="api-doc">
          <el-alert type="info" :closable="false" style="margin-bottom:16px">
            <template #title>
              <b>Agent 编排外部调用说明</b>
              <span style="font-weight:normal;margin-left:8px">
                外部系统通过 API Key 鉴权，调用平台编排的 AI 智能体。
                支持同步/异步/SSE 流式三种调用模式。
              </span>
            </template>
          </el-alert>

          <!-- 调用示例 -->
          <el-card style="margin-bottom:12px" body-style="padding:0" shadow="never">
            <template #header>
              <span style="font-size:13px;font-weight:600">📌 调用示例（复制即可使用）</span>
            </template>
            <el-tabs style="padding:0 16px 16px">
              <el-tab-pane label="cURL">
                <pre class="code-block"># 1. 同步调用（等待结果）
curl -X POST https://your-domain.com/api/v1/agent/external/run \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "goal": "查北京明天天气并发邮件给我",
    "tools": ["weather", "email"],
    "params": {"city": "北京"}
  }'

# 2. 异步调用（返回 taskId，通过 Webhook 接收结果）
curl -X POST https://your-domain.com/api/v1/agent/external/run-async \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "goal": "分析本月销售数据生成报告",
    "tools": ["sql_query", "report_generator"],
    "webhookUrl": "https://your-system.com/webhook/agent-result"
  }'

# 3. 查询任务状态
curl -X GET https://your-domain.com/api/v1/agent/external/tasks/{taskId} \
  -H "Authorization: Bearer YOUR_API_KEY"

# 4. SSE 流式调用（实时推送思考过程）
curl -X POST https://your-domain.com/api/v1/agent/external/run-stream \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"goal": "帮我写一份技术方案", "tools": ["code_generator"]}'</pre>
              </el-tab-pane>
              <el-tab-pane label="Python">
                <pre class="code-block">import requests

API_KEY = "YOUR_API_KEY"
BASE_URL = "https://your-domain.com"

headers = {"Authorization": f"Bearer {API_KEY}", "Content-Type": "application/json"}

# 同步调用
resp = requests.post(f"{BASE_URL}/api/v1/agent/external/run",
    headers=headers,
    json={"goal": "查北京明天天气", "tools": ["weather"]})
print(resp.json())

# 异步调用 + Webhook
resp = requests.post(f"{BASE_URL}/api/v1/agent/external/run-async",
    headers=headers,
    json={
        "goal": "分析本月销售数据",
        "tools": ["sql_query"],
        "webhookUrl": "https://your-system.com/webhook/agent-result"
    })
task = resp.json()["data"]
print(f"taskId: {task['taskId']}")

# 查询状态
task_id = task["taskId"]
status = requests.get(f"{BASE_URL}/api/v1/agent/external/tasks/{task_id}", headers=headers)
print(status.json())</pre>
              </el-tab-pane>
              <el-tab-pane label="JavaScript">
                <pre class="code-block">const API_KEY = "YOUR_API_KEY"
const BASE = "https://your-domain.com"

// 同步调用
const run = await fetch(`${BASE}/api/v1/agent/external/run`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${API_KEY}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ goal: '查北京明天天气', tools: ['weather'] })
})
const result = await run.json()
console.log(result)

// 异步调用
const task = await fetch(`${BASE}/api/v1/agent/external/run-async`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${API_KEY}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ goal: '分析销售数据', tools: ['sql'], webhookUrl: 'https://your.com/hook' })
})
const { data: { taskId } } = await task.json()

// SSE 流式
const es = new EventSource(`${BASE}/api/v1/agent/external/run-stream`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${API_KEY}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ goal: '帮我写技术方案', tools: [] })
})
es.onmessage = e => console.log(JSON.parse(e.data))</pre>
              </el-tab-pane>
            </el-tabs>
          </el-card>

          <!-- 接口列表 -->
          <el-card body-style="padding:0" shadow="never">
            <template #header>
              <span style="font-size:13px;font-weight:600">📖 接口清单</span>
            </template>
            <el-table :data="apiEndpoints" stripe size="small">
              <el-table-column prop="method" label="方法" width="80" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="row.method==='POST'?'success':row.method==='GET'?'primary':row.method==='DELETE'?'danger':'warning'"
                    size="small"
                  >{{ row.method }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="path" label="路径" min-width="320">
                <template #default="{ row }">
                  <code style="font-size:12px;color:#409eff">{{ row.path }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="说明" min-width="220" />
              <el-table-column label="鉴权" width="90" align="center">
                <template #default><el-tag size="small" type="info">API Key</el-tag></template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- Webhook 回调说明 -->
          <el-card style="margin-top:12px" body-style="padding:16px" shadow="never">
            <template #header>
              <span style="font-size:13px;font-weight:600">🔔 Webhook 回调说明</span>
            </template>
            <p style="font-size:13px;color:#606266;margin:0 0 8px">
              异步任务完成后，系统会 POST 回调你注册的 Webhook URL。
              请求头包含 <code>X-Webhook-Secret</code>（注册时填的密钥）和 <code>X-Task-Id</code>。
            </p>
            <pre class="code-block" style="background:#f5f7fa;padding:12px;border-radius:4px;font-size:12px">
# Webhook 回调格式（POST）
Headers:
  Content-Type: application/json
  X-Webhook-Secret: 你注册的密钥
  X-Task-Id: agt-xxx-xxx

Body:
{
  "event": "agent.task.completed",
  "taskId": "agt-12345-6789",
  "agentId": "data_analyst",
  "goal": "分析本月销售数据",
  "status": "SUCCESS",           // SUCCESS | FAILED
  "completedAt": "2026-08-12T06:30:00",
  "result": {
    "answer": "本月销售数据已生成...",
    "steps": [...],
    "durationMs": 3200
  },
  "error": null                    // FAILED 时有值
}</pre>
            <div style="margin-top:8px">
              <el-button size="small" :loading="testingWebhook" @click="testWebhook">
                🧪 测试 Webhook 连通性
              </el-button>
              <span style="font-size:12px;color:#909399;margin-left:8px">
                注册 Webhook URL 后可点击测试，确认外部系统可接收回调。
              </span>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- P1-8: Webhook测试结果弹窗 -->
    <el-dialog
      v-model="testResultVisible"
      title="🧪 Webhook 测试结果"
      width="500px"
      destroy-on-close
    >
      <div style="margin-bottom:12px">
        <el-tag
          :type="testResultTagType"
          size="large"
        >{{ testResultStatus }}</el-tag>
      </div>
      <pre class="dialog-pre">{{ testResultContent }}</pre>
      <template #footer>
        <el-button @click="testResultVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 生成 Key 弹窗 -->
    <el-dialog
      v-model="showCreate"
      title="生成 API Key"
      width="480px"
      :close-on-click-modal="false"
      @closed="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="newKey" :rules="createRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="newKey.name" placeholder="如：生产环境 Key" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newKey.description" placeholder="简要描述用途" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="日调用限额">
          <el-input-number
            v-model="newKey.quota"
            :min="0"
            style="width:100%"
            placeholder="0=无限"
          />
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="newKey.expireDays" style="width:100%">
            <el-option :value="0" label="永久有效" />
            <el-option :value="7" label="7 天" />
            <el-option :value="30" label="30 天" />
            <el-option :value="90" label="90 天" />
            <el-option :value="365" label="1 年" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限范围">
          <el-checkbox-group v-model="newKey.scopes">
            <el-checkbox label="chat">对话</el-checkbox>
            <el-checkbox label="embedding">向量化</el-checkbox>
            <el-checkbox label="image">图片生成</el-checkbox>
            <el-checkbox label="audio">音频处理</el-checkbox>
            <el-checkbox label="agent:run">🤖 Agent 运行</el-checkbox>
            <el-checkbox label="agent:stream">🤖 Agent 流式</el-checkbox>
            <el-checkbox label="agent:webhook">🔔 Webhook 管理</el-checkbox>
            <el-checkbox label="admin">管理权限</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createKey">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, View, Hide, CopyDocument, Search } from '@element-plus/icons-vue'
import { apiKeyApi } from '@/api/apikey'

const keys = ref([])
const loading = ref(false)
const creating = ref(false)
const togglingId = ref(null)
const refreshingId = ref(null)
const showCreate = ref(false)
const createFormRef = ref(null)
const newKey = reactive({
  name: '',
  description: '',
  quota: 0,
  expireDays: 0,
  scopes: ['chat'],
})
const filterName = ref('')
const filterStatus = ref('')

// 过滤后的列表
const filteredKeys = computed(() => {
  return keys.value.filter(k => {
    if (filterName.value && !k.name?.toLowerCase().includes(filterName.value.toLowerCase())) {
      return false
    }
    if (filterStatus.value === 'enabled' && !k.enabled) return false
    if (filterStatus.value === 'disabled' && k.enabled) return false
    return true
  })
})

const createRules = {
  name: [
    { required: true, message: '请输入 Key 名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度需在 2-50 字符', trigger: 'blur' },
  ],
}

// P1-8: Webhook测试结果
const testResultVisible = ref(false)
const testResultStatus = ref('')
const testResultContent = ref('')
const testingWebhook = ref(false)

const testResultTagType = computed(() => {
  if (testResultStatus.value.includes('成功')) return 'success'
  if (testResultStatus.value.includes('失败')) return 'danger'
  return 'warning'
})

// 外部 API 文档
const extTab = ref('')
const apiEndpoints = [
  { method: 'POST', path: '/api/v1/agent/external/run', desc: '同步运行 Agent（立即返回结果，最长 60s）' },
  { method: 'POST', path: '/api/v1/agent/external/run-async', desc: '异步运行（立即返回 taskId，结果通过 Webhook 回调）' },
  { method: 'GET', path: '/api/v1/agent/external/tasks/{taskId}', desc: '查询异步任务状态和结果' },
  { method: 'POST', path: '/api/v1/agent/external/run-stream', desc: 'SSE 流式运行（实时推送 Agent 思考过程）' },
  { method: 'GET', path: '/api/v1/agent/external/agents', desc: '列出当前用户可调用的 Agent 列表' },
  { method: 'POST', path: '/api/v1/agent/external/webhook', desc: '注册 Webhook URL（异步结果回调通知）' },
  { method: 'GET', path: '/api/v1/agent/external/webhooks', desc: '列出已注册的 Webhook' },
  { method: 'DELETE', path: '/api/v1/agent/external/webhook/{id}', desc: '删除 Webhook' },
  { method: 'GET', path: '/api/v1/agent/external/webhook/ping', desc: '测试 Webhook 连通性（发 ping）' },
]

async function testWebhook() {
  let webhookUrl
  try {
    const r = await ElMessageBox.prompt(
      '请输入要测试的 Webhook URL：',
      '测试 Webhook 连通性',
      { confirmButtonText: '发送测试', cancelButtonText: '取消', inputValue: 'https://' }
    )
    webhookUrl = r.value
  } catch {
    return
  }
  if (!webhookUrl) return

  testResultVisible.value = true
  testResultStatus.value = ''
  testResultContent.value = '正在发送测试请求…'
  testingWebhook.value = true

  try {
    const start = Date.now()
    await fetch(webhookUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        event: 'webhook.test',
        message: '这是一条来自 Liugl-AI 平台的 Webhook 测试消息',
        timestamp: new Date().toISOString(),
      }),
      mode: 'no-cors',
    })
    const elapsed = Date.now() - start
    testResultStatus.value = elapsed < 5000 ? '✅ 连接成功' : '⚠️ 连接成功但响应慢'
    testResultContent.value = `请求耗时: ${elapsed}ms\n\n说明: 由于跨域限制，无法获取完整响应内容。\n若 URL 有效，通常会返回 2xx 状态码。\n\n测试数据已发送，请检查目标服务器是否收到回调。`
    ElMessage.success('Webhook 测试完成')
  } catch (e) {
    testResultStatus.value = '❌ 连接失败'
    testResultContent.value = '错误: ' + (e.message || '无法连接到目标 URL\n请确认 URL 是否正确且服务器可访问')
    ElMessage.error('Webhook 测试失败')
  } finally {
    testingWebhook.value = false
  }
}

const activeCount = computed(() => keys.value.filter(k => k.enabled).length)
const totalUsed = computed(() => keys.value.reduce((s, k) => s + (k.used || 0), 0))
const totalQuotaLabel = computed(() => {
  const q = keys.value.reduce((s, k) => s + (k.quota || 0), 0)
  return q ? q.toLocaleString() : '无限'
})

function maskKey(k) {
  if (!k) return ''
  if (k.length <= 12) return '•'.repeat(k.length)
  return k.slice(0, 8) + '•'.repeat(20) + k.slice(-4)
}

function quotaPercent(row) {
  if (!row.quota) return 0
  return Math.min(100, Math.round(((row.used || 0) / row.quota) * 100))
}
function quotaStatus(row) {
  const p = quotaPercent(row)
  if (p >= 100) return 'exception'
  if (p >= 80) return 'warning'
  return ''
}

async function loadKeys() {
  loading.value = true
  try {
    const r = await apiKeyApi.list()
    keys.value = (r.data || []).map(k => ({ ...k, show: false }))
  } catch (e) {
    keys.value = []
    ElMessage.error('加载 API Key 失败：' + (e?.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  showCreate.value = true
}

function resetCreateForm() {
  newKey.name = ''
  newKey.description = ''
  newKey.quota = 0
  newKey.expireDays = 0
  newKey.scopes = ['chat']
  createFormRef.value?.clearValidate()
}

async function createKey() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    const r = await apiKeyApi.create(newKey)
    const newK = r.data || {}
    await ElMessageBox.alert(
      `<div style="font-size:13px">Key 已生成，请妥善保存（仅显示一次）：</div>` +
      `<div style="margin-top:8px;font-family:monospace;background:#f5f7fa;padding:8px;border-radius:4px;word-break:break-all">${newK.rawKey || newK.keyPrefix || '生成成功'}</div>`,
      'API Key',
      { dangerouslyUseHTMLString: true, confirmButtonText: '我已保存' }
    )
    showCreate.value = false
    ElMessage.success('API Key 已生成')
    loadKeys()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error('生成失败：' + (e.message || ''))
    }
  } finally {
    creating.value = false
  }
}

async function revokeKey(k) {
  try {
    await ElMessageBox.confirm(
      `撤销后 Key「${k.name}」将立即失效且无法恢复，确认？`,
      '警告',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await apiKeyApi.remove(k.id)
    ElMessage.success('已撤销')
    loadKeys()
  } catch (e) {
    ElMessage.error('撤销失败：' + (e?.message || '请稍后重试'))
  }
}

async function toggleKey(k) {
  togglingId.value = k.id
  try {
    await apiKeyApi.toggle(k.id, !k.enabled)
    k.enabled = !k.enabled
    ElMessage.success(k.enabled ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || '请稍后重试'))
  } finally {
    togglingId.value = null
  }
}

async function refreshKey(k) {
  refreshingId.value = k.id
  try {
    const r = await apiKeyApi.rotate(k.id, { expireDays: k.expireDays || 0 })
    const newK = r.data || {}
    ElMessageBox.alert(
      `<div style="font-size:13px">新 Key 已生成，旧 Key 已失效：</div>` +
      `<div style="margin-top:8px;font-family:monospace;background:#f5f7fa;padding:8px;border-radius:4px;word-break:break-all">${newK.rawKey || newK.keyPrefix || '轮换成功'}</div>`,
      'API Key 已刷新',
      { dangerouslyUseHTMLString: true, confirmButtonText: '我已保存' }
    )
    ElMessage.success('Key 已刷新')
    loadKeys()
  } catch (e) {
    ElMessage.error('刷新失败：' + (e?.message || '请稍后重试'))
  } finally {
    refreshingId.value = null
  }
}

async function copyKey(k) {
  if (!k.key) {
    ElMessage.warning('该 Key 不可复制（仅在创建时显示完整值）')
    return
  }
  try {
    await navigator.clipboard.writeText(k.key)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

onMounted(loadKeys)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.key-code {
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.api-doc { padding: 0; }
.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 6px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 360px;
  overflow-y: auto;
  margin: 0;
}
.dialog-pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
