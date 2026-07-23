<!--
  Function Call 工具管理页 (V3.5.5+ 新增)
  对应后端模块: minimax-pipeline (端口 8093)
  API 路径: /api/v1/function/*
-->
<!--
  @file views/function/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="function-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span class="title">
            <el-icon><Tools /></el-icon>
            Function Call 工具管理
          </span>
          <div class="actions">
            <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable style="width: 180px; margin-right: 12px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-button :icon="Refresh" @click="loadTools" :loading="loading">刷新</el-button>
            <el-button :icon="Plus" type="primary" @click="showAddDialog = true">添加工具</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredTools" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="工具名" width="160" />
        <el-table-column prop="displayName" label="显示名" width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTag(row.category)">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.callCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="invokeTool(row)">调用</el-button>
            <el-button link type="primary" @click="editTool(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteTool(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showAddDialog" :title="editing ? '编辑工具' : '添加工具'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="工具名" prop="name">
          <el-input v-model="form.name" placeholder="英文,如 get_weather" />
        </el-form-item>
        <el-form-item label="显示名" prop="displayName">
          <el-input v-model="form.displayName" placeholder="中文,如 获取天气" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="工具分类" style="width: 100%">
            <el-option label="数据查询" value="data" />
            <el-option label="业务工具" value="business" />
            <el-option label="计算工具" value="compute" />
            <el-option label="AI 增强" value="ai" />
            <el-option label="系统工具" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="参数定义 (JSON Schema)" prop="parameters">
          <el-input v-model="form.parameters" type="textarea" :rows="6"
                    placeholder='{"type":"object","properties":{...}}' />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTool">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调用测试对话框 -->
    <el-dialog v-model="showInvokeDialog" title="工具调用测试" width="600px">
      <el-form label-width="100px">
        <el-form-item label="工具名">
          <el-tag>{{ currentTool?.name }}</el-tag>
        </el-form-item>
        <el-form-item label="参数 (JSON)">
          <el-input v-model="invokeArgs" type="textarea" :rows="6" placeholder='{"key":"value"}' />
        </el-form-item>
        <el-form-item v-if="invokeResult">
          <el-divider />
          <p>调用结果:</p>
          <pre class="result">{{ invokeResult }}</pre>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInvokeDialog = false">关闭</el-button>
        <el-button type="primary" :loading="invoking" @click="doInvoke">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tools, Refresh, Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { functionApi } from '@/api/function'

const tools = ref([])
const loading = ref(false)
const categoryFilter = ref('')
const showAddDialog = ref(false)
const showInvokeDialog = ref(false)
const editing = ref(false)
const invoking = ref(false)
const currentTool = ref(null)
const invokeArgs = ref('{}')
const invokeResult = ref('')
const formRef = ref()

const categories = ['data', 'business', 'compute', 'ai', 'system']

const form = reactive({
  id: null, name: '', displayName: '', category: 'data',
  description: '', parameters: '{}', enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入工具名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  parameters: [{ required: true, message: '请输入参数定义', trigger: 'blur' }]
}

const categoryTag = (c) => ({
  data: 'primary', business: 'success', compute: 'warning',
  ai: 'danger', system: 'info'
})[c] || ''

const filteredTools = computed(() =>
  categoryFilter.value
    ? tools.value.filter(t => t.category === categoryFilter.value)
    : tools.value
)

const loadTools = async () => {
  loading.value = true
  try {
    const res = await functionApi.listTools()
    tools.value = res.data || res || []
  } catch (e) {
    ElMessage.error('加载工具失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const editTool = (row) => {
  Object.assign(form, row)
  editing.value = true
  showAddDialog.value = true
}

const deleteTool = async (row) => {
  await ElMessageBox.confirm(`确认删除工具 ${row.displayName}?`, '警告', { type: 'warning' })
  try {
    await functionApi.deleteTool(row.id)
    ElMessage.success('删除成功')
    loadTools()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

const saveTool = async () => {
  await formRef.value.validate()
  try {
    if (editing.value) {
      await functionApi.updateTool(form.id, form)
    } else {
      await functionApi.createTool(form)
    }
    ElMessage.success('保存成功')
    showAddDialog.value = false
    editing.value = false
    loadTools()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const invokeTool = (row) => {
  currentTool.value = row
  invokeArgs.value = '{}'
  invokeResult.value = ''
  showInvokeDialog.value = true
}

const doInvoke = async () => {
  let args = {}
  try { args = JSON.parse(invokeArgs.value) }
  catch (e) { return ElMessage.error('参数 JSON 格式错: ' + e.message) }

  invoking.value = true
  try {
    const res = await functionApi.invoke(currentTool.value.name, args)
    invokeResult.value = JSON.stringify(res, null, 2)
  } catch (e) {
    invokeResult.value = '❌ ' + e.message
  } finally {
    invoking.value = false
  }
}

onMounted(loadTools)
</script>

<style lang="scss" scoped>
.function-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.result { background: #f5f7fa; padding: 12px; border-radius: 4px; max-height: 400px; overflow: auto; white-space: pre-wrap; }
</style>
