<!-- @file function/Index.vue - Function 工具 V6.8.20 -->
<template>
  <PageStandard title="🛠️ Function 工具" subtitle="工具函数 · 自定义能力 · API 集成">
    <template #actions>
      <el-button type="primary" @click="showCreateDialog = true"><el-icon><Plus /></el-icon>注册工具</el-button>
    </template>

    <el-table :data="tools" v-loading="loading" stripe>
      <el-table-column prop="name" label="工具名" width="180" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" align="center" />
      <el-table-column label="风险等级" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="riskType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="testToolDialog(row)">测试</el-button>
          <el-button size="small" :type="row.enabled ? 'danger' : 'success'" @click="doToggleTool(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && !tools.length"
      description="暂无可用工具"
      :image-size="100"
      style="padding: 40px 0"
    >
      <el-button type="primary" @click="showCreateDialog = true">注册第一个工具</el-button>
    </el-empty>

    <!-- 测试弹窗 -->
    <el-dialog v-model="testVisible" :title="'测试: ' + testTool_.name" width="560px">
      <el-form label-width="80px">
        <el-form-item label="参数 JSON">
          <el-input v-model="testParams" type="textarea" :rows="4" placeholder='{"param": "value"}' />
        </el-form-item>
      </el-form>
      <div v-if="testResult" style="margin-top:12px">
        <div class="result-label">结果：</div>
        <pre class="result-code">{{ testResult }}</pre>
      </div>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testing" @click="runTest">执行</el-button>
      </template>
    </el-dialog>

    <!-- 注册工具弹窗 -->
    <el-dialog v-model="showCreateDialog" title="注册工具" width="560px" @close="resetForm">
      <el-form :model="toolForm" :rules="toolRules" ref="toolFormRef" label-width="100px">
        <el-form-item label="工具名称" prop="name">
          <el-input v-model="toolForm.name" placeholder="例: weather_lookup" />
        </el-form-item>
        <el-form-item label="显示名" prop="displayName">
          <el-input v-model="toolForm.displayName" placeholder="例: 天气查询" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="toolForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="端点 URL" prop="endpoint">
          <el-input v-model="toolForm.endpoint" placeholder="https://api.example.com/tool" />
        </el-form-item>
        <el-form-item label="方法">
          <el-select v-model="toolForm.method">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="入参 JSON" prop="paramsSchema">
          <el-input v-model="toolForm.paramsSchema" type="textarea" :rows="4"
                    placeholder='{"location": "string"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>
  </PageStandard>
</template>

<script setup>
import PageStandard from '@/components/PageStandard.vue'
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { functionApi } from '@/api/function'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const tools = ref([])
const loading = ref(false)
const testVisible = ref(false)
const testTool_ = ref({})
const testParams = ref('{}')
const testResult = ref('')
const testing = ref(false)
const showCreateDialog = ref(false)
const toolFormRef = ref(null)
const creating = ref(false)
const toolForm = ref({
  name: '',
  displayName: '',
  description: '',
  endpoint: '',
  method: 'POST',
  paramsSchema: '{}'
})
const toolRules = {
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  endpoint: [{ required: true, message: '请输入端点 URL', trigger: 'blur' }]
}

function riskType(r) {
  return { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[r] || 'info'
}

async function loadTools() {
  loading.value = true
  try {
    const r = await functionApi.listTools()
    tools.value = r.data || []
  } catch (e) {
    tools.value = []
    ElMessage.error('加载工具列表失败: ' + (e?.response?.data?.message || e?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

function testToolDialog(t) {
  testTool_.value = t
  testParams.value = '{}'
  testResult.value = ''
  testVisible.value = true
}

async function runTest() {
  testing.value = true
  testResult.value = ''
  try {
    // 验证 JSON 格式
    let args
    try {
      args = JSON.parse(testParams.value)
    } catch (parseErr) {
      ElMessage.error('参数 JSON 格式错误：' + parseErr.message)
      testResult.value = '❌ 参数解析失败：' + parseErr.message
      return
    }
    const r = await functionApi.invoke(testTool_.value.name, args)
    testResult.value = JSON.stringify(r.data || r, null, 2)
    ElMessage.success('测试调用成功')
  } catch (e) {
    const errMsg = '错误: ' + (e?.message || e)
    testResult.value = errMsg
    ElMessage.error('测试失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    testing.value = false
  }
}

async function doToggleTool(t) {
  const action = t.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}工具「${t.name}」吗？`,
      `${action}工具`,
      { confirmButtonText: `确认${action}`, cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    ElMessage.error('操作失败：' + (e?.message || '网络错误'))
    return
  }
  const ownerId = userStore.profile?.id || userStore.userInfo?.id
  if (!ownerId) { ElMessage.warning('无法获取用户ID'); return }
  try {
    await functionApi.updateTool(t.id, { ownerId, enabled: !t.enabled })
    t.enabled = !t.enabled
    ElMessage.success(t.enabled ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error(`${action}失败：` + (e?.message || '网络错误'))
  }
}

async function confirmCreate() {
  if (!toolFormRef.value) return
  try {
    await toolFormRef.value.validate()
  } catch (e) {
    return
  }
  creating.value = true
  try {
    await functionApi.createTool(toolForm.value)
    ElMessage.success('工具注册成功')
    showCreateDialog.value = false
    resetForm()
    if (typeof loadTools === 'function') loadTools()
  } catch (e) {
    ElMessage.error('注册失败: ' + (e?.message || e))
  } finally {
    creating.value = false
  }
}

function resetForm() {
  toolForm.value = {
    name: '',
    displayName: '',
    description: '',
    endpoint: '',
    method: 'POST',
    paramsSchema: '{}'
  }
  toolFormRef.value?.clearValidate()
}

onMounted(loadTools)
</script>

<style lang="scss" scoped>
.result-label { font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.result-code { background: #1e293b; color: #a5f3fc; padding: 10px; border-radius: 6px; font-size: 12px; max-height: 200px; overflow: auto; }
</style>
