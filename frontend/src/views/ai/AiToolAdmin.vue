<!--
  @file views/ai/AiToolAdmin.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/AiToolAdmin.vue (AI 工具 (AiToolAdmin))
  @version V3.5.12+ (前端注释补全)
  @description AI 工具 (AiToolAdmin)
-->
<template>
  <div class="page-ai-tool-admin">
    <!-- 1. page-header -->
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('tool.title') }}</h2>
        <p class="page-subtitle">工具列表 · 分类 · 状态 · 调用统计</p>
      </div>
      <el-button :icon="Refresh" @click="loadTools" :loading="loading">刷新</el-button>
    </header>

    <!-- 2. section: 过滤器 (分类 + 状态) -->
    <section class="section">
      <el-card shadow="hover">
        <el-row :gutter="16" align="middle">
          <el-col :xs="24" :sm="8">
            <el-select v-model="filterCategory" placeholder="按分类筛选" clearable style="width: 100%">
              <el-option label="数据清洗" value="DATA_CLEAN" />
              <el-option label="数据分析" value="DATA_ANALYZE" />
              <el-option label="代码生成" value="CODE_GEN" />
              <el-option label="SQL 查询" value="SQL_QUERY" />
              <el-option label="对话聊天" value="CHAT" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-input v-model="search" placeholder="搜索工具名..." clearable :prefix-icon="Search" />
          </el-col>
        </el-row>
      </el-card>
    </section>

    <!-- 3. section: 4 KPI -->
    <section class="section">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="工具总数" :value="tools.length" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="已启用" :value="enabledCount" :value-style="{ color: '#10b981' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="总调用" :value="totalCalls" :value-style="{ color: '#6366f1' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="成功率" :value="successRate" suffix="%" :value-style="{ color: '#a855f7' }" /></el-card></el-col>
      </el-row>
    </section>

    <!-- 4. section: 工具列表 (表格) -->
    <section class="section">
      <h3 class="section-title">🛠️ 工具列表 ({{ filteredTools.length }})</h3>
      <el-card shadow="hover">
        <el-table :data="filteredTools" stripe>
          <el-table-column prop="code" label="代码" width="140">
            <template #default="{ row }">
              <el-tag size="small">{{ row.code }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag :type="categoryColor(row.category)" size="small">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-switch v-model="row.status" active-value="ENABLED" inactive-value="DISABLED" @change="toggleTool(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="callCount" label="调用次数" width="100" sortable />
          <el-table-column prop="successRate" label="成功率" width="100">
            <template #default="{ row }">
              <el-progress :percentage="row.successRate" :stroke-width="6" />
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="editTool(row)">编辑</el-button>
              <el-button size="small" type="primary" @click="testTool(row)">测试</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!filteredTools.length" description="暂无工具" />
      </el-card>
    </section>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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