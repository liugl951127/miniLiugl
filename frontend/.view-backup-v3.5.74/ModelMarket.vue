<!--
  @file views/ai/ModelMarket.vue (AI 模型 (ModelMarket))
  @version V3.5.12+ (前端注释补全)
  @description AI 模型 (ModelMarket)
-->
<template>
  <div class="model-market-container">
    <div class="header">
      <h1>📦 AI 模型市场 <span class="badge">V2.9.1</span></h1>
      <p class="sub">上传 / 下载 / 评分 · 用户共建的模型生态</p>
    </div>

    <el-row :gutter="16" v-if="stats">
      <el-col :span="5">
        <el-card class="kpi">
          <div class="kpi-label">总模型</div>
          <div class="kpi-value">{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card class="kpi success">
          <div class="kpi-label">已发布</div>
          <div class="kpi-value">{{ stats.published }}</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card class="kpi primary">
          <div class="kpi-label">总下载</div>
          <div class="kpi-value">{{ stats.totalDownloads.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card class="kpi warn">
          <div class="kpi-label">总大小</div>
          <div class="kpi-value">{{ formatSize(stats.totalSize) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card style="margin-top: 16px">
      <el-form :inline="true" size="small">
        <el-form-item label="类型">
          <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="PyTorch" value="PYTORCH" />
            <el-option label="TensorFlow" value="TENSORFLOW" />
            <el-option label="ONNX" value="ONNX" />
            <el-option label="SafeTensors" value="SAFETENSORS" />
            <el-option label="GGUF" value="GGUF" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务">
          <el-input v-model="filterTask" placeholder="任务类型" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filterKeyword" placeholder="名称/描述" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filterSort" style="width: 120px">
            <el-option label="最新" value="" />
            <el-option label="评分" value="rating" />
            <el-option label="下载" value="download" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadModels" :loading="loading">🔍</el-button>
        <el-button type="success" @click="showUpload = true" icon="Upload">📤 发布模型</el-button>
      </el-form>
    </el-card>

    <!-- 类型分布 -->
    <el-card v-if="stats" style="margin-top: 16px">
      <v-chart :option="typeOption" style="height: 220px" autoresize />
    </el-card>

    <!-- 模型列表 -->
    <el-row :gutter="16" style="margin-top: 16px" v-loading="loading">
      <el-col :span="8" v-for="m in models" :key="m.id" style="margin-bottom: 16px">
        <el-card class="model-card" shadow="hover" @click.native="showDetail(m)">
          <div class="model-header">
            <span class="model-icon">{{ typeIcon(m.modelType) }}</span>
            <div class="model-meta">
              <div class="model-name">{{ m.name }}</div>
              <div class="model-author">by {{ m.authorName }} · v{{ m.version }}</div>
            </div>
            <el-tag size="small" :type="typeColor(m.modelType)">{{ m.modelType }}</el-tag>
          </div>
          <div class="model-desc">{{ m.description }}</div>
          <div class="model-tags">
            <el-tag v-for="t in (m.tags || '').split(',').filter(x => x)" :key="t" size="small" effect="plain" style="margin-right: 4px">
              {{ t }}
            </el-tag>
          </div>
          <div class="model-stats">
            <span>⭐ {{ (m.avgRating || 0).toFixed(1) }}</span>
            <span>📥 {{ m.downloadCount }}</span>
            <span>💾 {{ formatSize(m.fileSize) }}</span>
            <span>📄 {{ m.license }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && models.length === 0" description="暂无模型" />

    <!-- 发布对话框 -->
    <el-dialog v-model="showUpload" title="📤 发布模型" width="640px">
      <el-tabs v-model="uploadTab">
        <el-tab-pane label="📁 上传文件" name="file">
          <el-form label-width="100px">
            <el-form-item label="模型文件" required>
              <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange" drag>
                <div class="upload-area">
                  <el-icon style="font-size: 48px"><UploadFilled /></el-icon>
                  <div>点击或拖拽上传 (最大 5GB)</div>
                </div>
              </el-upload>
            </el-form-item>
            <el-form-item label="模型名" required>
              <el-input v-model="uploadForm.name" placeholder="例: 中文情感分析 BERT" />
            </el-form-item>
            <el-form-item label="任务类型">
              <el-input v-model="uploadForm.taskType" placeholder="TEXT_CLASSIFICATION" />
            </el-form-item>
            <el-form-item label="基础模型">
              <el-input v-model="uploadForm.baseModel" placeholder="bert-base-chinese" />
            </el-form-item>
            <el-form-item label="许可">
              <el-select v-model="uploadForm.license" style="width:100%">
                <el-option label="MIT" value="MIT" />
                <el-option label="Apache 2.0" value="APACHE_2_0" />
                <el-option label="GPL 3" value="GPL_3" />
                <el-option label="CC BY 4.0" value="CC_BY_4" />
                <el-option label="商业" value="COMMERCIAL" />
                <el-option label="专有" value="PROPRIETARY" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="uploadForm.tags" placeholder="逗号分隔" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="uploadForm.description" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="📝 仅元数据" name="meta">
          <el-form label-width="100px">
            <el-form-item label="模型名" required>
              <el-input v-model="metaForm.name" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="metaForm.modelType" style="width:100%">
                <el-option label="PyTorch" value="PYTORCH" />
                <el-option label="TensorFlow" value="TENSORFLOW" />
                <el-option label="ONNX" value="ONNX" />
                <el-option label="SafeTensors" value="SAFETENSORS" />
                <el-option label="GGUF" value="GGUF" />
              </el-select>
            </el-form-item>
            <el-form-item label="任务">
              <el-input v-model="metaForm.taskType" />
            </el-form-item>
            <el-form-item label="基础模型">
              <el-input v-model="metaForm.baseModel" />
            </el-form-item>
            <el-form-item label="版本">
              <el-input v-model="metaForm.version" placeholder="1.0.0" />
            </el-form-item>
            <el-form-item label="许可">
              <el-input v-model="metaForm.license" placeholder="MIT" />
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="metaForm.tags" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="metaForm.description" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button v-if="uploadTab === 'file'" type="primary" @click="submitUploadFile" :loading="uploading">上传</el-button>
        <el-button v-else type="primary" @click="submitUploadMeta" :loading="uploading">发布</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetail_" :title="detail?.name" width="700px" v-if="detail">
      <div class="detail-header">
        <span class="model-icon big">{{ typeIcon(detail.modelType) }}</span>
        <div>
          <h2>{{ detail.name }}</h2>
          <p>{{ detail.description }}</p>
          <el-tag size="small" :type="typeColor(detail.modelType)">{{ detail.modelType }}</el-tag>
          <el-tag size="small" style="margin-left: 6px">{{ detail.taskType }}</el-tag>
          <el-tag size="small" type="info" style="margin-left:6px">v{{ detail.version }}</el-tag>
          <el-tag size="small" type="success" style="margin-left:6px">⭐ {{ (detail.avgRating || 0).toFixed(1) }}</el-tag>
          <el-tag size="small" type="warning" style="margin-left:6px">{{ detail.license }}</el-tag>
        </div>
      </div>
      <el-divider />
      <el-row :gutter="16">
        <el-col :span="8"><b>基础模型:</b> {{ detail.baseModel || '-' }}</el-col>
        <el-col :span="8"><b>作者:</b> {{ detail.authorName }}</el-col>
        <el-col :span="8"><b>下载:</b> {{ detail.downloadCount }} 次</el-col>
      </el-row>
      <el-divider />
      <div class="detail-actions">
        <el-button type="primary" @click="downloadModel">
          📥 下载 ({{ formatSize(detail.fileSize) }})
        </el-button>
        <el-button @click="rateVisible = !rateVisible">⭐ 评分</el-button>
        <div v-if="rateVisible" class="rate-row">
          <el-rate v-model="myRating" :max="5" show-text />
          <el-input v-model="myComment" placeholder="评论" style="width: 240px" />
          <el-button type="primary" size="small" @click="submitRate">提交</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { modelMarketApi } from '@/api/modelMarket'
import { useUserStore } from '@/store/user'

use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent, LegendComponent])

const userStore = useUserStore()
const stats = ref(null)
const models = ref([])
const loading = ref(false)
const filterType = ref('')
const filterTask = ref('')
const filterKeyword = ref('')
const filterSort = ref('')

const showUpload = ref(false)
const uploadTab = ref('file')
const uploading = ref(false)
const uploadForm = reactive({ name: '', description: '', taskType: '', baseModel: '', license: 'MIT', tags: '' })
const metaForm = reactive({ name: '', description: '', modelType: 'PYTORCH', taskType: '', baseModel: '', version: '1.0.0', license: 'MIT', tags: '' })
const fileObj = ref(null)

const showDetail_ = ref(false)
const detail = ref(null)
const rateVisible = ref(false)
const myRating = ref(0)
const myComment = ref('')

const typeIcon = (t) => {
  return { PYTORCH: '🔥', TENSORFLOW: '📊', ONNX: '⚡', SAFETENSORS: '🛡️', GGUF: '🧠', OTHER: '📦' }[t] || '📦'
}

const typeColor = (t) => {
  return { PYTORCH: 'danger', TENSORFLOW: 'warning', ONNX: 'success', SAFETENSORS: 'primary', GGUF: 'info' }[t] || ''
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const typeOption = computed(() => {
  const dist = stats.value?.typeDistribution || {}
  return {
    title: { text: '模型类型分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: Object.entries(dist).map(([name, value]) => ({ name, value }))
    }]
  }
})

const loadStats = async () => {
  try {
    const res = await modelMarketApi.stats()
    stats.value = res.data
  } catch (e) { console.warn(e) }
}

const loadModels = async () => {
  loading.value = true
  try {
    const res = await modelMarketApi.browse({
      modelType: filterType.value || undefined,
      taskType: filterTask.value || undefined,
      keyword: filterKeyword.value || undefined,
      sortBy: filterSort.value || undefined
    })
    models.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const onFileChange = (file) => {
  fileObj.value = file.raw
}

const submitUploadFile = async () => {
  if (!fileObj.value) return ElMessage.warning('请选择文件')
  if (!uploadForm.name) return ElMessage.warning('请填写模型名')
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', fileObj.value)
    fd.append('name', uploadForm.name)
    fd.append('authorId', userStore.profile?.id || 0)
    fd.append('authorName', userStore.profile?.username || 'anon')
    fd.append('description', uploadForm.description || '')
    fd.append('taskType', uploadForm.taskType || '')
    fd.append('baseModel', uploadForm.baseModel || '')
    fd.append('license', uploadForm.license)
    fd.append('tags', uploadForm.tags || '')
    const res = await modelMarketApi.upload(fd)
    if (res.data?.code === 0) {
      ElMessage.success('上传成功!')
      showUpload.value = false
      loadModels()
      loadStats()
    } else {
      ElMessage.error(res.data?.message || '失败')
    }
  } catch (e) {
    ElMessage.error('上传失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

const submitUploadMeta = async () => {
  if (!metaForm.name) return ElMessage.warning('请填写名称')
  uploading.value = true
  try {
    const res = await modelMarketApi.publish({
      ...metaForm,
      authorId: userStore.profile?.id || 0,
      authorName: userStore.profile?.username || 'anon'
    })
    if (res.data?.code === 0) {
      ElMessage.success('发布成功')
      showUpload.value = false
      loadModels()
      loadStats()
    } else {
      ElMessage.error(res.data?.message || '失败')
    }
  } catch (e) {
    ElMessage.error('发布失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

const showDetail = async (m) => {
  const res = await modelMarketApi.detail(m.modelKey)
  detail.value = res.data
  showDetail_.value = true
  rateVisible.value = false
  myRating.value = 0
  myComment.value = ''
}

const downloadModel = () => {
  window.open(`/api/v1/ai/model-market/models/${detail.value.modelKey}/download`, '_blank')
}

const submitRate = async () => {
  if (!myRating.value) return ElMessage.warning('请选择评分')
  try {
    const res = await modelMarketApi.rate(detail.value.modelKey, {
      userId: userStore.profile?.id || 0,
      username: userStore.profile?.username || 'anon',
      rating: myRating.value,
      comment: myComment.value
    })
    if (res.data?.code === 0) {
      ElMessage.success('评分成功')
      const r2 = await modelMarketApi.detail(detail.value.modelKey)
      detail.value = r2.data
      rateVisible.value = false
    } else {
      ElMessage.error(res.data?.message)
    }
  } catch (e) {
    ElMessage.error('失败: ' + e.message)
  }
}

onMounted(async () => {
  await loadStats()
  await loadModels()
})
</script>

<style scoped>
.model-market-container { padding: 20px; }
.header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #e6a23c; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }
.kpi { text-align: center; }
.kpi-label { color: #909399; font-size: 12px; }
.kpi-value { font-size: 22px; font-weight: 600; margin-top: 4px; }
.kpi.success { border-left: 3px solid #67c23a; }
.kpi.primary { border-left: 3px solid #409eff; }
.kpi.warn { border-left: 3px solid #e6a23c; }

.model-card { cursor: pointer; height: 180px; }
.model-card :deep(.el-card__body) { padding: 12px; height: 100%; }
.model-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.model-icon { font-size: 32px; }
.model-icon.big { font-size: 60px; }
.model-meta { flex: 1; min-width: 0; }
.model-name { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.model-author { font-size: 11px; color: #909399; }
.model-desc { font-size: 12px; color: #606266; line-height: 1.4; height: 36px; overflow: hidden; }
.model-tags { margin: 8px 0; min-height: 24px; }
.model-stats { display: flex; justify-content: space-between; font-size: 11px; color: #909399; }

.upload-area { padding: 20px; text-align: center; }
.detail-header { display: flex; align-items: center; gap: 16px; }
.detail-header h2 { margin: 0; font-size: 20px; }
.detail-header p { margin: 4px 0 8px 0; color: #606266; font-size: 13px; }
.detail-actions { margin-top: 12px; }
.rate-row { display: flex; align-items: center; gap: 12px; margin-top: 8px; }
</style>
