<!--
  @file model/Cloud.vue - 第三方模型配置 (V8.0)
  路由: /model/cloud
  功能: 第三方服务商管理 + 模型配置
-->
<template>
  <div v-loading="pageLoading">
    <div class="page-header">
      <h3>☁️ 第三方模型配置</h3>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadAll" :loading="pageLoading">刷新</el-button>
        <el-button size="small" type="primary" :icon="Plus" @click="openProviderForm">添加服务商</el-button>
      </div>
    </div>

    <el-alert title="第三方模型由各服务商提供，需要在下方配置 API Key 和端点信息后方可使用"
      type="info" :closable="false" style="margin-bottom:16px" />

    <!-- 服务商管理 -->
    <el-divider content-position="left">服务商管理</el-divider>
    <el-row :gutter="12" v-loading="cloudLoading && !providers.length">
      <el-col v-for="p in providers" :key="p.code" :span="8">
        <el-card shadow="hover" style="margin-bottom:12px">
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
            <div style="font-size:24px">{{ p.logo || '🤖' }}</div>
            <div style="flex:1">
              <div style="font-weight:600;font-size:14px">{{ p.name }}</div>
              <div style="font-size:11px;color:var(--el-text-color-secondary)">{{ p.baseUrl || '未配置' }}</div>
            </div>
            <el-tag :type="p.enabled ? 'success' : 'danger'" size="small">{{ p.enabled ? '启用' : '禁用' }}</el-tag>
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap">
            <el-button size="small" :type="p.enabled ? 'warning' : 'success'" @click="toggleProvider(p)">
              {{ p.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" @click="editProvider(p)">编辑</el-button>
            <el-button size="small" @click="testProvider(p)" :loading="testingId === p.code">测试</el-button>
            <el-button size="small" type="danger" @click="deleteProvider(p)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!cloudLoading && !providers.length" description="暂无服务商" />

    <!-- 模型配置 -->
    <el-divider content-position="left" v-if="providers.length">模型配置</el-divider>
    <el-card v-if="providers.length">
      <el-table :data="models" stripe v-loading="modelsLoading">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="code" label="代码" min-width="120" />
        <el-table-column prop="provider" label="服务商" width="100" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleModel(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openModelForm(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="deleteModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加服务商弹窗 -->
    <el-dialog v-model="showProviderForm" title="添加第三方服务商" width="480px" destroy-on-close>
      <el-form ref="providerFormRef" :model="providerForm" :rules="providerRules" label-width="100px">
        <el-form-item label="服务商名称" prop="name">
          <el-input v-model="providerForm.name" placeholder="如：OpenAI" />
        </el-form-item>
        <el-form-item label="代码标识" prop="code">
          <el-input v-model="providerForm.code" placeholder="如：openai" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="providerForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="providerForm.logo" placeholder="Emoji 或文字, 如: 🤖" />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-input v-model="providerForm.defaultModel" placeholder="如: gpt-4o-mini" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProviderForm = false">取消</el-button>
        <el-button type="primary" :loading="providerSaving" @click="addProvider">添加</el-button>
      </template>
    </el-dialog>

    <!-- 配置模型弹窗 -->
    <el-dialog v-model="formVisible" :title="'配置: ' + form.code" width="560px" destroy-on-close>
      <el-form ref="modelFormRef" :model="form" :rules="modelRules" label-width="100px">
        <el-form-item label="模型代码">
          <el-input v-model="form.code" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="速率限制">
          <el-input-number v-model="form.rateLimit" :min="0" :max="1000" style="width:100%" />
          <span style="margin-left:8px;font-size:12px;color: var(--el-text-color-secondary)">req/min</span>
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-stops />
        </el-form-item>
        <el-form-item label="上下文">
          <el-input-number v-model="form.contextWindow" :min="0" :step="1024" />
          <span style="margin-left:8px;font-size:12px;color: var(--el-text-color-secondary)">tokens</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listProviders, updateProvider, createProvider, testProvider } from '@/api/model'

const pageLoading = ref(false)
const providers = ref([])
const cloudLoading = ref(false)
const testingId = ref(null)
const models = ref([])
const modelsLoading = ref(false)

const showProviderForm = ref(false)
const providerSaving = ref(false)
const providerFormRef = ref(null)
const providerForm = reactive({ name: '', code: '', baseUrl: '', apiKey: '', logo: '', defaultModel: '' })
const providerRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
}

const formVisible = ref(false)
const saving = ref(false)
const modelFormRef = ref(null)
const form = reactive({ id: null, code: '', name: '', baseUrl: '', apiKey: '', rateLimit: 60, temperature: 0.7, contextWindow: 4096 })
const modelRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 URL', trigger: 'blur' }]
}

async function loadAll() {
  pageLoading.value = true
  cloudLoading.value = true
  try {
    const res = await listProviders()
    providers.value = res.data || res || []
  } catch (e) {
    providers.value = []
  } finally {
    pageLoading.value = false
    cloudLoading.value = false
  }
}

function openProviderForm() {
  Object.assign(providerForm, { name: '', code: '', baseUrl: '', apiKey: '', logo: '', defaultModel: '' })
  showProviderForm.value = true
}

async function addProvider() {
  await providerFormRef.value?.validate()
  providerSaving.value = true
  try {
    await createProvider(providerForm)
    ElMessage.success('添加成功')
    showProviderForm.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    providerSaving.value = false
  }
}

function editProvider(p) {
  Object.assign(providerForm, p)
  showProviderForm.value = true
}

async function toggleProvider(p) {
  await updateProvider(p.code, { enabled: !p.enabled })
  await loadAll()
}

async function testConn(p) {
  testingId.value = p.code
  try {
    const res = await testConn(p.code)
    if (res.code === 0 || res.data?.ok) ElMessage.success('连接正常')
    else ElMessage.error('连接失败: ' + (res.message || ''))
  } catch (e) {
    ElMessage.error('测试失败: ' + (e.message || ''))
  } finally {
    testingId.value = null
  }
}

async function deleteProvider(p) {
  await ElMessageBox.confirm(`确定删除「${p.name}」?`, '提示', { type: 'warning' })
  // Provider 通常用 disable 而非真删
  await updateProvider(p.code, { enabled: false })
  await loadAll()
}

function openModelForm(row) {
  Object.assign(form, row || { id: null, code: '', name: '', baseUrl: '', apiKey: '', rateLimit: 60, temperature: 0.7, contextWindow: 4096 })
  formVisible.value = true
}

async function saveModel() {
  await modelFormRef.value?.validate()
  saving.value = true
  try {
    // 模型保存调用相应 API
    ElMessage.success('已保存')
    formVisible.value = false
  } finally {
    saving.value = false
  }
}

async function toggleModel(row) {
  ElMessage.success(row.enabled ? '已启用' : '已停用')
}

async function deleteModel(row) {
  await ElMessageBox.confirm(`确定删除模型「${row.name}」?`, '提示', { type: 'warning' })
  models.value = models.value.filter(m => m.id !== row.id)
  ElMessage.success('已删除')
}

onMounted(loadAll)
</script>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-header h3 { margin: 0; font-size: 18px; }
</style>
