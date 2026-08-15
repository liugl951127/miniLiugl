<!-- @file pipeline/Index.vue - 工作流管理 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>工作流</h2>
      <el-button type="primary" @click="goDesigner"><el-icon><EditPen /></el-icon>新建工作流</el-button>
    </div>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkflows, runWorkflow as runWf, deleteWorkflow } from '@/api/pipeline'
import { EditPen } from '@element-plus/icons-vue'

const router = useRouter()
const workflows = ref([])
const loading = ref(false)

async function loadWorkflows() {
  loading.value = true
  try {
    const r = await listWorkflows()
    workflows.value = r.data || []
  } catch { workflows.value = [] }
  finally { loading.value = false }
}

function goDesigner(id) {
  router.push(id ? '/pipeline/designer/' + id : '/pipeline/designer')
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
</style>
