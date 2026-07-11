<template>
  <div class="music-stream">
    <el-card>
      <template #header>
        <div class="header">
          <span>🎵 音乐流式生成 (V2.8.1 - SSE 实时推送)</span>
          <el-button-group>
            <el-button type="primary" :loading="running" @click="startStream">▶️ 启动</el-button>
            <el-button type="danger" :disabled="!running" @click="cancel">⏹ 取消</el-button>
            <el-button @click="clearChunks">🗑 清空</el-button>
          </el-button-group>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 左侧配置 -->
        <el-col :span="7">
          <el-card shadow="never">
            <template #header>⚙️ 生成配置</template>
            <el-form :model="cfg" label-width="100px" size="small">
              <el-form-item label="风格">
                <el-select v-model="cfg.style">
                  <el-option v-for="s in styles" :key="s" :label="s" :value="s" />
                </el-select>
              </el-form-item>
              <el-form-item label="调式">
                <el-select v-model="cfg.key">
                  <el-option v-for="k in keys" :key="k" :label="k" :value="k" />
                </el-select>
              </el-form-item>
              <el-form-item label="大小调">
                <el-radio-group v-model="cfg.scale">
                  <el-radio-button label="major">大调</el-radio-button>
                  <el-radio-button label="minor">小调</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="BPM">
                <el-input-number v-model="cfg.bpm" :min="60" :max="240" />
              </el-form-item>
              <el-form-item label="小节数">
                <el-input-number v-model="cfg.bars" :min="1" :max="64" />
              </el-form-item>
              <el-form-item label="Chunk 小节">
                <el-input-number v-model="cfg.chunkBars" :min="1" :max="8" />
              </el-form-item>
              <el-form-item label="小节间隔 (ms)">
                <el-input-number v-model="cfg.interval" :min="50" :max="2000" :step="50" />
              </el-form-item>
              <el-alert type="info" :closable="false" show-icon style="margin-top: 8px">
                <template #title>将生成 {{ cfg.bars }} 小节, 分 {{ Math.ceil(cfg.bars / cfg.chunkBars) }} 块推送</template>
              </el-alert>
            </el-form>
          </el-card>
        </el-col>

        <!-- 中间: MIDI 块展示 -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span>🎼 MIDI 块</span>
                <el-tag :type="statusType">{{ statusText }}</el-tag>
              </div>
            </template>

            <el-progress v-if="progress > 0" :percentage="progress" :status="progressStatus" style="margin-bottom: 12px" />

            <div class="chunk-list">
              <el-card v-for="(c, i) in chunks" :key="i" class="chunk-card" shadow="hover">
                <div class="chunk-header">
                  <el-tag size="small" :type="i === chunks.length - 1 && running ? 'warning' : 'success'">
                    Chunk #{{ i + 1 }}
                  </el-tag>
                  <span class="chunk-info">
                    小节 {{ c.startBar + 1 }} - {{ c.startBar + c.bars }} ({{ c.bars }} bars)
                  </span>
                  <el-text size="small" type="info">{{ formatBytes(c.data.length) }}</el-text>
                </div>
                <div class="chunk-actions">
                  <el-button size="small" @click="downloadChunk(c, i)">💾 下载</el-button>
                </div>
              </el-card>
              <el-empty v-if="!chunks.length && !running" description="点击 ▶️ 启动接收 MIDI 块" />
              <el-empty v-else-if="!chunks.length" description="等待第一个 chunk..." />
            </div>

            <el-row :gutter="8" v-if="progress > 0" style="margin-top: 12px">
              <el-col :span="6"><el-statistic title="已生成" :value="chunks.length" suffix="块" /></el-col>
              <el-col :span="6"><el-statistic title="已完成" :value="info.doneBars" :suffix="`/${info.totalBars}`" /></el-col>
              <el-col :span="6"><el-statistic title="耗时" :value="info.elapsedMs" suffix="ms" /></el-col>
              <el-col :span="6"><el-statistic title="流量" :value="formatBytes(info.totalBytes)" /></el-col>
            </el-row>
          </el-card>
        </el-col>

        <!-- 右侧: 日志 -->
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
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelMusicStream } from '@/api/ai'

const cfg = ref({
  style: 'POP', key: 'C', scale: 'major', bpm: 120,
  bars: 8, chunkBars: 2, interval: 200
})

const styles = ['POP', 'CLASSICAL', 'ROCK', 'JAZZ', 'FOLK', 'ELECTRONIC']
const keys = ['C', 'D', 'E', 'F', 'G', 'A', 'B']
const running = ref(false)
const status = ref('IDLE')
const chunks = ref([])
const progress = ref(0)
const events = ref([])
const info = ref({ totalBars: 0, doneBars: 0, elapsedMs: 0, totalBytes: 0 })

const statusText = computed(() => ({
  IDLE: '待启动', CONNECTING: '连接中', RUNNING: '生成中', COMPLETED: '完成', FAILED: '失败', CANCELLED: '已取消'
}[status.value]))
const statusType = computed(() => ({
  IDLE: 'info', CONNECTING: 'warning', RUNNING: 'primary', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info'
}[status.value]))
const progressStatus = computed(() => {
  if (status.value === 'FAILED') return 'exception'
  if (status.value === 'COMPLETED') return 'success'
  return ''
})

let eventSource = null
let activeTaskId = null

function log(type, msg) {
  events.value.unshift({ type, msg, ts: new Date().toLocaleTimeString() })
  if (events.value.length > 50) events.value.length = 50
}

function logType(type) {
  return { start: 'primary', chunk: 'success', progress: 'info', complete: 'success', error: 'danger', heartbeat: 'info' }[type] || 'info'
}

function startStream() {
  if (eventSource) eventSource.close()
  chunks.value = []
  events.value = []
  progress.value = 0
  info.value = { totalBars: 0, doneBars: 0, elapsedMs: 0, totalBytes: 0 }
  status.value = 'CONNECTING'
  running.value = true

  const params = new URLSearchParams({
    style: cfg.value.style, key: cfg.value.key, scale: cfg.value.scale,
    bpm: cfg.value.bpm, bars: cfg.value.bars,
    chunkBars: cfg.value.chunkBars, interval: cfg.value.interval
  })
  const url = `/api/ai/music/stream/sse?${params}`
  log('info', 'Connecting ' + url)

  eventSource = new EventSource(url, { withCredentials: true })

  eventSource.addEventListener('start', (e) => {
    const d = JSON.parse(e.data)
    activeTaskId = d.taskId
    info.value.totalBars = d.totalBars
    status.value = 'RUNNING'
    log('start', `${d.style} ${d.key} ${d.scale} @ ${d.bpm}bpm x ${d.totalBars}bars`)
  })

  eventSource.addEventListener('chunk', (e) => {
    const d = JSON.parse(e.data)
    chunks.value.push(d)
  })

  eventSource.addEventListener('progress', (e) => {
    const d = JSON.parse(e.data)
    progress.value = d.percent
    info.value = { ...info.value, ...d }
    log('progress', `${d.percent}% (${d.done}/${d.total})`)
  })

  eventSource.addEventListener('complete', (e) => {
    const d = JSON.parse(e.data)
    status.value = 'COMPLETED'
    progress.value = 100
    running.value = false
    log('complete', `${formatBytes(d.totalBytes)} in ${d.durationMs}ms`)
    eventSource.close()
    eventSource = null
  })

  eventSource.addEventListener('error', (e) => {
    try { const d = JSON.parse(e.data); log('error', d.message || '未知') } catch { log('error', 'SSE 错误') }
    status.value = 'FAILED'
    running.value = false
    if (eventSource) { eventSource.close(); eventSource = null }
  })

  eventSource.addEventListener('heartbeat', () => {})
}

async function cancel() {
  if (!activeTaskId) return
  try {
    await cancelMusicStream(activeTaskId)
    status.value = 'CANCELLED'
    log('info', '已取消 ' + activeTaskId)
    if (eventSource) { eventSource.close(); eventSource = null }
    running.value = false
  } catch (e) { ElMessage.error('取消失败') }
}

function clearChunks() {
  chunks.value = []
  events.value = []
  progress.value = 0
  status.value = 'IDLE'
  info.value = { totalBars: 0, doneBars: 0, elapsedMs: 0, totalBytes: 0 }
}

function downloadChunk(c, i) {
  const bytes = Uint8Array.from(atob(c.data), ch => ch.charCodeAt(0))
  const blob = new Blob([bytes], { type: 'audio/midi' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `music-chunk-${i + 1}.mid`
  link.click()
  URL.revokeObjectURL(link.href)
}

function formatBytes(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

onUnmounted(() => { if (eventSource) eventSource.close() })
</script>

<style scoped>
.music-stream { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.chunk-list { max-height: 500px; overflow-y: auto; }
.chunk-card { margin-bottom: 8px; }
.chunk-header { display: flex; align-items: center; gap: 8px; }
.chunk-info { flex: 1; color: #666; font-size: 13px; }
.chunk-actions { margin-top: 8px; }
.log-list { max-height: 500px; overflow-y: auto; }
.log-item { display: flex; align-items: center; gap: 6px; padding: 4px; border-bottom: 1px solid #f5f5f5; font-size: 12px; }
.log-msg { flex: 1; color: #666; }
.log-ts { color: #999; font-size: 10px; }
</style>
