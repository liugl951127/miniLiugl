<!-- @file pipeline/Index.vue - 工作流管理 V7.0 (内嵌设计器/监控预览) -->
<template>
  <PageStandard title="🔄 工作流" subtitle="可视化设计 · 节点编排 · 监控执行">
    <template #actions>
      <el-button type="primary" :loading="creating" @click="goDesigner()">
        <el-icon><EditPen /></el-icon>新建工作流
      </el-button>
    </template>

    <!-- V7.0 Tab 导航 -->
    <el-tabs v-model="activeTab" class="pipeline-tabs">
      <!-- ═══ 1. 工作流列表 ═══ -->
      <el-tab-pane label="📋 工作流列表" name="list">
        <el-table :data="workflows" v-loading="loading" stripe empty-text="暂无工作流，点击右上角新建">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status || 'DRAFT' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="runCount" label="运行次数" width="100" align="center">
            <template #default="{ row }">{{ row.runCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" :loading="runningId === row.id" @click="runWorkflow(row)">运行</el-button>
              <el-button size="small" @click="goDesigner(row.id)">编辑</el-button>
              <el-button size="small" type="danger" :loading="deletingId === row.id" @click="deleteWf(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!loading && workflows.length === 0"
          description="还没有工作流，点击「新建工作流」开始构建"
          :image-size="80"
          style="margin-top:24px"
        />
      </el-tab-pane>

      <!-- ═══ 2. 画布设计器预览 ═══ -->
      <el-tab-pane label="🎨 画布设计器" name="designer">
        <div class="preview-header">
          <span class="preview-title">最近 5 个工作流</span>
          <el-button type="primary" link @click="goDesigner()">
            打开完整设计器 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-table :data="recentWorkflows" v-loading="recentLoading" stripe empty-text="暂无工作流">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
          <el-table-column prop="nodeCount" label="节点数" width="100" align="center">
            <template #default="{ row }">{{ row.nodeCount ?? (row.nodes?.length ?? '-') }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="goDesigner(row.id)">编辑</el-button>
              <el-button size="small" @click="runWorkflow(row)">运行</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!recentLoading && recentWorkflows.length === 0"
          description="暂无工作流可预览"
          :image-size="60"
        />
      </el-tab-pane>

      <!-- ═══ 3. 运行监控 ═══ -->
      <el-tab-pane label="📊 运行监控" name="runs">
        <div class="preview-header">
          <span class="preview-title">最近 5 次运行</span>
          <el-button type="primary" link @click="goRuns()">
            查看完整监控 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-table :data="recentRuns" v-loading="runsLoading" stripe empty-text="暂无运行记录">
          <el-table-column prop="runId" label="运行 ID" width="100" />
          <el-table-column prop="workflowName" label="工作流" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="180" />
          <el-table-column prop="duration" label="耗时" width="100" align="center">
            <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="goRuns()">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!runsLoading && recentRuns.length === 0"
          description="暂无运行记录，先去运行一个工作流吧"
          :image-size="60"
        />
      </el-tab-pane>
    </el-tabs>
  </PageStandard>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkflows, runWorkflow as runWf, deleteWorkflow, listRecentRuns } from '@/api/pipeline'
import { EditPen, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// V7.0 Tab 导航
const activeTab = ref(route.query.tab || 'list')

// 工作流列表
const workflows = ref([])
const loading = ref(false)
const runningId = ref(null)
const deletingId = ref(null)
const creating = ref(false)

// 最近 5 个工作流 (画布设计器预览)
const recentWorkflows = ref([])
const recentLoading = ref(false)

// 最近 5 次运行 (监控预览)
const recentRuns = ref([])
const runsLoading = ref(false)

// ==================== 状态映射 ====================
function statusType(s) {
  return {
    PENDING: 'info',
    RUNNING: 'primary',
    SUCCESS: 'success',
    FAILED: 'danger',
    STOPPED: 'warning'
  }[s] || 'info'
}

function formatDuration(d) {
  if (d === null || d === undefined) return '-'
  if (typeof d === 'number') {
    if (d < 1000) return d + 'ms'
    if (d < 60000) return (d / 1000).toFixed(1) + 's'
    return Math.floor(d / 60000) + 'm' + Math.floor((d % 60000) / 1000) + 's'
  }
  return d
}

// ==================== 加载函数 ====================
async function loadWorkflows() {
  loading.value = true
  try {
    const r = await listWorkflows()
    workflows.value = r.data?.list || r.data || []
  } catch (e) {
    workflows.value = []
  } finally {
    loading.value = false
  }
}

async function loadRecentWorkflows() {
  recentLoading.value = true
  try {
    const r = await listWorkflows({ limit: 5 })
    recentWorkflows.value = r.data?.list || r.data || []
  } catch (e) {
    recentWorkflows.value = []
  } finally {
    recentLoading.value = false
  }
}

async function loadRecentRuns() {
  runsLoading.value = true
  try {
    recentRuns.value = await listRecentRuns(5, 5)
  } catch (e) {
    recentRuns.value = []
  } finally {
    runsLoading.value = false
  }
}

// ==================== 路由操作 ====================
function goDesigner(id) {
  if (!id) creating.value = true
  router.push(id ? '/pipeline/designer/' + id : '/pipeline/designer')
    .finally(() => { creating.value = false })
}

function goRuns() {
  router.push('/pipeline/runs')
}

// ==================== CRUD 操作 ====================
async function runWorkflow(wf) {
  runningId.value = wf.id
  try {
    await runWf(wf.id)
    ElMessage.success('工作流已触发：' + wf.name)
    await Promise.all([loadRecentRuns(), loadRecentWorkflows()])
  } catch (e) {
    ElMessage.error('运行失败：' + (e?.message || '未知错误'))
  } finally {
    runningId.value = null
  }
}

async function deleteWf(wf) {
  try {
    await ElMessageBox.confirm(
      `确认删除工作流「${wf.name || wf.id}」？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (_) {
    return // 用户取消
  }
  deletingId.value = wf.id
  try {
    await deleteWorkflow(wf.id)
    ElMessage.success('已删除')
    await Promise.all([loadWorkflows(), loadRecentWorkflows(), loadRecentRuns()])
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || '未知错误'))
  } finally {
    deletingId.value = null
  }
}

// ==================== Tab 切换时按需刷新 ====================
watch(activeTab, (newTab) => {
  if (newTab === 'designer' && recentWorkflows.value.length === 0) {
    loadRecentWorkflows()
  } else if (newTab === 'runs' && recentRuns.value.length === 0) {
    loadRecentRuns()
  }
})

onMounted(() => {
  loadWorkflows()
  // 默认预取另外两个 tab 的数据, 用户切换时无需等待
  loadRecentWorkflows()
  loadRecentRuns()
})
</script>

<style lang="scss" scoped>
.preview-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px;
  padding: 4px 0;
}
.preview-title {
  font-size: 14px; font-weight: 600; color: var(--el-text-color-primary);
}
</style>
