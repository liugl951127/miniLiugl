<!--
  @file model/Trained.vue - 训练模型管理 (V8.0)
  路由: /model/trained
  功能: 自研模型 + 训练任务 + 添加/编辑表单
-->
<template>
  <div v-loading="pageLoading">
    <div class="page-header">
      <h3>🧬 训练模型管理</h3>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadAll" :loading="pageLoading">刷新</el-button>
        <el-button size="small" type="primary" :icon="Plus" @click="openTrainedForm()">添加训练模型</el-button>
      </div>
    </div>

    <!-- 自研模型专区 -->
    <el-card body-style="padding:0" style="margin-bottom:16px;border:2px solid var(--el-color-primary)">
      <div style="padding:16px 20px;background:linear-gradient(135deg,#f0f7ff 0%,#e8f4fd 100%);border-radius:8px 8px 0 0">
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
          <span style="font-size:24px">🏷️</span>
          <div>
            <div style="font-size:16px;font-weight:700;color: var(--el-color-primary)">自研模型专区</div>
            <div style="font-size:12px;color: var(--el-color-primary)">平台自主训练 · 完全自主可控 · 行业深度定制</div>
          </div>
          <el-tag v-if="trainedEnabled" type="success" style="margin-left:auto" size="large">🟢 {{ trainedEnabled }} 个已启用</el-tag>
          <el-tag v-else type="warning" style="margin-left:auto" size="large">⚠️ 暂无启用</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 统计 -->
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6">
        <el-card body-style="text-align:center;padding:12px" shadow="hover">
          <div style="font-size:24px;font-weight:700;color:var(--el-color-primary)">{{ trainedEnabled }}</div>
          <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">启用模型</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="text-align:center;padding:12px" shadow="hover">
          <div style="font-size:24px;font-weight:700;color:var(--el-color-success)">{{ trainedAccuracy }}%</div>
          <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">平均准确率</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="text-align:center;padding:12px" shadow="hover">
          <div style="font-size:24px;font-weight:700;color:var(--el-color-warning)">{{ trainedCalls }}</div>
          <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">总调用次数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="text-align:center;padding:12px" shadow="hover">
          <div style="font-size:24px;font-weight:700;color:var(--el-color-info)">{{ trainingRecords.length }}</div>
          <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">训练任务</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索 -->
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <el-input v-model="trainedSearch" placeholder="搜索模型名称/代码/行业" clearable style="max-width:400px" />
    </div>

    <!-- 训练模型列表 -->
    <el-card style="margin-bottom:16px">
      <template #header><span>📋 训练模型列表</span></template>
      <el-table :data="filteredTrainedModels" v-loading="trainedLoading" stripe>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
            <el-tag v-if="row.industry" size="small" type="info" style="margin-left:6px">{{ row.industry }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="代码" min-width="120" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="视觉" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.vision" type="success" size="small">✓</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="上下文" width="100">
          <template #default="{ row }">{{ row.contextWindow || 4096 }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleTrained(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openTrainedForm(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="confirmDeleteTrained(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 训练任务列表 -->
    <el-card>
      <template #header><span>🏋️ 训练任务</span></template>
      <el-table :data="trainingRecords" stripe>
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="modelName" label="模型" min-width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : row.status === 'running' ? 'warning' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160" />
        <el-table-column prop="duration" label="耗时" width="100" />
      </el-table>
    </el-card>

    <!-- 添加/编辑训练模型弹窗 -->
    <el-dialog v-model="showTrainedForm" :title="trainedForm.id ? '编辑训练模型' : '添加训练模型'" width="560px" destroy-on-close>
      <el-form ref="trainedFormRef" :model="trainedForm" :rules="trainedRules" label-width="100px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="trainedForm.name" placeholder="如：Law-GPT 法律助手" />
        </el-form-item>
        <el-form-item label="模型代码" prop="code">
          <el-input v-model="trainedForm.code" placeholder="如：law-gpt-v1" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select v-model="trainedForm.industry" style="width:100%">
            <el-option v-for="i in industries" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持视觉">
          <el-switch v-model="trainedForm.vision" />
        </el-form-item>
        <el-form-item label="上下文长度">
          <el-input-number v-model="trainedForm.contextWindow" :min="1024" :max="128000" :step="1024" style="width:100%" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="trainedForm.version" placeholder="如：v1.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="trainedForm.description" type="textarea" :rows="2" placeholder="模型简介…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTrainedForm = false">取消</el-button>
        <el-button type="primary" :loading="trainedSaving" @click="saveTrainedModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { trainedModelApi, trainingApi } from '@/api/training'
import { dictApi } from '@/api/dict'

const pageLoading = ref(false)
const trainedModels = ref([])
const trainingRecords = ref([])
const trainedLoading = ref(false)
const trainedSearch = ref('')

// V8.0.3: 字典下拉
const industries = ref([])
dictApi.industries().then(r => industries.value = r.data?.data || r.data || []).catch(() => {})

const filteredTrainedModels = computed(() => {
  const k = trainedSearch.value
  if (!k) return trainedModels.value
  return trainedModels.value.filter(m =>
    (m.name && m.name.includes(k)) ||
    (m.code && m.code.includes(k)) ||
    (m.industry && m.industry.includes(k))
  )
})

const trainedEnabled = computed(() => trainedModels.value.filter(m => m.enabled).length)
const trainedAccuracy = computed(() => {
  const list = trainedModels.value.filter(m => m.accuracy)
  if (!list.length) return 0
  return (list.reduce((s, m) => s + m.accuracy, 0) / list.length * 100).toFixed(1)
})
const trainedCalls = computed(() => trainedModels.value.reduce((s, m) => s + (m.calls || 0), 0))

const showTrainedForm = ref(false)
const trainedSaving = ref(false)
const trainedFormRef = ref(null)
const trainedForm = reactive({
  id: null, name: '', code: '', industry: '通用',
  vision: false, contextWindow: 4096, version: 'v1.0', description: ''
})
const trainedRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
}

async function loadAll() {
  pageLoading.value = true
  try {
    const [models, tasks] = await Promise.all([
      trainedModelApi.list().catch(() => ({ data: [] })),
      trainingApi.list().catch(() => ({ data: [] }))
    ])
    trainedModels.value = models.data || models || []
    trainingRecords.value = tasks.data || tasks || []
  } finally {
    pageLoading.value = false
  }
}

function openTrainedForm(row) {
  if (row) Object.assign(trainedForm, row)
  else Object.assign(trainedForm, { id: null, name: '', code: '', industry: '通用', vision: false, contextWindow: 4096, version: 'v1.0', description: '' })
  showTrainedForm.value = true
}

async function saveTrainedModel() {
  await trainedFormRef.value?.validate()
  trainedSaving.value = true
  try {
    if (trainedForm.id) {
      await trainedModelApi.update(trainedForm.id, trainedForm)
    } else {
      await trainedModelApi.create(trainedForm)
    }
    ElMessage.success('保存成功')
    showTrainedForm.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || ''))
  } finally {
    trainedSaving.value = false
  }
}

async function confirmDeleteTrained(row) {
  await ElMessageBox.confirm(`确定删除模型「${row.name}」?`, '提示', { type: 'warning' })
  await trainedModelApi.remove(row.id)
  ElMessage.success('已删除')
  await loadAll()
}

async function toggleTrained(row) {
  await trainedModelApi.update(row.id, { enabled: row.enabled })
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
