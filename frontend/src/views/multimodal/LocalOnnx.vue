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
      <p class="sub">基于 ONNX Runtime 的本地图片/语音/视频/语言智能 (V7.4 · ResNet50/YOLO/CLIP/Whisper/VAD/Video/BGE/Qwen2.5)</p>
    </header>

    <!-- 模型状态卡 -->
    <el-row :gutter="16" class="status-row">
      <el-col :span="8" :md="8" :sm="12" :xs="24" v-for="m in models" :key="m.key">
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

      <!-- 4. 语音转文字 (Whisper) -->
      <el-tab-pane label="语音转文字 (Whisper)" name="whisper">
        <el-card>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="onAudioFile"
            :show-file-list="false"
            accept="audio/*,.wav"
            v-loading="loading.whisper"
          >
            <el-icon class="el-icon--upload"><microphone /></el-icon>
            <div class="el-upload__text">拖拽音频到此处, 或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 WAV/PCM, Whisper-tiny 39M 参数, 中英双语</div>
            </template>
          </el-upload>

          <el-form v-if="audioUrl" style="margin-top: 16px">
            <el-form-item label="语言">
              <el-radio-group v-model="whisperLang">
                <el-radio-button value="zh">中文</el-radio-button>
                <el-radio-button value="en">English</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.whisper" @click="runTranscribe">开始转写</el-button>
            </el-form-item>
            <el-form-item>
              <audio :src="audioUrl" controls style="width: 100%" />
            </el-form-item>
          </el-form>

          <div v-if="whisperText" class="transcribe-result">
            <h3>转写结果 <el-tag size="small">{{ whisperCost }}ms</el-tag></h3>
            <el-input v-model="whisperText" type="textarea" :rows="4" readonly />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 5. 语音活动检测 (VAD) -->
      <el-tab-pane label="语音活动检测 (VAD)" name="vad">
        <el-card>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="onAudioFile"
            :show-file-list="false"
            accept="audio/*,.wav"
            v-loading="loading.vad"
          >
            <el-icon class="el-icon--upload"><video-camera /></el-icon>
            <div class="el-upload__text">拖拽音频到此处, 或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">Silero VAD v5, 16kHz 32ms chunk</div>
            </template>
          </el-upload>

          <el-form v-if="audioUrl" style="margin-top: 16px">
            <el-form-item>
              <audio :src="audioUrl" controls style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.vad" @click="runVad">检测语音段</el-button>
            </el-form-item>
          </el-form>

          <div v-if="vadResult" class="vad-result">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="总时长">{{ vadResult.totalDuration?.toFixed(2) }}s</el-descriptions-item>
              <el-descriptions-item label="语音占比">{{ ((vadResult.speechRatio || 0) * 100).toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="段数">{{ vadResult.segments?.length || 0 }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="vadResult.segments" stripe style="margin-top: 12px">
              <el-table-column label="起始">
                <template #default="{ row }">{{ row.start.toFixed(2) }}s</template>
              </el-table-column>
              <el-table-column label="结束">
                <template #default="{ row }">{{ row.end.toFixed(2) }}s</template>
              </el-table-column>
              <el-table-column label="时长">
                <template #default="{ row }">{{ row.duration.toFixed(2) }}s</template>
              </el-table-column>
              <el-table-column label="置信度">
                <template #default="{ row }">
                  <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 6. 视频智能分析 -->
      <el-tab-pane label="视频智能分析" name="video">
        <el-card>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="onVideoFile"
            :show-file-list="false"
            accept="video/*,.mp4,.mov,.avi"
            v-loading="loading.video"
          >
            <el-icon class="el-icon--upload"><video-play /></el-icon>
            <div class="el-upload__text">拖拽视频到此处, 或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 MP4/MOV/AVI, 复用 ResNet50 + Whisper</div>
            </template>
          </el-upload>

          <el-form v-if="videoUrl" style="margin-top: 16px">
            <el-form-item>
              <video :src="videoUrl" controls style="max-width: 100%; max-height: 360px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.video" @click="runAnalyzeVideo">开始分析</el-button>
            </el-form-item>
          </el-form>

          <div v-if="videoResult" class="video-result">
            <h3>分析结果 <el-tag size="small">{{ videoResult.costMs }}ms</el-tag></h3>

            <!-- 媒体信息 -->
            <el-descriptions v-if="videoResult.media" :column="4" border>
              <el-descriptions-item label="时长">{{ videoResult.media.durationSec?.toFixed(1) }}s</el-descriptions-item>
              <el-descriptions-item label="分辨率">{{ videoResult.media.width }}×{{ videoResult.media.height }}</el-descriptions-item>
              <el-descriptions-item label="帧率">{{ videoResult.media.fps?.toFixed(1) }} fps</el-descriptions-item>
              <el-descriptions-item label="轨道">{{ (videoResult.media.hasVideo ? '视频 ' : '') + (videoResult.media.hasAudio ? '音频' : '') }}</el-descriptions-item>
            </el-descriptions>

            <!-- 关键时间轴 (同类合并) -->
            <div v-if="videoResult.timeline?.length" class="timeline-block">
              <h4>🎬 关键场景时间轴 ({{ videoResult.timeline.length }} 段)</h4>
              <el-table :data="videoResult.timeline" stripe>
                <el-table-column label="时间">
                  <template #default="{ row }">
                    {{ row.start.toFixed(1) }}s ~ {{ row.end.toFixed(1) }}s ({{ row.duration.toFixed(1) }}s)
                  </template>
                </el-table-column>
                <el-table-column label="场景">
                  <template #default="{ row }">
                    <span class="cn">{{ row.labelCn }}</span>
                    <span class="en"> / {{ row.labelEn }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="置信度">
                  <template #default="{ row }">
                    <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 完整文本转写 -->
            <div v-if="videoResult.transcript" class="transcript-block">
              <h4>📝 音轨转写 <el-tag size="small">{{ videoResult.asrCostMs }}ms</el-tag></h4>
              <el-input v-model="videoResult.transcript" type="textarea" :rows="4" readonly />
            </div>

            <el-empty v-if="!videoResult.timeline?.length && !videoResult.transcript"
              description="视频无有效内容或模型未就绪" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 7. BGE 文本 Embedding -->
      <el-tab-pane label="文本 Embedding (BGE)" name="bge">
        <el-card>
          <el-form>
            <el-form-item label="输入文本 (一行一条)">
              <el-input
                v-model="bgeInput"
                type="textarea"
                :rows="6"
                placeholder="例如:&#10;今天天气真好&#10;人工智能改变世界&#10;深度学习是机器学习的一个分支"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.bge" @click="runEmbed">计算 Embedding</el-button>
            </el-form-item>
          </el-form>

          <div v-if="bgeResult" class="bge-result">
            <h3>Embedding 结果 ({{ bgeResult.length }} 个, {{ bgeDim }} 维)</h3>
            <el-table :data="bgeResult" stripe>
              <el-table-column prop="index" label="#" width="60" />
              <el-table-column prop="text" label="文本" />
              <el-table-column label="向量 (前 10 维)">
                <template #default="{ row }">
                  <code class="vec">[{{ row.vector.slice(0, 10).map(v => v.toFixed(3)).join(', ') }}...]</code>
                </template>
              </el-table-column>
              <el-table-column label="范数">
                <template #default="{ row }">
                  {{ Math.sqrt(row.vector.reduce((s, v) => s + v * v, 0)).toFixed(3) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 8. Qwen2.5 对话 -->
      <el-tab-pane label="Qwen2.5 对话" name="qwen">
        <el-card>
          <el-form>
            <el-form-item label="系统 Prompt (可选)">
              <el-input v-model="qwenSystem" placeholder="你是 MiniMax 智能助手, 简洁专业地回答" />
            </el-form-item>
            <el-form-item label="用户输入">
              <el-input
                v-model="qwenInput"
                type="textarea"
                :rows="3"
                placeholder="例如: 介绍一下你自己"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.qwen" @click="runChat">发送</el-button>
            </el-form-item>
          </el-form>

          <div v-if="qwenResult" class="qwen-result">
            <h3>回复 <el-tag size="small">{{ qwenResult.costMs }}ms</el-tag> <el-tag size="small" type="info">{{ qwenResult.length }} 字符</el-tag></h3>
            <el-input v-model="qwenResult.text" type="textarea" :rows="6" readonly />
          </div>
          <el-alert
            v-if="!models.find(m => m.key === 'qwen').ready"
            title="Qwen2.5 模型未就绪"
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 16px"
          >
            <template #default>
              请先执行 <code>./scripts/download-models.sh qwen</code> 下载 Qwen2.5-0.5B-Instruct int4 量化版 (488MB).
            </template>
          </el-alert>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Aim, Picture, Microphone, VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import { multimodalApi } from '@/api/multimodal'

const active = ref('classify')
const currentFile = ref(null)
const previewUrl = ref('')
const loading = reactive({ classify: false, detect: false, clip: false, whisper: false, vad: false, video: false, bge: false, qwen: false })

const models = ref([
  { key: 'resnet50', name: 'ResNet50 分类', icon: '🏷️', ready: false, path: '' },
  { key: 'clip',     name: 'CLIP 双塔',   icon: '🔗', ready: false, path: '' },
  { key: 'yolo',     name: 'YOLOv8 检测', icon: '🎯', ready: false, path: '' },
  { key: 'whisper',  name: 'Whisper STT', icon: '🎙️', ready: false, path: '' },
  { key: 'vad',      name: 'Silero VAD', icon: '🔊', ready: false, path: '' },
  { key: 'video',    name: '视频智能',  icon: '🎬', ready: false, path: '' },
  { key: 'bge',      name: 'BGE 中文 Embedding', icon: '📐', ready: false, path: '' },
  { key: 'qwen',     name: 'Qwen2.5 对话',     icon: '💬', ready: false, path: '' }
])

const classifications = ref([])
const detections = ref([])
const clipScore = ref(null)
const clipText = ref('a cat sitting on a sofa')

// Audio
const currentAudio = ref(null)
const audioUrl = ref('')
const whisperText = ref('')
const whisperCost = ref(0)
const whisperLang = ref('zh')
const vadResult = ref(null)
// Video
const currentVideo = ref(null)
const videoUrl = ref('')
const videoResult = ref(null)
// BGE + Qwen
const bgeInput = ref('今天天气真好\n人工智能改变世界\n深度学习是机器学习的一个分支')
const bgeResult = ref(null)
const bgeDim = ref(0)
const qwenSystem = ref('你是 MiniMax 智能助手, 简洁专业地回答')
const qwenInput = ref('用一句话介绍一下你自己')
const qwenResult = ref(null)

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
      models.value[3].ready = d.whisper.ready
      models.value[3].path = d.whisper.path
      models.value[4].ready = d.vad.ready
      models.value[4].path = d.vad.path
      models.value[5].ready = d.video.available
      models.value[5].path = '复用 ResNet50 + Whisper'
      models.value[6].ready = d.bge.ready
      models.value[6].path = d.bge.path
      models.value[7].ready = d.qwen.ready
      models.value[7].path = d.qwen.path  // V7.4
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

function onAudioFile(file) {
  if (!file?.raw) return
  currentAudio.value = file.raw
  audioUrl.value = URL.createObjectURL(file.raw)
  whisperText.value = ''
  vadResult.value = null
  if (active.value === 'whisper') runTranscribe()
  else if (active.value === 'vad') runVad()
}

async function runTranscribe() {
  if (!currentAudio.value) return ElMessage.warning('请先上传音频')
  loading.whisper = true
  try {
    const res = await multimodalApi.transcribe(currentAudio.value, whisperLang.value)
    if (res.code === 0) {
      whisperText.value = res.data.text || ''
      whisperCost.value = res.data.costMs
      if (!whisperText.value) ElMessage.info('未识别到语音内容')
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '转写失败')
    }
  } catch (e) {
    ElMessage.error('转写失败: ' + (e.message || ''))
  } finally {
    loading.whisper = false
  }
}

async function runVad() {
  if (!currentAudio.value) return ElMessage.warning('请先上传音频')
  loading.vad = true
  try {
    const res = await multimodalApi.vad(currentAudio.value)
    if (res.code === 0) {
      vadResult.value = res.data
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || 'VAD 失败')
    }
  } catch (e) {
    ElMessage.error('VAD 失败: ' + (e.message || ''))
  } finally {
    loading.vad = false
  }
}

function onVideoFile(file) {
  if (!file?.raw) return
  currentVideo.value = file.raw
  videoUrl.value = URL.createObjectURL(file.raw)
  videoResult.value = null
}

async function runAnalyzeVideo() {
  if (!currentVideo.value) return ElMessage.warning('请先上传视频')
  loading.video = true
  try {
    const res = await multimodalApi.analyzeVideo(currentVideo.value)
    if (res.code === 0) {
      videoResult.value = res.data
      const total = res.data.timeline?.length || 0
      ElMessage.success(`分析完成: ${total} 段场景${res.data.transcript ? ' + 文本' : ''}`)
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '分析失败')
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + (e.message || ''))
  } finally {
    loading.video = false
  }
}

async function runEmbed() {
  if (!bgeInput.value.trim()) return ElMessage.warning('请输入文本')
  const texts = bgeInput.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (!texts.length) return ElMessage.warning('请输入有效文本')
  loading.bge = true
  try {
    const res = await multimodalApi.embedText(texts)
    if (res.code === 0) {
      bgeResult.value = res.data
      bgeDim.value = res.data[0]?.dim || 0
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '计算失败')
    }
  } catch (e) {
    ElMessage.error('计算失败: ' + (e.message || ''))
  } finally {
    loading.bge = false
  }
}

async function runChat() {
  if (!qwenInput.value.trim()) return ElMessage.warning('请输入内容')
  loading.qwen = true
  try {
    const res = await multimodalApi.chatQwen(qwenInput.value, qwenSystem.value || null)
    if (res.code === 0) {
      qwenResult.value = res.data
      if (!res.data.text) ElMessage.info('生成内容为空')
    } else if (res.code === 1001) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.error(res.message || '对话失败')
    }
  } catch (e) {
    ElMessage.error('对话失败: ' + (e.message || ''))
  } finally {
    loading.qwen = false
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

.transcribe-result { margin-top: 24px; }
.transcribe-result h3 { margin: 0 0 12px; color: #1e293b; }
.vad-result { margin-top: 24px; }
.vad-result h3 { margin: 0 0 12px; color: #1e293b; }

.video-result { margin-top: 24px; }
.video-result h3 { margin: 0 0 12px; color: #1e293b; }
.video-result h4 { margin: 16px 0 8px; color: #334155; font-size: 1.05em; }
.timeline-block .cn { font-weight: 600; color: #1e293b; }
.timeline-block .en { color: #64748b; font-size: 0.9em; }
.transcript-block { margin-top: 16px; }

.bge-result { margin-top: 24px; }
.bge-result h3 { margin: 0 0 12px; color: #1e293b; }
.bge-result .vec { font-size: 0.8em; color: #475569; }
.qwen-result { margin-top: 24px; }
.qwen-result h3 { margin: 0 0 12px; color: #1e293b; }
</style>
