<!--
  @file multimodal/LocalAudio.vue - 本地语音智能 (V7.6)
  路由: /multimodal/local/audio
-->
<template>
  <div class="local-audio">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <el-tab-pane label="转写 (Whisper)" name="whisper">
        <el-upload drag :auto-upload="false" :on-change="onAudioFile" :show-file-list="false"
          accept="audio/*,.wav" v-loading="loading.whisper">
          <el-icon class="el-icon--upload"><Microphone /></el-icon>
          <div class="el-upload__text">拖拽音频, 或<em>点击上传</em></div>
          <div class="el-upload__tip">WAV/PCM, 39M 参数, 中英双语</div>
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
          <audio :src="audioUrl" controls style="width: 100%" />
        </el-form>
        <div v-if="whisperText" class="transcribe-result">
          <h3>转写结果 <el-tag size="small">{{ whisperCost }}ms</el-tag></h3>
          <el-input v-model="whisperText" type="textarea" :rows="4" readonly />
        </div>
      </el-tab-pane>

      <el-tab-pane label="语音活动 (VAD)" name="vad">
        <el-upload drag :auto-upload="false" :on-change="onAudioFile" :show-file-list="false"
          accept="audio/*,.wav" v-loading="loading.vad">
          <el-icon class="el-icon--upload"><VideoCamera /></el-icon>
          <div class="el-upload__text">拖拽音频, 或<em>点击上传</em></div>
          <div class="el-upload__tip">16kHz 32ms chunk</div>
        </el-upload>
        <el-form v-if="audioUrl" style="margin-top: 16px">
          <audio :src="audioUrl" controls style="width: 100%" />
          <el-button type="primary" :loading="loading.vad" @click="runVad" style="margin-top: 8px">检测语音段</el-button>
        </el-form>
        <div v-if="vadResult" class="vad-result">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="总时长">{{ vadResult.totalDuration?.toFixed(2) }}s</el-descriptions-item>
            <el-descriptions-item label="语音占比">{{ ((vadResult.speechRatio || 0) * 100).toFixed(1) }}%</el-descriptions-item>
            <el-descriptions-item label="段数">{{ vadResult.segments?.length || 0 }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, VideoCamera } from '@element-plus/icons-vue'
import { multimodalApi } from '@/api/multimodal'

const activeTab = ref('whisper')
const currentAudio = ref(null)
const audioUrl = ref('')
const whisperText = ref('')
const whisperCost = ref(0)
const whisperLang = ref('zh')
const vadResult = ref(null)
const loading = reactive({ whisper: false, vad: false })

function onAudioFile(file) {
  if (!file?.raw) return
  currentAudio.value = file.raw
  audioUrl.value = URL.createObjectURL(file.raw)
  whisperText.value = ''
  vadResult.value = null
  if (activeTab.value === 'whisper') runTranscribe()
  else if (activeTab.value === 'vad') runVad()
}

async function runTranscribe() {
  if (!currentAudio.value) return ElMessage.warning('请上传音频')
  loading.whisper = true
  try {
    const res = await multimodalApi.transcribe(currentAudio.value, whisperLang.value)
    if (res.code === 0) {
      whisperText.value = res.data.text || ''
      whisperCost.value = res.data.costMs
    } else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message)
  } finally { loading.whisper = false }
}

async function runVad() {
  if (!currentAudio.value) return ElMessage.warning('请上传音频')
  loading.vad = true
  try {
    const res = await multimodalApi.vad(currentAudio.value)
    if (res.code === 0) vadResult.value = res.data
    else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message)
  } finally { loading.vad = false }
}
</script>

<style scoped>
.local-audio { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.transcribe-result, .vad-result { margin-top: 16px; }
h3 { margin: 0 0 12px; color: #1e293b; }
</style>
