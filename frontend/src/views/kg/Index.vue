<!--
  @file kg/Index.vue - 知识图谱 V8.0 (组件化拆分)
  拆分: 1129 行 → ~400 行主文件 + 2 组件
  - KgStats.vue       - 4 统计卡片
  - KgEntityPanel.vue - 实体管理 (搜索 + 添加 + 列表)
  - KgGraph.vue       - 图谱可视化 (复用现有 @/components/KgGraph)
-->
<template>
  <PageStandard
    title="🕸️ 知识图谱"
    subtitle="实体管理 · 关系网络 · 图谱可视化"
  >
    <!-- 工具栏 -->
    <div class="kg-toolbar">
      <el-button type="primary" size="small" :icon="Upload" @click="showUpload = true">导入数据</el-button>
      <el-dropdown @command="handleExport" trigger="click">
        <el-button size="small" :icon="Download">导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="png">导出 PNG</el-dropdown-item>
            <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button size="small" :icon="Refresh" @click="loadKg">刷新</el-button>
      <span style="margin-left:auto;font-size:12px;color:var(--el-text-color-secondary)">
        共 {{ nodes.length }} 节点 / {{ links.length }} 边
      </span>
    </div>

    <KgStats :stats="stats" />

    <!-- 图谱可视化 -->
    <el-card style="margin-bottom:12px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>图谱可视化</span>
          <div style="display:flex;gap:8px;align-items:center;font-size:12px;color:var(--el-text-color-secondary)">
            <span>缩放: {{ Math.round(zoomScale * 100) }}%</span>
            <el-button size="small" link @click="resetView">重置</el-button>
          </div>
        </div>
      </template>
      <div class="kg-canvas" ref="canvasRef">
        <KgGraph
          v-if="nodes.length"
          :entities="kgGraphEntities"
          :relations="kgGraphLinks"
          @entity-click="onGraphEntityClick"
          @relation-click="onGraphRelationClick"
        />
        <div v-else-if="!loading" class="kg-empty">暂无图谱数据，请先导入实体</div>
        <div v-else style="text-align:center;padding:60px"><el-icon class="is-loading"><Loading /></el-icon></div>
      </div>
    </el-card>

    <KgEntityPanel
      :entities="entities"
      :loading="loading"
      :show-add-form="showAddForm"
      :add-entity-loading="addEntityLoading"
      :loading-neighbors-id="loadingNeighborsId"
      :deleting-entity-id="deletingEntityId"
      @search="onSearch"
      @add="showAddForm = !showAddForm"
      @add-entity="handleAddEntity"
      @reset-add="resetAddForm"
      @view-neighbors="viewNeighbors"
      @select="selectEntity"
      @delete="confirmDeleteEntity"
    />

    <!-- 邻居弹窗 -->
    <el-dialog v-model="neighborVisible" title="邻居实体" width="500px">
      <div v-if="neighbors.length">
        <el-tag v-for="n in neighbors" :key="n.id" style="margin:4px">{{ n.name }}</el-tag>
      </div>
      <el-empty v-else description="无邻居实体" />
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="实体详情" width="500px">
      <el-descriptions v-if="selectedEntity" :column="1" border>
        <el-descriptions-item label="名称">{{ selectedEntity.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ selectedEntity.type }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ selectedEntity.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button size="small" link type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="showUpload" title="导入图谱数据" width="500px" :close-on-click-modal="!importing">
      <el-upload ref="uploadRef" :auto-upload="false" :on-change="onFileSelect" :show-file-list="false" accept=".json">
        <el-button type="primary" :disabled="importing">选择 JSON 文件</el-button>
        <span style="margin-left:8px;font-size:12px;color:var(--el-text-color-secondary)">
          {{ pendingFile ? pendingFile.name : '未选择文件' }}
        </span>
      </el-upload>
      <el-progress v-if="importing" :percentage="importProgress" :format="p => `已导入 ${p}%`" style="margin-top:12px" />
      <template #footer>
        <el-button @click="showUpload = false" :disabled="importing">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </PageStandard>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, Refresh, ArrowDown, Loading } from '@element-plus/icons-vue'
import PageStandard from '@/components/PageStandard.vue'
import KgGraph from '@/components/KgGraph.vue'
import KgStats from './KgStats.vue'
import KgEntityPanel from './KgEntityPanel.vue'
import { kgApi } from '@/api/kg'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.profile?.id || userStore.userInfo?.id || null)

const nodes = ref([])
const links = ref([])
const entities = ref([])
const neighbors = ref([])
const selectedEntity = ref(null)
const showUpload = ref(false)
const showAddForm = ref(false)
const loading = ref(false)
const searchKw = ref('')
const searchResults = ref([])
const zoomScale = ref(1)
const canvasRef = ref(null)
const uploadRef = ref(null)
const pendingFile = ref(null)
const importing = ref(false)
const importProgress = ref(0)
const addEntityLoading = ref(false)
const loadingNeighborsId = ref(null)
const deletingEntityId = ref(null)
const neighborVisible = ref(false)
const detailVisible = ref(false)

const stats = reactive({ entities: 0, edges: 0, types: 0, queries: 0 })

const kgGraphEntities = computed(() => nodes.value.map(n => ({
  id: n.id, label: n.label || n.name, type: n.type
})))
const kgGraphLinks = computed(() => links.value.map(l => ({
  source: l.source, target: l.target, label: l.label
})))

const addForm = reactive({ name: '', type: 'Concept' })

async function loadKg() {
  loading.value = true
  try {
    const res = await kgApi.list()
    const data = res.data?.data ?? res.data ?? res ?? {}
    nodes.value = data.nodes || []
    links.value = data.links || data.edges || []
    entities.value = data.entities || nodes.value
    stats.entities = nodes.value.length
    stats.edges = links.value.length
    stats.types = new Set(nodes.value.map(n => n.type)).size
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.value = false }
}

function onSearch(kw) {
  if (!kw) return loadKg()
  searchResults.value = entities.value.filter(e =>
    (e.name && e.name.includes(kw)) || (e.type && e.type.includes(kw))
  )
  if (!searchResults.value.length) ElMessage.info('无匹配结果')
  else ElMessage.success(`找到 ${searchResults.value.length} 个匹配`)
}

function resetAddForm() {
  Object.assign(addForm, { name: '', type: 'Concept' })
}

async function handleAddEntity(form) {
  if (!form.name) return ElMessage.warning('请输入名称')
  addEntityLoading.value = true
  try {
    await kgApi.createEntity({ ...form, userId: currentUserId.value })
    ElMessage.success('已添加')
    resetAddForm()
    showAddForm.value = false
    loadKg()
  } catch (e) { ElMessage.error('添加失败') }
  finally { addEntityLoading.value = false }
}

async function viewNeighbors(row) {
  loadingNeighborsId.value = row.id
  try {
    const res = await kgApi.getNeighbors(row.id)
    neighbors.value = res.data?.data ?? res.data ?? []
    neighborVisible.value = true
  } catch (e) { ElMessage.error('查询失败') }
  finally { loadingNeighborsId.value = null }
}

function selectEntity(row) {
  selectedEntity.value = row
  detailVisible.value = true
}

async function confirmDeleteEntity(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」?`, '提示', { type: 'warning' })
  deletingEntityId.value = row.id
  try {
    await kgApi.deleteEntity(row.id)
    ElMessage.success('已删除')
    loadKg()
  } catch (e) { ElMessage.error('删除失败') }
  finally { deletingEntityId.value = null }
}

function resetView() {
  zoomScale.value = 1
  ElMessage.success('视图已重置')
}

function onFileSelect(file) {
  if (!file?.raw) return
  pendingFile.value = file.raw
}

async function handleImport() {
  if (!pendingFile.value) return ElMessage.warning('请选择文件')
  importing.value = true
  importProgress.value = 0
  try {
    const text = await pendingFile.value.text()
    const data = JSON.parse(text)
    // 模拟进度
    for (let i = 0; i <= 100; i += 10) {
      importProgress.value = i
      await new Promise(r => setTimeout(r, 100))
    }
    await kgApi.import(data)
    ElMessage.success('导入成功')
    showUpload.value = false
    loadKg()
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.message || ''))
  } finally {
    importing.value = false
    pendingFile.value = null
  }
}

function handleExport(cmd) {
  if (cmd === 'png') ElMessage.info('PNG 导出: ' + (canvasRef.value?.querySelector('svg')?.outerHTML?.length || 0) + ' bytes')
  else if (cmd === 'json') {
    const blob = new Blob([JSON.stringify({ nodes: nodes.value, links: links.value }, null, 2)])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'kg.json'; a.click()
    URL.revokeObjectURL(url)
  }
}

function onGraphEntityClick(entity) {
  const found = entities.value.find(e => e.id === entity.id)
  if (found) selectEntity(found)
}
function onGraphRelationClick(relation) {
  ElMessage.info(`关系: ${relation.source} → ${relation.target}`)
}

onMounted(loadKg)
</script>

<style scoped>
.kg-toolbar {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 12px; flex-wrap: wrap;
}
.kg-canvas {
  min-height: 400px; background: #fafbfc; border-radius: 8px; position: relative;
}
.kg-empty {
  text-align: center; color: var(--el-text-color-secondary);
  padding: 60px; font-size: 14px;
}
</style>
