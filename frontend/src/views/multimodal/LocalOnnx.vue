<!--
  @file multimodal/LocalOnnx.vue - 本地 ONNX 多模态 V7.1
  
  提供:
    1. 图片分类 (ResNet50 ONNX)
    2. 目标检测 (YOLOv8n ONNX)
    3. 文图相似度 (CLIP-like 双塔: ResNet50 + BPE)
  
  路由: /multimodal/local
  后端: /api/v1/multimodal/* (V7.1 minimax-ai)
-->
<template>
  <div class="mm-page">
    <header class="mm-header">
      <h1>🎨 本地多模态智能</h1>
      <p class="sub">基于 ONNX Runtime 的本地图片分类 / 目标检测 / 以文搜图 (V7.1)</p>
    </header>

    <!-- 模型状态卡 -->
    <el-row :gutter="16" class="status-row">
      <el-col :span="8" v-for="m in models" :key="m.key">
        <el-card class="status-card" :class="{ ready: m.ready }">
          <div class="status-row-inner">
            <div class="status-icon">{{ m.icon }}</div>
            <div>
              <div class="status-name">{{ m.name }}</div>
              <div class="status-detail">
                <el-tag v-if="m.ready" type="success" size="small">就绪</el-tag>
                <el-tag v-else type="warning" size="small">未就绪</el-tag>
                <span class="path">{{ m.path }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="active" class="mm-tabs">
      <!-- 1. 分类 -->
      <el-tab-pane label="图片分类 (ResNet50)" name="classify">
        <el-card>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="onFileChange"
            :show-file-list="false"
            accept="image/*"
            v-loading="loading.classify"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽图片到此处, 或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png/webp, ResNet50 1000 类</div>
            </template>
          </el-upload>

          <div v-if="previewUrl" class="preview-row">
            <img :src="previewUrl" class="preview-img" />
            <div class="result-list">
              <h3>Top-{{ classifications.length }} 分类结果</h3>
              <el-empty v-if="!classifications.length && !loading.classify" description="上传图片后自动分类" />
              <div v-for="(c, i) in classifications" :key="i" class="class-row">
                <div class="rank">#{{ i + 1 }}</div>
                <div class="class-info">
                  <div class="class-name">
                    <span class="cn">{{ c.labelCn }}</span>
                    <span class="en">{{ c.labelEn }}</span>
                  </div>
                  <el-progress
                    :percentage="Math.round(c.probability * 100)"
                    :stroke-width="14"
                    :color="i === 0 ? '#67c23a' : '#909399'"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 2. 目标检测 -->
      <el-tab-pane label="目标检测 (YOLOv8)" name="detect">
        <el-card>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="onFileChange"
            :show-file-list="false"
            accept="image/*"
            v-loading="loading.detect"
          >
            <el-icon class="el-icon--upload"><aim /></el-icon>
            <div class="el-upload__text">拖拽图片到此处, 或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png/webp, YOLOv8 80 类 COCO</div>
            </template>
          </el-upload>

          <div v-if="previewUrl" class="preview-row">
            <div class="detect-canvas">
              <img ref="detectImg" :src="previewUrl" class="preview-img" @load="drawDetections" />
              <canvas ref="detectCanvas" class="overlay"></canvas>
            </div>
            <div class="result-list">
              <h3>检测到 {{ detections.length }} 个目标</h3>
              <el-empty v-if="!detections.length && !loading.detect" description="上传图片后自动检测" />
              <el-table :data="detections" stripe>
                <el-table-column prop="class" label="类别" />
                <el-table-column label="置信度">
                  <template #default="{ row }">
                    <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
                  </template>
                </el-table-column>
                <el-table-column label="位置">
                  <template #default="{ row }">
                    <code>[{{ Math.round(row.bbox[0]) }}, {{ Math.round(row.bbox[1]) }}, {{ Math.round(row.bbox[2]) }}×{{ Math.round(row.bbox[3]) }}]</code>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 3. 文图相似度 -->
      <el-tab-pane label="文图相似度 (CLIP-like)" name="clip">
        <el-card>
          <el-form>
            <el-form-item label="查询文本">
              <el-input v-model="clipText" placeholder="输入英文或中文 (例: a cat sitting on a sofa)" clearable />
            </el-form-item>
            <el-form-item label="图片">
              <el-upload
                drag
                :auto-upload="false"
                :on-change="onFileChange"
                :show-file-list="false"
                accept="image/*"
              >
                <el-icon class="el-icon--upload"><picture /></el-icon>
                <div class="el-upload__text">拖拽图片到此处, 或<em>点击上传</em></div>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.clip" @click="runSimilarity">计算相似度</el-button>
            </el-form-item>
          </el-form>

          <div v-if="previewUrl" class="preview-row">
            <img :src="previewUrl" class="preview-img" />
            <div class="result-list">
              <h3>相似度结果</h3>
              <el-statistic
                v-if="clipScore !== null"
                :value="clipScore"
                :precision="4"
                :title="'Cosine Similarity'"
              />
              <el-alert
                v-if="!models.find(m => m.key === 'clip').ready"
                title="CLIP 真实模型未加载, 当前使用 ResNet50 + BPE fallback"
                type="info"
                :closable="false"
                show-icon
                style="margin-top: 16px"
              />
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Aim, Picture } from '@element-plus/icons-vue'
import { multimodalApi } from '@/api/multimodal'

const active = ref('classify')
const currentFile = ref(null)
const previewUrl = ref('')
const loading = reactive({ classify: false, detect: false, clip: false })

const models = ref([
  { key: 'resnet50', name: 'ResNet50 分类', icon: '🏷️', ready: false, path: '' },
  { key: 'clip',     name: 'CLIP 双塔',   icon: '🔗', ready: false, path: '' },
  { key: 'yolo',     name: 'YOLOv8 检测', icon: '🎯', ready: false, path: '' }
])

const classifications = ref([])
const detections = ref([])
const clipScore = ref(null)
const clipText = ref('a cat sitting on a sofa')

const detectImg = ref(null)
const detectCanvas = ref(null)

async function loadStatus() {
  try {
    const res = await multimodalApi.status()
    if (res.code === 0) {
      const d = res.data
      models.value[0].ready = d.resnet50.ready
      models.value[0].path = d.resnet50.path
      models.value[1].ready = d.clip.ready
      models.value[1].path = '(BPE tokenizer)'
      models.value[2].ready = d.yolo.ready
      models.value[2].path = d.yolo.path
    }
  } catch (e) {
    console.error('loadStatus', e)
  }
}

function onFileChange(file) {
  if (!file?.raw) return
  currentFile.value = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
  classifications.value = []
  detections.value = []
  clipScore.value = null
  if (active.value === 'classify') runClassify()
  else if (active.value === 'detect') runDetect()
}

async function runClassify() {
  if (!currentFile.value) return ElMessage.warning('请先上传图片')
  loading.classify = true
  try {
    const res = await multimodalApi.classify(currentFile.value, 5)
    if (res.code === 0) {
      classifications.value = res.data || []
      if (!classifications.value.length) ElMessage.info('模型未识别出任何类别')
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '分类失败')
    }
  } catch (e) {
    ElMessage.error('分类失败: ' + (e.message || ''))
  } finally {
    loading.classify = false
  }
}

async function runDetect() {
  if (!currentFile.value) return ElMessage.warning('请先上传图片')
  loading.detect = true
  try {
    const res = await multimodalApi.detect(currentFile.value)
    if (res.code === 0) {
      detections.value = res.data || []
      if (!detections.value.length) ElMessage.info('未检测到目标')
      else setTimeout(drawDetections, 100)
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '检测失败')
    }
  } catch (e) {
    ElMessage.error('检测失败: ' + (e.message || ''))
  } finally {
    loading.detect = false
  }
}

function drawDetections() {
  const img = detectImg.value
  const canvas = detectCanvas.value
  if (!img || !canvas) return
  canvas.width = img.clientWidth
  canvas.height = img.clientHeight
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  const scaleX = canvas.width / img.naturalWidth
  const scaleY = canvas.height / img.naturalHeight
  detections.value.forEach((d, i) => {
    const [x, y, w, h] = d.bbox
    const x1 = x * scaleX, y1 = y * scaleY, x2 = (x + w) * scaleX, y2 = (y + h) * scaleY
    const colors = ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#9c27b0']
    const color = colors[i % colors.length]
    ctx.strokeStyle = color
    ctx.lineWidth = 3
    ctx.strokeRect(x1, y1, x2 - x1, y2 - y1)
    ctx.fillStyle = color
    ctx.font = '14px sans-serif'
    const label = `${d.class} ${(d.confidence * 100).toFixed(1)}%`
    const tw = ctx.measureText(label).width + 10
    ctx.fillRect(x1, y1 - 22, tw, 22)
    ctx.fillStyle = '#fff'
    ctx.fillText(label, x1 + 5, y1 - 6)
  })
}

async function runSimilarity() {
  if (!currentFile.value) return ElMessage.warning('请先上传图片')
  if (!clipText.value.trim()) return ElMessage.warning('请输入文本')
  loading.clip = true
  try {
    const res = await multimodalApi.similarity(currentFile.value, clipText.value)
    if (res.code === 0) {
      clipScore.value = res.data.score
    } else {
      ElMessage.error(res.message || '计算失败')
    }
  } catch (e) {
    ElMessage.error('计算失败: ' + (e.message || ''))
  } finally {
    loading.clip = false
  }
}

onMounted(loadStatus)
</script>

<style scoped>
.mm-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.mm-header h1 { font-size: 1.8em; margin: 0; color: #1e293b; }
.mm-header .sub { color: #64748b; margin: 4px 0 24px; }
.status-row { margin-bottom: 16px; }
.status-card { border-radius: 12px; }
.status-card.ready { border-color: #67c23a; background: linear-gradient(135deg, #f0f9eb 0%, #fff 100%); }
.status-row-inner { display: flex; align-items: center; gap: 12px; }
.status-icon { font-size: 32px; }
.status-name { font-weight: 600; font-size: 1.05em; }
.status-detail { font-size: 0.85em; color: #64748b; margin-top: 4px; }
.status-detail .path { display: block; margin-top: 2px; word-break: break-all; opacity: 0.6; }
.mm-tabs { background: white; border-radius: 12px; padding: 16px; }
.preview-row { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 16px; }
.preview-img { max-width: 100%; max-height: 480px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.detect-canvas { position: relative; display: inline-block; }
.detect-canvas .overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }
.result-list h3 { margin: 0 0 12px; color: #1e293b; }
.class-row { display: flex; align-items: center; gap: 12px; margin: 12px 0; }
.rank { font-size: 1.4em; font-weight: 700; color: #94a3b8; min-width: 36px; }
.class-info { flex: 1; }
.class-name .cn { font-weight: 600; margin-right: 8px; color: #1e293b; }
.class-name .en { color: #64748b; font-size: 0.9em; }
@media (max-width: 768px) { .preview-row { grid-template-columns: 1fr; } }
</style>
