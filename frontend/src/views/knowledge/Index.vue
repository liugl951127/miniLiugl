<!--
  @file views/knowledge/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="rag-page">
    <!-- 顶部统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#5b8def"><Files /></el-icon>
            <div>
              <div class="stat-num">{{ kbs.length }}</div>
              <div class="stat-label">我的知识库</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#67c23a"><Document /></el-icon>
            <div>
              <div class="stat-num">{{ totalDocs }}</div>
              <div class="stat-label">文档总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#e6a23c"><Reading /></el-icon>
            <div>
              <div class="stat-num">{{ publicKbs.length }}</div>
              <div class="stat-label">公开知识库</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#f56c6c"><Histogram /></el-icon>
            <div>
              <div class="stat-num">{{ retrieveCount }}</div>
              <div class="stat-label">本会话检索</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="rag-tabs">
      <!-- Tab 1: 我的知识库 -->
      <el-tab-pane label="📚 我的知识库" name="mine">
        <div class="toolbar">
          <el-button type="primary" @click="showCreateKb = true">
            <el-icon><Plus /></el-icon> 新建知识库
          </el-button>
          <el-input v-model="kbSearch" placeholder="搜索知识库..." clearable style="width: 240px" />
        </div>

        <el-table :data="filteredKbs" v-loading="loading.kbs" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="名称" prop="name" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
              <el-tag v-if="row.visibility === 'public'" type="success" size="small" style="margin-left: 8px">公开</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="描述" prop="description" show-overflow-tooltip min-width="220" />
          <el-table-column label="标签" min-width="160">
            <template #default="{ row }">
              <el-tag v-for="t in (row.tags || '').split(',').filter(Boolean)" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createdAt" width="170" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="selectKb(row)"><el-icon><FolderOpened /></el-icon> 进入</el-button>
              <el-button size="small" @click="editKb(row)"><el-icon><Edit /></el-icon> 编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteKb(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="还没有知识库, 点击右上角新建" />
          </template>
        </el-table>
      </el-tab-pane>

      <!-- Tab 2: 公开知识库 -->
      <el-tab-pane :label="`🌐 公开知识库 (${publicKbs.length})`" name="public">
        <el-table :data="publicKbs" v-loading="loading.public" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="名称" prop="name" min-width="180" />
          <el-table-column label="描述" prop="description" show-overflow-tooltip min-width="280" />
          <el-table-column label="所有者" prop="ownerId" width="100" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="selectKb(row)">使用</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无公开知识库" />
          </template>
        </el-table>
      </el-tab-pane>

      <!-- Tab 3: 检索问答 -->
      <el-tab-pane label="🔍 检索问答" name="retrieve">
        <el-card>
          <el-form :inline="true">
            <el-form-item label="知识库">
              <el-select v-model="retrieveForm.kbId" placeholder="全部" clearable style="width: 200px">
                <el-option label="全部知识库" :value="null" />
                <el-option v-for="kb in kbs" :key="kb.id" :label="kb.name" :value="kb.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="Top K">
              <el-input-number v-model="retrieveForm.topK" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="Prompt 模板">
              <el-select v-model="selectedPromptTpl" placeholder="默认模板" clearable
                style="width: 200px" value-key="id">
                <el-option v-for="t in promptTemplates" :key="t.id" :label="t.name" :value="t" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-input
            v-model="retrieveForm.query"
            type="textarea"
            :rows="3"
            placeholder="输入问题, e.g. 'Liugl-AI 平台支持哪些模型?'"
            @keydown.ctrl.enter="handleRetrieve"
          />
          <div style="margin-top: 12px">
            <el-button type="primary" @click="handleRetrieve" :loading="loading.retrieve">
              <el-icon><Search /></el-icon> 检索
            </el-button>
            <el-button type="success" @click="handleAsk" :loading="loading.ask" :disabled="!retrieveForm.query">
              <el-icon><MagicStick /></el-icon> RAG 问答
            </el-button>
            <span style="margin-left: 12px; color: #909399; font-size: 12px">提示: Ctrl+Enter 快速检索</span>
          </div>

          <!-- 检索结果 -->
          <div v-if="retrieveHits.length" class="hit-section">
            <h4>📄 命中切片 ({{ retrieveHits.length }})</h4>
            <el-card v-for="(hit, i) in retrieveHits" :key="i" class="hit-card" shadow="hover">
              <div class="hit-header">
                <el-tag>片段 {{ i + 1 }}</el-tag>
                <el-tag type="info">相似度: {{ (hit.score || 0).toFixed(3) }}</el-tag>
                <span class="hit-meta">{{ hit.docTitle }} · chunk #{{ hit.chunkIndex }}</span>
              </div>
              <div class="hit-content">{{ hit.text }}</div>
            </el-card>
          </div>

          <!-- 问答结果 -->
          <div v-if="askAnswer" class="answer-section">
            <h4>🤖 RAG 回答</h4>
            <el-card class="answer-card">
              <div class="answer-text">{{ askAnswer.answer }}</div>
              <div v-if="askAnswer.citations && askAnswer.citations.length" class="citations">
                <strong>引用来源:</strong>
                <el-tag v-for="(c, i) in askAnswer.citations" :key="i" size="small" style="margin: 4px 4px 0 0">
                  [{{ i + 1 }}] {{ c.docTitle || c.docId }}
                </el-tag>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 知识库文档管理 (侧滑) -->
    <el-drawer v-model="drawerVisible" :title="`📁 ${currentKb?.name || ''} - 文档管理`" direction="rtl" size="60%">
      <div v-if="currentKb" class="kb-detail">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="知识库 ID">{{ currentKb.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentKb.name }}</el-descriptions-item>
          <el-descriptions-item label="可见性">
            <el-tag :type="currentKb.visibility === 'public' ? 'success' : 'info'">{{ currentKb.visibility }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所有者 ID">{{ currentKb.ownerId }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="t in (currentKb.tags || '').split(',').filter(Boolean)" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentKb.description || '(无)' }}</el-descriptions-item>
        </el-descriptions>

        <div class="doc-toolbar">
          <h4>📄 文档 ({{ docs.length }})</h4>
          <!-- V5.22: 上传进度条 + 取消按钮 -->
          <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress-bar">
            <span class="upload-name">{{ uploadingFileName }}</span>
            <el-progress :percentage="uploadProgress" :stroke-width="8" style="flex:1;margin:0 12px" />
            <el-button size="small" type="danger" @click="cancelUpload">取消</el-button>
          </div>
          <el-upload
            v-else
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="customUpload"
            accept=".txt,.md,.pdf,.docx,.doc,.html"
          >
            <el-button type="primary" :loading="loading.upload">
              <el-icon><Upload /></el-icon> 上传文档
            </el-button>
          </el-upload>
        </div>

        <el-table :data="docs" v-loading="loading.docs" stripe size="small">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
          <el-table-column label="来源" prop="sourceType" width="90" />
          <el-table-column label="切片数" prop="chunkCount" width="80" />
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
          </el-table-column>
          <el-table-column label="上传时间" prop="createdAt" width="160" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="viewChunks(row)">切片</el-button>
              <el-button size="small" @click="renameDoc(row)">重命名</el-button>
              <el-button size="small" type="danger" @click="handleDeleteDoc(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 新建知识库对话框 -->
    <el-dialog v-model="showCreateKb" title="新建知识库" width="500px">
      <el-form :model="newKb" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="newKb.name" placeholder="e.g. 产品手册" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newKb.description" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="newKb.visibility">
            <el-radio value="private">私有</el-radio>
            <el-radio value="public">公开</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="newKb.tags" placeholder="逗号分隔, e.g. AI,产品,手册" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateKb = false">取消</el-button>
        <el-button type="primary" :loading="loading.create" @click="handleCreateKb">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑知识库对话框 -->
    <el-dialog v-model="editKbVisible" title="编辑知识库" width="500px">
      <el-form :model="editKbForm" label-width="80">
        <el-form-item label="名称" required>
          <el-input v-model="editKbForm.name" placeholder="e.g. 产品手册" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editKbForm.description" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="editKbForm.visibility">
            <el-radio value="private">私有</el-radio>
            <el-radio value="public">公开</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editKbForm.tags" placeholder="逗号分隔, e.g. AI,产品" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editKbVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.editKb" @click="doEditKb">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重命名文档对话框 -->
    <el-dialog v-model="renameDocVisible" title="重命名文档" width="420px">
      <el-form label-width="70">
        <el-form-item label="新名称">
          <el-input v-model="renameDocTitle" placeholder="输入文档新名称" maxlength="200" clearable autofocus />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDocVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.renameDoc" @click="doRenameDoc">确认</el-button>
      </template>
    </el-dialog>

    <!-- 切片查看对话框 -->
    <el-dialog v-model="showChunks" :title="`文档切片: ${currentDoc?.title || ''}`" width="700px">
      <el-scrollbar height="500px">
        <el-card v-for="(chunk, i) in chunks" :key="chunk.id || i" class="chunk-card" shadow="hover">
          <div class="chunk-header">
            <el-tag>切片 {{ i + 1 }}</el-tag>
            <span class="chunk-meta">#{{ chunk.chunkIndex }} · {{ chunk.text?.length || 0 }} 字</span>
          </div>
          <div class="chunk-text">{{ chunk.text }}</div>
        </el-card>
      </el-scrollbar>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Files, Document, Reading, Histogram, Plus, FolderOpened, Delete, Search, MagicStick, Upload, Edit, EditPen } from '@element-plus/icons-vue'
import * as ragApi from '@/api/rag'
import { promptApi } from '@/api/prompt'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const ownerId = computed(() => userStore.userInfo?.id || 1)

const activeTab = ref('mine')
const kbs = ref([])
const publicKbs = ref([])
const docs = ref([])
const chunks = ref([])
const currentKb = ref(null)
const currentDoc = ref(null)
const totalDocs = ref(0)
const retrieveCount = ref(0)

const drawerVisible = ref(false)
const showCreateKb = ref(false)
const showChunks = ref(false)
const editKbVisible = ref(false)
const editKbForm = reactive({ id: null, name: '', description: '', visibility: 'private', tags: '' })
const renameDocVisible = ref(false)
const renameDocTitle = ref('')
const renameDocId = ref(null)
const currentEditingKb = ref(null)
const kbSearch = ref('')

const newKb = reactive({ name: '', description: '', visibility: 'private', tags: '' })

const retrieveForm = reactive({ kbId: null, query: '', topK: 5 })
const retrieveHits = ref([])
const askAnswer = ref(null)

// Day 23: Prompt 模板选择 (用于 RAG 自定义 system prompt)
const promptTemplates = ref([])
const selectedPromptTpl = ref(null)

async function loadPromptTemplates() {
  try {
    const r = await promptApi.list({ size: 50 })
    const list = r?.data?.records || r?.data || []
    promptTemplates.value = list
  } catch { /* 模板加载失败不影响 RAG 问答 */ }
}

const loading = reactive({
  kbs: false, public: false, docs: false, upload: false,
  retrieve: false, ask: false, create: false, editKb: false, renameDoc: false
})

// V5.22: 上传进度状态
const uploadProgress = ref(0)
const uploadingFileName = ref('')
let uploadCancel = null

const filteredKbs = computed(() => {
  if (!kbSearch.value) return kbs.value
  const q = kbSearch.value.toLowerCase()
  return kbs.value.filter(k =>
    k.name?.toLowerCase().includes(q) ||
    k.description?.toLowerCase().includes(q)
  )
})

function formatBytes(b) {
  if (!b) return '-'
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

async function loadKbs() {
  loading.kbs = true
  try {
    const res = await ragApi.listMyKbs(ownerId.value)
    kbs.value = res.data?.data || res.data || []
    if (kbs.value.length) totalDocs.value = await countDocs()
  } finally { loading.kbs = false }
}

async function countDocs() {
  let total = 0
  for (const kb of kbs.value.slice(0, 5)) {
    try {
      const r = await ragApi.listDocs(kb.id, 100)
      total += (r.data?.data || r.data || []).length
    } catch {}
  }
  return total
}

async function loadPublicKbs() {
  loading.public = true
  try {
    const res = await ragApi.listPublicKbs()
    publicKbs.value = res.data?.data || res.data || []
  } finally { loading.public = false }
}

async function handleCreateKb() {
  if (!newKb.name.trim()) return ElMessage.warning('请输入名称')
  loading.create = true
  try {
    await ragApi.createKb(ownerId.value, { ...newKb })
    ElMessage.success('创建成功')
    showCreateKb.value = false
    Object.assign(newKb, { name: '', description: '', visibility: 'private', tags: '' })
    await loadKbs()
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.create = false }
}

async function handleDeleteKb(row) {
  await ElMessageBox.confirm(`确认删除知识库 "${row.name}" 及其所有文档?`, '警告', { type: 'warning' })
  try {
    await ragApi.deleteKb(row.id, ownerId.value)
    ElMessage.success('删除成功')
    await loadKbs()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

function editKb(row) {
  currentEditingKb.value = row
  Object.assign(editKbForm, {
    id: row.id,
    name: row.name || '',
    description: row.description || '',
    visibility: row.visibility || 'private',
    tags: row.tags || '',
  })
  editKbVisible.value = true
}

async function doEditKb() {
  if (!editKbForm.name.trim()) return ElMessage.warning('请输入名称')
  loading.editKb = true
  try {
    await ragApi.updateKb(editKbForm.id, ownerId.value, { ...editKbForm })
    ElMessage.success('保存成功')
    editKbVisible.value = false
    await loadKbs()
    if (currentKb.value?.id === editKbForm.id) {
      currentKb.value = { ...currentKb.value, ...editKbForm }
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.editKb = false
  }
}

function renameDoc(row) {
  renameDocId.value = row.id
  renameDocTitle.value = row.title || ''
  renameDocVisible.value = true
}

async function doRenameDoc() {
  if (!renameDocTitle.value.trim()) return ElMessage.warning('请输入新名称')
  loading.renameDoc = true
  try {
    await ragApi.renameDoc(renameDocId.value, ownerId.value, renameDocTitle.value.trim())
    ElMessage.success('重命名成功')
    renameDocVisible.value = false
    await loadDocs()
  } catch (e) {
    ElMessage.error('重命名失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.renameDoc = false
  }
}

async function selectKb(row) {
  currentKb.value = row
  drawerVisible.value = true
  await loadDocs()
}

async function loadDocs() {
  if (!currentKb.value) return
  loading.docs = true
  try {
    const res = await ragApi.listDocs(currentKb.value.id, 100)
    docs.value = res.data?.data || res.data || []
  } finally { loading.docs = false }
}

async function handleDeleteDoc(row) {
  await ElMessageBox.confirm(`确认删除文档 "${row.title}"?`, '警告', { type: 'warning' })
  try {
    await ragApi.deleteDoc(row.id, ownerId.value)
    ElMessage.success('删除成功')
    await loadDocs()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

function beforeUpload(file) {
  const max = 50 * 1024 * 1024
  if (file.size > max) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

async function customUpload({ file }) {
  uploadProgress.value = 0
  uploadingFileName.value = file.name
  loading.upload = true
  let cancelled = false
  try {
    const { promise, cancel } = ragApi.uploadDocWithCancel(
      ownerId.value,
      currentKb.value.id,
      file,
      {
        title: file.name,
        sourceType: 'upload',
        onProgress: (pct) => {
          if (!cancelled) uploadProgress.value = pct
        }
      }
    )
    uploadCancel = () => {
      cancelled = true
      cancel()
    }
    await promise
    if (!cancelled) {
      ElMessage.success('上传成功')
      uploadProgress.value = 0
      uploadingFileName.value = ''
      await loadDocs()
    }
  } catch (e) {
    if (e?.__cancelled || e?.message?.includes('cancel') || e?.name === 'CanceledError') {
      ElMessage.info('上传已取消')
    } else {
      ElMessage.error('上传失败: ' + (e.response?.data?.message || e.message))
    }
    uploadProgress.value = 0
    uploadingFileName.value = ''
  } finally {
    loading.upload = false
    uploadCancel = null
  }
}

function cancelUpload() {
  if (uploadCancel) {
    uploadCancel()
    uploadCancel = null
  }
}

async function viewChunks(row) {
  currentDoc.value = row
  showChunks.value = true
  try {
    const res = await ragApi.listChunks(row.id)
    chunks.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载切片失败')
  }
}

async function handleRetrieve() {
  if (!retrieveForm.query.trim()) return ElMessage.warning('请输入问题')
  loading.retrieve = true
  try {
    const res = await ragApi.retrieve({
      kbId: retrieveForm.kbId,
      query: retrieveForm.query,
      topK: retrieveForm.topK
    })
    retrieveHits.value = res.data?.data || res.data || []
    retrieveCount.value++
    askAnswer.value = null
  } catch (e) {
    ElMessage.error('检索失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.retrieve = false }
}

async function handleAsk() {
  if (!retrieveForm.query.trim()) return ElMessage.warning('请输入问题')
  loading.ask = true
  try {
    const payload = {
      kbId: retrieveForm.kbId,
      question: retrieveForm.query,
      topK: retrieveForm.topK,
    }
    // Day 23: 如果选了 Prompt 模板, 用模板 content 作为 system prompt
    if (selectedPromptTpl.value?.content) {
      payload.systemPrompt = selectedPromptTpl.value.content
    }
    const res = await ragApi.ask(payload)
    askAnswer.value = res.data?.data || res.data || null
    retrieveCount.value++
  } catch (e) {
    ElMessage.error('问答失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.ask = false }
}

onMounted(async () => {
  await Promise.all([loadKbs(), loadPublicKbs(), loadPromptTemplates()])
})
</script>

<style scoped>
.rag-page { padding: 0; }
.stats-row { margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-num { font-size: 24px; font-weight: 600; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.rag-tabs { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.kb-detail { padding: 0 8px; }
.doc-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.doc-toolbar h4 { margin: 0; }
.upload-progress-bar { display: flex; align-items: center; flex: 1; gap: 8px; padding: 6px 12px; background: #f0f9ff; border-radius: 6px; border: 1px solid #d0e8ff; }
.upload-name { font-size: 12px; color: #409eff; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hit-section, .answer-section { margin-top: 20px; }
.hit-section h4, .answer-section h4 { margin-bottom: 12px; color: #303133; }
.hit-card { margin-bottom: 12px; }
.hit-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.hit-meta { color: #909399; font-size: 12px; }
.hit-content { color: #303133; line-height: 1.6; white-space: pre-wrap; }
.answer-card { background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); }
.answer-text { font-size: 15px; line-height: 1.7; color: #303133; white-space: pre-wrap; }
.citations { margin-top: 16px; padding-top: 12px; border-top: 1px dashed #c0c4cc; font-size: 13px; }
.chunk-card { margin-bottom: 12px; }
.chunk-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.chunk-meta { color: #909399; font-size: 12px; }
.chunk-text { line-height: 1.6; white-space: pre-wrap; color: #303133; }
</style>