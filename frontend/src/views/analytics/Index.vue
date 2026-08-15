<!-- @file analytics/Index.vue - 数据分析中心 V6.8.12 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>📊 数据分析中心</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadAll">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="activeTab = 'nlsql'">
          <el-icon><Search /></el-icon>NL2SQL
        </el-button>
      </div>
    </div>

    <!-- 指标卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px" v-loading="metricsLoading">
      <el-col v-for="m in metrics" :key="m.label" :span="6">
        <el-tooltip :content="m.tip" placement="top" effect="light">
          <el-card shadow="hover" body-style="padding:12px;text-align:center;cursor:help">
            <div style="font-size:12px;color:#909399;margin-bottom:6px">{{ m.label }}</div>
            <div style="font-size:26px;font-weight:700" :style="{ color: m.color || '#1e40af' }">{{ m.value }}</div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <!-- 数据概览 -->
      <el-tab-pane name="overview">
        <template #label>
          <el-tooltip content="查看平台 API 调用的整体趋势、用户分布和性能指标" placement="top" effect="light">
            <span>📈 数据概览</span>
          </el-tooltip>
        </template>
        <el-row :gutter="12">
          <el-col :span="16">
            <el-card title="调用趋势" body-style="padding:16px">
              <template #header><span>调用趋势（近7天）</span></template>
              <div ref="trendChartRef" style="height:280px"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card title="模型分布" body-style="padding:16px">
              <template #header><span>模型调用分布</span></template>
              <div ref="pieChartRef" style="height:280px"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="12" style="margin-top:12px">
          <el-col :span="12">
            <el-card title="TOP 10 用户" body-style="padding:0">
              <el-table :data="topUsers" size="small" stripe>
                <el-table-column type="index" width="50" />
                <el-table-column prop="user" label="用户" />
                <el-table-column prop="calls" label="调用次数" width="100" align="center" />
                <el-table-column prop="avgLatency" label="平均延迟" width="100" align="center" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card title="成功率趋势" body-style="padding:16px">
              <template #header><span>成功率</span></template>
              <div ref="successRateRef" style="height:200px"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- NL2SQL -->
      <el-tab-pane name="nlsql">
        <template #label>
          <el-tooltip content="用自然语言生成 SQL 查询，连接数据库直接获取分析结果" placement="top" effect="light">
            <span>🔮 NL2SQL</span>
          </el-tooltip>
        </template>
        <el-row :gutter="12">
          <!-- 左侧：查询 & 结果 -->
          <el-col :span="12">
            <!-- 查询配置栏 -->
            <el-card body-style="padding:12px;margin-bottom:12px">
              <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
                <span style="font-size:12px;color:#909399;white-space:nowrap">数据源：</span>
                <el-select v-model="nlDsId" placeholder="选择数据源" size="small" style="width:140px" clearable @change="onNlDsChange">
                  <el-option v-for="ds in dataSources.filter(d => d.status === 'CONNECTED')" :key="ds.id" :label="ds.name" :value="ds.id" />
                </el-select>
                <span style="font-size:12px;color:#909399;white-space:nowrap">模型：</span>
                <el-select v-model="nlModel" placeholder="默认模型" size="small" style="width:150px" clearable filterable>
                  <el-option-group v-if="nlModelOptions.filter(m => m.local).length" label="🏷️ 自研模型">
                    <el-option v-for="m in nlModelOptions.filter(m => m.local)" :key="m.value" :label="m.label" :value="m.value" />
                  </el-option-group>
                  <el-option-group label="🤖 云端模型">
                    <el-option v-for="m in nlModelOptions.filter(m => !m.local)" :key="m.value" :label="m.label" :value="m.value" />
                  </el-option-group>
                </el-select>
                <el-tooltip content="生成 SQL 后自动执行查询" placement="top">
                  <el-switch v-model="nlAutoExec" size="small" active-text="自动执行" inactive-text="手动" />
                </el-tooltip>
                <el-button size="small" link type="primary" @click="showDsForm = true">
                  <el-icon><Plus /></el-icon>添加数据源
                </el-button>
              </div>
            </el-card>

            <!-- 自然语言查询 -->
            <el-card body-style="padding:16px" style="margin-bottom:12px">
              <template #header>
                <span>用自然语言描述你的查询需求</span>
                <el-button size="small" type="primary" :loading="nlLoading" @click="runNlQuery" style="float:right">
                  <el-icon><Search /></el-icon>{{ nlAutoExec ? '生成并执行' : '生成 SQL' }}
                </el-button>
              </template>
              <el-input v-model="nlQuery" type="textarea" :rows="4"
                placeholder="例如：过去一周每天的活跃用户数？哪些用户调用量最大？模型响应时间排名？"
                @keydown.enter.ctrl.prevent="runNlQuery" />
            </el-card>

            <!-- SQL 结果 -->
            <div v-if="nlResult">
              <!-- SQL 展示 + 操作 -->
              <el-card body-style="padding:16px" style="margin-bottom:12px">
                <template #header>
                  <div style="display:flex;align-items:center;gap:8px">
                    <span>生成的 SQL</span>
                    <el-tag v-if="nlResult.success !== false" type="success" size="small">执行成功</el-tag>
                    <el-tag v-else type="danger" size="small">失败</el-tag>
                  </div>
                  <div style="display:flex;gap:6px;float:right">
                    <el-button size="small" link type="primary" :loading="nlExplainLoading" @click="explainSql">
                      <el-icon><InfoFilled /></el-icon>解释
                    </el-button>
                    <el-button size="small" link @click="copySql">
                      <el-icon><DocumentCopy /></el-icon>复制
                    </el-button>
                    <el-button size="small" link type="success" @click="exportCsv" :disabled="!nlResult.rows?.length">
                      <el-icon><Download /></el-icon>导出 CSV
                    </el-button>
                    <el-button v-if="nlAutoExec" size="small" link type="warning" @click="runNlQuery(true)">
                      <el-icon><RefreshRight /></el-icon>重新执行
                    </el-button>
                  </div>
                </template>
                <pre v-if="nlResult.sql" class="sql-code">{{ nlResult.sql }}</pre>
                <div v-if="nlResult.promptTokens || nlResult.completionTokens" style="margin-top:6px;font-size:12px;color:#909399">
                  <span v-if="nlResult.promptTokens">Prompt tokens: {{ nlResult.promptTokens }}</span>
                  <span v-if="nlResult.completionTokens" style="margin-left:12px">Completion tokens: {{ nlResult.completionTokens }}</span>
                </div>
                <!-- SQL 解释 -->
                <div v-if="nlExplanation" style="margin-top:10px;padding:10px;background:#f0f9eb;border-radius:6px;font-size:13px;color:#67c23a">
                  <el-icon><InfoFilled /></el-icon> {{ nlExplanation }}
                </div>
              </el-card>

              <!-- 结果表格 -->
              <el-card v-if="nlResult.rows?.length" body-style="padding:0" style="margin-bottom:12px">
                <template #header>
                  <span>查询结果</span>
                  <span style="font-size:12px;color:#909399;float:right">
                    {{ nlResult.rows.length }} 条 · {{ nlResult.duration || 0 }}ms
                  </span>
                </template>
                <el-table :data="nlResult.rows" stripe size="small" max-height="320" show-summary
                  :summary-method="nlTableSummary">
                  <el-table-column v-for="col in nlResult.columns" :key="col" :prop="col" :label="col" min-width="100" show-overflow-tooltip />
                </el-table>
              </el-card>

              <!-- 安全警告 -->
              <div v-if="nlResult.explanation?.includes('⚠️')" style="padding:8px 12px;background:#fef0f0;border-radius:6px;font-size:13px;color:#f56c6c">
                {{ nlResult.explanation }}
              </div>
            </div>

            <!-- 查询历史 -->
            <el-card body-style="padding:16px">
              <template #header>
                <span>查询历史</span>
                <el-button size="small" link @click="loadHistory" :loading="nlHistoryLoading">
                  <el-icon><Refresh /></el-icon>刷新
                </el-button>
              </template>
              <div v-for="h in nlHistory" :key="h.id" class="nl-history-item" @click="loadHistoryItem(h)">
                <div style="display:flex;align-items:center;gap:6px;flex:1;min-width:0">
                  <el-tag size="small" :type="h.success ? 'success' : 'danger'" style="flex-shrink:0">
                    {{ h.success ? '✓' : '✗' }}
                  </el-tag>
                  <span class="nl-query-text">{{ h.question || h.query }}</span>
                </div>
                <span style="font-size:11px;color:#909399;white-space:nowrap;margin-left:8px">{{ h.createdAt?.slice(0,16) || '' }}</span>
              </div>
              <div v-if="!nlHistoryLoading && !nlHistory.length" style="text-align:center;color:#909399;padding:20px">暂无查询历史</div>
            </el-card>
          </el-col>

          <!-- 右侧：Schema 浏览器 -->
          <el-col :span="12">
            <el-card body-style="padding:12px;margin-bottom:12px">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
                <el-icon style="color:#409eff"><Database /></el-icon>
                <span style="font-weight:600;font-size:14px">数据 Schema</span>
                <el-tag v-if="nlDsId && nlDb" size="small" type="primary" style="margin-left:auto">{{ nlDb }}</el-tag>
                <el-button v-if="nlDb" size="small" link @click="nlDb = ''">重置</el-button>
              </div>
              <!-- 数据库选择 -->
              <el-select v-if="nlDsId" v-model="nlDb" placeholder="选择数据库" size="small" clearable style="width:100%;margin-bottom:8px" :loading="nlDbLoading" @change="onNlDbChange">
                <el-option v-for="db in nlDatabases" :key="db" :label="db" :value="db" />
              </el-select>
              <div v-if="!nlDsId" style="text-align:center;color:#909399;padding:20px 0;font-size:13px">
                请先在左侧选择数据源
              </div>

              <!-- Schema 树 -->
              <el-tree v-if="nlDb" ref="nlSchemaTree" :props="{ label: 'name', children: 'children' }"
                :load="loadSchemaTree" lazy node-key="id" empty-text="加载中..."
                style="background:transparent;font-size:13px">
                <template #default="{ node, data }">
                  <span style="display:flex;align-items:center;gap:4px">
                    <el-icon v-if="data.type === 'db'"><Collection /></el-icon>
                    <el-icon v-else-if="data.type === 'table'"><Grid /></el-icon>
                    <el-icon v-else><Plus /></el-icon>
                    <span>{{ node.label }}</span>
                    <el-tag v-if="data.type === 'column' && data.columnType" size="small" type="info" style="margin-left:4px">{{ data.columnType }}</el-tag>
                    <el-tag v-if="data.type === 'table' && data.rowCount != null" size="small" style="margin-left:4px">{{ data.rowCount }}行</el-tag>
                  </span>
                </template>
              </el-tree>
            </el-card>

            <!-- 示例查询 -->
            <el-card body-style="padding:16px">
              <template #header><span>💡 示例查询</span></template>
              <div v-for="q in sampleQueries" :key="q.text" class="sample-query" @click="nlQuery = q.text; nlDsId = q.dsId || nlDsId">
                <div style="font-size:13px;color:#409eff;font-weight:500">{{ q.text }}</div>
                <div v-if="q.tip" style="font-size:11px;color:#909399;margin-top:2px">{{ q.tip }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 投票统计 -->
      <el-tab-pane name="vote">
        <template #label>
          <el-tooltip content="用户对不同模型的投票统计和实时排行" placement="top" effect="light">
            <span>🗳️ 投票统计</span>
          </el-tooltip>
        </template>
        <!-- V6.8.1: 统计汇总 -->
        <el-row :gutter="12" style="margin-bottom:12px">
          <el-col v-for="m in voteMetrics" :key="m.label" :span="6">
            <div style="text-align:center;padding:12px;background:#f5f7fa;border-radius:8px">
              <div style="font-size:22px;font-weight:700;color:#409eff">{{ m.value }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">{{ m.label }}</div>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="16">
            <el-card title="投票趋势" body-style="padding:16px">
              <div ref="voteTrendRef" style="height:300px"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card title="实时投票" body-style="padding:16px">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
                <span style="font-size:13px;color:#909399">共 {{ recentVotes.length }} 条记录</span>
                <el-button size="small" type="success" @click="exportVotesCsv" :loading="exportingVotes">
                  <el-icon><Download /></el-icon>导出 CSV
                </el-button>
              </div>
              <!-- Day 43: 通知邮箱输入 -->
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
                <el-input v-model="voteNotifyEmail" size="small" placeholder="投票结束后通知邮箱（可选）" clearable style="flex:1" />
                <el-tooltip content="投票结束后自动发送邮件通知结果" placement="top">
                  <el-icon style="color:#909399"><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
              <div v-for="v in recentVotes" :key="v.id" class="vote-item">
                <el-avatar :size="24">{{ v.user?.charAt(0) || 'U' }}</el-avatar>
                <span style="flex:1;font-size:13px;margin-left:8px">{{ v.model }}</span>
                <el-tag size="small" type="success">{{ v.votes }}票</el-tag>
                <el-tooltip content="查看投票详情（各模型答案及置信度）" placement="top">
                  <el-button size="small" type="primary" link style="margin-left:6px" @click="openVoteDetail(v)">
                    查看详情
                  </el-button>
                </el-tooltip>
                <el-tooltip content="使用相同问题重新发起投票（可选填通知邮箱）" placement="top">
                  <el-button size="small" type="default" style="margin-left:4px" @click="onRevote(v)">
                    <el-icon><RefreshRight /></el-icon>重新投票
                  </el-button>
                </el-tooltip>
              </div>
              <div v-if="!recentVotes.length" style="text-align:center;color:#909399;padding:20px">暂无投票</div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 数据源表单 -->
    <el-dialog v-model="showDsForm" title="添加数据源" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="dsForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="dsForm.type" style="width:100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="MongoDB" value="mongodb" />
          </el-select>
        </el-form-item>
        <el-form-item label="Host"><el-input v-model="dsForm.host" placeholder="localhost" /></el-form-item>
        <el-form-item label="Port"><el-input-number v-model="dsForm.port" :min="1" :max="65535" /></el-form-item>
        <el-form-item label="数据库"><el-input v-model="dsForm.database" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="dsForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="dsForm.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDsForm = false">取消</el-button>
        <el-button type="primary" @click="addDataSource">保存并测试</el-button>
      </template>
    </el-dialog>

    <!-- 投票详情弹窗 (Day 44) -->
    <el-dialog v-model="voteDetailVisible" title="投票详情" width="680px" destroy-on-close>
      <div v-if="voteDetail">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="问题">{{ voteDetail.text }}</el-descriptions-item>
          <el-descriptions-item label="策略">{{ voteDetail.strategy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投票总数">{{ voteDetail.votes }}</el-descriptions-item>
          <el-descriptions-item label="一致率">
            <el-progress :percentage="Math.round(voteDetail.agreementRate || 0)" :color="agreementColor(voteDetail.agreementRate)" style="width:120px;display:inline-block;vertical-align:middle" :show-text="false" />
            <span style="margin-left:8px">{{ (voteDetail.agreementRate || 0).toFixed(1) }}%</span>
          </el-descriptions-item>
          <el-descriptions-item label="投票时间" :span="2">{{ voteDetail.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="font-size:14px;font-weight:600;margin-bottom:10px">各模型答案</div>
        <el-table :data="voteDetail.modelVotes || []" stripe size="small">
          <el-table-column prop="model" label="模型" width="160" />
          <el-table-column prop="answer" label="答案" min-width="200" show-overflow-tooltip />
          <el-table-column label="置信度" width="120">
            <template #default="{ row }">
              <el-progress :percentage="Math.round((row.confidence || 0) * 100)" :color="confidenceColor(row.confidence)" style="width:90px" :show-text="false" />
              <span style="margin-left:6px;font-size:12px">{{ ((row.confidence || 0) * 100).toFixed(0) }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="voteDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  nl2sqlAsk, nl2sqlHistory, nl2sqlExplain, executeQuery,
  listDataSources, createDataSource, testDataSource,
  listDatabases, listTables, describeTable,
  getVoteStatsSummary, getVoteTrend, getVoteRecords, duplicateVote,
} from '@/api/analytics'
import { listEnabledModels } from '@/api/model'
import http from '@/api/http'
import { Refresh, RefreshRight, Search, Plus, Download, InfoFilled, DocumentCopy, Collection, FolderOpened, Grid } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const activeTab = ref('overview')
const metricsLoading = ref(false)
const metrics = ref([
  { label: '总调用量', value: '-', color: '#409eff', tip: '平台累计所有 API 调用总次数' },
  { label: '今日调用', value: '-', color: '#67c23a', tip: '今日 0 点至今的 API 调用量' },
  { label: '独立用户', value: '-', color: '#e6a23c', tip: '调用过 API 的去重用户数' },
  { label: '平均延迟', value: '-', color: '#f56c6c', tip: 'API 响应的平均延迟时间' },
])

// NL2SQL
const nlQuery = ref('')
const nlLoading = ref(false)
const nlResult = ref(null)
const nlHistory = ref([])
const nlHistoryLoading = ref(false)
const showDsForm = ref(false)
const nlDsId = ref(null)        // 选中的数据源 ID
const nlDb = ref('')           // 选中的数据库
const nlDatabases = ref([])
const nlDbLoading = ref(false)
const nlModel = ref('')
const nlModelOptions = ref([])
const nlAutoExec = ref(true)
const nlExplanation = ref('')
const nlExplainLoading = ref(false)
const nlSchemaTree = ref(null)
const voteDetailVisible = ref(false)
const voteDetail = ref(null)
const sampleQueries = [
  { text: '过去7天每天的 API 调用量？', tip: '统计每日趋势', dsId: null },
  { text: '调用量最多的用户 TOP5？', tip: '按用户聚合', dsId: null },
  { text: '各模型的平均响应时间？', tip: '性能分析', dsId: null },
  { text: '失败请求的常见原因？', tip: '错误分析', dsId: null },
]

function openVoteDetail(v) {
  voteDetail.value = v
  voteDetailVisible.value = true
}

function agreementColor(rate) {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

function confidenceColor(conf) {
  if (conf >= 0.8) return '#67c23a'
  if (conf >= 0.5) return '#e6a23c'
  return '#f56c6c'
}
const dsForm = reactive({ name: '', type: 'mysql', host: 'localhost', port: 3306, database: '', username: '', password: '' })
const dataSources = ref([])
const topUsers = ref([])

const trendData = ref([])
const pieData = ref([])
const successRateData = ref([])
const modelTrendData = ref([])

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const successRateRef = ref(null)
const voteTrendRef = ref(null)
let trendChart, pieChart, successChart, voteChart

// 加载概览真实数据
async function loadOverview() {
  metricsLoading.value = true
  try {
    const [overview, trend, dist, users, successRate, modelTrend] = await Promise.all([
      http.get('/api/v1/analytics/stats/overview').catch(() => ({})),
      http.get('/api/v1/analytics/stats/trend').catch(() => []),
      http.get('/api/v1/analytics/stats/distribution').catch(() => []),
      http.get('/api/v1/analytics/stats/top-users').catch(() => []),
      http.get('/api/v1/analytics/stats/success-rate').catch(() => []),
      http.get('/api/v1/analytics/stats/model-trend').catch(() => []),
    ])

    // overview 可能是 Result 包装 { data: {...} } 或已 unwrap {...}
    const o = overview.data?.data ?? overview.data ?? overview ?? {}
    metrics.value[0].value = (o.totalCalls || 0).toLocaleString()
    metrics.value[1].value = (o.todayCalls || 0).toLocaleString()
    metrics.value[2].value = (o.totalUsers || 0).toLocaleString()
    metrics.value[3].value = o.avgLatency ? `${o.avgLatency}ms` : '—'

    // trendData: API 可能返回 { data: [...] } 或已 unwrap [...]
    const td = trend.data?.data ?? trend.data ?? trend ?? []
    trendData.value = Array.isArray(td) ? td.map(d => ({
      day: d.day || d.date || '',
      calls: Number(d.calls ?? d.cnt ?? d.value ?? 0),
    })) : []

    // pieData
    const pd = dist.data?.data ?? dist.data ?? dist ?? []
    pieData.value = Array.isArray(pd) ? pd.map(d => ({
      name: d.name || d.model || '',
      value: Number(d.value ?? d.calls ?? 0),
    })) : []

    // topUsers
    const tu = users.data?.data ?? users.data ?? users ?? []
    topUsers.value = (Array.isArray(tu) ? tu : []).slice(0, 10).map(u => ({
      user: u.user || u.userName || u.userId || '—',
      calls: Number(u.calls ?? 0),
      avgLatency: u.avgLatency ? `${u.avgLatency}ms` : '—',
    }))

    // 成功率趋势
    const sr = successRate.data?.data ?? successRate.data ?? successRate ?? []
    successRateData.value = Array.isArray(sr) ? sr : []

    // 模型调用趋势
    const mt = modelTrend.data?.data ?? modelTrend.data ?? modelTrend ?? []
    modelTrendData.value = Array.isArray(mt) ? mt : []
  } catch (e) {
    console.warn('[Analytics] 概览数据加载失败:', e)
  } finally {
    metricsLoading.value = false
  }
}

async function loadAll() {
  await loadOverview()
  await nextTick()
  renderCharts()
}

function renderCharts() {
  // 趋势图（真实数据）
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    const labels = trendData.value.map(d => d.day)
    const values = trendData.value.map(d => d.calls)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['调用量'] },
      xAxis: { type: 'category', data: labels.length ? labels : ['暂无数据'] },
      yAxis: [{ type: 'value' }],
      series: [{
        name: '调用量',
        type: 'bar',
        data: values.length ? values : [0],
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.15 },
        smooth: true,
      }]
    })
  }
  // 饼图（真实数据）
  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: pieData.value.length ? pieData.value : [{ name: '暂无数据', value: 1 }],
        label: { formatter: '{b}: {d}%' },
      }]
    })
  }
  // 成功率（真实数据）
  if (successRateRef.value) {
    if (!successChart) successChart = echarts.init(successRateRef.value)
    const srDays = successRateData.value.map(d => d.day?.slice(5) || d.day || '')
    const srValues = successRateData.value.map(d => d.rate ?? 0)
    successChart.setOption({
      tooltip: { formatter: '{c}%' },
      xAxis: { type: 'category', data: srDays.length ? srDays : ['暂无数据'] },
      yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      series: [{ type: 'line', data: srValues.length ? srValues : [0], smooth: true, areaStyle: { color: '#e8f4f0' }, itemStyle: { color: '#67c23a' } }]
    })
  }
  // 模型调用趋势（真实数据）
  if (voteTrendRef.value) {
    if (!voteChart) voteChart = echarts.init(voteTrendRef.value)
    const mtDays = modelTrendData.value.map(d => d.day?.slice(5) || d.day || '')
    const modelNames = Object.keys(modelTrendData.value[0] || {}).filter(k => k !== 'day')
    const series = modelNames.map((name, i) => ({
      name,
      type: 'line',
      data: modelTrendData.value.map(d => d[name] || 0),
    }))
    voteChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: modelNames.length ? modelNames : ['暂无数据'] },
      xAxis: { type: 'category', data: mtDays.length ? mtDays : ['暂无数据'] },
      yAxis: { type: 'value' },
      series: series.length ? series : [{ name: '暂无数据', type: 'line', data: [] }],
    })
  }
}

async function runNlQuery(reExec = false) {
  if (!nlQuery.value.trim()) { ElMessage.warning('请输入查询内容'); return }
  nlLoading.value = true
  nlResult.value = null
  nlExplanation.value = ''
  try {
    const req = { question: nlQuery.value }
    if (nlDsId.value) req.dataSourceId = nlDsId.value
    if (nlDb.value) req.database = nlDb.value
    if (nlModel.value) req.model = nlModel.value
    if (!reExec) req.autoExecute = nlAutoExec.value

    const r = await nl2sqlAsk(req)
    const data = r.data || {}

    if (data.generatedSql) {
      nlResult.value = {
        sql: data.generatedSql,
        success: data.success !== false,
        explanation: data.explanation || '',
        promptTokens: data.promptTokens,
        completionTokens: data.completionTokens,
        duration: data.durationMs || data.duration || 0,
        rows: [],
        columns: [],
      }
      // 自动执行或手动重执行
      if (nlAutoExec.value || reExec) {
        try {
          const qr = await executeQuery({ sql: data.generatedSql })
          nlResult.value.rows = qr.data?.rows || qr.data || []
          nlResult.value.columns = qr.data?.columns || (nlResult.value.rows[0] ? Object.keys(nlResult.value.rows[0]) : [])
          nlResult.value.duration = qr.data?.duration || 0
          nlResult.value.success = true
        } catch (e) {
          nlResult.value.success = false
          nlResult.value.errorMsg = e.message || '执行失败'
          ElMessage.error('SQL 执行失败：' + (e.message || ''))
        }
      }
    } else {
      nlResult.value = { sql: null, success: false, errorMsg: '未生成 SQL' }
      ElMessage.error(data.explanation || '未生成 SQL')
    }
  } catch (e) {
    ElMessage.error('查询失败：' + (e.message || ''))
    nlResult.value = { success: false, errorMsg: e.message || '' }
  } finally { nlLoading.value = false }
}

async function explainSql() {
  if (!nlResult.value?.sql) return
  nlExplainLoading.value = true
  try {
    const r = await nl2sqlExplain(nlResult.value.sql)
    nlExplanation.value = r.data || r || ''
  } catch (e) {
    nlExplanation.value = '(解释失败: ' + (e.message || '') + ')'
  } finally { nlExplainLoading.value = false }
}

async function exportCsv() {
  const rows = nlResult.value?.rows
  const cols = nlResult.value?.columns
  if (!rows?.length || !cols?.length) { ElMessage.warning('无数据可导出'); return }
  const csvRows = [cols.join(',')]
  for (const row of rows) {
    csvRows.push(cols.map(c => {
      const v = String(row[c] ?? '')
      return v.includes(',') || v.includes('"') || v.includes('\n') ? `"${v.replace(/"/g, '""')}"` : v
    }).join(','))
  }
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvRows.join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `nl2sql_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('已导出 ' + rows.length + ' 条数据')
}

function nlTableSummary({ columns, data }) {
  return columns.map(col => {
    const nums = data.map(r => parseFloat(r[col])).filter(v => !isNaN(v))
    if (!nums.length) return ''
    const sum = nums.reduce((a, b) => a + b, 0)
    return isInteger(sum) ? sum.toLocaleString() : sum.toFixed(2)
  })
}

function isInteger(n) { return Number.isInteger(n) }

async function loadHistory() {
  nlHistoryLoading.value = true
  try {
    const r = await nl2sqlHistory({ page: 1, size: 20 })
    nlHistory.value = r.data || r || []
  } catch { nlHistory.value = [] }
  finally { nlHistoryLoading.value = false }
}

function loadHistoryItem(h) {
  nlQuery.value = h.question || h.query || ''
  if (h.generatedSql) {
    nlResult.value = {
      sql: h.generatedSql,
      success: h.success !== false,
      rows: [],
      columns: [],
    }
    nlExplanation.value = ''
  }
}

async function copySql() {
  if (!nlResult.value?.sql) return
  try {
    await navigator.clipboard.writeText(nlResult.value.sql)
    ElMessage.success('SQL 已复制')
  } catch { ElMessage.error('复制失败') }
}

async function onNlDsChange() {
  nlDb.value = ''
  nlDatabases.value = []
  if (nlDsId.value) {
    nlDbLoading.value = true
    try {
      const r = await listDatabases(nlDsId.value)
      nlDatabases.value = r.data || r || []
    } catch { nlDatabases.value = [] }
    finally { nlDbLoading.value = false }
  }
}

function onNlDbChange() {
  if (nlSchemaTree.value) nlSchemaTree.value.clearChecked()
}

async function loadSchemaTree(node, resolve) {
  const { data, level } = node
  if (level === 0) {
    // 根节点：展示表列表
    if (!nlDsId.value || !nlDb.value) return resolve([])
    try {
      const r = await listTables(nlDsId.value, nlDb.value)
      const tables = r.data || r || []
      resolve(tables.map(t => ({
        id: `table:${t.name}`,
        name: t.name,
        type: 'table',
        rowCount: t.rowCount,
        children: [],
      })))
    } catch { resolve([]) }
  } else if (level === 1) {
    // 表节点：展示列
    const tableName = data.name
    try {
      const r = await describeTable(nlDsId.value, nlDb.value, tableName)
      const info = r.data || r || {}
      const cols = info.columns || []
      resolve(cols.map((c, i) => ({
        id: `col:${tableName}:${c.name || c.field || c.columnName || 'col' + i}`,
        name: c.name || c.field || c.columnName || 'col' + i,
        type: 'column',
        columnType: c.type || c.dataType || c.colType || '',
      })))
    } catch { resolve([]) }
  } else {
    resolve([])
  }
}

async function loadDataSources() {
  try {
    const r = await listDataSources()
    dataSources.value = r.data || []
    // 自动选中第一个已连接的数据源
    if (!nlDsId.value && dataSources.value.length) {
      const first = dataSources.value.find(d => d.status === 'CONNECTED') || dataSources.value[0]
      nlDsId.value = first.id
      await onNlDsChange()
    }
  } catch { dataSources.value = [] }
}

async function addDataSource() {
  if (!dsForm.name) { ElMessage.warning('请填写名称'); return }
  try {
    await createDataSource(dsForm)
    ElMessage.success('数据源已添加')
    showDsForm.value = false
    loadDataSources()
  } catch (e) { ElMessage.error('添加失败：' + (e.message || '')) }
}

async function testDs(row) {
  try {
    await testDataSource(row)
    ElMessage.success('连接正常')
  } catch { ElMessage.error('连接失败') }
}

const recentVotes = ref([])
const exportingVotes = ref(false)
const voteNotifyEmail = ref('') // Day 43: 投票结束通知邮箱

/**
 * 投票历史导出 CSV (Day 42)
 * 将 recentVotes 中的所有记录导出为 .csv 文件
 */
async function exportVotesCsv() {
  if (!recentVotes.value.length) {
    ElMessage.warning('暂无投票记录可导出')
    return
  }
  exportingVotes.value = true
  try {
    // 获取完整投票记录（不限条数）
    const r = await getVoteRecords({ limit: 500 })
    const records = r.data?.records || r.data?.list || r.data || []

    const rows = []
    // CSV 表头
    rows.push(['ID', '问题', '策略', '参与模型数', '总票数', '状态', '创建时间'].join(','))
    for (const v of records) {
      const row = [
        v.id ?? '',
        (v.text || '').replace(/"/g, '""'),
        v.strategy || '',
        Array.isArray(v.models) ? v.models.length : (v.modelCount || 0),
        v.totalVotes ?? 0,
        v.status || '',
        v.createdAt || v.createTime || ''
      ]
      rows.push(row.map(cell => {
        const s = String(cell)
        return s.includes(',') || s.includes('"') || s.includes('\n')
          ? `"${s.replace(/"/g, '""')}"`
          : s
      }).join(','))
    }

    const BOM = '\uFEFF' // UTF-8 BOM
    const csvContent = BOM + rows.join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `投票历史_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${records.length} 条投票记录`)
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || ''))
  } finally {
    exportingVotes.value = false
  }
}

// V6.8.1 fix: vote 真实数据
const voteMetrics = ref([
  { label: '投票总数', value: '-', color: '#409eff', tip: '平台累计投票总次数' },
  { label: '一致率', value: '-', color: '#67c23a', tip: '模型投票一致率均值（越高越好）' },
  { label: '共识率', value: '-', color: '#e6a23c', tip: '达成共识的投票占比' },
  { label: '活跃模型', value: '-', color: '#f56c6c', tip: '参与投票的模型数量' },
])

onMounted(async () => {
  await loadAll()
  await loadHistory()
  await loadDataSources()
  await loadVotes()
  await loadVoteStats()   // V6.8.1: 投票统计汇总
  await loadVoteTrend()   // V6.8.1: 投票趋势真实数据
  // NL2SQL 模型列表
  try {
    const r = await listEnabledModels()
    nlModelOptions.value = (r.data || []).map(m => ({
      value: m.code || m.model_code,
      label: m.displayName || m.name || m.code,
      local: !!(m.protocol === 'local' || (m.providerCode || '').startsWith('local-')),
    }))
  } catch { nlModelOptions.value = [] }
})

async function loadVotes() {
  try {
    const r = await getVoteRecords({ limit: 10 })
    // V6.8.1 fix: 后端返回 { records, total, page, size }，不是 list
    recentVotes.value = (r.data?.records || r.data?.list || r.data || []).map(v => ({
      id: v.id,
      text: v.text,
      user: 'U',
      model: v.models?.[0] || v.answer || '-',
      votes: v.totalVotes || 0,
      agreementRate: v.agreementRate || 0,
      modelVotes: v.modelVotes || [],
      createdAt: v.createdAt || '',
      strategy: v.strategy || '',
    }))
  } catch { /* 降级：显示空 */ }
}

// 重新投票 (Day 41)
// Day 43: 投票结束时如设置了通知邮箱则自动发送邮件
async function onRevote(vote) {
  if (!vote.id) {
    ElMessage.warning('投票记录 ID 无效')
    return
  }
  try {
    const r = await duplicateVote(vote.id)
    const data = r.data || {}
    const email = voteNotifyEmail.value?.trim()
    if (email) {
      // 如果填了通知邮箱，通过 POST /ai/voting 提交新投票（含邮件）
      await http.post('/api/v1/ai/voting', {
        text: data.text || '',
        strategy: data.strategy || 'majority',
        votes: (data.models || []).map(m => ({ model: m, answer: '', confidence: 0.5 })),
        final: '',
        notifyEmail: email
      }).catch(() => {/* 静默忽略 */})
    }
    ElMessage.success('已重新发起投票：' + (data.text || '使用相同问题') + (email ? '（已设置结果通知到 ' + email + '）' : ''))
    // 刷新投票列表
    await loadVotes()
    await loadVoteStats()
    await loadVoteTrend()
  } catch (e) {
    ElMessage.error('重新投票失败：' + (e.message || ''))
  }
}

// V6.8.1 fix: 投票统计汇总
async function loadVoteStats() {
  try {
    const r = await getVoteStatsSummary()
    const s = r.data || {}
    voteMetrics.value = [
      { label: '投票总数', value: s.totalVotes ?? '-', color: '#409eff', tip: '平台累计投票总次数' },
      { label: '一致率', value: s.avgAgreement != null ? (s.avgAgreement * 100).toFixed(1) + '%' : '-', color: '#67c23a', tip: '模型投票一致率均值' },
      { label: '共识率', value: s.consensusRate != null ? (s.consensusRate * 100).toFixed(1) + '%' : '-', color: '#e6a23c', tip: '达成共识的投票占比' },
      { label: '活跃模型', value: s.activeModels ?? '-', color: '#f56c6c', tip: '参与投票的模型数量' },
    ]
  } catch { /* 降级：显示空 */ }
}

// V6.8.1 fix: 投票趋势真实数据
async function loadVoteTrend() {
  try {
    const r = await getVoteTrend({ bucket: 'day' })
    const data = r.data || []
    if (!voteChart) voteChart = echarts.init(voteTrendRef.value)
    const dates = data.map(d => d.date || d.day || '')
    const votes = data.map(d => d.votes || 0)
    const agreements = data.map(d => d.agreement != null ? (d.agreement * 100).toFixed(1) : 0)
    voteChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['投票数', '一致率%'] },
      xAxis: { type: 'category', data: dates },
      yAxis: [
        { type: 'value', name: '投票数' },
        { type: 'value', name: '一致率%', max: 100, axisLabel: { formatter: '{value}%' } },
      ],
      series: [
        { name: '投票数', type: 'bar', data: votes, itemStyle: { color: '#409eff' } },
        { name: '一致率%', type: 'line', yAxisIndex: 1, data: agreements, smooth: true, itemStyle: { color: '#67c23a' } },
      ]
    })
  } catch { /* 降级：保留模板默认 */ }
}

onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  successChart?.dispose()
  voteChart?.dispose()
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.sql-code { background: #1e293b; color: #a5f3fc; padding: 12px; border-radius: 6px; font-size: 13px; overflow-x: auto; }
.nl-history-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px;
  background: #f5f7fa; transition: background 0.15s;
  &:hover { background: #ecf5ff; }
}
.nl-query-text { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.sample-query {
  padding: 8px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px;
  font-size: 13px; color: #409eff; background: #ecf5ff; transition: background 0.15s;
  &:hover { background: #dbeafe; }
}
.vote-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
</style>
