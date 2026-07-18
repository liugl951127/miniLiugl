<!--
  @file views/pipeline/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🔀 工作流列表 (V5.32)</span>
          <div>
            <el-input v-model="searchKey" placeholder="搜索名称" style="width:200px;margin-right:8px" clearable />
            <el-button type="primary" @click="$router.push('/pipeline/designer')">+ 新建工作流</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredWorkflows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status || '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nodeCount" label="节点数" width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/pipeline/designer?id=${row.id}`)">编辑</el-button>
            <el-button size="small" type="success" @click="runOne(row)">运行</el-button>
            <el-button size="small" @click="$router.push(`/pipeline/runs/${row.id}`)">历史</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listWorkflows, runWorkflow, deleteWorkflow } from '@/api/pipeline'

const router = useRouter()
const workflows = ref([])
const loading = ref(false)
const searchKey = ref('')

const filteredWorkflows = computed(() => {
  if (!searchKey.value) return workflows.value
  const k = searchKey.value.toLowerCase()
  return workflows.value.filter(w =>
    (w.name || '').toLowerCase().includes(k) || (w.description || '').toLowerCase().includes(k)
  )
})

async function load() {
  loading.value = true
  try {
    const res = await listWorkflows({ page: 1, size: 50 })
    workflows.value = res.data?.records || res.data || []
  } catch (e) {} finally { loading.value = false }
}

async function runOne(row) {
  try {
    await ElMessageBox.confirm(`运行工作流 "${row.name}"?`, '提示', { type: 'info' })
    const res = await runWorkflow(row.id)
    ElMessage.success('运行已启动, Run ID: ' + (res.data?.runId || 'N/A'))
    router.push('/pipeline/runs')
  } catch (e) { if (e !== 'cancel') {} }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除工作流 "${row.name}"? 不可恢复`, '危险操作', { type: 'warning' })
    await deleteWorkflow(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') {} }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
