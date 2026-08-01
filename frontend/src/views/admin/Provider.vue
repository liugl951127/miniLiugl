<!--
  @file views/admin/Provider.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Provider.vue (模型 Provider)
  @version V3.5.12+ (前端注释补全)
  @description 模型 Provider
-->
<template>
  <div class="page-provider">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('provider.title') }}</h2>
        <p class="page-subtitle">OpenAI / Anthropic / Gemini / 自定义 LLM 接口</p>
      </div>
      <el-button-group>
        <el-input v-model="search" placeholder="搜索..." clearable style="width: 200px" />
        <el-button :icon="Refresh" @click="loadProviders" />
        <el-button type="primary" :icon="Plus" @click="showForm = true">新增 Provider</el-button>
      </el-button-group>
    </header>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>💡 提示</template>
      Provider 用于统一管理 LLM 接口, 配合模型路由使用 (V5.7/V5.10)
    </el-alert>

    <!-- 2. section: Provider 列表 -->
    <section class="section">
      <h3 class="section-title">📋 Provider 列表 ({{ filteredProviders.length }})</h3>
      <el-card shadow="hover">
        <el-table :data="filteredProviders" v-loading="loading" stripe>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="代码" prop="code" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.code }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="名称" prop="name" min-width="160" />
          <el-table-column label="类型" prop="type" width="100" />
          <el-table-column label="Base URL" prop="baseUrl" min-width="200" show-overflow-tooltip />
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleProvider(row)" />
            </template>
          </el-table-column>
          <el-table-column label="优先级" prop="priority" width="100" sortable />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="editProvider(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteProvider(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <!-- 3. dialog: 新增 / 编辑 -->
    <el-dialog v-model="showForm" :title="editingId ? '编辑 Provider' : '新增 Provider'" width="640px">
      <el-form :model="form" label-width="100px" size="default">
        <el-form-item label="代码"><el-input v-model="form.code" placeholder="e.g. openai" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="e.g. OpenAI" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="Anthropic" value="ANTHROPIC" />
            <el-option label="Gemini" value="GEMINI" />
            <el-option label="自研" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL"><el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password /></el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="saveProvider" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Connection, Edit, Delete } from '@element-plus/icons-vue'
import { listProviders, createProvider, updateProvider, deleteProvider, testProvider } from '@/api/model'

const { t } = useI18n()
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