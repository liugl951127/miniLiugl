<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📥 文件导入 (V5.31)</span>
          <span style="color:#909399;font-size:12px">CSV / JSON / LOG 异步解析 + 质量分析</span>
        </div>
      </template>

      <el-upload
        :http-request="customUpload"
        :show-file-list="false"
        :before-upload="beforeUpload"
        drag
        accept=".csv,.json,.log,.txt"
      >
        <el-icon style="font-size:48px;color:#409eff"><UploadFilled /></el-icon>
        <div>拖拽文件到此处, 或<em>点击上传</em></div>
        <template #tip>
          <div style="color:#909399;font-size:12px">支持 CSV / JSON / LOG / TXT, 单文件最大 100MB</div>
        </template>
      </el-upload>

      <el-divider>任务列表</el-divider>

      <el-table :data="tasks" v-loading="loading">
        <el-table-column prop="taskId" label="任务 ID" width="200" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rows" label="行数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="viewTask(row)">详情</el-button>
            <el-button size="small" @click="viewQuality(row)">质量</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="detailVisible" :title="`任务 ${current?.taskId}`" width="720">
        <pre v-if="current" class="json">{{ JSON.stringify(current, null, 2) }}</pre>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadIngestFile, getIngestTask, getIngestQuality } from '@/api/analytics'

const tasks = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const current = ref(null)

function statusType(s) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', PENDING: 'info' }[s] || ''
}

function beforeUpload(file) {
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('文件超过 100MB')
    return false
  }
  return true
}

async function customUpload(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  try {
    const res = await uploadIngestFile(fd)
    ElMessage.success('上传成功, 任务 ID: ' + res.data?.taskId)
    loadTasks()
  } catch (e) {}
}

async function loadTasks() {
  loading.value = true
  try {
    // 简化: 实际应有 list 端点, 这里从 quality 反查
    tasks.value = JSON.parse(localStorage.getItem('minimax-ingest-tasks') || '[]')
  } finally { loading.value = false }
}

async function viewTask(row) {
  const res = await getIngestTask(row.taskId)
  current.value = res.data
  detailVisible.value = true
}

async function viewQuality(row) {
  const res = await getIngestQuality(row.taskId)
  current.value = res.data
  detailVisible.value = true
}

onMounted(loadTasks)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.json { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 12px; max-height: 500px; overflow: auto; }
</style>
