<!--
  @file analytics/Nlsql.vue - NL2SQL 自然语言查询 (V8.0)
  路由: /analytics/nlsql
-->
<template>
  <div v-loading="nlLoading">
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>💬 NL2SQL 自然语言查询</span>
              <el-button size="small" :icon="Plus" @click="showDsForm = true">数据源</el-button>
            </div>
          </template>
          <el-form label-width="80px">
            <el-form-item label="数据源">
              <el-select v-model="nlDsId" placeholder="选择数据源" style="width:100%" @change="onDsChange">
                <el-option v-for="d in dataSources" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="数据库">
              <el-select v-model="nlDb" placeholder="选择数据库" style="width:100%" :loading="nlDbLoading">
                <el-option v-for="db in nlDatabases" :key="db" :label="db" :value="db" />
              </el-select>
            </el-form-item>
            <el-form-item label="问题">
              <el-input v-model="nlQuery" type="textarea" :rows="3" placeholder="用自然语言描述想查询的内容, 例如: 过去7天每天的 API 调用量" />
            </el-form-item>
            <el-form-item label="模型">
              <el-select v-model="nlModel" style="width:100%">
                <el-option v-for="m in nlModelOptions" :key="m.modelCode" :label="m.displayName || m.modelCode" :value="m.modelCode" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="nlLoading" @click="runQuery">执行</el-button>
              <el-button @click="nlQuery = ''">清空</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><span>💡 示例问题</span></template>
          <div style="display:flex;flex-direction:column;gap:8px">
            <div v-for="q in sampleQueries" :key="q.text" class="sample-query" @click="nlQuery = q.text">
              <div>{{ q.text }}</div>
              <div style="font-size:11px;color:var(--el-text-color-secondary)">{{ q.tip }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="nlResult" style="margin-top:12px">
      <template #header>
        <span>📊 查询结果 (SQL: <code>{{ nlResult.sql }}</code>)</span>
      </template>
      <el-table :data="nlPaginatedRows" stripe>
        <el-table-column
          v-for="(col, i) in (nlResult.columns || [])" :key="i"
          :prop="col" :label="col" min-width="120" show-overflow-tooltip
        />
      </el-table>
      <el-pagination
        v-model:current-page="nlPage"
        :page-size="nlPageSize"
        :total="(nlResult.rows || []).length"
        layout="prev, pager, next, total"
        style="margin-top:12px"
      />
    </el-card>

    <el-card style="margin-top:12px" v-loading="nlHistoryLoading">
      <template #header><span>📜 查询历史</span></template>
      <el-table :data="nlHistory" stripe :empty-text="nlHistoryEmptyText">
        <el-table-column prop="text" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sql" label="SQL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>

    <!-- 数据源表单 -->
    <el-dialog v-model="showDsForm" title="添加数据源" width="480px">
      <el-form :model="dsForm" :rules="dsFormRules" ref="dsFormRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="dsForm.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="dsForm.type" style="width:100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="MongoDB" value="mongodb" />
          </el-select>
        </el-form-item>
        <el-form-item label="Host" prop="host">
          <el-input v-model="dsForm.host" />
        </el-form-item>
        <el-form-item label="Port" prop="port">
          <el-input-number v-model="dsForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库" prop="database">
          <el-input v-model="dsForm.database" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="dsForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="dsForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDsForm = false">取消</el-button>
        <el-button type="primary" :loading="addingDs" @click="addDataSource">保存并测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { nl2sqlAsk, nl2sqlHistory, listDataSources, createDataSource, testDataSource, listDatabases } from '@/api/analytics'
import { listEnabledModels } from '@/api/model'

const nlQuery = ref('')
const nlLoading = ref(false)
const nlResult = ref(null)
const nlHistory = ref([])
const nlHistoryLoading = ref(false)
const nlPage = ref(1)
const nlPageSize = 20
const nlPaginatedRows = computed(() => {
  const rows = nlResult.value?.rows || []
  return rows.slice((nlPage.value - 1) * nlPageSize, nlPage.value * nlPageSize)
})
const nlHistoryEmptyText = computed(() => nlHistoryLoading.value ? '加载中...' : '暂无查询历史')

const showDsForm = ref(false)
const addingDs = ref(false)
const dsFormRef = ref(null)
const dataSources = ref([])
const nlDsId = ref(null)
const nlDb = ref('')
const nlDatabases = ref([])
const nlDbLoading = ref(false)
const nlModel = ref('')
const nlModelOptions = ref([])

const dsForm = reactive({ name: '', type: 'mysql', host: 'localhost', port: 3306, database: '', username: '', password: '' })
const dsFormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }]
}

const sampleQueries = [
  { text: '过去7天每天的 API 调用量？', tip: '统计每日趋势' },
  { text: '调用量最多的用户 TOP5？', tip: '按用户聚合' },
  { text: '各模型的平均响应时间？', tip: '性能分析' },
  { text: '失败请求的常见原因？', tip: '错误分析' }
]

async function loadDataSources() {
  try {
    const res = await listDataSources()
    dataSources.value = res.data?.data || res.data || []
  } catch (e) { dataSources.value = [] }
}

async function loadHistory() {
  nlHistoryLoading.value = true
  try {
    const res = await nl2sqlHistory()
    nlHistory.value = res.data?.data || res.data || []
  } finally { nlHistoryLoading.value = false }
}

async function loadModels() {
  try {
    const res = await listEnabledModels()
    const list = Array.isArray(res) ? res : (res?.data || [])
    nlModelOptions.value = list.filter(m => m.category === 'nl2sql' || m.category === 'sql')
  } catch (e) { nlModelOptions.value = [] }
}

async function onDsChange(dsId) {
  if (!dsId) return
  nlDbLoading.value = true
  try {
    const res = await listDatabases(dsId)
    nlDatabases.value = res.data?.data || res.data || []
  } catch (e) { nlDatabases.value = [] }
  finally { nlDbLoading.value = false }
}

async function runQuery() {
  if (!nlQuery.value.trim()) return ElMessage.warning('请输入问题')
  nlLoading.value = true
  try {
    const res = await nl2sqlAsk({ question: nlQuery.value, dsId: nlDsId.value, db: nlDb.value, model: nlModel.value })
    if (res.code === 0) {
      nlResult.value = res.data
      ElMessage.success('查询成功')
      loadHistory()
    } else ElMessage.error(res.message || '查询失败')
  } finally { nlLoading.value = false }
}

async function addDataSource() {
  await dsFormRef.value?.validate()
  addingDs.value = true
  try {
    await createDataSource(dsForm)
    ElMessage.success('添加成功')
    showDsForm.value = false
    loadDataSources()
  } catch (e) { ElMessage.error('添加失败') }
  finally { addingDs.value = false }
}

onMounted(() => {
  loadDataSources()
  loadHistory()
  loadModels()
})
</script>

<style scoped>
.sample-query {
  padding: 10px; background: #f8fafc; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
}
.sample-query:hover { background: #eff6ff; transform: translateX(4px); }
</style>
