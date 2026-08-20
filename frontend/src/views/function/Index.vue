<!-- @file function/Index.vue - Function 工具 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>Function 工具</h2>
      <el-button type="primary" @click="showCreate = true"><el-icon><Plus /></el-icon>注册工具</el-button>
    </div>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
const showCreate = ref(false)

function riskType(r) {
  return { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[r] || 'info'
}

async function loadTools() {
  loading.value = true
  try { tools.value = ((await functionApi.listTools())).data || [] }
  catch { tools.value = [] }
  finally { loading.value = false }
}

function testToolDialog(t) { testTool_.value = t; testParams.value = '{}'; testResult.value = ''; testVisible.value = true }

async function runTest() {
  testing.value = true
  try {
    const args = JSON.parse(testParams.value)
    const r = await functionApi.invoke(testTool_.value.name, args)
    testResult.value = JSON.stringify(r.data || r, null, 2)
  } catch (e) { testResult.value = '错误: ' + (e.message || '') }
  finally { testing.value = false }
}

async function doToggleTool(t) {
  const ownerId = userStore.profile?.id || userStore.userInfo?.id
  if (!ownerId) { ElMessage.warning('无法获取用户ID'); return }
  try {
    await functionApi.updateTool(t.id, { ownerId, enabled: !t.enabled })
    t.enabled = !t.enabled
    ElMessage.success(t.enabled ? '已启用' : '已禁用')
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(loadTools)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.result-label { font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.result-code { background: #1e293b; color: #a5f3fc; padding: 10px; border-radius: 6px; font-size: 12px; max-height: 200px; overflow: auto; }
</style>
