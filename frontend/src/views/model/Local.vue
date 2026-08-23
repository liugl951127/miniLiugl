<!--
  @file model/Local.vue - 本地推理服务器管理 (V8.0)
  路由: /model/local
  功能: 注册 Ollama/vLLM/FastAPI 推理服务器, 发现/同步模型
-->
<template>
  <div v-loading="pageLoading">
    <div class="page-header">
      <h3>🏠 本地推理服务器</h3>
      <el-button size="small" @click="loadAll" :loading="pageLoading">刷新</el-button>
    </div>

    <el-alert
      title="本地模型通过内网调用，数据完全自主可控。需先注册推理服务器，再同步模型列表。"
      type="success" :closable="false" style="margin-bottom:16px" />

    <el-divider content-position="left">本地推理服务器</el-divider>

    <el-row :gutter="12" v-loading="localLoading">
      <el-col v-for="p in localProviders" :key="p.id" :span="8">
        <el-card shadow="hover" style="margin-bottom:12px">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
            <div style="font-size:22px">🖥️</div>
            <div style="flex:1">
              <div style="font-weight:600;font-size:14px">{{ p.name }}</div>
              <div style="font-size:11px;color: var(--el-text-color-secondary);word-break:break-all">{{ p.baseUrl }}</div>
            </div>
            <el-tag :type="p.enabled ? 'success' : 'danger'" size="small">{{ p.enabled ? '启用' : '禁用' }}</el-tag>
          </div>
          <div style="font-size:12px;color: var(--el-color-success);margin-bottom:8px">
            协议: {{ p.protocol }} | {{ p.description || '无描述' }}
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap">
            <el-button size="small" type="primary" @click="discoverLocalModels(p)" :loading="discoveringId === p.id">
              🔍 发现模型
            </el-button>
            <el-button size="small" type="success" @click="syncLocalModels(p)" :loading="syncingId === p.id">
              ↻ 同步全部
            </el-button>
            <el-button size="small" :type="p.enabled ? 'warning' : 'success'" @click="confirmToggleLocalProvider(p)">
              {{ p.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="confirmDeleteLocalProvider(p)">删除</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="add-card" @click="openLocalProviderForm()" style="cursor:pointer;height:160px;display:flex;align-items:center;justify-content:center">
          <div style="text-align:center">
            <el-icon :size="32" color="#409eff"><Plus /></el-icon>
            <div style="margin-top:8px;font-size:14px;color: var(--el-color-primary)">注册推理服务器</div>
            <div style="font-size:11px;color: var(--el-text-color-secondary)">Ollama / vLLM / FastAPI</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!localLoading && !localProviders.length" description="暂无本地推理服务器，点击右上角卡片注册" style="margin-top:16px" />

    <!-- 发现模型弹窗 -->
    <el-dialog v-model="showDiscoveredModels" title="发现可用模型" width="500px" destroy-on-close>
      <el-alert v-if="discoverError" :title="discoverError" type="error" style="margin-bottom:12px" />
      <div v-if="discoveredModels.length > 0" v-loading="discoveringId !== null">
        <el-checkbox-group v-model="selectedModels" style="margin-bottom:12px">
          <el-checkbox v-for="m in discoveredModels" :key="m" :value="m" style="display:block;margin-bottom:4px">
            {{ m }}
          </el-checkbox>
        </el-checkbox-group>
        <el-button type="primary" :loading="addingSelected" @click="addSelectedModels">
          添加到模型列表 ({{ selectedModels.length }} 个)
        </el-button>
      </div>
      <el-empty v-else-if="!discoveringId" description="暂未发现模型，请确保服务器在线" />
      <div v-else style="text-align:center;padding:24px;color: var(--el-text-color-secondary)">
        <el-icon class="is-loading"><Loading /></el-icon> 正在发现模型…
      </div>
    </el-dialog>

    <!-- 注册服务商弹窗 -->
    <el-dialog v-model="showLocalProviderForm" title="注册本地推理服务器" width="480px" destroy-on-close>
      <el-form ref="localProviderFormRef" :model="localProviderForm" :rules="localProviderRules" label-width="110px">
        <el-form-item label="服务器名称" prop="name">
          <el-input v-model="localProviderForm.name" placeholder="如：公司 Ollama 服务器" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="localProviderForm.baseUrl" placeholder="http://192.168.1.100:11434" />
          <div style="font-size:11px;color: var(--el-text-color-secondary);margin-top:4px">
            Ollama 默认端口 11434，vLLM 默认 8000
          </div>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="localProviderForm.apiKey" type="password" show-password placeholder="本地服务通常留空" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="localProviderForm.description" placeholder="服务器用途或备注…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLocalProviderForm = false">取消</el-button>
        <el-button type="primary" :loading="localSaving" @click="registerLocalProvider">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import { localModelApi } from '@/api/model'

const pageLoading = ref(false)
const localProviders = ref([])
const localLoading = ref(false)

const showLocalProviderForm = ref(false)
const localSaving = ref(false)
const localProviderFormRef = ref(null)
const localProviderForm = reactive({ name: '', baseUrl: '', apiKey: '', description: '' })
const localProviderRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 URL', trigger: 'blur' }]
}

const showDiscoveredModels = ref(false)
const discoveredModels = ref([])
const selectedModels = ref([])
const discoverError = ref('')
const discoveringId = ref(null)
const syncingId = ref(null)
const addingSelected = ref(false)

async function loadAll() {
  pageLoading.value = true
  localLoading.value = true
  try {
    const res = await localModelApi.listProviders()
    localProviders.value = res.data || res || []
  } catch (e) {
    localProviders.value = []
  } finally {
    pageLoading.value = false
    localLoading.value = false
  }
}

function openLocalProviderForm() {
  Object.assign(localProviderForm, { name: '', baseUrl: '', apiKey: '', description: '' })
  showLocalProviderForm.value = true
}

async function registerLocalProvider() {
  await localProviderFormRef.value?.validate()
  localSaving.value = true
  try {
    await localModelApi.createProvider(localProviderForm)
    ElMessage.success('注册成功')
    showLocalProviderForm.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error('注册失败: ' + (e.message || ''))
  } finally {
    localSaving.value = false
  }
}

async function confirmDeleteLocalProvider(p) {
  await ElMessageBox.confirm(`确定删除「${p.name}」?`, '提示', { type: 'warning' })
  await localModelApi.removeProvider(p.id)
  ElMessage.success('已删除')
  await loadAll()
}

async function confirmToggleLocalProvider(p) {
  await localModelApi.updateProvider(p.id, { enabled: !p.enabled })
  await loadAll()
}

async function discoverLocalModels(p) {
  discoveringId.value = p.id
  discoverError.value = ''
  discoveredModels.value = []
  selectedModels.value = []
  showDiscoveredModels.value = true
  try {
    const res = await localModelApi.discover(p.id)
    discoveredModels.value = res.data || res || []
    if (!discoveredModels.value.length) discoverError.value = '未发现模型, 请检查服务器'
  } catch (e) {
    discoverError.value = e.message || '发现失败'
  } finally {
    discoveringId.value = null
  }
}

async function syncLocalModels(p) {
  syncingId.value = p.id
  try {
    await localModelApi.sync(p.id)
    ElMessage.success('同步完成')
  } catch (e) {
    ElMessage.error('同步失败')
  } finally {
    syncingId.value = null
  }
}

async function addSelectedModels() {
  if (!selectedModels.value.length) return ElMessage.warning('请先选择模型')
  addingSelected.value = true
  try {
    await localModelApi.addDiscovered({ models: selectedModels.value })
    ElMessage.success(`已添加 ${selectedModels.value.length} 个模型`)
    showDiscoveredModels.value = false
  } finally {
    addingSelected.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-header h3 { margin: 0; font-size: 18px; }
.add-card:hover { transform: translateY(-2px); transition: all 0.2s; }
</style>
