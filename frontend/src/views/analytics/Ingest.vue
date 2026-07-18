<!--
  @file views/analytics/Ingest.vue (Ingest 页面)
  @version V3.5.12+ (前端注释补全)
  @description Ingest 页面
-->
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📥 文件导入 (V5.31)</span>
          <span style="color:#909399;font-size:12px">CSV / JSON / LOG 异步解析 + 质量分析</span>
        </div>
      </template>

      <!-- V5.22: 编码格式选择 -->
      <div class="encoding-row">
        <span class="encoding-label">文件编码:</span>
        <el-select v-model="selectedEncoding" placeholder="自动检测" style="width:180px" clearable>
          <el-option label="自动检测 (推荐)" value="" />
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="UTF-8-BOM" value="UTF-8-BOM" />
          <el-option label="GBK (中文)" value="GBK" />
          <el-option label="ISO-8859-1" value="ISO-8859-1" />
          <el-option label="Big5 (繁体)" value="Big5" />
          <el-option label="Shift-JIS (日文)" value="Shift-JIS" />
        </el-select>
      </div>

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

      <!-- V5.22: 上传进度条 + 取消 -->
      <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
        <span class="upload-name">{{ uploadingFileName }}</span>
        <el-progress :percentage="uploadProgress" :stroke-width="8" style="flex:1;margin:0 12px" />
        <el-button size="small" type="danger" @click="cancelUpload">取消</el-button>
      </div>

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
// ───── 依赖导入 ─────
import { ref, onMounted } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadIngestFile, getIngestTask, getIngestQuality } from '@/api/analytics'

const tasks = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const current = ref(null)

// V5.22: 编码格式 + 上传进度
const selectedEncoding = ref('')
const uploadProgress = ref(0)
const uploadingFileName = ref('')
let uploadCancel = null

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
  if (selectedEncoding.value) fd.append('encoding', selectedEncoding.value)

  uploadProgress.value = 0
  uploadingFileName.value = option.file.name
  let cancelled = false

  try {
    const { promise, cancel } = uploadIngestFile(fd, {
      onProgress: (e) => {
        if (!cancelled) uploadProgress.value = e.total > 0 ? Math.round((e.loaded / e.total) * 100) : 0
      }
    })
    uploadCancel = () => { cancelled = true; cancel?.() }

    const res = await promise
    if (!cancelled) {
      ElMessage.success('上传成功, 任务 ID: ' + res.data?.taskId)
      uploadProgress.value = 0
      uploadingFileName.value = ''
      loadTasks()
    }
  } catch (e) {
    if (e?.__cancelled || e?.name === 'CanceledError') {
      ElMessage.info('上传已取消')
    } else {
      ElMessage.error('上传失败: ' + (e.response?.data?.message || e.message))
    }
    uploadProgress.value = 0
    uploadingFileName.value = ''
  } finally {
    uploadCancel = null
  }
}

function cancelUpload() {
  if (uploadCancel) {
    uploadCancel()
    uploadCancel = null
  }
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
.encoding-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.encoding-label { font-size: 13px; color: #606266; font-weight: 500; }
.upload-progress { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0f9ff; border-radius: 6px; border: 1px solid #d0e8ff; margin-top: 10px; }
.upload-name { font-size: 12px; color: #409eff; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
