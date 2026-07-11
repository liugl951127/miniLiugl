<template>
  <div class="ai-tool-admin">
    <el-tabs v-model="activeTab">
      <!-- 工具列表 -->
      <el-tab-pane label="AI 工具" name="tools">
        <el-card>
          <template #header>
            <div class="header">
              <span>🤖 AI 工具配置中心</span>
              <div>
                <el-select v-model="filterCategory" placeholder="按分类筛选" clearable style="width: 180px; margin-right: 12px">
                  <el-option label="数据清洗" value="DATA_CLEAN" />
                  <el-option label="数据分析" value="DATA_ANALYZE" />
                  <el-option label="代码生成" value="CODE_GEN" />
                  <el-option label="SQL 查询" value="SQL_QUERY" />
                  <el-option label="对话聊天" value="CHAT" />
                </el-select>
                <el-button type="primary" @click="loadTools">🔄 刷新</el-button>
              </div>
            </div>
          </template>

          <el-table :data="tools" v-loading="loading" stripe>
            <el-table-column prop="code" label="编码" width="200" />
            <el-table-column prop="name" label="名称" width="180" />
            <el-table-column prop="category" label="分类" width="100">
              <template #default="scope">
                <el-tag :type="categoryTag(scope.row.category)">{{ categoryLabel(scope.row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="implType" label="实现" width="80">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.implType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="builtin" label="内置" width="60">
              <template #default="scope">
                <el-tag v-if="scope.row.builtin === 1" type="success" size="small">是</el-tag>
                <el-tag v-else type="info" size="small">否</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="启用" width="60">
              <template #default="scope">
                <el-switch v-model="scope.row.enabled" :active-value="1" :inactive-value="0" @change="toggleTool(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openInvoke(scope.row)">调用</el-button>
                <el-button size="small" type="primary" @click="openEdit(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" :disabled="scope.row.builtin === 1" @click="del(scope.row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 数据源管理 -->
      <el-tab-pane label="数据源" name="datasources">
        <el-card>
          <template #header>
            <div class="header">
              <span>🗄️ 数据源管理 (MySQL/PostgreSQL/Oracle/SQL Server/H2/ClickHouse)</span>
              <el-button type="primary" @click="openDsEdit()">➕ 新增数据源</el-button>
            </div>
          </template>

          <el-table :data="datasources" v-loading="dsLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" width="180" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="scope">
                <el-tag>{{ scope.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="jdbcUrl" label="JDBC URL" show-overflow-tooltip />
            <el-table-column prop="username" label="用户名" width="100" />
            <el-table-column prop="testStatus" label="状态" width="100">
              <template #default="scope">
                <el-tag v-if="scope.row.testStatus === 'OK'" type="success" size="small">✓ OK</el-tag>
                <el-tag v-else-if="scope.row.testStatus === 'FAILED'" type="danger" size="small">✗ 失败</el-tag>
                <el-tag v-else type="info" size="small">未测试</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="启用" width="60">
              <template #default="scope">
                <el-switch v-model="scope.row.enabled" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="testDs(scope.row)">测试连接</el-button>
                <el-button size="small" type="primary" @click="openDsEdit(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="delDs(scope.row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 项目代码生成 -->
      <el-tab-pane label="代码生成" name="codegen">
        <el-card>
          <template #header>
            <span>🚀 AI 项目代码生成 (Spring Boot / Vue / React / Python Flask / Node Express / HTML)</span>
          </template>
          <el-form :model="genForm" label-width="120px" style="max-width: 700px">
            <el-form-item label="项目类型">
              <el-select v-model="genForm.projectType">
                <el-option label="Spring Boot (Java)" value="spring-boot" />
                <el-option label="Vue 3" value="vue" />
                <el-option label="React" value="react" />
                <el-option label="Python Flask" value="python-flask" />
                <el-option label="Node Express" value="node-express" />
                <el-option label="HTML 静态" value="html" />
              </el-select>
            </el-form-item>
            <el-form-item label="项目名称">
              <el-input v-model="genForm.projectName" placeholder="my-awesome-app" />
            </el-form-item>
            <el-form-item label="项目描述">
              <el-input v-model="genForm.description" type="textarea" :rows="3" placeholder="用一段话描述项目要做什么" />
            </el-form-item>
            <el-form-item label="功能列表">
              <el-input v-model="genForm.features" placeholder="逗号分隔: list, create, redis, security" />
            </el-form-item>
            <el-form-item label="数据库" v-if="genForm.projectType === 'spring-boot'">
              <el-select v-model="genForm.database">
                <el-option label="H2 (内存)" value="h2" />
                <el-option label="MySQL" value="mysql" />
                <el-option label="PostgreSQL" value="postgresql" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="generate" :loading="genLoading">🚀 生成项目代码</el-button>
            </el-form-item>
          </el-form>

          <el-divider v-if="genResult" />

          <div v-if="genResult">
            <h3>📁 {{ genResult.projectName }} ({{ genResult.totalFiles }} 个文件, ~{{ genResult.totalLines }} 行)</h3>
            <p style="color: #666; font-size: 12px">耗时: {{ genResult.durationMs }}ms</p>
            <el-tabs>
              <el-tab-pane label="目录结构">
                <pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; overflow: auto; max-height: 400px">{{ genResult.structure }}</pre>
              </el-tab-pane>
              <el-tab-pane label="文件内容">
                <el-select v-model="selectedFile" placeholder="选择文件" style="width: 300px; margin-bottom: 8px">
                  <el-option v-for="(content, path) in genResult.files" :key="path" :label="path" :value="path" />
                </el-select>
                <pre v-if="selectedFile" style="background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 4px; overflow: auto; max-height: 500px; font-size: 12px">{{ genResult.files[selectedFile] }}</pre>
              </el-tab-pane>
              <el-tab-pane label="启动说明">
                <pre style="background: #f0f8ff; padding: 12px; border-radius: 4px">{{ genResult.runInstructions }}</pre>
                <el-button type="success" @click="downloadProject" style="margin-top: 12px">📥 下载项目 (zip)</el-button>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 数据分析 -->
      <el-tab-pane label="数据分析" name="analysis">
        <el-card>
          <template #header>
            <span>📊 数据智能分析 (描述统计 / 异常检测 / 趋势 / 分布)</span>
          </template>
          <el-form :model="analysisForm" label-width="120px" style="max-width: 600px">
            <el-form-item label="数据源">
              <el-select v-model="analysisForm.dataSourceId" placeholder="选择数据源">
                <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="表名">
              <el-input v-model="analysisForm.table" placeholder="user / order / log..." />
            </el-form-item>
            <el-form-item label="分析类型">
              <el-select v-model="analysisForm.tool">
                <el-option label="描述统计 (count/mean/std/分位数)" value="data.analyze.stats" />
                <el-option label="异常检测 (Z-Score / IQR)" value="data.analyze.anomaly" />
                <el-option label="趋势分析 (时间序列)" value="data.analyze.trend" />
              </el-select>
            </el-form-item>
            <el-form-item label="列名">
              <el-input v-model="analysisForm.column" placeholder="amount / age / price..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="runAnalysis" :loading="analysisLoading">🚀 启动分析</el-button>
            </el-form-item>
          </el-form>

          <div v-if="analysisResult" style="margin-top: 24px">
            <h3>📈 分析结果</h3>
            <el-table :data="analysisTable" border>
              <el-table-column v-for="(value, key) in analysisResult" :key="key" :label="key">
                <template #default="scope">
                  <code>{{ formatVal(scope.row[key]) }}</code>
                </template>
              </el-table-column>
            </el-table>
            <el-button @click="downloadAnalysis" type="success" style="margin-top: 12px">📥 下载结果 (JSON)</el-button>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 工具调用弹窗 -->
    <el-dialog v-model="invokeVisible" :title="`调用工具: ${currentTool?.name}`" width="700px">
      <el-form :model="invokeForm" label-width="150px">
        <el-form-item label="数据源 ID" v-if="needsDs(currentTool)">
          <el-input-number v-model="invokeForm.dataSourceId" :min="1" />
        </el-form-item>
        <el-form-item label="表名" v-if="needsTable(currentTool)">
          <el-input v-model="invokeForm.table" />
        </el-form-item>
        <el-form-item label="列名" v-if="needsColumn(currentTool)">
          <el-input v-model="invokeForm.column" />
        </el-form-item>
        <el-form-item label="输入参数 (JSON)">
          <el-input v-model="invokeForm.json" type="textarea" :rows="6" placeholder='{"key": "value"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invokeVisible = false">取消</el-button>
        <el-button type="primary" :loading="invokeLoading" @click="doInvoke">🚀 调用</el-button>
      </template>
    </el-dialog>

    <!-- 工具编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑工具' : '新增工具'" width="700px">
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="编码"><el-input v-model="editForm.code" :disabled="!!editForm.builtin" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category">
            <el-option label="数据清洗" value="DATA_CLEAN" />
            <el-option label="数据分析" value="DATA_ANALYZE" />
            <el-option label="代码生成" value="CODE_GEN" />
            <el-option label="SQL 查询" value="SQL_QUERY" />
            <el-option label="对话聊天" value="CHAT" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实现方式">
          <el-select v-model="editForm.implType">
            <el-option label="Java 类" value="java" />
            <el-option label="SQL" value="sql" />
            <el-option label="Prompt" value="prompt" />
            <el-option label="HTTP" value="http" />
          </el-select>
        </el-form-item>
        <el-form-item label="实现值"><el-input v-model="editForm.implValue" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="限流 (次/分钟)"><el-input-number v-model="editForm.rateLimit" :min="0" :max="10000" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="editForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTool">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据源编辑弹窗 -->
    <el-dialog v-model="dsEditVisible" :title="dsEdit.id ? '编辑数据源' : '新增数据源'" width="600px">
      <el-form :model="dsEdit" label-width="120px">
        <el-form-item label="名称"><el-input v-model="dsEdit.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="dsEdit.type">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
            <el-option label="H2" value="h2" />
            <el-option label="ClickHouse" value="clickhouse" />
            <el-option label="Doris" value="doris" />
          </el-select>
        </el-form-item>
        <el-form-item label="JDBC URL"><el-input v-model="dsEdit.jdbcUrl" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="dsEdit.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="dsEdit.password" type="password" show-password /></el-form-item>
        <el-form-item label="连接池大小"><el-input-number v-model="dsEdit.poolSize" :min="1" :max="50" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="dsEdit.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dsEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDs">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listTools as apiListTools,
  getTool as apiGetTool,
  createTool as apiCreateTool,
  updateTool as apiUpdateTool,
  deleteTool as apiDeleteTool,
  invokeTool as apiInvokeTool,
  listDataSources as apiListDataSources,
  createDataSource as apiCreateDataSource,
  updateDataSource as apiUpdateDataSource,
  deleteDataSource as apiDeleteDataSource,
  testDataSource as apiTestDataSource,
  generateProject as apiGenerateProject
} from '@/api/ai'

const activeTab = ref('tools')

// 工具列表
const tools = ref([])
const loading = ref(false)
const filterCategory = ref(null)
async function loadTools() {
  loading.value = true
  try {
    const res = await apiListTools({ category: filterCategory.value })
    tools.value = (res.data || []).filter(t => !filterCategory.value || t.category === filterCategory.value)
  } catch (e) {
    ElMessage.error('加载工具失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function categoryLabel(c) {
  return { DATA_CLEAN: '数据清洗', DATA_ANALYZE: '数据分析', CODE_GEN: '代码生成', SQL_QUERY: 'SQL查询', CHAT: '对话', CUSTOM: '自定义' }[c] || c
}
function categoryTag(c) {
  return { DATA_CLEAN: 'success', DATA_ANALYZE: 'warning', CODE_GEN: 'primary', SQL_QUERY: 'info', CHAT: '', CUSTOM: '' }[c] || ''
}

// 工具调用
const invokeVisible = ref(false)
const invokeLoading = ref(false)
const currentTool = ref(null)
const invokeForm = ref({ dataSourceId: 1, table: '', column: '', json: '{}' })

function needsDs(t) {
  if (!t) return false
  if (t.category === 'CHAT' || t.category === 'CUSTOM') return false
  return true
}
function needsTable(t) {
  if (!t) return false
  return needsDs(t) && t.code !== 'code.gen.from-schema'
}
function needsColumn(t) {
  if (!t) return false
  const code = t.code || ''
  return code.includes('stats') || code.includes('anomaly') || code.includes('clean') || code.includes('distribution')
}
function needsLimit(t) {
  if (!t) return false
  const code = t.code || ''
  return code.includes('analyze') || code.includes('clean') || code.includes('deduplicate')
}

function openInvoke(t) {
  currentTool.value = t
  invokeForm.value = {
    dataSourceId: 1, table: '', column: '', buckets: 10, limit: 10000,
    message: '', sessionId: '', question: '', projectName: '', basePackage: 'com.example',
    json: '{}'
  }
  invokeVisible.value = true
}

async function doInvoke() {
  invokeLoading.value = true
  try {
    let input = {}
    try { input = JSON.parse(invokeForm.value.json || '{}') } catch {}
    const t = currentTool.value
    if (needsDs(t)) {
      input.dataSourceId = invokeForm.value.dataSourceId
      if (needsTable(t) && invokeForm.value.table) input.table = invokeForm.value.table
      if (needsColumn(t) && invokeForm.value.column) input.column = invokeForm.value.column
      if (t.code === 'data.analyze.distribution' && invokeForm.value.buckets) input.buckets = invokeForm.value.buckets
      if (needsLimit(t) && invokeForm.value.limit) input.limit = invokeForm.value.limit
    } else if (t.code === 'chat.assistant') {
      if (invokeForm.value.message) input.message = invokeForm.value.message
      if (invokeForm.value.sessionId) input.sessionId = invokeForm.value.sessionId
    } else if (t.code === 'sql.query') {
      if (invokeForm.value.dataSourceId) input.dataSourceId = invokeForm.value.dataSourceId
      if (invokeForm.value.question) input.question = invokeForm.value.question
    } else if (t.code === 'code.gen.from-schema') {
      if (invokeForm.value.dataSourceId) input.dataSourceId = invokeForm.value.dataSourceId
      if (invokeForm.value.table) input.table = invokeForm.value.table
      if (invokeForm.value.projectName) input.projectName = invokeForm.value.projectName
      if (invokeForm.value.basePackage) input.basePackage = invokeForm.value.basePackage
    }
    const res = await apiInvokeTool(t.code, input)
    if (res.data && res.data.success) {
      ElMessage.success(`调用成功 (${res.data.durationMs || 0}ms)`)
      let body = res.data.data || res.data
      if (body && body.zipBase64) {
        try {
          const bin = atob(body.zipBase64)
          const bytes = new Uint8Array(bin.length)
          for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
          const blob = new Blob([bytes], { type: 'application/zip' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = (body.projectName || 'project') + '.zip'
          a.click()
          URL.revokeObjectURL(url)
        } catch (e) { /* noop */ }
      }
      ElMessageBox.alert(JSON.stringify(body, null, 2), '结果', { type: 'success' })
      invokeVisible.value = false
    } else {
      ElMessage.error('调用失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (e) {
    ElMessage.error('调用失败: ' + (e.message || e))
  } finally {
    invokeLoading.value = false
  }
}

// 工具编辑
const editVisible = ref(false)
const editForm = ref({})
function openEdit(t) {
  editForm.value = { ...t }
  editVisible.value = true
}
async function saveTool() {
  try {
    if (editForm.value.id) {
      await apiUpdateTool(editForm.value.id, editForm.value)
    } else {
      await apiCreateTool(editForm.value)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    loadTools()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}
async function toggleTool(t) {
  try {
    await apiUpdateTool(t.id, t)
    ElMessage.success('已更新')
  } catch (e) {
    ElMessage.error('更新失败: ' + e.message)
  }
}
async function del(t) {
  await ElMessageBox.confirm(`确定删除工具 ${t.name}?`, '确认', { type: 'warning' })
  try {
    await apiDeleteTool(t.id)
    ElMessage.success('已删除')
    loadTools()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

// 数据源管理
const datasources = ref([])
const dsLoading = ref(false)
async function loadDatasources() {
  dsLoading.value = true
  try {
    const res = await apiListDataSources()
    datasources.value = res.data.list
  } catch (e) {
    ElMessage.error('加载数据源失败: ' + e.message)
  } finally {
    dsLoading.value = false
  }
}
async function testDs(ds) {
  try {
    const res = await apiTestDataSource(ds.id)
    if (res.data.success) {
      ElMessage.success('连接成功: ' + res.data.message)
    } else {
      ElMessage.error('连接失败: ' + res.data.message)
    }
    loadDatasources()
  } catch (e) {
    ElMessage.error('测试失败: ' + e.message)
  }
}
const dsEditVisible = ref(false)
const dsEdit = ref({})
function openDsEdit(ds) {
  dsEdit.value = ds ? { ...ds } : { name: '', type: 'mysql', jdbcUrl: '', username: '', password: '', poolSize: 10, description: '' }
  dsEditVisible.value = true
}
async function saveDs() {
  try {
    if (dsEdit.value.id) {
      await apiUpdateDataSource(dsEdit.value.id, dsEdit.value)
    } else {
      await apiCreateDataSource(dsEdit.value)
    }
    ElMessage.success('保存成功')
    dsEditVisible.value = false
    loadDatasources()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}
async function delDs(ds) {
  await ElMessageBox.confirm(`确定删除数据源 ${ds.name}?`, '确认', { type: 'warning' })
  await apiDeleteDataSource(ds.id)
  ElMessage.success('已删除')
  loadDatasources()
}

// 代码生成
const genForm = ref({ projectType: 'spring-boot', projectName: 'my-app', description: '', features: 'list, create', database: 'h2' })
const genLoading = ref(false)
const genResult = ref(null)
const selectedFile = ref(null)
async function generate() {
  genLoading.value = true
  try {
    const res = await apiGenerateProject(genForm.value)
    genResult.value = res.data
    selectedFile.value = res.data.keyFiles[0] || Object.keys(res.data.files)[0]
    ElMessage.success(`生成 ${res.data.totalFiles} 个文件`)
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  } finally {
    genLoading.value = false
  }
}
function downloadProject() {
  // 简单 zip 打包
  const blob = new Blob([JSON.stringify(genResult.value.files, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = genResult.value.projectName + '-files.json'
  a.click()
}

// 数据分析
const analysisForm = ref({ dataSourceId: null, table: '', tool: 'data.analyze.stats', column: '' })
const analysisLoading = ref(false)
const analysisResult = ref(null)
async function runAnalysis() {
  if (!analysisForm.value.dataSourceId || !analysisForm.value.table || !analysisForm.value.column) {
    ElMessage.warning('请填写完整')
    return
  }
  analysisLoading.value = true
  try {
    const res = await apiInvokeTool(analysisForm.value.tool, {
      dataSourceId: analysisForm.value.dataSourceId,
      table: analysisForm.value.table,
      column: analysisForm.value.column
    })
    if (res.data.success) {
      analysisResult.value = res.data.data
      ElMessage.success(`分析完成 (${res.data.durationMs}ms)`)
    } else {
      ElMessage.error('分析失败: ' + res.data.message)
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  } finally {
    analysisLoading.value = false
  }
}
const analysisTable = computed(() => {
  if (!analysisResult.value) return []
  return [analysisResult.value]
})
function formatVal(v) {
  if (typeof v === 'number') return v.toFixed(4)
  return JSON.stringify(v)
}
function downloadAnalysis() {
  const blob = new Blob([JSON.stringify(analysisResult.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'analysis.json'
  a.click()
}

onMounted(() => {
  loadTools()
  loadDatasources()
})
</script>

<style scoped>
.ai-tool-admin { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>