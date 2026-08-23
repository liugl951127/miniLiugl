<!--
  @file multimodal/Video.vue - 视频能力 (V7.7)
  路由: /multimodal/video
  包含: 视频理解 + 视频生成 + 人脸识别
-->
<template>
  <div class="video-page">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- 视频理解 -->
      <el-tab-pane label="视频理解" name="video-understand">
        <el-card v-loading="understandLoading">
          <template #header><span>🎥 视频理解</span></template>
          <el-upload drag :auto-upload="false" :on-change="onVideoUpload" :show-file-list="false" accept="video/*">
            <el-icon class="el-icon--upload"><VideoPlay /></el-icon>
            <div class="el-upload__text">拖拽视频, 或<em>点击上传</em></div>
          </el-upload>
          <el-input v-if="videoFile" v-model="understandPrompt" type="textarea" :rows="2" placeholder="可选: 具体问题" style="margin-top: 12px" />
          <el-button v-if="videoFile" type="primary" :loading="understandLoading" :icon="Search" @click="analyzeVideo" style="margin-top: 8px">分析</el-button>
          <div v-if="understandResult" class="result-text" style="margin-top: 12px">
            <h4>分析结果</h4>
            <el-input v-model="understandResult" type="textarea" :rows="4" readonly />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 视频生成 -->
      <el-tab-pane label="视频生成" name="video-gen">
        <el-card v-loading="genLoading">
          <template #header><span>🎬 视频生成 <el-tag size="small" type="warning">Beta</el-tag></span></template>
          <el-input v-model="genPrompt" type="textarea" :rows="3" placeholder="描述想生成的视频" />
          <el-button type="primary" :loading="genLoading" :icon="VideoCamera" @click="generateVideo" style="margin-top: 12px">生成</el-button>
          <div v-if="genResult?.url" class="video-preview" style="margin-top: 12px">
            <video :src="genResult.url" controls style="max-width: 100%; max-height: 360px" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 人脸识别 -->
      <el-tab-pane label="人脸识别" name="face">
        <el-card>
          <template #header><span>👤 人脸识别</span></template>
          <el-alert type="info" :closable="false" show-icon
            title="需要摄像头权限"
            description="浏览器请求摄像头后, 拍照上传, 系统会检测人脸位置、年龄、表情等"
            style="margin-bottom: 12px"
          />
          <video ref="videoEl" autoplay style="width: 100%; max-height: 360px; background: #000; border-radius: 8px" />
          <el-button @click="captureFrame" :icon="Camera" style="margin-top: 8px">拍照</el-button>
          <div v-if="capturedUrl" class="face-preview" style="margin-top: 12px">
            <img :src="capturedUrl" alt="captured" style="max-width: 100%; border-radius: 8px" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoCamera, Search, Camera } from '@element-plus/icons-vue'
import { videoUnderstand, videoGenerate } from '@/api/multimodal'

const activeTab = ref('video-understand')

const videoFile = ref(null)
const understandPrompt = ref('')
const understandLoading = ref(false)
const understandResult = ref('')

const genPrompt = ref('')
const genLoading = ref(false)
const genResult = ref(null)

const videoEl = ref(null)
const capturedUrl = ref('')
let mediaStream = null

function onVideoUpload(file) {
  if (!file?.raw) return
  videoFile.value = file.raw
  understandResult.value = ''
}

async function analyzeVideo() {
  if (!videoFile.value) return ElMessage.warning('请上传视频')
  understandLoading.value = true
  try {
    // 上传视频获取 URL, 调 videoUnderstand
    const url = URL.createObjectURL(videoFile.value)
    const res = await videoUnderstand(url, understandPrompt.value)
    if (res.code === 0) {
      understandResult.value = res.data?.text || res.data?.description || JSON.stringify(res.data)
      ElMessage.success('分析完成')
    } else ElMessage.error(res.message || '分析失败')
  } finally { understandLoading.value = false }
}

async function generateVideo() {
  if (!genPrompt.value.trim()) return ElMessage.warning('请输入描述')
  genLoading.value = true
  try {
    const res = await videoGenerate({ prompt: genPrompt.value })
    if (res.code === 0) {
      genResult.value = res.data
      ElMessage.success('生成完成')
    } else ElMessage.error(res.message || '生成失败')
  } finally { genLoading.value = false }
}

async function startCamera() {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: true })
    if (videoEl.value) videoEl.value.srcObject = mediaStream
  } catch (e) {
    ElMessage.warning('无法访问摄像头: ' + e.message)
  }
}

function captureFrame() {
  if (!videoEl.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoEl.value.videoWidth
  canvas.height = videoEl.value.videoHeight
  canvas.getContext('2d').drawImage(videoEl.value, 0, 0)
  capturedUrl.value = canvas.toDataURL('image/png')
  ElMessage.success('已拍照')
}

onMounted(() => {
  if (activeTab.value === 'face') startCamera()
})
onBeforeUnmount(() => {
  if (mediaStream) mediaStream.getTracks().forEach(t => t.stop())
})
</script>

<style scoped>
.video-page { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.result-text h4 { margin: 0 0 8px; color: #1e293b; }
</style>
