<!-- @file pipeline/Index.vue - 工作流管理 V6.9 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🔄 工作流</h2>
      <el-button type="primary" @click="goDesigner()">
        <el-icon><EditPen /></el-icon>新建工作流
      </el-button>
    </div>

    <!-- V6.9 Tab 导航 -->
    <el-tabs v-model="activeTab" class="pipeline-tabs">
      <el-tab-pane label="📋 工作流列表" name="list">
        <el-table :data="workflows" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="runCount" label="运行次数" width="100" align="center" />
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="runWorkflow(row)">运行</el-button>
              <el-button size="small" @click="goDesigner(row.id)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteWf(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="🎨 画布设计器" name="designer">
        <div class="tab-placeholder">
          <el-card>
            <div style="text-align:center;padding:40px">
              <el-icon :size="48" style="color:#409eff"><EditPen /></el-icon>
              <h3 style="margin:16px 0 8px">工作流画布设计器</h3>
              <p style="color:#909399;margin-bottom:16px">可视化拖拽构建工作流，支持 10+ 节点类型与条件分支</p>
              <el-button type="primary" @click="goDesigner()">打开设计器</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="📊 运行监控" name="runs">
        <div class="tab-placeholder">
          <el-card>
            <div style="text-align:center;padding:40px">
              <el-icon :size="48" style="color:#67c23a"><Monitor /></el-icon>
              <h3 style="margin:16px 0 8px">工作流运行监控</h3>
              <p style="color:#909399;margin-bottom:16px">实时查看工作流执行状态、日志与性能指标</p>
              <el-button type="primary" @click="goRuns()">查看监控</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkflows, runWorkflow as runWf, deleteWorkflow } from '@/api/pipeline'
import { EditPen, Monitor } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// V6.9 Tab 导航
const activeTab = ref(route.query.tab || 'list')

async function loadWorkflows() {
  loading.value = true
  try {
    const r = await listWorkflows()
    workflows.value = r.data || []
  } catch { workflows.value = [] }
  finally { loading.value = false }
}

const workflows = ref([])
const loading = ref(false)

function goDesigner(id) {
  router.push(id ? '/pipeline/designer/' + id : '/pipeline/designer')
}

function goRuns() {
  router.push('/pipeline/runs')
}

async function runWorkflow(wf) {
  try {
    await runWf(wf.id)
    ElMessage.success('工作流已触发')
  } catch (e) { ElMessage.error('运行失败') }
}

async function deleteWf(wf) {
  await ElMessageBox.confirm('确认删除工作流「' + wf.name + '」？')
  try {
    await deleteWorkflow(wf.id)
    ElMessage.success('已删除')
    loadWorkflows()
  } catch { ElMessage.error('删除失败') }
}

onMounted(loadWorkflows)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; } }
.pipeline-tabs { margin-bottom: 0; }
.tab-placeholder { padding-top: 8px; }
.tab-placeholder .el-card { max-width: 480px; margin: 0 auto; }
</style>
