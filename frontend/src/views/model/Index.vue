<!--
  Model 模型管理页 (V3.5.5+ 新增)
  对应后端模块: minimax-model (端口 8084)
  API 路径: /api/v1/models/*
-->
<template>
  <div class="model-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span class="title">
            <el-icon><Cpu /></el-icon>
            Model 模型管理
          </span>
          <div class="actions">
            <el-button :icon="Refresh" @click="loadModels" :loading="loading">刷新</el-button>
            <el-button :icon="Plus" type="primary" @click="showAddDialog = true">添加模型</el-button>
          </div>
        </div>
      </template>

      <!-- 模型列表 -->
      <el-table :data="models" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="模型代码" width="160" />
        <el-table-column prop="name" label="模型名称" width="200" />
        <el-table-column prop="provider" label="提供商" width="120">
          <template #default="{ row }">
            <el-tag :type="providerTag(row.provider)">{{ row.provider || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type || 'chat' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contextWindow" label="上下文" width="100">
          <template #default="{ row }">
            {{ row.contextWindow || 4096 }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="testModel(row)">测试</el-button>
            <el-button link type="primary" @click="editModel(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showAddDialog" :title="editing ? '编辑模型' : '添加模型'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模型代码" prop="code">
          <el-input v-model="form.code" placeholder="如 gpt-4 / minimax-chat" />
        </el-form-item>
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="如 GPT-4 / 自研大模型" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="选择提供商" style="width: 100%">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Google" value="google" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Moonshot" value="moonshot" />
            <el-option label="自研 (MiniMax)" value="minimax" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="模型类型" style="width: 100%">
            <el-option label="对话 (chat)" value="chat" />
            <el-option label="补全 (completion)" value="completion" />
            <el-option label="嵌入 (embedding)" value="embedding" />
            <el-option label="图像 (image)" value="image" />
            <el-option label="语音 (audio)" value="audio" />
          </el-select>
        </el-form-item>
        <el-form-item label="上下文窗口" prop="contextWindow">
          <el-input-number v-model="form.contextWindow" :min="512" :max="200000" :step="512" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试结果对话框 -->
    <el-dialog v-model="showTestDialog" title="模型测试" width="700px">
      <div v-loading="testing">
        <p>测试 prompt: <b>{{ testPrompt }}</b></p>
        <el-divider />
        <p>响应:</p>
        <pre class="test-result">{{ testResult }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu, Refresh, Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { modelApi } from '@/api/model'

const models = ref([])
const loading = ref(false)
const showAddDialog = ref(false)
const showTestDialog = ref(false)
const editing = ref(false)
const testing = ref(false)
const testPrompt = ref('')
const testResult = ref('')
const formRef = ref()

const form = reactive({
  id: null,
  code: '',
  name: '',
  provider: 'minimax',
  type: 'chat',
  contextWindow: 4096,
  status: 1,
  description: ''
})

const rules = {
  code: [{ required: true, message: '请输入模型代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const providerTag = (p) => ({
  openai: 'success', anthropic: 'warning', google: 'info',
  deepseek: '', moonshot: '', minimax: 'primary'
})[p] || ''

const loadModels = async () => {
  loading.value = true
  try {
    const res = await modelApi.list()
    models.value = res.data || res || []
  } catch (e) {
    ElMessage.error('加载模型失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const editModel = (row) => {
  Object.assign(form, row)
  editing.value = true
  showAddDialog.value = true
}

const deleteModel = async (row) => {
  await ElMessageBox.confirm(`确认删除模型 ${row.name}?`, '警告', { type: 'warning' })
  try {
    await http.delete(`/api/v1/models/${row.id}`)
    ElMessage.success('删除成功')
    loadModels()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

const saveModel = async () => {
  await formRef.value.validate()
  try {
    if (editing.value) {
      await http.put(`/api/v1/models/${form.id}`, form)
    } else {
      await http.post('/api/v1/models', form)
    }
    ElMessage.success('保存成功')
    showAddDialog.value = false
    editing.value = false
    loadModels()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const testModel = async (row) => {
  testPrompt.value = '你好,请介绍你自己 (测试消息,10 字以内回答)'
  testResult.value = ''
  showTestDialog.value = true
  testing.value = true
  try {
    const res = await modelApi.chat({
      model: row.code,
      messages: [{ role: 'user', content: testPrompt.value }]
    })
    testResult.value = JSON.stringify(res, null, 2)
  } catch (e) {
    testResult.value = '❌ ' + e.message
  } finally {
    testing.value = false
  }
}

onMounted(loadModels)
</script>

<style lang="scss" scoped>
.model-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.test-result { background: #f5f7fa; padding: 12px; border-radius: 4px; max-height: 400px; overflow: auto; white-space: pre-wrap; }
</style>
