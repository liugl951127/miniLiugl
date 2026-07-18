<!--
  @file views/admin/Provider.vue (模型 Provider)
  @version V3.5.12+ (前端注释补全)
  @description 模型 Provider
-->
<template>
  <div class="provider-page">
    <div class="page-header">
      <h2>🛠️ 模型 Provider 管理</h2>
      <div class="actions">
        <el-input v-model="search" placeholder="搜索..." clearable style="width: 200px" />
        <el-button type="primary" @click="showForm = true">
          <el-icon><Plus /></el-icon> 新增 Provider
        </el-button>
        <el-button @click="loadProviders"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>💡 提示</template>
      Provider 用于统一管理 OpenAI / Anthropic / Gemini / 自定义 等 LLM 接口, 配合模型路由使用 (V5.7/V5.10)
    </el-alert>

    <el-table :data="filteredProviders" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="代码" prop="code" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.code }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="name" min-width="160" />
      <el-table-column label="类型" prop="providerType" width="120">
        <template #default="{ row }">
          <el-tag :type="typeColor(row.providerType)" size="small">{{ row.providerType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Base URL" prop="baseUrl" min-width="200" show-overflow-tooltip />
      <el-table-column label="API Key" min-width="160">
        <template #default="{ row }">
          <code v-if="row.apiKey">{{ maskKey(row.apiKey) }}</code>
          <el-tag v-else type="info" size="small">未配置</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === 1" @change="(v) => toggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="权重" prop="sort" width="70" sortable />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleTest(row)" :loading="testing[row.id]">
            <el-icon><Connection /></el-icon> 测试
          </el-button>
          <el-button size="small" @click="editProvider(row)"><el-icon><Edit /></el-icon></el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)"><el-icon><Delete /></el-icon></el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有 Provider, 点击右上角新增" />
      </template>
    </el-table>

    <!-- 表单对话框 -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑 Provider' : '新增 Provider'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="代码" required>
          <el-input v-model="form.code" placeholder="e.g. openai-main" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="e.g. OpenAI 主线路" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.providerType" style="width: 100%">
            <el-option label="OpenAI 兼容" value="openai" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Gemini" value="gemini" />
            <el-option label="Ollama (本地)" value="ollama" />
            <el-option label="自定义" value="custom" />
            <el-option label="Mock" value="mock" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-input v-model="form.defaultModel" placeholder="e.g. gpt-4o-mini" />
        </el-form-item>
        <el-form-item label="权重 (sort)">
          <el-input-number v-model="form.sort" :min="0" :max="100" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">数值越大越优先</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabledBool" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试结果对话框 -->
    <el-dialog v-model="showTestResult" title="Provider 连接测试" width="520px">
      <div v-if="testResult" class="test-result">
        <el-result :icon="testResult.ok ? 'success' : 'error'" :title="testResult.ok ? '✓ 连接正常' : '✗ 连接失败'">
          <template #sub-title>
            <div class="test-meta">
              <div><strong>Provider:</strong> {{ testResult.provider }}</div>
              <div><strong>Base URL:</strong> {{ testResult.baseUrl }}</div>
              <div><strong>API Key:</strong> {{ testResult.apiKeyMasked }}</div>
              <div v-if="testResult.note" class="note">{{ testResult.note }}</div>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Connection, Edit, Delete } from '@element-plus/icons-vue'
import { listProviders, createProvider, updateProvider, deleteProvider, testProvider } from '@/api/model'

const providers = ref([])
const loading = ref(false)
const saving = ref(false)
const search = ref('')
const showForm = ref(false)
const showTestResult = ref(false)
const testResult = ref(null)
const testing = ref({})

const form = reactive({
  id: null, code: '', name: '', providerType: 'openai',
  baseUrl: '', apiKey: '', defaultModel: '', sort: 0,
  enabled: 1, enabledBool: true, remark: ''
})

const filteredProviders = computed(() => {
  if (!search.value) return providers.value
  const q = search.value.toLowerCase()
  return providers.value.filter(p =>
    p.code?.toLowerCase().includes(q) ||
    p.name?.toLowerCase().includes(q) ||
    p.providerType?.toLowerCase().includes(q)
  )
})

function maskKey(k) {
  if (!k) return ''
  return k.length > 12 ? k.slice(0, 4) + '***' + k.slice(-4) : '***'
}

function typeColor(t) {
  const m = { openai: '', anthropic: 'success', gemini: 'warning', ollama: 'info', mock: '', custom: '' }
  return m[t] || ''
}

async function loadProviders() {
  loading.value = true
  try {
    const res = await listProviders(1, 100)
    providers.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.value = false }
}

function resetForm() {
  Object.assign(form, {
    id: null, code: '', name: '', providerType: 'openai',
    baseUrl: '', apiKey: '', defaultModel: '', sort: 0,
    enabled: 1, enabledBool: true, remark: ''
  })
}

function editProvider(row) {
  Object.assign(form, {
    ...row,
    enabledBool: row.enabled === 1
  })
  showForm.value = true
}

async function handleSave() {
  if (!form.code.trim() || !form.name.trim() || !form.baseUrl.trim()) {
    return ElMessage.warning('代码/名称/Base URL 必填')
  }
  saving.value = true
  try {
    form.enabled = form.enabledBool ? 1 : 0
    if (form.id) {
      await updateProvider(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createProvider(form)
      ElMessage.success('创建成功')
    }
    showForm.value = false
    resetForm()
    await loadProviders()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally { saving.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除 Provider "${row.name}"?`, '警告', { type: 'warning' })
  try {
    await deleteProvider(row.id)
    ElMessage.success('删除成功')
    await loadProviders()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

async function handleTest(row) {
  testing.value[row.id] = true
  try {
    const res = await testProvider(row.id)
    testResult.value = res.data?.data || res.data
    showTestResult.value = true
  } catch (e) {
    testResult.value = { ok: false, note: '测试失败: ' + (e.response?.data?.message || e.message) }
    showTestResult.value = true
  } finally {
    testing.value[row.id] = false
  }
}

async function toggleEnabled(row, val) {
  try {
    await updateProvider(row.id, { ...row, enabled: val ? 1 : 0, enabledBool: val })
    row.enabled = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已停用')
  } catch (e) {
    ElMessage.error('切换失败')
    await loadProviders()
  }
}

onMounted(loadProviders)
</script>

<style scoped>
.provider-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.actions { display: flex; gap: 12px; align-items: center; }
.test-result .test-meta { text-align: left; line-height: 1.8; padding: 12px; background: #f5f7fa; border-radius: 6px; }
.test-result .note { margin-top: 8px; color: #909399; font-size: 13px; font-style: italic; }
code { font-family: 'JetBrains Mono', monospace; color: #d63384; font-size: 12px; }
</style>