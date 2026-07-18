<!--
  @file views/prompts/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="prompts-page">
    <!-- 顶部 -->
    <div class="page-header">
      <div>
        <h1>📝 {{ t('prompt.title') }} <span class="badge">V4.3</span></h1>
        <p class="sub">{{ t('prompt.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openEditor()">+ {{ t('prompt.newTemplate') }}</el-button>
    </div>

    <!-- 过滤器 -->
    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-input v-model="searchKw" placeholder="搜索名称或描述..." clearable @change="loadList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterCategory" :placeholder="t('prompt.allCategories')" clearable @change="loadList" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-col>
        <el-col :span="12" style="text-align:right">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="grid">{{ t('prompt.cardView') }}</el-radio-button>
            <el-radio-button value="list">{{ t('prompt.listView') }}</el-radio-button>
          </el-radio-group>
        </el-col>
      </el-row>
    </el-card>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'grid'" class="template-grid">
      <el-card v-for="tpl in templates" :key="tpl.id" shadow="hover" class="template-card">
        <div class="tpl-header">
          <span class="tpl-icon">{{ iconOf(tpl.category) }}</span>
          <div class="tpl-title-wrap">
            <h3 class="tpl-name">{{ tpl.name }}</h3>
            <span class="tpl-meta">
              <el-tag size="small" :type="tagType(tpl.category)">{{ tpl.category }}</el-tag>
              <span v-if="tpl.isPublic" class="public-badge">🌐 {{ t('prompt.public') }}</span>
              <span v-else class="private-badge">🔒 {{ t('prompt.private') }}</span>
            </span>
          </div>
        </div>
        <p class="tpl-desc">{{ tpl.description || '—' }}</p>

        <!-- 变量预览 -->
        <div v-if="extractVars(tpl.content).length > 0" class="tpl-vars">
          <el-tag v-for="v in extractVars(tpl.content)" :key="v" size="small" type="info" style="margin-right:4px">
            {{ wrapVar(v) }}
          </el-tag>
        </div>

        <div class="tpl-footer">
          <span class="use-count">📊 {{ tpl.useCount || 0 }} {{ t('prompt.uses') }}</span>
          <div style="flex:1"></div>
          <el-button size="small" @click="openUse(tpl)">▶ {{ t('prompt.use') }}</el-button>
          <el-button size="small" type="primary" :icon="Edit" @click="openEditor(tpl)" />
          <el-button v-if="isOwner(tpl)" size="small" type="danger" :icon="Delete" @click="handleDelete(tpl)" />
        </div>
      </el-card>
    </div>

    <!-- 列表视图 -->
    <el-card v-else>
      <el-table :data="templates" stripe>
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.category)">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="isPublic" :label="t('prompt.visibility')" width="80">
          <template #default="{ row }">
            {{ row.isPublic ? '🌐 ' + t('prompt.public') : '🔒 ' + t('prompt.private') }}
          </template>
        </el-table-column>
        <el-table-column prop="useCount" label="使用次数" width="100" />
        <el-table-column prop="creatorName" label="创建者" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openUse(row)">▶ {{ t('prompt.use') }}</el-button>
            <el-button size="small" :icon="Edit" @click="openEditor(row)" />
            <el-button v-if="isOwner(row)" size="small" type="danger" :icon="Delete" @click="handleDelete(row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadList"
        @current-change="loadList"
      />
    </div>

    <!-- 变量填值对话框 -->
    <el-dialog v-model="showUseDialog" :title="t('prompt.fillVariables')" width="560px">
      <div v-if="activeTemplate">
        <p class="use-desc">{{ activeTemplate.description }}</p>
        <el-divider />
        <el-form label-position="top">
          <el-form-item v-for="v in extractVars(activeTemplate.content)" :key="v" :label="v">
            <el-input v-model="varValues[v]" :placeholder="`请输入 ${v}`" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <el-divider />
        <p class="preview-label">{{ t('prompt.preview') }}:</p>
        <el-input v-model="previewContent" type="textarea" :rows="6" readonly class="preview-box" />
      </div>
      <template #footer>
        <el-button @click="showUseDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" @click="goUse">🚀 {{ t('prompt.fillToChat') }}</el-button>
      </template>
    </el-dialog>

    <!-- 编辑器对话框 -->
    <el-dialog v-model="showEditor" :title="editingTemplate?.id ? t('prompt.editTemplate') : t('prompt.newTemplate')" width="640px">
      <el-form :model="editorForm" label-position="top">
        <el-form-item :label="t('prompt.templateName')" required>
          <el-input v-model="editorForm.name" placeholder="如: 翻译助手" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input v-model="editorForm.description" :placeholder="t('prompt.descPlaceholder')" maxlength="500" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('common.category')" required>
          <el-select v-model="editorForm.category" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in ALL_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('prompt.templateContent')" required>
          <el-input v-model="editorForm.content" type="textarea" :rows="8" placeholder="支持 {{变量名}} 占位符..."
            @input="updateVarList" />
          <div class="hint">{{ t('prompt.varsAutoExtract') }}</div>
        </el-form-item>
        <el-form-item :label="t('prompt.isPublic')">
          <el-switch v-model="editorForm.isPublic" :active-text="t('prompt.publicDesc')" :inactive-text="t('prompt.privateDesc')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditor = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { promptApi } from '@/api/prompt'
import { useUserStore } from '@/store/user'
import { t } from '@/i18n'

const router = useRouter()
const userStore = useUserStore()

const ALL_CATEGORIES = ['翻译', '代码', '写作', '分析', '营销', '客服', '其他']
const CATEGORY_ICONS = { '翻译': '🌍', '代码': '💻', '写作': '✍️', '分析': '📊', '营销': '📣', '客服': '🎧', '其他': '📋' }

const searchKw = ref('')
const filterCategory = ref('')
const viewMode = ref('grid')
const current = ref(1)
const size = ref(12)
const total = ref(0)
const templates = ref([])
const categories = ref([])
const showUseDialog = ref(false)
const showEditor = ref(false)
const activeTemplate = ref(null)
const editingTemplate = ref(null)
const varValues = reactive({})
const previewContent = ref('')
const editorForm = reactive({ name: '', description: '', category: '其他', content: '', isPublic: false })

onMounted(async () => {
  await Promise.all([loadList(), loadCategories()])
})

async function loadList() {
  try {
    const res = await promptApi.list({ current: current.value, size: size.value,
      category: filterCategory.value || undefined, keyword: searchKw.value || undefined })
    if (res && res.records) {
      templates.value = res.records
      total.value = res.total || 0
    } else if (res && res.data) {
      const d = res.data
      templates.value = d.records || []
      total.value = d.total || 0
    }
  } catch (e) {
    ElMessage.error(t('prompt.loadFailed') + e.message)
  }
}

async function loadCategories() {
  try {
    const res = await promptApi.categories()
    categories.value = res.data || res || []
  } catch (e) { /* ignore */ }
}

function iconOf(cat) { return CATEGORY_ICONS[cat] || '📋' }
function tagType(cat) {
  const map = { '翻译': '', '代码': 'info', '写作': 'success', '分析': 'warning', '营销': 'danger', '客服': '', '其他': 'info' }
  return map[cat] || 'info'
}

function extractVars(content) {
  if (!content) return []
  const matches = content.match(/\{\{([^}]+)\}\}/g) || []
  return [...new Set(matches.map(m => m.replace(/\{\{|\}\}/g, '').trim()))]
}

function wrapVar(v) { return '{{' + v + '}}' }

function isOwner(tpl) {
  return tpl.creatorId === userStore.profile?.id || tpl.creatorId === 1
}

function openUse(tpl) {
  activeTemplate.value = tpl
  Object.keys(varValues).forEach(k => delete varValues[k])
  previewContent.value = tpl.content || ''
  showUseDialog.value = true
}

function updateVarList() {
  // reactive watch handled below
}

watch([editorForm, () => editorForm.content], () => {
  const vars = extractVars(editorForm.content)
  vars.forEach(v => { if (!editorForm._vars) editorForm._vars = [] })
}, { deep: true })

watch(() => editorForm.content, (val) => {
  // Update preview in use dialog if open
  if (showUseDialog.value && activeTemplate.value) {
    let content = activeTemplate.value.content
    Object.entries(varValues).forEach(([k, v]) => {
      content = content.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v || '')
    })
    previewContent.value = content
  }
})

watch(varValues, (vals) => {
  if (!activeTemplate.value) return
  let content = activeTemplate.value.content
  Object.entries(vals).forEach(([k, v]) => {
    content = content.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v || '')
  })
  previewContent.value = content
}, { deep: true })

function goUse() {
  showUseDialog.value = false
  router.push({ path: '/chat', query: { prompt: encodeURIComponent(previewContent.value) } })
}

function openEditor(tpl = null) {
  editingTemplate.value = tpl
  if (tpl) {
    Object.assign(editorForm, {
      name: tpl.name, description: tpl.description || '',
      category: tpl.category || '其他',
      content: tpl.content || '', isPublic: tpl.isPublic || false
    })
  } else {
    Object.assign(editorForm, { name: '', description: '', category: '其他', content: '', isPublic: false })
  }
  showEditor.value = true
}

async function handleSave() {
  if (!editorForm.name || !editorForm.content) {
    ElMessage.warning(t('prompt.nameAndContentRequired'))
    return
  }
  try {
    if (editingTemplate.value?.id) {
      await promptApi.update(editingTemplate.value.id, editorForm)
      ElMessage.success(t('prompt.updateSuccess'))
    } else {
      await promptApi.create(editorForm)
      ElMessage.success(t('prompt.createSuccess'))
    }
    showEditor.value = false
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || t('prompt.saveFailed'))
  }
}

async function handleDelete(tpl) {
  try {
    await ElMessageBox.confirm(t('prompt.deleteConfirm') + '「' + tpl.name + '」' + t('prompt.deleteConfirmSuffix'), t('prompt.deleteConfirmTitle'), { type: 'warning' })
    await promptApi.remove(tpl.id)
    ElMessage.success(t('prompt.deleted'))
    await loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('prompt.deleteFailed'))
  }
}
</script>

<style scoped>
.prompts-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 24px; }
.sub { margin: 4px 0 0; color: #888; font-size: 14px; }
.badge { background: #f0f9eb; color: #67c23a; padding: 2px 8px; border-radius: 8px; font-size: 12px; margin-left: 8px; }
.filter-card { margin-bottom: 20px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; margin-bottom: 20px; }
.template-card { cursor: default; }
.tpl-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 8px; }
.tpl-icon { font-size: 28px; flex-shrink: 0; }
.tpl-title-wrap { flex: 1; }
.tpl-name { margin: 0 0 4px; font-size: 15px; font-weight: 600; }
.tpl-meta { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.public-badge { font-size: 12px; color: #67c23a; }
.private-badge { font-size: 12px; color: #909399; }
.tpl-desc { color: #666; font-size: 13px; margin: 0 0 10px; line-height: 1.5; }
.tpl-vars { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
.tpl-footer { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.use-count { font-size: 12px; color: #aaa; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
.use-desc { color: #666; font-size: 14px; margin: 0; }
.preview-label { font-size: 13px; color: #888; margin-bottom: 6px; }
.preview-box { font-family: monospace; font-size: 13px; background: #f5f7fa; }
.hint { font-size: 12px; color: #aaa; margin-top: 4px; }
</style>
