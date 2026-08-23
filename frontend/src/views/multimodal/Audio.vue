<!--
  @file multimodal/Audio.vue - 语音能力 (V7.7)
  路由: /multimodal/audio
  包含: TTS + ASR
-->
<template>
  <div class="audio-page">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- TTS -->
      <el-tab-pane label="语音合成 (TTS)" name="tts">
        <el-card v-loading="ttsLoading">
          <template #header><span>🔊 文本转语音</span></template>
          <el-input v-model="ttsText" type="textarea" :rows="3" placeholder="输入要合成的文本" />
          <el-form inline style="margin-top: 12px">
            <el-form-item label="声音">
              <el-select v-model="ttsVoice" style="width: 160px">
                <el-option v-for="v in voices" :key="v.code || v" :label="v.name || v" :value="v.code || v" />
              </el-select>
            </el-form-item>
            <el-form-item label="语速">
              <el-input-number v-model="ttsSpeed" :min="0.5" :max="2" :step="0.1" />
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="ttsLoading" :icon="Microphone" @click="runTts">合成</el-button>
          <audio v-if="ttsAudioUrl" :src="ttsAudioUrl" controls style="width: 100%; margin-top: 12px" />
        </el-card>
      </el-tab-pane>

      <!-- ASR -->
      <el-tab-pane label="语音识别 (ASR)" name="asr">
        <el-card v-loading="asrLoading">
          <template #header><span>🎤 语音转文本</span></template>
          <el-upload drag :auto-upload="false" :on-change="onAudioUpload" :show-file-list="false" accept="audio/*">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽音频, 或<em>点击上传</em></div>
          </el-upload>
          <el-button v-if="audioFile" type="primary" :loading="asrLoading" :icon="VideoCamera" @click="runAsr" style="margin-top: 12px">识别</el-button>
          <div v-if="asrResult" class="result-text" style="margin-top: 12px">
            <h4>识别结果</h4>
            <el-input v-model="asrResult" type="textarea" :rows="3" readonly />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, UploadFilled, VideoCamera } from '@element-plus/icons-vue'
import { audioTts, audioAsr, audioTtsVoices } from '@/api/multimodal'

const activeTab = ref('tts')

const ttsText = ref('')
const ttsVoice = ref('female')
const ttsSpeed = ref(1.0)
const ttsLoading = ref(false)
const ttsAudioUrl = ref('')
const voices = ref([])

const audioFile = ref(null)
const asrLoading = ref(false)
const asrResult = ref('')

async function loadVoices() {
  try {
    const res = await audioTtsVoices()
    if (res.code === 0) {
      voices.value = res.data?.list || res.data || [{ code: 'female', name: '女声' }, { code: 'male', name: '男声' }]
    } else {
      voices.value = [{ code: 'female', name: '女声' }, { code: 'male', name: '男声' }]
    }
  } catch (e) {
    voices.value = [{ code: 'female', name: '女声' }, { code: 'male', name: '男声' }]
  }
}

async function runTts() {
  if (!ttsText.value.trim()) return ElMessage.warning('请输入文本')
  ttsLoading.value = true
  try {
    const res = await audioTts({ text: ttsText.value, voice: ttsVoice.value, speed: ttsSpeed.value })
    if (res.code === 0) {
      ttsAudioUrl.value = res.data?.url || res.data?.audioUrl || ''
      ElMessage.success('合成完成')
    } else ElMessage.error(res.message || '合成失败')
  } finally { ttsLoading.value = false }
}

function onAudioUpload(file) {
  if (!file?.raw) return
  audioFile.value = file.raw
  asrResult.value = ''
}

async function runAsr() {
  if (!audioFile.value) return ElMessage.warning('请上传音频')
  asrLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', audioFile.value)
    const res = await audioAsr(fd)
    if (res.code === 0) {
      asrResult.value = res.data?.text || JSON.stringify(res.data)
      ElMessage.success('识别完成')
    } else ElMessage.error(res.message || '识别失败')
  } finally { asrLoading.value = false }
}

onMounted(loadVoices)
</script>

<style scoped>
.audio-page { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.result-text h4 { margin: 0 0 8px; color: #1e293b; }
</style>
