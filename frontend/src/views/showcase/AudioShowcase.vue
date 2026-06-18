<!--
  语音能力 ShowCase (V4.1)
  - ASR 语音转文字 (上传音频 + Whisper/Paraformer)
  - TTS 文字转语音 (5 个音色 + Edge-TTS/CosyVoice)
  - 用浏览器原生 MediaRecorder 录音
-->
<template>
  <div class="audio">
    <header class="header">
      <h1>🎤 语音能力</h1>
      <p class="subtitle">ASR 语音转文字 + TTS 文字转语音, 5 个真实模型</p>
      <div class="badges">
        <span class="badge">Whisper V3</span>
        <span class="badge">SenseVoice</span>
        <span class="badge">CosyVoice</span>
        <span class="badge">Edge-TTS</span>
      </div>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ASR -->
      <el-tab-pane label="🎙 ASR (语音转文字)" name="asr">
        <div class="asr-grid">
          <section class="input-side">
            <h3>录音 / 上传</h3>
            <div class="record-area">
              <div v-if="!recording && !audioBlob" class="placeholder">
                <el-icon :size="48"><Microphone /></el-icon>
                <p>点 "开始录音" 或上传音频文件 (支持 mp3/wav/m4a/ogg)</p>
              </div>
              <div v-else-if="recording" class="recording">
                <div class="rec-pulse"></div>
                <span>正在录音... {{ recordSeconds }}s</span>
              </div>
              <div v-else class="audio-preview">
                <audio :src="audioUrl" controls style="width: 100%"></audio>
              </div>
            </div>
            <div class="actions">
              <el-button v-if="!recording" type="primary" @click="startRecord">
                <el-icon><Microphone /></el-icon> 开始录音
              </el-button>
              <el-button v-else type="danger" @click="stopRecord">
                <el-icon><VideoPause /></el-icon> 停止录音
              </el-button>
              <el-upload :show-file-list="false" :auto-upload="false" accept="audio/*"
                         :on-change="onFileChange">
                <el-button>上传音频</el-button>
              </el-upload>
              <el-button v-if="audioBlob" @click="clearAudio">清空</el-button>
            </div>

            <div class="form-item">
              <label>ASR 模型</label>
              <el-select v-model="asrModel" style="width: 100%">
                <el-option v-for="m in asrModels" :key="m.code"
                           :label="m.displayName" :value="m.code" />
              </el-select>
            </div>
            <div class="form-item">
              <label>语言</label>
              <el-select v-model="language" style="width: 100%">
                <el-option label="中文" value="zh" />
                <el-option label="英文" value="en" />
                <el-option label="日文" value="ja" />
                <el-option label="中英混合" value="zh-en" />
              </el-select>
            </div>
            <el-button type="primary" :loading="transcribing" :disabled="!audioBlob"
                       @click="transcribe" block size="large">
              {{ transcribing ? '转写中...' : '🚀 开始转写' }}
            </el-button>
          </section>

          <section class="result-side">
            <h3>转写结果</h3>
            <div v-if="asrResult" class="result">
              <div class="result-meta">
                <el-tag :type="asrResult.mock ? 'info' : 'success'">
                  {{ asrResult.mock ? 'Mock' : '真实调用' }}
                </el-tag>
                <span>模型: {{ asrResult.model }}</span>
                <span>大小: {{ asrResult.sizeKb }} KB</span>
                <span>延迟: {{ asrResult.latencyMs }} ms</span>
              </div>
              <pre class="transcript">{{ asrResult.text }}</pre>
              <div v-if="asrResult.segments" class="segments">
                <h4>分段时间戳</h4>
                <div v-for="(seg, i) in asrResult.segments" :key="i" class="seg">
                  <span class="seg-time">[{{ (seg.start/1000).toFixed(1) }}s - {{ (seg.end/1000).toFixed(1) }}s]</span>
                  <span class="seg-text">{{ seg.text }}</span>
                </div>
              </div>
              <el-button-group style="margin-top: 12px">
                <el-button @click="copyText(asrResult.text)">复制</el-button>
                <el-button @click="speakText(asrResult.text)">用 TTS 朗读</el-button>
              </el-button-group>
            </div>
            <el-empty v-else description="录音后点 "🚀 开始转写"" />
          </section>
        </div>
      </el-tab-pane>

      <!-- TTS -->
      <el-tab-pane label="🔊 TTS (文字转语音)" name="tts">
        <div class="tts-grid">
          <section class="input-side">
            <h3>输入文字</h3>
            <el-input v-model="ttsText" type="textarea" :rows="6"
                      placeholder="输入要转换的文字..." maxlength="500" show-word-limit />
            <div class="quick-prompts">
              <el-tag v-for="qp in ttsQuickPrompts" :key="qp" @click="ttsText = qp" class="qp">
                {{ qp }}
              </el-tag>
            </div>

            <div class="form-item">
              <label>音色 ({{ ttsVoices.length }} 个)</label>
              <el-select v-model="ttsVoice" style="width: 100%" filterable>
                <el-option v-for="v in ttsVoices" :key="v.code"
                           :label="`${v.name} (${v.language})`" :value="v.code" />
              </el-select>
            </div>
            <div class="form-item">
              <label>语速: {{ ttsSpeed.toFixed(1) }}x</label>
              <el-slider v-model="ttsSpeed" :min="0.5" :max="2.0" :step="0.1" />
            </div>
            <el-button type="primary" :loading="ttsLoading" :disabled="!ttsText.trim()"
                       @click="synthesize" block size="large">
              {{ ttsLoading ? '合成中...' : '🔊 开始合成' }}
            </el-button>
          </section>

          <section class="result-side">
            <h3>合成结果</h3>
            <div v-if="ttsResult" class="result">
              <div class="result-meta">
                <el-tag :type="ttsResult.mock ? 'info' : 'success'">
                  {{ ttsResult.mock ? 'Mock' : '真实调用' }}
                </el-tag>
                <span>音色: {{ ttsResult.voice }}</span>
                <span>大小: {{ (ttsResult.sizeBytes / 1024).toFixed(1) }} KB</span>
                <span>延迟: {{ ttsResult.latencyMs }} ms</span>
              </div>
              <audio :src="ttsResult.audio" controls style="width: 100%; margin: 12px 0" />
              <p v-if="ttsResult.note" class="note">💡 {{ ttsResult.note }}</p>
              <el-button-group>
                <el-button @click="downloadAudio(ttsResult)">下载音频</el-button>
                <el-button @click="copyText(ttsResult.text)">复制文字</el-button>
              </el-button-group>
            </div>
            <el-empty v-else description="输入文字后点 "🔊 开始合成"" />
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, VideoPause } from '@element-plus/icons-vue'
import http from '@/api/http'

const activeTab = ref('asr')

// ===== ASR =====
const asrModels = ref([
  { code: 'whisper-large-v3', displayName: 'Whisper Large V3' },
  { code: 'FunAudioLLM/SenseVoiceSmall', displayName: 'SenseVoice Small' },
  { code: 'mock', displayName: 'Mock (沙箱演示)' },
])
const asrModel = ref('mock')
const language = ref('zh')
const recording = ref(false)
const recordSeconds = ref(0)
const audioBlob = ref(null)
const audioUrl = ref('')
const transcribing = ref(false)
const asrResult = ref(null)
let mediaRecorder = null
let recordTimer = null

async function startRecord() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    const chunks = []
    mediaRecorder.ondataavailable = (e) => chunks.push(e.data)
    mediaRecorder.onstop = () => {
      const blob = new Blob(chunks, { type: 'audio/webm' })
      audioBlob.value = blob
      audioUrl.value = URL.createObjectURL(blob)
      stream.getTracks().forEach(t => t.stop())
    }
    mediaRecorder.start()
    recording.value = true
    recordSeconds.value = 0
    recordTimer = setInterval(() => recordSeconds.value++, 1000)
  } catch (e) {
    ElMessage.error('麦克风权限被拒: ' + e.message)
  }
}
function stopRecord() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  recording.value = false
  if (recordTimer) { clearInterval(recordTimer); recordTimer = null }
}
function onFileChange(file) {
  audioBlob.value = file.raw
  audioUrl.value = URL.createObjectURL(file.raw)
  asrResult.value = null
}
function clearAudio() {
  audioBlob.value = null
  audioUrl.value = ''
  asrResult.value = null
}

async function transcribe() {
  if (!audioBlob.value) return
  transcribing.value = true
  try {
    const fd = new FormData()
    fd.append('file', audioBlob.value, 'recording.webm')
    fd.append('model', asrModel.value)
    fd.append('language', language.value)
    const r = await http.post('/api/v1/audio/asr/transcribe', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (r && r.data) {
      asrResult.value = r.data
      ElMessage.success('转写完成')
    }
  } catch (e) {
    ElMessage.error('转写失败: ' + e.message)
  } finally {
    transcribing.value = false
  }
}

// ===== TTS =====
const ttsVoices = ref([
  { code: 'zh-CN-XiaoxiaoNeural', name: '晓晓 (温柔女声)', language: 'zh-CN' },
  { code: 'zh-CN-YunxiNeural', name: '云希 (青年男声)', language: 'zh-CN' },
  { code: 'zh-CN-YunyangNeural', name: '云扬 (新闻男声)', language: 'zh-CN' },
  { code: 'en-US-JennyNeural', name: 'Jenny (US Female)', language: 'en-US' },
  { code: 'mock', name: 'Mock (沙箱演示)', language: 'zh' },
])
const ttsVoice = ref('zh-CN-XiaoxiaoNeural')
const ttsSpeed = ref(1.0)
const ttsText = ref('你好, 我是 MiniMax 智能助手. 这是 TTS 语音合成演示.')
const ttsLoading = ref(false)
const ttsResult = ref(null)

const ttsQuickPrompts = [
  '你好, 欢迎使用 MiniMax 大模型平台',
  '今天天气晴朗, 适合户外运动',
  'Hello, this is a text-to-speech demo',
  '人工智能正在改变世界',
]

async function synthesize() {
  if (!ttsText.value.trim()) return
  ttsLoading.value = true
  try {
    const r = await http.post('/api/v1/audio/tts/synthesize', {
      text: ttsText.value,
      voice: ttsVoice.value,
      speed: ttsSpeed.value,
    })
    if (r && r.data) {
      ttsResult.value = r.data
      ElMessage.success('合成完成! 点播放试听')
    }
  } catch (e) {
    ElMessage.error('合成失败: ' + e.message)
  } finally {
    ttsLoading.value = false
  }
}

function speakText(text) {
  // 复用 TTS
  ttsText.value = text
  synthesize()
}

async function copyText(text) {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

function downloadAudio(r) {
  const a = document.createElement('a')
  a.href = r.audio
  a.download = `tts-${r.voice}-${Date.now()}.${r.format || 'wav'}`
  a.click()
  ElMessage.success('已下载')
}

onUnmounted(() => {
  if (recordTimer) clearInterval(recordTimer)
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})
</script>

<style scoped>
.audio { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; border-radius: 12px; font-size: 12px; }
.asr-grid, .tts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 16px; }
.input-side, .result-side { background: #f8fafc; border-radius: 12px; padding: 20px; }
h3 { margin: 0 0 16px; }
.record-area { background: #fff; border-radius: 12px; padding: 24px;
  display: flex; align-items: center; justify-content: center; min-height: 140px;
  margin-bottom: 16px; }
.placeholder { text-align: center; color: #64748b; }
.recording { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #ef4444; }
.rec-pulse { width: 24px; height: 24px; border-radius: 50%; background: #ef4444;
  animation: pulse 1.2s infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.5); opacity: 0.5; } }
.actions { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.form-item { margin-bottom: 16px; }
label { display: block; font-weight: 600; margin-bottom: 6px; color: #334155; font-size: 13px; }
.quick-prompts { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.qp { cursor: pointer; }

.result-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; font-size: 13px; color: #64748b; margin-bottom: 12px; }
.transcript { background: #0f172a; color: #e2e8f0; padding: 16px; border-radius: 8px;
  white-space: pre-wrap; word-break: break-word; font-family: inherit; font-size: 14px; line-height: 1.7; max-height: 240px; overflow-y: auto; }
.segments { margin-top: 16px; }
.segments h4 { margin: 0 0 8px; font-size: 14px; }
.seg { display: flex; gap: 12px; padding: 6px 0; border-bottom: 1px solid #e2e8f0; font-size: 13px; }
.seg-time { color: #6366f1; font-weight: 600; min-width: 110px; }
.seg-text { color: #334155; }
.note { background: #fef3c7; padding: 8px 12px; border-radius: 6px; font-size: 13px; color: #92400e; }
</style>
