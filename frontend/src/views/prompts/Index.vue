<!-- @file prompts/Index.vue - Prompt 模板中心 V6.8.12 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>💬 Prompt 模板中心</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadPrompts">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新建模板
        </el-button>
      </div>
    </div>

    <el-row :gutter="12">
      <!-- 左侧: 列表 -->
      <el-col :span="14">
        <!-- 搜索 & 筛选 -->
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="keyword" size="small" placeholder="搜索模板名称或描述…" style="width:200px" clearable @change="loadPrompts">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterCategory" size="small" style="width:120px" clearable placeholder="全部分类" @change="loadPrompts">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="filterTag" size="small" style="width:120px" clearable placeholder="全部标签" @change="loadPrompts">
            <el-option v-for="t in allTags" :key="t" :label="t" :value="t" />
          </el-select>
        </div>

        <el-table :data="prompts" v-loading="loading" stripe>
          <el-table-column type="selection" width="40" />
          <el-table-column prop="name" label="名称" width="150">
            <template #default="{ row }">
              <div style="font-weight:600">{{ row.name }}</div>
              <div style="font-size:11px;color:#909399">{{ row.author || '系统' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="likes" label="❤" width="60" align="center">
            <template #default="{ row }">{{ row.likes || 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="preview(row)">预览</el-button>
              <!-- P2-2: 复制按钮统一 -->
              <el-button size="small" link type="success" @click="usePrompt(row)">
                <el-icon><CopyDocument /></el-icon>复制
              </el-button>
              <el-button size="small" link @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>

      <!-- 右侧: 预览/测试 -->
      <el-col :span="10">
        <el-card title="模板预览" body-style="padding:16px">
          <template #header><span>模板预览 & 测试</span></template>
          <div v-if="!selectedPrompt" style="text-align:center;color:#909399;padding:40px">
            点击左侧「预览」查看模板详情
          </div>
          <div v-else>
            <div style="margin-bottom:12px">
              <div style="font-weight:600;font-size:15px">{{ selectedPrompt.name }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">{{ selectedPrompt.description }}</div>
              <div style="margin-top:6px;display:flex;gap:4px;flex-wrap:wrap">
                <el-tag size="small">{{ selectedPrompt.category }}</el-tag>
                <el-tag v-for="t in (selectedPrompt.tags || [])" :key="t" size="small" type="info">{{ t }}</el-tag>
              </div>
            </div>

            <!-- 变量提取 -->
            <div v-if="templateVars.length" style="margin-bottom:12px">
              <div style="font-size:12px;font-weight:600;margin-bottom:8px;color:#409eff">🔧 变量填写</div>
              <el-form label-width="80px" size="small">
                <el-form-item v-for="v in templateVars" :key="v" :label="v">
                  <el-input v-model="varValues[v]" :placeholder="`输入 ${v}`" />
                </el-form-item>
              </el-form>
            </div>

            <!-- 模板内容预览 -->
            <div style="margin-bottom:12px">
              <div style="font-size:12px;font-weight:600;margin-bottom:6px">📝 模板内容</div>
              <div class="template-preview" v-html="highlightTemplate(selectedPrompt.template || '')"></div>
            </div>

            <!-- 渲染后预览 -->
            <div v-if="templateVars.length" style="margin-bottom:12px">
              <div style="font-size:12px;font-weight:600;margin-bottom:6px">👁 渲染预览</div>
              <div class="rendered-preview">{{ renderTemplate() }}</div>
            </div>

            <div style="display:flex;gap:8px">
              <!-- P2-2: 复制按钮统一 -->
              <el-button type="primary" size="small" @click="usePrompt(selectedPrompt)">
                <el-icon><CopyDocument /></el-icon>复制到剪贴板
              </el-button>
              <el-button size="small" @click="openEdit(selectedPrompt)">编辑模板</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑模板' : '新建模板'" width="680px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="给模板起个名字" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width:100%" allow-create filterable>
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简短描述模板用途…" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple style="width:100%" allow-create filterable placeholder="添加标签">
            <el-option v-for="t in allTags" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-input v-model="form.template" type="textarea" :rows="10"
            placeholder="使用 {变量名} 占位，例如：帮我分析 {topic} 这个主题，给出 {count} 个关键点。"
            class="template-textarea" />
          <div style="font-size:11px;color:#909399;margin-top:4px">
            支持变量占位：<code>{变量名}</code> · 支持 Markdown 格式
          </div>
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="form.model" style="width:100%" clearable placeholder="不限制">
            <el-option label="GPT-4o" value="gpt-4o" />
            <el-option label="Claude-3.5" value="claude-3.5" />
            <el-option label="DeepSeek" value="deepseek" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch v-model="form.public" />
          <span style="margin-left:8px;font-size:12px;color:#909399">公开模板可供所有用户使用</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrompt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { promptApi } from '@/api/prompt'
import { Plus, Refresh, Search, CopyDocument } from '@element-plus/icons-vue'

const prompts = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = ref({})
const selectedPrompt = ref(null)
const keyword = ref('')
const filterCategory = ref('')
const filterTag = ref('')
const varValues = reactive({})
const allTags = ref(['system', 'user', 'assistant', 'few-shot', 'chain-of-thought', 'role-play'])

const categories = ['通用', '代码', '写作', '分析', '客服', 'RAG', '教育', '办公', '营销', '技术']

const templateVars = computed(() => {
  const t = selectedPrompt.value?.template || ''
  const matches = t.match(/\{([^}]+)\}/g) || []
  return [...new Set(matches.map(m => m.slice(1, -1)))]
})

function highlightTemplate(t) {
  return t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\{([^}]+)\}/g, '<code class="var-token">{$1}</code>')
    .replace(/\n/g, '<br>')
}

function renderTemplate() {
  let t = selectedPrompt.value?.template || ''
  for (const [k, v] of Object.entries(varValues)) {
    t = t.replace(new RegExp(`\\{${k}\\}`, 'g'), v || `<span class="var-placeholder">{${k}}</span>`)
  }
  return t
}

async function loadPrompts() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (filterCategory.value) params.category = filterCategory.value
    const r = await promptApi.list(params)
    prompts.value = r.data?.list || r.data || []
  } catch { prompts.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  form.value = { category: '通用', tags: [], public: false, model: '' }
  formVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  formVisible.value = true
}

function preview(row) {
  selectedPrompt.value = row
  Object.keys(varValues).forEach(k => delete varValues[k])
}

async function savePrompt() {
  if (!form.value.name?.trim()) { ElMessage.warning('请填写模板名称'); return }
  if (!form.value.template?.trim()) { ElMessage.warning('请填写模板内容'); return }
  try {
    if (form.value.id) {
      await promptApi.update(form.value.id, form.value)
    } else {
      await promptApi.create(form.value)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadPrompts()
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '')) }
}

function usePrompt(p) {
  const content = p.template || ''
  navigator.clipboard.writeText(content)
  ElMessage.success('已复制')
}

onMounted(loadPrompts)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.template-preview {
  background: #1e293b; color: #a5f3fc; padding: 12px; border-radius: 6px;
  font-size: 13px; font-family: monospace; max-height: 200px; overflow: auto;
  :deep(.var-token) { color: #fbbf24; background: rgba(251, 191, 36, 0.15); padding: 1px 4px; border-radius: 3px; }
}
.rendered-preview {
  background: #f0f9eb; color: #333; padding: 12px; border-radius: 6px;
  font-size: 13px; white-space: pre-wrap; max-height: 200px; overflow: auto;
  border: 1px solid #e1f3d8;
  :deep(.var-placeholder) { color: #909399; font-style: italic; border-bottom: 1px dashed #ccc; }
}
:deep(.template-textarea textarea) { font-family: monospace; font-size: 13px; }
</style>
