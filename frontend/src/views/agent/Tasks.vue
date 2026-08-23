<!--
  @file agent/Tasks.vue - 任务编排页 (V7.6)
  路由: /agent/tasks
-->
<template>
  <div class="tasks-page">
    <div class="toolbar">
      <el-input
        v-model="search"
        placeholder="搜索任务"
        size="default"
        style="width: 240px"
        clearable
        @input="loadTasks"
      />
      <el-select v-model="statusFilter" placeholder="状态" size="default" clearable style="width: 140px" @change="loadTasks">
        <el-option label="待执行" value="pending" />
        <el-option label="执行中" value="running" />
        <el-option label="已完成" value="completed" />
        <el-option label="失败" value="failed" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
      <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
    </div>

    <el-table :data="filteredTasks" v-loading="loading" stripe>
      <el-table-column prop="name" label="任务名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="agentType" label="Agent" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.agentType || row.type || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column prop="duration" label="耗时" width="100">
        <template #default="{ row }">{{ row.duration || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="viewTask(row)">查看</el-button>
          <el-button size="small" link type="success" @click="retryTask(row)">重试</el-button>
          <el-button size="small" link type="danger" @click="deleteTask(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState
      v-if="!loading && tasks.length === 0"
      title="暂无任务"
      description="创建第一个 Agent 任务试试"
    />

    <!-- 创建任务对话框 -->
    <el-dialog v-model="createDialog" title="新建任务" width="500px">
      <el-form :model="newTask" label-width="80px">
        <el-form-item label="任务名">
          <el-input v-model="newTask.name" />
        </el-form-item>
        <el-form-item label="Agent">
          <el-select v-model="newTask.agentType">
            <el-option label="ReAct" value="react" />
            <el-option label="Plan-Execute" value="plan_execute" />
            <el-option label="Reflection" value="reflection" />
            <el-option label="Multi-Agent" value="multi" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model="newTask.goal" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createTask">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { agentApi } from '@/api/agent'

const tasks = ref([])
const loading = ref(false)
const search = ref('')
const statusFilter = ref('')

const createDialog = ref(false)
const creating = ref(false)
const newTask = reactive({ name: '', agentType: 'react', goal: '' })

const filteredTasks = computed(() => {
  let result = tasks.value
  if (statusFilter.value) result = result.filter(t => t.status === statusFilter.value)
  if (search.value) {
    const kw = search.value.toLowerCase()
    result = result.filter(t =>
      (t.name || '').toLowerCase().includes(kw) ||
      (t.goal || '').toLowerCase().includes(kw)
    )
  }
  return result
})

function statusType(s) {
  return { pending: 'info', running: 'primary', completed: 'success', failed: 'danger' }[s] || 'info'
}
function statusLabel(s) {
  return { pending: '待执行', running: '执行中', completed: '已完成', failed: '失败' }[s] || s
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await agentApi.listTasks({ search: search.value, status: statusFilter.value })
    if (res.code === 0) tasks.value = res.data?.list || res.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openCreate() {
  newTask.name = ''
  newTask.agentType = 'react'
  newTask.goal = ''
  createDialog.value = true
}

async function createTask() {
  if (!newTask.name) return ElMessage.warning('请输入任务名')
  creating.value = true
  try {
    await agentApi.createTask(newTask)
    ElMessage.success('创建成功')
    createDialog.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('创建失败: ' + e.message)
  } finally { creating.value = false }
}

function viewTask(row) {
  // 跳转到详情页或打开 drawer
  ElMessage.info(`任务 #${row.id} - ${row.goal || row.name}`)
}

async function retryTask(row) {
  try {
    await agentApi.retryTask(row.id)
    ElMessage.success('已重试')
    loadTasks()
  } catch (e) { ElMessage.error('重试失败') }
}

async function deleteTask(row) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.name}」?`, '提示', { type: 'warning' })
    await agentApi.deleteTask(row.id)
    ElMessage.success('已删除')
    loadTasks()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.tasks-page { background: white; border-radius: 12px; padding: 16px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
</style>
