<!-- @file memory/Index.vue - 记忆中心 V7.0 (UX 强化) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🧠 记忆中心</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" :loading="loading" @click="loadFacts">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" :loading="creating" @click="openAdd">
          <el-icon><Plus /></el-icon>添加记忆
        </el-button>
      </div>
    </div>

    <el-alert title="记忆数据用于 Agent 上下文增强，支持长期知识存储和语义检索" type="info" :closable="false" style="margin-bottom:16px" />

    <!-- 统计 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="hover">
          <div style="font-size:22px;font-weight:700;color:#409eff">{{ facts.length }}</div>
          <div style="font-size:12px;color:#909399">记忆总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="hover">
          <div style="font-size:22px;font-weight:700;color:#67c23a">{{ userFacts }}</div>
          <div style="font-size:12px;color:#909399">用户记忆</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="hover">
          <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ systemFacts }}</div>
          <div style="font-size:12px;color:#909399">系统记忆</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="hover">
          <div style="font-size:22px;font-weight:700;color:#909399">{{ contextFacts }}</div>
          <div style="font-size:12px;color:#909399">上下文</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索 & 筛选 (P0 竞态修复: 加 @input 防抖) -->
    <div class="filter-bar">
      <el-input v-model="keyword" size="small" placeholder="搜索记忆内容…" style="width:240px" clearable @input="onSearchInput" @change="loadFacts" @keyup.enter="loadFacts">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="category" size="small" style="width:130px" clearable placeholder="全部分类" @change="loadFacts">
        <el-option label="用户" value="user" />
        <el-option label="系统" value="system" />
        <el-option label="上下文" value="context" />
      </el-select>
      <el-select v-model="typeFilter" size="small" style="width:130px" clearable placeholder="全部类型" @change="loadFacts">
        <el-option label="短期" value="short" />
        <el-option label="长期" value="long" />
      </el-select>
      <el-button size="small" @click="resetFilter">重置筛选</el-button>
    </div>

    <el-table
      :data="facts"
      v-loading="loading"
      stripe
      empty-text="暂无记忆记录"
    >
      <el-table-column label="键" width="200">
        <template #default="{ row }">
          <code style="font-size:12px">{{ row.key || row.factKey || '-' }}</code>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="240">
        <template #default="{ row }">
          <div class="fact-content">{{ row.value || row.content || row.text || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'long' ? 'success' : 'info'">{{ row.type === 'long' ? '长期' : '短期' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ categoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.source === 'manual' ? 'primary' : 'info'">{{ row.source === 'manual' ? '手动' : '自动' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="170" />
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link @click="viewDetail(row)">详情</el-button>
          <el-button size="small" link type="primary" :loading="editingId === row.id" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" link type="danger" :loading="deletingId === row.id" @click="deleteFact(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty
      v-if="!loading && facts.length === 0"
      :description="keyword || category || typeFilter ? '没有匹配的记录，换个筛选条件试试' : '还没有记忆，点击右上角「添加记忆」开始'"
      :image-size="80"
      style="margin-top:24px"
    />

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑记忆' : '添加记忆'"
      width="560px"
      destroy-on-close
      :close-on-click-modal="!saving"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
        @submit.prevent="saveFact"
      >
        <el-form-item label="键 (Key)" prop="key">
          <el-input v-model="form.key" placeholder="如: user_preference_topic" maxlength="100" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="内容" prop="value">
          <el-input v-model="form.value" type="textarea" :rows="5" placeholder="记忆的具体内容…" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width:100%">
            <el-option label="用户" value="user" />
            <el-option label="系统" value="system" />
            <el-option label="上下文" value="context" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="short">短期记忆</el-radio>
            <el-radio value="long">长期记忆</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="form.source">
            <el-radio value="manual">手动添加</el-radio>
            <el-radio value="auto">自动生成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false" :disabled="saving">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="记忆详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="键">{{ detail.key || detail.factKey }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ categoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.type === 'long' ? '长期记忆' : '短期记忆' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source === 'manual' ? '手动添加' : '自动生成' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <pre class="content-pre">{{ detail.value || detail.content || detail.text }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recentLongTerm, storeLongTerm, deleteLongTerm } from '@/api/memory'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { debounce } from '@/utils/debounce'

const facts = ref([])
const loading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const keyword = ref('')
const category = ref('')
const typeFilter = ref('')
const formRef = ref(null)
const saving = ref(false)
const creating = ref(false)
const editingId = ref(null)
const deletingId = ref(null)

const form = reactive({
  id: null,
  key: '',
  value: '',
  category: 'context',
  type: 'short',
  source: 'manual'
})

const formRules = {
  key: [
    { required: true, message: '请输入键名', trigger: 'blur' },
    { min: 1, max: 100, message: '键名长度应在 1-100 个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '键名只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  value: [
    { required: true, message: '请填写内容', trigger: 'blur' },
    { min: 1, max: 1000, message: '内容长度应在 1-1000 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  type: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  source: [
    { required: true, message: '请选择来源', trigger: 'change' }
  ]
}

const userFacts = computed(() => facts.value.filter(f => f.category === 'user').length)
const systemFacts = computed(() => facts.value.filter(f => f.category === 'system').length)
const contextFacts = computed(() => facts.value.filter(f => f.category === 'context').length)

function categoryLabel(c) {
  return { user: '用户', system: '系统', context: '上下文' }[c] || (c || '-')
}

function resetFilter() {
  keyword.value = ''
  category.value = ''
  typeFilter.value = ''
  loadFacts()
}

// P0 竞态修复: 取消上一次未完成的请求
let loadFactsController = null
async function loadFacts() {
  if (loadFactsController) {
    try { loadFactsController.abort() } catch (e) { /* ignore */ }
  }
  loadFactsController = new AbortController()
  const signal = loadFactsController.signal
  loading.value = true
  try {
    // P0 竞态修复: 通过 axios config 传递 signal 实现请求取消
    const r = await recentLongTerm(undefined, 200, { signal })
    if (signal.aborted) return
    let data = r.data || []
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      data = data.filter(f => (f.value || f.content || f.text || '').toLowerCase().includes(kw))
    }
    if (category.value) data = data.filter(f => f.category === category.value)
    if (typeFilter.value) data = data.filter(f => f.type === typeFilter.value)
    facts.value = data.map((item, i) => ({
      id: item.id || i,
      key: item.key || item.factKey || '',
      value: item.value || item.content || item.text || '',
      category: item.category || 'context',
      type: item.type || 'short',
      source: item.source || 'auto',
      createdAt: item.createdAt || '',
      updatedAt: item.updatedAt || item.updated_at || '',
    }))
  } catch (e) {
    if (e?.name === 'AbortError' || signal.aborted) return
    facts.value = []
    ElMessage.error('加载记忆失败：' + (e?.message || '未知错误'))
  } finally {
    if (!signal.aborted) loading.value = false
    if (loadFactsController && loadFactsController.signal === signal) {
      loadFactsController = null
    }
  }
}

// P0 竞态修复: 搜索防抖
const debouncedLoadFacts = debounce(() => loadFacts(), 300)
function onSearchInput() {
  debouncedLoadFacts()
}

function openAdd() {
  Object.assign(form, { id: null, key: '', value: '', category: 'context', type: 'short', source: 'manual' })
  formVisible.value = true
  creating.value = true
  setTimeout(() => {
    formRef.value?.clearValidate()
    creating.value = false
  }, 0)
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    key: row.key,
    value: row.value,
    category: row.category,
    type: row.type,
    source: row.source
  })
  editingId.value = row.id
  formVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

async function saveFact() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (_) {
    ElMessage.warning('请检查表单填写')
    return
  }
  saving.value = true
  try {
    await storeLongTerm({
      key: form.key.trim(),
      value: form.value.trim(),
      category: form.category,
      type: form.type,
      source: form.source
    })
    ElMessage.success(form.id ? '更新成功' : '保存成功')
    formVisible.value = false
    editingId.value = null
    await loadFacts()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function deleteFact(f) {
  try {
    await ElMessageBox.confirm(
      `确认删除记忆「${f.key || f.factKey}」？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch (_) {
    return
  }
  deletingId.value = f.id
  try {
    await deleteLongTerm(f.id, null)
    ElMessage.success('已删除')
    await loadFacts()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || '未知错误'))
  } finally {
    deletingId.value = null
  }
}

function viewDetail(f) {
  detail.value = f
  detailVisible.value = true
}

onMounted(loadFacts)

// P0 内存泄漏/竞态修复: 组件卸载时取消未完成的请求
onBeforeUnmount(() => {
  if (loadFactsController) {
    try { loadFactsController.abort() } catch (e) { /* ignore */ }
    loadFactsController = null
  }
  debouncedLoadFacts.cancel()
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
.filter-bar {
  display: flex; gap: 8px; margin-bottom: 12px; align-items: center;
}
.fact-content {
  font-size: 13px; color: #606266;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400px;
}
.content-pre {
  white-space: pre-wrap;
  font-size: 13px;
  word-break: break-all;
  margin: 0;
  font-family: inherit;
}
</style>
