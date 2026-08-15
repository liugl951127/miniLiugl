<!-- @file memory/Index.vue - 记忆中心 V6.8.12 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🧠 记忆中心</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadFacts">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>添加记忆
        </el-button>
      </div>
    </div>

    <el-alert title="记忆数据用于 Agent 上下文增强，支持长期知识存储和语义检索" type="info" :closable="false" style="margin-bottom:16px" />

    <!-- 统计 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#409eff">{{ facts.length }}</div>
        <div style="font-size:12px;color:#909399">记忆总数</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#67c23a">{{ userFacts }}</div>
        <div style="font-size:12px;color:#909399">用户记忆</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ systemFacts }}</div>
        <div style="font-size:12px;color:#909399">系统记忆</div>
      </el-card></el-col>
      <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
        <div style="font-size:22px;font-weight:700;color:#909399">{{ contextFacts }}</div>
        <div style="font-size:12px;color:#909399">上下文</div>
      </el-card></el-col>
    </el-row>

    <!-- 搜索 & 筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <el-input v-model="keyword" size="small" placeholder="搜索记忆内容…" style="width:220px" clearable @change="loadFacts">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="category" size="small" style="width:120px" clearable placeholder="全部分类" @change="loadFacts">
        <el-option label="用户" value="user" />
        <el-option label="系统" value="system" />
        <el-option label="上下文" value="context" />
      </el-select>
      <el-select v-model="typeFilter" size="small" style="width:120px" clearable placeholder="全部类型" @change="loadFacts">
        <el-option label="短期" value="short" />
        <el-option label="长期" value="long" />
      </el-select>
    </div>

    <el-table :data="facts" v-loading="loading" stripe>
      <el-table-column label="键" width="200">
        <template #default="{ row }">
          <code style="font-size:12px">{{ row.key || row.factKey || '-' }}</code>
        </template>
      </el-table-column>
      <el-table-column label="内容">
        <template #default="{ row }">
          <div class="fact-content">{{ row.value || row.content || row.text || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'long' ? 'success' : 'info'">{{ row.type === 'long' ? '长期' : '短期' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.source === 'manual' ? 'primary' : 'info'">{{ row.source === 'manual' ? '手动' : '自动' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="160" />
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button size="small" link @click="viewDetail(row)">详情</el-button>
          <el-button size="small" link type="danger" @click="deleteFact(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑记忆' : '添加记忆'" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="键 (Key)" required>
          <el-input v-model="form.key" placeholder="如: user_preference_topic" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.value" type="textarea" :rows="5" placeholder="记忆的具体内容…" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width:100%">
            <el-option label="用户" value="user" />
            <el-option label="系统" value="system" />
            <el-option label="上下文" value="context" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="short">短期记忆</el-radio>
            <el-radio value="long">长期记忆</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="来源">
          <el-radio-group v-model="form.source">
            <el-radio value="manual">手动添加</el-radio>
            <el-radio value="auto">自动生成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'记忆详情'" width="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="键">{{ detail.key || detail.factKey }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.type === 'long' ? '长期记忆' : '短期记忆' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <pre style="white-space:pre-wrap;font-size:13px">{{ detail.value || detail.content || detail.text }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recentLongTerm, storeLongTerm, deleteLongTerm } from '@/api/memory'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const facts = ref([])
const loading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const keyword = ref('')
const category = ref('')
const typeFilter = ref('')

const form = reactive({ key: '', value: '', category: 'context', type: 'short', source: 'manual' })

const userFacts = computed(() => facts.value.filter(f => f.category === 'user').length)
const systemFacts = computed(() => facts.value.filter(f => f.category === 'system').length)
const contextFacts = computed(() => facts.value.filter(f => f.category === 'context').length)

async function loadFacts() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (category.value) params.category = category.value
    const r = await recentLongTerm(params, 200)
    let data = r.data || []
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
  } catch { facts.value = [] }
  finally { loading.value = false }
}

function openAdd() {
  Object.assign(form, { key: '', value: '', category: 'context', type: 'short', source: 'manual' })
  formVisible.value = true
}

async function saveFact() {
  if (!form.key?.trim()) { ElMessage.warning('请填写键名'); return }
  if (!form.value?.trim()) { ElMessage.warning('请填写内容'); return }
  try {
    await storeLongTerm({ key: form.key, value: form.value, category: form.category, type: form.type, source: form.source })
    ElMessage.success('保存成功')
    formVisible.value = false
    loadFacts()
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '')) }
}

async function deleteFact(f) {
  await ElMessageBox.confirm('确认删除该记忆？', '提示', { type: 'warning' })
  try {
    await deleteLongTerm(f.id, null)
    ElMessage.success('已删除')
    loadFacts()
  } catch { ElMessage.error('删除失败') }
}

function viewDetail(f) {
  detail.value = f
  detailVisible.value = true
}

onMounted(loadFacts)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.fact-content {
  font-size: 13px; color: #606266;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400px;
}
</style>
