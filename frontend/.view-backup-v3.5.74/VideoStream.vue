<!--
  @file views/ai/VideoStream.vue (AI 流式对话 (VideoStream))
  @version V3.5.12+ (前端注释补全)
  @description AI 流式对话 (VideoStream)
-->
<template>
  <div class="video-stream">
    <el-card>
      <template #header>
        <div class="header">
          <span>🎬 视频流式生成 (V2.7.6 - SSE 实时推送)</span>
          <el-button-group>
            <el-button type="primary" :loading="running" @click="startStream">▶️ 启动</el-button>
            <el-button type="danger" :disabled="!running" @click="cancel">⏹ 取消</el-button>
            <el-button @click="clearFrames">🗑 清空</el-button>
          </el-button-group>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 左侧配置 -->
        <el-col :span="7">
          <el-card shadow="never">
            <template #header>⚙️ 生成配置</template>
            <el-form :model="cfg" label-width="100px" size="small">
              <el-form-item label="标题">
                <el-input v-model="cfg.title" />
              </el-form-item>
              <el-form-item label="宽度">
                <el-input-number v-model="cfg.width" :min="100" :max="1920" :step="40" />
              </el-form-item>
              <el-form-item label="高度">
                <el-input-number v-model="cfg.height" :min="100" :max="1080" :step="40" />
              </el-form-item>
              <el-form-item label="帧率">
                <el-input-number v-model="cfg.fps" :min="1" :max="60" />
              </el-form-item>
              <el-form-item label="时长 (秒)">
                <el-input-number v-model="cfg.duration" :min="1" :max="60" />
              </el-form-item>
              <el-form-item label="Chunk">
                <el-input-number v-model="cfg.chunkSize" :min="1" :max="50" />
              </el-form-item>
              <el-form-item label="帧间隔 (ms)">
                <el-input-number v-model="cfg.interval" :min="20" :max="500" :step="10" />
              </el-form-item>
              <el-alert type="info" :closable="false" show-icon style="margin-top: 8px">
                <template #title>总帧数: {{ cfg.fps * cfg.duration }}</template>
              </el-alert>
            </el-form>
          </el-card>
        </el-col>

        <!-- 中间预览 -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span>🖼️ 实时预览</span>
                <el-tag :type="statusType">{{ statusText }}</el-tag>
              </div>
            </template>

            <div class="preview">
              <div v-if="currentFrame" class="frame-display">
                <img :src="frameUrl" alt="frame" />
                <div class="frame-info">
                  Frame #{{ currentFrame.index + 1 }} / {{ totalFrames }}
                  <span v-if="!isLast"> · {{ remaining }} remaining</span>
                </div>
              </div>
              <el-empty v-else description="点击 ▶️ 启动 接收流式帧" />
            </div>

            <el-progress
              v-if="running || progress > 0"
              :percentage="progress"
              :status="progressStatus"
              style="margin-top: 12px"
            />

            <el-row :gutter="8" style="margin-top: 12px" v-if="progress > 0">
              <el-col :span="6">
                <el-statistic title="已生成" :value="frames.length" suffix="帧" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="耗时" :value="elapsed" suffix="ms" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="ETA" :value="eta" suffix="ms" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="流量" :value="formatBytes(totalBytes)" />
              </el-col>
            </el-row>
          </el-card>
        </el-col>

        <!-- 右侧日志 -->
        <el-col :span="5">
          <el-card shadow="never">
            <template #header>📋 事件日志</template>
            <div class="log-list">
              <div v-for="(e, i) in events" :key="i" :class="['log-item', `log-${e.type}`]">
                <el-tag size="small" :type="logType(e.type)">{{ e.type }}</el-tag>
                <span class="log-msg">{{ e.msg }}</span>
                <span class="log-ts">{{ e.ts }}</span>
              </div>
              <el-empty v-if="!events.length" description="暂无事件" :image-size="60" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelVideoStream } from '@/api/ai'

const cfg = ref({
  title: 'Liugl-AI Streaming Demo',
  width: 640,
  height: 360,
  fps: 12,
  duration: 4,
  chunkSize: 5,
  interval: 80
})

const running = ref(false)
const status = ref('IDLE')  // IDLE / CONNECTING / RUNNING / COMPLETED / FAILED / CANCELLED
const frames = ref([])
const currentFrame = ref(null)
const progress = ref(0)
const elapsed = ref(0)
const eta = ref(0)
const totalBytes = ref(0)
const events = ref([])
const totalFrames = computed(() => cfg.value.fps * cfg.value.duration)
const isLast = computed(() => currentFrame.value?.isLast === true)
const remaining = computed(() => totalFrames.value - frames.value.length)
const frameUrl = computed(() => currentFrame.value ? `data:image/png;base64,${currentFrame.value.data}` : '')
const statusText = computed(() => {
  return { IDLE: '待启动', CONNECTING: '连接中', RUNNING: '生成中', COMPLETED: '完成', FAILED: '失败', CANCELLED: '已取消' }[status.value]
})
const statusType = computed(() => {
  return { IDLE: 'info', CONNECTING: 'warning', RUNNING: 'primary', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info' }[status.value]
})
const progressStatus = computed(() => {
  if (status.value === 'FAILED') return 'exception'
  if (status.value === 'COMPLETED') return 'success'
  return ''
})

let eventSource = null
let activeTaskId = null

function log(type, msg) {
  events.value.unshift({
    type,
    msg,
    ts: new Date().toLocaleTimeString()
  })
  if (events.value.length > 50) events.value.length = 50
}

function logType(type) {
  return { start: 'primary', frame: 'success', progress: 'info', complete: 'success', error: 'danger', heartbeat: 'info' }[type] || 'info'
}

function startStream() {
  if (eventSource) {
    eventSource.close()
  }
  frames.value = []
  events.value = []
  progress.value = 0
  totalBytes.value = 0
  currentFrame.value = null
  status.value = 'CONNECTING'
  running.value = true

  const params = new URLSearchParams({
    title: cfg.value.title,
    width: cfg.value.width,
    height: cfg.value.height,
    fps: cfg.value.fps,
    duration: cfg.value.duration,
    chunkSize: cfg.value.chunkSize,
    interval: cfg.value.interval
  })

  const url = `/api/ai/video/stream/sse?${params}`
  log('info', 'Connecting to ' + url)

  // SSE 用原生 EventSource (axios 不支持流式)
  eventSource = new EventSource(url, { withCredentials: true })

  eventSource.addEventListener('start', (e) => {
    const data = JSON.parse(e.data)
    activeTaskId = data.taskId
    status.value = 'RUNNING'
    log('start', `taskId=${data.taskId}, ${data.totalFrames} frames`)
  })

  eventSource.addEventListener('frame', (e) => {
    const data = JSON.parse(e.data)
    currentFrame.value = data
    frames.value.push(data)
    totalBytes.value += data.data.length
  })

  eventSource.addEventListener('progress', (e) => {
    const data = JSON.parse(e.data)
    progress.value = data.percent
    elapsed.value = data.elapsedMs
    eta.value = data.etaMs
    log('progress', `${data.percent}% (${data.done}/${data.total})`)
  })

  eventSource.addEventListener('complete', (e) => {
    const data = JSON.parse(e.data)
    status.value = 'COMPLETED'
    progress.value = 100
    running.value = false
    log('complete', `总时长 ${data.durationMs}ms, ${formatBytes(data.totalBytes)}`)
    eventSource.close()
    eventSource = null
  })

  eventSource.addEventListener('error', (e) => {
    try {
      const data = JSON.parse(e.data)
      log('error', data.message || '未知错误')
    } catch {
      log('error', 'SSE 连接错误')
    }
    status.value = 'FAILED'
    running.value = false
    if (eventSource) { eventSource.close(); eventSource = null }
  })

  eventSource.addEventListener('heartbeat', (e) => {
    // 不打日志避免刷屏
  })
}

async function cancel() {
  if (!activeTaskId) return
  try {
    await cancelVideoStream(activeTaskId)
    status.value = 'CANCELLED'
    log('info', '已取消 ' + activeTaskId)
    if (eventSource) { eventSource.close(); eventSource = null }
    running.value = false
  } catch (e) {
    ElMessage.error('取消失败')
  }
}

function clearFrames() {
  frames.value = []
  currentFrame.value = null
  progress.value = 0
  events.value = []
  status.value = 'IDLE'
  totalBytes.value = 0
  elapsed.value = 0
  eta.value = 0
}

function formatBytes(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

onUnmounted(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.video-stream { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.preview {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 4px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.frame-display { width: 100%; text-align: center; }
.frame-display img { max-width: 100%; max-height: 400px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.frame-info { margin-top: 8px; color: #666; font-size: 14px; }
.log-list { max-height: 500px; overflow-y: auto; }
.log-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-bottom: 1px solid #f5f5f5;
  font-size: 12px;
}
.log-msg { flex: 1; color: #666; }
.log-ts { color: #999; font-size: 10px; }
</style>
