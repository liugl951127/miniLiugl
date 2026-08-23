<!--
  @file multimodal/LocalImage.vue - 本地图片智能 (V7.6)
  路由: /multimodal/local/image
  合并: 分类 + 检测 + 相似度 (原 3 tab)
-->
<template>
  <div class="local-image">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- 图片分类 -->
      <el-tab-pane label="分类 (ResNet50)" name="classify">
        <el-upload drag :auto-upload="false" :on-change="onFileChange" :show-file-list="false"
          accept="image/*" v-loading="loading.classify">
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽图片到此处, 或<em>点击上传</em></div>
          <div class="el-upload__tip">支持 jpg/png/webp, 1000 类 ImageNet</div>
        </el-upload>
        <div v-if="previewUrl" class="preview-row">
          <img :src="previewUrl" class="preview-img" />
          <div class="result-list">
            <h3>Top-{{ classifications.length }} 分类</h3>
            <el-empty v-if="!classifications.length && !loading.classify" description="上传后自动分类" />
            <div v-for="(c, i) in classifications" :key="i" class="class-row">
              <div class="rank">#{{ i + 1 }}</div>
              <div class="class-info">
                <div class="class-name">
                  <span class="cn">{{ c.labelCn }}</span>
                  <span class="en">{{ c.labelEn }}</span>
                </div>
                <el-progress :percentage="Math.round(c.probability * 100)" :stroke-width="14" />
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 目标检测 -->
      <el-tab-pane label="检测 (YOLOv8)" name="detect">
        <el-upload drag :auto-upload="false" :on-change="onFileChange" :show-file-list="false"
          accept="image/*" v-loading="loading.detect">
          <el-icon class="el-icon--upload"><Aim /></el-icon>
          <div class="el-upload__text">拖拽图片到此处, 或<em>点击上传</em></div>
          <div class="el-upload__tip">支持 jpg/png/webp, 80 类 COCO</div>
        </el-upload>
        <div v-if="previewUrl" class="preview-row">
          <div class="detect-canvas">
            <img ref="detectImg" :src="previewUrl" class="preview-img" @load="drawDetections" />
            <canvas ref="detectCanvas" class="overlay"></canvas>
          </div>
          <div class="result-list">
            <h3>检测到 {{ detections.length }} 个目标</h3>
            <el-empty v-if="!detections.length && !loading.detect" description="上传后自动检测" />
            <el-table :data="detections" stripe>
              <el-table-column prop="class" label="类别" />
              <el-table-column label="置信度">
                <template #default="{ row }">
                  <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 文图相似度 -->
      <el-tab-pane label="以文搜图 (CLIP)" name="clip">
        <el-form>
          <el-form-item label="查询文本">
            <el-input v-model="clipText" placeholder="a cat sitting on a sofa" clearable />
          </el-form-item>
          <el-form-item label="图片">
            <el-upload drag :auto-upload="false" :on-change="onFileChange" :show-file-list="false" accept="image/*">
              <el-icon class="el-icon--upload"><Picture /></el-icon>
              <div class="el-upload__text">拖拽图片到此处</div>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading.clip" @click="runSimilarity">计算</el-button>
          </el-form-item>
        </el-form>
        <div v-if="previewUrl" class="preview-row">
          <img :src="previewUrl" class="preview-img" />
          <div class="result-list">
            <h3>相似度</h3>
            <el-statistic v-if="clipScore !== null" :value="clipScore" :precision="4" title="Cosine" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Aim, Picture } from '@element-plus/icons-vue'
import { multimodalApi } from '@/api/multimodal'

const activeTab = ref('classify')
const currentFile = ref(null)
const previewUrl = ref('')
const loading = reactive({ classify: false, detect: false, clip: false })

const classifications = ref([])
const detections = ref([])
const clipText = ref('a cat sitting on a sofa')
const clipScore = ref(null)

const detectImg = ref(null)
const detectCanvas = ref(null)

function onFileChange(file) {
  if (!file?.raw) return
  currentFile.value = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
  classifications.value = []
  detections.value = []
  clipScore.value = null
  if (activeTab.value === 'classify') runClassify()
  else if (activeTab.value === 'detect') runDetect()
}

async function runClassify() {
  if (!currentFile.value) return ElMessage.warning('请上传图片')
  loading.classify = true
  try {
    const res = await multimodalApi.classify(currentFile.value, 5)
    if (res.code === 0) classifications.value = res.data || []
    else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message || '失败')
  } finally { loading.classify = false }
}

async function runDetect() {
  if (!currentFile.value) return ElMessage.warning('请上传图片')
  loading.detect = true
  try {
    const res = await multimodalApi.detect(currentFile.value)
    if (res.code === 0) {
      detections.value = res.data || []
      setTimeout(drawDetections, 100)
    } else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message || '失败')
  } finally { loading.detect = false }
}

function drawDetections() {
  const img = detectImg.value
  const canvas = detectCanvas.value
  if (!img || !canvas) return
  canvas.width = img.clientWidth
  canvas.height = img.clientHeight
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  const sx = canvas.width / img.naturalWidth
  const sy = canvas.height / img.naturalHeight
  detections.value.forEach((d, i) => {
    const [x, y, w, h] = d.bbox
    const colors = ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#9c27b0']
    const c = colors[i % colors.length]
    ctx.strokeStyle = c
    ctx.lineWidth = 3
    ctx.strokeRect(x * sx, y * sy, w * sx, h * sy)
    ctx.fillStyle = c
    ctx.font = '14px sans-serif'
    ctx.fillText(`${d.class} ${(d.confidence * 100).toFixed(0)}%`, x * sx + 4, y * sy - 4)
  })
}

async function runSimilarity() {
  if (!currentFile.value) return ElMessage.warning('请上传图片')
  loading.clip = true
  try {
    const res = await multimodalApi.similarity(currentFile.value, clipText.value)
    if (res.code === 0) clipScore.value = res.data.score
    else ElMessage.error(res.message)
  } finally { loading.clip = false }
}
</script>

<style scoped>
.local-image { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.preview-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.preview-img { max-width: 100%; max-height: 360px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.detect-canvas { position: relative; display: inline-block; }
.detect-canvas .overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }
.result-list h3 { margin: 0 0 12px; color: #1e293b; }
.class-row { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
.rank { font-size: 1.2em; font-weight: 700; color: #94a3b8; min-width: 30px; }
.class-name .cn { font-weight: 600; margin-right: 8px; }
.class-name .en { color: #64748b; font-size: 0.9em; }
</style>
