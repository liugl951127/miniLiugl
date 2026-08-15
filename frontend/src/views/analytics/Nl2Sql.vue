<template>
  <div class="nl2sql-page">
    <!-- ====== 顶部工具栏 ====== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="title">💬 NL2SQL 实验室</span>
        <!-- 当前连接状态 -->
        <el-tag v-if="currentDs" :type="dsConnected ? 'success' : 'danger'" size="small">
          {{ dsConnected ? '🟢 已连接' : '🔴 未连接' }}: {{ currentDs.name }}
        </el-tag>
        <el-tag v-else type="info" size="small">未选择数据源</el-tag>
        <!-- 快速切换数据源下拉 (V7.3) -->
        <el-select
          v-if="dataSources.length"
          v-model="selectedDsId"
          size="small"
          placeholder="选择数据源"
          style="width:180px;margin-left:8px"
          @change="onDsQuickSelect"
          clearable
          @clear="currentDs = null; schemaTree = []"
        >
          <el-option
            v-for="ds in dataSources"
            :key="ds.id"
            :label="ds.name + ' · ' + ds.type"
            :value="ds.id"
          >
            <span>{{ ds.name }}</span>
            <el-tag size="small" type="info" style="float:right;font-size:11px">{{ ds.type }}</el-tag>
          </el-option>
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="showDsDialog = true">
          {{ currentDs ? '切换数据源' : '+ 链接数据库' }}
        </el-button>
      </div>
    </div>

    <div class="main-layout">
      <!-- ====== 左侧: Schema 树 ====== -->
      <div class="schema-panel" v-loading="schemaLoading">
        <div v-if="!currentDs" class="no-ds-hint">
          <p>👈 点击左侧"链接数据库"开始</p>
        </div>
        <div v-else>
          <div class="panel-header">
            <span>📦 数据结构</span>
            <el-button text size="small" @click="refreshSchema">🔄</el-button>
          </div>
          <!-- 数据库列表 -->
          <el-tree
            :data="schemaTree"
            :props="{ label: 'label', children: 'children' }"
            node-key="key"
            default-expand-all
            @node-click="onTreeNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span>{{ node.label }}</span>
                <el-tag v-if="data.count" size="small" type="info" style="margin-left:4px">{{ data.count }}</el-tag>
              </span>
            </template>
          </el-tree>
          <!-- 表详情面板 (点击表后展开) -->
          <div v-if="selectedTable" class="table-detail">
            <div class="detail-header">
              <span>📋 {{ selectedTable }}</span>
              <el-button text size="small" @click="selectedTable = null">✕</el-button>
            </div>

            <!-- 表统计信息 -->
            <div v-if="tableInfo" class="table-stats">
              <el-tag size="small" type="info">🗄️ {{ tableInfo.rowCount?.toLocaleString() ?? '?' }} 行</el-tag>
              <el-tag size="small" type="info" v-if="tableInfo.engine">{{ tableInfo.engine }}</el-tag>
              <el-tag size="small" type="info" v-if="tableInfo.dataSize">{{ formatSize(tableInfo.dataSize) }}</el-tag>
              <el-tag size="small" type="info" v-if="tableInfo.createTime">{{ tableInfo.createTime }}</el-tag>
            </div>

            <!-- 查看表数据按钮 -->
            <div style="margin: 6px 0">
              <el-button type="primary" plain size="small" @click="viewTableData" :loading="loadingTableData">
                📊 查看表数据
              </el-button>
              <el-button size="small" plain @click="viewTableDDL" v-if="tableInfo?.ddl">
                📄 DDL
              </el-button>
            </div>

            <!-- 表列结构 -->
            <div style="font-size:12px;color:#666;margin-bottom:4px;font-weight:600">字段结构 ({{ tableColumns.length }} 列)</div>
            <el-table :data="tableColumns" stripe size="small" max-height="160">
              <el-table-column prop="name" label="列名" width="130" />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="nullable" label="可空" width="55">
                <template #default="{ row }">
                  <span :style="{ color: row.nullable ? '#67c23a' : '#f56c6c' }">{{ row.nullable ? 'YES' : 'NO' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="keyType" label="键" width="50">
                <template #default="{ row }">
                  <span v-if="row.keyType" style="color:#e6a23c;font-size:11px">{{ row.keyType }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" show-overflow-tooltip />
            </el-table>

            <!-- 表数据 (真实数据) -->
            <div v-if="tableDataResult" class="table-data-result">
              <div class="result-header">
                <span>📊 {{ selectedTable }} 数据 (共 {{ tableDataResult.rowCount?.toLocaleString() ?? 0 }} 行, 截取前 {{ tableDataResult.rows?.length }} 条)</span>
                <el-button text size="small" @click="tableDataResult = null">✕</el-button>
              </div>
              <el-table :data="tableDataResult.rows" stripe size="small" max-height="280" border>
                <el-table-column
                  v-for="col in tableDataResult.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                  min-width="110"
                  show-overflow-tooltip
                />
              </el-table>
            </div>

            <!-- DDL 展示 -->
            <div v-if="showDdl" class="table-ddl">
              <div class="result-header">
                <span>📄 DDL 语句</span>
                <el-button text size="small" @click="showDdl = false">✕</el-button>
              </div>
              <pre class="sql-block" style="max-height:200px">{{ tableInfo.ddl }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== 右侧: 查询区 ====== -->
      <div class="query-panel">
        <!-- NL 查询框 -->
        <div class="query-box">
          <div class="query-hint">
            用自然语言描述你想查什么，我帮你生成 SQL
            <span v-if="selectedTable" style="color:#409eff">（已选表: {{ selectedTable }}）</span>
          </div>
          <div class="query-input-row">
            <el-input
              v-model="question"
              type="textarea"
              :rows="3"
              :disabled="!currentDs"
              :placeholder="currentDs ? '例: 统计最近7天每天的新增用户数' : '请先链接数据库'"
              @keydown.ctrl.enter="ask"
              @keydown.meta.enter="ask"
            />
            <el-button type="primary" :loading="asking" @click="ask" :disabled="!currentDs || !question.trim()">
              🎯 生成 SQL
            </el-button>
          </div>
        </div>

        <!-- 生成结果区 -->
        <div v-if="result" class="result-area">
          <!-- SQL 展示 -->
          <div class="result-block">
            <div class="result-header">
              <span>🔧 生成的 SQL</span>
              <div>
                <el-button size="small" @click="explain" :disabled="!result.sql">解释</el-button>
                <el-button size="small" type="success" @click="runQuery" :loading="running">
                  ▶ 执行查询
                </el-button>
                <el-button size="small" type="warning" @click="dryRun" :disabled="!result.sql">
                  ⚡ 试运行
                </el-button>
              </div>
            </div>
            <pre class="sql-block">{{ result.sql }}</pre>
          </div>

          <!-- 解释 -->
          <div v-if="result.explanation" class="result-block">
            <div class="result-header"><span>📖 说明</span></div>
            <p style="margin:0;font-size:13px;color:#555">{{ result.explanation }}</p>
          </div>

          <!-- 执行结果表格 -->
          <div v-if="queryResult" class="result-block">
            <div class="result-header">
              <span>📊 查询结果</span>
              <span style="font-size:12px;color:#888">共 {{ queryResult.rowCount ?? 0 }} 行, 耗时 {{ queryResult.durationMs }}ms</span>
            </div>
            <el-table :data="queryResult.rows" stripe size="small" max-height="400" border>
              <el-table-column
                v-for="col in queryResult.columns"
                :key="col"
                :prop="col"
                :label="col"
                min-width="120"
                show-overflow-tooltip
              />
            </el-table>
          </div>

          <!-- 试运行结果 -->
          <div v-if="dryRunResult && !queryResult" class="result-block">
            <div class="result-header">
              <span>⚡ 试运行结果</span>
              <span style="font-size:12px;color:#888">共 {{ dryRunResult.rowCount ?? 0 }} 行</span>
            </div>
            <el-table :data="dryRunResult.rows" stripe size="small" max-height="300" border>
              <el-table-column
                v-for="col in dryRunResult.columns"
                :key="col"
                :prop="col"
                :label="col"
                min-width="120"
                show-overflow-tooltip
              />
            </el-table>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-else-if="currentDs" description="输入自然语言问题，点击生成 SQL" />

        <!-- 历史记录 -->
        <div v-if="history.length" class="history-area">
          <el-divider content-position="left"><span style="font-size:13px">📜 历史记录</span></el-divider>
          <div v-for="h in history" :key="h.id" class="history-item" @click="loadHistoryFromItem(h)">
            <div class="hist-q">{{ h.question }}</div>
            <pre class="hist-sql">{{ h.sql }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 链接数据库对话框 ====== -->
    <el-dialog v-model="showDsDialog" title="🔗 链接数据库" width="560px" destroy-on-close>
      <!-- 数据源列表 -->
      <div v-if="!showDsForm">
        <div style="margin-bottom:12px">
          <el-button type="primary" size="small" @click="showDsForm = true">+ 新建数据源</el-button>
        </div>
        <el-table :data="dataSources" stripe size="small">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" width="80" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="selectDs(scope.row)">连接</el-button>
              <el-button size="small" type="primary" plain @click="editDs(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="deleteDs(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!dataSources.length" description="暂无数据源，点击上方新建" />
      </div>

      <!-- 新建/编辑表单 -->
      <el-form v-else :model="dsForm" label-width="100px" style="padding-right:8px">
        <el-form-item label="名称" required>
          <el-input v-model="dsForm.name" placeholder="如: 生产 MySQL" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="dsForm.type" style="width:100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="H2 内存" value="h2" />
            <el-option label="PostgreSQL" value="postgresql" />
          </el-select>
        </el-form-item>
        <el-form-item label="JDBC URL" required>
          <el-input
            v-model="dsForm.jdbcUrl"
            placeholder="jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC"
            type="textarea"
            :rows="2"
          />
          <div style="color:#999;font-size:11px;margin-top:2px">
            <span v-if="dsForm.type === 'mysql'">
              格式: jdbc:mysql://host:port/dbname?useSSL=false&amp;serverTimezone=UTC
            </span>
            <span v-if="dsForm.type === 'h2'">格式: jdbc:h2:mem:testdb 或 jdbc:h2:./data/testdb</span>
            <span v-if="dsForm.type === 'postgresql'">格式: jdbc:postgresql://host:port/dbname</span>
          </div>
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="dsForm.username" placeholder="root" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="dsForm.password" type="password" show-password placeholder="********" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dsForm.description" placeholder="可选备注" />
        </el-form-item>

        <!-- 连接测试 -->
        <el-form-item>
          <el-button @click="testDs" :loading="testingDs" :type="dsTestOk === true ? 'success' : dsTestOk === false ? 'danger' : 'info'" plain>
            {{ dsTestOk === true ? '✅ 连接成功' : dsTestOk === false ? '❌ 连接失败' : '🔍 测试连接' }}
          </el-button>
          <span v-if="dsTestMsg" style="margin-left:8px;font-size:12px" :style="{ color: dsTestOk ? '#67c23a' : '#f56c6c' }">{{ dsTestMsg }}</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveDs" :loading="savingDs">保存数据源</el-button>
          <el-button @click="cancelDsForm">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listDataSources, createDataSource, updateDataSource, deleteDataSource, testDataSource,
  listDatabases, listTables, describeTable,
  nl2sqlAsk, nl2sqlExplain, nl2sqlHistory,
  executeQuery, dryRunQuery
} from '@/api/analytics'

// ── 状态 ──────────────────────────────────────────────
const currentDs = ref(null)
const dsConnected = ref(false)
const showDsDialog = ref(false)
const showDsForm = ref(false)
const dataSources = ref([])
const selectedDsId = ref(null)  // 工具栏快速下拉的值

// ── 快速切换数据源 (工具栏下拉, V7.3) ─────────────────
async function onDsQuickSelect(dsId) {
  if (!dsId) {
    currentDs.value = null
    schemaTree.value = []
    return
  }
  const ds = dataSources.value.find(d => d.id === dsId)
  if (ds) await selectDs(ds)
}
const schemaLoading = ref(false)
const schemaTree = ref([])
const selectedTable = ref(null)
const selectedDb = ref(null)
const tableColumns = ref([])
const tableSample = ref([])
const tableSampleColumns = ref([])
const tableInfo = ref(null)
const loadingTableData = ref(false)
const tableDataResult = ref(null)
const showDdl = ref(false)

const dsForm = reactive({ id: null, name: '', type: 'mysql', jdbcUrl: '', username: '', password: '', description: '' })
const testingDs = ref(false)
const savingDs = ref(false)
const dsTestOk = ref(null)
const dsTestMsg = ref('')

const question = ref('')
const asking = ref(false)
const running = ref(false)
const result = ref(null)
const queryResult = ref(null)
const dryRunResult = ref(null)
const explanation = ref('')
const history = ref([])

// ── 生命周期 ──────────────────────────────────────────
onMounted(async () => {
  await loadDataSources()
  await fetchHistory()
})

// ── 数据源 ────────────────────────────────────────────
async function loadDataSources() {
  try {
    const res = await listDataSources()
    dataSources.value = res.data || []
    // 如果有已选中的，保持选中
    if (currentDs.value) {
      currentDs.value = dataSources.value.find(d => d.id === currentDs.value.id) || null
      if (currentDs.value) {
        selectedDsId.value = currentDs.value.id  // 同步下拉
        await loadSchema()
      }
    }
  } catch {}
}

function editDs(ds) {
  dsForm.id = ds.id
  dsForm.name = ds.name
  dsForm.type = ds.type
  dsForm.jdbcUrl = ds.jdbcUrl || ''
  dsForm.username = ds.username || ''
  dsForm.password = ds.password || ''
  dsForm.description = ds.description || ''
  dsTestOk.value = null
  dsTestMsg.value = ''
  showDsForm.value = true
}

function cancelDsForm() {
  showDsForm.value = false
  dsForm.id = null
  dsForm.name = ''
  dsForm.jdbcUrl = ''
  dsForm.username = ''
  dsForm.password = ''
  dsForm.description = ''
  dsTestOk.value = null
  dsTestMsg.value = ''
}

async function testDs() {
  testingDs.value = true
  dsTestOk.value = null
  dsTestMsg.value = ''
  try {
    await testDataSource({ ...dsForm })
    dsTestOk.value = true
    dsTestMsg.value = '连接成功！'
  } catch (e) {
    dsTestOk.value = false
    dsTestMsg.value = e?.response?.data?.message || e?.message || '连接失败，请检查配置'
  } finally {
    testingDs.value = false
  }
}

async function saveDs() {
  if (!dsForm.name || !dsForm.jdbcUrl || !dsForm.username) {
    ElMessage.warning('请填写必填项')
    return
  }
  savingDs.value = true
  try {
    if (dsForm.id) {
      await updateDataSource(dsForm.id, { ...dsForm })
      ElMessage.success('更新成功')
    } else {
      await createDataSource({ ...dsForm })
      ElMessage.success('创建成功')
    }
    await loadDataSources()
    cancelDsForm()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    savingDs.value = false
  }
}

async function selectDs(ds) {
  currentDs.value = ds
  selectedDsId.value = ds.id  // 同步工具栏下拉
  dsConnected.value = true
  showDsDialog.value = false
  selectedTable.value = null
  tableInfo.value = null
  tableDataResult.value = null
  showDdl.value = false
  await loadSchema()
  ElMessage.success(`已连接到: ${ds.name}`)
}

async function deleteDs(ds) {
  await ElMessageBox.confirm(`确定删除数据源 "${ds.name}"?`, '确认删除')
  try {
    await deleteDataSource(ds.id)
    ElMessage.success('已删除')
    if (currentDs.value?.id === ds.id) {
      currentDs.value = null
      dsConnected.value = false
      schemaTree.value = []
    }
    await loadDataSources()
  } catch {}
}

// ── Schema 加载 ────────────────────────────────────────
async function loadSchema() {
  if (!currentDs.value) return
  schemaLoading.value = true
  schemaTree.value = []
  try {
    const dbs = await listDatabases(currentDs.value.id)
    const dbList = dbs.data || []
    schemaTree.value = await Promise.all(
      dbList.map(async db => {
        try {
          const tables = await listTables(currentDs.value.id, db)
          const tableList = tables.data || []
          return {
            label: db,
            key: `db:${db}`,
            children: tableList.map(t => ({
              label: t.name || t,
              key: `table:${db}:${t.name || t}`,
              count: t.rowCount || null
            }))
          }
        } catch {
          return { label: db, key: `db:${db}`, children: [] }
        }
      })
    )
  } catch (e) {
    ElMessage.error('加载结构失败: ' + (e?.message || ''))
  } finally {
    schemaLoading.value = false
  }
}

async function refreshSchema() {
  selectedTable.value = null
  selectedDb.value = null
  tableInfo.value = null
  tableDataResult.value = null
  showDdl.value = false
  await loadSchema()
}

async function onTreeNodeClick(data) {
  if (!data.key.startsWith('table:')) return
  const parts = data.key.split(':')
  const db = parts[1]
  const table = parts[2]
  selectedDb.value = db
  selectedTable.value = table
  tableColumns.value = []
  tableSample.value = []
  tableSampleColumns.value = []
  tableInfo.value = null
  tableDataResult.value = null
  showDdl.value = false
  try {
    const info = await describeTable(currentDs.value.id, db, table)
    tableInfo.value = info.data
    tableColumns.value = tableInfo.value.columns || []
    tableSample.value = tableInfo.value.sample || []
    tableSampleColumns.value = tableSample.value.length ? Object.keys(tableSample.value[0]) : []
  } catch (e) {
    ElMessage.error('加载表详情失败: ' + (e?.message || ''))
  }
}

/** 查看表真实数据 (SELECT * LIMIT 200) */
async function viewTableData() {
  if (!currentDs.value || !selectedTable.value) return
  loadingTableData.value = true
  tableDataResult.value = null
  showDdl.value = false
  try {
    const sql = `SELECT * FROM \`${selectedTable.value}\` LIMIT 200`
    const res = await executeQuery({ sql, dataSourceId: currentDs.value.id })
    tableDataResult.value = res.data
  } catch (e) {
    ElMessage.error('加载数据失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    loadingTableData.value = false
  }
}

/** 查看 DDL */
function viewTableDDL() {
  showDdl.value = !showDdl.value
  tableDataResult.value = null
}

/** 格式化字节大小 */
function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// ── NL2SQL ────────────────────────────────────────────
async function ask() {
  if (!question.value.trim()) return
  asking.value = true
  result.value = null
  queryResult.value = null
  dryRunResult.value = null
  explanation.value = ''
  try {
    const res = await nl2sqlAsk({
      question: question.value,
      dataSourceId: currentDs.value?.id,
      tableHint: selectedTable.value || null
    })
    result.value = res.data
    // 自动把已选表名加到问题里传给 NL2SQL
    await fetchHistory()
  } catch (e) {
    ElMessage.error('生成 SQL 失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    asking.value = false
  }
}

async function explain() {
  if (!result.value?.sql) return
  try {
    const res = await nl2sqlExplain(result.value.sql)
    result.value = { ...result.value, explanation: res.data }
  } catch (e) {
    ElMessage.error('解释失败: ' + (e?.message || ''))
  }
}

async function dryRun() {
  if (!result.value?.sql) return
  running.value = true
  dryRunResult.value = null
  queryResult.value = null
  try {
    const res = await dryRunQuery({
      sql: result.value.sql,
      dataSourceId: currentDs.value?.id
    })
    dryRunResult.value = res.data
  } catch (e) {
    ElMessage.error('试运行失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    running.value = false
  }
}

async function runQuery() {
  if (!result.value?.sql) return
  running.value = true
  queryResult.value = null
  dryRunResult.value = null
  try {
    const res = await executeQuery({
      sql: result.value.sql,
      dataSourceId: currentDs.value?.id
    })
    queryResult.value = res.data
  } catch (e) {
    ElMessage.error('执行失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    running.value = false
  }
}

async function fetchHistory() {
  try {
    const res = await nl2sqlHistory({ page: 1, size: 20 })
    history.value = res.data || []
  } catch {}
}

function loadHistoryFromItem(item) {
  question.value = item.question
  result.value = { sql: item.sql, explanation: '' }
  queryResult.value = null
  dryRunResult.value = null
}
</script>

<style scoped>
.nl2sql-page { display: flex; flex-direction: column; height: 100%; padding: 0; }

.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px; background: #fff; border-bottom: 1px solid #eee;
  flex-wrap: wrap; gap: 8px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.title { font-size: 16px; font-weight: 600; }

.main-layout { display: flex; flex: 1; overflow: hidden; gap: 0; }

/* Schema 面板 */
.schema-panel {
  width: 260px; min-width: 200px; background: #fafafa;
  border-right: 1px solid #eee; overflow-y: auto; padding: 12px;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; font-weight: 600; color: #333; margin-bottom: 8px;
}
.no-ds-hint { color: #aaa; font-size: 13px; text-align: center; margin-top: 40px; }
.tree-node { font-size: 13px; display: flex; align-items: center; }
.table-detail { margin-top: 12px; border-top: 1px solid #eee; padding-top: 8px; }
.detail-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; font-weight: 600; color: #333; margin-bottom: 6px;
}

/* 查询面板 */
.query-panel { flex: 1; overflow-y: auto; padding: 16px; background: #fff; }

.query-box { margin-bottom: 16px; }
.query-hint { font-size: 13px; color: #888; margin-bottom: 8px; }
.query-input-row { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
.query-input-row .el-textarea { flex: 1; min-width: 0; }

/* 结果 */
.result-area { display: flex; flex-direction: column; gap: 12px; }
.result-block { border: 1px solid #eee; border-radius: 6px; overflow: hidden; }
.result-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; background: #f5f7fa; font-size: 13px; font-weight: 600; color: #333;
  flex-wrap: wrap; gap: 6px;
}
.table-stats { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
.table-data-result { margin-top: 10px; border: 1px solid #e8e8e8; border-radius: 6px; overflow-x: auto; }
.table-ddl { margin-top: 10px; border: 1px solid #e8e8e8; border-radius: 6px; overflow: hidden; }
.sql-block {
  background: #1e1e1e; color: #d4d4d4; padding: 12px;
  font-size: 13px; overflow-x: auto; margin: 0; line-height: 1.6;
}

/* 历史 */
.history-area { margin-top: 16px; }
.history-item {
  padding: 8px 10px; border-radius: 4px; cursor: pointer;
  border: 1px solid transparent; margin-bottom: 8px; transition: all .15s;
}
.history-item:hover { border-color: #409eff; background: #f0f7ff; }
.hist-q { font-size: 13px; color: #333; margin-bottom: 4px; }
.hist-sql { font-size: 11px; color: #888; margin: 0; background: #f8f8f8; padding: 4px 8px; border-radius: 3px; overflow-x: auto; }

/* ============================================================
   H5 移动端适配 (max-width: 768px)
   ============================================================ */
@media (max-width: 768px) {
  // 主布局: 侧边栏叠到上面或隐藏
  .main-layout {
    flex-direction: column;
    overflow: visible;
  }

  // Schema 面板: 折叠为可展开抽屉
  .schema-panel {
    width: 100%;
    min-width: unset;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid #eee;
  }

  // 工具栏: 紧凑换行
  .toolbar {
    padding: 8px 12px;
    gap: 6px;
  }
  .toolbar-left { gap: 6px; }
  .title { font-size: 14px; }
  .toolbar :deep(.el-select) { width: 140px !important; }

  // 查询面板: 全宽
  .query-panel { padding: 12px; }

  // 输入行: 按钮换行
  .query-input-row {
    gap: 6px;
  }
  .query-input-row :deep(.el-textarea) {
    width: 100% !important;
    flex: unset;
  }
  .query-input-row :deep(.el-button) {
    width: 100%;
    font-size: 14px;
    padding: 10px;
  }

  // 结果区: 全宽, 按钮换行
  .result-header { padding: 6px 10px; }
  .result-header :deep(.el-button) {
    font-size: 12px;
    padding: 4px 8px;
  }
  .sql-block { font-size: 12px; padding: 10px; }

  // 对话框: 全屏
  .nl2sql-page :deep(.el-dialog) {
    width: 95vw !important;
    max-width: 95vw;
    margin: 2vh auto !important;
  }

  // 历史记录: 全宽
  .history-item { padding: 6px 8px; }
  .hist-sql { font-size: 11px; }

  // 数据表: 横向滚动
  .query-panel :deep(.el-table) {
    font-size: 12px;
    overflow-x: auto;
  }
}

/* 极小屏幕 */
@media (max-width: 400px) {
  .toolbar :deep(.el-tag) { display: none; }
  .toolbar :deep(.el-select) { width: 120px !important; }
  .query-panel { padding: 8px; }
}
</style>
