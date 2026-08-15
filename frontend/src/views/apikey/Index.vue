<!-- @file apikey/Index.vue - API Key 管理 V6.8.12 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🔑 API Key 管理</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadKeys">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="showCreate = true">
          <el-icon><Plus /></el-icon>生成 Key
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#409eff">{{ keys.length }}</div>
        <div style="font-size:12px;color:#909399">总 Key 数</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#67c23a">{{ activeCount }}</div>
        <div style="font-size:12px;color:#909399">启用中</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ totalUsed.toLocaleString() }}</div>
        <div style="font-size:12px;color:#909399">总调用量</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#909399">{{ totalQuota || '无限' }}</div>
        <div style="font-size:12px;color:#909399">总限额</div>
      </el-card></el-col>
    </el-row>

    <el-table :data="keys" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.name }}</div>
          <div style="font-size:11px;color:#909399">{{ row.description || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="Key" width="300">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:4px">
            <code style="font-size:12px;background:#f5f7fa;padding:2px 6px;border-radius:4px;flex:1;overflow:hidden;text-overflow:ellipsis">
              {{ row.show ? row.key : row.key?.slice(0, 8) + '•'.repeat(28) + row.key?.slice(-4) }}
            </code>
            <el-button size="small" link @click="row.show = !row.show">
              <el-icon><View v-if="!row.show" /><Hide v-else /></el-icon>
            </el-button>
            <el-button size="small" link type="primary" @click="copyKey(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用量" width="140">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:6px">
            <el-progress :percentage="quotaPercent(row)" :stroke-width="6"
              :status="quotaStatus(row)" style="width:80px" />
            <span style="font-size:12px;color:#909399">{{ row.used || 0 }}/{{ row.quota || '∞' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="110">
        <template #default="{ row }">
          <span style="font-size:12px;color:#909399">{{ row.expireAt || '永久' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button size="small" link @click="toggleKey(row)">
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
          <el-card style="margin-bottom:12px" body-style="padding:0">
            <template #header><span style="font-size:13px;font-weight:600">📌 调用示例（复制即可使用）</span></template>
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
          <el-card body-style="padding:0">
            <template #header><span style="font-size:13px;font-weight:600">📖 接口清单</span></template>
            <el-table :data="apiEndpoints" stripe size="small">
              <el-table-column prop="method" label="方法" width="70" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.method==='POST'?'success':row.method==='GET'?'primary':row.method==='DELETE'?'danger':'warning'" size="small">{{ row.method }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="path" label="路径" min-width="300">
                <template #default="{ row }">
                  <code style="font-size:12px;color:#409eff">{{ row.path }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="说明" min-width="200" />
              <el-table-column label="鉴权" width="80" align="center">
                <template #default="{ row }"><el-tag size="small" type="info">API Key</el-tag></template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- Webhook 回调说明 -->
          <el-card style="margin-top:12px" body-style="padding:16px">
            <template #header><span style="font-size:13px;font-weight:600">🔔 Webhook 回调说明</span></template>
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
              <el-button size="small" @click="testWebhook">🧪 测试 Webhook 连通性</el-button>
              <span style="font-size:12px;color:#909399;margin-left:8px">
                注册 Webhook URL 后可点击测试，确认外部系统可接收回调。
              </span>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 生成 Key 弹窗 -->
    <el-dialog v-model="showCreate" title="生成 API Key" width="480px">
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="newKey.name" placeholder="如：生产环境 Key" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newKey.description" placeholder="简要描述用途" />
        </el-form-item>
        <el-form-item label="日调用限额">
          <el-input-number v-model="newKey.quota" :min="0" style="width:100%" placeholder="0=无限" />
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
import { apiKeyApi } from '@/api/apikey'
import { Plus, Refresh, View, Hide, CopyDocument } from '@element-plus/icons-vue'

const keys = ref([])
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const newKey = reactive({ name: '', description: '', quota: 0, expireDays: 0, scopes: ['chat'] })

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
  ElMessage.info('请在外部调用文档中注册 Webhook 后，使用 cURL 测试连通性')
}

const activeCount = computed(() => keys.value.filter(k => k.enabled).length)
const totalUsed = computed(() => keys.value.reduce((s, k) => s + (k.used || 0), 0))
const totalQuota = computed(() => {
  const q = keys.value.reduce((s, k) => s + (k.quota || 0), 0)
  return q || null
})

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
  } catch { keys.value = [] }
  finally { loading.value = false }
}

async function createKey() {
  if (!newKey.name.trim()) { ElMessage.warning('请填写名称'); return }
  creating.value = true
  try {
    const r = await apiKeyApi.create(newKey)
    const newK = r.data || {}
    // V6.8.1 fix: 后端 ApiKeyResponse.rawKey，前端误用 newK.key
    ElMessageBox.alert(
      `<div style="font-size:13px">Key 已生成，请妥善保存：</div><div style="margin-top:8px;font-family:monospace;background:#f5f7fa;padding:8px;border-radius:4px;word-break:break-all">${newK.rawKey || newK.keyPrefix || '生成成功'}</div>`,
      'API Key', { dangerouslyUseHTMLString: true }
    )
    showCreate.value = false
    loadKeys()
  } catch (e) { ElMessage.error('生成失败：' + (e.message || '')) }
  finally { creating.value = false }
}

async function revokeKey(k) {
  await ElMessageBox.confirm('撤销后该 Key 将立即失效，确认？', '警告', { type: 'warning' })
  try {
    await apiKeyApi.remove(k.id)
    ElMessage.success('已撤销')
    loadKeys()
  } catch { ElMessage.error('撤销失败') }
}

async function toggleKey(k) {
  try {
    // V6.8.1 fix: update → toggle (apikey.js 只有 toggle 方法)
    await apiKeyApi.toggle(k.id, !k.enabled)
    k.enabled = !k.enabled
    ElMessage.success(k.enabled ? '已启用' : '已禁用')
  } catch { ElMessage.error('操作失败') }
}

async function refreshKey(k) {
  try {
    await apiKeyApi.refresh?.(k.id)
    ElMessage.success('Key 已刷新')
    loadKeys()
  } catch { ElMessage.error('刷新失败') }
}

function copyKey(k) {
  navigator.clipboard.writeText(k.key || '').then(() => ElMessage.success('已复制')).catch(() => ElMessage.error('复制失败'))
}

onMounted(loadKeys)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
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
}
</style>
